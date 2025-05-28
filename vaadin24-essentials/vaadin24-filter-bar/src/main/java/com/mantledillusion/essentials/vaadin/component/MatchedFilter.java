package com.mantledillusion.essentials.vaadin.component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A {@link MatchedFilter} created by a user of a {@link FilterBar}.
 *
 * @param <T> The {@link MatchedTerm} representing the terms (for example a name, an address, a specific ID, ...)
 * @param <K> The ({@link MatchedKeyword} representing the distinguishable keywords of a term (a first name, a last name, a company name, a zip code, ...)
 */
public final class MatchedFilter<T extends MatchedTerm<K>, K extends MatchedKeyword> {

    private final T term;
    private final Map<K, String> keywords;

    MatchedFilter(T term, Map<K, String> keywords) {
        this.term = term;
        this.keywords = Collections.unmodifiableMap(keywords);
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
     * Returns whether the given keyword is included in the matched filter and can be retrieved using {@link #getKeywordInput(MatchedKeyword)}}.
     *
     * @param keyword The keyword; might be null.
     * @return True if the keyword is included, false otherwise
     */
    public boolean hasKeyword(K keyword) {
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
}
