import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import java.util.*;

/** 
 *   This plugin for ImageJ implements several MRI calculations
 *   such as the generation of a pixelwise Perfusion map 
 *   T1 map and T2 map from appropriate scan stacks.
 *
 *    AUTHOR
 *    @author Karl Schmidt kfschmidt@bwh.harvard.edu
 *    @version 0.9 Beta rel. 05/20/02
 *
 *    REVISIONS:  (Please note revisions to private 
 *                 classes here as well)
 * 
 *    06/05/02 - Diffusion calc is not fully functional, Released as 1.0
 *    06/07/02 - Diffusion calc fixed, basic testing passed
 *
 *
 *
*/ 
public class MRI_Analysis_T2_2TE implements PlugIn {


    // possible modes querried in the first UI
    private static int PERF_CALC = 1;
    private static int DIFF_CALC = 2;
    private static int T1_CALC = 3;
    private static int T2_CALC = 4;


    // ---------  STRING LITERALS  ------------
    // General 
    static final String NOT_GRAY32_MSG = 
	"One of the images selected \n "+
	"is not 32 bit Grayscale: \n"+
	"We'll try to make a 32 Grayscale copy\n"+
	"but try to do the conversion manually \n"+
	"if things don't work out.";
    static final String MSG_PERF_ERROR = "ERROR";
    static final String ERROR_WIN_TITLE = "MRI Calculator Error";
    static final String ABOUT_WIN_TITLE = "MRI Analysis Calculator";
    static final String ABOUT_TEXT = 
	"This plugin calculates the pixel by "+
	"pixel T1 value from \n" +
	"an image stack.  The TR values for each "+
	"slice in the stack\n" + 
	"must be provided.\n Pls. contact the author "+
	"w/ questions: \n"+
	"Karl Schmidt \n kfschmidt@bwh.harvard.edu";
    static final String UI1_TEXT = "MRI Analysis Calculator v1.0\n\n"+
	"Please feel free to \ncontact the author with any questions:\n"+
	"Karl Schmidt \n kfschmidt@bwh.harvard.edu\n\n"+
	"Please choose from one of\n"+
	"the calculation choices below\n "+
	"and click OK to get started:\n\n";
    static final String T1_OPTION_TEXT = "T1 Calculation";
    static final String T2_OPTION_TEXT = "T2 Calculation";
    static final String PERF_OPTION_TEXT = "Perfusion Calculation";
    static final String DIFF_OPTION_TEXT = "Diffusion Calculation";
    static final String UI1_TITLE = "MRI Analysis Calculator";
    static final String LAYER_DISPLAY_OPTION_TEXT = "Display results using layer toolkit?";


     // T2 literals
    private static String T2_TE_VAL_MSG = 
	"TE values (in secs) for each \nslice, seperated by spaces";
    private static String T2_TE_DEFAULT_VALUES = 
	"0.0147 0.06";
/**
 *	Pre-contrast: 0.02 0.0476 0.06 0.0876
 *	Post-contrast: 0.0169 0.04067 0.0507 0.07447
 */

    private static String T2_DLG_TITLE = "T2 Calculation Parameters";
    private static String T2_STACK_SELECT = "T2 image stack: ";
    private static String T2_ERROR_TITLE = "T2 Calculation Error";
    private static String T2_NOTHING_OPEN_ERROR_MSG = 
	"You must open a T2 Stack to perform a T1 measurement";
    private static String T2_FIT_EQ_MSG = "This Calculation uses \n"+
	"a Simplex algortihm to fit the values \n"+
	"from each slice in a T2 stack to the \n"+
	"exponential eq:\n\tSn = SoEXP(-TEn/T2)\n";
    private static String T2_CALC_EXCEPTION = "T2 Calc problem: ";    
    private static String T2_ERROR_THRESHOLD = 
	"0.0001";
	//"Zero T2 values with an R^2 less than: ";
    private static String T2_CLIP_THRESHOLD = 
	"0.2";
	//"Clip T2 values exceeding: ";
    private static String T2_R2_MAP_DISPLAY_OPTION = 
	"Also generate R^2 Map ";
    private static String T2_MAP_TITLE = "T2Map";
    private static String T2_R2_MAP_TITLE = "T2Quality";


    // T1 literals
    private static String T1_TR_VAL_MSG = 
	"TR values (in secs) for each \nslice, seperated by spaces";
    private static String T1_TR_DEFAULT_VALUES = 
	"0.1 0.35 0.75 1.25 2.5 5";
    private static String T1_DLG_TITLE = "T1 Calculation Parameters";
    private static String T1_STACK_SELECT = "T1 image stack: ";
    private static String T1_ERROR_TITLE = "T1 Calculation Error";
    private static String T1_NOTHING_OPEN_ERROR_MSG = 
	"You must open a T1 Stack to perform a T1 measurement";
    private static String T1_FIT_EQ_MSG = "This Calculation uses \n"+
	"a Simplex algortihm to fit the values \n"+
	"from each slice in a T1 stack to the \n"+
	"exponential eq:\n\tSn = So(1 - EXP(-TRn/T1)\n";
    private static String T1_CALC_EXCEPTION = "T1 Calc problem: ";    
    private static String T1_ERROR_THRESHOLD = 
	"Zero T1 values with an R^2 less than: ";
    private static String T1_CLIP_THRESHOLD = 
	"Clip T1 values exceeding: ";
    private static String T1_R2_MAP_DISPLAY_OPTION = 
	"Also generate R^2 Map ";
    private static String T1_MAP_TITLE = "T1 Calculation Result";
    private static String T1_R2_MAP_TITLE = "T1 Fit Quality Map (R^2)";



    // Perfusion Calc literals
    private static String PERF_CLIP_THRESHOLD = "Perfusion Clip Threshold:";
    private static String PERF_CALC_EXCEPTION = "Perfusion Calc Exception:";
    private static String PERF_ERROR_TITLE = "Perfusion calc error";
    private static String PERF_NOTHING_OPEN_ERROR_MSG = "Please open a T1 image stack and an ASL stack for perfusion calculations";
    private static String PERF_DLG_TITLE = "Perfusion Calculation";
    private static String PERF_FIT_EQ_MSG = 
	"This calculation uses two image stacks, \n"+
	"(T1 & ASL) to produce a Perfusion map.\n\n"+
	"The Arterial Spin Labeled (ASL) stack \n"+
	"is used to produce the ASL contrast image:\n"+
	"     ASLW = (S2-S1)/S2\n\n"+
	"The T1 map is produced using a Simplex\n"+
	"algortihm to fit the values from each \n"+
	"slice to the exponential eq:\n"+
	"     Sn = So(1 - EXP(-TRn/T1))\n\n"+
	"The ASL and T1 values are then combined: \n"+
	"     Perfusion = (ASLW*6000*0.9)/(1.6*T1)\n\n"+
	"     units: ml/100mg/min";
    private static String PERF_MAP_TITLE = "Perfusion (ml/100g/min)";
    private static String PERF_STACK_SELECT = 
	"ASL stack:";


    // Diffusion Calc literals
    private static String DIFF_MAP_TITLE = "Diffusion ADC (10^-3*mm^2/sec)";
    private static String DIFF_STACK_SELECT = "Diffusion Image Stack";
    private static String DIFF_B_VAL_MSG = 
	"Enter the b values separated by spaces";
    private static String DIFF_DLG_TITLE = "Diffusion calculation";
    public static String DIFF_BAD_NUM_B_VALS = "Need a b value for each slice";
    public static String DIFF_CALC_EXCEPTION = 
	"Diffusion Calc Exception";
    public static String DIFF_NEED_SLICES_ERROR =
	"Diffusion image stack must have 2 or more slices";

