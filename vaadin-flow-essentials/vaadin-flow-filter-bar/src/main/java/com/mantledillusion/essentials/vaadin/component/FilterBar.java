package com.mantledillusion.essentials.vaadin.component;

import com.mantledillusion.data.collo.InputAnalyzer;
import com.mantledillusion.data.collo.InputPart;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.shared.Registration;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Component for building the most complex filter constellations on-the-fly in the UI.
 * <p>
 * Uses Collo's {@link InputAnalyzer} for realtime input categorization.
 *
 * @param <G> The {@link Enum} representing the input groups (for example a name, an address, a specific ID, ...).
 * @param <P> The ({@link ValidateableInputPart} implementing) {@link Enum} representing the distinguishable parts of
 *           the input groups (a first name, a last name, a company name, a zip code, ...).
 */
public class FilterBar<G extends Enum<G>, P extends Enum<P> & FilterBar.ValidateableInputPart> extends Composite<Component> implements HasSize {

    /**
     * Extension to {@link InputPart} that is able to deep-validate a {@link String} input that already is matching a
     * group's part by that part's matcher.
     */
    public interface ValidateableInputPart extends InputPart {

        /**
         * Validates the given input.
         *
         * @param value The input; might <b>not</b> be null.
         * @return True if the input is valid, false otherwise.
         */
        default boolean isValid(String value) {
            return true;
        }

        /**
         * Returns a invalidity label to display.
         * <p>
         * Only called after {@link #isValid(String)} returned false.
         *
         * @param value The invalid value; might <b>not</b> be null.
         * @return The label to display, might be null
         */
        default String getInvalidLabel(String value) {
            return null;
        }
    }

    /**
     * {@link ComponentEvent} fired when there either is a {@link Filter} {@link FilterChangeAction#ADDED} or
     * {@link FilterChangeAction#REMOVED} to/from the {@link FilterBar}.
     *
     * @param <G> The {@link Enum} representing the input groups (for example a name, an address, a specific ID, ...).
     * @param <P> The ({@link ValidateableInputPart} implementing) {@link Enum} representing the distinguishable parts of
     *           the input groups (a first name, a last name, a company name, a zip code, ...).
     */
    public static final class FilterChangedEvent<G extends Enum<G>, P extends Enum<P> & FilterBar.ValidateableInputPart> extends ComponentEvent<FilterBar<G, P>> {

        /**
         * Defines the possible types of {@link FilterChangedEvent}s.
         */
        public enum FilterChangeAction {
            ADDED, REMOVED;
        }

        private final Filter<G, P> changedFilter;
        private final FilterChangeAction action;

        private FilterChangedEvent(FilterBar<G, P> source, boolean fromClient, Filter<G, P> changedFilter, FilterChangeAction action) {
            super(source, fromClient);
            this.changedFilter = changedFilter;
            this.action = action;
        }

        /**
         * Returns the {@link Filter} that changed
         *
         * @return The filter, never null
         */
        public Filter<G, P> getChangedFilter() {
            return changedFilter;
        }

        /**
         * Returns the type of action to the {@link Filter} that caused this {@link FilterChangedEvent}.
         *
         * @return The {@link FilterChangeAction}, never null
         */
        public FilterChangeAction getAction() {
            return action;
        }
    }

    /**
     * A {@link Filter} created by a user of a {@link FilterBar}.
     *
     * @param <G> The {@link Enum} representing the input groups (for example a name, an address, a specific ID, ...).
     * @param <P> The ({@link ValidateableInputPart} implementing) {@link Enum} representing the distinguishable parts of
     *           the input groups (a first name, a last name, a company name, a zip code, ...).
     */
    public static final class Filter<G extends Enum<G>, P extends Enum<P> & FilterBar.ValidateableInputPart> {

        private final FilterBar<G, P> parent;
        private final G group;
        private final Map<P, String> parts;

