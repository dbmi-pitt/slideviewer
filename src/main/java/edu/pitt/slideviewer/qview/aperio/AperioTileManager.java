package edu.pitt.slideviewer.qview.aperio;
import java.awt.*;

import edu.pitt.slideviewer.ViewerFactory;
import edu.pitt.slideviewer.qview.connection.*;
import edu.pitt.slideviewer.qview.connection.handler.DataStreamHandler;
import edu.pitt.slideviewer.qview.connection.handler.StreamHandler;

import java.net.*;


public class AperioTileManager extends TileManager {
	private Communicator comm;
	private AperioTileConnectionManager connection;
	private int TILE_JPEG_QUALITY;
	
	/**
	 * Initialize tile manager for given connection
	 * @param c
	 */
	public AperioTileManager(AperioTileConnectionManager c){
		this.connection = c;
		comm = Communicator.getInstance();
		//handler = connection.getDataHandler();
		try{TILE_JPEG_QUALITY = Integer.parseInt(ViewerFactory.getProperty("qview.tile.jpeg.quality"));}catch(Exception ex){}
	}
	
	/**
	 * Dispose of this tile manager
	 */
	public void dispose(){
		super.dispose();
		//handler = null;
		connection = null;
	}

	/**
	 * Get info about image
	 */
	public TileConnectionManager getTileConnectionManager() {
		return connection;
	}
	
	/**
	 * Grant work of fetching tiles
	 */
	public Tile fetchTile(Rectangle r, Dimension tileSize, int timeout) {
		StreamHandler handler = new DataStreamHandler();
		//long time = System.currentTimeMillis();
		URL server = connection.getServer();
		
		// get scale
		ImageInfo info = connection.getImageInfo();
		
		if(info == null)
			return null;
		
		//Dimension tileSize = info.getTileSize();
		double scale = tileSize.getWidth()/r.width;
		
		 // fetch tile
        String x = (r.x >= 0)?"0"+r.x:""+((int)(r.x*scale));
        String y = (r.y >= 0)?"0"+r.y:""+((int)(r.y*scale));
        String w = ""+tileSize.width;
        String h = ""+tileSize.height;
        String z = ""+(1/scale);
        String q = (TILE_JPEG_QUALITY > 0)?"+"+TILE_JPEG_QUALITY:"";
        
        Tile tile = null;
        if(comm.queryServer(server,x+"+"+y+"+"+w+"+"+h+"+"+z+q,handler,timeout)){
        	// get tile offset
        	//int [] idx = getTileOffset(r);
        	
        	tile = new Tile();
            //tile.setImage((Image) handler.getResult());
        	tile.setData((byte []) handler.getResult());
            tile.setBounds(r);
            tile.setScale(scale);
           
            //tile.setOffset(new Point(idx[0],idx[1]));
    		//tile.setPyramidLevel(idx[2]);
            //System.out.println("fetched tile in "+(System.currentTimeMillis()-time)+" ms");
        }
        handler = null;
		return tile;
	}
	

}
