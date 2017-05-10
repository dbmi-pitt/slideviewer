package edu.pitt.slideviewer;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.beans.*;
import javax.swing.*;
import edu.pitt.slideviewer.markers.*;
import java.awt.event.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;

/**
 * This class plays saved viewer events
 * @author Eugene Tseytlin (University of Pittsburgh)
 */
public class Player implements Runnable, ActionListener, ChangeListener{
	private BufferedReader reader;
	private Viewer viewer;
	private boolean stop,pause;
	private Thread thread;
	private double speed = 1.0;
	//private final int FRAME_NUM = 10;
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	// temp vars
	private ViewPosition view = new ViewPosition(0,0,0);
	private long lastTime = -666;
	private Object lock = "lock";
	
	// icons
	private final String playIconStr="/icons/Play24.gif";
	private final String pauseIconStr="/icons/Pause24.gif";
	private final String fasterIconStr="/icons/FastForward24.gif";
	private final String slowerIconStr="/icons/Rewind24.gif";
	private final String stopIconStr="/icons/Stop24.gif";
	
	// GUI elements
	private Container controlPanel;
	private JTextField speedTxt;
	private JSlider locationSlider;
	private AbstractButton pauseButton, playButton;
	private int iter; // list iterator
	private boolean iterUpdated;
	
	/**
	 * Create an instance of the player with this viewer
	 * @param viewer
	 */
	public Player(Viewer viewer){
		this.viewer = viewer;
	}
	
	/**
	 * Return instance of control panel
	 * @return
	 */
	public Container getControlPanel(){
		if(controlPanel == null)
			controlPanel = createControlPanel(this,this);
		return controlPanel;
	}
	
	
	/**
	 * Create control panel for this player
	 * @return
	 */
	public Container createControlPanel(ActionListener listener, ChangeListener change){
		//play button
		JPanel cp = new JPanel();
		cp.setBorder(new TitledBorder("Playback Control"));
		cp.setLayout(new BorderLayout());
		
		// buttons
		JToolBar toolbar = new JToolBar();
		
		// stopButton
		Icon icon = new ImageIcon(getClass().getResource(stopIconStr));
		JButton stopButton = new JButton(icon);
		stopButton.setToolTipText("Stop Playback");
		stopButton.addActionListener(listener);
		stopButton.setActionCommand("playback-stop");
		toolbar.add(stopButton);
		
		
		//pause
		icon = new ImageIcon(getClass().getResource(pauseIconStr));
		AbstractButton pause = new JToggleButton(icon);
		pause.setToolTipText("Pause Playback");
		pause.addActionListener(listener); 
		pause.setActionCommand("playback-pause");
		toolbar.add(pause);
		pauseButton = pause;
		
		// play
		icon = new ImageIcon(getClass().getResource(playIconStr));
		AbstractButton play = new JToggleButton(icon);
		play.setToolTipText("Resume Playback");
		play.addActionListener(listener);
		play.setActionCommand("playback-play");
		toolbar.add(play);
		toolbar.add(Box.createHorizontalGlue());
		playButton = play;
		
		// rewind 
		icon = new ImageIcon(getClass().getResource(slowerIconStr));
		JButton slower = new JButton(icon);
		slower.setToolTipText("Slow Down Playback");
		slower.addActionListener(listener);
		slower.setActionCommand("playback-slower");
		toolbar.add(slower);
		
		// counter
		speedTxt = new JTextField("    "+(int)speed+" X ");
		speedTxt.setEditable(false);
		speedTxt.setMaximumSize(slower.getPreferredSize());
		
		speedTxt.setToolTipText("Playback Speed");
		toolbar.add(speedTxt);
		
		// faster
		icon = new ImageIcon(getClass().getResource(fasterIconStr));
		JButton faster = new JButton(icon);
		faster.setToolTipText("Speed Up Playback");
		faster.addActionListener(listener);
		faster.setActionCommand("playback-faster");
		toolbar.add(faster);
		
		cp.add(toolbar,BorderLayout.NORTH);
		
		// slider
		locationSlider = new JSlider(0,1);
		locationSlider.setToolTipText("Playback Location");
		locationSlider.setValue(0);
		locationSlider.setEnabled(false);
		locationSlider.addChangeListener(change);
		cp.add(locationSlider,BorderLayout.CENTER);
		
		// status
		//statusLbl = new JLabel(" ");
		//cp.add(statusLbl,BorderLayout.SOUTH);
		return cp;
	}
	
