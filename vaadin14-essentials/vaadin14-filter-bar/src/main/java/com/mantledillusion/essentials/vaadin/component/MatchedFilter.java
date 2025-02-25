package com.mantledillusion.essentials.vaadin.component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * A {@link MatchedFilter} created by a user of a {@link FilterBar}.
 *
 * @param <G> The {@link MatchedFilterInputGroup} implementing {@link Enum} representing the input groups
 *           (for example a name, an address, a specific ID, ...).
 * @param <P> The ({@link MatchedFilterInputPart} implementing) {@link Enum} representing the distinguishable parts of
 *           the input groups (a first name, a last name, a company name, a zip code, ...).
 */
public final class MatchedFilter<G extends Enum<G> & MatchedFilterInputGroup, P extends Enum<P> & MatchedFilterInputPart> {

    private final String term;
    private final G group;
    private final Map<P, String> parts;

    private Long matchCount;

    MatchedFilter(String term, G group, Map<P, String> parts) {
        this.term = term;
        this.group = group;
        this.parts = Collections.unmodifiableMap(parts);
    }

    /**
     * The raw term as it was put into the search bar.
     *
     * @return The term, never null
     */
    public String getTerm() {
        return term;
    }

    /**
     * Returns the group the input of the filter belongs to.
     *
     * @return The group, never null
     */
    public G getGroup() {
        return this.group;
    }

    /**
     * Returns the priority this {@link MatchedFilter} has in relation to other {@link MatchedFilter}s based on its group.
     * <p>
     * The lower the value the higher the priority.
     *
     * @return The priority
     */
    public long getGroupPriority() {
        return this.group.getPriority();
    }

    /**
     * Returns whether the given part is included in the matched filter and can be retrieved using {@link #getPart(Enum)}.
     *
     * @param part The part; might be null.
     * @return True if the part is included, false otherwise
     */
    public boolean hasPart(P part) {
        return this.parts.containsKey(part);
    }

    /**
     * Returns all parts the input was separated into.
     *
     * @return The parts, never null or empty
     */
    public Set<P> getParts() {
        return this.parts.keySet();
    }

    /**
     * Returns all parts the input was separated into.
     *
     * @return The parts, never null or empty
     */
    public Map<P, String> getPartMappings() {
        return this.parts;
    }

    /**
     * Retrieves a part from this {@link FilterBar}.
     *
     * @param part The part to retrieve; might be null.
     * @return The part, never null if the given part is included in {@link #getParts()}
     */
    public String getPart(P part) {
        return this.parts.get(part);
    }

    /**
     * Returns the priority this {@link MatchedFilter} has in relation to other {@link MatchedFilter}s based on its group.
     * <p>
     * The lower the value the higher the priority.
     *
     * @return The priority
     */
    public long getPartPriority() {
        return this.parts.keySet().stream().mapToLong(MatchedFilterInputPart::getPriority).sum();
    }

    /**
     * Returns the amount of matches this filter got as counted by {@link FilterBar#setMatchCountRetriever(Function)}.
     *
     * @return The amount of matches, might be null depending on the {@link FilterBar}'s match count settings
     */
    public Long getMatchCount() {
        return this.matchCount;
    }

    void setMatchCount(Long matchCount) {
        this.matchCount = matchCount;
    }
}
