package com.mantledillusion.essentials.camunda.migration;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ProcessEngineExtension.class)
public class MigrateWhereTest extends AbstractProcessMigrationTest {

    private ProcessEngine engine;

    @Override
    public ProcessEngine getEngine() {
        return engine;
    }

    @Test
    public void testMigrateWhereDefinitionId() {
        ProcessDefinition sourceDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV1);
        ProcessInstance instanceBefore = start(sourceDefinition.getId());
        ProcessDefinition targetDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV2);

        ProcessMigration
                .in(engine)
                .defineScenario()
                .whereDefinitionId(sourceDefinition.getId())
                .usingDefaultMappings()
                .toDefinitionId(targetDefinition.getId())
                .finalizeScenario()
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(targetDefinition.getId(), instanceAfter.getProcessDefinitionId());
    }

    @Test
    public void testMigrateWhereAnyDefinitionKey() {
        ProcessDefinition sourceDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV1);
        ProcessInstance instanceBefore = start(sourceDefinition.getId());
        ProcessDefinition targetDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV2);

        ProcessMigration
                .in(engine)
                .defineScenario()
                .whereDefinitionKey(Processes.RelabelActivity.DEFINITION_KEY)
                .usingDefaultMappings()
                .toDefinitionId(targetDefinition.getId())
                .finalizeScenario()
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(targetDefinition.getId(), instanceAfter.getProcessDefinitionId());
    }

    @Test
    public void testMigrateWhereSpecificDefinitionKey() {
        ProcessDefinition sourceDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV1);
        ProcessInstance instanceBefore = start(sourceDefinition.getId());
        ProcessDefinition targetDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV2);

        ProcessMigration
                .in(engine)
                .defineScenario()
                .whereDefinitionKey(Processes.RelabelActivity.DEFINITION_KEY)
                .whereVersion(sourceDefinition.getVersion())
                .usingDefaultMappings()
                .toDefinitionId(targetDefinition.getId())
                .finalizeScenario()
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(targetDefinition.getId(), instanceAfter.getProcessDefinitionId());
    }

    @Test
    public void testMigrateWhereTaggedDefinitionKey() {
        ProcessDefinition sourceDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV1);
        ProcessInstance instanceBefore = start(sourceDefinition.getId());
        ProcessDefinition targetDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV2);

        ProcessMigration
                .in(engine)
                .defineScenario()
                .whereDefinitionKey(Processes.RelabelActivity.DEFINITION_KEY)
                .whereVersionTag(Processes.Common.VersionTags.REV1)
                .usingDefaultMappings()
                .toDefinitionId(targetDefinition.getId())
                .finalizeScenario()
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(targetDefinition.getId(), instanceAfter.getProcessDefinitionId());
    }

    @Test
    public void testMigrateWhereActivity() {
        ProcessDefinition sourceDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV1);
        ProcessInstance instanceBefore = start(sourceDefinition.getId());
        ProcessDefinition targetDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV2);

        ProcessMigration
                .in(engine)
                .defineScenario()
                .whereActivity(Processes.RelabelActivity.Activities.RELABELED_ACTIVITY)
                .usingDefaultMappings()
                .toDefinitionId(targetDefinition.getId())
                .finalizeScenario()
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(targetDefinition.getId(), instanceAfter.getProcessDefinitionId());
    }

}
