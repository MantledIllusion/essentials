package com.mantledillusion.essentials.camunda.migration;

import org.junit.jupiter.api.Test;

public class ProcessMigrationTest {

    @Test
    public void test() {
        ProcessMigration
                .findProcesses(null)
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
                        .defineMigrationTo("definition-id")
                            .withDefaultMappings()
                            .done()
                    .defineScenario("rev2 to rev3")
                        .withVersionTag("rev2")
                        .when("abc is set to foo")
                            .withVariable("abc", "foo")
                            .afterMigrate()
                                .setVariable("abc", "bar")
                                .done()
                        .defineScenarios()
                            .defineScenario("rev2 with A to rev3")
                                .withVariable("xyz")
                                .defineMigrationTo("eco-proc-bla", "rev3")
                                    .withMapping("a1", "A1")
                                    .withMapping("b1", "B1")
                                    .done()
                            .defineScenario("rev2 with B to rev3")
                                .withVariable("xyz", "bar")
                                .defineMigrationTo("eco-proc-bla", "rev3")
                                    .withDefaultMappings()
                                    .done()
                            .done()
                    .done()
                .migrate();
    }
}
