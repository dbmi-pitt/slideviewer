package edu.pitt.slideviewer.markers;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.LinkedHashSet;

import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import edu.pitt.slideviewer.AnnotationManager;
import edu.pitt.slideviewer.Constants;
import edu.pitt.slideviewer.ViewerHelper;
import edu.pitt.slideviewer.qview.connection.Utils;

public class TextShape  extends Annotation {
	private final int shade = 30;
	private Color background;
	private JLabel label;
	private double scale,area;
	private Font font;
	
	// This class represents rectangle shapes.
	public TextShape(Rectangle r, Color c, boolean hasM) {
		super(r,c,hasM);
		setType("Text");
		Color y = Color.yellow;
		background = new Color(255,255,255,200);
		selectionColor = new Color(y.getRed(),y.getGreen(),y.getBlue(),shade*3);  // Selection color
	
		// init label
		label = new JLabel();
		font = label.getFont();
		setText("right click on the center marker to edit text");
	}
	
	
	
	public void setColor(Color color) {
		super.setColor(color);
		if(label != null)
			label.setForeground(color);
	}



	/**
	 * Determine whether point is inside tutor marker
	 */
	public boolean contains(int x, int y){
		//Rectangle r = new Rectangle(getImgXSt(),getImgXSt(),getImgWidth(),getImgHeight());
		return getBounds().contains(x,y);	
	}
	
	public void drawShape(Graphics g) {
        // return if not visible
        if(!isVisible())
            return;
        
        // get label background
        Rectangle r = getRelativeBounds();
        
        // correct rectangle
        r = Utils.correctRectangle(new Rectangle(r));
        
        g.setColor(background);
		g.fillRoundRect(r.x,r.y,r.width,r.height,15,15);
		
		// don't bother drawing a label when rectangle is too small 
		if((r.width * r.height) < 600)
			return;
		
		// get text and wrap it in html
		String txt = getText();
		String str = "<html><table width="+(r.width)+" height="+(r.height)+">" +
				     "<tr><td align=center valign=middle>"+txt+"</td></tr></table></html>";
		
		//determine font size if scale changed or area changed
		if(getViewer() != null && (scale != getViewer().getScale() || area != r.width * r.height)){
			Font f = font;
			for(float size = 120; size >=6; size--){
				f = font.deriveFont(size);
				FontMetrics fm = g.getFontMetrics(f);
				int h = fm.getHeight();
				int w = fm.stringWidth(txt);
				//System.out.println(w+" "+h);
				// simulate rough wrapping
				if(w > (r.width-5) && r.width > 0){
					h = (int)((h+10)* ((double)w + 2)/(r.width-5));
					w = r.width-5;
				}
				//System.out.println(r+" | "+new Rectangle(r.x,r.y,w,h)+" | "+r.contains(new Rectangle(r.x,r.y,w,h)));
				// if we fit, great
				if(r.contains(new Rectangle(r.x,r.y,w,h))){
					break;
				}
				
			}
			// pick a right font to display the label
			label.setFont(f);
		}
		
		// reset string if necessary
		if(!str.equals(label.getText()))
			label.setText(str);
				
		// draw label
		label.setBounds(r);
		label.paint(g.create(r.x,r.y,r.width,r.height));
		
		// remember scale and area
		if(getViewer() != null)
			scale = getViewer().getScale();
		area = r.width * r.height;
	}

	
	
	public void setText(String text){
		setTags(Collections.singleton(text));
	}
	
	public String getText(){
		return getTag();
	}
	
	
	protected void addMarkers() {
		markers.add(new Marker.CenterMarker(this));
		markers.add(new Marker.NResizeMarker(this));
		markers.add(new Marker.SResizeMarker(this));
		markers.add(new Marker.WResizeMarker(this));
		markers.add(new Marker.EResizeMarker(this));
	}

	
	/**
	 * get context menu for this annotation
	 * @return
	 */
	public JPopupMenu getContextMenu(){
		if(contextMenu == null){
			contextMenu = new JPopupMenu("Edit Annotation");
			
			// add options
			contextMenu.add(ViewerHelper.createMenuItem("Edit Text","Edit Annotation Text",EDIT_ICON,this));
			contextMenu.add(ViewerHelper.createMenuItem("Edit Color","Change Annotation Color",COLOR_ICON,this));
			contextMenu.addSeparator();
			contextMenu.add(ViewerHelper.createMenuItem("Delete","Delete Annotation",DELETE_ICON,this));
		}
		return contextMenu;
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if("Edit Text".equals(cmd) && viewer != null){
			ViewerHelper.MultipleEntryPanel panel = new ViewerHelper.MultipleEntryPanel("Enter Text");
			panel.setEntries(getTags());
			int r = JOptionPane.showConfirmDialog(viewer.getViewerComponent(),panel,
					"Edit Tags",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
			if(JOptionPane.OK_OPTION == r){
				setText(panel.getTextPanel().getText().trim());
				viewer.repaint();
				area = 0;
				pcs.firePropertyChange(Constants.UPDATE_SHAPE_TAGS,null,this);
			}
		}else{
			super.actionPerformed(e);
		}
	}
	
	/**
	 * Sends the bounds back to listeners through the listener.
	 * Property name is "UpdateShape".
	 */
	public void notifyBoundsChange() {
		super.notifyBoundsChange();
		
		// correct rectangle coordinates
		// this method is called after shape has been morphed
		// simply make sure that the bounds are valid
      	if(imageRect != null){
			if(imageRect.width < 0){
				imageRect.x = imageRect.x + imageRect.width;
				imageRect.width =  -imageRect.width;
			}
			
			if(imageRect.height < 0){
				imageRect.y = imageRect.y + imageRect.height;
				imageRect.height =  -imageRect.height;
			}
      	}
	}
	
}

