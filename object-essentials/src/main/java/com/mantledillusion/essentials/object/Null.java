package com.mantledillusion.essentials.object;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Static utility that offers a sleek and tidy API to perform null-insecure
 * operations safely using Java8's functional interfaces and lambdas.
 * <p>
 * This utility should be used in situations where the code to execute is small,
 * but the required null checking required to run it safely is exponentially
 * bigger.
 */
public class Null {

	private Null() {
	}

	/**
	 * Checks whether the given value is null.
	 * 
	 * @param <T>
	 *            The type of object to check.
	 * @param value
	 *            The value to check; might be null.
	 * @return True if the given value is null, false otherwise
	 */
	public static <T> boolean is(T value) {
		return value == null;
	}

	/**
	 * Checks whether the given value is null.
	 * 
	 * @param <T>
	 *            The type of object to check.
	 * @param value
	 *            The value to check; might be null.
	 * @return True if the given value is not null, false otherwise
	 */
	public static <T> boolean isNot(T value) {
		return value != null;
	}

	/**
	 * Checks whether the given value is null.
	 * 
	 * @param <T>
	 *            The type of object to check.
	 * @param value
	 *            The value to check; might be null.
	 * @throws NullPointerException
	 *             Is thrown if the given value is null.
	 */
	public static <T> void not(T value) throws NullPointerException {
		not(value, null);
	}

	/**
	 * Checks whether the given value is null.
	 * 
	 * @param <T>
	 *            The type of object to check.
	 * @param value
	 *            The value to check; might be null.
	 * @param message
	 *            The message du include in the {@link NullPointerException}.
	 * @throws NullPointerException
	 *             Is thrown if the given value is null.
	 */
	public static <T> void not(T value, String message) throws NullPointerException {
		if (value == null) {
			throw new NullPointerException(message);
		}
	}

	/**
	 * Returns the default value if the given value is null.
	 * 
	 * @param <T>
	 *            The type of object to default.
	 * @param value
	 *            The value to check; might be null.
	 * @param defaultValue
	 *            The default value to return if the given value is null; might be
	 *            null.
	 * @return The value if not null or the default value, might be null
	 */
	public static <T> T def(T value, T defaultValue) {
		return value == null ? defaultValue : value;
	}

	/**
	 * Retrieves an object using the given {@link Supplier}.
	 * <P>
	 * {@link NullPointerException}s thrown upon calling the given {@link Supplier}
	 * are treated as if the {@link Supplier} had returned null.
	 * 
	 * @param <T>
	 *            The type of object to retrieve.
	 * @param supplier
	 *            The {@link Supplier} to use to get the value; might <b>not</b> be
	 *            null.
	 * @return The result of the {@link Supplier}, might be null
	 */
	public static <T> T get(Supplier<T> supplier) {
		return get(supplier, null);
	}

	/**
	 * Retrieves an object using the given {@link Supplier}.
	 * <P>
	 * {@link NullPointerException}s thrown upon calling the given {@link Supplier}
	 * are treated as if the {@link Supplier} has returned null.
	 * 
	 * @param <T>
	 *            The object type to retrieve.
	 * @param supplier
	 *            The {@link Supplier} to use to get the value; might <b>not</b> be
	 *            null.
	 * @param defaultValue
	 *            The default value to return if the object retrieved from the
	 *            {@link Supplier} is null.
	 * @return The result of the retriever, or if that is null, the default value.
	 */
	public static <T> T get(Supplier<T> supplier, T defaultValue) {
		if (is(supplier)) {
			throw new IllegalArgumentException("Cannot get a value from a null supplier.");
		}

		try {
			return def(supplier.get(), defaultValue);
		} catch (NullPointerException e) {
			return defaultValue;
		}
	}

	/**
	 * Calls the given {@link Consumer} if and only if the value is not null.
	 * 
	 * @param <T>
	 *            The type of object to operate on.
	 * @param value
	 *            The value that has to be null for the {@link Consumer} to be
	 *            called; might be null.
	 * @param consumer
	 *            The {@link Consumer} to call if the given value is not null; might
	 *            <b>not</b> be null.
	 */
	public static <T> void call(T value, Consumer<T> consumer) {
		call(value, null, consumer);
	}

	/**
	 * Calls the given {@link Consumer} if and only if the value is not null.
	 * 
	 * @param <T>
	 *            The type of object to operate on.
	 * @param value
	 *            The value that has to be null for the {@link Consumer} to be
	 *            called; might be null.
	 * @param defaultValue
	 *            The default value to call the {@link Consumer} with if the given
	 *            value is null; might be null although in this case the
	 *            {@link Consumer} is also not called.
	 * @param consumer
	 *            The {@link Consumer} to call if the given value is not null; might
	 *            <b>not</b> be null.
	 */
	public static <T> void call(T value, T defaultValue, Consumer<T> consumer) {
		if (is(consumer)) {
			throw new IllegalArgumentException("Cannot call a null consumer.");
		}

		if (value != null) {
			consumer.accept(value);
		} else if (defaultValue != null) {
			consumer.accept(defaultValue);
		}
	}

	/**
	 * Maps using the given {@link Function} if and only if the value is not null.
	 * 
	 * @param <T>
	 *            The type of object to map.
	 * @param <R>
	 *            The type to map to.
	 * @param value
	 *            The value that has to be not null for the {@link Function} to be
	 *            called; might be null.
	 * @param function
	 *            The {@link Function} to call if the given value is not null; might
	 *            <b>not</b> be null.
	 * @return The mapped value, might be null
	 */
	public static <T, R> R map(T value, Function<T, R> function) {
		return map(value, null, function);
	}

	/**
	 * Maps using the given {@link Function} if and only if the value is not null.
	 * 
	 * @param <T>
	 *            The type of object to map.
	 * @param <R>
	 *            The type to map to.
	 * @param value
	 *            The value that has to be null for the {@link Function} to be
	 *            called; might be null.
	 * @param defaultValue
	 *            The default value to call the {@link Function} with if the given
	 *            value is null; might be null although in this case the
	 *            {@link Function} is also not called.
	 * @param function
	 *            The {@link Function} to call if the given value is not null; might
	 *            <b>not</b> be null.
	 * @return The mapped value, might be null
	 */
	public static <T, R> R map(T value, T defaultValue, Function<T, R> function) {
		return map(value, defaultValue, function, null);
	}

	/**
	 * Maps using the given {@link Function} if and only if the value is not null.
	 * 
	 * @param <T>
	 *            The type of object to map.
	 * @param <R>
	 *            The type to map to.
	 * @param value
	 *            The value that has to be null for the {@link Function} to be
	 *            called; might be null.
	 * @param defaultValue
	 *            The default value to call the {@link Function} with if the given
	 *            value is null; might be null although in this case the
	 *            {@link Function} is also not called.
	 * @param function
	 *            The {@link Function} to call if the given value is not null; might
	 *            <b>not</b> be null.
	 * @param defaultResult
	 *            The default result to return if either the value for the function
	 *            is null or the function returns null; might be null.
	 * @return The mapped value, might be null
	 */
	public static <T, R> R map(T value, T defaultValue, Function<T, R> function, R defaultResult) {
		if (is(function)) {
			throw new IllegalArgumentException("Cannot map using a null function.");
		}

		if (value != null) {
			return def(function.apply(value), defaultResult);
		} else if (defaultValue != null) {
			return def(function.apply(defaultValue), defaultResult);
		} else {
			return defaultResult;
		}
	}
}
