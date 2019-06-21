import java.io.*;
import ij.*;
import ij.io.*;
import ij.plugin.PlugIn;
import ij.gui.*;


// This plugin imports Raw data. 
// Shanrong Zhang, 12/31/03

public class RawImporter implements PlugIn {

	private static String defaultDirectory = null;

	public void run(String arg) {
		OpenDialog od = new OpenDialog("Open Raw...", arg);
		String directory = od.getDirectory();
		String fileName = od.getFileName();
		if (fileName==null)
			return;
		ImportDialog d = new ImportDialog(fileName, directory);
		d.openImage();
	}
}
