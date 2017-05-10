package edu.pitt.slideviewer;
import javax.swing.*;

import java.io.*;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.image.*;
import javax.imageio.*;

import edu.pitt.slideviewer.markers.Annotation;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;


/**
 * This class acts as a collection of usefull
 * helper methods
 * @author tseytlin
 */
public class ViewerHelper {
    
    /**
     * Method to save an Image as Jpeg file using new Java1.4 API
     * @param Image img - this is the image you want to save
     * @param File file - output file
     * @throws IOException - in case file cannot be saved.
     */
    public static void writeJpegImage(Image img, File file) throws IOException {
        String fileName = file.getAbsolutePath();
        if(!fileName.toLowerCase().endsWith(".jpg"))
            fileName = fileName + ".jpg";
        
        FileOutputStream f = new FileOutputStream(fileName);
        //int quality = 80;
        /*
        MediaTracker tracker = new MediaTracker(frame);
        tracker.addImage( img, 0 );
        try {
            tracker.waitForAll();
        } catch ( InterruptedException ie ) {}
        */
        int width = img.getWidth( null );
        int height = img.getHeight( null );
        //System.err.println("Saving image: "+fileName+" "+width+"x"+height);
            
        //convert Image to BufferedImage
        BufferedImage bimg = new BufferedImage( width, height, BufferedImage.TYPE_INT_RGB );
        Graphics2D graphics2D = bimg.createGraphics();
        graphics2D.drawImage( img, 0, 0, width, height, null );

        //clean up
        img = null;
        graphics2D = null;

        ImageIO.write(bimg,"JPEG",f);
        
        /*
        //get JPEG Image Writer
        ImageWriter writer = ( ImageWriter ) ImageIO.getImageWritersByFormatName( "JPEG" ).next();
        ImageWriteParam param = writer.getDefaultWriteParam();

        //set quality
        param.setCompressionMode( ImageWriteParam.MODE_EXPLICIT );
        param.setCompressionQuality( ( float ) quality / 100 );

        //write jpeg image
        writer.setOutput( ImageIO.createImageOutputStream( f ) );
        writer.write( null, new IIOImage( bimg, null, null ), param );
		*/
        //clean up
        f.close();
        //writer.dispose();
        bimg = null;
        img = null;
    }
    
    /**
     * FileFilter for JFileChooser for viewing JPEG images
     * @author tseytlin
     */
    public static class JpegFileFilter extends javax.swing.filechooser.FileFilter {
        public boolean accept(File f){
            String n = f.getName().toLowerCase();
            return f.isDirectory() || n.endsWith(".jpg") || n.endsWith(".jpeg");
        }
        public String getDescription(){
            return  "JPEG Image files (.jpg)";
        }
    }
    /**
     * Renderer for JList to view TutorMarkers
     * @author tseytlin
     */
    public static class ShapeRenderer extends DefaultListCellRenderer {
         public Component getListCellRendererComponent(JList l,Object v,int i,boolean s,boolean f) {
            Component c = super.getListCellRendererComponent(l,v,i,s,f);
            if(c instanceof JLabel && v instanceof Annotation){
            	JLabel lbl = (JLabel) c;
            	Annotation m = (Annotation) v;
                lbl.setText(m.getName());
                lbl.setForeground(m.getColor().darker());
            }
            return c;
         }  
    }
    
    
    /**
     * This class is the panel for JFileChooser to select a
     * way image snapshot will be saved
     * @author tseytlin
     *
     */
    public static class SnapshotChooserPanel extends JPanel {
        private ButtonGroup chooserRadioButtons;
        
        // create panel
        public SnapshotChooserPanel(){
            super();
            chooserRadioButtons = new ButtonGroup();
            setBorder(new TitledBorder(new LineBorder(Color.black),"Save Options"));
            setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
            Font ft = getFont().deriveFont(Font.PLAIN);
            
            // add radio buttons
            JRadioButton bt2 = new JRadioButton("all annotations",true);
            bt2.setFont(ft);
            bt2.setActionCommand("ALL_MARKERS");
            add(bt2);
            chooserRadioButtons.add(bt2);
            
            // add radio buttons
            JRadioButton bt3 = new JRadioButton("selected annotations");
            bt3.setFont(ft);
            bt3.setActionCommand("SELECTED_MARKERS");
            add(bt3);
            chooserRadioButtons.add(bt3);
            
            // add radio buttons
            JRadioButton bt1 = new JRadioButton("no annotations");
            bt1.setFont(ft);
            bt1.setActionCommand("NO_MARKERS");
            add(bt1);
            chooserRadioButtons.add(bt1);
        }
        
