package com.mantledillusion.essentials.camunda.migration;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ProcessEngineExtension.class)
public class MigrateReportTest extends AbstractProcessMigrationTest {

    private static final String NAME_ROOT = "My Migration";
    private static final String NAME_1_TO_2 = "rev1 -> rev2";
    private static final String NAME_2_TO_3 = "rev2 -> rev3";

    private ProcessEngine engine;

    @Override
    public ProcessEngine getEngine() {
        return engine;
    }

    @Test
    public void testMigrateReport() {
        ProcessDefinition sourceDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV1);
        start(sourceDefinition.getId());
        deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV2);
        deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV3);

        ProcessMigration.Report report = ProcessMigration
                .in(engine)
                .defineScenario(NAME_ROOT)
                .whereDefinitionKey(Processes.RelabelActivity.DEFINITION_KEY)
                .toDefinitionKey(Processes.RelabelActivity.DEFINITION_KEY)
                .usingDefaultMappings()
                .defineScenarios()
                    .defineScenario(NAME_1_TO_2)
                        .whereVersionTag(Processes.Common.VersionTags.REV1)
                        .toVersionTag(Processes.Common.VersionTags.REV2)
                        .finalizeScenario()
                    .defineScenario(NAME_2_TO_3)
                        .whereVersionTag(Processes.Common.VersionTags.REV2)
                        .toVersionTag(Processes.Common.VersionTags.REV3)
                        .finalizeScenario()
                    .finalizeScenarios()
                .migrate();

        assertEquals("Scenario: " + NAME_ROOT, report.getTitle());
        assertTrue(report.isSuccess());
        assertEquals(2, report.getChildren().size());

        ProcessMigration.Report child = report.getChildren().get(0);
        assertEquals("Scenario: " + NAME_1_TO_2, child.getTitle());
        assertTrue(child.isSuccess());

        child = report.getChildren().get(1);
        assertEquals("Scenario: " + NAME_2_TO_3, child.getTitle());
        assertTrue(child.isSuccess());
    }
}
