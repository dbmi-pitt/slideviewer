import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import javax.swing.JApplet;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import edu.pitt.slideviewer.Constants;
import edu.pitt.slideviewer.Viewer;



/**
 * navigator applet that hooks up to a viewer applet
 * @author tseytlin
 */
public class NavigatorApplet extends JApplet {
	private Viewer viewer;
	/**
	 * initialize the viewer
	 */
	public void init() {
		super.init();
		setLayout(new BorderLayout());
		setBackground(Color.white);
		//JPanel p = new JPanel();
		//p.setBackground(Color.white);
		//p.setOpaque(true);
		//add(p,BorderLayout.CENTER);
		setViewer(getViewer());
	}
	public void start() {}
	private void resizeNavigator(){
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if(viewer != null && viewer.hasImage()){
					Dimension size = new Dimension(getSize());
					Dimension isize = viewer.getImageSize();
					if(isize.width > isize.height){
						size.height = size.width * isize.height / isize.width;
					}else{
						size.width = size.height * isize.width / isize.height;
					}
					viewer.getNavigator().setSize(size);
					viewer.getNavigator().validate();
					viewer.getNavigator().getComponent().repaint();
					validate();
					repaint();
				}
			}
		});
	}
	
	
	/**
	 * set viewer for this navigator
	 * @param viewer
	 */
	public void setViewer(Viewer v){
		if(v != null && v.getNavigator() != null && v  != viewer){
			viewer = v;
			viewer.getNavigator().getComponent().setBackground(Color.white);
			viewer.addPropertyChangeListener(new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					if(Constants.IMAGE_CHANGE.equals(evt.getPropertyName()) || Constants.IMAGE_TRANSFORM.equals(evt.getPropertyName())){
						resizeNavigator();
					}
				}
			});
			add(viewer.getNavigator().getComponent(),BorderLayout.CENTER);
			validate();
			repaint();
			resizeNavigator();
		}
	}
	
	
	/**
	 * get viewer from viewer applet
	 * @return
	 */
	private Viewer getViewer(){
		// first attempt
		for(Enumeration<Applet> app = getAppletContext().getApplets();app.hasMoreElements();){
			Applet a = app.nextElement();
			if(a instanceof ViewerApplet){
				ViewerApplet va = (ViewerApplet) a;
				if(va.getViewer() != null)
					return va.getViewer();
			}
		}
		return null;
	}
}
