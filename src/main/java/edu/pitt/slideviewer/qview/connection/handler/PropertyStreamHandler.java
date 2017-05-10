package edu.pitt.slideviewer.qview.connection.handler;

import java.io.*;
import java.util.Properties;
import edu.pitt.slideviewer.qview.connection.handler.StreamHandler;

/**
 * This class handles Aperio INFO format
 * information a|b||c
 * is returned as a List
 * @author tseytlin
 */
public class PropertyStreamHandler implements StreamHandler {
    private Properties prop;
    /**
     * Get Java object that was recieved
     * @return
     */
    public Object getResult(){
        return prop;
    }
    
    /**
     * Process input stream as Object stream
     */
    public boolean processStream(InputStream in) throws Exception{
       
    	prop = new Properties();
    	prop.load(in);
    
        return true;
    }
}