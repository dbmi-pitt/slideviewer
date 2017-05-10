package edu.pitt.slideviewer;

import java.util.*;
import java.awt.*;
import java.awt.image.*;

/**
 * This class describes misc metadata about this image, one can
 * use it to get general information about image w/ dimensions, 
 * thumbnails etc...
 * @author tseytlin
 */
public class ImageProperties implements java.io.Serializable {
    protected String name,imagePath,source;
    protected Dimension imageSize,tileSize;
    protected double pixelSize;
    protected double [] scales;
    protected Properties properties;
    protected Image thumbnail, label, macro;
    protected ImageTransform imageTransform;
    protected Map<String,Color> channelMap;
  
	/**
     * Does the image have vertical orientation?
     * @return
     */
    public boolean isVertical(){
    	if(imageSize != null){
    		return imageSize.width <= imageSize.height;
    	}else
    		return false;
    }
    
    /**
     * Does the image have horizontal orientation?
     * @return
     */
    public boolean isHorizontal(){
    	if(imageSize != null){
    		return imageSize.width >= imageSize.height;
    	}else
    		return false;
    }
    
    
	/**
	 * @return the imageSize
	 */
	public Dimension getImageSize() {
		return imageSize;
	}
	/**
	 * @param imageSize the imageSize to set
	 */
	public void setImageSize(Dimension imageSize) {
		this.imageSize = imageSize;
	}
	
	/**
	 * @return the label
	 */
	public Image getLabel() {
		if(label == null){
			label = new BufferedImage(150,150,BufferedImage.TYPE_INT_RGB);
			Graphics2D g = (Graphics2D) label.getGraphics();
			g.setColor(Color.white);
			g.fillRect(0,0,label.getWidth(null),label.getHeight(null));
			g.setColor(Color.GRAY);
			g.setStroke(new BasicStroke(3));
			g.drawLine(0,0,150,150);
			g.drawLine(0,150,150,0);
			g.drawRect(0,0,149,149);
			//g.drawString("label not available",5,75);
		}
		return label;
	}
	/**
	 * @param label the label to set
	 */
	public void setLabel(Image label) {
		this.label = label;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	
	/**
	 * @return the name
	 */
	public String getImagePath() {
		return (imagePath == null)?getName():imagePath;
	}
	
	
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @param name the name to set
	 */
	public void setImagePath(String name) {
		this.imagePath = name;
	}
	
	/**
	 * @return the pixelSize
	 */
	public double getPixelSize() {
		return pixelSize;
	}
	/**
	 * @param pixelSize the pixelSize to set
	 */
	public void setPixelSize(double pixelSize) {
		this.pixelSize = pixelSize;
	}
	/**
	 * @return the properties
	 */
	public Properties getProperties() {
		if(properties == null)
			properties = new Properties();
		return properties;
	}
	/**
	 * @param properties the properties to set
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}
	/**
	 * list of scales at which image can be requested
	 * ex: [1.0, 0.5, 0.25, 0.125, 0.625, 0.3125, 0.15625, 0]
	 * scale factor of 0 represents minimal zoom (thumbnail)
	 * @return the scales
	 */
	public double[] getScales() {
		return scales;
	}
	/**
	 * @param scales the scales to set
	 */
	public void setScales(double[] scales) {
		this.scales = scales;
	}
	/**
	 * @return the thumbnail
	 */
	public Image getThumbnail() {
		return thumbnail;
	}
	/**
	 * @param thumbnail the thumbnail to set
	 */
	public void setThumbnail(Image thumbnail) {
		this.thumbnail = thumbnail;
	}
	/**
	 * @return the tileSize
	 */
	public Dimension getTileSize() {
		return tileSize;
	}
	/**
	 * @param tileSize the tileSize to set
	 */
	public void setTileSize(Dimension tileSize) {
		this.tileSize = tileSize;
	}
	
	/**
	 * is image multi-channel? 
	 * @return true - if multichannel, false otherwise
	 */
	public boolean isMultiChannel(){
		return channelMap != null;
	}
	
	/**
	 * get original channel map associated with multi-channel image
	 * @return
	 */
	public Map<String, Color> getChannelMap() {
		return channelMap;
	}

	/**
	 * set original channel map associated with multi-channel image
	 * @return
	 */
	public void setChannelMap(Map<String, Color> channelMap) {
		this.channelMap = channelMap;
	}

	/**
	 * get the source of the image
	 * @return
	 */
	public String getSource(){
		if(source == null){
			if(name.endsWith(".vms") || name.endsWith(".ndpi"))
				source = "Hamamatsu NanoZoomer";
			else if(name.endsWith(".svs") || name.startsWith("AP"))
				source = "Aperio ScanScope";
			else if(name.endsWith(".tif"))
				source = "InterScope";
			else if(name.endsWith(".mrxs"))
				source = "Mirax Scan";
			else if(name.endsWith(".scn"))
				source = "Leica Scan Station";
			else
				source = "Unknown";
		}
		return source;
	}
	
	/**
	 * set the source of the image
	 * @return
	 */
	public void setSource(String s){
		this.source = s;
	}
	
	/**
	 * return string representation of an image
	 */
	public String toString(){
		return getName();
	}
	
	/**
	 * Pretty print representation as html
	 * @return
	 */
	public String getHTMLDescription(){
		Dimension d = getImageSize();
		Dimension t = getTileSize();
		// init some strings
		String ds = (d != null)?d.width+" x "+d.height:"none";
		String ts = (t != null)?t.width+" x "+t.height:"none";
		ArrayList<String> slist = new ArrayList<String>();
		double [] scales = getScales();
		if(scales != null){
			for(int i=0;i<scales.length;i++)
				slist.add(""+Math.round(scales[i]*100));
		}
		StringBuffer s = new StringBuffer();
		s.append("<html><table border=0 width=400>");
		s.append("<tr><td>image name:</td><td>"+getName()+"</td></tr>");
		s.append("<tr><td>image vendor:</td><td>"+getSource()+"</td></tr>");
		s.append("<tr><td>image size:</td><td>"+ds+"</td></tr>");
		s.append("<tr><td>tile size:</td><td>"+ts+"</td></tr>");
		s.append("<tr><td>pixel size:</td><td>"+getPixelSize()+" mm</td></tr>");
		s.append("<tr><td>zoom levels:</td><td>"+slist+"</td></tr></table>");
		return s.toString();
	}

	/**
	 * get macro image of a slide
	 * @return
	 */
	public Image getMacroImage() {
		return macro;
	}

	/**
	 * set macro image of a slide
	 * @param macro
	 */
	public void setMacroImage(Image macro) {
		this.macro = macro;
	}
	
	
	/**
	 * get image transform associated with this image
	 * @return transoform, if there was not transformation
	 * returns empty transform
	 */
    public ImageTransform getImageTransform() {
		if(imageTransform == null){
			imageTransform = new ImageTransform();
			imageTransform.setOriginalImageSize(imageSize);
			imageTransform.setOriginalChannelMap(getChannelMap());
		}
    	return imageTransform;
	}

    
    /**
     * set image transform associated with this image
     * @param imageTransform
     */
	public void setImageTransform(ImageTransform imageTransform) {
		this.imageTransform = imageTransform;
		this.imageTransform.setOriginalImageSize(imageSize);
		this.imageTransform.setOriginalChannelMap(getChannelMap());
	}

}
