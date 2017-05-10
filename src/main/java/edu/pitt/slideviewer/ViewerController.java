package edu.pitt.slideviewer;
import java.awt.event.*;


/**
 * This class is responsible for all viewer operations, s.a. mouse navigation and button presses.
 */
public interface ViewerController extends MouseListener, MouseMotionListener, MouseWheelListener {
	public static int NOT_MOVING = 0;
	public static int MOVE_N  = 1;
	public static int MOVE_S  = 2;
	public static int MOVE_E  = 3;
	public static int MOVE_W  = 4;
	public static int MOVE_NW = 5;
	public static int MOVE_SW = 6;
	public static int MOVE_NE = 7;
	public static int MOVE_SE = 8;
	
	/**
	 * if viewer is dragged get direction that it is 
	 * beeing dragged
	 * MOVE_N, MOVE_S, MOVE_E, MOVE_W, MOVE_NE etc...
	 * @return
	 */
	public int getMoveDirection();
	
	/**
	 * Is viewer currently being dragged
	 * @return
	 */
	public boolean isDragged();
	
	
	/**
	 * Sometimes it is desirable to "freeze" viewer movements.
	 * This can be done by setting this to false. Default is true (of course)
	 * @param boolean enable/disable navigation
	 */
	public void setNavigationEnabled( boolean b );

	/**
	 * Check whether navigation is currently enabled or disabled
	 * @return boolean is navigation enabled/disabled
	 */
	public boolean isNavigationEnabled();

	/**
	 * Move viewing window to the right
	 */
	public void panRight();
	
	/**
	 * Move viewing window to the left
	 */
	public void panLeft();
	
	/**
	 * Move viewing window up
	 */
	public void panUp();
	
	/**
	 * Move viewing window down
	 */
	public void panDown();
	
	/**
	 * Do zoom in action
	 */
	public void zoomIn();

	/**
	 * Do zoom out action
	 */
	public void zoomOut();

	/**
	 * Do a complete zoom out to the smallest possible
	 * zoom level.
	 */
	public void resetZoom();
	
	
	/**
	 * release all resources
	 */
	public void dispose();
	
}
