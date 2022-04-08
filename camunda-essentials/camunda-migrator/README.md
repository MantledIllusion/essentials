# Camunda Migrator

The **_ProcessMigration_** class allows executing migrations on **Camunda** processes that have been build using its fluent style builder.

```xml
<dependency>
    <groupId>com.mantledillusion.essentials</groupId>
    <artifactId>camunda-migrator</artifactId>
</dependency>
```

Get the newest version at [mvnrepository.com/camunda-migrator](https://mvnrepository.com/artifact/com.mantledillusion.essentials/camunda-migrator)

## The Gist about Scenarios

**_ProcessMigration_**'s fluent patter builder allows defining migrations as so-called **scenarios**, with a scenario being defined as an enclosed rule set for process instances and definitions that migrations can take place in.

As a result, a scenario's definition is complete once:
- There are sufficient filters to retrieve the process instances meant to be migrated
- There are sufficient filters to determine the one process definition to migrate them to
- There are sufficient activity mappings to execute the migration

Here is an example using the builder on a simple case:

```java
ProcessMigration
        .in(processEngine)                          // <- hand over Camunda's process engine
        .defineScenario()                           // <- create the base scenario
            .whereDefinitionId("my-process:1:1")    // <- filter process instances to migrate
            .usingDefaultMappings()                 // <- advice Camunda to use the default 1:1 activity mappings
            .toDefinitionId("my-process:2:3")       // <- filter process definitions to migrate to
            .finalizeScenario()                     // <- wrap up the scenario as complete
        .migrate();                                 // <- trigger migration
```

As applications grow, process definitions might go through several revisions, or there might be different cases on how to migrate a process instance depending on the circumstances.

Scenarios are able to adapt to such cases; instead of wrapping up a scenario using _finalizeScenario()_, multiple child scenarios can be defined using _defineScenarios()_:

```java
ProcessMigration
        .in(processEngine)
        .defineScenario()
            .whereDefinitionKey("my-process")       // <- define a general definition key filter
            .usingDefaultMappings()
            .toDefinitionKey("my-process")
            .defineScenarios()                      // <- begin defining child scenarios
                .defineScenario()
                    .whereVersionTag("rev1")        // <- define a specific tag filter for rev1 to rev2
                    .usingMapping("act1", "act2")   // <- define an additional activity mapping to only apply in this child scenario
                    .toDefinitionTag("rev2")
                    .finalizeScenario()
                .defineScenario()
                    .whereVersionTag("rev2")        // <- define a specific tag filter for rev2 to rev3
                    .toDefinitionTag("rev3")
                    .finalizeScenario()
                .finalizeScenarios()
        .migrate();
```

All declarations made by a parent scenario are automatically inherited by the child scenario.

When multiple scenarios are defined, they are executed in the exact same order as defined. This also allows that the same process instance might be migrated multiple times by the same migration in subsequent scenarios.

## Migration Options

The **_ScenarioBuilder_** used when declaring a scenario offers a set of flexible options allowing to migrate even complex cases.

### Process Instance Filters

The builder's _where()_ methods allow filtering the process instances to migrate.  All instances found using these filters will be migrated.

```java
ProcessMigration
        .in(processEngine)
        .defineScenario()
            .whereActive()
            .whereSuspended()
            .whereIncident()
            .whereDefinitionId("my-process:1:1")
            .whereDefinitionKey("my-process")
            .whereVersionTag("rev1")
            .whereVersion(1)
            .whereActivity("act2")
            .whereVariableEquals("myVar", "myValue")
            ...
            .finalizeScenario()
        .migrate();
```

Any combination of the filters are valid; they might logically exclude each other though, in a way that no instances can be found.

### Process Definition Filters

The builder's _to()_ methods allow filtering the process definition to migrate to.

```java
ProcessMigration
        .in(processEngine)
        .defineScenario()
            ...
            .toDefinitionId("my-process:2:3")
            .toDefinitionKey("my-process")
            .toDefinitionTag("rev2")
            .toSpecificDefinitionVersion(3)
            .toLatestDefinitionVersion()
            ...
            .finalizeScenario()
        .migrate();
```

Their combination has to find exactly 1 definition; if none or multiple definitions are found, the scenario fails and no instances are migrated.

### Activity Mappings

The builder's _using()_ methods allow declaring which source activityId is mapped to which target activityId.

```java
ProcessMigration
        .in(processEngine)
        .defineScenario()
            ...
            .usingDefaultMappings()
            .usingMapping("act1", "act2")
            ...
            .finalizeScenario()
        .migrate();
```

While _usingDefaultMappings()_ will auto-create mappings for all activities whose activityIds did not change, additional _usingMapping()_ calls might add mappings for changed activityIds.

### Process Instance Adjustments

The builder's _when()_ method begins an **_AdjustmentBuilder_** allowing to modify processes before/after the migration.

```java
ProcessMigration
        .in(processEngine)
        .defineScenario()
            ...
            .when()                                         // <- begin adjustment #1
                .whereVariableEquals("myVar", "myValue")    // <- only apply adjustment if filters match
                ...
                .beforeMigrate()                            // <- declare what to do before migrating the process
                    .removeVariable("myVar")
                    .modify()
                        .cancelAllForActivity("act1")
                        .then()
                .afterMigration()                           // <- declare what to do after migrating the process
                    .modify()
                        .startBeforeActivity("act2")
                        .then()
                    .correlate("MY_MESSAGE")
                        .withVariable("myVar2", "myValue2")
                        .toAll()
                    .then()
            .when()                                         // <- begin adjustment #2
                ...
            ...
            .finalizeScenario()
        .migrate();
```

Just as scenarios, the adjustment within a scenario are executed in the order they are defined.

### Failure Handling

The builders _on()_ methods allow defining failure handling for a scenario.

```java
ProcessMigration
        .in(processEngine)
        .defineScenario()
            ...
            .onFailureSuspend(true) // <- suspend a process instance when its migration fails
            .onFailureSkip(true)    // <- skip the rest of the scenario once one of its migrations fails
            ...
            .finalizeScenario()
        .migrate();
```

## Reporting

The _migrate()_ method returns a **_Report_** object indicating the migration's outcome.

The report contains the scenarios and their children as report children as well, so the migrations structure is retained. When defining scenarios and adjustments, titles can be set which will be taken over into the report.

```java
ProcessMigration
        .in(processEngine)
        .defineScenario("My Main Scenario")
            ...
            .defineScenarios()
                .defineScenario("My Sub Scenario #1")
                    ...
                    .finalizeScenario()
                .defineScenario("My Sub Scenario #2")
                    ...
                    .when("My Adjustment")
                        ...
                    ...
                    .finalizeScenario()
                .finalizeScenarios()
        .migrate();
```

Using _**Report**.prettyPrint()_ the whole report will be returned as a formatted string.

```text
Scenario: My Main Scenario
  Scenario: My Sub Scenario #1
    (succ) Found 1 definition matching definition filters: my-process:1:1
    (succ) Found 1 instance(s) matching instance filters
    Instance: 1
      (succ) Instance migration successful in 50ms
  Scenario: My Sub Scenario #2
    (succ) Found 1 definition matching definition filters: my-process:2:3
    (succ) Found 1 instance(s) matching instance filters
    Instance: 1
      (succ) My Adjustment
      (succ) Instance migration successful in 29ms
```

By default, the report will automatically be logged.