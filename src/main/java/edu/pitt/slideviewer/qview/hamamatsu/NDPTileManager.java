package edu.pitt.slideviewer.qview.hamamatsu;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.net.URL;

import edu.pitt.slideviewer.ViewerFactory;
import edu.pitt.slideviewer.qview.connection.Communicator;
import edu.pitt.slideviewer.qview.connection.ImageInfo;
import edu.pitt.slideviewer.qview.connection.Tile;
import edu.pitt.slideviewer.qview.connection.TileConnectionManager;
import edu.pitt.slideviewer.qview.connection.TileManager;
import edu.pitt.slideviewer.qview.connection.handler.DataStreamHandler;
import edu.pitt.slideviewer.qview.connection.handler.StreamHandler;


public class NDPTileManager extends TileManager {
	private Communicator comm;
	private NDPConnectionManager connection;
	private int TILE_JPEG_QUALITY;
	
	/**
	 * Initialize tile manager for given connection
	 * @param c
	 */
	public NDPTileManager(NDPConnectionManager c){
		this.connection = c;
		comm = Communicator.getInstance();
		try{TILE_JPEG_QUALITY = Integer.parseInt(ViewerFactory.getProperty("qview.tile.jpeg.quality"));}catch(Exception ex){}
	}
	
	/**
	 * Dispose of this tile manager
	 */
	public void dispose(){
		super.dispose();
		connection = null;
	}

	public TileConnectionManager getTileConnectionManager() {
		return connection;
	}

	
	public Tile fetchTile(Rectangle r, Dimension tileSize, int timeout) {
		StreamHandler handler = new DataStreamHandler();
		URL server = connection.getServer();
		
		// get scale
		ImageInfo info = connection.getImageInfo();
		
		if(info == null)
			return null;
		
		
		double scale = tileSize.getWidth()/r.width;
		Dimension is = info.getOriginalImageSize();
		
		// calculate positions
		// this is really weird, so far it looks like physical X and Y are in fact
		// a center of the image
		// all XPos and YPos suppose to be in nm and center of an image, hence
		// we need to do a conversion
		
		int pWidth = Integer.parseInt(info.getProperties().getProperty("physical.width"));
		int pHeight = Integer.parseInt(info.getProperties().getProperty("physical.height"));
		int pX = Integer.parseInt(info.getProperties().getProperty("physical.x"));
		int pY = Integer.parseInt(info.getProperties().getProperty("physical.y"));
		double sourceLens = Double.parseDouble(info.getProperties().getProperty("source.lens"));

		
		// convert pixel top-left-corner to nm in top-left-corner
		long xpos = ((long)r.x * pWidth  / is.width);
		long ypos = ((long)r.y * pHeight / is.height);
		
		// lets convert it to a center of requested region
		xpos = xpos + (long)(tileSize.width/(2*scale)*pWidth /is.width);
		ypos = ypos + (long)(tileSize.height/(2*scale)*pHeight /is.height);
		
		// translate in respect to center of image
		// and original image offset
		xpos = (xpos - pWidth/2) + pX;
		ypos = (ypos - pHeight/2) + pY;
		
		// calculate lens
		double lens = scale*sourceLens;
		
		 // fetch tile
        StringBuffer command = new StringBuffer();
        command.append("nspGetImage?");
        command.append("ItemID="+info.getProperties().getProperty("ItemID")+"&");
        command.append("FrameWidth="+tileSize.width+"&");
        command.append("FrameHeight="+tileSize.height+"&");
        command.append("XPos="+xpos+"&");
        command.append("YPos="+ypos+"&");
        command.append("Lens="+lens+"&");
        command.append("Quality="+((TILE_JPEG_QUALITY>0)?TILE_JPEG_QUALITY:75));
		
        Tile tile = null;
        if(comm.queryServer(server,""+command,handler,timeout)){
        	tile = new Tile();
        	tile.setData((byte []) handler.getResult());
            tile.setBounds(r);
            tile.setScale(scale);
        }
        handler = null;
		return tile;
	}

}
