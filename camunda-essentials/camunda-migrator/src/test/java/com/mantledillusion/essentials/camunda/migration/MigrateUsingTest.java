package com.mantledillusion.essentials.camunda.migration;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ProcessEngineExtension.class)
public class MigrateUsingTest extends AbstractProcessMigrationTest {

    private ProcessEngine engine;

    @Override
    public ProcessEngine getEngine() {
        return engine;
    }

    @Test
    public void testMigrateUsingDefault() {
        ProcessDefinition sourceDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV1);
        ProcessInstance instanceBefore = start(sourceDefinition.getId());
        ProcessDefinition targetDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV2);

        ProcessMigration
                .in(engine)
                .defineScenario()
                .withDefinitionId(sourceDefinition.getId())
                .usingDefaultMappings()
                .toDefinitionId(targetDefinition.getId())
                .finalizeScenario()
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(targetDefinition.getId(), instanceAfter.getProcessDefinitionId());
    }

    @Test
    public void testMigrateUsingSpecific() {
        ProcessDefinition sourceDefinition = deploy(Processes.RenamedActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV1);
        ProcessInstance instanceBefore = start(sourceDefinition.getId());
        ProcessDefinition targetDefinition = deploy(Processes.RenamedActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV2);

        ProcessMigration
                .in(engine)
                .defineScenario()
                .withDefinitionId(sourceDefinition.getId())
                .usingMapping(Processes.RenamedActivity.Activities.RENAMED_ACTIVITY_BEFORE, Processes.RenamedActivity.Activities.RENAMED_ACTIVITY_AFTER)
                .toDefinitionId(targetDefinition.getId())
                .finalizeScenario()
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(targetDefinition.getId(), instanceAfter.getProcessDefinitionId());
    }
}
