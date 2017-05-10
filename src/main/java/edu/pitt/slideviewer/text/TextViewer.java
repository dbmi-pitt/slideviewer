package edu.pitt.slideviewer.text;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Properties;

import javax.swing.JOptionPane;

import edu.pitt.slideviewer.AnnotationManager;
import edu.pitt.slideviewer.AnnotationPanel;
import edu.pitt.slideviewer.ImageProperties;
import edu.pitt.slideviewer.Magnifier;
import edu.pitt.slideviewer.Navigator;
import edu.pitt.slideviewer.ScalePolicy;
import edu.pitt.slideviewer.ViewPosition;
import edu.pitt.slideviewer.Viewer;
import edu.pitt.slideviewer.ViewerControlPanel;
import edu.pitt.slideviewer.ViewerController;
import edu.pitt.slideviewer.ViewerException;
import edu.pitt.slideviewer.simple.SimpleViewer;

public class TextViewer implements Viewer {

	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		// TODO Auto-generated method stub

	}
	public Viewer clone(){
		return null;
	}
	
	public void closeImage() {
		// TODO Auto-generated method stub

	}

	
	public Point convertImageToView(Point img) {
		// TODO Auto-generated method stub
		return null;
	}

	
	public Point convertViewToImage(Point view) {
		// TODO Auto-generated method stub
		return null;
	}

	
	public void dispose() {
		// TODO Auto-generated method stub

	}

	
	public void firePropertyChange(String p, Object a, Object b) {
		// TODO Auto-generated method stub

	}

	
	public AnnotationManager getAnnotationManager() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public AnnotationPanel getAnnotationPanel() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public ViewPosition getCenterPosition() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public String getImage() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public Properties getImageMetaData() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public ImageProperties getImageProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public Dimension getImageSize() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public Container getInfoPanel() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public Magnifier getMagnifier() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public double getMaximumScale() {
		// TODO Auto-generated method stub
		return 0;
	}

	
	public double getMinimumScale() {
		// TODO Auto-generated method stub
		return 0;
	}

	
	public Navigator getNavigator() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public Properties getParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public double getPixelSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	
	public PropertyChangeListener[] getPropertyChangeListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public double getScale() {
		// TODO Auto-generated method stub
		return 0;
	}

	
	public ScalePolicy getScalePolicy() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public URL getServer() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public Dimension getSize() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public Image getSnapshot(int mode) {
		// TODO Auto-generated method stub
		return null;
	}

	
	public Image getSnapshot() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public ViewPosition getViewPosition() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public Rectangle getViewRectangle() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public Component getViewerComponent() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public ViewerControlPanel getViewerControlPanel() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public ViewerController getViewerController() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public Container getViewerPanel() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public boolean hasImage() {
		// TODO Auto-generated method stub
		return false;
	}

	
	public void openImage(String name) throws ViewerException {
		// TODO Auto-generated method stub

	}

	
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		// TODO Auto-generated method stub

	}

	
	public void repaint() {
		// TODO Auto-generated method stub

	}

	
	public void reset() {
		// TODO Auto-generated method stub

	}

	
	public void setCenterPosition(ViewPosition p) {
		// TODO Auto-generated method stub

	}

	
	public void setCursor(Cursor c) {
		// TODO Auto-generated method stub

	}

	
	public void setParameters(Properties props) {
		// TODO Auto-generated method stub

	}

	
	public void setScale(double n) {
		// TODO Auto-generated method stub

	}

	
	public void setScalePolicy(ScalePolicy policy) {
		// TODO Auto-generated method stub

	}

	
	public void setServer(URL url) {
		// TODO Auto-generated method stub

	}

	
	public void setSize(Dimension d) {
		// TODO Auto-generated method stub

	}

	
	public void setSize(int w, int h) {
		// TODO Auto-generated method stub

	}

	
	public void setViewPosition(ViewPosition p) {
		// TODO Auto-generated method stub

	}

	
	public void setViewRectangle(Rectangle r) {
		// TODO Auto-generated method stub

	}

	
	public void setViewerControlPanel(ViewerControlPanel cp) {
		// TODO Auto-generated method stub

	}
	
	 /**
     * updates the viewer to reflect dynamic changes that were performed
     * on the viewer configuration or the image that is currently loaded
     * Ex: changes to image tranformation can lead to flashing cache as well
     * as image rotation brightening etc.. 
     */
    public void update(){
    	//NOOP
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		Viewer viewer = new SimpleViewer();
		viewer.getViewerPanel();
		viewer.setSize(500, 500);
		viewer.openImage("");
		JOptionPane.showMessageDialog(null, viewer.getViewerPanel(), "Viewer", JOptionPane.PLAIN_MESSAGE);

	}

}
