package com.mantledillusion.essentials.json.patch;

import com.fasterxml.jackson.databind.JsonNode;

public interface TestConstants {

    String ID_ABC = "abc";
    String ID_DEF = "def";
    String PATH_FIRST = "/first";
    String PATH_SECOND = "/second";
    String PATH_UNPATCHABLE = "/unpatchable";
    String PATH_FIRST_ID = "/first/id";
    String PATH_UNPATCHABLE_ID = "/unpatchable/id";
    String PATH_LISTED = "/listed";

    default JsonNode asNode(Object o) {
        return PatchUtil.asIgnoredNode(o);
    }
}
