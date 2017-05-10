package edu.pitt.slideviewer;

import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.ImageIcon;

import edu.pitt.slideviewer.simple.*;
//import edu.pitt.slideviewer.xippix.*;
import edu.pitt.slideviewer.qview.*;
import edu.pitt.slideviewer.qview.aperio.AperioTileConnectionManager;
import edu.pitt.slideviewer.qview.connection.Communicator;
import edu.pitt.slideviewer.qview.connection.handler.ListHandler;
import edu.pitt.slideviewer.qview.connection.handler.StreamHandler;
import edu.pitt.slideviewer.qview.hamamatsu.NDPConnectionManager;
/**
 * This class creates viewer instances. 
 * The idea is to hide implementation details from the user,
 * hence users need to use this class to get an instance of Viewer
 * interface.
 */
public class ViewerFactory {
	private static final String logoIcon = "/icons/dbmi-logo.gif";
	private static ImageIcon logo;
	
	// constants
	private static boolean noXippix = false;
	
	public static final int SIMPLE = 0;
	public static final int MSCOPE = 1;
	//public static final int XIPPIX = 3;
    public static final int APERIO = 4;
    public static final int OPENSLIDE = 5;
    public static final int HAMAMATSU = 6;
    public static final int ZEISS = 7;
    
	//private static Properties props = new Properties();
	private static Map<String,Properties> propertyMap = new LinkedHashMap<String,Properties>();
	private static String currentLocation;
	
	static {
		// check if xippix is available
		try{
			Class.forName("edu.pitt.slideviewer.xippix.XippixViewer");
		}catch(Throwable ex){
			noXippix = true;
		}
	}
	
    
    /**
     * Set Server properties
     */
    public static void setProperties(Properties p){
        addProperties("default",p);
    }
    
    /**
     * Set Server properties
     */
    public static void addProperties(String location, Properties p){
        propertyMap.put(location,p);
        currentLocation = location;
    }
    
    /**
     * Set Server properties
     */
    public static void removeProperties(String location){
        propertyMap.remove(location);
    }
    
    /**
     * get a list of registered locations
     * @return
     */
    public static Collection<String> getPropertyLocations(){
    	return propertyMap.keySet();
    }
    
    /**
     * set current property location
     * @param location
     */
    public static void setPropertyLocation(String location){
    	currentLocation = location;
    }
    
    /**
     * get current property location
     * @param location
     */
    public static String getPropertyLocation(){
    	return currentLocation;
    }
    
    
    /**
	 * @return the props
	 */
	public static Properties getProperties() {
		// make sure such location is there
		if(propertyMap.containsKey(currentLocation))
			return propertyMap.get(currentLocation);
		// if not, are there any properties set?
		if(!propertyMap.values().isEmpty())
			return propertyMap.values().iterator().next();
		// else just make new object to avoid NPE
		//return new Properties();
		Properties p = new Properties();
		propertyMap.put("",p);
		return p;
	}
    
	/**
	 * get property
	 * @param key
	 */
	public static String getProperty(String key){
		if(getProperties().containsKey(key))
			return getProperties().getProperty(key).trim();
		return "";
	}
	
	/**
	 * Create an instance of Viewer.
	 * qview viewer instance is returned by default.
	 */
	public static Viewer getViewerInstance(){
		return getViewerInstance(APERIO);
	}
	
