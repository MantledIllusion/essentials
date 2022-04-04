package com.mantledillusion.essentials.camunda.migration;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.migration.MigratingProcessInstanceValidationException;
import org.camunda.bpm.engine.migration.MigrationPlan;
import org.camunda.bpm.engine.migration.MigrationPlanBuilder;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.camunda.commons.utils.StringUtil;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class ProcessMigration {

    public interface FilteringBuilder<This> {

        This withDefinitionId(String definitionId);

        This withDefinitionKey(String definitionKey);

        This withVersionTag(String versionTag);

        This withVersion(int version);

        This withActivity(String activityName);

        This withVariableEquals(String variableName, Object value);
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

            public ScenarioBuilder<SubScenarioBuilder<Parent>> defineScenario(String name) {
                ScenarioBuilder<SubScenarioBuilder<Parent>> scenarioBuilder = new ScenarioBuilder<>(this, name);
                this.scenario.addMigrator(scenarioBuilder::migrate);
                return scenarioBuilder;
            }

            public Parent finalizeScenarios() {
                return this.scenario.parent;
            }
        }

        private final Parent parent;
        private final String name;
        private final List<Consumer<ProcessMigration>> adaptors = new ArrayList<>();
        private final List<Function<ProcessMigration, Report>> migrators = new ArrayList<>();

        private ScenarioBuilder(Parent parent, String name) {
            this.parent = parent;
            this.name = name;
        }

        @Override
        public ScenarioBuilder<Parent> withDefinitionId(String definitionId) {
            return addAdaptor(migration -> migration.addProcessPreFilter(query -> query.processDefinitionId(definitionId)));
        }

        @Override
        public ScenarioBuilder<Parent> withDefinitionKey(String definitionKey) {
            return addAdaptor(migration -> migration.addProcessPreFilter(query -> query.processDefinitionKey(definitionKey)));
        }

        @Override
        public ScenarioBuilder<Parent> withVersionTag(String versionTag) {
            return addAdaptor(migration -> migration.addProcessPostFilter((definition, instance) -> definition.getVersionTag().equals(versionTag)));
        }

        @Override
        public ScenarioBuilder<Parent> withVersion(int version) {
            return addAdaptor(migration -> migration.addProcessPostFilter((definition, instance) -> definition.getVersion() == version));
        }

        @Override
        public ScenarioBuilder<Parent> withActivity(String activityId) {
            return addAdaptor(migration -> migration.addProcessPreFilter(query -> query.activityIdIn(activityId)));
        }

        @Override
        public ScenarioBuilder<Parent> withVariableEquals(String variableName, Object value) {
            return addAdaptor(migration -> migration.addProcessPreFilter(query -> query.variableValueEquals(variableName, value)));
        }

        public PredicateBuilder<Parent> when(String name) {// TODO note name for report
            return new PredicateBuilder<>(this);
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

        private <M extends Function<ProcessMigration, Report>> void addMigrator(M migrator) {
            this.migrators.add(migrator);
        }

        private BranchReport migrate(ProcessMigration processMigration) {
            BranchReport scenarioReport = new BranchReport("Scenario: " + this.name);
            Stream.of(processMigration)
                    .map(ProcessMigration::copy)
                    .peek(migration -> this.adaptors.forEach(adaptor -> adaptor.accept(migration)))
                    .forEach(migration -> this.migrators.stream()
                            .map(migrator -> migrator.apply(migration))
                            .forEach(scenarioReport.children::add));
            return scenarioReport;
        }
    }

    public static class PredicateBuilder<Parent> implements FilteringBuilder<PredicateBuilder<Parent>> {

        private final ScenarioBuilder<Parent> parent;

        private PredicateBuilder(ScenarioBuilder<Parent> parent) {
            this.parent = parent;
        }

        @Override
        public PredicateBuilder<Parent> withDefinitionId(String definitionId) {
            return null; // TODO
        }

        @Override
        public PredicateBuilder<Parent> withDefinitionKey(String definitionKey) {
            return this; // TODO
        }

        @Override
        public PredicateBuilder<Parent> withVersionTag(String versionTag) {
            return this; // TODO
        }

        @Override
        public PredicateBuilder<Parent> withVersion(int version) {
            return this; // TODO
        }

        @Override
        public PredicateBuilder<Parent> withActivity(String activityName) {
            return this; // TODO
        }

        @Override
        public PredicateBuilder<Parent> withVariableEquals(String variableName, Object value) {
            return this; // TODO
        }

        public BeforeMigrationManipulationBuilder<Parent> beforeMigrate() {
            return new BeforeMigrationManipulationBuilder<>(this.parent);
        }

        public AfterMigrationManipulationBuilder<Parent> afterMigrate() {
            return new AfterMigrationManipulationBuilder<>(this.parent);
        }
    }

    @SuppressWarnings("unchecked")
    public static abstract class AbstractManipulationBuilder<Parent, This> {

        private final ScenarioBuilder<Parent> parent;

        private AbstractManipulationBuilder(ScenarioBuilder<Parent> parent) {
            this.parent = parent;
        }

        public This cancelActivity(String activityName) {
            // TODO
            return (This) this;
        }

        public This triggerMessage(String messageName) {
            // TODO
            return (This) this;
        }

        public This setVariable(String variableName, String value) {
            // TODO
            return (This) this;
        }

        public ScenarioBuilder<Parent> then() {
            return this.parent;
        }
    }

    public static class BeforeMigrationManipulationBuilder<Parent> extends AbstractManipulationBuilder<Parent, BeforeMigrationManipulationBuilder<Parent>> {

        private BeforeMigrationManipulationBuilder(ScenarioBuilder<Parent> parent) {
            super(parent);
        }

        public AfterMigrationManipulationBuilder<Parent> afterMigrate() {
            return new AfterMigrationManipulationBuilder<>(this.then());
        }
    }

    public static class AfterMigrationManipulationBuilder<Parent> extends AbstractManipulationBuilder<Parent, AfterMigrationManipulationBuilder<Parent>> {

        private AfterMigrationManipulationBuilder(ScenarioBuilder<Parent> parent) {
            super(parent);
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

        public ScenarioBuilder<ExecutionBuilder> defineScenario(String name) {
            ExecutionBuilder executionBuilder = new ExecutionBuilder(this.processEngine);
            ScenarioBuilder<ExecutionBuilder> scenarioBuilder = new ScenarioBuilder<>(executionBuilder, name);

            executionBuilder.rootScenario = scenarioBuilder;

            return scenarioBuilder;
        }
    }

    public static class ExecutionBuilder {

        private final ProcessEngine processEngine;
        private ScenarioBuilder<ExecutionBuilder> rootScenario;

        private ExecutionBuilder(ProcessEngine processEngine) {
            this.processEngine = processEngine;
        }

        public Report migrate() {
            return this.rootScenario.migrate(new ProcessMigration(this.processEngine,
                    // DEFAULT DEFINITION FILTERS
                    new ArrayList<>(Collections.singletonList(
                            ProcessDefinitionQuery::active
                    )),
                    // DEFAULT INSTANCE PRE FILTERS
                    new ArrayList<>(Collections.singletonList(
                            ProcessInstanceQuery::active
                    )),
                    // DEFAULT INSTANCE POST FILTERS
                    new ArrayList<>(),
                    // DEFAULT MIGRATION PLAN ADJUSTMENTS
                    new ArrayList<>()
            ));
        }
    }

    public static abstract class Report {

        public enum Result {
            SUCCESS(true, "(ok) "),
            IGNORE(true, "(ign)"),
            FAILURE(false, "(nok)");

            private final boolean success;
            private final String print;

            Result(boolean success, String print) {
                this.success = success;
                this.print = print;
            }
        }

        private final String name;

        private Report(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public abstract Result getResult();

        public boolean isSuccess() {
            return getResult().success;
        }

        public abstract List<Report> getChildren();

        @Override
        public final String toString() {
            return toString(0);
        }

        protected String getPadding(int layer) {
            return IntStream.range(0, layer).mapToObj(i -> "  ").reduce(String::concat).orElse("");
        }

        protected abstract String toString(int layer);
    }

    public static final class BranchReport extends Report {

        private final List<Report> children = new ArrayList<>();

        private BranchReport(String name) {
            super(name);
        }

        private <C extends Report> C add(C child) {
            this.children.add(child);
            return child;
        }

        @Override
        public Result getResult() {
            return this.children.stream()
                    .map(Report::getResult)
                    .reduce((result, result2) -> result.ordinal() > result2.ordinal() ? result : result2)
                    .orElse(Result.IGNORE);
        }

        @Override
        public List<Report> getChildren() {
            return this.children;
        }

        @Override
        protected String toString(int layer) {
            String[] report = Stream
                    .concat(
                            Stream.of(getPadding(layer) + getName()),
                            this.children.stream().map(child -> child.toString(layer+1))
                    )
                    .toArray(String[]::new);

            return StringUtil.join("\n", report);
        }
    }

    public static final class LeafReport extends Report {

        private final Result result;

        public LeafReport(String name, Result result) {
            super(name);
            this.result = result;
        }

        @Override
        public Result getResult() {
            return this.result;
        }

        @Override
        public List<Report> getChildren() {
            return Collections.emptyList();
        }

        @Override
        protected String toString(int layer) {
            return getPadding(layer) + result.print + ' ' + getName();
        }
    }

    private final ProcessEngine processEngine;
    private final List<Consumer<ProcessDefinitionQuery>> definitionFilters;
    private final List<Consumer<ProcessInstanceQuery>> instancePreFilters;
    private final List<BiPredicate<ProcessDefinition, ProcessInstance>> instancePostFilters;
    private final List<Consumer<MigrationPlanBuilder>> migrationPlanAdjustments;

    private ProcessMigration(ProcessEngine processEngine,
                             List<Consumer<ProcessDefinitionQuery>> definitionFilters,
                             List<Consumer<ProcessInstanceQuery>> instancePreFilters,
                             List<BiPredicate<ProcessDefinition, ProcessInstance>> instancePostFilters,
                             List<Consumer<MigrationPlanBuilder>> migrationPlanAdjustments) {
        this.processEngine = processEngine;
        this.definitionFilters = definitionFilters;
        this.instancePreFilters = instancePreFilters;
        this.instancePostFilters = instancePostFilters;
        this.migrationPlanAdjustments = migrationPlanAdjustments;
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

    private void addMigrationPlanAdjustment(Consumer<MigrationPlanBuilder> adjustment) {
        this.migrationPlanAdjustments.add(adjustment);
    }

    private ProcessMigration copy() {
        return new ProcessMigration(this.processEngine,
                new ArrayList<>(this.definitionFilters),
                new ArrayList<>(this.instancePreFilters),
                new ArrayList<>(this.instancePostFilters),
                new ArrayList<>(this.migrationPlanAdjustments));
    }

    private Report migrate() {
        // FILTER TARGET DEFINITIONS IN THE ENGINE
        ProcessDefinitionQuery definitionQuery = this.processEngine.getRepositoryService()
                .createProcessDefinitionQuery();
        this.definitionFilters.forEach(filter -> filter.accept(definitionQuery));

        BranchReport migrationReport = new BranchReport("Migration: Definition X"); // TODO REPORT
        if (definitionQuery.count() != 1) {
            migrationReport.add(new LeafReport("Found " + definitionQuery.count() + " definitions matching definition filters", Report.Result.FAILURE));
        } else {
            ProcessDefinition targetDefinition = definitionQuery.list().iterator().next();
            migrationReport.add(new LeafReport("Found definition " + targetDefinition.getId() + " matching definition filters", Report.Result.SUCCESS));

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
                migrationReport.add(new LeafReport("Found no instances matching instance filters", Report.Result.IGNORE));
            } else {
                migrationReport.add(new LeafReport("Found " + instances.size() + " instance(s) matching instance filters", Report.Result.SUCCESS));

                // MIGRATE EVERY INSTANCE
                for (ProcessInstance instance: instances) {
                    BranchReport instanceReport = migrationReport.add(new BranchReport("Instance: " + instance.getId()));

                    // CREATE MIGRATION PLAN
                    MigrationPlanBuilder builder = this.processEngine.getRuntimeService()
                            .createMigrationPlan(instance.getProcessDefinitionId(), targetDefinition.getId());

                    // ADJUST MIGRATION PLAN
                    this.migrationPlanAdjustments.forEach(adjustment -> adjustment.accept(builder));

                    // BUILD MIGRATION PLAN
                    MigrationPlan plan = builder.build();

                    // EXECUTE MIGRATION
                    try {
                        long ms = System.currentTimeMillis();
                        this.processEngine.getRuntimeService()
                                .newMigration(plan)
                                .processInstanceIds(instance.getId())
                                .execute();
                        ms = System.currentTimeMillis() - ms;

                        instanceReport.add(new LeafReport("Instance migration successful in " + ms + "ms", Report.Result.SUCCESS));
                    } catch (MigratingProcessInstanceValidationException e) {
                        instanceReport.add(new LeafReport("Instance migration failed: " + e.getMessage(), Report.Result.FAILURE));
                    }
                }
            }
        }

        return migrationReport;
    }

    public static EngineBuilder in(ProcessEngine processEngine) {
        return new EngineBuilder(processEngine);
    }
}
