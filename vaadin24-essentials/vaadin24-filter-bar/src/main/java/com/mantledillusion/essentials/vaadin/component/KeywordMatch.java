package com.mantledillusion.essentials.vaadin.component;

import java.util.*;
import java.util.function.Function;

/**
 * A map of matching keyword segments to a term.
 * <p>
 * Instantiate using {@link KeywordMatch#ofKeyword(MatchedKeyword, String)}.
 *
 * @param <K> The ({@link MatchedKeyword} representing the distinguishable keywords of a term (a first name, a last name, a company name, a zip code, ...)
 */
public class KeywordMatch<K extends MatchedKeyword> {

    private final Map<K, String> keywords;

    private KeywordMatch(Map<K, String> keywords) {
        this.keywords = keywords;
    }

    /**
     * Returns an unmodifiable view of the matches' keywords.
     *
     * @return The keywords, never null
     */
    public Map<K, String> getKeywords() {
        return this.keywords;
    }

    /**
     * Returns a new match with all keywords of this match, including the given one.
     *
     * @param keyword The keyword matching a segment of a term; might <b>not</b> be null.
     * @param segment The segment matched by the keyword; might <b>not</b> be null.
     * @return A new {@link KeywordMatch} instance
     */
    public KeywordMatch<K> andKeyword(K keyword, String segment) {
        if (keyword == null) {
            throw new IllegalArgumentException("Cannot create a match with a null keyword");
        } else if (segment == null) {
            throw new IllegalArgumentException("Cannot create a match with a null input");
        }

        var keywords = new HashMap<>(this.keywords);
        keywords.put(keyword, segment);

        return new KeywordMatch<>(Collections.unmodifiableMap(keywords));
    }

    /**
     * Returns a new match with the given keyword.
     *
     * @param <K> The ({@link MatchedKeyword} representing the distinguishable keywords of a term (a first name, a last name, a company name, a zip code, ...)
     * @param keyword The keyword matching a segment of a term; might <b>not</b> be null.
     * @param segment The segment matched by the keyword; might <b>not</b> be null.
     * @return A new {@link KeywordMatch} instance
     */
    public static <K extends MatchedKeyword> KeywordMatch<K> ofKeyword(K keyword, String segment) {
        return new KeywordMatch<K>(Collections.emptyMap()).andKeyword(keyword, segment);
    }

    /**
     * Returns a list of matches with the given keywords.
     *
     * @param <K> The ({@link MatchedKeyword} representing the distinguishable keywords of a term (a first name, a last name, a company name, a zip code, ...)
     * @param keyword The keyword matching a segment of a term; might <b>not</b> be null.
     * @param segments The segments matched by the keyword; might <b>not</b> be null.
     * @return A new list of {@link KeywordMatch} instances
     */
    public static <K extends MatchedKeyword> List<KeywordMatch<K>> ofKeywords(K keyword, String... segments) {
        return ofKeywords(keyword, segment -> segment, segments);
    }

    /**
     * Returns a list of matches with the given keywords.
     *
     * @param <K> The ({@link MatchedKeyword} representing the distinguishable keywords of a term (a first name, a last name, a company name, a zip code, ...)
     * @param keyword The keyword matching a segment of a term; might <b>not</b> be null.
     * @param segments The segments matched by the keyword; might <b>not</b> be null.
     * @return A new list of {@link KeywordMatch} instances
     */
    @SafeVarargs
    public static <K extends MatchedKeyword, S> List<KeywordMatch<K>> ofKeywords(K keyword, Function<S, String> mapper, S... segments) {
        return Arrays.stream(segments)
                .map(mapper)
                .map(segment -> new KeywordMatch<K>(Collections.emptyMap()).andKeyword(keyword, segment))
                .toList();
    }
}
