package com.mantledillusion.essentials.camunda.migration;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.migration.MigrationInstructionBuilder;
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
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Customizer builder style migrator for Camunda processes.
 * <p>
 * New builders are created using {@link ProcessMigration#in(ProcessEngine)}, which leads to beginning a root-scenario.
 * A scenario is a differentiated set of boundaries in which process instances are migrated.
 * <p>
 * Within a scenario, any of the following methods can be used to define its boundaries:<br>
 * <code>where*()</code> - define filters for the source process instances to migrate<br>
 * <code>to*()</code> - define filters to determine the single target process definition to migrate to<br>
 * <code>using*()</code> - define activity mappings for the migration plan<br>
 * <code>when()</code> - define conditions for modifying process instances before/after commencing the migration<br>
 * <code>on*()</code> - (optional)
 * <p>
 * Calling the following method allows defining nested sub-scenarios, based on the boundaries defined for its parent:
 * <code>defineScenario()</code>
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
         * Filter for process instances whose definition has one of the given ids.
         *
         * @see ProcessInstanceQuery#processDefinitionId(String)
         * @param definitionIds The definitionIds to filter for; might <b>not</b> be null
         * @return this
         */
        This whereDefinitionIdIn(String... definitionIds);

        /**
         * Filter for process instances whose definition's id matches the given pattern.
         *
         * @see ProcessInstanceQuery#processDefinitionId(String)
         * @param definitionIdPattern The definitionId regex pattern to filter for; might <b>not</b> be null
         * @return this
         */
        This whereDefinitionIdLike(String definitionIdPattern);

        /**
         * Filter for process instances whose definition has the given key.
         *
         * @see ProcessInstanceQuery#processDefinitionKey(String)
         * @param definitionKey The definitionKey to filter for; might <b>not</b> be null
         * @return this
         */
        This whereDefinitionKey(String definitionKey);

        /**
         * Filter for process instances whose definition has one of the given keys.
         *
         * @see ProcessInstanceQuery#processDefinitionKey(String)
         * @param definitionKeys The definitionKeys to filter for; might <b>not</b> be null
         * @return this
         */
        This whereDefinitionKeyIn(String... definitionKeys);

        /**
         * Filter for process instances whose definition's key matches the given pattern.
         *
         * @see ProcessInstanceQuery#processDefinitionKey(String)
         * @param definitionKeyPattern The definitionKey regex pattern to filter for; might <b>not</b> be null
         * @return this
         */
        This whereDefinitionKeyLike(String definitionKeyPattern);

        /**
         * Filter for process instances whose definition has the given version tag.
         *
         * @see ProcessDefinition#getVersionTag()
         * @param versionTag The versionTag to filter for; might be null.
         * @return this
         */
        This whereVersionTag(String versionTag);

        /**
         * Filter for process instances whose definition has one of the given version tags.
         *
         * @see ProcessDefinition#getVersionTag()
         * @param versionTags The versionTags to filter for; might be null.
         * @return this
         */
        This whereVersionTagIn(String... versionTags);

        /**
         * Filter for process instances whose definition's version tag matches the given pattern.
         *
         * @see ProcessDefinition#getVersionTag()
         * @param versionTagPattern The versionTag regex pattern to filter for; might be null.
         * @return this
         */
        This whereVersionTagLike(String versionTagPattern);

        /**
         * Filter for process instances whose definition has the given version.
         *
         * @see ProcessDefinition#getVersion()
         * @param version The version to filter for; might <b>not</b> be null.
         * @return this
         */
        This whereVersion(int version);

        /**
         * Filter for process instances whose definition has one of the given versions.
         *
         * @see ProcessDefinition#getVersion()
         * @param versions The versions to filter for; might <b>not</b> be null.
         * @return this
         */
        This whereVersionIn(Integer... versions);

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
     * @param <This> The class type of the implementing builder
     */
    public abstract static class AbstractScenarioBuilder<This> implements FilteringBuilder<This> {

        private final String title;
        private final List<Consumer<ProcessMigration>> adaptors = new ArrayList<>();
        private final List<BiConsumer<ProcessMigration, BranchReport>> migrators = new ArrayList<>();

        private AbstractScenarioBuilder(String title) {
            this.title = title;
        }

        @Override
        public This whereActive() {
            return addAdaptor(migration -> migration.addProcessPreFilter(ProcessInstanceQuery::active));
        }

        @Override
        public This whereSuspended() {
            return addAdaptor(migration -> migration.addProcessPreFilter(ProcessInstanceQuery::suspended));
        }

        @Override
        public This whereDefinitionId(String definitionId) {
            if (definitionId == null) {
                throw new IllegalArgumentException("Unable to filter for a null definition id");
            }
            return addAdaptor(migration -> migration.addProcessPreFilter(query -> query.processDefinitionId(definitionId)));
        }

        @Override
        public This whereDefinitionIdIn(String... definitionIds) {
            Set<String> definitionIdSet = new HashSet<>(Arrays.asList(definitionIds));
            return addAdaptor(migration -> migration.addProcessPostFilter((definition, instance) -> definitionIdSet.contains(definition.getId())));
        }

        @Override
        public This whereDefinitionIdLike(String definitionIdPattern) {
            return addAdaptor(migration -> migration.addProcessPostFilter((definition, instance) -> definition.getId().matches(definitionIdPattern)));
        }

        @Override
        public This whereDefinitionKey(String definitionKey) {
            if (definitionKey == null) {
                throw new IllegalArgumentException("Unable to filter for a null definition key");
            }
            return addAdaptor(migration -> migration.addProcessPreFilter(query -> query.processDefinitionKey(definitionKey)));
        }

        @Override
        public This whereDefinitionKeyIn(String... definitionKeys) {
            return addAdaptor(migration -> migration.addProcessPreFilter(query -> query.processDefinitionKeyIn(definitionKeys)));
        }

        @Override
        public This whereDefinitionKeyLike(String definitionKeyPattern) {
            return addAdaptor(migration -> migration.addProcessPostFilter((definition, instance) -> definition.getKey().matches(definitionKeyPattern)));
        }

        @Override
        public This whereVersionTag(String versionTag) {
            return addAdaptor(migration -> migration.addProcessPostFilter((definition, instance) -> Objects.equals(definition.getVersionTag(), versionTag)));
        }

        @Override
        public This whereVersionTagIn(String... versionTags) {
            Set<String> versionTagSet = new HashSet<>(Arrays.asList(versionTags));
            return addAdaptor(migration -> migration.addProcessPostFilter((definition, instance) -> versionTagSet.contains(definition.getVersionTag())));
        }

        @Override
        public This whereVersionTagLike(String versionTagPattern) {
            return addAdaptor(migration -> migration.addProcessPostFilter((definition, instance) -> StringUtil.defaultString(definition.getVersionTag()).matches(versionTagPattern)));
        }

        @Override
        public This whereVersion(int version) {
            return addAdaptor(migration -> migration.addProcessPostFilter((definition, instance) -> definition.getVersion() == version));
        }

        @Override
        public This whereVersionIn(Integer... versions) {
            Set<Integer> versionSet = new HashSet<>(Arrays.asList(versions));
            return addAdaptor(migration -> migration.addProcessPostFilter((definition, instance) -> versionSet.contains(definition.getVersion())));
        }

        @Override
        public This whereActivity(String activityId) {
            if (activityId == null) {
                throw new IllegalArgumentException("Unable to filter for a null activity id");
            }
            return addAdaptor(migration -> migration.addProcessPreFilter(query -> query.activityIdIn(activityId)));
        }

        @Override
        public This whereIncident() {
            return addAdaptor(migration -> migration.addProcessPreFilter(ProcessInstanceQuery::withIncident));
        }

        @Override
        public This whereVariableEquals(String variableName, Object value) {
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
        public This toDefinitionId(String definitionId) {
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
        public This toDefinitionKey(String definitionKey) {
            if (definitionKey == null) {
                throw new IllegalArgumentException("Unable to filter for a null definition key");
            }
            return addAdaptor(migration -> migration.addDefinitionFilter(query -> query.processDefinitionKey(definitionKey)));
        }

        /**
         * Migrate the process instances to a definition with the given tag.
         *
         * @see ProcessDefinitionQuery#versionTag(String)
         * @param versionTag The tag to migrate to; might be null.
         * @return this
         */
        public This toVersionTag(String versionTag) {
            return addAdaptor(migration -> migration.addDefinitionFilter(query -> query.versionTag(versionTag)));
        }

        /**
         * Migrate the process instances to a definition with a specific version.
         *
         * @see ProcessDefinitionQuery#processDefinitionVersion(Integer)
         * @param definitionVersion The key to migrate to.
         * @return this
         */
        public This toSpecificDefinitionVersion(int definitionVersion) {
            return addAdaptor(migration -> migration.addDefinitionFilter(query -> query.processDefinitionVersion(definitionVersion)));
        }

        /**
         * Migrate the process instances to the definition with the latest version.
         *
         * @see ProcessDefinitionQuery#latestVersion
         * @return this
         */
        public This toLatestDefinitionVersion() {
            return addAdaptor(migration -> migration.addDefinitionFilter(ProcessDefinitionQuery::latestVersion));
        }

        /**
         * Use the default activity mappings between the source and target process definitions.
         *
         * @see MigrationPlanBuilder#mapEqualActivities
         * @return this
         */
        public This usingDefaultMappings() {
            return addAdaptor(migration -> migration.addMigrationPlanModification(MigrationPlanBuilder::mapEqualActivities));
        }

        /**
         * Use the given specific activity mapping.
         *
         * @see MigrationPlanBuilder#mapActivities(String, String)
         * @param sourceActivityId The activityId in the process definition of the source process instance; might <b>not</b> be null.
         * @param targetActivityId The activityId in the target process definition; might <b>not</b> be null.
         * @return this
         */
        public This usingMapping(String sourceActivityId, String targetActivityId) {
            return usingMapping(sourceActivityId, targetActivityId, false);
        }

        /**
         * Use the given specific activity mapping.
         *
         * @see MigrationPlanBuilder#mapActivities(String, String)
         * @param sourceActivityId The activityId in the process definition of the source process instance; might <b>not</b> be null.
         * @param targetActivityId The activityId in the target process definition; might <b>not</b> be null.
         * @param updateEventTrigger Whether to update triggers for the given activities' events using {@link MigrationInstructionBuilder#updateEventTrigger()}
         * @return this
         */
        public This usingMapping(String sourceActivityId, String targetActivityId, boolean updateEventTrigger) {
            if (sourceActivityId == null) {
                throw new IllegalArgumentException("Cannot map from a null source activity");
            } else if (targetActivityId == null) {
                throw new IllegalArgumentException("Cannot map to a null target activity");
            }
            return addAdaptor(migration -> migration.addMigrationPlanModification(plan -> {
                MigrationInstructionBuilder instruction = plan.mapActivities(sourceActivityId, targetActivityId);
                if (updateEventTrigger) {
                    instruction.updateEventTrigger();
                }
            }));
        }

        /**
         * Begins defining modifications to a process instance to migrate in case it matches a specifiable condition.
         *
         * @return this
         */
        public This when(Consumer<ConditionBuilder> conditionCustomizer) {
            return when("unnamed", conditionCustomizer);
        }

        /**
         * Begins defining modifications to a process instance to migrate in case it matches a specifiable condition.
         *
         * @param title The modification's title; might <b>not</b> be null.
         * @return this
         */
        @SuppressWarnings("unchecked")
        public This when(String title, Consumer<ConditionBuilder> conditionCustomizer) {
            if (title == null) {
                throw new IllegalArgumentException("Cannot create a condition with a null title");
            }
            ProcessInstanceModification modification = new ProcessInstanceModification(title);
            this.adaptors.add(migration -> migration.addProcessInstanceModification(modification));
            conditionCustomizer.accept(new ConditionBuilder(modification));
            return (This) this;
        }

        /**
         * Upon failing migration in this scenario, the rest of the scenario will be skipped.
         * <p>
         * If one of the sub scenarios defined in this scenario fails, all subsequent sub scenarios within this scenario will be skipped.
         * <p>
         * If one of the process instance migrated by this scenario fails, all subsequent process instances within this scenario will be skipped.
         *
         * @param skip True if the rest of the scenario should be skipped upon failure, false otherwise.
         * @return this
         */
        public This onFailureSkip(boolean skip) {
            return addAdaptor(migration -> migration.setSkip(skip));
        }

        /**
         * Upon failing migration in this scenario, all process instances handled in the rest of this scenario will be suspended.
         * <p>
         * If one of the process instance migrated by this scenario fails, all subsequent process instances within this scenario will be suspended.
         *
         * @param suspend True if the rest of the scenario's process instances should be suspended upon failure, false otherwise.
         * @return this
         */
        public This onFailureSuspend(boolean suspend) {
            return addAdaptor(migration -> migration.setSuspend(suspend));
        }

        /**
         * Begins defining a new sub-scenario.
         *
         * @param scenarioCustomizer A customizer that for the sub-scenario using the given builder; might <b>not</b> be null.
         * @return A new {@link AbstractScenarioBuilder}, never null
         */
        public This defineScenario(Consumer<SubScenarioBuilder> scenarioCustomizer) {
            return defineScenario("unnamed", scenarioCustomizer);
        }

        /**
         * Begins defining a new sub-scenario.
         *
         * @param title The title of the sub-scenario, might <b>not</b> be null.
         * @param scenarioCustomizer A customizer that for the sub-scenario using the given builder; might <b>not</b> be null.
         * @return this
         */
        @SuppressWarnings("unchecked")
        public This defineScenario(String title, Consumer<SubScenarioBuilder> scenarioCustomizer) {
            if (title == null) {
                throw new IllegalArgumentException("Cannot create a scenario without a title");
            }
            SubScenarioBuilder scenarioBuilder = new SubScenarioBuilder(title);
            scenarioCustomizer.accept(scenarioBuilder);
            addMigrator((migration, report) -> report.add(((AbstractScenarioBuilder<This>) scenarioBuilder).migrate(migration)));
            return (This) this;
        }

        @SuppressWarnings("unchecked")
        private <A extends Consumer<ProcessMigration>> This addAdaptor(A adaptor) {
            this.adaptors.add(adaptor);
            return (This) this;
        }

        private <M extends BiConsumer<ProcessMigration, BranchReport>> void addMigrator(M migrator) {
            this.migrators.add(migrator);
        }

        private BranchReport migrate(ProcessMigration processMigration) {
            // ADAPT MIGRATION
            ProcessMigration scenarioMigration = processMigration.copy();
            this.adaptors.forEach(adaptor -> adaptor.accept(scenarioMigration));

            // CREATE REPORT
            BranchReport scenarioReport = new BranchReport(Optional.ofNullable(this.title)
                    .map(title -> "Scenario: " + title)
                    .orElse(null));

            // MIGRATE SCENARIO
            if (this.migrators.isEmpty()) {
                scenarioMigration.migrate(scenarioReport);
            } else {
                for (BiConsumer<ProcessMigration, BranchReport> migrator: this.migrators) {
                    migrator.accept(scenarioMigration, scenarioReport);
                    if (scenarioMigration.skip && !scenarioReport.isSuccess()) {
                        scenarioReport.add(new LeafReport("Skipping configured; scenario will not continue any further", true));
                        break;
                    }
                }
            }

            return scenarioReport;
        }
    }

    private static final class ProcessInstanceModification {

        private final String title;
        private final List<ProcessPredicate> predicates = new ArrayList<>();
        private final List<BiConsumer<RuntimeService, String>> before = new ArrayList<>();
        private final List<BiConsumer<RuntimeService, String>> after = new ArrayList<>();

        private ProcessInstanceModification(String title) {
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

        private void addApplyAfter(BiConsumer<RuntimeService, String> modification) {
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
     * A builder for defining filters determining whether to modify to a process instance before/after migrating.
     */
    public static class ConditionBuilder implements FilteringBuilder<ConditionBuilder> {

        private final ProcessInstanceModification modification;

        private ConditionBuilder(ProcessInstanceModification modification) {
            this.modification = modification;
        }

        @Override
        public ConditionBuilder whereActive() {
            return addPredicate((service, definition, instance) -> !instance.isSuspended());
        }

        @Override
        public ConditionBuilder whereSuspended() {
            return addPredicate((service, definition, instance) -> instance.isSuspended());
        }

        @Override
        public ConditionBuilder whereDefinitionId(String definitionId) {
            return addPredicate((service, definition, instance) -> definition.getId().equals(definitionId));
        }

        @Override
        public ConditionBuilder whereDefinitionIdIn(String... definitionIds) {
            Set<String> definitionIdSet = new HashSet<>(Arrays.asList(definitionIds));
            return addPredicate((service, definition, instance) -> definitionIdSet.contains(definition.getId()));
        }

        @Override
        public ConditionBuilder whereDefinitionIdLike(String definitionIdPattern) {
            return addPredicate((service, definition, instance) -> definition.getId().matches(definitionIdPattern));
        }

        @Override
        public ConditionBuilder whereDefinitionKey(String definitionKey) {
            return addPredicate((service, definition, instance) -> definition.getKey().equals(definitionKey));
        }

        @Override
        public ConditionBuilder whereDefinitionKeyIn(String... definitionKeys) {
            Set<String> definitionKeySet = new HashSet<>(Arrays.asList(definitionKeys));
            return addPredicate((service, definition, instance) -> definitionKeySet.contains(definition.getKey()));
        }

        @Override
        public ConditionBuilder whereDefinitionKeyLike(String definitionKeyPattern) {
            return addPredicate((service, definition, instance) -> definition.getKey().matches(definitionKeyPattern));
        }

        @Override
        public ConditionBuilder whereVersionTag(String versionTag) {
            return addPredicate((service, definition, instance) -> definition.getVersionTag().equals(versionTag));
        }

        @Override
        public ConditionBuilder whereVersionTagIn(String... versionTags) {
            Set<String> versionTagSet = new HashSet<>(Arrays.asList(versionTags));
            return addPredicate((service, definition, instance) -> versionTagSet.contains(definition.getVersionTag()));
        }

        @Override
        public ConditionBuilder whereVersionTagLike(String versionTagPattern) {
            return addPredicate((service, definition, instance) -> StringUtil.defaultString(definition.getVersionTag()).matches(versionTagPattern));
        }

        @Override
        public ConditionBuilder whereVersion(int version) {
            return addPredicate((service, definition, instance) -> definition.getVersion() == version);
        }

        @Override
        public ConditionBuilder whereVersionIn(Integer... versions) {
            Set<Integer> versionSet = new HashSet<>(Arrays.asList(versions));
            return addPredicate((service, definition, instance) -> versionSet.contains(definition.getVersion()));
        }

        @Override
        public ConditionBuilder whereActivity(String activityId) {
            return addPredicate((service, definition, instance) -> service.getActiveActivityIds(instance.getId()).contains(activityId));
        }

        @Override
        public ConditionBuilder whereIncident() {
            return addPredicate((service, definition, instance) -> service.getActivityInstance(instance.getId()).getIncidents().length > 0);
        }

        @Override
        public ConditionBuilder whereVariableEquals(String variableName, Object value) {
            return addPredicate((service, definition, instance) -> Objects.equals(service.getVariable(instance.getId(), variableName), value));
        }

        private ConditionBuilder addPredicate(ProcessPredicate predicate) {
            this.modification.addPredicate(predicate);
            return this;
        }

        /**
         * Begin defining modifications to execute before migrating a process instance.
         *
         * @param modificationCustomizer A customizer that for the modification using the given builder; might <b>not</b> be null.
         * @return this
         */
        public ConditionBuilder beforeMigrate(Consumer<ModificationBuilder> modificationCustomizer) {
            modificationCustomizer.accept(new ModificationBuilder(this.modification::addApplyBefore));
            return this;
        }

        /**
         * Begin defining modifications to execute after migrating a process instance.
         *
         * @param modificationCustomizer A customizer that for the modification using the given builder; might <b>not</b> be null.
         * @return this
         */
        public ConditionBuilder afterMigrate(Consumer<ModificationBuilder> modificationCustomizer) {
            modificationCustomizer.accept(new ModificationBuilder(this.modification::addApplyAfter));
            return this;
        }
    }

    /**
     * A builder for defining modifications to a process instance.
     */
    public static class ModificationBuilder {

        /**
         * A builder to define an activity modification.
         */
        public static final class ActivityModificationBuilder {

            private final List<Consumer<ProcessInstanceModificationBuilder>> modifications;

            private ActivityModificationBuilder(List<Consumer<ProcessInstanceModificationBuilder>> modifications) {
                this.modifications = modifications;
            }

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
        }

        /**
         * A builder to define a message correlation.
         */
        public static final class MessageModificationBuilder {

            private final List<Consumer<MessageCorrelationBuilder>> modifications;

            private MessageModificationBuilder(List<Consumer<MessageCorrelationBuilder>> modifications) {
                this.modifications = modifications;
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
        }

        private final Consumer<BiConsumer<RuntimeService, String>> modifications;

        private ModificationBuilder(Consumer<BiConsumer<RuntimeService, String>> modifications) {
            this.modifications = modifications;
        }

        /**
         * Begins defining activity modifications to a process instance.
         *
         * @param activityModificationCustomizer A customizer that for the activity modification using the given builder; might <b>not</b> be null.
         * @return this
         */
        public ModificationBuilder modify(Consumer<ActivityModificationBuilder> activityModificationCustomizer) {
            List<Consumer<ProcessInstanceModificationBuilder>> modifications = new ArrayList<>();
            activityModificationCustomizer.accept(new ActivityModificationBuilder(modifications));
            addModification((runtimeService, processInstanceId) -> {
                ProcessInstanceModificationBuilder builder = runtimeService
                        .createProcessInstanceModification(processInstanceId);

                modifications.forEach(modification -> modification.accept(builder));

                builder.execute();
            });
            return this;
        }

        /**
         * Begins defining a message to correlate to one process instance.
         *
         * @param messageName The message to correlate; might <b>not</b> be null.
         * @return this
         */
        public ModificationBuilder correlateOnce(String messageName) {
            return correlateOnce(messageName, message -> {});
        }

        /**
         * Begins defining a message to correlate to one process instance.
         *
         * @param messageName The message to correlate; might <b>not</b> be null.
         * @param messageModificationCustomizer A customizer that for the message modification using the given builder; might <b>not</b> be null.
         * @return this
         */
        public ModificationBuilder correlateOnce(String messageName, Consumer<MessageModificationBuilder> messageModificationCustomizer) {
            return correlate(messageName, false, messageModificationCustomizer);
        }

        /**
         * Begins defining a message to correlate to all process instances.
         *
         * @param messageName The message to correlate; might <b>not</b> be null.
         * @return this
         */
        public ModificationBuilder correlateAll(String messageName) {
            return correlateAll(messageName, message -> {});
        }

        /**
         * Begins defining a message to correlate to all process instances.
         *
         * @param messageName The message to correlate; might <b>not</b> be null.
         * @param messageModificationCustomizer A customizer that for the message modification using the given builder; might <b>not</b> be null.
         * @return this
         */
        public ModificationBuilder correlateAll(String messageName, Consumer<MessageModificationBuilder> messageModificationCustomizer) {
            return correlate(messageName, true, messageModificationCustomizer);
        }

        /**
         * Begins defining a message to correlate to a process instance.
         *
         * @param messageName The message to correlate; might <b>not</b> be null.
         * @return A new {@link MessageModificationBuilder}, never null
         */
        public ModificationBuilder correlate(String messageName, boolean all, Consumer<MessageModificationBuilder> messageModificationCustomizer) {
            if (messageName == null) {
                throw new IllegalArgumentException("Cannot correlate a message with a null name");
            }
            List<Consumer<MessageCorrelationBuilder>> modifications = new ArrayList<>();
            messageModificationCustomizer.accept(new MessageModificationBuilder(modifications));
            addModification((runtimeService, processInstanceId) -> {
                MessageCorrelationBuilder builder = runtimeService
                        .createMessageCorrelation(messageName)
                        .processInstanceId(processInstanceId);

                modifications.forEach(modification -> modification.accept(builder));

                if (all) {
                    builder.correlateAll();
                } else {
                    builder.correlate();
                }
            });
            return this;
        }

        /**
         * Sets the given variable to the process instance.
         *
         * @param variableName The variable to set; might <b>not</b> be null.
         * @param value The variable's value to set; might be null.
         * @return this
         */
        public ModificationBuilder setVariable(String variableName, Object value) {
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
        public ModificationBuilder removeVariable(String variableName) {
            if (variableName == null) {
                throw new IllegalArgumentException("Cannot remove a variable with a null name");
            }
            return addModification(((runtimeService, instanceId) -> runtimeService.removeVariable(instanceId, variableName)));
        }

        private ModificationBuilder addModification(BiConsumer<RuntimeService, String> modification) {
            this.modifications.accept(modification);
            return this;
        }
    }

    /**
     * A builder to define a root scenario for a process engine with.
     */
    public static class RootScenarioBuilder extends AbstractScenarioBuilder<RootScenarioBuilder> {

        private final ProcessEngine processEngine;
        private boolean log = true;

        private RootScenarioBuilder(ProcessEngine processEngine, String title) {
            super(title);
            this.processEngine = processEngine;
        }

        /**
         * Whether to log the migration's {@link Report}.
         *
         * @param log True if the migration's {@link Report} should be logged, false otherwise
         * @return this
         */
        public RootScenarioBuilder log(boolean log) {
            this.log = log;
            return this;
        }

        /**
         * Triggers the migration of all scenarios defined.
         *
         * @return A new {@link Report} containing the information about the migration, never null
         */
        public Report migrate() {
            Report rootReport = ((AbstractScenarioBuilder<RootScenarioBuilder>) this).migrate(new ProcessMigration(this.processEngine,
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
                    // DEFAULT PROCESS INSTANCE MODIFICATIONS
                    new ArrayList<>(),
                    // DEFAULT MIGRATION PLAN MODIFICATIONS
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
     * A builder to define a sub scenario for a process engine with.
     */
    public static class SubScenarioBuilder extends AbstractScenarioBuilder<SubScenarioBuilder> {

        private SubScenarioBuilder(String title) {
            super(title);
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
         * @return The title, might be null
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
            var title = Optional.ofNullable(getTitle());
            String[] report = Stream
                    .concat(
                            title.stream().map(t -> getPadding(layer) + t),
                            this.children.stream().map(child -> child.prettyPrint(layer+(title.isPresent() ? 1 : 0), prefix, infix, postFix))
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
    private final List<ProcessInstanceModification> processInstanceModifications;
    private final List<Consumer<MigrationPlanBuilder>> migrationPlanModifications;

    private boolean skip;
    private boolean suspend;

    private ProcessMigration(ProcessEngine processEngine, AtomicInteger referenceGenerator,
                              boolean skip, boolean suspend,
                              List<Consumer<ProcessDefinitionQuery>> definitionFilters,
                              List<Consumer<ProcessInstanceQuery>> instancePreFilters,
                              List<BiPredicate<ProcessDefinition, ProcessInstance>> instancePostFilters,
                              List<ProcessInstanceModification> processInstanceModifications,
                              List<Consumer<MigrationPlanBuilder>> migrationPlanModifications) {
        this.processEngine = processEngine;
        this.referenceGenerator = referenceGenerator;

        this.skip = skip;
        this.suspend = suspend;

        this.definitionFilters = definitionFilters;
        this.instancePreFilters = instancePreFilters;
        this.instancePostFilters = instancePostFilters;
        this.processInstanceModifications = processInstanceModifications;
        this.migrationPlanModifications = migrationPlanModifications;
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

    private void addProcessInstanceModification(ProcessInstanceModification modification) {
        this.processInstanceModifications.add(modification);
    }

    private void addMigrationPlanModification(Consumer<MigrationPlanBuilder> modification) {
        this.migrationPlanModifications.add(modification);
    }

    private ProcessMigration copy() {
        return new ProcessMigration(this.processEngine, this.referenceGenerator,
                this.skip, this.suspend,
                new ArrayList<>(this.definitionFilters),
                new ArrayList<>(this.instancePreFilters),
                new ArrayList<>(this.instancePostFilters),
                new ArrayList<>(this.processInstanceModifications),
                new ArrayList<>(this.migrationPlanModifications));
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
                    .toList();

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

                    // MODIFY MIGRATION PLAN
                    this.migrationPlanModifications.forEach(modification -> modification.accept(builder));

                    // BUILD MIGRATION PLAN
                    MigrationPlan plan = builder.build();

                    // GATHER APPLYING INSTANCE MODIFICATIONS
                    List<ProcessInstanceModification> modifications = this.processInstanceModifications.stream()
                            .filter(modification -> modification.applies(instanceReport, this.processEngine.getRuntimeService(), targetDefinition, instance))
                            .collect(Collectors.toList());

                    // MODIFY INSTANCE BEFORE MIGRATION
                    boolean success = applyModifications(instance, modifications, true, instanceReport);
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

                    // MODIFY INSTANCE AFTER MIGRATION
                    success = applyModifications(instance, modifications, false, instanceReport);
                    if (!success && this.skip) {
                        break;
                    }
                }
            }
        }
    }

    private boolean applyModifications(ProcessInstance instance, List<ProcessInstanceModification> modifications,
                                       boolean before, BranchReport instanceReport) {
        for (ProcessInstanceModification instanceModification: modifications) {
            for (BiConsumer<RuntimeService, String> phaseModification: (before
                    ? instanceModification.getApplyBefore()
                    : instanceModification.getApplyAfter())) {
                try {
                    phaseModification.accept(this.processEngine.getRuntimeService(), instance.getId());
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

    /**
     * Begin building a new migration.
     *
     * @param processEngine The {@link ProcessEngine} to migrate in; might <b>not</b> be null.
     * @return A new {@link RootScenarioBuilder}, never null
     *
     */
    public static RootScenarioBuilder in(ProcessEngine processEngine) {
        return in(processEngine, null);
    }

    /**
     * Begin building a new migration.
     *
     * @param processEngine The {@link ProcessEngine} to migrate in; might <b>not</b> be null.
     * @param title The title of the root-scenario, might be null.
     * @return A new {@link RootScenarioBuilder}, never null
     *
     */
    public static RootScenarioBuilder in(ProcessEngine processEngine, String title) {
        return new RootScenarioBuilder(processEngine, title);
    }
}