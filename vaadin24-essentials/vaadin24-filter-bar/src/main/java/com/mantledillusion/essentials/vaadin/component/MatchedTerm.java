package com.mantledillusion.essentials.vaadin.component;

import com.mantledillusion.data.collo.Term;

/**
 * A term of {@link MatchedKeyword}s.
 */
public interface MatchedTerm extends Term {

    /**
     * Returns the priority of the term against other terms when listing {@link MatchedFilter}s.
     * <p>
     * The lower the value the higher the priority.
     *
     * @return the priority, 0 by default
     */
    default long getPriority() {
        return 0L;
    }
}