	/**
	 * Create an instance of Viewer.
	 * @param int type - Possible values are MSCOPE,XIPPIX, SIMPLE
	 */
	public static Viewer getViewerInstance(int type){
        URL server = null;
        
        // if no xippix, don't initialize it
        //if(noXippix && type == XIPPIX)
        	type = OPENSLIDE;
                
        
        switch (type){
			case SIMPLE:
				SimpleViewer viewer = new SimpleViewer();
				try{
                	server  = new URL(getProperty("simple.server.url"));
                	viewer.setServer(server);
    		    }catch(MalformedURLException ex){
                	System.err.println("Error: server URL is "+getProperty("simple.server.url"));
                    //ex.printStackTrace();
                }
    			return viewer;
                
			
			/*
			case MSCOPE:
                try{
                    server  = new URL(props.getProperty("mscope.server.url",props.getProperty("image.server.url")));
                    servlet = new URL(props.getProperty("mscope.servlet.url",props.getProperty("image.servlet.url")));
                }catch(MalformedURLException ex){
                	System.err.println("URL: "+props.getProperty("mscope.server.url"));
                    ex.printStackTrace();
                }
                return new mScopeViewer(server,servlet);
            */
			/*case XIPPIX:
                try{
                    server  = new URL(getProperty("xippix.server.url"));
                }catch(MalformedURLException ex){
                	System.err.println("Error: server URL is "+getProperty("xippix.server.url"));
                    //ex.printStackTrace();
                }
                return new XippixViewer(server);*/
            case APERIO:
                try{
                	server  = new URL(getProperty("aperio.server.url"));
                }catch(MalformedURLException ex){
                	System.err.println("Error: server URL is "+getProperty("aperio.server.url"));
                   // ex.printStackTrace();
                }
            	return new QuickViewer(server,QuickViewer.APERIO_TYPE);
            case OPENSLIDE:
                try{
                	server  = new URL(getProperty("openslide.server.url"));
                }catch(MalformedURLException ex){
                	System.err.println("Error: server URL is "+getProperty("openslide.server.url"));
                   // ex.printStackTrace();
                }
            	return new QuickViewer(server,QuickViewer.OPENSLIDE_TYPE); 
            case HAMAMATSU:
                try{
                   	server  = new URL(getProperty("hamamatsu.server.url"));
                }catch(MalformedURLException ex){
                	System.err.println("Error: server URL is "+getProperty("hamamatsu.server.url"));
                    //ex.printStackTrace();
                }
            	return new QuickViewer(server,QuickViewer.HAMAMATSU_TYPE); 
            case ZEISS:
                try{
                   	server  = new URL(getProperty("zeiss.server.url"));
                }catch(MalformedURLException ex){
                	System.err.println("Error: server URL is "+getProperty("zeiss.server.url"));
                    //ex.printStackTrace();
                }
            	return new QuickViewer(server,QuickViewer.ZEISS_TYPE); 
            default:
				return new SimpleViewer();
		}
	}
	
	/**
	 * Create an instance of Viewer.
	 * @param String type - Possible values are "MSCOPE","XIPPIX", "SIMPLE"
	 */
	public static Viewer getViewerInstance(String str){
		int type = SIMPLE;
		
		// set aperio as default
		if(str == null)
			str = QuickViewer.getViewerType(QuickViewer.APERIO_TYPE);
		
		 // if no xippix, don't initialize it
        if(noXippix && str.equalsIgnoreCase("xippix"))
        	str = QuickViewer.getViewerType(QuickViewer.OPENSLIDE_TYPE);
		
		
		if(str.equalsIgnoreCase(QuickViewer.getViewerType(QuickViewer.APERIO_TYPE)))
	        type = APERIO;
		else if(str.equalsIgnoreCase(QuickViewer.getViewerType(QuickViewer.OPENSLIDE_TYPE)))
	        type = OPENSLIDE;
		else if(str.equalsIgnoreCase(QuickViewer.getViewerType(QuickViewer.HAMAMATSU_TYPE)))
	        type = HAMAMATSU;
		else if(str.equalsIgnoreCase(QuickViewer.getViewerType(QuickViewer.ZEISS_TYPE)))
	        type = ZEISS;
		else if(str.equalsIgnoreCase("qview")){
			// for backword compatibility
			type = APERIO;
			if(!getProperties().containsKey("aperio.server.url"))
				getProperties().setProperty("aperio.server.url",getProperty("qview.server.url"));
			if(!getProperties().containsKey("aperio.image.dir"))
				getProperties().setProperty("aperio.image.dir",getProperty("qview.image.dir"));
		}
		//}else if(!noXippix && str.equalsIgnoreCase(XippixViewer.getViewerType()))
		//	type = XIPPIX;
		else if(str.equalsIgnoreCase(SimpleViewer.getViewerType()))
			type = SIMPLE;
		/*
		else if(str.equalsIgnoreCase(mScopeViewer.getViewerType()))
			type = MSCOPE;
		*/
		return getViewerInstance(type);
	}

    
    /**
     * Based on input filename extention, recommend a viewer type
     * that supports this format the best.
     * NOTE: This is a hack, because viewer capabilities should
     * be decided not just on file extension alone
     * @param name filename (with file extention, dahh :)
     * @return
     */
    public static String recomendViewerType(String name){
    	if(name == null)
    		return null;
    	
    	// lowercase name to ease the matching
    	name = name.toLowerCase();
    	if(name.lastIndexOf("/") > -1){
    		name = name.substring(name.lastIndexOf("/")+1);
    	}
    	
    	// this is just another UGLY hack, but I don't know what else to do
    	//if(!noXippix && name.toLowerCase().startsWith("apx_") && name.matches(XippixViewer.getSupportedFormat()))
    	//	return XippixViewer.getViewerType();
    	
    	
    	// ask each viewer what it supports
    	if(checkServer("aperio") && name.matches(QuickViewer.getSupportedFormat(QuickViewer.APERIO_TYPE)))
     		return QuickViewer.getViewerType(QuickViewer.APERIO_TYPE);
    	else if(checkServer("hamamatsu") && name.matches(QuickViewer.getSupportedFormat(QuickViewer.HAMAMATSU_TYPE)))
     		return QuickViewer.getViewerType(QuickViewer.HAMAMATSU_TYPE);
    	else if(checkServer("zeiss") && name.matches(QuickViewer.getSupportedFormat(QuickViewer.ZEISS_TYPE)))
     		return QuickViewer.getViewerType(QuickViewer.ZEISS_TYPE);
    	//else if(!noXippix && checkServer("xippix") &&  name.matches(XippixViewer.getSupportedFormat()))
     	//	return XippixViewer.getViewerType();
    	else if(checkServer("openslide") && name.matches(QuickViewer.getSupportedFormat(QuickViewer.OPENSLIDE_TYPE)))
     		return QuickViewer.getViewerType(QuickViewer.OPENSLIDE_TYPE);
    	else if(name.matches(SimpleViewer.getSupportedFormat()))
    		return SimpleViewer.getViewerType();
    	//else if(name.matches(mScopeViewer.getSupportedFormat()))
    	//	return mScopeViewer.getViewerType();
    	//else 
    	//	return SimpleViewer.getViewerType();
    	return null;
    }

