package edu.pitt.slideviewer.markers;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class CrossShape extends Annotation {
	private BasicStroke stroke,b_stroke,w_stroke;

	// This class represents cross shapes.
	private int offset = 5;
	private int shadow = 3;

	public CrossShape(Rectangle r, Color c, boolean hasM) {
		super(r,c,hasM);
		setType("Cross");
		//Comment out for web course
		stroke = new BasicStroke(LINE_THICKNESS_DEFAULT+1,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
		b_stroke = new BasicStroke(LINE_THICKNESS_DEFAULT+4,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
		w_stroke = new BasicStroke(LINE_THICKNESS_DEFAULT+8,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
		//min_width = 5;
		//min_height = 5;
	}
    
    /**
     * Is this shape resizable
     * @return
     */
    public boolean isResizable(){
        return false;
    }
    

	protected void addMarkers() {
		markers.add(new Marker.CrossCenterMarker(this));
	}

	public void drawShape(Graphics g) {
        // return if not visible
        if(!isVisible())
            return;
        
		// get coordinates
		Point p = getRelativeLocation();
		
		//tutor
		/* */
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		
		//g2.setStroke(stroke);

		//g2.setColor(Color.black);
		//reset
		//g2.drawLine(p.x-offset+shadow, p.y-offset, p.x+offset+shadow, p.y+offset);
		//g2.drawLine(p.x-offset+shadow, p.y+offset, p.x+offset+shadow, p.y-offset);
		g2.setColor(Color.white);
		g2.setStroke(w_stroke);
		g2.drawLine(p.x-offset, p.y-offset, p.x+offset, p.y+offset);
		g2.drawLine(p.x-offset, p.y+offset, p.x+offset, p.y-offset);
		
		
		g2.setColor(Color.black);
		g2.setStroke(b_stroke);
		g2.drawLine(p.x-offset, p.y-offset, p.x+offset, p.y+offset);
		g2.drawLine(p.x-offset, p.y+offset, p.x+offset, p.y-offset);
		
		g2.setColor(color);
		g2.setStroke(stroke);
		g2.drawLine(p.x-offset, p.y-offset, p.x+offset, p.y+offset);
		g2.drawLine(p.x-offset, p.y+offset, p.x+offset, p.y-offset);
		/* */
	}
	
	/*
	public static void main(String [] args) throws Exception{
		BufferedImage img = new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
		Rectangle r = new Rectangle(10,10,10,10);
		CrossShape crs = new CrossShape(r,Color.yellow, false);
		crs.drawShape(img.createGraphics());
		
		// save image
		ImageIO.write(img,"png",new File("/home/tseytlin/CrossCursor.png"));
	}
	*/
}

