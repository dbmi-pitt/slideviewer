package edu.pitt.slideviewer.qview.connection;
import java.awt.*;
import java.awt.event.ComponentListener;


/**
 * This interface describes the behaviour of the
 * connection to the image server
 * @author tseytlin
 */
public interface ConnectionManager {
    /**
     * Get meta data for slide image
     * @return ImageInfo object
     */
    public ImageInfo getImageInfo();
    
    
    
    /**
     * get image thumbnail of appropriate size
     * (size of the current viewer window)
     * @return
     */
    public Image getImageThumbnail();
    
    
    /**
     * get slide label (if available)
     * @return null if not available
     */
    public Image getImageLabel() ;
    
    
    /**
     * get macro image for a slide
     * @return
     */
    public Image getMacroImage();
    
    /**
     * Get image of a given region at 
     * @param region specified in absolute image coordinates
     * @param scale factor
     * @return Image, null if no image is loaded
     */
    public void drawImageRegion(Graphics g, Rectangle r, double scale);
    
    
    /**
     * Connect to image server and load image
     * @param image  - path of the image
     */
    public void connect(String image) throws Exception;
    
    
    /**
     * Disconnect from image server
     */
    public void disconnect();
    
    /**
     * Release all resources (like distractor)
     */
    public void dispose();
    
    /**
     * Return component listener. This is used when viewer is
     * resized
     * @return
     */
    public ComponentListener getComponentListener();
}
