//=================================================================================
// Time-stamp: <2002-04-04 12:17:15 miller>
//
// SegmentingAssistant_.java
//  	$Id: SegmentingAssistant_.java,v 1.5 2002/04/04 17:19:28 miller Exp $	
//
// An ImageJ plugin that lets me use the wand tool for fancy segmentation.
//
// Copyright (C) 2002 Michael A. Miller <mmiller3@iupui.edu>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or (at
// your option) any later version.
//
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
// USA.
//
//=================================================================================

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.measure.*;

public class SegmentingAssistant_ 
    implements PlugInFilter, Measurements {

    ImagePlus imp;
    WandMM wand;
    Roi roi;
    ImageStack stack;

    double x_centroid;
    double y_centroid;
    double min_level;
    double max_level;
    double image_min;
    double image_max;

    int min_width=15;
    int min_height=15;
    int xd=20;

    boolean debug = false;

    public int setup(String arg, ImagePlus imp) {
	if ( debug ) IJ.write("SegmentingAssistant.setup...");
	this.imp = imp;
	// See if this is a stack
	stack = imp.getStack();
	return DOES_ALL;
    }

    public void run(ImageProcessor ip) {
	if ( debug ) IJ.write("SegmentingAssistant.run...");

	initialize();

	if ( roi == null || roi.getType() > 3 ) {
	    IJ.error("You must draw a closed ROI before using this plugin." );
	    return;
	}
	if ( debug ) IJ.write( "roi.getType() = " + roi.getType() );

	makeWindow();

	if ( outline() ) IJ.write( "found:" + imp.getRoi() );

    }

    public void initialize() {
	if ( debug ) IJ.write("SegmentingAssistant.initialize...");

	imp = WindowManager.getCurrentImage();
	roi = imp.getRoi();

	int measurements = Analyzer.getMeasurements(); 
	                           // defined in Set Measurements dialog
	measurements |= CENTROID; // make sure centroid is included
	measurements |= MIN_MAX;  // make sure min_max is included
	Analyzer.setMeasurements(measurements);

	ImageStatistics stats = imp.getStatistics(measurements);

	x_centroid = stats.xCentroid;
	y_centroid = stats.yCentroid;

	min_level = stats.min;
	max_level = stats.max;

	imp.killRoi();
	stats = imp.getStatistics(measurements);
	image_min = stats.min;
	image_max = stats.max;
	IJ.write("min max = " + image_min + " " + image_max );

	if ( debug ) IJ.write("Centroid = (" + x_centroid + ", " + y_centroid + ")" );
	if ( debug ) IJ.write("Pixel levels are in [" + min_level + ", " + max_level + "]" );
    }
    
    boolean outline() {
	if ( debug ) IJ.write("SegmentingAssistant.outline...");
		
	int x, y;

	if ( debug ) IJ.write("trying to make a WandMM...");
	wand = new WandMM( imp.getProcessor() ) ;
	if ( debug ) IJ.write("made a WandMM...");

	imp.killRoi();
	wand.npoints = 0;

	x = (int)x_centroid;
	y = (int)y_centroid;
	wand.autoOutline( x, y, (int)min_level, (int)max_level );

	String s = ":[" + x + "," + y + "]: ("
	    + wand.npoints + ":" + wand.xpoints[0] + "," + wand.ypoints[0] + ")";
	if ( wand.npoints < 3 ) {
	    IJ.write( "wand.autoOutline failed" + s );
	    return false;
	} 

	roi = new PolygonRoi( wand.xpoints, wand.ypoints, wand.npoints, imp, Roi.POLYGON );
	imp.setRoi( roi );
	Rectangle r = roi.getBoundingRect();
	if ( r.width < min_width || r.height < min_height ) {
	    IJ.write( "wand object too small:" + r.width + "x" + r.height + " " + s );
	    return false;
	} 
	if ( wand.xpoints[0] - x > xd ) { 
	    IJ.write( "wand too far?" + s );
	    return false;
	} 
	return true;
    }

    public void makeWindow() {
	if ( debug ) IJ.write("SegmentingAssistant.makeWindow...");

	final JFrame f = new JFrame("Segmenting Assistant");
	f.setSize( 200, 200 );
	f.setLocation(100, 100);

	// Create a label for the x position slider:
	final JLabel xLabel = new JLabel( "Horizontal Centroid:: " + (int)x_centroid,
					  JLabel.LEFT );
	xLabel.setAlignmentX( Component.LEFT_ALIGNMENT );
	
	// Create the x position slider:
	final JSlider xSlider = new JSlider( JSlider.HORIZONTAL, 
				       0, imp.getWidth(), (int)x_centroid );
	xSlider.setMajorTickSpacing( imp.getWidth()/4 );
        xSlider.setMinorTickSpacing( imp.getWidth()/16 );
        xSlider.setPaintTicks( true );
        xSlider.setPaintLabels( true );
        xSlider.setBorder( BorderFactory.createEmptyBorder( 0, 0, 10, 0 ) );


	// Create a label for the y position slider:
	final JLabel yLabel = new JLabel( "Vertical Centroid:: " + (int)y_centroid,
					  JLabel.LEFT );
	xLabel.setAlignmentX( Component.LEFT_ALIGNMENT );

	// Create the y position slider:
	final JSlider ySlider = new JSlider( JSlider.HORIZONTAL, 
				       0, imp.getHeight(), (int)y_centroid );
	ySlider.setMajorTickSpacing( imp.getHeight()/4 );
        ySlider.setMinorTickSpacing( imp.getHeight()/16 );
        ySlider.setPaintTicks( true );
        ySlider.setPaintLabels( true );
        ySlider.setBorder( BorderFactory.createEmptyBorder( 0, 0, 10, 0 ) );


	// Create a label for the min level slider:
	final JLabel minLevelLabel = new JLabel( "Min Level: " + (int)min_level,
						 JLabel.LEFT );
	minLevelLabel.setAlignmentX( Component.LEFT_ALIGNMENT );

	// Create the min level slider:
	final JSlider minLevelSlider = new JSlider( JSlider.HORIZONTAL, 
						    (int)image_min, (int)image_max,
						    (int)min_level );
	minLevelSlider.setMajorTickSpacing( (int)(image_max-image_min)/4 );
        minLevelSlider.setMinorTickSpacing( (int)(image_max-image_min)/16 );
        minLevelSlider.setPaintTicks( true );
        minLevelSlider.setPaintLabels( true );
        minLevelSlider.setBorder( BorderFactory.createEmptyBorder( 0, 0, 10, 0 ) );
	
	
	// Create a label for the max level slider:
	final JLabel maxLevelLabel = new JLabel( "Max Level: " + (int)max_level,
						 JLabel.LEFT );
	maxLevelLabel.setAlignmentX( Component.LEFT_ALIGNMENT );

	// Create the max level slider:
	final JSlider maxLevelSlider = new JSlider( JSlider.HORIZONTAL, 
						    (int)image_min, (int)image_max,
						    (int)max_level );
	maxLevelSlider.setMajorTickSpacing( (int)(image_max-image_min)/4 );
        maxLevelSlider.setMinorTickSpacing( (int)(image_max-image_min)/16 );
        maxLevelSlider.setPaintTicks( true );
        maxLevelSlider.setPaintLabels( true );
        maxLevelSlider.setBorder( BorderFactory.createEmptyBorder( 0, 0, 10, 0 ) );


	// Create buttons for outlining, clearing and undoing:
	final JButton outlineButton = new JButton( "Outline" );
	outlineButton.setActionCommand( "Outline" );

	final JButton newROIButton = new JButton( "Set New ROI" );
	newROIButton.setActionCommand( "Set New ROI" );

	final JButton clearButton = new JButton( "Clear ROI" );
	clearButton.setActionCommand( "Clear ROI" );
	final JButton clearOutsideButton = new JButton( "Clear Outside ROI" );
	clearOutsideButton.setActionCommand( "Clear Outside ROI" );
	final JButton undoButton = new JButton( "Undo" );
	undoButton.setActionCommand( "Undo" );
	 
	final JButton drawButton = new JButton( "Draw" );
	drawButton.setActionCommand( "Draw" );
	final JButton fillButton = new JButton( "Fill" );
	fillButton.setActionCommand( "Fill" );
	 
	final JButton doneButton = new JButton( "Done" );
	doneButton.setActionCommand( "Done" );

	// For stacks:
	int ns = stack.getSize();
	if ( debug ) IJ.write( "getSize = " + ns );
	 
	final JButton nextButton = new JButton( "Next Slice" );
	undoButton.setActionCommand( "Next Slice" );
	final JButton previousButton = new JButton( "Previous Slice" );
	undoButton.setActionCommand( "Previous Slice" );
	 


        // Setup event handlers for the sliders:
	xSlider.addChangeListener( new ChangeListener() {
		public void stateChanged( ChangeEvent e ) {
		    xLabel.setText( "Horizontal Centroid:: " + xSlider.getValue() );
		    x_centroid = (double)xSlider.getValue();
		    outline();
		}
	    } );
	ySlider.addChangeListener( new ChangeListener() {
		public void stateChanged( ChangeEvent e ) {
		    yLabel.setText( "Vertical Centroid:: " + ySlider.getValue() );
		    y_centroid = (double)ySlider.getValue();
		    outline();
		}
	    } );
	minLevelSlider.addChangeListener( new ChangeListener() {
		public void stateChanged( ChangeEvent e ) {
		    minLevelLabel.setText( "Min Level: " + minLevelSlider.getValue() );
		    min_level = (double)minLevelSlider.getValue();
		    outline();
		}
	    } );
	maxLevelSlider.addChangeListener( new ChangeListener() {
		public void stateChanged( ChangeEvent e ) {
		    maxLevelLabel.setText( "Max Level: " + maxLevelSlider.getValue() );
		    max_level = (double)maxLevelSlider.getValue();
		    outline();
		}
	    } );

	
	// Register for mouse events:
        outlineButton.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( debug ) IJ.write( "Button pressed: " + e.getActionCommand() );
		    outline();
		}
	    } );
        newROIButton.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( debug ) IJ.write( "Button pressed: " + e.getActionCommand() );
		    initialize();
		    minLevelSlider.setValue( (int)min_level );
		    minLevelLabel.setText( "Min Level: " + minLevelSlider.getValue() );
		    maxLevelSlider.setValue( (int)max_level );
		    maxLevelLabel.setText( "Max Level: " + maxLevelSlider.getValue() );
		    xSlider.setValue( (int)x_centroid );
		    xLabel.setText( "Horizontal Centroid: " + xSlider.getValue() );
		    ySlider.setValue( (int)y_centroid );
		    yLabel.setText( "Vertical Centroid: " + ySlider.getValue() );
		}
	    } );
        clearButton.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( debug ) IJ.write( "Button pressed: " + e.getActionCommand() );
		    IJ.run( "Clear" );
		}
	    } );
        clearOutsideButton.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( debug ) IJ.write( "Button pressed: " + e.getActionCommand() );
		    IJ.run( "Clear Outside" );
		}
	    } );
        undoButton.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( debug ) IJ.write( "Button pressed: " + e.getActionCommand() );
		    IJ.run( "Undo" );
		}
	    } );

        drawButton.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( debug ) IJ.write( "Button pressed: " + e.getActionCommand() );
		    IJ.run( "Draw" );
		    outline();
		}
	    } );
        fillButton.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( debug ) IJ.write( "Button pressed: " + e.getActionCommand() );
		    IJ.run( "Fill" );
		    outline();
		}
	    } );



        doneButton.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( debug ) IJ.write( "Button pressed: " + e.getActionCommand() );
		    f.setVisible( false );
		}
	    } );

        nextButton.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( debug ) IJ.write( "Button pressed: " + e.getActionCommand() );
		    if ( imp.getCurrentSlice() < stack.getSize() ) {
			imp.setSlice( imp.getCurrentSlice() + 1 );
			outline();
		    }		   
		}
	    } );
        previousButton.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( debug ) IJ.write( "Button pressed: " + e.getActionCommand() );
		    if ( imp.getCurrentSlice() > 0 ) {
			imp.setSlice( imp.getCurrentSlice() - 1 );
			outline();
		    }		   
		}
	    } );


	// Position everything in the content pane:
	JPanel contentPane = new JPanel();
        contentPane.setLayout( new BoxLayout( contentPane, BoxLayout.Y_AXIS ) );
        contentPane.add( xLabel );
        contentPane.add( xSlider );
        contentPane.add( yLabel );
        contentPane.add( ySlider );
        contentPane.add( minLevelLabel );
        contentPane.add( minLevelSlider );
        contentPane.add( maxLevelLabel );
        contentPane.add( maxLevelSlider );
	
	// Use a slightly 10 pt font for the buttons:
	Font buttonFont = outlineButton.getFont().deriveFont((float)10.0);
	outlineButton.setFont( buttonFont );
	newROIButton.setFont( buttonFont );
	clearButton.setFont( buttonFont );
	clearOutsideButton.setFont( buttonFont );
	drawButton.setFont( buttonFont );
	fillButton.setFont( buttonFont );
	undoButton.setFont( buttonFont );
	previousButton.setFont( buttonFont );
	nextButton.setFont( buttonFont );
	doneButton.setFont( buttonFont );

	JPanel roiPanel = new JPanel();
	roiPanel.add( outlineButton );
	roiPanel.add( newROIButton );
	contentPane.add(roiPanel);
	
	JPanel editPanel = new JPanel();
	editPanel.add( clearButton );
	editPanel.add( clearOutsideButton );
	editPanel.add( drawButton );
	editPanel.add( fillButton );
	editPanel.add( undoButton );
	contentPane.add(editPanel);
	
	JPanel slicePanel = new JPanel();
	slicePanel.add( previousButton );
	slicePanel.add( nextButton );
	contentPane.add(slicePanel);

	contentPane.add( doneButton );

        contentPane.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );
        
	f.setContentPane( contentPane );

	f.pack();
	f.setVisible( true );
    }
}


