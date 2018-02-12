package com.mantledillusion.essentials.concurrency.locks;

import java.util.Arrays;

/**
 * Identifier base type that identifies a specific lock.
 */
public abstract class LockIdentifier {

	private final Object[] properties;

	/**
	 * Constructor.
	 * <p>
	 * The given properties will be used as identifying values for the lock, in
	 * exactly the given order.
	 * 
	 * @param properties
	 *            The properties that identify this identifier; might be null but
	 *            might <b>not</b> be empty.
	 */
	public LockIdentifier(Object... properties) {
		if (properties != null && properties.length == 0) {
			throw new IllegalArgumentException(
					"Cannot create a lock identifier using 0 properties for identification, as that would cause all identifiers to be equal.");
		}
		this.properties = properties;
	}

	final Object[] getProperties() {
		return this.properties;
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(properties);
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LockIdentifier other = (LockIdentifier) obj;
		if (!Arrays.equals(properties, other.properties))
			return false;
		return true;
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + " [properties=" + Arrays.toString(properties) + "]";
	}
}
