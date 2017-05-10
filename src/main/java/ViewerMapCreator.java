import java.text.NumberFormat;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.net.*;
import java.awt.event.*;
import edu.pitt.slideviewer.*;
import edu.pitt.slideviewer.policy.DiscreteScalePolicy;
import edu.pitt.slideviewer.qview.connection.Utils;

public class ViewerMapCreator implements ActionListener {
	private JFrame frame;
	private JTabbedPane result;
	private JTextField input;
	private JButton ibrowse, create;
	private Properties prefs;
	private Map map;
	private final double[] scales = new double[] { 0.02, 0.12, 0.5};
	//private final double[] scales = new double[] { 0.06, 0.13, 0.4, 1.0 };
	
	private String cwd = System.getProperty("user.home");
	private boolean showImageTitle = true;;

	/**
	 * init map creator
	 * 
	 * @param file
	 */
	public ViewerMapCreator(String server) throws Exception {
		prefs = initViewerProperties(server);
		frame = createGUI();
		frame.setVisible(true);
		ViewerFactory.setProperties(prefs);
	}

	/**
	 * init map creator
	 * 
	 * @param file
	 */
	public ViewerMapCreator(String server, boolean createGUI) throws Exception {
		prefs = initViewerProperties(server);
		if (createGUI) {
			frame = createGUI();
			frame.setVisible(true);
		}
		ViewerFactory.setProperties(prefs);
	}

