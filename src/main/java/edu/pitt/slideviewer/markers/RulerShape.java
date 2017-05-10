/**
 * This class allows creation of a ruler on the slide
 * Author: Eugene Tseytlin (University of Pittsburgh)
 */

package edu.pitt.slideviewer.markers;


import java.awt.*;
import java.awt.geom.*;
import java.text.NumberFormat;
import javax.swing.UIManager;

import edu.pitt.slideviewer.qview.connection.Utils;

/**
 * This is a ruler that can be used for measurements
 * based on Arrow code
 *
 */
public class RulerShape extends Annotation {
	private BasicStroke stroke;
	// preset MM to pxl steps
	public static final int INTERSCOPE_STEP = 2893;
	public static final int APERIO_STEP = 2116;
	
	private int pxlPerMm = -1;  //2116 round it to 2100   // not exact 1875
	private Point endPoint;
	private Shape line;
    private boolean showTotal;
	
	
	// text font for measurements
	private static Font textFont = ((Font)UIManager.getLookAndFeelDefaults().get("TextPane.font")).deriveFont(Font.BOLD);
	
	/**
	 * How many pixels in absolute coordinates fit in mm.
     * @deprecated This should be done automaticly now
     * by utilizing Viewer.getPixelSize()
	 */	
	public static void setUnitStep(int step){
		//pxlPerMm = step;	 
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
	public RulerShape(Rectangle r, Color c, boolean hasM) {
		super(r,c,hasM);
		
		//Comment out for web course
		//stroke = new BasicStroke(LINE_THICKNESS_DEFAULT+4);
		stroke = new BasicStroke(LINE_THICKNESS_DEFAULT+4,BasicStroke.CAP_ROUND,BasicStroke.JOIN_MITER);
		setType("Ruler");
				
		// reset color
		color = Color.orange;
  	}

	/*
	public void rescale(double ratio) {
		Point2D end_p = imageToView2(img_x_end, img_y_end);
		width = Math.abs(left-getXEnd());
		height = Math.abs(top-getYEnd());
	}
	*/	
	/**
	 * Get end point in absolute coordinates
	 */
	public Point getEndPoint(){
		if(endPoint == null)
			endPoint = super.getEndPoint();
		return endPoint;
	}
	
	/**
	 * Get end point
	 */	
	public Point getRelativeEndPoint(){
		Point p = getEndPoint();
		if(p != null)
			return viewer.convertImageToView(p);
		else
			return null;
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
	public void reshape(double l, double t, double x_end, double y_end) {
		width = Math.abs(left-x_end);
		height = Math.abs(top-y_end);
		left = l;
		top = t;
		Point2D dp_end = viewToImage2(x_end, y_end);
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
        // create shape
        if(getLocation() != null)
            line = new Line2D.Double(getLocation(),endPoint);
        
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
    
	
	/*
	public Point2D imageToView2(double imgX, double imgY) {
		Point p = viewer.convertImageToView(new Point((int) imgX,(int) imgY));
		return new Point2D.Double(p.x,p.y);	
	}

	public Point2D viewToImage2(double viewX, double viewY) {
		Point p = viewer.convertViewToImage(new Point((int) viewX,(int) viewY));
		return new Point2D.Double(p.x,p.y);		
		//return new Point2D.Double(viewer.calcImageX(viewX), viewer.calcImageY(viewY));
	}
	*/
    
    public void setShowTotalMeasurement(boolean b){
    	showTotal = b;
    }
    
	
	// draw shape
	public void drawShape(Graphics g) {
        // return if not visible
        if(!isVisible())
            return;
         
		Graphics2D g2 = (Graphics2D) g;

		Point st_p = getRelativeLocation();
		Point en_p = getRelativeEndPoint();
		
			
		// don't draw if end point is null
		if(en_p == null || st_p == null)
			return;
		
		
		// don't draw if points are the same
		if(st_p.x == en_p.x && st_p.y == en_p.y)
			return;
	
		
		// Enable antialiasing for shapes
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    
         // Enable antialiasing for text
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    			
		// Draw Main Line
		g2.setColor(color);
		g2.setStroke(stroke);
		g2.drawLine(st_p.x, st_p.y, en_p.x, en_p.y);
		
		// setup step function if needed
        if(pxlPerMm == -1)
            pxlPerMm = (int) (1.0/viewer.getPixelSize());
        
        // calculate how many pixels corespond to 1 mm
		float step = Math.round(pxlPerMm*(float)viewer.getScale());
		
		//	System.out.println(step+" pixels per mm "+viewer.getScale());
		
		// perhaps this needs to be in CM?
		String units = "mm";
		if(step <= 10){
			step *= 10;
			units = "cm";
		}	
		
		// print debug info
		//double d = Math.round(Math.sqrt(Math.pow(st_p.x-en_p.x,2)+Math.pow(st_p.y-en_p.y,2)));
		//System.out.println("("+st_p.x+","+st_p.y+")-("+en_p.x+","+en_p.y+") d="+d+
		//				    " mm="+(int)(100*d/pxlPerMm)/100.0+" STEP="+pxlPerMm);
		
		// draw numbers
		//////////////////////////////////
		
		// calculate length and angle of the line
		double length = Math.sqrt(Math.pow(en_p.getX()-st_p.getX(),2)+Math.pow(en_p.getY()-st_p.getY(),2));
		double angle  = Math.atan((en_p.getY()-st_p.getY())/(en_p.getX()-st_p.getX()));
		
		// setup location  increments and initial coordinates
		double stepX = (en_p.getX() - st_p.getX())*step/length;
		double stepY = (en_p.getY() - st_p.getY())*step/length;
		double x = st_p.x, y = st_p.y;
		float i = 0,inc = 1;
	
		// rotate text
		AffineTransform fontAT = new AffineTransform();
        fontAT.rotate(angle);
		g2.setFont(textFont.deriveFont(fontAT));
		
		// put .1 marks
		//if(viewer.getScale() >  0.33){
		if(step >= 500){
			stepX /= 10;
			stepY /= 10;
			inc = 0.1f;
		// put. 5 marks
		//}else if(viewer.getScale() >  0.12){
		}else if(step >= 100){	
			stepX /= 2;
			stepY /= 2;
			inc = 0.5f;
		//}else if(viewer.getScale() < 0.015){
		}else if(step < 5){	
			stepX *= 5;
			stepY *= 5;
			inc = 5.0f;
		}
		
		// for number formatting
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(1);
		
		// background "line" stroke setup
		int fontHeight = g2.getFontMetrics(textFont).getHeight();
		g2.setStroke(new BasicStroke(fontHeight,BasicStroke.CAP_ROUND,BasicStroke.JOIN_MITER));
				
		// offsets for units
		double xi = 15 * Math.sin(angle);
		double yi = 15 * Math.cos(angle);
		
		// offsets for background line
		double xiii = (17-fontHeight/2) * Math.sin(angle);
		double yiii = (17-fontHeight/2) * Math.cos(angle);
		int len = g2.getFontMetrics(textFont).stringWidth(units); 
		
		// draw background line
		g2.setColor(Color.white);
		g2.drawLine((int)(x-xiii),(int)(y+yiii),(int)(x-xiii+len*Math.cos(angle)),(int)(y+yiii+len*Math.sin(angle)));
		
		// draw measurement unit string
		g2.setColor(Color.black);
		
		// which units?
		g2.drawString(units,(int) (x-xi),(int) (y+yi));
		
		// draw total measurment
		if(showTotal){
			NumberFormat nform = NumberFormat.getInstance();
			nform.setMaximumFractionDigits(2);
			String totalStr = nform.format(length/step);
			
			// offsets for background line
			int lenT = g2.getFontMetrics(textFont).stringWidth(totalStr); 
			
			int ex = en_p.x;
			int ey = en_p.y;
			
			// draw background line
			g2.setColor(Color.white);
			g2.drawLine((int)(ex-xiii),(int)(ey+yiii),(int)(ex-xiii+lenT*Math.cos(angle)),(int)(ey+yiii+lenT*Math.sin(angle)));
			
			// draw measurement unit string
			g2.setColor(Color.black);
			g2.drawString(totalStr,(int)(ex-xi),(int)(ey+yi));
		}
		
		
		// offsets for background line
		xiii = (3+fontHeight/2) * Math.sin(angle);
		yiii = (3+fontHeight/2) * Math.cos(angle);
		
		// offsets for numbers
		double xii = 5 * Math.sin(angle);
		double yii = 5 * Math.cos(angle);
		
		// now iterate over marked numbers
		while((stepX >0 && x < en_p.x) || (stepX <0 && x > en_p.x) || 
		 	  (stepY >0 && y < en_p.y) || (stepY <0 && y > en_p.y)){
				  
			String nm = nf.format(i);
			len = g2.getFontMetrics(textFont).stringWidth(nm); 
			
			// draw background
			g2.setColor(Color.white);
			g2.drawLine((int)(x+xiii),(int)(y-yiii),(int)(x+xiii+len*Math.cos(angle)),(int)(y-yiii+len*Math.sin(angle)));
			
			// draw number
			g2.setColor(Color.black);
			g2.drawString(nm,(int)(x+xii),(int)(y-yii));
			
			// increment			
			x += stepX;
			y += stepY;
			i+=inc;
		}
			
		///draw MM tick marks
		g2.setColor(Color.black);
		float [] dash = new float [] {1.0f,step};
		g2.setStroke(new BasicStroke(10.0f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,1.0f,dash,0.0f));
		g2.drawLine(st_p.x, st_p.y, en_p.x, en_p.y);
		
		// draw 1/10 MM tick marks
		//if(viewer.getScale() >=0.015){
		if(step >= 30){	
			float step2 = (step-9)/10;    // we need to subtract 9 to compensate for 9 lines of length 1
			dash = new float [] {1.0f,step2};
			g2.setStroke(new BasicStroke(5.0f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,1.0f,dash,0.0f));
			g2.drawLine(st_p.x, st_p.y, en_p.x, en_p.y);
		
			// draw 1/100 MM tick mark
			//if(viewer.getScale() >= 0.25){
			if(step >= 500){
				float step3 = (step2-9)/10;
				dash = new float [] {1.0f,step3};
				g2.setStroke(new BasicStroke(2.0f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,1.0f,dash,0.0f));
				g2.drawLine(st_p.x, st_p.y, en_p.x, en_p.y);
			}
		}	
	}
    
    /**
     * Get real shape of Annotation
     */
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

