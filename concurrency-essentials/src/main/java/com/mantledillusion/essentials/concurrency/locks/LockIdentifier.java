package com.mantledillusion.essentials.concurrency.locks;

import java.util.Arrays;

/**
 * Identifier base type that identifies a specific lock.
 * <p>
 * A lock represents an immutable sequence of {@link Object}s (called lock
 * properties), where the {@link Object}s itself and their order determines the
 * outcome of {@link #equals(Object)} and {@link #hashCode()}.
 */
public abstract class LockIdentifier {

	private final Object[] properties;

	/**
	 * Constructor.
	 * <p>
	 * The given properties will be used as identifying values for the lock, in
	 * exactly the given order.
	 * 
	 * @param lockProperties
	 *            The properties that identify this identifier; might contain nulls,
	 *            but might <b>not</b> be null or empty.
	 */
	protected LockIdentifier(Object... lockProperties) {
		if (lockProperties == null || lockProperties.length == 0) {
			throw new IllegalArgumentException(
					"Cannot create a lock identifier using a null or empty property sequence for identification.");
		}
		this.properties = lockProperties;
	}

	/**
	 * Returns a copy of the underlying lock property sequence.
	 * <p>
	 * Modifying the sequence has no effect on the lock.
	 * 
	 * @return A copy of the lock sequence; never null.
	 */
	public final Object[] getLockProperties() {
		return Arrays.copyOf(this.properties, this.properties.length);
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
	public String toString() {
		return getClass().getSimpleName() + " [properties=" + Arrays.toString(properties) + "]";
	}
}
