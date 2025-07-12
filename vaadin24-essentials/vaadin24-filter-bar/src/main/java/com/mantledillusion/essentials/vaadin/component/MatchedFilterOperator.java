package com.mantledillusion.essentials.vaadin.component;

/**
 * Operator defining how multiple selected filters are meant to be conjugated.
 */
public enum MatchedFilterOperator {

    /**
     * Filters are linked, an element has to match all of them to be a match.
     */
    AND,

    /**
     * Filters are linked, an element has to match one of them to be a match.
     */
    OR
}
