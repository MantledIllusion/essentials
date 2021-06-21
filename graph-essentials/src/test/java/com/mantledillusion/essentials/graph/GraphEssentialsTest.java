package com.mantledillusion.essentials.graph;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GraphEssentialsTest {

    private static final int RADIUS = 2;

    private static class DefaultGraphNode implements GraphEssentials.GraphNode<String> {

        private final String id;
        private final double radius;
        private final Set<String> neighbors;
        private double orbit,x,y;

        private DefaultGraphNode(String id, double radius, String... childIds) {
            this.id = id;
            this.radius = radius;
            this.neighbors = new HashSet<>(Arrays.asList(childIds));
        }

        @Override
        public String getIdentifier() {
            return this.id;
        }

        @Override
        public double getRadius() {
            return this.radius;
        }

        @Override
        public Set<String> getNeighbors() {
            return this.neighbors;
        }

        @Override
        public void setOrbit(double orbit) {
            this.orbit = orbit;
        }

        @Override
        public void setX(double x) {
            this.x = x;
        }

        @Override
        public void setY(double y) {
            this.y = y;
        }

        @Override
        public String toString() {
            return "DefaultNode{" +
                    "id='" + id + '\'' +
                    ", x=" + x +
                    ", y=" + y +
                    '}';
        }
    }

    @Test
    public void testSingleGraph() {
        List<DefaultGraphNode> myNodes = Arrays.asList(
                new DefaultGraphNode("A", RADIUS,"B", "D"),

                new DefaultGraphNode("B", RADIUS, "A","C"),
                new DefaultGraphNode("C", RADIUS, "B"),

                new DefaultGraphNode("D", RADIUS,"A", "E", "F"),
                new DefaultGraphNode("E", RADIUS, "D"),
                new DefaultGraphNode("F", RADIUS, "D")
        );

        GraphEssentials.distribute(myNodes);
    }

    @Test
    public void testMultiGraph() {
        List<DefaultGraphNode> myNodes = Arrays.asList(
                new DefaultGraphNode("A", RADIUS,"B"),
                new DefaultGraphNode("B", RADIUS, "A","C"),
                new DefaultGraphNode("C", RADIUS, "B"),

                new DefaultGraphNode("D", RADIUS,"E", "F"),
                new DefaultGraphNode("E", RADIUS, "D"),
                new DefaultGraphNode("F", RADIUS, "D", "G", "H"),
                new DefaultGraphNode("G", RADIUS, "F"),
                new DefaultGraphNode("H", RADIUS, "F")
        );

        GraphEssentials.distribute(myNodes);
    }

    @Test
    public void testIllegalGraph() {
        List<DefaultGraphNode> myNodes = Arrays.asList(
                new DefaultGraphNode("A", RADIUS,"B", "F", "I"),

                new DefaultGraphNode("B", RADIUS,"A", "C"),
                new DefaultGraphNode("C", RADIUS,"B"),

                new DefaultGraphNode("D", RADIUS,"A", "E"),
                new DefaultGraphNode("E", RADIUS,"D"),
                new DefaultGraphNode("F", RADIUS,"D")
        );

        Assertions.assertThrows(IllegalArgumentException.class, () -> GraphEssentials.distribute(myNodes));
    }

    @Test
    public void testDistributionDistance() {
        Map<String, DefaultGraphNode> nodes = Stream.of(
                new DefaultGraphNode("A", RADIUS,"B", "F", "I"),

                new DefaultGraphNode("B", RADIUS,"A", "C"),
                new DefaultGraphNode("C", RADIUS,"B", "D", "E"),
                new DefaultGraphNode("D", RADIUS,"C", "E"),
                new DefaultGraphNode("E", RADIUS,"C", "D"),

                new DefaultGraphNode("F", RADIUS,"A", "G"),
                new DefaultGraphNode("G", RADIUS,"F", "H"),
                new DefaultGraphNode("H", RADIUS,"G"),

                new DefaultGraphNode("I", RADIUS,"J", "K"),
                new DefaultGraphNode("J", RADIUS,"I"),
                new DefaultGraphNode("K", RADIUS,"I", "L", "M"),
                new DefaultGraphNode("L", RADIUS,"K", "M"),
                new DefaultGraphNode("M", RADIUS,"K", "L")
        ).collect(Collectors.toMap(DefaultGraphNode::getIdentifier, node -> node));

        GraphEssentials.distribute(nodes.values());

        nodes.values().forEach(node -> node.getNeighbors().stream()
                        .map(nodes::get)
                        .forEach(neighbor -> {
                            double vx = node.x - neighbor.x;
                            double vy = node.y - neighbor.y;
                            double distance = Math.sqrt(vx * vx + vy * vy);

                            // ROUND UP TO 1/1000th DECIMAL PLACE TO ELIMINATE INACCURACY
                            distance = Math.round(((int) (distance * 1000)) / 10.0) / 100.0;

                            Assertions.assertTrue(2 * RADIUS <= distance, "The distance between the nodes "
                                    + node.getIdentifier() + " and " + neighbor.getIdentifier() + " is " + distance
                                    + ", which is lower than " + (2 * RADIUS));
                        }));
    }
}
