package com.mantledillusion.essentials.object;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Specialized {@link ArrayList} extension that uses {@link System#identityHashCode(Object)} instead of
 * {@link Object#equals(Object)} on the element for the following methods:
 * <p>
 * - {@link #contains(Object)}<br>
 * - {@link #containsAll(Collection)}<br>
 * - {@link #remove(Object)}<br>
 * - {@link #removeAll(Collection)}<br>
 * - {@link #retainAll(Collection)}<br>
 *
 * @param <E> The list's element type.
 */
public class IdentityArrayList <E> extends ArrayList<E> {

    public IdentityArrayList() {

    }

    public IdentityArrayList(int initialSize) {
        super(initialSize);
    }

    public IdentityArrayList(Collection<E> collection) {
        super(collection);
    }

    @Override
    public boolean contains(Object o) {
        for (E element : this) {
            if (element == o) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        for (int i = 0; i < size(); i++) {
            if (get(i) == o) {
                remove(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        for (Object o : c) {
            while (remove(o)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean modified = false;
        for (int i = size() - 1; i >= 0; i--) {
            if (!c.contains(get(i))) {
                remove(i);
                modified = true;
            }
        }
        return modified;
    }
}