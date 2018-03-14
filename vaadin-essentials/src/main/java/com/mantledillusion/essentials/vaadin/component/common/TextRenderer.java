package com.mantledillusion.essentials.vaadin.component.common;

/**
 * Interface for renderers that can render a certain type to a {@link String} to
 * be used as text.
 *
 * @param <T>
 */
public interface TextRenderer<T> {

	/**
	 * Renders the given element to a {@link String}.
	 * 
	 * @param element
	 *            The element to render; might be null.
	 * @return A {@link String} the given element has been rendered to; might be
	 *         null
	 */
	public String render(T element);
}