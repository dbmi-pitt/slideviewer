package edu.pitt.slideviewer.qview.aperio;

import java.awt.*;
import java.awt.image.*;
import java.net.*;
import java.util.*;
import edu.pitt.slideviewer.*;
import edu.pitt.slideviewer.qview.*;
import edu.pitt.slideviewer.qview.connection.*;
import edu.pitt.slideviewer.qview.connection.handler.ImageStreamHandler;
import edu.pitt.slideviewer.qview.connection.handler.StreamHandler;

import java.awt.event.*;

//import java.beans.*;

public class AperioConnectionManager implements ConnectionManager, ComponentListener {
	private Viewer viewer;

	private ImageInfo info;

	private BufferedImage thumbnail;

	private Tile data, lowrez;

	private Communicator comm;

	private StreamHandler infoHandler, jpegHandler;

	private TileFetcher fetcher;

	private URL server;

	// private QuickViewController controller;
	// private Rectangle viewport;
	// private double viewScale;
	// private GraphicsConfiguration creator;

	public AperioConnectionManager(Viewer v) {
		this.viewer = v;
		// viewer.addPropertyChangeListener(this);
		comm = Communicator.getInstance();
		// controller = (QuickViewController) viewer.getViewerController();
		// create handler objects
		infoHandler = new AperioInfoHandler();
		jpegHandler = new ImageStreamHandler();
		fetcher = new TileFetcher();
		fetcher.start();
		/*
		 * GraphicsEnvironment env =
		 * GraphicsEnvironment.getLocalGraphicsEnvironment(); GraphicsDevice gd =
		 * env.getDefaultScreenDevice(); creator = gd.getDefaultConfiguration();
		 */
	}

	/**
	 * Get component listener
	 */
	public ComponentListener getComponentListener() {
		return this;
	}

	/**
	 * Establish connection to the server and retrieve image info
	 */
	public void connect(String image) throws Exception {
		server = new URL(viewer.getServer() + "/" + image);

		System.out.println("connecting to: " + server + " ...");

		// get general file info
		if (comm.queryServer(server, "INFO", infoHandler)) {
			String[] params = (String[]) infoHandler.getResult();
			if (params != null && params.length > 5) {
				// iterate over parameters based on expected result
				info = new ImageInfo();
				info.setName(image);
				int iw = Integer.parseInt(params[0]);
				int ih = Integer.parseInt(params[1]);
				int tw = Integer.parseInt(params[2]);
				int th = Integer.parseInt(params[3]);
				info.setImageSize(new Dimension(iw, ih));
				info.setTileSize(new Dimension(tw, th));

				Properties p = new Properties();
				p.setProperty("title", params[4]);
				p.setProperty("description", params[5]);
				// see if there is anything else
				for (int i = 6; i < params.length; i++) {
					String[] kv = params[i].split("=");
					if (kv.length > 1) {
						p.setProperty(kv[0].trim(), kv[1].trim());
					} else {
						p.setProperty("prop" + i, params[i]);
					}
				}
				// check if pixel size is available
				// if(p.containsKey("MPP")){
				String v = p.getProperty("MPP", "" + Constants.APERIO_PXL_SIZE);
				double pxl = 0;

				// get microns per pixel and convert to mm per pixel
				pxl = Double.parseDouble(v) / 1000;

				info.setPixelSize(pxl);
				// }
				info.setProperties(p);

				// now query for file compression info
				if (comm.queryServer(server, "FINFO", infoHandler)) {
					params = (String[]) infoHandler.getResult();
					if (params != null && params.length > 3) {
						info.setFileSize(Integer.parseInt(params[0]));
						info.setCompressionType(Integer.parseInt(params[1]));
						info.setCompressionQuality(Integer.parseInt(params[2]));
						info.getProperties().setProperty("compression codec", params[3]);

						// now query for pyramid info
						if (comm.queryServer(server, "PINFO", infoHandler)) {
							params = (String[]) infoHandler.getResult();
							if (params != null && params.length > 0) {

								int levels = Integer.parseInt(params[0]);
								//java.util.List lvls = new ArrayList();
								for (int i = 0; i < levels; i++) {
									int w = Integer.parseInt(params[3 * i + 1]);
									int h = Integer.parseInt(params[3 * i + 2]);
									double z = Double.parseDouble(params[3 * i + 3]);
									ImageInfo.PyramidLevel lvl = new ImageInfo.PyramidLevel();
									lvl.setLevelSize(new Dimension(w, h));
									lvl.setZoom(1 / z);
									//lvls.add(lvl);
									info.addLevel(lvl);
								}
								//info.setLevels((ImageInfo.PyramidLevel[]) lvls.toArray(new ImageInfo.PyramidLevel[0]));

								// get thumbnail
								String sz = "";
								if (iw > ih)
									sz = viewer.getSize().width + "+0";
								else
									sz = "0+" + viewer.getSize().height;
								if (comm.queryServer(server, "0+0+" + sz + "+-1", jpegHandler)) {
									// thumbnail = (BufferedImage)
									// jpegHandler.getResult();
									thumbnail = Utils.createBufferedImage((Image) jpegHandler.getResult());
									info.setThumbnail(thumbnail);
								} else
									throw new Exception("Could not get thumbnail for " + image);
							} else
								throw new Exception("Malformed PINFO for " + image);
						} else
							throw new Exception("Couldn't get PINFO for " + image);
					} else
						throw new Exception("Malformed FINFO for " + image);
				} else
					throw new Exception("Couldn't get FINFO for " + image);
			} else
				throw new Exception("Malformed INFO for " + image);
		} else
			throw new Exception("Couldn't get INFO for " + image+" on "+server);
		// else connection failed
		// return false;
	}

