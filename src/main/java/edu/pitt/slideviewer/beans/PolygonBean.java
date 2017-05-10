package edu.pitt.slideviewer.beans;

/**
 * To avoid Security problems with koala, each property is a String...
 */
import java.awt.Polygon;

public class PolygonBean extends Polygon {
	private String xPoints;
	private String yPoints;
	private String type;
	private String xStart;
	private String yStart;
	private String xEnd;
	private String yEnd;
	private String width;
	private String height;
	private String tag;
	private String color;
	private String wasLoaded; // is true when loaded from Protege.
	private String name;
	// viewer params
	private String viewX;
	private String viewY;
	private String zoom;
	private String image;

	/**
	 * @return the image
	 */
	public String getImage() {
		// in older systems tag was used as image name
		if(image == null)
			return tag;
		return image;
	}

	/**
	 * @param image the image to set
	 */
	public void setImage(String image) {
		this.image = image;
	}

	public PolygonBean() {
		super();
		setWasLoaded( "false" );
	}

	public PolygonBean( int[] x, int[] y, int n ) {
		super( x, y, n );
		setWasLoaded( "false" );
	}

	public void setWasLoaded( String b ) {
		wasLoaded = b;
	}

	public String getWasLoaded() {
		return wasLoaded;
	}

	public void setName( String n ) {
		name = n;
	}

	public String getName() {
		return name;
	}

	public void setType( String n ) {
		type = n;
	}
	public String getType() {
		return type;
	}

	public void setXStart( String ix ) {
		xStart = ix;
	}
	public String getXStart() {
		return xStart;
	}

	public void setYStart( String iy ) {
		yStart = iy;
	}
	public String getYStart() {
		return yStart;
	}

	public void setXEnd( String ix ) {
		xEnd = ix;
	}
	public String getXEnd() {
		return xEnd;
	}

	public void setYEnd( String iy ) {
		yEnd = iy;
	}
	public String getYEnd() {
		return yEnd;
	}

	public String getWidth() {
		return width;
	}
	public void setWidth( String w ) {
		width = w;
	}

	public String getHeight() {
		return height;
	}
	public void setHeight( String h ) {
		height = h;
	}

	public String getXPoints() {
		return xPoints;
	}

	public String getYPoints() {
		return yPoints;
	}

	public void setXPoints( String x ) {
		xPoints = x;
	}

	public void setYPoints( String y ) {
		yPoints = y;
	}

	public void setTag( String n ) {
		tag = n;
	}
	public String getTag() {
		return tag;
	}

	public void setColor( String n ) {
		color = n;
	}
	public String getColor() {
		return color;
	}

	public void setViewX( String n ) {
		viewX = n;
	}
	public String getViewX() {
		return viewX;
	}

	public void setViewY( String n ) {
		viewY = n;
	}
	public String getViewY() {
		return viewY;
	}

	public void setZoom( String n ) {
		zoom = n;
	}
	public String getZoom() {
		return zoom;
	}

	public void setProperty( String p_name, Object p_val ) {
		if ( p_val == null )
			return ;
		if ( p_name.equalsIgnoreCase( "TAG" ) )
			setTag( p_val.toString() );
		else if ( p_name.equalsIgnoreCase( "TYPE" ) )
			setType( p_val.toString() );
		else if ( p_name.equalsIgnoreCase( "NAME" ) )
			setName( p_val.toString() );
		else if ( p_name.equalsIgnoreCase( "COLOR" ) )
			setColor( p_val.toString() );
		else if ( p_name.equalsIgnoreCase( "XPOINTS" ) )
			setXPoints( p_val.toString() );
		else if ( p_name.equalsIgnoreCase( "YPOINTS" ) )
			setYPoints( p_val.toString() );
		else if ( p_name.equalsIgnoreCase( "XSTART" ) )
			setXStart( p_val.toString() );
		else if ( p_name.equalsIgnoreCase( "XEND" ) )
			setXEnd( p_val.toString() );
		else if ( p_name.equalsIgnoreCase( "YSTART" ) )
			setYStart( p_val.toString() );
		else if ( p_name.equalsIgnoreCase( "YEND" ) )
			setYEnd( p_val.toString() );
		else if ( p_name.equalsIgnoreCase( "WIDTH" ) )
			setWidth( p_val.toString() );
		else if ( p_name.equalsIgnoreCase( "HEIGHT" ) )
			setHeight( p_val.toString() );
		else if ( p_name.equalsIgnoreCase( "WASLOADED" ) )
			setWasLoaded( p_val.toString() );
		else if ( p_name.equalsIgnoreCase( "VIEWX" ) )
			setViewX( p_val.toString() );
		else if ( p_name.equalsIgnoreCase( "VIEWY" ) )
			setViewY( p_val.toString() );
		else if ( p_name.equalsIgnoreCase( "ZOOM" ) )
			setZoom( p_val.toString() );
		else if ( p_name.equalsIgnoreCase( "IMAGE" ) )
			setImage( p_val.toString() );
	}
}