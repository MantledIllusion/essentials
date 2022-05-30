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
         * Relations be either uni- or bi-directional; it won't have any effect on the distribution.
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
     * In the {@link Collection} of {@link GraphNode}s given, there might be 1 or even multiple sub-sets of nodes which
     * are connected; if there are multiple, the center of the graph will be the node of the heaviest sub-set, while the
     * heaviest nodes of the other sets are placed on its orbit.
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
                .collect(Collectors.toMap(GraphNode::getIdentifier, node -> node));

        // REGISTER NEIGHBORS BI-DIRECTIONALLY
        Map<IdType, Set<IdType>> neighborRegistry = new HashMap<>();
        nodes.forEach(node -> {
            Set<IdType> neighbors = node.getNeighbors();
            if (neighbors == null || neighbors.isEmpty()) {
                neighborRegistry.put(node.getIdentifier(), new HashSet<>());
            } else {
                for (IdType neighbor: neighbors) {
                    if (!nodeRegistry.containsKey(neighbor)) {
                        throw new IllegalArgumentException(String.format("The neighbor %s of node %s is unknown",
                                neighbor, node.getIdentifier()));
                    }

                    // REGISTER NODE -> NEIGHBOR
                    neighborRegistry
                            .computeIfAbsent(node.getIdentifier(), id -> new HashSet<>())
                            .add(neighbor);

                    // REGISTER NEIGHBOR -> NODE
                    neighborRegistry
                            .computeIfAbsent(neighbor, id -> new HashSet<>())
                            .add(node.getIdentifier());
                }
            }
        });

        // DETERMINE THE HEAVIEST NODE OF ALL; IT IS THE BEST ROOT FOR THE GRAPH
        IdType graphRoot = determineHeaviestNode(neighborRegistry.keySet(), neighborRegistry);

        // IF THERE IS NO GRAPH ROOT, THE GRAPH IS EMPTY
        if (graphRoot != null) {
            // THERE MIGHT BE SETS OF NODES UNCONNECTED TO EACH OTHER; DETERMINE ALL OF THESE SUB GRAPHS
            determineSubGraphs(neighborRegistry).stream()
                    // FILTER OUR THE ONE SUB GRAPH CONTAINING THE ROOT OF THE WHOLE GRAPH
                    .filter(subGraph -> !subGraph.contains(graphRoot))
                    // DETERMINE THE HEAVIEST NODES OF THE SUB GRAPH
                    .map(subGraph -> determineHeaviestNode(subGraph, neighborRegistry))
                    // ADD THAT SUB NODE AS A NEIGHBOR TO THE ROOT
                    .forEach(subGraphRoot -> neighborRegistry.get(graphRoot).add(subGraphRoot));

            // FROM THE LEAVES UP: DETERMINE THE RADIUS EVERY NODE REQUIRES FOR ITSELF
            // AND ALL POSSIBLE CHILDREN ON ITS ORBIT, AS WELL AS THE ANGLES WHERE
            // THOSE CHILDREN ARE PLACED
            Map<IdType, Double> radiusRegistry = new HashMap<>(nodeRegistry.size());
            Map<IdType, Double> angleRegistry = new HashMap<>(nodeRegistry.size());
            determineRelation(nodeRegistry, neighborRegistry, radiusRegistry, angleRegistry, graphRoot,
                    Collections.singleton(graphRoot));

            // FROM THE ROOT DOWN: DETERMINE THE POSITION WHERE TO PLACE A NODE AND
            // PLACE CHILDREN ON ITS ORBIT
            determinePosition(nodeRegistry, neighborRegistry, radiusRegistry, angleRegistry, graphRoot,
                    0, 0, 0, -1, Collections.singleton(graphRoot));
        }
    }

    private static <IdType> List<Set<IdType>> determineSubGraphs(Map<IdType, Set<IdType>> neighborRegistry) {
        Map<IdType, Set<IdType>> graphs = new HashMap<>();
        neighborRegistry.keySet().stream()
                .filter(node -> !graphs.containsKey(node))
                .forEach(node -> determineSubGraph(new HashSet<>(), graphs, neighborRegistry, node));
        return new ArrayList<>(graphs.values());
    }

    private static <IdType> void determineSubGraph(Set<IdType> graph,
                                                   Map<IdType, Set<IdType>> graphs,
                                                   Map<IdType, Set<IdType>> neighborRegistry,
                                                   IdType currentNode) {
        graph.add(currentNode);
        if (graphs.containsKey(currentNode)) {
            for (IdType neighbor: graphs.get(currentNode)) {
                graph.add(neighbor);
                graphs.put(neighbor, graph);
            }
            graphs.put(currentNode, graph);
        } else {
            graphs.put(currentNode, graph);
            for (IdType neighbor : neighborRegistry.get(currentNode)) {
                determineSubGraph(graph, graphs, neighborRegistry, neighbor);
            }
        }
    }

    private static <IdType> IdType determineHeaviestNode(Set<IdType> graph, Map<IdType, Set<IdType>> neighborRegistry) {
        return graph.stream()
                .collect(Collectors.toMap(n -> n, n -> determineWeight(neighborRegistry, n, new HashSet<>(), 1)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static <IdType> double determineWeight(Map<IdType, Set<IdType>> neighborRegistry,
                                                   IdType currentNode,
                                                   Set<IdType> used,
                                                   int depth) {
        // REGISTER CURRENT NODE
        used.add(currentNode);

        // TRIGGER DETERMINING WEIGHTS OF CURRENT NODE'S CHILDREN AND SUM THEM
        Double neighborWeights = neighborRegistry.get(currentNode).stream()
                .filter(n -> !used.contains(n))
                .map(n -> determineWeight(neighborRegistry, n, used, depth+1))
                .reduce(Double::sum)
                .orElse(0d);

        // DETERMINE CURRENT NODE'S OWN WEIGHT BY ADDING THE BASE WEIGHT 1
        // AND THE ACCUMULATED CHILD WEIGHTS IN RELATION TO THEIR DEPTH
        return 1 + neighborWeights / depth;
    }

    private static <N extends GraphNode<IdType>, IdType> void determineRelation(Map<IdType, N> nodeRegistry,
                                                                                Map<IdType, Set<IdType>> neighborRegistry,
                                                                                Map<IdType, Double> radiusRegistry,
                                                                                Map<IdType, Double> angleRegistry,
                                                                                IdType currentNode,
                                                                                Set<IdType> used) {
        // DETERMINE CURRENT NODE
        N node = nodeRegistry.get(currentNode);

        // REGISTER CURRENT NODE'S NEIGHBORS
        Set<IdType> usedChildren = new HashSet<>(used);
        usedChildren.addAll(neighborRegistry.get(currentNode));

        // TRIGGER RADIUS DETERMINATION FOR CHILDREN;
        // SUM THEIR RADII AND FIND THEIR MAXIMUM
        double maxChildCircleRadius = 0;
        double childRadiiSum = 0;
        for (IdType childId: neighborRegistry.get(currentNode)) {
            if (!used.contains(childId)) {
                determineRelation(nodeRegistry, neighborRegistry, radiusRegistry, angleRegistry, childId, usedChildren);
                maxChildCircleRadius = Math.max(maxChildCircleRadius, radiusRegistry.get(childId));
                childRadiiSum += radiusRegistry.get(childId);
            }
        }

        // DETERMINE ANGLES FOR ALL CHILDREN
        boolean isFirst = true;
        double childAngleSum = 0;
        double parentRadiiSum = 0;
        int childCount = 0;
        for (IdType childId: neighborRegistry.get(currentNode)) {
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
        if (neighborRegistry.get(currentNode).stream().filter(childId -> !used.contains(childId)).count() > 2) {
            radius = Math.max(radius, parentRadiiSum / Math.max(1, childCount) + maxChildCircleRadius);
        }
        radiusRegistry.put(currentNode, radius);
    }

    private static <N extends GraphNode<IdType>, IdType> void determinePosition(Map<IdType, N> nodeRegistry,
                                                                                Map<IdType, Set<IdType>> neighborRegistry,
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
        usedChildren.addAll(neighborRegistry.get(currentNode));

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
        double childAngleMax = neighborRegistry.get(currentNode).stream()
                .filter(childId -> !used.contains(childId))
                .mapToDouble(angleRegistry::get)
                .max()
                .orElse(0);

        double baseAngle = 0;
        for (IdType childId: neighborRegistry.get(currentNode)) {
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
        for (IdType childId: neighborRegistry.get(currentNode)) {
            if (!used.contains(childId)) {
                double childAngle = baseAngle + angleRegistry.get(childId);
                double childX = currentX + Math.cos(childAngle) * parentVectorX - Math.sin(childAngle) * parentVectorY;
                double childY = currentY + Math.sin(childAngle) * parentVectorX + Math.cos(childAngle) * parentVectorY;
                determinePosition(nodeRegistry, neighborRegistry, radiusRegistry, angleRegistry, childId,
                        childX, childY, currentX, currentY, usedChildren);
            }
        }
    }
}
