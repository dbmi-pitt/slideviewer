package edu.pitt.slideviewer.markers;
import java.awt.*;

import edu.pitt.slideviewer.Constants;
import edu.pitt.slideviewer.qview.connection.Utils;

public class RectangleShape extends Annotation {
	private BasicStroke stroke;
	
	// This class represents rectangle shapes.
	public RectangleShape(Rectangle r, Color c, boolean hasM) {
		super(r,c,hasM);
		setType("Rectangle");
		stroke = new BasicStroke(LINE_THICKNESS_DEFAULT);
	}

	/**
	 * Determine whether point is inside tutor marker
	 */
	public boolean contains(int x, int y){
		//Rectangle r = new Rectangle(getImgXSt(),getImgXSt(),getImgWidth(),getImgHeight());
		return getBounds().contains(x,y);	
	}
	
	public void drawShape(Graphics g) {
        // return if not visible
        if(!isVisible())
            return;
        
		//Comment out for web course tutor
		Rectangle r = getRelativeBounds();
		
		// correct rectangle
        r = Utils.correctRectangle(new Rectangle(r));
        
		Graphics2D g2 = (Graphics2D) g;
		g2.setStroke(stroke);
		g2.setColor(color);
		g2.drawRect(r.x,r.y,r.width,r.height);
	}

	protected void addMarkers() {
		markers.add(new Marker.CenterMarker(this));
		markers.add(new Marker.NResizeMarker(this));
		markers.add(new Marker.SResizeMarker(this));
		markers.add(new Marker.WResizeMarker(this));
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

