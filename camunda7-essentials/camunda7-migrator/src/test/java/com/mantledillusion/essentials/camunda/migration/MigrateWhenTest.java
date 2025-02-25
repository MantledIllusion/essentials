package com.mantledillusion.essentials.camunda.migration;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.junit5.ProcessEngineExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ProcessEngineExtension.class)
public class MigrateWhenTest extends AbstractProcessMigrationTest {

    private ProcessEngine engine;

    @Override
    public ProcessEngine getEngine() {
        return engine;
    }

    @Test
    public void testMigrateWhen() {
        ProcessDefinition sourceDefinition = deploy(Processes.SplitActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV1);
        ProcessInstance instanceBefore = start(sourceDefinition.getId(), Collections.singletonMap(Processes.Common.Variables.INT, 1));
        ProcessDefinition targetDefinition = deploy(Processes.SplitActivity.DEFINITION_KEY, Processes.Common.VersionTags.REV2);

        ProcessMigration
                .in(engine)
                .defineScenario()
                .whereDefinitionId(sourceDefinition.getId())
                .usingMapping(Processes.SplitActivity.Activities.DELETED_ACTIVITY, Processes.SplitActivity.Activities.FOO)
                .when()
                    .whereVariableEquals(Processes.Common.Variables.INT, 1)
                    .beforeMigrate()
                        .removeVariable(Processes.Common.Variables.INT)
                    .afterMigrate()
                        .setVariable(Processes.Common.Variables.STRING, "BAR")
                        .modify()
                            .cancelAllForActivity(Processes.SplitActivity.Activities.FOO)
                            .startBeforeActivity(Processes.SplitActivity.Activities.BAR)
                            .then()
                    .then()
                .usingDefaultMappings()
                .toDefinitionId(targetDefinition.getId())
                .finalizeScenario()
                .migrate();

        ProcessInstance instanceAfter = get(instanceBefore.getId());

        assertEquals(targetDefinition.getId(), instanceAfter.getProcessDefinitionId());

        List<String> activityIds = this.engine.getRuntimeService().getActiveActivityIds(instanceAfter.getId());
        assertEquals(1, activityIds.size());
        assertEquals(Processes.SplitActivity.Activities.BAR, activityIds.iterator().next());

        assertNull(this.engine.getRuntimeService().getVariable(instanceAfter.getId(), Processes.Common.Variables.INT));
        assertEquals("BAR", this.engine.getRuntimeService().getVariable(instanceAfter.getId(), Processes.Common.Variables.STRING));
    }
}
