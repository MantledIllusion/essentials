package com.mantledillusion.essentials.camunda.migration;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.junit5.ProcessEngineExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ProcessEngineExtension.class)
public class MigrateScenarioTest extends AbstractProcessMigrationTest {

    private ProcessEngine engine;

    @Override
    public ProcessEngine getEngine() {
        return engine;
    }

    @Test
    public void testMigrateScenarios() {
        ProcessDefinition sourceDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV1);
        ProcessInstance instanceBefore = start(sourceDefinition.getId());
        deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV2);
        ProcessDefinition targetDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV3);

        ProcessMigration
                .in(engine)
                .defineScenario()
                .whereDefinitionKey(Processes.RelabelActivity.DEFINITION_KEY)
                .toDefinitionKey(Processes.RelabelActivity.DEFINITION_KEY)
                .usingDefaultMappings()
                .defineScenarios()
                    .defineScenario()
                        .whereVersionTag(Processes.Common.VersionTags.REV1)
                        .toVersionTag(Processes.Common.VersionTags.REV2)
                        .finalizeScenario()
                    .defineScenario()
                        .whereVersionTag(Processes.Common.VersionTags.REV2)
                        .toVersionTag(Processes.Common.VersionTags.REV3)
                        .finalizeScenario()
                    .finalizeScenarios()
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(targetDefinition.getId(), instanceAfter.getProcessDefinitionId());
    }
}
