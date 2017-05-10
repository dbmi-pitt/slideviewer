import java.util.*;
import java.net.*;
import javax.swing.*;
import edu.pitt.slideviewer.*;
import edu.pitt.slideviewer.markers.Annotation;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class ViewerLiveMap {
	private final String OPEN_ICON = "/icons/Open.gif";
	private Properties prefs;
	private String imageDir;
	private JLabel imageName;
	private JFrame frame;
	private Viewer viewer;
	
	//std constractor
	public ViewerLiveMap(String server){
		prefs = new Properties();
		boolean loaded = false;
		
		// load properties from server
		try{
			URL url = new URL(server+"/SlideViewer.conf");
			prefs.load(url.openStream());
			loaded = true;
		}catch(Exception ex){}
		
		// prop not loaded, then load hard-coded defaults
		if(!loaded){
			String host = server;
			try {
				URL url = new URL(server);
				host = "http://" + url.getHost();
			} catch (MalformedURLException ex) {}
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
		imageDir = prefs.getProperty(prefs.getProperty("image.server.type","qview")+ ".image.dir");

		// create gui
		createFrame().setVisible(true);
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
	

		// add something to viewer window
		imageName = new ViewerHelper.ContrastLabel("SlideViewer");
		//imageName = new JLabel("SlideViewer");
		Dimension d = imageName.getPreferredSize();
		imageName.setBounds(new Rectangle(10, 10, d.width, d.height));
		viewer.getAnnotationPanel().addComponent(imageName);
	
		// add content
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(createToolBar(), BorderLayout.NORTH);
		frame.getContentPane().add(viewer.getViewerPanel(), BorderLayout.CENTER);
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
	// make toolbar
	private JToolBar createToolBar() {
		JToolBar toolbar = new JToolBar(JToolBar.HORIZONTAL);
		// add load/set buttons to toolbar
		JButton load = new JButton("Load History File",new ImageIcon(getClass().getResource(OPEN_ICON)));
		load.setToolTipText("Load history file");
		load.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				loadFile();
			}
		});
		
		toolbar.add(load);
		return toolbar;
	}
	
	// load file
	private void loadFile(){
		JFileChooser chooser = new JFileChooser();
		int result = chooser.showOpenDialog(frame);
		if(result == JFileChooser.APPROVE_OPTION){
			doCreateMap(chooser.getSelectedFile().getAbsolutePath());
		}
	}
	
	/**
	 * Create maps
	 */
	public void doCreateMap(String file){
		try{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			createMap(reader);
			reader.close();
		}catch(Exception ex){
			ex.printStackTrace();
			JOptionPane.showMessageDialog(frame,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * Given reader that has pointer to file orig image size and source 
	 * thumbnail image build a map
	 * @param reader
	 * @param isize
	 * @param thumbnail
	 */
	public void createMap(BufferedReader reader) throws Exception{
		AnnotationManager mm = viewer.getAnnotationManager();
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
				if( p.equalsIgnoreCase(Constants.OBSERVE_TAG)){
					int x = Integer.parseInt(entries[1]);
					int y = Integer.parseInt(entries[2]);
					double s = Double.parseDouble(entries[3]);
					// add rectangle to overlay
					int w = (int)(viewSize.getWidth()/s);
					int h = (int)(viewSize.getHeight()/s);
					Rectangle r = new Rectangle(x,y,w,h);
					// figure out color
					Color c = Color.green;
					if(s > .03){
						Annotation tm = mm.createAnnotation(AnnotationManager.REGION_SHAPE,r,c,false);
						mm.addAnnotation(tm);
					}
				}else if(p.equalsIgnoreCase(Constants.RESIZE_TAG)){
					int w = Integer.parseInt(entries[1]);
					int h = Integer.parseInt(entries[2]);
					viewSize.setSize(w,h);
				}else if(p.equalsIgnoreCase(Constants.IMAGE_TAG)){
					String imageName = entries[1].trim();
					
					// load image to get its screenshot and thumbnail
					viewer.openImage(imageDir+imageName);
					//viewer.getViewerController().resetZoom();
					viewSize.setSize(frame.getSize());
					
				}
			}
		}	
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: ViewerLiveMap <server url>");
			return;
		}
		new ViewerLiveMap(args[0]);
	}

}
