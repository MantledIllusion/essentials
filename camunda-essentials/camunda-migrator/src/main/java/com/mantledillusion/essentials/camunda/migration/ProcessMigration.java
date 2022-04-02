package com.mantledillusion.essentials.camunda.migration;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class ProcessMigration {

    private static abstract class AbstractMigrator {

        protected abstract void migrate(ProcessMigration migration);
    }

    public interface FilteringBuilder<This> {

        This withDefinitionKey(String definitionKey);

        This withVersionTag(String versionTag);

        This withVersion(int version);

        This withActivity(String activityName);

        This withVariableEquals(String variableName, Object value);
    }

    public static class ScenarioBuilder<Parent> extends AbstractMigrator implements FilteringBuilder<ScenarioBuilder<Parent>> {

        public static class SubScenarioBuilder<Parent> {

            private final ScenarioBuilder<Parent> scenario;

            private SubScenarioBuilder(ScenarioBuilder<Parent> scenario) {
                this.scenario = scenario;
            }

            public ScenarioBuilder<SubScenarioBuilder<Parent>> defineScenario(String name) {
                return this.scenario.addMigrator(new ScenarioBuilder<>(this));
            }

            public Parent done() {
                // TODO
                return this.scenario.parent;
            }
        }

        private final Parent parent;
        private final List<Consumer<ProcessMigration>> migrationAdaptors = new ArrayList<>();
        private final List<AbstractMigrator> migrators = new ArrayList<>();

        private ScenarioBuilder(Parent parent) {
            this.parent = parent;
        }

        @Override
        public ScenarioBuilder<Parent> withDefinitionKey(String definitionKey) {
            return addAdaptor(migration -> migration.addPreFilter(query -> query.processDefinitionKey(definitionKey)));
        }

        @Override
        public ScenarioBuilder<Parent> withVersionTag(String versionTag) {
            return addAdaptor(migration -> migration.addPostFilter((definition, instance) -> definition.getVersionTag().equals(versionTag)));
        }

        @Override
        public ScenarioBuilder<Parent> withVersion(int version) {
            return addAdaptor(migration -> migration.addPostFilter((definition, instance) -> definition.getVersion() == version));
        }

        @Override
        public ScenarioBuilder<Parent> withActivity(String activityId) {
            return addAdaptor(migration -> migration.addPreFilter(query -> query.activityIdIn(activityId)));
        }

        @Override
        public ScenarioBuilder<Parent> withVariableEquals(String variableName, Object value) {
            return addAdaptor(migration -> migration.addPreFilter(query -> query.variableValueEquals(variableName, value)));
        }

        private <A extends Consumer<ProcessMigration>> ScenarioBuilder<Parent> addAdaptor(A adaptor) {
            this.migrationAdaptors.add(adaptor);
            return this;
        }

        public PredicateBuilder<Parent> when(String name) {
            return new PredicateBuilder<>(this); // TODO note name for report
        }

        public SubScenarioBuilder<Parent> defineScenarios() {
            return new SubScenarioBuilder<>(this);
        }

        public MigrationBuilder<Parent> defineMigrationToId(String definitionId) {
            return addMigrator(new MigrationBuilder<>(parent,
                    query -> query.processDefinitionId(definitionId)));
        }

        public MigrationBuilder<Parent> defineMigrationToLatestKey(String definitionKey) {
            return addMigrator(new MigrationBuilder<>(parent,
                    query -> query.processDefinitionKey(definitionKey),
                    query -> query.latestVersion()));
        }

        public MigrationBuilder<Parent> defineMigrationToTaggedKey(String definitionKey, String versionTag) {
            return addMigrator(new MigrationBuilder<>(parent,
                    query -> query.processDefinitionKey(definitionKey),
                    query -> query.versionTag(versionTag),
                    query -> query.latestVersion()));
        }

        public MigrationBuilder<Parent> defineMigrationToSpecificKey(String definitionKey, int definitionVersion) {
            return addMigrator(new MigrationBuilder<>(parent,
                    query -> query.processDefinitionKey(definitionKey),
                    query -> query.processDefinitionVersion(definitionVersion)));
        }

        private <M extends AbstractMigrator> M addMigrator(M migrator) {
            this.migrators.add(migrator);
            return migrator;
        }

        @Override
        protected void migrate(ProcessMigration processMigration) {
            Stream.of(processMigration)
                    .map(ProcessMigration::copy)
                    .peek(filter -> this.migrationAdaptors.forEach(adaptor -> adaptor.accept(filter)))
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

    public static class MigrationBuilder<Parent> extends AbstractMigrator {

        private final Parent parent;
        private final List<Consumer<ProcessDefinitionQuery>> definitionFilters;

        @SafeVarargs
        private MigrationBuilder(Parent parent, Consumer<ProcessDefinitionQuery>... definitionFilters) {
            this.parent = parent;
            this.definitionFilters = Arrays.asList(definitionFilters);
        }

        public MigrationBuilder<Parent> withDefaultMappings() {
            // TODO auto-generate default mappings using camunda
            return this;
        }

        public MigrationBuilder<Parent> withMapping(String sourceActivityId, String targetActivityId) {
            // TODO add specific mapping
            return this;
        }

        public Parent done() {
            return this.parent;
        }

        @Override
        protected void migrate(ProcessMigration processMigration) {
            this.definitionFilters.forEach(processMigration::addDefinitionFilter);
            processMigration.migrate(); // TODO mappings
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

    private ProcessMigration(RepositoryService repositoryService, RuntimeService runtimeService) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
    }

    private void addDefinitionFilter(Consumer<ProcessDefinitionQuery> definitionFilter) {
        this.definitionFilters.add(definitionFilter);
    }

    private void addPreFilter(Consumer<ProcessInstanceQuery> preFilter) {
        this.preFilters.add(preFilter);
    }

    private void addPostFilter(BiPredicate<ProcessDefinition, ProcessInstance> postFilter) {
        this.postFilters.add(postFilter);
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
        query.list();

        // POST FILTER PROCESSES IN MEMORY

    }

    public static ScenarioBuilder<ExecutionBuilder> findProcesses(RepositoryService repositoryService, RuntimeService runtimeService) {
        ExecutionBuilder executionBuilder = new ExecutionBuilder(repositoryService, runtimeService);
        ScenarioBuilder<ExecutionBuilder> scenarioBuilder = new ScenarioBuilder<>(executionBuilder);

        executionBuilder.rootScenario = scenarioBuilder;

        return scenarioBuilder;
    }
}
