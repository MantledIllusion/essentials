package com.mantledillusion.essentials.object;

import java.util.HashSet;
import java.util.Set;

/**
 * Static utility that offers some specialized methods about {@link Set}s.
 */
public class SetEssentials {

    private SetEssentials() {}

    /**
     * Creates a new {@link Set} from the given array and individual items.
     * <p>
     * Explicit null values in the array will be taken into the {@link Set} as well.
     *
     * @param <T> The object type
     * @param itemArray The item array; might be null or contain nulls.
     * @return A new, non immutable {@link Set} containing all items, never null
     */
    @SafeVarargs
    public static <T> Set<T> asSet(T... itemArray) {
        HashSet<T> set = new HashSet<>();
        CollectionEssentials.add(set, itemArray);
        return set;
    }

    /**
     * Creates a new {@link Set} from the given array and individual items.
     * <p>
     * Explicit null values in the array or in between the items will be taken into the {@link Set} as well.
     *
     * @param <T> The object type
     * @param itemArray The item array; might be null or contain nulls.
     * @param item The first additional item; might be null.
     * @param moreItems More additional items; might be null or contain nulls.
     * @return A new, non immutable {@link Set} containing all items, never null
     */
    @SafeVarargs
    public static <T> Set<T> toList(T[] itemArray, T item, T... moreItems) {
        HashSet<T> set = new HashSet<>();
        CollectionEssentials.add(set, itemArray, item, moreItems);
        return set;
    }

    /**
     * Creates a new {@link Set} from the given arrays.
     * <p>
     * Explicit null values in the arrays will be taken into the {@link Set} as well.
     *
     * @param <T> The object type
     * @param itemArrays The item arrays; might be null or contain nulls.
     * @return A new, non immutable {@link Set} containing all items, never null
     */
    @SafeVarargs
    public static <T> Set<T> toList(T[]... itemArrays) {
        HashSet<T> set = new HashSet<>();
        CollectionEssentials.add(set, itemArrays);
        return set;
    }
}
