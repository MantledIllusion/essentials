package com.mantledillusion.essentials.json.patch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.mantledillusion.essentials.json.patch.model.PatchOperation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OperationFulfilmentTest {

    private static final String DEF_FROM = "";
    private static final JsonNode DEF_VALUE = new TextNode("");

    @Test
    public void testFromEmptyValueEmpty() {
        Assertions.assertTrue(PatchOperation.remove.isFulfilledBy(null, null));
    }

    @Test
    public void testFromFulFilledValueEmpty() {
        Assertions.assertTrue(PatchOperation.copy.isFulfilledBy(DEF_FROM, null));
    }

    @Test
    public void testFromNotFulFilledValueEmpty() {
        Assertions.assertFalse(PatchOperation.copy.isFulfilledBy(null, null));
    }

    @Test
    public void testFromFulFilledValueUnrequired() {
        Assertions.assertFalse(PatchOperation.copy.isFulfilledBy(DEF_FROM, DEF_VALUE));
    }

    @Test
    public void testFromNotFulFilledValueUnrequired() {
        Assertions.assertFalse(PatchOperation.copy.isFulfilledBy(null, DEF_VALUE));
    }

    @Test
    public void testFromEmptyValueFulFilled() {
        Assertions.assertTrue(PatchOperation.add.isFulfilledBy(null, DEF_VALUE));
    }

    @Test
    public void testFromEmptyValueNotFulFilled() {
        Assertions.assertFalse(PatchOperation.add.isFulfilledBy(null, null));
    }

    @Test
    public void testFromUnrequiredValueFulFilled() {
        Assertions.assertFalse(PatchOperation.add.isFulfilledBy(DEF_FROM, DEF_VALUE));
    }

    @Test
    public void testFromUnrequiredValueNotFulFilled() {
        Assertions.assertFalse(PatchOperation.add.isFulfilledBy(DEF_FROM, null));
    }
}
