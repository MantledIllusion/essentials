package com.mantledillusion.essentials.vaadin.component;

import com.mantledillusion.data.collo.InputPart;

/**
 * Extension to {@link InputPart} that is able to deep-validate a {@link String} input that already is matching a
 * group's part by that part's matcher.
 */
public interface MatchedFilterInputPart extends InputPart {

    /**
     * Validates the given input.
     *
     * @param value The input; might <b>not</b> be null.
     * @return True if the input is valid, false otherwise.
     */
    default boolean isValid(String value) {
        return true;
    }

    /**
     * Returns a invalidity label to display.
     * <p>
     * Only called after {@link #isValid(String)} returned false.
     *
     * @param value The invalid value; might <b>not</b> be null.
     * @return The label to display, might be null
     */
    default String getInvalidLabel(String value) {
        return null;
    }

    /**
     * Returns the priority of the part against other part when listing {@link MatchedFilter}s.
     * <p>
     * The lower the value the higher the priority.
     *
     * @return the priority, 0 by default
     */
    default long getPriority() {
        return 0L;
    }
}
