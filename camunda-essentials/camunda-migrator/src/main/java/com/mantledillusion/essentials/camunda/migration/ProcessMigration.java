package com.mantledillusion.essentials.camunda.migration;

import org.camunda.bpm.engine.RuntimeService;

public class ProcessMigration {

    public static abstract class AbstractFilteringBuilder<This> {

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

        public ScenarioListBuilder<Parent> migrateScenarios() {
            return set(new ScenarioListBuilder<>(this));
        }

        public MigrationBuilder<Parent> migrateProcess(String definitionId) {
            // TODO
            return set(new MigrationBuilder<>(parent));
        }

        public MigrationBuilder<Parent> migrateProcess(String definitionKey, String definitionVersion) {
            // TODO
            return set(new MigrationBuilder<>(parent));
        }

        private <Builder extends ProcessMigration> Builder set(Builder migration) {
            this.migration = migration;
            return migration;
        }
    }

    public static class ScenarioListBuilder<Parent> extends ProcessMigration {

        private final ScenarioBuilder<Parent> parent;

        private ScenarioListBuilder(ScenarioBuilder<Parent> parent) {
            this.parent = parent;
        }

        public ScenarioBuilder<ScenarioListBuilder<Parent>> scenario(String name) {
            return new ScenarioBuilder<>(this);
        }

        public ScenarioBuilder<Parent> migrate() {
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

        public AbstractManipulationBuilder(ScenarioBuilder<Parent> parent) {
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

        public This setVariable(String variableName) {
            // TODO
            return (This) this;
        }

        public ScenarioBuilder<Parent> done() {
            return this.parent;
        }
    }

    public static class BeforeMigrationManipulationBuilder<Parent> extends AbstractManipulationBuilder<Parent, BeforeMigrationManipulationBuilder<Parent>> {

        public BeforeMigrationManipulationBuilder(ScenarioBuilder<Parent> parent) {
            super(parent);
        }

        public AfterMigrationManipulationBuilder<Parent> afterMigrate() {
            return new AfterMigrationManipulationBuilder<>(this.done());
        }
    }

    public static class AfterMigrationManipulationBuilder<Parent> extends AbstractManipulationBuilder<Parent, AfterMigrationManipulationBuilder<Parent>> {

        public AfterMigrationManipulationBuilder(ScenarioBuilder<Parent> parent) {
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

        public Parent migrate() {
            return this.parent;
        }
    }

    public static ScenarioBuilder<Void> findProcesses(RuntimeService runtimeService) {
        return new ScenarioBuilder<>(null);
    }
}
