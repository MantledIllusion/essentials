package com.mantledillusion.essentials.json.patch;

import com.mantledillusion.essentials.json.patch.model.Patch;
import com.mantledillusion.essentials.json.patch.model.PatchOperation;
import com.mantledillusion.essentials.json.patch.testclasses.ListedPojo;
import com.mantledillusion.essentials.json.patch.testclasses.RootPojo;
import com.mantledillusion.essentials.json.patch.testclasses.SubPojo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class RecordPatchTest implements TestConstants {

    @Test
    public void testRecordNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> PatchUtil.record(null));
    }

    @Test
    public void testSetValue() {
        RootPojo pojo = new RootPojo();

        PatchRecorder recorder = PatchUtil.record(pojo);

        SubPojo first = new SubPojo();
        pojo.setFirst(first);

        List<Patch> ops = recorder.capture();

        check(ops,
                new Patch(PatchOperation.add, PATH_FIRST, toNode(first)));
    }

    @Test
    public void testExchangeValue() {
        RootPojo pojo = new RootPojo();
        pojo.setFirst(new SubPojo());
        pojo.getFirst().setId(ID_ABC);

        PatchRecorder recorder = PatchUtil.record(pojo);

        pojo.getFirst().setId(ID_DEF);

        List<Patch> ops = recorder.capture();

        check(ops,
                new Patch(PatchOperation.replace, PATH_FIRST_ID, toNode(ID_DEF)));
    }

    @Test
    public void testMoveValue() {
        RootPojo pojo = new RootPojo();
        SubPojo sub = new SubPojo();
        sub.setId(ID_ABC);
        pojo.setFirst(sub);

        PatchRecorder recorder = PatchUtil.record(pojo);

        pojo.setFirst(null);
        pojo.setSecond(sub);

        List<Patch> ops = recorder.capture();

        check(ops,
                new Patch(PatchOperation.move, PATH_FIRST, PATH_SECOND));
    }

    @Test
    public void testDeleteValue() {
        RootPojo pojo = new RootPojo();
        pojo.setFirst(new SubPojo());
        pojo.getFirst().setId(ID_ABC);

        PatchRecorder recorder = PatchUtil.record(pojo);

        pojo.getFirst().setId(null);

        List<Patch> ops = recorder.capture();

        check(ops,
                new Patch(PatchOperation.remove, PATH_FIRST_ID));
    }

    @Test
    public void testAddValue() {
        RootPojo pojo = new RootPojo();

        PatchRecorder recorder = PatchUtil.record(pojo);

        ListedPojo listed = new ListedPojo();
        listed.setId(ID_ABC);
        pojo.getListed().add(listed);

        List<Patch> ops = recorder.capture();

        check(ops,
                new Patch(PatchOperation.add, PATH_LISTED+"/0", toNode(listed)));
    }

    @Test
    public void testRemoveValue() {
        RootPojo pojo = new RootPojo();
        ListedPojo listed = new ListedPojo();
        listed.setId(ID_ABC);
        pojo.getListed().add(listed);

        PatchRecorder recorder = PatchUtil.record(pojo);

        pojo.getListed().remove(0);

        List<Patch> ops = recorder.capture();

        check(ops,
                new Patch(PatchOperation.remove, PATH_LISTED+"/0"));
    }

    @Test
    public void testCopyValue() {
        RootPojo pojo = new RootPojo();
        ListedPojo listed = new ListedPojo();
        listed.setId(ID_ABC);
        pojo.getListed().add(listed);

        PatchRecorder recorder = PatchUtil.record(pojo);

        pojo.getListed().add(listed);

        List<Patch> ops = recorder.capture();

        check(ops,
                new Patch(PatchOperation.copy, PATH_LISTED+"/0", PATH_LISTED+"/1"));
    }

    private void check(List<Patch> ops, Patch... expected) {
        Assertions.assertEquals(Arrays.asList(expected), ops);
    }
}
