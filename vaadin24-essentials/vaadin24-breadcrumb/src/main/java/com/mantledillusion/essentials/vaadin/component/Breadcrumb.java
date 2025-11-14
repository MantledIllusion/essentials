package com.mantledillusion.essentials.vaadin.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Component for navigating through a route hierarchy.
 */
public class Breadcrumb extends Composite<Component> implements HasSize {

    private interface Enabler {

        void enable();
    }

    /**
     * Customer to a builder.
     *
     * @param <B> The type of builder to customize.
     */
    public interface Customizer<B> {

        /**
         * Customizes the given builder.
         *
         * @param builder The builder; might <b>not</b> be null.
         */
        void customize(B builder);
    }

    /**
     * Builder to a crumb nested in the {@link Breadcrumb} itself.
     */
    public class ParentCrumbBuilder extends AbstractCrumbBuilder<ParentCrumbBuilder> {

        private ParentCrumbBuilder(MenuItem crumbMenu, Enabler enabler) {
            super(crumbMenu, enabler);
        }

        public ParentCrumbBuilder pointLeft() {
            this.crumbMenu.setText("⯇");
            return  this;
        }
    }

    /**
     * Builder to a crumb nested in one of the {@link Breadcrumb}'s menus.
     */
    public class ChildCrumbBuilder extends AbstractCrumbBuilder<ChildCrumbBuilder> {

        private ChildCrumbBuilder(MenuItem crumbMenu, Enabler enabler) {
            super(crumbMenu, enabler);
        }
    }

    public class AbstractCrumbBuilder<B extends AbstractCrumbBuilder<B>> {

        protected final MenuItem crumbMenu;
        private final Enabler enabler;

        private AbstractCrumbBuilder(MenuItem crumbMenu, Enabler enabler) {
            this.crumbMenu = crumbMenu;
            this.enabler = enabler;
        }

        /**
         * Customizes the crumb's {@link Style}.
         *
         * @param customizer The customizer to the style; might <b>not</b> be null
         * @return this
         */
        @SuppressWarnings("unchecked")
        public B setStyle(Customizer<Style> customizer) {
            customizer.customize(this.crumbMenu.getStyle());
            return (B) this;
        }

        /**
         * Customizes the crumb's {@link Tooltip}.
         *
         * @param customizer The customizer to the tooltip; might <b>not</b> be null
         * @return this
         */
        @SuppressWarnings("unchecked")
        public B setTooltip(Customizer<Tooltip> customizer) {
            customizer.customize(Tooltip.forComponent(this.crumbMenu));
            return (B) this;
        }

        /**
         * Customizes the crumb's {@link Tooltip}.
         *
         * @param visible Whether the crumb should be visible
         * @return this
         */
        @SuppressWarnings("unchecked")
        public B setVisible(boolean visible) {
            this.crumbMenu.setVisible(visible);
            return (B) this;
        }

        /**
         * Adds a child crumb to nest under another crumb.
         *
         * @param href The URL to navigate to; might <b>not</b> be null.
         * @param label The label of the crumb; might <b>not</b> be null.
         * @return this
         */
        public B addCrumb(String href, String label) {
            return addCrumb(new Anchor(href, label));
        }

        /**
         * Adds a child crumb to nest under another crumb.
         *
         * @param <NavigationTargetType> The {@link Component} type to navigate to.
         * @param target The @{@link com.vaadin.flow.router.Route} annotated class to navigate to; might <b>not</b> be null.
         * @param label The label of the crumb; might <b>not</b> be null.
         * @return this
         */
        public <NavigationTargetType extends Component> B addCrumb(Class<NavigationTargetType> target, String label) {
            return addCrumb(target, null, null, label);
        }

        /**
         * Adds a child crumb to nest under another crumb.
         *
         * @param <NavigationTargetType> The {@link Component} type to navigate to.
         * @param target The @{@link com.vaadin.flow.router.Route} annotated class to navigate to; might <b>not</b> be null.
         * @param routeParameters The parameters within the route to the target; might be null.
         * @param label The label of the crumb; might <b>not</b> be null.
         * @return this
         */
        public <NavigationTargetType extends Component> B addCrumb(Class<NavigationTargetType> target, RouteParameters routeParameters, String label) {
            return addCrumb(target, routeParameters, null, label);
        }

