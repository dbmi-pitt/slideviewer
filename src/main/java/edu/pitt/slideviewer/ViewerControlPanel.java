package edu.pitt.slideviewer;

import javax.swing.border.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.*;

import edu.pitt.slideviewer.qview.QuickViewer;
import edu.pitt.slideviewer.qview.connection.Tile;
import edu.pitt.slideviewer.qview.connection.TileConnectionManager;
import edu.pitt.slideviewer.qview.connection.TileManager;
import edu.pitt.slideviewer.qview.connection.Utils;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class is a simple control panel for any viewer
 * Author: Eugene Tseytlin (University of Pittsburgh)
 */
public class ViewerControlPanel extends JToolBar implements 
	ActionListener, KeyListener,MouseListener,PropertyChangeListener{
	private final String zoomInIcon    = "/icons/ZoomIn";
	private final String zoomOutIcon   = "/icons/ZoomOut";
	private final String resetIcon     = "/icons/Refresh";
	private final String navigatorIcon = "/icons/Navigator";
	private final String magnifierIcon = "/icons/Zoom";
	private final String transformIcon = "/icons/Transform";
	private final String channelIcon = "/icons/Color";
	private final String gif = ".gif";
	private int size = 24; //size of icons
	
	private Viewer viewer;
	private JTextField text;
	private JButton zoomIn,zoomOut,reset;
	private JToggleButton navigator,magnifierButton,transforms,channels;
	private Window navigatorDialog,magnifierDialog,channelDialog;
	private Map<AbstractButton,Integer> buttonOffsets;
	private Magnifier magnifier;
	private ImageTransformationPanel transformPanel;
	private ViewPosition lastPosition;
	
	
	/**
	 * Create new control panel
	 * @param Viewer viewer that this panel will control
	 */
	public ViewerControlPanel(Viewer v){
		this(v,false);
	}
	
	/**
	 * Create new control panel
	 * @param Viewer viewer that this panel will control
	 * @param boolean true to make panel a bit smaller
	 */
	public ViewerControlPanel(Viewer v,boolean small){
		super();
		viewer = v;
		viewer.addPropertyChangeListener(this);
		if(small)
			size = 16;
		viewer.getViewerComponent().setFocusable(true);
		viewer.getViewerComponent().addKeyListener(this);
		viewer.getViewerComponent().addMouseListener(this);
		//addKeyListener(this);
        createGUI();
        setFloatable(false);
	}
	
	/**
	 * Call this method to dispose of this object
	 * (This unregisters all listeners)
	 */
	public void dispose(){
		close();
		viewer.removePropertyChangeListener(this);
		viewer.getViewerComponent().removeKeyListener(this);
		viewer.getViewerComponent().removeMouseListener(this);
		viewer = null;
	}
	
	/**
	 * close all open windows (like navigator and magnifierButton)
	 */
	public void close(){
		navigator.setSelected(false);
		magnifierButton.setSelected(false);
		dispose(navigatorDialog);
		navigatorDialog = null;
		dispose(magnifierDialog);
		magnifierDialog = null;
		//showNavigatorWindow(false);
		//if(magnifierButton != null)
		//	magnifierButton.setSelected(false);
	}
	
	/**
	 * dispose of window
	 * @param win
	 */
	private void dispose(Window win){
		if(win == null)
			return;
		MouseListener [] ml = win.getMouseListeners();
		MouseMotionListener [] mml = win.getMouseMotionListeners();
		if(ml != null)
			for(int i=0;i<ml.length;i++)
				win.removeMouseListener(ml[i]);
		if(mml != null)
			for(int i=0;i<mml.length;i++)
				win.removeMouseMotionListener(mml[i]);
		win.dispose();
	}
	
	
	
	/**
	 * Show/hide navigator window
	 * @param b
	 */
	public void showNavigatorWindow(boolean b){
		if(navigator != null && navigator.isEnabled()){
			if(b ^ navigator.isSelected())
				navigator.doClick();
		}
	}
	
	
	/**
	 * Enable/Disable navigator button
	 * when disabled, navigator button is hidden
	 * @param b
	 */
	private void setButtonEnabled(AbstractButton bt, boolean enabled){
		bt.setEnabled(enabled);
		if(enabled){
			// if last component is NOT navigator then add it
			if(!hasComponent(bt)){
				if(getButtonOffsetMap().containsKey(bt))
					add(bt,getButtonOffsetMap().get(bt));
				else
					add(bt);
			}
		}else{
			// if last component IS navigator then remove it
			if(hasComponent(bt)){
				getButtonOffsetMap().put(bt,getComponentIndex(bt));
				remove(bt);
			}
		}
		revalidate();
	}
	
	/**
	 * get map of button offset
	 * @return
	 */
	private Map<AbstractButton,Integer> getButtonOffsetMap(){
		if(buttonOffsets == null)
			buttonOffsets = new HashMap<AbstractButton, Integer>();
		return buttonOffsets;
	}
	
	/**
	 * Enable/Disable navigator button
	 * when disabled, navigator button is hidden
	 * @param b
	 */
	public void setNavigatorEnabled(boolean enabled){
		setButtonEnabled(navigator, enabled);
	}
	
	/**
	 * Enable/Disable transofrm button
	 * when disabled, navigator button is hidden
	 * @param b
	 */
	public void setTransformsEnabled(boolean enabled){
		setButtonEnabled(transforms, enabled);
	}
	
	/**
	 * check if parent has a child compent
	 * @param p
	 * @param c
	 * @return
	 */
	private boolean hasComponent(Component c){
		Component [] comp = getComponents();
		for(int i=0;i<comp.length;i++)
			if(comp[i] == c)
				return true;
		return false;
	}
	
	/**
	 * Enable/Disable navigator button
	 * when disabled, navigator button is hidden
	 * @param b
	 */
	public void setMagnifierEnabled(boolean enabled){
		setButtonEnabled(magnifierButton,enabled);
	}
	
	
	
	// create GUI component
	private void createGUI(){
		Icon icon;
		setLayout(new FlowLayout(FlowLayout.CENTER));
		setBackground(Color.white);
		
		// create buttons
		icon = new ImageIcon(getClass().getResource(zoomInIcon+size+gif));
		zoomIn = new JButton(icon);
		zoomIn.setPreferredSize(new Dimension(icon.getIconWidth()+10,icon.getIconHeight()+6));
		zoomIn.setToolTipText("Zoom In");
		zoomIn.addActionListener(this);
		zoomIn.setActionCommand("zoom in");
		
		icon = new ImageIcon(getClass().getResource(zoomOutIcon+size+gif));
		zoomOut = new JButton(icon);
		zoomOut.setPreferredSize(zoomIn.getPreferredSize());
		zoomOut.setToolTipText("Zoom Out");
		zoomOut.addActionListener(this);
		zoomOut.setActionCommand("zoom out");
		
		icon = new ImageIcon(getClass().getResource(resetIcon+size+gif));
		reset = new JButton(icon);
		reset.setPreferredSize(zoomIn.getPreferredSize());
		reset.setToolTipText("Reset Zoom");
		reset.addActionListener(this);
		reset.setActionCommand("reset");
		
		icon = new ImageIcon(getClass().getResource(navigatorIcon+size+gif));
		navigator = new JToggleButton(icon);
		navigator.setPreferredSize(zoomIn.getPreferredSize());
		navigator.setToolTipText("Navigator - navigate an image using slide thumbnail");
		navigator.addActionListener(this);
		navigator.setActionCommand("navigator");
		
		icon = new ImageIcon(getClass().getResource(magnifierIcon+size+gif));
		magnifierButton = new JToggleButton(icon);
		magnifierButton.setPreferredSize(zoomIn.getPreferredSize());
		magnifierButton.setToolTipText("Magnifier - mouse over an image region to magnify");
		magnifierButton.addActionListener(this);
		magnifierButton.setActionCommand("magnify");
		
		// create text area
		text = new JTextField("100",3);
		text.setHorizontalAlignment(JTextField.HORIZONTAL);
		text.setEditable(false);
		text.setPreferredSize(zoomIn.getPreferredSize());
		text.setToolTipText("Image Magnification Level");
		
		
		// create transforms menu
		icon = new ImageIcon(getClass().getResource(transformIcon+size+gif));
		transforms = new JToggleButton(icon);
		transforms.setPreferredSize(zoomIn.getPreferredSize());
		transforms.setToolTipText("Rotate and Flip digital slide in real-time");
		transforms.addActionListener(this);
		transforms.setActionCommand("transform");
		
		// create channels menu
		icon = new ImageIcon(getClass().getResource(channelIcon+size+".png"));
		channels = new JToggleButton(icon);
		channels.setPreferredSize(zoomIn.getPreferredSize());
		channels.setToolTipText("Adjust Channels");
		channels.addActionListener(this);
		transforms.setActionCommand("transform");
		
		// add components
		if(viewer instanceof QuickViewer){
			//add(channels);
			add(transforms);
			add(new JSeparator(SwingConstants.VERTICAL));
		}
		add(zoomOut);
		add(text);
		add(zoomIn);
		add(new JSeparator(SwingConstants.VERTICAL));
		add(reset);
		add(magnifierButton);
		add(navigator);
		
	}
	
	// drawing is done, event is sent
	public void propertyChange(PropertyChangeEvent evt){	
		if(evt.getPropertyName().equals(Constants.VIEW_CHANGE)){
			ViewPosition l = (ViewPosition) evt.getNewValue();
			if(viewer.getScalePolicy() != null)
			    text.setText(viewer.getScalePolicy().getScaleString(l.scale));
            else
                text.setText(""+Math.round(l.scale*100));
		}else if(evt.getPropertyName().equals(Constants.IMAGE_CHANGE)){
			//resize navigator
			Navigator nav = (viewer.hasImage())?viewer.getNavigator():null;
			if(navigatorDialog != null && nav != null && navigatorDialog.isVisible()){
				navigatorDialog.setSize(nav.getSize());
				navigatorDialog.validate();
			}
			//enable disable channels
			if(viewer.hasImage())
				channels.setEnabled(viewer.getImageProperties().isMultiChannel());
			channelDialog = null;
		}else if(evt.getPropertyName().equals(Constants.IMAGE_TRANSFORM)){
			if(lastPosition != null)
				viewer.setViewPosition(lastPosition);
			lastPosition = null;
		}
	}
	
	/**
	 * enable/disable buttons
	 */
	public void setEnabled(boolean b){
		/*
		zoomIn.setEnabled(b);
		zoomOut.setEnabled(b);
		reset.setEnabled(b);
		navigator.setEnabled(b);
		magnifierButton.setEnabled(b);
		transforms.setEnabled(b);
		*/
		ViewerHelper.setEnabled(this,b);
	}
	
	//action performed
	public void actionPerformed(ActionEvent e){
		if(e.getSource() == zoomIn){
			viewer.getViewerController().zoomIn();	
		}else if(e.getSource() == zoomOut){
			viewer.getViewerController().zoomOut();	
		}else if(e.getSource() == reset){
			viewer.getViewerController().resetZoom();
		}else if(e.getSource() == navigator){
			Navigator nav = (viewer.hasImage())?viewer.getNavigator():null;
			if(nav != null){
				if(navigatorDialog == null){
					navigatorDialog = createWindow("navigator",nav.getComponent());
				}
				// determine location 
				Dimension vs = viewer.getSize();
				Dimension cs = nav.getSize();
				Point p = new Point(vs.width-cs.width-10,10);
				placeWindow(navigatorDialog,cs,p,navigator.isSelected());
				viewer.firePropertyChange(Constants.NAVIGATOR,null,(navigator.isSelected())?"OPEN":"CLOSED");
			}
		
		}else if(e.getSource() == magnifierButton){
			magnifier = (viewer.hasImage())?viewer.getMagnifier():null;
			if(magnifier != null){
				// for some reason I get weird behavior when window
				// is not recreated, it is easier to remake it everytime
				// then figure out why I get transparent borders
				if(magnifierDialog == null){
					magnifierDialog = createWindow("magnifierButton",magnifier);
					magnifierDialog.addComponentListener(new ComponentListener() {
						public void componentShown(ComponentEvent e) {}
						public void componentResized(ComponentEvent e) {}
						public void componentHidden(ComponentEvent e) {}
						public void componentMoved(ComponentEvent e) {
							if(magnifier.isShowing() && viewer.getViewerComponent().isShowing()){
								// translate point
								Point p = magnifierDialog.getLocation();
								SwingUtilities.convertPointFromScreen(p,viewer.getViewerComponent());
								Dimension sz = magnifier.getSize();
								Dimension d = magnifier.getViewSize();
					        	magnifier.setRectangle(new Rectangle(new Point(p.x+sz.width/2-d.width/2,p.y+sz.height/2-d.height/2),d));
							}
						}
					});
				}
				//	determine location
				Dimension vs = viewer.getSize();
				Dimension cs = magnifier.getSize();
				Point p = new Point(vs.width-cs.width-10,vs.height-cs.height-10);
				magnifier.setRectangle(null);
				placeWindow(magnifierDialog,cs,p,magnifierButton.isSelected());
				viewer.firePropertyChange(Constants.MAGNIFIER,null,(magnifierButton.isSelected())?"OPEN":"CLOSED");
			}
		}else if(e.getSource() == transforms){
			// initialize transform panel
			if(transformPanel == null)
				transformPanel = new ImageTransformationPanel();
			
			// start transforms
			if(transforms.isSelected() && viewer.getImageProperties() != null){
				
				// first set an existing transform
				transformPanel.setImageTransform(viewer.getImageProperties().getImageTransform());
				
				// now set the source image from the viewer
				transformPanel.setOriginalImage(viewer.getSnapshot(Viewer.IMG_NO_MARKERS));
				transformPanel.setViewer(viewer);
				
				// if we can get the original image, we should try
				if(viewer instanceof QuickViewer && !viewer.getImageProperties().getImageTransform().isIdentity()){
					if(((QuickViewer) viewer).getConnectionManager() instanceof TileConnectionManager){
						TileManager m = ((TileConnectionManager)((QuickViewer) viewer).getConnectionManager()).getTileManager();
						ImageProperties ip = viewer.getImageProperties();
						Rectangle view = viewer.getViewRectangle();
						Tile t = m.fetchTile(Utils.getTransformedRectangle(Utils.correctRectangle(ip.getImageSize(),view),ip));
						transformPanel.setOriginalImage(t.getImage());
					}
				}
				
				
				// now lets prompt the user
				int r = JOptionPane.showConfirmDialog(viewer.getViewerPanel(),transformPanel,
						"Transformation Properties",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
				
				// if yes update
				if(r == JOptionPane.OK_OPTION){
					viewer.getImageProperties().setImageTransform(transformPanel.getImageTransform());
					viewer.update();
					viewer.getViewerController().resetZoom();
				}
			}
			
			// unselect button
			transforms.setSelected(false);
		}else if(e.getSource() == channels){
			if(!viewer.hasImage() || !viewer.getImageProperties().isMultiChannel())
				return;
			
			if(channelDialog == null){
				channelDialog = createWindow("Channels",createChannelPanel());
			}
			//	determine location
			Dimension vs = viewer.getSize();
			Dimension cs = channelDialog.getPreferredSize();
			Point p = new Point(10,vs.height-cs.height-10);
			placeWindow(channelDialog,cs,p,channels.isSelected());
		}
	}
	
	/**
	 * create channel selection panel
	 * @return
	 */
	private Component createChannelPanel() {
		JPanel panel = new JPanel();
		panel.setBorder(new TitledBorder("Channels"));
		panel.setLayout(new GridLayout(0,1));

		// create listener
		ActionListener l = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JButton bt = (JButton) e.getSource();
				String nm = e.getActionCommand();
				// check colors from transform first
				ImageTransform t = viewer.getImageProperties().getImageTransform();
				Map<String,Color> map = t.getChannelMap();
				if(map == null)
					map = t.getOriginalChannelMap();
				
				// copy map
				Map<String,Color> nmap = new LinkedHashMap<String, Color>(t.getOriginalChannelMap());
				if(t.getChannelMap() != null)
					nmap.putAll(t.getChannelMap());
				
				if(bt.getBackground().equals(Color.black)){
					Color color = map.get(nm);
					if(Color.black.equals(color))
						color = t.getOriginalChannelMap().get(nm);
					bt.setForeground(Utils.getForegroundForBackground(color));
					bt.setBackground(color);
					
					nmap.put(nm,color);
					t.setChannelMap(nmap);
					
					// if channel map is equal to original, clear it
					if(nmap.equals(t.getOriginalChannelMap()))
						t.setChannelMap(null);
				
				}else{
					bt.setBackground(Color.black);
					bt.setForeground(Color.white);
					
					nmap.put(nm,Color.black);
					t.setChannelMap(nmap);
				}
				
				// update
				lastPosition = viewer.getViewPosition();
				viewer.update();
			}
		};
		// create mouse listener
		MouseAdapter ml = new MouseAdapter() {
			public void mouseClicked(MouseEvent e){
				if(e.getButton() == MouseEvent.BUTTON3){
					JButton bt = (JButton) e.getSource();
					if(bt.getBackground().equals(Color.black)){
						JOptionPane.showMessageDialog(viewer.getViewerComponent(),"Channel must be enabled before customizing its color");
					}else{
						String name = bt.getActionCommand();
						Color color = JColorChooser.showDialog(viewer.getViewerComponent(),"Select Color for "+name+" Channel",bt.getBackground());
						// customize color
						ImageTransform t = viewer.getImageProperties().getImageTransform();
						Map<String,Color> nmap = new LinkedHashMap<String, Color>(t.getOriginalChannelMap());
						if(t.getChannelMap() != null)
							nmap.putAll(t.getChannelMap());
						nmap.put(name,color);
						bt.setForeground(Utils.getForegroundForBackground(color));
						bt.setBackground(color);
						t.setChannelMap(nmap);
						
						// update
						lastPosition = viewer.getViewPosition();
						viewer.update();
					}
				}
			}
		};
		
		
		// create buttons
		Map<String,Color> map = viewer.getImageProperties().getChannelMap();
		for(String key: map.keySet()){
			JButton bt = new JButton(key);
			bt.setOpaque(true);
			bt.setToolTipText("Left-click to enable/disable, Right-click to customize channel color");
			bt.setForeground(Utils.getForegroundForBackground(map.get(key)));
			bt.setBackground(map.get(key));
			bt.setActionCommand(key);
			bt.addActionListener(l);
			bt.addMouseListener(ml);
			panel.add(bt);
		}

		return panel;
	}

	/**
	 * Create JWindow
	 * @param c
	 * @return
	 */
	private Window createWindow(String title,Component c){
		JDialog dialog = null; 
		title = null; 
		
		// find parent
		//Container vc = viewer.getViewerComponent().getParent();
		Window vc = SwingUtilities.getWindowAncestor(viewer.getViewerComponent());
		if(vc != null){
			if(vc instanceof Frame)
				dialog = new JDialog((Frame)vc);
			else if(vc instanceof Dialog)
				dialog = new JDialog((Dialog)vc);
		}
		
		// try to find a frame
		if(dialog == null){
			for(Container comp = viewer.getViewerComponent().getParent();comp != null; comp=comp.getParent()){
				if(comp instanceof Frame){
					dialog = new JDialog((Frame) comp);
					break;
				}
			}
		}
	
		// we didn't succeed :(	
		if(dialog == null)
			dialog = new JDialog();
		
		// dialog stuff
		dialog.setModal(false);
		dialog.setUndecorated(true);
		
		// create panel
		Border border = new LineBorder(Color.black,4);
		if(title != null){
			TitledBorder tb = new TitledBorder(border,title);
			tb.setTitleFont(new Font("Sans",Font.PLAIN,10));
			tb.setTitleJustification(TitledBorder.CENTER);
			tb.setTitlePosition(TitledBorder.TOP);
			Border margin = new EmptyBorder(-7,0,0,0);
			border = new CompoundBorder(tb,margin);
		}
		((JComponent)dialog.getContentPane()).setBorder(border);
		dialog.getContentPane().add(c);
		dialog.setFocusable(true);
		dialog.pack();
		dialog.repaint();
		
		// attach motion adapter
		DialogAdapter ad = new DialogAdapter(dialog,c);
		dialog.addMouseListener(ad);
		dialog.addMouseMotionListener(ad);
		
		return dialog;
	}
	
	
	//	place navigator window on screen
	private void placeWindow(Window win, Dimension size,Point offset,boolean open){
		if(open){
			if(viewer.getViewerComponent().isShowing()){
				showWindow(win, offset);
			}else{
				final Window fwin = win;
				final Point offs = offset;
				// if window is not showing, then wait until it is
				if(viewer.getViewerComponent() instanceof JComponent){
					((JComponent)viewer.getViewerComponent()).addAncestorListener(new AncestorListener() {
						public void ancestorRemoved(AncestorEvent event) {}
						public void ancestorMoved(AncestorEvent event) {}
						public void ancestorAdded(AncestorEvent event) {
							showWindow(fwin,offs);
							((JComponent)viewer.getViewerComponent()).removeAncestorListener(this);
						}
					});
				}else{
					viewer.getViewerComponent().addComponentListener(new ComponentAdapter() {
						public void componentShown(ComponentEvent e) {
							showWindow(fwin,offs);
							viewer.getViewerComponent().removeComponentListener(this);
						}
					});
				}
			}
	    }else if(!open){
	    	win.setVisible(open);
		}
	    
	}
	
	/**
	 * show window
	 * @param win
	 * @param offset
	 */
	private void showWindow(Window win, Point offset){
		Point p = viewer.getViewerComponent().getLocationOnScreen();
		//win.setBounds(p.x+offset.x,p.y+offset.y,size.width,size.height);
		win.setLocation(p.x+offset.x,p.y+offset.y);
		win.setVisible(true);
		win.toFront();
	}
	
	
	
	public void	keyPressed(KeyEvent e){
		// bring up slide info
		if(e.getKeyCode() == KeyEvent.VK_I && (e.isControlDown() || e.isAltDown())){
			Container cont = viewer.getInfoPanel();
			if(cont != null){
				JOptionPane.showMessageDialog(viewer.getViewerComponent(),cont,
											"Information",JOptionPane.PLAIN_MESSAGE);
			}	
		}
	}
    public void	keyTyped(KeyEvent e){} 
	public void	keyReleased(KeyEvent e){
		switch(e.getKeyCode()){
			case (KeyEvent.VK_UP): viewer.getViewerController().panUp(); break;
			case (KeyEvent.VK_DOWN): viewer.getViewerController().panDown(); break;
			case (KeyEvent.VK_LEFT): viewer.getViewerController().panLeft(); break;
			case (KeyEvent.VK_RIGHT): viewer.getViewerController().panRight(); break;
			case (KeyEvent.VK_HOME):  viewer.getViewerController().resetZoom();break;
			case (KeyEvent.VK_EQUALS):  viewer.getViewerController().zoomIn();break;
			case (KeyEvent.VK_MINUS):  viewer.getViewerController().zoomOut();break;
		}
	}
    
	private class DialogAdapter extends MouseAdapter implements MouseMotionListener {
		private Point orig;
		private Window dialog;
		private Component navigator;
		private boolean resize;
		public DialogAdapter(Window d, Component n){
			dialog = d;
			navigator = n;
		}
		public void mouseEntered(MouseEvent e) {
			Point p = e.getPoint();
			Dimension d = dialog.getSize();
			resize = p.x > (d.width - 20) && p.y > (d.height - 20);
			if(resize)
				dialog.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
			else
				dialog.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));	
		}
		public void mouseExited(MouseEvent e) {
			dialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));	
		}
		public void mousePressed(MouseEvent e) {
        	orig = e.getPoint();            //remember the location of mouse cursor
		}
		public void mouseReleased(MouseEvent e){
			if(resize){
				navigator.validate();
				dialog.validate();
			}
		}
		
		public void	mouseDragged(MouseEvent e){
			Point pt = (Point) e.getPoint().clone();
			if(resize){
				Dimension d = dialog.getSize();
				int dx = pt.x - orig.x;
				int dy = pt.y - orig.y;
				/* fix aspect ratio
				if(dx > dy)
					dy = (int)(dx*d.height/d.width);
				else
					dx = (int)(dy*d.width/d.height);
				*/
				dialog.setSize(d.width+dx,d.height+dy);
				navigator.setSize(dialog.getSize());
				orig = pt;
				navigator.validate();
			}else{
				SwingUtilities.convertPointToScreen(pt,dialog);
				pt.x = pt.x - orig.x;
				pt.y = pt.y - orig.y;
				dialog.setLocation(pt);
			}
		}
		public void mouseMoved(MouseEvent e){
			Point p = e.getPoint();
			Dimension d = dialog.getSize();
			resize = p.x > (d.width - 20) && p.y > (d.height - 20);
			if(resize)
				dialog.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
			else
				dialog.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		}
	}
	
	/**
	 * misc mouse events attached by controler
	 */
	public void mousePressed(MouseEvent e){}
	public void mouseReleased(MouseEvent e){}
	public void mouseEntered(MouseEvent e){}
	public void mouseExited(MouseEvent e){}
	public void mouseClicked(MouseEvent e){
		if(e.getButton() == MouseEvent.BUTTON2){
			if(magnifierButton.isShowing() && magnifierButton.isEnabled())
				magnifierButton.doClick();
		}
	}
	
}
