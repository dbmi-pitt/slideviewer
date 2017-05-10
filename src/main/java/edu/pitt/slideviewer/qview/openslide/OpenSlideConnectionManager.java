package edu.pitt.slideviewer.qview.openslide;


import java.awt.*;
import java.net.*;
import java.util.*;
import edu.pitt.slideviewer.*;
import edu.pitt.slideviewer.qview.connection.*;
import edu.pitt.slideviewer.qview.connection.handler.DataStreamHandler;
import edu.pitt.slideviewer.qview.connection.handler.ImageStreamHandler;
import edu.pitt.slideviewer.qview.connection.handler.StreamHandler;
import edu.pitt.slideviewer.qview.*;
import java.awt.event.*;
//import java.awt.image.BufferedImage;
import java.awt.image.BufferedImage;

import javax.swing.Timer;



public class OpenSlideConnectionManager extends TileConnectionManager implements ComponentListener {
	private Viewer viewer;
	private ImageInfo info;
	private URL server;
	private Communicator comm;
	private StreamHandler infoHandler,imgHandler, dataHandler;
	private OpenSlideTileManager manager;
	private Timer  resizeTimer, thumbnailTimer;
    private int CONNECTION_TIMEOUT;
    
    public OpenSlideConnectionManager(Viewer v){
        this.viewer = v;
        comm = Communicator.getInstance();
        //controller = (QuickViewController) viewer.getViewerController();
        // create handler objects
        infoHandler = new OpenSlideInfoHandler();
        imgHandler = new ImageStreamHandler();
        dataHandler = new DataStreamHandler();
        
        // create manager
        manager = new OpenSlideTileManager(this);
        
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
     * Get stream handler for handling images
     * @return
     */
    public StreamHandler getImageHandler(){
    	return imgHandler;
    }
    
    /**
     * Get stream handler for handling images
     * @return
     */
    public StreamHandler getDataHandler(){
    	return dataHandler;
    }
    
    /**
     * Get server URL
     * @return
     */
    public URL getServer(){
    	return server;
    }
    
    /**
     * init temp buffer
     *
     */
    private void initBuffer(){
    	Dimension d = viewer.getSize();
    	Utils.checkDimensions(d);
    	//Image img = viewer.getViewerComponent().createVolatileImage(d.width,d.height);
        BufferedImage img = new BufferedImage(d.width,d.height,BufferedImage.TYPE_INT_RGB);
		Graphics g = img.createGraphics();
		g.setColor(Color.white);
		g.fillRect(0,0,d.width,d.height);
        buffer.setImage(img);
    }
    
    
	/**
     * Establish connection to the server and retrieve image info
     */
    public void connect(String image) throws Exception {
    	 long time = System.currentTimeMillis();
    	 server = viewer.getServer();
    	 
         // get general file info
         //long time = System.currentTimeMillis();
    	 if(comm.queryServer(server,"action=info&path="+Utils.filterURL(image), infoHandler,CONNECTION_TIMEOUT)){
    		 // get props
        	Properties params = (Properties) infoHandler.getResult();
            
        	ImageInfo inf = new ImageInfo();
        	inf.setName(image);
        	inf.setConnectionManager(this);
        	int iw = Integer.parseInt(params.getProperty("image.width"));
            int ih = Integer.parseInt(params.getProperty("image.height"));
            int tw = Integer.parseInt(params.getProperty("tile.width"));
            int th = Integer.parseInt(params.getProperty("tile.height"));
        	inf.setImageSize(new Dimension(iw,ih));
            inf.setTileSize(new Dimension(tw,th));
            
            //get microns per pixel and convert to mm per pixel
            try{
            	inf.setPixelSize(Double.parseDouble(params.getProperty("pixel.size")));
            }catch(NumberFormatException ex){
            	inf.setPixelSize(Constants.APERIO_PXL_SIZE);
            }
            inf.setProperties(params);
            inf.setFileSize(Long.parseLong(params.getProperty("image.size")));
            
            ///////////////////////////////////////////////
        	// Because I in fact use regions and not blocks
        	// I can get any resolution I want, so I might 
        	// as well have more levels
        	///////////////////////////////////////////////
            //ArrayList lvls = new ArrayList();
            int levels = Integer.parseInt(params.getProperty("layer.count"));
            ScalePolicy scalePolicy = viewer.getScalePolicy();
        	double [] scales = null;
        	
        	if(scalePolicy != null){
        		scales = scalePolicy.getAvailableScales();
        		if(scales != null)
        			levels = scales.length;
        	}
        	// iterate over scales
            for(int i=0;i<levels;i++){
            	// honestly I don't care about dimensions
                int w = (scales == null)?Integer.parseInt(params.getProperty("layer."+i+".width")):0;
                int h = (scales == null)?Integer.parseInt(params.getProperty("layer."+i+".height")):0;
                double z = (scales == null)?w/iw:scales[i];
                if(z > 0.01){
                    ImageInfo.PyramidLevel lvl = new ImageInfo.PyramidLevel();
                    lvl.setLevelSize(new Dimension(w,h));
                    lvl.setZoom((scales == null)?1/z:z);
                    lvl.setLevelNumber(i);
                    //lvls.add(lvl);
                    inf.addLevel(lvl);
                }
            }
            //inf.setLevels((ImageInfo.PyramidLevel []) lvls.toArray(new ImageInfo.PyramidLevel[0]));
            //inf.sortLevels(); 	
            
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
    	
    	//Dimension is = Utils.getRotatedDimension(info.getImageSize(),rotate);
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
	    	int sz = (hrz)?(!isOrigHrz)?ah:w:(isOrigHrz)?aw:h;
			
	    	boolean success = false;
	    	// for mirax images thumbnail background is black???? so we should use region then
	    	// however some hamamatsu vms won't open, hence we need thumbnail
	    	if("mirax".equalsIgnoreCase(info.getProperties().getProperty("image.vendor")))
	    		success = comm.queryServer(server,"action=region&path="+Utils.filterURL(info.getName())+"&x=0&y=0&width="+is.width+"&height="+is.height+"&size="+sz,imgHandler);
	    	else
	    		success = comm.queryServer(server,"action=image&path="+Utils.filterURL(info.getName())+"&size="+sz,imgHandler);
	    	
	    	if(success){
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
    
    /**
     * get macro image for a slide
     * @return
     */
    public Image getMacroImage(){
    	if(info == null)
    		return null;
    	Image img = null;
		if(comm.queryServer(server,"action=macro&path="+info.getName(),imgHandler))
			img = Utils.createBufferedImage((Image) imgHandler.getResult());
		return img;
    }
    
    
    /**
     * get slide label (if available)
     * @return null if not available
     */
    public Image getImageLabel(){
    	if(info == null)
    		return null;
    	Image img = null;
		if(comm.queryServer(server,"action=label&path="+info.getName(),imgHandler))
			img = Utils.createBufferedImage((Image) imgHandler.getResult());
		return img;
    }
    
    
    /**
     * Listen for componet being resized
     */
    public void componentResized(ComponentEvent evt){
    	resizeTimer.start();
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
    public void componentHidden(ComponentEvent e){}
    public void componentMoved(ComponentEvent e){}
    public void componentShown(ComponentEvent e){}
    
    
    //disconnect
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
     * Get image meta data
     */
    public ImageInfo getImageInfo() {
        return info;
    }
    
    /**
     * Get viewer
     */
    public Viewer getViewer(){
    	return viewer;
    }
    
    /**
     * Get tile manager
     */
    public TileManager getTileManager(){
    	return manager;
    }
    /**
     * Get component listener
     */
    public ComponentListener getComponentListener(){
    	return this;
    }
    
}
