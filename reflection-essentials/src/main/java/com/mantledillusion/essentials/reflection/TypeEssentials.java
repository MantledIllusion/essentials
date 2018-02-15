package com.mantledillusion.essentials.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Essential utilities for {@link Type} handling.
 */
public final class TypeEssentials {

	private static final Set<Class<?>> STANDARD_PRIMITIVE_TYPES = Collections
			.unmodifiableSet(new HashSet<>(Arrays.asList(new Class<?>[] { byte.class, boolean.class, short.class,
					int.class, long.class, float.class, double.class, char.class })));
	private static final Set<Class<?>> STANDARD_PRIMITIVE_WRAPPER_TYPES = Collections
			.unmodifiableSet(new HashSet<>(Arrays.asList(new Class<?>[] { Byte.class, Boolean.class, Short.class,
					Integer.class, Long.class, Float.class, Double.class, Character.class, String.class })));
	private static final Set<Class<?>> STANDARD_PRIMITIVE_ARRAY_TYPES = Collections
			.unmodifiableSet(new HashSet<>(Arrays.asList(new Class<?>[] { byte[].class, boolean[].class, short[].class,
					int[].class, long[].class, float[].class, double[].class, char[].class })));

	/**
	 * Finds all super {@link Class}s in the given type.
	 * <p>
	 * The sort order will be according to the given {@link Class}es hierarchy, in
	 * the following order:<br>
	 * - the highest super {@link Class}<br>
	 * - {@link Class} of the first, second, nth highest super {@link Class}es'
	 * interface<br>
	 * - the second highest super {@link Class}<br>
	 * - etc
	 * <p>
	 * {@link Class}es of interfaces that are implemented multiple times in the
	 * {@link Class}es hierarchy are only listed once on their highest occurrence.
	 * 
	 * @param clazz
	 *            The {@link Class} to search super {@link Class}es on; might be
	 *            null, although the result will be empty.
	 * @return An immutable, ordered {@link List} of all super {@link Class}s of the
	 *         given type; never null, might be empty.
	 */
	public static List<Class<?>> getSuperClasses(Class<?> type) {
		LinkedHashSet<Class<?>> classes = new LinkedHashSet<>();
		addSuperClasses(classes, type);
		return Collections.unmodifiableList(new ArrayList<>(classes));
	}

	private static void addSuperClasses(Set<Class<?>> classes, Class<?> type) {
		if (type != null) {
			addSuperClasses(classes, type.getSuperclass());
			for (Class<?> iface : type.getInterfaces()) {
				addSuperClasses(classes, iface);
			}
			if (!classes.contains(type)) {
				classes.add(type);
			}
		}
	}

	/**
	 * Finds all super {@link Class}s in the given type that are annotated with the
	 * given annotation.
	 * <p>
	 * The sort order will be according to the given {@link Class}es hierarchy, in
	 * the following order:<br>
	 * - the highest super {@link Class}<br>
	 * - {@link Class} of the first, second, nth highest super {@link Class}es'
	 * interface<br>
	 * - the second highest super {@link Class}<br>
	 * - etc
	 * <p>
	 * {@link Class}es of interfaces that are implemented multiple times in the
	 * {@link Class}es hierarchy are only listed once on their highest occurrence.
	 * 
	 * @param clazz
	 *            The {@link Class} to search super {@link Class}es on; might be
	 *            null, although the result will be empty.
	 * @param annotationClass
	 *            The {@link Annotation} type to search for on {@link Class}es;
	 *            might <b>not</b> be null.
	 * @return An immutable, ordered {@link List} of all super {@link Class}s of the
	 *         given type; never null, might be empty.
	 */
	public static List<Class<?>> getSuperClassesAnnotatedWith(Class<?> type,
			Class<? extends Annotation> annotationClass) {
		if (annotationClass == null) {
			throw new IllegalArgumentException(
					"Cannot search annotated classes on a class with a null annotation type to search for.");
		}
		LinkedHashSet<Class<?>> classes = new LinkedHashSet<>();
		addSuperClassesAnnotatedWith(classes, type, annotationClass);
		return Collections.unmodifiableList(new ArrayList<>(classes));
	}

	private static void addSuperClassesAnnotatedWith(Set<Class<?>> classes, Class<?> type,
			Class<? extends Annotation> annotationClass) {
		if (type != null) {
			addSuperClassesAnnotatedWith(classes, type.getSuperclass(), annotationClass);
			for (Class<?> iface : type.getInterfaces()) {
				addSuperClassesAnnotatedWith(classes, iface, annotationClass);
			}
			if (!classes.contains(type) && type.isAnnotationPresent(annotationClass)) {
				classes.add(type);
			}
		}
	}

