import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.net.*;
import java.awt.event.*;
import edu.pitt.slideviewer.*;

public class ViewerPathCreator implements ActionListener {
	private JFrame frame;
	private JTabbedPane result;
	private JTextField input;
	private JButton ibrowse,create;
	private Properties prefs;
	private Map map;
	private final double [] scales = new double [] {0.06,0.13,0.4,1.0};
	//private final double [] scales = new double [] {0,0.0625,0.25,1.0};
	private String cwd = System.getProperty("user.home");
	
	/**
	 * init map creator
	 * @param file
	 */
	public ViewerPathCreator(String server) throws Exception{
		prefs = initViewerProperties(server);
		frame = createGUI();
		frame.setVisible(true);
		ViewerFactory.setProperties(prefs);
	}
	
	private JFrame createGUI(){
		JFrame frame = new JFrame("Viewer Path Creator");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// input panel
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBorder(new TitledBorder("Input"));
		panel.add(new JLabel("Input text file  "),BorderLayout.WEST);
		input = new JTextField();
		panel.add(input,BorderLayout.CENTER);
		ibrowse = new JButton("Browse");
		ibrowse.addActionListener(this);
		panel.add(ibrowse,BorderLayout.EAST);
		create = new JButton("Create View Path");
		create.addActionListener(this);
		panel.add(create,BorderLayout.SOUTH);
		
		// result panel
		result = new JTabbedPane();
		result.setPreferredSize(new Dimension(550,550));
		
		// combined
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,panel,result);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(split,BorderLayout.CENTER);
		frame.pack();
		return frame;
	}
	
	private Properties initViewerProperties(String server){
		Properties prefs = new Properties();
		boolean loaded = false;
		
		// load properties from server
		try{
			URL url = new URL(server+"/SlideViewer.conf");
			prefs.load(url.openStream());
			loaded = true;
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		// prop not loaded, then load hard-coded defaults
		if(!loaded){
			String host = server;
			try {
				URL url = new URL(server);
				host = "http://" + url.getHost();
			} catch (MalformedURLException ex) {
				ex.printStackTrace();
				
			}
			// default viewer
			String dview = "qview";
			prefs.setProperty("image.server.type", dview);
	
			// default params
			prefs.setProperty("mscope.server.url", server + "/servlet/mScopeServlet");
			prefs.setProperty("mscope.servlet.url", server + "/servlet/mScopeThumbnailServlet");
			prefs.setProperty("mscope.image.dir", "e:\\slideimages\\"); // "/e/slideimages/");
			prefs.setProperty("xippix.server.url", server + "/servlet/IPServer");
			prefs.setProperty("xippix.image.dir", "e:\\slideimages\\");
			prefs.setProperty("qview.server.url", host + ":82/");
			prefs.setProperty("qview.image.dir", "");
		}
		return prefs;
	}
	
	
	/**
	 * Open image in the viewer temporarily
	 * Get image dimensions and thumbnail image
	 * @param name
	 * @return { Dimension, Image }
	 *
	private Object [] getImageInfo(String name){
		Object [] result = new Object [2];
		
		// create viewer window & viewer
		if(viewer == null){
			// create viewer
			ViewerFactory.setProperties(prefs);
			viewer = ViewerFactory.getViewerInstance(prefs.getProperty("image.server.type","qview"));
		}

		// switch viewer if necessary
		String type = ViewerFactory.recomendViewerType(name);
		if(!type.equals(prefs.getProperty("image.server.type"))){
			prefs.setProperty("image.server.type",type);
			
			Viewer v = ViewerFactory.getViewerInstance(type);
			
			// remove component
			viewer.dispose();
			viewer = null;

			// add something to viewer window
			viewer = v;
		}
		
		// now, load image
		try {
			viewer.setSize(new Dimension(500,500));
			viewer.setImage(prefs.getProperty(type+".image.dir","")+name);
		} catch (ViewerException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(frame,ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
		// resize frame to correct size
		Dimension isize = viewer.getImageSize();
		Dimension size = new Dimension(500,500);
		
		// adjust size horizontal adjust height
		if(isize.width > isize.height)
			size.height = (int) ((size.width*isize.getHeight())/isize.getWidth());
		else
			size.width = (int) ((size.height*isize.getWidth())/isize.getHeight());
		
		// resize frame (for beutiful screenshot)
		viewer.setSize(size);
		viewer.getViewerController().resetZoom();
				
		// take snapshot
		Image img = viewer.getSnapshot();
		
		// assign results
		result[0] = isize;
		result[1] = img;
		
		return result;
	}
	*/
	 /**
     * Make sure image loads!!!!
     * @param img
     */
    public void flushImage(Image img){
    	MediaTracker tracker = new MediaTracker(frame);
    	tracker.addImage(img,0);
    	try{
    		tracker.waitForAll();
    	}catch(InterruptedException ex){}
    	tracker.removeImage(img,0);
    }
	
	/**
	 * Create maps
	 */
	public void doCreateMap(String file){
		try{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			map = createMap(reader);
			reader.close();
		}catch(Exception ex){
			ex.printStackTrace();
			JOptionPane.showMessageDialog(frame,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
		}
		if(map != null){
			displayMap(map);
		}
	}
	
	/**
	 * Save output as a file
	 * @param overlay
	 * @param file
	 */
	public void doSaveOverlay(PathMap overlay, File file){
		try{
			Image thumb = overlay.getThumbnail();
			Image image = overlay.getOverlay();
			
			Dimension size = new Dimension(thumb.getWidth(null),thumb.getHeight(null));
			
			// figure out direction we are going to add stuff
			BufferedImage result = new BufferedImage(size.width,size.height,BufferedImage.TYPE_INT_RGB);
		
			// draw overlay
			Graphics g = result.getGraphics();
			g.setColor(Color.black);
			
			g.drawImage(overlay.getOverlay(),0,0,null);
			g.drawString(overlay.getImageName(),5,15);
			//g.drawRoundRect(0,0,size.width-2,size.height-2,15,15);
					
			//ImageIO.write(result,"JEPG",file);
			ViewerHelper.writeJpegImage(result,file);
			JOptionPane.showMessageDialog(frame,new ImageIcon(result),"",JOptionPane.PLAIN_MESSAGE);
		}catch(IOException ex){
			JOptionPane.showMessageDialog(frame,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}
		
	}
	
	
	
	
	// button press handler
	public void actionPerformed(ActionEvent e){
		String cmd = e.getActionCommand();
		if(e.getSource() == ibrowse){
			JFileChooser chooser = new JFileChooser(cwd);
			int result = chooser.showOpenDialog(frame);
			if(result == JFileChooser.APPROVE_OPTION){
				input.setText(chooser.getSelectedFile().getAbsolutePath());
			}
		}else if(e.getSource() == create){
			doCreateMap(input.getText());
		}else if(cmd.startsWith("save:")){
			String name = cmd.substring(5);
			if(map != null){
				PathMap overlay = (PathMap) map.get(name);
				if(overlay != null){
					JFileChooser chooser = new JFileChooser(cwd);
					String fname = cwd+File.separator+"viewmap-"+overlay.getImageName();
					int n = fname.lastIndexOf(".");
					if(n > -1)
						fname = fname.substring(0,n);
					chooser.setSelectedFile(new File(fname+".jpg"));
					int result = chooser.showSaveDialog(frame);
					if(result == JFileChooser.APPROVE_OPTION){
						doSaveOverlay(overlay,chooser.getSelectedFile());
					}
				}
			}
		}
	}
	
	
	/**
	 * Given reader that has pointer to file orig image size and source 
	 * thumbnail image build a map
	 * @param reader
	 * @param isize
	 * @param thumbnail
	 */
	public Map createMap(BufferedReader reader) throws Exception{
		PathMap overlay = null;
		Map overlays = new HashMap();
		// now iterate over input file to record viewer movements
		Dimension viewSize = new Dimension(0,0);
		for(String line = reader.readLine();line != null;line=reader.readLine()){
			//	skip comments
			if(line.startsWith("#"))
				continue;
			
			//	 parse line
			String [] entries = line.split("\\"+Constants.ENTRY_SEPARATOR);
			//System.out.println("processing line :"+line);
			if(entries.length > 1){
				String p = entries[0].trim();
				if( p.equalsIgnoreCase(Constants.VIEW_TAG) || 
					p.equalsIgnoreCase(Constants.OBSERVE_TAG)){
					int x = Integer.parseInt(entries[1]);
					int y = Integer.parseInt(entries[2]);
					double s = Double.parseDouble(entries[3]);
					// add rectangle to overlay
					if(overlay != null){
						int w = (int)(viewSize.getWidth()/s);
						int h = (int)(viewSize.getHeight()/s);
						overlay.addViewRectangle(new Rectangle(x,y,w,h));
					}
				}else if(p.equalsIgnoreCase(Constants.RESIZE_TAG)){
					int w = Integer.parseInt(entries[1]);
					int h = Integer.parseInt(entries[2]);
					viewSize.setSize(w,h);
				}else if(p.equalsIgnoreCase(Constants.IMAGE_TAG)){
					String imageName = entries[1].trim();
					
					// load image to get its screenshot and thumbnail
					//Object [] obj = getImageInfo(imageName);
					ImageProperties ip = ViewerFactory.getImageProperties(imageName);
					
					if(ip == null)
						return overlays;
					
					// for now hardcoded
					Dimension isize = ip.getImageSize(); //(Dimension) obj[0];
					Image thumbnail = ip.getThumbnail(); //(Image) obj[1];
					
					// prepare images for different powers
					Dimension size = null;
					if(thumbnail != null){
						size = new Dimension(thumbnail.getWidth(null),thumbnail.getHeight(null));
					}
					viewSize.setSize(size);
					
					// create overlay
					overlay = new PathMap(isize,size,scales);
					overlay.setThumbnail(thumbnail);
					overlay.setImageName(imageName);
					overlay.setDate(Constants.DATE_FORMAT.parse(entries[2]));
					
					// add to map
					overlays.put(imageName,overlay);
					
					
				}
			}
		}
		
		return overlays;
	}
	
	
	/**
	 * Create output panel
	 * @return
	 */
	
	private JPanel createOutputPanel(PathMap overlay){
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		
		// get overlay images
		Image thumb = overlay.getThumbnail();
		Image image = overlay.getOverlay();
		
		
			// add overlays to list
		Dimension d = null;
		double [] scales = overlay.getScales();
		
		Icon icon = new ImageIcon(image);
		if(d == null)
			d = new Dimension(icon.getIconWidth()+30,icon.getIconHeight());
		
		ArrayList slist = new ArrayList();
		for(int i=0;i<scales.length;i++){
			slist.add(new Double(scales[i]));
		}
		
		
		// create container
		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints gc = new GridBagConstraints(0,0,1,1,1,1,
		GridBagConstraints.NORTHWEST,GridBagConstraints.NONE,new Insets(5,5,5,5),0,0);
		JPanel cont = new JPanel();
		cont.setLayout(gb);
		JScrollPane scroll = new JScrollPane(new JLabel(icon));
		scroll.setMinimumSize(d);
		gb.setConstraints(scroll,gc);
		cont.add(scroll);
		
		// create info label
		Dimension isize = overlay.getImageSize();
		
		JLabel lbl = new JLabel("<html><table border=0 width=300>" +
				"<tr><td>image name:</td><td><b>"+overlay.getImageName()+"</b></td></tr>"+
				"<tr><td>image size:</td><td><b>"+isize.width+" x "+isize.height+"</b></td></tr>"+
				"<tr><td>view time:</td><td><b>"+overlay.getDate()+"</b></td></tr>"+
				"<tr><td>zoom gradient:</td><td>"+slist+"</td></tr></table>");
		
		// add info label
		if(isize.width > isize.height){
			gc.gridy ++;
			gc.gridheight = 2;
			gc.anchor = GridBagConstraints.NORTHWEST;
			gc.fill = GridBagConstraints.HORIZONTAL;
		}else{
			gc.gridx ++;
			gc.gridwidth = 2;
			gc.anchor = GridBagConstraints.NORTHWEST;
			gc.fill = GridBagConstraints.HORIZONTAL;
		}
		
		gb.setConstraints(lbl,gc);
		cont.add(lbl);
		
		// create save button 
		JButton save = new JButton("Save Result");
		save.setActionCommand("save:"+overlay.getImageName());
		save.addActionListener(this);
		
		// add to panel
		//panel.add(lbl,BorderLayout.NORTH);
		panel.add(cont,BorderLayout.CENTER);
		panel.add(save,BorderLayout.SOUTH);
		
		return panel;
	}
	
	
	/**
	 * Display overlay images
	 * @param map
	 */
	public void displayMap(Map map){
		// remove previous results
		result.removeAll();
		
		// iterate over images
		Set keys = map.keySet();
		for(Iterator k = keys.iterator();k.hasNext();){
			String key = ""+k.next();
			PathMap overlay = (PathMap) map.get(key);
			result.addTab(key,createOutputPanel(overlay));
			//JOptionPane.showMessageDialog(null,new JScrollPane(list),key,JOptionPane.PLAIN_MESSAGE);
		}
	}
	
	
	
	/**
	 * This class encapsulates overlays
	 * @author tseytlin
	 */
	private class PathMap {
		private BufferedImage overlay;
		private DirectedPath path;
		private double [] scales;
		private Dimension isize, size;
		private Image thumbnail;
		private String imageName;
		private Date date;
		private Rectangle lastRect;
		
	
		/**
		 * @return the date
		 */
		public Date getDate() {
			return date;
		}

		/**
		 * @param date the date to set
		 */
		public void setDate(Date date) {
			this.date = date;
		}

		/**
		 * @return the imageName
		 */
		public String getImageName() {
			return imageName;
		}

		/**
		 * @param imageName the imageName to set
		 */
		public void setImageName(String imageName) {
			this.imageName = imageName;
		}

		/**
		 * Get size of image in pixels
		 * @return
		 */
		public Dimension getImageSize(){
			return isize;
		}
		
		/**
		 * Create an overlay map
		 * @param isize - size of map in absolute coordinates
		 * @param size  - size of map in relative coordinates
		 * @param scales [] - list of scales that correspond to
		 * map granularity Ex. {0,0.25,0.5,1} will create
		 * 3 overlays
		 * 0 < x <= 0.25
		 * 0.25 < x <= 0.5
		 * 0.5 <  x <= 1 
		 */
		public PathMap(Dimension isize, Dimension size, double [] scales){
			this.isize = isize;
			this.size = size;
			this.scales = scales;
			
			// init images
			overlay = new BufferedImage(size.width,size.height,BufferedImage.TYPE_INT_ARGB);
			
			// init path
			path = new DirectedPath();
			path.setShadow(false);
		}
		
		/**
		 * Draw view rectangle on some overlay image
		 */
		public void addViewRectangle(Rectangle r){
			Rectangle vr = new Rectangle();
			vr.width = (int)(r.width*size.width/isize.width);
            vr.height = (int)(r.height*size.height/isize.height);
            vr.x = (int)(r.x * size.width/isize.width);
            vr.y = (int)(r.y * size.height/isize.height);
             
            // determine scale
            boolean hrz = isize.width > isize.height;
            double scale = (hrz)?size.getWidth()/r.width:size.getHeight()/r.height;
            
            
            // draw  on mold
            int pathWidth = getPathWidth(scale);
            boolean draw = true;
            /* check if it was a jump or drag
            if(lastRect != null && !(lastRect.intersects(vr) || 
               lastRect.contains(vr) || vr.contains(lastRect))){
            	draw = false;
            }*/
            
            path.addPoint((int)vr.getCenterX(),(int)vr.getCenterY(),pathWidth,draw);
            
            // save previous view
            lastRect = vr;
        }
		
		/**
		 * Get individual overlay image based on input scale
		 * @return
		 */
		private int getPathWidth(double scale){
			for(int i=0;i<scales.length-1;i++){
				if(scale <= scales[i]){
					return scales.length+1;
				}else if(scales[i] < scale && scale <= scales[i+1]){
					return scales.length-i;
				}
			}
			return 1;
		}
		
		/**
		 * Get "combined" overlay image
		 * @return
		 */
		public Image getOverlay(){
			BufferedImage image = new BufferedImage(size.width, size.height,BufferedImage.TYPE_INT_ARGB);
			Graphics g = image.createGraphics();
			if(thumbnail != null)
				g.drawImage(thumbnail,0,0,null);
			Graphics2D g2 = (Graphics2D)overlay.getGraphics();
			g2.setColor(Color.black);
			path.draw(g2);
			g.drawImage(overlay,0,0,null);
			return image;
		}
		
		/**
		 * @return the thumbnail
		 */
		public Image getThumbnail() {
			return thumbnail;
		}

		/**
		 * @param thumbnail the thumbnail to set
		 */
		public void setThumbnail(Image thumbnail) {
			this.thumbnail = thumbnail;
		}

		/**
		 * @return the scales
		 */
		public double[] getScales() {
			return scales;
		}
	}
	
	
	/**
	 * This class represents a shape that is a directed path
	 * @author tseytlin
	 */
	private class DirectedPath extends Polygon {
		private Stroke arrowStroke = new BasicStroke(2,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
		private Stroke arrowShadowStroke = new BasicStroke(4,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
		private final int arrowWidth = 6, arrowHeight = 10;
		private java.util.List points = new ArrayList();
		private boolean shadow;
		private Color shadowColor = Color.white;
		
		/**
		 * @param shadowColor the shadowColor to set
		 */
		public void setShadowColor(Color shadowColor) {
			this.shadowColor = shadowColor;
		}

		/**
		 * @param shadow the shadow to set
		 */
		public void setShadow(boolean shadow) {
			this.shadow = shadow;
		}

		/**
		 * Translates the vertices of the Polygon by deltaX along the x axis and by deltaY along the y axis.
		 */
		public void translate(int dx, int dy){
			super.translate(dx,dy);
			for(int i=0;i<points.size();i++){
				Rectangle r = (Rectangle) points.get(i);
				r.x += dx;
				r.y += dy;
			}
		}
		
		/**
		 * Add new point to this path
		 */
		public void addPoint(int x, int y){
			addPoint(x,y,1);
		}
		
		/**
		 * Add new point to this path, with the width of a stroke
		 * @param x
		 * @param y
		 * @param width
		 */
		public void addPoint(Point p, int width){
			addPoint(p.x,p.y,width);
		}
		
		/**
		 * Add new point to this path, with the width of a stroke
		 * @param x
		 * @param y
		 * @param width
		 */
		public void addPoint(int x, int y, int width){
			addPoint(x,y,width,true);
		}
		
		/**
		 * Add new point to this path, with the width of a stroke
		 * @param x
		 * @param y
		 * @param width
		 * @param draw line between this and privous point
		 */
		public void addPoint(int x, int y, int width, boolean draw){
			points.add(new Rectangle(x,y,width,(draw)?1:0));
			super.addPoint(x,y);
		}
		
		
		/**
		 * Draw this shape
		 * @param g
		 */
		public void draw(Graphics2D g){
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			Point seg_st = null;
			// iterate over points
			for(int i=0;i<points.size()-1;i++){
				Rectangle p1 = (Rectangle) points.get(i);
				Rectangle p2 = (Rectangle) points.get(i+1);
				
				// assigne start of segment
				if(seg_st == null)
					seg_st = p1.getLocation();
				
				
				// draw if allowed
				Stroke stroke = new BasicStroke(p1.width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
					
				if(shadow){
					Stroke stroke2 = new BasicStroke(p1.width+2,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
					Color c = g.getColor();
					g.setColor(shadowColor);
					g.setStroke(stroke2);
					if(p2.height > 0)
						g.drawLine(p1.x,p1.y,p2.x,p2.y);
					else
						g.drawOval(p2.x,p2.y,1,1);
					g.setColor(c);
				}
		
				g.setStroke(stroke);
				if(p2.height > 0)
					g.drawLine(p1.x,p1.y,p2.x,p2.y);
				else
					g.drawOval(p2.x,p2.y,1,1);
				
				// get center of line
				Point st = p2.getLocation();
				Point en = p1.getLocation();
				if(i< (points.size()-2)){
					st.x = p1.x + (p2.x - p1.x)/2;
					st.y = p1.y + (p2.y - p1.y)/2;
				}
				
				
				// calc distance
				double d = Math.sqrt(Math.pow(st.x-seg_st.x,2)+Math.pow(st.y-seg_st.y,2));
				if(d >= 50 || (i >= (points.size()-2))){
					double d2 = Math.sqrt(Math.pow(st.x-en.x,2)+Math.pow(st.y-en.y,2));
					// draw arrow
					drawArrowPoints(g,st,(d2>arrowHeight)?en:seg_st);
					
					// reinitiate segment count
					seg_st = st;
				}
				
			}
		}
		
		/**
		 * Draw arrow polygon
		 */	
		private void drawArrowPoints(Graphics2D g, Point st_p, Point end_p) {
			// 2 points of the arrow
			Point arrowPoint = new Point();
			Point arrowEndPoint1 = new Point();
			Point arrowEndPoint2 = new Point();
			
			// calculate the distance
			/*
			double d = Math.sqrt(Math.pow(end_p.x-st_p.x,2)+Math.pow(end_p.y-st_p.y,2));
			// don't do anything if distance is too small
			if(d/2.0 < arrowHeight)
				return;
			*/
			
			// calculate the angle
			double arrowHeadAngle = Math.atan((double)arrowWidth / (double)arrowHeight);
			double arrowHeadLength = Math.sqrt(Math.pow(arrowWidth, 2D) + Math.pow(arrowHeight, 2D));
			
			
			double theta;
			if(end_p.getX() == st_p.x) {
				if(end_p.getY() > st_p.y)
					theta = 4.7123889803846897D;
				else
					theta = 1.5707963267948966D;
			} else {
				theta = Math.atan((-1D * (double)(st_p.y - end_p.getY())) / (double)(st_p.x - end_p.getX()));
			}
			if(st_p.x > end_p.getX())
				theta += 3.1415926535897931D;

			double alpha1 = theta + arrowHeadAngle;
			double alpha2 = theta - arrowHeadAngle;
			int xp1 = (int)(Math.cos(alpha1) * arrowHeadLength);
			int yp1 = (int)(Math.sin(alpha1) * arrowHeadLength);
			int xp2 = (int)(Math.cos(alpha2) * arrowHeadLength);
			int yp2 = (int)(Math.sin(alpha2) * arrowHeadLength);
			
			
			arrowPoint.x = (int)(st_p.x - Math.cos(theta));
			arrowPoint.y = (int)(st_p.y +  Math.sin(theta));
			arrowEndPoint1.x = arrowPoint.x + xp1;
			arrowEndPoint1.y = arrowPoint.y - yp1;
			arrowEndPoint2.x = arrowPoint.x + xp2;
			arrowEndPoint2.y = arrowPoint.y - yp2;
			
			// draw arrow lines
			Stroke old = g.getStroke();
			
			//	draw shadow
			if(shadow){
				Color c = g.getColor();
				g.setColor(shadowColor);
				g.setStroke(arrowShadowStroke);
				g.drawLine(arrowPoint.x,arrowPoint.y,arrowEndPoint1.x,arrowEndPoint1.y);
				g.drawLine(arrowPoint.x,arrowPoint.y,arrowEndPoint2.x,arrowEndPoint2.y);
				g.setColor(c);
			}
			
			g.setStroke(arrowStroke);
			g.drawLine(arrowPoint.x,arrowPoint.y,arrowEndPoint1.x,arrowEndPoint1.y);
			g.drawLine(arrowPoint.x,arrowPoint.y,arrowEndPoint2.x,arrowEndPoint2.y);
			g.setStroke(old);
		}
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		if(args.length > 0){
			new ViewerPathCreator(args[0]);
		}else{
			System.err.println("Usage: java ViewerPathCreator <server url>");
		}
	}

	
}
