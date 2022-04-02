package com.mantledillusion.essentials.camunda.migration;

import org.camunda.bpm.engine.RuntimeService;

public abstract class ProcessMigration {

    public static abstract class AbstractFilteringBuilder<This> {

        private AbstractFilteringBuilder() {

        }

        public This withDefinitionKey(String definitionKey) {
            // TODO
            return (This) this;
        }

        public This withVersionTag(String versionTag) {
            // TODO
            return (This) this;
        }

        public This withActivity(String activityName) {
            // TODO
            return (This) this;
        }

        public This withVariable(String variableName) {
            // TODO
            return (This) this;
        }

        public This withVariable(String variableName, String value) {
            // TODO
            return (This) this;
        }
    }

    public static class ScenarioBuilder<Parent> extends AbstractFilteringBuilder<ScenarioBuilder<Parent>> {

        private final Parent parent;
        private ProcessMigration migration;

        private ScenarioBuilder(Parent parent) {
            this.parent = parent;
        }

        public PredicateBuilder<Parent> when(String name) {
            return new PredicateBuilder<>(this);
        }

        public ScenarioListBuilder<Parent> defineScenarios() {
            return set(new ScenarioListBuilder<>(parent));
        }

        public MigrationBuilder<Parent> defineMigrationTo(String definitionId) {
            // TODO
            return set(new MigrationBuilder<>(parent));
        }

        public MigrationBuilder<Parent> defineMigrationTo(String definitionKey, String definitionVersion) {
            // TODO
            return set(new MigrationBuilder<>(parent));
        }

        private <Builder extends ProcessMigration> Builder set(Builder migration) {
            this.migration = migration;
            return migration;
        }
    }

    public static class ScenarioListBuilder<Parent> extends ProcessMigration {

        private final Parent parent;

        private ScenarioListBuilder(Parent parent) {
            this.parent = parent;
        }

        public ScenarioBuilder<ScenarioListBuilder<Parent>> defineScenario(String name) {
            return new ScenarioBuilder<>(this);
        }

        public Parent done() {
            // TODO
            return this.parent;
        }
    }

    public static class PredicateBuilder<Parent> extends AbstractFilteringBuilder<PredicateBuilder<Parent>> {

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

    public static class AbstractManipulationBuilder<Parent, This> {

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

    public static class MigrationBuilder<Parent> extends ProcessMigration {

        private final Parent parent;

        private MigrationBuilder(Parent parent) {
            this.parent = parent;
        }

        public MigrationBuilder<Parent> withDefaultMappings() {
            // TODO
            return this;
        }

        public MigrationBuilder<Parent> withMapping(String sourceActivityId, String targetActivityId) {
            // TODO
            return this;
        }

        public Parent done() {
            return this.parent;
        }
    }

    public static class ExecutionBuilder {

        public void migrate() {
            // TODO
        }
    }

    private ProcessMigration() {

    }

    public static ScenarioBuilder<ExecutionBuilder> findProcesses(RuntimeService runtimeService) {
        return new ScenarioBuilder<>(new ExecutionBuilder());
    }
}
