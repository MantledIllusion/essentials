package com.mantledillusion.essentials.camunda.migration;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ProcessEngineExtension.class)
public class ProcessMigrationTest {

    private ProcessEngine engine;

    @Test
    public void testMigrate() {
        String sourceDefinitionId = deploy("relabel-activity", "rev1");
        ProcessInstance instanceBefore = start(sourceDefinitionId);
        String targetDefinitionId = deploy("relabel-activity", "rev2");

        ProcessMigration
                .findProcesses(engine.getRepositoryService(), engine.getRuntimeService())
                .withDefinitionId(sourceDefinitionId)
                .usingDefaultMappings()
                .toDefinitionId(targetDefinitionId)
                .finalizeScenario()
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(targetDefinitionId, instanceAfter.getProcessDefinitionId());
    }

    private String deploy(String definitionKey, String versionTag) {
        return engine.getRepositoryService()
                .createDeployment()
                .addClasspathResource("processes/"+definitionKey+'_'+versionTag+".bpmn")
                .deployWithResult()
                .getDeployedProcessDefinitions()
                .iterator().next()
                .getId();
    }

    private ProcessInstance start(String definitionId) {
        return engine.getRuntimeService()
                .startProcessInstanceById(definitionId);
    }

    private ProcessInstance get(String instanceId) {
        return engine.getRuntimeService()
                .createProcessInstanceQuery()
                .processInstanceId(instanceId)
                .list()
                .iterator().next();
    }
}
