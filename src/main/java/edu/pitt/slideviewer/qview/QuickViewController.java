package edu.pitt.slideviewer.qview;

import java.awt.*;
import java.awt.event.*;
import edu.pitt.slideviewer.*;
import edu.pitt.slideviewer.markers.*;
import edu.pitt.slideviewer.markers.Marker.PolyEndMarker;
import edu.pitt.slideviewer.qview.connection.Utils;
import javax.swing.JComponent;

public class QuickViewController implements ViewerController {
    private QuickViewer viewer;
    private Marker markerBeingDragged;
    private boolean navigationOn = true;
    private boolean cursorModified,inViewDrag,blockClick;
    private int prevDragX, prevDragY;
    private int deltaX,deltaY;
    private AnnotationPanel panel;
    private Point start;
    //private long interval;
    //private final int timeInterval = 500;
    
    public QuickViewController(QuickViewer v){
        viewer = v;
        panel = v.getAnnotationPanel();
        
        //register
        viewer.getViewerComponent().addMouseListener(this);
        viewer.getViewerComponent().addMouseMotionListener(this);
        viewer.getViewerComponent().addMouseWheelListener(this);
    }
    
    // release all resources
    public void dispose(){
    	viewer.getViewerComponent().removeMouseListener(this);
    	viewer.getViewerComponent().removeMouseMotionListener(this);
    	viewer.getViewerComponent().removeMouseWheelListener(this);
    	panel = null;
    	viewer = null;
    }
    
    
    /**
	 * if viewer is dragged get direction that it is 
	 * beeing dragged
	 * DRAG_N, DRAG_S, DRAG_E, DRAG_W, DRAG_NE etc...
	 * @return
	 */
	public int getMoveDirection(){
		int moveDirection = ViewerController.NOT_MOVING;
		
		// are we moving in straight direction
		boolean zy = (Math.abs(deltaY) <= Math.abs(deltaX)/4);
		boolean zx = (Math.abs(deltaX) <= Math.abs(deltaY)/4);
		
		if(zx && deltaY < 0){
			moveDirection = ViewerController.MOVE_N;
		}else if(zx && deltaY > 0){
			moveDirection = ViewerController.MOVE_S;
		}else if(zy && deltaX < 0){
			moveDirection = ViewerController.MOVE_W;
		}else if(zy && deltaX > 0){
			moveDirection = ViewerController.MOVE_E;
		}else if(deltaX > 0 && deltaY < 0){
			moveDirection = ViewerController.MOVE_NE;
		}else if(deltaX < 0 && deltaY < 0){
			moveDirection = ViewerController.MOVE_NW;
		}else if(deltaX > 0 && deltaY >  0){
			moveDirection = ViewerController.MOVE_SE;
		}else if(deltaX < 0 && deltaY > 0){
			moveDirection = ViewerController.MOVE_SW;
		}
		return moveDirection;
	}
	
	/**
	 * Is viewer currently being dragged
	 * @return
	 */
	public boolean isDragged(){
		return inViewDrag;
	}
	
    /**
     * Check whether navigation is currently enabled or disabled
     * @return boolean is navigation enabled/disabled
     */
    public boolean isNavigationEnabled() {
        return navigationOn;
    }

    /**
     * move viewer window
     */
    public void panDown() {
    	if(!navigationOn || !viewer.hasImage() )
    		return;
    	
        Rectangle r = viewer.getViewRectangle();
        if(r != null){
            viewer.setViewRectangle(new Rectangle(r.x,r.y+r.height/2,r.width,r.height));
            //      send event
            viewer.notifyViewObserve(true); 
        }
    }

    /**
     * move viewer window
     */
    public void panLeft() {
    	if(!navigationOn || !viewer.hasImage() )
    		return;
    	
        Rectangle r = viewer.getViewRectangle();
        if(r != null){
            viewer.setViewRectangle(new Rectangle(r.x-r.width/2,r.y,r.width,r.height));
            //      send event
            viewer.notifyViewObserve(true); 
        }
    }

