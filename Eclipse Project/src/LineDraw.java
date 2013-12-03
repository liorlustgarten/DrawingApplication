import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;


public class LineDraw 
{
	public static final int WIDTH = 900;
	public static final int HEIGHT = 450;	
	
	
	//the main funcion for the program
	public static void main(String[] args) {
		
	    JFrame f = new JFrame("LEGO NXT DRAWING");
	    
	    //instantiate the NPanel class
	    NPanel d = new NPanel (true);
	    
	    f.setSize(WIDTH, HEIGHT);

	    f.add(d);
	    f.setVisible(true);
	    f.createBufferStrategy(2);
	    f.addWindowListener(new WindowAdapter(){
	    	public void windowClosing(WindowEvent e)
	    	{
	    		System.exit(0);
	    	}
	    });
	}
}
