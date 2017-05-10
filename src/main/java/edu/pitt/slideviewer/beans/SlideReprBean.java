package edu.pitt.slideviewer.beans;
/**
 * JavaBean class for authoring.
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

import java.awt.Shape;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;

//Marshall
/*
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.exolab.castor.xml.Unmarshaller;
*/
// Koala
//import fr.dyade.koala.xml.kbml.*;


public class SlideReprBean implements java.io.Serializable {
	private String slideName;
	private String xStart;
	private String yStart;
	private String initScale;
	private boolean freezeSlide;
	private Shape [] shapes = new Shape[0];

	public static void main(String[] args) {
		SlideReprBean sl = new SlideReprBean();
		sl.setSlideName("The Slide");
		sl.setTestShapes();
		/*
		    // java 1.4
		    try{
		    XMLEncoder enc = new XMLEncoder(new BufferedOutputStream (new FileOutputStream("test.xml")));
		    enc.writeObject(sl);
		    enc.close();
		 
		    XMLDecoder decoder = new XMLDecoder(
		        new BufferedInputStream(
		        new FileInputStream("testMarsh.xml")));
		      Object obj = decoder.readObject();
		      Object shapes_obj = getBeanPropertyValue(obj, "SHAPES");
		      System.out.println("shapes = "+shapes_obj);
		      Shape[] shapes = (Shape[]) shapes_obj;
		      for(int i=0; i<shapes.length; i++){
		        if(shapes[i] instanceof Rectangle)
		          System.out.println("Rect = width = "+((Rectangle)shapes[i]).getWidth());
		        else if(shapes[i].getClass().getName().endsWith("PolygonBean")){
		          String xx = (String)getBeanPropertyValue(shapes[i], "XPoints");
		          String yy = (String)getBeanPropertyValue(shapes[i], "YPoints");
		          System.out.println("xx = "+xx+" yy = "+yy);
		        }
		      }
		      decoder.close();
		*/
		//test: write a decodedobj to a file;
		// nop

		//    try{
		//      FileOutputStream ostream = new FileOutputStream("testDec.xml");
		//      ObjectOutputStream p = new ObjectOutputStream(ostream);
		//      p.writeObject(obj);
		//      p.flush();
		//      ostream.close();
		//   } catch (IOException e) { }

		/*
		      XMLEncoder enc1 = new XMLEncoder(new BufferedOutputStream (new FileOutputStream("testMarsh.xml")));
		      enc1.writeObject(obj);
		      enc1.close();
		    } catch (FileNotFoundException e){ e.printStackTrace(); }
		*/
		//Marshaller (>= 700KB)
		/*
		try{
		  BufferedWriter out = new BufferedWriter (new FileWriter("testMarsh.xml"));
		  Marshaller.marshal(sl, out);
	}
		catch (IOException e){ e.printStackTrace();}
		catch (MarshalException e){ e.printStackTrace();}
		catch (ValidationException e){ e.printStackTrace();}
		*/

		// koala ( 30 KB)
		//serialize
		/*
			try {
		 BufferedOutputStream ostream = new BufferedOutputStream(new FileOutputStream("testKoala.xml"));
		 KBMLSerializer bxo = new KBMLSerializer(ostream);
		        bxo.writeXMLDeclaration();
		        bxo.writeDocumentTypeDefinition();
		 bxo.writeKBMLStartTag();
		 bxo.writeBean(sl);
		 bxo.writeKBMLEndTag();
		 bxo.flush();
		 bxo.close();
	} catch (Exception e) {
		             e.printStackTrace();
	}

		    //deserialize
		  Object obj = null;
		  try {
		    BufferedInputStream istream =  new BufferedInputStream(new FileInputStream("testKoala.xml"));
		    KBMLDeserializer bxi = new KBMLDeserializer(istream);
		    obj = bxi.readBean();
		    bxi.close();
		  } catch (Exception e) {
		    e.printStackTrace();
		  }
		*/
		// just de/serialization
		Object obj = null;
		try {
			ObjectOutputStream objOut = new ObjectOutputStream(new FileOutputStream("testSer.xml"));
			objOut.writeObject(sl);

			ObjectInputStream objIn = new ObjectInputStream(new FileInputStream("testSer.xml"));
			obj = objIn.readObject();

		} catch (IOException e) { }
		catch (ClassNotFoundException e) { }



		if(obj != null) {
			SlideReprBean bean = (SlideReprBean)obj;
			System.out.println("bean name = "+bean.getSlideName());
		}
		/*
		      Object shapes_obj = getBeanPropertyValue(obj, "SHAPES");
		      Shape[] shapes = (Shape[]) shapes_obj;
		      String curType;
		      for(int i=0; i<shapes.length; i++){
		      curType = getBeanPropertyValue(shapes[i], "type").toString();
		      if(curType.equalsIgnoreCase("Rectangle"))
		        System.out.println("Rect = width = "+((PolygonBean)shapes[i]).getWidth());
		      else if(curType.equalsIgnoreCase("Polygon")){
		        // this way is slow
		        long curTime = System.currentTimeMillis();
		        String xx = getBeanPropertyValue(shapes[i], "XPoints").toString();
		        String yy = getBeanPropertyValue(shapes[i], "YPoints").toString();
		        System.out.println("general: xx = "+xx+" yy = "+yy+" time = "+(System.currentTimeMillis()-curTime));
		        // this way is fast
		        long curTime1 = System.currentTimeMillis();
		        String xx1 = ((PolygonBean)shapes[i]).getXPoints().toString();
		        String yy1 = ((PolygonBean)shapes[i]).getXPoints().toString();
		        System.out.println("object: xx = "+xx1+" yy = "+yy1+" time = "+(System.currentTimeMillis()-curTime1));
		        }
		      }
		  */
	}