	/**
	 * Searches for a {@link ParameterizedType} bound in the given {@link Type}
	 * where the given {@link Class} is the raw type.
	 * <P>
	 * NOTE: It is searched for direct a <b>bound</b> in the root {@link Type}, not
	 * an occurrence that could appear somewhere down the generic declarations. So
	 * when searching for a parameterized {@link Class} A in the following types,
	 * these will be the results:<br>
	 * - 'A' = null<br>
	 * - 'A[]' = null<br>
	 * - 'A&ltB&gt' = A&ltB&gt<br>
	 * - 'T extends A&ltB&gt' = A&ltB&gt<br>
	 * - '? super A&ltB&gt' = A&ltB&gt<br>
	 * - 'A&ltB&gt[]' = A&ltB&gt<br>
	 * - 'Map&ltString, A&ltB&gt&gt = null<br>
	 * <P>
	 * Also note that on {@link WildcardType}s, the lower bounds are preferred.
	 * 
	 * @param typeToFind
	 *            The {@link Class} to find a {@link ParameterizedType} of.
	 * @param typeToFindIn
	 *            The {@link Type} to find the {@link ParameterizedType} in.
	 * @return The {@link ParameterizedType} occurance, or null if there is no
	 *         occurance or the given type does not have type parameters.
	 */
	public static <T> ParameterizedType getParameterizedBound(Class<T> classToFind, Type typeToFindIn) {
		if (typeToFindIn instanceof Class) {
			return null;
		} else if (typeToFindIn instanceof GenericArrayType) {
			return getParameterizedBound(classToFind, ((GenericArrayType) typeToFindIn).getGenericComponentType());
		} else if (typeToFindIn instanceof ParameterizedType) {
			return ((ParameterizedType) typeToFindIn).getRawType() == classToFind ? (ParameterizedType) typeToFindIn
					: null;
		} else if (typeToFindIn instanceof TypeVariable) {
			for (Type boundToFindIn : ((TypeVariable<?>) typeToFindIn).getBounds()) {
				ParameterizedType possibleResult = getParameterizedBound(classToFind, boundToFindIn);
				if (possibleResult != null) {
					return possibleResult;
				}
			}
			return null;
		} else if (typeToFindIn instanceof WildcardType) {
			for (Type boundToFindIn : ((WildcardType) typeToFindIn).getUpperBounds()) {
				ParameterizedType possibleResult = getParameterizedBound(classToFind, boundToFindIn);
				if (possibleResult != null) {
					return possibleResult;
				}
			}
			for (Type boundToFindIn : ((WildcardType) typeToFindIn).getLowerBounds()) {
				ParameterizedType possibleResult = getParameterizedBound(classToFind, boundToFindIn);
				if (possibleResult != null) {
					return possibleResult;
				}
			}
			return null;
		} else {
			throw new IllegalArgumentException(
					"Unknown " + Type.class.getSimpleName() + " implementation '" + typeToFindIn + "'.");
		}
	}

	/**
	 * Returns whether the given {@link Class} is one of the Java language's
	 * standard primitive types:
	 * {@code byte, boolean, short, int, long, float, double,
	 * char}.
	 * 
	 * @param clazz
	 *            The {@link Class} to check; might be null, although the result
	 *            will always be false.
	 * @return True if the {@link Class} is a primitive type, false otherwise
	 */
	public static boolean isStandartPrimitiveType(Class<?> clazz) {
		return TypeEssentials.STANDARD_PRIMITIVE_TYPES.contains(clazz);
	}

	/**
	 * Returns {@link Class} instances of all of the Java language's standard
	 * primitive types: {@code byte, boolean, short, int, long, float, double,
	 * char}.
	 * 
	 * @return An immutable {@link Set} of all of the Java language's standard
	 *         primitive types; never null, never empty
	 */
	public static Set<Class<?>> getStandartPrimitiveTypes() {
		return TypeEssentials.STANDARD_PRIMITIVE_TYPES;
	}

	/**
	 * Returns whether the given {@link Class} is one of the Java language's
	 * primitive wrapper types:
	 * {@code Byte, Boolean, Short, Integer, Long, Float, Double,
	 * Character, String}
	 * 
	 * @param clazz
	 *            The {@link Class} to check; might be null, although the result
	 *            will always be false.
	 * @return True if the {@link Class} is a primitive wrapper type, false
	 *         otherwise
	 */
	public static boolean isStandartPrimitiveWrapperType(Class<?> clazz) {
		return TypeEssentials.STANDARD_PRIMITIVE_WRAPPER_TYPES.contains(clazz);
	}

	/**
	 * Returns {@link Class} instances of all of the Java language's standard
	 * primitive wrapper types:
	 * {@code Byte, Boolean, Short, Integer, Long, Float, Double,
	 * Character, String}
	 * 
	 * @return An immutable {@link Set} of all of the Java language's standard
	 *         primitive wrapper types; never null, never empty
	 */
	public static Set<Class<?>> getStandartPrimitiveWrapperTypes() {
		return TypeEssentials.STANDARD_PRIMITIVE_WRAPPER_TYPES;
	}

	/**
	 * Returns whether the given {@link Class} is one of the Java language's
	 * primitive array types:
	 * {@code byte[], boolean[], short[], int[], long[], float[],
	 * double[], char[]}
	 * 
	 * @param clazz
	 *            The {@link Class} to check; might be null, although the result
	 *            will always be false.
	 * @return True if the {@link Class} is a primitive array type, false otherwise
	 */
	public static boolean isStandartPrimitiveArrayType(Class<?> type) {
		return TypeEssentials.STANDARD_PRIMITIVE_ARRAY_TYPES.contains(type);
	}

	/**
	 * Returns {@link Class} instances of all of the Java language's standard
	 * primitive array types: {@code byte[],
	 * boolean[], short[], int[], long[], float[], double[], char[]}
	 * 
	 * @return An immutable {@link Set} of all of the Java language's standard
	 *         primitive array types; never null, never empty
	 */
	public static Set<Class<?>> getStandartPrimitiveArrayTypes() {
		return TypeEssentials.STANDARD_PRIMITIVE_ARRAY_TYPES;
	}

	private TypeEssentials() {
	}
}
