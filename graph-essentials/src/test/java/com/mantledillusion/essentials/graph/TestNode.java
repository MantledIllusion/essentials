package com.mantledillusion.essentials.graph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

class TestNode implements Node<TestNode> {

    public static final int RADIUS = 2;
    private final Set<Integer> cluster;
    private double orbit, x, y;

    TestNode(Integer... cluster) {
        this(new HashSet<>(Arrays.asList(cluster)));
    }

    private TestNode(Set<Integer> cluster) {
        this.cluster = cluster;
    }

    @Override
    public double getRadius() {
        return RADIUS;
    }

    @Override
    public void setOrbit(double orbit) {
        this.orbit = orbit;
    }

    public double getX() {
        return x;
    }

    @Override
    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    @Override
    public void setY(double y) {
        this.y = y;
    }

    @Override
    public int getMinClusterSize() {
        return 0;
    }

    @Override
    public int getMaxClusterSize() {
        return Integer.MAX_VALUE;
    }

    @Override
    public Clusterability clusterableWith(TestNode other) {
        return this.cluster.stream().anyMatch(other.cluster::contains) ? Clusterability.SIBLINGS : Clusterability.DENY;
    }

    @Override
    public TestNode clusterWith(TestNode other) {
        return new TestNode(NodeEssentials.union(this.cluster, other.cluster).stream()
                .filter(this.cluster::contains)
                .filter(other.cluster::contains)
                .collect(Collectors.toSet()));
    }

    @Override
    public String toString() {
        return "DefaultNode{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
