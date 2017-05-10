import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.*;
import edu.pitt.slideviewer.*;
import edu.pitt.slideviewer.markers.Annotation;
import edu.pitt.slideviewer.qview.QuickViewer;


/**
 * initiallize new applet
 * @author tseytlin
 *
 */
public class ViewerApplet extends JApplet {
	private Properties config;
	private Viewer viewer;
	private Map<String,Set<Properties>> annotationMap;
	
	/**
	 * initialize the viewer
	 */
	public void init() {
		super.init();
		setLayout(new BorderLayout());
	
		// get properties configuration
		config = new Properties();
		String s = getParameter("config.url");
		if(s != null){
			try{
				config.load((new URL(s)).openStream());
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
		// setup image server
		ViewerFactory.setProperties(config);
		
		// create virtual microscope panel
		setupViewer(QuickViewer.getViewerType(QuickViewer.APERIO_TYPE));	
		
		// open slide
		String image = getParameter("image.path");
		if(image != null)
			openImage(image);
	}

	public void start() {
		viewer.getViewerController().resetZoom();
		// load properties
		load();
	}
	
	public void setSize(int width, int height) { 
		super.setSize(width,height); 
		//viewer.setSize(width,height);
		validate();
	} 

	/**
	 * load case information (open image)
	 */
	public void closeImage() {
		if(viewer.hasImage()){
			setAnnotationsVisible(false);
			viewer.closeImage();
		}
	}
	
	/**
	 * load case information (open image)
	 */
	public void openImage(String path) {
		closeImage();
		
		// check if we need to switch viewer based on image type
		String type = config.getProperty("image.server.type");
		String rtype = ViewerFactory.recomendViewerType(path);
		if (!type.equals(rtype))
			setupViewer(rtype);

		// load image
		try {
			viewer.openImage(getImageDir()+ path);
			viewer.reset();
		} catch (ViewerException ex) {
			 ex.printStackTrace();
		}
	}
	
	/**
	 * Replace a current viewer w/ a viewer of different type
	 * @param type
	 */

	private void setupViewer(String type) {
		//Dimension dim = getSize();
		
		// remove previous viewer if exists
		if (viewer != null) {
			//dim = viewer.getSize();
			remove(viewer.getViewerPanel());
			viewer.dispose();
			viewer = null;
			System.gc();
		}
		
		// add new viewr
		config.setProperty("image.server.type", type);
		
		// create virtual microscope panel
		viewer = ViewerFactory.getViewerInstance(type);
		//viewer.setSize(dim);
		
		
		// add to viewer
		viewer.getViewerPanel().setBackground(Color.white);
		add(viewer.getViewerPanel(),BorderLayout.CENTER);
		validate();
		
		// setup navigator if necessary
		setupNavigator();
	}
	
	
	private void setupNavigator() {
		// first attempt
		for(Enumeration<Applet> app = getAppletContext().getApplets();app.hasMoreElements();){
			Applet a = app.nextElement();
			if(a instanceof NavigatorApplet){
				((NavigatorApplet) a).setViewer(viewer);
			}
		}
	}

	// return appropriate image directory
	private String getImageDir() {
		String type = config.getProperty("image.server.type");
		if (config.getProperty(type + ".image.dir") != null)
			return config.getProperty(type + ".image.dir");
		return config.getProperty("image.dir");
	}
	

	/**
	 * load varies applet properties
	 * @return
	 */
	
	private void load() {
		// handle viewer options
		if(hasParameter("viewer.initial.position"))
			viewer.setViewPosition(ViewerHelper.parseViewPosition(getParameter("viewer.initial.position")));
		if(hasParameter("viewer.disable.navigation"))
			viewer.getViewerController().setNavigationEnabled(Boolean.parseBoolean(getParameter("viewer.disable.navigation")));
		if(hasParameter("viewer.navigator.enabled"))
			viewer.getViewerControlPanel().setNavigatorEnabled(Boolean.parseBoolean(getParameter("viewer.navigator.enabled")));
		if(hasParameter("viewer.magnifier.enabled"))
			viewer.getViewerControlPanel().setMagnifierEnabled(Boolean.parseBoolean(getParameter("viewer.magnifier.enabled")));
		if(hasParameter("viewer.transforms.enabled"))
			viewer.getViewerControlPanel().setTransformsEnabled(Boolean.parseBoolean(getParameter("viewer.transforms.enabled")));
		if(hasParameter("viewer.navigator.history.enabled") && viewer.getNavigator() != null)
			viewer.getNavigator().setHistoryVisible(Boolean.parseBoolean(getParameter("viewer.navigator.history.enabled")));
		
		// handle image transformations
		Properties p = new Properties();
		if(hasParameter("transform.flip"))
			p.setProperty("transform.flip", getParameter("transform.flip"));
		if(hasParameter("transform.rotate"))
			p.setProperty("transform.rotate", getParameter("transform.rotate"));
		if(hasParameter("transform.contrast"))
			p.setProperty("transform.contrast", getParameter("transform.contrast"));
		if(hasParameter("transform.brightness"))
			p.setProperty("transform.brightness", getParameter("transform.brightness"));
		if(hasParameter("transform.crop"))
			p.setProperty("transform.crop", getParameter("transform.crop"));
		if(!p.isEmpty() && viewer.hasImage()){
			ImageTransform it = new ImageTransform();
			it.setProperties(p);
			viewer.getImageProperties().setImageTransform(it);
			viewer.update();
		}
		
		// handle annotations
		int count = 0;
		try{
			count = Integer.parseInt(getParameter("annotation.count"));
		}catch(Exception ex){}
		// init map
		annotationMap = new HashMap<String, Set<Properties>>();
		// go over annotations
		final String [] fields = new String []
		        {"type","color","tag","name","xstart","ystart","xend","yend",
				 "width","height","xpoints","ypoints","view.x","view.y","view.scale"};
		
		// go over all annotations in applets
		for(int i=0;i<count;i++){
			Properties a = new Properties();
			for(String f : fields){
				String key = "annotation."+i+"."+f;
				if(hasParameter(key)){
					a.setProperty(f,getParameter(key));
				}
			}
			// reset the name
			a.setProperty("name","annotation."+i);
			
			// save inside annotation map
			for(String key: Arrays.asList("name","tag")){
				if(a.containsKey(key)){
					for(String value : a.getProperty(key).split("\\s*,\\s*")){
						Set<Properties> list = annotationMap.get(value.trim());
						if(list == null){
							list = new HashSet<Properties>();
							annotationMap.put(value.trim(),list);
						}
						list.add(a);
					}
				}
			}
		}
		// show all annotations
		setAnnotationsVisible(Boolean.parseBoolean(""+getParameter("annotation.show")));
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if(hasParameter("viewer.navigator.show"))
					viewer.getViewerControlPanel().showNavigatorWindow(Boolean.parseBoolean(getParameter("viewer.navigator.show")));
			}
		});
	}
	
	/**
	 * show/hide all annotations that are associated has been loaded into this applet
	 */
	public void setAnnotationsVisible(boolean b){
		setAnnotationVisible(null, b);
	}
	
	/**
	 * show/hide annotations identified by a name or a tag that has been loaded into this applet
	 */
	public void setAnnotationVisible(String tag, boolean show){
		if(annotationMap == null)
			return;
		
		// if tag is null, then all annotations
		if(tag == null){
			if(show){
				for(String key: annotationMap.keySet()){
					if(key.startsWith("annotation.")){
						for(Properties p: annotationMap.get(key)){
							Annotation a = getAnnotation(p);
							if(a != null)
								viewer.getAnnotationManager().addAnnotation(a);
						}
					}
				}
			}else{
				viewer.getAnnotationManager().removeAnnotations();
			}
		}else{
			for(Properties p: annotationMap.get(tag)){
				if(show){
					Annotation a = getAnnotation(p);
					if(a != null){
						viewer.getAnnotationManager().addAnnotation(a);
						//viewer.setCenterPosition(getCenterPosition(a));
						viewer.setViewPosition(a.getViewPosition());
					}
				}else{
					viewer.getAnnotationManager().removeAnnotation(p.getProperty("name"));
				}
			}
		}
		viewer.repaint();
	}
	
	/**
	 * 
	 * @param tag
	 */
	public void showAnnotationLocation(String tag){
		if(tag != null && annotationMap.containsKey(tag)){
			for(Properties p: annotationMap.get(tag)){
				Annotation a = getAnnotation(p);
				if(a != null){
					//viewer.setCenterPosition(getCenterPosition(a));
					viewer.setViewPosition(a.getViewPosition());
				}	
			}
		}
	}
	
	/**
	 * 
	 * @param tag
	 */
	public void showAnnotation(String tag){
		setAnnotationVisible(tag,true);
	}
	
	
	/**
	 * 
	 * @param tag
	 */
	public void hideAnnotation(String tag){
		setAnnotationVisible(tag,false);
	}
	
	
	/**
	 * set center position of this viewer
	 * @param x
	 * @param y
	 * @param scale
	 */
	public void setCenterPosition(int x, int y, double scale){
		viewer.setCenterPosition(new ViewPosition(x,y,scale));
	}
	
	
	
	/**
	 * get parameter info
	 */
	public String [][] getParameterInfo(){
		return new String [][] {
				// required parameters
				{"config.url", "url", "a path to SlideViewer.conf file that describes server configuration"},
				{"image.path", "string", "a path to virtual slide image"},
								
				// viewer options
				{"viewer.initial.position", "(x,y,scale)","initial viewer position"},
				{"viewer.disable.navigation", "boolean","disable viewer navigation"},
				
				{"viewer.navigator.enabled", "boolean","enable navigator button"},
				{"viewer.magnifier.enabled", "boolean","enable magnifier button"},
				{"viewer.transforms.enabled", "boolean","enable transforms button"},
				{"viewer.navigator.show", "boolean","show navigator window right away"},
				{"viewer.navigator.history.enabled", "boolean","enable navigation history in navigator window"},
				
				// image transformations
				{"transform.flip", "integer","horizontal flip > 0, vertical filp < 0, no flip is 0"},
				{"transform.rotate", "integer","quadrant rotation Ex: 1 = 90' 2 = 180', -1 = -90'  (270')"},
				{"transform.brightness", "float","increase brightness > 0.0, decrease brightness < 0.0"},
				{"transform.contrast", "float","increase contrast > 1.0, decrease contrast < 1.0"},
				{"transform.crop", "(x,y,width,height)","crop a portion of an image, rectangle in absolute slide coordinates"},
	
				// annotations
				{"annotation.show","boolean","show annotations"},
				{"annotation.count","integer","how many annotations are there"},
				
				{"annotation.0.type","string","type of annotation"},
				{"annotation.0.color","string","color of annotation"},
				{"annotation.0.tag","string","annotation tag"},
				{"annotation.0.name","string","name of annotation"},
				{"annotation.0.xstart","string","x offset of annotation"},
				{"annotation.0.ystart","string","y offset of annotation"},
				{"annotation.0.xend","string","x offset of end point"},
				{"annotation.0.yend","string","y offset of end point"},
				{"annotation.0.width","string","width of annotation"},
				{"annotation.0.height","string","height of annotation"},
				{"annotation.0.xpoints","string","a set of x points"},
				{"annotation.0.ypoints","string","a set of y points"},
				{"annotation.0.view.x","string","view x coordinate"},
				{"annotation.0.view.y","string","view y coordinate"},
				{"annotation.0.view.scale","string","view scale"}
		};
	}
	
	
	/**
	 * is there a paramter
	 * @param s
	 * @return
	 */
	private boolean hasParameter(String s){
		return getParameter(s) != null;
	}
	
	/**
	 * Create a tutor marker from ShapeEntry
	 */
	public Annotation getAnnotation(Properties p){
		// create marker
		Annotation marker = null;
		try{
			int shapeType = AnnotationManager.convertType(p.getProperty("type"));
			int xStart = Integer.parseInt(p.getProperty("xstart"));
			int yStart = Integer.parseInt(p.getProperty("ystart"));
			int xEnd = Integer.parseInt(p.getProperty("xend"));
			int yEnd = Integer.parseInt(p.getProperty("yend"));
			int width = Integer.parseInt(p.getProperty("width"));
			int height = Integer.parseInt(p.getProperty("height"));
			int viewX = Integer.parseInt(p.getProperty("view.x"));
			int viewY = Integer.parseInt(p.getProperty("view.y"));
			double viewZoom = Double.parseDouble(p.getProperty("view.scale"));
			Color color = AnnotationManager.convertColor(p.getProperty("color"));
			
			// init annotation
			Rectangle r = new Rectangle(xStart,yStart,width,height);
			marker = viewer.getAnnotationManager().createAnnotation(p.getProperty("name"),shapeType,r,color,false);
			marker.setTag(p.getProperty("tag"));
			marker.setImage(viewer.getImage());
			
			// setup marker
			switch(shapeType){
				case AnnotationManager.ARROW_SHAPE:
				case AnnotationManager.RULER_SHAPE:
					marker.addPoint(xEnd,yEnd);
					break;
				case AnnotationManager.POLYGON_SHAPE:
				case AnnotationManager.PARALLELOGRAM_SHAPE:
					marker.setVertices(p.getProperty("xpoints"),p.getProperty("ypoints"));
				    break;
			}
			  
			//set viewer params
			marker.setViewPosition(new ViewPosition(viewX,viewY,viewZoom));
		}catch(Exception ex){
			ex.printStackTrace();
			return null;
		}
		return marker;	
	}
	
	/**
	 * get viewer instance
	 * @return
	 */
	public Viewer getViewer(){
		return viewer;
	}
	
	
	/**
	 * return a center position of a shape
	 * if shape is a polygon this will use 
	 * polygon's centroid
	 * the scale is shape's scale factor
	 * @return
	 */
	private ViewPosition getCenterPosition(Annotation annotation){
		ViewPosition p = annotation.getCenterPosition();
		boolean arrow = annotation.getType().equalsIgnoreCase("arrow");	
		
		// calculate the best zoom
		Dimension d = viewer.getSize();
		Rectangle r = annotation.getShape().getBounds();
		
		
		// dow something special for arrows
		if(arrow){
			p.x = annotation.getLocation().x;
			p.y = annotation.getLocation().y;
			r.width  = r.width *2;
			r.height = r.height *2;
		}
		
		double zw = d.getWidth()/r.width;
		double zh = d.getHeight()/r.height;
		double z = Math.min(zw,zh);
		
		// adjust based on what is valid
		if(viewer.getScalePolicy() != null){
			z = viewer.getScalePolicy().getValidScale(z);
		}
		
		p.scale = z;
		
		// transform position
		Point l = viewer.getImageProperties().getImageTransform().getTransformedPoint(p.getPoint(),true);
		p.x = l.x;
		p.y = l.y;
				
		return p;
	}
}
