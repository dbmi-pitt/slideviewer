package edu.pitt.slideviewer.markers;
import java.awt.*;
import java.awt.geom.*;

import edu.pitt.slideviewer.qview.connection.Utils;

public class CircleShape extends Annotation {
	private BasicStroke stroke;
    
	// This class represents circle shapes.
	public CircleShape(Rectangle r, Color c, boolean hasM) {
		super(r,c,hasM);
		setType("Circle");
		stroke = new BasicStroke(LINE_THICKNESS_DEFAULT);
	}

	/**
	 * Determine whether point is inside tutor marker
	 */
	public boolean contains(int x, int y){
		Rectangle r = getBounds();
		Ellipse2D.Double oval = new Ellipse2D.Double((double)r.x,(double)r.y,(double)r.width,(double)r.height);
		return oval.contains((double) x,(double) y);	
	}
	    
	public void drawShape(Graphics g) {
        // return if not visible
        if(!isVisible())
            return;
		// get coordinates
		Rectangle p = getRelativeBounds();
		
		//web course
		p = Utils.correctRectangle(new Rectangle(p));
		
		//Comment out for web course tutor
		/* */
		Graphics2D g2 = (Graphics2D) g;
		g2.setStroke(stroke);
		g2.setColor(color);
		g2.drawOval(p.x, p.y, p.width, p.height);
		/* */

	}

	protected void addMarkers() {
		markers.add(new Marker.CenterMarker(this));
		markers.add(new Marker.NResizeMarker(this));
		markers.add(new Marker.EResizeMarker(this));
	}
	
	/**
	 * Sends the bounds back to listeners through the listener.
	 * Property name is "UpdateShape".
	 */
	public void notifyBoundsChange() {
		super.notifyBoundsChange();
		
		// correct rectangle coordinates
		// this method is called after shape has been morphed
		// simply make sure that the bounds are valid
      	if(imageRect != null){
			if(imageRect.width < 0){
				imageRect.x = imageRect.x + imageRect.width;
				imageRect.width =  -imageRect.width;
			}
			
			if(imageRect.height < 0){
				imageRect.y = imageRect.y + imageRect.height;
				imageRect.height =  -imageRect.height;
			}
      	}
	}
}
