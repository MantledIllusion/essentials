package com.mantledillusion.essentials.vaadin.component;

import com.vaadin.flow.component.ComponentEvent;

import java.util.List;

/**
 * {@link ComponentEvent} fired when there is at least one {@link MatchedFilter} added or removed from its {@link FilterBar}.
 *
 * @param <G> The {@link MatchedFilterInputGroup} implementing {@link Enum} representing the input groups
 *           (for example a name, an address, a specific ID, ...).
 * @param <P> The ({@link MatchedFilterInputPart} implementing) {@link Enum} representing the distinguishable parts of
 *           the input groups (a first name, a last name, a company name, a zip code, ...).
 */
public final class MatchedFilterChangedEvent<G extends Enum<G> & MatchedFilterInputGroup, P extends Enum<P> & MatchedFilterInputPart> extends ComponentEvent<FilterBar<G, P>> {

    private final List<MatchedFilter<G, P>> added;
    private final List<MatchedFilter<G, P>> removed;

    MatchedFilterChangedEvent(FilterBar<G, P> source, boolean fromClient, List<MatchedFilter<G, P>> added, List<MatchedFilter<G, P>> removed) {
        super(source, fromClient);
        this.added = added;
        this.removed = removed;
    }

    /**
     * The {@link MatchedFilter}s added to the {@link FilterBar}.
     *
     * @return The {@link MatchedFilter}s, never null
     */
    public List<MatchedFilter<G, P>> getAdded() {
        return this.added;
    }

    /**
     * The {@link MatchedFilter}s removed from the {@link FilterBar}.
     *
     * @return The {@link MatchedFilter}s, never null
     */
    public List<MatchedFilter<G, P>> getRemoved() {
        return this.removed;
    }
}