        /**
         * Get the mode selected in this panel
         * @return snapshot mode
         * Values: Viewer.IMG_NO_MARKERS,Viewer.IMG_SELECTED_MARKERS,Viewer.IMG_ALL_MARKERS
         */
        public int getSelectedMode(){
            // select mode
            int mode = 0;
            String cmd = chooserRadioButtons.getSelection().getActionCommand();
            if(cmd.equalsIgnoreCase("NO_MARKERS"))
                mode = Viewer.IMG_NO_MARKERS;
            else if(cmd.equalsIgnoreCase("SELECTED_MARKERS"))
                mode = Viewer.IMG_SELECTED_MARKERS;
            else
                mode = Viewer.IMG_ALL_MARKERS;
            return mode;
        }
    }
    
    /**
     * Label with borders around text
     * Encouraged by 
     * @author tseytlin
     */
    public static class ContrastLabel extends JLabel {
        private Color shadowColor = Color.white; 
        
        public ContrastLabel(String text) {
        	super();
        	setFont(getFont().deriveFont(Font.BOLD,15));
        }
        
        public void setShadowColor(Color c){
    		shadowColor = c;
        }
      
        /* paint */
    	public void paintComponent(Graphics g) {
    		Graphics2D g2 = (Graphics2D) g;
    		// Enable antialiasing for text
    		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    		char[] chars = getText().toCharArray();

    		FontMetrics fm = this.getFontMetrics(getFont());
    		int h = fm.getAscent();
    		g.setFont(getFont());

    		for (int i=0,x=0,w=0; i < chars.length; i++, x+=w) {
    			char ch = chars[i];
    			w = fm.charWidth(ch);

    			g.setColor(shadowColor);
    			g.drawString("" + chars[i], x - 1, h + 1);
    			g.drawString("" + chars[i], x + 1, h + 1);
    			g.drawString("" + chars[i], x, h - 1);
    			g.drawString("" + chars[i], x - 1, h - 1);
    		}
    		super.paintComponent(g);
    	}
    }
    
    
    
    /**
     * Converts scale zoom to string representation that
     * makes sence to pathologists: 40X,20X, etcc.
     * @param scl - float scale factor
     * @return "2X", "4X", "10X", "20X", "40X"
     */
    public static String convertScaleToPathologyZoom(double scl){
    	if(scl >= .5)
    		return "40x";
    	else if(scl >= .2)
    		return "20x";
    	else if(scl >= .1)
    		return "10x";
    	else if(scl >= .03)
    		return "4x";
    	else
    		return "1x";
    }
    
    /**
     * Converts scale zoom to string representation to zoom levels
     * @param scl - float scale factor
     * @return "low", "medium", "high"
     */
    public static String convertScaleToLevelZoom(double scl){
    	if(scl >= .5)
    		return "high";
    	else if(scl >= .12)
    		return "medium";
    	else
    		return "low";
    }
    
    /**
     * Converts zoom represented as 4x, 10x etc .. as scale value
     * @param path "2X", "4X", "10X", "20X", "40X"
     * @return float scale factor
     */
    public static double convertPathologyZoomToScale(String path){
    	if(path.equalsIgnoreCase("40x"))
    		return .5;
    	else if(path.equalsIgnoreCase("20x"))
    		return .25;
    	else if(path.equalsIgnoreCase("10x"))
    		return .125;
    	else if(path.equalsIgnoreCase("4x"))
    		return .0625;
    	else
    		return -1;
    }
    
    
    /**
     * Converts string representation to zoom levels to scale zoom
     * @param "low", "medium", "high"
     * @return scl - float scale factor
     */
    public static double convertLevelZoomToScale(String level){
    	if(level.equalsIgnoreCase("high"))
    		return .5;
    	else if(level.equalsIgnoreCase("medium"))
    		return .125;
    	else if(level.equalsIgnoreCase("low"))
    		return .03125;
    	else
    		return -1;
    }
    
