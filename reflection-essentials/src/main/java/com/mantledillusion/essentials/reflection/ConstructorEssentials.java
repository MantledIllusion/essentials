package com.mantledillusion.essentials.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Essential utilities for {@link Constructor} handling.
 */
public final class ConstructorEssentials {

	/**
	 * Finds all declared constructors of the given type, ordered by parameter
	 * count.
	 * 
	 * @param <T>
	 *            The {@link Class} type.
	 * @param clazz
	 *            The {@link Class} to search {@link Constructor}s on; might be
	 *            null, although the result will be empty.
	 * @return An immutable, ordered {@link List} of all {@link Constructor}s of the
	 *         given type; never null, might be empty.
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<Constructor<T>> getDeclaredConstructors(Class<T> clazz) {
		List<Constructor<T>> constructors = new ArrayList<>();
		if (clazz != null) {
			Arrays.asList(clazz.getDeclaredConstructors()).stream()
					.forEach(constructor -> constructors.add((Constructor<T>) constructor));
			constructors.sort(new Comparator<Constructor<T>>() {

				@Override
				public int compare(Constructor<T> o1, Constructor<T> o2) {
					return Integer.valueOf(o1.getParameterCount()).compareTo(Integer.valueOf(o2.getParameterCount()));
				}
			});
		}
		return Collections.unmodifiableList(constructors);
	}

	/**
	 * Finds all declared constructors of the given type, annotated with the given
	 * annotation and ordered ascending by parameter count.
	 * 
	 * @param <T>
	 *            The {@link Class} type.
	 * @param clazz
	 *            The {@link Class} to search {@link Constructor}s on; might be
	 *            null, although the result will be empty.
	 * @param annotationClass
	 *            The {@link Annotation} type to search for on {@link Constructor}s;
	 *            might <b>not</b> be null.
	 * @return An immutable, ordered {@link List} of all {@link Constructor}s of the
	 *         given type that are annotated with the given {@link Annotation} type;
	 *         never null, might be empty.
	 */
	public static <T> List<Constructor<T>> getDeclaredConstructorsAnnotatedWith(Class<T> clazz,
			Class<? extends Annotation> annotationClass) {
		if (annotationClass == null) {
			throw new IllegalArgumentException(
					"Cannot search annotated constructors on a class with a null annotation type to search for.");
		}
		List<Constructor<T>> constructors = new ArrayList<>();
		if (clazz != null) {
			getDeclaredConstructors(clazz).stream().forEachOrdered(constructor -> {
				if (constructor.isAnnotationPresent(annotationClass)) {
					constructors.add(constructor);
				}
			});
		}
		return Collections.unmodifiableList(constructors);
	}

	private ConstructorEssentials() {
	}
}