//=================================================================================
// WandMM.java
// 
// This class implements something like ImageJ's wand (tracing) tool.
// The difference is that this one is intended to work with all image
// types, not just byte and 8 bit color images.
//
//=================================================================================
class WandMM {
    boolean debug = false;

    static final int UP = 0,
        DOWN = 1, 
        UP_OR_DOWN = 2, 
        LEFT = 3, 
        RIGHT = 4, 
        LEFT_OR_RIGHT = 5, 
        NA = 6;
    
    // The number of points in the generated outline:
    public int npoints;
    private int maxPoints = 1000;

    // The x-coordinates of the points in the outline:
    public int[] xpoints = new int[maxPoints];
    // The y-coordinates of the points in the outline:
    public int[] ypoints = new int[maxPoints];

    private ImageProcessor wandip;

    private int width, height;
    private float lowerThreshold, upperThreshold;

    // Construct a Wand object from an ImageProcessor:
    public WandMM( ImageProcessor ip ) {
	if ( debug ) IJ.write("WandMM...");
	
	wandip = ip;

        width = ip.getWidth();
        height = ip.getHeight();
	if ( debug ) IJ.write("WandMM middle pixel = " + ip.getPixelValue(128,128));
	if ( debug ) IJ.write("done with constructor");
    }
        
