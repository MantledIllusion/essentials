package com.mantledillusion.essentials.vaadin.component;

import com.mantledillusion.data.collo.TermAnalyzer;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Component for building the most complex filter constellations on-the-fly in the UI.
 * <p>
 * Uses Collo's {@link TermAnalyzer} for realtime input recognition.
 *
 * @param <T> The {@link MatchedTerm} representing the terms (for example a name, an address, a specific ID, ...)
 * @param <K> The ({@link MatchedKeyword} representing the distinguishable keywords of a term (a first name, a last name, a company name, a zip code, ...)
 */
public class FilterBar<T extends MatchedTerm, K extends MatchedKeyword> extends Composite<Component> implements HasSize {

    private class MatchedFilterQuery {

        private final List<MatchedFilter<T, K>> matches;

        private MatchedFilterQuery(String term) {
            this.matches = FilterBar.this.analyzer.analyze(term).entrySet().stream()
                    .flatMap(entry -> entry.getValue().stream()
                            .map(keywords -> new MatchedFilter<>(term, entry.getKey(), keywords)))
                    .sorted((f1, f2) -> f1.getTermPriority() == f2.getTermPriority()
                            ? Long.compare(f1.getKeywordPriority(), f2.getKeywordPriority())
                            : Long.compare(f1.getTermPriority(), f2.getTermPriority()))
                    .collect(Collectors.toList());

            if (FilterBar.this.matchCountRetriever != null) {
                if (FilterBar.this.matchCountMode == MatchCountMode.SKIP) {
                    if (FilterBar.this.matchCountThreshold >= this.matches.size()) {
                        count(this.matches.size());
                    }
                } else {
                    count(Math.min(this.matches.size(), FilterBar.this.matchCountThreshold));
                }
            }
        }

        private void count(int limit) {
            this.matches.stream().limit(limit).forEach(match -> match.setMatchCount(FilterBar.this.matchCountRetriever.apply(match)));
        }

        private int count() {
            return this.matches.size();
        }

        private Stream<MatchedFilter<T, K>> fetch(int offset, int limit) {
            return this.matches.stream().skip(offset).limit(limit);
        }
    }

    private final class Favorite {

        private final String label;
        private final FilterModification modification;

        private Favorite(String label, FilterModification modification) {
            this.label = label;
            this.modification = modification;
        }
    }

    private final class FilterModification {

        private final List<BiConsumer<List<MatchedFilter<T, K>>, List<MatchedFilter<T, K>>>> modifications = new ArrayList<>();

        private void add(BiConsumer<List<MatchedFilter<T, K>>, List<MatchedFilter<T, K>>> modification) {
            this.modifications.add(modification);
        }

        private void apply(boolean isFromClient) {
            List<MatchedFilter<T, K>> added = new ArrayList<>();
            List<MatchedFilter<T, K>> removed = new ArrayList<>();

            this.modifications.forEach(modification -> modification.accept(added, removed));

            removed.forEach(FilterBar.this::removeFilter);
            added.forEach(FilterBar.this::addFilter);

            FilterBar.this.notify(Collections.unmodifiableList(added), Collections.unmodifiableList(removed), isFromClient);
        }
    }

    /**
     * The modes when to assert a {@link MatchedFilter}'s count.
     */
    public enum MatchCountMode {

        /**
         * Retrieves the result count for the {@link MatchedFilter}s with the highest priority, capped by {@link #setMatchCountThreshold(int)}.
         */
        CAPPED,

        /**
         * Retrieves the result count only if there are equal or fewer {@link MatchedFilter}s than {@link #setMatchCountThreshold(int)}.
         */
        SKIP
    }

    public abstract class MatchedFilterBuilder<B> {

        /**
         * Builder for a single modification to a specific term of {@link MatchedFilter}s.
         */
        public class MatchedTermBuilder {

            private final T term;
            private final List<Predicate<Map<K, String>>> keywords = new ArrayList<>();

            private MatchedTermBuilder(T term) {
                this.term = term;
            }

            /**
             * Adds a {@link Predicate} for a specific {@link MatchedKeyword} being present.
             *
             * @param keyword The keyword that needs to be present in a term in order for the modification to apply; might <b>not</b> be null.
             * @return this
             */
            public MatchedTermBuilder andKeywordPresent(K keyword) {
                if (keyword == null) {
                    throw new IllegalArgumentException("Cannot filter for a null keyword");
                }
                this.keywords.add(keywords -> keywords.containsKey(keyword));
                return this;
            }

            /**
             * Adds a {@link Predicate} for a specific {@link MatchedKeyword} being absent.
             *
             * @param keyword The keyword that needs to be absent in a term in order for the modification to apply; might <b>not</b> be null.
             * @return this
             */
            public MatchedTermBuilder andKeywordAbsent(K keyword) {
                if (keyword == null) {
                    throw new IllegalArgumentException("Cannot filter for a null keyword");
                }
                this.keywords.add(keywords -> !keywords.containsKey(keyword));
                return this;
            }

            /**
             * Adds a {@link Predicate} for a specific {@link MatchedKeyword} being present and matching the given regular expression.
             *
             * @param keyword The keyword that needs to be present and matching in a term in order for the modification to apply; might <b>not</b> be null.
             * @param regex The regex the keyword needs to match; might <b>not</b> be null.
             * @return this
             */
            public MatchedTermBuilder andKeywordMatching(K keyword, String regex) {
                if (keyword == null) {
                    throw new IllegalArgumentException("Cannot filter for a null keyword");
                } else if (regex == null) {
                    throw new IllegalArgumentException("Cannot match against a null regex");
                }
                this.keywords.add(keywords -> keywords.containsKey(keyword) && keywords.get(keyword).matches(regex));
                return this;
            }

            /**
             * Adds {@link MatchedFilter}s for the given term.
             * <p>
             * From all the keyword sets analyzed from the given term, only such {@link MatchedFilter}s are added whose
             * {@link MatchedKeyword}s match the filters specified by:
             * <p>
             * - {@link #andKeywordPresent(MatchedKeyword)}<br>
             * - {@link #andKeywordAbsent(MatchedKeyword)}<br>
             * - {@link #andKeywordMatching(MatchedKeyword, String)}<br>
             *
             * @param input The term to add {@link MatchedFilter}s for; might <b>not</b> be null.
             * @return This {@link MatchedTermBuilder}'s parent {@link MatchedFilterBuilder}
             */
            public MatchedFilterBuilder<B> add(String input) {
                if (input == null) {
                    throw new IllegalArgumentException("Cannot add a filter for a null term");
                }

                MatchedFilterBuilder.this.modification.add((added, removed) -> FilterBar.this
                        .analyzer.analyze(input, this.term).stream()
                        .filter(keywords -> this.keywords.stream().allMatch(keyword -> keyword.test(keywords)))
                        .map(keywords -> new MatchedFilter<>(input, this.term, keywords))
                        .forEach(added::add));

                return MatchedFilterBuilder.this;
            }

            /**
             * Removes {@link MatchedFilter}s.
             * <p>
             * Only such {@link MatchedFilter}s are added whose {@link MatchedKeyword}s match the filters specified by:
             * <p>
             * - {@link #andKeywordPresent(MatchedKeyword)}<br>
             * - {@link #andKeywordAbsent(MatchedKeyword)}<br>
             * - {@link #andKeywordMatching(MatchedKeyword, String)}<br>
             *
             * @return This {@link MatchedTermBuilder}'s parent {@link MatchedFilterBuilder}
             */
            public MatchedFilterBuilder<B> remove() {
                MatchedFilterBuilder.this.modification.add((added, removed) -> FilterBar.this
                        .filters.keySet().stream()
                        .filter(filter -> filter.getTerm() == this.term)
                        .filter(filter -> this.keywords.stream().allMatch(keyword -> keyword.test(filter.getKeywordInputs())))
                        .forEach(removed::add));

                return MatchedFilterBuilder.this;
            }
        }

        private final FilterModification modification = new FilterModification();

        private MatchedFilterBuilder() {}

        protected FilterModification getModification() {
            return this.modification;
        }

        /**
         * Begins a new {@link MatchedTermBuilder} for a specific term.
         *
         * @param term The term to create a modification for; might <b>not</b> be null.
         * @return A new {@link MatchedTermBuilder}, never null
         */
        public MatchedTermBuilder forTerm(T term) {
            if (term == null) {
                throw new IllegalArgumentException("Cannot begin modifying a null term");
            }
            return new MatchedTermBuilder(term);
        }

        /**
         * Applies all created modifications.
         */
        public abstract B apply();
    }

    /**
     * Builder for a set of add/remove modifications to the {@link FilterBar}'s current {@link MatchedFilter}s.
     */
    public class FilterModificationBuilder extends MatchedFilterBuilder<Void> {

        private FilterModificationBuilder() {}

        /**
         * Applies all created modifications to this {@link FilterModificationBuilder}'s {@link FilterBar}.
         */
        @Override
        public Void apply() {
            getModification().apply(false);
            return null;
        }
    }

    /**
     * Builder for a set of add/remove modifications to the {@link FilterBar}'s current favorites.
     */
    public class FavoriteModificationBuilder {

        public class FavoriteSetBuilder extends MatchedFilterBuilder<FavoriteModificationBuilder> {

            private final String label;

            private FavoriteSetBuilder(String label) {
                this.label = label;
            }

            @Override
            public FavoriteModificationBuilder apply() {
                FavoriteModificationBuilder.this.favorites.add(new Favorite(this.label, getModification()));
                return FavoriteModificationBuilder.this;
            }
        }

        private final List<Favorite> favorites = new ArrayList<>();

        private FavoriteModificationBuilder() {

        }

        /**
         * Begins adding a new favorite.
         *
         * @param label The label to display on the favorite; might be null.
         * @return A new {@link FavoriteSetBuilder}, never null
         */
        public FavoriteSetBuilder withFavorite(String label) {
            return new FavoriteSetBuilder(label);
        }

        /**
         * Sets all the entered filters as favorites to the {@link FilterBar}.
         */
        public void set() {
            SubMenu menu = FilterBar.this.favorites.getItems().iterator().next().getSubMenu();
            menu.removeAll();

            this.favorites.forEach(favorite -> menu.addItem(favorite.label, event -> favorite.modification.apply(event.isFromClient())));

            FilterBar.this.favorites.setVisible(!this.favorites.isEmpty());
        }
    }

    private final TermAnalyzer<T, K> analyzer;
    private final Map<MatchedFilter<T, K>, Component> filters = new IdentityHashMap<>();

    private final VerticalLayout mainLayout;
    private final MenuBar favorites;
    private final ComboBox<MatchedFilter<T, K>> filterInput;
    private final HorizontalLayout filterLayout;

    private Function<T, String> termRenderer = String::valueOf;
    private Function<K, String> keywordRenderer = String::valueOf;

    private Function<MatchedFilter<T, K>, Long> matchCountRetriever = null;
    private MatchCountMode matchCountMode = MatchCountMode.CAPPED;
    private Integer matchCountThreshold = Integer.MAX_VALUE;

    /**
     * Advanced constructor.
     *
     * @param analyzer The {@link TermAnalyzer} to use; might <b>not</b> be null;
     */
    public FilterBar(TermAnalyzer<T, K> analyzer) {
        if (analyzer == null) {
            throw new IllegalArgumentException("Cannot operate on a null analyzer");
        }

        this.analyzer = analyzer;

        this.mainLayout = new VerticalLayout();
        this.mainLayout.setWidth(null);
        this.mainLayout.setHeight(null);
        this.mainLayout.setMargin(false);
        this.mainLayout.setPadding(false);
        this.mainLayout.setSpacing(false);

        HorizontalLayout functionLayout = new HorizontalLayout();
        functionLayout.setWidthFull();
        functionLayout.setHeight(null);
        functionLayout.setMargin(false);
        functionLayout.setPadding(false);
        functionLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        this.mainLayout.add(functionLayout);

        this.favorites = new MenuBar();
        this.favorites.addItem(VaadinIcon.HEART.create());
        this.favorites.addThemeVariants(MenuBarVariant.LUMO_ICON);
        this.favorites.setVisible(false);
        functionLayout.add(this.favorites);

        CallbackDataProvider.FetchCallback<MatchedFilter<T, K>, MatchedFilterQuery> fetcher = query -> query.getFilter()
                .map(matches -> matches.fetch(query.getOffset(), query.getLimit())).orElse(Stream.empty());
        CallbackDataProvider.CountCallback<MatchedFilter<T, K>, MatchedFilterQuery> counter = query -> query.getFilter()
                .map(MatchedFilterQuery::count).orElse(0);
        this.filterInput = new ComboBox<>();
        this.filterInput.setWidthFull();
        this.filterInput.setDataProvider(new CallbackDataProvider<>(fetcher, counter), MatchedFilterQuery::new);
        this.filterInput.setItemLabelGenerator(MatchedFilter::getInput);
        this.filterInput.setRenderer(new ComponentRenderer<>(this::renderAsMatch));
        this.filterInput.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                addFilter(event.getValue());
                notify(Collections.singletonList(event.getValue()), Collections.emptyList(), event.isFromClient());
                this.filterInput.setValue(null);
            }
        });
        functionLayout.add(this.filterInput);

        Button removeAllBtn = new Button(VaadinIcon.CLOSE_SMALL.create());
        removeAllBtn.getElement().getStyle().set("margin-top", "0px");
        removeAllBtn.getElement().getStyle().set("margin-bottom", "0px");
        removeAllBtn.addClickListener(event -> {
            List<MatchedFilter<T, K>> filters = Collections.unmodifiableList(new ArrayList<>(this.filters.keySet()));
            filters.forEach(this::removeFilter);
            notify(Collections.emptyList(), filters, event.isFromClient());
        });
        functionLayout.add(removeAllBtn);

        this.filterLayout = new HorizontalLayout();
        this.filterLayout.setWidthFull();
        this.filterLayout.setHeight(null);
        this.filterLayout.setMargin(false);
        this.filterLayout.setPadding(false);
        this.filterLayout.getElement().getStyle().set("overflow-x", "auto");
        this.filterLayout.getElement().getStyle().set("margin-top", "5px");
        this.mainLayout.add(this.filterLayout);
    }

    @Override
    protected Component initContent() {
        return this.mainLayout;
    }

    private Component renderAsMatch(MatchedFilter<T, K> filter) {
        VerticalLayout matchLayout = new VerticalLayout();
        matchLayout.setWidthFull();
        matchLayout.setHeight(null);
        matchLayout.setMargin(false);
        matchLayout.setPadding(false);
        matchLayout.setSpacing(false);

        HorizontalLayout termLayout = new HorizontalLayout();
        termLayout.setWidthFull();
        termLayout.setHeight(null);
        termLayout.setMargin(false);
        termLayout.setPadding(false);
        matchLayout.add(termLayout);

        NativeLabel termLabel = new NativeLabel(this.termRenderer.apply(filter.getTerm()));
        termLabel.setWidthFull();
        termLayout.getElement().getStyle().set("font-weight", "bold");
        termLayout.add(termLabel);

        if (filter.getMatchCount() != null) {
            NativeLabel countLabel = new NativeLabel(String.valueOf(filter.getMatchCount()));
            countLabel.getElement().getStyle().set("padding-top", "2px");
            countLabel.getElement().getStyle().set("padding-bottom", "2px");
            countLabel.getElement().getStyle().set("padding-right", "5px");
            countLabel.getElement().getStyle().set("padding-left", "5px");
            countLabel.getElement().getStyle().set("background-color", "rgba(128,128,128,0.1)");
            countLabel.getElement().getStyle().set("font-size", "0.75em");
            termLayout.add(countLabel);
            termLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, countLabel);
        }

        String keywordHtml = filter.getKeywordInputs().entrySet().stream()
                .map(entry -> "<b>" + this.keywordRenderer.apply(entry.getKey()) + "</b>: "
                        + "<i>" + entry.getValue() + "</i>")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        Html keywordLabel = new Html("<span>" + keywordHtml + "</span>");
        keywordLabel.getElement().getStyle().set("font-size", "0.70em");
        matchLayout.add(keywordLabel);

        return matchLayout;
    }

    private void addFilter(MatchedFilter<T, K> filter) {
        String badgeLabel = filter.getKeywordInputs().entrySet().stream()
                .map(entry -> this.keywordRenderer.apply(entry.getKey()) + ": " + entry.getValue())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        Button filterBadge = new Button(badgeLabel, VaadinIcon.CLOSE_SMALL.create());
        filterBadge.getElement().getStyle().set("margin-top", "0px");
        filterBadge.getElement().getStyle().set("margin-bottom", "0px");
        filterBadge.addThemeVariants(ButtonVariant.LUMO_SMALL);
        filterBadge.addClickListener(event -> {
            removeFilter(filter);
            notify(Collections.emptyList(), Collections.singletonList(filter), event.isFromClient());
        });

        this.filters.put(filter, filterBadge);
        this.filterLayout.add(filterBadge);
    }

    private void removeFilter(MatchedFilter<T, K> filter) {
        this.filterLayout.remove(this.filters.get(filter));
        this.filters.remove(filter);
    }

    private void notify(List<MatchedFilter<T, K>> added, List<MatchedFilter<T, K>> removed, boolean isFromClient) {
        fireEvent(new MatchedFilterChangedEvent<>(this, isFromClient, added, removed));
    }

    /**
     * Begins a new {@link FilterModificationBuilder} to programmatically apply modifications to the
     * {@link FilterBar}'s current set of {@link MatchedFilter}s.
     *
     * @return A new {@link FilterModificationBuilder}, never null
     */
    public FilterModificationBuilder modifyFilters() {
        return new FilterModificationBuilder();
    }

    /**
     * Begins a new {@link FavoriteModificationBuilder} to programmatically apply modifications to the
     * {@link FilterBar}'s current set of favorites.
     *
     * @return A new {@link FavoriteModificationBuilder}, never null
     */
    public FavoriteModificationBuilder modifyFavorites() {
        return new FavoriteModificationBuilder();
    }

    /**
     * Returns the currently used renderer for displaying terms.
     *
     * @return The renderer, never null
     */
    public Function<T, String> getTermRenderer() {
        return this.termRenderer;
    }

    /**
     * Sets the render for displaying terms.
     *
     * @param termRenderer The renderer; might <b>not</b> be null.
     */
    public void setTermRenderer(Function<T, String> termRenderer) {
        if (termRenderer == null) {
            throw new IllegalArgumentException("Cannot render terms using a null renderer");
        }
        this.termRenderer = termRenderer;
    }

    /**
     * Returns the currently used renderer for displaying keywords.
     *
     * @return The renderer, never null
     */
    public Function<K, String> getKeywordRenderer() {
        return keywordRenderer;
    }

    /**
     * Sets the render for displaying keywords.
     *
     * @param keywordRenderer The renderer; might <b>not</b> be null.
     */
    public void setKeywordRenderer(Function<K, String> keywordRenderer) {
        if (termRenderer == null) {
            throw new IllegalArgumentException("Cannot render keywords using a null renderer");
        }
        this.keywordRenderer = keywordRenderer;
    }

    /**
     * Sets a retriever capable to estimate the amount of matches the given filter would receive.
     * <p>
     * The resulting count is displayed in the filter selection popup.
     *
     * @param matchCountRetriever The estimating {@link Function}; might be null, then no match count is displayed
     */
    public void setMatchCountRetriever(Function<MatchedFilter<T, K>, Long> matchCountRetriever) {
        this.matchCountRetriever = matchCountRetriever;
    }

    /**
     * Sets the mode on how to estimate the amount of matches a filter would receive.
     * <p>
     * Only effective if {@link #setMatchCountRetriever(Function)} is set.
     *
     * @param matchCountMode The mode; might <b>not</b> be null.
     */
    public void setMatchCountMode(MatchCountMode matchCountMode) {
        if (matchCountMode == null) {
            throw new IllegalArgumentException("Cannot set the match count mode to null");
        }
        this.matchCountMode = matchCountMode;
    }

    /**
     * Sets the maximum amount of filters an amount a matches will be retrieved for.
     * <p>
     * Only effective if {@link #setMatchCountRetriever(Function)} is set.
     *
     * @param threshold The amount of filters
     */
    public void setMatchCountThreshold(int threshold) {
        this.matchCountThreshold = threshold;
    }

    /**
     * Sets the placeholder for when no input is entered yet.
     *
     * @param inputPlaceholder The placeholder; might be null
     */
    public void setInputPlaceholder(String inputPlaceholder) {
        this.filterInput.setPlaceholder(inputPlaceholder);
    }

    /**
     * Adds a {@link ComponentEventListener} to listen to {@link MatchedFilterChangedEvent}s.
     *
     * @param listener The listener to add; might <b>not</b> be null.
     * @return A {@link Registration} to remove the listener with, never null
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Registration addFilterChangedListener(ComponentEventListener<MatchedFilterChangedEvent<T, K>> listener) {
        return addListener(MatchedFilterChangedEvent.class, (ComponentEventListener) listener);
    }
}