        private Filter(FilterBar<G, P> parent, G group, Map<P, String> parts) {
            this.parent = parent;
            this.group = group;
            this.parts = Collections.unmodifiableMap(parts);
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
         * Returns all parts the input was separated into.
         *
         * @return The parts, never null or empty
         */
        public Set<P> getParts() {
            return this.parts.keySet();
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
            return this.parent.groupRenderer.apply(this.group) + "; " + renderParts();
        }

        private String renderParts() {
            return this.parts.entrySet().stream().
                    map(entry -> this.parent.partRenderer.apply(entry.getKey()) + ": " + entry.getValue()).
                    reduce((a, b) -> a + ", " + b).orElse("");
        }
    }

    /**
     * The value change modes the {@link FilterBar} can operate on.
     */
    public enum ValueChangeMode {

        /**
         * Will cause the text input only to react upon pressing {@link Key#ENTER} or leaving the field.
         * <p>
         * Upon the input being confirmed, the selections for group and parts will be filled and automatically selected
         * if only one possibility applies and opening their menu if there are multiple possibilities.
         * <p>
         * The input dialog will stay open until the user explicitly clicks on the button to signal the input being
         * finished.
         */
        LAZY,

        /**
         * Will cause the text input to react upon every new char entered or deleted.
         * <p>
         * Upon every key stroke, the selections for group and parts will be filled and automatically selected if only
         * one possibility applies.
         * <p>
         * Upon pressing {@link Key#ENTER}, the selections for group and parts will open their menu if there are
         * multiple possibilities.
         * <p>
         * The input dialog will stay open until the user presses {@link Key#ENTER} and there is exactly one group and
         * part selected or the user explicitly clicks on the button to signal the input being finished.
         */
        EAGER;
    }

    private InputAnalyzer<G, P> analyzer;
    private final List<Filter<G, P>> filters = new ArrayList<>();
    private final Set<ComponentEventListener<FilterChangedEvent<G, P>>> listeners = Collections.newSetFromMap(new IdentityHashMap<>());

    private final HorizontalLayout filterLayout;
    private final HorizontalLayout mainLayout;

    private ValueChangeMode valueChangeMode = ValueChangeMode.EAGER;
    private String dialogWidth;
    private Function<G, String> groupRenderer = String::valueOf;
    private Function<P, String> partRenderer = String::valueOf;
    private String inputPlaceholder;
    private String groupPlaceholder;
    private String partPlaceholder;

    /**
     * Advanced constructor.
     *
     * @param analyzer The {@link InputAnalyzer} to use; might <b>not</b> be null;
     */
    public FilterBar(InputAnalyzer<G, P> analyzer) {
        if (analyzer == null) {
            throw new IllegalArgumentException("Cannot operate on a null analyzer");
        }

        this.analyzer = analyzer;

        Button filterBtn = new Button(VaadinIcon.FILTER.create());
        filterBtn.addClickListener(event -> openInput());

        this.filterLayout = new HorizontalLayout();
        this.filterLayout.setWidthFull();
        this.filterLayout.setHeight(null);
        this.filterLayout.getElement().setAttribute("overflow-x", "auto");

        this.mainLayout = new HorizontalLayout(filterBtn, this.filterLayout);
        this.mainLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, this.filterLayout);
    }

    @Override
    protected Component initContent() {
        return this.mainLayout;
    }

