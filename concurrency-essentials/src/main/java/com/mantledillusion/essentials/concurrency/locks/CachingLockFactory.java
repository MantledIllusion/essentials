package com.mantledillusion.essentials.concurrency.locks;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * A factorizing cache that builds and caches intrinsic locks for synchronized()
 * statements.
 */
public final class CachingLockFactory<T extends LockIdentifier> {

	public final class Lock {
		private Lock() {
		};
	}

	private final Map<T, Lock> cache;

	/**
	 * Constructor.
	 */
	public CachingLockFactory() {
		this.cache = new HashMap<>();
	}

	/**
	 * Returns a lock for the given own implementation T of {@link LockIdentifier}
	 * thread safely.
	 * <p>
	 * For two identifier instances of T where T1.equals(T2) = true, this
	 * {@link Method} will return exactly the same {@link Lock} instance.
	 * 
	 * @param identifier
	 *            The identifier to return a lock for; might be null.
	 * @return A new {@link Lock} instance that will also be returned at other times
	 *         for an equal identifier; never null
	 */
	public synchronized Lock retrieveLock(T identifier) {
		if (!cache.containsKey(identifier)) {
			this.cache.put(identifier, new Lock());
		}

		return this.cache.get(identifier);
	}

	/**
	 * Clears the cached {@link Lock}s.
	 * <p>
	 * NOTE: Calling {@link #retrieveLock(LockIdentifier)} / {@link #clear()} /
	 * {@link #retrieveLock(LockIdentifier)} with an equal identifier would cause a
	 * different {@link Lock} to be returned, which breaks the mechanism of this
	 * factory to always return the same {@link Lock} for equal identifiers; be
	 * careful at which point this factory is cleared.
	 */
	public synchronized void clear() {
		this.cache.clear();
	}
}
