package edu.pitt.slideviewer.qview;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.net.*;
import java.util.*;
import edu.pitt.slideviewer.*;

import javax.swing.*;
import javax.swing.Timer;

import edu.pitt.slideviewer.markers.*;
import edu.pitt.slideviewer.policy.DiscreteScalePolicy;
import edu.pitt.slideviewer.qview.aperio.*;
import edu.pitt.slideviewer.qview.connection.*;
import edu.pitt.slideviewer.qview.hamamatsu.NDPConnectionManager;
import edu.pitt.slideviewer.qview.openslide.OpenSlideConnectionManager;
import edu.pitt.slideviewer.qview.zeiss.ZeissConnectionManager;


/**
 * This is a Quick and Dirty Viewer for Aperio image server
 * @author tseytlin
 */
public class QuickViewer extends JPanel implements Viewer {
    public static final int APERIO_TYPE = 0;
    public static final int OPENSLIDE_TYPE = 1;
    public static final int HAMAMATSU_TYPE = 2;
    public static final int ZEISS_TYPE = 3;
    
	
	//private final String logoIcon = "/icons/dbmi-logo.png";
    //private final String DEFAULT_PASSWORD = "secret";
    private ImageIcon logo;
    private URL server; // image server
    private int type;
    private ScalePolicy scalePolicy;
    private QuickNavigator navigator;
    private QuickAnnotationPanel annotationPanel;
    private ViewerController controller;
    private ViewerControlPanel controlPanel;
    private Container viewerPanel;
    private ImageInfo info; // image meta data
    private Properties params; // misc controls outside of API
    private ConnectionManager manager;
    // area of the slide that is being viewed in absolute coordinates
    private double scale;
    private Rectangle viewport;
	private EventSender eventSender;
	private JPanel infoPanel;
	private Magnifier magnifier;
	//private Progress progress;
	private Timer busyTimer;
	private boolean busy;
	private int arcOffset = 0;
	// control the size of the edge margin in pixels
	private int marginSize = 0;
	private boolean centerOnClick;
	
	/**
	 * new quick viewer
	 * @param server url
	 */
    public QuickViewer(URL server) {
    	this(server,APERIO_TYPE);
    }
	
    
   /**
    * New quick viewer of type 
    * @param server
    * @param type APERIO_TYPE, OPENSLIDE_TYPE
    */
    public QuickViewer(URL server,int type) {
        super();
        this.server = server;
        this.type = type;
        // setup displayable component for Utils
        Utils.setComponent(this);
        
        annotationPanel = new QuickAnnotationPanel(this);
        controller = new QuickViewController(this);
        navigator = new QuickNavigator(this);
        
        // init different types
        switch (type) {
        case APERIO_TYPE:
			manager = new AperioTileConnectionManager(this);
			break;
		case OPENSLIDE_TYPE:
			manager = new OpenSlideConnectionManager(this);
			break;
		case HAMAMATSU_TYPE:
			manager = new NDPConnectionManager(this);
			break;
		case ZEISS_TYPE:
			manager = new ZeissConnectionManager(this);
			break;
		default:
			manager = new AperioTileConnectionManager(this);
			break;
		}
        	
        
        // create logo
        logo = ViewerFactory.getLogoIcon();
        
        //take care of resizing
        addComponentListener(manager.getComponentListener());
        
        // create scale policy
        scalePolicy = new DiscreteScalePolicy();
        
        //set size
        setSize(new Dimension(500,500));
        
        // setup some props
    	try{setMarginSize(Integer.parseInt(ViewerFactory.getProperty("qview.margin.size")));}catch(Exception ex){}
    	setCenterOnClick(Boolean.parseBoolean(ViewerFactory.getProperty("qview.center.on.click")));
        
    }

    /**
	 * clone viewer
	 */
	public Viewer clone(){
		Viewer v = new QuickViewer(server,type);
		if(hasImage())
			try {
				v.openImage(getImage());
			} catch (ViewerException e) {
				e.printStackTrace();
			}
		return v;
	}
    

