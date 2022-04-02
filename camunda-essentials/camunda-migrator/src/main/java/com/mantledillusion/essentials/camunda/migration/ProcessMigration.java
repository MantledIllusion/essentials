package com.mantledillusion.essentials.camunda.migration;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.migration.MigratingProcessInstanceValidationException;
import org.camunda.bpm.engine.migration.MigrationPlan;
import org.camunda.bpm.engine.migration.MigrationPlanBuilder;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class ProcessMigration {

    public interface FilteringBuilder<This> {

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

            public ScenarioBuilder<SubScenarioBuilder<Parent>> defineScenario(String name) {
                return this.scenario.addMigrator(new ScenarioBuilder<>(this)); // TODO note name for report
            }

            public Parent done() {
                return this.scenario.parent;
            }
        }

        private final Parent parent;
        private final List<Consumer<ProcessMigration>> adaptors = new ArrayList<>();
        private final List<ScenarioBuilder<?>> migrators = new ArrayList<>();

        private ScenarioBuilder(Parent parent) {
            this.parent = parent;
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

        public PredicateBuilder<Parent> when(String name) {
            return new PredicateBuilder<>(this); // TODO note name for report
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

        public Parent done() {
            return this.parent;
        }

        private <A extends Consumer<ProcessMigration>> ScenarioBuilder<Parent> addAdaptor(A adaptor) {
            this.adaptors.add(adaptor);
            return this;
        }

        private <M extends ScenarioBuilder<?>> M addMigrator(M migrator) {
            this.migrators.add(migrator);
            return migrator;
        }

        private void migrate(ProcessMigration processMigration) {
            Stream.of(processMigration)
                    .map(ProcessMigration::copy)
                    .peek(filter -> this.adaptors.forEach(adaptor -> adaptor.accept(filter)))
                    .forEach(filter -> this.migrators.forEach(migrator -> migrator.migrate(filter)));
        }
    }

    public static class PredicateBuilder<Parent> implements FilteringBuilder<PredicateBuilder<Parent>> {

        private final ScenarioBuilder<Parent> parent;

        private PredicateBuilder(ScenarioBuilder<Parent> parent) {
            this.parent = parent;
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

        public ScenarioBuilder<Parent> done() {
            return this.parent;
        }
    }

    public static class BeforeMigrationManipulationBuilder<Parent> extends AbstractManipulationBuilder<Parent, BeforeMigrationManipulationBuilder<Parent>> {

        private BeforeMigrationManipulationBuilder(ScenarioBuilder<Parent> parent) {
            super(parent);
        }

        public AfterMigrationManipulationBuilder<Parent> afterMigrate() {
            return new AfterMigrationManipulationBuilder<>(this.done());
        }
    }

    public static class AfterMigrationManipulationBuilder<Parent> extends AbstractManipulationBuilder<Parent, AfterMigrationManipulationBuilder<Parent>> {

        private AfterMigrationManipulationBuilder(ScenarioBuilder<Parent> parent) {
            super(parent);
        }
    }

    public static class ExecutionBuilder {

        private final RepositoryService repositoryService;
        private final RuntimeService runtimeService;
        private ScenarioBuilder<ExecutionBuilder> rootScenario;

        public ExecutionBuilder(RepositoryService repositoryService, RuntimeService runtimeService) {
            this.repositoryService = repositoryService;
            this.runtimeService = runtimeService;
        }

        public void migrate() {
            this.rootScenario.migrate(new ProcessMigration(repositoryService, runtimeService));
        }
    }

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final List<Consumer<ProcessDefinitionQuery>> definitionFilters = new ArrayList<>(Collections.singletonList(
            ProcessDefinitionQuery::active
    ));
    private final List<Consumer<ProcessInstanceQuery>> preFilters = new ArrayList<>(Collections.singletonList(
            ProcessInstanceQuery::active
    ));
    private final List<BiPredicate<ProcessDefinition, ProcessInstance>> postFilters = new ArrayList<>();
    private final List<Consumer<MigrationPlanBuilder>> migrationPlanAdjustments = new ArrayList<>();

    private ProcessMigration(RepositoryService repositoryService, RuntimeService runtimeService) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
    }

    private void addDefinitionFilter(Consumer<ProcessDefinitionQuery> definitionFilter) {
        this.definitionFilters.add(definitionFilter);
    }

    private void addProcessPreFilter(Consumer<ProcessInstanceQuery> preFilter) {
        this.preFilters.add(preFilter);
    }

    private void addProcessPostFilter(BiPredicate<ProcessDefinition, ProcessInstance> postFilter) {
        this.postFilters.add(postFilter);
    }

    private void addMigrationPlanAdjustment(Consumer<MigrationPlanBuilder> adjustment) {
        this.migrationPlanAdjustments.add(adjustment);
    }

    private ProcessMigration copy() {
        return null; // TODO
    }

    private void migrate() {
        // FILTER TARGET DEFINITIONS IN THE ENGINE
        ProcessDefinitionQuery definitionQuery = this.repositoryService.createProcessDefinitionQuery();
        this.definitionFilters.forEach(filter -> filter.accept(definitionQuery));
        List<ProcessDefinition> targetDefinitions = definitionQuery.listPage(0, 2);

        ProcessDefinition targetDefinition;
        if (targetDefinitions.isEmpty()) {
            // REPORT
            return;
        } else if (targetDefinitions.size() > 1) {
            // REPORT
            return;
        } else {
            targetDefinition = targetDefinitions.iterator().next();
        }

        // PRE FILTER PROCESSES IN THE ENGINE
        ProcessInstanceQuery query = this.runtimeService.createProcessInstanceQuery();

        // RETRIEVE
        List<ProcessInstance> instances = query.list();

        // POST FILTER PROCESSES IN MEMORY

        // MIGRATE EVERY INSTANCE
        for (ProcessInstance instance: instances) {
            MigrationPlanBuilder builder = this.runtimeService
                    .createMigrationPlan(instance.getProcessDefinitionId(), targetDefinition.getId());

            this.migrationPlanAdjustments.forEach(adjustment -> adjustment.accept(builder));

            MigrationPlan plan = builder.build();

            try {
                this.runtimeService
                        .newMigration(plan)
                        .processInstanceIds(instance.getId())
                        .execute();
            } catch (MigratingProcessInstanceValidationException e) {
                // TODO REPORT
            }
        }
    }

    public static ScenarioBuilder<ExecutionBuilder> findProcesses(RepositoryService repositoryService, RuntimeService runtimeService) {
        ExecutionBuilder executionBuilder = new ExecutionBuilder(repositoryService, runtimeService);
        ScenarioBuilder<ExecutionBuilder> scenarioBuilder = new ScenarioBuilder<>(executionBuilder);

        executionBuilder.rootScenario = scenarioBuilder;

        return scenarioBuilder;
    }
}