	// for location
	public void stateChanged(ChangeEvent e){
		if(locationSlider != null && locationSlider.getValueIsAdjusting()){
			iter = locationSlider.getValue();
			iterUpdated = true;
			if(thread != null)
				thread.interrupt();
		}
	}
	
	/**
	 * button commands
	 */
	public void actionPerformed(ActionEvent e){
		String cmd = e.getActionCommand();
		if(cmd.equals("playback-stop")){
			stop();
		}else if(cmd.equals("playback-pause")){
			pause = (pauseButton != null)?pauseButton.isSelected():
						((AbstractButton)e.getSource()).isSelected();
			if(playButton != null)
				playButton.setSelected(!pause);
			//	if pause is unselected
			if(!pause){
				synchronized (lock) {
					lock.notifyAll();
				}
			}
		}else if(cmd.equals("playback-play")){
			pause = !((AbstractButton)e.getSource()).isSelected();
			if(pauseButton != null)
				pauseButton.setSelected(pause);
			
			//	if pause is unselected
			if(!pause){
				synchronized (lock) {
					lock.notifyAll();
				}
			}
		}else if(cmd.equals("playback-slower")){
			if(speed > 0.015625){
				speed /= 2;
				int x = (int) speed;
				if(x >= 1){
					speedTxt.setText("  "+x+" X ");
				}else{
					x = (int) (1/speed);
					speedTxt.setText(" 1/"+x+" X ");
				}
				if(thread != null)
					thread.interrupt();
			}
		}else if(cmd.equals("playback-faster")){
			if(speed < 64){
				speed *= 2;
				int x = (int) speed;
				if(x >= 1){
					speedTxt.setText("  "+x+" X ");
				}else{
					x = (int)(1/speed);
					speedTxt.setText(" 1/"+x+" X");
				}
				
				if(thread != null)
					thread.interrupt();
			}
		}
	}
	
	/**
	 * Add property change listener
	 * @param l
	 */
	public void addPropertyChangeListener(PropertyChangeListener l){
		pcs.addPropertyChangeListener(l);
	}
	
	/**
	 * Remove property change listener
	 * @param l
	 */
	public void removePropertyChangeListener(PropertyChangeListener l){
		pcs.removePropertyChangeListener(l);
	}
	
	
	/**
	 * @param viewer the viewer to set
	 */
	public void setViewer(Viewer viewer) {
		this.viewer = viewer;
	}

	/**
	 * @return the speed
	 */
	public double getSpeed() {
		return speed;
	}


	/**
	 * @param speed the speed to set
	 */
	public void setSpeed(double speed) {
		if(speed == 0)
			speed = -1;
		this.speed = speed;
	}


	/**
	 * Set reader from which events will be read
	 */
	public void setReader(Reader r){
		this.reader = (r instanceof BufferedReader)?(BufferedReader)r:new BufferedReader(r);
	}
	
	/**
	 * Set reader from which events will be read
	 */
	public void setInputStream(InputStream is){
		setReader(new InputStreamReader(is));
	}
	
	/**
	 * Set input file 
	 * @param file
	 * @throws IOException
	 */
	public boolean setFile(String file) {
		try{
			setFile(new File(file));
		}catch(IOException ex){
			return false;
		}
		return true;
	}
	
	
	/**
	 * Set input file 
	 * @param file
	 * @throws IOException
	 */
	public void setFile(File file) throws IOException{
		setReader(new FileReader(file));
	}
	
	// destractor
	protected void finalize() throws Throwable {
		dispose();
	}
	
	/**
	 * Dispose of resources
	 *
	 */
	public void dispose(){
		stop();
		if(reader != null){
			try{
				reader.close();
				reader = null;
			}catch(IOException ex){}
		}
		controlPanel = null;
		viewer = null;
	}
	
	/**
	 * Play input file
	 */
	public void play(){
		// stop prvious running Thread if any
		stop();
		stop = false;
		thread = new Thread(this);
		thread.start();
	}
	
	// do something before play
	private void playStart(){
		if(playButton != null)
			playButton.setSelected(true);
		//		 enable slider
		if(locationSlider != null){
			locationSlider.setEnabled(true);
			locationSlider.setValue(0);
		}
		
		// figure out max for locationSlider
	}
	// do something after play
	private void playStop(){
		if(playButton != null)
			playButton.setSelected(false);
		if(locationSlider != null){
			locationSlider.setEnabled(true);
		}
	}
	
