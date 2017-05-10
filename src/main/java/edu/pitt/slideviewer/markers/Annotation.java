package edu.pitt.slideviewer.markers;

//Comment out for web course
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.beans.*;
import java.util.*;

import javax.swing.JColorChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import edu.pitt.slideviewer.*;
import edu.pitt.slideviewer.qview.connection.Utils;

//import com.imagepump.applets.gui.ImageView;

/**
 * Abstract parent class for all annotation markers.
 */

public abstract class Annotation implements ActionListener {
	public static final String EDIT_ICON = "/icons/Edit16.gif";
	public static final String COLOR_ICON = "/icons/Color16.png";
	public static final String DELETE_ICON = "/icons/Delete16.gif";
	
	protected final int LINE_THICKNESS_DEFAULT = 2;  // default line thickness
	
	protected List markers;  // List of markers for moving/resizing shape
	protected Viewer viewer = null;       // needed for drawing
	protected PropertyChangeSupport pcs;  // notification support
	protected boolean isMovable = false;  // are shapes movable/resizable
	protected boolean selected = false;   // is shape selected
	protected boolean tagVisible = false; // display tag as tooltip 
	
	protected String type;  // shape type
	protected String name;  // unique (hopefully) shape name
	//protected String tag = ""; //tag
	protected Set<String> tags;
	protected String image; // name of the image
	
	protected Color selectionColor = Color.yellow;  // Selection color
	protected Color color = Color.blue;  // Current color of this shape.
	protected Color preferredColor = color; // color selected by user
	protected Cursor cursor;
	protected JPopupMenu contextMenu;
	// this is the shape dimensions in image coodrinates
	protected Rectangle imageRect;
	// this is the viewer position, that shape is best viewed at (maybe)
	protected ViewPosition viewPosition;
	// is marker visible
    protected boolean visible = true, showmarkers = true, editable = false; 
    
	//protected int min_width = 30;
	//protected int min_height = 30;
	//protected Image background_img = null;
		
	// flags
	//protected boolean inDrag = false;

	
	/**
	 * Default contructor
	 */
	public Annotation() {
        this(null,Color.blue,false);
        //this(new Rectangle(10,10,10,10),Color.blue,false);	
	}
	
	/**
	 * Create tutor marker with initial bounds and color
	 * @param Rectangle - bounds of this shape
	 * @param Color - color of this shape
	 */		
	public Annotation(Rectangle r, Color c) {
		this(r,c,false);	
	}
	
	/**
	 * Create tutor marker with initial bounds, color, and ability to 
	 * modify position/size of this shape
	 * @param Rectangle - bounds of this shape
	 * @param Color - color of this shape
	 * @param boolean - can this shape be moved/resizded?
	 */	
	public Annotation(Rectangle r,Color c, boolean hasMarks) {
		markers = new ArrayList();
		pcs = new PropertyChangeSupport(this);
		setBounds(r);
		setColor(c);
		setMovable(hasMarks);
	}
	
