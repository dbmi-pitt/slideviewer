package edu.pitt.slideviewer.markers;
import java.awt.*;

public class RegionShape  extends Annotation {
	private final int shade = 30;
	// This class represents rectangle shapes.
	public RegionShape(Rectangle r, Color c, boolean hasM) {
		super(r,c,hasM);
		setType("Region");
		Color y = Color.yellow;
		selectionColor = new Color(y.getRed(),y.getGreen(),y.getBlue(),shade*3);  // Selection color
	}
	
	
	/**
	 * Make color transparent by default
	 */
	public void setColor(Color c){
		if(c.getTransparency() == Transparency.OPAQUE)
			c = new Color(c.getRed(),c.getGreen(),c.getBlue(),shade);
		super.setColor(c);
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
        Rectangle r = getRelativeBounds();
        g.setColor(color);
		g.fillRect(r.x,r.y,r.width,r.height);
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
