/**
 * @author Selina Lui
 * UW Department of Radiology
 * 
 * Summary of PlugIn:
 * Performs preliminary MTR Analysis by obtaining the MT Ratio Map.
 * The final output will need to be saved by the user.
 * 
 * Created 8.23.17 - Initially testing directory calls
 * 		   8.24.17 - Adding capability to find slopes from visu_pars
 * 				   - Added slope mutiplication
 * 		   8.29.17 - Finialized the output of the averaged nmt and ymt images
 * 				   - First clean-up of code
 * 				   - Created Mac version. Must change class variables if
 * 					 running on MacOS
 * 		   8.31.17 - Updating Mac version compile and run on MacOS
 * 				   - Adding the image closer private method
 */

import java.util.*;
import java.io.*;

import ij.*;
import ij.io.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;

public class MT_Ratio implements PlugIn {
	/*
	// For Windows:
	// Ctrl + F for "If running Windows"
	public static final String PATHS = "pdata\\1";
	public static final String IM = "\\2dseq"; // 2dseq image file
	public static final String VIS_PAR = "\\visu_pars";
	*/
	
	// For Macs:	
	public static final String PATHS = "pdata/1";
	public static final String IM = "/2dseq";
	public static final String VIS_PAR = "/visu_pars";
	
	
	public void run(String arg) {
		// Opens the directories for _nmt and _ymt paths
		IJ.log("Please c"
				+ "hoose a directory where the NMT folder is " +
				"the last item in the path (i.e. 7_nmt)");
		String nmtDirectory = this.grabDir("NMT");
		IJ.log("Please c"
				+ "hoose a directory where the YMT folder is " +
				"the last item in the path (i.e. 8_ymt)");
		String ymtDirectory = this.grabDir("YMT");
		this.mtDirChecker(nmtDirectory, ymtDirectory);
			
		// Get Slopes for each image
		Map<String, Double> nmtRatio = null;
		Map<String, Double> ymtRatio = null;
		try {
			String slope = "##$VisuCoreDataSlope=(";
			nmtRatio = this.getSlope(nmtDirectory+PATHS, 
					slope);
			ymtRatio = this.getSlope(ymtDirectory+PATHS, 
					slope);
			IJ.log("The slope that was retreived for _nmt is: " 
					+ nmtRatio.get("slope"));
			IJ.log("The slope that was retrieved for _ymt is: "
					+ ymtRatio.get("slope"));
		} catch (FileNotFoundException e) {
			IJ.error("Slope Information was not have been found.");
		}
		
		// Open image files
		Opener op = new Opener();
		ImagePlus nIm = op.openImage(nmtDirectory + PATHS, IM.substring(1));
		nIm = this.modify(nIm, nmtRatio.get("slope"), nmtDirectory,
				"nNorm.tif");
		ImagePlus yIm = op.openImage(ymtDirectory + PATHS, IM.substring(1));
		yIm = this.modify(yIm, ymtRatio.get("slope"), ymtDirectory,
				"yNorm.tif");
		
		ImageCalculator ic = new ImageCalculator();
		nIm = WindowManager.getImage("nNorm.tif");
		yIm = WindowManager.getImage("yNorm.tif");
		ImagePlus subRes = ic.run("Subtract create stack", nIm, yIm);
		subRes.show();
		ImagePlus res = ic.run("Divide create stack", subRes, nIm);
		res.show();
		IJ.run(res, "Multiply...", "value=100 stack");
		
		// Closes Images
		this.closer(nIm);
		this.closer(yIm);
		this.closer(subRes);
	}
	
	/** 
	 * Input from user to grab the root directory for NMT and YMT files
	 * @param typeMT either No or Yes MT
	 * @returns the directory of each MT file path in a string 
	 */
	private String grabDir(String typeMT) {
		DirectoryChooser path = new DirectoryChooser("Select " + typeMT
				+ " Path");
		String dir = path.getDirectory();
		IJ.log("Directory return for " + typeMT + ": " + dir);
		return dir;
	}
	
	/** 
	 * Checks if the input directory strings are the same
	 * @param nmtDir directory for NMT files
	 * @param ymtDir directory for YMT files
	 * @throw IllegalArgumentException if the given directories are the same
	 */
	private void mtDirChecker(String nmtDir, String ymtDir) {
		if (nmtDir.equalsIgnoreCase(ymtDir)) {
			throw new IllegalArgumentException("NMT and YMT directories are" +
					" the same paths");
		}
	}
	
	/**
	 * Opens any file
	 * @param file name of the file
	 * @param dir the directory where the file lives
	 * @return the FileInfo that can be used to open the image
	 */
	private FileInfo openFiles(String file, String dir) {		
		ImportDialog currFile = new ImportDialog(file, dir);
		return currFile.getFileInfo();
	}
	
	/**
	 * Grabs the VisuCoreDataSlope data from the visu_par file.
	 * Assumes the file structure in the visu_pars file will be consistent
	 * @param vp the name of the file that contains an image's parameters
	 * @param find item that needs to be found within the visu_par file
	 * @return slope the slope that multiplies the image data
	 * @return zero if the slope cannot be found
	 * @throws FileNotFoundException
	 */	
	private Map<String, Double> getSlope(String dir, String find) 
			throws FileNotFoundException {
		
		File vp = new File(dir + VIS_PAR);
		if (!vp.exists()) {
			throw new FileNotFoundException();
		}
		Map<String, Double> pars = new HashMap<String, Double>();
		Scanner scanner = new Scanner(vp); 
		while (scanner.hasNext()) {
			String currItem = scanner.next();
			if (currItem.equalsIgnoreCase(find)) {
				if (scanner.hasNext()) {
					pars.put("numImages", scanner.nextDouble()); // Number of images
					scanner.next(); // Discard ')'
					if (scanner.hasNextDouble()) {
						pars.put("slope", scanner.nextDouble());
						return pars;
					}
				}
			}
		}		
		return pars;
	}
	
	/**
	 * Modifies the ImagePlus file to be 32-bits and mutlipled by its slope
	 * @param imp ImagePlus file that takes holds the images
	 * @param slope the value that the images are all multiplied by
	 * @return the modified ImagePlus file
	 */
	private ImagePlus modify(ImagePlus imp, double slope, String dir,
			String title) {
		IJ.run(imp, "32-bit", "");
		IJ.run(imp, "Multiply...", "value=" + slope + " stack");
		IJ.run(imp, "Save", "save=[" + dir + "/" + title + "]"); 
		// If running Windows, switch String "/" with "\\"
		return imp;
	}

	/**
	 * Closes the image that has the associated title
	 * @param imp the ImagePlus object that will be closed
	 */
	private void closer(ImagePlus imp) {
		if (imp != null) {
			imp.changes = false;
			imp.close();
		}
	}
}
