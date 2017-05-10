package edu.pitt.slideviewer.qview.hamamatsu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Timer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.pitt.slideviewer.Constants;
import edu.pitt.slideviewer.ImageTransform;
import edu.pitt.slideviewer.ScalePolicy;
import edu.pitt.slideviewer.Viewer;
import edu.pitt.slideviewer.ViewerException;
import edu.pitt.slideviewer.ViewerFactory;
import edu.pitt.slideviewer.qview.QuickNavigator;
import edu.pitt.slideviewer.qview.QuickViewer;
import edu.pitt.slideviewer.qview.connection.Communicator;
import edu.pitt.slideviewer.qview.connection.ImageInfo;
import edu.pitt.slideviewer.qview.connection.Tile;
import edu.pitt.slideviewer.qview.connection.TileConnectionManager;
import edu.pitt.slideviewer.qview.connection.TileManager;
import edu.pitt.slideviewer.qview.connection.Utils;
import edu.pitt.slideviewer.qview.connection.handler.*;

public class NDPConnectionManager extends TileConnectionManager implements ComponentListener { 
	private Viewer viewer;
	private ImageInfo info;
	private URL server;
	private Communicator comm;
	private StreamHandler xmlHandler,imgHandler, dataHandler;
	private NDPTileManager manager;
	private Timer  resizeTimer, thumbnailTimer;
	private boolean signedIn;
	private static Map<String,String> pathToID;
	private int CONNECTION_TIMEOUT;
	
