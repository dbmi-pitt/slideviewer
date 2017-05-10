package edu.pitt.slideviewer;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.*;
import java.util.*;
import javax.swing.*;
import edu.pitt.slideviewer.markers.*;

import java.beans.*;

public abstract class AbstractViewer implements Viewer  {
	
    private ScalePolicy scalePolicy;
    private DefaultNavigator navigator;
    private AnnotationPanel annotationPanel;
    private ViewerController controller;
    private ViewerControlPanel controlPanel;
    private Container viewerPanel;
    private ImageProperties info; // image meta data
    private Properties params; // misc controls outside of API
    private EventSender eventSender;
	private JPanel infoPanel;
	private Magnifier magnifier;
    private URL server; // image server
    
    //  property support
	PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    

	/**
	 * clone viewer
	 */
	public abstract Viewer clone();
    
    /**
	 * Adds a listener to monitor events from the viewer.
	 * Supported properties are: 'ViewChange'
	 * @param PropertyChangeListener listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}

	/**
	 * Remove a listener that monitor events from the viewer.
	 * Supported properties are: 'ViewChange'
	 * @param PropertyChangeListener listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	/**
	 * Get list of all change listeners
	 * @return All listeners
	 */
	public PropertyChangeListener[] getPropertyChangeListeners() {
		return pcs.getPropertyChangeListeners();
	}

	/**
	 * Send out property change event.
	 * @param String property name
	 * @param Object old value
	 * @param Object new value
	 */
	public void firePropertyChange(String p, Object a, Object b) {
		pcs.firePropertyChange(p, a, b);
	}

    

