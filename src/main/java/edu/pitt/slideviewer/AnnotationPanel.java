package edu.pitt.slideviewer;

import java.awt.*;
import edu.pitt.slideviewer.markers.Annotation;

public interface AnnotationPanel {
	/**
	 * Adds a component to the annotation layer. 
	 * Ex. JLabel is probably the most useful component to add.
	 * MOTE: Make sure you use component.setBounds() to indicate where
	 * this component should be drawn.
	 * @param Component - component to be added to the panel
	 */
	public void addComponent(Component comp);
	
	/**
	 * Removes a component from the annotation layer. 
	 * @param Component - component to be added to the panel
	 */
	public void removeComponent(Component comp);
	
	/**
	 * Removes all components attached to the viewer.
	 */
	public void removeComponents(); 
	
	/**
	 * Get all components attached to the viewer.
	 */
	public Component [] getComponents(); 
	
	
	/**
	 * This method is here for convinience. It is the same as:
	 * getMarkerManager().addTutorMarker()
	 * @param Annotation annotation to add
	 */
	public void addAnnotation(Annotation tm);
	
	/**
	 * Make annotation panel visible/invisible
	 * @param boolean b true to make annotation panel visible
	 */
	public void setVisible(boolean b);
	
	/** 
	 * Is annotation panel visible?
	 * @return boolean 
	 */
	public boolean isVisible(); 
	
	/**
	 * Start drawing a given Annotation in the viewer.
	 * The marker gets automaticly added to AnnotationManager
	 * @param Annotation - a shape to be sketched.
	 */	
	public void sketchAnnotation(Annotation tm);
	
	/**
	 * Finish drawing privously given Annotation in the viewer.
	 */
	public void sketchDone();
	
	/**
	 * Get the 'in sketch' mode status
	 * @return boolean true if annotation panel is in sketch mode.
	 */	
	public boolean isSketching();
	
	/**
	 * Get Annotation currently being sketched
	 * @return Annotation 
	 */
	public Annotation getCurrentAnnotation();
	
	/**
	 * Get an instance of AnnotationManager. AnnotationManager handles all
	 * Annotation drawings and keeps track of them.
	 */
	public AnnotationManager getAnnotationManager();
	
	/**
	 * release all resources
	 */
	public void dispose();
	
	/**
	 * draw annotation panel
	 * @param g
	 */
	public void draw(Graphics g);
}
