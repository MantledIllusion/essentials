package com.mantledillusion.essentials.vaadin.component;

import com.vaadin.flow.component.ComponentEvent;

import java.util.List;

/**
 * {@link ComponentEvent} fired when there is at least one {@link MatchedFilter} added or removed from its {@link FilterBar}.
 *
 * @param <T> The {@link MatchedTerm} representing the terms (for example a name, an address, a specific ID, ...)
 * @param <P> The ({@link MatchedKeyword} representing the distinguishable keywords of a term (a first name, a last name, a company name, a zip code, ...)
 */
public final class MatchedFilterChangedEvent<T extends MatchedTerm, P extends MatchedKeyword> extends ComponentEvent<FilterBar<T, P>> {

    private final List<MatchedFilter<T, P>> added;
    private final List<MatchedFilter<T, P>> removed;

    MatchedFilterChangedEvent(FilterBar<T, P> source, boolean fromClient, List<MatchedFilter<T, P>> added, List<MatchedFilter<T, P>> removed) {
        super(source, fromClient);
        this.added = added;
        this.removed = removed;
    }

    /**
     * The {@link MatchedFilter}s added to the {@link FilterBar}.
     *
     * @return The {@link MatchedFilter}s, never null
     */
    public List<MatchedFilter<T, P>> getAdded() {
        return this.added;
    }

    /**
     * The {@link MatchedFilter}s removed from the {@link FilterBar}.
     *
     * @return The {@link MatchedFilter}s, never null
     */
    public List<MatchedFilter<T, P>> getRemoved() {
        return this.removed;
    }
}
