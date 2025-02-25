package com.mantledillusion.essentials.vaadin.component.common;

import com.vaadin.server.Resource;

/**
 * Interface for renderers that can render a certain type to a {@link Resource}
 * to be used as icon.
 * 
 * @param <T>
 *            The type to render as {@link Resource}.
 */
public interface IconRenderer<T> {

	/**
	 * Renders the given element to a {@link Resource}.
	 * 
	 * @param element
	 *            The element to render; might be null.
	 * @return A {@link Resource} the given element has been rendered to; might be
	 *         null
	 */
	public Resource render(T element);
}