package edu.pitt.slideviewer;

import java.awt.*;

/**
 * This is a slide navigator. It enables users to navigate slide via 
 * thumbnail slide.
 */
public interface Navigator {
	/**
	 * Get Navigator component that can be placed
	 * in some sort of container Ex. applet, JFrame
	 * @return Component
	 */	
	public Component getComponent();
	
	/**
	 * Set navigator panel size
	 * @param Dimension d
	 */
	public void setSize(Dimension d);
	
	/**
	 * Get navigator panel size
	 * @param Dimension d
	 */
	public Dimension getSize();

	/**
	 * release all resources
	 */
	public void dispose();
	
	/**
	 * resize content
	 */
	public void validate();
	
	
    /**
     * Display viewer exploration history "green mold"
     * @param b
     */
    public void setHistoryVisible(boolean b);
	
}
