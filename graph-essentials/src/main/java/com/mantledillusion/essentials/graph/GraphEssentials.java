package com.mantledillusion.essentials.graph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Static utility that is able to distribute a graph's nodes on a 2-dimensional surface with the littlest interference.
 */
public class GraphEssentials {

    /**
     * The node of a graph.
     *
     * @param <IdType> The type of ID that uniquely identifies nodes.
     */
    public interface GraphNode<IdType> {

        /**
         * Returns the identifier of the node.
         *
         * @return The identifier, never null
         */
        IdType getIdentifier();

        /**
         * Returns the radius the node requires just for its core, without considering any children.
         *
         * @return The radius, &gt; 0
         */
        double getRadius();

        /**
         * Returns the identifiers of all nodes that are neighbors to this node.
         * <p>
         * Relations have to be bi-directional, so A has to include its neighbor B in the returned {@link Set} as well
         * as B is required to include A in its.
         *
         * @return The {@link Set} of neighbors, never null, might be empty
         */
        Set<IdType> getNeighbors();

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
    }

    /**
     * Distributes the given nodes on a planetary system around the graphs weighted center.
     * <p>
     * The node at the weighted center (highest weight when considering all children and their respective weights) is
     * placed on the coordinates 0|0. From there on, every node's children (lower weight neighbors) are placed on an
     * orbit around that parent node as a center. The radius of the orbit and the amount of angle assigned to each
     * child around its parent depends on that child's own weight.
     * <p>
     * At the end of the calculation, {@link GraphNode#setX(double)} and {@link GraphNode#setY(double)} is called once
     * to set the determined coordinates, where parents get called before their children.
     *
     * @param <N> The node type
     * @param <IdType> The node's identifier type
     * @param nodes The nodes to distribute; might <b>not</b> be null.
     */
    public static <N extends GraphNode<IdType>, IdType> void distribute(Collection<N> nodes) {
        // REGISTER ALL NODES
        Map<IdType, N> nodeRegistry = nodes.stream()
                .collect(Collectors.toMap(GraphNode::getIdentifier, n -> n));

        // DETERMINE THE HEAVIEST NODE; IT IS THE BEST ROOT FOR THE GRAPH
        IdType heaviestNode = nodeRegistry.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, n -> determineWeight(nodeRegistry, n.getKey(), new HashSet<>(), 1)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // FROM THE LEAVES UP: DETERMINE THE RADIUS EVERY NODE REQUIRES FOR ITSELF
        // AND ALL POSSIBLE CHILDREN ON ITS ORBIT, AS WELL AS THE ANGLES WHERE
        // THOSE CHILDREN ARE PLACED
        Map<IdType, Double> radiusRegistry = new HashMap<>(nodeRegistry.size());
        Map<IdType, Double> angleRegistry = new HashMap<>(nodeRegistry.size());
        determineRelation(nodeRegistry, radiusRegistry, angleRegistry, heaviestNode,
                Collections.singleton(heaviestNode));

        // FROM THE ROOT DOWN: DETERMINE THE POSITION WHERE TO PLACE A NODE AND
        // PLACE CHILDREN ON ITS ORBIT
        determinePosition(nodeRegistry, radiusRegistry, angleRegistry, heaviestNode,
                0, 0, 0, -1, Collections.singleton(heaviestNode));
    }

    private static <N extends GraphNode<IdType>, IdType> double determineWeight(Map<IdType, N> nodeRegistry, IdType currentNode,
                                                                                Set<IdType> used, int depth) {
        // DETERMINE CURRENT NODE
        N node = nodeRegistry.get(currentNode);

        // REGISTER CURRENT NODE'S NEIGHBORS
        used.addAll(node.getNeighbors());

        // TRIGGER DETERMINING WEIGHTS OF CURRENT NODE'S CHILDREN AND SUM THEM
        Double neighborWeights = node.getNeighbors().stream()
                .filter(n -> !used.contains(n))
                .map(n -> determineWeight(nodeRegistry, n, used, depth+1))
                .reduce(Double::sum)
                .orElse(0d);

        // DETERMINE CURRENT NODE'S OWN WEIGHT BY ADDING THE BASE WEIGHT 1
        // AND THE ACCUMULATED CHILD WEIGHTS IN RELATION TO THEIR DEPTH
        return 1 + neighborWeights / depth;
    }

