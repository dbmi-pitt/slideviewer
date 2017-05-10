package edu.pitt.slideviewer.simple;

import java.util.Properties;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.net.*;
import javax.swing.*;

import edu.pitt.slideviewer.*;
import edu.pitt.slideviewer.markers.*;
import edu.pitt.slideviewer.policy.*;
import edu.pitt.slideviewer.qview.connection.Utils;

/**
 * This class is a middleware interface to VirtualMicroscope viewer(s).
 * Currently it implements mScope Viewer.
 */
public class SimpleViewer extends JPanel implements Viewer, ComponentListener {
	private Image image;
	private Rectangle viewport;
	private URL server;
	private double scale;
	// control the size of the edge margin in pixels
	private int marginSize = 0;

	// viewer panel
	private String imageName;
	private Properties params;
	private ViewerController controller;
	private AnnotationPanel annotationPanel;
	private ViewerControlPanel controlPanel;
	private DefaultNavigator navigator;
	private Container viewerPanel;
	private ScalePolicy scalePolicy;
	private Magnifier magnifier;
	private ImageProperties info;

	/**
	 * Construct embpty viewer.
	 */
	public SimpleViewer() {
		super();
		setPreferredSize(new Dimension(500, 500));

		// setup annotation panel
		annotationPanel = new DefaultAnnotationPanel(this);
		//navigator = new DefaultNavigator(this);

		// setup controller
		controller = new DefaultViewerController(this);
		addMouseListener(controller);
		addMouseMotionListener(controller);

		addComponentListener(this);
		
		// create scale policy
		scalePolicy = new DiscreteScalePolicy();
		// scalePolicy = new NoScalePolicy();
	}

	/**
	 * clone viewer
	 */
	public Viewer clone(){
		Viewer v = new SimpleViewer();
		if(hasImage())
			try {
				v.openImage(getImage());
			} catch (ViewerException e) {
				e.printStackTrace();
			}
		return v;
	}
	
	/**
	 * Get server URL
	 * 
	 * @return URL
	 */
	public URL getServer() {
		return server;
	}

	public void setServer(URL url) {
		server = url;
	}

	/**
	 * Get the name/path of the currently loaded image
	 * 
	 * @return String image name
	 */
	public String getImage() {
		return imageName;
	}

	/**
	 * Check if there is a loaded slide
	 * 
	 * @return boolean true if image is loaded
	 */
	public boolean hasImage() {
		return image != null;
	}

	/**
	 * Get the size of the current image in absolute coordinates
	 * 
	 * @return Dimension size of the image
	 */
	public Dimension getImageSize() {
		if(hasImage())
			return new Dimension(image.getWidth(null), image.getHeight(null));
		return new Dimension(0,0);
	}

	/**
	 * Get Image Navigator. Little thumbnail window with slide map
	 * 
	 * @return Navigator
	 */
	public Navigator getNavigator() {
		if (navigator == null) {
			navigator = new DefaultNavigator(this);
			navigator.setImageProperties(info);
			navigator.setHistoryVisible(false);
		}
		return navigator;
	}

	/**
	 * Get the instance of annotation panel
	 */
	public AnnotationPanel getAnnotationPanel() {
		return annotationPanel;
	}

	/**
	 * Get the instance of annotation panel
	 */
	public ViewerController getViewerController() {
		return controller;
	}

	/**
	 * Get control panel
	 */
	public ViewerControlPanel getViewerControlPanel() {
		String sz = (params != null) ? params.getProperty("viewer.button.size") : "big";
		boolean small = sz.equalsIgnoreCase("small");
		if (controlPanel == null) {
			controlPanel = new ViewerControlPanel(this, small);
			addPropertyChangeListener(controlPanel);
		}
		return controlPanel;
	}

	/**
	 * set custom control panel
	 * 
	 * @param cp
	 */
	public void setViewerControlPanel(ViewerControlPanel cp) {
		controlPanel = cp;
	}