    /**
     * move viewer window
     */
    public void panRight() {
    	if(!navigationOn || !viewer.hasImage() )
    		return;
    	
        Rectangle r = viewer.getViewRectangle();
        if(r != null){
            viewer.setViewRectangle(new Rectangle(r.x+r.width/2,r.y,r.width,r.height));
            //      send event
            viewer.notifyViewObserve(true); 
        }
    }

    /**
     * move viewer window
     */
    public void panUp() {
    	if(!navigationOn || !viewer.hasImage() )
    		return;
    	
        Rectangle r = viewer.getViewRectangle();
        if(r != null){
            viewer.setViewRectangle(new Rectangle(r.x,r.y-r.height/2,r.width,r.height));
            //      send event
            viewer.notifyViewObserve(true); 
        }
    }

    
    /**
     * Zoom out completly
     */
    public void resetZoom() {
    	if(!navigationOn || !viewer.hasImage() )
    		return;
    	
        viewer.setScale(viewer.getMinimumScale());

    }
    /**
     * Sometimes it is desirable to "freeze" viewer movements.
    * This can be done by setting this to false. Default is true (of course)
    * @param boolean enable/disable navigation
    */
    public void setNavigationEnabled(boolean b) {
        navigationOn = b;
        viewer.getViewerControlPanel().setEnabled(b);
    }
    
    /**
     * zoom into the slide
     */
    public void zoomIn() {
    	if(!navigationOn || !viewer.hasImage() )
    		return;
    	
        double scl = viewer.getScale();
        if(viewer.getScalePolicy() != null)
            scl = viewer.getScalePolicy().getNextScale(scl);
        else
            scl = scl * 2;
        viewer.setScale(scl);
        //send event
        viewer.notifyViewObserve(true); 
    }


    /**
     * zoom out the slide
     */
    public void zoomOut() {
    	if(!navigationOn || !viewer.hasImage() )
    		return;
    	
        double scl = viewer.getScale();
        if(viewer.getScalePolicy() != null)
            scl = viewer.getScalePolicy().getPreviousScale(scl);
        else
            scl = scl / 2;
        viewer.setScale(scl);
        //send event
        viewer.notifyViewObserve(true); 
    }
    
    // perform zoom in/out
    public void mouseClicked(MouseEvent evt) {
        if(!navigationOn || !viewer.hasImage() )
            return;
        
        if(!blockClick){
            // check time interval
           // if((System.currentTimeMillis()-interval) < timeInterval)
            //	return;
            //interval = System.currentTimeMillis();
        	
            ScalePolicy policy = viewer.getScalePolicy();
            // do zoom manually
            Point mp = evt.getPoint();
            
            Point pt = viewer.convertViewToImageNoTransform(mp);
            
                 
            double scl = viewer.getScale();
            if(evt.getButton() == MouseEvent.BUTTON1)
                scl = (policy != null)?policy.getNextScale(scl):scl*2;
            else if(evt.getButton() == MouseEvent.BUTTON3)
                scl = (policy != null)?policy.getPreviousScale(scl):scl/2;
            
            if(viewer.isCenterOnClick()){
            	viewer.setCenterPosition(new ViewPosition(pt,scl)); 
            }else{
                pt = new Point(pt.x-(int)(mp.x/scl),pt.y-(int)(mp.y/scl));
                viewer.setViewPosition(new ViewPosition(pt,scl)); 
            }  
        }
    }

    //  move to new location
    private void move(Point p) {
        // get delta
        int dx = start.x - p.x;
        int dy = start.y - p.y;
       
        // calculate direction
        deltaX += dx;
        deltaY += dy;
        
        // get current rectangle and delta in image system
        double scale = viewer.getScale();
        Rectangle r = viewer.getViewRectangle();
        r.translate((int)(dx/scale),(int)(dy/scale));
        
        //reset rect
        viewer.setViewRectangle(r);
        start = p;
        viewer.repaint();
    }

    
    
