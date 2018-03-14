package com.mantledillusion.essentials.vaadin.component;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.mantledillusion.essentials.vaadin.component.common.IconRenderer;
import com.mantledillusion.essentials.vaadin.component.common.TextRenderer;
import com.vaadin.server.Resource;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;

/**
 * {@link Composite} {@link Component} that is able to arrange elements of a
 * specifiable type as a menu bar.
 *
 * @param <T>
 *            The element type of the {@link ElementMenuBar}'s elements.
 */
public final class ElementMenuBar<T> extends Composite {

	private static final long serialVersionUID = 1L;

	private final Command command = new Command() {

		private static final long serialVersionUID = 1L;

		@Override
		public void menuSelected(MenuItem selectedItem) {
			ElementMenuBar.this.fireElementClicked(selectedItem);
		}
	};

	/**
	 * An element of an {@link ElementMenuBar}.
	 *
	 * @param <T>
	 *            The element type of the {@link MenuBarElement}.
	 */
	public static final class MenuBarElement<T> {

		private final ElementMenuBar<T> bar;
		private final T payload;
		private final MenuItem item;

		private final Map<T, MenuItem> children = new HashMap<>();

		private MenuBarElement(ElementMenuBar<T> bar, T payload, MenuItem item) {
			this.bar = bar;
			this.payload = payload;
			this.item = item;

			bar.elements.put(item, this);
			rerender();
		}

		private void rerender() {
			String text = this.bar.textRenderer.render(this.payload);
			if (text == null) {
				throw new IllegalArgumentException("A " + TextRenderer.class.getSimpleName()
						+ " is not allowed to return a null text for an element, even if the element is null.");
			}
			this.item.setText(text);
			this.item.setIcon(this.bar.iconRenderer.render(this.payload));
		}

		/**
		 * Adds a child element to this {@link MenuBarElement}.
		 * 
		 * @param element
		 *            The child element to add; might be null, might <b>not</b> be
		 *            already added.
		 * @return A new {@link MenuBarElement} representing the given element
		 */
		public MenuBarElement<T> addElement(T element) {
			if (this.children.containsKey(element)) {
				throw new IllegalStateException("The element '" + element + "' is already added to the element.");
			}
			MenuBarElement<T> elem = new MenuBarElement<T>(this.bar, element, this.item.addItem("", this.bar.command));
			this.children.put(element, elem.item);
			this.item.setCommand(null);
			return elem;
		}

		/**
		 * Removes a child element from this {@link MenuBarElement}.
		 * 
		 * @param element
		 *            The child element to remove; might be null
		 * @return True if the child element was removed, false otherwise
		 */
		public boolean removeElement(T element) {
			if (this.children.containsKey(element)) {
				this.item.removeChild(this.children.get(element));
				this.children.remove(element);
				if (this.children.isEmpty()) {
					this.item.setCommand(this.bar.command);
				}
				return true;
			}
			return false;
		}

		/**
		 * Removes all child elements from this {@link MenuBarElement}
		 */
		public void removeAllElements() {
			this.item.removeChildren();
			this.children.clear();
		}
	}

	private final MenuBar menuBar = new MenuBar();
	private TextRenderer<T> textRenderer = element -> Objects.toString(element);
	private IconRenderer<T> iconRenderer = element -> null;

	private final Map<T, MenuItem> children = new HashMap<>();
	private final Map<MenuItem, MenuBarElement<T>> elements = new HashMap<>();

	/**
	 * {@link Constructor}.
	 */
	public ElementMenuBar() {
		setCompositionRoot(this.menuBar);
	}

	// ##################################################################################
	// ################################# CONFIGURATION ##################################
	// ##################################################################################

	/**
	 * Sets the {@link TextRenderer} to render the {@link ElementMenuBar} element's
	 * {@link String} text with.
	 * 
	 * @param renderer
	 *            The renderer to use, {@link Objects#toString(Object)} by default;
	 *            might be null.
	 */
	public void setElementTextRenderer(TextRenderer<T> renderer) {
		if (renderer == null) {
			this.textRenderer = element -> Objects.toString(element);
		} else {
			this.textRenderer = renderer;
		}
		rerender();
	}

	/**
	 * Sets the {@link IconRenderer} to render the {@link ElementMenuBar} element's
	 * {@link Resource} icon with.
	 * 
	 * @param renderer
	 *            The renderer to use; might be null
	 */
	public void setElementIconRenderer(IconRenderer<T> renderer) {
		if (renderer == null) {
			this.iconRenderer = element -> null;
		} else {
			this.iconRenderer = renderer;
		}
		rerender();
	}

	private void rerender() {
		for (MenuBarElement<T> element : this.elements.values()) {
			element.rerender();
		}
	}

	// ##################################################################################
	// ################################### ELEMENTS #####################################
	// ##################################################################################

	/**
	 * Adds the given element as a root element to the {@link ElementMenuBar}.
	 * 
	 * @param element
	 *            The child element to add; might be null, might <b>not</b> be
	 *            already added.
	 * @return A new {@link MenuBarElement} representing the given element
	 */
	public MenuBarElement<T> addElement(T element) {
		if (this.children.containsKey(element)) {
			throw new IllegalStateException("The element '" + element + "' is already added to the bar.");
		}
		MenuBarElement<T> elem = new MenuBarElement<T>(this, element, this.menuBar.addItem("", this.command));
		this.children.put(element, elem.item);
		return elem;
	}

	/**
	 * Removes a roor child element from the {@link ElementMenuBar}.
	 * 
	 * @param element
	 *            The child element to remove; might be null
	 * @return True if the child element was removed, false otherwise
	 */
	public boolean removeElement(T element) {
		if (this.children.containsKey(element)) {
			this.menuBar.removeItem(this.children.get(element));
			this.children.remove(element);
			return true;
		}
		return false;
	}

	/**
	 * Removes all child elements from this {@link MenuBarElement}
	 */
	public void removeAllElements() {
		this.menuBar.removeItems();
		this.children.clear();
	}

	// ##################################################################################
	// #################################### EVENT #######################################
	// ##################################################################################

	/**
	 * Event that is thrown when a non-parent {@link MenuBarElement} is clicked.
	 *
	 * @param <T>
	 *            The element type of the {@link ElementMenuBar}'s elements.
	 */
	public static final class MenuBarElementClickedEvent<T> extends Event {

		private static final long serialVersionUID = 1L;

		private final T element;

		private MenuBarElementClickedEvent(ElementMenuBar<T> source, T element) {
			super(source);
			this.element = element;
		}

		/**
		 * Returns the clicked element.
		 * 
		 * @return The clicked element; might be null if the element is null
		 */
		public T getElement() {
			return element;
		}
	}

	private void fireElementClicked(MenuItem item) {
		fireEvent(new MenuBarElementClickedEvent<T>(this, this.elements.get(item).payload));
	}
}
