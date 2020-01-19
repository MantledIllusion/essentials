package com.mantledillusion.essentials.json.patch.ignore;

import com.mantledillusion.essentials.json.patch.model.Patch;

import java.util.List;

/**
 * {@link RuntimeException} that is thrown if a {@link Patch} tries to manipulate a path whose property is annotated
 * with @{@link NoPatch}.
 */
public class NoPatchException extends RuntimeException {

    private final List<Patch> patches;

    /**
     * Constructor.
     *
     * @param patches The {@link Patch}es that tried to manipulate a path whose propertes are annotated
     *                with @{@link NoPatch}; might <b>not</b> be null or empty.
     */
    public NoPatchException(List<Patch> patches) {
        super(toMessage(patches));
        this.patches = patches;
    }

    /**
     * Returns the {@link Patch}es that tried to manipulate a path whose propertes are annotated with @{@link NoPatch}.
     *
     * @return The {@link Patch}es, never null or empty.
     */
    public List<Patch> getPatches() {
        return patches;
    }

    private static String toMessage(List<Patch> patches) {
        StringBuilder sb = new StringBuilder().append("There are ").append(patches.size()).
                append(" patches trying to manipulate a unpatchable properties: ");
        patches.forEach(patch -> sb.append("\n").append(patch));
        return sb.toString();
    }
}
