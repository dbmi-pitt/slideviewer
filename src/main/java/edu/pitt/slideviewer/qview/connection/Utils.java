package edu.pitt.slideviewer.qview.connection;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.pitt.slideviewer.ImageProperties;
import edu.pitt.slideviewer.ImageTransform;
import edu.pitt.slideviewer.ViewerHelper;


/**
 * contains varies utility methods
 * @author tseytlin
 */
public class Utils {
	private static Component component;
	private static MediaTracker tracker;
	
	/**
	 * Provide Utils with any active component, so that
	 * MediaTracker and other things could be instanciated
	 * @param c
	 */
	public static void setComponent(Component c){
		component = c;
		if(c == null && tracker != null){
			tracker = null;
		}else
			tracker = new MediaTracker(c);
	}
	
	/**
     * convert simple image into buffered image
     * @param img
     * @return
     */
    public static BufferedImage createBufferedImage(Image img){
		if(img == null || img.getWidth(null) < 0)
			return null;
    	
		// why convert it if you have it
		if(img instanceof BufferedImage)
			return (BufferedImage) img;
		
    	//long time = System.currentTimeMillis();
		BufferedImage bi = new BufferedImage(img.getWidth(null),img.getHeight(null),BufferedImage.TYPE_INT_RGB);
		bi.createGraphics().drawImage(img,0,0,null);
		//System.out.println("buffered img convert time "+(System.currentTimeMillis()-time)+" ms");
		return bi;
    }
	
    /**
     * makes sure we don't have negative dimensions
     * @param d
     */
    public static void checkDimensions(Dimension d){
    	if(d.width <= 0)
    		d.width = 1;
    	if(d.height <= 0)
    		d.height = 1;
    }
    
    /**
     * convert simple image into volotile image
     * @param img
     * @return
     */
    public static VolatileImage createVolatileImage(Image img){
    	if(img == null || img.getWidth(null) < 0)
			return null;
    	
    	//long time = System.currentTimeMillis();
    	VolatileImage vi = component.createVolatileImage(img.getWidth(null),img.getHeight(null));
    	if(vi != null)
    		vi.getGraphics().drawImage(img,0,0,null);
    	//System.out.println("volotile img convert time "+(System.currentTimeMillis()-time)+" ms");
    	return vi;
    }
    
    /**
     * Do some sanity checking for input rectangle
     * return a "correct" copy
     * @param image size in absolut coordinates
     * @param view that is requested
     * @return
     */
    public static Rectangle correctRectangle(Dimension idim, Rectangle view){
        Rectangle r = new Rectangle(view);
        // do some sanity checking for bounds
        if(r.x < 0)
            r.x = 0;
        
        if(r.y < 0)
            r.y = 0;
        
        if(r.width > idim.width){
            r.width = idim.width;
            //sw = (int)((sh * r.width)/r.height);
        }
        if(r.height > idim.height){
            r.height = idim.height;
            //sh = (int)((sw * r.height)/r.width);
        }
        return r;
    }
	
    
    /**
     * Do some sanity checking for input rectangle
     * This method simply makes sure there are no negative sides
     * @param view that is requested
     * @return
     */
    public static Rectangle correctRectangle(Rectangle r){
        if(r == null)
        	return null;
  
        if(r.width <  0){
        	r.x = r.x + r.width;
            r.width = - r.width;
        }
        
        if(r.height <  0){
        	r.y = r.y + r.height;
            r.height = - r.height;
        }
   
        return r;
    }
    
