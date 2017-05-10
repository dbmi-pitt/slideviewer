//============================================================================
// Title:	AnnotationManager -- Tracks and draws annotation markers on the
//		current view as directed by the tutor
//
// Created:	27 january 2002 by Christina Schulman
//
//============================================================================

package edu.pitt.slideviewer;

import java.lang.reflect.Field;
import java.awt.*;
import java.util.*;
import java.util.List;

import edu.pitt.slideviewer.markers.*;
import edu.pitt.slideviewer.beans.*;

//import pitt.edu.slidetutor.agent.MessageReceiverRegistry;
//import edu.pitt.slidetutor.pointer.Pointer;
//import edu.pitt.slidetutor.pointer.PointerDisplay;


/**
 * The AnnotationManager class allows annotation markers to be superimposed
 * on the current image view.
 *
 * These markers are colored circles, squares, or arrows that indicate
 * a particular point on the image.  Markers maintain a constant size,
 * regardless of the current zoom level.
 *
 * @author Christina Schulman
 * @version 1.0
 *
 * Redesigned for a new Xippix viewer code. Added movabale markers, PointerDisplay
 * interface.
 * @author Olga
 * @version 2.0
 */
// implements PointerDisplay
public class AnnotationManager { 


	//--------- member variables -----------------
	protected Hashtable<String,Annotation> annotations = null;
	private Viewer viewer = null;
	private int shape_num = 1;
	private String registerAs = "AnnotationManager";


	//------------------ static variables -----------------------
	public final static int	 CIRCLE_SHAPE	= 0;
	public final static int	 SQUARE_SHAPE	= 1;
	public final static int	 ARROW_SHAPE  	= 2;
	public final static int	 POLYGON_SHAPE	= 3;
	public final static int	 CROSS_SHAPE   = 4;
	public final static int	 SIMPLE_ARROW_SHAPE = 5; //disappears under some conditions
	public final static int RULER_SHAPE    = 6;
	public final static int PARALLELOGRAM_SHAPE  = 7;
	public final static int REGION_SHAPE  = 8;
	public final static int TEXT_SHAPE  = 9;

	//------------------ final variables -----------------------
	public final int	LINE_THICKNESS_DEFAULT	= 3;
	public final int	ARROWHEAD_RADIUS_DEFAULT	= 6;
	public final Color	DEFAULT_MARKER_COLOR	= Color.green;

	public final static String crossCursorResource = "/icons/cursor-cross.gif";
	public final static String rulerCursorResource = "/icons/cursor-ruler.gif";
	public final static String drawCursorResource  = "/icons/cursor-draw.gif";
	public final static String rotateCursorResource  = "/icons/cursor-rotate.gif";
	private static Cursor crossCursor,rulerCursor,drawCursor;
	
	
	/**
	 * 
	 * @param regAs - used to diferrentiate the managers for dif views
	 * @param ivpanel_
	 */
	public AnnotationManager(Viewer v) {
		this.viewer = v;
		annotations = new Hashtable<String, Annotation>();
	}

	// release all resources
	public void dispose(){
		removeAnnotations();
		viewer = null;
	}
	
	/**
	 * Name of the class under which it gets registered.
	 * @return String name (usually class name)
	 */	
	public String getRegistrationName(){
		return registerAs;	
	}
	
	
	/**
	 * Tests whether there are currently any visible annotation markers.
	 * @return True if there are any visible markers.
	 */
	public boolean hasAnnotations() {
		boolean bHasMarks = annotations == null ? false : !annotations.isEmpty();
		return bHasMarks;
	}

	/**
	 * Draws all the current markers onto the supplied graphics context.
	 * @param Graphics context onto which the marker shapes should be drawn.
	 */
	public void drawAnnotations( Graphics g ) {
		if ( hasAnnotations() ) {
			/*
			 //this way for some reasons causes ConcurrentModification errors
			for(Annotation marker: annotations.values()){
				marker.draw(g);
			}*/
			for(Enumeration<Annotation> e = annotations.elements();e.hasMoreElements();){
				e.nextElement().draw(g);
			}
		}
	}

	/**
	 * Adds a Annotation to AnnotationManager.
	 * @param Annotation
	 * @return
	 */
	public void addAnnotation( Annotation tm ) {
		annotations.put( tm.getName(), tm );
		viewer.repaint();
	}

	/**
	 * This is backword compatibility method (for Jess)
	 * It creates Annotation from polygon bean and adds it to list of markers.
	 * @param PolygonBean marker info
	 * @return Annotation that was created
	 *	
	public Annotation addAnnotationMarker( PolygonBean pb ) {
		Annotation tm = createTutorMarker(pb,false);
		addTutorMarker(tm);
		viewer.repaint();
		return tm;
	}*/

