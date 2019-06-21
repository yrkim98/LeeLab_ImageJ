/**
 * @author Selina Lui
 * UW Department of Radiology
 * 
 * Summary:
 * Performs the ADC map generation for data analysis.
 * Requires the input of the user to direct the plugin to the location of the 
 * ADC map folder.
 * 
 * Updates:
 * 08.31.17 - Created
 * 09.01.17 - Added support for the ta adc image openning
 * 09.06.17 - Added macro calls and more private functions to edit images
 * 09.12.17 - Debugging retrieval of visu_par slopes and values
 * 	        - Adding flexibility to macro calls
 * 09.14.17 - All images will be multiplied by their slopes
 *            and user will decide on which slices to use for ROIs afterward
 * 09.18.17 - Added slope multiplication 
 * 09.20.17 - Created a GUI window to have the user select the slices that 
 *            will have the ROIs placed on the main stack
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

public class ADC_MAP implements PlugIn {
	
	public static final String DESIRED_IMAGE = "3"; // Diffusion Constant image
	public static final int START_SLICE = Integer.parseInt(DESIRED_IMAGE);
	public static final int NUMBER_IN_SET = 5;
	public static final int NUM_BVALS = 8;
	public static final String VP_SLOPE = "##$VisuCoreDataSlope=(";
	
	/**
	// For Windows:
	public static final String PATHS1 = "pdata\\1";
	public static final String PATHS = "pdata\\2";
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
		IJ.run("Close All", "");
		
		// Gets ADC directory
		String dir = this.grabDir("ADC", 4);
		
		// Grabs the images from the selected ADC files
		ImagePlus adc = this.modifyADC(dir);
		
		// Grabs and modifies the images from the selected ta_adc file
		ImagePlus taADC = this.makeTaADC();
		
		// Open second 2dseq image stack of 80 images for B-value separation
		ImagePlus adc1 = this.getImage(dir, PATHS1, 80);
		
		// Basic Modification of the ImagePlus file (Gray, 32-bit, 256x256)
		adc1 = this.basicModify(adc1);
		adc1.changes = false; // Allows modified ImagePlus to be closed
		IJ.run("Stack to Images", "");
		
		// Grabs the slope and B values, or throws an error
		double[] slopes = null;
		int[] bVal = null;
		try {
			String vpDir = dir + PATHS1; // file directory of the visu_par file
			slopes = this.getSlope(vpDir, VP_SLOPE);
			bVal = this.getBVals(vpDir);
		} catch (FileNotFoundException e) {
			IJ.error("The slopes were not retrieved");
		}
		
		// Normalization of image stacks with slopes
		int[] openImp = WindowManager.getIDList();
		int index = slopes.length - 1;
		String s = "";
		for (int i = 0; i < openImp.length; i++) {
			s = s + openImp[i];
		}
		
		int counter = 1;
		int slice = 80;
		ImagePlus curr = null;
		for (int i = openImp.length - 1; i >= openImp.length - 80; i--) {
			curr = WindowManager.getImage(openImp[i]);
			IJ.run(curr, "Multiply...", "value=" + slopes[index]);
			IJ.log("slice index " + slice + " is multiplied by " + slopes[index]);
			
			
			if (counter % 10 == 0) {
				index--;
			}
			counter++;
			slice--;
		}
		
		IJ.run(curr, "Images to Stack", "name=Stack title=2dseq-1 use");
		
		IJ.run("DeInterleave ", "number=10"); // 10 substacks created
		
		// Updates list of open images
		openImp = WindowManager.getIDList();
		
		// Creates the dialog prompting the user to select the image slices
		// that will be analyzed
		boolean[] userSlice = this.createDialog();
		
		if (userSlice != null & userSlice.length == 10) {
			int count = 10;
			for (int i = openImp.length - 1; i >= openImp.length - 10; i--) {
				int currIm = openImp[i];
				ImagePlus currIp = WindowManager.getImage(currIm);
				
				// Checks if the if the slice was selected by user
				if (userSlice[count - 1]) {
					IJ.log("Selected slice substacks are created for slice " + count);
					IJ.run(currIp, "Substack Maker", "slices=1-3");
					WindowManager.getImage("Substack (1-3)").setTitle("Slice " + count
							+ " Substack (1-3)");
					
					IJ.run(currIp, "Substack Maker", "slices=4-8");
					WindowManager.getImage("Substack (4-8)").setTitle("Slice " + count
							+ " Substack (4-8)");
				}
				
				// Closes images
				this.closer(currIp.getTitle());
				count--;
			}
			
			// Prompts users to close the create the diffusion maps
			IJ.showMessage("Create Diffusion maps. \n Plugins -> MRIAnalysisPak -> "
					+ "MRI analysis calc \n Substacks 1-3 B Values: 7 47 81"
					+ "\n Substacks 4-8 B Values: 126 180 234 340 549");
			
			//Prompts user to auto contrast the modified 2dseq now labled "Stack"
			IJ.showMessage("Auto contrast image labeled \"Stack\"");
			WindowManager.getImage("Stack");
			IJ.run("Brightness/Contrast...");
			WindowManager.getImage("Stack");
		}
	}
	
	/**
	 * Grabs the ADC 2dseq file and scales the images to the corresponding
	 * slopes found in the ADC visu_par file. 2desq images for the low and high
	 * B-value separation are also gathered in this methods
	 * @param dir the string directory of the selected ADC root file
	 */
	public ImagePlus modifyADC(String dir) {
		//Opens 2dseq stack
		ImagePlus adc2 = this.getImage(dir, PATHS2, 50);
		
		// Image Slice Indices
		String sliceNums = this.strStackBuilder(adc2.getNSlices());
		
		// Gather VisuCoreDataSlopes and checks if image is an ADC Sequence
		double[] slopes = null;
		try {
			slopes = this.getSlope(dir + PATHS2, "##$VisuCoreDataSlope=(");
			
		} catch (FileNotFoundException e) {
			IJ.error("The visu_par file was not found.");
		}
		
		// Multiply cooresponding slope values to each image in substack
		adc2 = this.modifyStack(adc2, slopes, sliceNums);
		
		// Closes the rescaled 2dseq
		this.closer("2dseq_256_256");
		
		// Prompts User for Auto contrast and save sequence
		IJ.log("Auto adjust brightness and contrast ("
				+ "Select \"Auto\") and Save as"
				+ " \"adc2_unstratified_map\"");
		
		return adc2;
	}
	
	/**
	 * Opens a the ta adc image and prompts user to create and save ROIs
	 * @return ImagePlus of the opened ta_adc 2dseq image
	 */
	public ImagePlus makeTaADC() {
		// Retrieves the directory for the ta_adc folder
		String dir = this.grabDir("ta_adc", 5);
		
		// Opens the Image stack 
		ImagePlus taADC = this.getImage(dir, PATHS1, 50);
		
		// Performs 256x256 scaling, 32-bit, and gray changes on ImagePlus
		taADC = this.basicModify(taADC);
		
		// Saves the taADC image file in the same directory as selected
		// folder as .tif file
		IJ.run(taADC, "Save", "save=[" + dir + PATHS1 + IM + ".tif]");
		
		//IJ.showMessage("Select ROIs on 3 slices and save ROIs.");
		return taADC;
	}

	/** 
	 * Input from user to grab the root directory for desired files
	 * @param folder, folder that contains desired files
	 * @param fNum, the number that corresponds to the example file folder - 5
	 * @returns the directory of each folder path in a string 
	 */
	private String grabDir(String folder, int fNum) {
		IJ.log("Please c"
				+ "hoose a directory where the " +  folder + " folder is " +
				"located (i.e. " + (NUMBER_IN_SET + fNum) + "_" + folder + ")");
		DirectoryChooser path = new DirectoryChooser("Select Path");
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
		IJ.log("    Height: 128 pixels");
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
	 * Builds the string that will create a substack
	 * i.e. returns "3,8,13,18,..." for the 3rd image in each set of five
	 * @param numImages number of images in the stack
	 * @return String that has the slice numberID of the images that will be in
	 * the final stack
	 */
	private String strStackBuilder(int numImages) {
		String returnSlices = DESIRED_IMAGE;
		for (int i = START_SLICE + NUMBER_IN_SET; i <= numImages;
				i += NUMBER_IN_SET) {
			returnSlices += "," + i;
		}
		return returnSlices;
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
				if (find.contains("Slope") && dir.contains(PATHS2)) {
					return this.getCertainSlopes(scanner, pars);
				} else {
					return this.get8Slopes(scanner, pars);
				}
			}
		}
		// IJ.error("The visu_par file did not read any slope data.");
		return pars;
	}
	
	/**
	 * Retrieves certain slopes
	 * @param scanner that is attached to the visu_par data file
	 * @param pars the array that holds the slope values
	 * @return the slopes in the array, or leaves pars unchanged if the
	 * slope data did not exist
	 */
	private double[] getCertainSlopes(Scanner scanner, double[] pars) {
		if (scanner.hasNext()) {
			int numIm = scanner.nextInt(); // Number of Images
			scanner.next(); // Discard ')'
			pars = new double[numIm / NUMBER_IN_SET];
			double currSlope;
			int arrIndex = 0;
			// Search for particular slopes
			int i = 1;
			int sliceMod = 0;
			while (i <= numIm && arrIndex < numIm / NUMBER_IN_SET) {
				currSlope = scanner.nextDouble();
				sliceMod = i % (2 * NUMBER_IN_SET);
				if (sliceMod == START_SLICE || 
						sliceMod == (START_SLICE + NUMBER_IN_SET)) {
					pars[arrIndex] = currSlope;
					arrIndex++;
				}
				i++;
			}
		}
		return pars;
	}
	
	/** 
	 * The second algorithm to obtain the visu_par slopes for the 8 substacks
	 * @param scanner that is attached to the visu_par data file
	 * @param pars the array that will store the parameters
	 * @return the parameters gathered from the file in the updated pars array
	 */
	private double[] get8Slopes(Scanner scanner, double[] pars) {
		pars = new double[8];
		if (scanner.hasNext()) {
			int numIm = scanner.nextInt(); // number of images
			if (numIm / 10 == 8) {
				scanner.next(); // Discard the ")"
				int indx = 0;
				for (int i = 1; i <= 80; i++) {
					double curr = scanner.nextDouble(); // gets next slope
					
					// Retains only the first value for every set 
					// of 10 (i.e. 1, 11, 21, ... etc)
					if (i % 10 == 1) {
						pars[indx] = curr;
						indx++;
					}
				}
				return pars;
			} else {
				IJ.error("There are not 80 images in the stack");
			}
		}
		return null;
	}
	
	/**
	 * Retrieves the B values
	 * @param dir string where the location of the visu_par file is
	 * @return updated pars file that contains B values
	 * @throws FileNotFoundException
	 */
	private int[] getBVals(String dir) 			
			throws FileNotFoundException {
		// Open visu_par file
		File vp = new File(dir + VIS_PAR);
		if (!vp.exists()) {
			throw new FileNotFoundException();
		}
		int[] pars = new int[8]; // Will hold the bVals
		Scanner scanner = new Scanner(vp); 
		while (scanner.hasNext()) {
			String find = "##$VisuFGElemComment=(";
			String current = scanner.next();
			if (current.equalsIgnoreCase(find)) {
				if (scanner.hasNext()) {
					// Number of B vals, also number of substacks
					String s = scanner.next(); // integer with a comma. i.e: 8,
					int numIn = Integer.parseInt(s.substring(0, 1)); 
					scanner.next(); // Integer number
					scanner.next(); // Discard closing parentheses, )
					int i = 1;
					int line = 0; // Length of line
					while (scanner.hasNext() && i <= numIn) {
						scanner.next(); // Discard <Dir
						scanner.next(); // Discard 1
						scanner.next(); // Discard B
						// The B value retrieved and placed into stack
						String num = scanner.next();
						if (num.contains(">")) {
							pars[i - 1] = Integer.parseInt(
									num.substring(0, num.length() - 1));
						} else {
							pars[i - 1] = Integer.parseInt(num);
							scanner.next(); // Removes ">" on new line
						}
						IJ.log("Bval " + i + " is: " + pars[i - 1]);
						i++;
					}
				}
				return pars;
			}
		}
		return pars;
	}
	
	/**
	 * Takes in a stack and multiplies by the corresponding slope values
	 * @param curr the stack of images that will be modified
	 * @param slopes the slopes that will multiply the corresponding image
	 * @param sliceNums desired slices within the stack
	 * @return the modified ImagePlus of the stack of images
	 */
	private ImagePlus modifyStack(ImagePlus curr, double[] slopes, String sliceNums) {
		// Scales curr to 256 by 256 pixels
		IJ.run(curr, "Scale...", "x=2 y=2 z=1.0 width=256 height=256 depth=50 "
				+ "interpolation=Bilinear average process create title=" + 
				curr.getTitle() + "_256_256");
		this.closer(curr.getTitle());
		curr = WindowManager.getImage(IM.substring(1) + "_256_256");
		
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
			double val = slopes[j - 1] * 1000; // 1000 Yields 10^-3 mm^2/s
			IJ.run(curSub, "Multiply...", "value=" + val + " stack");			
		}
		IJ.run(curr, "Images to Stack", "name=Stack title=[] use");
		return curr;
	}
	
	/**
	 * Takes in an ImagePlus file and does basic 32-bit, gray, and 256x256 
	 * scaling and returns the modified ImagePlus file
	 * @param curr the ImagePlus that needs to be modified
	 * @return the modified the ImagePlus file
	 */
	private ImagePlus basicModify(ImagePlus curr) {
		int depth = curr.getNSlices();
		IJ.run(curr, "Grays", "");
		IJ.run("32-bit", "");
		IJ.run(curr, "Scale...", "x=- y=- z=1.0 width=256 height=256 " + 
				"depth=" + depth + " interpolation=Bilinear average " +
				"process create" + " title=2dseq-1");
		
		return curr;
	}
	
	/**
	 * Creates the diffusion/perfusion map
	 * @param curr the current ImagePlus
	 * @param dir the directory path for saving file
	 * @param num example: 1-3 or 4-8 for substack slices
	 * @param bVal high and low bValues i.e. 7 47 81 or 126 180 234 340 549
	 * @param count the current slice
	 */
	private void diffusionMap(ImagePlus curr, String dir, String num, 
			String bVal, int count) {
		
		if (curr.getNSlices() == 8) {
			IJ.run(curr, "Substack Maker", "slices=" + num);
			ImagePlus front = WindowManager.getImage("Substack (" + num + ")");
			IJ.run("MRI Analysis Calc", "calculation=[Diffusion Calculation] "
					+ "=[Substack (" + num + ")] =[" + bVal + "] =0 =800000");
			ImagePlus sub = WindowManager.getImage(
					"Diffusion ADC (10^-3*mm^2/sec)");
			IJ.run(sub, "Save", "save=[" + dir + PATHS1 + "/slice " + count +
					" (" + num + ") norm map" + ".tif]");
			this.closer("Substack (" + num + ")");
			this.closer("Diffusion ADC (10^-3*mm^2/sec)");
		} else {
			IJ.error("There are not 8 slices to get the diffusion map.");
		}
	}
	
	/**
	 * Creates a dialog box to know which slices the user will select 
	 * for map creation
	 * @return the user's selection in the particular image slices
	 */
	public boolean[] createDialog() {
		GenericDialog gd = new GenericDialog("Slice Selection");
		String[] labels = {"1", "2", "3", "4", "5", 
				"6", "7", "8", "9", "10"};
		boolean[] defaults = {false, false, false, false, 
				false, true, true, true, false, false};
		gd.addMessage("Select the Image slices that \n you will select ROIs upon.");
		gd.addCheckboxGroup(5, 4, labels, defaults);
		gd.showDialog();
		
		if (gd.wasCanceled()) {
			return null;
		}
		
		boolean[] userSlice = new boolean[10];
		for (int index = 0; index < defaults.length; index++) {
			userSlice[index] = gd.getNextBoolean();
		}
		return userSlice;
	}
}