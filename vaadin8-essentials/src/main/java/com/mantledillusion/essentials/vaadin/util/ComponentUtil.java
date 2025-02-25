package com.mantledillusion.essentials.vaadin.util;

import java.math.BigInteger;
import java.security.SecureRandom;

import com.vaadin.ui.Component;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.JavaScriptFunction;

import elemental.json.JsonArray;
import elemental.json.JsonException;

/**
 * Utility class for {@link Component}s.
 */
public final class ComponentUtil {

	private static final String JAVASCRIPT_METHOD_NAME = "fetchComponentInfo";
	private static final String JAVASCRIPT_METHOD_NAME_ID = "$0";
	private static final String JAVASCRIPT_METHOD_COMPONENT_ID = "$1";
	private static final String JAVASCRIPT_METHOD = "$0"
			+ "(document.getElementById('$1').getBoundingClientRect().left, "
			+ "document.getElementById('$1').getBoundingClientRect().top, "
			+ "document.getElementById('$1').clientWidth, " + "document.getElementById('$1').clientHeight);";

	private ComponentUtil() {
	}

	/**
	 * A callback to notify when the {@link Component} information fetching from the
	 * client side has finished.
	 */
	@FunctionalInterface
	public interface ClientSideInfoCallback {

		/**
		 * Handles the fetched info from client side.
		 * 
		 * @param xPos
		 *            The x position of the {@link Component} in pixel
		 * @param yPos
		 *            The y position of the {@link Component} in pixel
		 * @param width
		 *            The width of the {@link Component} in pixel
		 * @param height
		 *            The height of the {@link Component} in pixel
		 */
		public void infoFetched(int xPos, int yPos, int width, int height);
	}

	/**
	 * Fetches position and size in pixels of the given {@link Component} from the
	 * client side.
	 * <p>
	 * To retrieve the information from the client side, a JavaScript method will be
	 * injected into the given component on client side that will then be called to
	 * retrieve component information from, which will be given to callback.
	 * <p>
	 * NOTE: If the given component does NOT have an ID set, it will get an unique
	 * alphanumeric ID for the time of the execution because it is necessary to
	 * identify the component's java script element on client side!
	 * 
	 * @param c
	 *            The component to fetch information for; might <b>not</b> be null.
	 * @param callback
	 *            The callback to notify once the info fetching is finished; might
	 *            <b>not</b> be null.
	 */
	public static void fetchClientSideComponentInfo(final Component c, final ClientSideInfoCallback callback) {
		if (c == null) {
			throw new IllegalArgumentException("Cannot fetch client side information of a null component");
		} else if (callback == null) {
			throw new IllegalArgumentException("Cannot fetch client side information for a null callback");
		}
		final String savedComponentId = c.getId();
		String componentId = savedComponentId == null || savedComponentId.isEmpty() ? generateUniqueId()
				: savedComponentId;
		c.setId(componentId);
		final String uniqueMethodName = JAVASCRIPT_METHOD_NAME + generateUniqueId();
		JavaScript.getCurrent().addFunction(uniqueMethodName, new JavaScriptFunction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void call(final JsonArray arguments) throws JsonException {
				JavaScript.getCurrent().removeFunction(uniqueMethodName);
				c.setId(savedComponentId);
				callback.infoFetched((int) arguments.getNumber(0), (int) arguments.getNumber(1),
						(int) arguments.getNumber(2), (int) arguments.getNumber(3));
			}
		});
		JavaScript.getCurrent().execute(JAVASCRIPT_METHOD.replace(JAVASCRIPT_METHOD_NAME_ID, uniqueMethodName)
				.replace(JAVASCRIPT_METHOD_COMPONENT_ID, componentId));
	}

	private static String generateUniqueId() {
		return new BigInteger(130, new SecureRandom()).toString(32);
	}
}
