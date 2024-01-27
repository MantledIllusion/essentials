package com.mantledillusion.essentials.graph;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class NodeEssentialsTest {

    @Test
    public void testDoNotCluster() {
        Collection<Set<NodeId<String>>> clusters = cluster(
                new TestNode(1).register("A"),
                new TestNode(2).register("B")
        );

        assertContainsAll(clusters, "A", "B");
        assertContainsSubset(clusters, "A");
        assertContainsSubset(clusters, "B");
    }

    @Test
    public void testDoCluster() {
        Collection<Set<NodeId<String>>> clusters = cluster(
                new TestNode(1).register("A"),
                new TestNode(1).register("B")
        );

        assertContainsAll(clusters, "A", "B");
        assertContainsSubset(clusters, "A", "B");
    }

    @Test
    public void testPrioritizeClusterSize() {
        Collection<Set<NodeId<String>>> clusters = cluster(
                new TestNode(1, 3).register("A"),
                new TestNode(2, 3).register("B"),
                new TestNode(2, 4).register("C"),
                new TestNode(2, 4).register("D"),
                new TestNode(2, 4).register("E")
        );

        assertContainsAll(clusters, "A", "B", "C", "D", "E");
        assertContainsSubset(clusters, "A");
        assertContainsSubset(clusters, "B", "C", "D", "E");
    }

    @SafeVarargs
    private final Collection<Set<NodeId<String>>> cluster(Node.Registration<TestNode, String>... children) {
        String[] childIds = Arrays.stream(children)
                .map(Node.Registration::getIdentifier)
                .toArray(String[]::new);

        Node.Registration<TestNode, String> parent =  new TestNode().register("X", childIds);

        List<Node.Registration<TestNode, String>> nodes = Stream
                .concat(Stream.of(parent), Arrays.stream(children))
                .collect(Collectors.toList());

        Map<NodeId<String>, TestNode> nodeRegistry = NodeEssentials.registerNodes(nodes);

        Map<NodeId<String>, Set<NodeId<String>>> neighborRegistry = NodeEssentials.registerNeighbors(nodeRegistry, nodes);

        return NodeEssentials.clusterSiblings(nodeRegistry, neighborRegistry, new NodeId<>("X"), Collections.singleton(new NodeId<>("X"))).values();
    }

    private void assertContainsAll(Collection<Set<NodeId<String>>> clusters, String... childIds) {
        Set<NodeId<String>> children = Arrays.stream(childIds).map(NodeId::new).collect(Collectors.toSet());
        assertTrue(clusters.stream().allMatch(children::removeAll), String.format("The clusters %s have at least one overlap", clusters));
        assertTrue(children.isEmpty(), String.format("The clusters %s do not contain the children %s", clusters, children));
    }

    private void assertContainsSubset(Collection<Set<NodeId<String>>> clusters, String... childIds) {
        Set<NodeId<String>> children = Arrays.stream(childIds).map(NodeId::new).collect(Collectors.toSet());
        assertTrue(clusters.contains(children), String.format("The clusters %s to not contain the subset %s", clusters, children));
    }
}