    /**
     * if rectangle has negative width or height, convert to equivalent rectangle
     * with posititve width or height, else don't do anything
     * @param rect
     * @return
     */
    public static Rectangle getAbsoluteRectangle(Rectangle rect){
    	if(rect == null)
    		return null;
    	
    	Rectangle r = new Rectangle(rect);
    	// take care of negative width height
    	if(r.width < 0){
    		r.width = - r.width;
    		r.x = r.x - r.width;
    	}
    	if(r.height < 0){
    		r.height = - r.height;
    		r.y = r.y - r.height;
    	}
    	// take care of width height that are 0
    	if(r.width == 0)
    		r.width = 1;
    	if(r.height == 0)
    		r.height = 1;
    	return r;
    }
    
    
    /**
     * Calculate which side of a rectanlge should be used to calculate
     * the other based on image and window dimensions
     * @param size - size of viewing window
     * @param isize - absolute size of image
     * @return - true if horizontal side to be used
     */
    
    public static boolean isHorizontal(Dimension size, Dimension isize){
    	double vratio = (size.getWidth()/size.height);
    	double iratio = (isize.getWidth()/isize.height);
    	return vratio < iratio;
    }
    
        
    /**
	 * Create project selection dialog component
	 */
	public static JPanel createDynamicSelectionPanel(JList list, Collection elements){
		final JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		//panel.add(new JLabel("Select an image you want to load "),BorderLayout.NORTH);
		
		// create list populated w/ available images
		// ???? queryServlet should be moved to AuthorUtils
		Vector vec = new Vector();
		vec.addAll(elements);
		
		final JList input = list;
		input.setListData(vec);
		//input.setCellRenderer(new SlideInfoCellRenderer());
		
		JScrollPane scroll = new JScrollPane(input);
		scroll.setPreferredSize(new Dimension(250,200));
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(new TitledBorder("Image List"));
		panel.add(scroll,BorderLayout.CENTER);
		
		// create a preview panel
		// consult panel
		final JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BorderLayout());
		infoPanel.setBorder(new TitledBorder("Image Preview"));
		
		final JPanel previewPanel = new JPanel();
		URL url = ViewerHelper.class.getResource("/icons/Preview128.png");
		final JButton preview = (url != null)?new JButton(new ImageIcon(url)):new JButton("Preview");
			
		previewPanel.setLayout(new GridBagLayout());
		previewPanel.setPreferredSize(new Dimension(200,200));
		previewPanel.add(preview,new GridBagConstraints());
		previewPanel.setBorder(new TitledBorder("Image Preview"));
		panel.add(previewPanel,BorderLayout.EAST);
		
