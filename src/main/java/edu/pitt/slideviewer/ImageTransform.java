package edu.pitt.slideviewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import edu.pitt.slideviewer.qview.connection.Utils;

/**
 * this class is a collection of values to conduct image transformations
 * 
 * @author tseytlin
 * 
 */
public class ImageTransform implements Serializable{
	private int rotate, flip;
	private float contrast   = 1.0f;
	private float brightness = 0.0f;
	private Dimension originalImageSize;
	private Rectangle crop;
	private Map<String,Color> originalChannelMap,channelMap;
	

	/**
	 * get a copy of this transformation
	 */
	public ImageTransform clone(){
		ImageTransform it = new ImageTransform();
		it.rotate = rotate;
		it.flip = flip;
		it.contrast = contrast;
		it.brightness = brightness;
		it.originalImageSize = originalImageSize;
		it.crop = crop;
		return it;
	}
	
	/**
	 * get contrast value
	 * @return
	 */
	public float getContrast() {
		return contrast;
	}

	/**
	 * set contrast value
	 * @param contrast
	 */
	public void setContrast(float contrast) {
		this.contrast = contrast;
	}


	/**
	 * get brightness value
	 * @return
	 */
	public float getBrightness() {
		return brightness;
	}


	/**
	 * set brighness value
	 * @param brightness
	 */
	public void setBrightness(float brightness) {
		this.brightness = brightness;
	}

	
	/**
	 * was the image transformed in any way?
	 * @return
	 */
	public boolean isIdentity(){
		return rotate == 0 && flip == 0 && contrast == 1.0 && brightness == 0.0;
	}
	
	/**
	 * reset transfomations to the original
	 */
	public void reset(){
		rotate = flip = 0;
		brightness = 0.0f;
		contrast = 1.0f;
		crop = null;
	}
	
	/**
	 * get rotation transform of current image
	 * 
	 * @return
	 */
	public int getRotationTransform() {
		return rotate;
	}

	/**
	 * rotate current image.
	 * 
	 * @param int rotate - quadrant rotation Ex: 1 = 90' 2 = 180', -1 = -90'  (270') ..
	 * @param rotationTransform
	 */
	public void setRotationTransform(int rotationTransform) {
		this.rotate = rotationTransform  % 4;
	}

	/**
	 * get flip transform
	 * @param int flip - flip > 0 = HORIZONTAL_FLIP, flip < 0 -> VERTICAL_FLIP,  0 no filp.
	 * @param flipTransform
	 */
	public int getFlipTransform() {
		return flip;
	}

	/**
	 * flip current image
	 * @param int flip - flip > 0 = HORIZONTAL_FLIP, flip < 0 -> VERTICAL_FLIP,  0 no filp.
	 * @param flipTransform
	 */
	public void setFlipTransform(int flipTransform) {
		this.flip = flipTransform;
	}

	
	/**
	 * get state of image transform as a property list
	 * @return
	 */
	public Properties getProperties(){
		Properties p = new Properties();
		p.setProperty("transform.flip",""+getFlipTransform());
		p.setProperty("transform.rotate",""+getRotationTransform());
		p.setProperty("transform.brightness",""+getBrightness());
		p.setProperty("transform.contrast",""+getContrast());
		if(isCropped())
			p.setProperty("transform.crop","["+crop.x+", "+crop.y+", "+crop.width+", "+crop.height+"]");
		return p;
	}
	