	 /**
     * get macro image for a slide
     * @return
     */
    public Image getMacroImage(){
    	return null;
    }
	
	 /**
     * get image thumbnail of appropriate size
     * (size of the current viewer window)
     * @return
     */
    public Image getImageThumbnail(){
    	Image img = null;
    	int w = viewer.getSize().width;
        int h = viewer.getSize().height;
    	boolean hrz = Utils.isHorizontal(viewer.getSize(),info.getImageSize());
		String sz = (hrz)?w +"+0":"0+"+h;
	    if(comm.queryServer(server,"0+0+"+sz+"+-1",jpegHandler)){
            img = Utils.createBufferedImage((Image)jpegHandler.getResult());
        }
	    return img;
    }
    
    
    /**
     * get slide label (if available)
     * @return null if not available
     */
    public Image getImageLabel(){
    	Image img = null;
		if(comm.queryServer(server,"0+0+150+0+-2",jpegHandler))
			img = Utils.createBufferedImage((Image) jpegHandler.getResult());
		return img;
    }
	
	
	/**
	 * Listen for componet being resized
	 */
	public void componentResized(ComponentEvent evt) {
		// we need to refetch thumb image
		if (viewer.hasImage()) {
			// get thumbnail
			int w = viewer.getSize().width;
			int h = viewer.getSize().height;
			if (comm.queryServer(server, "0+0+" + w + "+0+-1", jpegHandler)) {
				thumbnail = Utils.createBufferedImage((Image) jpegHandler.getResult());
				info.setThumbnail(thumbnail);
				
			} else
				System.err.println("Error: Could not get thumbnail for " + viewer.getImage());

			// resize navigator
			((QuickNavigator) viewer.getNavigator()).setImage(info);

			// data = null;
			double scl = viewer.getScale();
			if (scl <= viewer.getMinimumScale()) {
				viewer.getViewerController().resetZoom();
			} else {
				Rectangle r = viewer.getViewRectangle();
				viewer.setViewRectangle(new Rectangle(r.x, r.y, (int) (w / scl), (int) (h / scl)));
			}
			viewer.repaint();
		}
	}

	public void componentHidden(ComponentEvent e) {
	}

	public void componentMoved(ComponentEvent e) {
	}

	public void componentShown(ComponentEvent e) {
	}

	// disconnect
	public void disconnect() {
		info = null;
		data = null;
	}

	// dispose of resources
	public void dispose() {
		disconnect();
		fetcher.dispose();
		// viewer.removePropertyChangeListener(this);
		viewer = null;
		lowrez = null;
		thumbnail = null;
	}

	/**
	 * Get image meta data
	 */
	public ImageInfo getImageInfo() {
		return info;
	}

