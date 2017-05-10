package edu.pitt.slideviewer.qview.aperio;

import java.awt.*;
import java.net.*;
import java.util.*;
import java.util.List;

import edu.pitt.slideviewer.*;
import edu.pitt.slideviewer.qview.connection.*;
import edu.pitt.slideviewer.qview.connection.handler.DataStreamHandler;
import edu.pitt.slideviewer.qview.connection.handler.ImageStreamHandler;
import edu.pitt.slideviewer.qview.connection.handler.ListHandler;
import edu.pitt.slideviewer.qview.connection.handler.StreamHandler;
import edu.pitt.slideviewer.qview.*;

import java.awt.event.*;
import java.awt.image.BufferedImage;

import javax.swing.Timer;

public class AperioTileConnectionManager extends TileConnectionManager implements ComponentListener {
	private Viewer viewer;
	private ImageInfo info;
    private URL server;
    private Communicator comm;
    private StreamHandler infoHandler,imgHandler, dataHandler;
    private AperioTileManager manager;
    private Timer  resizeTimer, thumbnailTimer;
	private int CONNECTION_TIMEOUT;
    
    public AperioTileConnectionManager(Viewer v){
        this.viewer = v;
        comm = Communicator.getInstance();
        //controller = (QuickViewController) viewer.getViewerController();
        // create handler objects
        infoHandler = new AperioInfoHandler();
        imgHandler = new ImageStreamHandler();
        dataHandler = new DataStreamHandler();
        
        // create manager
        manager = new AperioTileManager(this);
        
        // init buffer
        buffer = new Tile();
        
        // init resize timer
        resizeTimer = new Timer(200,getResizeAction());
        resizeTimer.setRepeats(false);
        thumbnailTimer = new Timer(600,getThumnailFetchAction());
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
    	server = new URL(viewer.getServer(),Utils.filterURL(image));
       
        // get general file info
        if(comm.queryServer(server,"INFO", infoHandler,CONNECTION_TIMEOUT)){
            String [] params = (String []) infoHandler.getResult();
            if(params != null && params.length > 5){
                //iterate over parameters based on expected result
                ImageInfo inf = new ImageInfo();
                inf.setName(image);
                inf.setConnectionManager(this);
                int iw = Integer.parseInt(params[0]);
                int ih = Integer.parseInt(params[1]);
                int tw = Integer.parseInt(params[2]);
                int th = Integer.parseInt(params[3]);
                inf.setImageSize(new Dimension(iw,ih));
                inf.setTileSize(new Dimension(tw,th));
               
                Properties p = new Properties();
                p.setProperty("title",params[4]);
                p.setProperty("description",params[5]);
                // see if there is anything else
                for(int i=6;i<params.length;i++){
                    String [] kv = params[i].split("=");
                    if(kv.length > 1){
                        p.setProperty(kv[0].trim(),kv[1].trim());
                    }else{
                        p.setProperty("prop"+i,params[i]);
                    }   
                }
                // check if pixel size is available
                String v = p.getProperty("MPP");
                double pxl = 0;
                //get microns per pixel and convert to mm per pixel
                pxl = (v != null)?Double.parseDouble(v)/1000:Constants.APERIO_PXL_SIZE;
                inf.setPixelSize(pxl);
               
                inf.setProperties(p);
                
                // now query for file compression info
                if(comm.queryServer(server,"FINFO", infoHandler)){
                    params = (String []) infoHandler.getResult();
                    if(params != null && params.length > 3){
                        inf.setFileSize(Long.parseLong(params[0]));
                        inf.setCompressionType(Integer.parseInt(params[1]));
                        inf.setCompressionQuality(Integer.parseInt(params[2]));
                        inf.getProperties().setProperty("compression codec",params[3]);
                      
                        //  now query for pyramid info 
                        if(comm.queryServer(server,"PINFO", infoHandler)){
                            params = (String []) infoHandler.getResult();
                            if(params != null && params.length > 0){
                            	//java.util.List lvls = new ArrayList();
                            	int levels = Integer.parseInt(params[0]);
                            	
                            	///////////////////////////////////////////////
                            	// Because I in fact use regions and not blocks
                            	// I can get any resolution I want, so I might 
                            	// as well have more levels
                            	///////////////////////////////////////////////
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
                                    int w = (scales == null)?Integer.parseInt(params[3*i+1]):0;
                                    int h = (scales == null)?Integer.parseInt(params[3*i+2]):0;
                                    double z = (scales == null)?Double.parseDouble(params[3*i+3]):scales[i];
                                    if(z > 0.01){
	                                    ImageInfo.PyramidLevel lvl = new ImageInfo.PyramidLevel();
	                                    lvl.setLevelSize(new Dimension(w,h));
	                                    lvl.setZoom((scales == null)?1/z:z);
	                                    lvl.setLevelNumber(i);
	                                    //lvls.add(lvl);
	                                    inf.addLevel(lvl);
                                    }
                                }
                               // inf.setLevels((ImageInfo.PyramidLevel []) lvls.toArray(new ImageInfo.PyramidLevel[0]));
                                //inf.sortLevels(); 
                                // printout levels
                                /*
                                for(int k=0;k<info.getLevelCount();k++){
                                	ImageInfo.PyramidLevel l = info.getLevels()[k];
                                	System.out.println("size: "+l.getLevelSize()+" scale="+l.getZoom());
                                } */                              
                            
                                // copy info
                                this.info = inf;
                                
                                Constants.debug("got image meta data",time);
                                
                                // get thumbnail
                                time = System.currentTimeMillis();
                                
                                // first get smaller image to shave off time, then get full size image
                                int w = viewer.getSize().width;
                                int h = viewer.getSize().height;
                                
                                // set a horizontal limit
                                if(w > 1024){
                                	h = (1024 * h) / w;
                                	w = 1024;
                                }
                                Dimension hd = (w < 800)?new Dimension(w,h):new Dimension(w/2,h/2);
                                
                                // now do a fetch
                                Image thumbnail = getImageThumbnail(hd);
                                Constants.debug("got half sized thumbnail",time);
                                // Image thumbnail = getImageThumbnail();
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
                            	throw new Exception("Malformed PINFO for "+image); 
                        }else
                        	throw new Exception("Couldn't get PINFO for "+image);
                    }else
                    	throw new Exception("Malformed FINFO for "+image);
                }else
                	throw new Exception("Couldn't get FINFO for "+image);
            }else
            	throw new Exception("Malformed INFO for "+image);
        }else
        	throw new Exception("Couldn't get INFO for "+image+"\non "+server);
        // else connection failed
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
    public synchronized Image getImageThumbnail(Dimension vsize){
    	if(viewer == null)
    		return null;
    	
    	// do normal fetch
    	ImageTransform it = info.getImageTransform();
    	Image img = null;
    	int w = vsize.width;
        int h = vsize.height;
        boolean hrz = (info == null)?true:Utils.isHorizontal(viewer.getSize(),info.getImageSize());
        //Utils.getRotatedDimension(info.getOriginalImageSize(),getTileManager().getRotateTransform()));
       if(!it.isCropped()){
        	String sz = (hrz)?w +"+0":"0+"+h;
			if(comm.queryServer(server,"0+0+"+sz+"+-1",imgHandler)){
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
    	return null;
    }
    
    /**
     * get slide label (if available)
     * @return null if not available
     */
    public Image getImageLabel(){
    	if(info == null)
    		return null;
    	Image img = null;
		if(comm.queryServer(server,"0+0+150+0+-2",imgHandler))
			img = Utils.createBufferedImage((Image) imgHandler.getResult());
		return img;
    }
    
    
    /**
     * Listen for componet being resized
     */
    public void componentResized(ComponentEvent evt){
    	resizeTimer.start();
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
			            }else
			                System.err.println("Error: Could not get thumbnail for "+viewer.getImage()); 
		        		thumbnailTimer.stop();
		          		Constants.debug("got full size thumbnail",time);
					}
				})).start();
			}
    	};
    }
    
    
	/**
	 * get the list of images on the server
	 * @param path
	 * @param should folder names be included
	 * @return
	 */
	public List<String> getImageList(String path, boolean includeFolders){
		URL server = (viewer != null)?viewer.getServer():null;
		
		// setup server if unavailable
		if(server == null){
			if(ViewerFactory.getProperties().containsKey("aperio.server.url")){
				try {
					server = new URL(ViewerFactory.getProperties().getProperty("aperio.server.url"));
				} catch (MalformedURLException e) {
					//e.printStackTrace();
				}
			}
		}
		
		
		// make path work
		if(path == null)
			path = "/";
		if(!path.endsWith("/"))
			path += "/";
		
		// make URL
		try {
			String s = ""+server;
			if(!s.endsWith("/"))
				s += "/";
			server = new URL(s+Communicator.escape(path));
		} catch (MalformedURLException e) {
			//e.printStackTrace();
		}
	
		
		// now lets do browsing 
		List<String> filelist = new ArrayList<String>();
		StreamHandler handler = new ListHandler();
		if(comm.queryServer(server,"DIR",handler)){
			for(String line: (List<String>) handler.getResult()){
				String [] params = line.split("\\|");
				if(params.length >= 2){
					boolean isDir = "dir".equals(params[1]);
					filelist.add(params[0].trim()+((isDir)?"/":""));
				}
			}
		}
		// return result		
		return filelist;
	}
}
