package edu.pitt.slideviewer.qview.connection;

import java.awt.*;
import java.awt.image.VolatileImage;
import edu.pitt.slideviewer.*;


/**
 * This is a generic class that handles image layout using tiling
 * 
 * @author tseytlin
 */
public abstract class TileConnectionManager implements ConnectionManager {
	private final double scaleLimit = 0.04;
	protected Tile buffer;
	
	// those methods should be implemented by specific imlimentation
	public abstract void connect(String image) throws Exception;

	// disconnect from image server
	public void disconnect() {
		getTileManager().clearCache();
	}

	/**
	 * Get reference to the drawing buffer
	 * 
	 * @return
	 */
	public Tile getBuffer() {
		if(buffer == null){
			buffer = new Tile();
			createVolatileBufferImage(buffer);
		}
		// check if volatile image is valid
		if(buffer.getImage() instanceof VolatileImage){
			VolatileImage vi = (VolatileImage) buffer.getImage();
			if( vi.validate(getViewer().getViewerComponent().getGraphicsConfiguration()) ==
			    VolatileImage.IMAGE_INCOMPATIBLE){
				createVolatileBufferImage(buffer);
			}
		}
		return buffer;
	}
	
	/**
	 * create volatile buffer image
	 * @param buffer
	 */
	private void createVolatileBufferImage(Tile buffer){
		Viewer v = getViewer();
		Dimension d = v.getSize();
		Image img = v.getViewerComponent().createVolatileImage(d.width,d.height);
		buffer.setImage(img);
	}
	

	public abstract ImageInfo getImageInfo();
	public abstract Viewer getViewer();
	public abstract TileManager getTileManager();

	// dispose of resources
	public void dispose() {
		disconnect();
		getTileManager().dispose();
		buffer = null;
	}

	// /////////////////////////////////////////////////
	// /////////////////////////////////////////////////

	/**
	 * Create low-res image from thumbnail
	 * @param g - graphics where to draw 
	 * @param img  - source image
	 * @param size - size of image in absolute coordinates
	 * @prame rect - rectangular portion of the source image
	 * @param layout - layout dimensions of the image
	 * @return
	 */
	private void drawLowResImage(Graphics g,ImageInfo info, Rectangle r, Rectangle l, Dimension d) {
		// long time = System.currentTimeMillis();
		Image img = info.getThumbnail();
		Dimension idim = info.getImageSize();
		
		// get thumbnail portion
		int w = (int) (r.width * img.getWidth(null) / idim.width);
		int h = (int) (r.height * img.getHeight(null) / idim.height);
		int x = (int) (r.x * img.getWidth(null) / idim.width);
		int y = (int) (r.y * img.getHeight(null) / idim.height);

		// draw and scale
		if(!d.equals(l.getSize())){
			g.setColor(Color.white);
			g.fillRect(0,0,d.width,d.height);
		}
		g.drawImage(img,0,0,l.width,l.height,x,y,x+w,y+h,Color.white,null);
	}
	
	
	/**
	 * Draw zoomed in image (better detail then lowrez
	 * @param g
	 * @param data
	 * @param r
	 * @param scale
	 */
	private void drawZoomedImage(Graphics g, Tile data, Rectangle r, double scale, Rectangle l, Dimension d){
		Rectangle dr = data.getBounds();
		double diff = scale / data.getScale();
		int x = (int) (Math.abs(r.x - dr.x) * data.getScale());
		int y = (int) (Math.abs(r.y - dr.y) * data.getScale());
		int w = (int) (l.width / diff);
		int h = (int) (l.height / diff);
		Image img = Utils.createVolatileImage(data.getImage());
		// under unknown circumstances volatile image can be null
		if(img == null)
			img = data.getImage();
		
		//Image img = Utils.createBufferedImage(data.getImage());
		//g.drawImage(img,l.x,l.y,l.x+l.width,l.y+l.height,x,y,w+x,h+y,Color.white,null);
		if(!d.equals(l.getSize())){
			g.setColor(Color.white);
			g.fillRect(0,0,d.width,d.height);
		}
		g.drawImage(img,0,0,l.width,l.height,x,y,w+x,h+y,Color.white,null);
	}
	
	
	/**
	 * Draw region
	 */
	public void drawImageRegion(Graphics g, Rectangle view, double scale) {
		ImageInfo info = getImageInfo();
		Viewer viewer = getViewer();
		TileManager manager = getTileManager();
		Dimension dim = viewer.getSize();
		long time = System.currentTimeMillis();
		
		// once in a blue moon, maybe info is null
		if(info == null)
			return;
		
		// copy rect
		Rectangle r = Utils.correctRectangle(viewer.getImageSize(), view);
		Rectangle layout = Utils.getLayoutRectangle(dim,view,r);
		
		// draw thumbnail when appropriate
		if (scale == viewer.getMinimumScale()) {
			//g.drawImage(info.getThumbnail(),layout.x, layout.y, null);
			g.drawImage(info.getThumbnail(),layout.x,layout.y,
					    layout.width,layout.height,Color.white,null);
			return;
		}
		
		// get buffer image
		Tile buffer = getBuffer();
		Image img = buffer.getImage();
		
		//image has to be there
		if(img == null)
			return;
		
		// correct for coordinates outside of drawing rectangle
		if(view.x < 0 && layout.x == 0){
			layout.x -= (int)(view.x*scale);
		}
		if(view.y < 0 && layout.y == 0){
			layout.y -= (int)(view.y*scale);
		}
			
		// draw lowrez image
		//drawLowResImage(g,info.getThumbnail(),info.getImageSize(),r,layout);
		if(!r.equals(buffer.getBounds())){
			if(scale > buffer.getScale() && scale > scaleLimit && buffer.getBounds() != null){
				drawZoomedImage(img.getGraphics(),buffer, r,scale,layout,dim);
			}else{
				drawLowResImage(img.getGraphics(),info,r,layout,dim);
			}
		}
		// get buffer (currently only used for bounds)
		buffer.setBounds(r);
		buffer.setScale(scale);
		
		// draw and request tiles
		manager.drawAndRequestTiles(img.getGraphics(),r,scale);
		manager.compactCache(r);
		
		// draw image
		g.drawImage(img,layout.x,layout.y,Color.white,null);
		//g.drawImage(img,0,0,Color.white,null);
		
		Constants.debug("draw time",time);
		Constants.debug("cache size : "+(manager.getCacheSize()/1048576)+" mb");
	}
}
