/**
 * @author Selina Lui, Vineeth Sai
 * UW Department of Radiology
 *
 * Generates DTI maps. This script is based of of the ADC_MAP plugin.
 * Takes the Fractional anisotropy, tensor trace, and tensor eigenvalues
 * 1, 2, and 3 to and multiplies each by the slope
 *
 * Created 10.09.17
 * 11.03.17 - Creating infrastructure to grab slopes and open images
 * 11.14.17 - Multiplies slopes with substacks
 * 12.07.17 - Fixing indexing issues
 * 12.21.17 - Adjusting scaling of imported images
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

public class DTI_Map implements PlugIn {

	public static final String VP_SLOPE = "##$VisuCoreDataSlope=(";
	public static final int[] IM_INDEX = {1, 2, 3, 11, 12, 13};
	// Image index corresponds to the accessing Fractional anisotropy, tensor
	// trace, and Tensor eigenvalues 1, 2, and 3.

	/*
	// For Windows:
	public static final String PATHS1 = "pdata\\1";
	public static final String PATHS2 = "pdata\\2";
	public static final String IM = "\\2dseq"; // 2dseq image file
	public static final String VIS_PAR = "\\visu_pars";
	*/



	// For Macs:
	public static final String PATHS1 = "pdata/1";
	public static final String PATHS2 = "pdata/2";
	public static final String IM = "/2dseq";
	public static final String VIS_PAR = "/visu_pars";



