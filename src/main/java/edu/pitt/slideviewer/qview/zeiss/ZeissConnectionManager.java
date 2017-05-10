package edu.pitt.slideviewer.qview.zeiss;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.Timer;
import edu.pitt.slideviewer.Constants;
import edu.pitt.slideviewer.ImageTransform;
import edu.pitt.slideviewer.ScalePolicy;
import edu.pitt.slideviewer.Viewer;
import edu.pitt.slideviewer.ViewerException;
import edu.pitt.slideviewer.ViewerFactory;
import edu.pitt.slideviewer.qview.QuickNavigator;
import edu.pitt.slideviewer.qview.connection.Communicator;
import edu.pitt.slideviewer.qview.connection.ImageInfo;
import edu.pitt.slideviewer.qview.connection.Tile;
import edu.pitt.slideviewer.qview.connection.TileConnectionManager;
import edu.pitt.slideviewer.qview.connection.TileManager;
import edu.pitt.slideviewer.qview.connection.Utils;
import edu.pitt.slideviewer.qview.connection.handler.DataStreamHandler;
import edu.pitt.slideviewer.qview.connection.handler.ImageStreamHandler;
import edu.pitt.slideviewer.qview.connection.handler.PropertyStreamHandler;
import edu.pitt.slideviewer.qview.connection.handler.StreamHandler;


public class ZeissConnectionManager extends TileConnectionManager implements ComponentListener {
	private Viewer viewer;
	private ImageInfo info;
	private URL server;
	private Communicator comm;
	private StreamHandler infoHandler,imgHandler, dataHandler;
	private ZeissTileManager manager;
	private Timer  resizeTimer, thumbnailTimer;
	private int CONNECTION_TIMEOUT = 0;
	
	/**
	 * create new instance of connection manager
	 * @param v
	 */
	public ZeissConnectionManager(Viewer v){
        this.viewer = v;
        comm = Communicator.getInstance();
        
        // create handler objects
        infoHandler = new PropertyStreamHandler();
        imgHandler = new ImageStreamHandler();
        dataHandler = new DataStreamHandler();
        
        // create manager
        manager = new ZeissTileManager(this);
        
        // init buffer
        buffer = new Tile();
        
        // init resize timer
        resizeTimer = new Timer(200,getResizeAction());
        resizeTimer.setRepeats(false);
        thumbnailTimer = new Timer(300,getThumnailFetchAction());
        thumbnailTimer.setRepeats(false);
        
        // get connection timeout
        try{CONNECTION_TIMEOUT = Integer.parseInt(ViewerFactory.getProperty("qview.connection.timeout"));}catch(Exception ex){}
    }
	