	/**
	 * Creates a new marker with the given characteristics.
	 * @param name Arbitrary unique string representing this marker.
	 * @param shape  Specifies a square, circular, or arrow marker.
	 * @param Rectangle bounds of this tutor marker s.a. x,y,with and height
	 * @param color  Should be a valid AWT Color. 
	 * @param boolean is this shape movable
	 * @return Annotation that was created
	 */
	public Annotation createAnnotation(String szName, int shape, Rectangle r, Color color, boolean isMovable ) {
		Annotation	marker = null;
		switch ( shape ) {
			case CIRCLE_SHAPE:
			marker = new CircleShape(r, color, isMovable );
			break;

			case SQUARE_SHAPE:
			marker = new RectangleShape(r, color, isMovable );
			break;

			case POLYGON_SHAPE:
			marker = new PolygonShape(r, color, isMovable );
			break;

			case CROSS_SHAPE:
			marker = new CrossShape(r, color, isMovable );
			break;

			case ARROW_SHAPE:
			marker = new ArrowShape( r, color, isMovable );
			break;

			case SIMPLE_ARROW_SHAPE:
			marker = new ArrowShape(r, color, false );
			marker.setType( "SimpleArrow" );
			break;

			case RULER_SHAPE:
			marker = new RulerShape(r, color, isMovable );
			break;
			
			case PARALLELOGRAM_SHAPE:
			marker = new ParallelogramShape(r, color, isMovable );
			break;
			
			case REGION_SHAPE:
			marker = new RegionShape(r, color, isMovable );
			break;
			
			case TEXT_SHAPE:
				marker = new TextShape(r, color, isMovable );
				break;
		}
		marker.setName( szName );
		marker.setViewer(viewer );
		marker.setImage(viewer.getImage());
		return marker;
	}

	/**
	 * Creates a new marker with the given characteristics.
	 * using default location (for sketching) and color
	 * @param shape  Specifies a square, circular, or arrow marker.
	 * @param boolean is this shape movable
	 * @return Annotation that was created
	 */
	public Annotation createAnnotation(int shape, boolean isMovable){
		//Rectangle r = new Rectangle(0,0,10,10);
		Color color = null;
		switch(shape){
			case CROSS_SHAPE:
				color = Color.yellow; break;
			case RULER_SHAPE:
				color = Color.orange; break;
			case POLYGON_SHAPE:
				color = Color.blue; break;	
			case TEXT_SHAPE:
				color = Color.black; break;	
			default:
				color = Color.green;
		}
		return createAnnotation(shape,null,color,isMovable);
	}
	
	/**
	 * Creates a new marker with the given characteristics.
	 * @param shape  Specifies a square, circular, or arrow marker.
	 * @param Rectangle bounds of this tutor marker s.a. x,y,with and height
	 * @param color  Should be a valid AWT Color. 
	 * @param boolean is this shape movable
	 * @return Annotation that was created
	 */
	public Annotation createAnnotation( int shape, Rectangle r, Color color, boolean isMovable ) {
		Annotation tm = createAnnotation("Marker" + shape_num, shape, r, color, isMovable );
		tm.setName(tm.getType()+shape_num);
		shape_num++;
		return tm;
	}

	/**
	 * Create a Annotation from a PolygonBean
	 * @param PolygonBean class that has all of the info
	 * @param boolean can this shape be moved or resized?
	 * @return Annotation that was created
	 **/
	public Annotation createAnnotation( PolygonBean pb, boolean isMovable ) {
		Color color = Color.green;
		String type = pb.getType();
		int shape = convertType(type);
		switch(shape){
			case(RULER_SHAPE):
				color = Color.orange;
				break;
			case(POLYGON_SHAPE):
				color = Color.blue;
				break;
		}
		
		int x = 0;
		int y = 0;
		try {
			x = Integer.parseInt( pb.getXStart() );
			y = Integer.parseInt( pb.getYStart() );
		} catch ( NullPointerException e ) {
		} catch ( NumberFormatException e ) { 
		}
		int img_w = 10;
		int img_h = 10;
		try {
			img_w = Integer.parseInt( pb.getWidth() );
			img_h = Integer.parseInt( pb.getHeight() );
		} catch ( NullPointerException e ) {
		} catch ( NumberFormatException e ) { 
		}
		
		Annotation tm = createAnnotation(pb.getName(),shape,new Rectangle(x,y,img_w,img_h),color,isMovable);
		tm.setTag(pb.getTag());
		tm.setImage(pb.getImage());
		shape_num++;
		switch ( shape ) {
			case ARROW_SHAPE:
			case RULER_SHAPE:
				int x_end = Integer.parseInt( pb.getXEnd() );
				int y_end = Integer.parseInt( pb.getYEnd() );
				tm.addPoint(x_end,y_end);
				break;
			case POLYGON_SHAPE:
			case PARALLELOGRAM_SHAPE:
				String xPoints = pb.getXPoints();
				String yPoints = pb.getYPoints();
				tm.setVertices(xPoints,yPoints);
				//tm = addPolygonPointsStrArgs(tm,xPoints,yPoints);
			break;
		}
		//set viewer params
		try{
			int vx =  Integer.parseInt(pb.getViewX());
			int vy =  Integer.parseInt(pb.getViewY());
			double vz =  Double.parseDouble(pb.getZoom());
			tm.setViewPosition(new ViewPosition(vx,vy,vz));
		}catch(NumberFormatException ex){}
		return tm;
	}

