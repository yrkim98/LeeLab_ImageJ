import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.measure.Calibration;


/** This plugin removes slices from a stack. */
public class DeInterleave_ implements PlugIn {
	private static int slices= 1;
	private static int nCh = 2;
	private static int last;
	private static int nStacks;
	static boolean keep = Prefs.get("di_keep.boolean", true);

	String title;
	Calibration oc;

	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null)
			{IJ.noImage(); return;}
		ImageStack stack = imp.getStack();
		oc = imp.getCalibration().copy();
		if (stack.getSize()==1)
			{IJ.error("Stack Required"); return;}
		if (!showDialog(stack))
			return;
		title=imp.getTitle();
		deinterleave(stack, nStacks);
		if(!keep)
			imp.getWindow().close();

		IJ.register(DeInterleave_.class);
	}

	public boolean showDialog(ImageStack stack) {
		
		int last = stack.getSize();
		GenericDialog gd = new GenericDialog("DeInterleave_plus");
		gd.addNumericField("Number of substacks", nStacks, 0);
		gd.addCheckbox("Keep source", keep);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		nStacks= (int) gd.getNextNumber();
		keep = gd.getNextBoolean();
		return true;
	}
	
	public void deinterleave(ImageStack stack, int nStacks) 
	{
		
		int first =0;
		last = stack.getSize();
		int slices = last/nStacks;
 		int count = 0;
		int sliceCount=0;		
		ImageStack newstack; 
		ImageProcessor ip;
		int s2;
		String sliceName="";
		for (int i=0; i<nStacks; i++) 
			{
			newstack = new ImageStack(stack.getWidth(), stack.getHeight()) ;
			for(int s=0; s<slices; s++)
				{
				
				s2=i+(nStacks*s)+1;
				if(s2<=last){
				ip = stack.getProcessor(s2);
				sliceName = stack.getSliceLabel(s2);
				newstack.addSlice("slice:" + stack.getSliceLabel(s2) +"  "+ s2, ip);}
				}
			new ImagePlus(title+"_"+sliceName, newstack).show();
			ImagePlus impTmp = WindowManager.getCurrentImage();
			impTmp.setCalibration(oc);
			impTmp.getWindow().repaint();
			}
		
		
	}

}