        /**
         * Adds a child crumb to nest under another crumb.
         *
         * @param <NavigationTargetType> The {@link Component} type to navigate to.
         * @param target The @{@link com.vaadin.flow.router.Route} annotated class to navigate to; might <b>not</b> be null.
         * @param queryParameters The parameters to the query for the target; might be null.
         * @param label The label of the crumb; might <b>not</b> be null.
         * @return this
         */
        public <NavigationTargetType extends Component> B addCrumb(Class<NavigationTargetType> target, QueryParameters queryParameters, String label) {
            return addCrumb(target, null, queryParameters, label);
        }

        /**
         * Adds a child crumb to nest under another crumb.
         *
         * @param <NavigationTargetType> The {@link Component} type to navigate to.
         * @param target The @{@link com.vaadin.flow.router.Route} annotated class to navigate to; might <b>not</b> be null.
         * @param routeParameters The parameters within the route to the target; might be null.
         * @param queryParameters The parameters to the query for the target; might be null.
         * @param label The label of the crumb; might <b>not</b> be null.
         * @return this
         */
        public <NavigationTargetType extends Component> B addCrumb(Class<NavigationTargetType> target, RouteParameters routeParameters, QueryParameters queryParameters, String label) {
            return addCrumb(buildLink(target, routeParameters, queryParameters, label));
        }

        @SuppressWarnings("unchecked")
        private B addCrumb(Component link) {
            this.enabler.enable();
            link.getStyle().set("pointer-events", "all");
            this.crumbMenu.getSubMenu().addItem(link)
                    .getStyle().set("pointer-events", "none");

            return (B) this;
        }

        /**
         * Adds a child crumb to nest under another crumb.
         *
         * @param label The label of the crumb; might <b>not</b> be null.
         * @param customizer A customizer to the {@link ParentCrumbBuilder} building the crumb; might <b>not</b> be null.
         * @return this
         */
        @SuppressWarnings("unchecked")
        public B addCrumb(String label, Customizer<ChildCrumbBuilder> customizer) {
            this.enabler.enable();
            var crumbMenu = this.crumbMenu.getSubMenu().addItem(label);
            crumbMenu.setEnabled(false);

            customizer.customize(new ChildCrumbBuilder(crumbMenu, () -> {
                crumbMenu.setEnabled(true);
                crumbMenu.getStyle().set("cursor", "pointer");
            }));

            return (B) this;
        }
    }

    private class CrumbStyle implements Style {

        private final Map<String, String> properties = new HashMap<>();

        private void apply(Style style) {
            this.properties.forEach(style::set);
        }

        @Override
        public boolean has(String name) {
            return this.properties.containsKey(name);
        }

        @Override
        public Stream<String> getNames() {
            return this.properties.keySet().stream();
        }

        @Override
        public String get(String name) {
            return this.properties.get(name);
        }

        @Override
        public Style set(String name, String value) {
            this.properties.put(name, value);
            Breadcrumb.this.crumbScrollLeftButton.getStyle().set(name, value);
            Breadcrumb.this.crumbLayout.getChildren().forEach(child -> child.getStyle().set(name, value));
            Breadcrumb.this.crumbScrollRightButton.getStyle().set(name, value);
            return this;
        }

        @Override
        public Style remove(String name) {
            this.properties.remove(name);
            Breadcrumb.this.crumbScrollLeftButton.getStyle().remove(name);
            Breadcrumb.this.crumbLayout.getChildren().forEach(child -> child.getStyle().remove(name));
            Breadcrumb.this.crumbScrollRightButton.getStyle().remove(name);
            return this;
        }

        @Override
        public Style clear() {
            this.properties.clear();
            Breadcrumb.this.crumbScrollLeftButton.getStyle().clear();
            Breadcrumb.this.crumbLayout.getChildren().forEach(child -> child.getStyle().clear());
            Breadcrumb.this.crumbScrollRightButton.getStyle().clear();
            return this;
        }
    }

