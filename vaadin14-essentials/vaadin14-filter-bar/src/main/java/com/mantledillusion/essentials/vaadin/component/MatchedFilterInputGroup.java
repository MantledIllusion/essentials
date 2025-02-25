package com.mantledillusion.essentials.vaadin.component;

/**
 * A group of {@link MatchedFilterInputPart}s.
 */
public interface MatchedFilterInputGroup {

    /**
     * Returns the priority of the group against other groups when listing {@link MatchedFilter}s.
     * <p>
     * The lower the value the higher the priority.
     *
     * @return the priority, 0 by default
     */
    default long getPriority() {
        return 0L;
    }
}
