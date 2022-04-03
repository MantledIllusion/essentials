package com.mantledillusion.essentials.camunda.migration;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ProcessEngineExtension.class)
public class MigrateWithTest extends AbstractProcessMigrationTest {

    private ProcessEngine engine;

    @Override
    public ProcessEngine getEngine() {
        return engine;
    }

    @Test
    public void testMigrateWithDefinitionId() {
        ProcessDefinition sourceDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, REV1);
        ProcessInstance instanceBefore = start(sourceDefinition.getId());
        ProcessDefinition targetDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, REV2);

        ProcessMigration
                .defineScenario(engine)
                .withDefinitionId(sourceDefinition.getId())
                .usingDefaultMappings()
                .toDefinitionId(targetDefinition.getId())
                .finalizeScenario()
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(targetDefinition.getId(), instanceAfter.getProcessDefinitionId());
    }

    @Test
    public void testMigrateWithAnyDefinitionKey() {
        ProcessDefinition sourceDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, REV1);
        ProcessInstance instanceBefore = start(sourceDefinition.getId());
        ProcessDefinition targetDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, REV2);

        ProcessMigration
                .defineScenario(engine)
                .withDefinitionKey(Processes.RelabelActivity.DEFINITION_KEY)
                .usingDefaultMappings()
                .toDefinitionId(targetDefinition.getId())
                .finalizeScenario()
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(targetDefinition.getId(), instanceAfter.getProcessDefinitionId());
    }

    @Test
    public void testMigrateWithSpecificDefinitionKey() {
        ProcessDefinition sourceDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, REV1);
        ProcessInstance instanceBefore = start(sourceDefinition.getId());
        ProcessDefinition targetDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, REV2);

        ProcessMigration
                .defineScenario(engine)
                .withDefinitionKey(Processes.RelabelActivity.DEFINITION_KEY)
                .withVersion(sourceDefinition.getVersion())
                .usingDefaultMappings()
                .toDefinitionId(targetDefinition.getId())
                .finalizeScenario()
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(targetDefinition.getId(), instanceAfter.getProcessDefinitionId());
    }

    @Test
    public void testMigrateWithTaggedDefinitionKey() {
        ProcessDefinition sourceDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, REV1);
        ProcessInstance instanceBefore = start(sourceDefinition.getId());
        ProcessDefinition targetDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, REV2);

        ProcessMigration
                .defineScenario(engine)
                .withDefinitionKey(Processes.RelabelActivity.DEFINITION_KEY)
                .withVersionTag(REV1)
                .usingDefaultMappings()
                .toDefinitionId(targetDefinition.getId())
                .finalizeScenario()
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(targetDefinition.getId(), instanceAfter.getProcessDefinitionId());
    }

    @Test
    public void testMigrateWithActivity() {
        ProcessDefinition sourceDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, REV1);
        ProcessInstance instanceBefore = start(sourceDefinition.getId());
        ProcessDefinition targetDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, REV2);

        ProcessMigration
                .defineScenario(engine)
                .withActivity(Processes.RelabelActivity.Activities.RELABELED_ACTIVITY)
                .usingDefaultMappings()
                .toDefinitionId(targetDefinition.getId())
                .finalizeScenario()
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(targetDefinition.getId(), instanceAfter.getProcessDefinitionId());
    }

}