	/**
	 * Stop current playback
	 *
	 */
	public void stop(){
		stop = true;
		synchronized (lock) {
			lock.notifyAll();
		}
		if(thread != null)
			thread.interrupt();
		/*
		if(thread != null && thread.isAlive()){
			try{
				thread.join();
			}catch(InterruptedException ex){}
		}*/
		thread = null;
	}
		
	
	/**
	 * Sleep for required number of ms if necessary
	 * @param last time
	 * @param current time 
	 */
	private void sleep(long lastTime, long time) {
		long delta = (long)((time - lastTime)/speed);
		//System.out.println("Sleeping for "+delta+" ms");
		// wait for delta ms
		if(delta > 0 && lastTime > 0){
			try{
				Thread.sleep(delta);
			}catch(InterruptedException ex){}
		}
	}
	
	//	find container window
    private Container getParentContainer(Container cont){
    	if(cont == null)
    		return null;
    	else if(cont.getParent() == null || 
    			cont instanceof Frame || 
    			cont instanceof Window ||
    			cont instanceof java.applet.Applet)
    		return cont;
    	else
    		return getParentContainer(cont.getParent());
    }
    
    // resize container window
    private void resizeViewer(Dimension size){
    	Dimension os = viewer.getSize();
    	viewer.setSize(size);
    	int dx = size.width -os.width;
    	int dy = size.height-os.height;
    	
    	Container win = getParentContainer((Container)viewer.getViewerComponent());
    	if(win != null && win != viewer.getViewerComponent()){
    		//System.out.println(win.getClass().getName());
    		Dimension d = win.getSize();
    		win.setSize(d.width+dx,d.height+dy);
    		win.validate();
    	}
    }
	
    /**
	 * Create a smoother animation of view changed
	 * @param delay
	 * @param target
	 */
    private void animateViewObserve(long prev_delay, long cur_delay, ViewPosition target){
    	ViewPosition init = viewer.getViewPosition();
    	final int delta = 40;
    	int distx = target.x - init.x;
    	int disty = target.y - init.y;
    	int FRAME_NUM = (int)((Math.abs(distx) > Math.abs(disty))?
    						   Math.abs(distx)/(delta/init.scale):
    						   Math.abs(disty)/(delta/init.scale));
    	if(FRAME_NUM < 1)
    		FRAME_NUM = 1;
    	
    	int dx = distx / FRAME_NUM;
    	int dy = disty / FRAME_NUM;
    	
    	// devide region into frames
    	for(int i=0;i<FRAME_NUM;i++){
    		sleep((long)(prev_delay/FRAME_NUM),(long)(cur_delay/FRAME_NUM));
    		
    		// figure out delta
    		init.x = init.x+dx;
    		init.y = init.y+dy;
    		viewer.setViewPosition(init);
    	}
    }
    
    /**
     * Play view change in the viewer
     * @param x
     * @param y
     * @param scale
     * @param timestamp
     */
    public void playViewChange(String xstr, String ystr, String zstr, Date time) throws NumberFormatException{
    	view.x = Integer.parseInt(xstr);
		view.y = Integer.parseInt(ystr);
		view.scale = Double.parseDouble(zstr);
		
		// init initial time
		sleep(lastTime,time.getTime());
		
		// set position
		viewer.setViewPosition(view);
		lastTime = time.getTime();
    }
    
    /**
     * Play image change
     * @param name
     * @param time
     * @throws ViewerException
     */
    public void playImageChange(String name, Date time) throws ViewerException{
    	if(name.equals(viewer.getImage()))
    		return;
    	
    	//	init initial time
		sleep(lastTime,time.getTime());
			
		// set position
		String type = ViewerFactory.getProperties().getProperty("image.server.type","qview");
		String path =  ViewerFactory.getProperties().getProperty(type+".image.dir","");
		viewer.openImage(path+name);
		lastTime = time.getTime();
		
		// notify listeners that new image is loaded
		pcs.firePropertyChange("PlaybackImage",null,name);
    }
    
    /**
     * Play viewer resize
     * @param wstr
     * @param hstr
     * @param time
     * @throws NumberFormatException
     */
		
    public void playViewResize(String wstr, String hstr, Date time) throws NumberFormatException {
    	int w = Integer.parseInt(wstr);
		int h = Integer.parseInt(hstr);
		
		//	init initial time
		sleep(lastTime,time.getTime());
		
		// set position
		resizeViewer(new Dimension(w,h));
		
		lastTime = time.getTime();
    	
    }
    
    /**
     * Play tutor marker added (everything, but polygon)
     * @param type
     * @param x
     * @param y
     * @param w
     * @param h
     * @param time
     * @throws NumberFormatException
     */
    public void playAnnotation(String type, String name, String tag, String x, String y, String w, String h,Date time)
    	throws NumberFormatException {
    	playAnnotation(type,name,tag,x,y,w,h,time,null,null);
    }
    
