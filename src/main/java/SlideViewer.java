import java.beans.*;
import edu.pitt.slideviewer.*;
import edu.pitt.slideviewer.markers.*;
import edu.pitt.slideviewer.policy.*;
import edu.pitt.slideviewer.qview.QuickViewer;
import edu.pitt.slideviewer.simple.SimpleViewer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.io.*;
import java.net.MalformedURLException;
import java.util.*;
import java.util.List;
import java.net.*;

/**
 * This is a test class that uses Viewer.
 */
public class SlideViewer implements ActionListener, PropertyChangeListener, ChangeListener {
	private JFrame frame;
	private Recorder recorder;
	private Player player;
	private JLabel imageName;
	private JButton load, screen, config, close;
	private JLabel statusLocation, statusOperation;
	private JDialog shapesDialog;
	private JList list;
	private final String iconPath = "/icons/";
	private final String iconExt = ".gif";
	private Viewer viewer;
	private JToggleButton cur_selection = null, viewShapes, record, play;
	//private String imageDir;
	private ViewerHelper.SnapshotChooserPanel chooserPanel;
	private JPanel configPanel;
	private JToolBar statusPanel;
	private JPopupMenu popup;
	private JComboBox viewerType, policyChooser;
	private JTextField serverText, prefixText, marginText;
	private JRadioButton low,high;
	private final int APERIO = 0, OPENSLIDE =1, HAMAMATSU = 2, ZEISS = 3, XIPPIX = 4, SIMPLE = 5; 
	private final String[] viewerTypes = new String[] { "aperio","openslide","hamamatsu","zeiss","xippix","simple"};
	private final String[] zoomPolicies = new String[] { "none", "discrete", "pathology" };
	private Properties prefs;
	private JDialog playDialog;
	private File lastPlayFile;
	private ImageSelectionPanel imageSelectionPanel;
	
	
	/**
	 * new slide server
	 * @param server
	 */
	
	public SlideViewer(String server) {
		prefs = new Properties();
		
		// is it a file 
		File f = new File(server);
		if(f.exists()){
			FileInputStream in;
			try {
				in = new FileInputStream(f);
				prefs.load(in);
				in.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
		// load properties from server
		try{
			URL url = (server.endsWith(".conf"))?new URL(server):new URL(server+"/SlideViewer.conf");
			prefs.load(url.openStream());
		}catch(Exception ex){
			ex.printStackTrace();
		}
		// create gui
		createFrame().setVisible(true);
	}

	private ViewerHelper.SnapshotChooserPanel getChooserPanel() {
		if (chooserPanel == null) {
			chooserPanel = new ViewerHelper.SnapshotChooserPanel();
		}
		return chooserPanel;
	}

	private JFrame createFrame() {
		// build GUI
		frame = new JFrame("Viewer (" + prefs.getProperty("image.server.type") + ")");
		frame.addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				viewer.getViewerController().resetZoom();
			}
		});

		// get viewer instance
		ViewerFactory.setProperties(prefs);
		viewer = ViewerFactory.getViewerInstance(prefs.getProperty("image.server.type"));
		/*
		 * try{ viewer.setImage(imageDir+name); }catch(ViewerException ex){
		 * ex.printStackTrace(); }
		 */
		viewer.addPropertyChangeListener(this);

		// add something to viewer window
		imageName = new ViewerHelper.ContrastLabel("SlideViewer");
		//imageName = new JLabel("SlideViewer");
		Dimension d = imageName.getPreferredSize();
		imageName.setBounds(new Rectangle(10, 10, d.width, d.height));
		viewer.getAnnotationPanel().addComponent(imageName);

