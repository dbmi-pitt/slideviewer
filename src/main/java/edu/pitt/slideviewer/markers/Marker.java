package edu.pitt.slideviewer.markers;

import java.awt.*;
import java.awt.geom.*;

import edu.pitt.slideviewer.AnnotationManager;
import edu.pitt.slideviewer.ImageTransform;
import edu.pitt.slideviewer.qview.connection.Utils;

public abstract class Marker {
	//protected int cursorState = 0;
	protected Cursor cursor;
	protected Annotation shape;
	protected Point point = null;
	protected int size = 8;


	public Marker( Annotation s ) {
		shape = s;
	}

	public void setCursorState( int cur ) {
		//cursorState = cur;
		cursor = Cursor.getPredefinedCursor(cur);
	}
	public Cursor getCursor() {
		return cursor;
	}

	public void setCursor(Cursor c){
		cursor = c;
	}
	
	public void setShape( Annotation s ) {
		shape = s;
	}
	
	public Annotation getShape() {
		return shape;
	}
	/*
	   public void setInDrag(boolean b){
	     shape.setInDrag(b);
	   }
	*/
	
	public ImageTransform getTransform(){
		if(shape.getViewer() != null)
			return shape.getViewer().getImageProperties().getImageTransform();
		return new ImageTransform();
	}
	
	public void setPoint( double x, double y ) {
		if ( point == null )
			point = new Point( ( int ) x, ( int ) y );
		else
			point.setLocation( ( int ) x, ( int ) y );
	}

	public Point getPoint() {
		return point;
	}

	abstract public void moveBy( int dx, int dy );

    
    public void draw( Graphics g ) {
        draw(g,Color.black);
    }
	public void draw( Graphics g,Color c) {
		// web course
		//g.fillRect((point.x-size/2),(point.y-size/2), size, size);
		g.setColor(Color.white);
        g.fillRect((int)(point.getX()-size/2)-1,(int)(point.getY()-size/2)-1,size,size);
        g.setColor(c);
        g.fillRect((int)(point.getX()-size/2),(int)(point.getY()-size/2),size-2,size-2);
	}

	public boolean inside( int x, int y ) {
		// if marker is outside of viewer bounds
        // then don't bother doing anything else
        if(point == null ||
           !shape.getViewer().getViewerComponent().getBounds().contains(point))
            return false;
        
        try {
			// web course
			//int deltaX = point.x - x;
			//int deltaY = point.y - y;
			int deltaX = ( int ) point.getX() - x;
			int deltaY = ( int ) point.getY() - y;
			int tst_radius = size + 2;
			return ( ( deltaX * deltaX + deltaY * deltaY ) <= tst_radius * tst_radius );
		} catch ( NullPointerException e ) {
			return false;
		}
	}

	public void notifyBoundsChange() {
		shape.notifyBoundsChange();
	}


	static public class CenterMarker extends Marker {

		public CenterMarker( Annotation s ) {
			super( s );
			setCursorState( Cursor.HAND_CURSOR );
		}

		public void moveBy( int dx, int dy ) {
			double z = shape.getViewer().getScale();
			Point d = new Point((int)(dx/z),(int)(dy/z));
			shape.getBounds().translate(d.x,d.y);
		}

		public void draw( Graphics g ) {
			Rectangle r = shape.getRelativeBounds();
			setPoint( r.getCenterX(), r.getCenterY() );
			super.draw( g );
		}
	}

	//invisible
	static public class CrossCenterMarker extends CenterMarker {
		public CrossCenterMarker( Annotation s ) {
			super( s );
		}
		public void draw( Graphics g ) {
			Point p = shape.getRelativeLocation();
			setPoint( p.x + 3, p.y + 3 );
		}
	}

	static public class ArrowCenterMarker extends Marker {
		public ArrowCenterMarker( Annotation s ) {
			super( s );
			setCursorState( Cursor.HAND_CURSOR);
		}
		public void moveBy( int dx, int dy ) {
			double z = shape.getViewer().getScale();
			Point d = new Point((int)(dx/z),(int)(dy/z));
			Point e = shape.getEndPoint();
			shape.getBounds().translate(d.x,d.y);
			// this is overwritten in relevant shapes to set end point
			shape.addPoint(e.x+d.x,e.y+d.y);
		}

