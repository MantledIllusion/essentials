package com.mantledillusion.essentials.object;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A specialized {@link LinkedHashMap} extension that uses {@link System#identityHashCode(Object)} instead of
 * {@link Object#equals(Object)} on the key for the following methods:
 * <p>
 * - {@link #containsKey(Object)}
 * - {@link #get(Object)}
 * - {@link #put(Object, Object)}
 * - {@link #remove(Object)}
 *
 * @param <K> The map's key type.
 * @param <V> The map's value type.
 */
public class IdentityLinkedHashMap<K, V> extends LinkedHashMap<K, V> {

    @Override
    public boolean containsKey(Object key) {
        for (K k : keySet()) {
            if (k == key) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        for (Map.Entry<K, V> entry : entrySet()) {
            if (entry.getKey() == key) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        for (Map.Entry<K, V> entry : entrySet()) {
            if (entry.getKey() == key) {
                return super.put(entry.getKey(), value);
            }
        }
        return super.put(key, value);
    }

    @Override
    public V remove(Object key) {
        for (Map.Entry<K, V> entry : entrySet()) {
            if (entry.getKey() == key) {
                return super.remove(entry.getKey());
            }
        }
        return null;
    }
}