	/**
	 * Helper method to set xy points in Annotation
	 *
	private Annotation addPolygonPointsStrArgs( Annotation tm, String xPoints, String yPoints ) {
		StringTokenizer xt = new StringTokenizer( xPoints, " " );
		StringTokenizer yt = new StringTokenizer( yPoints, " " );
		String x_st;
		String y_st;
		int x_p;
		int y_p;
		while ( xt.hasMoreTokens() ) {
			x_st = xt.nextToken();
			y_st = yt.nextToken();

			try {
				x_p = Integer.parseInt( x_st );
				y_p = Integer.parseInt( y_st );
				(( PolygonShape ) tm ).addPoint(x_p,y_p,false);
			} catch ( NumberFormatException e ) {
				e.printStackTrace();
			}
		}
		return tm;
	}
	*/
	
	/**
	 * Get Annotation with specified name
	 * @param String tutor marker name
	 * @return Annotation marker that was found or null
	 */
	public Annotation getAnnotation( String m_name ) {
		return annotations.get( m_name );
	}

	/**
	 * Get selected TutorMarkers
	 * @return list of Annotation marker that was found or null
	 */
	public List<Annotation> getSelectedAnnotations() {
		ArrayList<Annotation> list = new ArrayList<Annotation>();
		for(Annotation marker: annotations.values()){
			if(marker.isSelected())
				list.add(marker);
		}
		return list;
	}
	
	
	/**
	 * Clean up: remove all the TutoMarkers.
	 */
	public void removeAnnotations() {
		if ( annotations != null ) {
			synchronized(annotations){
				for(Annotation marker: annotations.values()){
					//tm.removePropertyChangeListener( ivpanel_ );
					marker.delete();
				}
				annotations.clear();
			}
		}
		shape_num = 1;
		viewer.repaint();
	}

	/**
	 * Remove selection (highlighting) for all tutor markers
	 */	
	public void deselectAnnotations() {
		if ( annotations != null ) {
			for(Annotation tm: annotations.values()){
				tm.setSelected( false );
			}
		}
	}

	/**
	 * Deletes the marker with the given name.
	 *
	 * @param szName Arbitrary unique string with which the marker to be deleted
	 *               was created.
	 */
	public void removeAnnotation( String szName ) {
		if ( annotations != null ) {
			//Annotation m = ( Annotation ) annotations.get( szName );
			annotations.remove( szName );
			//ivpanel_.repaint();
			viewer.repaint();
		}
	}
	
	/**
	 * Deletes the marker with the given name.
	 *
	 * @param tutor marker Arbitrary unique string with which the marker to be deleted
	 *               was created.
	 */
	public void removeAnnotation( Annotation tm) {
		removeAnnotation(tm.getName());
	}

	/*
	public Pointer getUnPointer( Pointer p ) {
		String methodName = p.getMethodName();
		if ( methodName.equalsIgnoreCase("ADDANNOTATIONMARKER")){
			ArrayList l = p.getArgs();
			PolygonBean pb = ( PolygonBean ) ( l.get( 0 ) );
			Annotation tm = createTutorMarker(pb,false);
			addTutorMarker(tm);
			String markerName = pb.getName();
			ArrayList list = new ArrayList();
			list.add( markerName );
			return new Pointer(registerAs,"removeTutorMarker",list);
		}
		return null;
	}
    */

	public void point( Object o ) {
		if ( o instanceof Annotation ) {
			Annotation tm = ( Annotation ) o;
			annotations.put( tm.getName(), tm );
		}
	}
	
	
	/**
	 * Get a hashtable of all tutor markers contained
	 * in the manager.
	 */	
	public Collection<Annotation> getAnnotations() {
		return annotations.values();
	}
	
    
    /**
     * Show markers for tutor markers
     * (little draggable squares)
     */
    public void setAnnotationMarkersVisible(boolean show){
        for(Iterator i=getAnnotations().iterator();i.hasNext();){
            Annotation tm = (Annotation) i.next();
            tm.setShowMarkers(show);
        }
    }
    
