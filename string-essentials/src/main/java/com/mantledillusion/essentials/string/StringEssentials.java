package com.mantledillusion.essentials.string;

import java.util.Objects;
import java.util.function.Function;

/**
 * Essential utilities for {@link String} handling.
 */
public final class StringEssentials {

	public static final String DEFAULT_REPLACE_PREFIX = "${";
	public static final String DEFAULT_REPLACE_POSTFIX = "}";

	/**
	 * Replaces placeholders in a {@link String} recursively.
	 * <p>
	 * Equals {@link #deepReplace(String, Function, String, String)} using
	 * {@link #DEFAULT_REPLACE_PREFIX} and {@value #DEFAULT_REPLACE_POSTFIX}.
	 * 
	 * @see #deepReplace(String, Function, String, String)
	 * 
	 * @param template
	 *            The base template to replace in; might be null.
	 * @param replacementProvider
	 *            The {@link Function} that is able to receive {@link String} keys
	 *            it then provides replacements for; might <b>not</b> be null,
	 *            should return the key if no replacement is available.
	 * @return The template, with every value replaced that was available at the
	 *         provider
	 * @throws IllegalArgumentException
	 *             If the template or one of its replacements have an unequal
	 *             prefix/postfix amount.
	 */
	public static String deepReplace(String template, Function<String, Object> replacementProvider) {
		return deepReplace(template, replacementProvider, DEFAULT_REPLACE_PREFIX, DEFAULT_REPLACE_POSTFIX);
	}

	/**
	 * Replaces placeholders in a {@link String} recursively.
	 * <p>
	 * The algorithm will find placeholders with the given prefix and postfix in the
	 * given template, calculate their replacement using the given provider and then
	 * perform a replacement run over that replacement itself before nesting it in
	 * the template.
	 * <p>
	 * For example, the template...<br>
	 * <code>"Hello! Your contact ${reaction.${contact.availability}}; please wait for further instructions."</code><br>
	 * ...might be combined with the following static {@link String}
	 * replacements:<br>
	 * - "reaction.AVAILABLE"="is ${contact.salutation} ${contact.name}"<br>
	 * - "reaction.UNAVAILABLE"="is not free yet"
	 * <p>
	 * These static replacements might now be made available via the replacement
	 * provider together with dynamic replacement resources.
	 * <p>
	 * The dynamic example set 1...<br>
	 * - "contact.availability" : "AVAILABLE"<br>
	 * - "contact.salutation" : "Mrs."<br>
	 * - "contact.name" : "Haberdasher";<br>
	 * ...will be replaced as: "Hello! Your contact is Mrs. Haberdasher; please wait
	 * for further instructions."
	 * <p>
	 * The dynamic example set 2...<br>
	 * - "contact.availability" : "UNAVAILABLE"<br>
	 * ...will be replaced as: "Hello! Your contact is not free yet; please wait for
	 * further instructions."
	 * 
	 * @param template
	 *            The base template to replace in; might be null.
	 * @param replacementProvider
	 *            The {@link Function} that is able to receive {@link String} keys
	 *            it then provides replacements for; might <b>not</b> be null,
	 *            should return the key if no replacement is available.
	 * @param replacementPrefix
	 *            The prefix of every replacement; might <b>not</b> be null or
	 *            empty, might also not equal the suffix or contain it.
	 * @param replacementPostfix
	 *            The suffix of every replacement; might <b>not</b> be null or
	 *            empty, might also not equal the prefix or contain it.
	 * @return The template, with every value replaced that was available at the
	 *         provider
	 * @throws IllegalArgumentException
	 *             If the template or one of its replacements have an unequal
	 *             prefix/postfix amount.
	 */
	public static String deepReplace(String template, Function<String, Object> replacementProvider,
			String replacementPrefix, String replacementPostfix) {
		if (replacementProvider == null) {
			throw new IllegalArgumentException("Cannot replace using a null replacement provider");
		} else if (replacementPrefix == null || replacementPrefix.isEmpty()) {
			throw new IllegalArgumentException("Cannot replace using a null or empty replacement prefix");
		} else if (replacementPostfix == null || replacementPostfix.isEmpty()) {
			throw new IllegalArgumentException("Cannot replace using a null or empty replacement postfix");
		} else if (replacementPrefix.equals(replacementPostfix)) {
			throw new IllegalArgumentException("Cannot replace using equal replacement prefix and postfix");
		} else if (replacementPrefix.contains(replacementPostfix)) {
			throw new IllegalArgumentException("Cannot replace using a replacement prefix that contains the postfix");
		} else if (replacementPostfix.contains(replacementPrefix)) {
			throw new IllegalArgumentException("Cannot replace using a replacement postfix that contains the prefix");
		}

		StringBuilder sb = new StringBuilder();
		if (deepReplace(template != null ? template : "", sb, replacementProvider, replacementPrefix,
				replacementPostfix).isEmpty()) {
			return sb.toString();
		}
		throw new IllegalArgumentException("Invalid prefix/postfix use in the template or one of its replacements");
	}

	private static String deepReplace(String part, StringBuilder mainAppender,
			Function<String, Object> replacementProvider, String replacementPrefix, String replacementPostfix) {
		StringBuilder partAppender = new StringBuilder();

		int begin;
		int end;
		do {
			begin = part.indexOf(replacementPrefix);
			end = part.indexOf(replacementPostfix);

			if (begin == -1) {
				int until = end == -1 ? part.length() : end;
				partAppender.append(part.substring(0, until));
				if (part.length() == until) {
					mainAppender.append(partAppender.toString());
					return "";
				} else {
					part = part.substring(until);
					break;
				}
			} else {
				if (end == -1) {
					throw new IllegalArgumentException(
							"Invalid prefix/postfix use in the template or one of its replacements");
				} else if (begin < end) {
					partAppender.append(part.substring(0, begin));
					part = deepReplace(part.substring(begin + replacementPrefix.length()), partAppender,
							replacementProvider, replacementPrefix, replacementPostfix);
					if (part.isEmpty()) {
						throw new IllegalArgumentException(
								"Invalid prefix/postfix use in the template or one of its replacements");
					}
					part = part.substring(replacementPostfix.length());
				} else {
					partAppender.append(part.substring(0, end));
					part = part.substring(end);
					break;
				}
			}
		} while (begin != -1);

		String replaceable = partAppender.toString();
		String replaced = Objects.toString(replacementProvider.apply(replaceable));
		deepReplace(replaced, mainAppender, replacementProvider, replacementPrefix, replacementPostfix);

		return part;
	}
}