    private final HorizontalLayout crumbLayout;
    private final Button crumbScrollLeftButton;
    private final Button crumbScrollRightButton;
    private final CrumbStyle parentCrumbStyle = new CrumbStyle();

    private int crumbScrollDistance = 50;

    /**
     * Default constructor.
     */
    public Breadcrumb() {
        this.crumbLayout = new HorizontalLayout();
        this.crumbLayout.setWidthFull();
        this.crumbLayout.setPadding(false);
        this.crumbLayout.setSpacing(false);
        this.crumbLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        this.crumbLayout.getStyle().set("overflow-x", "auto");
        this.crumbLayout.getStyle().set("scrollbar-width", "none");

        this.crumbScrollLeftButton = new Button(VaadinIcon.ANGLE_LEFT.create());
        this.crumbScrollLeftButton.addClickListener(event -> {
            this.crumbLayout.getElement().executeJs("this.scrollBy(-"+this.crumbScrollDistance +", 0);");
        });

        this.crumbScrollRightButton = new Button(VaadinIcon.ANGLE_RIGHT.create());
        this.crumbScrollRightButton.addClickListener(event -> {
            this.crumbLayout.getElement().executeJs("this.scrollBy("+this.crumbScrollDistance +", 0);");
        });
    }

    @Override
    protected Component initContent() {
        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);
        mainLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        mainLayout.add(this.crumbScrollLeftButton);
        mainLayout.add(this.crumbLayout);
        mainLayout.add(this.crumbScrollRightButton);