	/**
	 * Create low-res image from thumbnail
	 * 
	 * @param r
	 * @return
	 */
	private Tile createLowResImage(Rectangle r, int sw, int sh, double scale) {
		//long time = System.currentTimeMillis();
		Dimension idim = info.getImageSize();
		BufferedImage thumbnail = (BufferedImage) info.getThumbnail();

		// create output tile
		Tile tile = new Tile();
		tile.setBounds(r);
		tile.setScale(scale);

		// adjust hight/width of result
		sw = (int) ((sh * r.width) / r.height);
		sh = (int) ((sw * r.height) / r.width);

		// get thumbnail portion
		int w = (int) (r.width * thumbnail.getWidth() / idim.width);
		int h = (int) (r.height * thumbnail.getHeight() / idim.height);
		int x = (int) (r.x * thumbnail.getWidth() / idim.width);
		int y = (int) (r.y * thumbnail.getHeight() / idim.height);

		// if we are at low rez we can scale entire thumbnail
		if (scale < 0.04) {
			sw = (int) (idim.width * scale);
			sh = (int) (idim.height * scale);

			// memory check?
			if (sw * sh < 5000000) {
				// set image in tile
				tile.setBounds(new Rectangle(0, 0, idim.width, idim.height));
				tile.setImage(thumbnail.getScaledInstance(sw, sh, Image.SCALE_FAST));
				return tile;
			}
		}

		// check if we can get a bigger chunck *3
		if ((x - w) > 0 && (x + w * 2) < thumbnail.getWidth() && (y - h) > 0 && (y + h * 2) < thumbnail.getHeight()) {
			x = x - w;
			y = y - h;
			w *= 3;
			h *= 3;
			sw *= 3;
			sh *= 3;

			// memory check?
			if (sw * sh < 5000000)
				tile.setBounds(new Rectangle(r.x - r.width, r.y - r.height, r.width * 3, r.height * 3));
		}

		// get subimage
		BufferedImage timg = thumbnail.getSubimage(x, y, w, h);
		// rescale it to fill space
		Image ttimg = timg.getScaledInstance(sw, sh, Image.SCALE_FAST);
		Utils.flushImage(ttimg);
		// set image in tile
		tile.setImage(ttimg);
		//System.out.println("low rez image "
		//	+ (System.currentTimeMillis() - time) + " ms");
		//time = System.currentTimeMillis();
		return tile;
	}

	/**
	 * All fun happens here
	 */
	public void drawImageRegion(Graphics g, Rectangle view, double scale) {
		if (info == null)
			return;

		// long time = System.currentTimeMillis();
		Dimension dim = viewer.getSize();

		// copy rect
		Rectangle r = Utils.correctRectangle(info.getImageSize(), view);

		int sw = dim.width;
		int sh = dim.height;

		// do some sanity checking for bounds in case image is smaller
		// then viewing window
		if (r.width == 0 || r.height == 0)
			return;

		if (r.width < view.width) {
			sw = (int) ((sh * r.width) / r.height);
		}

		if (r.height < view.height) {
			sh = (int) ((sw * r.height) / r.width);
		}

		// calculate offset of main image region in case it is smaller
		// then viewing window
		int offx = (int) ((dim.width - sw) / 2);
		int offy = (int) ((dim.height - sh) / 2);

		// draw thumbnail when appropriate
		if (scale == viewer.getMinimumScale()) {
			g.drawImage(thumbnail, offx, offy, null);
			return;
		}

		// extract low quality image from thumb as temp measure
		// then add to tile fetcher
		if (data == null || !data.getBounds().equals(r)) {
			// create a lowrez image out of thumbnail
			// unless zoom in is applied
			if (data == null || !(data.getScale() < scale && scale > (viewer.getMinimumScale() * 4))) {

				// try to derive lowrez chunk if old lowrez chunk doesn't
				// contain
				// current viewport region
				if (lowrez == null || lowrez.getScale() != scale || !lowrez.getBounds().contains(r)) {
					lowrez = createLowResImage(r, sw, sh, scale);
				}

				// calculate offset of lowrez image
				Rectangle lr = lowrez.getBounds();
				int lx = (int) ((lr.x - r.x) * scale) + offx;
				int ly = (int) ((lr.y - r.y) * scale) + offy;
				g.drawImage(lowrez.getImage(), lx, ly, null);
				// System.out.println("low rez image
				// "+(System.currentTimeMillis()-time)+"
				// ms");time=System.currentTimeMillis();
			}

			// see if we can get a better looking image from previes data
			if (data != null) {
				Rectangle dr = data.getBounds();

				// zoom in was performed
				if (data.getScale() < scale && scale > (viewer.getMinimumScale() * 4)) {
					// long time = System.currentTimeMillis();
					Image result = null;
					double diff = scale / data.getScale();
					int x = (int) (Math.abs(r.x - dr.x) * data.getScale());
					int y = (int) (Math.abs(r.y - dr.y) * data.getScale());
					int w = (int) (sw / diff);
					int h = (int) (sh / diff);
					try {
						BufferedImage bi = Utils.createBufferedImage(data.getImage());
						Image img = bi.getSubimage(x, y, w, h);
						result = img.getScaledInstance(sw, sh, Image.SCALE_FAST);
					} catch (Exception ex) {
						// ex.printStackTrace();
						// if RasterException occures draw a low res image then
						lowrez = createLowResImage(r, sw, sh, scale);
						Rectangle lr = lowrez.getBounds();
						int lx = (int) ((lr.x - r.x) * scale) + offx;
						int ly = (int) ((lr.y - r.y) * scale) + offy;
						g.drawImage(lowrez.getImage(), lx, ly, null);
					}
					// draw on top
					g.drawImage(result, offx, offy, null);
					// System.out.println("zoom in overlay
					// "+(System.currentTimeMillis()-time)+"
					// ms");time=System.currentTimeMillis();
					// zoom out was performed
				} else {
					if (data.getScale() > scale && scale > (viewer.getMinimumScale() * 4)) {
						double diff = data.getScale() / scale;
						int x = (int) (Math.abs(dr.x - r.x) * scale);
						int y = (int) (Math.abs(dr.y - r.y) * scale);
						int w = (int) (sw / diff);
						int h = (int) (sh / diff);
						Image img = data.getImage().getScaledInstance(w, h, Image.SCALE_FAST);
						g.drawImage(img, offx + x, offy + y, null);
						// System.out.println((offx+x)+" , "+(offy+y));
						// System.out.println("zoom out overlay
						// "+(System.currentTimeMillis()-time)+"
						// ms");time=System.currentTimeMillis();
						// no zoom, only movement
					} else if (data.getScale() == scale) {
						int x = (int) ((dr.x - r.x) * scale);
						int y = (int) ((dr.y - r.y) * scale);
						g.drawImage(data.getImage(), offx + x, offy + y, null);
						// System.out.println("pan overlay
						// "+(System.currentTimeMillis()-time)+"
						// ms");time=System.currentTimeMillis();
					}
				}
			}

			// request tiles
			fetcher.fetch(r, scale);
			// System.out.println("notify request
			// "+(System.currentTimeMillis()-time)+" ms\n");

		} else {
			// System.out.println("data available
			// "+(System.currentTimeMillis()-time)+" ms\n");
			Image result = data.getImage();
			g.drawImage(result, offx, offy, null);
		}
	}

