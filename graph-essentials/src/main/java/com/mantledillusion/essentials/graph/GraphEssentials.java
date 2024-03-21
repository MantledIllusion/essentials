package com.mantledillusion.essentials.graph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Static utility that is able to distribute a graph's nodes on a 2-dimensional surface with the littlest interference.
 */
public class GraphEssentials {

    /**
     * Distributes the given nodes on a planetary system around the graphs weighted center.
     * <p>
     * The node at the weighted center (highest weight when considering all children and their respective weights) is
     * placed on the coordinates 0|0. From there on, every node's children (lower weight neighbors) are placed on an
     * orbit around that parent node as a center. The radius of the orbit and the amount of angle assigned to each
     * child around its parent depends on that child's own weight.
     * <p>
     * In the {@link Collection} of {@link Node.Registration}s given, there might be 1 or even multiple sub-sets of
     * nodes which are connected; if there are multiple, the center of the graph will be the node of the heaviest
     * sub-set, while the heaviest nodes of the other sets are placed on its orbit.
     * <p>
     * At the end of the calculation, {@link Node#setX(double)} and {@link Node#setY(double)} is called once
     * to set the determined coordinates, where parents get called before their children.
     *
     * @param <NodeType> The node type
     * @param <IdType> The node's identifier type
     * @param registrations The {@link Node.Registration}s of all @{@link Node}s to distribute; might <b>not</b> be null.
     * @return A {@link Collection} of nodes that have been distributed after forming possible clusters, never null
     */
    public static <NodeType extends Node<NodeType>, IdType> Collection<Node.Registration<NodeType, NodeId<IdType>>> distribute(Collection<Node.Registration<NodeType, IdType>> registrations) {
        // REGISTER ALL NODES
        Map<NodeId<IdType>, NodeType> nodeRegistry = NodeEssentials.registerNodes(registrations);

        // REGISTER NEIGHBORS BI-DIRECTIONALLY
        Map<NodeId<IdType>, Set<NodeId<IdType>>> neighborRegistry = NodeEssentials.registerNeighbors(nodeRegistry, registrations);

        // DETERMINE THE HEAVIEST NODE OF ALL; IT IS THE BEST ROOT FOR THE GRAPH
        NodeId<IdType> graphRoot = determineHeaviestNode(neighborRegistry.keySet(), neighborRegistry);

        // IF THERE IS NO GRAPH ROOT, THE GRAPH IS EMPTY
        if (graphRoot != null) {
            // THERE MIGHT BE SETS OF NODES UNCONNECTED TO EACH OTHER; DETERMINE ALL OF THESE SUB GRAPHS
            determineSubGraphs(neighborRegistry).stream()
                    // FILTER OUT THE ALL SUB GRAPHS CONTAINING THE ROOT OF THE WHOLE GRAPH
                    .filter(subGraph -> !subGraph.contains(graphRoot))
                    // DETERMINE THE HEAVIEST NODES FOR EACH OF THESE UNCONNECTED SUB GRAPHS
                    .map(subGraph -> determineHeaviestNode(subGraph, neighborRegistry))
                    // ADD THAT SUB GRAPH ROOT NODE AS A NEIGHBOR TO THE ROOT
                    .peek(subGraphRoot -> neighborRegistry.get(graphRoot).add(subGraphRoot))
                    .forEach(subGraphRoot -> neighborRegistry.get(subGraphRoot).add(graphRoot));

            // CLUSTER CHILD NODES FROM THE ROOT DOWN
            determineClusters(nodeRegistry, neighborRegistry, graphRoot, new HashSet<>(Collections.singleton(graphRoot)));

            // FROM THE LEAVES UP: DETERMINE THE RADIUS EVERY NODE REQUIRES FOR ITSELF
            // AND ALL POSSIBLE CHILDREN ON ITS ORBIT, AS WELL AS THE ANGLES WHERE
            // THOSE CHILDREN ARE PLACED
            Map<NodeId<IdType>, Double> radiusRegistry = new HashMap<>(nodeRegistry.size());
            Map<NodeId<IdType>, Double> angleRegistry = new HashMap<>(nodeRegistry.size());
            determineRelation(nodeRegistry, neighborRegistry, radiusRegistry, angleRegistry, graphRoot,
                    Collections.singleton(graphRoot));

            // FROM THE ROOT DOWN: DETERMINE THE POSITION WHERE TO PLACE A NODE AND
            // PLACE CHILDREN ON ITS ORBIT
            determinePosition(nodeRegistry, neighborRegistry, radiusRegistry, angleRegistry, graphRoot,
                    0, 0, 0, -1, Collections.singleton(graphRoot));
        }

        return nodeRegistry.entrySet().stream()
                .map(entry -> entry.getValue().register(entry.getKey(), neighborRegistry.get(entry.getKey())))
                .collect(Collectors.toList());
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

    private static <IdType> NodeId<IdType> determineHeaviestNode(Set<NodeId<IdType>> graph,
                                                                 Map<NodeId<IdType>, Set<NodeId<IdType>>> neighborRegistry) {
        return graph.stream()
                .collect(Collectors.toMap(n -> n, n -> determineWeight(neighborRegistry, n)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static <IdType> double determineWeight(Map<NodeId<IdType>, Set<NodeId<IdType>>> neighborRegistry,
                                                   NodeId<IdType> currentNode) {
        // REGISTER THE DEPTHS FROM THE CURRENT NODE
        Map<NodeId<IdType>, Integer> depthRegistry = new HashMap<>();
        depthRegistry.put(currentNode, 1);

        // DETERMINE THE DEPTHS IF THE CURRENT NODE WAS THE ROOT
        NodeEssentials.determineDepths(neighborRegistry, depthRegistry, currentNode, 2);

        // ITERATE ALL DEPTHS
        return depthRegistry.values().stream()
                // DETERMINE THE WEIGHT OF EACH NODE DEPENDING ON ITS DEPTH FROM THE ROOT
                .mapToDouble(nodeDepth -> 1.0 / nodeDepth)
                // SUM UP ALL WEIGHTS
                .sum();
    }

    private static <NodeType extends Node<NodeType>, IdType> void determineClusters(Map<NodeId<IdType>, NodeType> nodeRegistry,
                                                                                    Map<NodeId<IdType>, Set<NodeId<IdType>>> neighborRegistry,
                                                                                    NodeId<IdType> currentNode,
                                                                                    Set<NodeId<IdType>> used) {
        // CLUSTER NODES AND UPDATE REGISTRIES
        Set<NodeId<IdType>> clusters = NodeEssentials.clusterSiblings(nodeRegistry, neighborRegistry, currentNode, used);

        // REGISTER NEW FOUND CLUSTERS AS USED
        used.addAll(clusters);

        // RECURSIVELY CLUSTER NEIGHBORS OF CLUSTERS
        for (NodeId<IdType> clusterId: clusters) {
            determineClusters(nodeRegistry, neighborRegistry, clusterId, used);
        }
    }

    private static <NodeType extends Node<NodeType>, IdType> void determineRelation(Map<NodeId<IdType>, NodeType> nodeRegistry,
                                                                                    Map<NodeId<IdType>, Set<NodeId<IdType>>> neighborRegistry,
                                                                                    Map<NodeId<IdType>, Double> radiusRegistry,
                                                                                    Map<NodeId<IdType>, Double> angleRegistry,
                                                                                    NodeId<IdType> currentNode,
                                                                                    Set<NodeId<IdType>> used) {
        // DETERMINE CURRENT NODE
        NodeType node = nodeRegistry.get(currentNode);

        // REGISTER CURRENT NODE'S NEIGHBORS
        Set<NodeId<IdType>> usedChildren = new HashSet<>(used);
        usedChildren.addAll(neighborRegistry.get(currentNode));

        // TRIGGER RADIUS DETERMINATION FOR CHILDREN;
        // SUM THEIR RADII AND FIND THEIR MAXIMUM
        double maxChildCircleRadius = 0;
        double childRadiiSum = 0;
        for (NodeId<IdType> childId: neighborRegistry.get(currentNode)) {
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
        for (NodeId<IdType> childId: neighborRegistry.get(currentNode)) {
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

    private static <NodeType extends Node<NodeType>, IdType> void determinePosition(Map<NodeId<IdType>, NodeType> nodeRegistry,
                                                                                    Map<NodeId<IdType>, Set<NodeId<IdType>>> neighborRegistry,
                                                                                    Map<NodeId<IdType>, Double> radiusRegistry,
                                                                                    Map<NodeId<IdType>, Double> angleRegistry,
                                                                                    NodeId<IdType> currentNode,
                                                                                    double currentX, double currentY,
                                                                                    double parentX, double parentY,
                                                                                    Set<NodeId<IdType>> used) {
        // SET POSITION OF CURRENT NODE
        NodeType node = nodeRegistry.get(currentNode);
        node.setX(currentX);
        node.setY(currentY);
        node.setOrbit(radiusRegistry.get(currentNode));

        // REGISTER CURRENT NODE'S NEIGHBORS
        Set<NodeId<IdType>> usedChildren = new HashSet<>(used);
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
        for (NodeId<IdType> childId: neighborRegistry.get(currentNode)) {
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
        for (NodeId<IdType> childId: neighborRegistry.get(currentNode)) {
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