    /**
     * Play tutor marker added (everything, but polygon)
     * @param type
     * @param x
     * @param y
     * @param w
     * @param h
     * @param time
     * @throws NumberFormatException
     */
    public void playAnnotation(String type, String name, String tag,String x, String y, String w, String h,
    	Date time, String xpoints, String ypoints) throws NumberFormatException {
		Rectangle r = new Rectangle();
		r.x = Integer.parseInt(x);
		r.y = Integer.parseInt(y);
		r.width  = Integer.parseInt(w);
		r.height = Integer.parseInt(h);
		
		Color c = Color.green;
		int shape = AnnotationManager.convertType(type);
		switch(shape){
			case (AnnotationManager.CROSS_SHAPE):
				c = Color.yellow;
				break;
			case (AnnotationManager.RULER_SHAPE):
				c = Color.orange;
				break;
			case (AnnotationManager.POLYGON_SHAPE):
				c = Color.blue;
				break;
		}
		
		//	init initial time
		sleep(lastTime,time.getTime());
		
		// set marker
		if(shape > -1){
			AnnotationManager mm = viewer.getAnnotationPanel().getAnnotationManager();
			Annotation marker = mm.createAnnotation(name,shape,r,c,false);
			marker.setTag(tag);
			marker.setTagVisible(true);
			marker.setMovable(true);
			mm.addAnnotation(marker);
			marker.setViewPosition(viewer.getViewPosition());
			if(xpoints != null && ypoints != null){
				marker.setVertices(xpoints,ypoints);
			}
			viewer.repaint();
		}
		lastTime = time.getTime();
    }

    /**
     * Play tutor marker added (everything, but polygon)
     * @param type
     * @param x
     * @param y
     * @param w
     * @param h
     * @param time
     * @throws NumberFormatException
     */
    public void playUpdateAnnotation(String type,String name, String x, String y, String w, String h,Date time)
    	throws NumberFormatException {
    	playUpdateAnnotation(type,name, x, y, w, h,time,null,null);
    }
    
    /**
     * Play tutor marker added (everything, but polygon)
     * @param type
     * @param x
     * @param y
     * @param w
     * @param h
     * @param time
     * @throws NumberFormatException
     */
    public void playUpdateAnnotation(String type,String name, String x, String y, String w, String h,
    		Date time, String xpoints, String ypoints)	throws NumberFormatException {
		Rectangle r = new Rectangle();
		r.x = Integer.parseInt(x);
		r.y = Integer.parseInt(y);
		r.width  = Integer.parseInt(w);
		r.height = Integer.parseInt(h);
						
		//	init initial time
		sleep(lastTime,time.getTime());
		
		// set marker
		AnnotationManager mm = viewer.getAnnotationPanel().getAnnotationManager();
		Annotation marker = mm.getAnnotation(name);
		if(marker != null){
			marker.setBounds(r);
			marker.setViewPosition(viewer.getViewPosition());
			if(xpoints != null && ypoints != null){
				marker.setVertices(xpoints,ypoints);
			}
		}
		viewer.repaint();
		lastTime = time.getTime();
    }
  
    
    /**
     * Play Open/Close navigator
     * @param op
     * @param time
     */
    public void playNavigator(String op, Date time){
    	boolean open = "OPEN".equalsIgnoreCase(op.trim());
		
    	//	init initial time
		sleep(lastTime,time.getTime());
		
		// set position
		viewer.getViewerControlPanel().showNavigatorWindow(open);
		lastTime = time.getTime();
    }
    
