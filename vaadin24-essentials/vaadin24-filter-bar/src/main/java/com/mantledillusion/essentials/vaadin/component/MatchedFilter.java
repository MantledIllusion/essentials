package com.mantledillusion.essentials.vaadin.component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * A {@link MatchedFilter} created by a user of a {@link FilterBar}.
 *
 * @param <T> The {@link MatchedTerm} representing the terms (for example a name, an address, a specific ID, ...)
 * @param <K> The ({@link MatchedKeyword} representing the distinguishable keywords of a term (a first name, a last name, a company name, a zip code, ...)
 */
public final class MatchedFilter<T extends MatchedTerm, K extends MatchedKeyword> {

    private final String input;
    private final T term;
    private final Map<K, String> keywords;

    private Long matchCount;

    MatchedFilter(String input, T term, Map<K, String> keywords) {
        this.input = input;
        this.term = term;
        this.keywords = Collections.unmodifiableMap(keywords);
    }

    /**
     * The raw input as it was put into the search bar.
     *
     * @return The input, never null
     */
    public String getInput() {
        return input;
    }

    /**
     * Returns the term the input of the filter belongs to.
     *
     * @return The term, never null
     */
    public T getTerm() {
        return this.term;
    }

    /**
     * Returns the priority this {@link MatchedFilter} has in relation to other {@link MatchedFilter}s based on its term.
     * <p>
     * The lower the value the higher the priority.
     *
     * @return The priority
     */
    public long getTermPriority() {
        return this.term.getPriority();
    }

    /**
     * Returns whether the given keyword is included in the matched filter and can be retrieved using {@link #getKeywordInput(MatchedKeyword)}}.
     *
     * @param keyword The keyword; might be null.
     * @return True if the keyword is included, false otherwise
     */
    public boolean hasPart(K keyword) {
        return this.keywords.containsKey(keyword);
    }

    /**
     * Returns all keywords the input was separated into.
     *
     * @return The keywords, never null or empty
     */
    public Set<K> getKeywords() {
        return this.keywords.keySet();
    }

    /**
     * Returns all keywords the input was separated into.
     *
     * @return The keywords, never null or empty
     */
    public Map<K, String> getKeywordInputs() {
        return this.keywords;
    }

    /**
     * Retrieves a keyword's input from this {@link MatchedFilter}.
     *
     * @param keyword The keyword to retrieve; might be null.
     * @return The keyword, never null if the given part is included in {@link #getKeywords()}
     */
    public String getKeywordInput(K keyword) {
        return this.keywords.get(keyword);
    }

    /**
     * Returns the priority this {@link MatchedFilter} has in relation to other {@link MatchedFilter}s based on its keywords.
     * <p>
     * The lower the value the higher the priority.
     *
     * @return The priority
     */
    public long getKeywordPriority() {
        return this.keywords.keySet().stream().mapToLong(MatchedKeyword::getPriority).sum();
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
