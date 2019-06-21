/**
 * @author Selina Lui
 * UW Department of Radiology
 * 
 * 8.29.17 -Created Script based of MT_Ratio.java
 * 8.30.17 -Added functionallity to gather slopes and generate substack
 * 		   -Reduced down the method calls in run method	
 * 8.31.17 -Rearranging order of the instructions displayed in log
 * 11.03.17 - Fixing bug that made only 50 images be processed and not any number
 * 			  that is found in the 2dseq file
 * 
 * This method performs the T1 and T2 analysis of post processed 2dseq image
 * stacks. User needs to give the root directory that ends at /t1/ or /t2/
 * in the file paths. Users will also need to perform the auto contrast in
 * ImageJ
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

public class T2_Analysis_mouse implements PlugIn {
	
	/**
	// For Windows:
	public static final String PATHS = "pdata\\2";
	public static final String IM = "\\2dseq"; // 2dseq image file
	public static final String VIS_PAR = "\\visu_pars";
	*/
	
	
	// For Macs:	
	public static final String PATHS = "pdata/2";
	public static final String IM = "/2dseq";
	public static final String VIS_PAR = "/visu_pars";
	
	
	public static final String DESIRED_IMAGE = "3";
	public static final int NUMBER_IN_SET = 5;

	public void run(String arg) {
		// Opens the directory for T folder
		IJ.log("Please c"
				+ "hoose a directory where the T1 or T2 folder is " +
				"the last item in the path (i.e. 5_t1 or 6_t2)");
		String tDir = this.grabDir("General T folder");		
		
		// Get 2dseq stack
		ImagePlus curr = this.getImage(tDir);
		IJ.log("Print num images: " + curr.getNSlices());
		
		// Image Slice Indices
		String sliceNums = this.strStackBuilder(curr.getNSlices());	
		
		// Get slopes
		double[] slopes = null;
		try {
			slopes = this.getSlope(tDir + PATHS, "##$VisuCoreDataSlope=(",
					sliceNums);
		} catch (FileNotFoundException e) {
			IJ.error("The visu_par file was not found.");
		}
		
		// Multiply corresponding slope values to each image in stack
		curr = this.modifyStack(curr, slopes, sliceNums);
			
		// Autocontrast done by user
		IJ.log("Please adjust auto-contrast: Command + Shift + C -> Select \"Auto\"");
		
		// Closes the rescaled 2dseq
		this.closer("2dseq_256_256");
		
	}
	
	/** 
	 * Input from user to grab the root directory for desired files
	 * @param folder, folder that contains desired files
	 * @returns the directory of each folder path in a string 
	 */
	private String grabDir(String folder) {
		DirectoryChooser path = new DirectoryChooser("Select " + folder
				+ " Path");
		String dir = path.getDirectory();
		IJ.log("Directory return for " + folder + ": " + dir);
		return dir;
	}

	/**
	 * Grabs the VisuCoreDataSlope data from the visu_par file.
	 * Assumes the file structure in the visu_pars file will be consistent
	 * @param vp the name of the file that contains an image's parameters
	 * @param find item that needs to be found within the visu_par file
	 * @param indx index values that are desired
	 * @return slope the slope that multiplies the image data
	 * @return zero if the slope cannot be found
	 * @throws FileNotFoundException
	 */	
	private double[] getSlope(String dir, String find, String indx)
			throws FileNotFoundException {
		
		File vp = new File(dir + VIS_PAR);
		if (!vp.exists()) {
			throw new FileNotFoundException();
		}
		double[] pars = null;
		Scanner scanner = new Scanner(vp); 
		while (scanner.hasNext()) {
			String currItem = scanner.next();
			if (currItem.equalsIgnoreCase(find)) {
				if (scanner.hasNext()) {
					// Number of images
					int numIm = scanner.nextInt(); // Number of Images
					scanner.next(); // Discard ')'
					pars = new double[numIm / NUMBER_IN_SET];
					double currSlope;
					int arrIndex = 0;
					String[] ind = indx.split(",");
					// Search for particular slopes
					int i = 1;
					while (i <= numIm && arrIndex < ind.length) {
 						currSlope = scanner.nextDouble();
 						if (i == Integer.parseInt(ind[arrIndex])) {
 							pars[arrIndex] = currSlope;
 							arrIndex++;
						}
 						i++;
					}
 					return pars;
				}
			}
		}
		IJ.error("The visu_par file did not read any slope data.");
		return pars;
	}
	
	/**
	 * Builds the string that will create a substack
	 * i.e. returns "3,8,13,18,..." for the 3rd image in each set of five
	 * @param numImages number of images in the stack
	 * @return String that has the slice numberID of the images that will be in
	 * the final stack
	 */
	private String strStackBuilder(int numImages) {
		String returnSlices = DESIRED_IMAGE;
		for(int i = Integer.parseInt(DESIRED_IMAGE) + NUMBER_IN_SET; 
				i <= numImages; i += NUMBER_IN_SET) {
			returnSlices += "," + i;
		}
		return returnSlices;
	}
	
	/**
	 * Closes the image that has the associated title
	 * @param title the image's title or name in the Window Manager
	 */
	private void closer(String title) {
		ImagePlus imp = WindowManager.getImage(title);
		if (imp != null) {
			imp.changes = false;
			imp.close();
		}
	}
	
	/**
	 * Retrieves image and scales to correct size
	 * @param tDir directory that the images files are located at
	 * @return ImagePlus of the scaled image stack
	 */
	private ImagePlus getImage(String tDir) {
		// Import Instructions to user
		this.instructions();
		
		IJ.openImage(tDir + PATHS + IM);		
		
		ImagePlus curr = WindowManager.getImage(IM.substring(1));
		int numIm = curr.getNSlices();
		
		IJ.run(curr, "Scale...", "x=- y=- z=1.0 width=256 height=256 depth=" + numIm
				+ " interpolation=Bilinear average process create " + 
				"title=2dseq_256_256");
		
		// Change current to 2dseq_256_256
		this.closer(IM.substring(1));
		curr = WindowManager.getImage(IM.substring(1) + "_256_256");
		return curr;
	}
	
	/**
	 * Provides instructions to Import raw image files
	 * @param numIm number of images in the original image stack
	 */
	private void instructions() {
		IJ.log("Please adjust import settings to:");
		IJ.log("    Image type: 32-bit signed");
		IJ.log("    Width: 128 pixels");
		IJ.log("    Height: 256 pixels");
		IJ.log("    Number of images: 50 images");
		IJ.log("    Check Little-endian byte order");
	}
	
	
	/**
	 * Takes in a stack and multiplies by the corresponding slope values
	 * @param curr the stack of images that will be modified
	 * @param slopes the slopes that will multiply the corresponding image
	 * @param sliceNums desired slices within the stack
	 * @return the modified ImagePlus of the stack of images
	 */
	private ImagePlus modifyStack(ImagePlus curr, double[] slopes, String sliceNums) {
		int numImages = curr.getNSlices(); // number of images in stack
		IJ.run(curr, "Substack Maker", "slices=" + sliceNums);
		IJ.run("Stack to Images", "");
		
		// Get the Window ID
		String title = "Substack-00";
		String currTitle;
		ImagePlus curSub;
		for (int j = 1; j <= numImages / NUMBER_IN_SET; j++) {
			if (j < 10) {
				currTitle = title + "0";
			} else if (j < 100){
				currTitle = title;
			} else if (j < 1000) {
				currTitle = title.substring(0, title.length() - 2);
			} else {
				currTitle = title.substring(0, title.length() - 3);
			}
			IJ.log("Image " + currTitle + j + " will be multiplied by: " 
					+ slopes[j - 1]);
			curSub = WindowManager.getImage(currTitle + j);
			IJ.run(curSub, "Multiply...", "value=" + slopes[j - 1]
					+ " stack");			
		}
		IJ.run(curr, "Images to Stack", "name=Stack title=[] use");
		return curr;
	}
}
