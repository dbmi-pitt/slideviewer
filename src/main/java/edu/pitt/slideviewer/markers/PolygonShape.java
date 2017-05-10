package edu.pitt.slideviewer.markers;
import java.awt.*;
import java.awt.geom.*;
import edu.pitt.slideviewer.*;

/**
 * This class represents a not editable free hand drawing with a closed path.
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
public class PolygonShape extends Annotation {
	private BasicStroke stroke;
	//private Polygon view_poly = null;
	private Polygon img_poly  = null;
	private Point lastPoint;
	private double delta = 3.0;

	public PolygonShape(Rectangle r, Color c, boolean hasM) {
		super(r,c,hasM);
		//view_poly = new Polygon();
		img_poly = new Polygon();
		setType("Polygon");
		stroke = new BasicStroke(LINE_THICKNESS_DEFAULT);
	}

	/**
	 * Determine whether point is inside tutor marker
	 */
	public boolean contains(int x, int y){
		return img_poly.contains((double) x,(double) y);	
	}
	
	/**
     * move shape by dx/dy
     * @param dx
     * @param dy
     */
    public void translate(int dx, int dy){
    	img_poly.translate(dx,dy);
    }
	
	/*
	public void setStartPoint(int x, int y) {
		view_poly.addPoint(x, y);
		addImagePoint(x,y);
	}
	*/
	
	/**
	 * Add a vertex of a shape. Uses absolute image coordinates since
	 * Overritten in PolygonShape class (x,y) are view coordinates
	 * @param int x
	 * @param int y
	 */
	public void addRelativePoint(int x, int y) {
		//eliminate num of points (use view coordinates for that)
		Point p = new Point(x,y);
        Point np = viewer.convertViewToImage(p);
          
        if(lastPoint != null){
			double d = Math.pow(lastPoint.x-p.x,2) + Math.pow(lastPoint.y-p.y,2);
			if(Math.sqrt(d) < delta*3){
                // reset last point
				img_poly.xpoints[img_poly.npoints -1] = np.x;
                img_poly.ypoints[img_poly.npoints -1] = np.y;
                //lastPoint = p;
                return;
            }
		}
		lastPoint = p;
        
		// add point to polygon
		img_poly.addPoint(np.x,np.y);
	}
	
    /**
     * Add a vertex of a shape. Uses absolute image coordinates since
     * Overritten in PolygonShape class (x,y) are view coordinates
     * @param int x
     * @param int y
     */
    public void addPoint(int x, int y) {
        addPoint(x,y,true);
    }
    
	
	/**
	 * Add a vertex of a shape. Uses absolute image coordinates since
	 * Overritten in PolygonShape class (x,y) are view coordinates
	 * @param int x
	 * @param int y
     * @param boolean shortcut, try to get rid of redundant points if true
	 */
	public void addPoint(int x, int y, boolean shortcut) {
		//eliminate num of points (use view coordinates for that)
		if(shortcut){
            Point p = viewer.convertImageToView(new Point(x,y));
    		if(lastPoint != null){
    			double d = Math.pow(lastPoint.x-p.x,2) + Math.pow(lastPoint.y-p.y,2);
    			if(Math.sqrt(d) < delta){
    				return;
                }
    		}
    		lastPoint = p;
        }
		// add to polygon
		img_poly.addPoint(x, y);
	}


    /**
     * Get bounds of polygon
     */
    public Rectangle getBounds(){
        if(img_poly.npoints > 0)
            return img_poly.getBounds();
        else
            return null;
    }
 

    
    /**
     * Get real shape of Annotation
     */
    public Shape getShape(){
        return img_poly;
    }
    
    /**
     * Transform shape
     * @param af
     * @param pt
     */
    public void transform(AffineTransform af){
        double [] xy = new double [2];
        // iterate over polygon and save its coordinates in 
        int k = 0;
        for(PathIterator i = img_poly.getPathIterator(af);!i.isDone();i.next()){
            i.currentSegment(xy);
            if(k < img_poly.npoints){
                img_poly.xpoints[k] = (int) xy[0];
                img_poly.ypoints[k] = (int) xy[1];
            }
            k++;
        }
        img_poly.invalidate();
    }
    
    
	/**
	 * Restore polygon from Polygonbean
	 * @param ix
	 * @param iy
	 *
	public void addImageAndViewPoint(int ix, int iy) {
		img_poly.addPoint(ix, iy);
		DoublePoint p = imageToView((double)ix, (double)iy);
		view_poly.addPoint((int)p.getX(), (int)p.getY());
	}

	private void addImagePoint(int x, int y) {
		DoublePoint p = viewToImage(x, y);
		img_poly.addPoint((int)p.getX(), (int)p.getY());
	}
	*/

	/**
	 * nothing to do with not resizable polygon.
	 * @param img_w
	 * @param i_height
	 *
	public void resetViewSize(int img_w, int i_height) { }
	*/
	
	/**
	 * PolygonShape can't be inDrag, only zoom and image position matter
	 *
	public void updateCoords() {
		int s = img_poly.xpoints.length;
		for(int i=0; i<s; i++) {
			DoublePoint p = imageToView((double)img_poly.xpoints[i], (double)img_poly.ypoints[i]);
			view_poly.xpoints[i] = (int)p.getX();
			view_poly.ypoints[i] = (int)p.getY();
		}
	}
	*/
    
    /**
     * Get the last point of this polygon
     */
    public Point getLastVertex(){
        int x = img_poly.xpoints[img_poly.npoints-1];
        int y = img_poly.ypoints[img_poly.npoints-1];
        return new Point(x,y);
    }
    
    /**
     * Get relative last point of this polygon
     */
    public Point getRelativeLastVertex(){
        return viewer.convertImageToView(getLastVertex());
    }
    
	
	/**
	 * get polygon in relative view coordiantes
	 */	
	private Polygon getRelativePolygon(){
		Polygon view_poly = new Polygon();
		for(int i=0; i<img_poly.npoints; i++) {
			Point p = viewer.convertImageToView(new Point(img_poly.xpoints[i],img_poly.ypoints[i]));
			view_poly.addPoint(p.x,p.y);
		}
		return view_poly;
	}
	
	// this is where fun happens
	public void drawShape(Graphics g) {
        //System.err.println("Bounds: "+getBounds()+" "+img_poly.npoints);
        //System.err.println("Relative: "+getRelativeBounds());
        // return if not visible
        if(!isVisible())
            return;
        
		Polygon view_poly = getRelativePolygon();
		
		//web course
		/*
		GraphicsUtil.drawPolygon(g, view_poly, color);
		*/
		//tutor
		/* */
		if(view_poly.npoints > 0){
			Graphics2D g2 = (Graphics2D) g;
			// Enable antialiasing for text
	        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(color);
			g2.setStroke(stroke);
			g2.drawPolygon(view_poly);
		}
		//else
        //    System.err.println("ERROR: No relative polygon, absolute "+img_poly.npoints);
		/* */
	}

	/**
	 * Get x point values in absolute image coordinates.
	 * @return int [] x coordinate
	 */	
	public int[] getXPoints() {
		return img_poly.xpoints;
	}

	/**
	 * Get y point values in absolute image coordinates.
	 * @return int [] y coordinate
	 */	
	public int[] getYPoints() {
		return img_poly.ypoints;
	}

	/**
	 * Get x point values in absolute image coordinates as string
	 * @return String x coordinate string
	 * @deprecated use Annotation.getVerticies()
	 */
	public String getXPointsAsString() {
		return getImgPointsStr(img_poly.xpoints, img_poly.ypoints);
	}
	
	/**
	 * Get y point values in absolute image coordinates as string
	 * @return String y coordinate string
	 * @deprecated use Annotation.getVerticies()
	 */	
	public String getYPointsAsString() {
		return getImgPointsStr(img_poly.ypoints, img_poly.xpoints);
	}

	/**
	 * Eleminates zero point values
	 * @param src
	 * @param other
	 * @return non zero points
	 */
	private String getImgPointsStr(int[] src, int[] other) {
		StringBuffer sb = new StringBuffer();
		int s = src.length;
		for(int i=0; i<s-1; i++) {
			if(src[i] != 0 && other[i] != 0) {
				sb.append(src[i]);
				sb.append(" ");
			}
		}
		if(src[s-1] != 0 && other[s-1] != 0)
			sb.append(src[s-1]);
		return sb.toString();
	}
    
    protected void addMarkers() {
        markers.add(new Marker.PolyCenterMarker(this));
        markers.add(new Marker.PolyNResizeMarker(this));
        markers.add(new Marker.PolyEResizeMarker(this));
        markers.add(new Marker.PolySRotateMarker(this));
        markers.add(new Marker.PolyWRotateMarker(this));
        markers.add(new Marker.PolyEndMarker(this));
    } 
    
    
    /**
	 * get view position of the center that this shape can be
	 * best be viewed at
	 * @return
	 */
	public ViewPosition getCenterPosition(){
		int x = (int) getBounds().getCenterX();
		int y = (int) getBounds().getCenterY();
		double z = getViewPosition().scale;
		
		Rectangle v_size = viewer.getViewRectangle();
        Rectangle s_size = getBounds();
        // if shape isn't entirely contained in
        // the viewer window, then figure out zoom
        if(s_size.width > v_size.width || s_size.height > v_size.height){
            double s1 = v_size.getWidth()/s_size.getWidth();
            double s2 = v_size.getHeight()/s_size.getHeight();
            z = viewer.getScale() * Math.min(s1,s2);
            if(viewer.getScalePolicy() != null){
            	z = viewer.getScalePolicy().getValidScale(z);
            }
        }
    	return new ViewPosition(x,y,z);
	}
}
