package edu.pitt.slideviewer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.pitt.slideviewer.markers.Annotation;
import edu.pitt.slideviewer.qview.connection.Utils;

/**
 * image transformation dialog panel
 * @author tseytlin
 */
public class ImageTransformationPanel extends JPanel implements ActionListener, ChangeListener, PropertyChangeListener{
	private final Dimension imageSize = new Dimension(200,200);
	private final String TRANSFORM_16 = "/icons/Transform16.gif";
	private final String CROP_16 = "/icons/Crop16.png";
	private final String CROP_32 = "/icons/Crop32.png";
	private final String BRIGHTNESS_CONTRAST_16 = "/icons/BrightnessContrast16.png";
	private final String ROTATE_CLOCKWISE_32 = "/icons/RotateClockwise32.png";
	private final String ROTATE_CCLOCKWISE_32 = "/icons/RotateCounterClockwise32.png";
	private final String FLIP_VERTICAL_32 = "/icons/FlipVertical32.png";
	private final String FLIP_HORIZONTAL_32 = "/icons/FlipHorizontal32.png";
	private final String UNDO_24 = "/icons/Undo24.gif";
	private final String BRIGHTNESS_32 = "/icons/Brightness32.png";
	private final String CONTRAST_32 = "/icons/Contrast32.png";
		
	private final float BRIGHTNESS_INCREMENT = 255*.05f; // increment in 5% increments
	private final float CONTRAST_INCREMENT   = 1.05f;    // increment in 5% increments
	
	
	private PreviewPanel targetImage;
	private JPanel originalImagePanel,cropPanel;
	private JRootPane viewerPanel;
	private JToggleButton cropButton;
	private ImageTransform imageTransform;
	private BufferedImage originalImage; 
	private JSlider bSlider,cSlider;
	private Viewer viewer,cropViewer;
	private Annotation cropAnnotation;
	private Image croppedImage;
	private Rectangle croppedRectangle;
	
	/**
	 *  initialize transformation panel
	 */
	public ImageTransformationPanel(){
		createUI();
	}
	
	
	private void createUI(){
		setLayout(new BorderLayout());
		
		// create operations tabbed interface
		JTabbedPane tabs = new JTabbedPane();
		tabs.setBorder(new TitledBorder("Transformations and Filters"));
		tabs.addTab("Transform",new ImageIcon(getClass().getResource(TRANSFORM_16)),createFRPanel());
		tabs.addTab("Brightness / Contrast",new ImageIcon(getClass().getResource(BRIGHTNESS_CONTRAST_16)),createBCPanel());
		tabs.addTab("Crop",new ImageIcon(getClass().getResource(CROP_16)),getCropPanel());
		tabs.setPreferredSize(new Dimension(400,350));
		
		
		// create preview panel
		JPanel preview = createPreviewPanel();
		
		JPanel bp = new JPanel();
		bp.setLayout(new BorderLayout());
		bp.setBorder(new EmptyBorder(10,30,10,30));
		
		JButton revert = new JButton("Revert Image");
		revert.setIcon(new ImageIcon(getClass().getResource(UNDO_24)));
		revert.setToolTipText("Revert Image to Original Form");
		revert.setActionCommand("revert");
		revert.addActionListener(this);
		bp.add(revert,BorderLayout.CENTER);
		
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(tabs,BorderLayout.CENTER);
		p.add(bp,BorderLayout.SOUTH);
		
		add(p,BorderLayout.CENTER);
		add(preview,BorderLayout.EAST);
	}