    private static <N extends GraphNode<IdType>, IdType> void determineRelation(Map<IdType, N> nodeRegistry,
                                                                                Map<IdType, Double> radiusRegistry,
                                                                                Map<IdType, Double> angleRegistry,
                                                                                IdType currentNode,
                                                                                Set<IdType> used) {
        // DETERMINE CURRENT NODE
        N node = nodeRegistry.get(currentNode);

        // REGISTER CURRENT NODE'S NEIGHBORS
        Set<IdType> usedChildren = new HashSet<>(used);
        usedChildren.addAll(node.getNeighbors());

        // TRIGGER RADIUS DETERMINATION FOR CHILDREN;
        // SUM THEIR RADII AND FIND THEIR MAXIMUM
        double maxChildCircleRadius = 0;
        double childRadiiSum = 0;
        for (IdType childId: node.getNeighbors()) {
            if (!used.contains(childId)) {
                determineRelation(nodeRegistry, radiusRegistry, angleRegistry, childId, usedChildren);
                maxChildCircleRadius = Math.max(maxChildCircleRadius, radiusRegistry.get(childId));
                childRadiiSum += radiusRegistry.get(childId);
            }
        }

        // DETERMINE ANGLES FOR ALL CHILDREN
        boolean isFirst = true;
        double childAngleSum = 0;
        double parentRadiiSum = 0;
        int childCount = 0;
        for (IdType childId: node.getNeighbors()) {
            if (!used.contains(childId)) {
                // DEPENDING ON THE CHILD NODE'S RADIUS: DETERMINE THEIR SHARE OF CURRENT NODE'S ORBIT
                double childAngle = (2.0 * Math.PI * radiusRegistry.get(childId)) / childRadiiSum;
                childAngleSum += isFirst ? 0 : (childAngle / 2.0);

                // REGISTER THE CHILD NODE'S ANGLE
                angleRegistry.put(childId, childAngleSum);
                parentRadiiSum += radiusRegistry.get(childId) / Math.sin(childAngle / 2.0);

                // INCREASE PADDING FOR THE NEXT CHILD
                childAngleSum += childAngle / 2.0;
                isFirst = false;
                childCount++;
            }
        }

        // DETERMINE CURRENT NODE'S RADIUS FROM ITS CHILDREN'S RADII
        double radius = node.getRadius() + maxChildCircleRadius;
        if (node.getNeighbors().stream().filter(childId -> !used.contains(childId)).count() > 2) {
            radius = Math.max(radius, parentRadiiSum / Math.max(1, childCount) + maxChildCircleRadius);
        }
        radiusRegistry.put(currentNode, radius);
    }

    private static <N extends GraphNode<IdType>, IdType> void determinePosition(Map<IdType, N> nodeRegistry,
                                                                                Map<IdType, Double> radiusRegistry,
                                                                                Map<IdType, Double> angleRegistry,
                                                                                IdType currentNode,
                                                                                double currentX, double currentY,
                                                                                double parentX, double parentY,
                                                                                Set<IdType> used) {
        // SET POSITION OF CURRENT NODE
        N node = nodeRegistry.get(currentNode);
        node.setX(currentX);
        node.setY(currentY);
        node.setOrbit(radiusRegistry.get(currentNode));

        // REGISTER CURRENT NODE'S NEIGHBORS
        Set<IdType> usedChildren = new HashSet<>(used);
        usedChildren.addAll(node.getNeighbors());

        // DETERMINE VECTOR OF PARENT TO CURRENT
        double radius = radiusRegistry.get(currentNode);
        double parentVectorX = currentX - parentX;
        double parentVectorY = currentY - parentY;
        double parentVectorLength = Math.sqrt(parentVectorX * parentVectorX + parentVectorY * parentVectorY);
        parentVectorX = (parentVectorX / parentVectorLength) * radius;
        parentVectorY = (parentVectorY / parentVectorLength) * radius;

        // DETERMINE AT WHICH ANGLE TO START DISTRIBUTING CHILDREN;
        // THE OPTIMAL ONE IS IN THE MIDDLE OF THE SPACE THE BIGGEST CHILD,
        // AS THAT GIVES THE PARENT CONNECTION THE MOST SPACE
        double childAngleMax = node.getNeighbors().stream()
                .filter(childId -> !used.contains(childId))
                .mapToDouble(angleRegistry::get)
                .max()
                .orElse(0);

        double baseAngle = 0;
        for (IdType childId: node.getNeighbors()) {
            if (!used.contains(childId)) {
                double childAngle = angleRegistry.get(childId);
                if (childAngle == childAngleMax) {
                    baseAngle -= childAngle / 2.0;
                    break;
                } else {
                    baseAngle -= childAngle;
                }
            }
        }

        // TRIGGER SETTING POSITIONS OF CHILDREN ACCORDING TO THEIR ANGLE
        for (IdType childId: node.getNeighbors()) {
            if (!used.contains(childId)) {
                double childAngle = baseAngle + angleRegistry.get(childId);
                double childX = currentX + Math.cos(childAngle) * parentVectorX - Math.sin(childAngle) * parentVectorY;
                double childY = currentY + Math.sin(childAngle) * parentVectorX + Math.cos(childAngle) * parentVectorY;
                determinePosition(nodeRegistry, radiusRegistry, angleRegistry, childId,
                        childX, childY, currentX, currentY, usedChildren);
            }
        }
    }
}