	////////////////////////////////////////
	/**
	 * Get predefined cursor for Annotation
	 * @param int type - the type of shape
	 * @return Cursor
	 */
	public static Cursor getCursor(Annotation marker){
		// if there is a predefined cursor, then return that instead
		if(marker.getCursor() != null)
			return marker.getCursor();
		
		// cross shape
		if(marker instanceof CrossShape){
			if(crossCursor == null)
				crossCursor = createCursor("Cross",crossCursorResource,new Point(10,10));
			return crossCursor;
		// ruler shape	
		}else if (marker instanceof RulerShape){
			if(rulerCursor == null)
				rulerCursor = createCursor("Ruler",rulerCursorResource,new Point(0,0));
			return rulerCursor;
		// any other drawing	
		}else{
			if(drawCursor == null)
				drawCursor = createCursor("Draw",drawCursorResource,new Point(2,16));
			return drawCursor;
		}
	}
	
	
	/**
	 * Get predefined cursor for Annotation
	 * @param int type - the type of shape
	 * @return Cursor
	 */
	public static Cursor getCursor(Marker marker){
        /* east/west resize marker
		if( marker instanceof Marker.ECircleResizeMarker ||
			marker instanceof Marker.EResizeMarker || 
			marker instanceof Marker.WResizeMarker){
			return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
		// north/south resize marker	
		}else if (marker instanceof Marker.SResizeMarker || marker instanceof Marker.NResizeMarker){
			return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
		// any other drawing	
		}else {
			return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
		}*/
        return marker.getCursor();
	}
	
	//create custom cursor that is 32x32
	public static Cursor createCursor(String name,String location,Point p) {
		Toolkit tk = Toolkit.getDefaultToolkit();
		// get cursor image
		Image img = tk.createImage(AnnotationManager.class.getResource(location));
		return tk.createCustomCursor(img,p,name);
	}
	
	/**
	 * This is a helper method that converts color string
	 * to color object. If no suitable color found default color
	 * is returned (Color.green)
	 * @param String color name
	 * @return Color color object
	 */
	public static Color convertColor(String s){
		if ( s == null )
			return Color.green;
		// try to get predefined color
		try {
			// Find the field and value of colorName
			Field field = Class.forName( "java.awt.Color" ).getField(s);
			return ( Color ) field.get(null);
		} catch ( Exception e ) {
			// try to create RGB color
			StringTokenizer st = new StringTokenizer(s," " );
			if ( st.countTokens() != 3 ) {
				//System.err.println( "Can't create Color from " + s );
				return Color.green;
			}
			int[] rgb = new int[ 3 ];
			int i = 0;
			while ( st.hasMoreElements() ) {
				try {
					rgb[ i ] = Integer.parseInt( st.nextToken() );
				} catch ( NumberFormatException ex ) {
					//System.err.println( "Can't create Color from " + s );
					return Color.green;
				}
				i++;
			}
			return new Color( rgb[ 0 ], rgb[ 1 ], rgb[ 2 ] );
		}
	}
	
	/**
	 * This is a helper method that converts color string.
	 * to color object
	 * @param String color name
	 * @return Color color object
	 */
	public static int convertType( String type ) {
		if (type.equalsIgnoreCase( "Oval") || type.equalsIgnoreCase("Circle"))
			return CIRCLE_SHAPE;
		else if (type.equalsIgnoreCase("Rectangle") || type.equalsIgnoreCase("Square"))
			return SQUARE_SHAPE;
		else if ( type.equalsIgnoreCase( "Arrow" ) )
			return ARROW_SHAPE;
		else if ( type.equalsIgnoreCase( "SimpleArrow" ) )
			return SIMPLE_ARROW_SHAPE;
		else if ( type.equalsIgnoreCase( "Polygon" ) )
			return POLYGON_SHAPE;
		else if ( type.equalsIgnoreCase( "Cross" ) )
			return CROSS_SHAPE;
		else if ( type.equalsIgnoreCase( "Ruler" ) )
			return RULER_SHAPE;
		else if ( type.equalsIgnoreCase( "Parallelogram"))
			return PARALLELOGRAM_SHAPE;
		else if ( type.equalsIgnoreCase( "Region"))
			return REGION_SHAPE;
		else if ( type.equalsIgnoreCase( "Text"))
			return TEXT_SHAPE;
		else
			return -1;
	}
}

