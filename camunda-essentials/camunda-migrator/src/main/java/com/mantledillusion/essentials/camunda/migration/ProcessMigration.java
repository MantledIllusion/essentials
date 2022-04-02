package com.mantledillusion.essentials.camunda.migration;

import org.camunda.bpm.engine.RuntimeService;

import java.util.ArrayList;
import java.util.List;

public abstract class ProcessMigration {

    private static abstract class AbstractMigrator {

        protected abstract void migrate(RuntimeService runtimeService);
    }

    @SuppressWarnings("unchecked")
    public interface FilteringBuilder<This> {

        default This withDefinitionKey(String definitionKey) {
            // TODO
            return (This) this;
        }

        default This withVersionTag(String versionTag) {
            // TODO
            return (This) this;
        }

        default This withActivity(String activityName) {
            // TODO
            return (This) this;
        }

        default This withVariable(String variableName) {
            // TODO
            return (This) this;
        }

        default This withVariable(String variableName, String value) {
            // TODO
            return (This) this;
        }
    }

    public static class ScenarioBuilder<Parent> extends AbstractMigrator implements FilteringBuilder<ScenarioBuilder<Parent>> {

        public static class SubScenarioBuilder<Parent> {

            private final ScenarioBuilder<Parent> scenario;

            private SubScenarioBuilder(ScenarioBuilder<Parent> scenario) {
                this.scenario = scenario;
            }

            public ScenarioBuilder<SubScenarioBuilder<Parent>> defineScenario(String name) {
                return this.scenario.add(new ScenarioBuilder<>(this));
            }

            public Parent done() {
                // TODO
                return this.scenario.parent;
            }
        }

        private final Parent parent;
        private final List<AbstractMigrator> migrators = new ArrayList<>();

        private ScenarioBuilder(Parent parent) {
            this.parent = parent;
        }

        public PredicateBuilder<Parent> when(String name) {
            return new PredicateBuilder<>(this);
        }

        public SubScenarioBuilder<Parent> defineScenarios() {
            return new SubScenarioBuilder<>(this);
        }

        public MigrationBuilder<Parent> defineMigrationTo(String definitionId) {
            // TODO overtake given definitionId
            return add(new MigrationBuilder<>(parent));
        }

        public MigrationBuilder<Parent> defineMigrationTo(String definitionKey, String definitionVersion) {
            // TODO determine definitionId using key/version
            return add(new MigrationBuilder<>(parent));
        }

        public MigrationBuilder<Parent> defineMigrationTo(String definitionKey, MigrationBuilder.ProcessVersion version) {
            // TODO determine definitionId using key/version
            return add(new MigrationBuilder<>(parent));
        }

        private <M extends AbstractMigrator> M add(M migrator) {
            this.migrators.add(migrator);
            return migrator;
        }

        @Override
        protected void migrate(RuntimeService runtimeService) {
            this.migrators.forEach(migrator -> migrator.migrate(runtimeService));
        }
    }

    public static class PredicateBuilder<Parent> implements FilteringBuilder<PredicateBuilder<Parent>> {

        private final ScenarioBuilder<Parent> parent;

        private PredicateBuilder(ScenarioBuilder<Parent> parent) {
            this.parent = parent;
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

        public enum ProcessVersion {
            EARLIEST,
            LATEST
        }

        private final Parent parent;

        private MigrationBuilder(Parent parent) {
            this.parent = parent;
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
        protected void migrate(RuntimeService runtimeService) {
            // TODO actually migrate the process in camunda using the gathered mappings
        }
    }

    public static class ExecutionBuilder {

        private final RuntimeService runtimeService;
        private ScenarioBuilder<ExecutionBuilder> rootScenario;

        public ExecutionBuilder(RuntimeService runtimeService) {
            this.runtimeService = runtimeService;
        }

        public void migrate() {
            this.rootScenario.migrate(this.runtimeService);
        }
    }

    private ProcessMigration() {

    }

    public static ScenarioBuilder<ExecutionBuilder> findProcesses(RuntimeService runtimeService) {
        ExecutionBuilder executionBuilder = new ExecutionBuilder(runtimeService);
        ScenarioBuilder<ExecutionBuilder> scenarioBuilder = new ScenarioBuilder<>(executionBuilder);

        executionBuilder.rootScenario = scenarioBuilder;

        return scenarioBuilder;
    }
}
