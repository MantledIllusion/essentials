package com.mantledillusion.essentials.graph;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NodeEssentials {

    private static final Comparator<Set<?>> COMP_SET_SIZE_ASC = Comparator.comparingInt(Set::size);
    private static final Comparator<Set<?>> COMP_SET_SIZE_DESC = COMP_SET_SIZE_ASC.reversed();

    public static <NodeType extends Node<NodeType>, IdType> Map<NodeId<IdType>, NodeType> registerNodes(Collection<Node.Registration<NodeType, IdType>> nodes) {
        return nodes.stream().collect(Collectors.toMap(registration -> new NodeId<>(registration.getIdentifier()), Node.Registration::getNode));
    }

    public static <NodeType extends Node<NodeType>, IdType> Map<NodeId<IdType>, Set<NodeId<IdType>>> registerNeighbors(Map<NodeId<IdType>, NodeType> nodeRegistry,
                                                                                                                       Collection<Node.Registration<NodeType, IdType>> nodes) {
        Map<NodeId<IdType>, Set<NodeId<IdType>>> neighborRegistry = new HashMap<>();

        nodes.forEach(registration -> {
            NodeId<IdType> nodeId = new NodeId<>(registration.getIdentifier());
            Set<IdType> neighbors = registration.getNeighbors();
            if (neighbors == null || neighbors.isEmpty()) {
                neighborRegistry.put(nodeId, new HashSet<>());
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

    public static <IdType> void determineDepths(Map<NodeId<IdType>, Set<NodeId<IdType>>> neighborRegistry,
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

    public static <NodeType extends Node<NodeType>, IdType> Map<NodeId<IdType>, Set<NodeId<IdType>>> clusterSiblings(Map<NodeId<IdType>, NodeType> nodeRegistry,
                                                                                                                     Map<NodeId<IdType>, Set<NodeId<IdType>>> neighborRegistry,
                                                                                                                     NodeId<IdType> currentNode,
                                                                                                                     Set<NodeId<IdType>> excluded) {
        // ITERATE OVER ALL NEIGHBORS OF THE CURRENT NODE
        Map<NodeId<IdType>, Set<NodeId<IdType>>> candidateRegistry = neighborRegistry.get(currentNode).stream()
                .collect(Collectors.toMap(
                        // REGISTER EACH NEIGHBOR ...
                        neighbor -> neighbor,
                        // ... WITH ALL OTHER NEIGHBORS ...
                        neighbor -> neighborRegistry.get(currentNode).stream()
                                // ... WHICH ARE NOT EXPLICITLY EXCLUDED ...
                                .filter(other -> !excluded.contains(other))
                                // ... AND ARE NOT THE NEIGHBOR ITSELF, BUT ...
                                .filter(other -> !Objects.equals(other, neighbor))
                                // ... WITH WHICH THE NEIGHBOR ALLOWS TO BE CLUSTERED
                                .filter(other -> nodeRegistry.get(neighbor).clusterableWith(nodeRegistry.get(other)) == Node.Clusterability.SIBLINGS)
                                // COLLECT THEM
                                .collect(Collectors.toSet())
                ));

        // DETERMINE ALL NEIGHBORS THAT HAVE TO BE CONTAINED BY EXACTLY ONE CLUSTER ...
        Set<NodeId<IdType>> neighbors = new HashSet<>(neighborRegistry.get(currentNode));

        // ... BUT WHICH ARE NOT EXPLICITLY EXCLUDED
        neighbors.removeAll(excluded);

        // ITERATE OVER ALL POTENTIAL CLUSTERS
        return candidateRegistry.entrySet().stream()
                // GENERATE ALL POSSIBLE PERMUTATIONS OF SUB SETS FROM NEIGHBOR CANDIDATE SETS
                .flatMap(entry -> Stream.concat(
                        Stream.of(Collections.singleton(entry.getKey())),
                        streamSubSets(entry.getValue()).map(subSet -> NodeEssentials.union(Collections.singleton(entry.getKey()), subSet)))
                )
                // ELIMINATE EQUALING CLUSTERS
                .distinct()
                // ITERATE OVER ALL CLUSTERS ...
                .map(cluster -> cluster.stream()
                        // ... AND THEIR INDIVIDUAL NODES ...
                        .map(Collections::singleton)
                        // ... AND RECLUSTER THEM BASED ON WHETHER THE NODES ARE CANDIDATES OF EACH OTHER
                        .reduce((these, those) -> these.isEmpty() || those.isEmpty()
                                ? Collections.emptySet()
                                : (these.stream().allMatch(neighbor -> candidateRegistry.get(neighbor).containsAll(those))
                                && those.stream().allMatch(neighbor -> candidateRegistry.get(neighbor).containsAll(these))
                                ? NodeEssentials.union(these, those)
                                : Collections.emptySet()))
                        .orElse(Collections.emptySet()))
                // FILTER OUT EMPTY CLUSTERS
                .filter(cluster -> !cluster.isEmpty())
                // FILTER OUT CLUSTERS ...
                .filter(cluster -> cluster.stream()
                        // ... WHERE AT LEAST ONE OF THE NODES REQUIRES A BIGGER CLUSTER
                        .allMatch(child -> candidateRegistry.get(child).isEmpty()
                                || nodeRegistry.get(child).getMinClusterSize() <= cluster.size()))
                // FILTER OUT CLUSTERS ...
                .filter(cluster -> cluster.stream()
                        // ... WHERE AT LEAST ONE OF THE NODES REQUIRES A SMALLER CLUSTER
                        .allMatch(child -> candidateRegistry.get(child).isEmpty()
                                || nodeRegistry.get(child).getMaxClusterSize() >= cluster.size()))
                // SORT CLUSTERS BY SIZE TO HAVE THE LARGEST CLUSTERS FIRST
                .sorted(COMP_SET_SIZE_DESC)
                // FILTER CLUSTERS FOR THOSE WHOSE NODES ARE STILL AVAILABLE
                // TODO correct like this or does filter and peek need to be done in one step?
                .filter(neighbors::containsAll)
                .peek(neighbors::removeAll)
                // COLLECT CLUSTERS
                .collect(Collectors.toMap(
                        cluster -> cluster.stream()
                                .reduce(NodeId::clusterWith)
                                .orElseThrow(IllegalStateException::new),
                        cluster -> cluster
                ));
    }

    public static <IdType> Stream<Set<NodeId<IdType>>> streamSubSets(Set<NodeId<IdType>> children) {
        if (children.isEmpty()) {
            return Stream.empty();
        } else {
            Set<NodeId<IdType>> child = Collections.singleton(children.iterator().next());
            Set<NodeId<IdType>> others = children.stream()
                    .filter(o -> !child.contains(o))
                    .collect(Collectors.toSet());
            return Stream.concat(Stream.of(child), streamSubSets(others).flatMap(o -> Stream.concat(Stream.of(o), Stream.of(NodeEssentials.union(o, child)))));
        }
    }

    public static <T> Set<T> union(Set<T> these, Set<T> those) {
        return Stream.concat(these.stream(), those.stream()).collect(Collectors.toSet());
    }
}
