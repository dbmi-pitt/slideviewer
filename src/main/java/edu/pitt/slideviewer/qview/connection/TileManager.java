package edu.pitt.slideviewer.qview.connection;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

import edu.pitt.slideviewer.Constants;
import edu.pitt.slideviewer.ImageProperties;
import edu.pitt.slideviewer.ViewerFactory;

/**
 * This class manages tile requests
 * 
 * @author tseytlin
 */
public abstract class TileManager {
	// some constants
	private int NUMBER_OF_FETCHERS = 1;
	private int SHORT_TIMEOUT = 2000;
	private int LONG_TIMEOUT  = 5000;
	
	//protected TileFetcher fetcher;
	protected List<TileFetcher> fetchers;
	protected TileRenderer renderer;
	private Rectangle tiledRectangle;
	private Map<String,Tile> cache = new HashMap<String,Tile>();
	private Map<Rectangle,Boolean> blacklist = new HashMap<Rectangle,Boolean>();
	private int fetcherOffset;
	
	/**
	 * default constractor
	 */
	public TileManager() {
		// start renderer
		renderer = new TileRenderer();
		renderer.start();
		
		// check property for number of fetchers
		try{NUMBER_OF_FETCHERS = Integer.parseInt(ViewerFactory.getProperty("qview.concurrent.tile.requests"));	}catch(Exception ex){}
		try{SHORT_TIMEOUT = Integer.parseInt(ViewerFactory.getProperty("qview.tile.request.short.timeout"));	}catch(Exception ex){}
		try{LONG_TIMEOUT = Integer.parseInt(ViewerFactory.getProperty("qview.tile.request.long.timeout"));	    }catch(Exception ex){}
		if(NUMBER_OF_FETCHERS < 1)
			NUMBER_OF_FETCHERS = 1;
		
		// load the fetchers
		fetchers = new ArrayList<TileFetcher>();
		for(int i=0; i<NUMBER_OF_FETCHERS; i++){
			TileFetcher f = new TileFetcher();
			f.start();
			fetchers.add(f);
		}
		
	}
	
	
	/**
	 * get fetcher
	 * @return
	 */
	private TileFetcher getFetcher(){
		// figure out the best fetcher to use 
		if(fetcherOffset >= fetchers.size())
			fetcherOffset = 0;
		return fetchers.get(fetcherOffset++);
	}
	
	/**
	 * dispose of all fetchers
	 */
	private void disposeFetchers(){
		if(fetchers != null){
			for(TileFetcher tf: fetchers){
				tf.dispose();
			}
			fetchers.clear();
			fetchers = null;
		}
	}
	

	/**
	 * Clear cache, delete all cache information
	 */
	public void clearCache() {
		cache.clear();
		blacklist.clear();
	}
	
	/**
	 * Delete non-escentual cache information
	 * s.a tile images or not frequently used tiles
	 */
	public void compactCache(Rectangle rect){
		Rectangle view = new Rectangle(rect);
		//long time = System.currentTimeMillis();
		// slightly expand the viewing window
		view.grow(view.width/2, view.height/2);
		
		// copy list
		ArrayList<Tile> list = null;
		synchronized(cache){
			list = new ArrayList<Tile>(cache.values());
		}
		// iterate over tiles in cache
		for(Tile t : list){
			Rectangle r = t.getBounds();
			if(t.isLoaded() && !view.contains(r) && !view.intersects(r)){
				t.setImage(null);
			}
		}
		//System.out.println("compacting cache in "+(System.currentTimeMillis()-time)+" ms");
	}
	
