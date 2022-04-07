package com.mantledillusion.essentials.camunda.migration;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.migration.MigrationPlan;
import org.camunda.bpm.engine.migration.MigrationPlanBuilder;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceModificationBuilder;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.camunda.commons.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class ProcessMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessMigration.class);

    public interface FilteringBuilder<This> {

        This whereActive();

        This whereSuspended();

        This whereDefinitionId(String definitionId);

        This whereDefinitionKey(String definitionKey);

        This whereVersionTag(String versionTag);

        This whereVersion(int version);

        This whereActivity(String activityName);

        This whereIncident();

        This whereVariableEquals(String variableName, Object value);
    }

    public static class ScenarioBuilder<Parent> implements FilteringBuilder<ScenarioBuilder<Parent>> {

        public static class SubScenarioBuilder<Parent> {

            private final ScenarioBuilder<Parent> scenario;

            private SubScenarioBuilder(ScenarioBuilder<Parent> scenario) {
                this.scenario = scenario;
            }

            public ScenarioBuilder<SubScenarioBuilder<Parent>> defineScenario() {
                return defineScenario("unnamed");
            }

            public ScenarioBuilder<SubScenarioBuilder<Parent>> defineScenario(String title) {
                ScenarioBuilder<SubScenarioBuilder<Parent>> scenarioBuilder = new ScenarioBuilder<>(this, title);
                this.scenario.addMigrator((migration, report) -> report.add(scenarioBuilder.migrate(migration)));
                return scenarioBuilder;
            }

            public Parent finalizeScenarios() {
                return this.scenario.parent;
            }
        }

        private final Parent parent;
        private final String title;
        private final List<Consumer<ProcessMigration>> adaptors = new ArrayList<>();
        private final List<BiConsumer<ProcessMigration, BranchReport>> migrators = new ArrayList<>();

        private ScenarioBuilder(Parent parent, String title) {
            this.parent = parent;
            this.title = title;
        }

        @Override
        public ScenarioBuilder<Parent> whereActive() {
            return addAdaptor(migration -> migration.addProcessPreFilter(ProcessInstanceQuery::active));
        }

        @Override
        public ScenarioBuilder<Parent> whereSuspended() {
            return addAdaptor(migration -> migration.addProcessPreFilter(ProcessInstanceQuery::suspended));
        }

        @Override
        public ScenarioBuilder<Parent> whereDefinitionId(String definitionId) {
            return addAdaptor(migration -> migration.addProcessPreFilter(query -> query.processDefinitionId(definitionId)));
        }

        @Override
        public ScenarioBuilder<Parent> whereDefinitionKey(String definitionKey) {
            return addAdaptor(migration -> migration.addProcessPreFilter(query -> query.processDefinitionKey(definitionKey)));
        }

        @Override
        public ScenarioBuilder<Parent> whereVersionTag(String versionTag) {
            return addAdaptor(migration -> migration.addProcessPostFilter((definition, instance) -> Objects.equals(definition.getVersionTag(), versionTag)));
        }

        @Override
        public ScenarioBuilder<Parent> whereVersion(int version) {
            return addAdaptor(migration -> migration.addProcessPostFilter((definition, instance) -> definition.getVersion() == version));
        }

        @Override
        public ScenarioBuilder<Parent> whereActivity(String activityId) {
            return addAdaptor(migration -> migration.addProcessPreFilter(query -> query.activityIdIn(activityId)));
        }

        @Override
        public ScenarioBuilder<Parent> whereIncident() {
            return addAdaptor(migration -> migration.addProcessPreFilter(ProcessInstanceQuery::withIncident));
        }

        @Override
        public ScenarioBuilder<Parent> whereVariableEquals(String variableName, Object value) {
            return addAdaptor(migration -> migration.addProcessPreFilter(query -> query.variableValueEquals(variableName, value)));
        }

        /**
         * Upon failing migration in this scenario, the rest of the scenario will be skipped.
         * <p>
         * If using {@link #defineScenarios()} in this scenario and one sub scenario fails, all subsequent
         * sub scenarios within this scenario will be skipped.
         * <p>
         * If using {@link #finalizeScenario()} in this scenario and migrating one process instance fails, all
         * subsequent process instances within this scenario will be skipped.
         *
         * @param skip True if the rest of the scenario should be skipped upon failure, false otherwise.
         * @return This
         */
        public ScenarioBuilder<Parent> onFailureSkip(boolean skip) {
            return addAdaptor(migration -> migration.setSkip(skip));
        }

        /**
         * Upon failing migration in this scenario, all process instances handled in the rest of this scenario
         * will be suspended.
         * <p>
         * If using {@link #finalizeScenario()} in this scenario and migrating one process instance fails, all
         * subsequent process instances within this scenario will be suspended.
         *
         * @param suspend The if the rest of the scenario's process instances should be suspended upon failure, false otherwise.
         * @return This
         */
        public ScenarioBuilder<Parent> onFailureSuspend(boolean suspend) {
            return addAdaptor(migration -> migration.setSuspend(suspend));
        }

        public PredicateBuilder<Parent> when() {
            return when("unnamed");
        }

        public PredicateBuilder<Parent> when(String title) {
            ProcessInstanceAdjustment adjustment = new ProcessInstanceAdjustment(title);
            this.adaptors.add(migration -> migration.addInstanceAdjustment(adjustment));
            return new PredicateBuilder<>(this, adjustment);
        }

        public ScenarioBuilder<Parent> toDefinitionId(String definitionId) {
            return addAdaptor(migration -> migration.addDefinitionFilter(query -> query.processDefinitionId(definitionId)));
        }

        public ScenarioBuilder<Parent> toDefinitionKey(String definitionKey) {
            return addAdaptor(migration -> migration.addDefinitionFilter(query -> query.processDefinitionKey(definitionKey)));
        }

        public ScenarioBuilder<Parent> toDefinitionTag(String definitionTag) {
            return addAdaptor(migration -> migration.addDefinitionFilter(query -> query.versionTag(definitionTag)));
        }

        public ScenarioBuilder<Parent> toSpecificDefinitionVersion(int definitionVersion) {
            return addAdaptor(migration -> migration.addDefinitionFilter(query -> query.processDefinitionVersion(definitionVersion)));
        }

        public ScenarioBuilder<Parent> toLatestDefinitionVersion() {
            return addAdaptor(migration -> migration.addDefinitionFilter(ProcessDefinitionQuery::latestVersion));
        }

        public ScenarioBuilder<Parent> usingDefaultMappings() {
            return addAdaptor(migration -> migration.addMigrationPlanAdjustment(MigrationPlanBuilder::mapEqualActivities));
        }

        public ScenarioBuilder<Parent> usingMapping(String sourceActivityId, String targetActivityId) {
            return addAdaptor(migration -> migration.addMigrationPlanAdjustment(plan -> plan.mapActivities(sourceActivityId, targetActivityId)));
        }

        public SubScenarioBuilder<Parent> defineScenarios() {
            return new SubScenarioBuilder<>(this);
        }

        public Parent finalizeScenario() {
            addMigrator(ProcessMigration::migrate);
            return this.parent;
        }

        private <A extends Consumer<ProcessMigration>> ScenarioBuilder<Parent> addAdaptor(A adaptor) {
            this.adaptors.add(adaptor);
            return this;
        }

        private <M extends BiConsumer<ProcessMigration, BranchReport>> void addMigrator(M migrator) {
            this.migrators.add(migrator);
        }

        private BranchReport migrate(ProcessMigration processMigration) {
            // ADAPT MIGRATION
            ProcessMigration scenarioMigration = processMigration.copy();
            this.adaptors.forEach(adaptor -> adaptor.accept(scenarioMigration));

            // MIGRATE SCENARIO
            BranchReport scenarioReport = new BranchReport("Scenario: " + this.title);
            for (BiConsumer<ProcessMigration, BranchReport> migrator: this.migrators) {
                migrator.accept(scenarioMigration, scenarioReport);
                if (scenarioMigration.skip && !scenarioReport.isSuccess()) {
                    scenarioReport.add(new LeafReport("Skipping configured; scenario will not continue any further", true));
                    break;
                }
            }

            return scenarioReport;
        }
    }

    private static final class ActivityModification implements BiConsumer<RuntimeService, String> {

        private final List<Consumer<ProcessInstanceModificationBuilder>> modifications;

        private ActivityModification(List<Consumer<ProcessInstanceModificationBuilder>> modifications) {
            this.modifications = modifications;
        }

        @Override
        public void accept(RuntimeService runtimeService, String processInstanceId) {
            ProcessInstanceModificationBuilder builder = runtimeService
                    .createProcessInstanceModification(processInstanceId);

            this.modifications.forEach(modification -> modification.accept(builder));

            builder.execute();
        }
    }

    private static final class MessageModification implements BiConsumer<RuntimeService, String> {

        private final String messageName;
        private final boolean all;
        private final List<Consumer<MessageCorrelationBuilder>> modifications;

        private MessageModification(String messageName, boolean all, List<Consumer<MessageCorrelationBuilder>> modifications) {
            this.messageName = messageName;
            this.all = all;
            this.modifications = modifications;
        }

        @Override
        public void accept(RuntimeService runtimeService, String processInstanceId) {
            MessageCorrelationBuilder builder = runtimeService
                    .createMessageCorrelation(this.messageName)
                    .processInstanceId(processInstanceId);

            this.modifications.forEach(modification -> modification.accept(builder));

            if (this.all) {
                builder.correlateAll();
            } else {
                builder.correlate();
            }
        }
    }

    private static final class ProcessInstanceAdjustment {

        private final String title;
        private final List<ProcessPredicate> predicates = new ArrayList<>();
        private final List<BiConsumer<RuntimeService, String>> before = new ArrayList<>();
        private final List<BiConsumer<RuntimeService, String>> after = new ArrayList<>();

        private ProcessInstanceAdjustment(String title) {
            this.title = title;
        }

        private void addPredicate(ProcessPredicate predicate) {
            this.predicates.add(predicate);
        }

        private boolean applies(Report report, RuntimeService service, ProcessDefinition definition, ProcessInstance instance) {
            if (this.predicates.stream().allMatch(predicate -> predicate.test(service, definition, instance))) {
                report.getChildren().add(new LeafReport(this.title, true));
                return true;
            } else {
                report.getChildren().add(new LeafReport(this.title, true));
                return false;
            }
        }

        private void addApplyBefore(BiConsumer<RuntimeService, String> modification) {
            this.before.add(modification);
        }

        public List<BiConsumer<RuntimeService, String>> getApplyBefore() {
            return this.before;
        }

        private void applyAfter(BiConsumer<RuntimeService, String> modification) {
            this.after.add(modification);
        }

        public List<BiConsumer<RuntimeService, String>> getApplyAfter() {
            return this.after;
        }
    }

    private interface ProcessPredicate {

        boolean test(RuntimeService service, ProcessDefinition definition, ProcessInstance instance);
    }

    public static class PredicateBuilder<Parent> implements FilteringBuilder<PredicateBuilder<Parent>> {

        private final ScenarioBuilder<Parent> parent;
        private final ProcessInstanceAdjustment adjustment;

        private PredicateBuilder(ScenarioBuilder<Parent> parent, ProcessInstanceAdjustment adjustment) {
            this.parent = parent;
            this.adjustment = adjustment;
        }

        @Override
        public PredicateBuilder<Parent> whereActive() {
            return addPredicate((service, definition, instance) -> !instance.isSuspended());
        }

        @Override
        public PredicateBuilder<Parent> whereSuspended() {
            return addPredicate((service, definition, instance) -> instance.isSuspended());
        }

        @Override
        public PredicateBuilder<Parent> whereDefinitionId(String definitionId) {
            return addPredicate((service, definition, instance) -> definition.getId().equals(definitionId));
        }

        @Override
        public PredicateBuilder<Parent> whereDefinitionKey(String definitionKey) {
            return addPredicate((service, definition, instance) -> definition.getKey().equals(definitionKey));
        }

        @Override
        public PredicateBuilder<Parent> whereVersionTag(String versionTag) {
            return addPredicate((service, definition, instance) -> definition.getVersionTag().equals(versionTag));
        }

        @Override
        public PredicateBuilder<Parent> whereVersion(int version) {
            return addPredicate((service, definition, instance) -> definition.getVersion() == version);
        }

        @Override
        public PredicateBuilder<Parent> whereActivity(String activityId) {
            return addPredicate((service, definition, instance) -> service.getActiveActivityIds(instance.getId()).contains(activityId));
        }

        @Override
        public PredicateBuilder<Parent> whereIncident() {
            return addPredicate((service, definition, instance) -> service.getActivityInstance(instance.getId()).getIncidents().length > 0);
        }

        @Override
        public PredicateBuilder<Parent> whereVariableEquals(String variableName, Object value) {
            return addPredicate((service, definition, instance) -> Objects.equals(service.getVariable(instance.getId(), variableName), value));
        }

        private PredicateBuilder<Parent> addPredicate(ProcessPredicate predicate) {
            this.adjustment.addPredicate(predicate);
            return this;
        }

        public BeforeMigrationManipulationBuilder<Parent> beforeMigrate() {
            return new BeforeMigrationManipulationBuilder<>(this.parent, this.adjustment);
        }

        public AfterMigrationManipulationBuilder<Parent> afterMigrate() {
            return new AfterMigrationManipulationBuilder<>(this.parent, this.adjustment);
        }
    }

    @SuppressWarnings("unchecked")
    public static abstract class AbstractManipulationBuilder<Parent, This> {

        public final class ActivityManipulationBuilder {

            private final List<Consumer<ProcessInstanceModificationBuilder>> modifications = new ArrayList<>();

            private ActivityManipulationBuilder() {}

            public ActivityManipulationBuilder cancelActivityInstance(String activityInstanceId) {
                this.modifications.add(builder -> builder.cancelActivityInstance(activityInstanceId));
                return this;
            }

            public ActivityManipulationBuilder cancelAllForActivity(String activityId) {
                this.modifications.add(builder -> builder.cancelAllForActivity(activityId));
                return this;
            }

            public ActivityManipulationBuilder cancelTransitionInstance(String transitionInstanceId) {
                this.modifications.add(builder -> builder.cancelTransitionInstance(transitionInstanceId));
                return this;
            }

            public ActivityManipulationBuilder startBeforeActivity(String activityId) {
                this.modifications.add(builder -> builder.startBeforeActivity(activityId));
                return this;
            }

            public ActivityManipulationBuilder startAfterActivity(String activityId) {
                this.modifications.add(builder -> builder.startAfterActivity(activityId));
                return this;
            }

            public ActivityManipulationBuilder startTransition(String activityId) {
                this.modifications.add(builder -> builder.startTransition(activityId));
                return this;
            }

            public This then() {
                addModification(new ActivityModification(this.modifications));
                return (This) AbstractManipulationBuilder.this;
            }
        }

        public final class MessageManipulationBuilder {

            private final String messageName;
            private final List<Consumer<MessageCorrelationBuilder>> modifications = new ArrayList<>();

            private MessageManipulationBuilder(String messageName) {
                this.messageName = messageName;
            }

            public MessageManipulationBuilder withVariable(String variableName, Object value) {
                this.modifications.add(builder -> builder.setVariable(variableName, value));
                return this;
            }

            public MessageManipulationBuilder withLocalVariable(String variableName, Object value) {
                this.modifications.add(builder -> builder.setVariableLocal(variableName, value));
                return this;
            }

            public This toOne() {
                addModification(new MessageModification(messageName, false, this.modifications));
                return (This) AbstractManipulationBuilder.this;
            }

            public This toAll() {
                addModification(new MessageModification(messageName, true, this.modifications));
                return (This) AbstractManipulationBuilder.this;
            }
        }

        private final ScenarioBuilder<Parent> parent;
        private final Consumer<BiConsumer<RuntimeService, String>> modifications;

        private AbstractManipulationBuilder(ScenarioBuilder<Parent> parent, Consumer<BiConsumer<RuntimeService, String>> modifications) {
            this.parent = parent;
            this.modifications = modifications;
        }

        public ActivityManipulationBuilder modify() {
            return new ActivityManipulationBuilder();
        }

        public MessageManipulationBuilder correlate(String messageName) {
            return new MessageManipulationBuilder(messageName);
        }

        public This setVariable(String variableName, Object value) {
            return addModification(((runtimeService, instanceId) -> runtimeService.setVariable(instanceId, variableName, value)));
        }

        public This removeVariable(String variableName) {
            return addModification(((runtimeService, instanceId) -> runtimeService.removeVariable(instanceId, variableName)));
        }

        private This addModification(BiConsumer<RuntimeService, String> modification) {
            this.modifications.accept(modification);
            return (This) this;
        }

        public ScenarioBuilder<Parent> then() {
            return this.parent;
        }
    }

    public static class BeforeMigrationManipulationBuilder<Parent> extends AbstractManipulationBuilder<Parent, BeforeMigrationManipulationBuilder<Parent>> {

        private final ProcessInstanceAdjustment adjustment;

        private BeforeMigrationManipulationBuilder(ScenarioBuilder<Parent> parent, ProcessInstanceAdjustment adjustment) {
            super(parent, adjustment::addApplyBefore);
            this.adjustment = adjustment;
        }

        public AfterMigrationManipulationBuilder<Parent> afterMigrate() {
            return new AfterMigrationManipulationBuilder<>(this.then(), this.adjustment);
        }
    }

    public static class AfterMigrationManipulationBuilder<Parent> extends AbstractManipulationBuilder<Parent, AfterMigrationManipulationBuilder<Parent>> {

        private AfterMigrationManipulationBuilder(ScenarioBuilder<Parent> parent, ProcessInstanceAdjustment adjustment) {
            super(parent, adjustment::applyAfter);
        }
    }

    public static class EngineBuilder {

        private final ProcessEngine processEngine;

        private EngineBuilder(ProcessEngine processEngine) {
            this.processEngine = processEngine;
        }

        public ScenarioBuilder<ExecutionBuilder> defineScenario() {
            return defineScenario("root");
        }

        public ScenarioBuilder<ExecutionBuilder> defineScenario(String title) {
            ExecutionBuilder executionBuilder = new ExecutionBuilder(this.processEngine);
            ScenarioBuilder<ExecutionBuilder> scenarioBuilder = new ScenarioBuilder<>(executionBuilder, title);

            executionBuilder.rootScenario = scenarioBuilder;

            return scenarioBuilder;
        }
    }

    public static class ExecutionBuilder {

        private final ProcessEngine processEngine;
        private ScenarioBuilder<ExecutionBuilder> rootScenario;

        private boolean log = true;

        private ExecutionBuilder(ProcessEngine processEngine) {
            this.processEngine = processEngine;
        }

        public ExecutionBuilder log(boolean log) {
            this.log = log;
            return this;
        }

        public Report migrate() {
            Report rootReport = this.rootScenario.migrate(new ProcessMigration(this.processEngine,
                    // DEFAULT SKIP
                    new AtomicInteger(0), false,
                    // DEFAULT SUSPENSION
                    false,
                    // DEFAULT DEFINITION FILTERS
                    new ArrayList<>(),
                    // DEFAULT INSTANCE PRE FILTERS
                    new ArrayList<>(),
                    // DEFAULT INSTANCE POST FILTERS
                    new ArrayList<>(),
                    // DEFAULT INSTANCE ADJUSTMENTS
                    new ArrayList<>(),
                    // DEFAULT MIGRATION PLAN ADJUSTMENTS
                    new ArrayList<>()
            ));

            if (this.log) {
                log(rootReport, rootReport.isSuccess() ? LOGGER::info : LOGGER::error);
            }

            return rootReport;
        }

        private void log(Report report, Consumer<String> logger) {
            logger.accept("Camunda process migration completed migrating " + report.count() + " process(es):\n"+report.prettyPrint(true));
        }
    }

    public static abstract class Report {

        private final String title;

        private Report(String title) {
            this.title = title;
        }

        public String getTitle() {
            return this.title;
        }

        public abstract boolean isSuccess();

        public abstract int count();

        public abstract List<Report> getChildren();

        public String prettyPrint(boolean wrap) {
            return prettyPrint(0, wrap ? "" : "[", wrap ? "\n" : ", ", wrap ? "" : "]");
        }

        protected abstract String prettyPrint(int layer, String prefix, String infix, String postFix);

        protected String getPadding(int layer) {
            return IntStream.range(0, layer).mapToObj(i -> "  ").reduce(String::concat).orElse("");
        }

        @Override
        public final String toString() {
            return prettyPrint(false);
        }
    }

    public static final class BranchReport extends Report {

        private final List<Report> children = new ArrayList<>();

        private BranchReport(String title) {
            super(title);
        }

        private <C extends Report> C add(C child) {
            this.children.add(child);
            return child;
        }

        @Override
        public boolean isSuccess() {
            return this.children.stream().allMatch(Report::isSuccess);
        }

        @Override
        public int count() {
            return this.children.stream().map(Report::count).reduce(Integer::sum).orElse(0);
        }

        @Override
        public List<Report> getChildren() {
            return this.children;
        }

        @Override
        protected String prettyPrint(int layer, String prefix, String infix, String postFix) {
            String[] report = Stream
                    .concat(
                            Stream.of(getPadding(layer) + getTitle()),
                            this.children.stream().map(child -> child.prettyPrint(layer+1, prefix, infix, postFix))
                    )
                    .toArray(String[]::new);

            return prefix + StringUtil.join(infix, report) + postFix;
        }
    }

    public static final class LeafReport extends Report {

        private final boolean success;
        private final boolean migrated;

        private LeafReport(String title, boolean success) {
            this(title, success, false);
        }

        private LeafReport(String title, boolean success, boolean migrated) {
            super(title);
            this.success = success;
            this.migrated = migrated;
        }

        @Override
        public boolean isSuccess() {
            return this.success;
        }

        @Override
        public int count() {
            return this.migrated ? 1 : 0;
        }

        @Override
        public List<Report> getChildren() {
            return Collections.emptyList();
        }

        @Override
        protected String prettyPrint(int layer, String prefix, String infix, String postFix) {
            return prefix + getPadding(layer) + (this.success ? "(succ) " : "(fail) ") + getTitle() + postFix;
        }
    }

    private final ProcessEngine processEngine;
    private final AtomicInteger referenceGenerator;

    private final List<Consumer<ProcessDefinitionQuery>> definitionFilters;
    private final List<Consumer<ProcessInstanceQuery>> instancePreFilters;
    private final List<BiPredicate<ProcessDefinition, ProcessInstance>> instancePostFilters;
    private final List<ProcessInstanceAdjustment> instanceAdjustments;
    private final List<Consumer<MigrationPlanBuilder>> migrationPlanAdjustments;

    private boolean skip;
    private boolean suspend;

    private ProcessMigration(ProcessEngine processEngine, AtomicInteger referenceGenerator,
                             boolean skip, boolean suspend,
                             List<Consumer<ProcessDefinitionQuery>> definitionFilters,
                             List<Consumer<ProcessInstanceQuery>> instancePreFilters,
                             List<BiPredicate<ProcessDefinition, ProcessInstance>> instancePostFilters,
                             List<ProcessInstanceAdjustment> instanceAdjustments,
                             List<Consumer<MigrationPlanBuilder>> migrationPlanAdjustments) {
        this.processEngine = processEngine;
        this.referenceGenerator = referenceGenerator;

        this.skip = skip;
        this.suspend = suspend;

        this.definitionFilters = definitionFilters;
        this.instancePreFilters = instancePreFilters;
        this.instancePostFilters = instancePostFilters;
        this.instanceAdjustments = instanceAdjustments;
        this.migrationPlanAdjustments = migrationPlanAdjustments;
    }

    private void setSkip(boolean skip) {
        this.skip = skip;
    }

    private void setSuspend(boolean suspend) {
        this.suspend = suspend;
    }

    private void addDefinitionFilter(Consumer<ProcessDefinitionQuery> definitionFilter) {
        this.definitionFilters.add(definitionFilter);
    }

    private void addProcessPreFilter(Consumer<ProcessInstanceQuery> preFilter) {
        this.instancePreFilters.add(preFilter);
    }

    private void addProcessPostFilter(BiPredicate<ProcessDefinition, ProcessInstance> postFilter) {
        this.instancePostFilters.add(postFilter);
    }

    private void addInstanceAdjustment(ProcessInstanceAdjustment adjustment) {
        this.instanceAdjustments.add(adjustment);
    }

    private void addMigrationPlanAdjustment(Consumer<MigrationPlanBuilder> adjustment) {
        this.migrationPlanAdjustments.add(adjustment);
    }

    private ProcessMigration copy() {
        return new ProcessMigration(this.processEngine, this.referenceGenerator,
                this.skip, this.suspend,
                new ArrayList<>(this.definitionFilters),
                new ArrayList<>(this.instancePreFilters),
                new ArrayList<>(this.instancePostFilters),
                new ArrayList<>(this.instanceAdjustments),
                new ArrayList<>(this.migrationPlanAdjustments));
    }

    private void migrate(BranchReport scenarioReport) {
        // FILTER TARGET DEFINITIONS IN THE ENGINE
        ProcessDefinitionQuery definitionQuery = this.processEngine.getRepositoryService()
                .createProcessDefinitionQuery();
        this.definitionFilters.forEach(filter -> filter.accept(definitionQuery));

        if (definitionQuery.count() != 1) {
            scenarioReport.add(new LeafReport("Found " + definitionQuery.count() + " definitions matching definition filters", false));
        } else {
            ProcessDefinition targetDefinition = definitionQuery.list().iterator().next();
            scenarioReport.add(new LeafReport("Found 1 definition matching definition filters: " + targetDefinition.getId(), true));

            // PRE FILTER PROCESSES IN THE ENGINE
            ProcessInstanceQuery query = this.processEngine.getRuntimeService()
                    .createProcessInstanceQuery();
            this.instancePreFilters.forEach(filter -> filter.accept(query));

            // POST FILTER PROCESSES IN MEMORY
            List<ProcessInstance> instances = query.list().stream()
                    .filter(instance -> {
                        ProcessDefinition sourceDefinition = this.processEngine.getRepositoryService()
                                .getProcessDefinition(instance.getProcessDefinitionId());

                        return this.instancePostFilters.stream().allMatch(filter -> filter.test(sourceDefinition, instance));
                    })
                    .collect(Collectors.toList());

            // MIGRATE INSTANCES
            if (instances.isEmpty()) {
                scenarioReport.add(new LeafReport("Found no instances matching instance filters", true));
            } else {
                scenarioReport.add(new LeafReport("Found " + instances.size() + " instance(s) matching instance filters", true));

                // MIGRATE EVERY INSTANCE
                for (ProcessInstance instance: instances) {
                    BranchReport instanceReport = scenarioReport.add(new BranchReport("Instance: " + instance.getId()));

                    // CREATE MIGRATION PLAN
                    MigrationPlanBuilder builder = this.processEngine.getRuntimeService()
                            .createMigrationPlan(instance.getProcessDefinitionId(), targetDefinition.getId());

                    // ADJUST MIGRATION PLAN
                    this.migrationPlanAdjustments.forEach(adjustment -> adjustment.accept(builder));

                    // BUILD MIGRATION PLAN
                    MigrationPlan plan = builder.build();

                    List<ProcessInstanceAdjustment> adjustments = this.instanceAdjustments.stream()
                            .filter(adjustment -> adjustment.applies(instanceReport, this.processEngine.getRuntimeService(), targetDefinition, instance))
                            .collect(Collectors.toList());

                    // ADJUST INSTANCE BEFORE MIGRATION
                    boolean success = applyAdjustments(instance, adjustments, true, instanceReport);
                    if (!success) {
                        if (this.skip) {
                            scenarioReport.add(new LeafReport("Skipping configured; no further process instances will be migrated", true));
                            break;
                        } else {
                            continue;
                        }
                    }

                    // EXECUTE MIGRATION
                    try {
                        long ms = System.currentTimeMillis();
                        this.processEngine.getRuntimeService()
                                .newMigration(plan)
                                .processInstanceIds(instance.getId())
                                .execute();
                        ms = System.currentTimeMillis() - ms;

                        instanceReport.add(new LeafReport("Instance migration successful in " + ms + "ms", true, true));
                    } catch (Exception e) {
                        int referenceId = log(e);
                        instanceReport.add(new LeafReport("Instance migration failed (referenceId=#" + referenceId + ")", false));

                        if (this.suspend && !instance.isSuspended()) {
                            this.processEngine.getRuntimeService().suspendProcessInstanceById(instance.getId());
                            instanceReport.add(new LeafReport("Suspension configured; process instance is now suspended", true));
                        }

                        if (this.skip) {
                            scenarioReport.add(new LeafReport("Skipping configured; no further process instances will be migrated", true));
                            break;
                        } else {
                            continue;
                        }
                    }

                    // ADJUST INSTANCE AFTER MIGRATION
                    success = applyAdjustments(instance, adjustments, false, instanceReport);
                    if (!success && this.skip) {
                        break;
                    }
                }
            }
        }
    }

    private boolean applyAdjustments(ProcessInstance instance, List<ProcessInstanceAdjustment> adjustments,
                                     boolean before, BranchReport instanceReport) {
        for (ProcessInstanceAdjustment adjustment: adjustments) {
            for (BiConsumer<RuntimeService, String> modification: (before ? adjustment.getApplyBefore() : adjustment.getApplyAfter())) {
                try {
                    modification.accept(this.processEngine.getRuntimeService(), instance.getId());
                } catch (Exception e) {
                    int referenceId = log(e);
                    instanceReport.add(new LeafReport("Instance modification " + (before ? "before" : "after")
                            + " migration failed (referenceId=#" + referenceId + ")", false));
                    if (this.suspend && !instance.isSuspended()) {
                        this.processEngine.getRuntimeService().suspendProcessInstanceById(instance.getId());
                        instanceReport.add(new LeafReport("Suspension configured; process instance is now suspended", true));
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private int log(Exception e) {
        int referenceId = this.referenceGenerator.incrementAndGet();
        LOGGER.error("Error (referenceId=#" + referenceId + ") during process migration", e);
        return referenceId;
    }

    public static EngineBuilder in(ProcessEngine processEngine) {
        return new EngineBuilder(processEngine);
    }
}
