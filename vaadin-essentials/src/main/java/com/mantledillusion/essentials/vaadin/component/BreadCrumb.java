package com.mantledillusion.essentials.vaadin.component;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.mantledillusion.essentials.vaadin.component.common.TextRenderer;
import com.vaadin.server.Resource;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;

/**
 * {@link Composite} {@link Component} that is able to display instances of a
 * specifiable type as crumbs on a breadcrumb.
 *
 * @param <T>
 *            The element type of the crumbs.
 */
public final class BreadCrumb<T> extends Composite {

	private static final long serialVersionUID = 1L;

	/**
	 * Defines the possible placements of the indicator in between elements.
	 */
	public static enum IndicatorPlacement {
		BEFORE, AFTER;
	}

	/**
	 * Defines the parts of a single element.
	 */
	public static enum CrumbPart {
		INDICATOR, TEXT;
	}

	/**
	 * Defines the states an element can be in.
	 */
	public static enum CrumbState {
		SELECTED, UNSELECTED, BOTH;
	}

	private final class Crumb extends HorizontalLayout {

		private static final long serialVersionUID = 1L;

		private final Image icon;
		private final Label label;
		private T payload;

		public Crumb(T element) {
			setSpacing(true);
			setMargin(false);

			this.icon = new Image(null, BreadCrumb.this.elementIcon);
			this.icon.addStyleNames(BreadCrumb.this.styles.get(CrumbPart.INDICATOR).get(CrumbState.UNSELECTED).stream()
					.toArray(String[]::new));
			this.icon.addStyleNames(BreadCrumb.this.styles.get(CrumbPart.INDICATOR).get(CrumbState.BOTH).stream()
					.toArray(String[]::new));

			this.label = new Label(BreadCrumb.this.renderer.render(element), BreadCrumb.this.elementContentMode);
			this.label.addStyleNames(BreadCrumb.this.styles.get(CrumbPart.TEXT).get(CrumbState.UNSELECTED).stream()
					.toArray(String[]::new));
			this.label.addStyleNames(
					BreadCrumb.this.styles.get(CrumbPart.TEXT).get(CrumbState.BOTH).stream().toArray(String[]::new));

			this.payload = element;

			if (BreadCrumb.this.placement == IndicatorPlacement.BEFORE) {
				addComponents(this.icon, this.label);
			} else {
				addComponents(this.label, this.icon);
			}
			setComponentAlignment(this.icon, Alignment.MIDDLE_LEFT);
			setComponentAlignment(this.label, Alignment.MIDDLE_LEFT);
		}

		private void addStyles(CrumbPart part, String... styles) {
			if (part == CrumbPart.INDICATOR) {
				this.icon.addStyleNames(styles);
			} else {
				this.label.addStyleNames(styles);
			}
		}

		private void removeStyles(CrumbPart part, String... styles) {
			if (part == CrumbPart.INDICATOR) {
				this.icon.removeStyleNames(styles);
			} else {
				this.label.removeStyleNames(styles);
			}
		}
	}

	private final IndicatorPlacement placement;
	private final HorizontalLayout layout;
	private final List<Crumb> crumbs = new ArrayList<>();
	private Crumb selected;

	private TextRenderer<T> renderer;
	private ContentMode elementContentMode = ContentMode.TEXT;
	private Resource elementIcon;

	private final Map<CrumbPart, Map<CrumbState, Set<String>>> styles = new HashMap<>();

	/**
	 * {@link Constructor}.
	 * <p>
	 * Indicators will be placed {@link IndicatorPlacement#BEFORE}.
	 */
	public BreadCrumb() {
		this(IndicatorPlacement.BEFORE);
	}

	/**
	 * {@link Constructor}.
	 * <p>
	 * Indicators will be placed as specified.
	 * 
	 * @param placement
	 *            Where to place the indicator of each element; might <b>not</b> be
	 *            null.
	 */
	@SuppressWarnings("unchecked")
	public BreadCrumb(IndicatorPlacement placement) {
		if (placement == null) {
			throw new IllegalArgumentException("Cannot create a BreadCrumb with a null placement.");
		}

		this.placement = placement;

		this.layout = new HorizontalLayout();
		this.layout.setMargin(false);
		this.layout.setSpacing(true);
		this.layout.addLayoutClickListener(event -> {
			if (event.getButton() == MouseButton.LEFT) {
				if (event.getClickedComponent() instanceof Image) {
					BreadCrumb.this.indicatorClicked((Crumb) event.getChildComponent());
				} else {
					BreadCrumb.this.select((Crumb) event.getChildComponent());
				}
			}
		});

		this.renderer = element -> Objects.toString(element);

		Arrays.asList(CrumbPart.values()).forEach(part -> styles.put(part, new HashMap<>()));
		Arrays.asList(CrumbState.values()).forEach(state -> Arrays.asList(CrumbPart.values())
				.forEach(part -> styles.get(part).put(state, new HashSet<>())));

		setCompositionRoot(this.layout);
	}

