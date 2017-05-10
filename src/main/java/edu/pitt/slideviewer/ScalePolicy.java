package edu.pitt.slideviewer;

/**
 * This interface enforces a policy 
 * for valid zoom/scale levels 
 * @author tseytlin
 */
public interface ScalePolicy { 
    /**
     * Return the closest valid equivalent for
     * given scale factor
     * @param given scale factor
     * @param which valid scale to return
     * @return valid scale factor
     */
    public double getValidScale(double scl);
    /**
     * Check if given scale factor is valid for
     * this policy class
     * @param given scale factor
     * @return true if valid/false if not
     */
    public boolean isValidScale(double scl);
    
    /**
     * Get text representation of given scale factor
     * To be used for output
     * @param scale as float
     * @return text representation of this scale
     */
    public String getScaleString(double scl);
    
    /**
     * Get next zoom level
     * @param scl - current scale factor
     * @return scl - next scale factor
     */
    public double getNextScale(double scl);
    
    /**
     * Get previous zoom level
     * @param scl - current scale factor
     * @return scl - next scale factor
      */
    public double getPreviousScale(double scl);
    
    /**
     * Set minimum scale factor for this slide
     * @param scl
     */
    public void setMinimumScale(double scl);
    
    
    /**
     * return an array of available scales, if 
     * it is a descrete list, else null is returned
     * @return
     */
    public double [] getAvailableScales();
    
}
