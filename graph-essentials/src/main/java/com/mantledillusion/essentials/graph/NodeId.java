package com.mantledillusion.essentials.graph;

import java.util.Collections;
import java.util.Set;

public class NodeId<IdType> {

    private final Set<IdType> ids;

    public NodeId(IdType id) {
        this(Collections.singleton(id));
    }

    public NodeId(Set<IdType> ids) {
        this.ids = ids;
    }

    public NodeId<IdType> clusterWith(NodeId<IdType> other) {
        return new NodeId<>(NodeEssentials.union(this.ids, other.ids));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeId<?> other = (NodeId<?>) o;
        return !Collections.disjoint(this.ids, other.ids);
    }

    @Override
    public int hashCode() {
        return 0; // ENFORCE HASHING CLASH
    }

    @Override
    public String toString() {
        return this.ids.toString();
    }
}
