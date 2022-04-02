package com.mantledillusion.essentials.camunda.migration;

import org.junit.jupiter.api.Test;

public class ProcessMigrationTest {

    @Test
    public void test() {
        ProcessMigration
                .findProcesses(null, null)
                .withDefinitionKey("eco-proc-bla")
                .defineScenarios()
                    .defineScenario("rev1 to rev2")
                        .withVersionTag("rev1")
                        .when("activity a1 active")
                            .withActivity("a1")
                            .beforeMigrate()
                                .cancelActivity("a1")
                            .afterMigrate()
                                .triggerMessage("MY_MSG")
                                .done()
                        .usingDefaultMappings()
                        .toDefinitionId("definition-id")
                        .done()
                    .defineScenario("rev2 to rev3")
                        .withVersionTag("rev2")
                        .toDefinitionKey("eco-proc-bla")
                        .toDefinitionTag("rev3")
                        .toLatestDefinitionVersion()
                        .when("abc is set to foo")
                            .withVariableEquals("abc", "foo")
                            .afterMigrate()
                                .setVariable("abc", "bar")
                                .done()
                        .defineScenarios()
                            .defineScenario("rev2 with A to rev3")
                                .withVariableEquals("xyz", "bar")
                                .usingMapping("a1", "A1")
                                .usingMapping("b1", "B1")
                                .done()
                            .defineScenario("rev2 with B to rev3")
                                .withVariableEquals("xyz", "bar")
                                .usingDefaultMappings()
                                .done()
                            .done()
                    .done()
                .migrate();
    }
}