	/**
	 * go through entire cache and reset tile values
	 *
	public void resetCache(){
		// copy list
		//ArrayList<Tile> list = null;
		synchronized(cache){
			list = new ArrayList<Tile>(cache.values());
		}
		ImageInfo info = getTileConnectionManager().getImageInfo();
		int rotate = info.getRotationTransform();
		int flip   = info.getFlipTransform();
		Dimension d = Utils.getRotatedDimension(info.getImageSize(),rotate);
		// iterate over tiles in cache
		for(Tile tile : list){
			// reset transformation info
			tile.setRotationTransform(rotate);
	    	tile.setFlipTransform(flip);
	    	
	    	// reset image data
	    	tile.setImage(null);
	    	
	    	// rotate bounds based on new values
	    	Rectangle r = Utils.getRotatedRectangle(tile.getOriginalBounds(),d,-rotate);
			tile.setBounds(Utils.getFlippedRectangle(r,d,flip));
		}
		
	}
	*/
	/**
	 * Get size of cache in bytes
	 * @return
	 */
	public int getCacheSize(){
		ArrayList<Tile> list = null;
		//	copy list
		synchronized (cache) {
			list = new ArrayList<Tile>(cache.values());
		}
		
		// iterate over tiles in cache
		int size = 0;
		for(Tile t : list){
			size += t.getTileSize();
		}
		return size;
	}
	
	

	/**
	 * Dispose of this tile manager
	 */
	public void dispose() {
		disposeFetchers();
		renderer.dispose();
		renderer = null;
		cache.clear();
		cache = null;
	}

	/**
	 * Fetches tile at coordinates specified by rectangle and scale
	 * @param retion of tile in absolute coordinates
	 * You can use getTileOffset to convert this region to tile coordinates
	 * @return
	 */
	public Tile fetchTile(Rectangle r){
		if(getTileConnectionManager().getViewer().getImageProperties() == null)
			return null;
		return fetchTile(r,getTileConnectionManager().getViewer().getImageProperties().getTileSize());
	}

	
	/**
	 * Fetches tile at coordinates specified by rectangle and scale
	 * @param retion of tile in absolute coordinates
	 * You can use getTileOffset to convert this region to tile coordinates
	 * @return
	 */
	public abstract Tile fetchTile(Rectangle r, Dimension tileSize, int timeout);
	
	
	/**
	 * Fetches tile at coordinates specified by rectangle and scale
	 * @param retion of tile in absolute coordinates
	 * You can use getTileOffset to convert this region to tile coordinates
	 * @return
	 */
	public Tile fetchTile(Rectangle r, Dimension tileSize){
		return fetchTile(r, tileSize, 2000);
	}
	
	/**
	 * get tile from the server, take care of transforms here
	 * @param r
	 * @return
	 */
	private Tile requestTile(Rectangle r, int timeout){
		ImageProperties prop = getTileConnectionManager().getImageInfo();
		if(prop == null)
			return null;
		
		// transform rectangle
		Rectangle rect = Utils.getTransformedRectangle(r,prop);
		
		// fetch all tiles in sequence
		long time = System.currentTimeMillis();
		Tile tile = fetchTile(rect,prop.getTileSize(),timeout);
		Constants.debug("fetched tile ["+r.x+","+r.y+","+r.width+","+r.height+"]",time);
		
		//Utils.transofrmTile(tile,r,rotate,flip);
		if(tile != null && prop != null){
			tile.setBounds(r);
			Utils.transofrmTile(tile,r,prop.getImageTransform());
		}
		return tile;
	}
	
	/**
	 * Get image info of the image
	 * 
	 * @return
	 */
	public abstract TileConnectionManager getTileConnectionManager();

	/**
	 * If there are any tiles in requested region, draw them
	 * else request them to be downloaded and drawn on overlay buffer
	 * @param rect
	 * @param scale
	 */
	public void drawAndRequestTiles(Graphics g, Rectangle rect, double scale) {
		ImageInfo info = getTileConnectionManager().getImageInfo();
		// once in a blue moon, maybe info is null
		if(info == null)
			return;
		ImageInfo.PyramidLevel lower = info.getLowerLevel(scale);
		
		// request tiles from upper level faster but lower quality
		// System.out.println("Viewport "+rect+" at scale="+scale);
		//long time = System.currentTimeMillis();
		Rectangle[] tiles = getTilesInRegion(rect,(lower !=  null)?lower.getZoom():scale);
		//System.out.println("tile resuest "+(System.currentTimeMillis()-time));
		// System.out.println("pyramid scale "+lower.getZoom());
		drawAndRequestTiles(g,rect,scale,tiles);
		
		// get high-rez tiles if necessary
		/*
		if(lower.getZoom() < scale){
			ImageInfo.PyramidLevel higher = info.getHigherLevel(scale);
			tiles = getTilesInRegion(rect, higher.getZoom());
			drawAndRequestTiles(g,rect,scale,tiles);
		}*/
	}
	