	/**
	 * get server
	 * @return
	 */
	public URL getServer(){
		return server;
	}
	
	
	/**
	 * connect to the image
	 */
	public void connect(String image) throws Exception {
		 long time = System.currentTimeMillis();
    	 server = viewer.getServer();
    	 
    	 // create a url
    	 if(server == null)
    		 throw new ViewerException("Zeiss Tile Server URL is not set");
    	 
    	 // create new URL for this image
    	 String sr = ""+server;
    	 String im = image.replaceAll(" ","%20");
    	 if(sr.endsWith("/"))
    		 sr = sr.substring(0,sr.length()-1);
    	 if(!im.startsWith("/"))
    		 im = "/"+im;
    	 server = new URL(sr+im);
    	     	 
    	  //long time = System.currentTimeMillis();
    	 if(comm.queryServer(server,"req=get-props&type=unified",infoHandler,CONNECTION_TIMEOUT)){
            // get props
        	Properties params = (Properties) infoHandler.getResult();
            // init new datastructure
        	ImageInfo inf = new ImageInfo();
        	inf.setName(image);
        	inf.setConnectionManager(this);
        	
        	// get number of levels
        	if(!params.containsKey("num-levels"))
        		throw new ViewerException("Unable to get image "+image+" meta data, num-levels field is absent");
        	int levels = Integer.parseInt(params.getProperty("num-levels"));
        	
        	// iterate over levels
        	double scl = 1.0;
        	int iw = 0;
            int ih = 0;
            int tw = 256;
            int th = 256;
            //ArrayList<ImageInfo.PyramidLevel> lvls = new ArrayList<ImageInfo.PyramidLevel>();
        	double resolution = 0;
            Point offset = null;
        	// iterate in reverse order
        	for(int i=levels-1;i>= 0; i--){
            	
            	// fetch approprate level informat
            	Dimension imageSize = Utils.parseDimension(params.getProperty(i+".image-size"));
            	Rectangle imageRect = Utils.parseRectangle(params.getProperty(i+".image-rect"));
            	Dimension tileSize = Utils.parseDimension(params.getProperty(i+".tile-size"));
            	Point2D imagePosition = Utils.parsePoint(params.getProperty(i+".image-position"));
            	Point2D imageResolution = Utils.parsePoint(params.getProperty(i+".image-resolution"));
            	
            	ImageInfo.PyramidLevel lvl = new ImageInfo.PyramidLevel();
            	lvl.setLevelSize(imageSize);
            	lvl.setZoom(scl);
            	lvl.setLevelNumber(i);
            	lvl.setTileSize(tileSize);
            	lvl.setResolution(Math.max(imageResolution.getX(),imageResolution.getY()));
            	lvl.setLevelRectangle(imageRect);
            	inf.addLevel(lvl);
            	
            	
            	//System.out.println(i+" "+imageSize+" "+imageRect+" "+imagePosition);
            	
            	// set image size from the very last 100% zoom layer
            	if(scl == 1.0){
            		iw = imageSize.width;
            		ih = imageSize.height;
            		resolution = lvl.getResolution();
            		offset = imageRect.getLocation();
            		//don't reset tile size just leave default in
            	}
            	
            	// decimate
            	scl = scl / 2;
            }
            //inf.setLevels((ImageInfo.PyramidLevel []) lvls.toArray(new ImageInfo.PyramidLevel[0]));
            //inf.sortLevels(); 	
        	inf.setImageSize(new Dimension(iw,ih));
            inf.setTileSize(new Dimension(tw,th));
            inf.setPixelSize(resolution*0.001);
            inf.setProperties(params);
            inf.setImageOffset(offset);       
            
            // get channel information
            if(params.containsKey("channel-colors") && params.containsKey("channel-names")){
            	Map<String,Color> ch = new LinkedHashMap<String,Color>();
            	String [] nm = params.getProperty("channel-names").split(",");
            	String [] cc = params.getProperty("channel-colors").split(",");
            	if(nm.length == cc.length){
            		for(int i=0;i<nm.length;i++){
            			int n = cc[i].length();
            			int r = (n >= 2)?Integer.parseInt(cc[i].substring(n-2,n),16):0;
            			int g = (n >= 4)?Integer.parseInt(cc[i].substring(n-4,n-2),16):0;
            			int b = (n >= 6)?Integer.parseInt(cc[i].substring(n-6,n-4),16):0;
            		
            			int rgb = r;
            			rgb = (rgb << 8) + g;
            			rgb = (rgb << 8) + b;
            			
            			ch.put(nm[i].trim(),new Color(rgb));
            		}
            		inf.setChannelMap(ch);
            	}
            }
            
            
            // copy info
            this.info = inf;
            
            // get thumbnail
            // first get smaller image to shave off time, then get full size
            // image
            int w = viewer.getSize().width;
            int h = viewer.getSize().height;
            
            // set a horizontal limit
            if(w > 1024){
            	h = (1024 * h) / w;
            	w = 1024;
            }
            Constants.debug("got image meta data",time);
            time = System.currentTimeMillis();
            Dimension hd = (w < 800)?new Dimension(w,h):new Dimension(w/2,h/2);
            
            // now do a fetch
            Image thumbnail = getImageThumbnail(hd);
            Constants.debug("got half sized thumbnail",time);
            
            if(thumbnail != null){
            	inf.setThumbnail(thumbnail);
                inf.scaleThumbnail(w,h);
            	// resize temp buffer
                initBuffer();
                // fetch hi-res image
                if(hd.width != w)
                	thumbnailTimer.start();
            }else
                throw new Exception("Could not get thumbnail for "+image); 
        }else
        	throw new Exception("Couldn't get INFO for "+image+"\non "+server);
        //return false;
    

	}
	
