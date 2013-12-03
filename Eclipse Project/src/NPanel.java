import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.ListIterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import javax.swing.*;


public class NPanel extends JPanel implements MouseListener,MouseMotionListener, ActionListener {

	int WIDTH; //width of the drawing pane
	int HEIGHT; // hight of the drawing pane
	public static final int TOOLBAR_HEIGHT = 40; //the height of the strip of buttons at the bottom of the screen
	static final int END_LINE_BAND=30; // the black "end polyline" button on the left side of the screen
	static final int COLOR_BAND = 30; // the width of the color selection bar on the right of the screen
	static final Color[] DRAW_COLORS = {Color.BLACK,Color.RED,Color.BLUE,Color.GREEN,Color.YELLOW}; //the array of drawable colors
	
	boolean newLine=true; //true if a line is not currently being drawn
	int mx,my; // mouse coordinates	
	int mouseButton=0; 
	
	int lx,ly; // last mouse coordinates
	BufferedImage image; //the image which the lines are drawn to as a buffer
	private ArrayList<PLine> polys; //list of all the polylines
	int curCol = 0; //the color currently selected
	boolean ortho; //true if orthographic snap is on
	boolean snap; //true if snapping to previous points is on
	boolean circleM; //true if in circle mode
	public final int MAX_SNAP_DIST = 10; //the furthest the mouse can be from a point to snap to it
	Point close; // the closest point to the mouse
	
	final int BUTTON_WIDTH=80; //width of the buttons at the bottom of the screen
	final Color MOUSE_OVER_COLOR = Color.MAGENTA; //the color buttons change to when the mouse is over them
	final Color NOT_OVER_COLOR = new Color(200,200,200); //the color buttons are when the mouse is not over them
	
	//whether or not the mouse is over each button
	boolean mouseOverSave = false;
	boolean mouseOverOpen = false;
	boolean mouseOverNew = false;
	boolean mouseOverPrint = false;
	
	//the file chooser dialoge for picking where save and open files
	final JFileChooser fc = new JFileChooser();
	
	
	//constructor
	public NPanel(boolean a) {
		super(a);
		setLayout(null);
		init();
	}

