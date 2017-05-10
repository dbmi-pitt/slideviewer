package edu.pitt.slideviewer.markers;
import java.awt.*;
import java.awt.geom.*;

import edu.pitt.slideviewer.qview.connection.Utils;

public class ArrowShape extends Annotation {
	// This class represents arrow shapes.
	private BasicStroke stroke;
	
	//protected double img_x_end;
	//protected double img_y_end;
	
	protected Point arrowPoint = new Point();
	protected Point arrowEndPoint1 = new Point();
	protected Point arrowEndPoint2 = new Point();
	private Point endPoint;
    private Shape line;
    
	static int arrowWidth;
	static int arrowHeight;
	static double arrowHeadAngle;
	static double arrowHeadLength;
	static {
		arrowWidth = 10;
		arrowHeight = 16;
		arrowHeadAngle = Math.atan((double)arrowWidth / (double)arrowHeight);
		arrowHeadLength = Math.sqrt(Math.pow(arrowWidth, 2D) + Math.pow(arrowHeight, 2D));
	}

	/**
	 *
	 * @param x_st
	 * @param y_st
	 * @param x_end - is a width for super.
	 * @param y_end - is a height for super.
	 * @param c
	 * @param hasM
	 */
	public ArrowShape(Rectangle r, Color c, boolean hasM) {
		super(r,c,hasM);
		//Comment out for web course
		stroke = new BasicStroke(LINE_THICKNESS_DEFAULT+2);
		setType("Arrow");
	}
	
	
	/**
     * move shape by dx/dy
     * @param dx
     * @param dy
     */
    public void translate(int dx, int dy){
    	Point e = getEndPoint();
		getBounds().translate(dx,dy);
		// this is overwritten in relevant shapes to set end point
		addPoint(e.x+dx,e.y+dy);
    }

	/*
	public void rescale(double ratio) {
		DoublePoint end_p = imageToView(img_x_end, img_y_end);
		width = Math.abs(left-getXEnd());
		height = Math.abs(top-getYEnd());
	}
	*/

	/*
	public void reshape(double l, double t, double x_end, double y_end) {
		width = Math.abs(left-x_end);
		height = Math.abs(top-y_end);
		left = l;
		top = t;
		DoublePoint dp_end = viewToImage(x_end, y_end);
		img_x_end = dp_end.getX();
		img_y_end = dp_end.getY();
		updateImageCoords();
	}
	*/
	
	
	/**
	 * Just reset the end point
	 * @param x
	 * @param y
	 */
	public void addPoint(int x, int y) {
        addPoint(new Point(x,y));
	}
	
    
    /**
     * Just reset the end point
     * @param x
     * @param y
     */
    public void addPoint(Point p) {
    	endPoint = p;
        Point lp = getLocation();
        
    	// create shape
        if(lp != null)
            line = new Line2D.Double(getLocation(),new Point(endPoint));
        
        // reset bounds
        Rectangle r = getBounds();
        r.width = p.x - r.x; 
        r.height = p.y - r.y; 
      }
    
	/**
	 * Just reset the end point
	 * @param x
	 * @param y
	 */
	public void addRelativePoint(int x, int y) {
		addPoint(viewer.convertViewToImage(new Point(x,y)));
    }
	
     /**
     * Is this Annotation currently in viewer window?
     * @return
     */
    public boolean inView(){
        if(line != null){
            Rectangle r = line.getBounds();
            if(r.width == 0)  r.width = 1;
        	if(r.height == 0) r.height =1;
        	// compensate for transforms
        	Rectangle rect = Utils.getTransformedRectangle(viewer.getViewRectangle(),viewer.getImageProperties());
        	
        	return rect.contains(r) || rect.intersects(r);   
        }else
            return true;
    }
    
	
	/**
	 * Draw arrow polygon
	 */	
	public Polygon makeArrowPoints(Point end_p) {
		Point st_p = getRelativeLocation();
		double theta;
		if(end_p.getX() == st_p.x) {
			if(end_p.getY() > st_p.y)
				theta = 4.7123889803846897D;
			else
				theta = 1.5707963267948966D;
		} else {
			theta = Math.atan((-1D * (double)(st_p.y - end_p.getY())) / (double)(st_p.x - end_p.getX()));
		}
		if(st_p.x > end_p.getX())
			theta += 3.1415926535897931D;

		double alpha1 = theta + arrowHeadAngle;
		double alpha2 = theta - arrowHeadAngle;
		int xp1 = (int)(Math.cos(alpha1) * arrowHeadLength);
		int yp1 = (int)(Math.sin(alpha1) * arrowHeadLength);
		int xp2 = (int)(Math.cos(alpha2) * arrowHeadLength);
		int yp2 = (int)(Math.sin(alpha2) * arrowHeadLength);
		//double length = Math.sqrt(Math.pow(st_p.x - end_p.getX(), 2D) + Math.pow(st_p.y - end_p.getY(), 2D));
		arrowPoint.x = (int)(st_p.x - Math.cos(theta));
		arrowPoint.y = (int)(st_p.y +  Math.sin(theta));
		arrowEndPoint1.x = arrowPoint.x + xp1;
		arrowEndPoint1.y = arrowPoint.y - yp1;
		arrowEndPoint2.x = arrowPoint.x + xp2;
		arrowEndPoint2.y = arrowPoint.y - yp2;
		int xx[] = { arrowPoint.x, arrowEndPoint1.x, arrowEndPoint2.x };
		int yy[] = { arrowPoint.y, arrowEndPoint1.y, arrowEndPoint2.y };
		return new Polygon(xx, yy,3);
	}

	/**
	 * Get end point in absolute coordinates
	 */
	public Point getEndPoint(){
		if(endPoint == null)
			endPoint = super.getEndPoint();
		return endPoint;
	}

	/**
	 * Draw arrow itself
	 */
	public void drawShape(Graphics g) {
        // return if not visible
        if(!isVisible())
            return;
        
		Point st_p = getRelativeLocation();
		Point en_p = getRelativeEndPoint();

		//Comment out for web course
		/* */
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(color);
		g2.setStroke(stroke);
		g2.drawLine(st_p.x,st_p.y, en_p.x,en_p.y);
		g2.fillPolygon(makeArrowPoints(en_p));
		/**/
	}

    public Shape getShape(){
        return line;
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
        int [] x = new int [2], y = new int [2];
        for(PathIterator i = line.getPathIterator(af);!i.isDone();i.next()){
            i.currentSegment(xy);
            if(k < 2){
                x[k] = (int) xy[0];
                y[k] = (int) xy[1];
            }
            k++;
        }
        // reset orientation
        setLocation(new Point(x[0],y[0]));
        addPoint(x[1],y[1]);
    }
    
    
	// add markers
	protected void addMarkers() {
		markers.add(new Marker.ArrowStartMarker(this));
		markers.add(new Marker.ArrowCenterMarker(this));
		markers.add(new Marker.ArrowEndMarker(this));
	}
	
	/**
	 * update bounds if you have rectangles with negative sides
	 */
	public void updateBounds(){
	
	}
}

