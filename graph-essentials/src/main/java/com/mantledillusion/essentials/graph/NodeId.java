package com.mantledillusion.essentials.graph;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Cumulative ID of one or multiple @{@link Node}'s IDs.
 *
 * @param <IdType> The node's identifier type
 */
public class NodeId<IdType> {

    private final Set<IdType> ids;

    /**
     * Default constructor.
     *
     * @param id A {@link Node}'s ID; might <b>not</b> be null.
     */
    public NodeId(IdType id) {
        this(Collections.singleton(id));
    }

    private NodeId(Set<IdType> ids) {
        this.ids = ids;
    }

    /**
     * Clusters this {@link NodeId} with the given one.
     *
     * @param other The {@link NodeId} to form cumulative {@link NodeId} with; might <b>not</b> be null.
     * @return A new {@link NodeId} with combined IDs, never null
     */
    public NodeId<IdType> clusterWith(NodeId<IdType> other) {
        return new NodeId<>(NodeEssentials.union(this.ids, other.ids));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NodeId)) return false;
        return Objects.equals(this.ids, ((NodeId<?>) o).ids);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.ids);
    }

    @Override
    public String toString() {
        return this.ids.toString();
    }
}
