package com.mantledillusion.essentials.vaadin.component;

import com.mantledillusion.data.collo.Keyword;

/**
 * Extension to {@link Keyword} that is able to deep-validate a {@link String} input that already is matching a
 * term's keyword by that keyword's matcher.
 */
public interface MatchedKeyword extends Keyword {

    /**
     * Validates the given input.
     *
     * @param input The input; might <b>not</b> be null.
     * @return True if the input is valid, false otherwise.
     */
    default boolean isValid(String input) {
        return true;
    }

    /**
     * Returns a invalidity label to display.
     * <p>
     * Only called after {@link #isValid(String)} returned false.
     *
     * @param input The invalid value; might <b>not</b> be null.
     * @return The label to display, might be null
     */
    default String getInvalidLabel(String input) {
        return null;
    }
}
