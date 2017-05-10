package edu.pitt.slideviewer.qview.connection;

import java.awt.*;
import java.util.*;

import edu.pitt.slideviewer.ImageProperties;
/**
 * This class encapsulates varies image meta data
 * @author tseytlin
 *
 */
public class ImageInfo extends ImageProperties {
    public static final int NONE = 0, LZW = 1, JPEG=2, JPEG2000=3; 
    private int compressionType,compressionQuality;
    //private int rotate,flip;
    private long fileSize;
    private Map<Double,PyramidLevel> levels;
    private Dimension tImageSize;
    private Image tThumbnail;
    private ConnectionManager connection;
    private Point imageOffset;
    
    public Point getImageOffset() {
    	if(imageOffset == null)
    		imageOffset = new Point();
		return imageOffset;
	}

	public void setImageOffset(Point imageOffset) {
		this.imageOffset = imageOffset;
	}

	public ConnectionManager getConnectionManager() {
		return connection;
	}
	
    public void setConnectionManager(ConnectionManager connection) {
		this.connection = connection;
	}
	
	
	
	public Image getLabel() {
		if(label == null && connection != null)
			setLabel(connection.getImageLabel());
		return super.getLabel();
	}

	public Image getMacroImage() {
		if(macro == null && connection != null)
			setMacroImage(connection.getMacroImage());
		return super.getMacroImage();
	}

	/**
     * Scale thumbnail
     */
    public void scaleThumbnail(int w, int h){
    	if(thumbnail != null){
    		if(isHorizontal()){
	        	h = (int)((w * thumbnail.getHeight(null))/thumbnail.getWidth(null));
	        }else{
	        	w = (int)((h * thumbnail.getWidth(null))/thumbnail.getHeight(null));
	        }
    		thumbnail = thumbnail.getScaledInstance(w,h,Image.SCALE_DEFAULT);
    		tThumbnail = null;
    	}
    }
    
    /**
     * This class describes image layers
     * @author tseytlin
     */
    public static class PyramidLevel implements Comparable{
        private Dimension levelSize,tileSize;
      	private double scale;
        private int levelNumber;
        private double resolution;
        private Rectangle levelRectangle;
        
        public Rectangle getLevelRectangle() {
			return levelRectangle;
		}
		public void setLevelRectangle(Rectangle levelRectangle) {
			this.levelRectangle = levelRectangle;
		}
		/**
		 * @return the levelNumber
		 */
		public int getLevelNumber() {
			return levelNumber;
		}
		/**
		 * @param levelNumber the levelNumber to set
		 */
		public void setLevelNumber(int levelNumber) {
			this.levelNumber = levelNumber;
		}
		
		public Dimension getTileSize() {
			return tileSize;
		}
		public void setTileSize(Dimension tileSize) {
			this.tileSize = tileSize;
		}
		public double getResolution() {
			return resolution;
		}
		public void setResolution(double resolution) {
			this.resolution = resolution;
		}
			
		/**
         * @return the levelSize
         */
        public Dimension getLevelSize() {
            return levelSize;
        }
        /**
         * @param levelSize the levelSize to set
         */
        public void setLevelSize(Dimension levelSize) {
            this.levelSize = levelSize;
        }
        /**
         * @return the scale
         */
        public double getZoom() {
            return scale;
        }
        /**
         * @param scale the scale to set
         */
        public void setZoom(double scale) {
            this.scale = scale;
        }
        
        /**
         * Compare to other level
         */
        public int compareTo(Object obj){
        	if(obj instanceof PyramidLevel){
        		return (int)(((PyramidLevel) obj).getZoom()*100 - scale*100);
        	}else
        		return 0;
        }
    }

    /**
     * @return the compressionQuality
     */
    public int getCompressionQuality() {
        return compressionQuality;
    }
    /**
     * @param compressionQuality the compressionQuality to set
     */
    public void setCompressionQuality(int compressionQuality) {
        this.compressionQuality = compressionQuality;
    }
    /**
     * @return the fileSize
     */
    public long getFileSize() {
        return fileSize;
    }
    /**
     * @param fileSize the fileSize to set
     */
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    /**
     * @return the levels
     */
    public Map<Double,PyramidLevel> getLevels() {
    	if(levels == null)
    		levels = new TreeMap<Double, PyramidLevel>();
    	return levels;
    }
 
    /**
     * @param levels the levels to set
     *
    public void setLevels(PyramidLevel[] levels) {
        this.levels = new TreeMap<Double,PyramidLevel>();
        // create list of scales
        double [] scales = new double [levels.length];
        for(int i=0;i<levels.length;i++){
        	this.levels.put(levels[i].getZoom(),levels[i]);
        	scales[i] = levels[i].getZoom();
        }
        setScales(scales);
    }
    */
    
    public double [] getScales(){
    	double [] scales = new double [getLevelCount()];
    	int i=0;
    	for(Double key: getLevels().keySet())
    		scales[i++] = key; 
    	return scales;
    }
    