    /**
     * Create viewer panel
     */
    public Container createViewerPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());
        pnl.add(getViewerComponent(), BorderLayout.CENTER);
        pnl.add(getViewerControlPanel(), BorderLayout.SOUTH);
        return pnl;
    }

    
    /**
     * Get annotation panel
     */
    public AnnotationPanel getAnnotationPanel() {
        return annotationPanel;
    }

    /**
     * Get image name
     */
    public String getImage() {
        if (hasImage())
            return info.getName();
        return null;
    }

    /**
     * Get misc image specific meta data like compression, scanner, type etc
     * Could be an empty list, if viewer API doesn't have any meta data to give
     * 
     * @return Properties
     */
    public Properties getImageMetaData() {
        if (hasImage())
            return info.getProperties();
        return null;
    }

    /**
     * Get slide dimensions in absolut coordinates
     */
    public Dimension getImageSize() {
        if (hasImage())
            return info.getImageSize();
        return null;
    }

    /**
     * Get maximum scale factor
     */
    public double getMaximumScale() {
        return 1.0;
    }

    /**
     * Get minimum scale factor
     */
    public double getMinimumScale() {
        if(hasImage()){
        	/*
        	double s;
        	Dimension isize = info.getImageSize();
            Dimension size = getSize();
            //if(isize.width > isize.height)
            if(Utils.isHorizontal(size, isize))
            	s = ((double) (size.width))/ isize.getWidth();
            else
            	s = ((double) (size.height))/ isize.getHeight();
            
            // check bounds of scale
            return (s > 1.0)?1.0:s; 
            */
        }
        return 0;
    }

    /**
     * Get slide navigator
     */
    public Navigator getNavigator() {
       return navigator;
    }

    /**
     * Get misc settings
     */
    public Properties getParameters() {
        return params;
    }

    /**
     * Get pixel size
     */
    public double getPixelSize() {
        if (hasImage())
            return info.getPixelSize();
        return 0;
    }

    /**
     * Get scale policy
     */

    public ScalePolicy getScalePolicy() {
        return scalePolicy;
    }

    /**
     * Get server URL
     */
    public URL getServer() {
        return server;
    }

    /**
     * Take snapshot of the slide
     */
    public Image getSnapshot(int mode) {
        Dimension d = getSize();
    	
        //Image img = createImage(getSize().width, getSize().height);
        Image img = new BufferedImage(d.width,d.height,BufferedImage.TYPE_INT_RGB);
        // disable annotations
        // else diable markers
        if (mode == IMG_NO_MARKERS || mode == IMG_SELECTED_MARKERS) {
            annotationPanel.setVisible(false);
        } else {
            annotationPanel.getAnnotationManager().setAnnotationMarkersVisible(false);
            repaint();
        }
        
        // paint
        getViewerComponent().paint(img.getGraphics());
        
        // draw selected markers
        if (mode == IMG_SELECTED_MARKERS) {
            Collection list = annotationPanel.getAnnotationManager().getSelectedAnnotations();
            for (Iterator i = list.iterator(); i.hasNext();) {
                Annotation marker = (Annotation) i.next();
                marker.drawShape(img.getGraphics());
            }
        }
        
        // reenable annotation panel
        annotationPanel.setVisible(true);
        annotationPanel.getAnnotationManager().setAnnotationMarkersVisible(true);
        repaint();
        
        return img;
    }

    /**
     * Take snapshot of the slide
     */
    public Image getSnapshot() {
        return getSnapshot(IMG_ALL_MARKERS);
    }

    /**
     * This viewer window
     */
    public abstract Component getViewerComponent();

    /**
     * Get control panel
     */
    public ViewerControlPanel getViewerControlPanel() {
    	String sz = (params != null)?""+params.getProperty("viewer.button.size"):"big";
        boolean small = sz.equalsIgnoreCase("small");
    	if (controlPanel == null) {
            controlPanel = new ViewerControlPanel(this,small);
            addPropertyChangeListener(controlPanel);     
        }
        return controlPanel;
    }

    /**
     * set custom control panel
     * @param cp
     */
    public void setViewerControlPanel(ViewerControlPanel cp){
    	controlPanel = cp;
    }

    /**
     * Get viewer controller
     */
    public ViewerController getViewerController() {
        return controller;
    }

    /**
     * Get viewer with control panel
     */
    public Container getViewerPanel() {
        if (viewerPanel == null) {
            viewerPanel = createViewerPanel();
        }
        return viewerPanel;
    }

    /**
     * Image is loaded
     */
    public boolean hasImage() {
        return info != null;
    }


    /**
     * Release all resources
     */
    public void dispose() {
    	reset();
    	//TODO: cleanup
    }
    
    /**
     * Remove all annotations and zoom out to original position
     */
    public void reset() {
        annotationPanel.getAnnotationManager().removeAnnotations();
        annotationPanel.removeComponents();
        controller.resetZoom();
        if(controlPanel != null)
        	controlPanel.showNavigatorWindow(false);
    }

    /**
     * Misc controls for viewer none for now
     */
    public void setParameters(Properties props) {
        params = props;
    }

    /**
     * @param scalePolicy the scalePolicy to set
     */
    public void setScalePolicy(ScalePolicy scalePolicy) {
        this.scalePolicy = scalePolicy;
        if (hasImage())
            scalePolicy.setMinimumScale(getMinimumScale());
    }

    /**
     * reset server URL
     */
    public void setServer(URL url) {
        server = url;
    }

    /**
     * set size of the viewer window
     */
    public void setSize(Dimension d) {
    	//TODO:
    	//setPreferredSize(d);
    }

    /**
     * set size of the viewer window
     */
    public void setSize(int w, int h) {
        setSize(new Dimension(w, h));
    }
    
    /**
     * Get viewer size
     */
    public Dimension getSize(){
    	//TODO:
    	/*
    	Dimension d = super.getSize();
        // NOTE: if viewer is not visible during set scale computation
        // the size of component will be 0, in that case we should
        // pick a number in this case 500 as default "size"
        if(d == null || (d.width <= 0 && d.height <= 0))
            return getPreferredSize();
        else
            return d;
           */
    	return null;
           
    }
    

    // ///////////////////////////////////////

    /**
     * Get current view position
     */
    public ViewPosition getViewPosition() {
        //TODO:
    	//return new ViewPosition(viewport.getLocation(),scale);
        //return new ViewPosition(viewport,scale,getImage());
    	return null;
    }

    /**
     * Get center position
     */
    public ViewPosition getCenterPosition() {
    	Rectangle viewport = getViewRectangle();
    	double scale = getScale();
    	int x = (int)viewport.getCenterX() + viewport.x;
        int y = (int)viewport.getCenterY() + viewport.y;
        return new ViewPosition(x,y,viewport.width,viewport.height,scale,getImage());
    }
    
    /**
     * Get current scale factor
     */
    public double getScale() {
    	//TODO:
    	return 0.0;
    }

    /**
     * Get view rectangle
     */
    public Rectangle getViewRectangle() {
    	return getViewPosition().getRectangle(); 
    }
    
    /**
     * Set center of viewer to that location
     */
    public void setCenterPosition(ViewPosition p) {
        Dimension d = getSize();
        int x = p.x - (int) ((d.getWidth()/2)/p.scale) ;
        int y = p.y - (int) ((d.getHeight()/2)/p.scale);
        setViewPosition(new ViewPosition(x,y,p.scale));
    }

    
    
    /**
     * Do some sanity checking for input rectangle
     * return a "correct" copy
     * @param view
     * @return
     */
    private final Rectangle correctRectangle(Rectangle view){
        Dimension idim = info.getImageSize();
        Rectangle r = new Rectangle(view);
        
        // do some sanity checking for bounds
        if(r.x < 0)
            r.x = 0;
        if(r.y < 0)
            r.y = 0;
        
        if(r.width > idim.width){
            r.width = idim.width;
        }
        if(r.height > idim.height){
            r.height = idim.height;
        }
        return r;
    }
    
    
    /**
     * Sometimes the requested image region is smaller then
     * the viewport requested, this happens for regions at
     * low power. Calculate this region offset so that it can
     * be drawn in the middle
     * @return
     */
    public final Point getImageRegionOffset(Rectangle r){
    	Rectangle viewport = getViewRectangle();
    	Dimension dim = getSize();
    	int sw = dim.width;
        int sh = dim.height;
    	
        // see if we need to have an offset
        if(r.width < viewport.width){
            sw = (int)((sh * r.width)/r.height);
        }
        if(r.height < viewport.height){
            sh = (int)((sw * r.height)/r.width);
        }
    	
		int offx = (int) ((dim.width - sw)/2);
	    int offy = (int) ((dim.height - sh)/2);
	    return new Point(offx,offy);
    }
    
    /**
     * Convert point from absolute image to view coordinates
     */
    public Point convertImageToView(Point img) {
    	Rectangle r = correctRectangle(getViewRectangle());
    	Point p = getImageRegionOffset(r);
    	double scale = getScale();
    	
    	// now calculate the offset
    	int x = (int) ((img.x - r.x) * scale)+p.x;
        int y = (int) ((img.y - r.y) * scale)+p.y;
        
        return new Point(x,y);
    }

    /**
     * Convert point from view to absolute image coordinates
     */
    public Point convertViewToImage(Point view) {
    	Rectangle r = correctRectangle(getViewRectangle());
    	Point p = getImageRegionOffset(r);
    	double scale = getScale();
    	
    	int x = (int) ((view.getX()-p.x) / scale) + r.x;
        int y = (int) ((view.getY()-p.y) / scale) + r.y;
      
        return new Point(x,y);
    }
    
    /**
     * Set location of the 
     */
    public void setViewPosition(ViewPosition p) {
    	if(!hasImage())
    		return;
        //TODO:
    }

    /**
     * Zoom in/out
     */
    public void setScale(double scl) {
    	if(!hasImage())
    		return;
    	
        //TODO:
    }
    
    /**
     * set viewport location
     */
    public void setViewRectangle(Rectangle r) {
    	//TODO:
    	
        // send events
        ViewPosition location = getViewPosition();
        firePropertyChange(Constants.VIEW_CHANGE,null,location);
    	//  if there is a previous event that has been 
		// started and didn't finish, cancel it
		notifyViewObserve(false);
	}
    
    /**
     * cancelt VIEW_OBSERVE even
     * from being sent
     */
    public void notifyViewObserve(boolean now){
    	//cancel previous event
    	if (eventSender != null) {
			eventSender.cancel();
			eventSender = null;
		}
    	
    	// notify now or later
    	if(now){
    		// send event now
    		firePropertyChange(Constants.VIEW_OBSERVE,null,getViewPosition()); 
    	}else{
    		// send view window event after X mseconds
    		eventSender = new EventSender(getViewPosition());
    		eventSender.start();	
    	}
    }
    
    
    /**
     * Close currently opened image
     */
    public abstract void closeImage();
    
   
    /**
     * LOADING new image, that is interesting
     */
    public abstract void openImage(String name) throws ViewerException ;

    /**
     * Create info panel
     * @return
     */
    private JPanel createInfoPanel(){
    	if(info == null)
    		return null;
    	JPanel panel = new JPanel();
    	panel.setLayout(new BorderLayout(5,5));
    	
    	// constract info label
     	JLabel inf = new JLabel(info.getHTMLDescription());
		JLabel lbl = new JLabel(new ImageIcon(info.getLabel()),JLabel.LEFT);
    	panel.add(inf,BorderLayout.WEST);
    	panel.add(lbl,BorderLayout.EAST);
    
    	return panel;
    }
    
    
    /**
     * Get panel with extra info
     * @return
     */
    public Container getInfoPanel(){
    	if(infoPanel == null)
    		infoPanel = createInfoPanel();
    	return infoPanel;
    }
    

	/**
	 * This class sends a viewer event if it is not
	 * interrupted for a set amount of time
	 * @author tseytlin
	 */
	public class EventSender extends Thread {
		private final int iterC = 20;
		private final int sleepC = 50;
		// WAIT TIME IS 30*50 = 1.0 s
		private ViewPosition p;

		/**
		 * Send window event with this view position
		 * @param p
		 */
		public EventSender(ViewPosition p) {
			this.p = p;
		}

		private boolean stopThread = false;

		/**
		 * cancel execution of this event
		 */
		public void cancel() {
			stopThread = true;
		}

		/**
		 * Wait for a time and then execute
		 */
		public void run() {
			// sleep for a time iterC* sleepC
			for (int i = 0; i < iterC; i++) {
				// if thead was canceled, quit
				if (stopThread)
					return;
				try {
					sleep(sleepC);
				} catch (InterruptedException ex) {
				}
			}
			//send event after waiting for a some time
			firePropertyChange(Constants.VIEW_OBSERVE, null, p);
		}
	}

    /**
	 * Convinent method to get AnnotationManager
	 * same thing as getAnnotationPanel().getMarkerManager()
	 * @return
	 */
	public AnnotationManager getAnnotationManager(){
		return (annotationPanel != null)?annotationPanel.getAnnotationManager():null;
	}
	
	
	
    /**
     * Get image properties object
     * This object includes name, image dimensions and thumbnail image (if avialable)
     * @return ImageProperties object
     */
    public ImageProperties getImageProperties(){
    	return info;
    }
    
    
    /**
     * Get Magnifier panel that is connected to this viewer instance
     */
    public Magnifier getMagnifier(){
    	if(magnifier == null)
    		magnifier = new Magnifier(this);
    	return magnifier;
    }
    
    
    /**
     * get a regular expression string that will match filanames
     * that are supported by this viewer. 
     * This is based on assumption (not always correct) that filename extension
     * is a good indicator of a type of image that it can support
     * Ex:   ".*\\.(jpg|jpeg|JPG)" expression should match to any JPEG image
     * @return regexp
     */
    public static String getSupportedFormat(){
    	//TODO:
    	return ".*\\.(svs|tif|tiff|jp2)";
    }
    
    /**
     * get "type" of the viewer
     * @return
     */
    public static String getViewerType(){
    	//TODO:
    	return "some";
    }
    
    /**
     * repaint viewer
     */
    public void repaint(){
    	getViewerComponent().repaint();
    }
    
    /**
     * set cursor for this viewer
     */
    public void setCursor(Cursor c){
    	getViewerComponent().setCursor(c);
    }
}