	/*
	 * follow viewer changes public void propertyChange(PropertyChangeEvent
	 * evt){ if(evt.getPropertyName().equals("ViewObserve")){ //
	 * fetcher.fetch(viewport,viewScale); } }
	 */

	/**
	 * This class fetches tiles
	 * 
	 * @author tseytlin
	 */
	private class TileFetcher extends Thread {
		private Rectangle rect;

		private double scale;

		private Object flag = "symaphore";

		private boolean stop;

		/**
		 * stop thread, release all resources
		 */
		public void dispose() {
			stop = true;
			synchronized (flag) {
				flag.notifyAll();
			}
			comm.cancel();
		}

		/**
		 * Fetch new rectangle
		 * 
		 * @param r
		 * @param scl
		 */
		public void fetch(Rectangle r, double scl) {
			synchronized (flag) {
				// interrupt previous
				// THIS IS GREAT, but sometimes it fails to update
				// probably because interrupt does not guarantee
				// when it will execute
				// if(comm.isProcessing())
				// interrupt();
				comm.cancel();
				this.rect = r;
				this.scale = scl;
				flag.notifyAll();
			}
		}

		public void run() {
			while (!stop) {
				// wait for tile
				if (rect == null) {
					synchronized (flag) {
						try {
							flag.wait();
						} catch (InterruptedException ex) {
							// ex.printStackTrace();
						}
					}
				} else if (!stop) {
					// long time=System.currentTimeMillis();
					// copy rectangle
					Rectangle r;
					synchronized (flag) {
						r = new Rectangle(rect);
						rect = null;
					}
					// fetch tile
					String x = "0" + r.x;
					String y = "0" + r.y;
					String w = "" + (int) (r.width * scale);
					String h = "" + (int) (r.height * scale);
					String z = "" + (1 / scale);

					if (comm.queryServer(server, x + "+" + y + "+" + w + "+" + h + "+" + z, jpegHandler)) {
						Tile tile = new Tile();
						tile.setImage((Image) jpegHandler.getResult());
						tile.setBounds(r);
						tile.setScale(scale);

						// System.out.println("data fetched
						// "+(System.currentTimeMillis()-time)+" ms");
						// if no new tiles have been requested, then
						// repaint viewer
						if (rect == null) {
							data = tile;
							// System.out.println("data used");
							viewer.repaint();
						}
					}
				}
			}
		}
	}
}
