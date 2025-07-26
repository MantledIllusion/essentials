package com.mantledillusion.essentials.vaadin.component;

import com.mantledillusion.data.collo.Keyword;

/**
 * Extension to {@link Keyword} that is able to deep-validate a {@link String} input that already is matching a
 * term's keyword by that keyword's matcher.
 */
public interface MatchedKeyword extends Keyword {

    /**
     * Returns a displayable label for this keyword.
     *
     * @return a label, never null
     */
    default String getLabel() {
        return toString();
    }
}
