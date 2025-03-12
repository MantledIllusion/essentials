package com.mantledillusion.essentials.camunda.migration;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.junit5.ProcessEngineExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ProcessEngineExtension.class)
public class MigrateFailureTest extends AbstractProcessMigrationTest {

    private ProcessEngine engine;

    @Override
    public ProcessEngine getEngine() {
        return engine;
    }

    @Test
    public void testMigrateSuspendingInstances() {
        ProcessDefinition sourceDefinition = deploy(Processes.RenamedActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV1);
        ProcessInstance instanceBefore = start(sourceDefinition.getId());
        ProcessDefinition targetDefinition = deploy(Processes.RenamedActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV2);

        ProcessMigration
                .in(engine)
                .whereDefinitionId(sourceDefinition.getId())
                .onFailureSuspend(true)
                .toDefinitionId(targetDefinition.getId())
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(sourceDefinition.getId(), instanceAfter.getProcessDefinitionId());
        assertTrue(instanceAfter.isSuspended());
    }

    @Test
    public void testMigrateSkippingInstances() {
        ProcessDefinition renamedSourceDefinition = deploy(Processes.RenamedActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV2);
        ProcessInstance renamedInstanceBefore = start(renamedSourceDefinition.getId());
        ProcessDefinition relabeledSourceDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV2);
        ProcessInstance relabeledInstanceBefore = start(relabeledSourceDefinition.getId());
        ProcessDefinition targetDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV3);

        ProcessMigration
                .in(engine)
                .whereVersionTag(Processes.Common.VersionTags.REV2)
                .usingDefaultMappings()
                .onFailureSkip(true)
                .toDefinitionId(targetDefinition.getId())
                .migrate();

        ProcessInstance renamedInstanceAfter = get(renamedInstanceBefore.getId());
        assertEquals(renamedSourceDefinition.getId(), renamedInstanceAfter.getProcessDefinitionId());

        ProcessInstance relabeledInstanceAfter = get(relabeledInstanceBefore.getId());
        assertEquals(relabeledSourceDefinition.getId(), relabeledInstanceAfter.getProcessDefinitionId());
    }

    @Test
    public void testMigrateSkippingScenarios() {
        ProcessDefinition renamedSourceDefinition = deploy(Processes.RenamedActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV1);
        ProcessInstance renamedInstanceBefore = start(renamedSourceDefinition.getId());
        ProcessDefinition relabeledSourceDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV1);
        ProcessInstance relabeledInstanceBefore = start(relabeledSourceDefinition.getId());
        ProcessDefinition targetDefinition = deploy(Processes.RelabelActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV3);

        ProcessMigration
                .in(engine)
                .usingDefaultMappings()
                .onFailureSkip(true)
                .toDefinitionId(targetDefinition.getId())
                .defineScenario(renamedScenario -> renamedScenario
                        .whereDefinitionId(renamedSourceDefinition.getId()))
                .defineScenario(relabeledScenario -> relabeledScenario
                        .whereDefinitionId(relabeledSourceDefinition.getId()))
                .migrate();

        ProcessInstance renamedInstanceAfter = get(renamedInstanceBefore.getId());
        assertEquals(renamedSourceDefinition.getId(), renamedInstanceAfter.getProcessDefinitionId());

        ProcessInstance relabeledInstanceAfter = get(relabeledInstanceBefore.getId());
        assertEquals(relabeledSourceDefinition.getId(), relabeledInstanceAfter.getProcessDefinitionId());
    }
}
