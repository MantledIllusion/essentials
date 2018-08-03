package com.mantledillusion.essentials.vaadin.component;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.Map;

import com.mantledillusion.essentials.vaadin.util.ComponentUtil;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.event.MouseEvents.ClickEvent;
import com.vaadin.server.Resource;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.AbsoluteLayout.ComponentPosition;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Slider;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

public class StreamedImageViewer extends Composite {

	private static final long serialVersionUID = 1L;

	private static final int MINIMUM_SCALE_PERCENTAGE = 1;
	private static final int MAXIMUM_SCALE_PERCENTAGE = 100;

	private static final int ZOOM_SLIDER_WIDTH = 150;

	public static enum OverlayAnchor {

		/**
		 * Overlay will be attached to its position on its left edge.
		 */
		LEFT_EDGE,

		/**
		 * Overlay will be attached to its position on its top edge.
		 */
		TOP_EDGE,

		/**
		 * Overlay will be attached to its position on its right edge.
		 */
		RIGHT_EDGE,

		/**
		 * Overlay will be attached to its position on its bottom edge.
		 */
		BOTTOM_EDGE;
	}

	private static final class OverlayPosition {

		private OverlayAnchor anchor;
		private int x;
		private int y;
		private int xShift;
		private int yShift;
	}

	private static class FullSizeView extends Window {

		private static final long serialVersionUID = 1L;

		private FullSizeView(BufferedImage img) {
			super(null);

			setContent(new StreamedImage(img));

			setModal(true);
			setClosable(true);
			setResizable(false);
			setDraggable(false);

			setSizeFull();
			center();

			UI.getCurrent().addWindow(this);
		}
	}

	private final VerticalLayout layout;

	private final Panel imageScrollPanel;
	private final VerticalLayout imageLayout;
	private final AbsoluteLayout imagePane;
	private final StreamedImage streamedImage;

	private final HorizontalLayout toolBar;
	private final Button fullSize;
	private final Button zoomOut;
	private final Slider zoom;
	private final Button zoomIn;

	private final Map<Component, OverlayPosition> dockedOverlays = new IdentityHashMap<>();

	private BufferedImage currentlyDisplayedImage = null;

	public StreamedImageViewer() {
		this(null, null, null);
	}

	public StreamedImageViewer(Resource fullSizeIconResource, Resource zoomOutIconResource,
			Resource zoomInIconResource) {
		this.layout = new VerticalLayout();
		this.layout.setSizeFull();
		this.layout.setMargin(false);
		this.layout.setSpacing(true);

		/*
		 * The imageScrollPanel is the back panel of the image area.
		 * 
		 * It offers its scrolling ability.
		 */
		this.imageScrollPanel = new Panel();
		this.imageScrollPanel.setSizeFull();
		this.imageScrollPanel.setStyleName(ValoTheme.PANEL_BORDERLESS);
		this.layout.addComponent(imageScrollPanel);
		this.layout.setExpandRatio(imageScrollPanel, 1);

		/*
		 * The imageLayout is the component that is the scrolling background.
		 * 
		 * Its needed for centering the possibly smaller image.
		 */
		this.imageLayout = new VerticalLayout();
		this.imageLayout.setMargin(false);
		this.imageLayout.setSpacing(true);
		this.imageScrollPanel.setContent(this.imageLayout);

		/*
		 * The imagePane the already centered image background that also has the same
		 * size as the image.
		 * 
		 * Its needed for holding the image, receiving click events on the image and
		 * placing overlays by coordinates.
		 */
		this.imagePane = new AbsoluteLayout();
		this.imageLayout.addComponent(this.imagePane);
		this.imageLayout.setComponentAlignment(this.imagePane, Alignment.MIDDLE_CENTER);

		/*
		 * The streamedImage is the image component itself.
		 */
		this.streamedImage = new StreamedImage();
		this.streamedImage.setSizeFull();
		this.imagePane.addComponent(this.streamedImage, "left: 0px; top:0px; right: 0px; bottom: 0px");

		this.toolBar = new HorizontalLayout();
		this.toolBar.setSizeUndefined();
		this.toolBar.setSpacing(true);
		this.layout.addComponent(this.toolBar);
		this.layout.setComponentAlignment(this.toolBar, Alignment.BOTTOM_RIGHT);
		this.layout.setExpandRatio(this.toolBar, 0);

		this.zoomOut = zoomOutIconResource != null ? new Button(zoomOutIconResource) : new Button("&#8722;");
		this.zoomOut.setCaptionAsHtml(true);
		this.zoomOut.setStyleName(ValoTheme.BUTTON_TINY);
		this.toolBar.addComponent(this.zoomOut);
		this.toolBar.setExpandRatio(this.zoomOut, 0);

		this.zoom = new Slider(MINIMUM_SCALE_PERCENTAGE, MAXIMUM_SCALE_PERCENTAGE, 1);
		this.zoom.setValue(100.0);
		this.zoom.setWidth(ZOOM_SLIDER_WIDTH, Unit.PIXELS);
		this.toolBar.addComponent(this.zoom);
		this.toolBar.setExpandRatio(this.zoom, 0);

		this.zoomIn = zoomInIconResource != null ? new Button(zoomInIconResource) : new Button("&#43;");
		this.zoomIn.setCaptionAsHtml(true);
		this.zoomIn.setStyleName(ValoTheme.BUTTON_TINY);
		this.toolBar.addComponent(this.zoomIn);
		this.toolBar.setExpandRatio(this.zoomIn, 0);

		this.fullSize = fullSizeIconResource != null ? new Button(fullSizeIconResource) : new Button("&#8689;");
		this.fullSize.setCaptionAsHtml(true);
		this.fullSize.setStyleName(ValoTheme.BUTTON_TINY);
		this.toolBar.addComponent(this.fullSize);
		this.toolBar.setExpandRatio(this.fullSize, 1);

		buildActionHandlers();

		setCompositionRoot(this.layout);
		this.layout.setEnabled(false);
	}
	
