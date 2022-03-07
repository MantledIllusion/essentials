package com.mantledillusion.essentials.vaadin.component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * A {@link MatchedFilter} created by a user of a {@link FilterBar}.
 *
 * @param <G> The {@link Enum} representing the input groups (for example a name, an address, a specific ID, ...).
 * @param <P> The ({@link MatchedFilterInputPart} implementing) {@link Enum} representing the distinguishable parts of
 *           the input groups (a first name, a last name, a company name, a zip code, ...).
 */
public final class MatchedFilter<G extends Enum<G>, P extends Enum<P> & MatchedFilterInputPart> {

    private final G group;
    private final Map<P, String> parts;

    private final Function<G, String> groupRenderer;
    private final Function<P, String> partRenderer;

    MatchedFilter(G group, Map<P, String> parts, Function<G, String> groupRenderer, Function<P, String> partRenderer) {
        this.group = group;
        this.parts = Collections.unmodifiableMap(parts);
        this.groupRenderer = groupRenderer;
        this.partRenderer = partRenderer;
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

    @Override
    public String toString() {
        return this.groupRenderer.apply(this.group) + "; " + toStringParts();
    }

    String toStringParts() {
        return this.parts.entrySet().stream().
                map(entry -> this.partRenderer.apply(entry.getKey()) + ": " + entry.getValue()).
                reduce((a, b) -> a + ", " + b).orElse("");
    }
}
