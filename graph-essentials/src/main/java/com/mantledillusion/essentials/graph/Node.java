package com.mantledillusion.essentials.graph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A node cluster of a graph.
 *
 * @param <NodeType> The type of the {@link Node} implementation itself.
 */
public interface Node<NodeType extends Node<NodeType>> {

    /**
     * Declares different forms in which a {@link Node} might allow to be clustered with another {@link Node}.
     */
    enum Clusterability {

        /**
         * The {@link Node} denies being clustered with the given other {@link Node}.
         */
        DENY,

        /**
         * The {@link Node} allows being clustered with the given other {@link Node} if they are both siblings of
         * the same parent {@link Node}.
         */
        SIBLINGS
    }

    /**
     * A @{@link Registration} of a @{@link Node} under an ID and the IDs of all of its neighbors.
     *
     * @param <NodeType> The type of the implementation of the registered {@link Node}.
     * @param <IdType>   The type of the registered {@link Node}'s ID.
     */
    class Registration<NodeType extends Node<NodeType>, IdType> {

        private final IdType identifier;
        private final Set<IdType> neighbors;
        private final NodeType node;

        private Registration(IdType identifier, Set<IdType> neighbors, NodeType node) {
            this.identifier = identifier;
            this.neighbors = neighbors;
            this.node = node;
        }

        /**
         * Returns the identifier of this @{@link Registration}.
         *
         * @return The identifier, never null
         */
        public IdType getIdentifier() {
            return this.identifier;
        }

        /**
         * Returns the identifiers of all nodes that are neighbors to this @{@link Registration}'s {@link Node}.
         * <p>
         * Relations be either uni- or bi-directional; it won't have any effect on the distribution.
         *
         * @return The {@link Set} of neighbors, never null, might be empty
         */
        public Set<IdType> getNeighbors() {
            return this.neighbors;
        }

        /**
         * Returns the node of this @{@link Registration}.
         *
         * @return The node, never null
         */
        public NodeType getNode() {
            return this.node;
        }
    }

    /**
     * Returns the radius the node requires just for its core, without considering any children.
     *
     * @return The radius, &gt; 0
     */
    double getRadius();

    /**
     * Sets the radius of the node children's orbit when they are placed.
     *
     * @param orbit The orbit, &gt; 0
     */
    void setOrbit(double orbit);

    /**
     * Sets the x-coordinate of the node when it is placed.
     *
     * @param x The x coordinate, -INF &lt; x &lt; INF
     */
    void setX(double x);

    /**
     * Sets the y-coordinate of the node when it is placed.
     *
     * @param y The x coordinate, -INF &lt; y &lt; INF
     */
    void setY(double y);

    int getMinClusterSize();

    int getMaxClusterSize();

    Clusterability clusterableWith(NodeType other);

    NodeType clusterWith(NodeType other);

    /**
     * Creates a new {@link Registration} of this @{@link Node} under the given ID and with all the given neighbors.
     *
     * @param <IdType>   The type of ID the @{@link Node} will be registered under
     * @param identifier The ID the {@link Node} will be registered under; might <b>not</b> be null
     * @param neighbors  The IDs of all of the @{@link Node}'s neighbors; might <b>not</b> be null, might be empty
     * @return A new {@link Registration} instance, never null
     */
    @SuppressWarnings("unchecked")
    default <IdType> Registration<NodeType, IdType> register(IdType identifier, IdType... neighbors) {
        return register(identifier, new HashSet<>(Arrays.asList(neighbors)));
    }

    /**
     * Creates a new {@link Registration} of this @{@link Node} under the given ID and with all the given neighbors.
     *
     * @param <IdType>   The type of ID the @{@link Node} will be registered under
     * @param identifier The ID the {@link Node} will be registered under; might <b>not</b> be null
     * @param neighbors  The IDs of all of the @{@link Node}'s neighbors; might <b>not</b> be null, might be empty
     * @return A new {@link Registration} instance, never null
     */
    @SuppressWarnings("unchecked")
    default <IdType> Registration<NodeType, IdType> register(IdType identifier, Set<IdType> neighbors) {
        return new Registration<>(identifier, neighbors, (NodeType) this);
    }

}
