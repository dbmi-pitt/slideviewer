package edu.pitt.slideviewer.qview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import edu.pitt.slideviewer.ImageProperties;
import edu.pitt.slideviewer.Magnifier;
import edu.pitt.slideviewer.qview.connection.Tile;
import edu.pitt.slideviewer.qview.connection.TileConnectionManager;
import edu.pitt.slideviewer.qview.connection.TileManager;
import edu.pitt.slideviewer.qview.connection.Utils;

public class QuickMagnifier extends Magnifier {
	private QuickViewer viewer;
	private Timer fetchTimer;
	private Tile hirezImage;
	
	
	public QuickMagnifier(QuickViewer qv){
		super(qv);
		this.viewer = qv;
		
		fetchTimer = new Timer(200,new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				if(r == null)
					return;
				
				// get a fetch rectangle of interest
				double scl = viewer.getScale();
				
				// make sure we can actually request something
				if(scl > 1/getMagnification())
					return;
				
				// get the right region
				ImageProperties ip = viewer.getImageProperties();
				Point p = viewer.convertViewToImageNoTransform(r.getLocation());
				Dimension d = new Dimension((int)(r.width/scl),(int)(r.height/scl));
									
				// fetch the right tile for this image
				TileManager tm = ((TileConnectionManager)viewer.getConnectionManager()).getTileManager();
				Rectangle r = new Rectangle(p,d);
				hirezImage = tm.fetchTile(Utils.getTransformedRectangle(r,ip),getSize());
				Utils.transofrmTile(hirezImage,r,ip.getImageTransform());
				repaint();
			}
		});
		fetchTimer.setRepeats(false);
	}


	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if(hirezImage != null){
			g.drawImage(hirezImage.getImage(),0,0,getSize().width,getSize().height,Color.white,null);
		}
	}


	public void setRectangle(Rectangle rectangle) {
		super.setRectangle(rectangle);
		
		// restart timer
		fetchTimer.stop();
		fetchTimer.start();
		hirezImage = null;
	}
	
	
	
}