    private boolean inside( int x, int y ) {
	//if ( debug ) IJ.write("WandMM.inside...");
        float value;
	if ( debug ) IJ.write( "WandMM.getPixel(x,y) = " + wandip.getPixelValue(x,y) );
	//value =  getPixel( x, y );
	value = wandip.getPixelValue(x,y);
        return ( value >= lowerThreshold ) && ( value <= upperThreshold );
    }

    // Are we tracing a one pixel wide line?
    boolean isLine( int xs, int ys ) {
	if ( debug ) IJ.write("WandMM.isLine...");

        int r = 5;
        int xmin = xs;
        int xmax = xs + 2 * r;
        if ( xmax >= width ) xmax = width - 1;
        int ymin = ys - r;
        if ( ymin < 0 ) ymin = 0;
        int ymax = ys + r;
        if ( ymax >= height ) ymax = height - 1;
        int area = 0;
        int insideCount = 0;
        for ( int x = xmin; ( x <= xmax ); x++ )
            for ( int y = ymin; y <= ymax; y++ ) {
                area++;
                if ( inside( x, y ) )
                    insideCount++;
            }
        if (IJ.debugMode)
            IJ.write((((double)insideCount)/area>=0.75?"line ":"blob ")
                     + insideCount
                     + " "
                     + area
                     + " "
                     + IJ.d2s(((double)insideCount)/area));
        return ((double)insideCount)/area>=0.75;
    }