	public void dispose(){
		if(cropViewer != null){
			cropViewer.dispose();
		}
	}
	
	
	/**
	 * create transform panel
	 * @return
	 */
	private JPanel createFRPanel(){
		JPanel p = new JPanel();
		
		int gap = 30;
		p.setLayout(new GridLayout(2,2,gap/2,gap/2));
		//p.setBorder(new CompoundBorder(new TitledBorder("Rotate / Flip Image"),new EmptyBorder(gap,gap,gap,gap)));
		p.setBorder(new EmptyBorder(gap,gap+25,gap,gap+25));
		//p.setPreferredSize(new Dimension(350,350));
		
		// clockwise rotation
		JButton crotate = new JButton("<html><center>Rotate<p>Right</center>");
		crotate.setIcon(new ImageIcon(getClass().getResource(ROTATE_CLOCKWISE_32)));
		crotate.setHorizontalTextPosition(JButton.CENTER);
		crotate.setVerticalTextPosition(JButton.BOTTOM);
		crotate.addActionListener(this);
		crotate.setActionCommand("rotate-right");
		
		// clockwise rotation
		JButton ccrotate = new JButton("<html><center>Rotate<p>Left</center>");
		ccrotate.setIcon(new ImageIcon(getClass().getResource(ROTATE_CCLOCKWISE_32)));
		ccrotate.setHorizontalTextPosition(JButton.CENTER);
		ccrotate.setVerticalTextPosition(JButton.BOTTOM);
		ccrotate.addActionListener(this);
		ccrotate.setActionCommand("rotate-left");
		
		
		// vertical flip
		JButton vflip = new JButton("<html><center>Flip<p>Vertically</center>");
		vflip.setIcon(new ImageIcon(getClass().getResource(FLIP_VERTICAL_32)));
		vflip.setHorizontalTextPosition(JButton.CENTER);
		vflip.setVerticalTextPosition(JButton.BOTTOM);
		vflip.addActionListener(this);
		vflip.setActionCommand("flip-vertical");
	
		// hortizontal flip
		JButton hflip = new JButton("<html><center>Flip<p>Horizontally</center>");
		hflip.setIcon(new ImageIcon(getClass().getResource(FLIP_HORIZONTAL_32)));
		hflip.setHorizontalTextPosition(JButton.CENTER);
		hflip.setVerticalTextPosition(JButton.BOTTOM);
		hflip.addActionListener(this);
		hflip.setActionCommand("flip-horizontal");
		
		p.add(crotate);
		p.add(ccrotate);
		p.add(vflip);
		p.add(hflip);
		
		
		// setup preview
		//		JPanel panel = new JPanel();
		//		panel.setLayout(new BorderLayout());
		//		panel.add(p,BorderLayout.CENTER);
		//		panel.add(createPreviewPanel(),BorderLayout.EAST);
		
		return p;
	}
	
	
	private JPanel createCropPanel(){
		JPanel p =  new JPanel();
		p.setLayout(new BorderLayout());
		
		// add crop button 
		cropButton = new JToggleButton("Crop Image");
		cropButton.setIcon(new ImageIcon(getClass().getResource(CROP_16)));
		cropButton.addActionListener(this);
		cropButton.setActionCommand("crop");
		cropButton.setToolTipText("<html>select a rectangular region<br>to remove edge areas from the image");
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout());
		buttons.setBackground(Color.white);
		buttons.add(cropButton);
		p.add(buttons,BorderLayout.NORTH);
		p.add(createViewerPanel(),BorderLayout.CENTER);
		
