package com.mantledillusion.essentials.json.patch.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.mantledillusion.essentials.json.patch.validation.ValidPatch;
import com.mantledillusion.essentials.json.patch.validation.ValidPath;
import com.mantledillusion.essentials.json.patch.validation.PatchDetailValidationGroup;

import javax.validation.GroupSequence;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/**
 * Generic object that represents a RFC 6902 compliant JSON Patch.
 */
@GroupSequence({PatchDetailValidationGroup.class, Patch.class})
@ValidPatch
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Patch {

    @NotNull(groups = PatchDetailValidationGroup.class)
    @XmlElement(required = true, nillable = false)
    private PatchOperation op;

    @ValidPath(groups = PatchDetailValidationGroup.class)
    @XmlElement(required = true, nillable = true)
    private String from;

    @NotNull(groups = PatchDetailValidationGroup.class)
    @ValidPath(groups = PatchDetailValidationGroup.class)
    @XmlElement(required = true, nillable = false)
    private String path;

    @XmlElement(required = true, nillable = true, type = String.class)
    private JsonNode value;

    /**
     * Default Constructor.
     */
    public Patch() {
    }

    /**
     * Advanced Constructor.
     *
     * @param op The operation type, might <b>not</b> be null.
     * @param path The node path; might <b>not</b> be null.
     */
    public Patch(PatchOperation op, String path) {
        this.op = op;
        this.path = path;
    }

    /**
     * Advanced Constructor.
     *
     * @param op The operation type, might <b>not</b> be null.
     * @param path The node path; might <b>not</b> be null.
     * @param value The value; might be null.
     */
    public Patch(PatchOperation op, String path, JsonNode value) {
        this.op = op;
        this.path = path;
        this.value = value;
    }

    /**
     * Advanced Constructor.
     *
     * @param op The operation type, might <b>not</b> be null.
     * @param from The from node path; might be null.
     * @param path The node path; might <b>not</b> be null.
     */
    public Patch(PatchOperation op, String from, String path) {
        this.op = op;
        this.from = from;
        this.path = path;
    }

    /**
     * Returns the operation type.
     * <p>
     * According to the specification it has to be one of<br>
     * - {@link PatchOperation#add}<br>
     * - {@link PatchOperation#remove}<br>
     * - {@link PatchOperation#replace}<br>
     * - {@link PatchOperation#copy}<br>
     * - {@link PatchOperation#move}<br>
     * - {@link PatchOperation#test}<br>
     *
     * @return The operation type, never null
     */
    public PatchOperation getOp() {
        return op;
    }

    /**
     * Sets the operation type.
     *
     * @param op The operation type, might <b>not</b> be null.
     */
    public void setOp(PatchOperation op) {
        this.op = op;
    }

    /**
     * Returns the path to another JSON node, only required for...<br>
     * - {@link PatchOperation#copy}<br>
     * - {@link PatchOperation#move}<br>
     * ...operations.
     * <p>
     * Begins with a slash and adds numeric sections for list indices, for example: '/subobject/list/2/field'
     *
     * @return The from node path, might be null
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets the path to a JSON node.
     *
     * @param from The from node path; might be null.
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Returns the path to the JSON node to patch.
     * <p>
     * Begins with a slash and adds numeric sections for list indices, for example: '/subobject/list/2/field'
     *
     * @return The node path, never null
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path to the JSON node to patch.
     *
     * @param path The node path; might <b>not</b> be null.
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Returns the value for the operation, only required for...<br>
     * - {@link PatchOperation#add}<br>
     * - {@link PatchOperation#replace}<br>
     * - {@link PatchOperation#test}<br>
     * ...operations.
     * <p>
     * A value can be everything that might be suitable for the node that is patched at the given path; a string, a
     * number or even a whole JSON object.
     *
     * @return The value, might be null
     */
    public JsonNode getValue() {
        return value;
    }

    /**
     * Returns the value for the operation.
     *
     * @param value The value; might be null.
     */
    public void setValue(JsonNode value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Patch patch = (Patch) o;
        return Objects.equals(op, patch.op) &&
                Objects.equals(path, patch.path) &&
                Objects.equals(value, patch.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(op, path, value);
    }

    @Override
    public String toString() {
        return "{op:" + this.op + ", path:" + path + (this.value == null ? "" : ", value: '" + this.value + "'}");
    }
}