    /**
     * Make sure image loads!!!!
     * @param img
     */
    public static void flushImage(Image img){
    	if(tracker != null && img != null){
    		// flush image to memory, if there are several viewers
    		// working at once, the tracker may become invalid when
    		// setComponent method is invoked, hence if there are
    		// any problems, simply catch them and ignore them
    		// this method is usefull, but once in a blue moon
    		// it can misfire.
    		try{
    			tracker.addImage(img,0);
		    	tracker.waitForAll();
		    	tracker.removeImage(img,0);
    		}catch(Exception ex){}
    	}
    }
  
    
    /**
     * get rectangle that corresponds to layout on the buffer
     * normally it would be 0,0, width, height, but on lower rez
     * of non square image it could be different
     * @param viewerSize
     * @param imageSize
     * @param viewSize
     * @param corrected size
     * @return
     */
    public static Rectangle getLayoutRectangle(Dimension dim,Rectangle view, Rectangle r){
    	int sw = (dim.width < view.width)?dim.width:view.width;
		int sh = (dim.height < view.height)?dim.height:view.height;
		//int sw = dim.width;
		//int sh = dim.height;
		
		// do some sanity checking for bounds in case image is smaller
		// then viewing window
		if (r.width < view.width) {
			sw = (int) ((sh * r.width) / r.height);
		}
		if (r.height < view.height) {
			sh = (int) ((sw * r.height) / r.width);
		}
		
			
		// calculate offset of main image region in case it is smaller
		// then viewing window
		int offx = (int) ((dim.width - sw) / 2);
		int offy = (int) ((dim.height - sh) / 2);
		return new Rectangle(offx,offy,sw,sh);
    }
    
    
    /**
     * Draw given tile on top of the graphics
     * @param tile - source tile
     * @param g  - graphics context
     * @param br - size of the region (buffer) or viewing area
     * @param bscale - scale of the region(buffer or viewing area
     */
    public static void drawTile(Tile tile, Graphics g, Rectangle br, double bscale){
  		// long time = System.currentTimeMillis();
    	
		// draw tile on top of buffer
		Rectangle tr = tile.getBounds();
		double tscale = tile.getScale();
		Image img = tile.getImage();
		
		// System.out.println(br+" "+tr);
		int x = (int) ((tr.x - br.x) * bscale);
		int y = (int) ((tr.y - br.y) * bscale);
		int w = img.getWidth(null);
		int h = img.getHeight(null);

		// scale image if necessary
		if (bscale != tscale) {
			w = (int) (w * bscale / tscale);
			h = (int) (h * bscale / tscale);
		}
		g.drawImage(img,x,y,x+w,y+h,0,0,img.getWidth(null),img.getHeight(null),Color.white,null);
    }
    
    /**
     * transform tile, based on parameters
     * @param tile
     * @param r
     * @param rotate
     * @param flip
     *
    public static void transofrmTile(Tile tile,Rectangle r, int rotate, int flip){
    	if(tile == null || (rotate == 0 && flip == 0))
    		return;
    	tile.setOriginalBounds(tile.getBounds());
    	tile.setBounds(r);
		tile.setRotationTransform(rotate);
    	tile.setFlipTransform(flip);
    	//tile.setImage(getTransformedImage(tile.getImage(),rotate,flip));
    }
    */
  
    /**
     * transform tile, based on parameters
     * @param tile
     * @param r
     * @param rotate
     * @param flip
     */
    public static void transofrmTile(Tile tile,Rectangle r, ImageTransform transform){
    	if(tile == null || transform.isIdentity())
    		return;
    	tile.setOriginalBounds(tile.getBounds());
    	tile.setBounds(r);
		tile.setImageTransform(transform);
    	//tile.setImage(getTransformedImage(tile.getImage(),rotate,flip));
    }
    
    
    /**
     * get transformed dimensions
     * @param d
     * @param rotate
     * @return
     */
    public static Dimension getRotatedDimension(Dimension d, int rotate){
    	 if(rotate %2 == 0)
         	return d;
         // else flip dimensions because of rotation
         return new Dimension(d.height,d.width);
    }
    
    /**
     * Scale input image to w/h keeping the same aspect ratio
     * @param Image src image
     * @param int w - target width
     * @param int h - target height
     * @return Image result image
     */
    public static Image scaleImage(Image img, int w, int h){
    	boolean isHorizontal = img.getWidth(null) >= img.getHeight(null);
    	if(img != null){
    		if(isHorizontal){
	        	h = (int)((w * img.getHeight(null))/img.getWidth(null));
	        }else{
	        	w = (int)((h * img.getWidth(null))/img.getHeight(null));
	        }
    		return img.getScaledInstance(w,h,Image.SCALE_DEFAULT);
    	}else
    		return null;
    }
    
    
    /**
     * Calculate which side of a rectanlge should be used to calculate
     * the other based on image and window dimensions
     * @param size - size of viewing window
     * @param isize - absolute size of image
     * @return - true if horizontal side to be used
     */
    