    public static String DIFF_R2_MAP_TITLE =
	"Diffusion R2 (fit quality)";
    public static String DIFF_B_DEFAULT_VALUES =
	"300 1200";
    public static String DIFF_ERROR_THRESHOLD =
	"Enter a min value (0-1) \nfor R2 fit quality";
    public static String DIFF_CLIP_THRESHOLD =
	"Enter a max value \n for calculated ADC's";
    public static String DIFF_R2_MAP_DISPLAY_OPTION =
	"Display R2 map for \n Diffusion fit quality";
    public static String DIFF_ERROR_TITLE = 
	"Diffusion Error";
    public static String DIFF_FIT_EQ_MSG =
	"     *********\n       NOTE!\n     **********\n\n"+
	"This ADC calculation \nis not fully tested \n\nuse with caution\n\n"+
	"For 2 slice DWI, the \nADC is calculated using:\n\n"+
	"ADC=1000*LN(S1/S2)/(b2-b1)\n\n"+
	"for > 2 slices, the ADC is fit\n"+
	"to the function: \n\n"+
	"Sn=So*EXP(-b*adc) \n\nADC map returned is adc*1000 \n\n";
    public static String DIFF_NOTHING_OPEN_ERROR_MSG =
	"Please open a Diffusion Weighted \n"+
	"Image stack before attempting calculation";




    // =========  Principal execution paths ==========

    /**
     *     Called by ImageJ on Plugin invocation
     */
    public void run(String arg) {
        IJ.register(MRI_Analysis_Calculator.class);

//	if (arg.equals("about"))
//            {showAbout(); return;}
	
//	 int m = displayUIOne();


	 try {
//	     if (m == T2_CALC) {
		 runT2Mode();
//	     } else if (m == T1_CALC) {
//		 runT1Mode();
//	     } else if (m == PERF_CALC) {
//		 runPerfMode();
//	     } else if (m == DIFF_CALC) {
//		 runDiffMode();
//	     }
	 } catch (Exception e) {
	     e.printStackTrace();
	     IJ.showMessage(e.getMessage());
	     run("");
	 }

    }
    

    /**
     *   Start a Perfusion calculation session
     *
     */
    private void runPerfMode() 
	throws Exception
    {

	// get the params
	Hashtable params = null;
	params = displayPerfUI();
	if (params == null) return;	

	boolean make_r2_map = 
	    ((Boolean) params.get("show_r2_map")).booleanValue();
	String tr_vals = (String) params.get("tr_values");
	double err_threshold = 
	    ((Double) params.get("err_threshold")).doubleValue();
	double t1_clip = 
	    ((Double) params.get("t1_clip")).doubleValue();
	double perf_clip = 
	    ((Double) params.get("perf_clip")).doubleValue();

	// check the T1 image stack
	ImagePlus s1 = (ImagePlus) params.get("t1_stack");
        int s1Size = s1.getStackSize();
        if (s1Size<2) {
	    throw new Exception (PERF_CALC_EXCEPTION+
				 "t1 stack must have 2 or more slices");
        }

	// check the ASL image stack
	ImagePlus asl = (ImagePlus) params.get("asl_stack");
        int aslSize = asl.getStackSize();
        if (aslSize!=2) {
	    throw new Exception (PERF_CALC_EXCEPTION+
				 "ASL stack must have exactly 2 slices");
        }

	// get T1
	float[][] t1a = calculateT1(s1, tr_vals, err_threshold, t1_clip);

	if (make_r2_map) {
	    // display the fit error results
	    showResults(t1a[1], 
			s1.getHeight(), 
			s1.getWidth(), 
			T1_R2_MAP_TITLE,
			false,
			false,
			false); 
	}

	// get the ASLW
	float[] alsw_arr = calculateASLW(asl);

	// get the final perfusion
	float[] perf = calculatePerfusion(t1a[0], alsw_arr, perf_clip);

	// display the results
	showResults(perf, 
		    s1.getHeight(), 
		    s1.getWidth(), 
		    PERF_MAP_TITLE,
		    false,
		    false,
		    false); 

    }


    /**
     *   Start a Diffusion calculation session
     *
     */
    private void runDiffMode() 
	throws Exception
    {
	// get the params
	Hashtable params = null;
	params = displayDiffUI();
	if (params == null) return;	

	boolean make_r2_map = 
	    ((Boolean) params.get("show_r2_map")).booleanValue();
	String b_vals = (String) params.get("b_values");
	double err_threshold = 
	    ((Double) params.get("err_threshold")).doubleValue();
	double adc_clip = 
	    ((Double) params.get("adc_clip")).doubleValue();



	// check the image stack
	ImagePlus s1 = (ImagePlus) params.get("d_stack");
        int s1Size = s1.getStackSize();
        if (s1Size<2) {
	    throw new Exception (DIFF_CALC_EXCEPTION+DIFF_NEED_SLICES_ERROR);
        }

	// get Diffusion array
	float[][] adc = calculateDiff(s1, b_vals, err_threshold, adc_clip);

	if (make_r2_map) {
	    // display the fit error results
	    showResults(adc[1], 
			s1.getHeight(), 
			s1.getWidth(), 
			DIFF_R2_MAP_TITLE,
			false,
			false,
			false); 
	}

	// display the results
	showResults(adc[0], 
		    s1.getHeight(), 
		    s1.getWidth(), 
		    DIFF_MAP_TITLE,
		    false,
		    false,
		    false); 

    }


    /**
     *   Start a T1 calculation
     *
     */
    private void runT1Mode() 
	throws Exception 
    {

	// get the params
	Hashtable params = null;
	params = displayT1UI();
	if (params == null) return;	

	boolean make_r2_map = 
	    ((Boolean) params.get("show_r2_map")).booleanValue();
	String tr_vals = (String) params.get("tr_values");
	double err_threshold = 
	    ((Double) params.get("err_threshold")).doubleValue();
	double t1_clip = 
	    ((Double) params.get("t1_clip")).doubleValue();

	// check the image stack
	ImagePlus s1 = (ImagePlus) params.get("t1_stack");
        int s1Size = s1.getStackSize();
        if (s1Size<2) {
	    throw new Exception (T1_CALC_EXCEPTION+
				 "t1 stack must have 2 or more slices");
        }

	// get T1
	float[][] t1a = calculateT1(s1, tr_vals, err_threshold, t1_clip);

	if (make_r2_map) {
	    // display the fit error results
	    showResults(t1a[1], 
			s1.getHeight(), 
			s1.getWidth(), 
			T1_R2_MAP_TITLE,
			false,
			false,
			false); 
	}

	// display the results
	showResults(t1a[0], 
		    s1.getHeight(), 
		    s1.getWidth(), 
		    T1_MAP_TITLE,
		    false,
		    false,
		    false); 
    }





    /**
     *   Start a T2 calculation session
     */
    private void runT2Mode() 
	throws Exception
    {
	// get the params
	Hashtable params = null;
	params = displayT2UI();
	if (params == null) return;	

	boolean make_r2_map = 
	    ((Boolean) params.get("show_r2_map")).booleanValue();
	String te_vals = (String) params.get("te_values");
	double err_threshold = 
	    ((Double) params.get("err_threshold")).doubleValue();
	double t2_clip = 
	    ((Double) params.get("t2_clip")).doubleValue();

	// check the image stack
	ImagePlus s1 = (ImagePlus) params.get("t2_stack");
        int s1Size = s1.getStackSize();
        if (s1Size<2) {
	    throw new Exception (T2_CALC_EXCEPTION+
				 "t2 stack must have 2 or more slices");
        }

	// get T2
	float[][] t2a = calculateT2(s1, te_vals, err_threshold, t2_clip);

	if (make_r2_map) {
	    // display the fit error results
	    showResults(t2a[1], 
			s1.getHeight(), 
			s1.getWidth(), 
			T2_R2_MAP_TITLE,
			false,
			false,
			false); 
	}

	// display the results
	showResults(t2a[0], 
		    s1.getHeight(), 
		    s1.getWidth(), 
		    T2_MAP_TITLE,
		    false,
		    false,
		    false); 

    }


