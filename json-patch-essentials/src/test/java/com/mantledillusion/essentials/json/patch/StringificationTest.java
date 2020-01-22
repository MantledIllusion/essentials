package com.mantledillusion.essentials.json.patch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.mantledillusion.essentials.json.patch.model.Patch;
import com.mantledillusion.essentials.json.patch.model.PatchOperation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringificationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void testAddStringification() {
        JsonNode node = OBJECT_MAPPER.createObjectNode().put("name", "Ginger Nut");
        Assertions.assertEquals("{ \"op\": \"add\", \"path\": \"/biscuits/1\", \"value\": {\"name\":\"Ginger Nut\"} }",
                new Patch(PatchOperation.add, "/biscuits/1", node).toString());
    }

    @Test
    public void testRemoveStringification() {
        Assertions.assertEquals("{ \"op\": \"remove\", \"path\": \"/biscuits/1\" }",
                new Patch(PatchOperation.remove, "/biscuits/1").toString());
    }

    @Test
    public void testReplaceStringification() {
        JsonNode node = new TextNode("Chocolate Digestive");
        Assertions.assertEquals("{ \"op\": \"replace\", \"path\": \"/biscuits/0/name\", \"value\": \"Chocolate Digestive\" }",
                new Patch(PatchOperation.replace, "/biscuits/0/name", node).toString());
    }

    @Test
    public void testCopyStringification() {
        Assertions.assertEquals("{ \"op\": \"copy\", \"from\": \"/biscuits/0\", \"path\": \"/best_biscuit\" }",
                new Patch(PatchOperation.copy, "/biscuits/0", "/best_biscuit").toString());
    }

    @Test
    public void testMoveStringification() {
        Assertions.assertEquals("{ \"op\": \"move\", \"from\": \"/biscuits\", \"path\": \"/cookies\" }",
                new Patch(PatchOperation.move, "/biscuits", "/cookies").toString());
    }

    @Test
    public void testTestStringification() {
        JsonNode node = new TextNode("Choco Chrunch");
        Assertions.assertEquals("{ \"op\": \"test\", \"path\": \"/best_biscuit/name\", \"value\": \"Choco Chrunch\" }",
                new Patch(PatchOperation.test, "/best_biscuit/name", node).toString());
    }
}