    private static boolean checkServer(String type){
    	// if valid non-null url, then cool
    	try{
    		new URL(getProperty(type+".server.url"));
        }catch(Exception ex){
        	return false;
        }
    	return true;
    }
    
	
    
    /**
	 * convinient method to get image properties file
	 * @param filename
	 * @return
	 */
	public static ImageProperties getImageProperties(String filename) throws ViewerException{
		return getImageProperties(filename,null);
	}
	
	/**
	 * convinient method to get image properties file
	 * @param filename
	 * @param type of server
	 * @return
	 */
	public static ImageProperties getImageProperties(String filename, String t) throws ViewerException{
		ImageProperties prop = null;
		String type = (t == null)?recomendViewerType(filename):t;
		if(type == null)
			return null;
		try{
			Viewer viewer = getViewerInstance(type);
			String dir = getProperty(type+".image.dir");
			if(!filename.startsWith(dir))
				filename = dir+filename;
			viewer.openImage(filename);
			prop = viewer.getImageProperties();
			viewer.dispose();
		}catch(Exception ex){
			//ex.printStackTrace();
			throw new ViewerException("failed to open slide "+filename,ex);
		}
		return prop;
	}
	
	
	/**
	 * convinient method to get image properties file
	 * @param filename
	 * @param type of server
	 * @return
	 */
	public static ImageProperties getImageProperties(String filename, URL server, String type) throws ViewerException{
		return getImageProperties(filename, server, type,250);
	}
	
	/**
	 * convinient method to get image properties file
	 * @param filename
	 * @param type of server
	 * @return
	 */
	public static ImageProperties getImageProperties(String filename, URL server, String type, int size) throws ViewerException{
		ImageProperties prop = null;
		try{
			Viewer viewer = getViewerInstance(type);
			viewer.setSize(size,size);
			viewer.setServer(server);
			viewer.openImage(filename);
			prop = viewer.getImageProperties();
			viewer.dispose();
		}catch(Exception ex){
			//ex.printStackTrace();
			throw new ViewerException("failed to open slide "+filename,ex);
		}
		return prop;
	}

	/**
	 * @return the logo
	 */
	public static ImageIcon getLogoIcon() {
		if(logo == null)
			logo =  new ImageIcon(ViewerFactory.class.getResource(logoIcon));
		return logo;
	}