    private void openInput() {
        TextField inputField = new TextField();
        inputField.setWidthFull();
        inputField.setHeight(null);
        inputField.setValueChangeMode(this.valueChangeMode == ValueChangeMode.EAGER ?
                com.vaadin.flow.data.value.ValueChangeMode.EAGER : com.vaadin.flow.data.value.ValueChangeMode.LAZY);
        inputField.setPlaceholder(this.inputPlaceholder);

        ComboBox<G> groupSelect = new ComboBox<>();
        groupSelect.setWidthFull();
        groupSelect.setHeight(null);
        groupSelect.setItemLabelGenerator(this.groupRenderer::apply);
        groupSelect.setRenderer(new TextRenderer<>(this.groupRenderer::apply));
        groupSelect.setPlaceholder(this.groupPlaceholder);
        groupSelect.setReadOnly(true);

        ComboBox<Filter<G, P>> partingSelect = new ComboBox<>();
        partingSelect.setWidthFull();
        partingSelect.setHeight(null);
        partingSelect.setItemLabelGenerator(Filter::renderParts);
        partingSelect.setRenderer(new TextRenderer<>(Filter::renderParts));
        partingSelect.setPlaceholder(this.partPlaceholder);
        partingSelect.setReadOnly(true);

        Button ok = new Button(VaadinIcon.CHECK.create());
        ok.addThemeVariants(ButtonVariant.LUMO_SMALL);
        ok.setEnabled(false);

        inputField.addValueChangeListener(event -> {
            Set<G> groups = inputField.getValue() == null ? Collections.emptySet() : this.analyzer.matching(inputField.getValue());
            groupSelect.setReadOnly(groups.isEmpty());
            groupSelect.setItems(groups);
            groupSelect.setValue(groups.size() == 1 ? groups.iterator().next() : null);
            if (this.valueChangeMode == ValueChangeMode.LAZY && groups.size() > 1) {
                groupSelect.setOpened(true);
            }
        });
        inputField.addKeyPressListener(Key.ENTER, event -> {
            if (this.valueChangeMode == ValueChangeMode.EAGER) {
                if (((ListDataProvider<G>) groupSelect.getDataProvider()).getItems().size() > 1) {
                    groupSelect.setOpened(true);
                } else if (((ListDataProvider<Filter<G, P>>) partingSelect.getDataProvider()).getItems().size() > 1) {
                    groupSelect.setOpened(true);
                } else if (groupSelect.getValue() != null && partingSelect.getValue() != null) {
                    ok.clickInClient();
                }
            }
        });
        groupSelect.addValueChangeListener(event -> {
            List<Filter<G, P>> filters = event.getValue() == null ? Collections.emptyList() : this.analyzer.
                    analyzeForGroup(inputField.getValue(), event.getValue()).stream().
                    map(parts -> new Filter<>(this, groupSelect.getValue(), parts)).
                    collect(Collectors.toList());
            partingSelect.setReadOnly(filters.isEmpty());
            partingSelect.setItems(filters);
            partingSelect.setValue(filters.size() == 1 ? filters.iterator().next() : null);
            if (this.valueChangeMode == ValueChangeMode.LAZY && filters.size() > 1) {
                partingSelect.setOpened(true);
            }
        });
        partingSelect.addValueChangeListener(event -> {
            boolean invalid = false;
            String invalidMsg = null;
            if (event.getValue() != null) {
                invalid = !event.getValue().parts.entrySet().stream().
                        allMatch(entry -> entry.getKey().isValid(entry.getValue()));
                invalidMsg = event.getValue().parts.entrySet().stream().
                        filter(entry -> !entry.getKey().isValid(entry.getValue())).
                        map(entry -> entry.getKey().getInvalidLabel(entry.getValue())).
                        findFirst().
                        orElse(null);
            }
            ok.setEnabled(!invalid);
            inputField.setInvalid(invalid);
            inputField.setErrorMessage(invalidMsg);
        });

        HorizontalLayout footerLayout = new HorizontalLayout(partingSelect, ok);
        footerLayout.setWidthFull();
        footerLayout.setHeight(null);
        footerLayout.setPadding(false);
        footerLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, ok);

        VerticalLayout dialogLayout = new VerticalLayout(inputField, groupSelect, footerLayout);
        dialogLayout.setWidthFull();
        dialogLayout.setHeight(null);
        dialogLayout.setPadding(false);

        Dialog dialog = new Dialog(dialogLayout);
        dialog.setWidth(this.dialogWidth);
        dialog.setHeight(null);
        dialog.setCloseOnOutsideClick(true);
        dialog.setCloseOnEsc(true);