	private boolean ignoreZooming = false;

	private void buildActionHandlers() {
		this.imagePane.addLayoutClickListener(new LayoutClickListener() {

			private static final long serialVersionUID = 1L;

			@Override
			public void layoutClick(LayoutClickEvent event) {
				double zoomScale = zoom.getValue() / 100;
				StreamedImageViewer.this.fireEvent(new ImageClickedEvent(StreamedImageViewer.this, event, zoomScale));
			}
		});

		this.zoomOut.addClickListener(new Button.ClickListener() {

			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
				ComponentUtil.fetchClientSideComponentInfo(imageScrollPanel,
						new ComponentUtil.ClientSideInfoCallback() {

							@Override
							public void infoFetched(int xPos, int yPos, int width, int height) {
								float scale = Math.max(MINIMUM_SCALE_PERCENTAGE / 100f,
										Math.min(1f, Math.min(width / ((float) currentlyDisplayedImage.getWidth()),
												height / ((float) currentlyDisplayedImage.getHeight()))));

								ignoreZooming = true;
								zoom.setValue((double) scale * 100);
								ignoreZooming = false;
								
								scaleCurrentImage(scale);
							}
						});
			}
		});

		this.zoom.addValueChangeListener(new ValueChangeListener<Double>() {

			private static final long serialVersionUID = 1L;

			@Override
			public void valueChange(ValueChangeEvent<Double> event) {
				if (!ignoreZooming) {
					scaleCurrentImage((float) (event.getValue() / 100));
				}
			}
		});