    /**
     * set new connection manager
     * @param m
     */
    public void setConnectionManager(ConnectionManager m){
    	if(manager != null)
    		removeComponentListener(manager.getComponentListener());
    	manager = m;
    	addComponentListener(manager.getComponentListener());
    }
    
    public ConnectionManager getConnectionManager(){
    	return manager;
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
     * Send out property change event.
     * 
     * @param String
     *            property name
     * @param Object
     *            old value
     * @param Object
     *            new value
     */
    public void firePropertyChange(String p, Object a, Object b) {
        super.firePropertyChange(p, a, b);
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
            return info.getImagePath();
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
     * Get slide dimensions in absolute coordinates
     */
    public Dimension getImageSize() {
        if (hasImage()){
            return info.getImageSize();
        }
        return new Dimension(0,0);
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
        paintComponent(img.getGraphics());
        
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
    public Component getViewerComponent() {
        return this;
    }

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
    	if (eventSender != null) {
			eventSender.cancel();
			eventSender = null;
		}
    	if( magnifier != null){
    		magnifier.dispose();
    		magnifier = null;
    	}
    	
    	Utils.setComponent(null);
        removeComponentListener(manager.getComponentListener());
        annotationPanel.dispose();
        navigator.dispose();
        if(controlPanel != null)
        	controlPanel.dispose();
        manager.dispose();
        controller.dispose();
        if(viewerPanel != null)
        	viewerPanel.removeAll();
        viewerPanel = null;
        infoPanel = null;
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
     * @param scalePolicy
     *            the scalePolicy to set
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
    	setPreferredSize(d);
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
    	Dimension d = super.getSize();
        // NOTE: if viewer is not visible during set scale computation
        // the size of component will be 0, in that case we should
        // pick a number in this case 500 as default "size"
        if(d == null || (d.width <= 0 && d.height <= 0))
            return getPreferredSize();
        else
            return d;
           
    }
    

    // ///////////////////////////////////////

    /**
     * Get current view position
     */
    public ViewPosition getViewPosition() {
        //return new ViewPosition(viewport.getLocation(),scale);
        return new ViewPosition(viewport,scale,getImage());
    }

    /**
     * Get center position
     */
    public ViewPosition getCenterPosition() {
        int x = (int)viewport.getCenterX() + viewport.x;
        int y = (int)viewport.getCenterY() + viewport.y;
        return new ViewPosition(x,y,scale);
    }
    
    /**
     * Get current scale factor
     */
    public double getScale() {
       return scale;
    }

    /**
     * Get view rectangle
     */
    public Rectangle getViewRectangle() {
        if(viewport != null)
            return new Rectangle(viewport);
        else
            return null;
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
        Dimension idim = getImageSize();
        return Utils.correctRectangle(idim, view);
    }
    
    
    /**
     * Sometimes the requested image region is smaller then
     * the viewport requested, this happens for regions at
     * low power. Calculate this region offset so that it can
     * be drawn in the middle
     * @return
     */
    public final Point getImageRegionOffset(Rectangle r){
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
    	Rectangle r = correctRectangle(viewport);
    	Point p = getImageRegionOffset(r);
    	
    	// try to compensate for rotation
    	img = info.getImageTransform().getTransformedPoint(img,true);
			
    	// now calculate the offset
    	int x = (int) ((img.x - r.x) * scale)+p.x;
        int y = (int) ((img.y - r.y) * scale)+p.y;
        
        return new Point(x,y);
    }

    /**
     * Convert point from view to absolute image coordinates
     */
    public Point convertViewToImage(Point view) {
    	Rectangle r = correctRectangle(viewport);
    	Point p = getImageRegionOffset(r);
    	
    	int x = (int) ((view.getX()-p.x) / scale) + r.x;
        int y = (int) ((view.getY()-p.y) / scale) + r.y;
      
        Point pt =  new Point(x,y);
        	
        // try to compensate for rotation
        pt = info.getImageTransform().getTransformedPoint(pt,false);
        
		return pt;
        
    }
    
    /**
     * Convert point from view to absolute image coordinates
     */
    public Point convertViewToImageNoTransform(Point view) {
    	Rectangle r = correctRectangle(viewport);
    	Point p = getImageRegionOffset(r);
      	
    	int x = (int) ((view.getX()-p.x) / scale) + r.x;
        int y = (int) ((view.getY()-p.y) / scale) + r.y;
      
        Point pt =  new Point(x,y);
        	
        // try to compensate for rotation
        //pt = info.getImageTransform().getTransformedPoint(pt,false);
        
		return pt;
        
    }
    
    /**
     * Set location of the 
     */
    public void setViewPosition(ViewPosition p) {
    	if(!hasImage())
    		return;
        setScale(p.scale);
        setViewRectangle(new Rectangle(p.getPoint(),viewport.getSize()));
        repaint();
    }

    /**
     * Zoom in/out
     */
    public void setScale(double scl) {
    	if(!hasImage())
    		return;
    	
        // avoid under/over zips
        if(scl < getMinimumScale())
            scl = getMinimumScale();
        if(scl > getMaximumScale())
            scl = getMaximumScale();
        
        // get current Dimensions 
        Dimension d = getSize();
        
        int w = (int) (d.getWidth() / scl);
        int h = (int) (d.getHeight() / scl);
        int x = viewport.x -w/2 + (int)(d.getWidth()/scale)/2;      //- w/2
        int y = viewport.y -h/2 + (int)(d.getHeight()/scale)/2;     //- h/2
        scale = scl;
        
        //rebound rectangle
        setViewRectangle(new Rectangle(x,y,w,h));
        repaint();    
    }
    
    /**
     * set viewport location
     */
    public void setViewRectangle(Rectangle r) {
    	if(!hasImage())
    		return;
    	
    	// make sure there is a component
    	if(Utils.getComponent() == null)
    		Utils.setComponent(getViewerComponent());
    	
    	// determine scale factor
        Dimension d = getSize();
        Dimension id = info.getImageSize();
        //boolean hrz = id.width > id.height;
        boolean hrz = Utils.isHorizontal(d,id);
        scale = (hrz)?d.getWidth()/r.width:d.getHeight()/r.height;
        int margin = (int) (marginSize/scale);
        
        // check bounds etc.
        int x = (r.x > -margin)?r.x:-margin;
        int y = (r.y > -margin)?r.y:-margin;
        x = ((x+r.width) > (id.width+margin))?id.width-r.width+margin:x;
        y = ((y+r.height) > (id.height+margin))?id.height-r.height+margin:y;
      
        // set new view rectangle
        if(viewport == null)
            viewport = new Rectangle();
        
        // set bounds 
        // fix aspect ratio and offsets
        if(hrz){
        	int h = (int)((r.width * d.getHeight())/d.getWidth());
        	viewport.setBounds(x,y,r.width,h);
        }else{
        	int w = (int)((r.height * d.getWidth())/d.getHeight());
        	viewport.setBounds(x,y,w,r.height);
        }
        repaint();
        
        // send events
        firePropertyChange(Constants.VIEW_CHANGE,null,getViewPosition());
        notifyViewObserve(false);
        //System.out.println("setViewport: "+viewport+" Offset "+getImageRegionOffset(r)+" scl:"+scale);
    }
    
    /**
     * cancel VIEW_OBSERVE even
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
    public void closeImage(){
    	if(controlPanel != null)
    		controlPanel.close();
    	// reset annotation panel
		annotationPanel.getAnnotationManager().removeAnnotations();
		manager.disconnect();
		infoPanel = null;
		info = null;
		navigator.setImage(null);
    	firePropertyChange(Constants.IMAGE_CHANGE, null,null);
    }
    
   
    /**
     * LOADING new image, that is interesting
     */
    public void openImage(String name) throws ViewerException {
    	// this should be a noop, if same image is alread loaded
		if (name.equals(getImage()))
			return;
		
        // setup displayable component for Utils
        Utils.setComponent(this);
		
		// reset annotation panel
		closeImage();
		
		// show progress 
		setBusy(true);
		
    	try{
        	manager.connect(name);
            info = manager.getImageInfo();
            if(info != null)
            	setViewRectangle(new Rectangle(new Point(0,0),info.getImageSize()));
            repaint();
            navigator.setImage(info);
            firePropertyChange(Constants.IMAGE_CHANGE, null, getImage());
        }catch(Exception ex){
        	//ex.printStackTrace();
        	throw new ViewerException("Unable to load image "+name+"\nReason: "+ex.getMessage(),ex);
        }finally{
        	 // we are done
        	 setBusy(false);
        }
    }

 
    public void paint(Graphics g){
    	paintComponent(g);
    }
    
    /**
     * Overwrite when nothing is loaded
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // don't bother when nothing is vsible
        if(!getViewerComponent().isShowing())
        	return;
        
        // draw component
        Rectangle r = getBounds();
        g.setColor(Color.white);
        g.fillRect(r.x, r.y, r.width, r.height);
        
        if (hasImage()) {
        	//long time = System.currentTimeMillis();
        	manager.drawImageRegion(g,viewport,scale);
            //System.out.println("draw image "+(System.currentTimeMillis()-time)+" ms");
            //draw annotations
            if(annotationPanel != null && annotationPanel.isVisible())
               annotationPanel.paintComponent(g);
            
        } else {
        	// draw progress bar in case we are busy
            if(busy){
            	// don't draw anything if we are busy, but didn't start the timer
            	if(busyTimer != null && busyTimer.isRunning()){
	            	final int diameter = 60;
	            	final Stroke stroke0 = new BasicStroke(20);
	        		final Stroke stroke1 = new BasicStroke(15);
	        		final Stroke stroke2 = new BasicStroke(10);
	            	int xi = (r.width - diameter)/2;
	        		int yi = (r.height - diameter)/2;
	        		
	        		Graphics2D g2 = (Graphics2D) g;
	        		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
	        		g2.setStroke(stroke0);
	        		g2.setColor(Color.gray);
	        		g2.drawOval(xi,yi, diameter,diameter);
	        		g2.setStroke(stroke1);
	        		g2.setColor(Color.white);
	        		g2.drawOval(xi,yi, diameter,diameter);
	        		g2.setStroke(stroke2);
	        		g2.setColor(Color.gray);
	        		g2.drawOval(xi,yi, diameter,diameter);
	        		g2.setColor(Color.black);
	        		g2.drawArc(xi,yi,diameter,diameter,arcOffset,45);
	        		arcOffset -=5;		
            	}
            }else{
            	// draw logo
                int x = (int) ((r.width - logo.getIconWidth()) / 2);
                int y = (int) ((r.height - logo.getIconHeight()) / 2);
                g.drawImage(logo.getImage(), x, y, null);
            }
            
         }
    }

    
    /**
     * Create info panel
     * @return
     */
    private JPanel createInfoPanel(){
    	if(info == null)
    		return null;
    	JPanel panel = new JPanel();
    	panel.setLayout(new BorderLayout(5,5));
    	
    	// fetch and organize useful data
    	/*
    	Dimension d = info.getImageSize();
    	Dimension t = info.getTileSize();
    	ArrayList slist = new ArrayList();
    	double [] scales = info.getScales();
    	for(int i=0;i<scales.length;i++)
    		slist.add(""+Math.round(scales[i]*100));
    	//Collections.reverse(slist);
    	String compression = "";
    	switch(info.getCompressionType()){
    		case(ImageInfo.LZW): compression = "LZW"; break;
    		case(ImageInfo.JPEG): compression = "JPEG"; break;
    		case(ImageInfo.JPEG2000): compression = "JPEG2000"; break;
    		default: compression = "NONE";
    	}
    	
    	// constract info label
     	JLabel inf = new JLabel("<html><table border=0 width=400>" +
		"<tr><td>image name:</td><td>"+info.getName()+"</td></tr>"+
		"<tr><td>image size:</td><td>"+d.width+" x "+d.height+"</td></tr>"+
		"<tr><td>tile size:</td><td>"+t.width+" x "+t.height+"</td></tr>"+
		"<tr><td>pixel size:</td><td>"+info.getPixelSize()+" mm</td></tr>"+
		"<tr><td>zoom levels:</td><td>"+slist+"</td></tr>"+
		"<tr><td>compression:</td><td>"+compression+"</td></tr></table>");
		//info.setLabel(manager.getImageLabel());
     	*/
     	Image img = info.getLabel();
     	int width = img.getWidth(null);
     	int height = img.getHeight(null);
     	
     	if(width > 200 || height > 200){
			Dimension d = new Dimension(width,height);
			if(width > height){
				d.width = 200;
				d.height = d.width*height/width;
			}else{
				d.height = 200;
				d.width = d.height*width/height;
			}
			img = img.getScaledInstance(d.width,d.height,Image.SCALE_SMOOTH);
		}
    	
    	JLabel inf = new JLabel(info.getHTMLDescription());
		JLabel lbl = new JLabel(new ImageIcon(img),JLabel.LEFT);
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
    
    public int getMarginSize() {
		return marginSize;
	}


	public void setMarginSize(int marginSize) {
		this.marginSize = marginSize;
	}

	public boolean isCenterOnClick() {
		return centerOnClick;
	}

	public void setCenterOnClick(boolean b) {
		this.centerOnClick = b;
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
    		magnifier = new QuickMagnifier(this);
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
    public static String getSupportedFormat(int type){
    	//return (type == OPENSLIDE_TYPE)?".*\\.(vms|vmu|mrxs|tiff?|svs|dcm|dicom|bmp|fits)":"(.*\\.(svs|jp2)|[Aa][Pp]_.*\\.tiff?)";
    	if(type == APERIO_TYPE)
    		return "(.*\\.(svs|jp2)|[Aa][Pp]_.*\\.tiff?)";
    	if(type == HAMAMATSU_TYPE)
    		return ".*\\.(vms|vmu|ndpi)";
    	if(type == ZEISS_TYPE)
    		return ".*\\.(tiff?|xtp|mrxs|tsl)";
    	// openslide default
    	return ".*\\.(vms|vmu|ndpi|mrxs|tiff?|svs|dcm|dicom|bmp|fits|scn|bif|svslide)";
    	
    }
    
    /**
     * get "type" of the viewer
     * @return
     */
    public static String getViewerType(int type){
    	if(type == APERIO_TYPE)
    		return "aperio";
    	if(type == HAMAMATSU_TYPE)
    		return "hamamatsu";
    	if(type == ZEISS_TYPE)
    		return "zeiss";
    	return "openslide";
    }
    
    /**
     * updates the viewer to reflect dynamic changes that were performed
     * on the viewer configuration or the image that is currently loaded
     * Ex: changes to image tranformation can lead to flashing cache as well
     * as image rotation brightening etc.. 
     */
    public void update(){
    	SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				if(info == null)
					return;
				info.updateTransforms();
				info.setThumbnail(manager.getImageThumbnail());
				if(manager instanceof TileConnectionManager)
					((TileConnectionManager)manager).getTileManager().clearCache();
				navigator.setImage(info);
				repaint();
				controller.resetZoom();
				// notify of transform
				firePropertyChange(Constants.IMAGE_TRANSFORM, null,info.getImageTransform());
			}
		});
    }
   
    
    /**
     * set busy
     * @param b
     */
    public void setBusy(boolean b){
    	busy = b;
    
    	// initialize busy timer
    	if(busyTimer == null){
    		busyTimer = new Timer(20,new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					repaint();
				}
			});
    		busyTimer.setRepeats(true);
    	}
    	if(busy){
    		Timer t = new Timer(300,new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if(busy)
						busyTimer.start();
					
				}
			});
    		t.setRepeats(false);
    		t.start();
    	}else{
    		busyTimer.stop();
    	}
    	
    	// do repaint
    	repaint();
    }
}