    public static boolean isHorizontal(Dimension size, Dimension isize){
    	return ViewerHelper.isHorizontal(size, isize);
    }

	/**
	 * @return the component
	 */
	public static Component getComponent() {
		return component;
	}
	
	
	/**
	 * rotate view/tile rectangle around image center.
	 * This is usefull to enable dynamic image rotation
	 * @param Rectangle r - original view rectangle (absolute coordinates)
	 * @param Dimension d - original image dimensions
	 * @param int rotate  - quadrant rotation Ex: 1 = 90' 2 = 180', -1 = -90' (270') ..
	 * @return Rectangle r - rectangle in rotated plane
	 */
	public static Rectangle getRotatedRectangle(Rectangle r, Dimension d, int rotate){
		// make sure rotation is consistent
		// that is you can only have rotation of 1, -1 and 2, -2
		rotate = rotate % 4;
		if(Math.abs(rotate) == 3)
			rotate = (rotate > 0)?rotate-4:rotate+4;
		
		// don't need to do anything if nothing needs to be rotated
		if(rotate == 0)
			return r;
	
		// setup rotation
		AffineTransform t = AffineTransform.getRotateInstance(rotate*Math.PI/2,d.getWidth()/2,d.getHeight()/2);
		
		// change width/height
		int width  = r.width;
		int height = r.height;
		if(rotate % 2 != 0){
			width  = r.height;
			height = r.width;
		}
		
		
		// calculate offset cause, we are going outside of raster image
		// if that image is flipped odd number of times
		int offset = 0;
		if(rotate%2 != 0){
			offset = (int) Math.abs((d.getWidth() - d.getHeight())/2.0);
			// if horizontal and negative rotation OR
			// if vertical and positive rotation then inverse
			if((((d.width - d.height) > 0) && rotate < 0)||
			   (((d.width - d.height) < 0) && rotate > 0))
				offset = -offset;
		}
		// rotate location of rectangle
		Point p = new Point();
		t.transform(r.getLocation(),p);
		
		// compensate for offset and location change
		int x = p.x + offset-((rotate ==  1 || Math.abs(rotate) == 2)?width:0);
		int y = p.y + offset-((rotate == -1 || Math.abs(rotate) == 2)?height:0);
		
		return new Rectangle(x,y,width,height);
	}
	
	
	/**
	 * rotate view/tile rectangle around image center.
	 * This is usefull to enable dynamic image rotation
	 * @param Rectangle r - original view rectangle (absolute coordinates)
	 * @param Dimension d - original image dimensions
	 * @param int rotate  - quadrant rotation Ex: 1 = 90' 2 = 180', -1 = -90' (270') ..
	 * @return Rectangle r - rectangle in rotated plane
	 */
	public static Point getRotatedPoint(Point pt, Dimension d, int rotate){
		// make sure rotation is consistent
		// that is you can only have rotation of 1, -1 and 2, -2
		rotate = rotate % 4;
		if(Math.abs(rotate) == 3)
			rotate = (rotate > 0)?rotate-4:rotate+4;
		
		// don't need to do anything if nothing needs to be rotated
		if(rotate == 0)
			return pt;
	
		// setup rotation
		AffineTransform t = AffineTransform.getRotateInstance(rotate*Math.PI/2,d.getWidth()/2,d.getHeight()/2);
				
		// calculate offset cause, we are going outside of raster image
		// if that image is flipped odd number of times
		int offset = 0;
		if(rotate%2 != 0){
			offset = (int) Math.abs((d.getWidth() - d.getHeight())/2.0);
			// if horizontal and negative rotation OR
			// if vertical and positive rotation then inverse
			if((((d.width - d.height) > 0) && rotate < 0)||
			   (((d.width - d.height) < 0) && rotate > 0))
				offset = -offset;
		}
		// rotate location of rectangle
		Point p = new Point();
		t.transform(pt,p);
		
		// compensate for offset and location change
		//int x = p.x + offset;
		//int y = p.y + offset;
		
		int x = p.x +((rotate ==  1 || Math.abs(rotate) == 2)?-offset:offset);
		int y = p.y +((rotate == -1 || Math.abs(rotate) == 2)?-offset:offset);
		
		return new Point(x,y);
	}
	
	
	