    // ================= UI ====================

    /**
     *  Initial UI for establishing mode
     *
     *
     */
    private int displayUIOne() 
    {

	// Initial dialog to establish mode
        GenericDialog gd = new GenericDialog(UI1_TITLE);
        gd.addMessage(UI1_TEXT);
	String[] options = {T2_OPTION_TEXT, T1_OPTION_TEXT, 
			    PERF_OPTION_TEXT, DIFF_OPTION_TEXT};
	gd.addChoice("", options, options[0]);
	
//        gd.showDialog();
        if (gd.wasCanceled())
            return 0;

	// get the inputs
        int i = gd.getNextChoiceIndex();
	if (options[i].equals(T2_OPTION_TEXT)) {
	    return T2_CALC;
//	} else if (options[i].equals(T1_OPTION_TEXT)) {
//	    return T1_CALC;
//	} else if (options[i].equals(PERF_OPTION_TEXT)) {
//	    return PERF_CALC;
//	} else if (options[i].equals(DIFF_OPTION_TEXT)) {
//	    return DIFF_CALC;
	} 

	return 0;
    }



    /**
     *   Display the ui for T1 calculation
     *
     */
    private Hashtable displayT1UI() {
	Hashtable rethash = new Hashtable();

	// get window list
        int[] wList = WindowManager.getIDList();
        if (wList==null) {
            IJ.showMessage(T1_ERROR_TITLE, T1_NOTHING_OPEN_ERROR_MSG);
            return null;
        }

	String[] titles = new String[wList.length];
	for (int i=0; i<wList.length; i++) {
	    ImagePlus imp = WindowManager.getImage(wList[i]);
	    if (imp!=null)
		titles[i] = imp.getTitle();
            else 
                titles[i] = "";
        } 
	
	// create the display dialog
	GenericDialog gd = new GenericDialog(T1_DLG_TITLE);
	gd.addMessage(T1_FIT_EQ_MSG);
	gd.addMessage(T1_STACK_SELECT);
	gd.addChoice(" ", titles, titles[0]);
	gd.addMessage(T1_TR_VAL_MSG);
	gd.addStringField("   ", T1_TR_DEFAULT_VALUES, 30);
	gd.addMessage(T1_ERROR_THRESHOLD);
	gd.addNumericField("          ", 0.0D, 2);
	gd.addMessage(T1_CLIP_THRESHOLD);
	gd.addNumericField("          ", 8D, 2);
	gd.addCheckbox(T1_R2_MAP_DISPLAY_OPTION, false);


        gd.showDialog();
        if (gd.wasCanceled())
            return null;	


	// populate the Hashtable
        int i = gd.getNextChoiceIndex();
        ImagePlus s1 = WindowManager.getImage(wList[i]);
        String tr_values = gd.getNextString();
	rethash.put("t1_stack", checkThatImageIsGray32(s1));
	rethash.put("tr_values", tr_values);

	if (gd.getNextBoolean())
	    { 
		rethash.put("show_r2_map", new Boolean (true));
	    } 
	else 
	    { 
		rethash.put("show_r2_map", new Boolean (false));
	    }

	rethash.put("err_threshold", new Double (gd.getNextNumber()));
	rethash.put("t1_clip", new Double (gd.getNextNumber()));

	return rethash;
    }



    /**
     *   Display the ui for Diffusion calculation
     *
     */
    private Hashtable displayDiffUI() {
	Hashtable rethash = new Hashtable();

	// get window list
        int[] wList = WindowManager.getIDList();
        if (wList==null) {
            IJ.showMessage(DIFF_ERROR_TITLE, DIFF_NOTHING_OPEN_ERROR_MSG);
            return null;
        }

	String[] titles = new String[wList.length];
	for (int i=0; i<wList.length; i++) {
	    ImagePlus imp = WindowManager.getImage(wList[i]);
	    if (imp!=null)
		titles[i] = imp.getTitle();
            else 
                titles[i] = "";
        } 
	
	// create the display dialog
	GenericDialog gd = new GenericDialog(DIFF_DLG_TITLE);
	gd.addMessage(DIFF_FIT_EQ_MSG);
	gd.addMessage(DIFF_STACK_SELECT);
	gd.addChoice(" ", titles, titles[0]);
	gd.addMessage(DIFF_B_VAL_MSG);
	gd.addStringField("   ", DIFF_B_DEFAULT_VALUES, 30);
	gd.addMessage(DIFF_ERROR_THRESHOLD);
	gd.addNumericField("          ", 0.0D, 2);
	gd.addMessage(DIFF_CLIP_THRESHOLD);
	gd.addNumericField("          ", 800000D, 2);
	gd.addCheckbox(DIFF_R2_MAP_DISPLAY_OPTION, true);

        gd.showDialog();
        if (gd.wasCanceled())
            return null;	

	// populate the Hashtable
        int i = gd.getNextChoiceIndex();
        ImagePlus s1 = WindowManager.getImage(wList[i]);
        String b_values = gd.getNextString();
	rethash.put("d_stack", checkThatImageIsGray32(s1));
	rethash.put("b_values", b_values);

	if (gd.getNextBoolean())
	    { 
		rethash.put("show_r2_map", new Boolean (true));
	    } 
	else 
	    { 
		rethash.put("show_r2_map", new Boolean (false));
	    }

	rethash.put("err_threshold", new Double (gd.getNextNumber()));
	rethash.put("adc_clip", new Double (gd.getNextNumber()));

	return rethash;
    }





    /**
     *   Display the ui for T2 calculation
     *
     */
    private Hashtable displayT2UI() {
	Hashtable rethash = new Hashtable();

	// get window list
        int[] wList = WindowManager.getIDList();
        if (wList==null) {
            IJ.showMessage(T2_ERROR_TITLE, T2_NOTHING_OPEN_ERROR_MSG);
            return null;
        }

	String[] titles = new String[wList.length];
	for (int i=0; i<wList.length; i++) {
	    ImagePlus imp = WindowManager.getImage(wList[i]);
	    if (imp!=null)
		titles[i] = imp.getTitle();
            else 
                titles[i] = "";
        } 
	
	// create the display dialog
	GenericDialog gd = new GenericDialog(T2_DLG_TITLE);
	gd.addMessage(T2_FIT_EQ_MSG);
	gd.addMessage(T2_STACK_SELECT);
	gd.addChoice(" ", titles, titles[0]);
	gd.addMessage(T2_TE_VAL_MSG);
	gd.addStringField("   ", T2_TE_DEFAULT_VALUES, 30);
	gd.addMessage(T2_ERROR_THRESHOLD);
	gd.addNumericField("          ", 0.0000D, 4);
	gd.addMessage(T2_CLIP_THRESHOLD);
	gd.addNumericField("          ", 0.2D, 4);
	gd.addCheckbox(T2_R2_MAP_DISPLAY_OPTION, true);


//        gd.showDialog();
        if (gd.wasCanceled())
            return null;	


	// populate the Hashtable
        int i = gd.getNextChoiceIndex();
        ImagePlus s1 = WindowManager.getImage(wList[i]);
        String tr_values = gd.getNextString();
	rethash.put("t2_stack", checkThatImageIsGray32(s1));
	rethash.put("te_values", tr_values);

	if (gd.getNextBoolean())
	    { 
		rethash.put("show_r2_map", new Boolean (true));
	    } 
	else 
	    { 
		rethash.put("show_r2_map", new Boolean (false));
	    }

	rethash.put("err_threshold", new Double (gd.getNextNumber()));
	rethash.put("t2_clip", new Double (gd.getNextNumber()));

	return rethash;
    }



