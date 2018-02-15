package com.mantledillusion.essentials.reflection;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A reference on a {@link Class}.
 * <p>
 * It might be persisted or serialized and loaded again even when the referred
 * {@link Class} is not available in the {@link ClassLoader} anymore.
 * <p>
 * Even in that case, the reference will still be able to provide basic
 * reflective information about the {@link Class} it refers to.
 */
public final class ClassReference implements Serializable {

	private static final long serialVersionUID = -7635539627930928054L;

	private static final Map<String, ClassReference> REGISTRY = new HashMap<>();
	private static final Set<String> PRIMITIVE_TYPES = TypeEssentials.getStandartPrimitiveTypes().stream()
			.map(type -> type.getName()).collect(Collectors.toSet());
	private static final Map<String, Class<?>> PRIMITIVEARRAYNAME_TO_PRIMITIVE = TypeEssentials
			.getStandartPrimitiveArrayTypes().stream()
			.collect(Collectors.toMap(key -> key.getName(), value -> value.getComponentType()));
	private static final Map<String, String> PRIMITIVENAME_TO_PRIMITIVEARRAYNAME = TypeEssentials
			.getStandartPrimitiveArrayTypes().stream()
			.collect(Collectors.toMap(key -> key.getComponentType().getName(), value -> value.getName()));

	private final String fullClassName;
	private final boolean isPrimitive;
	private final boolean isPrimitiveWrapper;
	private final boolean isArray;
	private final ClassReference componentType;

	private ClassReference(String fullTypeName, boolean isPrimitive, boolean isPrimitiveWrapper, boolean isArray,
			ClassReference componentType) {
		this.fullClassName = fullTypeName;
		this.isPrimitive = isPrimitive;
		this.isPrimitiveWrapper = isPrimitiveWrapper;
		this.isArray = isArray;
		this.componentType = componentType;
	}

	/**
	 * Returns the reflective full {@link Class} name of the referenced
	 * {@link Class}.
	 * 
	 * @return The full name of the represented {@link Class}; never null
	 */
	public String getFullClassName() {
		return fullClassName;
	}

	/**
	 * Returns whether this {@link ClassReference} refers to a Java language
	 * primitive {@link Class} like {@code boolean}, {@code int}, {@code char} etc.
	 * 
	 * @return True if the represented {@link Class} is a primitive, false otherwise
	 */
	public boolean isPrimitive() {
		return isPrimitive;
	}

	/**
	 * Returns whether this {@link ClassReference} refers to a Java language
	 * primitive {@link Class} wrapper like {@code Boolean}, {@code Integer},
	 * {@code Character} etc.
	 * 
	 * @return True if the represented {@link Class} is a primitive wrapper, false
	 *         otherwise
	 */
	public boolean isPrimitiveWrapper() {
		return isPrimitiveWrapper;
	}

	/**
	 * Returns whether this {@link ClassReference} refers to an array type of any
	 * kind.
	 * 
	 * @return True if the represented {@link Class} is an array, false otherwise
	 */
	public boolean isArray() {
		return isArray;
	}

	/**
	 * Returns the array component type of this {@link ClassReference}.
	 * 
	 * @return The represented {@link Class}es component type; may be null if the
	 *         represented {@link Class} is no array type
	 */
	public ClassReference getComponentType() {
		return componentType;
	}

	/**
	 * Loads the referenced {@link Class} using the given {@link ClassLoader}.
	 * 
	 * @param classLoader
	 *            The {@link ClassLoader} to use; might <b>not</b> be null.
	 * @return A {@link Class} instance of the referenced {@link Class}; never null
	 * @throws ClassNotFoundException
	 *             Is thrown if the referenced class cannot be found by the given
	 *             {@link ClassLoader}
	 */
	public Class<?> getReferredClass(ClassLoader classLoader) throws ClassNotFoundException {
		if (classLoader == null) {
			throw new IllegalArgumentException("Cannot load the referenced class using a null class loader.");
		}
		return classLoader.loadClass(this.fullClassName);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fullClassName == null) ? 0 : fullClassName.hashCode());
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
		ClassReference other = (ClassReference) obj;
		if (fullClassName == null) {
			if (other.fullClassName != null)
				return false;
		} else if (!fullClassName.equals(other.fullClassName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return ClassReference.class.getSimpleName() + " [fullClassName=" + fullClassName + "]";
	}

	/**
	 * Factory {@link Method} for building a new reference out of a full
	 * {@link Class} name.
	 * 
	 * @param fullClassName
	 *            The full {@link Class} name to refer to; might <b>not</b> be null.
	 * @return A new {@link ClassReference} instance; never null
	 */
	public static ClassReference of(String fullClassName) {
		if (fullClassName == null) {
			throw new IllegalArgumentException("Cannot create a reference from a null full class name.");
		}
		return retrieve(fullClassName);
	}

	/**
	 * Factory {@link Method} for building a new reference out of a {@link Class}
	 * instance.
	 * 
	 * @param clazz
	 *            The {@link Class} to build a reference to; might <b>not</b> be
	 *            null.
	 * @return A new {@link ClassReference} instance; never null
	 */
	public static ClassReference of(Class<?> clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("Cannot create a reference from a null class instance.");
		}
		return retrieve(clazz.getName());
	}

	/**
	 * Factory {@link Method} for building an array reference of an already existing
	 * reference.
	 * 
	 * @param componentType
	 *            The reference of the {@link Class} to use as the component type of
	 *            the array type; might <b>not</b> be null.
	 * @return A new {@link ClassReference} instance; never null
	 */
	public static ClassReference asArrayType(ClassReference componentType) {
		if (componentType == null) {
			throw new IllegalArgumentException("Cannot create an array reference of a null component type.");
		}

		if (componentType.isArray) {
			return retrieve('[' + componentType.fullClassName);
		} else if (componentType.isPrimitive) {
			return retrieve(PRIMITIVENAME_TO_PRIMITIVEARRAYNAME.get(componentType.fullClassName));
		} else {
			return retrieve("[L" + componentType.fullClassName);
		}
	}

	private static ClassReference retrieve(String fullTypeName) {
		if (!REGISTRY.containsKey(fullTypeName)) {

			boolean isArray = fullTypeName.indexOf('[') == 0;
			ClassReference componentType = null;
			if (isArray) {
				if (PRIMITIVEARRAYNAME_TO_PRIMITIVE.containsKey(fullTypeName)) {
					componentType = of(PRIMITIVEARRAYNAME_TO_PRIMITIVE.get(fullTypeName));
				} else if (fullTypeName.charAt(1) == 'L') {
					componentType = of(fullTypeName.substring(2));
				} else {
					componentType = of(fullTypeName.substring(1));
				}
			}

			ClassReference type = new ClassReference(fullTypeName, PRIMITIVE_TYPES.contains(fullTypeName),
					PRIMITIVE_TYPES.contains(fullTypeName), isArray, componentType);

			REGISTRY.put(fullTypeName, type);
		}
		return REGISTRY.get(fullTypeName);
	}
}
