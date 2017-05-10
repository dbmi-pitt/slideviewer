package edu.pitt.slideviewer.qview.connection.handler;

import java.io.*;


/**
 * Stream handler that processes incoming stream as Java Object
 * @author tseytlin
 */
public class ObjectStreamHandler implements StreamHandler {
    private Object obj;
    /**
     * Get Java object that was recieved
     * @return
     */
    public Object getResult(){
        return obj;
    }
    
    /**
     * Process input stream as Object stream
     */
    public boolean processStream(InputStream in) throws Exception{
        //recieve object
        ObjectInputStream objIn = null;
        try{
            objIn = new ObjectInputStream(in);
            obj = objIn.readObject();
        }catch(Exception ex){
            throw ex;
        }finally{
            if(objIn != null){
                objIn.close();
            }
        }
        return true;
    }
}