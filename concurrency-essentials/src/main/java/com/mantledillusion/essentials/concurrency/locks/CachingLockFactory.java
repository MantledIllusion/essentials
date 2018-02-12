package com.mantledillusion.essentials.concurrency.locks;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * A factorizing cache that builds and caches intrinsic {@link Lock}s.
 * <p>
 * The factory holds exactly one {@link Lock} for every unique
 * {@link LockIdentifier}, where unique means identifier.{@link #equals(Object)}
 * returns false on all other identifiers in the cache.
 * <p>
 * That being said, calling {@link #retrieveLock(LockIdentifier)} with 2 equal
 * identifier instances will returns the exactly same {@link Lock} instance.
 * <p>
 * Since {@link Lock} retrieval via {@link #retrieveLock(LockIdentifier)} is
 * synchronized on the factory instance it is called on, a single factory
 * instance may hold all {@link Lock}s for all callers of a specific
 * {@code synchronized} block from any {@link Thread} possible, where the block
 * synchronizes on the factories' {@link Lock} instances.
 */
public final class CachingLockFactory<T extends LockIdentifier> {

	/**
	 * A very basic {@link Lock} to be used in a {@code synchronized} block.
	 * <p>
	 * Is only instantiated on demand by
	 * {@link CachingLockFactory#retrieveLock(LockIdentifier)}.
	 */
	public final class Lock {
		private Lock() {
		}
	}

	private final Map<T, Lock> cache = new HashMap<>();

	/**
	 * Returns a {@link Lock} for the given own implementation T of
	 * {@link LockIdentifier} thread safely.
	 * <p>
	 * For two identifier instances of the type T where T1.equals(T2) = true, this
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
	 * {@link #retrieveLock(LockIdentifier)} with an equal identifier afterwars
	 * would cause a different {@link Lock} to be returned.
	 * <p>
	 * This breaks the mechanism of this factory to always return the same
	 * {@link Lock} for equal identifiers.
	 * <p>
	 * If any {@link Lock} instance cleared by calling this {@link Method} is still
	 * referenced somewhere and used in a {@code synchronized} block, a different
	 * {@link Lock} returned by this factory afterwards for the same identifier
	 * would <b>not</b> cause 2 {@link Thread}s using one of the {@link Lock}s each
	 * to be synchonized in that {@code synchronized} block.
	 * <p>
	 * That being said, this {@link Method} should be used very carefully at runtime
	 * when it is impossible that any of the cleared locks might be used again.
	 */
	public synchronized void clear() {
		this.cache.clear();
	}
}