    //request focus
    public void mouseEntered(MouseEvent e) {
        if(!navigationOn || !viewer.hasImage())
            return;
        //viewer.getViewerComponent().requestFocus();
    }

    // do nothing
    public void mouseExited(MouseEvent e) {
        // noop
    }

    public void mousePressed(MouseEvent evt) {
        if (!navigationOn || !viewer.hasImage())
            return ;
        viewer.getViewerComponent().requestFocus();
        
        // get point
        Point p = new Point( evt.getPoint() );

        // if shape is drawing
        if(panel != null){
            if (panel.isSketching() && panel.getCurrentAnnotation() != null ) {
                panel.getCurrentAnnotation().setRelativeLocation( p );
                panel.getCurrentAnnotation().addRelativePoint( p.x, p.y );
                evt.consume();
                return;
            }
    
            // if we pressed on the marker on shape
            for (Annotation s:  panel.getAnnotationManager().getAnnotations()) {
                Marker m = s.getMarkerAt( p.x, p.y );
                if ( m != null ) {
                    // check if this is could be a context menu
                	if(evt.isPopupTrigger() || evt.getButton() == MouseEvent.BUTTON3){
                		if(s.isEditable()){
                			blockClick = true;
                			s.getContextMenu().show(viewer.getViewerComponent(),p.x,p.y);
                		}
                	}
                	markerBeingDragged = m;
                    prevDragX = p.x;
                    prevDragY = p.y;
                    evt.consume();
                    return ;
                }
            }
        }

        // else remember point where we pressed for navigation
        start = p;
        viewer.getViewerComponent().setCursor( Cursor.getPredefinedCursor( Cursor.MOVE_CURSOR ) );
    }

    public void mouseReleased(MouseEvent evt) {
        if (!navigationOn || !viewer.hasImage())
            return ;
        
        Annotation marker = null;
        
        // cancel view dragging mode (while sketching)
        blockClick = false;
        inViewDrag = false; 
        deltaX = deltaY = 0;
        
        // if we were drawing a shape, then finish it
        if (panel != null && panel.isSketching() && panel.getCurrentAnnotation() != null ) {
            marker = panel.getCurrentAnnotation();
           
            // update bounds
            marker.updateBounds();
        	
            // we are done
            panel.sketchDone();
            viewer.repaint();
            blockClick = true;
        // if we are dragging a marker  
        } else if ( markerBeingDragged != null ) {
            Point p = new Point( evt.getPoint() ); // local copy
            
            // compensate for rotation and flipping
        	Point delta = new Point(p.x - prevDragX, p.y - prevDragY);
        	if(!(markerBeingDragged instanceof PolyEndMarker))
        		delta = getTransformedDelta(delta);
            markerBeingDragged.moveBy(delta.x, delta.y );
         
            //markerBeingDragged.setInDrag(false);
            markerBeingDragged.notifyBoundsChange();
            markerBeingDragged = null;
            viewer.repaint();
        // else the usual thing
        }else{
            //send event
            viewer.notifyViewObserve(true); 
        }
        
        viewer.getViewerComponent().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        evt.consume();
        
        if(marker != null)
            viewer.firePropertyChange(Constants.SKETCH_DONE, null,marker);
        

    }

