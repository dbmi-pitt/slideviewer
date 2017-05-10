package edu.pitt.slideviewer.qview;

import java.awt.*;
import java.util.*;
import edu.pitt.slideviewer.*;
import edu.pitt.slideviewer.markers.*;

public class QuickAnnotationPanel implements AnnotationPanel {
    private Viewer viewer;

    // The AnnotationManager handles the creation and display of annotation markers.
    private AnnotationManager annotationManager;
    private java.util.List components;

    // stuff for sketching new TutorMarkers
    protected boolean inSketch = false;
    private boolean visible = true;
    protected Annotation cur_marker = null;


    /**
     * Create new instance of AnnotationPanel (stating the obvoius)
     * 
     * @param Viewer
     *            viewer
     */
    public QuickAnnotationPanel(Viewer viewer) {
        this.viewer = viewer;

        // create marker manager
        annotationManager = new AnnotationManager(viewer);

        // components
        components = new ArrayList();
    }

    // cleanup resources
    public void dispose(){
    	annotationManager.dispose();
    	removeComponents();
    	annotationManager = null;
    	viewer = null;
    }
    
    /**
     * Adds a component to the annotation layer. Ex. JLabel is probably the most
     * useful component to add.
     * 
     * @param Component -
     *            component to be added to the panel
     * @param Rectangle -
     *            bounds to be applied to this component in relative viewer
     *            coordinates
     */
    public void addComponent(Component comp) {
    	components.add(comp);
    }

    /**
     * Removes a component from the annotation layer.
     * 
     * @param Component -
     *            component to be added to the panel
     */
    public void removeComponent(Component comp) {
        components.remove(comp);
    }

    /**
     * Removes all components attached to the viewer.
     */
    public void removeComponents() {
        components.clear();
    }

	/**
	 * Get all components attached to the viewer.
	 */
	public Component [] getComponents(){
		return (Component []) components.toArray(new Component[0]);
	}
	
    
    /**
     * This method is here for convinience. It is the same as:
     * getMarkerManager().addTutorMarker()
     * 
     * @param Annotation
     *            annotation to add
     */
    public void addAnnotation(Annotation tm) {
        annotationManager.addAnnotation(tm);
    }

    /**
     * Make annotation panel visible/invisible
     * 
     * @param boolean
     *            b true to make annotation panel visible
     */
    public void setVisible(boolean b) {
        visible = b;
    }

    /**
     * Is annotation panel visible?
     * 
     * @return boolean
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Get marker manager instance
     */
    public AnnotationManager getAnnotationManager() {
        return annotationManager;
    }

    public void sketchAnnotation(Annotation tm) {
        inSketch = true;
        cur_marker = tm;
        viewer.getViewerComponent().setCursor(AnnotationManager.getCursor(tm));
    }

    public void sketchDone() {
        // remember view positions
        if (inSketch && cur_marker != null) {
            cur_marker.setViewPosition(viewer.getViewPosition());

            // clear marker
            inSketch = false;
            cur_marker = null;
        }
    }

    /**
     * Get the 'in sketch' mode status
     * 
     * @return boolean true if annotation panel is in sketch mode.
     */
    public boolean isSketching() {
        return inSketch;
    }

    /**
     * Get Annotation currently being sketched
     * 
     * @return Annotation
     */
    public Annotation getCurrentAnnotation() {
        return cur_marker;
    }


	public void draw(Graphics g){
		paintComponent(g);
	}
	
    
    /**
     * Callback for painting. Ensure that the <code>event.getGraphics</code>
     * method is used to get the <code>Graphics</code> context for drawing
     */
    public void paintComponent(Graphics graphics) {

        // draw markers
        if (annotationManager.hasAnnotations() && isVisible()) {
            Graphics2D g = (Graphics2D) graphics;

            // this is where TutorMarkers are actually drawn
            annotationManager.drawAnnotations(g);

            if (inSketch && cur_marker != null) {
                cur_marker.drawShape(g);
            }
        }

        // draw components
        for (int i = 0; i < components.size(); i++) {
            Component comp = (Component) components.get(i);
            Rectangle r = comp.getBounds();
            comp.paint(graphics.create(r.x, r.y, r.width, r.height));
        }
    }
}