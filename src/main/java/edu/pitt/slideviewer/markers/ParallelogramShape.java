package edu.pitt.slideviewer.markers;
import java.awt.*;
import java.awt.geom.*;

public class ParallelogramShape extends Annotation {
	private AffineTransform transform;
	private BasicStroke stroke;
	private Polygon rectangle;
	
	// This class represents rectangle shapes.
	public ParallelogramShape(Rectangle r, Color c, boolean hasM) {
		super(r,c,hasM);
		setType("Parallelogram");
		stroke = new BasicStroke(LINE_THICKNESS_DEFAULT);
		rectangle = new Polygon();
	}

	/**
     * move shape by dx/dy
     * @param dx
     * @param dy
     */
    public void translate(int dx, int dy){
    	rectangle.translate(dx,dy);
    }
	
	/**
	 * Determine whether point is inside tutor marker
	 */
	public boolean contains(int x, int y){
		return rectangle.contains(x,y);	
	}

	/**
     * Get bounds of polygon
     */
    public Rectangle getBounds(){
    	if(rectangle.npoints > 0)
        	return rectangle.getBounds();
        else
        	return super.getBounds();
    }

    /**
	 * Get location of Annotation in absolute image coordinates
	 * @return Point shape location
	 */
	public Point getLocation(){
        if(rectangle.npoints > 0)
            return new Point(rectangle.xpoints[0],rectangle.ypoints[0]);
        return super.getLocation();
	}
    
    /**
     * constract rectangle when it is drawn
     */
    public void addPoint(int x, int y){
    	Point p = getLocation();
    	// traditional
		rectangle.reset();
	    rectangle.addPoint(p.x,p.y);
        rectangle.addPoint(x,p.y);
        rectangle.addPoint(x,y);
        rectangle.addPoint(p.x,y);
    }
	
    /**
     * Get real shape of Annotation
     */
    public Shape getShape(){
        return rectangle;
    }
    
    
    /**
     * Transform shape
     * @param af
     * @param pt
     */
    public void transform(AffineTransform af){
    	if(af == null)
    		return;
    	
    	// save transform
    	if(transform == null)
    		transform = af;
    	else
    		transform.concatenate(af);
    	
    	transformShape(af);
    	
    }
    
    private void transformShape(AffineTransform af){
    	double [] xy = new double [2];
        // iterate over polygon and save its coordinates in 
        int k = 0;
        for(PathIterator i = rectangle.getPathIterator(af);!i.isDone();i.next()){
            i.currentSegment(xy);
            if(k < rectangle.npoints){
            	rectangle.xpoints[k] = (int) xy[0];
            	rectangle.ypoints[k] = (int) xy[1];
            }
            k++;
        }
        rectangle.invalidate();
    }
    
    /**
     * make sure edges are not too close
     * @return
     
    private boolean checkSize(){
    	final int limit = 100;
    	for(int i=1;i<rectangle.npoints;i++){
    		int d1 = rectangle.xpoints[i] - rectangle.xpoints[i-1];  
    		int d2 = rectangle.ypoints[i] - rectangle.ypoints[i-1];
    		int d = (Math.abs(d1) > Math.abs(d2))?d1:d2;
    		if(Math.abs(d) < limit)
    			return false;
    	}
    	return true;
    }
    */
    
    /**
     * resize parallellogram
     */ 
    public void resize(int dw, int dh){
    	// check for size
    	//if(!checkSize())
    	//	return; 
    	
    	//Rectangle r = getBounds();
    	//int x = (int) r.getCenterX();
    	//int y = (int) r.getCenterY();
    	if(transform != null){
    		try{
    			transformShape(transform.createInverse());
    		}catch(NoninvertibleTransformException ex){
    			ex.printStackTrace();
    		}
    	}
    	
    	for(int i=0;i<rectangle.npoints;i++){
    		//rectangle.xpoints[i] += (rectangle.xpoints[i] > x)?dw:-dw;
    		//rectangle.ypoints[i] += (rectangle.ypoints[i] > y)?-dh:dh; 
    		rectangle.xpoints[i] += (i>0 && i<3)?dw:-dw;
    		rectangle.ypoints[i] += (i<2)?dh:-dh; 
    	}
    	rectangle.invalidate();
    	
    	if(transform != null)
    		transformShape(transform);
    }
    
    
    /**
	 * get polygon in relative view coordiantes
	 */	
	public Polygon getRelativePolygon(){
		//updateRectangle(getBounds());

		Polygon view_poly = new Polygon();
		for(int i=0; i<rectangle.npoints; i++) {
			Point p = viewer.convertImageToView(new Point(rectangle.xpoints[i],rectangle.ypoints[i]));
			view_poly.addPoint(p.x,p.y);
		}
		return view_poly;
	}
    
	public void drawShape(Graphics g) {
        // return if not visible
        if(!isVisible())
            return;
        
        // get relative        
		Polygon view_poly = getRelativePolygon();
		
		// 
		Graphics2D g2 = (Graphics2D) g;
	      
        // Enable antialiasing for text
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
       
		g2.setColor(color);
		g2.setStroke(stroke);
		if(view_poly.npoints > 0)
			g2.drawPolygon(view_poly);
		
        
		/*
		Rectangle r = getRelativeBounds();
		Graphics2D g2 = (Graphics2D) g;
		g2.setStroke(stroke);
		g2.setColor(color);
		g2.drawRect(r.x,r.y,r.width,r.height);
		*/
	}

	protected void addMarkers() {		
		markers.add(new Marker.PolyCenterMarker(this));
		markers.add(new Marker.ParallelogramNResizeMarker(this));
		markers.add(new Marker.ParallelogramEResizeMarker(this));
		markers.add(new Marker.ParallelogramSResizeMarker(this));
		markers.add(new Marker.ParallelogramWResizeMarker(this));
		markers.add(new Marker.ParallelogramRotateMarker(this));
		//markers.add(new Marker.ParallelogramWRotateMarker(this));
	
	}

	/**
	 * @return the transform
	 */
	public AffineTransform getTransform() {
		return transform;
	}
}
