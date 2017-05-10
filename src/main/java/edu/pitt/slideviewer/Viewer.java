package edu.pitt.slideviewer;

import java.awt.Image;
import java.beans.PropertyChangeListener;
import java.util.Properties;
import java.net.URL;
import java.awt.*;

/**
 * This interface describes common viewer behaviour. 
 * To get an instance of Viewer you need to use ViewerFactory  
 * @author tseytlin
 */

public interface Viewer {
	public final int IMG_NO_MARKERS       = 0;
	public final int IMG_ALL_MARKERS      = 1;
	public final int IMG_SELECTED_MARKERS = 2;
	
	/**
	 * Get server URL
	 * @return URL
	 */
	public URL getServer();
	
    
    /**
     * Set server URL
     */
    public void setServer(URL url);
    
	/**
	 * Return an instance of main annotation panel 
	 * that is attached to this viewer
	 */
	public AnnotationPanel getAnnotationPanel();
	
	/**
	 * Convinent method to get AnnotationManager
	 * same thing as getAnnotationPanel().getMarkerManager()
	 * @return
	 */
	public AnnotationManager getAnnotationManager();
	
	/**
	 * Adds a listener to monitor events from the viewer.
	 * Supported properties are: 
     * 'ViewChange','ViewObserve','ImageChange','sketchDone','
     * 'Navigator',ViewReset'
	 * @param PropertyChangeListener listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener);
	
	/**
	 * Send out property change event.
	 * @param String property name
	 * @param Object old value
	 * @param Object new value
	 */
	public void firePropertyChange(String p,Object a,Object b);
	
	
	/**
	 * Remove a listener that monitor events from the viewer.
	 * Supported properties are: 'ViewChange'
	 * @param PropertyChangeListener listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener);
	
    /**
     * Get list of all change listeners
     * @return All listeners
     */
    public PropertyChangeListener[] getPropertyChangeListeners(); 
    
    
	/**
	 * Set size of viwer component. 
	 * Calls setPrefferedSize on viewer component.
	 * @param Dimension new size of viewer
	 */
	public void setSize(Dimension d);
	
	/**
	 * Get size of viwer component. 
	 * Calls getPrefferedSize on viewer component.
	 * @return Dimension new size of viewer
	 */
	public Dimension getSize();
	
	/**
	 * Set size of viwer component.
	 * Calls setPrefferedSize on viewer component.
	 * @param int with
	 * @param int height
	 */
	public void setSize(int w, int h);


	/**
	 * Create panel that has both viewer and control panel
	 * @return JPanel viewer.
	 */	
	//public Container createViewerPanel();
	
    /**
     * Get panel that has both viewer and control panel
     * @return JPanel viewer.
     */ 
    public Container getViewerPanel();
    
	/**
	 * Get viewing component. This is the viewer itself.
	 * @return Container viewing component.
	 */	
	public Component getViewerComponent();
	
	/**
	 * Get viewer control panel. This is the panel that controls the viewer.
	 * @return ViewerControlPanel viewer controller.
	 */	
	public ViewerControlPanel getViewerControlPanel();
	
	/**
	 * Load slide image into the viewer.
	 * @param String - name of the slide (might require full path).
	 */		
	public void openImage(String name) throws ViewerException;
	
	
	/**
	 * Get the name/path of the currently loaded image
	 * @return String image name
	 */	
	public String getImage();
	
    /**
     * Check if there is a loaded slide
     * @return boolean true if image is loaded
     */ 
    public boolean hasImage();
    
    
	/**
	 * Get the size of the current image in absolute coordinates
	 * @return Dimension size of the image
	 */
	public Dimension getImageSize();
	
	/**
	 * Get Image Navigator. Little thumbnail window with slide map
	 * @return Navigator
	 */	
	public Navigator getNavigator();
	
	 
	/**
	 * Set different properties that reflect how this viewer will behave.
	 * This is a set of key=value pairs. Here is an example of supported pairs:
	 * zoom= smooth|discrete default: discrete
	 * pan = normal|reverse  default: normal
	 */
	public void setParameters(Properties props);
	
	/**
	 * Get different properties that reflect how this viewer behaves.
	 * This is a set of key=value pairs. 
	 */
	public Properties getParameters();
	
	/**
	 * Get the viewing window in absolute coordinates.
	 * @return Rectangle - view rectangle
	 */
	public Rectangle getViewRectangle();
	