    /**
     *   Display the ui for Perfusion calculation
     *
     */
    private Hashtable displayPerfUI() {
	Hashtable rethash = new Hashtable();

	// get window list
        int[] wList = WindowManager.getIDList();
        if (wList==null) {
            IJ.showMessage(PERF_ERROR_TITLE, PERF_NOTHING_OPEN_ERROR_MSG);
            return null;
        }

	String[] titles = new String[wList.length];
	for (int i=0; i<wList.length; i++) {
	    ImagePlus imp = WindowManager.getImage(wList[i]);
	    if (imp!=null)
		titles[i] = imp.getTitle();
            else 
                titles[i] = "";
        } 
	
	// create the display dialog
	GenericDialog gd = new GenericDialog(PERF_DLG_TITLE);
	gd.addMessage(PERF_FIT_EQ_MSG);
	gd.addMessage(PERF_STACK_SELECT);
	gd.addChoice(" ", titles, titles[0]);
	gd.addMessage(T1_STACK_SELECT);
	gd.addChoice(" ", titles, titles[0]);
	gd.addMessage(T1_TR_VAL_MSG);
	gd.addStringField("   ", T1_TR_DEFAULT_VALUES, 30);
	gd.addMessage(T1_ERROR_THRESHOLD);
	gd.addNumericField("          ", 0.0D, 2);
	gd.addMessage(T1_CLIP_THRESHOLD);
	gd.addNumericField("          ", 8D, 2);
	gd.addMessage(PERF_CLIP_THRESHOLD);
	gd.addNumericField("          ", 1200D, 0);
	gd.addCheckbox(T1_R2_MAP_DISPLAY_OPTION, false);


        gd.showDialog();
        if (gd.wasCanceled())
            return null;	

	// populate the Hashtable
        int i = gd.getNextChoiceIndex();
        ImagePlus perf = WindowManager.getImage(wList[i]);
	rethash.put("asl_stack", checkThatImageIsGray32(perf));

        i = gd.getNextChoiceIndex();
        ImagePlus s1 = WindowManager.getImage(wList[i]);
        String tr_values = gd.getNextString();
	rethash.put("t1_stack", checkThatImageIsGray32(s1));
	rethash.put("tr_values", tr_values);

	if (gd.getNextBoolean())
	    { 
		rethash.put("show_r2_map", new Boolean (true));
	    } 
	else 
	    { 
		rethash.put("show_r2_map", new Boolean (false));
	    }

	rethash.put("err_threshold", new Double (gd.getNextNumber()));
	rethash.put("t1_clip", new Double (gd.getNextNumber()));
	rethash.put("perf_clip", new Double (gd.getNextNumber()));

	return rethash;
    }


    // ===========   Calculations  ============

    /**
     *  Calculates the (S2-S1)/S2 image from the ASL
     *  stack
     *
     */
    private float[] calculateASLW(ImagePlus s1) {
        int width  = s1.getWidth();
        int height = s1.getHeight();
        int slices = s1.getStackSize();
        ImageStack stack1 = s1.getStack();
	float [] res_pixels = new float [width*height];
    
	// get the pixels from each image in the stack as ints
	float[][] source_pixels1 = new float[slices][width*height];

	// get the processor type, RGB is not supported
	// TODO: implement support for 16 & 8 bit
	for (int pp=0; pp<2;pp++) {
		source_pixels1 [pp] = 
		    (float[]) s1.getStack().getPixels(pp+1);
	}

	// calc the res_pixels array for PWI data
	for (int qq=0; qq< width*height; qq++) {
	    res_pixels[qq] = 
		(source_pixels1[1][qq] - 
		 source_pixels1[0][qq])/source_pixels1[1][qq];
	}
	return res_pixels;
    }





    /**
     *   Calculates the perfusion from the processed 
     *   t1 and aslw arrays
     *   @param t1_array 1D array of T1 values 
     *   @param pwi_array 1D array of ASLW values
     *   @param max_clip The maximum perfusion value 
     *               recorded (all values above max_clip
     *               are set to max_clip)
     */
    private float[] calculatePerfusion(float[] t1_array, 
				       float[] pwi_array,
				       double max_clip) 
	{
	    float [] res_pixels = new float [t1_array.length];
	    float tmp = -1F;
	    // calc the res_pixels array for PWI data
	    for (int qq=0; qq< t1_array.length; qq++) {
		tmp = (float) ((0.9/(1.6*t1_array[qq]))
		    *6000*pwi_array[qq]);
		if (tmp < 0) tmp = 0;
		if (tmp > max_clip) tmp = (float) max_clip;
		res_pixels[qq] = tmp;
	    }

	    return res_pixels;
	}
    



    /** 
     *   calculates the cooresponding pixelwise T1 
     *   values & imputs them into the result win
     *   @param  s1 Stack containing T1 images
     *   @param  tr_values TR values cooresponding to each image
     *   @param  zero_threshold_for_r2 Pixels with a fit with an 
     *           R^2 less than this threshold will have the T1 
     *           value set to zero
     *   @return 2D array of pixels ret[0] => T1 values, 
     *           ret[1] => R2 values
     *
     */
    public float[][] calculateT1(ImagePlus s1, 
				 String tr_values,
				 double zero_threshold_for_r2,
				 double clip_bound_for_t1) 
	throws Exception
    {
        int width  = s1.getWidth();
        int height = s1.getHeight();
        int slices = s1.getStackSize();
        ImageStack stack1 = s1.getStack();
	float [][] res_pixels = new float [2][width*height];
    
	// convert the tr values to numbers
	StringTokenizer tok = new StringTokenizer (tr_values, " ");
	Vector trs = new Vector();
	while (tok.hasMoreTokens()) {
	    try {
		trs.addElement(new Float(tok.nextToken()));
	    } catch (Exception e) { }
	}

	// copy the tr values to a float array
	Float[] tr_vals = new Float[trs.size()];
	trs.copyInto(tr_vals);	

	// check that we have the correct number of tr values
	if (s1.getStack().getSize() != trs.size()) {
	    throw new Exception (T1_CALC_EXCEPTION +s1.getStack().getSize()+" slices <> "+ trs.size()+" tr values");  
	}
	
	// get the pixels from each image in the stack as ints
	float[][] source_pixels1 = new float[slices][width*height];

	// get the processor type, RGB is not supported
	// TODO: implement support for 16 & 8 bit
	for (int pp=0; pp<slices;pp++) {
		source_pixels1 [pp] = 
		    (float[]) s1.getStack().getPixels(pp+1);
	}

	double[] dtmp = new double[2];
	// calc the res_pixels array
	for (int qq=0; qq< width*height; qq++) {
	    
	    dtmp = fitPointsToT1Curve(source_pixels1, qq, tr_vals);
	    
	    if (dtmp[1] < zero_threshold_for_r2) dtmp[0] = 0D;
	    if (dtmp[0] < 0) dtmp[0] = 0D;
	    if (dtmp[0] > clip_bound_for_t1) dtmp[0] = clip_bound_for_t1;
	    res_pixels[0][qq] = (float) dtmp[0]; // t1 val array
	    res_pixels[1][qq] = (float) dtmp[1]; // cooresponding r^2 array
	    
	    if ((qq % 100) == 0) {
		IJ.showProgress((double)qq/(width*height));
	    }
	}
	return res_pixels;
    }  



