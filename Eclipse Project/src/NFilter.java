import java.io.File;

import javax.swing.filechooser.FileFilter;

//the NFilter class only allows the user to open .nxt files
public class NFilter extends FileFilter {

	public NFilter() {
	}

	@Override
	public boolean accept(File f) {
		if (f.isDirectory()) {
	        return true;
	    }
		
		String[] name = f.getName().split("\\.");
		if (name[name.length-1].compareToIgnoreCase("nxd")==0)
			return true;
		return false;
	}

	@Override
	public String getDescription() {
		return ".nxd files";
	}

}
