package edu.pitt.slideviewer.qview.aperio;

import java.awt.*;
import java.io.InputStream;
import javax.imageio.*;
import edu.pitt.slideviewer.qview.connection.handler.StreamHandler;


/**
 * This class handles input stream
 * and returns an image object
 * It uses ImageIO to read input
 * ImageIO is slow, but through plugins can read
 * JPEG, TIFF, JPEG2000 etc...
 * @author tseytlin
 */
public class AperioImageHandler implements StreamHandler{
    private Image img;
   
    /**
     * Get Java object that was recieved
     * @return
     */
    public Object getResult(){
        return img;
    }
    
    /**
     * Process input stream as Object stream
     */
    public boolean processStream(InputStream in) throws Exception{
        try{
            img = ImageIO.read(in);
        }catch(Exception ex){
            throw ex;
        }
        return true;
    }
}