    /** 
     *   calculates the pixelwise ADC 
     *   values & imputs them into the result win
     *   @param  s1 Stack containing DWI images
     *   @param  b_values B values cooresponding to each image
     *   @param  zero_threshold_for_r2 Pixels with a fit with an 
     *           R^2 less than this threshold will have the ADC 
     *           value set to zero
     *   @return 2D array of pixels ret[0] => ADC values, 
     *           ret[1] => R2 values
     *
     */
    public float[][] calculateDiff(ImagePlus s1, 
				   String b_values,
				   double zero_threshold_for_r2,
				   double clip_bound_for_adc) 
	throws Exception
    {
        int width  = s1.getWidth();
        int height = s1.getHeight();
        int slices = s1.getStackSize();
        ImageStack stack1 = s1.getStack();
	float [][] res_pixels = new float [2][width*height];
    
	// convert the b values to numbers
	StringTokenizer tok = new StringTokenizer (b_values, " ");
	Vector bs = new Vector();
	while (tok.hasMoreTokens()) {
	    try {
		bs.addElement(new Float(tok.nextToken()));
	    } catch (Exception e) { }
	}

	// copy the tr values to a float array
	Float[] b_vals = new Float[bs.size()];
	bs.copyInto(b_vals);	

	// check that we have the correct number of b values
	if (s1.getStack().getSize() != bs.size()) {
	    throw new Exception (DIFF_CALC_EXCEPTION+DIFF_BAD_NUM_B_VALS);  
	}
	
	// get the pixels from each image in the stack as ints
	float[][] source_pixels1 = new float[slices][width*height];

	// get the processor type, RGB is not supported
	// TODO: implement support for 16 & 8 bit
	for (int pp=0; pp<slices;pp++) {
		source_pixels1 [pp] = 
		    (float[]) s1.getStack().getPixels(pp+1);
	}

	double[] dtmp = new double[2];
	// calc the res_pixels array
	for (int qq=0; qq< width*height; qq++) {
	    
	    dtmp = fitPointsToDiffCurve(source_pixels1, qq, b_vals);
	    
	    if (dtmp[1] < zero_threshold_for_r2) dtmp[0] = 0D;
	    if (dtmp[0] < 0) dtmp[0] = 0D;
	    if (dtmp[0] > clip_bound_for_adc) dtmp[0] = clip_bound_for_adc;
	    res_pixels[0][qq] = (float) dtmp[0]; // adc val array
	    res_pixels[1][qq] = (float) dtmp[1]; // cooresponding r^2 array
	    
	    if ((qq % 100) == 0) {
		IJ.showProgress((double)qq/(width*height));
	    }
	}
	return res_pixels;
    }  






    /** 
     *   calculates the cooresponding pixelwise T2 
     *   values & imputs them into the result win
     *   @param  s1 Stack containing T2 images
     *   @param  te_values TE values cooresponding to each image
     *   @param  zero_threshold_for_r2 Pixels with a fit with an 
     *           R^2 less than this threshold will have the T2 
     *           value set to zero
     *   @return 2D array of pixels ret[0] => T2 values, 
     *           ret[1] => R2 values
     *
     */
    public float[][] calculateT2(ImagePlus s1, 
				 String te_values,
				 double zero_threshold_for_r2,
				 double clip_bound_for_t1) 
	throws Exception
    {
        int width  = s1.getWidth();
        int height = s1.getHeight();
        int slices = s1.getStackSize();
        ImageStack stack1 = s1.getStack();
	float [][] res_pixels = new float [2][width*height];
    
	// convert the te values to numbers
	StringTokenizer tok = new StringTokenizer (te_values, " ");
	Vector trs = new Vector();
	while (tok.hasMoreTokens()) {
	    try {
		trs.addElement(new Float(tok.nextToken()));
	    } catch (Exception e) { }
	}

	// copy the te values to a float array
	Float[] te_vals = new Float[trs.size()];
	trs.copyInto(te_vals);	

	// check that we have the correct number of tr values
	if (s1.getStack().getSize() != trs.size()) {
	    throw new Exception (T2_CALC_EXCEPTION +s1.getStack().getSize()+" slices <-?-> "+ trs.size()+" te values");  
	}
	
	// get the pixels from each image in the stack as ints
	float[][] source_pixels1 = new float[slices][width*height];

	// get the processor type, RGB is not supported
	// TODO: implement support for 16 & 8 bit
	for (int pp=0; pp<slices;pp++) {
		source_pixels1 [pp] = 
		    (float[]) s1.getStack().getPixels(pp+1);
	}

	double[] dtmp = new double[2];
	// calc the res_pixels array
	for (int qq=0; qq< width*height; qq++) {
	    
	    dtmp = fitPointsToT2Curve(source_pixels1, qq, te_vals);
	    
	    if (dtmp[1] < zero_threshold_for_r2) dtmp[0] = 0D;
	    if (dtmp[0] < 0) dtmp[0] = 0D;
	    if (dtmp[0] > clip_bound_for_t1) dtmp[0] = clip_bound_for_t1;
	    res_pixels[0][qq] = (float) dtmp[0]; // t2 val array
	    res_pixels[1][qq] = (float) dtmp[1]; // cooresponding r^2 array
	    
	    if ((qq % 100) == 0) {
		IJ.showProgress((double)qq/(width*height));
	    }
	}
	return res_pixels;
    }  




    /**
     *   Fits the points to the curve Sn = So(1-EXP(-TR/T1))
     *     where So and T1 are unknown. Returns [T1, So, R^2]
     *     @return array of doubles [t1 value, r^2 value for fit]
     *
     */
    private double[] fitPointsToT2Curve(float[][] stack, 
					int xyoffset, 
					Float[] te_values) 
    {
	double[] t2 = new double[2];
	// get the signal samples for the pixel 
	double[] sigs = new double [stack.length];
	for (int p =0; p<stack.length; p++) {
	    sigs[p] = (double) stack[p][xyoffset];
	}

	// copy the tr values into an array
	double[] te = new double [te_values.length];
	for (int p=0; p<te.length; p++) {
	    te[p] = te_values[p].doubleValue();
	}

	T1T2CurveFitter cv = new T1T2CurveFitter(te, sigs);
	cv.doFit(T1T2CurveFitter.T2_DEPHASE);	

	if ((xyoffset % 1000) == 0) dumpFitStats(te, sigs, cv);
	
	t2[0] = cv.getParams()[1];
	t2[1] = cv.getFitGoodness();
	return t2;
    }




    /**
     *   Fits the points to the curve Sn = So(1-EXP(-TR/T1))
     *     where So and T1 are unknown. Returns [T1, So, R^2]
     *     @return array of doubles [t1 value, r^2 value for fit]
     *
     */
    private double[] fitPointsToT1Curve(float[][] stack, 
					int xyoffset, 
					Float[] tr_values) 
    {
	double[] t1 = new double[2];
	// get the signal samples for the pixel 
	double[] sigs = new double [stack.length];
	for (int p =0; p<stack.length; p++) {
	    sigs[p] = (double) stack[p][xyoffset];
	}

	// copy the tr values into an array
	double[] tr = new double [tr_values.length];
	for (int p=0; p<tr.length; p++) {
	    tr[p] = tr_values[p].doubleValue();
	}

	T1T2CurveFitter cv = new T1T2CurveFitter(tr, sigs);
	cv.doFit(T1T2CurveFitter.T1_SAT_RELAX);	

	if ((xyoffset % 1000) == 0) dumpFitStats(tr, sigs, cv);
	
	t1[0] = cv.getParams()[1];
	t1[1] = cv.getFitGoodness();
	return t1;
    }