		this.zoomIn.addClickListener(new Button.ClickListener() {

			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
				ignoreZooming = true;
				zoom.setValue(100d);
				ignoreZooming = false;
				
				scaleCurrentImage(1);
			}
		});

		this.fullSize.addClickListener(new Button.ClickListener() {

			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
				new FullSizeView(currentlyDisplayedImage);
			}
		});
	}

	private void scaleCurrentImage(float scale) {
		ComponentUtil.fetchClientSideComponentInfo(imageScrollPanel, new ComponentUtil.ClientSideInfoCallback() {

			@Override
			public void infoFetched(int xPos, int yPos, int width, int height) {
				resizeImagePane(width, height, scale);
			}
		});
	}
	
	private void resizeImagePane(int width, int height, float scale) {
		this.imagePane.setWidth(this.currentlyDisplayedImage.getWidth() * scale, Unit.PIXELS);
		this.imagePane.setHeight(this.currentlyDisplayedImage.getHeight() * scale, Unit.PIXELS);
		if (width < this.currentlyDisplayedImage.getWidth() * scale) {
			imageLayout.setWidth(this.currentlyDisplayedImage.getWidth() * scale, Unit.PIXELS);
		} else {
			imageLayout.setWidth(100, Unit.PERCENTAGE);
		}
		if (height < this.currentlyDisplayedImage.getHeight() * scale) {
			imageLayout.setHeight(this.currentlyDisplayedImage.getHeight() * scale, Unit.PIXELS);
		} else {
			imageLayout.setHeight(100, Unit.PERCENTAGE);
		}
		
		repositionOverlays();
	}
	
	public final void setValue(BufferedImage img) {
		this.currentlyDisplayedImage = img;
		
		ComponentUtil.fetchClientSideComponentInfo(imageScrollPanel, new ComponentUtil.ClientSideInfoCallback() {

			@Override
			public void infoFetched(int xPos, int yPos, int width, int height) {
				streamedImage.setValue(img);
				resizeImagePane(width, height, 1);
			}
		});
		
		this.ignoreZooming = true;
		this.zoom.setValue(100.0);
		this.ignoreZooming = false;
		
		this.layout.setEnabled(this.currentlyDisplayedImage != null);
	}

	/**
	 * Scrolls the image viewer to the given point on the viewer's canvas.
	 * 
	 * Note that the given pointer should be related to this viewer's image in
	 * original resolution - if this viewer is zoomed somehow, the correct point is
	 * automatically calculated.
	 */
	public void scrollToPoint(final Point p) {
		ComponentUtil.fetchClientSideComponentInfo(imageScrollPanel, new ComponentUtil.ClientSideInfoCallback() {

			@Override
			public void infoFetched(int xPos, int yPos, int width, int height) {
				imageScrollPanel
						.setScrollLeft(Math.max(0, (int) Math.round((p.x * (zoom.getValue() / 100) - width / 2.0))));
				imageScrollPanel
						.setScrollTop(Math.max(0, (int) Math.round((p.y * (zoom.getValue() / 100) - height / 2.0))));
			}
		});
	}

	public void setResizingToolVisible(boolean visible) {
		this.zoomOut.setVisible(visible);
		this.zoom.setVisible(visible);
		this.zoomIn.setVisible(visible);
		refreshToolbarVisibility();
	}
	
	public void setFullsizeToolVisible(boolean visible) {
		this.fullSize.setVisible(visible);
		refreshToolbarVisibility();
	}
	
	private void refreshToolbarVisibility() {
		this.toolBar.setVisible(this.zoomOut.isVisible() || this.fullSize.isVisible());
	}

	public void dockOverlay(Component overlay, int x, int y) {
		dockOverlay(overlay, OverlayAnchor.TOP_EDGE, x, y, 0, 0);
	}

	public void dockOverlay(Component overlay, OverlayAnchor anchor, int x, int y) {
		dockOverlay(overlay, anchor, x, y, 0, 0);
	}

	public void dockOverlay(Component overlay, OverlayAnchor anchor, int x, int y, int xShift, int yShift) {
		if (overlay == null) {
			throw new IllegalArgumentException("Cannot dock a null overlay");
		} else if (this.dockedOverlays.containsKey(overlay)) {
			throw new IllegalStateException("The given overlay is already docked.");
		}
		this.imagePane.addComponent(overlay);
		this.dockedOverlays.put(overlay, new OverlayPosition());
		repositionOverlay(overlay, anchor, x, y, xShift, yShift);
	}

	public void repositionOverlay(Component overlay) {
		if (!this.dockedOverlays.containsKey(overlay)) {
			throw new IllegalStateException("The given overlay is not docked.");
		}
		OverlayPosition pos = this.dockedOverlays.get(overlay);
		repositionOverlay(overlay, pos.anchor, pos.x, pos.y, pos.xShift, pos.yShift);
	}

	public void repositionOverlay(Component overlay, int x, int y) {
		if (!this.dockedOverlays.containsKey(overlay)) {
			throw new IllegalStateException("The given overlay is not docked.");
		}
		OverlayPosition pos = this.dockedOverlays.get(overlay);
		repositionOverlay(overlay, pos.anchor, x, y, pos.xShift, pos.yShift);
	}

	public void repositionOverlay(Component overlay, OverlayAnchor anchor, int x, int y) {
		if (!this.dockedOverlays.containsKey(overlay)) {
			throw new IllegalStateException("The given overlay is not docked.");
		}
		OverlayPosition pos = this.dockedOverlays.get(overlay);
		repositionOverlay(overlay, anchor, x, y, pos.xShift, pos.yShift);
	}

	public void repositionOverlay(Component overlay, OverlayAnchor anchor, int x, int y, int xShift, int yShift) {
		if (anchor == null) {
			throw new IllegalArgumentException("Cannot dock an overlay at a null anchor.");
		} else if (!this.dockedOverlays.containsKey(overlay)) {
			throw new IllegalStateException("The given overlay is not docked.");
		}

		OverlayPosition pos = this.dockedOverlays.get(overlay);
		pos.anchor = anchor;
		pos.x = x;
		pos.y = y;
		pos.xShift = xShift;
		pos.yShift = yShift;

		float zoomScale = (float) (this.zoom.getValue() / 100);

		final Float left;
		final Float top;
		final Float right;
		final Float bottom;
		switch (anchor) {
		case LEFT_EDGE:
			left = (x) * zoomScale;
			top = (y + yShift) * zoomScale;
			right = null;
			bottom = null;
			break;
		case TOP_EDGE:
			left = (x + xShift) * zoomScale;
			top = (y) * zoomScale;
			right = null;
			bottom = null;
			break;
		case RIGHT_EDGE:
			left = null;
			top = (y + yShift) * zoomScale;
			right = this.imagePane.getWidth() - x * zoomScale;
			bottom = null;
			break;
		case BOTTOM_EDGE:
			left = (x + xShift) * zoomScale;
			top = null;
			right = null;
			bottom = this.imagePane.getHeight() - y * zoomScale;
			break;
		default:
			left = null;
			top = null;
			right = null;
			bottom = null;
		}

		ComponentPosition p = this.imagePane.new ComponentPosition();
		p.setLeft(left, Unit.PIXELS);
		p.setTop(top, Unit.PIXELS);
		p.setRight(right, Unit.PIXELS);
		p.setBottom(bottom, Unit.PIXELS);
		this.imagePane.setPosition(overlay, p);

		// We can measure the components size only after it actually has been added to the DOM at the client.
		ComponentUtil.fetchClientSideComponentInfo(overlay, new ComponentUtil.ClientSideInfoCallback() {

			@Override
			public void infoFetched(int xPos, int yPos, int width, int height) {

				Float right = StreamedImageViewer.this.imagePane.getWidth() - (left + width);
				Float bottom = StreamedImageViewer.this.imagePane.getHeight() - (top + height);

				boolean isChanged = false;
				if (right < 0) {
					right = Math.max(0, right);
					isChanged = true;
				}
				if (bottom < 0) {
					bottom = Math.max(0, bottom);
					isChanged = true;
				}
				if (isChanged) {
					ComponentPosition p = StreamedImageViewer.this.imagePane.new ComponentPosition();
					p.setLeftValue(null);
					p.setTopValue(null);
					p.setRight(right, Unit.PIXELS);
					p.setBottom(bottom, Unit.PIXELS);
					StreamedImageViewer.this.imagePane.setPosition(overlay, p);
				}
			}
		});
	}

	public void repositionOverlays() {
		for (Component overlay : this.dockedOverlays.keySet()) {
			repositionOverlay(overlay);
		}
	}

	public void undockOverlay(Component overlay) {
		if (overlay == null) {
			throw new IllegalArgumentException("Cannot undock a null overlay");
		} else if (!this.dockedOverlays.containsKey(overlay)) {
			throw new IllegalStateException("The given overlay is not docked.");
		}
		this.imagePane.removeComponent(overlay);
		this.dockedOverlays.remove(overlay);
	}

	public void undockOverlays() {
		for (Component overlay : this.dockedOverlays.keySet()) {
			undockOverlay(overlay);
		}
	}

	/**
	 * Event that is fired when the image surface of the {@link StreamedImageViewer}
	 * is clicked on some point.
	 */
	public static class ImageClickedEvent extends Event {

		private static final long serialVersionUID = 1L;

		private final Point imageClickPoint;
		private final MouseButton button;
		private final boolean isDoubleClick;

		private ImageClickedEvent(StreamedImageViewer source, ClickEvent originalClickEvent, double zoomScale) {
			super(source);
			this.imageClickPoint = new Point((int) Math.round(originalClickEvent.getRelativeX() * (1 / zoomScale)),
					(int) Math.round(originalClickEvent.getRelativeY() * (1 / zoomScale)));
			this.button = originalClickEvent.getButton();
			this.isDoubleClick = originalClickEvent.isDoubleClick();
		}

		/**
		 * Returns the exact point on the image that was clicked.
		 */
		public Point getImageClickPoint() {
			return imageClickPoint;
		}

		public MouseButton getButton() {
			return button;
		}

		public boolean isDoubleClick() {
			return isDoubleClick;
		}
	}
}