	/**
	 * Get panel that has both viewer and control panel
	 * 
	 * @return JPanel viewer.
	 */
	public Container createViewerPanel() {
		JPanel pnl = new JPanel();
		pnl.setLayout(new BorderLayout());
		pnl.add(getViewerComponent(), BorderLayout.CENTER);
		pnl.add(getViewerControlPanel(), BorderLayout.SOUTH);
		return pnl;
	}

	/**
	 * Get panel that has both viewer and control panel
	 * 
	 * @return JPanel viewer.
	 */
	public Container getViewerPanel() {
		if (viewerPanel == null) {
			viewerPanel = createViewerPanel();
		}
		return viewerPanel;
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
	 * Set size of viwer component. Calls setPrefferedSize on viewer component.
	 * 
	 * @param Dimension
	 *            new size of viewer
	 */
	public void setSize(Dimension d) {
		setPreferredSize(d);
	}

	/**
	 * Get size of viwer component. Calls getPrefferedSize on viewer component.
	 * 
	 * @param Dimension
	 *            new size of viewer
	 */
	public Dimension getSize() {
		if (isShowing())
			return super.getSize();
		else
			return getPreferredSize();
	}



	/**
	 * Set size of viwer component. Calls setPrefferedSize on viewer component.
	 * 
	 * @param int with
	 * @param int height
	 */
	public void setSize(int w, int h) {
		setSize(new Dimension(w, h));
	}

	/**
	 * Get viewing component. This method should be used to get access to
	 * component that is going to be placed somewhere.
	 * 
	 * @return Container viewing component.
	 */
	public Component getViewerComponent() {
		return this;
	}

	/**
	 * Minimum scale factor for current image
	 */
	public double getMinimumScale() {
		if(info != null){
			double s;
			Dimension vsize = getSize();
			Dimension isize = info.getImageSize();
			if(Utils.isHorizontal(vsize,isize))
				s = ((double) vsize.width) / isize.width;
			else
				s = ((double) vsize.height) / isize.height;
			return (s > 1.0) ? 1.0 : s;
			}
		return 0;
	}

	/**
	 * Minimum scale factor for current image
	 */
	public double getMaximumScale() {
		return 1.0;
	}

	/**
	 * Make a snapshot of current viewer window
	 */
	public Image getSnapshot() {
		return getSnapshot(IMG_ALL_MARKERS);
	}

	/**
	 * Make a snapshot of current viewer window
	 */
	public Image getSnapshot(int mode) {
		Image img = createImage(getSize().width, getSize().height);
		// disable annotations
		if (mode == IMG_NO_MARKERS || mode == IMG_SELECTED_MARKERS)
			annotationPanel.setVisible(false);
		paint(img.getGraphics());
		// draw selected markers
		if (mode == IMG_SELECTED_MARKERS) {
			for (Annotation marker: getAnnotationManager().getSelectedAnnotations()){
				marker.drawShape(img.getGraphics());
			}
		}
		// reenable annotation panel
		annotationPanel.setVisible(true);
		repaint();
		return img;
	}

	/**
	 * Load slide image into the viewer.
	 * 
	 * @param String
	 *            - name of the slide (might require full path).
	 */
	public void openImage(String name) throws ViewerException {
		imageName = name;
		try {
			Toolkit tk = Toolkit.getDefaultToolkit();
			image = null;

			// check what type of image this is

			// is this a file
			File f = new File(name);
			if (f.exists()) {
				image = tk.createImage(f.getAbsolutePath());
				// is this a url
			} else {
				URL url = null;
				try {
					url = new URL(Utils.filterURL(name));
				} catch (MalformedURLException ex) {
					// NO it is not a url, maybe it is
					// an image that connects to a server
					// else this method will throw an exception
					url = new URL("" + server + "?action=file&path=" + Utils.filterURL(name));

				}
				if (url != null)
					image = tk.createImage(url);
				else
					throw new ViewerException("Could not load image " + name);
			}

			MediaTracker tracker = new MediaTracker(this);
			tracker.addImage(image, 0);
			tracker.waitForAll();
			
			// set properties
			info = createImageProperties();
			
			if (scalePolicy != null)
				scalePolicy.setMinimumScale(getMinimumScale());

			// image change
			firePropertyChange(Constants.IMAGE_CHANGE, null, getImage());
			
			// set init viewport
			setViewRectangle(new Rectangle(new Point(0, 0), info.getImageSize()));
			
			repaint();
			if (navigator != null)
				navigator.setImageProperties(info);

			// System.out.println("Loaded image "+name+" "+image.getWidth(null)+"x"+image.getHeight(null));
		} catch (Exception ex) {
			throw new ViewerException("Could not load image " + name, ex);
		}
		;
	}

	/**
	 * Close currently opened image
	 */
	public void closeImage() {
		reset();
		image = null;
		imageName = null;
		info = null;
		firePropertyChange(Constants.IMAGE_CHANGE, null, null);
	}

	/**
	 * Get view rectangle
	 */
	public Rectangle getViewRectangle() {
		if (viewport != null)
			return new Rectangle(viewport);
		else
			return null;
	}

	/**
	 * Get current view position
	 */
	public ViewPosition getViewPosition() {
		return new ViewPosition(viewport, scale, getImage());
	}

	/**
	 * Get center position
	 */
	public ViewPosition getCenterPosition() {
		int x = (int) viewport.getCenterX() + viewport.x;
		int y = (int) viewport.getCenterY() + viewport.y;
		return new ViewPosition(x, y, scale);
	}

	/**
	 * Set different properties that reflect how this viewer will behave. This
	 * is a set of key=value pairs. Here is an example of supported pairs: zoom=
	 * smooth|discrete default: discrete pan = normal|reverse default: normal
	 */
	public void setParameters(Properties props) {

	}

	/**
	 * Get different properties that reflect how this viewer behaves
	 */
	public Properties getParameters() {
		return params;
	}

	/**
	 * Set location of the
	 */
	public void setViewPosition(ViewPosition p) {
		if (!hasImage())
			return;
		setScale(p.scale);
		setViewRectangle(new Rectangle(p.getPoint(), viewport.getSize()));
		repaint();
	}

	/**
	 * Zoom in/out
	 */
	public void setScale(double scl) {
		if(!hasImage() || viewport == null)
			return;

		// avoid under/over zips
		if (scl < getMinimumScale())
			scl = getMinimumScale();
		if (scl > getMaximumScale())
			scl = getMaximumScale();

		// don't do anything if there is no change
		if(scale == scl)
			return;
		
		// get current Dimensions
		Dimension d = getSize();

		int w = (int) (d.getWidth() / scl);
		int h = (int) (d.getHeight() / scl);
		int x = viewport.x - w / 2 + (int) (d.getWidth() / scale) / 2; // - w/2
		int y = viewport.y - h / 2 + (int) (d.getHeight() / scale) / 2; // - h/2
		scale = scl;

		// rebound rectangle
		setViewRectangle(new Rectangle(x, y, w, h));
		repaint();
	}

	/**
	 * set viewport location
	 */
	public void setViewRectangle(Rectangle r) {
		if (!hasImage())
			return;

		// make sure there is a component
		// if(Utils.getComponent() == null)
		// Utils.setComponent(getViewerComponent());

		// determine scale factor
		Dimension d = getSize();
		Dimension id = info.getImageSize();
		// boolean hrz = id.width > id.height;
		boolean hrz = Utils.isHorizontal(d, id);
		scale = (hrz) ? d.getWidth() / r.width : d.getHeight() / r.height;
		
		// avoid under/over zips
		if (scale < getMinimumScale())
			scale = getMinimumScale();
		if (scale > getMaximumScale())
			scale = getMaximumScale();
		
		int margin = (int) (marginSize / scale);

		// check bounds etc.
		int x = (r.x > -margin) ? r.x : -margin;
		int y = (r.y > -margin) ? r.y : -margin;
		x = ((x + r.width) > (id.width + margin)) ? id.width - r.width + margin : x;
		y = ((y + r.height) > (id.height + margin)) ? id.height - r.height + margin : y;

		// set new view rectangle
		if (viewport == null)
			viewport = new Rectangle();

		// set bounds
		// fix aspect ratio and offsets
		if (hrz) {
			int h = (int) ((r.width * d.getHeight()) / d.getWidth());
			viewport.setBounds(x, y, r.width, h);
		} else {
			int w = (int) ((r.height * d.getWidth()) / d.getHeight());
			viewport.setBounds(x, y, w, r.height);
		}
		repaint();

		// send events
		firePropertyChange(Constants.VIEW_CHANGE, null, getViewPosition());

		// TODO:??
		// notifyViewObserve(false);
		// System.out.println("setViewport: "+viewport+" Offset "+getImageRegionOffset(r)+" scl:"+scale);
	}

	/**
	 * Set center of viewer to that location
	 */
	public void setCenterPosition(ViewPosition p) {
		Dimension d = getSize();
		int x = p.x - (int) ((d.getWidth() / 2) / p.scale);
		int y = p.y - (int) ((d.getHeight() / 2) / p.scale);
		setViewPosition(new ViewPosition(x, y, p.scale));
	}


	/**
	 * Get zoom level of the viewer
	 */
	public double getScale() {
		return scale;
	}

	public int getMarginSize() {
		return marginSize;
	}

	public void setMarginSize(int marginSize) {
		this.marginSize = marginSize;
	}

	/**
	 * Do some sanity checking for input rectangle return a "correct" copy
	 * 
	 * @param view
	 * @return
	 */
	private final Rectangle correctRectangle(Rectangle view) {
		Dimension idim = getImageSize();
		return Utils.correctRectangle(idim, view);
	}

	/**
	 * Sometimes the requested image region is smaller then the viewport
	 * requested, this happens for regions at low power. Calculate this region
	 * offset so that it can be drawn in the middle
	 * 
	 * @return
	 */
	public final Point getImageRegionOffset(Rectangle r) {
		Dimension dim = getSize();
		
		Rectangle layout = Utils.getLayoutRectangle(dim, viewport, r);
		// correct for coordinates outside of drawing rectangle
		if (viewport.x < 0 && layout.x == 0) {
			layout.x -= (int) (viewport.x * scale);
		}
		if (viewport.y < 0 && layout.y == 0) {
			layout.y -= (int) (viewport.y * scale);
		}
		/*
		int sw = dim.width;
		int sh = dim.height;

		// see if we need to have an offset
		if (r.width < viewport.width) {
			sw = (int) ((sh * r.width) / r.height);
		}
		if (r.height < viewport.height) {
			sh = (int) ((sw * r.height) / r.width);
		}

		int offx = (int) ((dim.width - sw) / 2);
		int offy = (int) ((dim.height - sh) / 2);
		return new Point(offx, offy);
		*/
		
		return new Point(layout.x,layout.y);
	}

	/**
	 * Convert point from absolute image to view coordinates
	 */
	public Point convertImageToView(Point img) {
		Rectangle r = correctRectangle(viewport);
		Point p = getImageRegionOffset(r);

		// now calculate the offset
		int x = (int) ((img.x - r.x) * scale) + p.x;
		int y = (int) ((img.y - r.y) * scale) + p.y;

		return new Point(x, y);
	}

	/**
	 * Convert point from view to absolute image coordinates
	 */
	public Point convertViewToImage(Point view) {
		Rectangle r = correctRectangle(viewport);
		Point p = getImageRegionOffset(r);

		int x = (int) ((view.getX() - p.x) / scale) + r.x;
		int y = (int) ((view.getY() - p.y) / scale) + r.y;

		return new Point(x, y);
	}

	public void paint(Graphics g) {
		paintComponent(g);
	}

	/**
	 * Overwrite when nothing is loaded
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		// don't bother when nothing is visible
		if (!getViewerComponent().isShowing())
			return;

		// draw component
		Rectangle r = getBounds();
		g.setColor(Color.white);
		g.fillRect(r.x, r.y, r.width, r.height);

		if (hasImage() && viewport != null) {
			// long time = System.currentTimeMillis();
			// manager.drawImageRegion(g,viewport,scale);
			Dimension vs = getSize();
			Dimension is = getImageSize();

			// copy rect
			Rectangle view = Utils.correctRectangle(is, viewport);
			Rectangle layout = Utils.getLayoutRectangle(vs, viewport, view);
			
			// correct for coordinates outside of drawing rectangle
			if (viewport.x < 0 && layout.x == 0) {
				layout.x -= (int) (viewport.x * scale);
			}
			if (viewport.y < 0 && layout.y == 0) {
				layout.y -= (int) (viewport.y * scale);
			}

			/*
			System.out.println("viewer size: "+vs+"\nimage: "+is+
					"\nviewport: "+viewport+"\ncorrected: "+view+
					"\nlayout: "+layout+"\nscale: "+scale+"\n");
			*/
			
			// draw image
			int dx1 = layout.x;
			int dy1 = layout.y;
			int dx2 = layout.x + layout.width;
			int dy2 = layout.y + layout.height;
			int sx1 = view.x;
			int sy1 = view.y;
			int sx2 = view.x + view.width;
			int sy2 = view.y + view.height;

			g.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, Color.white, null);