	private JFrame createGUI() {
		JFrame frame = new JFrame("Viewer Map Creator");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// input panel
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBorder(new TitledBorder("Input"));
		panel.add(new JLabel("Input text file  "), BorderLayout.WEST);
		input = new JTextField();
		panel.add(input, BorderLayout.CENTER);
		ibrowse = new JButton("Browse");
		ibrowse.addActionListener(this);
		panel.add(ibrowse, BorderLayout.EAST);
		create = new JButton("Create Viewer Map");
		create.addActionListener(this);
		panel.add(create, BorderLayout.SOUTH);

		// result panel
		result = new JTabbedPane();
		result.setPreferredSize(new Dimension(550, 550));

		// combined
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panel,
				result);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(split, BorderLayout.CENTER);
		frame.pack();
		return frame;
	}

	private Properties initViewerProperties(String server) {
		Properties prefs = new Properties();
		boolean loaded = false;

		// load properties from server
		try {
			URL url = new URL(server + "/SlideViewer.conf");
			prefs.load(url.openStream());
			loaded = true;
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// prop not loaded, then load hard-coded defaults
		if (!loaded) {
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
			prefs.setProperty("mscope.server.url", server
					+ "/servlet/mScopeServlet");
			prefs.setProperty("mscope.servlet.url", server
					+ "/servlet/mScopeThumbnailServlet");
			prefs.setProperty("mscope.image.dir", "e:\\slideimages\\"); // "/e/slideimages/");
			prefs
					.setProperty("xippix.server.url", server
							+ "/servlet/IPServer");
			prefs.setProperty("xippix.image.dir", "e:\\slideimages\\");
			prefs.setProperty("qview.server.url", host + ":82/");
			prefs.setProperty("qview.image.dir", "");
		}
		return prefs;
	}

	/**
	 * Open image in the viewer temporarily Get image dimensions and thumbnail
	 * image
	 * 
	 * @param name
	 * @return { Dimension, Image }
	 * 
	 *         private Object [] getImageInfo(String name){ Object [] result =
	 *         new Object [2];
	 * 
	 *         // create viewer window & viewer if(viewer == null){ // create
	 *         viewer ViewerFactory.setProperties(prefs); viewer =
	 *         ViewerFactory.
	 *         getViewerInstance(prefs.getProperty("image.server.type"
	 *         ,"qview")); }
	 * 
	 *         // switch viewer if necessary String type =
	 *         ViewerFactory.recomendViewerType(name);
	 *         if(!type.equals(prefs.getProperty("image.server.type"))){
	 *         prefs.setProperty("image.server.type",type);
	 * 
	 *         Viewer v = ViewerFactory.getViewerInstance(type);
	 * 
	 *         // remove component viewer.dispose(); viewer = null;
	 * 
	 *         // add something to viewer window viewer = v; }
	 * 
	 *         // now, load image try { viewer.setSize(new Dimension(500,500));
	 *         viewer.setImage(prefs.getProperty(type+".image.dir","")+name); }
	 *         catch (ViewerException ex) { ex.printStackTrace();
	 *         JOptionPane.showMessageDialog(frame,ex.getMessage(), "Error",
	 *         JOptionPane.ERROR_MESSAGE); return null; }
	 * 
	 *         // resize frame to correct size Dimension isize =
	 *         viewer.getImageSize(); Dimension size = new Dimension(500,500);
	 * 
	 *         // adjust size horizontal adjust height if(isize.width >
	 *         isize.height) size.height = (int)
	 *         ((size.width*isize.getHeight())/isize.getWidth()); else
	 *         size.width = (int)
	 *         ((size.height*isize.getWidth())/isize.getHeight());
	 * 
	 *         // resize frame (for beutiful screenshot) viewer.setSize(size);
	 *         viewer.getViewerController().resetZoom();
	 * 
	 *         // take snapshot Image img = viewer.getSnapshot();
	 * 
	 *         // assign results result[0] = isize; result[1] = img;
	 * 
	 *         return result; }
	 */
	/**
	 * Make sure image loads!!!!
	 * 
	 * @param img
	 */
	public void flushImage(Image img) {
		MediaTracker tracker = new MediaTracker(frame);
		tracker.addImage(img, 0);
		try {
			tracker.waitForAll();
		} catch (InterruptedException ex) {
		}
		tracker.removeImage(img, 0);
	}

	/**
	 * Create maps
	 */
	public void doCreateMap(String file) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			map = createMap(reader);
			reader.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
		if (map != null) {
			displayMap(map);
		}
	}

	/**
	 * Create maps
	 */
	public void doCreateMap(String file, boolean display) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			map = createMap(reader);
			reader.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
		if (display && map != null) {
			displayMap(map);
		}
	}

	public void saveOverlayByName(String name, String dir) {
		name = name.substring(name.lastIndexOf("-") + 1);
		name = name.replace(".txt", ".tif");
		OverlayMap overlay = (OverlayMap) map.get(name);
		System.out.println("about to save: "
				+ getJpegName(dir, overlay.getImageName()));
		File f = new File(getJpegName(dir, overlay.getImageName()));
		doSaveOverlay(overlay, f, false);
	}

	/**
	 * Save output as a file
	 * 
	 * @param overlay
	 * @param file
	 */
	public void doSaveOverlay(OverlayMap overlay, File file, boolean show) {
		try {
			BufferedImage result = null;
			Image thumb = overlay.getThumbnail();
			Image[] images = overlay.getOverlays();

			Dimension size = new Dimension(thumb.getWidth(null), thumb
					.getHeight(null));
			boolean horizontal = (size.width > size.height);
			int x = 0, y = 0;

			// figure out direction we are going to add stuff
			if (horizontal) {
				// if horizontal
				result = new BufferedImage(size.width,size.height * (2 + images.length),
						BufferedImage.TYPE_INT_RGB);
				//y = size.height;
			} else {
				// if vertical
				result = new BufferedImage(size.width* (2 + images.length), size.height,
						BufferedImage.TYPE_INT_RGB);
				//x = size.width;
			}

			// draw overlay
			Graphics g = result.getGraphics();
			g.setColor(Color.black);

			// draw thumbnail
			g.drawImage(thumb, x, y, null);
			g.drawRoundRect(x, y, size.width - 2, size.height - 2, 15, 15);

			// increment
			if (horizontal)
				y += size.height;
			else
				x += size.width;

			g.drawImage(overlay.getOverlay(), x, y, null);
			if (showImageTitle)
				g.drawString(overlay.getImageName(), 5, 15);
			g.drawRoundRect(x, y, size.width - 2, size.height - 2, 15, 15);

			// increment
			if (horizontal)
				y += size.height;
			else
				x += size.width;
			
			// add overlays to list
			double[] scales = overlay.getScales();

			NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(3);
			
			// draw overlays
			for (int i = 0; i < images.length; i++) {
				// add scale text to icon
				g.drawImage(thumb, x, y, null);
				g.drawImage(images[i], x, y, null);
				if (showImageTitle)
					g.drawString("" + nf.format(scales[i]) + " - " + ((i < scales.length-1)?nf.format(scales[i + 1]):1.0), x + 5,	y + 15);
				g.drawRoundRect(x, y, size.width - 2, size.height - 2, 15, 15);

				// increment
				if (horizontal)
					y += size.height;
				else
					x += size.width;
			}

			// ImageIO.write(result,"JEPG",file);
			ViewerHelper.writeJpegImage(result, file);
			if (show)
				JOptionPane.showMessageDialog(frame, new ImageIcon(result), "",
						JOptionPane.PLAIN_MESSAGE);
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}

	}

	// button press handler
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (e.getSource() == ibrowse) {
			JFileChooser chooser = new JFileChooser(cwd);
			int result = chooser.showOpenDialog(frame);
			if (result == JFileChooser.APPROVE_OPTION) {
				input.setText(chooser.getSelectedFile().getAbsolutePath());
			}
		} else if (e.getSource() == create) {
			doCreateMap(input.getText());
		} else if (cmd.startsWith("save:")) {
			String name = cmd.substring(5);
			if (map != null) {
				OverlayMap overlay = (OverlayMap) map.get(name);
				if (overlay != null) {
					JFileChooser chooser = new JFileChooser(cwd);
					String fname = cwd + File.separator + "viewmap-"
							+ overlay.getImageName();
					int n = fname.lastIndexOf(".");
					if (n > -1)
						fname = fname.substring(0, n);
					chooser.setSelectedFile(new File(fname + ".jpg"));
					int result = chooser.showSaveDialog(frame);
					if (result == JFileChooser.APPROVE_OPTION) {
						doSaveOverlay(overlay, chooser.getSelectedFile(), true);
					}
				}
			}
		}
	}

	public String getJpegName(String dir, String iname) {
		String fname = dir + File.separator + "viewmap-" + iname;
		int n = fname.lastIndexOf(".");
		if (n > -1)
			fname = fname.substring(0, n);
		return fname + ".jpg";
	}

	/**
	 * Given reader that has pointer to file orig image size and source
	 * thumbnail image build a map
	 * 
	 * @param reader
	 * @param isize
	 * @param thumbnail
	 */
	public Map createMap(BufferedReader reader) throws Exception {
		OverlayMap overlay = null;
		Map overlays = new HashMap();
		// now iterate over input file to record viewer movements
		Dimension viewSize = new Dimension(0, 0);
		boolean minScaleSet = false;
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			// skip comments
			if (line.startsWith("#"))
				continue;

			// parse line
			String[] entries = line.split("\\" + Constants.ENTRY_SEPARATOR);
			// System.out.println("processing line :"+line);
			if (entries.length > 1) {
				String p = entries[0].trim();
				if (p.equalsIgnoreCase(Constants.VIEW_TAG)
						|| p.equalsIgnoreCase(Constants.OBSERVE_TAG)) {
					int x = Integer.parseInt(entries[1]);
					int y = Integer.parseInt(entries[2]);
					double s = Double.parseDouble(entries[3]);
					// add rectangle to overlay
					if (overlay != null) {
						int w = (int) (viewSize.getWidth() / s);
						int h = (int) (viewSize.getHeight() / s);
						Rectangle r = new Rectangle(x, y, w, h);
						if(!minScaleSet){
							overlay.adjustScales(s,viewSize);
							minScaleSet = true;
						}
						overlay.addViewRectangle(r);
					}
				} else if (p.equalsIgnoreCase(Constants.RESIZE_TAG)) {
					int w = Integer.parseInt(entries[1]);
					int h = Integer.parseInt(entries[2]);
					viewSize.setSize(w, h);
				} else if (p.equalsIgnoreCase(Constants.IMAGE_TAG)) {
					String imageName = entries[1].trim();
					// load image to get its screenshot and thumbnail
					// Object [] obj = getImageInfo(imageName);
					ImageProperties ip = ViewerFactory
							.getImageProperties(imageName);

					// for now hardcoded
					// Dimension isize = (Dimension) obj[0];
					// Image thumbnail = (Image) obj[1];
					Dimension isize = ip.getImageSize();
					Image thumbnail = ip.getThumbnail();

					// prepare images for different powers
					Dimension size = null;
					if (thumbnail != null) {
						size = new Dimension(thumbnail.getWidth(null),
								thumbnail.getHeight(null));
					}
					viewSize.setSize(size);

					// create overlay
					overlay = new OverlayMap(isize, size, Arrays.copyOf(scales,scales.length));
					overlay.setThumbnail(thumbnail);
					overlay.setImageName(imageName);
					overlay.setDate(Constants.DATE_FORMAT.parse(entries[2]));

					// add to map
					overlays.put(imageName, overlay);

				}
			}
		}

		return overlays;
	}


	/**
	 * Create output panel
	 * 
	 * @return
	 */

	private JPanel createOutputPanel(OverlayMap overlay) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		// get overlay images
		Image thumb = overlay.getThumbnail();
		Image[] images = overlay.getOverlays();
		DefaultListModel model = new DefaultListModel();
		JList list = new JList(model);
		list.setVisibleRowCount(1);

		// add uber overlay
		model.addElement(new ImageIcon(overlay.getOverlay()));

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(3);
		
		// add overlays to list
		Dimension d = null;
		double[] scales = overlay.getScales();
		for (int i = 0; i < images.length; i++) {
			// add scale text to icon
			Image img = frame.createImage(images[i].getWidth(null), images[i]
					.getHeight(null));
			Graphics g = img.getGraphics();
			g.setColor(Color.black);
			g.drawImage(thumb, 0, 0, null);
			g.drawImage(images[i], 0, 0, null);
			g.drawString("" + nf.format(scales[i]) + " - " + ((i < scales.length-1)?nf.format(scales[i + 1]):1.0), 5, 15);

			// add icon to list
			Icon icon = new ImageIcon(img);
			model.addElement(icon);
			if (d == null)
				d = new Dimension(icon.getIconWidth() + 30, icon
						.getIconHeight());
		}
		ArrayList slist = new ArrayList();
		for (int i = 0; i < scales.length; i++) {
			slist.add(new Double(scales[i]));
		}

		// create container
		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints gc = new GridBagConstraints(0, 0, 1, 1, 1, 1,
				GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
				new Insets(5, 5, 5, 5), 0, 0);
		JPanel cont = new JPanel();
		cont.setLayout(gb);
		JScrollPane scroll = new JScrollPane(list);
		scroll.setMinimumSize(d);
		gb.setConstraints(scroll, gc);
		cont.add(scroll);

		// create info label
		Dimension isize = overlay.getImageSize();

		JLabel lbl = new JLabel("<html><table border=0 width=300>"
				+ "<tr><td>image name:</td><td><b>" + overlay.getImageName()
				+ "</b></td></tr>" + "<tr><td>image size:</td><td><b>"
				+ isize.width + " x " + isize.height + "</b></td></tr>"
				+ "<tr><td>view time:</td><td><b>" + overlay.getDate()
				+ "</b></td></tr>" + "<tr><td>zoom gradient:</td><td>" + slist
				+ "</td></tr></table>");

		// add info label
		if (isize.width > isize.height) {
			gc.gridy++;
			gc.gridheight = 2;
			gc.anchor = GridBagConstraints.NORTHWEST;
			gc.fill = GridBagConstraints.HORIZONTAL;
		} else {
			gc.gridx++;
			gc.gridwidth = 2;
			gc.anchor = GridBagConstraints.NORTHWEST;
			gc.fill = GridBagConstraints.HORIZONTAL;
		}

		gb.setConstraints(lbl, gc);
		cont.add(lbl);

		// create save button
		JButton save = new JButton("Save Result");
		save.setActionCommand("save:" + overlay.getImageName());
		save.addActionListener(this);

		// add to panel
		// panel.add(lbl,BorderLayout.NORTH);
		panel.add(cont, BorderLayout.CENTER);
		panel.add(save, BorderLayout.SOUTH);

		return panel;
	}

	/**
	 * Display overlay images
	 * 
	 * @param map
	 */
	public void displayMap(Map map) {
		// remove previous results
		result.removeAll();

		// iterate over images
		Set keys = map.keySet();
		for (Iterator k = keys.iterator(); k.hasNext();) {
			String key = "" + k.next();
			OverlayMap overlay = (OverlayMap) map.get(key);
			result.addTab(key, createOutputPanel(overlay));
			// JOptionPane.showMessageDialog(null,new
			// JScrollPane(list),key,JOptionPane.PLAIN_MESSAGE);
		}
	}

	/**
	 * This class encapsulates overlays
	 * 
	 * @author tseytlin
	 */
	private class OverlayMap {
		private BufferedImage[] overlay;
		private double[] scales,oscales;
		private Dimension isize, size;
		private Image thumbnail;
		private String imageName;
		private Date date;

		/**
		 * @return the date
		 */
		public Date getDate() {
			return date;
		}

		/**
		 * @param date
		 *            the date to set
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
		 * @param imageName
		 *            the imageName to set
		 */
		public void setImageName(String imageName) {
			this.imageName = imageName;
		}

		/**
		 * Get size of image in pixels
		 * 
		 * @return
		 */
		public Dimension getImageSize() {
			return isize;
		}

		/**
		 * Create an overlay map
		 * 
		 * @param isize
		 *            - size of map in absolute coordinates
		 * @param size
		 *            - size of map in relative coordinates
		 * @param scales
		 *            [] - list of scales that correspond to map granularity Ex.
		 *            {0,0.25,0.5,1} will create 3 overlays 0 < x <= 0.25 0.25 <
		 *            x <= 0.5 0.5 < x <= 1
		 */
		public OverlayMap(Dimension isize, Dimension size, double[] scales) {
			this.isize = isize;
			this.size = size;
			this.scales = scales;

			// init images
			overlay = new BufferedImage[scales.length];
			for (int i = 0; i < overlay.length; i++)
				overlay[i] = new BufferedImage(size.width, size.height,	BufferedImage.TYPE_INT_ARGB);
		}

		/**
		 * Draw view rectangle on some overlay image
		 */
		public void addViewRectangle(Rectangle r) {
			int w = (int) (r.width * size.width / isize.width);
			int h = (int) (r.height * size.height / isize.height);
			int x = (int) (r.x * size.width / isize.width);
			int y = (int) (r.y * size.height / isize.height);

			// determine scale
			boolean hrz = isize.width > isize.height;
			double scale = (hrz) ? size.getWidth() / r.width : size.getHeight() / r.height;

			// draw on mold
			float shade = getOverlayShade(scale);
			Image img = getOverlay(scale);
			if (img != null && scale >= scales[0]) {
				Graphics2D mg = (Graphics2D) img.getGraphics();
				mg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC,shade));
				mg.setColor(getOverlayColor(scale));
				mg.fillRect(x, y, w, h);
			}
		}

		/**
		 * Get individual overlay image based on input scale
		 * 
		 * @return
		 */
		public Image getOverlay(double scale) {
			int x = getOverlayIndex(scale);
			if (x > -1)
				return overlay[x];
			else
				return null;
		}

		/**
		 * Get individual overlay image based on input scale
		 * 
		 * @return
		 */
		private int getOverlayIndex(double scale) {
			/*
			for (int i = 0; i < scales.length - 1; i++) {
				if (scale <= scales[i]) {
					return -1;
				} else if (scales[i] < scale && scale <= scales[i + 1]) {
					return i;
				}
			}
			return overlay.length - 1;
			*/
			for(int i=scales.length-1;i>= 0;i--)
				if(scales[i] <= scale)
					return i;
			return -1;
		}
		
		public void adjustScales(double min,Dimension viewSize){
			// go to next power after min
			scales[0] = new DiscreteScalePolicy().getNextScale(min);
			
			// make sure that the new min is smaller then the 
			// next thing
			int x = 0;
			for(int i=1;i<scales.length;i++){
				if(min >= scales[i]){
					x = i;
				}
			}
			
			// if we do have a large min
			if(x > 0){
				double [] s = new double [scales.length - x];
				s[0] = min;
				for(int i=1;i<s.length;i++){
					s[i] = scales[s.length-1+i]; 
				}
				scales = s;
			}
			
			
			Dimension d = new Dimension(viewSize);
			oscales = new double [scales.length];
			
			// adjust view size to fit the same aspect ratio as size
			if(isize.width > isize.height){
				d.height = isize.height * d.width / isize.width;
			}else{
				d.width = isize.width * d.height / isize.height;
			}
			
			// adjust all scales
			for(int i=0;i<scales.length;i++){
				oscales[i] = scales [i];
				scales[i] = scales[i] * size.width / d.width;
			}
			
			// init images
			if(overlay.length != scales.length){
				overlay = new BufferedImage[scales.length];
				for (int i = 0; i < overlay.length; i++)
					overlay[i] = new BufferedImage(size.width, size.height,	BufferedImage.TYPE_INT_ARGB);
			}
			
			
		}

		/**
		 * Get individual overlay image based on input scale
		 * 
		 * @return
		 */
		private float getOverlayShade(double scale) {
			int i = getOverlayIndex(scale) + 1;
			float inc = 0.4f / overlay.length;
			return inc * i;
		}

		/**
		 * Get individual overlay image based on input scale
		 * 
		 * @return
		 */
		private Color getOverlayColor(double scale) {
			if (scale < scales[0])
				return Color.white;
			else
				return Color.green;
		}

		/**
		 * Get individual overlay image
		 * 
		 * @return
		 */
		public Image getOverlay(int i) {
			if (i >= 0 && i < overlay.length)
				return overlay[i];
			else
				return null;
		}

		/**
		 * Return overlays for each power
		 * 
		 * @return
		 */
		public Image[] getOverlays() {
			return overlay;
		}

		/**
		 * Get "combined" overlay image
		 * 
		 * @return
		 */
		public Image getOverlay() {
			BufferedImage image = new BufferedImage(size.width, size.height,
					BufferedImage.TYPE_INT_ARGB);
			Graphics g = image.createGraphics();
			if (thumbnail != null)
				g.drawImage(thumbnail, 0, 0, null);
			for (int i = 0; i < overlay.length; i++) {
				g.drawImage(overlay[i], 0, 0, null);
			}
			return image;
		}

		/**
		 * @return the thumbnail
		 */
		public Image getThumbnail() {
			return thumbnail;
		}

		/**
		 * @param thumbnail
		 *            the thumbnail to set
		 */
		public void setThumbnail(Image thumbnail) {
			this.thumbnail = thumbnail;
		}

		/**
		 * @return the scales
		 */
		public double[] getScales() {
			return (oscales != null)?oscales:scales;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if (args.length > 0) {
			new ViewerMapCreator(args[0]);
		} else {
			System.err.println("Usage: java ViewerMapCreator <server url>");
		}
	}

}
