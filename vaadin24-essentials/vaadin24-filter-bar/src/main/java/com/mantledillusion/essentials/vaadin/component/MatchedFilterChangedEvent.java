package com.mantledillusion.essentials.vaadin.component;

import com.vaadin.flow.component.ComponentEvent;

import java.util.List;

/**
 * {@link ComponentEvent} fired when there is at least one {@link MatchedFilter} added or removed from its {@link FilterBar}.
 *
 * @param <T> The {@link MatchedTerm} representing the terms (for example a name, an address, a specific ID, ...)
 * @param <K> The ({@link MatchedKeyword} representing the distinguishable keywords of a term (a first name, a last name, a company name, a zip code, ...)
 */
public final class MatchedFilterChangedEvent<T extends MatchedTerm<K>, K extends MatchedKeyword> extends ComponentEvent<FilterBar<T, K>> {

    private final List<MatchedFilter<T, K>> added;
    private final List<MatchedFilter<T, K>> removed;
    private final MatchedFilterOperator operator;

    MatchedFilterChangedEvent(FilterBar<T, K> source, boolean fromClient, List<MatchedFilter<T, K>> added, List<MatchedFilter<T, K>> removed, MatchedFilterOperator operator) {
        super(source, fromClient);
        this.added = added;
        this.removed = removed;
        this.operator = operator;
    }

    /**
     * The {@link MatchedFilter}s added to the {@link FilterBar}.
     *
     * @return The {@link MatchedFilter}s, never null
     */
    public List<MatchedFilter<T, K>> getAdded() {
        return this.added;
    }

    /**
     * The {@link MatchedFilter}s removed from the {@link FilterBar}.
     *
     * @return The {@link MatchedFilter}s, never null
     */
    public List<MatchedFilter<T, K>> getRemoved() {
        return this.removed;
    }

    /**
     * The operator conjugating the {@link MatchedFilter}s.
     *
     * @return The operator, never null
     */
    public MatchedFilterOperator getOperator() {
        return operator;
    }
}