	// ##################################################################################
	// ################################# CONFIGURATION ##################################
	// ##################################################################################

	/**
	 * Adds a single style name to a specifiable {@link CrumbPart} that is in a
	 * specifiable {@link CrumbState}.
	 * 
	 * @param part
	 *            The part of the element's crumb to add the style to; might
	 *            <b>not</b> be null.
	 * @param state
	 *            The state of the element's crumb to add the style to; might
	 *            <b>not</b> be null.
	 * @param style
	 *            The style to add; might be null
	 */
	public void addElementStyleName(CrumbPart part, CrumbState state, String style) {
		addElementStyleNames(part, state, style);
	}

	/**
	 * Adds style names to a specifiable {@link CrumbPart} that is in a specifiable
	 * {@link CrumbState}.
	 * 
	 * @param part
	 *            The part of the element's crumb to add the styles to; might
	 *            <b>not</b> be null.
	 * @param state
	 *            The state of the element's crumb to add the styles to; might
	 *            <b>not</b> be null.
	 * @param styles
	 *            The styles to add; might be null
	 */
	public void addElementStyleNames(CrumbPart part, CrumbState state, String... styles) {
		if (part == null) {
			throw new IllegalArgumentException("Add a style to a null crumb part.");
		} else if (state == null) {
			throw new IllegalArgumentException("Add a style to a null crumb state.");
		}

		this.styles.get(part).get(state).addAll(Arrays.asList(styles));
		if (state == CrumbState.SELECTED) {
			if (this.selected != null) {
				this.selected.addStyles(part, styles);
			}
		} else {
			this.crumbs.forEach(crumb -> {
				if (state == CrumbState.BOTH || crumb != selected) {
					crumb.addStyles(part, styles);
				}
			});
		}
	}

	/**
	 * Removes a single style name from a specifiable {@link CrumbPart} that is in a
	 * specifiable {@link CrumbState}.
	 * 
	 * @param part
	 *            The part of the element's crumb to remove the style from; might
	 *            <b>not</b> be null.
	 * @param state
	 *            The state of the element's crumb to remove the style from; might
	 *            <b>not</b> be null.
	 * @param style
	 *            The style to remove; might be null
	 */
	public void removeElementStyleName(CrumbPart part, CrumbState state, String style) {
		removeElementStyleNames(part, state, style);
	}

	/**
	 * Removes style names from a specifiable {@link CrumbPart} that is in a
	 * specifiable {@link CrumbState}.
	 * 
	 * @param part
	 *            The part of the element's crumb to remove the styles from; might
	 *            <b>not</b> be null.
	 * @param state
	 *            The state of the element's crumb to remove the styles from; might
	 *            <b>not</b> be null.
	 * @param styles
	 *            The style to remove; might be null
	 */
	public void removeElementStyleNames(CrumbPart part, CrumbState state, String... styles) {
		if (part == null) {
			throw new IllegalArgumentException("Add a style to a null crumb part.");
		} else if (state == null) {
			throw new IllegalArgumentException("Add a style to a null crumb state.");
		}

		this.styles.get(part).get(state).removeAll(Arrays.asList(styles));
		if (state == CrumbState.SELECTED) {
			if (this.selected != null) {
				this.selected.removeStyles(part, styles);
			}
		} else {
			this.crumbs.forEach(crumb -> {
				if (state == CrumbState.BOTH || crumb != selected) {
					crumb.removeStyles(part, styles);
				}
			});
		}
	}

	/**
	 * Sets whether to add a spacing between the indicator of an element and the
	 * next element.
	 * 
	 * @param spacing
	 *            True if there has to be a spacing, false otherwise; true by
	 *            default.
	 */
	public void setInterElementSpacing(boolean spacing) {
		this.layout.setSpacing(spacing);
	}

