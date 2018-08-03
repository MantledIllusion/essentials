package com.mantledillusion.essentials.vaadin.component;

import java.awt.image.RenderedImage;
import java.util.LinkedHashSet;
import java.util.Objects;

import com.mantledillusion.essentials.vaadin.util.RenderedImageStreamResource;
import com.vaadin.data.HasValue;
import com.vaadin.server.ErrorHandler;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.Image;

/**
 * {@link Composite} {@link Component} that is able to stream
 * {@link RenderedImage}s to the browser.
 */
public class StreamedImage extends Composite implements HasValue<RenderedImage> {

	private static final long serialVersionUID = 1L;

	private LinkedHashSet<ValueChangeListener<RenderedImage>> listenerList;

	public StreamedImage(RenderedImage img) {
		this();
		setValue(img);
	}

	public StreamedImage() {
		super(new Image());

		/*
		 * Errors on image components occur in 99.9% of cases because the client's
		 * browser interrupted the connection due to huge image files taking too long
		 * for transfer.
		 * 
		 * These Problems have no big impact on usability, but are displayed by vaadin
		 * on the image component automatically, which seems odd to the user as there
		 * does not seem to be any problem.
		 * 
		 * This empty error handler interrupts the error event from being shown on the
		 * component.
		 */
		setErrorHandler(new ErrorHandler() {
			private static final long serialVersionUID = 1L;

			@Override
			public void error(com.vaadin.server.ErrorEvent event) {
			}
		});
	}

	@Override
	public void setValue(RenderedImage value) {
		RenderedImage oldValue = getValue();
		((Image) getCompositionRoot()).setSource(new RenderedImageStreamResource(value));
		if (this.listenerList != null && !Objects.equals(oldValue, value)) {
			for (ValueChangeListener<RenderedImage> valueChangeListener : this.listenerList) {
				valueChangeListener.valueChange(new ValueChangeEvent<>(null, this, oldValue, false));
			}
		}
		this.markAsDirty();
	}

	@Override
	public RenderedImage getValue() {
		Image imgComponent = ((Image) getCompositionRoot());
		return imgComponent.getSource() != null
				? ((RenderedImageStreamResource) imgComponent.getSource()).getImageSource()
				: null;
	}

	@Override
	public Registration addValueChangeListener(ValueChangeListener<RenderedImage> listener) {
		Objects.requireNonNull(listener, "Listener must not be null.");
		if (this.listenerList == null) {
			this.listenerList = new LinkedHashSet<>();
		}
		this.listenerList.add(listener);

		return () -> this.listenerList.remove(listener);
	}

	@Override
	public boolean isRequiredIndicatorVisible() {
		return false;
	}

	@Override
	public void setRequiredIndicatorVisible(boolean visible) {
		if (visible)
			throw new IllegalArgumentException("Not Writable");
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		if (!readOnly)
			throw new IllegalArgumentException("Not Writable");
	}
}
