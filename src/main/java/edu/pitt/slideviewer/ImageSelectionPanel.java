package edu.pitt.slideviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.pitt.slideviewer.ViewerHelper.FilterDocument;
/**
 * this is an image selection panel for the viewer
 * @author tseytlin
 *
 */
public class ImageSelectionPanel extends JPanel {
	private final String FOLDER_ICON = "/icons/Open16.gif";
	private final String IMAGE_ICON  = "/icons/Image16.gif";
	private final String PREVIEW_ICON = "/icons/Preview128.png";
	private JList input;
	private JTextField inputImage;
	private JPanel searchPanel;
	private JProgressBar progress;
	private String currentPath;
	private Icon folderIcon,imageIcon;
	private Timer timer;
	private JComboBox locationSelector;
	
	/**
	 * create new image selection panel
	 */
	public ImageSelectionPanel(){
		super();
		createUI();
		load("");
	}
	
	/**
	 * get selected images, return empty array
	 * if no image was selected
	 * @return
	 */
	public String [] getSelectedImages(){
		Object  [] list = input.getSelectedValues();
		String  [] s = new String [list.length];
		for(int i=0;i<s.length;i++){
			s[i] = ((isEmpty(currentPath))?"":currentPath+"/")+list[i].toString();
		}
		return s;
	}
	
	public String getSelectedLocation(){
		return (locationSelector != null)?locationSelector.getSelectedItem().toString():ViewerFactory.getPropertyLocation();
	}
	
	/**
	 * display busy
	 * @param b
	 */
	private void setBusy(boolean busy){
		JComponent c = this;
		if(busy){
			c.remove(searchPanel);
			c.add(progress,BorderLayout.SOUTH);
		}else{
			c.remove(progress);
			c.add(searchPanel,BorderLayout.SOUTH);
		}
		c.revalidate();
		c.repaint();
	}
	
	/**
	 * load data 
	 * @param path
	 */
	private void load(String path){
		// strip slash
		if(path != null && path.endsWith("/"))
			path = path.substring(0,path.length()-1);
		
		// set current path
		if(isEmpty(currentPath) || isEmpty(path)){
			currentPath = path;
		}else if("..".equals(path)){
			int x = currentPath.lastIndexOf("/");
			currentPath = (x > -1)?currentPath.substring(0,x):null;
		}else{
			currentPath = currentPath+"/"+path;
		}
	
		// start new thread
		(new Thread(new Runnable(){
			public void run(){
				// create list populated w/ available images
				setBusy(true);
				Vector<String> list = new Vector<String>();
				if(currentPath != null && currentPath.length() > 0)
					list.add("..");
				list.addAll(ViewerFactory.getImageList(currentPath,true));
				
				// load vector
				input.setListData(list);
				inputImage.setDocument(new FilterDocument(inputImage,input,list));	
				
				setBusy(false);
			
			}
		})).start();
	}
	
	/**
	 * get selected image, return null
	 * if no image was selected
	 * @return
	 */
	public String getSelectedImage(){
		String [] s = getSelectedImages();
		if(s.length > 0)
			return s[0];
		if(inputImage.getText().length() > 0)
			return inputImage.getText().trim();
		return null;
	}
	
	/**
	 * is this a folder
	 * @param value
	 * @return
	 */
	private boolean isFolder(Object value){
		return value != null && (value.toString().endsWith("/") || value.toString().equals(".."));
	}
	
	/**
	 * is empth
	 * @param value
	 * @return
	 */
	private boolean isEmpty(String value){
		return value == null || value.length() == 0;
	}
	
