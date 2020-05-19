package com.mantledillusion.essentials.vaadin.component;

import com.mantledillusion.data.collo.InputAnalyzer;
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
 * @param <P> The ({@link MatchedFilterInputPart} implementing) {@link Enum} representing the distinguishable parts of
 *           the input groups (a first name, a last name, a company name, a zip code, ...).
 */
public class FilterBar<G extends Enum<G>, P extends Enum<P> & MatchedFilterInputPart> extends Composite<Component> implements HasSize {

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
    private final List<MatchedFilter<G, P>> filters = new ArrayList<>();

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
        filterBtn.getElement().getStyle().set("margin-top", "0px");
        filterBtn.getElement().getStyle().set("margin-bottom", "0px");
        filterBtn.addClickListener(event -> openInput());

        this.filterLayout = new HorizontalLayout();
        this.filterLayout.setWidthFull();
        this.filterLayout.setHeight(null);
        this.filterLayout.getElement().getStyle().set("overflow-x", "auto");

        this.mainLayout = new HorizontalLayout(filterBtn, this.filterLayout);
        this.mainLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, this.filterLayout);
        this.mainLayout.getElement().getStyle().set("background-color", "rgba(128,128,128,0.05)");
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

        ComboBox<MatchedFilter<G, P>> partingSelect = new ComboBox<>();
        partingSelect.setWidthFull();
        partingSelect.setHeight(null);
        partingSelect.setItemLabelGenerator(MatchedFilter::toStringParts);
        partingSelect.setRenderer(new TextRenderer<>(MatchedFilter::toStringParts));
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
                } else if (((ListDataProvider<MatchedFilter<G, P>>) partingSelect.getDataProvider()).getItems().size() > 1) {
                    groupSelect.setOpened(true);
                } else if (groupSelect.getValue() != null && partingSelect.getValue() != null) {
                    ok.clickInClient();
                }
            }
        });
        groupSelect.addValueChangeListener(event -> {
            List<MatchedFilter<G, P>> filters = event.getValue() == null ? Collections.emptyList() : this.analyzer.
                    analyzeForGroup(inputField.getValue(), event.getValue()).stream().
                    map(parts -> new MatchedFilter<>(groupSelect.getValue(), parts, this.groupRenderer, this.partRenderer)).
                    collect(Collectors.toList());
            partingSelect.setReadOnly(filters.isEmpty());
            partingSelect.setItems(filters);
            partingSelect.setValue(filters.size() == 1 ? filters.iterator().next() : null);
            if (this.valueChangeMode == ValueChangeMode.LAZY && filters.size() > 1) {
                partingSelect.setOpened(true);
            }
        });
        partingSelect.addValueChangeListener(event -> {
            boolean invalid = true;
            String invalidMsg = null;
            if (event.getValue() != null) {
                invalid = !event.getValue().getParts().stream().
                        allMatch(part -> part.isValid(event.getValue().getPart(part)));
                invalidMsg = event.getValue().getParts().stream().
                        filter(part -> !part.isValid(event.getValue().getPart(part))).
                        map(part -> part.getInvalidLabel(event.getValue().getPart(part))).
                        findFirst().
                        orElse(null);
                ok.setEnabled(!invalid);
            } else {
                ok.setEnabled(false);
            }
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

    private void addFilter(MatchedFilter<G, P> filter, boolean isFromClient) {
        Button filterBadge = new Button(filter.toString(), VaadinIcon.CLOSE_SMALL.create());
        filterBadge.addThemeVariants(ButtonVariant.LUMO_SMALL);
        filterBadge.addClickListener(event -> {
            this.filters.remove(filter);
            this.filterLayout.remove(filterBadge);
            fireEvent(new MatchedFilterChangedEvent<>(this, isFromClient, filter, MatchedFilterChangedEvent.FilterChangeAction.REMOVED));
        });

        this.filters.add(filter);
        this.filterLayout.add(filterBadge);
        fireEvent(new MatchedFilterChangedEvent<>(this, isFromClient, filter, MatchedFilterChangedEvent.FilterChangeAction.ADDED));
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
     * Returns the currently used renderer for displaying groups.
     *
     * @return The renderer, never null
     */
    public Function<G, String> getGroupRenderer() {
        return this.groupRenderer;
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
     * Returns the currently used renderer for displaying parts.
     *
     * @return The renderer, never null
     */
    public Function<P, String> getPartRenderer() {
        return partRenderer;
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
     * Adds a {@link ComponentEventListener} to listen to {@link MatchedFilterChangedEvent}s.
     *
     * @param listener The listener to add; might <b>not</b> be null.
     * @return A {@link Registration} to remove the listener with, never null
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Registration addFilterChangedListener(ComponentEventListener<MatchedFilterChangedEvent<G, P>> listener) {
        return addListener(MatchedFilterChangedEvent.class, (ComponentEventListener) listener);
    }
}
