package edu.pitt.slideviewer;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

import edu.pitt.slideviewer.qview.connection.Utils;


public class DefaultNavigator extends JPanel implements Navigator, 
	MouseListener, MouseMotionListener,MouseWheelListener, PropertyChangeListener {
    private Viewer viewer;
    private Dimension size;
    private ImageProperties info;
    private Image thumbnail, mold, mold2;
    private Rectangle rect;
    private int offs = 5;
    private boolean drag = false, showMold = true;
    private String imageName;
    
    public DefaultNavigator(Viewer v){
    	viewer = v;
    	viewer.addPropertyChangeListener(this);
    	addMouseListener(this);
    	addMouseMotionListener(this);
    	addMouseWheelListener(this);
    	rect = new Rectangle();
    }
    
    
    // distractor
    public void dispose(){
    	viewer.removePropertyChangeListener(this);
    	removeMouseListener(this);
    	removeMouseMotionListener(this);
    	removeMouseWheelListener(this);
    	viewer = null;
    }
    
    /**
     * Display viewer exploration history
     * "green mold"
     * @param b
     */
    public void setHistoryVisible(boolean b){
    	showMold = b;
    }
    
    /**
     * Should be invoked when image is set
     * @param info
     */
    public void setImageProperties(ImageProperties info){
    	setImageProperties(info,true);
    }
    
    
    /**
     * Should be invoked when image is set
     * @param info
     */
    private void setImageProperties(ImageProperties info, boolean resize){
		this.info = info;
    	if(info == null){
		    thumbnail = null;
		    imageName = null;
		}else if(info.getThumbnail() != null){
		    //calculate size 
		    Image img = info.getThumbnail();
		    if(resize)
		    	setSize(getOptimalSize(getOptimalWidth()));	
			int w = (size.width>offs*2)?size.width-offs*2:1;
			int h = (size.height>offs*2)?size.height-offs*2:1;
		    thumbnail = img.getScaledInstance(w,h,Image.SCALE_SMOOTH);
		    
		    // flush images
		    try{
				MediaTracker tracker = new MediaTracker(this);
				tracker.addImage(thumbnail, 0);
				tracker.waitForAll();
		    }catch(Exception ex){}
		    
		    // re-create mold
		    if(!info.getName().equals(imageName)){
		    	mold = null;
		    	mold2 = null;
		    }
		    
		    
		    // create mold image
		    if(mold == null || mold.getWidth(null) != w || mold.getHeight(null) != h){
		    	mold = createHistoryImage(mold, w, h);
		    }
		    //	create mold image
		    if(mold2 == null || mold2.getWidth(null) != w || mold2.getHeight(null) != h){
		    	mold2 = createHistoryImage(mold2, w, h);
		    }
		    // remember image name
		    imageName = info.getName();
		    
		    //((BufferedImage) mold).coerceData(true);
		    resizeContainer();
		    repaint();
		}
    }
    
    /**
     * Create new history image
     * @param mold
     * @param w
     * @param h
     * @return
     */
    private Image createHistoryImage(Image mold, int w, int h){
    	BufferedImage temp = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
    	Graphics2D g = temp.createGraphics();
    	if(mold != null){
    		g.drawImage(mold,0,0,w,h,0,0,mold.getWidth(null),mold.getHeight(null),null);
    	}
    	return temp;
    }
    
    // find container window
    private Container getParentContainer(Container cont){
    	if(cont == null)
    		return null;
    	else if(cont.getParent() == null || cont instanceof Window)
    		return cont;
    	else
    		return getParentContainer(cont.getParent());
    }
    
    // resize container window
    private void resizeContainer(){
    	Container win = getParentContainer(this);
    	if(win != null && win != this){
    		win.setSize(size);
    		win.validate();
    	}
    }
    
    //resize
    public void validate(){
    	setImageProperties(info,false);
    }
    
    
    //  get optimal size
    private Dimension getOptimalSize(int sz){
    	Dimension img   = viewer.getImageSize();
    	int panelWidth,panelHeight;
    	if ( img.width >= img.height ) {
    		double ratio= ( double ) ( ( float ) img.width/sz);
    		panelWidth =  ( int ) ( ( double ) img.width/ ratio );
    		panelHeight = ( int ) ( ( double ) img.height / ratio );
    	} else {
    		double ratio = ( double ) ( ( float ) img.height/sz);
    		panelWidth = ( int ) ( ( double ) img.width/ ratio );
    		panelHeight = ( int ) ( ( double )  img.height/ ratio );
    	}
    	return new Dimension(panelWidth,panelHeight);
    }

    // determine best thumbnail width
    // if width and height are somewhat equal, then make
    // navigator 1/3 of width, if one side is much greater
    // make it 1/2 of viewer width
    private int getOptimalWidth(){
    	Dimension d = viewer.getImageSize();
    	Dimension s = viewer.getSize();
    	int dim = Math.max(s.width,s.height);
    	int max = Math.max(d.width,d.height);
    	int min = Math.min(d.width,d.height);
    	int result = 0;
    	if(((double)max/min) > 1.5){
    		result = dim/2;
    		return (result > 350)?350:result;
    	}else{
    		result = dim/3;
    		return (result > 250)?250:result;
    	}
    }
    
    
    
    public Component getComponent() {
    	return this;
    }

    public Dimension getSize() {
    	return size;
    }
    
    public Dimension getPreferredSize() {
    	return size;
    }

    public void setSize(Dimension d) {
    	size = d;
    }
    
    public void propertyChange(PropertyChangeEvent evt){
		if(evt.getPropertyName().equals(Constants.VIEW_CHANGE)){
		    repaint();
		}
    }
    
    public void paintComponent(Graphics g){
        super.paintComponent(g);
        if(thumbnail != null){
            g.drawImage(thumbnail,offs,offs,null);
            // draw viewport
            Dimension idim = viewer.getImageSize();
            Rectangle r = Utils.correctRectangle(idim,viewer.getViewRectangle());
            int w = (int)(r.width*thumbnail.getWidth(null)/idim.width);
            int h = (int)(r.height*thumbnail.getHeight(null)/idim.height);
            int x = (int)(r.x * thumbnail.getWidth(null)/idim.width) + offs;
            int y = (int)(r.y * thumbnail.getHeight(null)/idim.height)+ offs;
            rect.setBounds(x,y,w,h);
          
            // draw  on mold
            if(showMold && viewer.getScale() > 0.2){
	            float shade = (viewer.getScale() == viewer.getMaximumScale())?0.3f:0.15f;
            	Image img = (viewer.getScale() == viewer.getMaximumScale())?mold2:mold;
	            Graphics2D mg = (Graphics2D) img.getGraphics();
	            mg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC,shade));
	            mg.setColor(Color.green);
	            mg.fillRect(rect.x-offs,rect.y-offs,rect.width,rect.height);
            }
            
            // draw mold
            if(showMold){
            	g.drawImage(mold,offs,offs,null);
            	g.drawImage(mold2,offs,offs,null);
            }
            
            
            // draw region
            g.setColor(Color.black);
            g.drawRect(x,y,w,h);
            g.drawLine(0,y+h/2,x,y+h/2);
            g.drawLine(x+w,y+h/2,size.width,y+h/2);
            g.drawLine(x+w/2,0,x+w/2,y);
            g.drawLine(x+w/2,y+h,x+w/2,size.height);
        }
    }
    
    public void mouseEntered(MouseEvent evt){}
    public void mouseExited(MouseEvent evt){};
    public void mousePressed(MouseEvent evt){
    	Point p = evt.getPoint();
		if(rect.contains(p))
			drag = true;
    };
    public void mouseReleased(MouseEvent evt){ 
    	drag = false;
    };
    public void mouseMoved(MouseEvent evt){
    	if(rect.contains(evt.getPoint()) && viewer.getScale() > viewer.getMinimumScale())
    		setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    	else
    		setCursor(Cursor.getDefaultCursor());    	
    };
    public void mouseDragged(MouseEvent event) {
		Point p = event.getPoint();
		if(drag){
			Dimension d = viewer.getImageSize();
			int x = (int) (((double)(p.x * d.width))/getSize().width);
			int y = (int) (((double)(p.y * d.height))/getSize().height);
			viewer.setCenterPosition(new ViewPosition(x,y,viewer.getScale()));
		}
	}
    
    public void mouseWheelMoved(MouseWheelEvent e){
		int r = e.getWheelRotation();
		// zoom out
		if(r > 0){
			viewer.getViewerController().zoomOut();
		// zoom in
		}else{
			viewer.getViewerController().zoomIn();
		}
		
	}
    
    public void mouseClicked(MouseEvent evt){
    	Dimension idim = viewer.getImageSize();
    	Point pt = evt.getPoint();
    	int x = (int)(pt.x * idim.width /thumbnail.getWidth(null));
        int y = (int)(pt.y * idim.height /thumbnail.getHeight(null));
    	viewer.setCenterPosition(new ViewPosition(x,y,viewer.getScale()));
    };
}
