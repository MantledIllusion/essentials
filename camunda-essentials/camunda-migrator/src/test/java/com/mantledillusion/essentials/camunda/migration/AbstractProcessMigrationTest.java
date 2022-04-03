package com.mantledillusion.essentials.camunda.migration;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractProcessMigrationTest {

    public interface Processes {

        interface Unaffected {

            String DEFINITION_KEY = "unaffected";
        }

        interface RelabelActivity {

            String DEFINITION_KEY = "relabel-activity";

            interface Activities {

                String RELABELED_ACTIVITY = "relabeled_activity";
            }
        }
    }

    public static final String REV1 = "rev1";
    public static final String REV2 = "rev2";

    protected abstract ProcessEngine getEngine();
    private String unaffectedDefinitionId;
    private String unaffectedInstanceId;

    @BeforeEach
    public void before() {
        getEngine().getRepositoryService().createDeploymentQuery().list()
                .forEach(deployment -> getEngine().getRepositoryService().deleteDeployment(deployment.getId(), true));

        unaffectedDefinitionId = deploy(Processes.Unaffected.DEFINITION_KEY, REV1).getId();
        unaffectedInstanceId = start(unaffectedDefinitionId).getId();
    }

    @AfterEach
    public void after() {
        ProcessInstance unaffected = get(unaffectedInstanceId);
        assertEquals(unaffectedDefinitionId, unaffected.getProcessDefinitionId());
    }

    protected ProcessDefinition deploy(String definitionKey, String versionTag) {
        return getEngine().getRepositoryService()
                .createDeployment()
                .addClasspathResource("processes/"+definitionKey+'_'+versionTag+".bpmn")
                .deployWithResult()
                .getDeployedProcessDefinitions()
                .iterator().next();
    }

    protected ProcessInstance start(String definitionId) {
        return getEngine().getRuntimeService()
                .startProcessInstanceById(definitionId);
    }

    protected ProcessInstance get(String instanceId) {
        return getEngine().getRuntimeService()
                .createProcessInstanceQuery()
                .processInstanceId(instanceId)
                .list()
                .iterator().next();
    }
}