	 /**
     * get image thumbnail of appropriate size
     * (size of the current viewer window)
     * @return
     */
    public synchronized Image getImageThumbnail(){
    	Dimension d = (viewer != null)?viewer.getSize():new Dimension(500,500);
    	return getImageThumbnail(d);
    }
    
    
    /**
     * get image thumbnail of appropriate size
     * (size of the current viewer window)
     * @return
     */
    private synchronized Image getImageThumbnail(Dimension vsize){
    	if(info == null)
    		return null;
    	
    	//long time = System.currentTimeMillis();
    	Image img = null;
    	ImageTransform it = info.getImageTransform();
    	
    	Dimension is = info.getImageSize();
    	
		// is original horizontal or vertical
		int rotate = getTileManager().getRotateTransform();
		boolean isOrigHrz = (rotate % 2 == 0)?info.isHorizontal():info.isVertical();
		int w = vsize.width;
        int h = vsize.height;
    	boolean hrz = Utils.isHorizontal(viewer.getSize(),is);

    	if(!it.isCropped()){
        	int aw = (h*is.width)/is.height;
			int ah = (w*is.height)/is.width;
	    	
	    	if(hrz){
	    		h = (isOrigHrz)?ah:w;
	    	}else{
	    		w = (isOrigHrz)?aw:h;
	    	}
			if(comm.queryServer(server,"req=get-thumbnail&size="+w+","+h+"&type=unified",imgHandler)){
				Utils.flushImage((Image)imgHandler.getResult());
				img = Utils.createBufferedImage((Image)imgHandler.getResult());
	        }	
    	}else{
    		Rectangle crop = it.getCropRectangle();
         	if(hrz)
         		h = w*crop.height/crop.width;
         	else
         		w = h*crop.width/crop.height;
         	Tile t = getTileManager().fetchTile(crop,new Dimension(w,h));
         	img = t.getImage();
    	}
		return img;
     }
	
	

	public ImageInfo getImageInfo() {
		return info;
	}

	public TileManager getTileManager() {
		return manager;
	}

	public Viewer getViewer() {
		return viewer;
	}
	
	public void disconnect() {
    	super.disconnect();
    	info = null;
    }

    // release resources
    public void dispose(){
    	super.dispose();
    	manager = null;
    	infoHandler = null;
        imgHandler = null;
        dataHandler = null;
        viewer = null;
    }
    
    /**
     * init temp buffer
     *
     */
    private void initBuffer(){
    	Dimension d = viewer.getSize();
    	Utils.checkDimensions(d);
    	BufferedImage img = new BufferedImage(d.width,d.height,BufferedImage.TYPE_INT_RGB);
		Graphics g = img.createGraphics();
		g.setColor(Color.white);
		g.fillRect(0,0,d.width,d.height);
        buffer.setImage(img);
    }
    
	
	/**
     * Listen for componet being resized
     */
    public void componentResized(ComponentEvent evt){
    	resizeTimer.start();
    }
	public void componentHidden(ComponentEvent e) {}
	public void componentMoved(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}
	public ComponentListener getComponentListener() {
		return this;
	}

	public Image getImageLabel() {
		if(comm.queryServer(server,"req=get-label",imgHandler)){
			Utils.flushImage((Image)imgHandler.getResult());
			return Utils.createBufferedImage((Image)imgHandler.getResult());
        }	
		return null;
	}

	public Image getMacroImage() {
		return null;
	}

	  /**
     * get resize action
     * @return
     */
    private ActionListener getResizeAction(){
    	return new ActionListener(){
			public void actionPerformed(ActionEvent a) {
				//we need to refetch thumb image
		        if(viewer != null && viewer.hasImage()){
	        	
		            //  get thumbnail
		        	int w = viewer.getSize().width;
		            int h = viewer.getSize().height;
		            
		           
		            // scale thumbnail as temp measure
		            info.scaleThumbnail(w, h);
		            
		            // resize temp buffer
		            initBuffer();
		            
		            // resize navigator
		            ((QuickNavigator)viewer.getNavigator()).setImage(info);
		            
		            //data = null;
		            double scl = viewer.getScale();
		            if(scl <= viewer.getMinimumScale()){
		            	viewer.getViewerController().resetZoom();
		            }else{
		            	Rectangle r = viewer.getViewRectangle();
		            	viewer.setViewRectangle(new Rectangle(r.x,r.y,(int)(w/scl),(int)(h/scl)));
		            }
		            viewer.repaint();
		            
		            // send resize event
		            viewer.firePropertyChange(Constants.VIEW_RESIZE,null,viewer.getSize());
		        
		            // stop resize timer
		            resizeTimer.stop();
		            
		            // request hi-res thumbnail
		            thumbnailTimer.start();
		        }   
			}
    	};
    }
    
    /**
     * get resize action
     * @return
     */
    private ActionListener getThumnailFetchAction(){
    	return new ActionListener(){
			public void actionPerformed(ActionEvent a) {
				(new Thread(new Runnable() {
					public void run() {
						long time = System.currentTimeMillis();
						Image t = getImageThumbnail();
		        		if(t != null){
		        			info.setThumbnail(t);
			                // resize navigator
			                ((QuickNavigator)viewer.getNavigator()).setImage(info);
			                viewer.repaint();
			            }else if(viewer != null)
			                System.err.println("Error: Could not get thumbnail for "+viewer.getImage()); 
		        		thumbnailTimer.stop();
		        		Constants.debug("got full size thumbnail",time);
					}
				})).start();
			}
    	};
    }
	
}