    /**
     *   Fits the points to the curve Sn = So(1-EXP(-TR/T1))
     *     where So and T1 are unknown. Returns [T1, So, R^2]
     *     @return array of doubles [t1 value, r^2 value for fit]
     *
     */
    private double[] fitPointsToDiffCurve(float[][] stack, 
					  int xyoffset, 
					  Float[] b_values) 
    {
	double[] diff = new double[2];

	if (stack.length == 2) {
	    // two slice case: D = LN(S1/S2)/(b2-b1)
	    diff[0] = 1000*Math.log(stack[0][xyoffset]/stack[1][xyoffset])
		/(b_values[1].floatValue()-b_values[0].floatValue());
	    diff[1] = 1;
	}
	else {
	    // THIS MULTI b-VALUE ADC CALCULATION IS
	    // UNTESTED!!!!!!! USE WITH CAUTION!!!!

	    // get the signal samples for the pixel 
	    double[] sigs = new double [stack.length];
	    for (int p =0; p<stack.length; p++) {
		sigs[p] = (double) stack[p][xyoffset];
	    }
	    
	    // copy the b values into an array
	    double[] b = new double [b_values.length];
	    for (int p=0; p<b.length; p++) {
		b[p] = b_values[p].doubleValue();
	    }
	    
	    T1T2CurveFitter cv = new T1T2CurveFitter(b, sigs);
	    cv.doFit(T1T2CurveFitter.DIFFUSION);	
	    
	    if ((xyoffset % 1000) == 0) dumpFitStats(b, sigs, cv);
	    
	    diff[0] = cv.getParams()[1]*1000; // scale for roi manipulation
	    diff[1] = cv.getFitGoodness();
	}
	return diff;
    }

    



    /**
     *   Check that the data image is 32 bit gray,
     *   make a new 32 bit image and return it if not
     */
    private ImagePlus checkThatImageIsGray32(ImagePlus i) {
	if (i.getType() != ImagePlus.GRAY32) 
	    {
		IJ.showMessage(NOT_GRAY32_MSG);
		StackConverter ic = new StackConverter(i);
		ic.convertToGray32();
	    }
	return i;

    }



    /**
     *   Displays results from a calculation
     *   @param resarray 32 bit signed greyscale result image
     *   @param height   the designated image height
     *   @param width    the designated image width
     *   @param title    the window title
     *   @param smooth   optionally smooth the image
     *   @param resize   optionally resize to 256 x 256 is resarray 
     *                     is smaller (like 128x128)
     *   @param invert   optionally invert the colors in 
     *                    the image (white = zero)
     */
    private void showResults(float[] resarray, 
			     int height, 
			     int width, 
			     String title,
			     boolean smooth,
			     boolean resize,
			     boolean invert) 
    {

	// create a new image for outputting results
	ImagePlus result_win = 
	    NewImage.createFloatImage(title,
				      width, 
				      height, 
				      1, NewImage.FILL_WHITE 
				      );

	// display the res_pixels in the new window
	result_win.getProcessor().setPixels(resarray);
	if (invert) result_win.getProcessor().invertLut();
	if (resize) {
	    if (width < 256)
	    result_win.setProcessor(null,
				    result_win.getProcessor().resize(256, 256)
				    );
	}
	if (smooth) result_win.getProcessor().smooth();
	result_win.getProcessor().resetMinAndMax();
	result_win.show();
	result_win.updateAndDraw();

    }



    // ============= DEBUG ===================
    private void dumpFitStats(double[] x, double[] y, T1T2CurveFitter cf) {
	for (int p=0; p<x.length; p++) {
	    System.out.println("("+x[p]+", "+y[p]+")\n");
	}
	for (int p=0; p<cf.getParams().length; p++) {
	    System.out.println("PARAM: "+p+"=>"+cf.getParams()[p]+"\n");
	}
	
	System.out.println(cf.getResultString());
    }



    void showAbout() {
        String title = "About " + getClass().getName() + "...";
        IJ.showMessage(ABOUT_WIN_TITLE, ABOUT_TEXT);
    }

}  


/** 
 *  This curve fitting class is copied almost entirely from the
 *  ImageJ class CurveFitter (thanks guys!)
 *
 *  It has only been altered to use the Simplex method to fit MRI data 
 *  to T1 and T2 curves. <b>The following description is taken
 *  directly from the original CurveFitter class:<br></b>
 *
 *
 *  Curve fitting class based on the Simplex method described
 *  in the article "Fitting Curves to Data" in the May 1984
 *  issue of Byte magazine, pages 340-362.
 *
 *  2001/02/14: Midified to handle a gamma variate curve.
 *  Uses altered Simplex method based on method in "Numerical Recipes in C".
 *  This method tends to converge closer in less iterations.
 *  Has the option to restart the simplex at the initial best solution in
 *  case it is "stuck" in a local minimum (by default, restarted once).  Also includes
 *  settings dialog option for user control over simplex parameters and functions to
 *  evaluate the goodness-of-fit.  The results can be easily reported with the
 *  getResultString() method.
 *
 * @author             Kieran Holland (email: holki659@student.otago.ac.nz)
 * @version            1.0
 *
 */
class T1T2CurveFitter {    
    public static final int STRAIGHT_LINE=0,POLY2=1,POLY3=2,POLY4=3,
    EXPONENTIAL=4,POWER=5,LOG=6,RODBARD=7,GAMMA_VARIATE=8;
    public static final int T1_SAT_RELAX = 9;
    public static final int T2_DEPHASE = 10;
    public static final int DIFFUSION = 11;
    public static final int IterFactor = 500;
    
    public static final String[] fitList = {"Straight Line","2nd Degree Polynomial",
    "3rd Degree Polynomial", "4th Degree Polynomial","Exponential","Power",
    "log","Rodbard", "Gamma Variate"};
    
    public static final String[] fList = {"y = a+bx","y = a+bx+cx^2",
    "y = a+bx+cx^2+dx^3", "y = a+bx+cx^2+dx^3+ex^4","y = a*exp(bx)","y = ax^b",
    "y = a*ln(bx)","y = c*((a-x)/(x-d))^(1/b)", "y = a*(x-b)^c*exp(-(x-b)/d)", "y=a*(1-exp(-x/b))"};
           
    private static final double alpha = -1.0;     // reflection coefficient
    private static final double beta = 0.5;   // contraction coefficient
    private static final double gamma = 2.0;      // expansion coefficient
    private static final double root2 = 1.414214; // square root of 2
    
    private int fit;                // Number of curve type to fit
    private double[] xData, yData;  // x,y data to fit
    private int numPoints;          // number of data points
    private int numParams;          // number of parametres
    private int numVertices;        // numParams+1 (includes sumLocalResiduaalsSqrd)
    private int worst;          // worst current parametre estimates
    private int nextWorst;      // 2nd worst current parametre estimates
    private int best;           // best current parametre estimates
    private double[][] simp;        // the simplex (the last element of the array at each vertice is the sum of the square of the residuals)
    private double[] next;      // new vertex to be tested
    private int numIter;        // number of iterations so far
    private int maxIter;    // maximum number of iterations per restart
    private int restarts;   // number of times to restart simplex after first soln.
    private double maxError;     // maximum error tolerance
    
    /** Construct a new T1T2CurveFitter. */
    public T1T2CurveFitter (double[] xData, double[] yData) {
        this.xData = xData;
        this.yData = yData;
        numPoints = xData.length;
    }
    
    /**  Perform curve fitting with the simplex method
     *          doFit(fitType) just does the fit
     *          doFit(fitType, true) pops up a dialog allowing control over simplex parameters
     *      alpha is reflection coefficient  (-1)
     *      beta is contraction coefficient (0.5)
     *      gamma is expansion coefficient (2)
     */
    public void doFit(int fitType) {
        doFit(fitType, false);
    }
    
