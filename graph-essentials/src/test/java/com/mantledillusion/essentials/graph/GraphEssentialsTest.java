package com.mantledillusion.essentials.graph;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GraphEssentialsTest {

    @Test
    public void testSingleGraph() {
        List<Node.Registration<TestNode, String>> myNodes = Arrays.asList(
                new TestNode().register("A","B", "D"),

                new TestNode().register("B", "A","C"),
                new TestNode().register("C", "B"),

                new TestNode().register("D","A", "E", "F"),
                new TestNode().register("E", "D"),
                new TestNode().register("F", "D")
        );

        GraphEssentials.distribute(myNodes);
    }

    @Test
    public void testMultiGraph() {
        List<Node.Registration<TestNode, String>> myNodes = Arrays.asList(
                new TestNode().register("A","B"),
                new TestNode().register("B", "A","C"),
                new TestNode().register("C", "B"),

                new TestNode().register("D","E", "F"),
                new TestNode().register("E", "D"),
                new TestNode().register("F", "D", "G", "H"),
                new TestNode().register("G", "F"),
                new TestNode().register("H", "F")
        );

        GraphEssentials.distribute(myNodes);
    }

    @Test
    public void testIllegalGraph() {
        List<Node.Registration<TestNode, String>> myNodes = Arrays.asList(
                new TestNode().register("A","B", "F", "I"),

                new TestNode().register("B","A", "C"),
                new TestNode().register("C","B"),

                new TestNode().register("D","A", "E"),
                new TestNode().register("E","D"),
                new TestNode().register("F","D")
        );

        Assertions.assertThrows(IllegalArgumentException.class, () -> GraphEssentials.distribute(myNodes));
    }

    @Test
    public void testDistributionDistance() {
        Map<String, Node.Registration<TestNode, String>> nodes = Stream.of(
                new TestNode().register("A","B", "F", "I"),

                new TestNode().register("B","A", "C"),
                new TestNode().register("C","B", "D", "E"),
                new TestNode().register("D","C", "E"),
                new TestNode().register("E","C", "D"),

                new TestNode().register("F","A", "G"),
                new TestNode().register("G","F", "H"),
                new TestNode().register("H","G"),

                new TestNode().register("I","J", "K"),
                new TestNode().register("J","I"),
                new TestNode().register("K","I", "L", "M"),
                new TestNode().register("L","K", "M"),
                new TestNode().register("M","K", "L")
        ).collect(Collectors.toMap(Node.Registration::getIdentifier, node -> node));

        GraphEssentials.distribute(nodes.values());

        nodes.values().forEach(registration -> registration.getNeighbors().stream()
                        .map(nodes::get)
                        .forEach(neighbor -> {
                            double vx = registration.getNode().getX() - neighbor.getNode().getX();
                            double vy = registration.getNode().getY() - neighbor.getNode().getY();
                            double distance = Math.sqrt(vx * vx + vy * vy);

                            // ROUND UP TO 1/1000th DECIMAL PLACE TO ELIMINATE INACCURACY
                            distance = Math.round(((int) (distance * 1000)) / 10.0) / 100.0;

                            Assertions.assertTrue(2 * TestNode.RADIUS <= distance, "The distance between the nodes "
                                    + registration.getIdentifier() + " and " + neighbor.getIdentifier() + " is " + distance
                                    + ", which is lower than " + (2 * TestNode.RADIUS));
                        }));
    }
}
