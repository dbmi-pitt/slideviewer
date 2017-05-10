package edu.pitt.slideviewer.policy;
import edu.pitt.slideviewer.ScalePolicy;

/**
 * Simple scale policy that doesn't enforce any policy :)
 * @author tseytlin
 */
public class NoScalePolicy implements ScalePolicy {
    private double min, max=1.0;
    public String getScaleString(double scl) {
        return ""+Math.round(scl*100);
    }
    public double getValidScale(double scl) {
        return scl;
    }
    public boolean isValidScale(double scl) {
        return true;
    }
    public void setMinimumScale(double scl){
        min = scl;
    }
    public double getNextScale(double scl){
        double newscl = scl*2;
        return (newscl > max)?max:newscl;
    }
    public double getPreviousScale(double scl){
        double newscl = scl/2;
        return (newscl < min)?min:newscl;
    }
    public double [] getAvailableScales(){
    	return null;
    }
}