	/**
	 * Add property change listener to this shape.
	 */
	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		pcs.addPropertyChangeListener(pcl);
	}
	
	/**
	 * Remove property change listener to this shape.
	 */
	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		pcs.removePropertyChangeListener(pcl);
	}

		
	/**
	 * Sends the bounds back to listeners through the listener.
	 * Property name is "UpdateShape".
	 */
	public void notifyBoundsChange() {
		pcs.firePropertyChange(Constants.UPDATE_SHAPE,null,this);
	}

	/**
	 * Set viewer for this Annotation.
	 * @param tiv
	 */
	public void setViewer(Viewer tiv) {
		viewer = tiv;
	}
	
	/**
	 * Get viewer for this Annotation.
	 * @return Viewer tiv
	 */
	public Viewer getViewer() {
		return viewer;
	}
	
	
	/**
	 * Get bounds of this Annotation in absolute image coordinates
	 * @return Rectangel image bounds of shape
	 */
	public Rectangle getBounds(){
		return imageRect;
	}
	
	/**
	 * Set bounds of this Annotation in absolute image coordinates
	 * @param Rectangel image bounds of shape
	 */
	public void setBounds(Rectangle r){
		// now set it
		if(imageRect == null)
        	imageRect = r;
        else
        	imageRect.setBounds(r);
	}
	
	/**
	 * Get bounds of this Annotation in relative view coordinates.
	 * This should be only used for drawing shapes.
	 * @return Rectangel image bounds of shape
	 */
	public Rectangle getRelativeBounds(){
		Point p = getRelativeLocation();
		Point e = getRelativeEndPoint();
		
		int width  = e.x - p.x;
		int height = e.y - p.y;
		Rectangle r = new Rectangle(p.x,p.y,width,height);
		
		return r;
	}
	
	/**
	 * Get location of Annotation in relative view coordinates
	 * @return Point shape location
	 */
	public Point getRelativeLocation(){
        Rectangle r = getBounds();
        if(r != null && viewer != null){
            //r = new Rectangle(Utils.correctRectangle(r));
        	return viewer.convertImageToView(r.getLocation());
        }else 
            return null;
	}
	
	/**
	 * Get center view position to view this shape
	 * @return ViewPosition - x,y and zoom coordinates to best view this shape
	 */
	public ViewPosition getCenterPosition(){
		Rectangle r = getBounds();
		int x = (int) r.getCenterX();
		int y = (int) r.getCenterY();
		double z = viewPosition.scale;
		if(viewer != null && viewer.getScalePolicy() != null)
			z = viewer.getScalePolicy().getValidScale(z);
		return new ViewPosition(x,y,z);
	}
	
	
	/**
	 * Get the best viewer position to view this shape.
	 * @return ViewPosition - x,y and zoom coordinates to best view this shape
	 */
	public ViewPosition getViewPosition(){
		return viewPosition;
	}
	
	/**
	 * Set the best viewer position to view this shape.
	 * @param ViewPosition - x,y and zoom coordinates to best view this shape
	 */
	public void setViewPosition(ViewPosition v){
		viewPosition = v;
		//try to adjust the view somewhat
		if(viewer != null ){
			ScalePolicy policy = viewer.getScalePolicy();
			double s = viewPosition.scale;
			double fs = (policy != null)?policy.getValidScale(s):s;
			
			// correct scale if it is not far off the mark
			if(Math.abs(s-fs) < 0.03)
				viewPosition.scale = fs;
		}
	}
	
	/**
	 * Get location of Annotation in absolute image coordinates
	 * @return Point shape location
	 */
	public Point getLocation(){
        Rectangle r = getBounds();
        if(r != null)
            return r.getLocation();
        else 
            return null;
	}
	
	/**
	 * Set location of Annotation in absolute image coordinates
	 * @param Point shape location
	 */
	public void setLocation(Point p){
        Rectangle r = getBounds();
        if(r != null)
            r.setLocation(p);
        else
            setBounds(new Rectangle(p,new Dimension(10,10)));
	}
	
	
	/**
	 * Get end point of Annotation in absolute image coordinates.
	 * This is mainly a legacy method for XEnd, YEnd data points which
	 * only make sence for arrows and ruler shapes (which overwrite this method anyway)
	 * @return Point shape location
	 */
	public Point getEndPoint(){
        Rectangle r = getBounds();
        if(r != null)
            return new Point(r.x+r.width,r.y+r.height);	 
        else
            return null;
    }
	
		
	/**
	 * Get end point in relative view coordinates. Used internaly.
	 */	
	public Point getRelativeEndPoint(){
		return (viewer != null)?viewer.convertImageToView(getEndPoint()):new Point(0,0);
	}
	
	/**
	 * Get end point in relative view coordinates. Used internaly.
	 */	
	public void setRelativeLocation(Point p){
        if(viewer == null)
        	return;
		Rectangle r = getBounds();
        if(r != null)
            r.setLocation(viewer.convertViewToImage(p));
        else
            setBounds(new Rectangle(viewer.convertViewToImage(p),new Dimension(10,10)));
	}
	
	
	
	/**
	 * Get primary Color of this shape
	 * @return Color color
	 */
	public Color getColor() {
		return preferredColor;
	}
	
	/**
	 * Set the color of this shape
	 * @param color - A color to be set.
	 */
	public void setColor(Color color) {
		this.color = color;
		this.preferredColor = color;
	}
	
	/**
	 * Get name of this shape
	 * @return String name
	 */	
	public String getName() {
		return name;
	}
	
	/**
	 * Set unique name for this shape
	 * @param String name
	 */	
	public void setName(String n) {
		name = n;
	}
	
	/**
	 * Set a unique tag that is associated with the shape
	 * @param String tag name
	 */	
	public void setTag(String t) {
		//tag = t;
		tags = new LinkedHashSet<String>();
		Collections.addAll(tags,t.split("\\s*,\\s*"));
	}
	
	/**
	 * Set a unique tag that is associated with the shape
	 * @param String tag name
	 */	
	public void setTags(Set<String> t) {
		tags = t;
	}
	
	/**
	 * add a tag to an annotation
	 * @param t
	 */
	public void addTag(String t){
		/*
		if(tag == null || tag.length() == 0){
			setTag(t);
		}else if(tag.indexOf(t) < 0){
			tag = tag+", "+t;
			setTag(tag);
		}
		*/
		getTags().add(t);
	}
	
	/**
	 * remove a tag from an annotation
	 * @param t
	 */
	public void removeTag(String t){
		/*
		tag = tag.replaceAll(t,"").replaceAll(",\\s?,",",");
		tag = tag.trim();
		if(tag.startsWith(","))
			tag = tag.substring(1).trim();
		if(tag.endsWith(","))
			tag = tag.substring(0,tag.length()-1).trim();
		setTag(tag);
		*/
		getTags().remove(t);
	}
	
	/**
	 * Does annotation have a tag
	 * @return
	 */
	public boolean hasTag(){
		return getTags().size() > 0;
	}
	
	
	/**
	 * Get a unique tag that is associated with the shape
	 * @return String tag name
	 */		
	public String getTag() {
		String s = getTags().toString();
		// remove paranthesis
		return s.substring(1,s.length()-1);
	}
	
	/**
	 * Get a unique tag that is associated with the shape
	 * @return String tag name
	 */		
	public Set<String> getTags() {
		if(tags == null)
			tags = new LinkedHashSet<String>();
		return tags;
	}
	
	/**
	 * Type of shape that this is. Usualy set by child class
	 * @param String name of the type
	 */
	public void setType(String t) {
		type = t;
	}

	/**
	 * Get string representation of this shape type/
	 * @return String type
	 */	
	public String getType() {
		return type;
	}

	/**
	 * Can this marker be movde by user?
	 * @param boolean true if it can be moved
	 */
	public void setMovable(boolean b) {
		isMovable = b;
		if(b)
			addMarkers();
		else
			removeMarkers();
	}
	
	/**
	 * Can this marker be moved by user?
	 * @param boolean true if it can
	 */
	public boolean isMovable(){
		return isMovable;	 
	}
    
    /**
     * Is this shape resizable
     * @return
     */
    public boolean isResizable(){
        return true;
    }
	
	/**
	  * clear all the references before a class destruction
	  */
	public void delete() {
		markers.clear();
		viewer = null;
		//pcs = null;
	}
	
	/**
	 * Add a vertex of a shape. Uses absolute image coordinates
	 * Overritten in PolygonShape class (x,y) are view coordinates
	 * @param int x
	 * @param int y
	 */
	public void addPoint(int x, int y) {}

	/**
	 * Add a vertex of a shape. Uses relative view coordinates.
	 * This method is for convinience, since your are more likely to have view coordinates.
	 * Overritten in PolygonShape class (x,y) are view coordinates
	 * @param int x
	 * @param int y
	 */
	public void addRelativePoint(int x, int y) {}
	
	
	/**
	 * Add markers to the shape. Those little black squares that 
	 * make shape movable on the slide. This method needs to be overwritten by
	 * classes that wish to add markers
	 */	
	protected void addMarkers() {}
	/**
	 * Remove all markers from shape. Those little black squares that 
	 * make shape movable on the slide
	 */		
	protected void removeMarkers() {
		markers.clear();
	}
	/**
	 * Get list of markers.
	 * @return List list of markers.
	 */	
	public List getMarkers() {
		return markers;
	}
	
	/**
	 * Get marker at x,y position in absolute image coordinates (maybe)
	 * @param int x - x position
	 * @param int y - y position
	 * @return Marker - marker at that x/y position, 
	 *          or null if marker was not found at this location
	 */
	public Marker getMarkerAt(int x, int y) {
		// no point in looking if shape is not visible
        if(!inView())
           return null;
        // look for marker
        for(int i=0; i<markers.size(); i++) {
			Marker m = (Marker)markers.get(i);
			if(m.inside(x,y))
				return m;
		}
		return null;
	}

	/**
	 * Draws shape as well as markers on top of it.
	 * @param Graphics2D - graphics on which to draw on
	 */	
	public void draw(Graphics g) {
        if(isVisible()){
    		drawShape(g);
    		// draw markers
    		if(!markers.isEmpty() && showmarkers)
    			for(int i=0; i<markers.size();i++)
    				((Marker)markers.get(i)).draw(g);
        }
	}
	
    /**
     * Set marker to be visible/invisible
     */
    public void setVisible(boolean b){
        visible = b;
    }
    
    /**
     * Is marker visible
     * @return
     */
    public boolean isVisible(){
        return visible && getBounds() != null;
    }
    
	/**
	 * This method is responsible for actually drawing this shape.
	 * Should be overwritten by class implementations.
	 * @param Graphics2D - graphics on which to draw on
	 */	
	public abstract void drawShape(Graphics g);

	/**
	 * Get string representation of Annotation (usualy name)
	 */ 	
	public String toString() {
		//return name;
		Rectangle r = getBounds();
        if(r != null)
            return ("x="+r.x+" y="+r.y+" w="+r.width+" h="+r.height);
        else
            return name;
	}

	
	/**
	 * Determine whether point is inside tutor marker.
	 * It uses absolute (image) coordinate system
	 * @param int x - x coordinate (absolute image coordinate system)
	 * @param int y - y coordinate (absolute image coordinate system)
	 * @return boolean true if point is contained within shape
	 */
	public boolean contains(int x, int y){
		return false;	
	}
		
	/**
	 * Resets the color of this shape
	 * @param b boolean isSelected
	 */
	public void setSelected(boolean selected) {
		this.selected = selected;
		color = (selected)?selectionColor:preferredColor;
	
		// repaint viewer
		if(viewer != null)
			viewer.repaint();
	}
	
	/**
	 * Is shape currently selected?
	 * @param boolean - true if selected
	 */	
	public boolean isSelected() {
		return selected;
	}
    
	
    /**
     * Get Shape representation of the Annotation.
     * By default it returns bounding box of tutor marker,
     * but can be overwritten to return actualy Polygon for example
     * @return
     */
    public Shape getShape(){
        return getBounds();
    }
    
    
    /**
     * Is this Annotation currently in viewer window?
     * @return
     */
    public boolean inView(){
       Rectangle r = getBounds();
        if(r != null && viewer != null){
        	if(r.width == 0)  r.width = 1;
        	if(r.height == 0) r.height =1;
        	
        	// compensate for transforms
        	Rectangle rect = Utils.getTransformedRectangle(viewer.getViewRectangle(),viewer.getImageProperties());
        	
        	return rect.contains(r) || rect.intersects(r);
                   
        }else
            return false;
    }
    
    /**
     * Transform shape based on AffineTransform
     * So far implimented only by PolygonShape
     * @param AffineTransform af
     */
    public void transform(AffineTransform af){}

    /**
     * move shape by dx/dy
     * @param dx
     * @param dy
     */
    public void translate(int dx, int dy){
    	getBounds().translate(dx,dy);
    }
    
    /**
     * @param showmarkers The showmarkers to set.
     */
    public void setShowMarkers(boolean showmarkers) {
        this.showmarkers = showmarkers;
    }
    
    /**
     * Is Annotation a type of polygon
     * @return
     */
    public boolean isPolygon(){
    	return (getShape() instanceof Polygon);
    }
    
    
    /**
     * get string representation of vertecies
     * @return String [2] array, where String[0] is X and String[1] is Y
     */
    public String [] getVertices(){
    	if(isPolygon()){
    		Polygon poly = (Polygon) getShape();
    		StringBuffer x_str = new StringBuffer();
    		StringBuffer y_str = new StringBuffer();
    		for(int i=0;i<poly.npoints;i++){
    			x_str.append(poly.xpoints[i]+" ");
    			y_str.append(poly.ypoints[i]+" ");
    		}
    		return new String [] {(""+x_str).trim(),(""+y_str).trim()};
    	}
    	return null;
    }
    
    /**
     * get string representation of vertecies
     * @return String [2] array, where String[0] is X and String[1] is Y
     */
    public void setVertices(String x_str, String y_str){
    	if(isPolygon()){
    		String [] xs = x_str.split("[^\\d]+");
    		String [] ys = y_str.split("[^\\d]+");
    		if(xs.length == ys.length){
    			((Polygon) getShape()).reset();
    			for(int i=0;i<xs.length;i++){
    				try{
    					int x = Integer.parseInt(xs[i]);
    					int y = Integer.parseInt(ys[i]);
    					addVertex(x,y);
    				}catch(NumberFormatException ex){}
    			}
    		}
    	}
    }
    
    /**
     * A way to add a vertex to polygon
     * @param x
     * @param y
     */
    public void addVertex(int x, int y){
    	if(isPolygon()){
    		((Polygon)getShape()).addPoint(x,y);
    	}
    }

	/**
	 * @return the cursor
	 */
	public Cursor getCursor() {
		return cursor;
	}

	/**
	 * @param cursor the cursor to set
	 */
	public void setCursor(Cursor cursor) {
		this.cursor = cursor;
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

	/**
	 * @return the tagVisible
	 */
	public boolean isTagVisible() {
		return tagVisible;
	}

	/**
	 * @param tagVisible the tagVisible to set
	 */
	public void setTagVisible(boolean tagVisible) {
		this.tagVisible = tagVisible;
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}
	
	/**
	 * get context menu for this annotation
	 * @return
	 */
	public JPopupMenu getContextMenu(){
		if(contextMenu == null){
			contextMenu = new JPopupMenu("Edit Annotation");
			
			// add options
			contextMenu.add(ViewerHelper.createMenuItem("Edit Tags","Add/Remove Annotation Tags",EDIT_ICON,this));
			contextMenu.add(ViewerHelper.createMenuItem("Edit Color","Change Annotation Color",COLOR_ICON,this));
			contextMenu.addSeparator();
			contextMenu.add(ViewerHelper.createMenuItem("Delete","Delete Annotation",DELETE_ICON,this));
		}
		return contextMenu;
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if("Edit Tags".equals(cmd) && viewer != null){
			ViewerHelper.MultipleEntryPanel panel = new ViewerHelper.MultipleEntryPanel("Edit a List of Tags");
			panel.setEntries(getTags());
			int r = JOptionPane.showConfirmDialog(viewer.getViewerComponent(),panel,
					"Edit Tags",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
			if(JOptionPane.OK_OPTION == r){
				setTags(new LinkedHashSet<String>(panel.getEntries()));
				pcs.firePropertyChange(Constants.UPDATE_SHAPE_TAGS,null,this);
			}
		}else if("Edit Color".equals(cmd) && viewer != null){
			Color clr = JColorChooser.showDialog(viewer.getViewerComponent(),"Annotation Color",getColor());
			if(clr != null){
				setColor(clr);
				viewer.repaint();
				pcs.firePropertyChange(Constants.UPDATE_SHAPE_COLOR,null,this);
			}
		}else if("Delete".equals(cmd) && viewer != null){
			AnnotationManager manager = viewer.getAnnotationManager();
			manager.removeAnnotation(this);
			pcs.firePropertyChange(Constants.DELETE_SHAPE,null,this);
		}
	}
	
	
	/**
	 * update bounds if you have rectangles with negative sides
	 */
	public void updateBounds(){
		setBounds(Utils.correctRectangle(getBounds()));
	}
	
}  