	/**
	 * Set the viewing window in absolute coordinates.
	 * @param Rectangle - view rectangle
	 */
	public void setViewRectangle(Rectangle r);
	
	/**
	 * Get the viewing window offset (top-left corner) in absolute coordinates.
	 * @return ViewerLocation - view offset
	 */
	public ViewPosition getViewPosition();
	
	/**
	 * Set the viewing window offset (top-left corner) in absolute coordinates.
	 * @param ViewerLocation - view offset
	 */
	public void setViewPosition(ViewPosition p);
	
	/**
	 * Get the viewing window center offset in absolute coordinates.
	 * @return ViewerLocation - view offset
	 */
	public ViewPosition getCenterPosition();
	
	/**
	 * Set the viewing window center offset in absolute coordinates.
	 * @param ViewerLocation - view offset
	 */
	public void setCenterPosition(ViewPosition p);
	
	/**
	 * Set zoom level of the viewer
	 */
	public void setScale(double n);
	
	/**
	 * Get zoom level of the viewer
	 */
	public double getScale();

	/**
	 * Get the minimum zoom level for current image
	 */	
	public double getMinimumScale();
	
	/**
	 * Get the maximum zoom level for current image
	 */	
	public double getMaximumScale();
	
	/**
	 * Make a snapshot of current viewer window
	 * @param  int mode: 
	 *  IMG_NO_MARKERS  - do not show annotations
	 *  IMG_ALL_MARKERS - show all annotations
	 *  IMG_SELECTED_MARKERS - show only selected annotations
	 * @return Image snapshot of the slide
	 */
	public Image getSnapshot(int mode);
	
	/**
	 * Make a snapshot of current viewer window
	 * @return Image snapshot of the slide
	 */
	public Image getSnapshot();
	
	/**
	 * Gets the controller for this viewer.
	 * @return ViewerController controller that is currently used in the viewer
	 */
	public ViewerController getViewerController();
	      	
	/**
	 * Convert point in absolute image coordinates to relative view coordinates.
	 * @param  Point - in absolute image coordinates
	 * @return Point - in relative view coordinates
	 */
	public Point convertImageToView(Point img);
	
	/**
	 * Convert point in relative view coordinates to absolute image coordinates.
	 * @param   Point - in relative view coordinates
	 * @return  Point - in absolute image coordinates
	 */
	public Point convertViewToImage(Point view);
	
	/**
	 * Repaint viewer content.
	 */
	public void repaint();
	
	/**
	 * Remove all annotations and zoom out to original position
	 */
	public void reset();
	
    /**
     * Should be called to release all reseources
     */
    public void dispose();
    
    /**
     * updates the viewer to reflect dynamic changes that were performed
     * on the viewer configuration or the image that is currently loaded
     * Ex: changes to image tranformation can lead to flashing cache as well
     * as image rotation brightening etc.. 
     */
    public void update();
    
    /**
     * Get size of pixel in mm.
     * Most proably 1/size will be used to get 
     * number of pixels in millimeter
     * return double size
     */
    public double getPixelSize();
    
    /**
     * Get misc image specific meta data
     * like compression, scanner, type etc
     * Could be an empty list, if viewer API
     * doesn't have any meta data to give
     * @return Properties
     */
    public Properties getImageMetaData();
    
    
    /**
     * Set the policy for zoom levels
     * @param given zoom level policy
     */
    public void setScalePolicy(ScalePolicy policy);
    
    /**
     * Get the zoom policy of this viewer
     * @return scale policy for given viewer
     */
    public ScalePolicy getScalePolicy();
    
    
    /**
     * Get panel with extra slide information
     */
    public Container getInfoPanel();
    
    /**
     * Set cursor for this viewer
     * @param c
     */
    public void setCursor(Cursor c);
    
    /**
     * close currently opened image
     */
    public void closeImage();
    
   
    /**
     * Get image properties object
     * This object includes name, image dimensions and thumbnail image (if avialable)
     * @return ImageProperties object
     */
    public ImageProperties getImageProperties();
    
    
    /**
     * Get Magnifier panel that is connected to this viewer instance
     */
    public Magnifier getMagnifier();
   
    
    /**
     * set custom control panel
     * @param cp
     */
    public void setViewerControlPanel(ViewerControlPanel cp);


    /**
     * create another instance of this viewer with all of the parameters
     * @return Viewer
     */
	public Viewer clone();  
}