    // Traces an object defined by lower and upper threshold
    // values. The boundary points are stored in the public xpoints
    // and ypoints fields.
    public void autoOutline( int startX, int startY, int lower, int upper ) {
	if ( debug ) IJ.write("WandMM.autoOutline...");

        int x = startX;
        int y = startY;
        int direction;
        lowerThreshold = lower;
        upperThreshold = upper;
        if ( inside(x,y) ) {
            do { x++; } while ( inside(x,y) );
            if ( ! inside( x-1, y-1 ) )
                direction = RIGHT;
            else if ( inside( x, y-1 ) )
                direction = LEFT;
            else
                direction = DOWN;
        } else {
            do { x++; } while ( ! inside(x,y) && x < width );
            direction = UP;
            if ( x >= width ) return;
        }
        traceEdge( x, y, direction );
    }

    void traceEdge( int xstart, int ystart, int startingDirection ) {
	if ( debug ) IJ.write("WandMM.traceEdge...");

        int[] table = {
            // 1234, 1=upper left pixel,  2=upper right, 3=lower left, 4=lower right
            NA,                 // 0000, should never happen
            RIGHT,              // 000X,
            DOWN,               // 00X0
            RIGHT,              // 00XX
            UP,                 // 0X00
            UP,                 // 0X0X
            UP_OR_DOWN,         // 0XX0 Go up or down depending on current direction
            UP,                 // 0XXX
            LEFT,               // X000
            LEFT_OR_RIGHT,      // X00X  Go left or right depending on current direction
            DOWN,               // X0X0
            RIGHT,              // X0XX
            LEFT,               // XX00
            LEFT,               // XX0X
            DOWN,               // XXX0
            NA,                 // XXXX Should never happen
        };
        int index;
        int newDirection;
        int x = xstart;
        int y = ystart;
        int direction = startingDirection;
        
        boolean UL = inside( x-1, y-1 );    // upper left
        boolean UR = inside( x, y-1 );      // upper right
        boolean LL = inside( x-1, y );      // lower left
        boolean LR = inside( x, y );        // lower right
	//xpoints[0] = x;
	//ypoints[0] = y;
	int count = 0;
        //IJ.write("");
        //IJ.write(count + " " + x + " " + y + " " + direction + " " + insideValue);
	do {
	    index = 0;
	    if (LR) index |= 1;
	    if (LL) index |= 2;
	    if (UR) index |= 4;
	    if (UL) index |= 8;
	    newDirection = table[index];
	    if (newDirection==UP_OR_DOWN) {
		if (direction==RIGHT)
		    newDirection = UP;
		else
		    newDirection = DOWN;
	    }
	    if (newDirection==LEFT_OR_RIGHT) {
		if (direction==UP)
		    newDirection = LEFT;
		else
		    newDirection = RIGHT;
	    }
	    if (newDirection!=direction) {
		xpoints[count] = x;
		ypoints[count] = y;
		count++;
		if (count==xpoints.length) {
		    int[] xtemp = new int[maxPoints*2];
		    int[] ytemp = new int[maxPoints*2];
		    System.arraycopy(xpoints, 0, xtemp, 0, maxPoints);
		    System.arraycopy(ypoints, 0, ytemp, 0, maxPoints);
		    xpoints = xtemp;
		    ypoints = ytemp;
		    maxPoints *= 2;
		}
                //if (count<10) IJ.write(count + " " + x + " " + y + " " + newDirection + " " + index);
	    }
	    switch (newDirection) {
	    case UP:
		y = y-1;
		LL = UL;
		LR = UR;
		UL = inside(x-1, y-1);
		UR = inside(x, y-1);
		break;
	    case DOWN:
		y = y + 1;
		UL = LL;
		UR = LR;
		LL = inside(x-1, y);
		LR = inside(x, y);
		break;
	    case LEFT:
		x = x-1;
		UR = UL;
		LR = LL;
		UL = inside(x-1, y-1);
		LL = inside(x-1, y);
		break;
	    case RIGHT:
		x = x + 1;
		UL = UR;
		LL = LR;
		UR = inside(x, y-1);
		LR = inside(x, y);
		break;
	    }
	    direction = newDirection;
	} while ((x!=xstart || y!=ystart || direction!=startingDirection));
	npoints = count;
    }
    
}
