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

/**
 * Fluent builder style migrator for Camunda processes.
 * <p>
 * New builders are created using {@link ProcessMigration#in(ProcessEngine)}, which leads to beginning a root scenario.
 * A scenario is a differentiated set of situations in which process instances are migrated.
 * <p>
 * A scenario always begins as follows:<br>
 * <code>defineScenario()</code> - (mandatory) - begin defining a new scenario
 * <p>
 * During a scenario, any of the following methods can be used to define its boundaries:<br>
 * <code>where*()</code> - define filters for the source process instances to migrate<br>
 * <code>to*()</code> - define filters to determine the single target process definition to migrate to<br>
 * <code>using*()</code> - define activity mappings for the migration plan<br>
 * <code>when()</code> - define adjustments to process instances before/after commencing the migration<br>
 * <code>on*()</code> - (optional)
 * <p>
 * To complete a scenario's definition, either of the following methods might be chosen:<br>
 * <code>finalizeScenario()</code> - migrate processes as defined so far<br>
 * <code>defineScenarios()</code> - instead of migrating processes in this scenario, split it into sub scenarios
 * which will define sub-cases and then migrate processes themselves
 */
public final class ProcessMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessMigration.class);

    /**
     * A builder for specifying filters for process instances.
     *
     * @param <This> The class type of the implementing builder
     */
    public interface FilteringBuilder<This> {

        /**
         * Filter for process instances that are active.
         *
         * @see ProcessInstanceQuery#active()
         * @return this
         */
        This whereActive();

        /**
         * Filter for process instances that are suspended.
         *
         * @see ProcessInstanceQuery#suspended()
         * @return this
         */
        This whereSuspended();

        /**
         * Filter for process instances whose definition has the given id.
         *
         * @see ProcessInstanceQuery#processDefinitionId(String)
         * @param definitionId The definitionId to filter for; might <b>not</b> be null
         * @return this
         */
        This whereDefinitionId(String definitionId);

        /**
         * Filter for process instances whose definition has the given key.
         *
         * @see ProcessInstanceQuery#processDefinitionKey(String)
         * @param definitionKey The definitionKey to filter for; might <b>not</b> be null
         * @return this
         */
        This whereDefinitionKey(String definitionKey);

        /**
         * Filter for process instances whose definition has the given version tag.
         *
         * @see ProcessDefinition#getVersionTag()
         * @param versionTag The versionTag to filter for; might be null.
         * @return this
         */
        This whereVersionTag(String versionTag);

        /**
         * Filter for process instances whose definition has the given version.
         *
         * @see ProcessDefinition#getVersion()
         * @param version The version to filter for; might <b>not</b> be null.
         * @return this
         */
        This whereVersion(int version);

        /**
         * Filter for process instances currently having an active activity with the given name.
         *
         * @see ProcessInstanceQuery#activityIdIn(String...)
         * @param activityName The name of the activity to filter for; might <b>not</b> be null.
         * @return this
         */
        This whereActivity(String activityName);

        /**
         * Filter for process instances currently having an open incident.
         *
         * @see ProcessInstanceQuery#withIncident()
         * @return this
         */
        This whereIncident();

        /**
         * Filter for process instances having a variable with the given name and value.
         *
         * @see ProcessInstanceQuery#variableValueEquals(String, Object) 
         * @param variableName The variable's name; might <b>not</b> be null
         * @param value The variable's value; might be null.
         * @return this
         */
        This whereVariableEquals(String variableName, Object value);
    }

    /**
     * A builder for defining a scenario.
     *
     * @param <Parent> The type of this {@link ScenarioBuilder}'s parent
     */
    public static class ScenarioBuilder<Parent> implements FilteringBuilder<ScenarioBuilder<Parent>> {

        /**
         * A builder for defining multiple sub-scenarios to a scenario.
         *
         * @param <Parent> The type of the parent of the {@link ScenarioBuilder} opening this {@link SubScenarioBuilder}
         */
        public static class SubScenarioBuilder<Parent> {

            private final ScenarioBuilder<Parent> scenario;

            private SubScenarioBuilder(ScenarioBuilder<Parent> scenario) {
                this.scenario = scenario;
            }

            /**
             * Begins defining a new sub-scenario.
             *
             * @return A new {@link ScenarioBuilder}, never null
             */
            public ScenarioBuilder<SubScenarioBuilder<Parent>> defineScenario() {
                return defineScenario("unnamed");
            }

            /**
             * Begins defining a new sub-scenario.
             *
             * @param title The title of the sub-scenario, might <b>not</b> be null.
             * @return A new {@link ScenarioBuilder}, never null
             */
            public ScenarioBuilder<SubScenarioBuilder<Parent>> defineScenario(String title) {
                if (title == null) {
                    throw new IllegalArgumentException("Cannot create a scenario without a title");
                }
                ScenarioBuilder<SubScenarioBuilder<Parent>> scenarioBuilder = new ScenarioBuilder<>(this, title);
                this.scenario.addMigrator((migration, report) -> report.add(scenarioBuilder.migrate(migration)));
                return scenarioBuilder;
            }

            /**
             * Completes defining sub-scenarios.
             *
             * @return The parent builder of the {@link ScenarioBuilder} to finish defining sub-scenario's for, never null
             */
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
            if (definitionId == null) {
                throw new IllegalArgumentException("Unable to filter for a null definition id");
            }
            return addAdaptor(migration -> migration.addProcessPreFilter(query -> query.processDefinitionId(definitionId)));
        }

        @Override
        public ScenarioBuilder<Parent> whereDefinitionKey(String definitionKey) {
            if (definitionKey == null) {
                throw new IllegalArgumentException("Unable to filter for a null definition key");
            }
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
            if (activityId == null) {
                throw new IllegalArgumentException("Unable to filter for a null activity id");
            }
            return addAdaptor(migration -> migration.addProcessPreFilter(query -> query.activityIdIn(activityId)));
        }

        @Override
        public ScenarioBuilder<Parent> whereIncident() {
            return addAdaptor(migration -> migration.addProcessPreFilter(ProcessInstanceQuery::withIncident));
        }

        @Override
        public ScenarioBuilder<Parent> whereVariableEquals(String variableName, Object value) {
            if (variableName == null) {
                throw new IllegalArgumentException("Unable to filter for a null variable name");
            }
            return addAdaptor(migration -> migration.addProcessPreFilter(query -> query.variableValueEquals(variableName, value)));
        }

        /**
         * Migrate the process instances to the definition with the given id.
         *
         * @see ProcessDefinitionQuery#processDefinitionId(String)
         * @param definitionId The id to migrate to; might <b>not</b> be null.
         * @return this
         */
        public ScenarioBuilder<Parent> toDefinitionId(String definitionId) {
            if (definitionId == null) {
                throw new IllegalArgumentException("Unable to filter for a null definition id");
            }
            return addAdaptor(migration -> migration.addDefinitionFilter(query -> query.processDefinitionId(definitionId)));
        }

        /**
         * Migrate the process instances to a definition with the given key.
         *
         * @see ProcessDefinitionQuery#processDefinitionKey(String)
         * @param definitionKey The key to migrate to; might <b>not</b> be null.
         * @return this
         */
        public ScenarioBuilder<Parent> toDefinitionKey(String definitionKey) {
            if (definitionKey == null) {
                throw new IllegalArgumentException("Unable to filter for a null definition key");
            }
            return addAdaptor(migration -> migration.addDefinitionFilter(query -> query.processDefinitionKey(definitionKey)));
        }

        /**
         * Migrate the process instances to a definition with the given tag.
         *
         * @see ProcessDefinitionQuery#processDefinitionKey(String)
         * @param definitionTag The tag to migrate to; might be null.
         * @return this
         */
        public ScenarioBuilder<Parent> toDefinitionTag(String definitionTag) {
            return addAdaptor(migration -> migration.addDefinitionFilter(query -> query.versionTag(definitionTag)));
        }

        /**
         * Migrate the process instances to a definition with a specific version.
         *
         * @see ProcessDefinitionQuery#processDefinitionVersion(Integer)
         * @param definitionVersion The key to migrate to.
         * @return this
         */
        public ScenarioBuilder<Parent> toSpecificDefinitionVersion(int definitionVersion) {
            return addAdaptor(migration -> migration.addDefinitionFilter(query -> query.processDefinitionVersion(definitionVersion)));
        }

        /**
         * Migrate the process instances to the definition with the latest version.
         *
         * @see ProcessDefinitionQuery#latestVersion
         * @return this
         */
        public ScenarioBuilder<Parent> toLatestDefinitionVersion() {
            return addAdaptor(migration -> migration.addDefinitionFilter(ProcessDefinitionQuery::latestVersion));
        }

        /**
         * Use the default activity mappings between the source and target process definitions.
         *
         * @see MigrationPlanBuilder##mapEqualActivities
         * @return this
         */
        public ScenarioBuilder<Parent> usingDefaultMappings() {
            return addAdaptor(migration -> migration.addMigrationPlanAdjustment(MigrationPlanBuilder::mapEqualActivities));
        }

        /**
         * Use the given specific activity mapping.
         *
         * @see MigrationPlanBuilder#mapActivities(String, String)
         * @param sourceActivityId The activityId in the process definition of the source process instance; might <b>not</b> be null.
         * @param targetActivityId The activityId in the target process definition; might <b>not</b> be null.
         * @return this
         */
        public ScenarioBuilder<Parent> usingMapping(String sourceActivityId, String targetActivityId) {
            if (sourceActivityId == null) {
                throw new IllegalArgumentException("Cannot map from a null source activity");
            } else if (targetActivityId == null) {
                throw new IllegalArgumentException("Cannot map to a null target activity");
            }
            return addAdaptor(migration -> migration.addMigrationPlanAdjustment(plan -> plan.mapActivities(sourceActivityId, targetActivityId)));
        }

        /**
         * Begins defining an adjustment to a process instance to migrate.
         *
         * @return A new {@link AdjustmentBuilder}, never null
         */
        public AdjustmentBuilder<Parent> when() {
            return when("unnamed");
        }

        /**
         * Begins defining an adjustment to a process instance to migrate.
         *
         * @param title The adjustment's title; might <b>not</b> be null.
         * @return A new {@link AdjustmentBuilder}, never null
         */
        public AdjustmentBuilder<Parent> when(String title) {
            if (title == null) {
                throw new IllegalArgumentException("Cannot create an adjustment with a null title");
            }
            ProcessInstanceAdjustment adjustment = new ProcessInstanceAdjustment(title);
            this.adaptors.add(migration -> migration.addInstanceAdjustment(adjustment));
            return new AdjustmentBuilder<>(this, adjustment);
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

        /**
         * Declare this scenario as not migrating process instances itself, but rather be a parent for more specific
         * sub-scenarios.
         *
         * @return A new {@link SubScenarioBuilder}, never null
         */
        public SubScenarioBuilder<Parent> defineScenarios() {
            return new SubScenarioBuilder<>(this);
        }

        /**
         * Declare this scenario as being fully defined for migrating process instances.
         *
         * @return This {@link ScenarioBuilder}'s parent, never null
         */
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

    /**
     * A builder for defining filters determining whether to execute adjustments to a process instance before/after migrating.
     *
     * @param <Parent> The class type of the {@link AdjustmentBuilder}'s parent builder.
     */
    public static class AdjustmentBuilder<Parent> implements FilteringBuilder<AdjustmentBuilder<Parent>> {

        private final ScenarioBuilder<Parent> parent;
        private final ProcessInstanceAdjustment adjustment;

        private AdjustmentBuilder(ScenarioBuilder<Parent> parent, ProcessInstanceAdjustment adjustment) {
            this.parent = parent;
            this.adjustment = adjustment;
        }

        @Override
        public AdjustmentBuilder<Parent> whereActive() {
            return addPredicate((service, definition, instance) -> !instance.isSuspended());
        }

        @Override
        public AdjustmentBuilder<Parent> whereSuspended() {
            return addPredicate((service, definition, instance) -> instance.isSuspended());
        }

        @Override
        public AdjustmentBuilder<Parent> whereDefinitionId(String definitionId) {
            return addPredicate((service, definition, instance) -> definition.getId().equals(definitionId));
        }

        @Override
        public AdjustmentBuilder<Parent> whereDefinitionKey(String definitionKey) {
            return addPredicate((service, definition, instance) -> definition.getKey().equals(definitionKey));
        }

        @Override
        public AdjustmentBuilder<Parent> whereVersionTag(String versionTag) {
            return addPredicate((service, definition, instance) -> definition.getVersionTag().equals(versionTag));
        }

        @Override
        public AdjustmentBuilder<Parent> whereVersion(int version) {
            return addPredicate((service, definition, instance) -> definition.getVersion() == version);
        }

        @Override
        public AdjustmentBuilder<Parent> whereActivity(String activityId) {
            return addPredicate((service, definition, instance) -> service.getActiveActivityIds(instance.getId()).contains(activityId));
        }

        @Override
        public AdjustmentBuilder<Parent> whereIncident() {
            return addPredicate((service, definition, instance) -> service.getActivityInstance(instance.getId()).getIncidents().length > 0);
        }

        @Override
        public AdjustmentBuilder<Parent> whereVariableEquals(String variableName, Object value) {
            return addPredicate((service, definition, instance) -> Objects.equals(service.getVariable(instance.getId(), variableName), value));
        }

        private AdjustmentBuilder<Parent> addPredicate(ProcessPredicate predicate) {
            this.adjustment.addPredicate(predicate);
            return this;
        }

        /**
         * Begin defining adjustments to execute before migrating a process instance.
         *
         * @return A new {@link BeforeMigrationAdjustmentBuilder}, never null
         */
        public BeforeMigrationAdjustmentBuilder<Parent> beforeMigrate() {
            return new BeforeMigrationAdjustmentBuilder<>(this.parent, this.adjustment);
        }

        /**
         * Begin defining adjustments to execute after migrating a process instance.
         *
         * @return A new {@link AfterMigrationAdjustmentBuilder}, never null
         */
        public AfterMigrationAdjustmentBuilder<Parent> afterMigrate() {
            return new AfterMigrationAdjustmentBuilder<>(this.parent, this.adjustment);
        }
    }

    /**
     * A builder for defining an adjustment's several modifications to a process instance.
     *
     * @param <Parent> The class type of this {@link AbstractAdjustmentBuilder}'s parent builder.
     * @param <This> The class type of this {@link AbstractAdjustmentBuilder}'s implementation.
     */
    public static abstract class AbstractAdjustmentBuilder<Parent, This> {

        /**
         * A builder to define an activity modification.
         */
        public final class ActivityModificationBuilder {

            private final class ActivityModification implements BiConsumer<RuntimeService, String> {

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

            private final List<Consumer<ProcessInstanceModificationBuilder>> modifications = new ArrayList<>();

            private ActivityModificationBuilder() {}

            /**
             * Cancel a specific activity instance of a given id.
             *
             * @see ProcessInstanceModificationBuilder#cancelActivityInstance(String)
             * @param activityInstanceId The activity instance's id; might <b>not</b> be null.
             * @return this
             */
            public ActivityModificationBuilder cancelActivityInstance(String activityInstanceId) {
                if (activityInstanceId == null) {
                    throw new IllegalArgumentException("Cannot cancel a null activity instance");
                }
                this.modifications.add(builder -> builder.cancelActivityInstance(activityInstanceId));
                return this;
            }

            /**
             * Cancel all activities of a given id.
             *
             * @see ProcessInstanceModificationBuilder#cancelAllForActivity(String)
             * @param activityId The activity's id; might <b>not</b> be null.
             * @return this
             */
            public ActivityModificationBuilder cancelAllForActivity(String activityId) {
                if (activityId == null) {
                    throw new IllegalArgumentException("Cannot cancel a null activity");
                }
                this.modifications.add(builder -> builder.cancelAllForActivity(activityId));
                return this;
            }

            /**
             * Cancel a specific transition instance of a given id.
             *
             * @see ProcessInstanceModificationBuilder#cancelTransitionInstance(String)
             * @param transitionInstanceId The transition instance's id; might <b>not</b> be null.
             * @return this
             */
            public ActivityModificationBuilder cancelTransitionInstance(String transitionInstanceId) {
                if (transitionInstanceId == null) {
                    throw new IllegalArgumentException("Cannot cancel a null transition instance");
                }
                this.modifications.add(builder -> builder.cancelTransitionInstance(transitionInstanceId));
                return this;
            }

            /**
             * Start a new activity right before the given activity.
             *
             * @see ProcessInstanceModificationBuilder#startBeforeActivity(String)
             * @param activityId The id of the activity to start before; might <b>not</b> be null.
             * @return this
             */
            public ActivityModificationBuilder startBeforeActivity(String activityId) {
                if (activityId == null) {
                    throw new IllegalArgumentException("Cannot start before a null activity");
                }
                this.modifications.add(builder -> builder.startBeforeActivity(activityId));
                return this;
            }

            /**
             * Start a new activity right after the given activity.
             *
             * @see ProcessInstanceModificationBuilder#startAfterActivity(String)
             * @param activityId The id of the activity to start after; might <b>not</b> be null.
             * @return this
             */
            public ActivityModificationBuilder startAfterActivity(String activityId) {
                if (activityId == null) {
                    throw new IllegalArgumentException("Cannot start after a null activity");
                }
                this.modifications.add(builder -> builder.startAfterActivity(activityId));
                return this;
            }

            /**
             * Start a new activity at the given transition.
             *
             * @see ProcessInstanceModificationBuilder#startTransition(String)
             * @param transitionId The id of the transition to start at; might <b>not</b> be null.
             * @return this
             */
            public ActivityModificationBuilder startTransition(String transitionId) {
                if (transitionId == null) {
                    throw new IllegalArgumentException("Cannot start after a null transition");
                }
                this.modifications.add(builder -> builder.startTransition(transitionId));
                return this;
            }

            /**
             * Declare this activity modification as fully defined.
             *
             * @return This {@link ActivityModificationBuilder}'s parent {@link AbstractAdjustmentBuilder}.
             */
            @SuppressWarnings("unchecked")
            public This then() {
                addModification(new ActivityModification(this.modifications));
                return (This) AbstractAdjustmentBuilder.this;
            }
        }

        /**
         * A builder to define a message correlation.
         */
        public final class MessageModificationBuilder {

            private final class MessageModification implements BiConsumer<RuntimeService, String> {

                private final String messageName;
                private final boolean all;
                private final List<Consumer<org.camunda.bpm.engine.runtime.MessageCorrelationBuilder>> modifications;

                private MessageModification(String messageName, boolean all, List<Consumer<org.camunda.bpm.engine.runtime.MessageCorrelationBuilder>> modifications) {
                    this.messageName = messageName;
                    this.all = all;
                    this.modifications = modifications;
                }

                @Override
                public void accept(RuntimeService runtimeService, String processInstanceId) {
                    org.camunda.bpm.engine.runtime.MessageCorrelationBuilder builder = runtimeService
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

            private final String messageName;
            private final List<Consumer<MessageCorrelationBuilder>> modifications = new ArrayList<>();

            private MessageModificationBuilder(String messageName) {
                this.messageName = messageName;
            }

            /**
             * Adds the given variable to the message to correlate.
             *
             * @see MessageCorrelationBuilder#setVariable(String, Object)
             * @param variableName The variable to correlate; might <b>not</b> be null.
             * @param value The variable's value to correlate; might be null.
             * @return this
             */
            public MessageModificationBuilder withVariable(String variableName, Object value) {
                if (variableName == null) {
                    throw new IllegalArgumentException("Cannot correlate a message's variable with a null name");
                }
                this.modifications.add(builder -> builder.setVariable(variableName, value));
                return this;
            }

            /**
             * Adds the given local variable to the message to correlate.
             *
             * @see MessageCorrelationBuilder#setVariableLocal(String, Object)
             * @param variableName The variable to correlate; might <b>not</b> be null.
             * @param value The variable's value to correlate; might be null.
             * @return this
             */
            public MessageModificationBuilder withLocalVariable(String variableName, Object value) {
                this.modifications.add(builder -> builder.setVariableLocal(variableName, value));
                return this;
            }

            /**
             * Declares to correlate the message to one receiver.
             *
             * @see MessageCorrelationBuilder#correlate()
             * @return The {@link MessageModificationBuilder}'s parent {@link AbstractAdjustmentBuilder}, never null
             */
            @SuppressWarnings("unchecked")
            public This toOne() {
                addModification(new MessageModification(messageName, false, this.modifications));
                return (This) AbstractAdjustmentBuilder.this;
            }

            /**
             * Declares to correlate the message to all receivers.
             *
             * @see MessageCorrelationBuilder#correlateAll()
             * @return The {@link MessageModificationBuilder}'s parent {@link AbstractAdjustmentBuilder}, never null
             */
            @SuppressWarnings("unchecked")
            public This toAll() {
                addModification(new MessageModification(messageName, true, this.modifications));
                return (This) AbstractAdjustmentBuilder.this;
            }
        }

        private final ScenarioBuilder<Parent> parent;
        private final Consumer<BiConsumer<RuntimeService, String>> modifications;

        private AbstractAdjustmentBuilder(ScenarioBuilder<Parent> parent, Consumer<BiConsumer<RuntimeService, String>> modifications) {
            this.parent = parent;
            this.modifications = modifications;
        }

        /**
         * Begins defining activity modifications to a process instance.
         *
         * @return A new {@link ActivityModificationBuilder}, never null
         */
        public ActivityModificationBuilder modify() {
            return new ActivityModificationBuilder();
        }

        /**
         * Begins defining a message to correlate to a process instance.
         *
         * @param messageName The message to correlate; might <b>not</b> be null.
         * @return A new {@link MessageModificationBuilder}, never null
         */
        public MessageModificationBuilder correlate(String messageName) {
            if (messageName == null) {
                throw new IllegalArgumentException("Cannot correlate a message with a null name");
            }
            return new MessageModificationBuilder(messageName);
        }

        /**
         * Sets the given variable to the process instance.
         *
         * @param variableName The variable to set; might <b>not</b> be null.
         * @param value The variable's value to set; might be null.
         * @return this
         */
        public This setVariable(String variableName, Object value) {
            if (variableName == null) {
                throw new IllegalArgumentException("Cannot set a variable with a null name");
            }
            return addModification(((runtimeService, instanceId) -> runtimeService.setVariable(instanceId, variableName, value)));
        }

        /**
         * Removed the given variable to the process instance.
         *
         * @param variableName The variable to remove; might <b>not</b> be null.
         * @return this
         */
        public This removeVariable(String variableName) {
            if (variableName == null) {
                throw new IllegalArgumentException("Cannot remove a variable with a null name");
            }
            return addModification(((runtimeService, instanceId) -> runtimeService.removeVariable(instanceId, variableName)));
        }

        private This addModification(BiConsumer<RuntimeService, String> modification) {
            this.modifications.accept(modification);
            return (This) this;
        }

        /**
         * Declares the modifications to the process instance complete.
         *
         * @return The {@link AbstractAdjustmentBuilder}'s parent {@link ScenarioBuilder}, never null
         */
        public ScenarioBuilder<Parent> then() {
            return this.parent;
        }
    }

    /**
     * A builder to define modifications to execute before migrating a process instance.
     *
     * @param <Parent> The class type of this {@link BeforeMigrationAdjustmentBuilder}'s parent builder.
     */
    public static class BeforeMigrationAdjustmentBuilder<Parent> extends AbstractAdjustmentBuilder<Parent, BeforeMigrationAdjustmentBuilder<Parent>> {

        private final ProcessInstanceAdjustment adjustment;

        private BeforeMigrationAdjustmentBuilder(ScenarioBuilder<Parent> parent, ProcessInstanceAdjustment adjustment) {
            super(parent, adjustment::addApplyBefore);
            this.adjustment = adjustment;
        }

        /**
         * Declares the definition of modifications to execute before a process instance's migration complete and
         * starts declaring modifications to execute after.
         *
         * @return A new {@link AfterMigrationAdjustmentBuilder}, never null
         */
        public AfterMigrationAdjustmentBuilder<Parent> afterMigrate() {
            return new AfterMigrationAdjustmentBuilder<>(this.then(), this.adjustment);
        }
    }

    /**
     * A builder to define modifications to execute after migrating a process instance.
     *
     * @param <Parent> The class type of this {@link AfterMigrationAdjustmentBuilder}'s parent builder.
     */
    public static class AfterMigrationAdjustmentBuilder<Parent> extends AbstractAdjustmentBuilder<Parent, AfterMigrationAdjustmentBuilder<Parent>> {

        private AfterMigrationAdjustmentBuilder(ScenarioBuilder<Parent> parent, ProcessInstanceAdjustment adjustment) {
            super(parent, adjustment::applyAfter);
        }
    }

    /**
     * A builder to start defining scenarios for a process engine with.
     */
    public static class EngineBuilder {

        private final ProcessEngine processEngine;

        private EngineBuilder(ProcessEngine processEngine) {
            this.processEngine = processEngine;
        }

        /**
         * Define the root scenario to all migrations.
         *
         * @return A new {@link ScenarioBuilder}, never null
         */
        public ScenarioBuilder<ExecutionBuilder> defineScenario() {
            return defineScenario("root");
        }

        /**
         * Define the root scenario to all migrations.
         *
         * @param title The root scenario's title; might <b>not</b> be null.
         * @return A new {@link ScenarioBuilder}, never null
         */
        public ScenarioBuilder<ExecutionBuilder> defineScenario(String title) {
            if (title == null) {
                throw new IllegalArgumentException("Cannot create a scenario without a title");
            }

            ExecutionBuilder executionBuilder = new ExecutionBuilder(this.processEngine);
            ScenarioBuilder<ExecutionBuilder> scenarioBuilder = new ScenarioBuilder<>(executionBuilder, title);

            executionBuilder.rootScenario = scenarioBuilder;

            return scenarioBuilder;
        }
    }

    /**
     * A builder to execute defined migration scenarios with.
     */
    public static class ExecutionBuilder {

        private final ProcessEngine processEngine;
        private ScenarioBuilder<ExecutionBuilder> rootScenario;

        private boolean log = true;

        private ExecutionBuilder(ProcessEngine processEngine) {
            this.processEngine = processEngine;
        }

        /**
         * Whether to log the migration's {@link Report}.
         *
         * @param log True if the migration's {@link Report} should be logged, false otherwise
         * @return this
         */
        public ExecutionBuilder log(boolean log) {
            this.log = log;
            return this;
        }

        /**
         * Triggers the migration of all scenarios defined.
         *
         * @return A new {@link Report} containing the information about the migration, never null
         */
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

    /**
     * A report about a {@link ProcessMigration}.
     */
    public static abstract class Report {

        private final String title;

        private Report(String title) {
            this.title = title;
        }

        /**
         * Returns the {@link Report}'s title.
         *
         * @return The title, never null.
         */
        public String getTitle() {
            return this.title;
        }

        /**
         * Whether all {@link ProcessMigration}'s listed in this report were successful.
         *
         * @return True if the migrations were all successful, false otherwise
         */
        public abstract boolean isSuccess();

        /**
         * Returns the count of process instances migrated.
         *
         * @return The total count migrated
         */
        public abstract int count();

        /**
         * Returns all child {@link Report}s to this {@link Report}.
         *
         * @return The children, never null
         */
        public abstract List<Report> getChildren();

        /**
         * Returns this {@link Report} in a pretty-printed form.
         *
         * @param wrap True if children of this {@link Report} should be printed using \n, false if the report should be printed in-line
         * @return A pretty-print of the {@link Report}, never null
         */
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

    private static final class BranchReport extends Report {

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

    private static final class LeafReport extends Report {

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