		public void draw( Graphics g ) {
			Point r = shape.getRelativeLocation();
			Point ep = shape.getRelativeEndPoint();
			double x = r.x;
			double y = r.y;
			double width  = ep.x-r.x;
			double height = ep.y-r.y;
			setPoint(x+width/2,y+height/2);
			super.draw(g);
		}

	}

	static public class ArrowStartMarker extends Marker {
		public ArrowStartMarker( Annotation s ) {
			super( s );
            setCursorState( Cursor.HAND_CURSOR );
		}

		public void moveBy( int dx, int dy ) {
			double z = shape.getViewer().getScale();
			Point d = new Point((int)(dx/z),(int)(dy/z));
			shape.getBounds().translate(d.x,d.y);
		}
		public void draw( Graphics g ) {
			Point p = shape.getRelativeLocation();
			setPoint( p.x, p.y);
			super.draw( g );
		}
	}

	static public class ArrowEndMarker extends Marker {
		public ArrowEndMarker( Annotation s ) {
			super( s );
            setCursorState( Cursor.HAND_CURSOR );
		}

		public void moveBy( int dx, int dy ) {
			double z = shape.getViewer().getScale();
			Point d = new Point((int)(dx/z),(int)(dy/z));
			Point e = shape.getEndPoint();
			// this is overwritten in relevant shapes to set end point
			shape.addPoint(e.x+d.x,e.y+d.y); 
		}
		public void draw( Graphics g ) {
			Point p = shape.getRelativeEndPoint();
			setPoint(p.x,p.y);
			super.draw(g);
		}
	}

	static public class NResizeMarker extends Marker {
		public NResizeMarker( Annotation s ) {
			super( s );
			setCursorState( Cursor.N_RESIZE_CURSOR );
		}
		public void moveBy( int dx, int dy ) {
			double z = shape.getViewer().getScale();
			Point d = new Point((int)(dx/z),(int)(dy/z));
					
			// transform point
			ImageTransform it = getTransform();
			int rotate = it.getRotationTransform();
			int flip   = it.getFlipTransform();
			if(rotate % 2 == 0){
				d.x = 0;
				if(!(Math.abs(rotate) == 2 ^ flip >= 0))
					d.y = -d.y;
			}else{
				d.y = 0;
				if(!((rotate == -1 || rotate == 3) ^ flip <= 0))
					d.x = -d.x;
			}
					
			// resize the bounds
			Rectangle r = shape.getBounds();
			//r = new Rectangle(r.x,r.y+d.y,r.width,r.height-2*d.y);
			shape.setBounds(new Rectangle(r.x+d.x,r.y+d.y,r.width-2*d.x,r.height-2*d.y));
		}
		public void draw( Graphics g ) {
			Rectangle r = Utils.correctRectangle(shape.getRelativeBounds());
			setPoint(r.x + r.width/ 2, r.y );
			super.draw( g );
		}
	}

	static public class SResizeMarker extends Marker {
		public SResizeMarker( Annotation s ) {
			super( s );
			setCursorState( Cursor.S_RESIZE_CURSOR );
		}

		public void moveBy( int dx, int dy ) {
			double z = shape.getViewer().getScale();
			Point d = new Point((int)(dx/z),(int)(dy/z));
			
			// transform point
			ImageTransform it = getTransform();
			int rotate = it.getRotationTransform();
			int flip   = it.getFlipTransform();
			if(rotate % 2 == 0){
				d.x = 0;
				if(!(Math.abs(rotate) == 2 ^ flip >= 0))
					d.y = -d.y;
			}else{
				d.y = 0;
				if(!((rotate == -1 || rotate == 3) ^ flip <= 0))
					d.x = -d.x;
			}
			
			Rectangle r = shape.getBounds();
			//shape.setBounds(new Rectangle(r.x,r.y - d.y,r.width,r.height+2*d.y));
			shape.setBounds(new Rectangle(r.x-d.x,r.y-d.y,r.width+2*d.x,r.height+2*d.y));
		}
		public void draw( Graphics g ) {
			Rectangle r = Utils.correctRectangle(shape.getRelativeBounds());
			setPoint( r.x + r.width/2, r.y+ r.height);
			super.draw( g );
		}
	}