        return mainLayout;
    }

    /**
     * Returns the {@link Style} applied to all crumbs directly nested in the {@link Breadcrumb} itself.
     *
     * @return The style, never null
     */
    public Style getParentCrumbStyle() {
        return this.parentCrumbStyle;
    }

    /**
     * Adds a parent crumb to directly nest in the {@link Breadcrumb} itself.
     *
     * @param label The label of the crumb; might <b>not</b> be null.
     */
    public void addCrumb(String label) {
        addCrumb(null, null, null, label, builder -> {});
    }

    /**
     * Adds a parent crumb to directly nest in the {@link Breadcrumb} itself.
     *
     * @param label The label of the crumb; might <b>not</b> be null.
     * @param customizer A customizer to the {@link ParentCrumbBuilder} building the crumb; might <b>not</b> be null.
     */
    public void addCrumb(String label, Customizer<ParentCrumbBuilder> customizer) {
        addCrumb(null, null, null, label, customizer);
    }

    /**
     * Adds a parent crumb to directly nest in the {@link Breadcrumb} itself.
     *
     * @param href The URL to navigate to; might <b>not</b> be null.
     * @param label The label of the crumb; might <b>not</b> be null.
     */
    public void addCrumb(String href, String label) {
        addCrumb(new Anchor(href, label), builder -> {});
    }

    /**
     * Adds a parent crumb to directly nest in the {@link Breadcrumb} itself.
     *
     * @param href The URL to navigate to; might <b>not</b> be null.
     * @param label The label of the crumb; might <b>not</b> be null.
     * @param customizer A customizer to the {@link ParentCrumbBuilder} building the crumb; might <b>not</b> be null.
     */
    public void addCrumb(String href, String label, Customizer<ParentCrumbBuilder> customizer) {
        addCrumb(new Anchor(href, label), customizer);
    }

    /**
     * Adds a parent crumb to directly nest in the {@link Breadcrumb} itself.
     *
     * @param <NavigationTargetType> The {@link Component} type to navigate to.
     * @param target The @{@link com.vaadin.flow.router.Route} annotated class to navigate to; might <b>not</b> be null.
     * @param label The label of the crumb; might <b>not</b> be null.
     */
    public <NavigationTargetType extends Component> void addCrumb(Class<NavigationTargetType> target, String label) {
        addCrumb(target, null, null, label, builder -> {});
    }

    /**
     * Adds a parent crumb to directly nest in the {@link Breadcrumb} itself.
     *
     * @param <NavigationTargetType> The {@link Component} type to navigate to.
     * @param target The @{@link com.vaadin.flow.router.Route} annotated class to navigate to; might <b>not</b> be null.
     * @param label The label of the crumb; might <b>not</b> be null.
     * @param customizer A customizer to the {@link ParentCrumbBuilder} building the crumb; might <b>not</b> be null.
     */
    public <NavigationTargetType extends Component> void addCrumb(Class<NavigationTargetType> target, String label, Customizer<ParentCrumbBuilder> customizer) {
        addCrumb(target, null, null, label, customizer);
    }

    /**
     * Adds a parent crumb to directly nest in the {@link Breadcrumb} itself.
     *
     * @param <NavigationTargetType> The {@link Component} type to navigate to.
     * @param target The @{@link com.vaadin.flow.router.Route} annotated class to navigate to; might <b>not</b> be null.
     * @param routeParameters The parameters within the route to the target; might be null.
     * @param label The label of the crumb; might <b>not</b> be null.
     */
    public <NavigationTargetType extends Component> void addCrumb(Class<NavigationTargetType> target, RouteParameters routeParameters, String label) {
        addCrumb(target, routeParameters, null, label, builder -> {});
    }

    /**
     * Adds a parent crumb to directly nest in the {@link Breadcrumb} itself.
     *
     * @param <NavigationTargetType> The {@link Component} type to navigate to.
     * @param target The @{@link com.vaadin.flow.router.Route} annotated class to navigate to; might <b>not</b> be null.
     * @param routeParameters The parameters within the route to the target; might be null.
     * @param label The label of the crumb; might <b>not</b> be null.
     * @param customizer A customizer to the {@link ParentCrumbBuilder} building the crumb; might <b>not</b> be null.
     */
    public <NavigationTargetType extends Component> void addCrumb(Class<NavigationTargetType> target, RouteParameters routeParameters, String label, Customizer<ParentCrumbBuilder> customizer) {
        addCrumb(target, routeParameters, null, label, customizer);
    }

    /**
     * Adds a parent crumb to directly nest in the {@link Breadcrumb} itself.
     *
     * @param <NavigationTargetType> The {@link Component} type to navigate to.
     * @param target The @{@link com.vaadin.flow.router.Route} annotated class to navigate to; might <b>not</b> be null.
     * @param queryParameters The parameters to the query for the target; might be null.
     * @param label The label of the crumb; might <b>not</b> be null.
     */
    public <NavigationTargetType extends Component> void addCrumb(Class<NavigationTargetType> target, QueryParameters queryParameters, String label) {
        addCrumb(target, null, queryParameters, label, builder -> {});
    }

    /**
     * Adds a parent crumb to directly nest in the {@link Breadcrumb} itself.
     *
     * @param <NavigationTargetType> The {@link Component} type to navigate to.
     * @param target The @{@link com.vaadin.flow.router.Route} annotated class to navigate to; might <b>not</b> be null.
     * @param queryParameters The parameters to the query for the target; might be null.
     * @param label The label of the crumb; might <b>not</b> be null.
     * @param customizer A customizer to the {@link ParentCrumbBuilder} building the crumb; might <b>not</b> be null.
     */
    public <NavigationTargetType extends Component> void addCrumb(Class<NavigationTargetType> target, QueryParameters queryParameters, String label, Customizer<ParentCrumbBuilder> customizer) {
        addCrumb(target, null, queryParameters, label, customizer);
    }

    /**
     * Adds a parent crumb to directly nest in the {@link Breadcrumb} itself.
     *
     * @param <NavigationTargetType> The {@link Component} type to navigate to.
     * @param target The @{@link com.vaadin.flow.router.Route} annotated class to navigate to; might <b>not</b> be null.
     * @param routeParameters The parameters within the route to the target; might be null.
     * @param queryParameters The parameters to the query for the target; might be null.
     * @param label The label of the crumb; might <b>not</b> be null.
     */
    public <NavigationTargetType extends Component> void addCrumb(Class<NavigationTargetType> target, RouteParameters routeParameters, QueryParameters queryParameters, String label) {
        addCrumb(target, routeParameters, queryParameters, label, builder -> {});
    }

    /**
     * Adds a parent crumb to directly nest in the {@link Breadcrumb} itself.
     *
     * @param <NavigationTargetType> The {@link Component} type to navigate to.
     * @param target The @{@link com.vaadin.flow.router.Route} annotated class to navigate to; might <b>not</b> be null.
     * @param routeParameters The parameters within the route to the target; might be null.
     * @param queryParameters The parameters to the query for the target; might be null.
     * @param label The label of the crumb; might <b>not</b> be null.
     * @param customizer A customizer to the {@link ParentCrumbBuilder} building the crumb; might <b>not</b> be null.
     */
    public <NavigationTargetType extends Component> void addCrumb(Class<NavigationTargetType> target, RouteParameters routeParameters, QueryParameters queryParameters, String label, Customizer<ParentCrumbBuilder> customizer) {
        addCrumb(buildLink(target, routeParameters, queryParameters, label), customizer);
    }

    private void addCrumb(Component link, Customizer<ParentCrumbBuilder> customizer) {
        var crumb = new MenuBar();
        crumb.setSizeUndefined();
        crumb.addThemeVariants(MenuBarVariant.LUMO_ICON, MenuBarVariant.LUMO_DROPDOWN_INDICATORS);
        crumb.setVisible(this.crumbLayout.getChildren().findAny().isPresent());
        this.crumbLayout.add(crumb);

        this.parentCrumbStyle.apply(link.getStyle()
                .set("margin-top", "-3px"));
        this.crumbLayout.add(link);

        var crumbMenu = crumb.addItem("⯈");
        this.parentCrumbStyle.apply(crumbMenu.getStyle()
                .set("margin-top", "-3px")
                .set("font-size", "0.6em"));
        crumbMenu.setEnabled(false);

        customizer.customize(new ParentCrumbBuilder(crumbMenu, () -> {
            crumb.setVisible(true);
            crumbMenu.setEnabled(true);
            crumbMenu.setText("⯆");
            crumbMenu.getStyle()
                    .set("cursor", "pointer");
        }));
    }

    private <NavigationTargetType extends Component> RouterLink buildLink(Class<NavigationTargetType> target, RouteParameters routeParameters, QueryParameters queryParameters, String label) {
        var link = new RouterLink();
        link.setText(label);

        if (target != null) {
            if (routeParameters != null) {
                link.setRoute(target, routeParameters);
            } else {
                link.setRoute(target);
            }

            if (queryParameters != null) {
                link.setQueryParameters(queryParameters);
            }
        } else {
            link.getStyle().set("font-weight", "bold");
        }

        return link;
    }

    /**
     * Removes all parent crumbs directly nested in the {@link Breadcrumb} itself.
     */
    public void clearCrumbs() {
        this.crumbLayout.removeAll();
    }

    /**
     * Returns the scroll distance in pixels on every scroll left/right click.
     *
     * @return The distance in pixels, greater than 0
     */
    public int getScrollDistance() {
        return this.crumbScrollDistance;
    }

    /**
     * Sets the scroll distance in pixels on every scroll left/right click.
     *
     * @param distance The distance to set, in pixels; might <b>not</b> be smaller than 1
     */
    public void setScrollDistance(int distance) {
        if (distance < 1) {
            throw new IllegalArgumentException("Cannot scroll for a distance smaller than 1 px");
        }

        this.crumbScrollDistance = distance;
    }

    /**
     * Sets a tooltip text for scrolling left on the crumbs.
     *
     * @param text The text, might be null
     */
    public void setScrollLeftTooltip(String text) {
        this.crumbScrollLeftButton.setTooltipText(text);
    }

    /**
     * Sets a tooltip text for scrolling right on the crumbs.
     *
     * @param text The text, might be null
     */
    public void setScrollRightTooltip(String text) {
        this.crumbScrollRightButton.setTooltipText(text);
    }
}
