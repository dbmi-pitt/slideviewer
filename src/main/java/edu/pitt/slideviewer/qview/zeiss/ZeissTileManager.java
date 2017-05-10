package edu.pitt.slideviewer.qview.zeiss;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.net.URL;

import edu.pitt.slideviewer.qview.connection.Communicator;
import edu.pitt.slideviewer.qview.connection.ImageInfo;
import edu.pitt.slideviewer.qview.connection.Tile;
import edu.pitt.slideviewer.qview.connection.TileConnectionManager;
import edu.pitt.slideviewer.qview.connection.TileManager;
import edu.pitt.slideviewer.qview.connection.ImageInfo.PyramidLevel;
import edu.pitt.slideviewer.qview.connection.handler.DataStreamHandler;
import edu.pitt.slideviewer.qview.connection.handler.StreamHandler;


public class ZeissTileManager extends TileManager {
	private Communicator comm;
	private ZeissConnectionManager connection;
	
	
	/**
	 * Initialize tile manager for given connection
	 * @param c
	 */
	public ZeissTileManager(ZeissConnectionManager c){
		this.connection = c;
		comm = Communicator.getInstance();
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
		//long time = System.currentTimeMillis();
		URL server = connection.getServer();
		
		// get scale
		ImageInfo info = connection.getImageInfo();
		
		if(info == null)
			return null;
		
		//Dimension tileSize = info.getTileSize();
		double scale = tileSize.getWidth()/r.width;
		int x = (int) ((r.x+info.getImageOffset().x)*scale);
		int y = (int) ((r.y+info.getImageOffset().y)*scale);
		int w = tileSize.width;
		int h = tileSize.height;
		
			
		 // fetch tile
        StringBuffer command = new StringBuffer();
        command.append("req=get-region&");
        command.append("rect="+x+","+y+","+w+","+h+"&");
        command.append("scale="+scale);
        command.append("&type=unified");
      
        
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
