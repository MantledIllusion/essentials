package com.mantledillusion.essentials.json.patch;

import com.mantledillusion.essentials.json.patch.model.Patch;
import com.mantledillusion.essentials.json.patch.model.PatchOperation;
import com.mantledillusion.essentials.json.patch.testclasses.ListedPojo;
import com.mantledillusion.essentials.json.patch.testclasses.RootPojo;
import com.mantledillusion.essentials.json.patch.testclasses.SubPojo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class ApplyPatchTest implements TestConstants {

    @Test
    public void testApplyOnNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> PatchUtil.apply(null, Collections.emptyList()));
    }

    @Test
    public void testSetValue() {
        RootPojo pojo = new RootPojo();

        SubPojo first = new SubPojo();
        first.setId(ID_ABC);

        pojo = PatchUtil.apply(pojo, new Patch(PatchOperation.add, PATH_FIRST, toNode(first)));

        Assertions.assertEquals(first, pojo.getFirst());
    }

    @Test
    public void testExchangeValue() {
        RootPojo pojo = new RootPojo();
        pojo.setFirst(new SubPojo());
        pojo.getFirst().setId(ID_ABC);

        pojo = PatchUtil.apply(pojo, new Patch(PatchOperation.replace, PATH_FIRST_ID, toNode(ID_DEF)));

        Assertions.assertEquals(ID_DEF, pojo.getFirst().getId());
    }

    @Test
    public void testMoveValue() {
        RootPojo pojo = new RootPojo();
        SubPojo first = new SubPojo();
        first.setId(ID_ABC);
        pojo.setFirst(first);

        pojo = PatchUtil.apply(pojo, new Patch(PatchOperation.move, PATH_FIRST, PATH_SECOND));

        Assertions.assertNull(pojo.getFirst());
        Assertions.assertEquals(first, pojo.getSecond());
    }

    @Test
    public void testDeleteValue() {
        RootPojo pojo = new RootPojo();
        pojo.setFirst(new SubPojo());
        pojo.getFirst().setId(ID_ABC);

        pojo = PatchUtil.apply(pojo, new Patch(PatchOperation.remove, PATH_FIRST_ID));

        Assertions.assertNull(pojo.getFirst().getId());
    }

    @Test
    public void testAddValue() {
        RootPojo pojo = new RootPojo();

        ListedPojo listed = new ListedPojo();
        listed.setId(ID_ABC);

        pojo = PatchUtil.apply(pojo, new Patch(PatchOperation.add, PATH_LISTED+"/0", toNode(listed)));

        Assertions.assertEquals(1, pojo.getListed().size());
        Assertions.assertEquals(listed, pojo.getListed().get(0));
    }

    @Test
    public void testRemoveValue() {
        RootPojo pojo = new RootPojo();
        ListedPojo listed = new ListedPojo();
        listed.setId(ID_ABC);
        pojo.getListed().add(listed);

        pojo = PatchUtil.apply(pojo, new Patch(PatchOperation.remove, PATH_LISTED+"/0"));

        Assertions.assertEquals(0, pojo.getListed().size());
    }

    @Test
    public void testCopyValue() {
        RootPojo pojo = new RootPojo();
        ListedPojo listed = new ListedPojo();
        listed.setId(ID_ABC);
        pojo.getListed().add(listed);

        pojo = PatchUtil.apply(pojo, new Patch(PatchOperation.copy, PATH_LISTED+"/0", PATH_LISTED+"/1"));

        Assertions.assertEquals(2, pojo.getListed().size());
        Assertions.assertEquals(listed, pojo.getListed().get(1));
    }
}