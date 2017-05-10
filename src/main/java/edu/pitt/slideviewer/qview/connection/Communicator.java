package edu.pitt.slideviewer.qview.connection;

import java.io.*;
import java.net.*;

import edu.pitt.slideviewer.qview.connection.handler.StreamHandler;

/**
 * This is class is responsible for client server communication
 * @author tseytlin
 */
public class Communicator {
	private boolean cancel;
    private static Communicator instance;
    private Communicator(){}
    private String cookies;
    
    /**
     * Get instance of communication manager
     * @return
     */
    public static Communicator getInstance(){
        if(instance == null)
            instance= new Communicator();
        return instance;
    }
    
    /**
     * Query servlet for object
     * @param URL server URL
     * @param String server parameters
     * @return Object object returned by the servlet
     */
    public boolean queryServer(URL servlet, String parameters, StreamHandler handler) {
    	return queryServer(servlet,parameters,handler,0);
    }
    
    /**
     * Query servlet for object
     * @param URL server URL
     * @param String server parameters
     * @param Handler that will interpret the result
     * @param timeout in ms
     * @return Object object returned by the servlet
     */
    public boolean queryServer(URL servlet, String parameters, StreamHandler handler, int timeout) {
    	boolean rep = false;
    	try {
            //long time = System.currentTimeMillis();
    		cancel = false;
    		URL url = new URL(servlet.toString()+"?"+escape(parameters));
            URLConnection conn = url.openConnection();
          
            // set cookies if available
            if(cookies != null)
            	conn.setRequestProperty("Cookie",cookies);
            
            
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            
            // Turn off caching
            conn.setDoOutput(true);
            conn.setUseCaches( false );
            
            //handle stream
            InputStream in = conn.getInputStream();
            
            // are there any cookies??? yammm
            if(conn.getHeaderField("Set-Cookie") != null){
            	cookies = conn.getHeaderField("Set-Cookie");
            }
            
            //System.out.println("\tdata fetched "+(System.currentTimeMillis()-time)+" ms");
            //time = System.currentTimeMillis();
            rep = handler.processStream(in);
            //System.out.println("\tdata processed "+(System.currentTimeMillis()-time)+" ms");
        } catch(InterruptedException ex){ 
        	//System.err.println("interrupted");
        	// communication was interrupted
            return false;
        } catch(SocketTimeoutException ex){
        	System.err.println("request ("+parameters+") timed out after "+timeout+" ms");
        	return false;
        } catch ( Exception e ) {
        	System.err.println("communicator error: "+e.getClass().getName()+" "+e.getMessage());
        	//e.printStackTrace();
            return false;
        }
        return rep;
    }

    /**
     * escape url
     * @param s
     * @return
     */
    public static String escape(String s){
    	//return Utils.filterURL(s);
    	return s.replaceAll(" ","%20");
    }
    
    /**
     * Cancel current communication
     */
    public void cancel(){
    	cancel = true;
    }
    
	/**
	 * @return the processing
	 */
	public final boolean isCanceled() {
		return cancel;
	}
    
}
