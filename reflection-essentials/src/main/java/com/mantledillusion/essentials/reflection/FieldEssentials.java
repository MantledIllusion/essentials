package com.mantledillusion.essentials.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Essential utilities for {@link Field} handling.
 */
public final class FieldEssentials {

	/**
	 * Finds all declared {@link Field}s in the given type and all of its extended
	 * super types or implemented interfaces.
	 * <p>
	 * The sort order will be according to the given {@link Class}es hierarchy, in
	 * the following order:<br>
	 * - {@link Field}s of the highest super type<br>
	 * - {@link Field}s of the first, second, nth highest super type's interface<br>
	 * - {@link Field}s of the second highest super type<br>
	 * - etc
	 * <p>
	 * {@link Field}s of interfaces that are implemented multiple times in the
	 * {@link Class}es hierarchy are only listed once on their highest occurrence.
	 * 
	 * @param <T>
	 *            The {@link Class} type.
	 * @param clazz
	 *            The {@link Class} to search {@link Field}s on; might be null,
	 *            although the result will be empty.
	 * @return An immutable, ordered {@link List} of all {@link Field}s of the given
	 *         type; never null, might be empty.
	 */
	public static <T> List<Field> getDeclaredFields(Class<T> clazz) {
		Set<Field> fields = new LinkedHashSet<>();
		addDeclaredFields(fields, clazz);
		return Collections.unmodifiableList(new ArrayList<>(fields));
	}

	private static <T> void addDeclaredFields(Set<Field> fields, Class<T> clazz) {
		if (clazz != null) {
			addDeclaredFields(fields, clazz.getSuperclass());
			for (Class<?> interf : clazz.getInterfaces()) {
				addDeclaredFields(fields, interf);
			}
			for (Field f : clazz.getDeclaredFields()) {
				if (!fields.contains(f)) {
					fields.add(f);
				}
			}
		}
	}

	/**
	 * Finds all declared {@link Field}s in the given type and all of its extended
	 * super types or implemented interfaces that are annotated with the given
	 * annotation.
	 * <p>
	 * The sort order will be according to the given {@link Class}es hierarchy, in
	 * the following order:<br>
	 * - {@link Field}s of the highest super type<br>
	 * - {@link Field}s of the first, second, nth highest super type's interface<br>
	 * - {@link Field}s of the second highest super type<br>
	 * - etc
	 * 
	 * @param <T>
	 *            The {@link Class} type.
	 * @param <A>
	 *            The {@link Annotation} type to search for.
	 * @param clazz
	 *            The {@link Class} to search {@link Field}s on; might be null,
	 *            although the result will be empty.
	 * @param annotationClass
	 *            The {@link Annotation} type to search for on {@link Field}s; might
	 *            <b>not</b> be null.
	 * @return An immutable, ordered {@link List} of all {@link Field}s of the given
	 *         type that are annotated with the given {@link Annotation} type; never
	 *         null, might be empty.
	 */
	public static <T, A extends Annotation> List<Field> getDeclaredFieldsAnnotatedWith(Class<T> clazz,
			Class<A> annotationClass) {
		if (annotationClass == null) {
			throw new IllegalArgumentException(
					"Cannot search annotated fields on a class with a null annotation type to search for.");
		}
		Set<Field> fields = new LinkedHashSet<>();
		addDeclaredFieldsAnnotatedWith(fields, clazz, annotationClass);
		return Collections.unmodifiableList(new ArrayList<>(fields));
	}

	private static <T, A extends Annotation> void addDeclaredFieldsAnnotatedWith(Set<Field> fields, Class<T> clazz,
			Class<A> annotationType) {
		if (clazz != null) {
			addDeclaredFieldsAnnotatedWith(fields, clazz.getSuperclass(), annotationType);
			for (Class<?> interf : clazz.getInterfaces()) {
				addDeclaredFieldsAnnotatedWith(fields, interf, annotationType);
			}
			for (Field f : clazz.getDeclaredFields()) {
				if (!fields.contains(f) && f.isAnnotationPresent(annotationType)) {
					fields.add(f);
				}
			}
		}
	}

	private FieldEssentials() {
	}
}
