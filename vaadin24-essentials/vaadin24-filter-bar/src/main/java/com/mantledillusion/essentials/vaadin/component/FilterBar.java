package com.mantledillusion.essentials.vaadin.component;

import com.mantledillusion.data.collo.TermAnalyzer;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.popover.PopoverVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * Component for building the most complex filter constellations on-the-fly in the UI.
 * <p>
 * Uses Collo's {@link TermAnalyzer} for realtime input recognition.
 *
 * @param <T> The {@link MatchedTerm} representing the terms (for example a name, an address, a specific ID, ...)
 * @param <K> The ({@link MatchedKeyword} representing the distinguishable keywords of a term (a first name, a last name, a company name, a zip code, ...)
 */
public class FilterBar<T extends MatchedTerm<K>, K extends MatchedKeyword> extends Composite<Component> implements HasSize {

    private final TermAnalyzer<T, K> analyzer;
    private final Map<MatchedFilter<T, K>, Component> filters = new IdentityHashMap<>();

    private final VerticalLayout mainLayout;
    private final TextField filterInput;
    private final Button filterInputClearButton;
    private final HorizontalLayout functionLayout;
    private final HorizontalLayout filterLayout;
    private final Button filterOperatorButton;
    private final Button filterClearButton;
    private final Button filterScrollLeftButton;
    private final Button filterScrollRightButton;

    private MatchedFilterOperator filterOperator = MatchedFilterOperator.AND;
    private int filterScrollDistance = 50;
    private Function<T, String> termRenderer = String::valueOf;
    private Function<K, String> keywordRenderer = String::valueOf;
    private IntFunction<String> thresholdRenderer = count -> "+"+count;

    /**
     * Default constructor.
     * <p>
     * Initializes the {@link FilterBar} with an empty {@link TermAnalyzer}.
     */
    public FilterBar() {
        this(new TermAnalyzer<>());
    }

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

        var matchedFilterLayout = new VerticalLayout();
        matchedFilterLayout.setWidth(null);
        matchedFilterLayout.setHeight(null);
        matchedFilterLayout.setPadding(false);
        matchedFilterLayout.setMargin(false);
        matchedFilterLayout.setSpacing(false);

        Popover matchedFilterPopover = new Popover(matchedFilterLayout);
        matchedFilterPopover.setWidth(null);
        matchedFilterPopover.setPosition(PopoverPosition.BOTTOM_START);
        matchedFilterPopover.addThemeVariants(PopoverVariant.ARROW);

        var matchedFilterExamples = new HashMap<T, VerticalLayout>();

        this.filterInputClearButton = buildFilterButton(VaadinIcon.TRASH, null);

        this.filterInput = new TextField();
        this.filterInput.setWidthFull();
        this.filterInput.setPrefixComponent(VaadinIcon.SEARCH.create());
        this.filterInput.setValueChangeMode(ValueChangeMode.LAZY);
        this.filterInput.addKeyDownListener(Key.ARROW_DOWN, event -> {
            // STREAM CHILDREN OF MATCHED FILTER LAYOUT
            matchedFilterLayout.getChildren()
                    // STREAM CHILDREN OF TERM LAYOUTS
                    .flatMap(Component::getChildren)
                    // FIND FIRST FILTER BUTTON
                    .filter(Button.class::isInstance)
                    .findFirst()
                    // FOCUS BUTTON
                    .map(Focusable.class::cast)
                    .ifPresent(Focusable::focus);
        });
        this.filterInput.addValueChangeListener(event -> {
            matchedFilterLayout.removeAll();

            if (StringUtils.isEmpty(event.getValue())) {
                matchedFilterExamples.values().forEach(matchedFilterLayout::add);
            } else {
                 FilterBar.this.analyzer.analyze(event.getValue())
                        .entrySet().stream()
                        .map(entry -> buildTermMatchLayout(entry.getKey(), entry.getValue()))
                        .forEach(matchedFilterLayout::add);

                if (matchedFilterLayout.getChildren().findAny().isEmpty()) {
                    matchedFilterLayout.add(this.filterInputClearButton);
                }
            }

            if (event.isFromClient()) {
                matchedFilterPopover.open();
            } else {
                matchedFilterPopover.close();
            }
        });
        this.mainLayout.add(this.filterInput);
        matchedFilterPopover.setTarget(this.filterInput);

        this.analyzer.addListener((term, keywordAnalyzer, added) -> {
            if (added) {
                var exampleLayout = this.buildTermExampleLayout(term, matchedFilterPopover);
                matchedFilterExamples.put(term, exampleLayout);
                if (this.filterInput.isEmpty()) {
                    matchedFilterLayout.add(exampleLayout);
                }
            } else {
                var exampleLayout = matchedFilterExamples.remove(term);
                if (this.filterInput.isEmpty()) {
                    matchedFilterLayout.remove(exampleLayout);
                }
            }
        });

        this.filterInputClearButton.addClickListener(evt -> {
            this.filterInput.setValue(this.filterInput.getEmptyValue());
            matchedFilterPopover.open();
            this.filterInput.focus();
        });

        this.functionLayout = new HorizontalLayout();
        this.functionLayout.setWidthFull();
        this.functionLayout.setHeight(null);
        this.functionLayout.setMargin(false);
        this.functionLayout.setPadding(false);
        this.functionLayout.setSpacing(false);
        this.functionLayout.setVisible(false);
        this.functionLayout.getStyle().set("margin-top", "5px");
        this.mainLayout.add(functionLayout);

        this.filterLayout = new HorizontalLayout();
        this.filterLayout.setWidthFull();
        this.filterLayout.setHeight(null);
        this.filterLayout.setMargin(false);
        this.filterLayout.setPadding(false);
        this.filterLayout.setSpacing(true);
        this.filterLayout.getStyle().set("margin-left", "5px");
        this.filterLayout.getStyle().set("overflow-x", "auto");
        this.filterLayout.getStyle().set("scrollbar-width", "none");
        this.filterLayout.getStyle().set("background-size", "10px 10px");
        this.filterLayout.getStyle().set("background-position", "0px 0px, 5px 5px");
        this.filterLayout.getStyle().set("background-image", "radial-gradient(rgba(128, 128, 128, 0.1) 2px, transparent 0px), radial-gradient(rgba(128, 128, 128, 0.1) 2px, transparent 0px)");

        this.filterOperatorButton = buildFilterButton(VaadinIcon.LINK, null);
        this.filterOperatorButton.addClickListener(event -> {
            updateOperator(this.filterOperator == MatchedFilterOperator.AND ? MatchedFilterOperator.OR : MatchedFilterOperator.AND, event.isFromClient());
        });
        this.functionLayout.add(this.filterOperatorButton);

        this.filterClearButton = buildFilterButton(VaadinIcon.CLOSE_SMALL, null);
        this.filterClearButton.setVisible(false);
        this.filterClearButton.getStyle().set("margin-left", "5px");
        this.filterClearButton.addClickListener(event -> {
            Set<MatchedFilter<T, K>> removed = Set.copyOf(this.filters.keySet());
            removed.forEach(this::removeFilter);
            notify(Collections.emptySet(), removed, this.filterOperator, event.isFromClient());
        });
        this.functionLayout.add(this.filterClearButton);

        this.filterScrollLeftButton = buildFilterButton(VaadinIcon.ANGLE_LEFT, null);
        this.filterScrollLeftButton.getStyle().set("margin-left", "5px");
        this.filterScrollLeftButton.addClickListener(event -> {
            this.filterLayout.getElement().executeJs("this.scrollBy(-"+this.filterScrollDistance+", 0);");
        });
        this.functionLayout.add(this.filterScrollLeftButton);

        this.functionLayout.add(this.filterLayout);

        this.filterScrollRightButton = buildFilterButton(VaadinIcon.ANGLE_RIGHT, null);
        this.filterScrollRightButton.getStyle().set("margin-left", "5px");
        this.filterScrollRightButton.addClickListener(event -> {
            this.filterLayout.getElement().executeJs("this.scrollBy("+this.filterScrollDistance+", 0);");
        });
        this.functionLayout.add(this.filterScrollRightButton);
    }

    @Override
    protected Component initContent() {
        return this.mainLayout;
    }

    private VerticalLayout buildTermExampleLayout(T term, Popover matchedFilterPopover) {
        var layout = buildTermLayout(term);

        for (KeywordMatch<K> example: Optional.ofNullable(term.getFavorites()).orElseGet(Collections::emptyList)) {
            var button = buildKeywordMatchButton(VaadinIcon.STAR, renderKeywords(example.getKeywords()));
            button.addClickListener(event -> {
                var added = new MatchedFilter<>(term, example.getKeywords());
                var removed = add(added);
                notify(Collections.singleton(added), removed, this.filterOperator, event.isFromClient());
                matchedFilterPopover.close();
            });
            layout.add(button);
        }

        for (KeywordMatch<K> example: Optional.ofNullable(term.getExamples()).orElseGet(Collections::emptyList)) {
            var button = buildKeywordMatchButton(VaadinIcon.CHECK, renderKeywords(example.getKeywords()));
            button.setEnabled(false);
            layout.add(button);
        }

        return layout;
    }

    private VerticalLayout buildTermMatchLayout(T term, List<LinkedHashMap<K, String>> keywordSets) {
        var layout = buildTermLayout(term);

        var threshold = Math.max(term.displayThreshold(), 1);
        addKeywordMatchButtons(layout, term, keywordSets.subList(0, Math.min(keywordSets.size(), threshold)));

        if (term.displayThreshold() < keywordSets.size()) {
            var button = buildKeywordMatchButton(VaadinIcon.LEVEL_DOWN, this.thresholdRenderer.apply(keywordSets.size()-threshold));
            button.addClickListener(event -> {
                layout.remove(button);
                addKeywordMatchButtons(layout, term, keywordSets.subList(threshold, keywordSets.size()));
            });
            layout.add(button);
        }

        return layout;
    }

    private void addKeywordMatchButtons(VerticalLayout termLayout, T term, List<LinkedHashMap<K, String>> keywordSets) {
        for (var keywords: keywordSets) {
            var button = buildKeywordMatchButton(VaadinIcon.PLUS, renderKeywords(keywords));
            button.addClickListener(event -> {
                var added = new MatchedFilter<>(term, keywords);
                var removed = add(added);
                notify(Collections.singleton(added), removed, this.filterOperator, event.isFromClient());
                this.filterInput.setValue(this.filterInput.getEmptyValue());
            });
            termLayout.add(button);
        }
    }

    private Button buildKeywordMatchButton(VaadinIcon icon, String label) {
        var button = buildFilterButton(icon, label);
        button.getStyle().set("margin-left", "1em");
        button.getStyle().set("margin-bottom", "5px");
        return button;
    }

    private VerticalLayout buildTermLayout(T term) {
        var layout = new VerticalLayout();
        layout.setWidth(null);
        layout.setHeight(null);
        layout.setMargin(false);
        layout.setPadding(false);
        layout.setSpacing(false);

        var termLabel = new NativeLabel(this.termRenderer.apply(term));
        termLabel.setWidth(null);
        termLabel.getStyle().set("font-weight", "bold");
        termLabel.getStyle().set("color", "var(--lumo-contrast-50pct)");
        layout.add(termLabel);

        return layout;
    }

    private Button buildFilterButton(VaadinIcon icon, String label) {
        var button = new Button(label, Optional.ofNullable(icon).map(VaadinIcon::create).orElse(null));
        button.getElement().getStyle().set("margin-top", "0px");
        button.getElement().getStyle().set("margin-bottom", "0px");
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        return button;
    }

    private String renderKeywords(Map<K, String> keywords) {
        return keywords.entrySet().stream()
                .map(entry -> this.keywordRenderer.apply(entry.getKey()) + ": " + entry.getValue())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private void updateOperator(MatchedFilterOperator operator, boolean isFromClient) {
        var previousOperator = this.filterOperator;
        this.filterOperator = operator;
        this.filterOperatorButton.setIcon(operator == MatchedFilterOperator.AND
                ? VaadinIcon.LINK.create()
                : VaadinIcon.UNLINK.create());

        if (!this.filters.isEmpty()) {
            notify(Collections.emptySet(), Collections.emptySet(), previousOperator, isFromClient);
        }
    }

    private void updateFunctionVisibility() {
        this.functionLayout.setVisible(!this.filters.isEmpty());
        this.filterClearButton.setVisible(this.filters.size() > 1);
    }

    private void notify(Set<MatchedFilter<T, K>> addedFilters, Set<MatchedFilter<T, K>> removedFilters, MatchedFilterOperator previousOperator, boolean isFromClient) {
        fireEvent(new MatchedFilterChangedEvent<>(this, isFromClient, addedFilters, removedFilters, getFilters(), previousOperator, this.filterOperator));
    }

    /**
     * Returns the used {@link TermAnalyzer}.
     *
     * @return The analyzer, never null
     */
    public TermAnalyzer<T, K> getAnalyzer() {
        return analyzer;
    }

    /**
     * Returns an unmodifiable view of all filters.
     *
     * @return A set of all filters, never null.
     */
    public Set<MatchedFilter<T, K>> getFilters() {
        return Collections.unmodifiableSet(this.filters.keySet());
    }

    /**
     * Adds a new {@link MatchedFilter} using the given term and keywords.
     * <p>
     * Any already added filters will be evaluated with {@link MatchedTerm#isCombinable(Map, MatchedFilter)} for their
     * combinability with the new filter and removed if they are not.
     *
     * @param term The term of the filter; might <b>not</b> be null.
     * @param keywords The keywords of the filter; might <b>not</b> be null.
     * @return The added filter, never null
     */
    public MatchedFilter<T, K> addFilter(T term, KeywordMatch<K> keywords) {
        if (term == null) {
            throw new IllegalArgumentException("cannot add a filter for a null term");
        } else if (keywords == null) {
            throw new IllegalArgumentException("Cannot add a filter with null keywords");
        }

        var added = new MatchedFilter<>(term, keywords.getKeywords());
        var removed = add(added);
        notify(Collections.singleton(added), removed, this.filterOperator, false);
        return added;
    }

    private Set<MatchedFilter<T, K>> add(MatchedFilter<T, K> filter) {
        var removed = removeFilters(other -> !filter.getTerm().isCombinable(filter.getKeywordInputs(), other)
                || !other.getTerm().isCombinable(other.getKeywordInputs(), filter));

        var label = renderKeywords(filter.getKeywordInputs());

        var button = buildFilterButton(VaadinIcon.CLOSE_SMALL, label);
        button.addClickListener(event -> {
            removeFilter(filter);
            notify(Collections.emptySet(), Collections.singleton(filter), this.filterOperator, event.isFromClient());
        });

        this.filters.put(filter, button);
        this.filterLayout.add(button);

        updateFunctionVisibility();

        return removed;
    }

    /**
     * Removes the given filter.
     *
     * @param filter The filter to remove; might <b>not</b> be null or unknown.
     */
    public void removeFilter(MatchedFilter<T, K> filter) {
        remove(filter);
        notify(Collections.emptySet(), Collections.singleton(filter), this.filterOperator, false);
    }

    private void remove(MatchedFilter<T, K> filter) {
        if (filter == null) {
            throw new IllegalArgumentException("Cannot remove a null filter");
        } else if (!this.filters.containsKey(filter)) {
            throw new IllegalArgumentException("Cannot remove bn unknown filter");
        }

        this.filterLayout.remove(this.filters.get(filter));
        this.filters.remove(filter);

        updateFunctionVisibility();
    }

    /**
     * Removes all filters of the given term.
     *
     * @param term The term whose filters to remove; might <b>not</b> be null.
     * @return A set of all filters removed, never null
     */
    public Set<MatchedFilter<T, K>> removeFilters(T term) {
        if (term == null) {
            throw new IllegalArgumentException("Cannot remove filters with a null term");
        }

        return removeFilters(filter -> Objects.equals(filter.getTerm(), term));
    }

    /**
     * Removes all filters matching the given predictae.
     *
     * @param predicate The predicate to match; might <b>not</b> be null.
     * @return A set of all filters removed, never null
     */
    public Set<MatchedFilter<T, K>> removeFilters(Predicate<MatchedFilter<T, K>> predicate) {
        if (predicate == null) {
            throw new IllegalArgumentException("Cannot remove filters using a null predicate");
        }

        var removed = remove(predicate);

        notify(Collections.emptySet(), removed, this.filterOperator, false);

        return removed;
    }

    /**
     * Removes all filters.
     *
     * @return A set of all filters removed, never null
     */
    public Set<MatchedFilter<T, K>> clearFilters() {
        return removeFilters(filter -> true);
    }

    private Set<MatchedFilter<T, K>> remove(Predicate<MatchedFilter<T, K>> predicate) {
        var removed = this.filters.keySet().stream()
                .filter(predicate)
                .collect(Collectors.toUnmodifiableSet());

        removed.forEach(this::remove);

        return removed;
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
     * Sets the text displayed to clear the input with if no match was found.
     *
     * @param text The text; might be null.
     */
    public void setNoMatchLabel(String text) {
        this.filterInputClearButton.setText(text);
    }

    /**
     * Returns the currently used renderer for displaying terms.
     *
     * @return The renderer, never null
     */
    public Function<T, String> getTermLabelRenderer() {
        return this.termRenderer;
    }

    /**
     * Sets the render for displaying terms.
     *
     * @param termRenderer The renderer; might <b>not</b> be null.
     */
    public void setTermLabelRenderer(Function<T, String> termRenderer) {
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
    public Function<K, String> getKeywordLabelRenderer() {
        return keywordRenderer;
    }

    /**
     * Sets the render for displaying keywords.
     *
     * @param keywordRenderer The renderer; might <b>not</b> be null.
     */
    public void setKeywordLabelRenderer(Function<K, String> keywordRenderer) {
        if (keywordRenderer == null) {
            throw new IllegalArgumentException("Cannot render keywords using a null renderer");
        }
        this.keywordRenderer = keywordRenderer;
    }

    /**
     * Returns the currently used renderer for displaying thresholds.
     *
     * @return The renderer, never null
     */
    public IntFunction<String> getThresholdLabelRenderer() {
        return thresholdRenderer;
    }

    /**
     * Sets the render for displaying thresholds.
     * <p>
     * The renderer is used when a term's match count exceeds {@link MatchedTerm#displayThreshold()}.
     *
     * @param thresholdRenderer The renderer; might <b>not</b> be null.
     */
    public void setThresholdLabelRenderer(IntFunction<String> thresholdRenderer) {
        if (thresholdRenderer == null) {
            throw new IllegalArgumentException("Cannot render thresholds using a null renderer");
        }
        this.thresholdRenderer = thresholdRenderer;
    }

    /**
     * Returns the logic operator combining the current filters.
     *
     * @return The operator, never null
     */
    public MatchedFilterOperator getOperator() {
        return this.filterOperator;
    }

    /**
     * Sets the operator combining the current filters.
     * <p>
     * Triggers a {@link MatchedFilterChangedEvent} if there currently are filters.
     *
     * @param operator The operator to set; might <b>not</b> be null.
     */
    public void setOperator(MatchedFilterOperator operator) {
        if (operator == null) {
            throw new IllegalArgumentException("Cannot set a null operator");
        }

        updateOperator(operator, false);
    }

    /**
     * Returns whether the user can switch the operator.
     *
     * @return True if the user can switch the operator, false otherwise
     */
    public boolean isOperatorEnabled() {
        return this.filterOperatorButton.isEnabled();
    }

    /**
     * Sets whether the user can switch the operator.
     *
     * @param enabled True if the user can switch the operator, false otherwise
     */
    public void enableOperator(boolean enabled) {
        this.filterOperatorButton.setEnabled(enabled);
    }

    /**
     * Returns whether the user can see the operator.
     *
     * @return True if the user can see the operator, false otherwise
     */
    public boolean isOperatorVisible() {
        return this.filterOperatorButton.isVisible();
    }

    /**
     * Returns whether the user can see the operator.
     *
     * @param visible True if the user can see the operator, false otherwise
     */
    public void setOperatorVisible(boolean visible) {
        this.filterOperatorButton.setVisible(visible);
    }

    /**
     * Sets a tooltip text for the operator.
     *
     * @param text The text, might be null
     */
    public void setOperatorTooltip(String text) {
        this.filterOperatorButton.setTooltipText(text);
    }

    /**
     * Sets a tooltip text for clearing all filters.
     *
     * @param text The text, might be null
     */
    public void setClearTooltip(String text) {
        this.filterClearButton.setTooltipText(text);
    }

    /**
     * Returns the scroll distance in pixels on every scroll left/right click.
     *
     * @return The distance in pixels, > 0
     */
    public int getScrollDistance() {
        return this.filterScrollDistance;
    }

    /**
     * Sets the scroll distance in pixels on every scroll left/right click.
     *
     * @param distance The distance to set, in pixels; might <b>not</b> be <1
     */
    public void setScrollDistance(int distance) {
        if (distance < 1) {
            throw new IllegalArgumentException("Cannot scroll for a distance smaller than 1 px");
        }

        this.filterScrollDistance = distance;
    }

    /**
     * Sets a tooltip text for scrolling left on the filters.
     *
     * @param text The text, might be null
     */
    public void setScrollLeftTooltip(String text) {
        this.filterScrollLeftButton.setTooltipText(text);
    }

    /**
     * Sets a tooltip text for scrolling right on the filters.
     *
     * @param text The text, might be null
     */
    public void setScrollRightTooltip(String text) {
        this.filterScrollRightButton.setTooltipText(text);
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