    public void doFit(int fitType, boolean showSettings) {
        if (fitType < STRAIGHT_LINE || fitType > DIFFUSION)
            throw new IllegalArgumentException("Invalid fit type");
        fit = fitType;
        initialize();
        if (showSettings) settingsDialog();
        restart(0);
        
        numIter = 0;
        boolean done = false;
        double[] center = new double[numParams];  // mean of simplex vertices
        while (!done) {
            numIter++;
            for (int i = 0; i < numParams; i++) center[i] = 0.0;
            // get mean "center" of vertices, excluding worst
            for (int i = 0; i < numVertices; i++)
                if (i != worst)
                    for (int j = 0; j < numParams; j++)
                        center[j] += simp[i][j];
            // Reflect worst vertex through centre
            for (int i = 0; i < numParams; i++) {
                center[i] /= numParams;
                next[i] = center[i] + alpha*(simp[worst][i] - center[i]);
            }
            sumResiduals(next);
            // if it's better than the best...
            if (next[numParams] <= simp[best][numParams]) {
                newVertex();
                // try expanding it
                for (int i = 0; i < numParams; i++)
                    next[i] = center[i] + gamma * (simp[worst][i] - center[i]);
                sumResiduals(next);
                // if this is even better, keep it
                if (next[numParams] <= simp[worst][numParams])
                    newVertex();
            }
            // else if better than the 2nd worst keep it...
            else if (next[numParams] <= simp[nextWorst][numParams]) {
                newVertex();
            }
            // else try to make positive contraction of the worst
            else {
                for (int i = 0; i < numParams; i++)
                    next[i] = center[i] + beta*(simp[worst][i] - center[i]);
                sumResiduals(next);
                // if this is better than the second worst, keep it.
                if (next[numParams] <= simp[nextWorst][numParams]) {
                    newVertex();
                }
                // if all else fails, contract simplex in on best
                else {
                    for (int i = 0; i < numVertices; i++) {
                        if (i != best) {
                            for (int j = 0; j < numVertices; j++)
                                simp[i][j] = beta*(simp[i][j]+simp[best][j]);
                            sumResiduals(simp[i]);
                        }
                    }
                }
            }
            order();
            
            double rtol = 2 * Math.abs(simp[best][numParams] - simp[worst][numParams]) /
            (Math.abs(simp[best][numParams]) + Math.abs(simp[worst][numParams]) + 0.0000000001);
            
            if (numIter >= maxIter) done = true;
            else if (rtol < maxError) {
                //System.out.print(getResultString());
                restarts--;
                if (restarts < 0) {
                    done = true;
                }
                else {
                    restart(best);
                }
            }
        }
    }
        
    /** Initialise the simplex
     */
    void initialize() {
        // Calculate some things that might be useful for predicting parametres
        numParams = getNumParams();
        numVertices = numParams + 1;      // need 1 more vertice than parametres,
        simp = new double[numVertices][numVertices];
        next = new double[numVertices];
        
        double firstx = xData[0];
        double firsty = yData[0];
        double lastx = xData[numPoints-1];
        double lasty = yData[numPoints-1];
        double xmean = (firstx+lastx)/2.0;
        double ymean = (firsty+lasty)/2.0;
        double slope;
        if ((lastx - firstx) != 0.0)
            slope = (lasty - firsty)/(lastx - firstx);
        else
            slope = 1.0;
        double yintercept = firsty - slope * firstx;
        maxIter = IterFactor * numParams * numParams;  // Where does this estimate come from?
        restarts = 1;
        maxError = 1e-9;
        switch (fit) {
            case STRAIGHT_LINE:
                simp[0][0] = yintercept;
                simp[0][1] = slope;
                break;
            case POLY2:
                simp[0][0] = yintercept;
                simp[0][1] = slope;
                simp[0][2] = 0.0;
                break;
            case POLY3:
                simp[0][0] = yintercept;
                simp[0][1] = slope;
                simp[0][2] = 0.0;
                simp[0][3] = 0.0;
                break;
            case POLY4:
                simp[0][0] = yintercept;
                simp[0][1] = slope;
                simp[0][2] = 0.0;
                simp[0][3] = 0.0;
                simp[0][4] = 0.0;
                break;
            case EXPONENTIAL:
                simp[0][0] = 0.1;
                simp[0][1] = 0.01;
                break;
            case POWER:
                simp[0][0] = 0.0;
                simp[0][1] = 1.0;
                break;
            case LOG:
                simp[0][0] = 0.5;
                simp[0][1] = 0.05;
                break;
            case RODBARD:
                simp[0][0] = firsty;
                simp[0][1] = 1.0;
                simp[0][2] = xmean;
                simp[0][3] = lasty;
                break;
            case T1_SAT_RELAX:
                simp[0][0] = firsty;
                simp[0][1] = 1.0;
                break;
            case T2_DEPHASE:
                simp[0][0] = firsty;
                simp[0][1] = 1.0;
                break;
            case DIFFUSION:
                simp[0][0] = firsty;
                simp[0][1] = 0.0005;
                break;
            case GAMMA_VARIATE:
                //  First guesses based on following observations:
                //  t0 [b] = time of first rise in gamma curve - so use the user specified first limit
                //  tm = t0 + a*B [c*d] where tm is the time of the peak of the curve
                //  therefore an estimate for a and B is sqrt(tm-t0)
                //  K [a] can now be calculated from these estimates
                simp[0][0] = firstx;
                double ab = xData[getMax(yData)] - firstx;
                simp[0][2] = Math.sqrt(ab);
                simp[0][3] = Math.sqrt(ab);
                simp[0][1] = yData[getMax(yData)] / (Math.pow(ab, simp[0][2]) * Math.exp(-ab/simp[0][3]));
                break;
        }
    }
    
    /** Pop up a dialog allowing control over simplex starting parameters */
    private void settingsDialog() {
        GenericDialog gd = new GenericDialog("Simplex Fitting Options", IJ.getInstance());
        gd.addMessage("Function name: " + fitList[fit] + "\n" +
        "Formula: " + fList[fit]);
        char pChar = 'a';
        for (int i = 0; i < numParams; i++) {
            gd.addNumericField("Initial "+(new Character(pChar)).toString()+":", simp[0][i], 2);
            pChar++;
        }
        gd.addNumericField("Maximum iterations:", maxIter, 0);
        gd.addNumericField("Number of restarts:", restarts, 0);
        gd.addNumericField("Error tolerance [1*10^(-x)]:", -(Math.log(maxError)/Math.log(10)), 0);
        gd.showDialog();
        if (gd.wasCanceled() || gd.invalidNumber()) {
            IJ.error("Parameter setting canceled.\nUsing default parameters.");
        }
        // Parametres:
        for (int i = 0; i < numParams; i++) {
            simp[0][i] = gd.getNextNumber();
        }
        maxIter = (int) gd.getNextNumber();
        restarts = (int) gd.getNextNumber();
        maxError = Math.pow(10.0, -gd.getNextNumber());
    }
    
    /** Restart the simplex at the nth vertex */
    void restart(int n) {
        // Copy nth vertice of simplex to first vertice
        for (int i = 0; i < numParams; i++) {
            simp[0][i] = simp[n][i];
        }
        sumResiduals(simp[0]);          // Get sum of residuals^2 for first vertex
        double[] step = new double[numParams];
        for (int i = 0; i < numParams; i++) {
            step[i] = simp[0][i] / 2.0;     // Step half the parametre value
            if (step[i] == 0.0)             // We can't have them all the same or we're going nowhere
                step[i] = 0.01;
        }
        // Some kind of factor for generating new vertices
        double[] p = new double[numParams];
        double[] q = new double[numParams];
        for (int i = 0; i < numParams; i++) {
            p[i] = step[i] * (Math.sqrt(numVertices) + numParams - 1.0)/(numParams * root2);
            q[i] = step[i] * (Math.sqrt(numVertices) - 1.0)/(numParams * root2);
        }
        // Create the other simplex vertices by modifing previous one.
        for (int i = 1; i < numVertices; i++) {
            for (int j = 0; j < numParams; j++) {
                simp[i][j] = simp[i-1][j] + q[j];
            }
            simp[i][i-1] = simp[i][i-1] + p[i-1];
            sumResiduals(simp[i]);
        }
        // Initialise current lowest/highest parametre estimates to simplex 1
        best = 0;
        worst = 0;
        nextWorst = 0;
        order();
    }
        