	/**
	 * Create project selection dialog component
	 */
	public void createUI(){
		setLayout(new BorderLayout());
		
		// progress bar
		progress = new JProgressBar();
		progress.setIndeterminate(true);
		progress.setString("Loading Images ...");
		
		// create image list
		input = new JList();
		input.setVisibleRowCount(10);
		input.setCellRenderer(new DefaultListCellRenderer(){
			public Component getListCellRendererComponent(JList list, Object value, 
				int index, boolean isSelected,	boolean cellHasFocus) {
				JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if(isFolder(value)){
					if(folderIcon == null)
						folderIcon = new ImageIcon(getClass().getResource(FOLDER_ICON));
					lbl.setIcon(folderIcon);
				}else{
					if(imageIcon == null)
						imageIcon = new ImageIcon(getClass().getResource(IMAGE_ICON));
					lbl.setIcon(imageIcon);
				}
				return lbl;
			}
		});
		
		// place selector 
		if(ViewerFactory.getPropertyLocations().size() > 1){
			locationSelector = new JComboBox(new Vector<String>(ViewerFactory.getPropertyLocations()));
			locationSelector.setSelectedItem(ViewerFactory.getPropertyLocation());
			locationSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if(e.getStateChange() == ItemEvent.SELECTED){
						ViewerFactory.setPropertyLocation((String)e.getItem());
						load("");
					}
				}
			});
			JPanel lp = new JPanel();
			lp.setLayout(new BorderLayout());
			lp.setBorder(new TitledBorder("Location"));
			lp.add(locationSelector,BorderLayout.CENTER);
			add(lp,BorderLayout.NORTH);
		}
		
		JScrollPane scroll = new JScrollPane(input);
		scroll.setPreferredSize(new Dimension(250,300));
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(new TitledBorder("Image List"));
		add(scroll,BorderLayout.CENTER);
		
		// create a preview panel
		// consult panel
		final JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BorderLayout());
		infoPanel.setBorder(new TitledBorder("Image Preview"));
		
		final JPanel previewPanel = new JPanel();
		URL url = ViewerHelper.class.getResource(PREVIEW_ICON);
		final JButton preview = (url != null)?new JButton(new ImageIcon(url)):new JButton("Preview");
		preview.setText("Click Here");
		preview.setVerticalTextPosition(JButton.BOTTOM);
		preview.setHorizontalTextPosition(JButton.CENTER);
		
		previewPanel.setLayout(new GridBagLayout());
		previewPanel.setPreferredSize(new Dimension(200,200));
		previewPanel.add(preview,new GridBagConstraints());
		previewPanel.setBorder(new TitledBorder("Image Preview"));
		add(previewPanel,BorderLayout.EAST);
		
		preview.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				String name = getSelectedImage();
				if(name != null){
					try{
						ImageProperties im = ViewerFactory.getImageProperties(name);
						if(im != null && im.getThumbnail() != null){
							infoPanel.removeAll();
							Image thumb = im.getThumbnail();
							int w,h;
							if(im.isHorizontal()){
								w = 200;
								h = (w * thumb.getHeight(null))/thumb.getWidth(null);
							}else{
								h = 200;
								w = (h * thumb.getWidth(null))/thumb.getHeight(null);
							}
							Image img = thumb.getScaledInstance(w,h,Image.SCALE_SMOOTH);
							JLabel lbl = new JLabel(new ImageIcon(img));
							lbl.setBorder(new LineBorder(Color.black));
							lbl.setMaximumSize(new Dimension(img.getWidth(null),img.getHeight(null)));
							//String d = (im.isHorizontal())?BorderLayout.SOUTH:BorderLayout.EAST;
							Dimension s = im.getImageSize();
							String text = "<html><table width="+w+"><tr><td><br>"+name+"</td><tr><td>"+s.width+" x "+
							s.height+"</td></tr><tr><td>"+im.getSource()+"</td></tr></table>";
							
							int axis = (im.isHorizontal())?BoxLayout.Y_AXIS:BoxLayout.X_AXIS;
							infoPanel.setLayout(new BoxLayout(infoPanel,axis));
							//infoPanel.add(lbl,BorderLayout.CENTER);
							//infoPanel.add(new JLabel(text),d);
							infoPanel.add(lbl);
							infoPanel.add(new JLabel(text));
							
							// add panel
							remove(previewPanel);
							add(infoPanel,BorderLayout.EAST);
							validate();
						}
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}});
		
		// create text field
		inputImage = new JTextField();
		//inputProject.setDocument(new FilterDocument(inputProject,input,vec));
				
		searchPanel = new JPanel();
		searchPanel.setLayout(new BorderLayout());
		searchPanel.add(inputImage,BorderLayout.CENTER);
		searchPanel.setBorder(new TitledBorder("Search"));
		add(searchPanel,BorderLayout.SOUTH);
		
		
		// add listener to list
		input.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e){
				if(inputImage != null && input.getSelectedValue() != null){
					inputImage.setText(input.getSelectedValue().toString());
					// add panel
					remove(infoPanel);
					add(previewPanel,BorderLayout.EAST);
					validate();
				}
			}
		});
		input.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					String path = (String) input.getSelectedValue();
					if(isFolder(path)){
						load(path);
					}else{
						// do click
						// iterate through parents
						for(Component c=(Component) e.getSource();c != null;c=c.getParent()){
							if(c instanceof JOptionPane){
								// set OK close option
								((JOptionPane)c).setValue(new Integer(JOptionPane.OK_OPTION));
							}else if(c instanceof Dialog){
								// we found dialog, great, lets close it
								((Dialog) c).dispose();
								break;
							}
						}
					}
				}
			}
		});
	}
	
	/**
	 * show dialog w/ selector
	 * @param c
	 */
	
	public boolean showDialog(Component c){
		// THIS IS A HACK TO GET AROUND SUN JAVA BUG
		timer = new Timer(100,new ActionListener() {
			public void actionPerformed(ActionEvent e){
	            if(inputImage.hasFocus()) {
	            	timer.setRepeats(false);
	                return;
	            }
	            inputImage.requestFocusInWindow();
	        }
	 
	    });
		timer.setRepeats(true);
		timer.start();
		boolean ok = JOptionPane.OK_OPTION == 
			   JOptionPane.showConfirmDialog(c,this,"Open Slide Image",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		
		// make sure image was selected and not a folder
		if(ok && isFolder(input.getSelectedValue())){
			load((String) input.getSelectedValue());
			return showDialog(c);
		}
		
		return ok;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Properties p = new Properties();
		//p.load((new URL("http://slidetutor.upmc.edu/viewer/SlideViewer.conf")).openStream());
		p.load((new URL("file:///home/tseytlin/Download/SlideViewer.conf")).openStream());
		
		//Properties p2 = new Properties();
		//p2.load((new URL("http://slidetutor.upmc.edu/domainbuilder/config/UPENN/DomainBuilder.conf")).openStream());
		
		
		ViewerFactory.setProperties(p);
		
		//ViewerFactory.addProperties("UPMC",p);
		//ViewerFactory.addProperties("UPENN",p2);
		//ViewerFactory.setPropertyLocation("UPMC");
		
		ImageSelectionPanel panel = new ImageSelectionPanel();
		if(panel.showDialog(null)){
			System.out.println(Arrays.toString(panel.getSelectedImages()));
		}
	}

}