			// System.out.println("draw image "+(System.currentTimeMillis()-time)+" ms");
			// draw annotations
			if (annotationPanel != null && annotationPanel.isVisible())
				annotationPanel.draw(g);

		} else {
			ImageIcon logo = ViewerFactory.getLogoIcon();
			int x = (int) ((r.width - logo.getIconWidth()) / 2);
			int y = (int) ((r.height - logo.getIconHeight()) / 2);
			g.drawImage(logo.getImage(), x, y, null);
		}
	}

	/**
     * Remove all annotations and zoom out to original position
     */
    public void reset() {
        annotationPanel.getAnnotationManager().removeAnnotations();
        annotationPanel.removeComponents();
        controller.resetZoom();
        if(controlPanel != null)
        	controlPanel.close();
    }

	/**
	 * Get size of pixel in mm. Most proably 1/size will be used to get number
	 * of pixels in millimeter return double size
	 */
	public double getPixelSize() {
		return Constants.APERIO_PXL_SIZE;
	}

	/**
	 * Get misc image specific meta data like compression, scanner, type etc
	 * Could be an empty list, if viewer API doesn't have any meta data to give
	 * 
	 * @return Properties
	 */
	public Properties getImageMetaData() {
		Properties p = new Properties();
		p.setProperty("image.name", getImage());
		Dimension s = getImageSize();
		if (s != null) {
			p.setProperty("image.width", "" + s.width);
			p.setProperty("image.height", "" + s.height);
		}
		return p;
	}

	/**
	 * @return the scalePolicy
	 */
	public ScalePolicy getScalePolicy() {
		return scalePolicy;
	}

	/**
	 * @param scalePolicy
	 *            the scalePolicy to set
	 */
	public void setScalePolicy(ScalePolicy scalePolicy) {
		this.scalePolicy = scalePolicy;
	}

	/**
	 * dispose
	 */
	public void dispose() {
		reset();
		removeComponentListener(this);
		image = null;
		if (magnifier != null) {
			magnifier.dispose();
			magnifier = null;
		}
		info = null;
	}

	/**
	 * Get panel with extra info
	 * 
	 * @return
	 */
	public Container getInfoPanel() {
		return null;
	}

	/**
	 * Convinent method to get AnnotationManager same thing as
	 * getAnnotationPanel().getMarkerManager()
	 * 
	 * @return
	 */
	public AnnotationManager getAnnotationManager() {
		return (annotationPanel != null) ? annotationPanel.getAnnotationManager() : null;
	}

	/**
	 * Create image properties object
	 * 
	 * @return
	 */
	private ImageProperties createImageProperties() {
		Dimension is = getImageSize();
		ImageProperties ip = new ImageProperties();
		ip.setName(imageName);
		ip.setImageSize(is);
		ip.setPixelSize(getPixelSize());
		ip.setTileSize(getImageSize());
		int width = 500;
		int height = 500;
	
		// use original image as thumbnail
		Image thumb = image;
	
		// if original is to big, resize it
		if(is.width > width || is.height > height){
			// fix aspect ratio
			if (ip.isHorizontal())
				height = (width * is.height) / is.width;
			else
				width = (height * is.width) / is.height;
			// resize and wait
			thumb = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
			try{
				MediaTracker tracker = new MediaTracker(this);
				tracker.addImage(thumb, 0);
				tracker.waitForAll();
			}catch(Exception ex){}
		}
		// set
		ip.setThumbnail(thumb);
		return ip;
	}

	/**
	 * Get image properties object This object includes name, image dimensions
	 * and thumbnail image (if avialable)
	 * 
	 * @return ImageProperties object
	 */
	public ImageProperties getImageProperties() {
		return info;
	}

	/**
	 * Get Magnifier panel that is connected to this viewer instance
	 */
	public Magnifier getMagnifier() {
		if (magnifier == null)
			magnifier = new Magnifier(this);
		return magnifier;
	}

	/**
	 * get a regular expression string that will match filanames that are
	 * supported by this viewer. This is based on assumption (not always
	 * correct) that filename extension is a good indicator of a type of image
	 * that it can support Ex: ".*\\.(jpg|jpeg|JPG)" expression should match to
	 * any JPEG image
	 * 
	 * @return regexp
	 */
	public static String getSupportedFormat() {
		return ".*\\.(jpg|jpeg|gif|png)";
	}

	/**
	 * get "type" of the viewer
	 * 
	 * @return
	 */
	public static String getViewerType() {
		return "simple";
	}

	public static void main(String[] args) throws Exception {
		// String pic = "C:\\Documents and Settings\\Eugene Tseytlin\\" +
		// "My Documents\\My Pictures\\Wallpaper\\1388506.jpg";
		//String pic = "/home/tseytlin/Pictures/Wallpaper/NATURE-SunsetInEasternFinland_1600x1200.png";
		String pic = "/home/tseytlin/Pictures/Photographs/ilana outside.jpg";
		
		//String pic = "/home/tseytlin/Pictures/Scenery/nice-model.jpg";
		// String pic =
		// "http://www.treehugger.com/gisele-bundchen-forest-campaign-full.jpg";

		Viewer viewer = new SimpleViewer();
		viewer.getViewerPanel();
		viewer.setSize(500, 500);
		viewer.openImage(pic);
		JOptionPane op = new JOptionPane(viewer.getViewerPanel(),JOptionPane.PLAIN_MESSAGE);
		JDialog d = op.createDialog("Viewer");
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		d.setModal(false);
		d.setResizable(true);
		d.setVisible(true);
		
		// add arrow
		AnnotationManager am = viewer.getAnnotationManager();
		Rectangle r = new Rectangle(0,0,100,100);
		Annotation arrow = am.createAnnotation(AnnotationManager.ARROW_SHAPE,r,Color.yellow,false);
		am.addAnnotation(arrow);
	}

	public void componentHidden(ComponentEvent arg0) {}

	public void componentMoved(ComponentEvent arg0) {}

	public void componentResized(ComponentEvent arg0) {
		// set init viewport
		if(info != null){
			// previous rectangle
			Rectangle r = new Rectangle(getViewRectangle());
			
			// new width and height
			r.width  = (int) (getSize().width / getScale());
			r.height = (int) (getSize().height / getScale());
			
			setViewRectangle(Utils.correctRectangle(info.getImageSize(),r));
		}
		
		//repaint();
	}

	public void componentShown(ComponentEvent arg0) {}
	
	 /**
     * updates the viewer to reflect dynamic changes that were performed
     * on the viewer configuration or the image that is currently loaded
     * Ex: changes to image tranformation can lead to flashing cache as well
     * as image rotation brightening etc.. 
     */
    public void update(){
    	//TODO:
    }
}
