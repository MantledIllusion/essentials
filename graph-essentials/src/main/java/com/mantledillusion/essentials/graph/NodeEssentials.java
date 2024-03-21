package com.mantledillusion.essentials.graph;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class NodeEssentials {

    private static class Cluster<IdType> {

        private final NodeId<IdType> id;
        private final Set<NodeId<IdType>> siblingIds;
        private final int minSize;
        private final int maxSize;

        private Cluster(NodeId<IdType> id, Set<NodeId<IdType>> siblingIds, int minSize, int maxSize) {
            this.id = id;
            this.siblingIds = siblingIds;
            this.minSize = minSize;
            this.maxSize = maxSize;
        }

        private NodeId<IdType> getId() {
            return this.id;
        }

        private boolean clusterableWith() {
            return !this.siblingIds.isEmpty() && this.maxSize > this.siblingIds.size();
        }

        private int getScore() {
            return this.siblingIds.size() - this.minSize;
        }

        private boolean clusterableWith(Cluster<IdType> siblingId) {
            int size = this.id.getClusterSize() + siblingId.id.getClusterSize();
            return this.siblingIds.contains(siblingId.getId()) && this.maxSize >= size;
        }

        private void addSibling(NodeId<IdType> siblingId) {
            this.siblingIds.add(siblingId);
        }

        @SafeVarargs
        private final void removeSiblings(Cluster<IdType>... siblings) {
            this.siblingIds.removeAll(Arrays.stream(siblings).map(Cluster::getId).collect(Collectors.toSet()));
        }

        @Override
        public String toString() {
            return this.id.toString() + ' ' + this.siblingIds;
        }
    }

    private static final Comparator<Cluster<?>> COMP_SCORE_ASC = Comparator.comparingInt(Cluster::getScore);
    private static final Comparator<Cluster<?>> COMP_SCORE_DESC = COMP_SCORE_ASC.reversed();

    static <NodeType extends Node<NodeType>, IdType> Map<NodeId<IdType>, NodeType> registerNodes(Collection<Node.Registration<NodeType, IdType>> nodes) {
        return nodes.stream().collect(Collectors.toMap(registration -> new NodeId<>(registration.getIdentifier()), Node.Registration::getNode));
    }

    static <NodeType extends Node<NodeType>, IdType> Map<NodeId<IdType>, Set<NodeId<IdType>>> registerNeighbors(Map<NodeId<IdType>, NodeType> nodeRegistry,
                                                                                                                Collection<Node.Registration<NodeType, IdType>> nodes) {
        Map<NodeId<IdType>, Set<NodeId<IdType>>> neighborRegistry = new HashMap<>();

        nodes.forEach(registration -> {
            NodeId<IdType> nodeId = new NodeId<>(registration.getIdentifier());
            Set<IdType> neighbors = registration.getNeighbors();
            if (neighbors == null || neighbors.isEmpty()) {
                neighborRegistry.computeIfAbsent(nodeId, id -> new HashSet<>());
            } else {
                for (IdType neighbor : neighbors) {
                    NodeId<IdType> neighborId = new NodeId<>(neighbor);
                    if (!nodeRegistry.containsKey(neighborId)) {
                        throw new IllegalArgumentException(String.format("The neighbor %s of node %s is unknown",
                                neighbor, nodeId));
                    }

                    // REGISTER NODE -> NEIGHBOR
                    neighborRegistry
                            .computeIfAbsent(nodeId, id -> new HashSet<>())
                            .add(neighborId);

                    // REGISTER NEIGHBOR -> NODE
                    neighborRegistry
                            .computeIfAbsent(neighborId, id -> new HashSet<>())
                            .add(nodeId);
                }
            }
        });

        return neighborRegistry;
    }

    static <IdType> void determineDepths(Map<NodeId<IdType>, Set<NodeId<IdType>>> neighborRegistry,
                                         Map<NodeId<IdType>, Integer> depthRegistry,
                                         NodeId<IdType> currentNode,
                                         Integer depth) {
        // ITERATE CURRENT NODE'S NEIGHBORS
        for (NodeId<IdType> neighbor: neighborRegistry.get(currentNode)) {
            // DETERMINE WHETHER THE NEIGHBORS DEPTH HAS NOT BEEN DETERMINED,
            // OR ITS DEPTH FROM THE CURRENT NODE IS SHALLOWER THAN DETERMINED PREVIOUSLY
            if (!depthRegistry.containsKey(neighbor) || depthRegistry.get(neighbor) > depth) {
                depthRegistry.put(neighbor, depth);
                determineDepths(neighborRegistry, depthRegistry, currentNode, depth + 1);
            }
        }
    }

    static <NodeType extends Node<NodeType>, IdType> Set<NodeId<IdType>> clusterSiblings(Map<NodeId<IdType>, NodeType> nodeRegistry,
                                                                                         Map<NodeId<IdType>, Set<NodeId<IdType>>> neighborRegistry,
                                                                                         NodeId<IdType> currentNode,
                                                                                         Set<NodeId<IdType>> excluded) {
        // DETERMINE ALL NEIGHBORS THAT HAVE TO BE CONTAINED BY EXACTLY ONE CLUSTER ...
        Set<NodeId<IdType>> neighbors = new HashSet<>(neighborRegistry.get(currentNode));

        // ... BUT WHICH ARE NOT EXPLICITLY EXCLUDED
        neighbors.removeAll(excluded);

        // REGISTER EACH NEIGHBOR AS A POTENTIAL CLUSTER
        Map<NodeId<IdType>, Cluster<IdType>> clusterRegistry = new HashMap<>();
        neighbors.forEach(neighborId -> clusterRegistry.put(neighborId, new Cluster<>(
                neighborId, collectSiblings(nodeRegistry, neighbors, neighborId),
                nodeRegistry.get(neighborId).getMinClusterSize(), nodeRegistry.get(neighborId).getMaxClusterSize()))
        );

        // LOOP CLUSTERING
        clusterLoop: while (true) {
            // ITERATE OVER ALL CURRENTLY KNOWN CLUSTERS
            List<Cluster<IdType>> neighborQueue = clusterRegistry.values().stream()
                    // FILTER FOR SUCH CLUSTERS THAT ARE GENERALLY ABLE TO MERGE WITH OTHER CLUSTERS
                    .filter(Cluster::clusterableWith)
                    // SORT CLUSTERS BY THEIR SCORE
                    .sorted(COMP_SCORE_DESC)
                    // COLLECT THEM IN ORDER
                    .collect(Collectors.toList());

            // ITERATE OVER ORDERED NEIGHBORS FOR A NEIGHBOR TO MERGE
            for (Cluster<IdType> neighbor: neighborQueue) {
                // ITERATE OVER ORDERED NEIGHBORS FOR A SIBLING TO MERGE WITH
                for (Cluster<IdType> sibling: neighborQueue) {
                    // CHECK IF NEIGHBOR AND SIBLING CAN CLUSTER WITH EACH OTHER
                    if (neighbor != sibling && neighbor.clusterableWith(sibling) && sibling.clusterableWith(neighbor)) {
                        // CLUSTER IDS
                        NodeId<IdType> clusterId = neighbor.getId().clusterWith(sibling.getId());

                        // CLUSTER NODES
                        NodeType clusterNode = nodeRegistry.get(neighbor.getId()).clusterWith(nodeRegistry.get(sibling.getId()));

                        // REMOVE THE CLUSTERED NEIGHBOR FROM NODE REGISTRY
                        nodeRegistry.remove(neighbor.getId());

                        // REMOVE THE CLUSTERED NEIGHBOR FROM NODE REGISTRY
                        nodeRegistry.remove(sibling.getId());

                        // ADD THE CREATED CLUSTER TO NODE REGISTRY
                        nodeRegistry.put(clusterId, clusterNode);

                        // REMOVE AND ITERATE ALL NEIGHBORS OF THE CLUSTERED NEIGHBOR
                        Set<NodeId<IdType>> clusterNeighbors = new HashSet<>();
                        neighborRegistry.remove(neighbor.getId()).stream()
                                .filter(other -> !Objects.equals(other, sibling.getId()))
                                // REPLACE THE CLUSTERED NEIGHBOR OF SUCH NEIGHBORS ...
                                .peek(other -> neighborRegistry.get(other).remove(neighbor.getId()))
                                // ... WITH THE CREATED CLUSTER IN THE NEIGHBOR REGISTRY ...
                                .peek(other -> neighborRegistry.get(other).add(clusterId))
                                // ... AND REGISTER IT AS A NEIGHBOR TO THE CREATED CLUSTER
                                .forEach(clusterNeighbors::add);

                        // REMOVE AND ITERATE ALL NEIGHBORS OF THE CLUSTERED SIBLING
                        neighborRegistry.remove(sibling.getId()).stream()
                                .filter(other -> !Objects.equals(other, neighbor.getId()))
                                // REPLACE THE CLUSTERED SIBLING OF SUCH NEIGHBORS ...
                                .peek(other -> neighborRegistry.get(other).remove(sibling.getId()))
                                // ... WITH THE CREATED CLUSTER IN THE NEIGHBOR REGISTRY ...
                                .peek(other -> neighborRegistry.get(other).add(clusterId))
                                // ... AND REGISTER IT AS A NEIGHBOR TO THE CREATED CLUSTER
                                .forEach(clusterNeighbors::add);

                        // REGISTER NEIGHBORS OF THE CREATED CLUSTER IN THE NEIGHBOR REGISTRY
                        neighborRegistry.put(clusterId, clusterNeighbors);

                        // REMOVE THE CLUSTERED NEIGHBOR FROM NODE REGISTRY
                        clusterRegistry.remove(neighbor.getId());

                        // REMOVE THE CLUSTERED SIBLING FROM NODE REGISTRY
                        clusterRegistry.remove(sibling.getId());

                        // ITERATE OVER ALL CLUSTERS
                        clusterRegistry.values().stream()
                                // REMOVE CLUSTERED NEIGHBOR AND SIBLING FROM THEIR SIBLINGS
                                .peek(other -> other.removeSiblings(neighbor, sibling))
                                // FILTER FOR SUCH CLUSTERS THAT CAN BE CLUSTERED WITH THE CREATED CLUSTER
                                .filter(other -> nodeRegistry.get(other.getId()).clusterableWith(clusterNode) == Node.Clusterability.SIBLINGS)
                                // ADD THE CREATED CLUSTER AS A SIBLING
                                .forEach(other -> other.addSibling(clusterId));

                        // RETRIEVE ALL SIBLINGS FOR THE CREATED CLUSTER
                        Set<NodeId<IdType>> others = collectSiblings(nodeRegistry, clusterRegistry.keySet(), clusterId);

                        // REGISTER THE CREATED CLUSTER IN THE CLUSTER REGISTRY
                        clusterRegistry.put(clusterId, new Cluster<>(clusterId, others,
                                clusterNode.getMinClusterSize(), clusterNode.getMaxClusterSize()));

                        // RESTART CLUSTERING AS NEIGHBORS HAVE CHANGED
                        continue clusterLoop;
                    }
                }
            }

            //
            break;
        }

        // RETURN ALL CLUSTERS
        return clusterRegistry.keySet();
    }

    private static <NodeType extends Node<NodeType>, IdType> Set<NodeId<IdType>> collectSiblings(Map<NodeId<IdType>, NodeType> nodeRegistry,
                                                                                                 Set<NodeId<IdType>> neighbors,
                                                                                                 NodeId<IdType> neighborId) {
        // ITERATE ALL OTHER NEIGHBORS ...
        return neighbors.stream()
                // ... WHICH ARE NOT THE NEIGHBOR ITSELF, BUT ...
                .filter(other -> !Objects.equals(other, neighborId))
                // ... WITH WHICH THE NEIGHBOR ALLOWS TO BE CLUSTERED
                .filter(other -> nodeRegistry.get(neighborId).clusterableWith(nodeRegistry.get(other)) == Node.Clusterability.SIBLINGS)
                // COLLECT THEM
                .collect(Collectors.toSet());
    }

    static <T> Set<T> union(Set<T> these, Set<T> those) {
        return Stream.concat(these.stream(), those.stream()).collect(Collectors.toSet());
    }
}