	/**
	 * If there are any tiles in requested region, draw them
	 * else request them to be downloaded and drawn on overlay buffer
	 * @param rect
	 * @param scale
	 */
	private void drawAndRequestTiles(Graphics g, Rectangle rect, double scale, Rectangle [] tiles) {
		//int rotate = getRotateTransform();
		//int flip   = getFlippedTransform();
		
		//iterate over tiles
		for (int i = 0; i < tiles.length; i++) {
			// System.out.println("Tile "+i+" "+tiles[i]);
			Rectangle r = tiles[i];
			String key = ""+r.x+","+r.y+","+r.width;
			Tile tile = (Tile) cache.get(key);
			// if tile is in cache, draw it, else request it
			if (tile != null && tile.isLoaded()){
				Utils.drawTile(tile,g,rect,scale);
			}else{
				getFetcher().fetch(r);
			}
		}
	}
	
	
	/**
	 * Request tiles in given rectangle and scale Fetched tiles should be
	 * drawn on top of the given buffer use setBuffer() to setup buffer to
	 * draw on
	 * 
	 * @param rect
	 * @param scale
	 */
	public void requestTiles(Rectangle rect, double scale) {
		ImageInfo info = getTileConnectionManager().getImageInfo();
		ImageInfo.PyramidLevel lower = info.getLowerLevel(scale);

		// request tiles from upper level faster but lower quality
		// System.out.println("Viewport "+rect+" at scale="+scale);
		Rectangle[] tiles = getTilesInRegion(rect, lower.getZoom());
		// System.out.println("pyramid scale "+lower.getZoom());

		// iterate over tiles
		for (int i = 0; i < tiles.length; i++) {
			// System.out.println("Tile "+i+" "+tiles[i]);
			getFetcher().fetch(tiles[i]);
		}
	}

	/**
	 * Get list of tile regions in absolute coordinates from given level
	 * 
	 * @param rectangle
	 * @param scale
	 * @return
	 */
	public Rectangle[] getTilesInRegion(Rectangle rect, double scale) {
		java.util.List<Rectangle> tiles = new LinkedList<Rectangle>();
		ImageInfo info = getTileConnectionManager().getImageInfo();
		double tileW = info.getTileSize().width / scale;
		double tileH = info.getTileSize().height / scale;

		// get offset of the left-top tile
		int x = (int) (tileW * Math.floor(rect.x / tileW));
		int y = (int) (tileH * Math.floor(rect.y / tileH));

		// init tiled region
		tiledRectangle = new Rectangle(x, y, (int) tileW, (int) tileH);

		// init more interesting rect 
		//Rectangle interest = new Rectangle(rect);
		//interest.grow(-rect.width/4, -rect.height/4);
			
		// go down and to the right to iterate over tiles
		for (int j = y; j < (rect.y + rect.height); j += tileH) {
			for (int i = x; i < (rect.x + rect.width); i += tileW) {
				Rectangle tr = new Rectangle(i, j, (int) tileW, (int) tileH);
			
				// add tile to the queue
				if(rect.contains(tr))
					tiles.add(tr);
				else
					tiles.add(0,tr);

				// keep track of loaded tiles
				tiledRectangle = tiledRectangle.union(tr);
			}
		}
		
		// sort tiles based on the proximity to the center of the region
		final Point2D c = new Point2D.Double(rect.getCenterX(),rect.getCenterY());
		Collections.sort(tiles,new Comparator<Rectangle>() {
			public int compare(Rectangle r1, Rectangle r2) {
				Point2D p1 = new Point2D.Double(r1.getCenterX(),r1.getCenterY());
				Point2D p2 = new Point2D.Double(r2.getCenterX(),r2.getCenterY());
				return (int) (c.distance(p2) - c.distance(p1));
			}
		});
		
		return  (Rectangle[]) tiles.toArray(new Rectangle[tiles.size()]);
	}

	
	/**
	 * get rotate transform
	 * @return
	 */
	public int getRotateTransform(){
		ImageInfo info = getTileConnectionManager().getImageInfo();
		return (info != null)?info.getImageTransform().getRotationTransform():0;
	}
	