	/**
	 * Set custom log that is displayed in empty viewer
	 * @param logo the logo to set
	 */
	public static void setLogoIcon(ImageIcon logo) {
		ViewerFactory.logo = logo;
	}
	
	
	/**
	 * get the list of images on the server
	 * @return
	 */
	public static List<String> getImageList(){
		return getImageList(null,false);
	}
	
	/**
	 * get the list of images on the server
	 * @param path
	 * @param should folder names be included
	 * @return
	 */
	public static List<String> getImageList(String path, boolean includeFolders){
		List<String> filelist = null;
		
		// check if there are other server specific implementations
		
		// if hamamatsu server is available, but there is a problem w/ a list server
		if(getProperties().containsKey("hamamatsu.server.url")){
			String listServer = getProperty("image.list.server.url");
			if(listServer.trim().length() == 0 || listServer.trim().equals(getProperty("hamamatsu.server.url").trim())){
				NDPConnectionManager conn = new NDPConnectionManager(null);
				List<String> slides = conn.getImageList(path, includeFolders);
				conn.dispose();
				return slides;
			}
		}
		
		if(getProperties().containsKey("aperio.server.url")){
			String listServer = getProperty("image.list.server.url");
			if(listServer.trim().length() == 0 || listServer.equals(getProperty("aperio.server.url"))){
				AperioTileConnectionManager conn = new AperioTileConnectionManager(null);
				filelist = conn.getImageList(path, includeFolders);
				conn.dispose();
			}
		}
		
		// this is the default implementation using list server
		if(filelist == null)
			filelist = getFileList(path);
	
		// now sort into filders and files, sort and filter images
		List<String> folders = new ArrayList<String>();
		List<String> files = new ArrayList<String>();
		List<String> formats = getSupportedFormats();
		for(String file: filelist){
			// add matchin files to the list
			for(String format: formats){
				if(file.toLowerCase().matches(format)){
					//TODO: this is a hack and it is ugly, but unfortunatly necessary
					//if file matches SimpleViewer format, but there is a corresponding file
					//that belongs to some TRUE format, then ignore it
					boolean ignore = false;
					if(file.toLowerCase().endsWith(".jpg")){
						String name = file.substring(0,file.length()-".jpg".length());
						//ignore files that already exist as VMS or TIF in the list
						if(filelist.contains(name+".vms") || filelist.contains(name+".tif"))
							ignore = true;
						//ignore VMS special files
						if(name.matches(".+(_map2.*|_macro|\\([01,\\-]+\\))"))
							ignore = true;
						
					}
					
					if(!ignore)
						files.add(file);
					break;
				}
			}
			// add folders to the list
			if(includeFolders && file.endsWith("/")){
				folders.add(file);
			}
		}
		// do sort
		Collections.sort(folders);
		Collections.sort(files);
		
		// combine lists
		List<String> list = new ArrayList<String>();
		list.addAll(folders);
		list.addAll(files);
		
		return list;
	}
	
	/**
	 * get list of files on the server
	 * @param path
	 * @return
	 */
	public static List<String> getFileList(String path){
		if(getProperties().containsKey("image.list.server.url")){
			try{
				URL url = new URL(getProperty("image.list.server.url"));
				Communicator comm = Communicator.getInstance();
				StreamHandler handler = new ListHandler();
				comm.queryServer(url,"action=list"+((path != null)?"&path="+path:""),handler);
				return (List<String>) handler.getResult();
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
		return Collections.EMPTY_LIST;
	}
	
	
	/**
	 * get a list of all supported image formats
	 * (based on the suffix :( )
	 * This method returns a list of regular expressions that can String.match()
	 * to one or the other format as oppose to human readable list
	 * @return
	 */
	public static List<String> getSupportedFormats(){
		List<String> list = new ArrayList<String>();
		list.add(QuickViewer.getSupportedFormat(QuickViewer.APERIO_TYPE));
		list.add(QuickViewer.getSupportedFormat(QuickViewer.OPENSLIDE_TYPE));
		list.add(QuickViewer.getSupportedFormat(QuickViewer.HAMAMATSU_TYPE));
		list.add(QuickViewer.getSupportedFormat(QuickViewer.ZEISS_TYPE));
		list.add(SimpleViewer.getSupportedFormat());
		//if(!noXippix)
		//	list.add(XippixViewer.getSupportedFormat());
		return list;
	}
}
