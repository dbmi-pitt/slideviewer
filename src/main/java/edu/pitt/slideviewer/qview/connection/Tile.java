package edu.pitt.slideviewer.qview.connection;

import java.awt.*;

import edu.pitt.slideviewer.ImageTransform;


/**
 * This class represents a single tile data
 * 
 * @author tseytlin
 */
public class Tile {
	private byte[] data;
	private Image image;
	private Point offset;
	private int pyramidLevel;
	private Rectangle bounds,originalBounds;
	private double scale;
	private int accessed;
	//private int rotate, flip;
	private ImageTransform transform;

	/**
	 * @return the isLoaded
	 */
	public boolean isLoaded() {
		return image != null;
	}

	/**
	 * return rough estimate of tile size in bytes
	 * 
	 * @return
	 */
	public int getTileSize() {
		int size = 16;
		if (data != null)
			size += data.length;
		if (image != null)
			size += image.getWidth(null) * image.getHeight(null) * 4;
		return size;
	}

	/**
	 * @return the offset
	 */
	public Point getOffset() {
		return offset;
	}

	/**
	 * @param offset
	 *            the offset to set
	 */
	public void setOffset(Point offset) {
		this.offset = offset;
	}

	/**
	 * @return the pyramidLevel
	 */
	public int getPyramidLevel() {
		return pyramidLevel;
	}

	/**
	 * @param pyramidLevel
	 *            the pyramidLevel to set
	 */
	public void setPyramidLevel(int pyramidLevel) {
		this.pyramidLevel = pyramidLevel;
	}

	/**
	 * Get number of times this tile was accessed
	 * 
	 * @return the accessed
	 */
	public int getAccessCount() {
		return accessed;
	}

	/**
	 * this is used for Most Frequently Used statistics
	 */
	public void incrementAccessCount() {
		accessed++;
	}

	/**
	 * @return the bounds in absolut coordinates
	 */
	public Rectangle getBounds() {
		return bounds;
	}

	/**
	 * @param bounds
	 *            the bounds to set
	 */
	public void setBounds(Rectangle bounds) {
		this.bounds = bounds;
	}

	/**
	 * @return the image
	 */
	public Image getImage() {
		// load image if data is there, but image was not
		// rendered
		if (image == null && data != null) {
			image = Toolkit.getDefaultToolkit().createImage(data);
			Utils.flushImage(image);
			//image = Utils.getTransformedImage(image,rotate,flip);
			image = getImageTransform().getTransformedImage(image);
			return image;
		}
		return image;
	}

	/**
	 * @param image
	 *            the image to set
	 */
	public void setImage(Image image) {
		this.image = image;
	}

	/**
	 * @return the scale
	 */
	public double getScale() {
		return scale;
	}

	/**
	 * @param scale
	 *            the scale to set
	 */
	public void setScale(double scale) {
		this.scale = scale;
	}

	/**
	 * @param data
	 *            the data to set
	 */
	public void setData(byte[] data) {
		this.data = data;
	}

	/*
	public int getRotationTransform() {
		return rotate;
	}

	public void setRotationTransform(int rotation) {
		this.rotate = rotation;
	}

	public int getFlipTransform() {
		return flip;
	}

	public void setFlipTransform(int flip) {
		this.flip = flip;
	}
	*/
	
	public void setImageTransform(ImageTransform transform){
		this.transform = transform;
	}
	
	public ImageTransform getImageTransform(){
		if(transform == null)
			transform = new ImageTransform();
		return transform;
	}
	

	public Rectangle getOriginalBounds() {
		return (originalBounds != null)?originalBounds:bounds;
	}

	public void setOriginalBounds(Rectangle originalBounds) {
		this.originalBounds = originalBounds;
	}

}