public void run(String arg) {

		// Clears previous images if any


		// Gets DTI directory
		IJ.log("Please c"
				+ "hoose a directory where the DTI folder is " +
				"");
		String dir = this.grabDir("DTI");

		// Grabs the images from the selected DTI files
		ImagePlus dti = this.modifyDTI(dir);
		int numSlices = dti.getNSlices();
		IJ.log("There are "+ numSlices + " slices");

		// Grabs the visu_par slopes
		double[] slopes = null;
		try {
			String vpDir = dir + PATHS2; // file directory of the visu_par file
			slopes = this.getSlope(vpDir, VP_SLOPE);
		} catch (FileNotFoundException e) {
			IJ.error("The slopes were not retrieved");
		}

		// Split stacks into the appropriate number
		IJ.run(dti, "Stack Splitter", "number=" + slopes.length);

		int typeInd = 0; // Which index within desired Image indexing
		// Multiplies the slopes to the corresponding desired images
		for (int i = 1; i <= slopes.length; i++) {
			String title = "stk_00";
			if (i < 10) {
				title = title + "0";
			}
			title = title + i + "_2dseq";

			ImagePlus im = WindowManager.getImage(title);
			if (i == IM_INDEX[typeInd]) {
				IJ.run(im, "Multiply...", "value=" + slopes[i - 1] +" stack");
				IJ.log(title + " was multiplied by " + slopes[i - 1]);
				im = this.basicModify(im, title + "scaled");
				if (typeInd < IM_INDEX.length - 1) {
					typeInd++;
				}
			}
			// Close non-essential images
			this.closer(title);
			//IJ.run(im, "Save", "save=[" + dir + "/" + title + "]");
			this.save(title, dir);
		}

		//Auto contrast
		IJ.log("Auto Contrast is selected using: ctrl+shift+C -> Auto");
	}

	/**
	 * Closes the image that has the associated title
	 * @param title the image's title or name in the Window Manager
	 */
	private void save(String title, String dir) {
		ImagePlus imp = WindowManager.getImage(title);
		IJ.run(imp, "Brightness/Contrast...", "");
		IJ.run(imp, "Enhance Contrast", "saturated=0.35");
		if (!dir.contains("20ms"))
		{
			if (title.equalsIgnoreCase("stk_0001_2dseq")) {
				IJ.run(imp, "Save", "save=[" + dir + "/" + "FA_Map.tif" + "]");
			}
			else if (title.equalsIgnoreCase("stk_0003_2dseq")) {
				IJ.run(imp, "Save", "save=[" + dir + "/" + "Signal_intensity_map.tif" + "]");
			}
			else if (title.equalsIgnoreCase("stk_0002_2dseq")) {
				IJ.run(imp, "Save", "save=[" + dir + "/" + "Tensor_Map.tif" + "]");
			}
			else if (title.equalsIgnoreCase("stk_0011_2dseq")) {
				IJ.run(imp, "Save", "save=[" + dir + "/" + "E1_Map.tif" + "]");
			}
			else if (title.equalsIgnoreCase("stk_0012_2dseq")) {
				IJ.run(imp, "Save", "save=[" + dir + "/" + "E2_Map.tif" + "]");
			}
			else if (title.equalsIgnoreCase("stk_0013_2dseq")) {
				IJ.run(imp, "Save", "save=[" + dir + "/" + "E3_Map.tif" + "]");
			}
		}
		else
		{
			if (title.equalsIgnoreCase("stk_0001_2dseq")) {
				IJ.run(imp, "Save", "save=[" + dir + "/" + "FA_20ms_Map.tif" + "]");
			}
			else if (title.equalsIgnoreCase("stk_0002_2dseq")) {
				IJ.run(imp, "Save", "save=[" + dir + "/" + "Tensor_20ms_Map.tif" + "]");
			}
			else if (title.equalsIgnoreCase("stk_0003_2dseq")) {
				IJ.run(imp, "Save", "save=[" + dir + "/" + "Signal_intensity_20ms_Map.tif" + "]");
			}
			else if (title.equalsIgnoreCase("stk_0011_2dseq")) {
				IJ.run(imp, "Save", "save=[" + dir + "/" + "E1_20ms_Map.tif" + "]");
			}
			else if (title.equalsIgnoreCase("stk_0012_2dseq")) {
				IJ.run(imp, "Save", "save=[" + dir + "/" + "E2_20ms_Map.tif" + "]");
			}
			else if (title.equalsIgnoreCase("stk_0013_2dseq")) {
				IJ.run(imp, "Save", "save=[" + dir + "/" + "E3_20ms_Map.tif" + "]");
			}
		}

	}

	/**
	 * Grabs the desired DTI 2dseq file and scales the images to the
	 * corresponding	 slopes found in the ADC visu_par file. 2desq images
	 * for the low and high B-value separation are also gathered
	 * in this methods
	 * @param dir the string directory of the selected DTI root file
	 */
	public ImagePlus modifyDTI(String dir) {
		//Opens 2dseq stack
		ImagePlus adc2 = this.getImage(dir, PATHS2, 9999);

		return adc2;
	}


	/**
	 * Input from user to grab the root directory for desired files
	 * @param folder, folder that contains desired files
	 * @param fNum, the number that corresponds to the example file folder - 5
	 * @returns the directory of each folder path in a string
	 */
	private String grabDir(String folder) {
		DirectoryChooser path = new DirectoryChooser("Select " + folder
						+ " Path");
		String dir = path.getDirectory();
		IJ.log("Directory return for the " + folder + " folder is :" + dir);
		return dir;
	}


	/**
	 * Provides instructions to Import raw image files
	 * @param numIm number of images in the original image stack
	 */
	private void instructions(int numIm) {
		IJ.log("Please adjust import settings to:");
		IJ.log("    Image type: 32-bit signed");
		IJ.log("    Width: 128 pixels");
		IJ.log("    Height: 64 pixels");
		IJ.log("    Number of images: " + numIm + " images");
		IJ.log("    Check Little-endian byte order");
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
	 * @param dir directory that the images files are located at
	 * @param pathChoice 1 or 2 from pdata folder
	 * @param numIm number of images in this image stack
	 * @return ImagePlus of the scaled image stack
	 */
	private ImagePlus getImage(String dir, String pathChoice, int numIm) {
		// Import Instructions to user
		this.instructions(numIm);
		IJ.openImage(dir + pathChoice+ IM);
		ImagePlus curr = WindowManager.getImage(IM.substring(1));
		return curr;
	}

	/**
	 * Grabs the VisuCoreDataSlope data from the visu_par file.
	 * Assumes the file structure in the visu_pars file will be consistent
	 * with desiring the DESIRED_IMAGE index of each image grouping
	 * @param vp the name of the file that contains an image's parameters
	 * @param find item that needs to be found within the visu_par file
	 * @return slope the slope that multiplies the image data
	 * @throws FileNotFoundException
	 */
	private double[] getSlope(String dir, String find)
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
				pars = this.getCertainSlopes(scanner, pars);
			}
		}
		// IJ.error("The visu_par file did not read any slope data.");
		return pars;
	}

	/**
	 * Retrieves all of the slopes in the VisuCoreDataSlope
	 * @param scanner that is attached to the visu_par data file
	 * @param pars the array that holds the slope values
	 * @return the slopes in the array, or leaves pars unchanged if the
	 * slope data did not exist
	 */
	private double[] getCertainSlopes(Scanner scanner, double[] pars) {
		if (scanner.hasNext()) {
			int numIm = scanner.nextInt(); // Number of Images
			scanner.next(); // Discard ')'
			pars = new double[numIm]; // Holds all slopes
			double currSlope;
			// Search for particular slopes
			int i = 0;
			while (i < numIm) {
				currSlope = scanner.nextDouble();
				pars[i] = currSlope;
				IJ.log("Slope for " + (i+1) + " is " + currSlope);
				i++;
			}
		}
		return pars;
	}

	/**
	 * Takes in an ImagePlus file and does basic 32-bit, gray, and 256x128
	 * scaling and returns the modified ImagePlus file
	 * @param curr the ImagePlus that needs to be modified
	 * @param title is the name of the new image produced
	 * @return the modified the ImagePlus file
	 */
	private ImagePlus basicModify(ImagePlus curr, String title) {
		int depth = curr.getNSlices();
		//IJ.run(curr, "Grays", "");
		//IJ.run("32-bit", "");
		IJ.run(curr, "Scale...", "x=- y=- z=1.0 width=256 height=128 " +
				"depth=" + depth + " interpolation=Bilinear average " +
				"process create" + " title=" + title);

		return curr;
	}
}