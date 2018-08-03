package com.mantledillusion.essentials.vaadin.util;

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;

/**
 * An immutable {@link Resource} that allows streaming a {@link RenderedImage}.
 */
public class RenderedImageStreamResource extends StreamResource {

	private static final long serialVersionUID = 1L;

	private static class RenderedImageStreamSource implements StreamSource {

		private static final long serialVersionUID = 1L;

		private final RenderedImage imageSource;

		private RenderedImageStreamSource(RenderedImage imageSource) {
			this.imageSource = imageSource;
		}

		@Override
		public InputStream getStream() {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if (this.imageSource != null) {
				try {
					ImageIO.write(this.imageSource, "png", baos);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return new ByteArrayInputStream(baos.toByteArray());
		}
	}

	/**
	 * {@link Constructor}; set the given {@link RenderedImage}.
	 * 
	 * @param img
	 *            The {@link RenderedImage} to stream; might be null.
	 */
	public RenderedImageStreamResource(RenderedImage img) {
		super(new RenderedImageStreamSource(img), makeImageFilename());
	}

	/*
	 * Creates a PNG image file name with a timestamp.
	 */
	private static String makeImageFilename() {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String timestamp = df.format(new Date());
		return "connectors-" + timestamp + ".png";
	}

	/**
	 * Getter for the current {@link RenderedImage} source.
	 * 
	 * @return The {@link RenderedImage} that is currently set; might be null
	 */
	public RenderedImage getImageSource() {
		return ((RenderedImageStreamSource) getStreamSource()).imageSource;
	}
}
