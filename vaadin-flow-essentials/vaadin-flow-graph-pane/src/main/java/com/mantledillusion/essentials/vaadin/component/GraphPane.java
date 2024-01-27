package com.mantledillusion.essentials.vaadin.component;

import com.mantledillusion.essentials.graph.GraphEssentials;
import com.mantledillusion.essentials.graph.Node;
import com.mantledillusion.essentials.graph.NodeId;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Vaadin {@link Component} to distribute other {@link Component}s in a orbital layout around each other depending on
 * those {@link Component}'s relationships to each other as graph nodes.
 * <p>
 * Create a new node graph using the {@link ComponentGraphBuilder} created by calling {@link #rebuild()}.
 *
 * @param <NodeIdType> The type of the ID the nodes are identified by
 */
@Tag("div")
public class GraphPane<NodeIdType> extends Component implements HasSize, HasStyle {

    private final class NodeComponent<NodeType> extends VerticalLayout implements Node<NodeComponent<NodeType>> {

        private int x, y, orbit;
        private final NodeType node;
        private final int width, height;
        private final BiFunction<NodeType, NodeType, Clusterability> clusterPredicate;
        private final BiFunction<NodeType, NodeType, NodeType> clusterFunction;

        private NodeComponent(NodeType node, int width, int height,
                              BiFunction<NodeType, NodeType, Clusterability> clusterPredicate,
                              BiFunction<NodeType, NodeType, NodeType> clusterFunction) {
            this.node = node;
            this.width = width;
            this.height = height;
            this.clusterPredicate = clusterPredicate;
            this.clusterFunction = clusterFunction;
            setPadding(false);
            setDefaultHorizontalComponentAlignment(Alignment.CENTER);
            getElement().getStyle().set("position", "absolute");
        }

        void initialize(Component component) {
            setWidth(this.width + "px");
            setHeight(this.height + "px");

            HorizontalLayout layout = new HorizontalLayout();
            layout.setWidth(null);
            layout.setHeightFull();
            layout.setPadding(false);
            layout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
            layout.add(component);

            add(layout);
        }

        private NodeType getNode() {
            return this.node;
        }

        @Override
        public double getRadius() {
            return Math.sqrt(this.width / 2.0 * this.width / 2.0 + this.height / 2.0 * this.height / 2.0);
        }

        @Override
        public void setOrbit(double orbit) {
            this.orbit = (int) Math.round(orbit);
        }

        @Override
        public void setX(double x) {
            this.x = (int) Math.round(x);
            getElement().getStyle().set("left", Math.round(this.x - this.width / 2.0) + "px");
        }

        @Override
        public void setY(double y) {
            this.y = (int) Math.round(y);
            getElement().getStyle().set("top", Math.round(this.y - this.height / 2.0) + "px");
        }

        @Override
        public int getMinClusterSize() {
            return GraphPane.this.defaultClusterMinSize;
        }

        @Override
        public int getMaxClusterSize() {
            return GraphPane.this.defaultClusterMaxSize;
        }

        @Override
        public Clusterability clusterableWith(NodeComponent<NodeType> other) {
            return this.clusterPredicate.apply(this.node, other.node);
        }

        @Override
        public NodeComponent<NodeType> clusterWith(NodeComponent<NodeType> other) {
            return new NodeComponent<>(this.clusterFunction.apply(this.node, other.node),
                    this.width, this.height, this.clusterPredicate, this.clusterFunction);
        }
    }

    @Tag("div")
    private final class NodePane extends Component implements ClickNotifier<NodePane> {

        private final BackPane backpane;

        private NodePane() {
            this.backpane = new BackPane();
            reinitialize(Collections.emptyList());
            getElement().getStyle().set("position", "relative");
        }

        <NodeType> void reinitialize(Collection<Node.Registration<NodeComponent<NodeType>, NodeId<NodeIdType>>> nodes) {
            // REMOVE ALL CHILDREN (BACK PANE AND NODE COMPONENTS)
            getElement().removeAllChildren();

            // DETERMINE WIDTH AND HEIGHT OF THE GRAPH
            int width = nodes.stream()
                    .map(Node.Registration::getNode)
                    .mapToDouble(node -> node.x + node.getRadius())
                    .mapToInt(w -> (int) Math.round(w))
                    .max()
                    .orElse(0);

            int height = nodes.stream()
                    .map(Node.Registration::getNode)
                    .mapToDouble(node -> node.y + node.getRadius())
                    .mapToInt(h -> (int) Math.round(h))
                    .max()
                    .orElse(0);

            // REINITIALIZE BACK PANE
            this.backpane.reinitialize(width, height);
            getElement().appendChild(this.backpane.getElement());

            // DRAW NODE ORBITS
            if (GraphPane.this.drawOrbit) {
                nodes.stream().map(Node.Registration::getNode)
                        .forEach(node -> this.backpane.drawOrbit(node.x, node.y, node.orbit));
            }

            // DRAW NODE CONNECTIONS
            Map<NodeId<NodeIdType>, NodeComponent<NodeType>> nodeRegistry = nodes.stream()
                    .collect(Collectors.toMap(
                            Node.Registration::getIdentifier,
                            Node.Registration::getNode
                    ));

            Set<Set<NodeId<NodeIdType>>> drawnConnections = new HashSet<>();
            nodes.forEach(registration -> registration.getNeighbors()
                    .forEach(neighborId -> {
                        NodeComponent<NodeType> neighbor = nodeRegistry.get(neighborId);
                        Set<NodeId<NodeIdType>> connection = new HashSet<>(Arrays.asList(registration.getIdentifier(), neighborId));
                        // DRAW THE BI-DIRECTIONAL CONNECTIONS ONCE
                        if (!drawnConnections.contains(connection)) {
                            drawnConnections.add(connection);
                            this.backpane.drawConnection(registration.getNode().x, registration.getNode().y, neighbor.x, neighbor.y);
                        }
                    }));

            // ADD NODE COMPONENTS
            nodes.forEach(registration -> getElement().appendChild(registration.getNode().getElement()));
        }
    }

    @Tag("canvas")
    private final class BackPane extends Component {

        private BackPane() {
            getElement().getStyle().set("position", "absolute");
        }

        void reinitialize(int width, int height) {
            getElement().setAttribute("width", String.valueOf(width));
            getElement().setAttribute("height", String.valueOf(height));
            callCanvasMethod("clearRect", 0, 0, width, height);
        }

        void drawOrbit(int x, int y, int orbit) {
            setCanvasProperty("lineCap", "round");
            setCanvasProperty("lineWidth", GraphPane.this.orbitWidth);
            setCanvasProperty("strokeStyle", GraphPane.this.orbitColor);
            callCanvasMethod("beginPath");
            callCanvasMethod("arc", x, y, orbit, 0, 2 * Math.PI);
            callCanvasMethod("stroke");
        }

        void drawConnection(int x1, int y1, int x2, int y2) {
            setCanvasProperty("lineCap", "round");
            setCanvasProperty("lineWidth", GraphPane.this.connectionWidth);
            setCanvasProperty("strokeStyle", GraphPane.this.connectionColor);
            callCanvasMethod("beginPath");
            callCanvasMethod("moveTo", x1, y1);
            callCanvasMethod("lineTo", x2, y2);
            callCanvasMethod("stroke");
        }

        private void setCanvasProperty(String propertyName, Serializable value) {
            String script = String.format("$0.getContext('2d').%s='%s'", propertyName, value);
            this.getElement().getNode().runWhenAttached(
                    ui -> ui.getInternals().getStateTree().beforeClientResponse(this.getElement().getNode(),
                            context -> ui.getPage().executeJs(script, this.getElement())));
        }

        private void callCanvasMethod(String methodName, Serializable... parameters) {
            this.getElement().callJsFunction("getContext('2d')." + methodName, parameters);
        }
    }

    private abstract class AbstractGraphBuilder<BuilderType extends AbstractGraphBuilder<BuilderType, NodeType>, NodeType> {

        private final Map<NodeIdType, Node.Registration<NodeComponent<NodeType>, NodeIdType>> nodeRegistry = new HashMap<>();
        private final Function<NodeType, Integer> minClusterSizeFunction;
        private final Function<NodeType, Integer> maxClusterSizeFunction;
        private final BiFunction<NodeType, NodeType, Node.Clusterability> clusterPredicate;
        private final BiFunction<NodeType, NodeType, NodeType> clusterFunction;

        private AbstractGraphBuilder(Function<NodeType, Integer> minClusterSizeFunction,
                                     Function<NodeType, Integer> maxClusterSizeFunction,
                                     BiFunction<NodeType, NodeType, Node.Clusterability> clusterPredicate,
                                     BiFunction<NodeType, NodeType, NodeType> clusterFunction) {
            this.minClusterSizeFunction = minClusterSizeFunction;
            this.maxClusterSizeFunction = maxClusterSizeFunction;
            this.clusterPredicate = clusterPredicate;
            this.clusterFunction = clusterFunction;
        }

        /**
         * Factory method that adds the given {@link NodeType} to the graph by using the given ID.
         * <p>
         * The neighbors declared for the node might be of single- or bi-directional relationships. They are not
         * required to be added by the time this node is added, but by the time {@link #render()} is invoked.
         * <p>
         * Used the default width and height for the node.
         *
         * @param id The ID to register the node under; might <b>not</b> be null.
         * @param node The node to distribute as the node; might <b>not</b> be null.
         * @param neighbors The IDs of all neighbors the node has; might be null or empty.
         * @return this
         */
        @SafeVarargs
        public final BuilderType addNode(NodeIdType id, NodeType node, NodeIdType... neighbors) {
            return addNode(id, node, GraphPane.this.defaultMaxNodeWidth, GraphPane.this.defaultMaxNodeHeight, neighbors);
        }

        /**
         * Factory method that adds the given {@link NodeType} to the graph by using the given ID.
         * <p>
         * The neighbors declared for the node might be of single- or bi-directional relationships. They are not
         * required to be added by the time this node is added, but by the time {@link #render()} is invoked.
         *
         * @param id The ID to register the node under; might <b>not</b> be null.
         * @param node The node to distribute as the node; might <b>not</b> be null.
         * @param maxNodeWidth The maximum width the given component is allowed to grow; smaller components will be centered.
         * @param maxNodeHeight The maximum height the given component is allowed to grow; smaller components will be centered.
         * @param neighbors The IDs of all neighbors the node has; might be null or empty.
         * @return this
         */
        @SafeVarargs
        @SuppressWarnings("unchecked")
        public final BuilderType addNode(NodeIdType id, NodeType node, int maxNodeWidth, int maxNodeHeight, NodeIdType... neighbors) {
            if (id == null) {
                throw new IllegalArgumentException("Cannot register a node for a null ID");
            } else if (node == null) {
                throw new IllegalArgumentException("Cannot register a null component as node");
            } else if (maxNodeWidth <= 0) {
                throw new IllegalArgumentException("Cannot set the width to a value <=0");
            } else if (maxNodeHeight <= 0) {
                throw new IllegalArgumentException("Cannot set the height to a value <=0");
            }

            this.nodeRegistry.put(id, new NodeComponent<>(node, maxNodeWidth, maxNodeHeight,
                    this.clusterPredicate, this.clusterFunction).register(id, neighbors));

            Set<NodeIdType> neighborIdSet = neighbors == null ? new HashSet<>() : new HashSet<>(Arrays.asList(neighbors));
            neighborIdSet.remove(null);
            neighborIdSet.remove(id);
            neighborIdSet.forEach(neighborId -> this.nodeRegistry.computeIfAbsent(neighborId, nId -> null));

            return (BuilderType) this;
        }

        protected abstract Component renderNode(NodeType node);

        /**
         * Uses all nodes added to this builder rebuild the pane, distribute the nodes and draw connections.
         */
        public final void render() {
            // MAKE SURE ALL NODES ARE INITIALIZED
            Set<NodeIdType> uninitialized = this.nodeRegistry.entrySet().stream()
                    .filter(entry -> entry.getValue() == null)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            if (!uninitialized.isEmpty()) {
                throw new IllegalStateException("The nodes [" + uninitialized
                        + "] are referenced as neighbors, but have never been specified");
            }

            // DISTRIBUTE THE NODES ON THE COORDINATE SYSTEM
            Collection<Node.Registration<NodeComponent<NodeType>, NodeId<NodeIdType>>> nodes = GraphEssentials.distribute(this.nodeRegistry.values());

            // INITIALIZE THE NODES AFTER POTENTIALLY FORMING CLUSTERS
            nodes.stream().map(Node.Registration::getNode)
                    .forEach(node -> node.initialize(renderNode(node.getNode())));

            // TRANSLATE THE POSITION OF ALL NODES SO THEIR COORDINATES BECOME POSITIVE FOR RENDERING
            double minX = nodes.stream()
                    .map(Node.Registration::getNode)
                    .mapToDouble(node -> node.x - node.getRadius())
                    .min()
                    .orElse(0);
            nodes.stream().map(Node.Registration::getNode)
                    .forEach(node -> node.setX(node.x - minX));

            double minY = nodes.stream()
                    .map(Node.Registration::getNode)
                    .mapToDouble(node -> node.y - node.getRadius())
                    .min()
                    .orElse(0);
            nodes.stream().map(Node.Registration::getNode)
                    .forEach(node -> node.setY(node.y - minY));

            // REINITIALIZE THE GRAPH PANE WITH THE NEW NODE SET
            GraphPane.this.nodePane.reinitialize(nodes);
        }
    }

    /**
     * Builder for re-creating a {@link GraphPane}'s content by directly supplying {@link Component}s for each Node.
     * <p>
     * After adding as many nodes as desired using, invoking {@link #render()} will re-create the {@link GraphPane}'s content.
     */
    public final class ComponentGraphBuilder extends AbstractGraphBuilder<ComponentGraphBuilder, Component> {

        private ComponentGraphBuilder() {
            super((node) -> {throw new IllegalStateException();}, (node) -> {throw new IllegalStateException();},
                    (node, other) -> Node.Clusterability.DENY, (node, other) -> {throw new IllegalStateException();});
        }

        @Override
        protected Component renderNode(Component node) {
            return node;
        }
    }

    /**
     * Builder for re-creating a {@link GraphPane}'s content by rendering nodes.
     * <p>
     * After adding as many nodes as desired using, invoking {@link #render()} will re-create the {@link GraphPane}'s content.
     */
    public final class NodeGraphBuilder<NodeType> extends AbstractGraphBuilder<NodeGraphBuilder<NodeType>, NodeType> {

        private final Function<NodeType, Component> nodeRenderer;

        private NodeGraphBuilder(Function<NodeType, Component> nodeRenderer) {
            super((node) -> {throw new IllegalStateException();}, (node) -> {throw new IllegalStateException();},
                    (node, other) -> Node.Clusterability.DENY, (node, other) -> {throw new IllegalStateException();});
            this.nodeRenderer = nodeRenderer;
        }

        @Override
        protected Component renderNode(NodeType node) {
            return this.nodeRenderer.apply(node);
        }
    }

    /**
     * Builder for re-creating a {@link GraphPane}'s content by rendering node clusters.
     * <p>
     * After adding as many nodes as desired using, invoking {@link #render()} will re-create the {@link GraphPane}'s content.
     */
    public final class ClusterGraphBuilder<NodeType> extends AbstractGraphBuilder<ClusterGraphBuilder<NodeType>, List<NodeType>> {

        private final Function<List<NodeType>, Component> clusterRenderer;

        private ClusterGraphBuilder(Function<NodeType, Integer> minClusterSizeFunction, Function<NodeType, Integer> maxClusterSizeFunction,
                                    BiPredicate<NodeType, NodeType> clusterPredicate, Function<List<NodeType>, Component> clusterRenderer) {
            super(nodes -> minClusterSizeFunction.apply(nodes.iterator().next()), nodes -> maxClusterSizeFunction.apply(nodes.iterator().next()),
                    (nodes, others) -> nodes.stream().allMatch(node -> others.stream().allMatch(other -> clusterPredicate.test(node, other))) ? Node.Clusterability.SIBLINGS : Node.Clusterability.DENY,
                    (nodes, others) -> Stream.concat(nodes.stream(), others.stream()).collect(Collectors.toList()));
            this.clusterRenderer = clusterRenderer;
        }

        @Override
        protected Component renderNode(List<NodeType> hub) {
            return this.clusterRenderer.apply(hub);
        }
    }

    private final NodePane nodePane;

    private int defaultMaxNodeWidth = 50, defaultMaxNodeHeight = 50;
    private int defaultClusterMinSize = 0, defaultClusterMaxSize = Integer.MAX_VALUE;

    private int connectionWidth = 1;
    private String connectionColor = "#4F4F4F";

    private boolean drawOrbit = false;
    private int orbitWidth = 2;
    private String orbitColor = "#EAEAEA";

    /**
     * Default constructor.
     */
    public GraphPane() {
        this.nodePane = new NodePane();
        this.getElement().appendChild(this.nodePane.getElement());
        this.getElement().getStyle().set("overflow", "auto");
    }

    /**
     * Factory method for rebuilding a new node graph to display by directly supplying {@link Component}s for each Node.
     *
     * @return A new {@link ComponentGraphBuilder} instance, never null
     */
    public ComponentGraphBuilder rebuild() {
        return new ComponentGraphBuilder();
    }

    /**
     * Factory method for rebuilding a new node graph to display by supplying a {@link Component} renderer for nodes.
     *
     * @param <NodeType> The type of node to render
     * @param nodeRenderer The renderer to turn a single node into a {@link Component} with; might <b>not</b> be null.
     * @return A new {@link ComponentGraphBuilder} instance, never null
     */
    public <NodeType> NodeGraphBuilder<NodeType> rebuild(Function<NodeType, Component> nodeRenderer) {
        if (nodeRenderer == null) {
            throw new IllegalArgumentException("Cannot use a null renderer create components with");
        }
        return new NodeGraphBuilder<>(nodeRenderer);
    }

    /**
     * Factory method for rebuilding a new node graph to display by supplying {@link Component} renderer for a list of
     * nodes joined together in clusters by the given predicate.
     *
     * @param <NodeType> The type of node to render
     * @param clusterRenderer The renderer to turn a node hub into a {@link Component} with; might <b>not</b> be null.
     * @param clusterPredicate A predicate to determine whether two nodes are able to join a cluster together; might <b>not</b> be null.
     * @return A new {@link ComponentGraphBuilder} instance, never null
     */
    public <NodeType> ClusterGraphBuilder<NodeType> rebuild(Function<List<NodeType>, Component> clusterRenderer, BiPredicate<NodeType, NodeType> clusterPredicate) {
        return rebuild(node -> this.defaultClusterMinSize, node -> this.defaultClusterMaxSize, clusterPredicate, clusterRenderer);
    }

    /**
     * Factory method for rebuilding a new node graph to display by supplying {@link Component} renderer for a list of
     * nodes joined together in clusters by the given predicate.
     *
     * @param <NodeType> The type of node to render
     * @param minClusterSizeRetriever A function to determine the minimum count of other nodes a node requires to allow being clustered; might <b>not</b> be null.
     * @param maxClusterSizeRetriever A function to determine the maximum count of other nodes a node denies being clustered with; might <b>not</b> be null.
     * @param clusterPredicate A predicate to determine whether two nodes are able to join a cluster together; might <b>not</b> be null.
     * @param clusterRenderer The renderer to turn a node hub into a {@link Component} with; might <b>not</b> be null.
     * @return A new {@link ComponentGraphBuilder} instance, never null
     */
    public <NodeType> ClusterGraphBuilder<NodeType> rebuild(Function<NodeType, Integer> minClusterSizeRetriever, Function<NodeType, Integer> maxClusterSizeRetriever,
                                                            BiPredicate<NodeType, NodeType> clusterPredicate, Function<List<NodeType>, Component> clusterRenderer) {
        if (clusterRenderer == null) {
            throw new IllegalArgumentException("Cannot use a null renderer create components with");
        } else if (clusterPredicate == null) {
            throw new IllegalArgumentException("Cannot use a null predicate to form hubs with");
        }
        return new ClusterGraphBuilder<>(minClusterSizeRetriever, maxClusterSizeRetriever, clusterPredicate, clusterRenderer);
    }

    /**
     * Returns the default width to use when adding nodes using {@link ComponentGraphBuilder#addNode(Object, Component, Object[])}.
     *
     * @return The node's width, &gt; 0
     */
    public int getDefaultMaxNodeWidth() {
        return this.defaultMaxNodeWidth;
    }

    /**
     * Sets the default width to use when adding nodes using {@link ComponentGraphBuilder#addNode(Object, Component, Object[])}.
     *
     * @param defaultMaxNodeWidth The node's width; &gt; 0.
     */
    public void setDefaultMaxNodeWidth(int defaultMaxNodeWidth) {
        if (defaultMaxNodeWidth <= 0) {
            throw new IllegalArgumentException("Cannot set the width to a value <=0");
        }
        this.defaultMaxNodeWidth = defaultMaxNodeWidth;
    }

    /**
     * Returns the default width to use when adding nodes using {@link ComponentGraphBuilder#addNode(Object, Component, Object[])}.
     *
     * @return The node's height, &gt; 0
     */
    public int getDefaultMaxNodeHeight() {
        return this.defaultMaxNodeHeight;
    }

    /**
     * Sets the default height to use when adding nodes using {@link ComponentGraphBuilder#addNode(Object, Component, Object[])}.
     *
     * @param defaultMaxNodeHeight The node's height; &gt; 0.
     */
    public void setDefaultMaxNodeHeight(int defaultMaxNodeHeight) {
        if (defaultMaxNodeHeight <= 0) {
            throw new IllegalArgumentException("Cannot set the height to a value <=0");
        }
        this.defaultMaxNodeHeight = defaultMaxNodeHeight;
    }

    /**
     * Returns the default minimum size to use when clustering nodes using {@link ComponentGraphBuilder#rebuild(Function, BiPredicate)}.
     *
     * @return The cluster's min size, &ge; 0
     */
    public int getDefaultClusterMinSize() {
        return this.defaultClusterMinSize;
    }

    /**
     * Sets the default minimum size to use when clustering nodes using {@link ComponentGraphBuilder#rebuild(Function, BiPredicate)}.
     *
     * @param defaultClusterMinSize The cluster's min size; &ge; 0.
     */
    public void setDefaultClusterMinSize(int defaultClusterMinSize) {
        if (defaultClusterMinSize < 0) {
            throw new IllegalArgumentException("Cannot set the size to a value <0");
        }
        this.defaultClusterMinSize = defaultClusterMinSize;
    }

    /**
     * Returns the default maximum size to use when clustering nodes using {@link ComponentGraphBuilder#rebuild(Function, BiPredicate)}.
     *
     * @return The cluster's max size, &ge; 0
     */
    public int getDefaultClusterMaxSize() {
        return this.defaultClusterMaxSize;
    }

    /**
     * Sets the default maximum size to use when clustering nodes using {@link ComponentGraphBuilder#rebuild(Function, BiPredicate)}.
     *
     * @param defaultClusterMaxSize The cluster's max size; &ge; 0.
     */
    public void setDefaultClusterMaxSize(int defaultClusterMaxSize) {
        if (defaultClusterMaxSize < 0) {
            throw new IllegalArgumentException("Cannot set the size to a value <0");
        }
        this.defaultClusterMaxSize = defaultClusterMaxSize;
    }

    /**
     * Returns the line width to draw when drawing connections between nodes.
     *
     * @return The line width, &gt; 0
     */
    public int getConnectionWidth() {
        return connectionWidth;
    }

    /**
     * Sets the line width to draw when drawing connections between nodes.
     *
     * @param connectionWidth The line width; &gt; 0
     */
    public void setConnectionWidth(int connectionWidth) {
        if (connectionWidth <= 0) {
            throw new IllegalArgumentException("Cannot set the line width to a value <=0");
        }
        this.connectionWidth = connectionWidth;
    }

    /**
     * Returns the CSS color used for drawing connections between nodes.
     *
     * @return The CSS color, never null
     */
    public String getConnectionColor() {
        return connectionColor;
    }

    /**
     * Sets the CSS color (for example "#FF0000" or "rgb(0, 0, 0)") to draw connections between nodes with.
     *
     * @param connectionColor The color to use; might <b>not</b> be null.
     */
    public void setConnectionColor(String connectionColor) {
        if (connectionColor == null) {
            throw new IllegalArgumentException("Cannot set a null color to draw with");
        }
        this.connectionColor = connectionColor;
    }

    /**
     * Returns whether the orbit of nodes is drawn.
     *
     * @return True if the orbit is drawn, false otherwise
     */
    public boolean isDrawOrbit() {
        return drawOrbit;
    }

    /**
     * Sets whether the orbit of nodes should be drawn.
     *
     * @param drawOrbit True if the orbit should be drawn, false otherwise.
     */
    public void setDrawOrbit(boolean drawOrbit) {
        this.drawOrbit = drawOrbit;
    }

    /**
     * Returns the line width to draw when drawing the orbit of nodes.
     *
     * @return The line width, &gt; 0
     */
    public int getOrbitWidth() {
        return orbitWidth;
    }

    /**
     * Sets the line width to draw when drawing the orbit of nodes.
     *
     * @param orbitWidth The line width; &gt; 0
     */
    public void setOrbitWidth(int orbitWidth) {
        if (orbitWidth <= 0) {
            throw new IllegalArgumentException("Cannot set the line width to a value <=0");
        }
        this.orbitWidth = orbitWidth;
    }

    /**
     * Returns the CSS color used for drawing node orbits.
     *
     * @return The CSS color, never null
     */
    public String getOrbitColor() {
        return orbitColor;
    }

    /**
     * Sets the CSS color (for example "#FF0000" or "rgb(0, 0, 0)") to draw node orbits with.
     *
     * @param orbitColor The color to use; might <b>not</b> be null.
     */
    public void setOrbitColor(String orbitColor) {
        if (orbitColor == null) {
            throw new IllegalArgumentException("Cannot set a null color to draw with");
        }
        this.orbitColor = orbitColor;
    }
}
