package edu.pitt.slideviewer.markers;

import java.awt.*;
import java.awt.geom.PathIterator;

public class PolygonUtils {
	private Polygon poly;
	
	// default buffer = 4
	public PolygonUtils(Polygon p) {
		this.poly = p;
	}

	
	
	/**
	 * get polygon from path iterator
	 * @param i
	 * @return
	 */
	public static Polygon getPolyton(PathIterator i){
		// convert c1 to polygon
		Polygon p = new Polygon();
		double [] xy = new double [6];
		// Returns the coordinates and type of the current path segment in the iteration. 
		// The return value is the path-segment type: SEG_MOVETO, SEG_LINETO, SEG_QUADTO, 
		// SEG_CUBICTO, or SEG_CLOSE. A double array of length 6 must be passed in and can 
		// be used to store the coordinates of the point(s). Each point is stored as a pair 
		// of double x,y coordinates. SEG_MOVETO and SEG_LINETO types returns one point, 
		// SEG_QUADTO returns two points, SEG_CUBICTO returns 3 points and SEG_CLOSE does not return any points.
		while(!i.isDone()){
			int seq = i.currentSegment(xy);
			switch(seq){
			case PathIterator.SEG_MOVETO:
			case PathIterator.SEG_LINETO: 
				p.addPoint((int)xy[0],(int)xy[1]);
				break;
			case PathIterator.SEG_QUADTO: 
				p.addPoint((int)xy[0],(int)xy[1]);
				p.addPoint((int)xy[2],(int)xy[3]);
				break;
			case PathIterator.SEG_CUBICTO: 
				p.addPoint((int)xy[0],(int)xy[1]);
				p.addPoint((int)xy[2],(int)xy[3]);
				p.addPoint((int)xy[4],(int)xy[5]);
				break;	
			}
			i.next();
		}
		return p;
	}
	
	
	
	// return the perimeter
	public double perimeter() {
		double sum = 0.0;
		for (int i = 0; i < poly.npoints; i++){
			int k = (i == poly.npoints-1)?0:i+1;
			sum = sum + distance(poly.xpoints[i],poly.ypoints[i],poly.xpoints[k],poly.ypoints[k]);
		}
		return sum;
	}

	/**
	 * calculate distance between two points
	 * @param x
	 * @param y
	 * @return
	 */
	private double distance(int ax,int ay,int bx, int by){
		int dx = ax - bx;
		int dy = ay - by;
		return Math.sqrt(dx*dx+dy*dy);
	}
	
	// return signed area of polygon
	public double area() {
		double sum = 0.0;
		for (int i = 0; i < poly.npoints; i++) {
			int k = (i == poly.npoints-1)?0:i+1;
			sum = sum + ((double)poly.xpoints[i] * (double)poly.ypoints[k]) - 
						((double)poly.ypoints[i] * (double)poly.xpoints[k]);
		}
		return 0.5 * sum;
	}

	// return the centroid of the polygon
	public Point centroid() {
		double cx = 0.0, cy = 0.0;
		for (int i = 0; i < poly.npoints; i++) {
			int k = (i == poly.npoints-1)?0:i+1;
			cx = cx + ((double)poly.xpoints[i] + (double)poly.xpoints[k])
					* ((double)poly.xpoints[i] * (double)poly.ypoints[k] - 
					   (double)poly.ypoints[i] * (double)poly.xpoints[k]);
			cy = cy + ((double)poly.ypoints[i] + (double) poly.ypoints[k])
					* ((double)poly.xpoints[i] * (double)poly.ypoints[k] - 
					   (double)poly.ypoints[i] * (double)poly.xpoints[k]);
		} 
	
		cx /= (6 * area());
		cy /= (6 * area());
		return new Point((int)cx,(int)cy);
	}

	
	
	public String toString(){
		StringBuffer b = new StringBuffer();
		for (int i = 0; i < poly.npoints; i++) {
			b.append("("+poly.xpoints[i]+","+poly.ypoints[i]+") ");
		}
		return b.toString().trim();
	}
	
	
	// test client
	
	public static void main(String[] args) {
		Polygon p = new Polygon();
		p.addPoint(1,1);
		p.addPoint(1,3);
		p.addPoint(3,3);
		p.addPoint(10,10);
		// a square
		PolygonUtils poly = new PolygonUtils(p);
		System.out.println("polygon    = " + poly);
		System.out.println("perimeter  = " + poly.perimeter());
		System.out.println("area       = " + poly.area());
		System.out.println("centroid   = " + poly.centroid());
	}
	
}