    // Display simplex [Iteration: s0(p1, p2....), s1(),....] in ImageJ window
    void showSimplex(int iter) {
        ij.IJ.write("" + iter);
        for (int i = 0; i < numVertices; i++) {
            String s = "";
            for (int j=0; j < numVertices; j++)
                s += "  "+ ij.IJ.d2s(simp[i][j], 6);
            ij.IJ.write(s);
        }
    }
        
    /** Get number of parameters for current fit function */
    public int getNumParams() {
        switch (fit) {
	case STRAIGHT_LINE: return 2;
	case POLY2: return 3;
	case POLY3: return 4;
	case POLY4: return 5;
	case EXPONENTIAL: return 2;
	case POWER: return 2;
	case LOG: return 2;
	case T1_SAT_RELAX: return 2;
	case T2_DEPHASE: return 2;
	case DIFFUSION: return 2;
	case RODBARD: return 4;
	case GAMMA_VARIATE: return 4;
        }
        return 0;
    }
        
    /** Returns "fit" function value for parametres "p" at "x" */
    public static double f(int fit, double[] p, double x) {
        switch (fit) {
            case STRAIGHT_LINE:
                return p[0] + p[1]*x;
            case POLY2:
                return p[0] + p[1]*x + p[2]* x*x;
            case POLY3:
                return p[0] + p[1]*x + p[2]*x*x + p[3]*x*x*x;
            case POLY4:
                return p[0] + p[1]*x + p[2]*x*x + p[3]*x*x*x + p[4]*x*x*x*x;
            case EXPONENTIAL:
                return p[0]*Math.exp(p[1]*x);
            case T1_SAT_RELAX:
                return p[0]*(1 - Math.exp(-(x / p[1])));
            case T2_DEPHASE:
                return p[0]*Math.exp(-(x / p[1]));
            case DIFFUSION:
                return p[0]*Math.exp(-x * p[1]);
            case POWER:
                if (x == 0.0)
                    return 0.0;
                else
                    return p[0]*Math.exp(p[1]*Math.log(x)); //y=ax^b
            case LOG:
                if (x == 0.0)
                    x = 0.5;
                return p[0]*Math.log(p[1]*x);
            case RODBARD:
                double ex;
                if (x == 0.0)
                    ex = 0.0;
                else
                    ex = Math.exp(Math.log(x/p[2])*p[1]);
                double y = p[0]-p[3];
                y = y/(1.0+ex);
                return y+p[3];
            case GAMMA_VARIATE:
                if (p[0] >= x) return 0.0;
                if (p[1] <= 0) return -100000.0;
                if (p[2] <= 0) return -100000.0;
                if (p[3] <= 0) return -100000.0;
                
                double pw = Math.pow((x - p[0]), p[2]);
                double e = Math.exp((-(x - p[0]))/p[3]);
                return p[1]*pw*e;
            default:
                return 0.0;
        }
    }

    /** Get the set of parameter values from the best corner of the simplex */
    public double[] getParams() {
        order();
        return simp[best];
    }
    
    /** Returns residuals array ie. differences between data and curve */
    public double[] getResiduals() {
        double[] params = getParams();
        double[] residuals = new double[numPoints];
        for (int i = 0; i < numPoints; i++)
            residuals[i] = yData[i] - f(fit, params, xData[i]);
        return residuals;
    }
    
    /* Last "parametre" at each vertex of simplex is sum of residuals
     * for the curve described by that vertex
     */
    public double getSumResidualsSqr() {
        double sumResidualsSqr = (getParams())[getNumParams()];
        return sumResidualsSqr;
    }
    
    /**  SD = sqrt(sum of residuals squared / number of params+1)
     */
    public double getSD() {
        double sd = Math.sqrt(getSumResidualsSqr() / numVertices);
        return sd;
    }
    
    /**  Get a measure of "goodness of fit" where 1.0 is best.
     *
     */
    public double getFitGoodness() {
        double sumY = 0.0;
        for (int i = 0; i < numPoints; i++) sumY += yData[i];
        double mean = sumY / numVertices;
        double sumMeanDiffSqr = 0.0;
        int degreesOfFreedom = numPoints - getNumParams();
        double fitGoodness = 0.0;
        for (int i = 0; i < numPoints; i++) {
            sumMeanDiffSqr += sqr(yData[i] - mean);
        }
        if (sumMeanDiffSqr > 0.0 && degreesOfFreedom != 0)
            fitGoodness = 1.0 - (getSumResidualsSqr() / degreesOfFreedom) * ((numParams) / sumMeanDiffSqr);
        
        return fitGoodness;
    }
    
    /** Get a string description of the curve fitting results
     * for easy output.
     */
    public String getResultString() {
        StringBuffer results = new StringBuffer("\nNumber of iterations: " + getIterations() +
        "\nMaximum number of iterations: " + getMaxIterations() +
        "\nSum of residuals squared: " + getSumResidualsSqr() +
        "\nStandard deviation: " + getSD() +
        "\nGoodness of fit: " + getFitGoodness() +
        "\nParameters:");
        char pChar = 'a';
        double[] pVal = getParams();
        for (int i = 0; i < numParams; i++) {
            results.append("\n" + pChar + " = " + pVal[i]);
            pChar++;
        }
        return results.toString();
    }
        
    double sqr(double d) { return d * d; }
    
    /** Adds sum of square of residuals to end of array of parameters */
    void sumResiduals (double[] x) {
        x[numParams] = 0.0;
        for (int i = 0; i < numPoints; i++) {
            x[numParams] = x[numParams] + sqr(f(fit,x,xData[i])-yData[i]);
            //        if (IJ.debugMode) ij.IJ.log(i+" "+x[n-1]+" "+f(fit,x,xData[i])+" "+yData[i]);
        }
    }

    /** Keep the "next" vertex */
    void newVertex() {
        for (int i = 0; i < numVertices; i++)
            simp[worst][i] = next[i];
    }
    
    /** Find the worst, nextWorst and best current set of parameter estimates */
    void order() {
        for (int i = 0; i < numVertices; i++) {
            if (simp[i][numParams] < simp[best][numParams]) best = i;
            if (simp[i][numParams] > simp[worst][numParams]) worst = i;
        }
        nextWorst = best;
        for (int i = 0; i < numVertices; i++) {
            if (i != worst) {
                if (simp[i][numParams] > simp[nextWorst][numParams]) nextWorst = i;
            }
        }
        //        IJ.write("B: " + simp[best][numParams] + " 2ndW: " + simp[nextWorst][numParams] + " W: " + simp[worst][numParams]);
    }

    /** Get number of iterations performed */
    public int getIterations() {
        return numIter;
    }
    
    /** Get maximum number of iterations allowed */
    public int getMaxIterations() {
        return maxIter;
    }
    
    /** Set maximum number of iterations allowed */
    public void setMaxIterations(int x) {
        maxIter = x;
    }
    
    /** Get number of simplex restarts to do */
    public int getRestarts() {
        return restarts;
    }
    
    /** Set number of simplex restarts to do */
    public void setRestarts(int x) {
        restarts = x;
    }

    /**
     * Gets index of highest value in an array.
     * 
     * @param              Double array.
     * @return             Index of highest value.
     */
    public static int getMax(double[] array) {
        double max = array[0];
        int index = 0;
        for(int i = 1; i < array.length; i++) {
            if(max < array[i]) {
                max = array[i];
                index = i;
            }
        }
        return index;
    }
 
}




