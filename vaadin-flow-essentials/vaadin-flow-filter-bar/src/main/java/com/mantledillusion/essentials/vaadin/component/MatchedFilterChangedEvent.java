package com.mantledillusion.essentials.vaadin.component;

import com.vaadin.flow.component.ComponentEvent;

/**
 * {@link ComponentEvent} fired when there either is a {@link MatchedFilter} {@link FilterChangeAction#ADDED} or
 * {@link FilterChangeAction#REMOVED} to/from the {@link FilterBar}.
 *
 * @param <G> The {@link Enum} representing the input groups (for example a name, an address, a specific ID, ...).
 * @param <P> The ({@link MatchedFilterInputPart} implementing) {@link Enum} representing the distinguishable parts of
 *           the input groups (a first name, a last name, a company name, a zip code, ...).
 */
public final class MatchedFilterChangedEvent<G extends Enum<G>, P extends Enum<P> & MatchedFilterInputPart> extends ComponentEvent<FilterBar<G, P>> {

    /**
     * Defines the possible types of {@link MatchedFilterChangedEvent}s.
     */
    public enum FilterChangeAction {
        ADDED, REMOVED;
    }

    private final MatchedFilter<G, P> changedFilter;
    private final FilterChangeAction action;

    MatchedFilterChangedEvent(FilterBar<G, P> source, boolean fromClient, MatchedFilter<G, P> changedFilter, FilterChangeAction action) {
        super(source, fromClient);
        this.changedFilter = changedFilter;
        this.action = action;
    }

    /**
     * Returns the {@link MatchedFilter} that changed
     *
     * @return The filter, never null
     */
    public MatchedFilter<G, P> getChangedFilter() {
        return changedFilter;
    }

    /**
     * Returns the type of action to the {@link MatchedFilter} that caused this {@link MatchedFilterChangedEvent}.
     *
     * @return The {@link FilterChangeAction}, never null
     */
    public FilterChangeAction getAction() {
        return action;
    }
}
