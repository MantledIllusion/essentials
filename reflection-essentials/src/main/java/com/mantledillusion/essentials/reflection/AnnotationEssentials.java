package com.mantledillusion.essentials.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Essential utilities for {@link Annotation} handling.
 */
public final class AnnotationEssentials {

	/**
	 * Defines an occurrence of an {@link Annotation} on an
	 * {@link AnnotatedElement}.
	 */
	public static final class AnnotationOccurrence {

		private final Annotation annotation;
		private final AnnotatedElement annotatedElement;

		private AnnotationOccurrence(Annotation annoation, AnnotatedElement annotatedElement) {
			this.annotation = annoation;
			this.annotatedElement = annotatedElement;
		}

		/**
		 * Returns the occurring {@link Annotation}.
		 * 
		 * @return The {@link Annotation}; never null
		 */
		public Annotation getAnnotation() {
			return annotation;
		}

		/**
		 * Returns the {@link AnnotatedElement} the occurrence is on.
		 * 
		 * @return The {@link AnnotatedElement}; never null
		 */
		public AnnotatedElement getAnnotatedElement() {
			return annotatedElement;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((annotation == null) ? 0 : annotation.hashCode());
			result = prime * result + ((annotatedElement == null) ? 0 : annotatedElement.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AnnotationOccurrence other = (AnnotationOccurrence) obj;
			if (annotation == null) {
				if (other.annotation != null)
					return false;
			} else if (!annotation.equals(other.annotation))
				return false;
			if (annotatedElement == null) {
				if (other.annotatedElement != null)
					return false;
			} else if (!annotatedElement.equals(other.annotatedElement))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "AnnotationOccurrence [annoation=" + annotation + ", annotatedElement=" + annotatedElement + "]";
		}
	}

	/**
	 * Searches for {@link Annotation}s that are placed somewhere on the given
	 * {@link Class} and are themselves annotated with the given annotation type.
	 * <p>
	 * Such {@link Annotation}s can be found on:<br>
	 * - The given {@link Class} type or any of its extended super types or
	 * implemented interfaces<br>
	 * - The {@link Package}s of any of these types<br>
	 * - The {@link Field}s of any of these types<br>
	 * - The {@link Constructor}s of any of these types<br>
	 * - The {@link Method}s of any of these types<br>
	 * - The {@link Parameter}s of of any of these type's {@link Constructor}s or
	 * {@link Method}s<br>
	 * - The receiver type of any of these type's {@link Method}s<br>
	 * - The return type of any of these type's {@link Method}s<br>
	 * - The {@link Annotation}s on any of the above<br>
	 * 
	 * @param clazz
	 *            The {@link Class} to search the annotated {@link Annotation}s on;
	 *            might be null, although the result will be empty.
	 * @param annotationClass
	 *            The {@link Annotation} type to search for on other
	 *            {@link Annotation}s; might <b>not</b> be null.
	 * @return An immutable {@link List} of all {@link Annotation}s found on the
	 *         given type, annotated with the given {@link Annotation}; never null,
	 *         might be empty
	 */
	public static List<AnnotationOccurrence> getAnnotationsAnnotatedWith(Class<?> clazz,
			Class<? extends Annotation> annotationClass) {
		if (annotationClass == null) {
			throw new IllegalArgumentException(
					"Cannot search annotated annotations on a class with a null annotation type to search for.");
		}
		return Collections.unmodifiableList(
				AnnotationEssentials.getAnnotationsAnnotatedWith(clazz, annotationClass, new HashSet<>()));
	}

	private static List<AnnotationOccurrence> getAnnotationsAnnotatedWith(Class<?> clazz,
			Class<? extends Annotation> annotationType, Set<Class<? extends AnnotatedElement>> visited) {
		List<AnnotationOccurrence> annotated;
		if (clazz != null) {
			annotated = getAnnotationsAnnotatedWith(clazz.getSuperclass(), annotationType, visited);
			for (Class<?> iface : clazz.getInterfaces()) {
				annotated.addAll(getAnnotationsAnnotatedWith(iface, annotationType, visited));
			}

			AnnotationEssentials.addAnnotatedAnnotations(clazz.getPackage(), annotationType, annotated, visited);
			AnnotationEssentials.addAnnotatedAnnotations(clazz, annotationType, annotated, visited);
			for (Field f : clazz.getDeclaredFields()) {
				AnnotationEssentials.addAnnotatedAnnotations(f, annotationType, annotated, visited);
			}
			for (Constructor<?> c : clazz.getDeclaredConstructors()) {
				AnnotationEssentials.addAnnotatedAnnotations(c, annotationType, annotated, visited);
				for (Parameter p : c.getParameters()) {
					AnnotationEssentials.addAnnotatedAnnotations(p, annotationType, annotated, visited);
				}
			}
			for (Method m : clazz.getDeclaredMethods()) {
				AnnotationEssentials.addAnnotatedAnnotations(m, annotationType, annotated, visited);
				for (Parameter p : m.getParameters()) {
					AnnotationEssentials.addAnnotatedAnnotations(p, annotationType, annotated, visited);
				}
				AnnotationEssentials.addAnnotatedAnnotations(m.getAnnotatedReceiverType(), annotationType, annotated,
						visited);
				AnnotationEssentials.addAnnotatedAnnotations(m.getAnnotatedReturnType(), annotationType, annotated,
						visited);
			}
		} else {
			annotated = new ArrayList<>();
		}
		return annotated;
	}

	private static void addAnnotatedAnnotations(AnnotatedElement e, Class<? extends Annotation> annotationType,
			List<AnnotationOccurrence> annotated, Set<Class<? extends AnnotatedElement>> visited) {
		if (e != null) {
			visited.add(e.getClass());
			for (Annotation a : e.getAnnotations()) {
				if (a.annotationType().isAnnotationPresent(annotationType)) {
					annotated.add(new AnnotationOccurrence(a, e));
					if (!visited.contains(a.annotationType())) {
						addAnnotatedAnnotations(a.annotationType(), annotationType, annotated, visited);
					}
				}
			}
		}
	}

	private AnnotationEssentials() {
	}
}
