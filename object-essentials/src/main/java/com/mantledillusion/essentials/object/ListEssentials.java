package com.mantledillusion.essentials.object;

import java.util.ArrayList;
import java.util.List;

/**
 * Static utility that offers some specialized methods about {@link List}s.
 */
public class ListEssentials {

    private ListEssentials() {}

    /**
     * Creates a new {@link List} from the given array and individual items.
     * <p>
     * All items will be taken over into the {@link List} in the exact order they are given.
     * <p>
     * Explicit null values in the array will be taken into the {@link List} as well.
     *
     * @param <T> The object type
     * @param itemArray The item array; might be null or contain nulls.
     * @return A new, non immutable {@link List} containing all items, never null
     */
    public static <T> List<T> asList(T... itemArray) {
        ArrayList<T> list = new ArrayList<>();
        CollectionEssentials.add(list, itemArray);
        return list;
    }

    /**
     * Creates a new {@link List} from the given array and individual items.
     * <p>
     * All items will be taken over into the {@link List} in the exact order they are given.
     * <p>
     * Explicit null values in the array or in between the items will be taken into the {@link List} as well.
     *
     * @param <T> The object type
     * @param itemArray The item array; might be null or contain nulls.
     * @param item The first additional item; might be null.
     * @param moreItems More additional items; might be null or contain nulls.
     * @return A new, non immutable {@link List} containing all items, never null
     */
    public static <T> List<T> toList(T[] itemArray, T item, T... moreItems) {
        ArrayList<T> list = new ArrayList<>();
        CollectionEssentials.add(list, itemArray, item, moreItems);
        return list;
    }

    /**
     * Creates a new {@link List} from the given arrays.
     * <p>
     * All items will be taken over into the {@link List} in the exact order they are given.
     * <p>
     * Explicit null values in the arrays will be taken into the {@link List} as well.
     *
     * @param <T> The object type
     * @param itemArrays The item arrays; might be null or contain nulls.
     * @return A new, non immutable {@link List} containing all items, never null
     */
    public static <T> List<T> toList(T[]... itemArrays) {
        ArrayList<T> list = new ArrayList<>();
        CollectionEssentials.add(list, itemArrays);
        return list;
    }
}
