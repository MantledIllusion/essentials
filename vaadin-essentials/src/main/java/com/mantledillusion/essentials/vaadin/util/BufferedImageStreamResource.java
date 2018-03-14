package com.mantledillusion.essentials.vaadin.util;

import java.awt.image.BufferedImage;
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
 * A {@link Resource} that allows streaming a {@link BufferedImage}.
 */
public class BufferedImageStreamResource extends StreamResource {

	private static class ImageStreamSource implements StreamSource {

		private static final long serialVersionUID = 1L;

		private BufferedImage imageSource;

		@Override
		public InputStream getStream() {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if (imageSource != null) {
				try {
					ImageIO.write(imageSource, "png", baos);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return new ByteArrayInputStream(baos.toByteArray());
		}
	}

	private static final long serialVersionUID = 1L;

	private final ImageStreamSource source;

	/**
	 * {@link Constructor}; no {@link BufferedImage} is set.
	 */
	public BufferedImageStreamResource() {
		this(new ImageStreamSource());
	}

	/**
	 * {@link Constructor}; set the given {@link BufferedImage}.
	 * 
	 * @param img
	 *            The {@link BufferedImage} to stream; might be null.
	 */
	public BufferedImageStreamResource(BufferedImage img) {
		this();
		setImageSource(img);
	}

	private BufferedImageStreamResource(ImageStreamSource sc) {
		super(sc, "img.png");
		this.source = sc;
	}

	/**
	 * Getter for the current {@link BufferedImage} source.
	 * 
	 * @return The {@link BufferedImage} that is currently set; might be null
	 */
	public BufferedImage getImageSource() {
		return this.source.imageSource;
	}

	/**
	 * Setter for the curren {@link BufferedImage} source.
	 * 
	 * @param img
	 *            The {@link BufferedImage} to set; might be null.
	 */
	public void setImageSource(BufferedImage img) {
		this.source.imageSource = img;
		this.setFilename(makeImageFilename());
	}

	/*
	 * Creates a PNG image file name with a timestamp.
	 */
	private String makeImageFilename() {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String timestamp = df.format(new Date());
		return "connectors-" + timestamp + ".png";
	}
}