    /**
     * add new level
     * @param level
     */
    public void addLevel(PyramidLevel level){
    	getLevels().put(level.getZoom(),level);
    }
    
    /**
     * Get number of levels
     * @return
     */
    public int getLevelCount(){
        return  getLevels().size();
    }
    /**
     * @return the compressionType
     */
    public int getCompressionType() {
        return compressionType;
    }
    /**
     * @param compressionType the compressionType to set
     */
    public void setCompressionType(int compressionType) {
        this.compressionType = compressionType;
    }
    
   
    public void sortLevels(){
    	if(levels != null){
    		//Arrays.sort(levels);
    	}
    }
    
    /**
     * Given scale factor, find a higher resolution
     * level. That is level.getZoom() >= scale
     * @param scale
     * @return
     */
    public PyramidLevel getHigherLevel(double scale){
    	/*PyramidLevel lvl = null;
    	if(levels != null){
    		for(int i=levels.length-1;i>=0;i--){
    			lvl = levels[i];
    			if(lvl.getZoom() >= scale)
    				return lvl;
    		}
    	}*/
    	double [] scales = getScales();
    	for(int i=0;i<scales.length;i++){
			if(scales[i] >= scale)
				return getLevel(scales[i]);
		}
    	return null;
    }
    
    /**
     * Given scale factor, find a lower resolution
     * level. That is level.getZoom() <= scale
     * @param scale
     * @return
     */
    public PyramidLevel getLowerLevel(double scale){
    	/*PyramidLevel lvl = null;
    	if(levels != null){
    		for(int i=0;i<levels.length;i++){
    			lvl = levels[i];
    			if(lvl.getZoom() <= scale)
    				return lvl;
    		}
    	}*/
    	double [] scales = getScales();
    	for(int i=scales.length-1;i>=0;i--){
			if(scales[i] <= scale)
				return getLevel(scales[i]);
		}
    	return null;
     }

    /**
     * Given scale factor, find an exact resolution
     * level. That is level.getZoom() == scale
     * @param scale
     * @return
     */
    public PyramidLevel getLevel(double scale){
    	return getLevels().get(scale);
    }
	
	/**
	 * get html descrption
	 *
	public String getHTMLDescription(){
    	// fetch and organize useful data
    	Dimension d = getImageSize();
    	Dimension t = getTileSize();
    	ArrayList slist = new ArrayList();
    	double [] scales = getScales();
    	for(int i=0;i<scales.length;i++)
    		slist.add(""+Math.round(scales[i]*100));
    	Collections.reverse(slist);
    	String compression = "";
    	switch(getCompressionType()){
    		case(ImageInfo.LZW): compression = "LZW"; break;
    		case(ImageInfo.JPEG): compression = "JPEG"; break;
    		case(ImageInfo.JPEG2000): compression = "JPEG2000"; break;
    		default: compression = "NONE";
    	}
    
    	// constract info label
     	return "<html><table border=0 width=300>" +
		"<tr><td>image name:</td><td>"+getName()+"</td></tr>"+
		"<tr><td>image size:</td><td>"+d.width+" x "+d.height+"</td></tr>"+
		"<tr><td>tile size:</td><td>"+t.width+" x "+t.height+"</td></tr>"+
		"<tr><td>pixel size:</td><td>"+getPixelSize()+" mm</td></tr>"+
		"<tr><td>zoom levels:</td><td>"+slist+"</td></tr>"+
		"<tr><td>compression:</td><td>"+compression+"</td></tr></table>";
	}
	*/
	
	/**
	 * correct image size based on transformation
	 */
	public Dimension getImageSize(){
		//if(tImageSize == null)
		//	tImageSize = Utils.getRotatedDimension(imageSize,getImageTransform().getRotationTransform());
		if(tImageSize == null)
			tImageSize = getImageTransform().getTransformedImageSize();
		return tImageSize;
	}
	
	/**
	 * correct image size based on transformation
	 */
	public Dimension getOriginalImageSize(){
		return super.getImageSize();
	}
	
	/**
	 * get original thumbnail
	 * @return
	 */
	public Image getOriginalThumbnail(){
		return super.getThumbnail();
	}
	
	/**
	 * set transformed thumbnail
	 */
	public Image getThumbnail(){
		// make sure thumbnail is there
		if(thumbnail == null && connection != null)
			setThumbnail(connection.getImageThumbnail());
		
		// do the transformed thumbnail
		if(tThumbnail == null){
			//tThumbnail = Utils.getTransformedImage(thumbnail,rotate,flip);
			tThumbnail = getImageTransform().getTransformedImage(thumbnail);
		}
		return tThumbnail;
	}
	
	public void setThumbnail(Image img){
		//TODO: handle original non- cropped thumbnail
		super.setThumbnail(img);
		tThumbnail = null;
	}
	
	
	/**
	 * transforms were updated
	 */
	public void updateTransforms(){
		tImageSize = null;
		tThumbnail = null;
	}
}
