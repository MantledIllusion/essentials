package com.mantledillusion.essentials.vaadin.component;

import com.vaadin.flow.component.ComponentEvent;

import java.util.Set;

/**
 * {@link ComponentEvent} fired when there is at least one {@link MatchedFilter} added or removed from its {@link FilterBar}.
 *
 * @param <T> The {@link MatchedTerm} representing the terms (for example a name, an address, a specific ID, ...)
 * @param <K> The ({@link MatchedKeyword} representing the distinguishable keywords of a term (a first name, a last name, a company name, a zip code, ...)
 */
public final class MatchedFilterChangedEvent<T extends MatchedTerm<K>, K extends MatchedKeyword> extends ComponentEvent<FilterBar<T, K>> {

    private final Set<MatchedFilter<T, K>> addedFilters;
    private final Set<MatchedFilter<T, K>> removedFilters;
    private final Set<MatchedFilter<T, K>> currentFilters;
    private final MatchedFilterOperator previousOperator;
    private final MatchedFilterOperator currentOperator;

    MatchedFilterChangedEvent(FilterBar<T, K> source, boolean fromClient,
                              Set<MatchedFilter<T, K>> addedFilters, Set<MatchedFilter<T, K>> removedFilters, Set<MatchedFilter<T, K>> currentFilters,
                              MatchedFilterOperator previousOperator, MatchedFilterOperator currentOperator) {
        super(source, fromClient);
        this.currentFilters = currentFilters;
        this.addedFilters = addedFilters;
        this.removedFilters = removedFilters;
        this.previousOperator = previousOperator;
        this.currentOperator = currentOperator;
    }

    /**
     * The {@link MatchedFilter}s added to the {@link FilterBar}.
     *
     * @return The {@link MatchedFilter}s, never null, might be empty if none was added
     */
    public Set<MatchedFilter<T, K>> getAddedFilters() {
        return this.addedFilters;
    }

    /**
     * The {@link MatchedFilter}s removed from the {@link FilterBar}.
     *
     * @return The {@link MatchedFilter}s, never null, might be empty if none was removed
     */
    public Set<MatchedFilter<T, K>> getRemovedFilters() {
        return this.removedFilters;
    }

    /**
     * The {@link MatchedFilter}s currently set in the {@link FilterBar}.
     *
     * @return The {@link MatchedFilter}s, never null, might be empty if none are set
     */
    public Set<MatchedFilter<T, K>> getCurrentFilters() {
        return currentFilters;
    }

    /**
     * The previous operator combining the {@link MatchedFilter}s.
     *
     * @return The operator, never null
     */
    public MatchedFilterOperator getPreviousOperator() {
        return previousOperator;
    }

    /**
     * The previous operator combining the {@link MatchedFilter}s.
     *
     * @return The operator, never null
     */
    public MatchedFilterOperator getCurrentOperator() {
        return currentOperator;
    }
}
