package com.mantledillusion.essentials.json.patch.model;

import com.fasterxml.jackson.databind.JsonNode;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Represents the possible JSON Patch operations of RFC 6902.
 */
@XmlType
@XmlEnum
public enum PatchOperation {

    add(false, true),
    remove(false, false),
    replace(false, true),
    copy(true, false),
    move(true, false),
    test(false, true);

    private final boolean requiresFrom;
    private final boolean requiresValue;

    PatchOperation(boolean requiresFrom, boolean requiresValue) {
        this.requiresFrom = requiresFrom;
        this.requiresValue = requiresValue;
    }

    /**
     * Returns whether this operation requires a 'from' field in the corresponding patch to exist.
     *
     * @return True if a 'from' field has to exist in the patch when this operation is performed, false otherwise
     */
    public boolean isRequiresFrom() {
        return requiresFrom;
    }

    /**
     * Returns whether this operation requires a 'value' field in the corresponding patch to exist.
     *
     * @return True if a 'value' field has to exist in the patch when this operation is performed, false otherwise
     */
    public boolean isRequiresValue() {
        return requiresValue;
    }

    /**
     * Returns whether the given values from a certain {@link Patch} fulfill the requirements for the 'from'/'value'
     * field of this operation.
     *
     * @param from  The {@link String} value of the {@link Patch}es' 'from' field; might be null.
     * @param value The {@link JsonNode} value of the {@link Patch}es' 'value' field; might be null.
     * @return True if the given values this operation's requirements, false otherwise
     */
    public boolean isFulfilledBy(String from, JsonNode value) {
        return ((from != null) == this.requiresFrom) && ((value != null) == this.requiresValue);
    }
}