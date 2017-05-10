package edu.pitt.slideviewer.qview.aperio;

import java.io.*;
import edu.pitt.slideviewer.qview.connection.handler.StreamHandler;

/**
 * This class handles Aperio INFO format
 * information a|b||c
 * is returned as a List
 * @author tseytlin
 */
public class AperioInfoHandler implements StreamHandler {
    private String [] list;
    /**
     * Get Java object that was recieved
     * @return
     */
    public Object getResult(){
        return list;
    }
    
    /**
     * Process input stream as Object stream
     */
    public boolean processStream(InputStream in) throws Exception{
        StringBuffer buf = new StringBuffer();
        
        //recieve object
        BufferedReader stream = null;
        try{
            stream = new BufferedReader(new InputStreamReader(in));
            for(String line=stream.readLine(); line != null; line=stream.readLine()){
                buf.append(line);
            }
        }catch(Exception ex){
            throw ex;
        }finally{
            if(stream != null){
                stream.close();
            }
        }
        // process object :)
        list = (String []) buf.toString().split("\\|");
        return true;
    }
}
