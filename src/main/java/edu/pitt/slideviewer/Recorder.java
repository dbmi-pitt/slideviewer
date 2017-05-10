package edu.pitt.slideviewer;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.beans.*;
import edu.pitt.slideviewer.markers.*;

/**
 * This class can be used to record viewer movements
 * @author  Eugene Tseytlin (University of Pittsburgh)
 */
public class Recorder implements PropertyChangeListener {
	private Writer writer;
	private boolean record;

	//separator char
	private final String SC = Constants.ENTRY_SEPARATOR;
	
	/**
	 * Record current session
	 * if false nothing is save
	 * @param b
	 */
	public void setRecord(boolean b){
		record = b;
		if(!record && writer != null){
			try{
				writer.close();
			}catch(IOException ex){}
		}
	}
	
	/**
	 * @return the record
	 */
	public boolean isRecording(){
		return record;
	}

	/**
	 * destractor
	 */
	protected void finalize(){
		dispose();
	}
	
	/**
	 * Set output file
	 * @param file path
	 * @return true if loaded, false otherwise
	 */
	
	public boolean setFile(String str){
		try{
			setFile(new File(str));
		}catch(IOException ex){
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	
	/**
	 * Set output file
	 * @param f
	 * @throws IOException
	 */
	public void setFile(File f) throws IOException {
		setWriter(new FileWriter(f,true));
	}
	
	
	/**
	 * Set writer for the output
	 * @param w
	 */
	public void setWriter(Writer w){
		this.writer = w;
	}
	
	/**
	 * Set output stream
	 * @param os
	 */
	public void setOutputStream(OutputStream os){
		setWriter(new OutputStreamWriter(os));
	}
	
	
	/**
	 * Dispose of all resources (distractor)
	 */
	public void dispose(){
		if(writer != null){
			try{
				writer.close();
			}catch(IOException ex){}
			writer = null;
		}
	}
	
	/**
	 * Listen to viewer movements and record
	 */
	public void propertyChange(PropertyChangeEvent e){
		String p = e.getPropertyName();
		if(p.equalsIgnoreCase(Constants.VIEW_CHANGE)){
			recordViewChange((ViewPosition)e.getNewValue());
		}else if(p.equalsIgnoreCase(Constants.VIEW_OBSERVE)){
			recordViewObserve((ViewPosition)e.getNewValue());
		}else if(p.equalsIgnoreCase(Constants.VIEW_RESIZE)){
			recordViewResize((Dimension)e.getNewValue());
		}else if(p.equalsIgnoreCase(Constants.IMAGE_CHANGE)){
			recordImageChange(""+e.getNewValue());
		}else if(p.equalsIgnoreCase(Constants.SKETCH_DONE)){
			recordAnnotation((Annotation) e.getNewValue());
		}else if(p.equalsIgnoreCase(Constants.NAVIGATOR)){
			recordNavigator(""+e.getNewValue());
		}else if(p.equalsIgnoreCase(Constants.UPDATE_SHAPE)){
			recordUpdateAnnotation((Annotation) e.getNewValue());
		}
	}
	

	/* -------------------  recorders ------------------------- */
	
	/**
	 * Record view position
	 */
	public boolean recordViewChange(ViewPosition p){
		return recordViewChange(p,new Date(System.currentTimeMillis()));
	}
	
	
	/**
	 * Record view position
	 */
	public boolean recordViewChange(ViewPosition p, Date date){
		if(record && writer != null){
			try{
				String time = Constants.DATE_FORMAT.format(date);
				writer.write(Constants.VIEW_TAG+"\t"+SC+p.x+SC+p.y+SC+p.scale+SC+time+"\n");
			}catch(IOException ex){
				ex.printStackTrace();
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Record view position
	 */
	public boolean recordComment(String comment){
		if(record && writer != null){
			try{
				writer.write("# "+comment+"\n");
			}catch(IOException ex){
				ex.printStackTrace();
				return false;
			}
		}
		return true;
	}
	
	
	
	/**
	 * Record view position
	 */
	public boolean recordViewObserve(ViewPosition p){
		return recordViewObserve(p,new Date(System.currentTimeMillis()));
	}
	
	/**
	 * Record view position
	 */
	public boolean recordViewObserve(ViewPosition p, Date date){
		if(record && writer != null){
			try{
				String time = Constants.DATE_FORMAT.format(date);
				writer.write(Constants.OBSERVE_TAG+"\t"+SC+p.x+SC+p.y+SC+p.scale+SC+time+"\n");
			}catch(IOException ex){
				ex.printStackTrace();
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Record viewer resize
	 */
	public boolean recordViewResize(Dimension d){
		return recordViewResize(d,new Date(System.currentTimeMillis()));
	}
	
	
	/**
	 * Record viewer resize
	 */
	public boolean recordViewResize(Dimension d, Date date){
		if(record && writer != null){
			try{
				String time = Constants.DATE_FORMAT.format(date);
				writer.write(Constants.RESIZE_TAG+"\t"+SC+d.width+SC+d.height+SC+time+"\n");
			}catch(IOException ex){
				ex.printStackTrace();
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Record image change
	 */
	public boolean recordImageChange(String image){
		return recordImageChange(image,new Date(System.currentTimeMillis()));
	}
	
	
	/**
	 * Record image change
	 */
	public boolean recordImageChange(String image, Date date){
		if(record && writer != null && image != null){
			try{
				String time = Constants.DATE_FORMAT.format(date);
				writer.write(Constants.IMAGE_TAG+"\t"+SC+image+SC+time+"\n");
			}catch(IOException ex){
				ex.printStackTrace();
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Record annotation
	 */
	public boolean recordAnnotation(Annotation m){
		return recordAnnotation(m,new Date(System.currentTimeMillis()));
	}
	
	
	/**
	 * Record annotation
	 */
	public boolean recordAnnotation(Annotation m,Date date){
		if(record && writer != null){
			try{
				String type = m.getType();
				String name = m.getName();
				String tag  = m.getTag();
				Rectangle r = m.getBounds();
				String time = Constants.DATE_FORMAT.format(date);
				writer.write(Constants.MARKER_TAG+"\t"+SC+type+SC+name+SC+tag+SC+r.x+SC+r.y+SC+r.width+SC+r.height+SC+time);
				// now if it is a parallelogram, save its vertecies
				if(m.isPolygon()){
					String [] xy = m.getVertices();
					if(xy != null && xy.length == 2)
						writer.write(SC+xy[0]+SC+xy[1]);
				}
				// write end of line
				writer.write("\n");
			}catch(IOException ex){
				ex.printStackTrace();
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * Record annotation
	 */
	public boolean recordUpdateAnnotation(Annotation m){
		return recordUpdateAnnotation(m, new Date(System.currentTimeMillis()));
	}
	
	/**
	 * Record annotation
	 */
	public boolean recordUpdateAnnotation(Annotation m, Date date){
		if(record && writer != null){
			try{
				String type = m.getType();
				String name = m.getName();
				String tag  = m.getTag();
				Rectangle r = m.getBounds();
				String time = Constants.DATE_FORMAT.format(date);
				writer.write(Constants.UPDATE_MARKER_TAG+"\t"+SC+type+SC+name+SC+tag+SC+r.x+SC+r.y+SC+r.width+SC+r.height+SC+time);
				//	 now if it is a parallelogram, save its vertecies
				if(m.isPolygon()){
					String [] xy = m.getVertices();
					if(xy != null && xy.length == 2)
						writer.write(SC+xy[0]+SC+xy[1]);
				}		
				writer.write("\n");
			}catch(IOException ex){
				ex.printStackTrace();
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * Record navigator
	 */
	public boolean recordNavigator(String op){
		return recordNavigator(op,new Date(System.currentTimeMillis()));
	}
	
	/**
	 * Record navigator
	 */
	public boolean recordNavigator(String op, Date date){
		if(record && writer != null){
			try{
				String time = Constants.DATE_FORMAT.format(date);
				writer.write(Constants.NAVIGATOR_TAG+"\t"+SC+op+SC+time+"\n");
			}catch(IOException ex){
				ex.printStackTrace();
				return false;
			}
		}
		return true;
	}

	
}