	/**
	 * create new instance of connection manager
	 * @param v
	 */
	public NDPConnectionManager(Viewer v){
        this.viewer = v;
        comm = Communicator.getInstance();
        pathToID = new HashMap<String, String>();
        
        // create handler objects
        xmlHandler = new XMLStreamHandler();
        imgHandler = new ImageStreamHandler();
        dataHandler = new DataStreamHandler();
        
        // create manager
        manager = new NDPTileManager(this);
        
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
    	BufferedImage img = new BufferedImage(d.width,d.height,BufferedImage.TYPE_INT_RGB);
		Graphics g = img.createGraphics();
		g.setColor(Color.white);
		g.fillRect(0,0,d.width,d.height);
        buffer.setImage(img);
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
    	int rotate = getTileManager().getRotateTransform();
		Dimension is = info.getImageSize();
		ImageTransform it = info.getImageTransform();
		
		// is original horizontal or vertical
		boolean isOrigHrz = (rotate % 2 == 0)?info.isHorizontal():info.isVertical();
		int w = vsize.width;
        int h = vsize.height;
    	boolean hrz = Utils.isHorizontal(viewer.getSize(),is);
    	
    	if(!it.isCropped()){
	    	int aw = (h*is.width)/is.height;
			int ah = (w*is.height)/is.width;
	    	//int sz = (hrz)?(!isOrigHrz)?ah:w:(isOrigHrz)?aw:h;
			
	    	if(hrz){
	    		h = (isOrigHrz)?ah:w;
	    	}else{
	    		w = (isOrigHrz)?aw:h;
	    	}
	    	String itemID = info.getProperties().getProperty("ItemID");
			
	    	if(comm.queryServer(server,"nspGetOverviewImage?ItemID="+itemID+"&FrameWidth="+w+"&FrameHeight="+h,imgHandler)){
	    		Utils.flushImage((Image)imgHandler.getResult());
				img = Utils.createBufferedImage((Image)imgHandler.getResult());
	        }	
			//System.out.println("thumbnail fetch "+(System.currentTimeMillis()-time));
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
		String itemID = info.getProperties().getProperty("ItemID");
		if(comm.queryServer(server,"GetMacroImage?ItemID="+itemID,imgHandler)){
    		img = Utils.createBufferedImage((Image)imgHandler.getResult());
        }	
    	
		return img;
    }
    
    
    /**
     * get slide label (if available)
     * @return null if not available
     */
    public Image getImageLabel(){
    	if(info == null)
    		return null;
    	Image img = getMacroImage();
    	if(img != null)
    		return createLabelFromMacro((BufferedImage)img);
		return img;
    }
   
	/**
	 * Assuming that the label is on the left of a slide, cut
	 * it out from macro image
	 * @param img
	 * @return
	 */
	private BufferedImage createLabelFromMacro(BufferedImage img){
		int h = img.getHeight();
		BufferedImage label = new BufferedImage(h,h,BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D) label.getGraphics();
		g.drawImage(img.getSubimage(0,0,h,h),0,0,null);
		return label;
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
    	xmlHandler = null;
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
     * sign in into server
     */
    public boolean signin(){
    	if(signedIn)
    		return true;
    	// check if we have authentication credentials to sign in
    	String user = ViewerFactory.getProperty("hamamatsu.server.username");
    	String pass = ViewerFactory.getProperty("hamamatsu.server.password");
    	if(user.length() > 0 && pass.length() > 0){
    		 if(comm.queryServer(server,"nspConnect&signin=Sign in&Username="+user+"&Password="+pass, xmlHandler)){
	    		Document doc = (Document) xmlHandler.getResult();
	    		Element st = Utils.getElementByTagName(doc.getDocumentElement(),"status");
	    		if(st != null && "succeeded".equals(st.getTextContent().trim())){
	    			signedIn = true;
	    			return true;
	    		}
    		 }
    	// else try to sign in as guest
    	}else if(comm.queryServer(server,"nspConnect&signin=Sign in as Guest", xmlHandler)){
    		Document doc = (Document) xmlHandler.getResult();
    		Element st = Utils.getElementByTagName(doc.getDocumentElement(),"status");
    		if(st != null && "succeeded".equals(st.getTextContent().trim())){
    			signedIn = true;
    			return true;
    		}
    	}
    	return false;
    }
    
	/**
	 * this is where fun happens
	 */
	public void connect(String image) throws Exception {
		int tileSize = 256;
		long time = System.currentTimeMillis();
		server = viewer.getServer();
		
		// first sign in as guest 
		if(!signin()){
			String user = ViewerFactory.getProperty("hamamatsu.server.username");
			if(user.length() == 0)
				user = "Guest";
			throw new ViewerException("Unable to Sign in as "+user+" into "+server);
		}
		
		String itemID = null;
		if(image.matches("\\d+"))
			itemID = image;
		
		// replace slashes and remove forward slash
		image = image.replace('\\','/');
		if(image.startsWith("/"))
			image = image.substring(1);
		
		
		// strip known suffixes if fits the bill
		if(image.matches(QuickViewer.getSupportedFormat(QuickViewer.HAMAMATSU_TYPE))){
			image = image.substring(0,image.lastIndexOf("."));
		}
		
		
		// lets lookup ID
		if(itemID == null){
			if(pathToID.containsKey(image)){
				itemID = pathToID.get(image);
			}else{
				// load ancestor direcories
				int x = image.lastIndexOf("/");
				if(x > -1)
					getImageList(image.substring(0,x),true);
				else
					getImageList(null,true);
				
				//check again
				if(pathToID.containsKey(image))
					itemID = pathToID.get(image);
				else 
					throw new ViewerException("Could not find image "+image);
			}
		}
		
		// now lets get image details
		if(comm.queryServer(server,"GetImageDetails?ItemID="+itemID,xmlHandler,CONNECTION_TIMEOUT)){
			Document doc = (Document) xmlHandler.getResult();
	          
			/*
			<ndpserveresponse>
				<name>3 - 2010-04-19 11.55.18</name>
				<sourcelens>20.000000</sourcelens>
				<filesize>123695030</filesize>
				<uncompressedsize>2831155200</uncompressedsize>
				<zmin>0</zmin>
				<zmax>0</zmax>
				<zstep>0</zstep>
				<physicalx>18493333</physicalx>
				<physicaly>-390000</physicaly>
				<physicalwidth>14020994</physicalwidth>
				<physicalheight>13979522</physicalheight>
				<pixelwidth>30720</pixelwidth>
				<pixelheight>30720</pixelheight>
				<viewingangle>0.000000</viewingangle>
				<creator>NDP.scan</creator>
				<slidephysicalx>0</slidephysicalx>
				<slidephysicaly>0</slidephysicaly>
				<slidephysicalwidth>76346153</slidephysicalwidth>
				<slidephysicalheight>26153846</slidephysicalheight>
			</ndpserveresponse>
			*/
			
			// create info object
			ImageInfo inf = new ImageInfo();
			inf.getProperties().setProperty("ItemID",itemID);
			inf.setImagePath(itemID);
			inf.setConnectionManager(this);
			int width = 1, height = 1;
			
        	// iterate through XML file
			NodeList nodeList = doc.getDocumentElement().getChildNodes();
			for(int i=0;i<nodeList.getLength();i++){
				if(nodeList.item(i) instanceof Element){
					Element e = (Element) nodeList.item(i);
					
					// now look at metadata
					try{
					if("name".equals(e.getTagName())){
						inf.setName(e.getTextContent().trim());
					}else if("sourcelens".equals(e.getTagName())){
						inf.getProperties().setProperty("source.lens",""+Double.parseDouble(e.getTextContent().trim()));
					}else if("viewingangle".equals(e.getTagName())){
						inf.getProperties().setProperty("viewing.angle",""+Double.parseDouble(e.getTextContent().trim()));
					}else if("pixelwidth".equals(e.getTagName())){
						width = Integer.parseInt(e.getTextContent().trim());
					}else if("pixelheight".equals(e.getTagName())){
						height = Integer.parseInt(e.getTextContent().trim());
					}else if("creator".equals(e.getTagName())){
						inf.getProperties().setProperty("creator",e.getTextContent().trim());
					}else if("filesize".equals(e.getTagName())){
						inf.setFileSize(Long.parseLong(e.getTextContent().trim()));
					}else if("physicalx".equals(e.getTagName())){
						inf.getProperties().setProperty("physical.x",""+Integer.parseInt(e.getTextContent().trim()));
					}else if("physicaly".equals(e.getTagName())){
						inf.getProperties().setProperty("physical.y",""+Integer.parseInt(e.getTextContent().trim()));
					}else if("physicalwidth".equals(e.getTagName())){
						inf.getProperties().setProperty("physical.width",""+Integer.parseInt(e.getTextContent().trim()));
					}else if("physicalheight".equals(e.getTagName())){
						inf.getProperties().setProperty("physical.height",""+Integer.parseInt(e.getTextContent().trim()));
					}else if("slidephysicalx".equals(e.getTagName())){
						inf.getProperties().setProperty("slide.physical.x",""+Integer.parseInt(e.getTextContent().trim()));
					}else if("slidephysicaly".equals(e.getTagName())){
						inf.getProperties().setProperty("slide.physical.y",""+Integer.parseInt(e.getTextContent().trim()));
					}else if("slidephysicalwidth".equals(e.getTagName())){
						inf.getProperties().setProperty("slide.physical.width",""+Integer.parseInt(e.getTextContent().trim()));
					}else if("slidephysicalheight".equals(e.getTagName())){
						inf.getProperties().setProperty("slide.physical.height",""+Integer.parseInt(e.getTextContent().trim()));
					}
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
			
			// set dimension
			inf.setImageSize(new Dimension(width,height));
            inf.setTileSize(new Dimension(tileSize,tileSize));
            
            //get microns per pixel and convert to mm per pixel
            try{
            	double pw = Double.parseDouble(inf.getProperties().getProperty("physical.width"));
            	inf.setPixelSize(((pw*0.000001)/width));
            }catch(NumberFormatException ex){
            	inf.setPixelSize(Constants.APERIO_PXL_SIZE);
            }
            
            ///////////////////////////////////////////////
        	// Because I in fact use regions and not blocks
        	// I can get any resolution I want, so I might 
        	// as well have more levels
        	///////////////////////////////////////////////
            //ArrayList lvls = new ArrayList();
            int levels = 0;
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
                int w = 0;
                int h = 0;
                double z = scales[i];
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
            Dimension hd = (w < 800)?new Dimension(w,h):new Dimension(w/2,h/2);
            Constants.debug("got image meta data ",time);
            
            // now do a fetch
            time = System.currentTimeMillis();
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
        }else{
        	throw new Exception("Couldn't get image meta data for "+image+"\non "+server);
		}	
	}
	
	
	/**
	 * get the list of images on the server
	 * @param path
	 * @param should folder names be included
	 * @return
	 */
	public List<String> getImageList(String path, boolean includeFolders){
		return getImageList(path, includeFolders,true);
	}
	
	
	
	
	/**
	 * get the list of images on the server
	 * @param path
	 * @param should folder names be included
	 * @return
	 */
	public List<String> getImageList(String path, boolean includeFolders,boolean recurse){
		Map<String,String> paths = new LinkedHashMap<String, String>();
		if(viewer != null)
			server = viewer.getServer();
		
		// setup server if unavailable
		if(server == null){
			if(ViewerFactory.getProperties().containsKey("hamamatsu.server.url")){
				try {
					server = new URL(ViewerFactory.getProperties().getProperty("hamamatsu.server.url"));
				} catch (MalformedURLException e) {
					//e.printStackTrace();
				}
			}
		}
		
		// first sign in as guest (just in case) 
		if(!signin()){
			return Collections.EMPTY_LIST;
		}
		
		// figure out path ID first
		String itemID = "";
		if(path == null || path.length() == 0){
			path = "";
		}else{
			// make sure there is a slash at the end
			if(!path.endsWith("/"))
				path += "/";
			
			// we are lucky and path is already there
			if(pathToID.containsKey(path)){
				itemID = "&ItemID="+pathToID.get(path);
			}else if(recurse){
				// we are unlucky and we need to figure out all of the ancestors
				List<String> componentPaths = new ArrayList<String>();
				componentPaths.add(null);
				
				// add component paths to list
				for(int i=path.indexOf("/");i > -1; i=path.indexOf("/",i+1)){
					componentPaths.add(path.substring(0,i)+"/");
				}
				
				// now go over list until we find it
				for(String p: componentPaths){
					// getch ancestors
					getImageList(p,true,false);
					
					//check global table
					if(pathToID.containsKey(path)){
						itemID = "&ItemID="+pathToID.get(path);
						break;
					}
				}
			}
		}
		
		// now lets do search 
		if(comm.queryServer(server,"nspSearch"+itemID,xmlHandler)){
			Document doc = (Document) xmlHandler.getResult();
			Element result = Utils.getElementByTagName(doc.getDocumentElement(),"result");
			if(result != null){
				NodeList nodeList = result.getElementsByTagName("item");
				for(int i=0;i<nodeList.getLength();i++){
					Element item = (Element) nodeList.item(i);
					// get items metadata
					Element id   = Utils.getElementByTagName(item,"id");
					Element name = Utils.getElementByTagName(item,"name");
					Element type = Utils.getElementByTagName(item,"type");
					if(id != null && name != null && type != null){
						// get image path
						String imageID = id.getTextContent().trim();
						String imagePath = name.getTextContent().trim();
						
						// if slide
						if("slide".equals(type.getTextContent().trim())){
							paths.put(imagePath,imageID);
						}else if("folder".equals(type.getTextContent().trim())){
							if(includeFolders)
								paths.put(imagePath+"/",imageID);
						}
					}
				}
			}
		}
		
		// add result to global table
		for(String p : paths.keySet())
			pathToID.put(path+p,paths.get(p));
		
		// return result		
		return new ArrayList<String>(paths.keySet());
	}
}
