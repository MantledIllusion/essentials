package com.mantledillusion.essentials.object;

import java.util.Arrays;
import java.util.Collection;

/**
 * Static utility that offers some specialized methods about {@link Collection}s.
 */
public class CollectionEssentials {

    private CollectionEssentials() {}

    /**
     * Adds the given items to the given {@link Collection}.
     * <p>
     * Explicit null values in the array will be taken into the {@link Collection} as well.
     *
     * @param <T> The object type
     * @param itemArray The item array; might be null or contain nulls.
     * @param collection The {@link Collection} to at the items to; might <b>not</b> be null.
     */
    @SafeVarargs
	public static <T> void add(Collection<T> collection, T... itemArray) {
        if (Null.is(collection)) {
            throw new IllegalArgumentException("Cannot add items to a null collection");
        }
        if (Null.isNot(itemArray)) {
            collection.addAll(Arrays.asList(itemArray));
        }
    }

    /**
     * Adds the given items to the given {@link Collection}.
     * <p>
     * Explicit null values in the array or in between the items will be taken into the {@link Collection} as well.
     *
     * @param <T> The object type
     * @param itemArray The item array; might be null or contain nulls.
     * @param item The first additional item; might be null.
     * @param moreItems More additional items; might be null or contain nulls.
     * @param collection The {@link Collection} to at the items to; might <b>not</b> be null.
     */
    @SafeVarargs
    public static <T> void add(Collection<T> collection, T[] itemArray, T item, T... moreItems) {
        if (Null.is(collection)) {
            throw new IllegalArgumentException("Cannot add items to a null collection");
        }
        if (Null.isNot(itemArray)) {
            collection.addAll(Arrays.asList(itemArray));
        }
        collection.add(item);
        if (Null.isNot(moreItems)) {
            collection.addAll(Arrays.asList(moreItems));
        }
    }

    /**
     * Adds the given items to the given {@link Collection}.
     * <p>
     * Explicit null values in the arrays will be taken into the {@link Collection} as well.
     *
     * @param <T> The object type
     * @param itemArrays The item arrays; might be null or contain nulls.
     * @param collection The {@link Collection} to at the items to; might <b>not</b> be null.
     */
    @SafeVarargs
    public static <T> void add(Collection<T> collection, T[]... itemArrays) {
        if (Null.is(collection)) {
            throw new IllegalArgumentException("Cannot add items to a null collection");
        }
        if (Null.isNot(itemArrays)) {
            for (T[] array: itemArrays) {
                if (Null.isNot(array)) {
                    collection.addAll(Arrays.asList(array));
                }
            }
        }
    }
}
