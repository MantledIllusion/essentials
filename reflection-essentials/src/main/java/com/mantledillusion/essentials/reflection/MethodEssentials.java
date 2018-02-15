package com.mantledillusion.essentials.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Essential utilities for {@link Method} handling.
 */
public final class MethodEssentials {

	/**
	 * Finds all declared {@link Method}s in the given type and all of its extended
	 * super types or implemented interfaces.
	 * <p>
	 * The sort order will be according to the given {@link Class}es hierarchy, in
	 * the following order:<br>
	 * - {@link Method}s of the highest super type<br>
	 * - {@link Method}s of the first, second, nth highest super type's
	 * interface<br>
	 * - {@link Method}s of the second highest super type<br>
	 * - etc
	 * <p>
	 * {@link Method}s of interfaces that are implemented multiple times in the
	 * {@link Class}es hierarchy are only listed once on their highest occurrence.
	 * 
	 * @param clazz
	 *            The {@link Class} to search {@link Method}s on; might be null,
	 *            although the result will be empty.
	 * @return An immutable, ordered {@link List} of all {@link Method}s of the
	 *         given type; never null, might be empty.
	 */
	public static List<Method> getDeclaredMethods(Class<?> clazz) {
		LinkedHashSet<Method> methods = new LinkedHashSet<>();
		addDeclaredMethods(methods, clazz);
		return Collections.unmodifiableList(new ArrayList<>(methods));
	}

	private static void addDeclaredMethods(Set<Method> methods, Class<?> clazz) {
		if (clazz != null) {
			addDeclaredMethods(methods, clazz.getSuperclass());
			for (Class<?> interf : clazz.getInterfaces()) {
				addDeclaredMethods(methods, interf);
			}
			for (Method m : clazz.getDeclaredMethods()) {
				if (!methods.contains(m)) {
					methods.add(m);
				}
			}
		}
	}

	/**
	 * Finds all declared {@link Method}s in the given type and all of its extended
	 * super types or implemented interfaces that are annotated with the given
	 * annotation.
	 * <p>
	 * The sort order will be according to the given {@link Class}es hierarchy, in
	 * the following order:<br>
	 * - {@link Method}s of the highest super type<br>
	 * - {@link Method}s of the first, second, nth highest super type's
	 * interface<br>
	 * - {@link Method}s of the second highest super type<br>
	 * - etc
	 * 
	 * @param clazz
	 *            The {@link Class} to search {@link Method}s on; might be null,
	 *            although the result will be empty.
	 * @param annotationClass
	 *            The {@link Annotation} type to search for on {@link Method}s;
	 *            might <b>not</b> be null.
	 * @return An immutable, ordered {@link List} of all {@link Method}s of the
	 *         given type that are annotated with the given {@link Annotation} type;
	 *         never null, might be empty.
	 */
	public static List<Method> getDeclaredMethodsAnnotatedWith(Class<?> clazz,
			Class<? extends Annotation> annotationClass) {
		if (annotationClass == null) {
			throw new IllegalArgumentException(
					"Cannot search annotated methods on a class with a null annotation type to search for.");
		}
		LinkedHashSet<Method> methods = new LinkedHashSet<>();
		addDeclaredMethodsAnnotatedWith(methods, clazz, annotationClass);
		return Collections.unmodifiableList(new ArrayList<>(methods));
	}

	private static void addDeclaredMethodsAnnotatedWith(Set<Method> methods, Class<?> clazz,
			Class<? extends Annotation> annotationClass) {
		if (clazz != null) {
			addDeclaredMethodsAnnotatedWith(methods, clazz.getSuperclass(), annotationClass);
			for (Class<?> interf : clazz.getInterfaces()) {
				addDeclaredMethodsAnnotatedWith(methods, interf, annotationClass);
			}
			for (Method m : clazz.getDeclaredMethods()) {
				if (!methods.contains(m) && m.isAnnotationPresent(annotationClass)) {
					methods.add(m);
				}
			}
		}
	}

	private MethodEssentials() {
	}
}