	/**
	 * get flipped transform
	 * @return
	 */
	public int getFlippedTransform(){
		ImageInfo info = getTileConnectionManager().getImageInfo();
		return (info != null)?info.getImageTransform().getFlipTransform():0;
	}
	
	/**
	 * get image size
	 * @return
	 */
	public Dimension getImageSize(){
		ImageInfo info = getTileConnectionManager().getImageInfo();
		return (info != null)?info.getImageSize():new Dimension(0,0);
	}
	
	/**
	 * Convert rectangle in absolute coordinates and scale factor to tile
	 * offset at level
	 * 
	 * @return int [3] x, y, level
	 */
	public int[] getTileOffset(Rectangle r) {
		int[] a = new int[3];
		ImageInfo info = getTileConnectionManager().getImageInfo();
		double scale = info.getTileSize().getWidth() / r.width;
		double tileW = info.getTileSize().width / scale;
		double tileH = info.getTileSize().height / scale;
		a[0] = (int) (r.x / tileW);
		a[1] = (int) (r.y / tileH);
		a[2] = info.getLowerLevel(scale).getLevelNumber();
		return a;
	}

	/**
	 * This priority queue behaves like a normal stack, BUT it gives higher
	 * priority to cached tiles
	 * 
	 * @author tseytlin
	 */
	private class PriorityQueue {
		private Stack<Rectangle> cachedTiles = new Stack<Rectangle>();
		private Stack<Rectangle> requestedTiles = new Stack<Rectangle>();

		/**
		 * push rectangle on top of stack
		 * 
		 * @param r
		 */
		public void push(Rectangle r) {
			String key = "" + r.x + "," + r.y + "," + r.width;
			// determine where to store this key
			if (cache.containsKey(key))
				cachedTiles.push(r);
			else
				requestedTiles.push(r);
		}
		
		/**
		 * At rectangle at the end of the queue
		 * @param r
		 */
		public void add(Rectangle r){
			String key = "" + r.x + "," + r.y + "," + r.width;
			// determine where to store this key
			if (cache.containsKey(key))
				cachedTiles.add(0,r);
			else
				requestedTiles.add(0,r);
			
		}
		
		/**
		 * pop rectangle on top of stack
		 * 
		 * @param r
		 */
		public Rectangle pop() {
			if (!cachedTiles.isEmpty())
				return (Rectangle) cachedTiles.pop();
			else
				return (Rectangle) requestedTiles.pop();

		}

		/**
		 * clear content
		 */
		public void clear() {
			requestedTiles.clear();
			cachedTiles.clear();
		}

		/**
		 * is stack empty?
		 * 
		 * @return
		 */
		public boolean isEmpty() {
			return cachedTiles.isEmpty() && requestedTiles.isEmpty();
		}
	}

	/**
	 * This class fetches tiles
	 * 
	 * @author tseytlin
	 */
	protected class TileFetcher extends Thread {
		private PriorityQueue stack = new PriorityQueue();

		private boolean stop, waiting;

		private volatile Object message;

		/**
		 * Is thread waiting
		 */
		public boolean isWaiting() {
			return waiting;
		}

		/**
		 * Fetch new tile rectangle
		 * 
		 * @param r
		 * @param scl
		 */
		public void fetch(Rectangle r) {
			stack.push(r);
			if (waiting) {
				synchronized (stack) {
					stack.notifyAll();
				}
			}
		}

