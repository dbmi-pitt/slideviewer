package edu.pitt.slideviewer;

import java.text.*;
/**
 * This class is a collection of
 * varies static constants
 * @author tseytlin
 */
public class Constants {
    public static final boolean DEBUG = false;
	
	// pixel size in mm for varies microscopes
    public static final double APERIO_PXL_SIZE = 0.00047259;
    public static final double INTERSCOPE_PXL_SIZE = 0.000345662;
    // THIS IS WHAT Aperio reports for pixel size: 0.0004667;
    
    // viewer properties
    public static final String VIEW_CHANGE  = "ViewChange";
    public static final String VIEW_OBSERVE = "ViewObserve";
    public static final String VIEW_RESIZE  = "ViewResize";
    public static final String IMAGE_CHANGE = "ImageChange";
    public static final String IMAGE_TRANSFORM = "ImageTransform";
    public static final String SKETCH_DONE  = "sketchDone";
    public static final String NAVIGATOR    = "Navigator";
    public static final String MAGNIFIER    = "Magnifier";
    public static final String UPDATE_SHAPE = "UpdateShape";
    public static final String UPDATE_SHAPE_TAGS = "UpdateShapeTags";
    public static final String UPDATE_SHAPE_COLOR = "UpdateShapeColor";
    public static final String DELETE_SHAPE = "DeleteShape";
    public static final String PLAYBACK     = "Playback";
    public static final String MAGNIFIER_OBSERVE = "MagnifierObserve";
    //  separator char
	public static final String ENTRY_SEPARATOR = "|";
	
	// tags
	public static final String VIEW_TAG = "VIEW";
	public static final String OBSERVE_TAG = "OBSERVE";
	public static final String IMAGE_TAG = "IMAGE";
	public static final String MARKER_TAG = "MARKER";
	public static final String NAVIGATOR_TAG = "THUMB";
	public static final String RESIZE_TAG = "RESIZE";
	public static final String UPDATE_MARKER_TAG = "UPDATE";
	
	// date format
	public static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd-yy HH:mm:ss.SSS");
	
	
	/**
	 * debug string
	 * @param o
	 */
	public static void debug(Object o){
		debug(o,0);
	}
	public static void debug(Object o,long time){
		if(DEBUG)
			System.out.println(o+((time > 0)?" in "+(System.currentTimeMillis()-time+" ms"):""));
	}
}