	/**
	 * Sets whether to add a spacing between the indicator of an element and the
	 * rest of the element.
	 * 
	 * @param spacing
	 *            True if there has to be a spacing, false otherwise; true by
	 *            default.
	 */
	public void setElementInternalSpacing(boolean spacing) {
		this.crumbs.forEach(crumb -> crumb.setSpacing(spacing));
	}

	/**
	 * Sets the {@link TextRenderer} to use when turning an element into a
	 * {@link String}.
	 * 
	 * @param renderer
	 *            The renderer to use, {@link Objects#toString(Object)} by default;
	 *            might be null.
	 */
	public void setElementRenderer(TextRenderer<T> renderer) {
		if (renderer == null) {
			this.renderer = element -> Objects.toString(element);
		} else {
			this.renderer = renderer;
		}
	}

	/**
	 * Sets the {@link ContentMode} for the rendered {@link String} text of every
	 * element.
	 * 
	 * @param mode
	 *            The mode to set; might <b>not</b> be null.
	 */
	public void setElementContentMode(ContentMode mode) {
		if (mode == null) {
			throw new IllegalArgumentException("Cannot set to a null content mode.");
		}
		this.elementContentMode = mode;
		this.crumbs.forEach(crumb -> crumb.label.setContentMode(mode));
	}

	/**
	 * Sets the {@link Resource} that represents all indicators.
	 * 
	 * @param icon
	 *            The {@link Resource} to set; might be null
	 */
	public void setElementIndicatorImage(Resource icon) {
		this.elementIcon = icon;
		this.crumbs.forEach(crumb -> crumb.icon.setIcon(icon));
	}

	// ##################################################################################
	// ##################################### ITEMS ######################################
	// ##################################################################################

	/**
	 * Adds the given element to the end of the breadcrumb.
	 * 
	 * @param element
	 *            The element to add; might be null.
	 */
	public void addElement(T element) {
		Crumb crumb = new Crumb(element);
		this.crumbs.add(crumb);
		this.layout.addComponent(crumb);
	}

	/**
	 * Adds the given element to the breadcrumb at the specified index.
	 * 
	 * @param element
	 *            The element to add; might be null.
	 * @param index
	 *            The index to add the element at; has to be
	 *            0&gt;=index&gt;={@link #getElementCount()}.
	 */
	public void addElementAt(T element, int index) {
		if (index < 0 || index > this.crumbs.size()) {
			throw new IndexOutOfBoundsException("The index " + index + " is out of bounds (0|" + this.crumbs.size()
					+ ") for adding to the breadcrumb's elements.");
		}
		Crumb crumb = new Crumb(element);
		this.crumbs.add(index, crumb);
		this.layout.addComponent(crumb, index);
	}

	/**
	 * Removes the given element from the breadcrumb.
	 * <p>
	 * Note that if there are multiple elements in the breadcrumb where
	 * {@link Objects#equals(Object, Object)}=true for the given element, only the
	 * first will be removed.
	 * 
	 * @param element
	 *            The element to remove; might be null.
	 */
	public void removeElement(T element) {
		Iterator<Crumb> iter = this.crumbs.iterator();
		while (iter.hasNext()) {
			Crumb crumb = iter.next();
			if (Objects.equals(crumb.payload, element)) {
				iter.remove();
				this.layout.removeComponent(crumb);
				if (crumb == this.selected) {
					this.selected = null;
				}
				return;
			}
		}
	}

	/**
	 * Removes the element at the given index from the breadcrumb.
	 * 
	 * @param index
	 *            The index of the element to remove; has to be
	 *            0&gt;=index&gt;{@link #getElementCount()}.
	 */
	@SuppressWarnings("unchecked")
	public void removeElementAt(int index) {
		if (index < 0 || index >= this.crumbs.size()) {
			throw new IndexOutOfBoundsException("The index " + index + " is out of bounds (0|" + this.crumbs.size()
					+ ") for removing from the breadcrumb's elements.");
		}
		this.crumbs.remove(index);
		Iterator<Component> iter = this.layout.iterator();
		int i = 0;
		while (iter.hasNext()) {
			Crumb crumb = (Crumb) iter.next();
			if (i == index) {
				iter.remove();
				if (crumb == this.selected) {
					this.selected = null;
				}
				return;
			}
		}
	}

	/**
	 * Removes all elements from the breadcrumb.
	 */
	public void removeAllElements() {
		this.crumbs.clear();
		this.layout.removeAllComponents();
		this.selected = null;
	}