	static public class WResizeMarker extends Marker {
		public WResizeMarker( Annotation s ) {
			super( s );
			setCursorState( Cursor.W_RESIZE_CURSOR );
		}
		public void moveBy( int dx, int dy ) {
			//Point d = shape.getViewer().convertViewToImage(new Point(dx,dy));
			double z = shape.getViewer().getScale();
			Point d = new Point((int)(dx/z),(int)(dy/z));
			
			// transform point
			ImageTransform it = getTransform();
			int rotate = it.getRotationTransform();
			int flip   = it.getFlipTransform();
			if(rotate % 2 == 0){
				d.y = 0;
				if(!(Math.abs(rotate) == 2 ^ flip <= 0))
					d.x = -d.x;
			}else{
				d.x = 0;
				if(!((rotate == -1 || rotate == 3) ^ flip <0))
					d.y = -d.y;
			}
		
			
			Rectangle r = shape.getBounds();
			//shape.setBounds(new Rectangle(r.x+d.x,r.y,r.width-2*d.x,r.height));
			shape.setBounds(new Rectangle(r.x+d.x,r.y+d.y,r.width-2*d.x,r.height-2*d.y));
		}
		public void draw( Graphics g ) {
			Rectangle r = Utils.correctRectangle(shape.getRelativeBounds());
			setPoint( r.x,r.y+r.height/2);
			super.draw( g );
		}
	}

	static public class EResizeMarker extends Marker {
		public EResizeMarker( Annotation s ) {
			super( s );
			setCursorState( Cursor.E_RESIZE_CURSOR );
		}

		public void moveBy( int dx, int dy ) {
			double z = shape.getViewer().getScale();
			Point d = new Point((int)(dx/z),(int)(dy/z));
			

			// transform point
			ImageTransform it = getTransform();
			int rotate = it.getRotationTransform();
			int flip   = it.getFlipTransform();
			if(rotate % 2 == 0){
				d.y = 0;
				if(!(Math.abs(rotate) == 2 ^ flip <= 0))
					d.x = -d.x;
			}else{
				d.x = 0;
				if(!((rotate == -1 || rotate == 3) ^ flip <0))
					d.y = -d.y;
			}
		
			Rectangle r = shape.getBounds();
			//shape.setBounds(new Rectangle(r.x-d.x, r.y, r.width+2*d.x, r.height));
			shape.setBounds(new Rectangle(r.x-d.x,r.y-d.y,r.width+2*d.x, r.height+2*d.y));
		}
		public void draw( Graphics g ) {
			Rectangle r = Utils.correctRectangle(shape.getRelativeBounds());
			setPoint(r.x + r.width, r.y+r.height/ 2 );
			super.draw( g );
		}
	}
    
    static public class PolyCenterMarker extends CenterMarker {
        public PolyCenterMarker( Annotation s ) {
            super(s);
        }
        public void moveBy( int dx, int dy ) {
            double z = shape.getViewer().getScale();
            Point d = new Point((int)(dx/z),(int)(dy/z));
            if(shape.getShape() instanceof Polygon){
                Polygon poly = (Polygon) shape.getShape();
                poly.translate(d.x,d.y);
            }
        }
    }
    
    static public class PolyEResizeMarker extends Marker {
        public PolyEResizeMarker( Annotation s ) {
            super( s );
            setCursorState(Cursor.E_RESIZE_CURSOR );
        }

