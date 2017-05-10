package edu.pitt.slideviewer;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import javax.swing.*;


/**
 * This class displayes a magnified version of whatever
 * is shown on the viewer at some region
 * @author tseytlin
 */
public class Magnifier extends JPanel {
	//private final String MAGNIFIER_ICON = "/icons/Zoom32.gif";
	private Viewer viewer;
	private double p = 2;
	protected Rectangle r;
	protected Dimension viewSize;
	protected Image buffer;
	private Cursor cursor;
	private Rectangle lastView;
	
	/**
	 * Magnifier only works when viewer is showing
	 * init magnifier for some viewer
	 * @param viewer
	 */
	public Magnifier(Viewer viewer){
		super();
		this.viewer = viewer;
		setBackground(Color.white);
		setViewSize(new Dimension(64,64)); 
		setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
	}
	
	/**
	 * cleanup
	 */
	public void dispose(){
		buffer = null;
		viewer = null;
	}

	/**
	 * create custom cursor that is 32x32 or better yet 64x64
	 */
	private Cursor createCursor() {
		Toolkit tk = Toolkit.getDefaultToolkit();
		Dimension d = tk.getBestCursorSize(viewSize.width,viewSize.height);
		Point p = new Point(d.width/2,d.height/2);
		// draw cursor image
		Image img = new BufferedImage(d.width,d.height,BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) img.getGraphics();
		g.setColor(Color.white);
		g.setStroke(new BasicStroke(3));
		g.drawRect(1,1,d.width-3,d.height-3);
		g.setColor(Color.black);
		g.setStroke(new BasicStroke(1));
		g.drawRect(1,1,d.width-3,d.height-3);
		
		// load cursor from image
		//Image img = (new ImageIcon(getClass().getResource(MAGNIFIER_ICON))).getImage();
		return tk.createCustomCursor(img,p,"Magnifier");
	}
	
	/**
	 * @return the magnification
	 */
	public double getMagnification() {
		return p;
	}

	/**
	 * @param magnification the magnification to set
	 */
	public void setMagnification(double magnification) {
		this.p = magnification;
		// setup preferred size
		setPreferredSize(new Dimension((int)(viewSize.width*p),(int)(viewSize.height*p)));
	}
	
	/**
	 * @return the rectangle
	 */
	public Rectangle getRectangle() {
		return r;
	}

	/**
	 * set rectangle in the viewing window to be magnified
	 * by magnification factor 
	 * this is always  done in relative coordinates.
	 * @param rectangle the rectangle to set
	 */
	public void setRectangle(Rectangle rectangle) {
		this.r = rectangle;
		if(isShowing()){
			repaint();
			// notify of events
			notifyObservation(rectangle);
		}
	}

	
	/**
	 * notify all listeners about magnifier movement
	 * @param r
	 */
	private void notifyObservation(Rectangle vr){
		if(vr == null){
			lastView = null;
			return;
		}
		
		double s = viewer.getScale();
		Point pt = viewer.convertViewToImage(vr.getLocation());
		Dimension d = new Dimension((int)(vr.width/s),(int)(vr.height/s));
		Rectangle ar = new Rectangle(pt,d);
		if(lastView == null){
			ViewPosition v = new ViewPosition(ar,s);
			viewer.firePropertyChange(Constants.MAGNIFIER_OBSERVE,null,v);
			lastView = ar;
			lastView.grow(-(int)(ar.width*.25),-(int)(ar.height*.25));
		}else if(!ar.intersects(lastView)){
			ViewPosition v = new ViewPosition(ar,s);
			viewer.firePropertyChange(Constants.MAGNIFIER_OBSERVE,null,v);
			lastView = ar;	
			lastView.grow(-(int)(ar.width*.25),-(int)(ar.height*.25));
		}
	}
	
	
	/**
	 * @return the cursor
	 */
	public Cursor getMagnifierCursor() {
		return cursor;
	}
	
	
	/**
	 * @return the viewSize
	 */
	public Dimension getViewSize() {
		return viewSize;
	}

	/**
	 * @param viewSize the viewSize to set
	 */
	public void setViewSize(Dimension viewSize) {
		this.viewSize = viewSize;
		
		// setup buffer
		buffer = new BufferedImage(viewSize.width,viewSize.height,BufferedImage.TYPE_INT_RGB);
		buffer.getGraphics().setColor(Color.white);
		buffer.getGraphics().fillRect(0,0,viewSize.width,viewSize.height);
		
		// setup preferred size
		setPreferredSize(new Dimension((int)(viewSize.width*p),(int)(viewSize.height*p)));
		
		// setup cursor
		cursor = createCursor();
	}
	
	/**
	 * this is where painting occures
	 */
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		if(r != null){
			// correct clip dimensions
			Dimension vd = viewer.getSize();
			Dimension d = getSize();
			
			int x = (r.x < 0)?0:-r.x;
			int y = (r.y < 0)?0:-r.y;
			int w = r.width -x;
			int h = r.height-y;
			if(w > vd.width)
				x = x - (vd.width-w);
			if(h > vd.height)
				y = y - (vd.height-h);
			
		
			// create alternate graphics to paint upon
			Graphics bg = buffer.getGraphics().create(x,y,w,h);
			//Graphics bg = g.create(x,y,w,h);
			viewer.getViewerComponent().paint(bg);
			
			// fast way to draw
			g.drawImage(buffer,0,0,d.width,d.height,Color.white,null);
			// slow way to draw
			//g.drawImage(buffer.getScaledInstance(d.width,d.height,Image.SCALE_SMOOTH),0,0,null);
		}
	}
	
	
	
	/**
     * @param args
     */
    public static void main(String[] args) {
        java.net.URL server = null;
        try{
            //server = new URL("http://157.229.221.175:8080");
            server = new java.net.URL("http://1upmc-opi-xip02.upmc.edu:82");
            //server = new URL("http://slidetutor.upmc.edu/tutor/servlet/AperioServlet/");
        }catch(java.net.MalformedURLException ex){
            ex.printStackTrace();
        }
        
        final Viewer viewer = new edu.pitt.slideviewer.qview.QuickViewer(server);
        viewer.setSize(500,500);
        
        //init frame
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(viewer.getViewerPanel());
        frame.pack();
        frame.setVisible(true);
        
        
        // init magnifier
        final Magnifier mag = new Magnifier(viewer);
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(mag);
        frame.pack();
        frame.setVisible(true);
        
        viewer.getViewerComponent().addMouseMotionListener(new MouseMotionAdapter(){
        	public void mouseMoved(MouseEvent e){
        		viewer.getViewerComponent().setCursor(mag.getCursor());
        		Dimension d = mag.getViewSize();
        		Point p = e.getPoint();
        		p = new Point(p.x-d.width/2,p.y-d.height/2);
        		mag.setRectangle(new Rectangle(p,d));
        	}
        });
       
        
        // load image
        try{
            viewer.openImage("AP_976_HE_L1.svs");
            //viewer.setImage("test_2_jp2.svs");
            System.out.println(viewer.getImage()+" "+viewer.getImageSize()+"\n"+viewer.getImageMetaData());
        }catch(ViewerException ex){
            ex.printStackTrace();
        } 
    }

	
}
