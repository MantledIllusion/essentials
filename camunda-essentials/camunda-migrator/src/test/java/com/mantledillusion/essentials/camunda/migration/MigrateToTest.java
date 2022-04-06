package com.mantledillusion.essentials.camunda.migration;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ProcessEngineExtension.class)
public class MigrateToTest extends AbstractProcessMigrationTest {

    private ProcessEngine engine;

    @Override
    public ProcessEngine getEngine() {
        return engine;
    }

    @Test
    public void testMigrateToDefinitionId() {
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
    public void testMigrateToLatestDefinitionKey() {
        ProcessDefinition sourceDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV1);
        ProcessInstance instanceBefore = start(sourceDefinition.getId());
        ProcessDefinition targetDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV2);

        ProcessMigration
                .in(engine)
                .defineScenario()
                .whereDefinitionId(sourceDefinition.getId())
                .usingDefaultMappings()
                .toDefinitionKey(Processes.RelabelActivity.DEFINITION_KEY)
                .toLatestDefinitionVersion()
                .finalizeScenario()
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(targetDefinition.getId(), instanceAfter.getProcessDefinitionId());
    }

    @Test
    public void testMigrateToSpecificDefinitionKey() {
        ProcessDefinition sourceDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV1);
        ProcessInstance instanceBefore = start(sourceDefinition.getId());
        ProcessDefinition targetDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV2);

        ProcessMigration
                .in(engine)
                .defineScenario()
                .whereDefinitionId(sourceDefinition.getId())
                .usingDefaultMappings()
                .toDefinitionKey(Processes.RelabelActivity.DEFINITION_KEY)
                .toSpecificDefinitionVersion(targetDefinition.getVersion())
                .finalizeScenario()
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(targetDefinition.getId(), instanceAfter.getProcessDefinitionId());
    }

    @Test
    public void testMigrateToTaggedDefinitionKey() {
        ProcessDefinition sourceDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV1);
        ProcessInstance instanceBefore = start(sourceDefinition.getId());
        ProcessDefinition targetDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV2);

        ProcessMigration
                .in(engine)
                .defineScenario()
                .whereDefinitionId(sourceDefinition.getId())
                .usingDefaultMappings()
                .toDefinitionKey(Processes.RelabelActivity.DEFINITION_KEY)
                .toDefinitionTag(Processes.Common.VersionTags.REV2)
                .finalizeScenario()
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(targetDefinition.getId(), instanceAfter.getProcessDefinitionId());
    }
}