		/**
		 * stop thread, release all resources
		 */
		public void dispose() {
			stop = true;
			synchronized (stack) {
				stack.clear();
				stack.notifyAll();
			}
		}

		/**
		 * iterate indefinatly
		 */
		public void run() {
			while (!stop) {
				waiting = false;
				message = null;

				// pop stack if not empty, wait otherwise
				synchronized (stack) {
					if (!stack.isEmpty()) {
						message = stack.pop();
					} else if (!stop) {
						waiting = true;
						try {
							stack.wait();
						} catch (InterruptedException ex) {
							ex.printStackTrace();
						}
					}
				}

				// if stack was poped, do something
				if (message != null && message instanceof Rectangle) {
					final Tile buffer = getTileConnectionManager().getBuffer();
					// extract info from stack
					final Rectangle r = (Rectangle) message;

					// check whether this tile is still relevant
					if (buffer.getBounds().contains(r) || buffer.getBounds().intersects(r)) {
						// check cache first
						// else request new tile
						// long time = System.currentTimeMillis();
						final String key = "" + r.x + "," + r.y + "," + r.width;
						if (cache.containsKey(key)) {
							Tile tile = (Tile) cache.get(key);
							tile.incrementAccessCount();
							// add to render queue
							renderer.render(tile);
						} else {
							// fetch all tiles in sequence
							Tile tile = requestTile(r,(blacklist.containsKey(r)?LONG_TIMEOUT:SHORT_TIMEOUT));
							if (tile != null){
								// add to queue
								synchronized (cache) {
									cache.put(key, tile);
								}
								// remove from blacklist
								blacklist.remove(r);
								
								//	add to render queue
								renderer.render(tile);
							}else{
								// if tile could not be fetched in time
								// put it back into queue unless we ask for it again
								if(!blacklist.containsKey(r)){
									stack.add(r);
									blacklist.put(r,true);
								}
								//System.err.println("Connection expired re-adding");
							}
						}
					}
				}
			}
		}
	}

	/**
	 * This class renders tiles on top of the buffer in different thread
	 * 
	 * @author tseytlin
	 */
	protected class TileRenderer extends Thread {
		private Stack<Tile> stack = new Stack<Tile>();

		private boolean stop, waiting;

		private volatile Object message;

		/**
		 * Is thread waiting
		 */
		public boolean isWaiting() {
			return waiting;
		}

		/**
		 * Fetch new tile rectangle
		 * 
		 * @param r
		 * @param scl
		 */
		public void render(Tile t) {
			stack.push(t);
			if (waiting) {
				synchronized (stack) {
					stack.notifyAll();
				}
			}
		}

		/**
		 * stop thread, release all resources
		 */
		public void dispose() {
			stop = true;
			synchronized (stack) {
				stack.clear();
				stack.notifyAll();
			}
		}

		/**
		 * iterate indefinatly
		 */
		public void run() {

			while (!stop) {
				message = null;
				waiting = false;
				// pop stack if not empty, wait otherwise
				synchronized (stack) {
					if (!stack.isEmpty()) {
						message = stack.pop();
					} else if (!stop) {
						try {
							waiting = true;
							stack.wait();
						} catch (InterruptedException ex) {
							ex.printStackTrace();
						}
					}
				}

				// if stack was poped, do something
				if (message != null && message instanceof Tile) {
					//Tile buffer = getTileConnectionManager().getBuffer();

					// extract info from stack
					Tile tile = (Tile) message;

					// process image
					// first call to this loads the image
					tile.getImage();
					
					// draw tile on top of buffer
					//Graphics g = buffer.getImage().getGraphics();
					//Utils.drawTile(tile,g,buffer.getBounds(),buffer.getScale());
										
					// repaint
					if (stack.isEmpty())
						getTileConnectionManager().getViewer().repaint();
				}
			}
		}
	}

	/**
	 * @return the tiledRectangle
	 */
	public Rectangle getTiledRectangle() {
		return tiledRectangle;
	}
}