    /**
     * Play ViewObserve tag
     * @param xstr
     * @param ystr
     * @param zstr
     * @param time
     * @throws NumberFormatException
     */
    public void playViewObserve(String xstr, String ystr, String zstr, Date time) 
    	throws NumberFormatException{
    	// check for redundency
		view.x = Integer.parseInt(xstr);
		view.y = Integer.parseInt(ystr);
		view.scale = Double.parseDouble(zstr);
		//time = Constants.DATE_FORMAT.parse(entries[4]);
		
		//skip this if it is the same as previous position
		if(!view.equals(viewer.getViewPosition())){
			//animate if drag detected
			Rectangle r = viewer.getViewRectangle();
			if(viewer.getScale() == view.scale &&
				r.intersects(new Rectangle(view.x,view.y,r.width,r.height))){
				//System.out.println("animating...");
				animateViewObserve(lastTime,time.getTime(),view);
			}else{
				//System.out.println("jumping ...");
				// init initial time
				sleep(lastTime,time.getTime());
				// set position
				viewer.setViewPosition(view);
			}
			lastTime = time.getTime();
		}
    }
    
    
    /**
     * get last image
     * @param list
     * @return
     */
    private String [] getLastImage(java.util.List list, int n){
    	String time = ""+new Date();
    	if(n>0 && n < list.size()){
    		for(int i=n;i>=0;i--){
	    		String line = ""+list.get(i);
	    		String [] entries = line.split("\\"+Constants.ENTRY_SEPARATOR);
				if(entries.length > 1){
					if(i == n)
						time = entries[entries.length-1];
					String p = entries[0].trim();
					if(p.equalsIgnoreCase(Constants.IMAGE_TAG)){
						return new String [] {entries[1].trim(),time};
					}
				}
	    	}
    	}
    	return new String [] {viewer.getImage(),time} ;
    }
    
    
	/**
	 * This is where playback occurs
	 */
	public void run(){
		playStart();
		try{
			view = new ViewPosition(0,0,0);
			Date time = null;
			lastTime = -666;
			
			// compile list of commands
			ArrayList list = new ArrayList();
			for(String line = reader.readLine();line != null;line = reader.readLine()){
				//	skip comments
				if(line.startsWith("#"))
					continue;
				list.add(line);
			}
			
			// set location slider
			if(locationSlider != null){
				locationSlider.setMaximum(list.size());
			}
			
			// iterate over entries in reader
			for(iter=0;iter<list.size() && !stop;iter++){
				String line = ""+list.get(iter);
				
				//	check for pause
				if(pause){
					synchronized (lock) {
						lock.wait();
					}
				}
				
				// set location slider
				if(locationSlider != null){
					locationSlider.setValue(iter);
				}
				
				// if iterator was updated from outside, reload appropriate image
				if(iterUpdated){
					String [] name = getLastImage(list,iter);
					if(name.length == 2 && !name[0].equals(viewer.getImage())){
						playImageChange(name[0],Constants.DATE_FORMAT.parse(name[1]));
					}
					// find image to load
					iterUpdated = false;
				}
				
				
				// parse line
				String [] entries = line.split("\\"+Constants.ENTRY_SEPARATOR);
				
				//System.out.println("processing line :"+line);
				if(entries.length > 1){
					String p = entries[0].trim();
					if(p.equalsIgnoreCase(Constants.VIEW_TAG)){
						time = Constants.DATE_FORMAT.parse(entries[4]);
						playViewChange(entries[1],entries[2],entries[3],time);
					}else if(p.equalsIgnoreCase(Constants.OBSERVE_TAG)){
						time = Constants.DATE_FORMAT.parse(entries[4]);
						playViewObserve(entries[1],entries[2],entries[3],time);
					}else if(p.equalsIgnoreCase(Constants.RESIZE_TAG)){
						time = Constants.DATE_FORMAT.parse(entries[3]);
						playViewResize(entries[1],entries[2],time);
					}else if(p.equalsIgnoreCase(Constants.IMAGE_TAG)){
						time = Constants.DATE_FORMAT.parse(entries[2]);
						playImageChange(entries[1].trim(),time);
					}else if(p.equalsIgnoreCase(Constants.MARKER_TAG)){
						time = Constants.DATE_FORMAT.parse(entries[8]);
						String xpnts = null,ypnts = null;
						if(entries.length>10){
							xpnts = entries[9];ypnts = entries[10];
						}
						playAnnotation(entries[1].trim(),entries[2].trim(),entries[3],
										entries[4],entries[5],entries[6],entries[7],time,xpnts,ypnts);
					}else if(p.equalsIgnoreCase(Constants.NAVIGATOR_TAG)){
						time = Constants.DATE_FORMAT.parse(entries[2]);
						playNavigator(entries[1].trim(),time);
					}else if(p.equalsIgnoreCase(Constants.UPDATE_MARKER_TAG)){
						time = Constants.DATE_FORMAT.parse(entries[8]);
						String xpnts = null, ypnts = null;
						if(entries.length>10){
							xpnts = entries[9]; ypnts = entries[10];
						}
						playUpdateAnnotation(entries[1].trim(),entries[2].trim(),entries[4],
										entries[5],entries[6],entries[7],time,xpnts,ypnts);
					}
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		// notify listeners that playback is done
		pcs.firePropertyChange("Playback",null,"STOP");
		playStop();	
	}
}
