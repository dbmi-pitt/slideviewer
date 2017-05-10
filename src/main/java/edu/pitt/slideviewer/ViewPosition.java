package edu.pitt.slideviewer;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;

/**
 * This is a simple container for x/y coordinates and zoom level.
 * It has been extended to contain more data s.a. width, height
 * and the image name
 */
public class ViewPosition implements Serializable {
	public int x,y,width,height;
	public double scale;
	private String image;
	
	/**
	 * Create viewer location container
	 */	
	public ViewPosition(int x,int y,double scale){
		this(x,y,0,0,scale);
	}
	
	/**
	 * Create viewer location container
	 */	
	public ViewPosition(Point p,double scale){
		this(p.x,p.y,scale);
	}
	
	/**
	 * Create viewer location container
	 */	
	public ViewPosition(Point p,Dimension d, double scale){
		this(p.x,p.y,d.width,d.height,scale);
	}
	
	/**
	 * Create viewer location container
	 */	
	public ViewPosition(Rectangle r, double scale){
		this(r.x,r.y,r.width,r.height,scale);
	}
	
	/**
	 * Create viewer location container
	 */	
	public ViewPosition(Rectangle r, double scale,String image){
		this(r.x,r.y,r.width,r.height,scale,image);
	}
	
	
	/**
	 * Create viewer location container
	 */	
	public ViewPosition(int x,int y,int w, int h, double scale){
		this(x,y,w,h,scale,null);
	}
	
	/**
	 * Create viewer location container
	 */	
	public ViewPosition(int x,int y,int w, int h, double scale, String image){
		this.x = x;
		this.y = y;
		this.width = w;
		this.height = h;
		this.scale = scale;
		this.image = image;
	}
	
	
	
	/**
	 * Get Point from this container.
	 */
	public Point getPoint(){
		return new Point(x,y);	 
	}
	
	/**
	 * get rectangle from this container
	 * @return
	 */
	public Rectangle getRectangle(){
		return new Rectangle(x,y,width,height);
	}
	
	/**
	 * get dimensions of the position
	 * @return
	 */
	public Dimension getSize(){
		return new Dimension(width,height);
	}
	
	/**
	 * get dimensions of the position
	 * @return
	 */
	public Dimension getRelativeSize(){
		return new Dimension((int)(width*scale),(int)(height*scale));
	}
	
	
	
	/**
	 * Pretty print this location
	 */
	public String toString(){
		return "ViewPosition: ("+x+","+y+") scale = "+Math.round(scale*100)/100.0;
	}
	
	/**
	 * compare viewer locations
	 */
	public boolean equals(Object obj){
		if(obj instanceof ViewPosition){
			ViewPosition v = (ViewPosition) obj;
			return x == v.x && y == v.y && scale == v.scale && width == v.width && height == v.height;
		}else
			return false;
	}
	
	/**
	 * need to implement since equals is overridden
	 */	
	public int hashCode(){
		return (""+x+""+y+""+scale).hashCode();
	}

	/**
	 * @return the height
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * @param height the height to set
	 */
	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * @return the width
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @param width the width to set
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * @return the image
	 */
	public String getImage() {
		return image;
	}

	/**
	 * @param image the image to set
	 */
	public void setImage(String image) {
		this.image = image;
	}
	
}