		// create list of shapes
		list = new JList(new DefaultListModel());
		list.setCellRenderer(new ViewerHelper.ShapeRenderer());
		list.setVisibleRowCount(6);
		list.addListSelectionListener(new ListSelectionListener() {
			private Annotation prev;

			public void valueChanged(ListSelectionEvent e) {
				Object obj = list.getSelectedValue();
				if (obj instanceof Annotation) {
					if (prev != null)
						prev.setSelected(false);
					Annotation tm = (Annotation) obj;
					tm.setSelected(true);
					// viewer.setViewPosition(tm.getViewPosition());

					int x = (int) tm.getBounds().getCenterX();
					int y = (int) tm.getBounds().getCenterY();
					ViewPosition p = new ViewPosition(x, y, tm.getViewPosition().scale);
					viewer.setCenterPosition(p);

					//System.out.println("Annotation: " + tm);
					prev = tm;
				}
			}
		});
		list.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON3){
					Object value = list.getSelectedValue();
					if(value != null && value instanceof Annotation){
						if(popup == null)
							popup = createPopup();
						Point p = e.getPoint();
						popup.show(list,p.x,p.y);
					}
					
				}else if (e.getClickCount() == 2) {
					Object value = list.getSelectedValue();
					if(value != null && value instanceof Annotation){
						pickNameAndColor(list,(Annotation) value);
					}
				}
			}
		});
		//JScrollPane scroll = new JScrollPane(list);
		//scroll.setPreferredSize(new Dimension(200, 300));


		statusPanel = new JToolBar();
		
		// create status fields
		statusLocation = new JLabel("no image");
		statusLocation.setFont(statusLocation.getFont().deriveFont(Font.PLAIN));
		// statusLocation.setMinimumSize(load.getPreferredSize());
		statusPanel.add(statusLocation);
		statusPanel.add(Box.createHorizontalGlue());
		
		// create status operation
		statusOperation = new JLabel("normal mode");
		statusPanel.add(statusOperation);
		statusPanel.addSeparator();
		
		// add content
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(createToolBar(), BorderLayout.NORTH);
		frame.getContentPane().add(viewer.getViewerPanel(), BorderLayout.CENTER);
		frame.getContentPane().add(statusPanel, BorderLayout.SOUTH);
		//frame.getContentPane().add(scroll, BorderLayout.EAST);
		frame.pack();
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		// activate thumbnail view
		// viewer.setThumbnail(true);

		return frame;
	}

	/**
	 * load different project
	 */	
	private void loadImage(){
		String name = null;
		
		// prompt server for images
		if(viewer instanceof SimpleViewer){
			JFileChooser chooser = new JFileChooser();
			if(JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(frame)){
				File f = chooser.getSelectedFile();
				name = f.getAbsolutePath();
			}
		}
		if(name == null){
			// try to get the list of images
			// if successfull, then give a choice
			/*
			if(imagesOnServer == null){
				imagesOnServer = ViewerFactory.getImageList();
			}
			
			if(imagesOnServer != null){
				if(JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
					frame,getSlideSelectionPanel(),
				   	"Open Slide Image",
					JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)){
					name = ""+input.getSelectedValue();
				}
			}else{
				name = JOptionPane.showInputDialog(frame, "Enter image name");
			}
			*/
			if(imageSelectionPanel == null)
				imageSelectionPanel = new ImageSelectionPanel();
			if(imageSelectionPanel.showDialog(frame)){
				name = imageSelectionPanel.getSelectedImage();
			}
		}	
		if (name != null && name.length() > 0) {
			// remove stale tutor markers
			((DefaultListModel) list.getModel()).removeAllElements();
			// viewer.reset();
			viewer.getAnnotationPanel().getAnnotationManager().removeAnnotations();

			// reset image
			try {
				viewer.openImage(getImageDirectory() + name);
			} catch (ViewerException ex) {
				//ex.printStackTrace();
				//check if we need to switch viewer based on image type
				String type = prefs.getProperty("image.server.type");
				String rtype = ViewerFactory.recomendViewerType(name);
				if(rtype == null){
					JOptionPane.showMessageDialog(frame,name+" is not of supported type","Error",JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				// warn
				JOptionPane.showMessageDialog(frame,"Can't open image "+name+" with "+type+
						" viewer.\nWill try to use "+rtype+" viewer", "Error", JOptionPane.WARNING_MESSAGE);
				
				if (!type.equals(rtype))
					switchViewer(rtype);
				
				try{
					viewer.openImage(getImageDirectory() + name);
				}catch(ViewerException ex1){
					ex1.printStackTrace();
					JOptionPane.showMessageDialog(frame, "Could not open image "+name+" with any viewer!", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
			
			// reset label
			int i = name.lastIndexOf('/');
			if(i < 0)
				i = name.lastIndexOf('\\');
			if(i > 0)
				name = name.substring(i+1);
			
			imageName.setText(name);
			Dimension d = imageName.getPreferredSize();
			imageName.setBounds(new Rectangle(10, 10, d.width, d.height));
			//System.out.println("Image MetaData: " + viewer.getImageMetaData());
		}
    }
	
	
	/**
	 * Get selection panel for new Project
	 * @return
	 *
	protected JPanel getSlideSelectionPanel(){
		if(slideSelectionPanel == null){
			input = new JList();
			slideSelectionPanel = ViewerHelper.createDynamicSelectionPanel(input,imagesOnServer);
		}
		return slideSelectionPanel;
	}
	*/
	
	/**
	 * Create popup menu
	 * @return
	 */
	private JPopupMenu createPopup(){
		JPopupMenu menu = new JPopupMenu();
		//edit
		ImageIcon icon = new ImageIcon(getClass().getResource(iconPath+"Edit16"+iconExt));
		JMenuItem edit = new JMenuItem("Edit",icon);
		edit.addActionListener(this);
		edit.setActionCommand("edit_marker");
		menu.add(edit);
		// remove
		icon = new ImageIcon(getClass().getResource(iconPath+"Delete16"+iconExt));
		JMenuItem rem = new JMenuItem("Delete",icon);
		rem.addActionListener(this);
		rem.setActionCommand("delete_marker");
		menu.add(rem);
		
		return menu;
	}
	
	
	/**
	 * prompt user for name and color
	 * @param marker
	 */
	private void pickNameAndColor(Component c, Annotation marker){
		final Component cmp = c;
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		
		final JTextField name = new JTextField(15);
		name.setText(marker.getName());
		panel.add(name,BorderLayout.CENTER);
		
		final JButton pick = new JButton("Color");
		Color clr = marker.getColor();
		if(clr.getTransparency() != Transparency.OPAQUE)
			clr = new Color(clr.getRed(),clr.getGreen(),clr.getBlue());
		pick.setBackground(clr);
		panel.add(pick,BorderLayout.EAST);
		pick.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				Color clr = JColorChooser.showDialog(cmp,"Annotation Color",pick.getBackground());
				pick.setBackground(clr);
			}
		});
		
		// display dialog
		int r = JOptionPane.showConfirmDialog(c,panel,"Modify Annotation",
					JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if(r == JOptionPane.OK_OPTION){
			marker.setName(name.getText());
			marker.setColor(pick.getBackground());
			viewer.repaint();
		}
	}
	
	
	private JToolBar createToolBar() {
		JToolBar toolbar = new JToolBar(JToolBar.HORIZONTAL);

		// create ruler cursor
		// rulerCursor = createRulerCursor();

		// add load/set buttons to toolbar
		close = new JButton(new ImageIcon(getClass().getResource(iconPath + "New24" + iconExt)));
		close.setToolTipText("Close Virtual Slide");
		close.addActionListener(this);
		//toolbar.add(close);
		
		
		load = new JButton(new ImageIcon(getClass().getResource(iconPath + "Open" + iconExt)));
		load.setToolTipText("Load Virtual Slide");
		load.addActionListener(this);
		toolbar.add(load);
		
		
		config = new JButton(new ImageIcon(getClass().getResource(iconPath + "Preferences24" + iconExt)));
		config.setToolTipText("Configure SlideViewer");
		config.addActionListener(this);
		toolbar.add(config);
		toolbar.addSeparator();

		screen = new JButton(new ImageIcon(getClass().getResource(iconPath + "Camera24" + iconExt)));
		screen.setToolTipText("Capture slide snapshot");
		screen.addActionListener(this);
		screen.setPreferredSize(load.getPreferredSize());
		toolbar.add(screen);

	
		record = new JToggleButton(new ImageIcon(getClass().getResource(iconPath + "Record24" + iconExt)));
		record.setToolTipText("Record (save) viewer session");
		record.addActionListener(this);
		record.setPreferredSize(load.getPreferredSize());
		toolbar.add(record);
		
		play = new JToggleButton(new ImageIcon(getClass().getResource(iconPath + "Play24" + iconExt)));
		play.setToolTipText("Play (saved) viewer session");
		play.addActionListener(this);
		play.setPreferredSize(load.getPreferredSize());
		toolbar.add(play);
		

		// create status fields
		// statusName = new JLabel("");
		// statusName.setEditable(false);
		// add(statusName);
		viewShapes = new JToggleButton(new ImageIcon(getClass().getResource(iconPath+"History24"+iconExt)));
		viewShapes.addActionListener(this);
		viewShapes.setToolTipText("Display Annotations");
		toolbar.add(viewShapes);
				
		
		// add drawing buttons to toolbar
		JToolBar toolbar2 = new JToolBar(JToolBar.HORIZONTAL);
		toolbar.addSeparator();
		toolbar2.add(createButton("Rectangle", "Draw Rectangle", "Rect"));
		toolbar2.add(createButton("Arrow", "Draw Arrow", "Arrow"));
		toolbar2.add(createButton("Free Hand", "Free Hand Drawing", "Polygon"));
		toolbar2.add(createButton("Measure", "Measure with Micrometer", "Ruler"));
		toolbar2.add(createButton("Cross", "Put a marker", "Cross"));
		toolbar2.add(createButton("Circle", "Draw an circle", "Circle"));
		toolbar2.add(createButton("Text", "Draw a Text Label", "Text"));
		//toolbar2.add(createButton("Region", "Draw rectangular region", "Region"));
		toolbar.add(toolbar2);
		toolbar.addSeparator();

		
		return toolbar;
	}

	/**
	 * Create button for toolbar
	 */
	private JToggleButton createButton(String text, String tiptext, String action) {
		JToggleButton tb = new JToggleButton();
		tb.setIcon(new ImageIcon(getClass().getResource(iconPath + action + iconExt)));
		tb.setToolTipText(tiptext);
		tb.setActionCommand(action);
		tb.addActionListener(this);
		// tb.setText(text);
		// tb.addItemListener(this);
		tb.setRolloverEnabled(true);
		return tb;
	}

	public void actionPerformed(ActionEvent e) {
		String comm = e.getActionCommand();
		if (e.getSource() instanceof JToggleButton && comm.length() > 0) {
			JToggleButton tb = (JToggleButton) e.getSource();
			
			if(!viewer.hasImage()){
				tb.setSelected(false);
				return;
			}
			
			if (cur_selection != null && cur_selection != tb)
				stopSketch();

			cur_selection = tb;
			if (cur_selection.isSelected()) {
				//String comm = cur_selection.getActionCommand();
				if (comm.equalsIgnoreCase("Rect")){
					//sketchShape(AnnotationManager.SQUARE_SHAPE, Color.green, Cursor
					//		.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
					sketchShape(AnnotationManager.PARALLELOGRAM_SHAPE, Color.green, null);
				}else if (comm.equalsIgnoreCase("Arrow")){
					sketchShape(AnnotationManager.ARROW_SHAPE, Color.green, null);
				}else if (comm.equalsIgnoreCase("Polygon")){
					sketchShape(AnnotationManager.POLYGON_SHAPE, Color.blue, null);
				}else if (comm.equalsIgnoreCase("Ruler")){
					// sketchRulerShape();
					sketchShape(AnnotationManager.RULER_SHAPE, Color.orange, null);
				}else if (comm.equalsIgnoreCase("Cross")){
					// sketchRulerShape();
					sketchShape(AnnotationManager.CROSS_SHAPE, Color.orange, null);
				}else if (comm.equalsIgnoreCase("Circle")){
					// sketchRulerShape();
					sketchShape(AnnotationManager.CIRCLE_SHAPE, Color.green, null);
				}else if (comm.equalsIgnoreCase("Region")){
					// sketchRulerShape();
					sketchShape(AnnotationManager.REGION_SHAPE, Color.green, null);
				}else if (comm.equalsIgnoreCase("Text")){
					// sketchRulerShape();
					sketchShape(AnnotationManager.TEXT_SHAPE, Color.white, null);
				}
			} else {
				stopSketch();
			}
		} else if (e.getSource() == load) {
			(new Thread(new Runnable(){
				public void run(){
					loadImage();
				}
			})).start();
		} else if (e.getSource() == close) {
			viewer.closeImage();
		} else if (e.getSource() == screen) {
			try {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileFilter(new ViewerHelper.JpegFileFilter());
				chooser.setAccessory(getChooserPanel());
				chooser.setPreferredSize(new Dimension(550, 350));
				int returnVal = chooser.showSaveDialog(frame);
				
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					// select mode
					int mode = getChooserPanel().getSelectedMode();
					try {
						ViewerHelper.writeJpegImage(viewer.getSnapshot(mode), chooser.getSelectedFile());
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			} catch (java.security.AccessControlException ex) {
				JOptionPane.showMessageDialog(frame, "You do not have permission to save screenshots on local disk.",
						"Error", JOptionPane.ERROR_MESSAGE);

			}
		} else if (e.getSource() == config) {
			boolean oldLow = (low != null)?low.isSelected():true;
			int r = JOptionPane.showConfirmDialog(frame, getConfigPanel(), "Preferences", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE);
			if (r == JOptionPane.OK_OPTION) {
				String type = prefs.getProperty("image.server.type");
				updatePreferences();
				ViewerFactory.setProperties(prefs);
				if (!prefs.getProperty("image.server.type").equals(type)) {
					viewer.closeImage();
					switchViewer(prefs.getProperty("image.server.type"));
				} else {
					try {
						viewer.setServer(new URL(serverText.getText()));
					} catch (MalformedURLException ex) {
						JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					}
				}

				// change zoom policy
				updateZoomPolicy("" + policyChooser.getSelectedItem());
				
				if(viewer instanceof QuickViewer){
					try{
						((QuickViewer)viewer).setMarginSize(Integer.parseInt(marginText.getText()));
					}catch(NumberFormatException ex){}
				}
				// if change in selection
				if(low != null && oldLow != low.isSelected()){
					if(low.isSelected()){
				        ViewerFactory.getProperties().setProperty("qview.tile.request.short.timeout","2000");
				        ViewerFactory.getProperties().setProperty("qview.tile.request.long.timeout","5000");
				        ViewerFactory.getProperties().setProperty("qview.concurrent.tile.requests","1");
					}else if(high.isSelected()){
				        ViewerFactory.getProperties().setProperty("qview.tile.request.short.timeout","3000");
				        ViewerFactory.getProperties().setProperty("qview.tile.request.long.timeout","6000");
				        ViewerFactory.getProperties().setProperty("qview.concurrent.tile.requests","10");
					}
					switchViewer(prefs.getProperty("image.server.type"));
				}

			}

		} else if (e.getSource() == viewShapes ){
			if(shapesDialog == null){
				JOptionPane op = new JOptionPane(new JScrollPane(list),JOptionPane.PLAIN_MESSAGE);
				shapesDialog = op.createDialog(frame,"Annotations");
				shapesDialog.setModal(false);
				shapesDialog.addWindowListener(new WindowAdapter(){
					public void windowDeactivated(WindowEvent e){
						viewShapes.setSelected(false);
					}
				});
			}
			shapesDialog.setVisible(viewShapes.isSelected());
		} else if (e.getSource() == record ){
			if(record.isSelected()){
				try {
					String name = (viewer.hasImage())?viewer.getImage():"unknown";
					// strip prefix
					String imageDir = getImageDirectory();
					if(imageDir.length() > 0 && name.startsWith(imageDir))
						name = name.substring(imageDir.length());
					// strip suffix
					int si = name.lastIndexOf(".");
					if(si > -1)
						name = name.substring(0,si);
					JFileChooser chooser = new JFileChooser();
					chooser.setPreferredSize(new Dimension(550, 350));
					chooser.setSelectedFile(new File("view_"+name+".txt"));
					int returnVal = chooser.showSaveDialog(frame);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						if(recorder == null)
							recorder = new Recorder();
						try{
							recorder.setFile(chooser.getSelectedFile());
							viewer.addPropertyChangeListener(recorder);
							recorder.setRecord(true);
							if(viewer.hasImage())
								recorder.recordImageChange(viewer.getImage());
							recorder.recordViewResize(viewer.getSize());
							recorder.recordViewObserve(viewer.getViewPosition());
							// change status field
							statusOperation.setForeground(Color.red);
							statusOperation.setText("recording ...");
						}catch(IOException ex){
							JOptionPane.showMessageDialog(frame,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
						}
					}else{
						record.setSelected(false);
					}
				} catch (java.security.AccessControlException ex) {
					JOptionPane.showMessageDialog(frame, "You do not have permission to save screenshots on local disk.",
							"Error", JOptionPane.ERROR_MESSAGE);
				}
			}else{
				if(recorder != null){
					recorder.setRecord(false);
					viewer.removePropertyChangeListener(recorder);
					JOptionPane.showMessageDialog(frame,"Recording Done!");
					statusOperation.setForeground(Color.black);
					statusOperation.setText("normal mode");
				}
			}
		}else if (e.getSource() == play ){
			if(play.isSelected()){
				try {
					JFileChooser chooser = new JFileChooser();
					chooser.setSelectedFile(lastPlayFile);
					chooser.setPreferredSize(new Dimension(550, 350));
					int returnVal = chooser.showOpenDialog(frame);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						if(player == null){
							player = new Player(viewer);
							player.addPropertyChangeListener(this);
						}
						try{
							lastPlayFile = chooser.getSelectedFile();
							player.setFile(lastPlayFile);
						}catch(IOException ex){
							JOptionPane.showMessageDialog(frame,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
						}
						JOptionPane op = new JOptionPane(player.getControlPanel(),JOptionPane.PLAIN_MESSAGE);
						playDialog = op.createDialog(frame,"Playback Controls");
						playDialog.setModal(false);
						playDialog.setVisible(true);
						player.play();
						statusOperation.setForeground(Color.green);
						statusOperation.setText("playing ...");
					}else{
						play.setSelected(false);
					}
				} catch (java.security.AccessControlException ex) {
					JOptionPane.showMessageDialog(frame, "You do not have permission to save screenshots on local disk.",
							"Error", JOptionPane.ERROR_MESSAGE);
				}
			}else{
				if(player != null){
					player.stop();
					statusOperation.setForeground(Color.black);
					statusOperation.setText("normal mode");
				}
			}
		}else if(comm.equals("edit_marker")){
			Object value = list.getSelectedValue();
			if(value != null && value instanceof Annotation){
				pickNameAndColor(list,(Annotation) value);
			}
		}else if(comm.equals("delete_marker")){
			Object value = list.getSelectedValue();
			if(value != null && value instanceof Annotation){
				viewer.getAnnotationManager().removeAnnotation((Annotation)value);
				((DefaultListModel)list.getModel()).removeElement(value);
			}
		}
	}

	private void updateZoomPolicy(String policy) {
		// descrete
		if (policy.equals(zoomPolicies[1])) {
			viewer.setScalePolicy(new DiscreteScalePolicy());
			// pathology
		} else if (policy.equals(zoomPolicies[2])) {
			viewer.setScalePolicy(new PathScalePolicy());
		}
	}

	private void updatePreferences() {
		if (configPanel != null) {
			prefs.setProperty("image.server.type", "" + viewerType.getSelectedItem());

			if (viewerType.getSelectedItem().equals(viewerTypes[XIPPIX])) {
				// xippix
				prefs.setProperty("xippix.server.url", serverText.getText());
				prefs.setProperty("xippix.image.dir", prefixText.getText());
			} 
			/*
			else if (viewerType.getSelectedItem().equals(viewerTypes[MSCOPE])) {
				// mscope
				prefs.setProperty("mscope.server.url", serverText.getText());
				prefs.setProperty("mscope.servlet.url", servletText.getText());
				prefs.setProperty("mscope.image.dir", prefixText.getText());
			} 
			*/
			else if (viewerType.getSelectedItem().equals(viewerTypes[APERIO])) {
				// qview
				prefs.setProperty("aperio.server.url", serverText.getText());
				prefs.setProperty("aperio.image.dir", prefixText.getText());
			} else if (viewerType.getSelectedItem().equals(viewerTypes[OPENSLIDE])) {
				// qview
				prefs.setProperty("openslide.server.url", serverText.getText());
				prefs.setProperty("openslide.image.dir", prefixText.getText());
			}else if (viewerType.getSelectedItem().equals(viewerTypes[HAMAMATSU])) {
				// qview
				prefs.setProperty("hamamatsu.server.url", serverText.getText());
				prefs.setProperty("hamamatsu.image.dir", prefixText.getText());
			}else if (viewerType.getSelectedItem().equals(viewerTypes[ZEISS])) {
				// qview
				prefs.setProperty("zeiss.server.url", serverText.getText());
				prefs.setProperty("zeiss.image.dir", prefixText.getText());
			}
		}
	}

	/**
	 * Replace a current viewer w/ a viewer of different type
	 * 
	 * @param type
	 */
	private void switchViewer(String type) {
		// create virtual microscope panel
		Viewer v = ViewerFactory.getViewerInstance(type);
		// int height
		// =frame.getSize().height-v.getViewerControlPanel().getPreferredSize().height;
		// v.setSize(frame.getSize().width,height);

		// remove component
		frame.getContentPane().remove(viewer.getViewerPanel());
		setViewer(v);

		// replace component
		frame.getContentPane().add(v.getViewerPanel(), BorderLayout.CENTER);
		frame.setTitle("Viewer (" + type + ")");
		frame.getContentPane().validate();
		viewer.repaint();

		// add something to viewer window
		imageName = new ViewerHelper.ContrastLabel("SlideViewer");
		//imageName = new JLabel("SlideViewer");
		Dimension d = imageName.getPreferredSize();
		imageName.setBounds(new Rectangle(10, 10, d.width, d.height));
		viewer.getAnnotationPanel().addComponent(imageName);
		
		//imageDir = prefs.getProperty(type+".image.dir","");
	}

	private String getImageDirectory(String type){
		return  prefs.getProperty(type+".image.dir","");
	}
	private String getImageDirectory(){
		return getImageDirectory(prefs.getProperty("image.server.type"));
	}
	
	
	/**
	 * Sets the viewer as main tutor viewer. Registers all appropriate
	 * listeners
	 * 
	 * @param Viewer
	 *                viewer
	 */
	private void setViewer(Viewer v) {
		// remove previous viewer if exists
		if (viewer != null) {
			if(recorder != null && recorder.isRecording()){
				viewer.removePropertyChangeListener(recorder);
				recorder.setRecord(false);
				recorder.dispose();
				recorder = null;
			}
			if(player != null){
				player.removePropertyChangeListener(this);
				player.dispose();
				player = null;
			}
			((DefaultListModel) list.getModel()).removeAllElements();
			viewer.removePropertyChangeListener(this);
			viewer.dispose();
			viewer = null;
			System.gc();
		}
		viewer = v;
		viewer.addPropertyChangeListener(this);
	}

	/**
	 * Annotation params will be updated in the TutorImageView during the
	 * sketching and then it should be added to the AnnotationManager
	 * 
	 * @param shape
	 * @param c
	 * @param cur
	 * @param tb
	 */
	public Annotation sketchShape(int shape, Color c, Cursor cur) {
		AnnotationPanel aPanel = viewer.getAnnotationPanel();
		AnnotationManager mm = aPanel.getAnnotationManager();
		Annotation tm = mm.createAnnotation(shape, true);
		tm.setEditable(true);
		tm.setTagVisible(true);
		mm.addAnnotation(tm);
		tm.addPropertyChangeListener(this);
		if(recorder != null)
			tm.addPropertyChangeListener(recorder);
		// tm.setName(tm.getType()+(shapeCount++));
		aPanel.sketchAnnotation(tm);

		// set property of the ruler
		if(tm instanceof RulerShape)
			((RulerShape)tm).setShowTotalMeasurement(true);
		
		return tm;
	}

	/**
	 * stop sketching shape This method is called by AnnotationViewPanel
	 */
	public void stopSketch() {
		AnnotationPanel aPanel = viewer.getAnnotationPanel();
		if (cur_selection != null) {
			cur_selection.setSelected(false);
			cur_selection = null;
			stopSketch();
		}
		aPanel.sketchDone();
		// author.getView().setCursor(avPanel_.getDefCursor());
	}

	// drawing is done, event is sent
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("ViewChange")) {
			ViewPosition l = (ViewPosition) evt.getNewValue();
			//statusLocation.setText("X=" + l.x + "  Y=" + l.y + "  Scale="
			//    + Math.round(l.scale * 100) / 100.0);
			statusLocation.setText(viewer.getImage()+": x="+l.x + " y=" + l.y+" scale="+Math.round(l.scale * 100) / 100.0);
			//+ Math.round(l.scale * 100) / 100.0 + ")");
		} else if (evt.getPropertyName().equals("sketchDone")) {
			Annotation marker = (Annotation) evt.getNewValue();
			stopSketch();
			((DefaultListModel) list.getModel()).addElement(marker);
		} else if (evt.getPropertyName().equals("UpdateShape")) {
			// Annotation marker = (Annotation) evt.getNewValue();
			// System.out.println(marker.getName()+" "+marker);
		} else if( evt.getPropertyName().equals("Playback")){
			if(play.isSelected()){
				play.setSelected(false);
				playDialog.dispose();
				playDialog = null;
				
				//JOptionPane.showMessageDialog(frame,"Playback Done!");
				statusOperation.setForeground(Color.black);
				statusOperation.setText("normal mode");
			}
		} else if( evt.getPropertyName().equals("PlaybackImage")){
			String name = ""+evt.getNewValue();
			imageName.setText(name);
			Dimension d = imageName.getPreferredSize();
			imageName.setBounds(new Rectangle(10, 10, d.width, d.height));
		}
	}

	/**
	 * Get config panel
	 * 
	 * @return
	 */
	public JPanel getConfigPanel() {
		if (configPanel == null)
			configPanel = createConfigPanel();
		return configPanel;
	}

	/**
	 * Create configuartion panel
	 * 
	 * @return
	 */
	private JPanel createConfigPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JPanel p = new JPanel();
		p.setBorder(new CompoundBorder(new EmptyBorder(new Insets(0,0,10,0)),new TitledBorder("Server Settings")));
		p.setLayout(new GridLayout(3, 2));
		p.add(new JLabel("Viewer Type"));
		viewerType = new JComboBox(viewerTypes);
		p.add(viewerType);
		p.add(new JLabel("Server URL"));
		serverText = new JTextField(15);
		p.add(serverText);
		//p.add(new JLabel("Navigator Servlet URL"));
		//p.add(servletText);
		p.add(new JLabel("Image Directory"));
		prefixText = new JTextField(15);
		p.add(prefixText);
		panel.add(p);

		p = new JPanel();
		p.setBorder(new CompoundBorder(new EmptyBorder(new Insets(0,0,10,0)),new TitledBorder("Client Settings")));
		p.setLayout(new GridLayout(2, 2));
		p.add(new JLabel("Zoom Policy"));
		policyChooser = new JComboBox(zoomPolicies);
		p.add(policyChooser);
		p.add(new JLabel("Slide Margin"));
		marginText  =  new JTextField();
		marginText.setToolTipText("Size of slide margin measured in pixels");
		p.add(marginText);
		panel.add(p);
		
		
		p = new JPanel();
		p.setBorder(new CompoundBorder(new EmptyBorder(new Insets(0,0,10,0)),new TitledBorder("Latency Settings")));
		p.setLayout(new GridLayout(0, 1));
		
		low = new JRadioButton("Low Network Latency",true);
		low.setToolTipText("There is a sufficient network bandwidth and the imaging server is geographically not too far away");
		
		high = new JRadioButton("High Network Latency");
		high.setToolTipText("The network bandwith is low and/or imaging server is geographically far away");
		
		ButtonGroup g = new ButtonGroup();
		g.add(low);
		g.add(high);
		
		p.add(low);
		p.add(high);
		panel.add(p);
		

		// setup listeners
		viewerType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String sel = "" + viewerType.getSelectedItem();
				// xippix
				if (sel.equals(viewerTypes[XIPPIX])) {
					serverText.setText(prefs.getProperty("xippix.server.url"));
					prefixText.setText(prefs.getProperty("xippix.image.dir"));
					serverText.setEditable(true);
					prefixText.setEditable(true);
					// mscope
				} 
				/*
				else if (sel.equals(viewerTypes[1])) {
					serverText.setText(prefs.getProperty("mscope.server.url"));
					servletText.setText(prefs.getProperty("mscope.servlet.url"));
					prefixText.setText(prefs.getProperty("mscope.image.dir"));
					serverText.setEditable(true);
					servletText.setEditable(true);
					prefixText.setEditable(true);
					// qview
				} 
				*/
				else if (sel.equals(viewerTypes[APERIO])) {
					serverText.setText(prefs.getProperty("aperio.server.url"));
					prefixText.setText(prefs.getProperty("aperio.image.dir"));
					serverText.setEditable(true);
					prefixText.setEditable(true);
					if(viewer instanceof QuickViewer)
						marginText.setText(""+((QuickViewer)viewer).getMarginSize());
					//openslide
				} else if (sel.equals(viewerTypes[OPENSLIDE])) {
					serverText.setText(prefs.getProperty("openslide.server.url"));
					prefixText.setText(prefs.getProperty("openslide.image.dir"));
					serverText.setEditable(true);
					prefixText.setEditable(true);
					if(viewer instanceof QuickViewer)
						marginText.setText(""+((QuickViewer)viewer).getMarginSize());
				} else if (sel.equals(viewerTypes[HAMAMATSU])) {
					serverText.setText(prefs.getProperty("hamamatsu.server.url"));
					prefixText.setText(prefs.getProperty("hamamatsu.image.dir"));
					serverText.setEditable(true);
					prefixText.setEditable(true);
					if(viewer instanceof QuickViewer)
						marginText.setText(""+((QuickViewer)viewer).getMarginSize());
				} else if (sel.equals(viewerTypes[ZEISS])) {
					serverText.setText(prefs.getProperty("zeiss.server.url"));
					prefixText.setText(prefs.getProperty("zeiss.image.dir"));
					serverText.setEditable(true);
					prefixText.setEditable(true);
					if(viewer instanceof QuickViewer)
						marginText.setText(""+((QuickViewer)viewer).getMarginSize());
					// simple
				} else {
					serverText.setText(prefs.getProperty("openslide.server.url"));
					prefixText.setText("");
					serverText.setEditable(false);
					prefixText.setEditable(false);
				}
			}
		});

		// set defaults
		String vtype = prefs.getProperty("image.server.type","aperio");
		String ttype = null;
		for(int i=0;i<viewerTypes.length;i++){
			ttype = viewerTypes[i];
			if(ttype.equals(vtype))
				break;
		}
		viewerType.setSelectedItem(vtype);
		serverText.setText(prefs.getProperty(vtype+".server.url"));
		prefixText.setText(prefs.getProperty(vtype+".image.dir"));
		// servletText.setText(prefs.getProperty("mscope.servlet.url"));
		
		return panel;
	}
	
	
	// invoke SlideViewerApp
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: SlideViewer <server url>");
			return;
		}
		new SlideViewer(args[0]);
	}

	public void stateChanged(ChangeEvent e) {
		
	}
}
