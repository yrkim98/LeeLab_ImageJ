import java.util.*;
import java.io.*;

import ij.*;
import ij.io.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;


	public class CEST_TEST implements PlugIn {
	
	public static final String DESIRED_IMAGE = "3"; // Diffusion Constant image
	public static final int START_SLICE = Integer.parseInt(DESIRED_IMAGE);
	public static final int NUMBER_IN_SET = 5;
	
	// For Macs:	
	public static final String PATHS1 = "pdata/1";
	public static final String IM = "/2dseq";
	public static final String VIS_PAR = "/visu_pars";
	
	/**
	// For Windows:
	public static final String PATHS1 = "pdata\\1";
	public static final String IM = "\\2dseq";
	public static final String VIS_PAR = "\\visu_pars";	
	*/
	@Override
	public void run(String arg) {
		IJ.log("running newest version: bkim");
		// Clears previous images if any
		IJ.run("Close All", "");
		
		// Gets CEST directory
		String dir = this.grabDir("cest_51_offsets", 25);
		ImagePlus cest1 = this.getImage(dir, PATHS1);
		cest1 = this.basicModify(cest1);
		int[] openWindows = WindowManager.getIDList();
		cest1 = WindowManager.getImage(openWindows[openWindows.length - 1]);
		cest1 = this.slopeImp(cest1, dir);
		this.closer(IM.substring(1));
		cest1 = WindowManager.getCurrentImage();
		
		// Gets 0 power directory
		String dir0 = this.grabDir("cest_0_power", 26);
		ImagePlus zeroPow = this.getImage(dir0, PATHS1);
		zeroPow = this.basicModify(zeroPow);
		openWindows = WindowManager.getIDList();
		zeroPow = WindowManager.getImage(openWindows[openWindows.length - 1]);
		zeroPow = this.slopeImp(zeroPow, dir0);
		this.closer(IM.substring(1));
		zeroPow = WindowManager.getCurrentImage();
		
		//Directory for saving the cest maps created
		String saveDir = dir + PATHS1.substring(0, PATHS1.length() - 1);
		
		// Amide: create cest_map_35 for -3.5 and 3.5 ppm splits
		ImagePlus map50 = this.gagCreate(1, 51, cest1, zeroPow, 50, saveDir);
		
		// Gag: looking at 0.5, 1.0, and 1.5
		//ImagePlus map05 = this.gagCreate(7, 8, cest1, zeroPow, 05, saveDir);
		//ImagePlus map10 = this.gagCreate(6, 9, cest1, zeroPow, 10, saveDir);
		//ImagePlus map15 = this.gagCreate(5, 10, cest1, zeroPow, 15, saveDir);
		
		IJ.showMessage("All Generated maps will be saved in:\n" + saveDir);
	}
	
	/**
	 * Finds slope and returns the updated ImagePlus file after multiplying 
	 * the slope to the image
	 * @param imp ImagePlus file
	 * @param dir String of the directory where visu_par is located
	 */
	public ImagePlus slopeImp(ImagePlus imp, String dir) {
		double slope = 0;
		// Retrieve visu_par slope for images
		try {
			slope = this.getSlope(dir + PATHS1, 
					"##$VisuCoreDataSlope=(");
		} catch (FileNotFoundException e) {
			IJ.error("Slopes were not retrieved.");
		}
		
		// Multiply all images by the visu_par slope
		IJ.run(imp, "Multiply...", "value=" + slope + " stack");	
		
		return imp;
	}
	
	/** 
	 * Input from user to grab the root directory for desired files
	 * @param folder, folder that contains desired files
	 * @param fNum, the number that corresponds to the example file folder - 5
	 * @returns the directory of each folder path in a string 
	 */
	private String grabDir(String folder, int fNum) {
		IJ.log("Please choose a directory where the " + folder + " folder is" +
			" located (i.e. " + (NUMBER_IN_SET + fNum) + "_" + folder + ")");
		DirectoryChooser path = new DirectoryChooser("Select Path");
		String dir = path.getDirectory();
		IJ.log("Directory return for the " + folder + " folder is :" + dir);
		return dir;
	}
	
	/**
	 * Retrieves image and scales to correct size
	 * @param dir directory that the images files are located at
	 * @param pathChoice 1 or 2 from pdata folder
	 * @return ImagePlus of the scaled image stack
	 */
	private ImagePlus getImage(String dir, String pathChoice) {
		IJ.openImage(dir + pathChoice+ IM);		
		ImagePlus curr = WindowManager.getImage(IM.substring(1));
		return curr;
	}
	
	/**
	 * Modifies the passed in ImagePlus file with grays, 32-bit,
	 * and scaling to 256x256
	 * @param imp ImagePlus that will have modifications done on it
	 * @return the modified ImagePlus file
	 */
	private ImagePlus basicModify(ImagePlus imp) {
		IJ.run(imp, "Grays", "");
		IJ.run("32-bit", "");
		IJ.run(imp, "Scale...", "x=2 y=2 z=1.0 width=256 height=256 depth="
					+ imp.getNSlices() // number of slices 
					+ " interpolation=Bilinear average process create "
					+ "title=2dseq-1");
		return imp;
	}
	
	/**
	 * Gets Signal Intensity from -5 to +5
	 * Prints the the ppm in the ImageJ Log
	 * @return the signal intensities for each image.
	 */
	private double[] getSI() {
		double[] sigIn = new double[51];
		for (int i = 0; i < 51; i++) {
			sigIn[i] = i * 0.2 - 5;
			IJ.log("Image " + (i + 1) + " = " + sigIn[i]);
		}
		return sigIn;
	}
	
	/**
	 * Grabs the VisuCoreDataSlope data from the visu_par file.
	 * Assumes the file structure in the visu_pars file will be consistent
	 * @param vp the name of the file that contains an image's parameters
	 * @param find item that needs to be found within the visu_par file
	 * @return slope the slope that multiplies the image data or 1;
	 * @throws FileNotFoundException
	 */	
	private double getSlope(String dir, String find) 
			throws FileNotFoundException {
		
		File vp = new File(dir + VIS_PAR);
		if (!vp.exists()) {
			throw new FileNotFoundException();
		}
		double pars = 1;
		Scanner scanner = new Scanner(vp); 
		while (scanner.hasNext()) {
			String currItem = scanner.next();
			if (currItem.equalsIgnoreCase(find)) {
				if (scanner.hasNext()) {
					scanner.nextDouble(); // discard number of images
					scanner.next(); // Discard ')'
					if (scanner.hasNextDouble()) {
						pars = scanner.nextDouble();
						return pars;
					}
				}
			}
		}		
		return pars;
	}
	
	/**
	 * Generates image by subtracting two imput images that will be saved in 
	 * save directory of the 2dseq image
	 * @param imp the ImagePlus that is providing the images
	 * @param first image slice that will be subtracted from
	 * @param second image slice be used to subtract from the first image by
	 * @return result of subtracting the two images
	 */
	private ImagePlus mapCreate(ImagePlus imp, int slice1, int slice2) {		
		IJ.run(imp, "Substack Maker", "slices=" + slice1);
		ImagePlus first = WindowManager.getImage("Substack (" + slice1 + ")");
		IJ.run(imp, "Substack Maker", "slices=" + slice2);
		ImagePlus second = WindowManager.getImage("Substack (" + slice2 + ")");
		ImageCalculator ic = new ImageCalculator();
		ImagePlus map = ic.run("Subtract create", first, second);

		return map;
	}
	
	/**
	 * Creates the Gag images for the CEST map generation
	 * @param slice1 the first image slice
	 * @param slice2 the second image slice
	 * @param impStack ImagePlus of the images that has 14 images in its stack
	 * @param zeroPow the ImagePlus of the zero power image
	 * @param num the number that will be in the name of the slice
	 * @param directory the directory the image will be saved as 
	 * @return the image that is generated
	 */
	private ImagePlus gagCreate(int slice1, int slice2, ImagePlus impStack,
			ImagePlus zeroPow, int num, String directory) {
		ImagePlus subtracted = this.mapCreate(impStack, slice1, slice2);
		ImageCalculator ic = new ImageCalculator();
		ImagePlus map = ic.run("Divide create", subtracted, zeroPow);
		this.saveImage(map, directory, num);
		return map;
	}
	
	/**
	 * Saves the desired image as a .tiff file
	 * @param imp ImagePlus file that will be saved
	 * @param directory location in computer file system
	 * @param num the number that will be placed in saved file's title
	 */
	private void saveImage(ImagePlus imp, String directory, int num) {
		// For Windows: change "/" to "\\"
		IJ.saveAs(imp, "Tiff", directory + "/cest_map_" + num + ".tif");
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
}