	/**
	 * set a state of image transform from a property list
	 * @return
	 */
	public void setProperties(Properties p){
		try{
			flip = Integer.parseInt(p.getProperty("transform.flip","0"));
			rotate = Integer.parseInt(p.getProperty("transform.rotate","0"));
			brightness = Float.parseFloat(p.getProperty("transform.brightness","0"));
			contrast = Float.parseFloat(p.getProperty("transform.contrast","1"));
			Rectangle r = ViewerHelper.parseRectangle(p.getProperty("transform.crop",""));
			if(r.x != 0 || r.y != 0)
				crop = r;
		}catch(NumberFormatException ex){}
	}
	
	
	/**
	 * get transofmed image based on transformations in this transorm
	 */
	public Image getTransformedImage(Image image) {
		Image target = Utils.getTransformedImage(image, rotate, flip);
		if(brightness != 0.0)
			target = applyBrightness(Utils.createBufferedImage(target),brightness);
		if(contrast != 1.0)
			target = applyContrast(Utils.createBufferedImage(target),contrast);
		
		// handle channels
		target=Utils.getTransformedImage(target,getOriginalChannelMap(),getChannelMap());
		
		return target;
		////return filterImage(Utils.createBufferedImage(target),new RescaleOp(contrast,brightness,null));
	}

	
	/**
	 * get transfomed point around given dimensions based on rotation and flipping values 
	 * @param img - point to be transformed
	 * @param iamgeToView - absolute image coordinates to transformed view coordinates if true
	 * @return
	 */
	public Point getTransformedPoint(Point img,  boolean imageToView){
    	Dimension d = getOriginalImageSize();
    	Point crop = getCropOffset();
     	img = new Point(img);
    	
		if(imageToView){
    		// compensate for transforms
			img = Utils.getFlippedPoint(img,d,flip);
	    	img = Utils.getRotatedPoint(img,d,rotate);
	    	
		  	// compoensate for crop
			img.x -= crop.x;
			img.y -= crop.y;
	  }else{
		  // compoensate for crop
			img.x += crop.x;
			img.y += crop.y;
	    	
			// compensate for transforms
		   	d = Utils.getRotatedDimension(d,rotate);
    		boolean even = rotate%2 == 0;
    		img = Utils.getFlippedPoint(img,d,(even)?flip:-flip);
    		img = Utils.getRotatedPoint(img,d,-rotate);
    	}
		
		return img;
	}
	
	
	/**
	 * get offset due to cropping, if no cropping offset is 0
	 * @return
	 */
	public Point getCropOffset(){
		if(crop != null){
			// copy crop rectangle
			Rectangle r = new Rectangle(crop);
			
			// rotate and flip with respect to rotated dimensions
			Dimension d =  Utils.getRotatedDimension(originalImageSize,rotate);
			r = Utils.getRotatedRectangle(r,d,rotate);
			r = Utils.getFlippedRectangle(r,d,(rotate %2 == 0)?flip:-flip);
			
			return r.getLocation();
		}
    	return new Point(0,0);
	}
	
	/**
	 * has the image been cropped
	 * @return
	 */
	public boolean isCropped(){
		return crop != null;
	}
	
	
	/**
	 * get rectangle from the original image, taking into account image transformations
	 * done in the parameter image transform
	 * @param r - rectangle that needs to be transferred
	 * @param imageToView - absolute image coordinates to transformed view coordinates if true
	 * @return
	 *
	private Rectangle getTransformedRectangle(Rectangle r, boolean imageToVew){
		Dimension d =  Utils.getRotatedDimension(originalImageSize,-rotate);
		Rectangle rect = Utils.getRotatedRectangle(r,d,(imageToVew)?rotate:-rotate);
		return Utils.getFlippedRectangle(rect,d,flip);
	}
	*/
	
	/**
	 * get transformed image size, that compensates for rotation as well as cropping
	 * @return
	 */
	public Dimension getTransformedImageSize(){
		if(crop != null){
			return Utils.getRotatedDimension(crop.getSize(),getRotationTransform());
		}else{
			return Utils.getRotatedDimension(getOriginalImageSize(),getRotationTransform());
		}
	}
	
	
	/**
	 * get cropped rectangle
	 * @return
	 */
	
	public Rectangle getCropRectangle() {
		return crop;
	}
	
	/**
	 * set cropped rectangle, set to null to clear cropping
	 * @return
	 */
	public void setCropRectangle(Rectangle r) {
		this.crop = r;
	}

	
	/**
	 * get original image dimensions (non-transformed)
	 * @return
	 */
	
	public Dimension getOriginalImageSize() {
		return originalImageSize;
	}
	
	/**
	 * set original image dimensions (non-transformed)
	 * @return
	 */
	public void setOriginalImageSize(Dimension originalImageSize) {
		this.originalImageSize = originalImageSize;
	}
	
	
	/**
	 * adjust brightness of the original image by
	 * 
	 * @param value
	 * @return
	 */
	public static BufferedImage applyBrightness(BufferedImage img, float value) {
		return filterImage(img,new RescaleOp(1.0f,value,null));
	}

	/**
	 * adjust brightness of the original image by
	 * 
	 * @param value
	 * @return
	 */
	public static BufferedImage applyContrast(BufferedImage img, float value) {
		return filterImage(img,new RescaleOp(value,0.0f,null));
	}
	
	
	/**
	 * get original channel map associated with multi-channel image
	 * @return
	 */
	public Map<String, Color> getOriginalChannelMap() {
		return originalChannelMap;
	}

