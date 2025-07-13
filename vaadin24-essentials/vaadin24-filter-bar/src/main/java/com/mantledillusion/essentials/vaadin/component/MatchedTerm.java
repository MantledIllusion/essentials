package com.mantledillusion.essentials.vaadin.component;

import com.mantledillusion.data.collo.Term;

import java.util.List;
import java.util.Map;

/**
 * A term of {@link MatchedKeyword}s.
 */
public interface MatchedTerm<K extends MatchedKeyword> extends Term<K> {

    /**
     * Returns a displayable label for this term.
     *
     * @return a label, never null
     */
    default String getLabel() {
        return toString();
    }

    /**
     * Returns an ordered list of matches serving as clickable favorites to the term.
     *
     * @return A list of {@link KeywordMatch}es, might be null
     */
    default List<KeywordMatch<K>> getFavorites() {
        return null;
    }

    /**
     * Returns an ordered list of matches serving as displayable examples to the term.
     *
     * @return A list of {@link KeywordMatch}es, might be null
     */
    default List<KeywordMatch<K>> getExamples() {
        return null;
    }

    /**
     * Returns whether this {@link MatchedTerm} in a {@link MatchedFilter} with the given keywords is combinable with
     * the other given {@link MatchedFilter}.
     * <p>
     * Can be used to implement logics where a term ...<br>
     * - can only be filtered once<br>
     * - can be filtered multiple times if the other instance does not match the same keywords<br>
     * - cannot be filtered together with specific other terms<br>
     * - etc
     * <p>
     * Returns true by default.
     *
     * @param keywords The keywords to this {@link MatchedTerm}; might <b>not</b> be null
     * @param other The {@link MatchedFilter} to check against; might <b>not</b> be null
     * @return True if this term and its keywords is combinable with the given other, false otherwise
     */
    default boolean isCombinable(Map<K, String> keywords, MatchedFilter<? extends MatchedTerm<K>, K> other) {
        return true;
    }

    /**
     * Returns the maximum count of matched keyword combinations to display for the term.
     * <p>
     * Defaults to three, the fourth and following matches after sorting by
     * {@link com.mantledillusion.data.collo.Keyword#weight(String, String)} will be shown collapsed and have to be
     * expanded by the user.
     *
     * @return The maximum count of matches to display, at least one
     */
    default int displayThreshold() {
        return 3;
    }
}
