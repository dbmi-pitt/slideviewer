package edu.pitt.slideviewer.policy;

import edu.pitt.slideviewer.ScalePolicy;


/**
 * This scale policy should be more useful to pathologists
 * Here there is only 20X, 10X, 4X and 1X available
 * @author tseytlin
 *
 */
public class PathScalePolicy implements ScalePolicy {
    private int startOffset = 0;
    private double [] scales = new double [] {0.0078125,0.0625,0.25,1.0};
   
    // get string representation
    public String getScaleString(double scl) {
        if(0.03 > scl)
            return "1X";
        else if(0.2 >= scl  && scl >= 0.03)
            return "4X";
        else if(0.7 >= scl && scl >= 0.2)
            return "10X";
        else if(1.0 >= scl && scl >= 0.7)
            return "20X";
        else
            return "?X";
    }
    
    // kick-ass way to find nearest match :)
    public double getValidScale(double scl) {
        return scales[getValidScaleIndex(scl)];
    }
    
    // kick-ass way to find nearest match :)
    private int getValidScaleIndex(double scl) {
        int st = startOffset;
        int en = scales.length-1;
        int offs = st+(int)((en-st)/2);
       
        // instead of dull for loop try divide and conquer approach
        while( offs > st) {
            //System.out.println("st="+st+" en="+en+" offs="+offs+" scale="+scales[offs]);
            // we are lucky found exact match
            if(scales[offs] == scl){ 
                return offs;
            }else if(scales[st] == scl){
                return st;
            }else if(scales[en] == scl){
                return en;
            }else if(scales[offs] < scl){
                st = offs;
            }else{
                en = offs;
            }
            offs = st+(int)((en-st)/2);
        }
        //System.out.println("end st="+st+" en="+en+" offs="+offs+" scale="+scales[offs]);
        
        // when/if we are here the value is in between st and en
        // we need to pick one that fits better
        //if(pol == 0)
        return ((scl-scales[st]) > (scales[en]-scl))?en:st;
    }
    
    public boolean isValidScale(double scl) {
        double tst = getValidScale(scl);
        return tst == scl;
    }
    
    //set minimum
    public void setMinimumScale(double scl){
        //find closest power offset
        int i = getValidScaleIndex(scl);
        startOffset = i;
        scales[startOffset] = scl;
    }
    
    public double getNextScale(double scl){
        int i = getValidScaleIndex(scl);
        return (i < (scales.length-1))?scales[i+1]:scales[i];
    }
    public double getPreviousScale(double scl){
        int i = getValidScaleIndex(scl);
        return (i > startOffset)?scales[i-1]:scales[i];
    }
    /**
     * return an array of available scales, if 
     * it is a descrete list, else null is returned
     * @return
     */
    public double [] getAvailableScales(){
    	return scales;
    }
    
    /*
    public static void main(String [] args){
        PathScalePolicy p = new PathScalePolicy(0.0078125);
        double [] tst = new double [] {1.0,0.85, 0.52, 0.005,0.00390626};
        for(int i=0;i<tst.length;i++){
            long time = System.currentTimeMillis();
            double scl = p.getValidScale(tst[i]);
            long elapsed = System.currentTimeMillis()-time;
            System.out.println("inscale= "+tst[i]+" outscale="+scl+" str="+p.getScaleString(scl)+" time="+elapsed);
            
        }
    }*/   
    
}
