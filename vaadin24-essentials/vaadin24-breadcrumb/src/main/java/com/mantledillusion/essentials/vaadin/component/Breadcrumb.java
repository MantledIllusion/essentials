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

public class Breadcrumb extends Composite<Component> implements HasSize {

    private interface Enabler {

        void enable();
    }

    public interface Customizer<B> {

        void customize(B builder);
    }

    public class ParentCrumbBuilder extends AbstractCrumbBuilder<ParentCrumbBuilder> {

        private ParentCrumbBuilder(MenuItem crumbMenu, Enabler enabler) {
            super(crumbMenu, enabler);
        }

        public ParentCrumbBuilder pointLeft() {
            this.crumbMenu.setText("⯇");
            return  this;
        }
    }

    public class ChildCrumbBuilder extends AbstractCrumbBuilder<ParentCrumbBuilder> {

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

        @SuppressWarnings("unchecked")
        public B setStyle(Customizer<Style> customizer) {
            customizer.customize(this.crumbMenu.getStyle());
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B setTooltip(Customizer<Tooltip> customizer) {
            customizer.customize(Tooltip.forComponent(this.crumbMenu));
            return (B) this;
        }

        public B addCrumb(String href, String label) {
            return addCrumb(new Anchor(href, label));
        }

        public <NavigationTargetType extends Component> B addCrumb(Class<NavigationTargetType> target, String label) {
            return addCrumb(target, null, null, label);
        }

        public <NavigationTargetType extends Component> B addCrumb(Class<NavigationTargetType> target, RouteParameters routeParameters, String label) {
            return addCrumb(target, routeParameters, null, label);
        }

        public <NavigationTargetType extends Component> B addCrumb(Class<NavigationTargetType> target, QueryParameters queryParameters, String label) {
            return addCrumb(target, null, queryParameters, label);
        }

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
    private final CrumbStyle crumbStyle = new CrumbStyle();

    private int crumbScrollDistance = 50;

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

    public Style getCrumbStyle() {
        return this.crumbStyle;
    }

    public void addCrumb(String label) {
        addCrumb(null, null, null, label, builder -> {});
    }

    public void addCrumb(String label, Customizer<ParentCrumbBuilder> customizer) {
        addCrumb(null, null, null, label, customizer);
    }

    public void addCrumb(String href, String label) {
        addCrumb(new Anchor(href, label), builder -> {});
    }

    public void addCrumb(String href, String label, Customizer<ParentCrumbBuilder> customizer) {
        addCrumb(new Anchor(href, label), customizer);
    }

    public <NavigationTargetType extends Component> void addCrumb(Class<NavigationTargetType> target, String label) {
        addCrumb(target, null, null, label, builder -> {});
    }

    public <NavigationTargetType extends Component> void addCrumb(Class<NavigationTargetType> target, String label, Customizer<ParentCrumbBuilder> customizer) {
        addCrumb(target, null, null, label, customizer);
    }

    public <NavigationTargetType extends Component> void addCrumb(Class<NavigationTargetType> target, RouteParameters routeParameters, String label) {
        addCrumb(target, routeParameters, null, label, builder -> {});
    }

    public <NavigationTargetType extends Component> void addCrumb(Class<NavigationTargetType> target, RouteParameters routeParameters, String label, Customizer<ParentCrumbBuilder> customizer) {
        addCrumb(target, routeParameters, null, label, customizer);
    }

    public <NavigationTargetType extends Component> void addCrumb(Class<NavigationTargetType> target, QueryParameters queryParameters, String label) {
        addCrumb(target, null, queryParameters, label, builder -> {});
    }

    public <NavigationTargetType extends Component> void addCrumb(Class<NavigationTargetType> target, QueryParameters queryParameters, String label, Customizer<ParentCrumbBuilder> customizer) {
        addCrumb(target, null, queryParameters, label, customizer);
    }

    public <NavigationTargetType extends Component> void addCrumb(Class<NavigationTargetType> target, RouteParameters routeParameters, QueryParameters queryParameters, String label) {
        addCrumb(target, routeParameters, queryParameters, label, builder -> {});
    }

    public <NavigationTargetType extends Component> void addCrumb(Class<NavigationTargetType> target, RouteParameters routeParameters, QueryParameters queryParameters, String label, Customizer<ParentCrumbBuilder> customizer) {
        addCrumb(buildLink(target, routeParameters, queryParameters, label), customizer);
    }

    private void addCrumb(Component link, Customizer<ParentCrumbBuilder> customizer) {
        var crumb = new MenuBar();
        crumb.setSizeUndefined();
        crumb.addThemeVariants(MenuBarVariant.LUMO_ICON, MenuBarVariant.LUMO_DROPDOWN_INDICATORS);
        crumb.setVisible(this.crumbLayout.getChildren().findAny().isPresent());
        this.crumbLayout.add(crumb);

        this.crumbStyle.apply(link.getStyle()
                .set("margin-top", "-3px"));
        this.crumbLayout.add(link);

        var crumbMenu = crumb.addItem("⯈");
        this.crumbStyle.apply(crumbMenu.getStyle()
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
     * Returns the scroll distance in pixels on every scroll left/right click.
     *
     * @return The distance in pixels, > 0
     */
    public int getScrollDistance() {
        return this.crumbScrollDistance;
    }

    /**
     * Sets the scroll distance in pixels on every scroll left/right click.
     *
     * @param distance The distance to set, in pixels; might <b>not</b> be <1
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