        public void moveBy( int dx, int dy ) {
            //if(shape instanceof PolygonShape){
            AffineTransform tf = new AffineTransform();
            tf.scale((double)(point.x+dx)/point.x,1);
           
            // remember center
            int cx = (int) shape.getBounds().getCenterX();
            
            // transform shape
            shape.transform(tf);
            
            // move shape to previous location
            int nx = (int) shape.getBounds().getCenterX();
            
            ((Polygon)shape.getShape()).translate(cx-nx,0);
           // }
        }
        public void draw( Graphics g ) {
            Rectangle r = shape.getRelativeBounds();
            setPoint(r.x + r.width, r.y+r.height/ 2 );
            super.draw( g );
        }
    }
   
    static public class PolyNResizeMarker extends Marker {

        PolyNResizeMarker( Annotation s ) {
            super( s );
            setCursorState(Cursor.N_RESIZE_CURSOR);
        }

        public void moveBy( int dx, int dy ) {
            //if(shape instanceof PolygonShape){
                  
            AffineTransform tf = new AffineTransform();
            tf.scale(1,(double)point.y/(point.y+dy));
           
            // remember center
            int cy = (int) shape.getBounds().getCenterY();
            
            // transform shape
            shape.transform(tf);
            
            // move shape to previous location
            int ny = (int) shape.getBounds().getCenterY();
            
            ((Polygon)shape.getShape()).translate(0,cy-ny);
            //}
        }
        public void draw( Graphics g ) {
            Rectangle r = shape.getRelativeBounds();
            setPoint(r.x + r.width/2, r.y );
            super.draw(g);
        }
    }
    
    static public class PolyEndMarker extends Marker {
        private Point lp;
        private Rectangle view;
    	public PolyEndMarker( Annotation s ) {
            super(s);
            setCursor(AnnotationManager.getCursor(shape));
        }

        public void moveBy( int dx, int dy ) {
        	// reseed previous point when there is none or when view changes
        	if(lp == null || !shape.getViewer().getViewRectangle().equals(view)){
        		lp = point;
        		view = shape.getViewer().getViewRectangle();
        	}
        	lp = new Point(lp.x+dx,lp.y+dy);
        	shape.addRelativePoint(lp.x,lp.y);
        }

        public void draw( Graphics g ) {
            if(shape instanceof PolygonShape){
                PolygonShape poly = (PolygonShape) shape;
                Point p = poly.getRelativeLastVertex();
                setPoint(p.x,p.y);
            }
            super.draw(g,Color.red);
        }
    }
    
    
    static public class PolyWRotateMarker extends Marker {

        PolyWRotateMarker( Annotation s ) {
            super( s );
            //setCursorState(Cursor.NE_RESIZE_CURSOR );
            String l = AnnotationManager.rotateCursorResource;
            Cursor c = AnnotationManager.createCursor("Rotate",l,new Point(2,2));
            setCursor(c);
        }

        public void moveBy( int dx, int dy ) {
            //if(shape instanceof PolygonShape){
            //  remember center
            int cx = (int) shape.getBounds().getCenterX();
            int cy = (int) shape.getBounds().getCenterY();
            
            
            AffineTransform tf = new AffineTransform();
            tf.rotate(Math.toRadians(5*(-dy)),cx,cy);
           
            // transform shape
            shape.transform(tf);
            //}
        }
        public void draw( Graphics g ) {
            Rectangle r = shape.getRelativeBounds();
            setPoint(r.x, r.y+r.height/ 2 );
            super.draw( g );
        }
    }
   
    static public class PolySRotateMarker extends Marker {

        PolySRotateMarker( Annotation s ) {
            super( s );
            String l = AnnotationManager.rotateCursorResource;
            Cursor c = AnnotationManager.createCursor("Rotate",l,new Point(2,2));
            //setCursorState(Cursor.NW_RESIZE_CURSOR);
            setCursor(c);
        }

