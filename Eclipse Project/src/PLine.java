
import java.awt.Point;
import java.util.ArrayList;
import java.util.ListIterator;


public class PLine {
	int col; //the index of the color of the polyline
	ArrayList<Point> polyLine; //the list of points composing the polyline
	
	//the constructor for a new polynine with no points
	public PLine(int color) {
		polyLine = new ArrayList<Point>(); 
		col = color;
	}
	
	//constructor to make an oval polyline within the rectangle defined by x2,y1 and x2,y2 
	public PLine (int color,int x1,int y1, int x2,int y2,int numSegments,boolean circular)
	{
		col=color;
		double xC = (x1 + x2)/2.0;
		double yC = (y1 + y2)/2.0;

		double xRad = Math.abs(xC-x1);
		double yRad;
		if (circular)
		{
			yRad = xRad;
		}
		else
		{
			yRad = Math.abs(yC-y1);
		}
		
		polyLine = new ArrayList<Point>();
		double angle;
		for (int i = 0; i <= numSegments; i ++)
		{
			angle = 2*Math.PI/numSegments * i;
			polyLine.add(new Point((int)Math.round(xC+xRad*Math.cos(angle)),(int)Math.round(yC+yRad*Math.sin(angle))));
		}
	}
	
	//creates a polyline from a string, this is for opening a file
	public PLine (String s)
	{
		polyLine = new ArrayList<Point>(); 
		String [] sSplit = s.split(" ");
		col = Integer.parseInt(sSplit[0]);
		
		int numLines = Integer.parseInt(sSplit[1]);
		
		for (int i =0 ; i< numLines; i++ )
		{
			polyLine.add(new Point(Integer.parseInt(sSplit[2+i*2]),Integer.parseInt(sSplit[3+i*2])));
		}
	}
	
	public int getColor()
	{
		return col;
	}
	
	public void addPoint (Point p)
	{
		polyLine.add(p);
	}
	
	public void addPoint (int x, int y)
	{
		polyLine.add(new Point(x,y));
	}
	
	public ArrayList <Point> getPoints ()
	{
		return polyLine;
	}

	//returns true is there are any points in the polyline
	public boolean notEmpty ()
	{
		if (polyLine.size()!=0)
			return true;
		return false;
	}
	
	//removes the last point in the arraylist of points
	//will return false if there are no points to remove
	public boolean removeLast ()
	{
		if (polyLine.size()>1)
		{	
			polyLine.remove(polyLine.size()-1);
			return true;
		}
		return false;
	}
	
	//returns the last point in the line
	public Point getLast ()
	{
		return polyLine.get(polyLine.size()-1);
	}
	
	
	//returns a string with all the information of the polyline (color, size and points)
	public String toString ()
	{
		String r = "";
			r = col+" "+polyLine.size();
		ListIterator <Point> i = polyLine.listIterator();
		Point p;
		boolean first = true;
		while (i.hasNext())
		{
			p = i.next();
			r += " "+ p.x + " " + p.y;
		}
		return r;
	}
	
	//returns a string with the information of the polyline in the format expected by the printer
	//it is centered at 0,0 and has characters between points so the printer knows when to put the pen down
	public String printLine(int w,int h)
	{
		String r = " U";
		ListIterator <Point> i = polyLine.listIterator();
		Point p;
		boolean first = true;
		while (i.hasNext())
		{
			p = i.next();
			if (!first)
			{	
				r+= " P";
			}
			else
				first = false;
			r += " "+ (int)(p.y - h/2.0) + " " + (int)(p.x -w/2.0);
		}
		return r;
	}
	
}
