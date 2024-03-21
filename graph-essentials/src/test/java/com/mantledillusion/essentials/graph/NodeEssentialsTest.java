package com.mantledillusion.essentials.graph;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class NodeEssentialsTest {

    @Test
    public void testDoNotCluster() {
        Set<NodeId<String>> clusters = cluster(
                new TestNode(1).register("A"),
                new TestNode(2).register("B")
        );

        assertContainsAllNodes(clusters, "A", "B");
        assertContainsCluster(clusters, "A");
        assertContainsCluster(clusters, "B");
    }

    @Test
    public void testDoCluster() {
        Set<NodeId<String>> clusters = cluster(
                new TestNode(1).register("A"),
                new TestNode(1).register("B")
        );

        assertContainsAllNodes(clusters, "A", "B");
        assertContainsCluster(clusters, "A", "B");
    }

    @Test
    public void testPrioritizeClusterSize() {
        Set<NodeId<String>> clusters = cluster(
                new TestNode(1, 3).register("A"),
                new TestNode(2, 3).register("B"),
                new TestNode(2, 4).register("C"),
                new TestNode(2, 4).register("D"),
                new TestNode(2, 4).register("E")
        );

        assertContainsAllNodes(clusters, "A", "B", "C", "D", "E");
        assertContainsCluster(clusters, "A");
        assertContainsCluster(clusters, "B", "C", "D", "E");
    }

    @SafeVarargs
    private final Set<NodeId<String>> cluster(Node.Registration<TestNode, String>... children) {
        String[] childIds = Arrays.stream(children)
                .map(Node.Registration::getIdentifier)
                .toArray(String[]::new);

        Node.Registration<TestNode, String> parent =  new TestNode().register("X", childIds);

        List<Node.Registration<TestNode, String>> nodes = Stream
                .concat(Stream.of(parent), Arrays.stream(children))
                .collect(Collectors.toList());

        Map<NodeId<String>, TestNode> nodeRegistry = NodeEssentials.registerNodes(nodes);

        Map<NodeId<String>, Set<NodeId<String>>> neighborRegistry = NodeEssentials.registerNeighbors(nodeRegistry, nodes);

        return NodeEssentials.clusterSiblings(nodeRegistry, neighborRegistry, new NodeId<>("X"), Collections.singleton(new NodeId<>("X")));
    }

    private void assertContainsAllNodes(Set<NodeId<String>> clusters, String... nodeIds) {
        Set<String> nodes = Arrays.stream(nodeIds).collect(Collectors.toSet());

        clusters.stream().reduce((cluster, other) -> {
            assertFalse(cluster.getIds().stream().anyMatch(other.getIds()::contains),
                    String.format("The clusters %s and %s have at least one overlap", cluster, other));
            return cluster.clusterWith(other);
        }).map(NodeId::getIds).orElse(Collections.emptySet()).forEach(nodes::remove);

        assertTrue(nodes.isEmpty(), String.format("The clusters %s do not contain the children %s", clusters, nodes));
    }

    private void assertContainsCluster(Set<NodeId<String>> clusters, String... clusterIds) {
        Set<String> cluster = Arrays.stream(clusterIds).collect(Collectors.toSet());
        assertTrue(clusters.stream().map(NodeId::getIds).anyMatch(other -> other.equals(cluster)),
                String.format("The clusters %s to not contain the subset %s", clusters, cluster));
    }
}