	//Initializes the window by adding the mouse listener and keybindings
	private void init() {
		fc.setFileFilter(new NFilter());
		polys = new ArrayList <PLine> ();
		addMouseListener (this);
		addMouseMotionListener (this);
		
		this.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "spaceD");
		this.getInputMap().put(KeyStroke.getKeyStroke("released SPACE"), "spaceU");
		this.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "enter");
		this.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "enter");
		this.getInputMap().put(KeyStroke.getKeyStroke("BACKSPACE"), "undo");
		this.getInputMap().put(KeyStroke.getKeyStroke("U"), "undo");
		this.getInputMap().put(KeyStroke.getKeyStroke("S"), "snapOn");
		this.getInputMap().put(KeyStroke.getKeyStroke("released S"), "snapOff");
		this.getInputMap().put(KeyStroke.getKeyStroke("C"), "circleMode");
		this.getInputMap().put(KeyStroke.getKeyStroke("released C"), "circleOff");
		
		Action circleMode = new AbstractAction() {
		    public void actionPerformed(ActionEvent e) {
		        circleM = true;
		    }
		};
		
		Action circleOff = new AbstractAction() {
		    public void actionPerformed(ActionEvent e) {
		        circleM = false;
		        newLine=true;
		    }
		};
				
		Action spaceDown = new AbstractAction() {
		    public void actionPerformed(ActionEvent e) {
		        ortho = true;
		    }
		};
		
		Action spaceUp = new AbstractAction() {
		    public void actionPerformed(ActionEvent e) {
		        ortho = false;
		    }
		};
		
		Action snapOn = new AbstractAction() {
		    public void actionPerformed(ActionEvent e) {
		        snap = true;
		    }
		};
		
		Action snapOff = new AbstractAction() {
		    public void actionPerformed(ActionEvent e) {
		        snap = false;
		    }
		};
		
		Action undo = new AbstractAction() {
		    public void actionPerformed(ActionEvent e) {

		        	newLine = false;
		        	if (polys.size()>0)
		        	{
		        		if (!polys.get(polys.size()-1).removeLast())
		        		{
		        			polys.remove(polys.size()-1);
		        		}
		        	}
		        	
		        	//if after removal it still has polyLines
		        	if (polys.size()>0)
		        	{
		        		PLine last = polys.get(polys.size()-1);
		        		lx = last.getLast().x;
		        		ly = last.getLast().y;
		        		curCol = last.getColor();
		        	}
		        	else
		        	{
		        		newLine =true;
		        	}
	        		paint();
		        }
		    	
		};
		
		Action enter = new AbstractAction() {
		    public void actionPerformed(ActionEvent e) {
		        newLine = true;
		    }
		};
		
		this.getActionMap().put("spaceD",spaceDown);
		this.getActionMap().put("spaceU", spaceUp);
		this.getActionMap().put("enter", enter);
		this.getActionMap().put("undo", undo);
		this.getActionMap().put("snapOn",snapOn);
		this.getActionMap().put("snapOff", snapOff);
		this.getActionMap().put("circleMode",circleMode);
		this.getActionMap().put("circleOff", circleOff);
	}
	
	
	//draws a small magenta square around a point if it is being snapped to
	public void drawSnap (Graphics2D g2,Point p)
	{
		final int SQUARE_SIZE= 3; 
		g2.setStroke( new BasicStroke(2.0f));
		g2.setColor(Color.MAGENTA);
		g2.drawRect(p.x-SQUARE_SIZE
					,p.y-SQUARE_SIZE
					,SQUARE_SIZE * 2
					,SQUARE_SIZE * 2);
	}

	//draws the array of polylines, buttons and currently being drawn shapes to the screen
  public void paint() {
		
		Graphics g = this.getGraphics();
	   if (image == null) {
			WIDTH = this.getWidth();
	        HEIGHT = this.getHeight()- TOOLBAR_HEIGHT;
            image = (BufferedImage)(this.createImage(WIDTH,HEIGHT+TOOLBAR_HEIGHT));
        }
	   
		Graphics2D gp = image.createGraphics();
		RenderingHints rh = new RenderingHints(
	             RenderingHints.KEY_ANTIALIASING,
	             RenderingHints.VALUE_ANTIALIAS_ON);
		
		gp.setRenderingHints(rh);
		gp.setColor(new Color(255,255,255));
		gp.fillRect(0,0,WIDTH,HEIGHT);
		gp.setColor(Color.BLACK);
		gp.fillRect(0, 0, END_LINE_BAND, HEIGHT+TOOLBAR_HEIGHT);
	    gp.setColor(DRAW_COLORS[curCol]);
        gp.setStroke( new BasicStroke(2.0f));
        
        int numC = DRAW_COLORS.length;
        for (int b = 0; b < numC; b++)
        {
        	for (int a = Math.round(1f*b/numC*HEIGHT); a < Math.round(1f*(b+1)/numC*HEIGHT);a++)
        	{
        	gp.setColor(DRAW_COLORS[b]);
        	gp.drawLine(WIDTH-COLOR_BAND, a, WIDTH, a);
        	}
		}
        

        //draws the array of polylines and the snap square
        if (polys!= null)
        {
        	drawPolys(gp,polys);
        	if (snap)
        	{	
        		if (close.distance(new Point(mx,my))<MAX_SNAP_DIST||(ortho && (Math.abs(mx-close.x)<MAX_SNAP_DIST||Math.abs(my-close.getY())<MAX_SNAP_DIST)))
				{
					drawSnap(gp,close);
				}
        	}
        }
        
        //the line being drawn
        if (!newLine && mx>END_LINE_BAND && mx < WIDTH-COLOR_BAND && my<HEIGHT)
        {
      		gp.setColor (DRAW_COLORS[curCol]);
      		if (circleM)
      		{
      			gp.drawOval(Math.min(lx,mx),Math.min(ly,my), Math.abs(mx-lx),Math.abs(my-ly));
      		}
      		else
      		{
            	gp.drawLine(lx,ly, mx, my);
      		}
        }
        
        //buttons:
        if (mouseOverOpen)
        {
        	gp.setColor(MOUSE_OVER_COLOR);
        }
        else
        {	
        	gp.setColor(NOT_OVER_COLOR);
        }
        gp.fillRect(BUTTON_WIDTH+END_LINE_BAND, HEIGHT, BUTTON_WIDTH, TOOLBAR_HEIGHT);
        
        if (mouseOverNew)
        {
        	gp.setColor(MOUSE_OVER_COLOR);
        }
        else
        {	
        	gp.setColor(NOT_OVER_COLOR);
        }
        gp.fillRect(END_LINE_BAND+2*BUTTON_WIDTH, HEIGHT, BUTTON_WIDTH, TOOLBAR_HEIGHT);
        
        if (mouseOverPrint)
        {
        	gp.setColor(MOUSE_OVER_COLOR);
        }
        else
        {	
        	gp.setColor(NOT_OVER_COLOR);
        }
        gp.fillRect(BUTTON_WIDTH*3+END_LINE_BAND, HEIGHT, WIDTH-BUTTON_WIDTH*3+END_LINE_BAND-60, TOOLBAR_HEIGHT);
        
        if (mouseOverSave)
        {
        	gp.setColor(MOUSE_OVER_COLOR);
        }
        else
        {	
        	gp.setColor(NOT_OVER_COLOR);
        }
        gp.fillRect(END_LINE_BAND, HEIGHT, BUTTON_WIDTH, TOOLBAR_HEIGHT);
        
        gp.setColor(Color.BLACK);
        gp.drawRect(END_LINE_BAND, HEIGHT, BUTTON_WIDTH, TOOLBAR_HEIGHT);
        gp.drawRect(BUTTON_WIDTH+END_LINE_BAND, HEIGHT, BUTTON_WIDTH, TOOLBAR_HEIGHT);
        gp.drawRect(END_LINE_BAND+2*BUTTON_WIDTH, HEIGHT, BUTTON_WIDTH, TOOLBAR_HEIGHT);
        gp.drawRect(BUTTON_WIDTH*3+END_LINE_BAND, HEIGHT, WIDTH-BUTTON_WIDTH*3+END_LINE_BAND-60, TOOLBAR_HEIGHT);
        gp.setFont(new Font("Ariel", Font.BOLD, 20));
        gp.drawString("SAVE", 42, HEIGHT+27);
        gp.drawString("OPEN", 42+BUTTON_WIDTH, HEIGHT+27);
        gp.drawString("NEW", 46+ 2*BUTTON_WIDTH, HEIGHT +27);
        gp.drawString("SEND TO PRINTER :D", 330, HEIGHT+27);
        
        Graphics2D g2 = (Graphics2D)g;
        g2.drawImage(image, null, 0, 0);
	}
	
  
  //draws an array of polylines to a graphics 2D
	public void drawPolys (Graphics2D g2, ArrayList <PLine> polyLines)
	{
		ListIterator<PLine> i = polyLines.listIterator();
		ListIterator<Point> j; 
		PLine pol;
		Point lastPoint = null;
		Point p;
		while (i.hasNext())
		{
			pol = i.next();
			j =  pol.getPoints().listIterator();
			g2.setColor(DRAW_COLORS[pol.getColor()]);
			if (j.hasNext())
			{
				lastPoint = j.next();
			}
			while (j.hasNext())
			{
				p = j.next();
				g2.drawLine((int)Math.round(lastPoint.getX()),
						(int)Math.round(lastPoint.getY()), 
						(int)Math.round(p.getX()),
						(int)Math.round(p.getY()));
				lastPoint = p;
			}
		}
	}
	
	//finds the closest point to the mouse from all the points in the array of polylines
	public Point findClosest(ArrayList <PLine> polyLines)
	{
		ListIterator<PLine> i = polyLines.listIterator();
		ListIterator<Point> j; 
		PLine pol;
		Point closest = null;
		Point p;
		double closestDist=Double.MAX_VALUE;
		while (i.hasNext())
		{
			pol = i.next();
			j =  pol.getPoints().listIterator();
			while (j.hasNext())
			{
				p = j.next();
				double dist = p.distance(new Point(mx,my));
				if (dist<closestDist)
				{
					closestDist = dist;
					closest = p;
				}
					
			}
		}
		return closest;
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		paint(this.getGraphics());
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}
 
	@Override
	public void mouseEntered(MouseEvent arg0) {
			}
	
	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	//this method is called every time the mouse is moved. 
	//It recalculated the position of the mouse, checks if the mouse is over a button,
	//adjusts the interpreted mouse position based on the drawing mode and calls the paint() function
	public void mouseMoved(MouseEvent e)
	{	
		mx = e.getX();
		my = e.getY();
		
		mouseOverSave=false;
		mouseOverOpen = false;
		mouseOverNew = false;
		mouseOverPrint = false;
		if(my > HEIGHT && my < HEIGHT + TOOLBAR_HEIGHT)
		{
			if (mx > END_LINE_BAND && mx < END_LINE_BAND + BUTTON_WIDTH)
			{
				mouseOverSave=true;
			}
			if (mx > END_LINE_BAND + BUTTON_WIDTH && mx < END_LINE_BAND + BUTTON_WIDTH*2)
			{
				mouseOverOpen=true;
			}
			if (mx > END_LINE_BAND + BUTTON_WIDTH*2 && mx < END_LINE_BAND + BUTTON_WIDTH*3)
			{
				mouseOverNew=true;
			}
			if (mx > END_LINE_BAND + BUTTON_WIDTH*3 && mx < WIDTH-60)
			{
				mouseOverPrint=true;
			}
			
		}	
		
		if (snap)
		{
			close = findClosest(polys);
			if (close.distance(new Point(mx,my))<MAX_SNAP_DIST||(ortho && (Math.abs(mx-close.x)<MAX_SNAP_DIST||Math.abs(my-close.getY())<MAX_SNAP_DIST)))
			{
				mx = close.x;
				my = close.y;
			}
		}	
		if (!newLine)
		{

			if (ortho)
			{
				if (circleM)
				{
					if(Math.abs(mx-lx)<Math.abs(my-ly))
					{
						if (my-ly>0)
							my = ly + Math.abs(mx - lx);
						else
							my = ly - Math.abs(mx - lx);
					}
					else
					{
						if (mx-lx>0)
							mx = lx + Math.abs(my - ly);
						else
							mx = lx - Math.abs(my - ly);
					}
				}
				else
				{
					if(Math.abs(mx-lx)<Math.abs(my-ly))
					{
						mx = lx;
					}
					else
					{
						my = ly;
					}	
				}
			}
		}
		paint();
	}
	
	//called when the mouse is pressed, this method controls the buttons and adds polylines when the user clicks
	public void mousePressed(MouseEvent e) {
		if (mouseOverSave)
			saveFile();
		if (mouseOverOpen)
		{
			openFile();
		}
		if (mouseOverNew)
		{
			polys = new ArrayList<PLine>(); 
			newLine = true;
		}
		if (mouseOverPrint)
		{
			System.out.println("printed");
			print (new File("C:\\Users\\Lior\\Documents\\School\\final project\\nxdFiles\\printReady.nxp"));
			JOptionPane.showMessageDialog(this,"File Readied for printing.");
		}
		
		if (my < HEIGHT && my > 0)
        {
			if (mx>END_LINE_BAND&& mx < WIDTH-COLOR_BAND && my<HEIGHT)
				{
				if (circleM)
				{
					if (newLine)
					{
						lx = mx;
						ly = my;
						newLine = false;
					}
					else
					{
						if (ortho)
						{
							polys.add(new PLine(curCol,lx,ly,mx,my,36,true));
							newLine = true;
						}
						else
						{
							polys.add(new PLine(curCol,lx,ly,mx,my,36,false));
							newLine = true;
						}
					}
				}	
				else
				{
					if (newLine)
					{
						polys.add(new PLine(curCol));
					}
					polys.get(polys.size()-1).addPoint(mx, my);
				    newLine = false;
					lx = mx;
					ly = my;
				}
			}
			else
			{
		        if (mx > WIDTH-COLOR_BAND)
		        {
		        	curCol = (int) Math.floor(1f*DRAW_COLORS.length*my/HEIGHT);
		        	
		        }
				newLine = true;
			}
        }
	}
	

	//opens and reads in a file
	private void openFile() {
       int returnVal = fc.showOpenDialog(this);
       if (returnVal == JFileChooser.APPROVE_OPTION) {
           File file = fc.getSelectedFile();
           BufferedReader in;
           try {
        	   	in = new BufferedReader (new FileReader(file));
        	   	String line;
        	   	int numPolys = Integer.parseInt(in.readLine());
        	   	polys = new ArrayList <PLine>();
        	   	for (int i = 0 ; i<numPolys;i++)
        	   	{	
        	   		line = in.readLine();
        	   		System.out.println(line);
        	   		polys.add(new PLine(line));
        	   	}
        	   	 
           } catch (Exception e) {
        	   e.printStackTrace();
           }
       } else {
           
       }
	}

	//pops up a dialogue asking where to save the file and what to call it. 
	//if the file doesn't end in ".nxd", it is appended 
	//the save funtion is then called to write the information to the file
	private void saveFile() {
		int returnVal = fc.showSaveDialog(this);
	       if (returnVal == JFileChooser.APPROVE_OPTION) {
	           File file = fc.getSelectedFile();
	           if (!new NFilter().accept(file))
	           {
	        	   file = new File(file.getPath()+".nxd");
	           }
	           save (file);
	       } else {
	           
	       }
	}

	//returns a passed in array, sorted by color
	public ArrayList <PLine> sortPolys (ArrayList<PLine> p)
	{
		ArrayList <PLine> sorted = new ArrayList <PLine> ();
		PLine pl;
		for (int c = 0;c<DRAW_COLORS.length;c++)
		{
			ListIterator<PLine> i = p.listIterator();
			while (i.hasNext())
			{
				pl = i.next();
				if (pl.getColor() == c)
				{
					sorted.add(pl);
				}
			}
		}
		return sorted;
	}
	
	//writes the drawing to a file
	public void save (File file)
	{
        BufferedWriter out;
        try {
     	   	out = new BufferedWriter (new FileWriter(file));
     	   	String size = "" +polys.size();
     	   	out.write(size);
     	   ListIterator<PLine> i;
     	   out.newLine();
    	   		 i = polys.listIterator();
     		PLine pol;
     		while (i.hasNext())
     		{
     			pol = i.next();
     			out.write(pol.toString());
     			out.newLine();
     		}
     	   	out.close();
     	   	 
        } catch (Exception e) {
     	   e.printStackTrace();
        }
	}
	
	//writes the drawing to a file based on protocols shared with the printer's read methods
	public void print (File file)
	{
        BufferedWriter out;
        int presColor=0;
        try {
     	   	out = new BufferedWriter (new FileWriter(file));
     	   ListIterator<PLine> i;
     	  
     	   if (polys.size()!=0)
     	   { 
     		  presColor = polys.get(0).getColor();
     	   }
    	   	out.write(HEIGHT+" "+WIDTH);
    	   	
    	   	out.write(" C " + presColor);
    	   	//out.newLine();
    	   	i = sortPolys (polys).listIterator();

     		PLine pol;
     		while (i.hasNext())
     		{
     			pol = i.next();
     			if (pol.getColor()!=presColor)
     			{
     				presColor=pol.getColor();
     				out.write(" C " + presColor);
     			}
     			out.write(pol.printLine(WIDTH,HEIGHT));
     		}
     		out.write(" E");
     	   	out.close();
     	   	 
        } catch (Exception e) {
     	   e.printStackTrace();
        }
	}
	
	@Override
	public void mouseReleased(MouseEvent arg0) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

}