	/**
	 * set original channel map associated with multi-channel image
	 * @return
	 */
	public void setOriginalChannelMap(Map<String, Color> originalChannelMap) {
		this.originalChannelMap = originalChannelMap;
	}

	/**
	 * get new channel map associated with multi-channel image
	 * @return
	 */
	public Map<String, Color> getChannelMap() {
		return channelMap;
	}

	/**
	 * set new channel map associated with multi-channel image
	 * @return
	 */
	public void setChannelMap(Map<String, Color> channelMap) {
		this.channelMap = channelMap;
	}

	/**
	 * histogram equalizes an image from
	 * 
	 * @author Tim Sharman
	 * @version July 1999
	 * @see code.iface.equalize
	 */
	public static BufferedImage applyHistogram(BufferedImage bi) {
		// width and height of the image
		int i_w = bi.getWidth();
		int i_h = bi.getHeight();
		// pixel arrays for input image and destination image
		int[] src_1d, dest_1d, nArray; // equalized image data
		int result, pix, src_rgb;
		;
		float tmp, tmp2, tmp3;
		src_1d = new int[i_w * i_h];
		dest_1d = new int[i_w * i_h];
		nArray = new int[256];
		bi.getRGB(0, 0, i_w, i_h, src_1d, 0, i_w);
		// The array of values corresponding to grey level frequencies
		pix = i_w * i_h;
		// Initialise the n array
		for (int i = 0; i < 256; i++) {
			nArray[i] = 0;
		}
		// Create the n array
		for (int i = 0; i < src_1d.length; i++) {
			src_rgb = src_1d[i] & 0x000000ff;
			nArray[src_rgb]++;
		}
		// Now calculate the new intensity values
		for (int i = 0; i < src_1d.length; i++) {
			src_rgb = src_1d[i] & 0x000000ff;
			tmp3 = 0;
			for (int j = 0; j < (src_rgb + 1); j++) {
				tmp = (float) nArray[j];
				tmp2 = (float) pix;
				tmp3 = tmp3 + (tmp / tmp2);
			}
			result = (int) (tmp3 * 255);
			if (result > 255) {
				result = 255;
			}
			if (result < 0) {
				result = 0;
			}
			dest_1d[i] = 0xff000000 | (result + (result << 16) + (result << 8));
		}
		bi.setRGB(0, 0, i_w, i_h, dest_1d, 0, i_w);
		return bi;
	}

	/**
	 * apply detect edges algorithm to the image
	 * 
	 * @param img
	 * @return
	 */
	public static BufferedImage applyDetectEdges(BufferedImage img) {
		float[] elements = { 0.0f, -1.0f, 0.0f, -1.0f, 4.0f, -1.0f, 0.0f, -1.0f, 0.0f };
		return convolveImage(img, 3, elements);
	}

	/**
	 * get sharpend image
	 */
	public static BufferedImage applySharpen(BufferedImage img) {
		float[] elements = { 0.0f, -1.0f, 0.0f, -1.0f, 5.0f, -1.0f, 0.0f, -1.0f, 0.0f };
		return convolveImage(img, 3, elements);
	}

	/**
	 * blur image
	 * 
	 * @param bi
	 * @return
	 */
	public static BufferedImage applyBlur(BufferedImage bi) {
		float[] elements = new float[9];
		float weight = 1.0f / 9.0f;
		for (int i = 0; i < 9; i++)
			elements[i] = weight;
		return convolveImage(bi, 3, elements);
	}

	/**
	 * filter image
	 * 
	 * @param bi
	 * @param op
	 * @return
	 */
	private static BufferedImage filterImage(BufferedImage bi, BufferedImageOp op) {
		if(bi == null)
			return null;
		BufferedImage fi = new BufferedImage(bi.getWidth(), bi.getHeight(), bi.getType());
		op.filter(bi, fi);
		return fi;
	}

	/**
	 * convolve image to a matrix
	 * 
	 * @param bi
	 * @param size
	 * @param elements
	 * @return
	 */
	private static BufferedImage convolveImage(BufferedImage bi, int size, float[] elements) {
		Kernel kernal = new Kernel(size, size, elements);
		ConvolveOp op = new ConvolveOp(kernal);
		return filterImage(bi, op);
	}
	
}