        ok.addClickListener(event -> {
            addFilter(partingSelect.getValue(), event.isFromClient());
            dialog.close();
        });

        dialog.open();
    }

    private void addFilter(Filter<G, P> filter, boolean isFromClient) {
        Button filterBadge = new Button(filter.toString(), VaadinIcon.CLOSE_SMALL.create());
        filterBadge.addThemeVariants(ButtonVariant.LUMO_SMALL);
        filterBadge.addClickListener(event -> {
            this.filters.remove(filter);
            this.filterLayout.remove(filterBadge);
            dispatch(filter, FilterChangedEvent.FilterChangeAction.REMOVED, event.isFromClient());
        });

        this.filters.add(filter);
        this.filterLayout.add(filterBadge);
        dispatch(filter, FilterChangedEvent.FilterChangeAction.ADDED, isFromClient);
    }

    private void dispatch(Filter<G, P> filter, FilterChangedEvent.FilterChangeAction action, boolean isFromClient) {
        FilterChangedEvent<G, P> filterChangedEvent = new FilterChangedEvent<>(this, isFromClient, filter, action);
        this.listeners.forEach(l -> l.onComponentEvent(filterChangedEvent));
    }

    /**
     * Sets the mode of how the filter input reacts to changed input.
     *
     * @see ValueChangeMode
     * @param valueChangeMode The mode to set; might <b>not</b> be null.
     */
    public void setValueChangeMode(ValueChangeMode valueChangeMode) {
        if (valueChangeMode == null) {
            throw new IllegalArgumentException("Cannot set a null value change mode");
        }
        this.valueChangeMode = valueChangeMode;
    }

    /**
     * Sets this input dialog's width to a fixed pixel value.
     *
     * @see Dialog#setWidth(String)
     * @param dialogWidth The width; might be null.
     */
    public void setDialogWidth(String dialogWidth) {
        this.dialogWidth = dialogWidth;
    }

    /**
     * Sets the render for displaying groups.
     *
     * @param groupRenderer The renderer; might <b>not</b> be null.
     */
    public void setGroupRenderer(Function<G, String> groupRenderer) {
        if (groupRenderer == null) {
            throw new IllegalArgumentException("Cannot render groups using a null renderer");
        }
        this.groupRenderer = groupRenderer;
    }

    /**
     * Sets the render for displaying parts.
     *
     * @param partRenderer The renderer; might <b>not</b> be null.
     */
    public void setPartRenderer(Function<P, String> partRenderer) {
        if (groupRenderer == null) {
            throw new IllegalArgumentException("Cannot render parts using a null renderer");
        }
        this.partRenderer = partRenderer;
    }

    /**
     * Sets the place holder for when no input is entered yet.
     *
     * @param inputPlaceholder The place holder; might be null
     */
    public void setInputPlaceholder(String inputPlaceholder) {
        this.inputPlaceholder = inputPlaceholder;
    }

    /**
     * Sets the place holder for when no group is selected yet.
     *
     * @param groupPlaceholder The place holder; might be null
     */
    public void setGroupPlaceholder(String groupPlaceholder) {
        this.groupPlaceholder = groupPlaceholder;
    }

    /**
     * Sets the place holder for when no part is selected yet.
     *
     * @param partPlaceholder The place holder; might be null
     */
    public void setPartPlaceholder(String partPlaceholder) {
        this.partPlaceholder = partPlaceholder;
    }

    /**
     * Adds a {@link ComponentEventListener} to listen to {@link FilterChangedEvent}s.
     *
     * @param listener The listener to add; might <b>not</b> be null.
     * @return A {@link Registration} to remove the listener with, never null
     */
    public Registration addFilterChangedListener(ComponentEventListener<FilterChangedEvent<G, P>> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Cannot add a null listener");
        }
        this.listeners.add(listener);
        return () -> this.listeners.remove(listener);
    }
}