	/**
	 * flip view/tile rectangle around vertical or horizontal axis.
	 * This is usefull to enable dynamic image flipping
	 * @param Rectangle r - original view rectangle (absolute coordinates)
	 * @param Dimension d - original image dimensions
	 * @param int flip    - flip > 0 = HORIZONTAL_FLIP, flip < 0 -> VERTICAL_FLIP, 0 no filp.
	 * @return Rectangle r - flipped rectangle
	 */
	public static Rectangle getFlippedRectangle(Rectangle r, Dimension d, int flip){
		// don't need to do anything if nothing needs to be rotated
		if(flip == 0)
			return r;
	
		// setup rotation
		AffineTransform tx = null;
		if(flip > 0){
			tx = AffineTransform.getScaleInstance(-1, 1);
			tx.translate(-d.getWidth(), 0);
		}else {
			tx = AffineTransform.getScaleInstance(1,-1);
			tx.translate(0,-d.getHeight());
		}
		// rotate location of rectangle
		Point p = new Point();
		tx.transform(r.getLocation(),p);
		
		// compensate for offset and location change
		int x = p.x -((flip > 0)?r.width:0);
		int y = p.y -((flip < 0)?r.height:0);
		
		return new Rectangle(x,y,r.width,r.height);
	}
	
	/**
	 * flip view/tile point around vertical or horizontal axis.
	 * This is usefull to enable dynamic image flipping
	 * @param Point p - original view rectangle (absolute coordinates)
	 * @param Dimension d - original image dimensions
	 * @param int flip    - flip > 0 = HORIZONTAL_FLIP, flip < 0 -> VERTICAL_FLIP, 0 no filp.
	 * @return Rectangle r - flipped rectangle
	 */
	public static Point getFlippedPoint(Point pt, Dimension d, int flip){
		// don't need to do anything if nothing needs to be rotated
		if(flip == 0)
			return pt;
	
		// setup rotation
		AffineTransform tx = null;
		if(flip > 0){
			tx = AffineTransform.getScaleInstance(-1, 1);
			tx.translate(-d.getWidth(), 0);
		}else {
			tx = AffineTransform.getScaleInstance(1,-1);
			tx.translate(0,-d.getHeight());
		}
		// rotate location of rectangle
		Point p = new Point();
		tx.transform(pt,p);

		return p;
	}
	
	
	/**
	 * Create a transformed image based on quadrant rotation and flip operations.
	 * If rotate and flip transforms are 0, original image is returned
	 * @param image - original image
	 * @param rotate - quadrant rotation Ex: 1 = 90' 2 = 180', -1 = -90' (270') ..
	 * @param int flip    - flip > 0 = HORIZONTAL_FLIP, flip < 0 = VERTICAL_FLIP, 0 no filp.
	 * @return image - transformed image
	 */
	public static Image getTransformedImage(Image image, int rotate, int flip){
		// don't do anything, if nothing needs to be done
		if((rotate%4 == 0 && flip == 0) || image == null)
			return image;
		
		// make sure rotation is consistent
		// that is you can only have rotation of 1, -1 and 2, -2
		rotate = rotate % 4;
		if(Math.abs(rotate) == 3)
			rotate = (rotate > 0)?rotate-4:rotate+4;
				
		// else do transformations
		int width = image.getWidth(null);
		int height = image.getHeight(null);
		
		if(width < 0 || height < 0)
			return image;
		
		AffineTransform tx = new AffineTransform();
		
		// rotate image if necessary
		if(rotate%4 != 0)
			tx.rotate(rotate*Math.PI/2,width/2.0,height/2.0);
		
		// change width and height based on rotation
		int w = width;
	    int h = height;
		if(rotate % 2 != 0){
	    	w = height;
	    	h = width;
	    }
		int s = (rotate>0)?1:-1;
		
		// flip image if necessary
		if(flip > 0){
			tx.scale(-1, 1);
			tx.translate(-width-s*(width-w),0);
		}else if(flip  < 0){
			tx.scale(1,-1);
			tx.translate(0,-height+s*(height-h));
		}
		
		BufferedImage img = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.white);
		g.fillRect(0,0,w,h);
		g.setTransform(tx);
		int x =  s*(width-w)/2;
		int y = -s*(height-h)/2;
		g.drawImage(image,x,y,null);
		return img;
	}

	
	/**
	 * Create a transformed image based on channel mapps
	 * @param origMap - original channel map
	 * @param newMap  - new channel map
	 * @return image - transformed image
	 */
	public static Image getTransformedImage(Image image, Map<String, Color> origMap,Map<String, Color> newMap){
		/*if(origMap != null && newMap != null){
			BufferedImage bimg  =createBufferedImage(image);
			for(int i=0;i<bimg.getWidth();i++){
				for(int j=0;j<bimg.getHeight();j++){
					bimg.setRGB(i,j,convertPixel(bimg.getRGB(i,j),origMap,newMap));
				}
			}
			return bimg;
		}*/
		return image;
	}
	
	/**
	 * convert a single pixel rgb value from orig color space to a new one
	 * @param rgb
	 * @param origMap
	 * @param newMap
	 * @return
	 *
	private static int convertPixel(int rgb, Map<String, Color> origMap, Map<String, Color> newMap) {
		
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		
		//TODO: ???????
		// go over channels
		for(String ch: origMap.keySet()){
			Color c = origMap.get(ch);
			int a = rgb & c.getRGB();
			
		}
	
		return rgb & 0xFFFF00;
	}
		*/

	/**
	 * get rectangle from the original image, taking into account image transformations
	 * done in the parameter image transform
	 * @param r
	 * @param ip
	 * @return
	 */
	public static Rectangle getTransformedRectangle(Rectangle r, ImageProperties ip){
		if(ip == null)
			return r;
		
		Rectangle rect = new Rectangle(r);
		ImageTransform tr = ip.getImageTransform();
				
		// compensate for cropping
		if(tr.getCropRectangle() != null){
			Point crop = tr.getCropOffset();
			rect.x += crop.x;
			rect.y += crop.y;
		}
		
		// compensate for rotation and flipping
		int rotate = tr.getRotationTransform();
		int flip   = tr.getFlipTransform();
		
		// get original dimensions if possible
		Dimension d =  (ip instanceof ImageInfo)?((ImageInfo)ip).getOriginalImageSize():ip.getImageSize();
		
		// rotate and flip
		rect = Utils.getRotatedRectangle(rect, d,-rotate);
		rect = Utils.getFlippedRectangle(rect, d,flip);
		
		
		return rect;
	}
	
	
	/**
	 * filter URL
	 * @param str
	 * @return
	 */
	public static String filterURL(String str){
		try{
			return URLEncoder.encode(str,"utf-8");
		}catch(UnsupportedEncodingException ex){
			return str.replaceAll("\\s","%20");
		}
	}
	
	  /**
     * get transformed delta point to move objects on the screen
     * @param delta
     * @return
     */
    public static Point getTransformedDelta(Point delta, ImageTransform it){
    	// do transformations
    	if(!it.isIdentity()){
    		int rotate = it.getRotationTransform() % 4;
    		int flip   = it.getFlipTransform();		
    		
    		// if rotation is odd then switch x and y places
    		if(rotate%2 != 0){
    			delta = new Point(delta.y,delta.x);
    		}
    	
    		// now compensate for rotation and flipping
    		
    		// 180 flip
    		if(Math.abs(rotate) == 2)
    			delta = new Point(-delta.x,-delta.y);
    		// if 90 or -270
    		else if(rotate == 1 || rotate == -3)
    			delta.y = - delta.y;
    		// if -90 or 270
    		else if(rotate == -1 || rotate == 3)
    			delta.x = - delta.x;
    		
    		// horizontal flip
    		if(flip > 0)
    			delta.x = - delta.x;
    		// vertical flip
    		else if(flip < 0)
    			delta.y = - delta.y;
    				
    	}
    	return delta;
    }
	

	
	/**
	 * parse XML document
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static Document parseXML(InputStream in) throws IOException {
		Document document = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//factory.setValidating(true);
		//factory.setNamespaceAware(true);

		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			//builder.setErrorHandler(new XmlErrorHandler());
			//builder.setEntityResolver(new XmlEntityResolver());
			document = builder.parse(in);
			
			// close input stream
			in.close();
		}catch(Exception ex){
			throw new IOException(ex.getMessage());
		}
		return document;
	}
	
	/**
	 * format XML into human readable form
	 * @param document
	 * @param root
	 * @param tab
	 */
	private static void formatXML(Document document,org.w3c.dom.Element root, String tab) {
		NodeList children = root.getChildNodes();
		// save the nodes in the array first
		Node[] nodes = new Node[children.getLength()];
		for (int i = 0; i < children.getLength(); i++)
			nodes[i] = children.item(i);
		// insert identations
		for (int i = 0; i < nodes.length; i++) {
			root.insertBefore(document.createTextNode("\n" + tab), nodes[i]);
			if (nodes[i] instanceof org.w3c.dom.Element)
				formatXML(document, (org.w3c.dom.Element) nodes[i], "  " + tab);
		}
		root.appendChild(document.createTextNode("\n"
				+ tab.substring(0, tab.length() - 2)));
	}
	
	/**
	 * write out an XML file
	 * 
	 * @param doc
	 * @param os
	 * @throws TransformerException 
	 * @throws IOException 
	 */
	public static void writeXML(Document doc, OutputStream os) 
		throws TransformerException, IOException{
		// write out xml file
		TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();

        //indent XML properly
        formatXML(doc,doc.getDocumentElement(),"  ");

        //normalize document
        doc.getDocumentElement().normalize();

		 //write XML to file
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(os);
        transformer.transform(source, result);
        os.close();
	}

	/**
	 * get single element by tag name
	 * @param element
	 * @param tag
	 * @return
	 */
	public static Element getElementByTagName(Element element, String tag){
		NodeList list = element.getElementsByTagName(tag);
		for(int i=0;i<list.getLength();i++){
			Node node = list.item(i);
			if(node instanceof org.w3c.dom.Element){
				return (Element) node;
			}
		}
		return null;
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
	 * parse integer
	 * @param value
	 * @return
	 */
	public static Dimension parseDimension(String value){
		// check for max value for size
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
	 * parse integer
	 * @param value
	 * @return
	 */
	public static Point2D parsePoint(String value){
		// check for max value for size
		try{
        	int i = 0;
    		double [] wh = new double [2];
    	
    		// split any string into potential rgb
    		for(String n : value.split("[^\\d\\.]")){
    			if(n.length() > 0 && i < wh.length){
    				 wh[i++] = Double.parseDouble(n);
    			}
    		}
    		return new Point2D.Double(wh[0],wh[1]);
    	}catch(Exception exe){
    		//log.severe("can't parse value "+value+" of property "+k+" Cause: "+exe.getMessage());
    	}
		return new Point2D.Double(0,0);
	}
	
	/**
	 * computer best foreground color for a given background color
	 * @param c
	 * @return
	 */
	public static Color getForegroundForBackground(Color c){
		//http://stackoverflow.com/questions/3116260/given-a-background-color-how-to-get-a-foreground-color-that-make-it-readable-on
		// calclulate luminocity Y
		double r = Math.pow(c.getRed()/255,2.2);
	    double b = Math.pow(c.getBlue()/255,2.2);
	    double g = Math.pow(c.getGreen()/255,2.2);
	    //Then combine them using sRGB constants (rounded to 4 places):
	    double Y = 0.2126*r + 0.7151*g + 0.0721*b;
	    return (Y > .5)?Color.black:Color.white;
	}
}
