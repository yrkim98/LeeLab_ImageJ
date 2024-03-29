{\rtf1\ansi\ansicpg1252\cocoartf1038\cocoasubrtf360
{\fonttbl\f0\fmodern\fcharset0 Courier;}
{\colortbl;\red255\green255\blue255;}
\margl1440\margr1440\vieww9000\viewh8400\viewkind0
\deftab720
\pard\pardeftab720\ql\qnatural

\f0\fs24 \cf0 import ij.plugin.*;\
import ij.*;\
import ij.gui.*;\
import ij.process.*;\
\
/** Creates a w*h slice stack from an wxh image montage. This is the opposite of ImageJ's "Make Montage" command. */\
\
public class Stack_Maker implements PlugIn \{\
\
	private static int w=2, h=2;\
\
	public void run(String arg) \{\
		ImagePlus imp = WindowManager.getCurrentImage();\
		if (imp==null)\
			\{IJ.noImage(); return;\}\
		GenericDialog gd = new GenericDialog("Stack Maker");\
		gd.addNumericField("Images_Per_Row: ", w, 0);\
		gd.addNumericField("Images_Per_Column: ", h, 0);\
		gd.showDialog();\
		if (gd.wasCanceled())\
			return;\
		w = (int)gd.getNextNumber();\
		h = (int)gd.getNextNumber();\
		ImageStack stack = makeStack(imp.getProcessor(), w, h);\
		new ImagePlus("Stack", stack).show();\
		IJ.register(Stack_Maker.class);\
	\}\
	\
	public ImageStack makeStack(ImageProcessor ip, int w, int h) \{\
		int stackSize = w*h;\
		int width = ip.getWidth()/w;\
		int height = ip.getHeight()/h;\
		ImageStack stack = new ImageStack(width, height);\
		for (int y=0; y<h; y++)\
			for (int x=0; x<w; x++) \{\
				ip.setRoi(x*width, y*height, width, height);\
				stack.addSlice(null, ip.crop());\
			\}\
		return stack;\
	\}\
	\
\}\
}