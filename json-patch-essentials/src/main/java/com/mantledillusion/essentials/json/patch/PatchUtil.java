package com.mantledillusion.essentials.json.patch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import com.flipkart.zjsonpatch.JsonPatchApplicationException;
import com.mantledillusion.essentials.json.patch.ignore.PatchIgnoreIntrospector;
import com.mantledillusion.essentials.json.patch.ignore.NoPatch;
import com.mantledillusion.essentials.json.patch.model.Patch;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Static util that is able to {@link #take(Object)}/{@link #apply(Object, List)} RFC 6902 compliant JSON patches
 * from/to Java {@link Object}s.
 * <p>
 * Use the {@link #take(Object)} method to create a pre patch {@link Snapshot} of an {@link Object}, make arbitrary
 * changes to that {@link Object}, and then call {@link Snapshot#peek()} or {@link Snapshot#capture()} to create
 * {@link Patch}es of the changes that happened in between of the {@link Object}'s two states.
 */
public class PatchUtil {

    private static final ObjectMapper STANDARD_MAPPER;
    private static final ObjectMapper IGNORING_MAPPER;

    static {
        STANDARD_MAPPER = new ObjectMapper();
        STANDARD_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        STANDARD_MAPPER.registerModule(new JavaTimeModule());

        IGNORING_MAPPER = new ObjectMapper();
        IGNORING_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        IGNORING_MAPPER.setAnnotationIntrospector(new PatchIgnoreIntrospector());
        IGNORING_MAPPER.registerModule(new JavaTimeModule());
    }

    /**
     * {@link Snapshot} of a Java {@link Object}'s state before it receives changes that can be expressed as a JSON patch.
     * <p>
     * A {@link Snapshot} is always stateful; it is initialized with a pre patch representation of the {@link Object} to
     * capture changes of.
     * <p>
     * The current changes to the pre patched {@link Object} can be retrieved any time by the methods:<br>
     * - {@link #peek()}: Returns the changes, but leaves the {@link Snapshot}'s perception of what the "pre patch"
     * state is unchanged<br>
     * - {@link #capture()}: Returns the changes and sets the observed {@link Object}'s current state as the
     * {@link Snapshot}'s new perception of what the "pre patch" state is
     */
    public static final class Snapshot {

        private final Object patchable;
        private JsonNode prePatch;

        private Snapshot(Object patchable) {
            this.patchable = patchable;
            this.prePatch = asIgnoredNode(patchable);
        }

        /**
         * Returns a {@link List} of {@link Patch}es of what has changed on the observed {@link Object}.
         * <p>
         * Leaves this {@link Snapshot}'s perception of the {@link Object}'s pre patch state unchanged. As a result,
         * subsequent calls to either {@link #peek()} or {@link #capture()} without further changes to the
         * {@link Object} will return the same result.
         *
         * @return A {@link List} of {@link Patch}es that describe the changes performed on the {@link Snapshot}'s
         * observed {@link Object}, never null but might be empty
         */
        public List<Patch> peek() {
            return getPatchOperations(false);
        }

        /**
         * Returns a {@link List} of {@link Patch}es of what has changed on the observed {@link Object}.
         * <p>
         * Changes this {@link Snapshot}'s perception of the {@link Object}'s pre patch state to its current state. As
         * a result, subsequent calls to either {@link #peek()} or {@link #capture()} without further changes will
         * return no changes.
         *
         * @return A {@link List} of {@link Patch}es that describe the changes performed on the {@link Snapshot}'s
         * observed {@link Object}, never null but might be empty
         */
        public List<Patch> capture() {
            return getPatchOperations(true);
        }

        private synchronized List<Patch> getPatchOperations(boolean keepCurrentStateAsPrePatch) {
            JsonNode postPatch = asIgnoredNode(this.patchable);

            List<Patch> patches = PatchUtil.getPatchOperations(this.prePatch, postPatch, IGNORING_MAPPER);
            if (keepCurrentStateAsPrePatch) {
                this.prePatch = postPatch;
            }
            return patches;
        }
    }

    private PatchUtil() {
    }

    static <T> JsonNode asStandardNode(T object) {
        return asNode(object, STANDARD_MAPPER);
    }

    static <T> JsonNode asIgnoredNode(T object) {
        return asNode(object, IGNORING_MAPPER);
    }

    private static JsonNode asNode(Object o, ObjectMapper objectMapper) {
        return objectMapper.valueToTree(o);
    }

    /**
     * Starts recording changes to a specifiable Java {@link Object} that are expressible via JSON patches.
     *
     * @param <T>      The type of {@link Object} to record changes for.
     * @param prePatch The yet unchanged Java {@link Object} to record changes on; might <b>not</b> be null.
     * @return A new {@link Snapshot} instance, never null
     */
    public static <T> Snapshot take(T prePatch) {
        if (prePatch == null) {
            throw new IllegalArgumentException("Cannot record changes on a null pre patch object");
        }
        return new Snapshot(prePatch);
    }

    /**
     * Applies a {@link List} of {@link Patch}es to a specifiable target Java {@link Object}.
     *
     * @param <T>        The type of {@link Object} to apply {@link Patch}es on.
     * @param target     The {@link Object} to apply {@link Patch}es on; might <b>not</b> be null.
     * @param operations The {@link Patch}es to apply; might be null or empty, which will cause an
     *                   unchanged {@link Object} to be returned.
     * @return A changed version of the given target {@link Object} where the given {@link List} of {@link Patch}es
     * have been applied, never null
     * @throws JsonPatchApplicationException If at least one of the {@link Patch}es is not applicable, for example
     * if a field is tried to be patched which annotated with @{@link NoPatch}.
     */
    public static <T> T apply(T target, Patch... operations) throws JsonPatchApplicationException {
        return apply(target, Arrays.asList(operations));
    }

    /**
     * Applies a {@link List} of {@link Patch}es to a specifiable target Java {@link Object}.
     *
     * @param <T>        The type of {@link Object} to apply {@link Patch}es on.
     * @param target     The {@link Object} to apply {@link Patch}es on; might <b>not</b> be null.
     * @param operations The {@link List} of {@link Patch}es to apply; might be null or empty, which will cause an
     *                   unchanged {@link Object} to be returned.
     * @return A changed version of the given target {@link Object} where the given {@link List} of {@link Patch}es
     * have been applied, never null
     * @throws JsonPatchApplicationException If at least one of the {@link Patch}es is not applicable, for example
     * if a field is tried to be patched which annotated with @{@link NoPatch}.
     */
    public static <T> T apply(T target, List<Patch> operations) throws JsonPatchApplicationException {
        T targetWithoutIngnored = apply(target, Collections.emptyList(), IGNORING_MAPPER);
        List<Patch> restoringOperations = getPatchOperations(
                asStandardNode(targetWithoutIngnored),
                asStandardNode(target),
                STANDARD_MAPPER);

        T patched = apply(target, operations, IGNORING_MAPPER);
        return apply(patched, restoringOperations, STANDARD_MAPPER);
    }

    private static <T> T apply(T target, List<Patch> operations, ObjectMapper mapper) throws JsonPatchApplicationException {
        if (target == null) {
            throw new IllegalArgumentException("Cannot apply changes on a null target object");
        }
        try {
            JsonNode prePatch = asNode(target, mapper);

            JsonNode postPatch;
            if (operations != null && !operations.isEmpty()) {
                JsonNode diff = asNode(operations, mapper);
                postPatch = JsonPatch.apply(diff, prePatch);
            } else {
                postPatch = prePatch;
            }

            String jsonPostPatch = mapper.writeValueAsString(postPatch);

            @SuppressWarnings("unchecked")
            T patched = (T) mapper.readValue(jsonPostPatch, target.getClass());
            return patched;
        } catch (IOException e) {
            throw new RuntimeException("Cannot apply patch operations", e);
        }
    }

    private static <T> List<Patch> getPatchOperations(JsonNode prePatch, JsonNode postPatch, ObjectMapper mapper) {
        try {
            JsonNode diff = JsonDiff.asJson(prePatch, postPatch);
            String jsonDiff = mapper.writeValueAsString(diff);
            Patch[] ops = mapper.readValue(jsonDiff, Patch[].class);
            return Arrays.asList(ops);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create patch ", e);
        }
    }
}