	/**
	 * returns the value of a bean property
	 */
	public static Object getBeanPropertyValue(Object obj, String propertyName) {
		Class cl = obj.getClass();
		try {
			BeanInfo info = Introspector.getBeanInfo(cl);
			PropertyDescriptor[] pd = info.getPropertyDescriptors();
			//for (int i =0; i<pd.length; i++)
			//System.out.println("bean property: "+pd[i].getName());

			for (int i =0; i<pd.length; i++) {
				try {
					if(pd[i].getName().equalsIgnoreCase(propertyName)) {
						Method m = pd[i].getReadMethod();
						return m.invoke(obj, new Object [0]);
					}
				} catch (java.lang.reflect.InvocationTargetException e) {
					e.printStackTrace();
				}
				catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
		} catch (IntrospectionException e) { }
		return null;
	}

	public SlideReprBean() {}

	public void setTestShapes() {
		// Rectangle r = new Rectangle(10, 20, 30, 40);
		setSlideName("The Slide");
		PolygonBean r = new PolygonBean();
		r.setType("Rectangle");
		r.setXStart("10");
		r.setYStart("20");
		r.setWidth("30");
		r.setHeight("40");

		int k = 5;
		int[] xp = new int[k];
		int[] yp = new int[k];
		for(int i=0; i<k; i++) {
			xp[i] = (i+1)*10;
			yp[i] = (i+1)*10+5;
		}
		//PolygonBean p = new PolygonBean(xp, yp, k);
		PolygonBean p = new PolygonBean();
		p.setXPoints(arrToStr(xp));
		p.setYPoints(arrToStr(yp));
		p.setType("Polygon");
		Shape [] s = new Shape[2];
		s[0] = r;
		s[1] = p;
		setShapes(s);
	}

	protected synchronized String arrToStr(int[] arr) {
		StringBuffer sb = new StringBuffer();
		int s = arr.length;
		for(int i=0; i<s-1; i++) {
			sb.append(arr[i]);
			sb.append(" ");
		}
		sb.append(arr[s-1]);
		return sb.toString();
	}

	public void setSlideName(String n) {
		slideName = n;
	}

	public String getSlideName() {
		return slideName;
	}

	public void setShapes(Shape[] s) {
		shapes = s;
	}

	public Shape[] getShapes() {
		return shapes;
	}

	public void setXStart(String ix) {
		xStart = ix;
	}
	public String getXStart() {
		return xStart;
	}

	public void setYStart(String iy) {
		yStart = iy;
	}
	public String getYStart() {
		return yStart;
	}

	public void setInitScale(String s) {
		initScale = s;
	}
	public String getInitScale() {
		return initScale;
	}
	
	public boolean getFreezeSlide(){
		return freezeSlide;	
	}
	
	public void setFreezeSlide(boolean b){
		freezeSlide = b;	
	}
	
	/**
	 * Reduce memory size.
	 */
	public void dispose() {
		shapes = null;
	}

}