        public void moveBy( int dx, int dy ) {
            //if(shape instanceof PolygonShape){
            //  remember center
            int cx = (int) shape.getBounds().getCenterX();
            int cy = (int) shape.getBounds().getCenterY();
                            
            AffineTransform tf = new AffineTransform();
            tf.rotate(Math.toRadians(5*(-dx)),cx,cy);
           
            // transform shape
            shape.transform(tf);
                
           // }
        }
        public void draw( Graphics g ) {
            Rectangle r = shape.getRelativeBounds();
            setPoint(r.x + r.width/2, r.y+r.height);
            super.draw(g);
        }
    }
    
    
    static public class ParallelogramNResizeMarker extends Marker {
		public ParallelogramNResizeMarker( Annotation s ) {
			super( s );
			setCursorState( Cursor.N_RESIZE_CURSOR );
		}
		public void moveBy( int dx, int dy ) {
			double z = shape.getViewer().getScale();
			if(shape instanceof ParallelogramShape){
				ParallelogramShape prl = (ParallelogramShape) shape;
				prl.resize(0,(int)(dy/z));
			}
		}
		public void draw( Graphics g ) {
			if(shape instanceof ParallelogramShape){
				Polygon p = ((ParallelogramShape) shape).getRelativePolygon();
				Point p1 = new Point(p.xpoints[0],p.ypoints[0]);
				Point p2 = new Point(p.xpoints[1],p.ypoints[1]);
				setPoint(p1.x+(p2.x-p1.x)/2,p1.y+(p2.y-p1.y)/2);
			}else{
				Rectangle r = shape.getRelativeBounds();
				setPoint(r.x + r.width/ 2, r.y );
			}
			super.draw( g );
		}
	}
    
    static public class ParallelogramEResizeMarker extends Marker {

		public ParallelogramEResizeMarker( Annotation s ) {
			super( s );
			setCursorState( Cursor.E_RESIZE_CURSOR );
		}

		public void moveBy( int dx, int dy ) {
			//Point d = shape.getViewer().convertViewToImage(new Point(dx,dy));
			double z = shape.getViewer().getScale();
			if(shape instanceof ParallelogramShape){
				ParallelogramShape prl = (ParallelogramShape) shape;
				prl.resize((int)(dx/z),0);
			}
		}
		public void draw( Graphics g ) {
			if(shape instanceof ParallelogramShape){
				Polygon p = ((ParallelogramShape) shape).getRelativePolygon();
				/*
				Rectangle r = p.getBounds();
				Point p1 = null,p2 = null;
				for(int i=0;i<p.npoints;i++){
					// assign side
					p1 = new Point(p.xpoints[i],p.ypoints[i]);
					p2 = new Point(p.xpoints[(i+1)%p.npoints],p.ypoints[(i+1)%p.npoints]);
					if(p1.x >= r.getCenterX() && p2.x >= r.getCenterX())
						break;
				}*/
				Point p1 = new Point(p.xpoints[1],p.ypoints[1]);
				Point p2 = new Point(p.xpoints[2],p.ypoints[2]);
				setPoint(p1.x+(p2.x-p1.x)/2,p1.y+(p2.y-p1.y)/2);
			}else{
				Rectangle r = shape.getRelativeBounds();
				setPoint(r.x + r.width, r.y+r.height/ 2 );
			}
			super.draw( g );
		}
	}
    
    
    static public class ParallelogramWResizeMarker extends Marker {
		public ParallelogramWResizeMarker( Annotation s ) {
			super( s );
			setCursorState( Cursor.W_RESIZE_CURSOR );
		}
		public void moveBy( int dx, int dy ) {
			//Point d = shape.getViewer().convertViewToImage(new Point(dx,dy));
			double z = shape.getViewer().getScale();
			if(shape instanceof ParallelogramShape){
				ParallelogramShape prl = (ParallelogramShape) shape;
				prl.resize(-(int)(dx/z),0);
			}
		}
		public void draw( Graphics g ) {
			if(shape instanceof ParallelogramShape){
				Polygon p = ((ParallelogramShape) shape).getRelativePolygon();
				/*
				Rectangle r = p.getBounds();
				Point p1 = null,p2 = null;
				for(int i=0;i<p.npoints;i++){
					// assign side
					p1 = new Point(p.xpoints[i],p.ypoints[i]);
					p2 = new Point(p.xpoints[(i+1)%p.npoints],p.ypoints[(i+1)%p.npoints]);
					if(p1.x <= r.getCenterX() && p2.x <= r.getCenterX())
						break;
				}
				*/
				Point p1 = new Point(p.xpoints[3],p.ypoints[3]);
				Point p2 = new Point(p.xpoints[0],p.ypoints[0]);
				setPoint(p1.x+(p2.x-p1.x)/2,p1.y+(p2.y-p1.y)/2);
			}else{
				Rectangle r = shape.getRelativeBounds();
				setPoint(r.x + r.width/ 2, r.y );
			}
			super.draw( g );
		}
	}
    
