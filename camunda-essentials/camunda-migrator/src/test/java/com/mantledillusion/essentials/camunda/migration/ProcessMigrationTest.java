package com.mantledillusion.essentials.camunda.migration;

import org.junit.jupiter.api.Test;

public class ProcessMigrationTest {

    @Test
    public void test() {
        ProcessMigration
                .findProcesses(null)
                .withDefinitionKey("eco-proc-bla")
                .migrateScenarios()
                    .scenario("rev1 to rev2")
                        .withVersionTag("rev1")
                        .when("a1")
                            .withActivity("a1")
                            .beforeMigrate()
                                .cancelActivity("a1")
                            .afterMigrate()
                                .triggerMessage("MY_MSG")
                                .done()
                        .migrateProcess("definition-id")
                            .withDefaultMappings()
                            .migrate()
                    .scenario("rev2 to rev3")
                        .withVersionTag("rev2")
                        .migrateScenarios()
                            .scenario("rev2 with A to rev3")
                                .withVariable("abc")
                                .migrateProcess("eco-proc-bla", "rev3")
                                    .withMapping("a1", "A1")
                                    .withMapping("b1", "B1")
                                    .migrate()
                            .scenario("rev2 with B to rev3")
                                .withVariable("abc", "bar")
                                .migrateProcess("eco-proc-bla", "rev3")
                                    .withDefaultMappings()
                                    .migrate()
                            .migrate()
        ;
    }
}