		return p;
	}
	
	private JPanel getCropPanel(){
		if(cropPanel == null)
			cropPanel = createCropPanel();
		return cropPanel;
	}
	
	
	private JComponent createViewerPanel(){
		// just in case
		if(viewer == null)
			return new JPanel();
		
		// dispose of previous viewer
		if(cropViewer != null)
			cropViewer.dispose();
		cropAnnotation = null;
		
		// clone existing viewer
		cropViewer = viewer.clone();
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				cropViewer.getViewerController().resetZoom();
			}
		});
		
		// add cropping panel
		viewerPanel = new JRootPane();
		viewerPanel.getContentPane().setLayout(new BorderLayout());
		viewerPanel.getContentPane().add(cropViewer.getViewerComponent(),BorderLayout.CENTER);
		viewerPanel.setGlassPane(new CropPanel());
		
		return viewerPanel;
	}
	
	
	private JPanel createPreviewPanel(){
		// create preview panel
		JPanel preview = new JPanel();
		preview.setLayout(new BoxLayout(preview, BoxLayout.Y_AXIS));
		
		// original image
		originalImagePanel = new JPanel();
		originalImagePanel.setBorder(new TitledBorder("Original Image"));
		originalImagePanel.setLayout(new BorderLayout());
		originalImagePanel.add(new JLabel(new ImageIcon(getOriginalImage())));
		originalImagePanel.setPreferredSize(new Dimension(210,230));
		
		// target image
		JPanel targetImagePanel = new JPanel();
		targetImagePanel.setBorder(new TitledBorder("Target Image"));
		targetImagePanel.setPreferredSize(new Dimension(210,230));
		targetImagePanel.setLayout(new BorderLayout());
		targetImage = new PreviewPanel();
		targetImagePanel.add(targetImage);
		
		
		preview.add(targetImagePanel);
		preview.add(originalImagePanel);
		
		return preview;
	}
	
	
	private JPanel createBCPanel(){
		JPanel p =  new JPanel();
		int gap = 30;
		p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
		//p.setBorder(new CompoundBorder(new TitledBorder("Brightness / Contrast"),new EmptyBorder(gap,gap,gap,gap)));
		p.setBorder(new EmptyBorder(gap,gap,gap,gap));
		
		
		// add brighness
		p.add(new JLabel("Brightness",new ImageIcon(getClass().getResource(BRIGHTNESS_32)),JLabel.TRAILING));
		bSlider = new JSlider();
		bSlider.setSnapToTicks(true);
		bSlider.setMajorTickSpacing(20);
		bSlider.setMinorTickSpacing(1);
		bSlider.setPaintTicks(true);
		bSlider.addChangeListener(this);
		bSlider.setMinimum(-20);
		bSlider.setMaximum(20);
		bSlider.setValue(0);
		
		p.add(bSlider);
		
		// separator
		p.add(Box.createRigidArea(new Dimension(40,40)));
		
		// add contrast
		p.add(new JLabel("Contrast",new ImageIcon(getClass().getResource(CONTRAST_32)),JLabel.TRAILING));
		cSlider = new JSlider();
		cSlider.setSnapToTicks(true);
		cSlider.addChangeListener(this);
		cSlider.setMajorTickSpacing(20);
		cSlider.setMinorTickSpacing(1);
		cSlider.setMinimum(-20);
		cSlider.setMaximum(20);
		cSlider.setValue(0);
		cSlider.setPaintTicks(true);
		p.add(cSlider);
		
		
		// setup preview
		//		JPanel panel = new JPanel();
		//		panel.setLayout(new BorderLayout());
		//		panel.add(p,BorderLayout.CENTER);
		//		panel.add(createPreviewPanel(),BorderLayout.EAST);
		//		
		return p;
	}
	
	
	/**
	 * set original preview image
	 * @param img
	 */
	public void setOriginalImage(Image img){
		int width = imageSize.width;
		int height = imageSize.height;
	
		// cut out an intersring part of the image
		int w = img.getWidth(null);
		int h = img.getHeight(null);
		Rectangle o = new Rectangle(0,0,imageSize.width,imageSize.height);
		if(w > h){
			o.height = h * o.width / w; 
			o.y = (int) ((imageSize.height - o.height) / 2.0);
		}else{
			o.width = w * o.height / h; 
			o.x = (int) ((imageSize.width - o.width) / 2.0);
		}
		
		// create original image
		originalImage = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		originalImage.getGraphics().setColor(Color.white);
		originalImage.getGraphics().fillRect(0,0, width, height);
		originalImage.getGraphics().drawImage(img,o.x,o.y,o.width,o.height,Color.white,null);
		
		// now repaint original
		originalImagePanel.removeAll();
		originalImagePanel.add(new JLabel(new ImageIcon(getOriginalImage())));
	}
	
	public void setViewer(Viewer viewer) {
		this.viewer = viewer;
		
		// if not the same image is opened we need to reload viewer UI
		if(cropViewer == null || !viewer.getImage().equals(cropViewer.getImage())){
			JPanel cp = getCropPanel();
			cp.remove(1);
			cp.add(createViewerPanel());
			cp.revalidate();
			cp.repaint();
			
		}
	}
	
	/**
	 * get original image
	 * @return
	 */
	public Image getOriginalImage(){
		if(originalImage == null){
			// create some original image
			int width = imageSize.width;
			int height = imageSize.height;
			originalImage = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
			Graphics2D g = originalImage.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(Color.white);
			g.fillRect(0,0, width, height);
			g.setColor(Color.pink);
			g.fillOval(width/4,height/4, width/2, height/2);
			g.setColor(Color.black);
			g.setStroke(new BasicStroke(3.0f));
			g.drawRect(3,3, width-6, height-6);
			g.drawOval(width/4,height/4, width/2, height/2);
			g.setFont(g.getFont().deriveFont(Font.BOLD,25f));
			g.drawString("N",width/2-10,30);
			g.setColor(Color.lightGray);
			g.fillPolygon(new int [] {0,width/2,0},new int [] {0,height/2,height/2}, 3);
			
		}
		return originalImage;
	}
	
	
	/**
	 * create a cropped image
	 * @return
	 */
	private Image getCroppedImage() {
		if(cropViewer != null && cropAnnotation != null && cropAnnotation.isVisible()){
			
			// if crop annotation is within bounds then draw whats on screen,
			// else cut it out from thumbnail
			if(cropViewer.getViewerComponent().isShowing() && 
			   cropViewer.getViewerComponent().getBounds().contains(cropAnnotation.getRelativeBounds())){
				// get screenshot
				croppedImage = cropViewer.getSnapshot(Viewer.IMG_NO_MARKERS);
				croppedRectangle = new Rectangle(cropAnnotation.getRelativeBounds());
			}else if(!cropAnnotation.getRelativeBounds().equals(croppedRectangle)){
				// crop out the image
				croppedImage = cropViewer.getImageProperties().getThumbnail();
				Dimension is = cropViewer.getImageSize();
				Dimension ts = new Dimension(croppedImage.getWidth(null),croppedImage.getHeight(null));
				Rectangle r = new Rectangle(cropAnnotation.getBounds());
				croppedRectangle = r;				
				
				// convert to relative the cropping rectangle
				r.width = r.width * ts.width / is.width;
				r.height = r.height * ts.height / is.height;
				r.x = r.x * ts.width/is.width;
				r.y = r.y * ts.height/is.height;
			}
			
			// create appropriate drawing image
			Rectangle o = new Rectangle(0,0,imageSize.width,imageSize.height);
			Rectangle r = croppedRectangle;
			if(r.width > r.height){
				o.height = r.height * o.width / r.width; 
				o.y = (int) ((imageSize.getHeight() - o.getHeight()) / 2.0);
			}else{
				o.width = r.width * o.height / r.height; 
				o.x = (int) ((imageSize.width - o.width) / 2.0);
			}
			
			// create image
			BufferedImage cropImage = new BufferedImage(imageSize.width,imageSize.height,BufferedImage.TYPE_INT_RGB);
			Graphics2D g = cropImage.createGraphics();
			g.setColor(Color.white);
			g.fillRect(0,0,imageSize.width,imageSize.height);
			g.drawImage(croppedImage,o.x,o.y,o.x+o.width,o.y+o.height,r.x, r.y,r.x+r.width,r.y+r.height,Color.white,null);
			return cropImage;
			
		}
		return null;
	}

	
	
	/**
	 * get target image
	 * @return
	 */
	public Image getTargetImage(){
		ImageTransform it = getImageTransform();
		Image img = getOriginalImage();
		if(it.isCropped())
			img = getCroppedImage();
		return it.getTransformedImage(img);
	}
	
	

	/**
	 * get image transform
	 * @return
	 */
	public ImageTransform getImageTransform() {
		if(imageTransform == null)
			imageTransform = new ImageTransform();
		return imageTransform;
	}

	
	/**
	 * set image transform
	 * @param imageTransform
	 */
	public void setImageTransform(ImageTransform imageTransform) {
		this.imageTransform = imageTransform;
		
		// brightness
		bSlider.setValue((int)(imageTransform.getBrightness()/BRIGHTNESS_INCREMENT));
				
		// set contrast
		cSlider.setValue((int)(Math.log(imageTransform.getContrast())/Math.log(CONTRAST_INCREMENT)));
		
		// set cropping
		cropButton.setSelected(imageTransform.isCropped());
		if(cropButton.isSelected())
			doCrop();
				
		// repaint image
		if(targetImage != null)
			targetImage.repaint();
	}

	
	/**
	 * display image
	 * @author tseytlin
	 */
	private class PreviewPanel extends JPanel {
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(getTargetImage(),0,0,null);
		}
	}
	
	/**
	 * display image
	 * @author tseytlin
	 */
	private class CropPanel extends JPanel {
		private Color background;
		private AlphaComposite composite;
		
		public CropPanel(){
			super();
			setOpaque(false);
			setFocusable(false);
			
			background = Color.gray;
			composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.6f);
		}
		
		public boolean contains(int x, int y){
			return false;
		}
		
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Dimension d = getSize();
		
			// fill rectangle
			Graphics2D g2 = (Graphics2D) g;
			g2.setComposite(composite);
			g.setColor(background);
			
			// make a clear window
			if(cropAnnotation != null && cropAnnotation.isVisible()){
				Rectangle r = cropAnnotation.getRelativeBounds();
			
				// horizontal lines
				g.fillRect(0,0,d.width,r.y);
				g.fillRect(0,r.y+r.height,d.width,d.height-r.y-r.height);
				
				// two sides
				g.fillRect(0,r.y,r.x,r.height);
				g.fillRect(r.x+r.width,r.y,d.width-r.x-r.width,r.height);
			}
		}
	}
	
	/**
	 * perform cropping action
	 */
	
	private void doCrop(){
		if(cropViewer == null)
			return;
		AnnotationManager am = cropViewer.getAnnotationManager();
		if(cropButton.isSelected()){
			if(cropAnnotation != null)
				cropAnnotation.removePropertyChangeListener(this);
			am.removeAnnotations();
			
			// create an initial annotation
			Dimension d = cropViewer.getImageSize();
			Rectangle v = Utils.correctRectangle(d,cropViewer.getViewRectangle());
			Rectangle r = (imageTransform.isCropped())?
							new Rectangle(imageTransform.getCropRectangle()):
							new Rectangle(v.x+v.width/4,v.y+v.height/4,v.width/2,v.height/2);
							
			cropAnnotation = am.createAnnotation(AnnotationManager.SQUARE_SHAPE,true);
			cropAnnotation.setEditable(true);
			cropAnnotation.setColor(Color.gray);
			cropAnnotation.setBounds(r);
			cropAnnotation.addPropertyChangeListener(this);
			am.addAnnotation(cropAnnotation);
			
			viewerPanel.getGlassPane().setVisible(true);
			getImageTransform().setCropRectangle(cropAnnotation.getBounds());
		}else{
			viewerPanel.getGlassPane().setVisible(false);
			am.removeAnnotations();
			getImageTransform().setCropRectangle(null);
		}
		updateTarget();
	}
	
	
	/**
	 * handle actions
	 */
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		
		// check if we are cropping
		if("crop".equals(cmd) && cropViewer != null){
			doCrop();
			return;
		}
		
		ImageTransform it = getImageTransform();
		boolean even = it.getRotationTransform()%2 == 0;
		int flip = 0;
		
		if("rotate-right".equals(cmd)){
			it.setRotationTransform(it.getRotationTransform()+1);
		}else if("rotate-left".equals(cmd)){
			it.setRotationTransform(it.getRotationTransform()-1);
		}else if("flip-vertical".equals(cmd)){
			flip = (even)?-1:1;
			//it.setFlipTransform((it.getFlipTransform()-((even)?1:-1))%2);
		}else if("flip-horizontal".equals(cmd)){
			flip = (even)?1:-1;
			//it.setFlipTransform((it.getFlipTransform()+((even)?1:-1))%2);
		}else if("revert".equals(cmd)){
			it.reset();
			bSlider.setValue(0);
			cSlider.setValue(0);
			cropButton.setSelected(false);
			doCrop();
		}
				
		// if slide has already been flipped in different direction, then we need to do a rotate
		if((it.getFlipTransform() > 0 && flip < 0) || it.getFlipTransform() < 0 && flip > 0){
			it.setFlipTransform(0);
			it.setRotationTransform(it.getRotationTransform()+2);
		// else if we do have a flip, simply do what we did before
		}else if(flip != 0){
			it.setFlipTransform((it.getFlipTransform()+flip)%2);
		}
		
		updateTarget();
	}
	
	private void updateTarget(){
		// update cropViewer
		/*
		if(cropViewer != null){
			ImageTransform it = getImageTransform().clone();
			it.setCropRectangle(null);
			cropViewer.getImageProperties().setImageTransform(it);
			cropViewer.update();
		}
		*/
		// repaint image
		if(targetImage != null){
			targetImage.repaint();
		}
	}

	public void stateChanged(ChangeEvent e) {
		if(e.getSource() == bSlider){
			getImageTransform().setBrightness(BRIGHTNESS_INCREMENT*bSlider.getValue());
		}else if(e.getSource() == cSlider){
			getImageTransform().setContrast((float)Math.pow(CONTRAST_INCREMENT,cSlider.getValue()));
		}
		
		// repaint image
		updateTarget();
	}
	

	public void propertyChange(PropertyChangeEvent evt) {
		if(Constants.UPDATE_SHAPE.equals(evt.getPropertyName())){
			if(cropAnnotation != null)
				getImageTransform().setCropRectangle(cropAnnotation.getBounds());
			
			// update target image
			updateTarget();
		}
	}	
}
