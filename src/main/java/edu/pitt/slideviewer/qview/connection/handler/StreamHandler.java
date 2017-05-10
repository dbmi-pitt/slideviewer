package edu.pitt.slideviewer.qview.connection.handler;
import java.io.InputStream;

/**
 * This interface object handles incoming stream
 * @author tseytlin
 */
public interface StreamHandler {
    /**
     * Process incoming input stream
     * @param in
     */
    public boolean processStream(InputStream in) throws Exception;
   
    /**
     * Get resulting object.
     * @return
     */
    public Object getResult();
}