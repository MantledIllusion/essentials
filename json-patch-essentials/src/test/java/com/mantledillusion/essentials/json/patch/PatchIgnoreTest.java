package com.mantledillusion.essentials.json.patch;

import com.flipkart.zjsonpatch.JsonPatchApplicationException;
import com.mantledillusion.essentials.json.patch.model.Patch;
import com.mantledillusion.essentials.json.patch.model.PatchOperation;
import com.mantledillusion.essentials.json.patch.testclasses.RootPojo;
import com.mantledillusion.essentials.json.patch.testclasses.SubPojo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PatchIgnoreTest implements TestConstants {

    @Test
    public void testNoPatchFieldRecording() {
        RootPojo pojo = new RootPojo();

        PatchRecorder recorder = PatchUtil.record(pojo);

        pojo.setUnpatchable(new SubPojo());

        List<Patch> ops = recorder.capture();
        Assertions.assertTrue(ops.isEmpty());
    }

    @Test
    public void testNoPatchSubFieldRecording() {
        RootPojo pojo = new RootPojo();
        pojo.setUnpatchable(new SubPojo());

        PatchRecorder recorder = PatchUtil.record(pojo);

        pojo.getUnpatchable().setId(ID_ABC);

        List<Patch> ops = recorder.capture();
        Assertions.assertTrue(ops.isEmpty());
    }

    @Test
    public void testNoPatchFieldApplying() {
        RootPojo pojo = new RootPojo();

        SubPojo unpatchable = new SubPojo();
        unpatchable.setId(ID_ABC);

        pojo = PatchUtil.apply(pojo, new Patch(PatchOperation.add, PATH_UNPATCHABLE, toNode(unpatchable)));

        Assertions.assertNull(pojo.getUnpatchable());
    }

    @Test
    public void testNoPatchSubFieldApplying() {
        RootPojo pojo = new RootPojo();
        pojo.setUnpatchable(new SubPojo());

        Assertions.assertThrows(JsonPatchApplicationException.class,
                () -> PatchUtil.apply(pojo, new Patch(PatchOperation.add, PATH_UNPATCHABLE_ID, toNode(ID_ABC))));
    }
}
