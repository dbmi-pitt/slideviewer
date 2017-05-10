

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.*;
import javax.swing.*;
import java.io.*;
import javax.swing.event.*;
import edu.pitt.slideviewer.*;
import javax.swing.border.*;

public class ImageBrowser  implements ListSelectionListener, ActionListener, Runnable {
	private final int TILE_LIMIT = 100;
	private URL server;
	private JList headerList, imageList;
	private JTextArea selectionText;
	private JLabel info,statusLabel;
	private JButton add,rem,clear;
	private JPanel status;
	private Set selection;
	private Map images;
	private Collection currentSelection;
	private boolean stop;
	private Thread currentThread;
	private JProgressBar progress;
	private Properties viewerPrefs;
	private JFrame frame;
	
	/**
	 * create instance of browser
	 *
	 */
	public ImageBrowser(String server){
		initViewer(server);
	}
	
	private void initViewer(String serverU){
		viewerPrefs = new Properties();
		// load properties from server
		try{
			URL url =new URL(serverU);
			server = new URL(url+"/SlideViewer.conf");
			viewerPrefs.load(server.openStream());
		}catch(Exception ex){}
		ViewerFactory.setProperties(viewerPrefs);
		//System.out.println(viewerPrefs);
	}
	
	/**
	 * build GUI
	 */
	public void init(){
		// init fields
		Dimension size = new Dimension(250,500);
		headerList = new JList(new DefaultListModel());
		headerList.addListSelectionListener(this);
		imageList = new JList(new DefaultListModel());
		imageList.addListSelectionListener(this);
		imageList.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2)
					openImage(imageList.getSelectedValue());
			}
		});
		JToolBar toolbar = new JToolBar();
		add = new JButton("    ADD    ");
		add.addActionListener(this);
		toolbar.add(add);
		rem = new JButton(" REMOVE ");
		rem.addActionListener(this);
		toolbar.add(rem);
		clear = new JButton(" CLEAR ");
		clear.addActionListener(this);
		toolbar.add(clear);
		selectionText = new JTextArea();
		selectionText.setLineWrap(true);
		selectionText.setWrapStyleWord(true);
		selectionText.setEditable(false);
		JScrollPane scroll = new JScrollPane(selectionText);
		scroll.setBorder(new TitledBorder("Selection"));
		scroll.setBackground(Color.white);
		info = new JLabel();
		info.setPreferredSize(new Dimension(250,200));
		info.setBorder(new TitledBorder("Info"));
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setLayout(new BorderLayout());
		panel.add(toolbar,BorderLayout.NORTH);
		panel.add(scroll,BorderLayout.CENTER);
		panel.add(info,BorderLayout.SOUTH);
		panel.setPreferredSize(size);
		statusLabel = new JLabel(" ");
		status = new JPanel();
		status.setLayout(new BorderLayout());
		status.add(statusLabel,BorderLayout.CENTER);
		progress = new JProgressBar();
		progress.setMinimum(0);
		progress.setMaximum(TILE_LIMIT);
		JScrollPane s1 = new JScrollPane(headerList);
		JScrollPane s2 = new JScrollPane(imageList);
		s1.setPreferredSize(size);
		s2.setPreferredSize(size);
		JSplitPane p1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,s1,s2);
		p1.setResizeWeight(.5);
		JSplitPane p2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,p1,panel);
		
		// init selection list
		selection = new HashSet();
		
		
		// init frame
		frame = new JFrame("ImageBrowser");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		((JComponent)frame.getContentPane()).setBorder(new BevelBorder(BevelBorder.RAISED));
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(p2,BorderLayout.CENTER);
		frame.getContentPane().add(status,BorderLayout.SOUTH);
		frame.pack();
		frame.setVisible(true);
		
	}
	
	
	
	
	/**
	 * open image
	 * @param img
	 */
	public void openImage(Object img){
		String filename = ""+img;
		
		// create virtual microscope panel
		String type = ViewerFactory.recomendViewerType(filename);
		final Viewer viewer = ViewerFactory.getViewerInstance(type);
		viewer.setSize(500,500);
		
		// load image
		try {
			viewer.openImage(viewerPrefs.getProperty(type+".image.dir","")+filename);
		} catch (ViewerException ex) {
			// ex.printStackTrace();
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		// open up dialog
		JDialog d = new JDialog(frame,"SlideViewer: "+filename,false);
		d.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				viewer.dispose();
			}
		});
		d.setResizable(true);
		d.getContentPane().add(viewer.getViewerPanel());
		d.pack();
		d.setVisible(true);
	}
	
	/**
	 * item was selected
	 */
	public void valueChanged(ListSelectionEvent e){
		if(e.getSource() == headerList){
			// stop current thread
			if(currentThread != null && currentThread.isAlive()){
				stop = true;
				try{
					currentThread.join();
				}catch(InterruptedException ex){}
			}
			stop = false;
			Object key = headerList.getSelectedValue();
			currentSelection = (Collection)images.get(key);
			// init progress bar
			status.remove(statusLabel);
			status.add(progress,BorderLayout.CENTER);
			status.revalidate();
			//start new thread
			currentThread = new Thread(this);
			currentThread.start();
		}else if(e.getSource() == imageList){
			// show info
			ImageEntry ie = (ImageEntry) imageList.getSelectedValue();
			if(ie != null && ie.getProperties() != null)
				info.setText(ie.getProperties().getHTMLDescription());
		}	
	}
	
	/**
	 * button actions
	 */
	public void actionPerformed(ActionEvent e){
		if(e.getSource() == add){
			Object [] vals = imageList.getSelectedValues();
			Collections.addAll(selection,vals);
		}else if(e.getSource() == rem){
			Object [] vals = imageList.getSelectedValues();
			selection.removeAll(Arrays.asList(vals));
		}else if(e.getSource() == clear){
			selection.clear();
		}
		selectionText.setText(""+selection);
		
		// update status
		statusLabel.setText("There are "+selection.size()+" selected slides");
	}
	
	/**
	 * shorten name
	 * @param name
	 * @return
	 */
	private String shorterName(String name){
		if(name == null)
			return "";
		
		if(name.length() > 15)
			return name.substring(0,15)+"..";
		return name;
	}
	
	/**
	 * load images
	 */
	public void load(){
		load(".*");
	}
	
	/**
	 * load images
	 */
	public void load(String filter){
		statusLabel.setText("fetching slide images ...");
		Collection imgs = ViewerFactory.getImageList();
		// create pages
		String first = null;
		ArrayList list = new ArrayList();
		Vector<String> keys = new Vector<String>();
		images = new LinkedHashMap();
		for(Iterator i=imgs.iterator();i.hasNext();){
			String img = ""+i.next();
			
			//	filter images
			//if(!img.matches(filter)){
			//	continue;
			
			
			// init first entry in the list
			if(list.size() == 0 && img.matches(filter)){
				first = img;
			}
			
			
			//add to list
			if(img.matches(filter))
				list.add(img);
						
			// if over the limit, create next page
			if(list.size() >= TILE_LIMIT || !i.hasNext()){
				String key = shorterName(first)+"-"+shorterName(""+list.get(list.size()-1));
				images.put(key,list);
				list = new ArrayList();
				// add to header 
				//((DefaultListModel) headerList.getModel()).addElement(key);
				keys.addElement(key);
			}
		}
		headerList.setListData(keys);
		statusLabel.setText("slide names fetched");
	}
	
	
	/**
	 * loading of thumbnails
	 */
	public void run(){
		if(currentSelection != null)
			loadThumbnails(currentSelection);
	}
	
	/**
	 * load thumbnails
	 * @param list
	 */
	private void loadThumbnails(Collection list){
		int j = 1;
		((DefaultListModel)imageList.getModel()).clear();
		for(Iterator i=list.iterator();i.hasNext() && !stop;){
			String name = ""+i.next();
			ImageEntry slide = new ImageEntry(name);

			if(slide.getThumbnail() == null){
				try{
					ImageProperties im = ViewerFactory.getImageProperties(slide.getSlideName());
					if(im != null && im.getThumbnail() != null){
						slide.setThumbnail(im.getThumbnail());
						slide.setImageSize(im.getImageSize());
						slide.setProperties(im);
						// clear image to save space
						im.setThumbnail(null);
					}
				}catch(Exception ex){
					System.err.println(ex.getMessage());
				}
			}
			((DefaultListModel)imageList.getModel()).addElement(slide);
			progress.setValue(j++);
		}
		status.remove(progress);
		status.add(statusLabel,BorderLayout.CENTER);
		status.revalidate();
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length == 0){
			System.err.println("Usage: java ImageBrowser <server url> [filter]");
			return ;
		}
		// 
		ImageBrowser ib = new ImageBrowser(args[0]);
		ib.init();
		ib.load((args.length > 1)?args[1]:".*");
	}
	
	
	/**
	 * This represents slide entry
	 * @author tseytlin
	 *
	 */
	private class ImageEntry implements Serializable, Icon, Comparable, Runnable {
		private final int iconOffset = 4;
		private Dimension iconSize = new Dimension(200,100);
		private String name, slideName;
		private boolean loaded, primarySlide, loadedSlide;
		private Dimension imageSize;
		private Image thumbnail;
		//private Color [] historyColor;
		//private float [] historyShade;
		private final Font font = new Font("Sans",Font.BOLD,11);
		//private final Color shadeColor = new Color(150,200,150,75);
		private transient Component component;
		private ImageProperties properties;
		private final Color flashColor = new Color(200,200,150,150);
		private boolean flashing,flash;
		private ViewPosition viewPosition;
		
		public ImageEntry(String slideName){
			this.slideName = slideName;
		}
		
				
		/**
		 * height of icon
		 * @return
		 */
		public int getIconHeight(){
			return iconSize.height+iconOffset*2;
		}
		
		/**
		 * height of icon
		 * @return
		 */
		public int getIconWidth(){
			return iconSize.width+iconOffset*2;
		}
		
			
		
		/**
		 * Is slide currently loaded
		 * @return the loaded
		 */
		public boolean isOpened() {
			return loaded;
		}
		/**
		 * @param loaded the loaded to set
		 */
		public void setOpened(boolean loaded) {
			this.loaded = loaded;
		}
		
		/**
		 * is slide currently being viewed
		 * @param v
		 * @return
		 */
		public boolean isCurrentlyOpen(Viewer v){
			return getImageName(slideName).equals(getImageName(v.getImage()));
		}
		
		/**
		 * generate name
		 * @return
		 */
		private String createName(){
			return slideName;
		}
		
		
		/**
		 * @return the name
		 */
		public String getName() {
			// compose unique name if name is unavailable
			if(name == null)
				name = createName();
			return name;
		}

		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}
	
		/**
		 * @return the slideName
		 */
		public String getSlideName() {
			return slideName;
		}
		/**
		 * @param slideName the slideName to set
		 */
		public void setSlideName(String slideName) {
			this.slideName = slideName;
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
		public void setThumbnail(Image thumb) {
			BufferedImage img = new BufferedImage(iconSize.width,iconSize.height,BufferedImage.TYPE_INT_RGB);
			// figure out offset
			int bw = img.getWidth();
			int bh = img.getHeight();
			int tw = thumb.getWidth(null);
			int th = thumb.getHeight(null);
			
			// image offset coordinages
			int w = bw;
			int h = bh;
			
			//adjust width or height
			if(((double)bw/bh) < ((double)tw/th)){
				h = (int)(((double)w*th)/tw);
			}else{
				w = (int)(((double)h*tw)/th);
			}
			
			// set origin
			int x = (w < bw)?bw-w:0;
			int y = 0;
			
			// draw image on buffer
			Graphics2D g = img.createGraphics();
			g.setColor(new Color(240,240,250));
			g.fillRect(0,0,bw,bh);
			g.drawImage(thumb,x,y,w,h,Color.white,null);
			g.setColor(Color.black);
			g.drawRect(0,0, bw-1, bh-1);
			
			this.thumbnail = img;
		}
		/**
		 * @return the viewPosition
		 */
		public ViewPosition getViewPosition() {
			return viewPosition;
		}
		
		/**
		 * @param viewPosition the viewPosition to set
		 */
		public void setViewPosition(ViewPosition viewPosition) {
			this.viewPosition = viewPosition;
		}
		
		/**
		 * return slideName 
		 */
		public String toString(){
			return slideName;
		}

		/**
		 * @return the loadedSlide
		 */
		public boolean isLoadedSlide() {
			return loadedSlide;
		}

		/**
		 * @param loadedSlide the loadedSlide to set
		 */
		public void setLoadedSlide(boolean loadedSlide) {
			this.loadedSlide = loadedSlide;
		}

		/**
		 * @return the primarySlide
		 */
		public boolean isPrimarySlide() {
			return primarySlide;
		}

		/**
		 * @param primarySlide the primarySlide to set
		 */
		public void setPrimarySlide(boolean primarySlide) {
			this.primarySlide = primarySlide;
		}
		
		/**
		 * paint this icon
		 */
		public void paintIcon(Component c, Graphics g, int x, int y){
			this.component = c;
			int w = iconSize.width;
			int h = iconSize.height;
			if(thumbnail != null){
				g.drawImage(thumbnail,x+iconOffset,y+iconOffset,w,h,Color.white,null);
			}else{
				g.setColor(Color.PINK);
				g.fillRect(x+iconOffset,y+iconOffset,w,h);
			}
			
			// draw viewed shade or flash
			if(flash){
				g.setColor(flashColor);
				g.fillRect(x+iconOffset,y+iconOffset,w,h);
			}
			
			
			// paint text on top
			paintText(c,g,getName(),x+iconOffset+3,y+iconOffset+h+6);
		}
		
		
		 /* paint */
		private void paintText(Component c, Graphics g, String text, int xo, int yo) {
			Graphics2D g2 = (Graphics2D) g;
			// Enable antialiasing for text
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			char[] chars = text.toCharArray();

			FontMetrics fm = c.getFontMetrics(font);
			int h = yo - fm.getAscent();
			g.setFont(font);

			for (int i=0,x=xo,w=0; i < chars.length; i++, x+=w) {
				char ch = chars[i];
				w = fm.charWidth(ch);

				g.setColor(Color.white);
				g.drawString("" + chars[i], x - 1, h + 1);
				g.drawString("" + chars[i], x + 1, h + 1);
				g.drawString("" + chars[i], x, h - 1);
				g.drawString("" + chars[i], x - 1, h - 1);
				g.setColor(Color.black);
				g.drawString(""+chars[i],x,h);
			}
		}
		

		
		/**
		 * compare this slide to other
		 */
		public int compareTo(Object obj){
			return toString().compareTo(obj.toString());
		}
		
		/**
		 * here for flashing purposes
		 */
		public void run(){
			Component comp = getParent(component);
			while(flashing){
				flash = !flash;
				try{
					Thread.sleep(200);
				}catch(InterruptedException ex){}
				
				if(comp != null){
					comp.repaint();
				}
			}
		}
		
		/**
		 * get parent component of this entry
		 * @param c
		 * @return
		 */
		private Component getParent(Component c){
			if(c == null)
				return null;
			if(c instanceof JList)
				return c;
			return getParent(c.getParent());
		}
		
		
		/**
		 * start/stop flashing of a thumbnail
		 * @param b
		 */
		public void setFlash(boolean b){
			// start new thread if not started yet
			if(b && !flashing){
				flashing = b;
				(new Thread(this)).start();
			}
			// set flashing
			flashing = b;
			flash = b;
		}
		
		
		/**
		 * @return the imageSize
		 */
		public Dimension getImageSize() {
			return imageSize;
		}

		/**
		 * @param imageSize the imageSize to set
		 */
		public void setImageSize(Dimension imageSize) {
			this.imageSize = imageSize;
		}
		
		
		
		/**
		 * get image name from image path
		 * @param name
		 * @return
		 */
		public String getImageName(String name){
			// check forward slash
			int i = name.lastIndexOf("/");
			// check backslash
			if(i < 0)
				i = name.lastIndexOf("\\");
			// if found get substring
			if(i > -1 && i<name.length()-1)
				return name.substring(i+1);
			else
				return name;
		}


		/**
		 * @return the properties
		 */
		public ImageProperties getProperties() {
			return properties;
		}


		/**
		 * @param properties the properties to set
		 */
		public void setProperties(ImageProperties properties) {
			this.properties = properties;
		}
	}

}
