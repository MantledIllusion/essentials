package com.mantledillusion.essentials.vaadin.component;

import com.mantledillusion.essentials.graph.GraphEssentials;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Vaadin {@link Component} to distribute other {@link Component}s in a orbital layout around each other depending on
 * those {@link Component}'s relationships to each other as graph nodes.
 * <p>
 * Create a new node graph using the {@link GraphBuilder} created by calling {@link #rebuild()}.
 *
 * @param <NodeIdType> The type of the ID the nodes are identified by
 */
@Tag("div")
public class GraphPane<NodeIdType> extends Component implements HasSize, HasStyle {

    private final class Node extends VerticalLayout implements GraphEssentials.GraphNode<NodeIdType> {

        private final NodeIdType id;
        private final Set<NodeIdType> neighbors;

        private int x, y, orbit;
        private int width, height;

        private Node(NodeIdType id) {
            this.id = id;
            this.neighbors = new HashSet<>();
            setPadding(false);
            setDefaultHorizontalComponentAlignment(Alignment.CENTER);
            getElement().getStyle().set("position", "absolute");
        }

        boolean isInitialized() {
            return getElement().getChildren().count() > 0;
        }

        void initialize(Component component, int width, int height) {
            if (isInitialized()) {
                throw new IllegalStateException("Node " + this.id + " already has been initialized");
            }

            this.width = width;
            this.height = height;
            setWidth(width + "px");
            setHeight(height + "px");

            HorizontalLayout layout = new HorizontalLayout();
            layout.setWidth(null);
            layout.setHeightFull();
            layout.setPadding(false);
            layout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
            layout.add(component);

            add(layout);
        }

        @Override
        public NodeIdType getIdentifier() {
            return this.id;
        }

        @Override
        public double getRadius() {
            return Math.sqrt(this.width / 2.0 * this.width / 2.0 + this.height / 2.0 * this.height / 2.0);
        }

        @Override
        public Set<NodeIdType> getNeighbors() {
            return this.neighbors;
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
    }

    @Tag("div")
    private final class NodePane extends Component implements ClickNotifier<NodePane> {

        private final BackPane backpane;

        private NodePane() {
            this.backpane = new BackPane();
            reinitialize(Collections.emptyMap());
        }

        void reinitialize(Map<NodeIdType, Node> nodes) {
            // REMOVE ALL CHILDREN (BACK PANE AND NODE COMPONENTS)
            getElement().removeAllChildren();

            // DETERMINE WIDTH AND HEIGHT OF THE GRAPH
            int width = nodes.values().stream()
                    .mapToDouble(node -> node.x + node.getRadius())
                    .mapToInt(w -> (int) Math.round(w))
                    .max()
                    .orElse(0);

            int height = nodes.values().stream()
                    .mapToDouble(node -> node.y + node.getRadius())
                    .mapToInt(h -> (int) Math.round(h))
                    .max()
                    .orElse(0);

            // REINITIALIZE BACK PANE
            this.backpane.reinitialize(width, height);
            getElement().appendChild(this.backpane.getElement());

            // DRAW NODE ORBITS
            if (GraphPane.this.drawOrbit) {
                nodes.values().forEach(node -> this.backpane.drawOrbit(node.x, node.y, node.orbit));
            }

            // DRAW NODE CONNECTIONS
            Set<Set<NodeIdType>> drawnConnections = new HashSet<>();
            nodes.values().forEach(node -> node.getNeighbors().stream()
                    .map(nodes::get)
                    .forEach(neighbor -> {
                        Set<NodeIdType> connection = new HashSet<>(Arrays.asList(node.id, neighbor.id));
                        // DRAW THE BI-DIRECTIONAL CONNECTIONS ONCE
                        if (!drawnConnections.contains(connection)) {
                            drawnConnections.add(connection);
                            this.backpane.drawConnection(node.x, node.y, neighbor.x, neighbor.y);
                        }
                    }));

            // ADD NODE COMPONENTS
            nodes.values().forEach(node -> getElement().appendChild(node.getElement()));
        }
    }

    @Tag("canvas")
    private final class BackPane extends Component {

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

    /**
     * Builder for re-creating a {@link GraphPane}'s content.
     * <p>
     * After adding as many components as desired using {@link #addNode(Object, Component, Object[])}, invoking
     * {@link #render()} will re-create the {@link GraphPane}'s content.
     */
    public final class GraphBuilder {

        private final Map<NodeIdType, Node> nodeRegistry;

        private GraphBuilder() {
            this.nodeRegistry = new HashMap<>();
        }

        /**
         * Factory method that adds the given {@link Component} to the graph by using the given ID.
         * <p>
         * The neighbors declared for the node might be of single- or bi-directional relationships. They are not
         * required to be added by the time this node is added, but by the time {@link #render()} is invoked.
         * <p>
         * Used the default width and height for the node.
         *
         * @param id The ID to register the node under; might <b>not</b> be null.
         * @param c The component to distribute as the node; might <b>not</b> be null.
         * @param neighbors The IDs of all neighbors the node has; might be null or empty.
         * @return this
         */
        @SafeVarargs
        public final GraphBuilder addNode(NodeIdType id, Component c, NodeIdType... neighbors) {
            return addNode(id, c, GraphPane.this.defaultMaxNodeWidth, GraphPane.this.defaultMaxNodeHeight, neighbors);
        }

        /**
         * Factory method that adds the given {@link Component} to the graph by using the given ID.
         * <p>
         * The neighbors declared for the node might be of single- or bi-directional relationships. They are not
         * required to be added by the time this node is added, but by the time {@link #render()} is invoked.
         *
         * @param id The ID to register the node under; might <b>not</b> be null.
         * @param c The component to distribute as the node; might <b>not</b> be null.
         * @param maxNodeWidth The maximum width the given component is allowed to grow; smaller components will be centered.
         * @param maxNodeHeight The maximum height the given component is allowed to grow; smaller components will be centered.
         * @param neighbors The IDs of all neighbors the node has; might be null or empty.
         * @return this
         */
        @SafeVarargs
        public final GraphBuilder addNode(NodeIdType id, Component c, int maxNodeWidth, int maxNodeHeight, NodeIdType... neighbors) {
            if (id == null) {
                throw new IllegalArgumentException("Cannot register a node for a null ID");
            } else if (c == null) {
                throw new IllegalArgumentException("Cannot register a null component as node");
            } else if (maxNodeWidth <= 0) {
                throw new IllegalArgumentException("Cannot set the width to a value <=0");
            } else if (maxNodeHeight <= 0) {
                throw new IllegalArgumentException("Cannot set the height to a value <=0");
            }

            Set<NodeIdType> neighborIdSet = neighbors == null ? new HashSet<>() : new HashSet<>(Arrays.asList(neighbors));
            neighborIdSet.remove(null);
            neighborIdSet.remove(id);

            Node node = ensureRegistered(id);
            node.initialize(c, maxNodeWidth, maxNodeHeight);
            node.neighbors.addAll(neighborIdSet);
            neighborIdSet.stream().map(this::ensureRegistered).forEach(neighbor -> neighbor.neighbors.add(id));

            return this;
        }

        private Node ensureRegistered(NodeIdType id) {
            return this.nodeRegistry.computeIfAbsent(id, nodeId -> new Node(id));
        }

        /**
         * Uses all nodes added to this builder rebuild the pane, distribute the nodes and draw connections.
         */
        public void render() {
            // MAKE SURE ALL NODES ARE INITIALIZED
            Set<NodeIdType> uninitialized = this.nodeRegistry.values().stream()
                    .filter(node -> !node.isInitialized())
                    .map(GraphEssentials.GraphNode::getIdentifier)
                    .collect(Collectors.toSet());
            if (!uninitialized.isEmpty()) {
                throw new IllegalStateException("The nodes [" + uninitialized
                        + "] are referenced as neighbors, but have never been specified");
            }

            // DISTRIBUTE THE NODES ON THE COORDINATE SYSTEM
            GraphEssentials.distribute(this.nodeRegistry.values());

            // TRANSLATE THE POSITION OF ALL NODES SO THEIR COORDINATES BECOME POSITIVE FOR RENDERING
            double minX = this.nodeRegistry.values().stream()
                    .mapToDouble(node -> node.x - node.getRadius())
                    .min()
                    .orElse(0);
            this.nodeRegistry.values().forEach(node -> node.setX(node.x - minX));

            double minY = this.nodeRegistry.values().stream()
                    .mapToDouble(node -> node.y - node.getRadius())
                    .min()
                    .orElse(0);
            this.nodeRegistry.values().forEach(node -> node.setY(node.y - minY));

            // REINITIALIZE THE GRAPH PANE WITH THE NEW NODE SET
            GraphPane.this.nodePane.reinitialize(this.nodeRegistry);
        }
    }

    private final NodePane nodePane;

    private int defaultMaxNodeWidth = 50, defaultMaxNodeHeight = 50;

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
    }

    /**
     * Factory method for rebuilding a new node graph to display.
     *
     * @return A new {@link GraphBuilder} instance, never null
     */
    public GraphBuilder rebuild() {
        return new GraphBuilder();
    }

    /**
     * Returns the default width to use when adding nodes using {@link GraphBuilder#addNode(Object, Component, Object[])}.
     *
     * @return The node's width, &gt; 0
     */
    public int getDefaultMaxNodeWidth() {
        return this.defaultMaxNodeWidth;
    }

    /**
     * Sets the default width to use when adding nodes using {@link GraphBuilder#addNode(Object, Component, Object[])}.
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
     * Returns the default width to use when adding nodes using {@link GraphBuilder#addNode(Object, Component, Object[])}.
     *
     * @return The node's height, &gt; 0
     */
    public int getDefaultMaxNodeHeight() {
        return this.defaultMaxNodeHeight;
    }

    /**
     * Sets the default height to use when adding nodes using {@link GraphBuilder#addNode(Object, Component, Object[])}.
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