	/**
	 * Selects the given element.
	 * <p>
	 * Note that if there are multiple elements in the breadcrumb where
	 * {@link Objects#equals(Object, Object)}=true for the given element, only the
	 * first will be selected.
	 * 
	 * @param element
	 *            The element to select; might be null.
	 * @return True if an element was selected, false otherwise
	 */
	public boolean select(T element) {
		Optional<Crumb> toSelect = this.crumbs.stream().filter(crumb -> Objects.equals(crumb.payload, element))
				.findFirst();
		if (toSelect.isPresent()) {
			select(toSelect.get());
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns a {@link List} of all elements in the breadcrumb.
	 * 
	 * @return A {@link List} of all elements; never null, might be empty
	 */
	public List<T> getElements() {
		return this.crumbs.stream().map(crumb -> crumb.payload).collect(Collectors.toList());
	}

	// ##################################################################################
	// ##################################### INFO #######################################
	// ##################################################################################

	/**
	 * Returns whether there is a selected element in the breadcrumb.
	 * 
	 * @return True if an element is selected, false otherwise
	 */
	public boolean hasSelected() {
		return this.selected != null;
	}

	/**
	 * Returns the currently selected element.
	 * 
	 * @return The selected element; might be null if there is none
	 */
	public T getSelected() {
		return this.selected != null ? this.selected.payload : null;
	}

	/**
	 * Returns the index of the currently selected element.
	 * 
	 * @return The index of the selected element; might be -1 if there is none
	 */
	public int getSelectedIndex() {
		return this.crumbs.indexOf(this.selected);
	}

	/**
	 * Returns the count of all elements in the breadcrumb.
	 * 
	 * @return The element count
	 */
	public int getElementCount() {
		return this.crumbs.size();
	}

	// ##################################################################################
	// #################################### EVENT #######################################
	// ##################################################################################

	/**
	 * Event that is thrown when an element in a {@link BreadCrumb} is selected.
	 *
	 * @param <T>
	 *            The element type of the crumbs.
	 */
	public static final class BreadCrumbSelectedEvent<T> extends Event {

		private static final long serialVersionUID = 1L;

		private final T element;

		private BreadCrumbSelectedEvent(BreadCrumb<T> source, T element) {
			super(source);
			this.element = element;
		}

		/**
		 * Returns the selected element.
		 * 
		 * @return The selected element.
		 */
		public T getElement() {
			return element;
		}
	}

	private void select(Crumb crumb) {
		if (crumb != null && this.selected != crumb) {
			shiftSelectedStylesTo(crumb);
			this.selected = crumb;
			fireEvent(new BreadCrumbSelectedEvent<>(this, crumb.payload));
		}
	}

	private void shiftSelectedStylesTo(Crumb target) {
		for (CrumbPart part : CrumbPart.values()) {
			this.styles.get(part).get(CrumbState.SELECTED).forEach(style -> {
				if (!this.styles.get(part).get(CrumbState.UNSELECTED).contains(style)
						&& !this.styles.get(part).get(CrumbState.BOTH).contains(style)) {
					if (this.selected != null) {
						this.selected.removeStyles(part, style);
					}
					target.addStyles(part, style);
				}
			});
			this.styles.get(part).get(CrumbState.UNSELECTED).forEach(style -> {
				if (!this.styles.get(part).get(CrumbState.SELECTED).contains(style)
						&& !this.styles.get(part).get(CrumbState.BOTH).contains(style)) {
					if (this.selected != null) {
						this.selected.addStyles(part, style);
					}
					target.removeStyles(part, style);
				}
			});
		}
	}

	/**
	 * Event that is thrown when the indicator of an element in a {@link BreadCrumb}
	 * is clicked.
	 *
	 * @param <T>
	 *            The element type of the crumbs.
	 */
	public static final class BreadCrumbIndicatorClickedEvent<T> extends Event {

		private static final long serialVersionUID = 1L;

		private final T element;

		private BreadCrumbIndicatorClickedEvent(BreadCrumb<T> source, T element) {
			super(source);
			this.element = element;
		}

		/**
		 * Returns the element whose indicator has been clicked.
		 * 
		 * @return The clicked indicator's element; might be null if the element is null
		 */
		public T getElement() {
			return element;
		}
	}

	private void indicatorClicked(Crumb crumb) {
		fireEvent(new BreadCrumbSelectedEvent<>(this, crumb.payload));
	}
}