    static public class ParallelogramSResizeMarker extends Marker {

		public ParallelogramSResizeMarker( Annotation s ) {
			super( s );
			setCursorState(Cursor.S_RESIZE_CURSOR );
			//String l = AnnotationManager.rotateCursorResource;
            //Cursor c = AnnotationManager.createCursor("Rotate",l,new Point(2,2));
            //setCursor(c);
		}

		public void moveBy( int dx, int dy ) {
			//Point d = shape.getViewer().convertViewToImage(new Point(dx,dy));
			double z = shape.getViewer().getScale();
			if(shape instanceof ParallelogramShape){
				ParallelogramShape prl = (ParallelogramShape) shape;
				prl.resize(0,-(int)(dy/z));
			}
		}
		public void draw( Graphics g ) {
			if(shape instanceof ParallelogramShape){
				Polygon p = ((ParallelogramShape) shape).getRelativePolygon();
				/*
				Rectangle r = p.getBounds();
				Point p1 = null,p2 = null;
				for(int i=0;i<p.npoints;i++){
					// assign side
					p1 = new Point(p.xpoints[i],p.ypoints[i]);
					p2 = new Point(p.xpoints[(i+1)%p.npoints],p.ypoints[(i+1)%p.npoints]);
					if(p1.y >= r.getCenterY() && p2.y >= r.getCenterY())
						break;
				}
				*/
				Point p1 = new Point(p.xpoints[2],p.ypoints[2]);
				Point p2 = new Point(p.xpoints[3],p.ypoints[3]);
				setPoint(p1.x+(p2.x-p1.x)/2,p1.y+(p2.y-p1.y)/2);
			}else{
				Rectangle r = shape.getRelativeBounds();
				setPoint(r.x + r.width, r.y+r.height/ 2 );
			}
			super.draw( g );
		}
	}
    
    
    
    static public class ParallelogramRotateMarker extends Marker {

    	ParallelogramRotateMarker( Annotation s ) {
            super( s );
            //setCursorState(Cursor.NW_RESIZE_CURSOR );
            String l = AnnotationManager.rotateCursorResource;
            Cursor c = AnnotationManager.createCursor("Rotate",l,new Point(2,2));
            setCursor(c);
        }

        public void moveBy( int dx, int dy ) {
            //  remember center
        	int cx = (int) shape.getBounds().getCenterX();
            int cy = (int) shape.getBounds().getCenterY();
            // relative coordinates
            Point cp = shape.getViewer().convertImageToView(new Point(cx,cy));
        	Point p = getPoint();
                  
            // calculate delta angle
            double a1 = Math.atan2(p.y-cp.y,p.x-cp.x);
            double a2 = Math.atan2(p.y-cp.y+dy,p.x-cp.x+dx);
            double a = a2 -a1;
            
            //transform shape
            AffineTransform tf = new AffineTransform();
            tf.rotate(a,cx,cy);
            shape.transform(tf);
        }
        public void draw( Graphics g ) {
        	if(shape instanceof ParallelogramShape){
				Polygon p = ((ParallelogramShape) shape).getRelativePolygon();
				setPoint(p.xpoints[3],p.ypoints[3]);
			}else{
				Rectangle r = shape.getRelativeBounds();
				setPoint(r.x, r.y+r.height/ 2 );
			}
            super.draw(g,Color.GRAY);
        }
    }
} // end Marker class