		preview.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				String name = (String) input.getSelectedValue();
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
							panel.remove(previewPanel);
							panel.add(infoPanel,BorderLayout.EAST);
							panel.validate();
						}
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}});
		
		// create text field
		final JTextField inputProject = new JTextField();
		inputProject.setDocument(new FilterDocument(inputProject,input,vec));
		//panel.add(inputProject,BorderLayout.SOUTH);
		
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(inputProject,BorderLayout.CENTER);
		p.setBorder(new TitledBorder("Search"));
		panel.add(p,BorderLayout.SOUTH);
		
		
		// add listener to list
		input.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e){
				if(inputProject != null && input.getSelectedValue() != null){
					inputProject.setText(input.getSelectedValue().toString());
					// add panel
					panel.remove(infoPanel);
					panel.add(previewPanel,BorderLayout.EAST);
					panel.validate();
				}
			}
		});
		input.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
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
		});
		return panel;		
	}
    
	/**
	 * this class helps in creating a dynamic list based on search
	 * @author tseytlin
	 */
	public static class FilterDocument extends PlainDocument {
		private JTextField textField;
		private JList list;
		private List problemList;
		
		public FilterDocument(JTextField text, JList list,List vec){
			this.textField = text;
			this.list = list;
			this.problemList = vec;
		}
		
		public void setSelectableObjects(List obj){
			problemList = obj;
		}
		
		public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
			super.insertString(offs, str, a);
			if(str.length() == 1)
				filter();
 	    }
		public void remove(int offs,int len) throws BadLocationException {
			super.remove(offs,len);
			filter();
		}
		public void replace(int offs,int len,String txt,AttributeSet a) throws BadLocationException{
			super.remove(offs,len);
			insertString(offs,txt,a);
		}
		
		public void filter(){
			textField.setForeground(Color.black);
			String text = textField.getText().toLowerCase();	
			Vector v = new Vector();
			for(int i=0;i<problemList.size();i++){
				Object prob = problemList.get(i);
				if(prob.toString().toLowerCase().contains(text))
					v.add(prob);
			}
			if(v.size() == 0)
				textField.setForeground(Color.red);
			
			list.setListData(v);
			
			if(v.size() == 1)
				list.setSelectedIndex(0);
			
		}
	}
	
	/**
	 * create menu item
	 * @param name
	 * @param tip
	 * @param icon
	 * @param listener
	 * @return
	 */
	public static JMenuItem createMenuItem(String name, String tip, String icon, ActionListener listener){
		JMenuItem item = new JMenuItem(name);
		item.setActionCommand(name);
		item.setToolTipText(tip);
		if(icon != null)
			item.setIcon(new ImageIcon(ViewerHelper.class.getResource(icon)));
		item.addActionListener(listener);
		return item;
	}
	
	
	/**
	 * text panel for entering new concepts
	 * @author tseytlin
	 */
	public static class MultipleEntryPanel extends JPanel {
		private JTextPane textPanel;
		private JPanel headerPanel;
		
		/**
		 * create multple entry panel
		 */
		public MultipleEntryPanel(){
			this("Enter a List of Entries");
		}
		
		/**
		 * create multple entry panel
		 */
		public MultipleEntryPanel(String title){
			super();
			setLayout(new BorderLayout());
			headerPanel = new JPanel();
			headerPanel.setLayout(new BoxLayout(headerPanel,BoxLayout.Y_AXIS));
			headerPanel.add(new JLabel("  1"));
			textPanel = new JTextPane(){
				// disable line wrapping
				public boolean getScrollableTracksViewportWidth(){
					if(getDocument().getLength() < 50)
						return true;
					return false;
				}
			};
			textPanel.getDocument().addDocumentListener(new DocumentListener(){
				public void changedUpdate(DocumentEvent e) {}
				public void insertUpdate(DocumentEvent e) {
					try{
						String s = e.getDocument().getText(e.getOffset(),e.getLength());
						if(s.contains("\n"))
							update();
					}catch(BadLocationException ex){}
				}
				public void removeUpdate(DocumentEvent e) {
					try{
						if(e.getLength() > 1){
							update();
						}else{
							String s = e.getDocument().getText(e.getOffset(),e.getLength());
							if(s.contains("\n"))
								update();
						}
					}catch(BadLocationException ex){
						ex.printStackTrace();
					}
				}
				private void update(){
					// if more newlines, simply add
					int n = getNewLineCount(textPanel.getText())+1;
					int c = headerPanel.getComponentCount();
					if(n > c){
						for(int i=c;i< n;i++){
							String s = (i<9)?"  ":"";
							headerPanel.add(new JLabel(s+(i+1)));
						}
					}else if(n < c){
						for(int i=c-1;i>=n;i--){
							headerPanel.remove(i);
						}
					}
					headerPanel.validate();
					headerPanel.repaint();
				}
				
			});
			JScrollPane scroll = new JScrollPane(textPanel);
			scroll.setBorder(new TitledBorder(title));
			scroll.setPreferredSize(new Dimension(400,200));
			scroll.setRowHeaderView(headerPanel);
			scroll.getViewport().setBackground(Color.white);

			add(scroll,BorderLayout.CENTER);
			JLabel hint = new JLabel("HINT: enter each entry on a new line");
			hint.setBackground(new Color(255,255,150));
			hint.setFont(hint.getFont().deriveFont(Font.PLAIN,11));
			hint.setHorizontalAlignment(JLabel.CENTER);
			add(hint,BorderLayout.SOUTH);
		}
		
		/**
		 * get number of newlines in a string
		 * @param str
		 * @return
		 */
		private int getNewLineCount(String str){
			int count = 0;
			for(int i = 0;i<str.length();i++){
				if(str.charAt(i) == '\n')
					count ++;
			}
			return count;
		}
		
		/**
		 * get text panel
		 * @return
		 */
		public JTextPane getTextPanel(){
			return textPanel;
		}
		
		/**
		 * get entries 
		 * @return
		 */
		public Collection<String> getEntries(){
			String str = textPanel.getText();
			ArrayList<String> list = new ArrayList<String>();
			for(String s: str.split("\n")){
				if(s.trim().length() > 0)
					list.add(s);
			}
			return list;
		}
		
		/**
		 * get entries 
		 * @return
		 */
		public void setEntries(Collection<String> entries){
			StringBuffer str = new StringBuffer();
			for(String s: entries){
				str.append(s+"\n");
			}
			textPanel.setText(str.toString().trim());
		}
	}
	
	/**
	 * parse rectangle string in whatever format
	 * 
	 * @param str
	 * @return
	 */
	public static Rectangle parseRectangle(String str) {
		try {
			int i = 0;
			int[] r = new int[4];
			for (String s : str.split("[^\\d]+")) {
				if (s.length() > 0 && i < r.length) {
					r[i++] = Integer.parseInt(s);
				}
			}
			return new Rectangle(r[0], r[1], r[2], r[3]);
		} catch (Exception ex) {
			// log.severe("can't parse value "+value+" of property "+k+" Cause: "+exe.getMessage());
		}
		return new Rectangle(0, 0, 0, 0);
	}

	/**
	 * parse line
	 * @param str
	 * @return
	 */
	public static Line2D parseLine(String str){
		Rectangle r = parseRectangle(str);
		Point e     = new Point(r.width,r.height);
		return new Line2D.Double(r.getLocation(),e);
	}
	
	
	/**
	 * parse view position string in whatever format
	 * 
	 * @param str
	 * @return
	 */
	public static ViewPosition parseViewPosition(String str) {
		try {
			int i = 0;
			int x = 0;
			int y = 0;
			float z = 0;
			for (String s : str.split("[^\\d\\.]+")) {
				if (s.length() > 0) {
					if(0 == i)
						x = Integer.parseInt(s);
					else if(1 == i)
						y = Integer.parseInt(s);
					else if(2 == i)
						z = Float.parseFloat(s);
					i++;
				}
			}
			return new ViewPosition(x,y, z);
		} catch (Exception ex) {
			// log.severe("can't parse value "+value+" of property "+k+" Cause: "+exe.getMessage());
		}
		return new ViewPosition(0, 0, 0.0);
	}
	
	
	/**
	 * parse integer
	 * @param value
	 * @return
	 */
	public static Dimension parseDimension(String value){
		// check for max value for size
		if(value.toLowerCase().startsWith("max")){
			return Toolkit.getDefaultToolkit().getScreenSize();
		}
		try{
        	int i = 0;
    		int [] wh = new int [2];
    	
    		// split any string into potential rgb
    		for(String n : value.split("[^\\d]")){
    			if(n.length() > 0 && i < wh.length){
    				 wh[i++] = Integer.parseInt(n);
    			}
    		}
    		
    		return new Dimension(wh[0],wh[1]);
    	}catch(Exception exe){
    		//log.severe("can't parse value "+value+" of property "+k+" Cause: "+exe.getMessage());
    	}
		return new Dimension(0,0);
	}
	
	
	/**
	 * enable/disable abstract buttons inside container unless
	 * they are part of exceptions
	 * @param cont
	 * @param exceptions
	 */
	public static void setEnabled(Container cont,boolean flag){
		for(Component c: (cont instanceof JMenu)?((JMenu)cont).getMenuComponents():cont.getComponents()){
			if(c instanceof JMenu){
				setEnabled((Container)c, flag);
			}else{
				c.setEnabled(flag);
			}
		}
	}
	
	
}