    public void mouseDragged(MouseEvent evt) {
        if ( !navigationOn  || !viewer.hasImage())
            return ;
         
        Point p = new Point( evt.getPoint() ); // local copy
        
        // ctrl has been pressed while dawing
        if ( evt.isControlDown() && panel.isSketching() ) {
            if ( !inViewDrag ) {
                inViewDrag = true;
                // reseed dragging
                start = p;
            }
            move( p );
            viewer.repaint();
            return;
        }

        // drawing shape
        if (panel != null && panel.isSketching() && panel.getCurrentAnnotation() != null ) {
            inViewDrag = false;
            Point ip = viewer.convertViewToImage( p );
            Point lp = panel.getCurrentAnnotation().getLocation();
            if(panel.getCurrentAnnotation().isResizable()){
            	panel.getCurrentAnnotation().setBounds(new Rectangle(lp.x,lp.y,ip.x-lp.x,ip.y-lp.y));
            }
            panel.getCurrentAnnotation().addPoint( ip.x, ip.y );
            viewer.repaint();
        // dragging a marker
        }else if ( markerBeingDragged != null ) {
            // compensate for rotation and flipping
        	Point delta = new Point(p.x - prevDragX, p.y - prevDragY);
        	if(!(markerBeingDragged instanceof PolyEndMarker))
        		delta = getTransformedDelta(delta);
        	       	
        	// move marker
        	markerBeingDragged.moveBy(delta.x,delta.y);
            
        	prevDragX = p.x;
            prevDragY = p.y;
           
            viewer.repaint();
        //normal viewer  movement
        }else {
        	inViewDrag = true;
            move(p);
        }
        // consume event (don't know why I need this though)
        evt.consume();

    }

    
    /**
     * Place cursor on markers
     */
    public void mouseMoved(MouseEvent evt) {
        if(!navigationOn || !viewer.hasImage())
            return;
        
        // check for panel and sketching
        if(panel == null || panel.isSketching())
            return;

        // go though all markers
        Marker marker = null;
        Point p = new Point(evt.getPoint());
        for(Annotation s: panel.getAnnotationManager().getAnnotations()){
            if((marker=s.getMarkerAt(p.x,p.y)) != null)
                break;
        }
        
        //get magnifier
        Magnifier mag = viewer.getMagnifier();
          
        // set cursor
        if(marker != null) {
            viewer.getViewerComponent().setCursor(AnnotationManager.getCursor(marker));
            if(marker.getShape().isTagVisible())
            	((JComponent)viewer.getViewerComponent()).setToolTipText(marker.getShape().getTag());
            cursorModified = true;
        }else if(mag.isShowing()){
        	viewer.getViewerComponent().setCursor(mag.getMagnifierCursor());
        	Dimension d = mag.getViewSize();
        	mag.setRectangle(new Rectangle(new Point(p.x-d.width/2,p.y-d.height/2),d));
        	cursorModified = true;
        }else if (cursorModified){
            viewer.getViewerComponent().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            ((JComponent)viewer.getViewerComponent()).setToolTipText(null);
            cursorModified = false;
        }

    }

    // zoom in/out
    public void mouseWheelMoved(MouseWheelEvent e) {
        if(!navigationOn || !viewer.hasImage())
            return;
        
        // check time interval
        //if((System.currentTimeMillis()-interval) < timeInterval)
        //	return;
        //interval = System.currentTimeMillis();
        
        int r = e.getWheelRotation();
        if(viewer.getScalePolicy() != null){
            Point mp = e.getPoint();
            Point pt = viewer.convertViewToImageNoTransform(mp);
                
            double scl = viewer.getScale();
            // zoom out
            if(r > 0){
                scl = viewer.getScalePolicy().getPreviousScale(scl);
                //zoomOut();
            // zoom in
            }else{
                scl = viewer.getScalePolicy().getNextScale(scl);
                //zoomIn();
            }   
            pt = new Point(pt.x-(int)(mp.x/scl),pt.y-(int)(mp.y/scl));
            viewer.setViewPosition(new ViewPosition(pt,scl));
            //send event
            viewer.notifyViewObserve(true); 
        }else {
            if(r > 0){
               zoomOut();
            }else{
               zoomIn();
            }   
        }
      
    }
    
    
    /**
     * get transformed delta point to move objects on the screen
     * @param delta
     * @return
     */
    private Point getTransformedDelta(Point delta){
    	ImageTransform it = viewer.getImageProperties().getImageTransform();
    	return Utils.getTransformedDelta(delta, it);
    }
}
