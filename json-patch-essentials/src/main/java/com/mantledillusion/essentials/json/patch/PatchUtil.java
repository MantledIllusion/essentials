package com.mantledillusion.essentials.json.patch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flipkart.zjsonpatch.JsonPatch;
import com.flipkart.zjsonpatch.JsonPatchApplicationException;
import com.mantledillusion.essentials.json.patch.ignore.PatchIgnoreIntrospector;
import com.mantledillusion.essentials.json.patch.ignore.NoPatch;
import com.mantledillusion.essentials.json.patch.model.Patch;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Static util that is able to {@link #record(Object)}/{@link #apply(Object, List)} RFC 6902 compliant JSON patches
 * from/to Java {@link Object}s.
 */
public class PatchUtil {

    static final ObjectMapper MAPPER;

    static {
       MAPPER = new ObjectMapper();
       MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
       MAPPER.setAnnotationIntrospector(new PatchIgnoreIntrospector());
       MAPPER.registerModule(new JavaTimeModule());
    }

    private PatchUtil() {
    }

    /**
     * Starts recording changes to a specifiable Java {@link Object} that are expressible via JSON patches.
     *
     * @param <T>      The type of {@link Object} to record changes for.
     * @param prePatch The yet unchanged Java {@link Object} to record changes on; might <b>not</b> be null.
     * @return A new {@link PatchRecorder} instance, never null
     */
    public static <T> PatchRecorder record(T prePatch) {
        if (prePatch == null) {
            throw new IllegalArgumentException("Cannot record changes on a null pre patch object");
        }
        return new PatchRecorder(prePatch);
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
        if (target == null) {
            throw new IllegalArgumentException("Cannot apply changes on a null target object");
        }
        try {
            JsonNode prePatch = MAPPER.valueToTree(target);

            JsonNode postPatch;
            if (operations != null && !operations.isEmpty()) {
                JsonNode diff = MAPPER.valueToTree(operations);
                postPatch = JsonPatch.apply(diff, prePatch);
            } else {
                postPatch = prePatch;
            }

            String jsonPostPatch = MAPPER.writeValueAsString(postPatch);

            @SuppressWarnings("unchecked")
            T patched = (T) MAPPER.readValue(jsonPostPatch, target.getClass());
            return patched;
        } catch (IOException e) {
            throw new RuntimeException("Cannot apply patch operations", e);
        }
    }
}