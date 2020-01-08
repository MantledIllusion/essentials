package com.mantledillusion.essentials.json.patch;

import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.mantledillusion.essentials.json.patch.model.Patch;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Recorder for changes to a specific Java {@link Object} that can be expressed as a JSON Patch.
 * <p>
 * A {@link PatchRecorder} is always stateful; it is initialized with a pre patch representation of the
 * {@link Object} to record changes of.
 * <p>
 * The current changes to the pre patched {@link Object} can be retrieved any time by the methods:<br>
 * - {@link #peek()}: Returns the changes, but leaved the recorder's perception of what "pre patch" is unchanged<br>
 * - {@link #capture()}: Returns the changes and sets the observed {@link Object}'s current state as the recorder's
 * new perception of what is "pre patch"
 */
public class PatchRecorder {

    private final Object patchable;
    private JsonNode prePatch;

    PatchRecorder(Object patchable) {
        this.patchable = patchable;
        this.prePatch = PatchUtil.MAPPER.valueToTree(patchable);
    }

    /**
     * Returns a {@link List} of {@link Patch}es of what has changed on the observed {@link Object}.
     * <p>
     * Leaves this recorder's perception of the {@link Object}'s pre patch state unchanged. As a result, subsequent
     * call to either {@link #peek()} or {@link #capture()} without further changes will return the same result.
     *
     * @return A {@link List} of {@link Patch}es that describe the changes performed on the recorder's observed
     * {@link Object}, never null but might be empty
     */
    public List<Patch> peek() {
        return getPatchOperations(false);
    }

    /**
     * Returns a {@link List} of {@link Patch}es of what has changed on the observed {@link Object}.
     * <p>
     * Changes this recorder's perception of the {@link Object}'s pre patch state to its current state. As a result,
     * subsequent call to either {@link #peek()} or {@link #capture()} without further changes will return no changes.
     *
     * @return A {@link List} of {@link Patch}es that describe the changes performed on the recorder's observed
     * {@link Object}, never null but might be empty
     */
    public List<Patch> capture() {
        return getPatchOperations(true);
    }

    private synchronized List<Patch> getPatchOperations(boolean keepCurrentStateAsPrePatch) {
        try {
            JsonNode postPatch = PatchUtil.MAPPER.valueToTree(patchable);
            JsonNode diff = JsonDiff.asJson(this.prePatch, postPatch);
            String jsonDiff = PatchUtil.MAPPER.writeValueAsString(diff);
            Patch[] ops = PatchUtil.MAPPER.readValue(jsonDiff, Patch[].class);

            if (keepCurrentStateAsPrePatch) {
                this.prePatch = postPatch;
            }

            return Arrays.asList(ops);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create patch ", e);
        }
    }
}