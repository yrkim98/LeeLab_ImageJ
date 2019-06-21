// ===============================================================================
// 
// VarianFDFReader.java
//
// This ImageJ plugin can be used to open MULTIPLE Varian imaging files in FDF format
// (FDF = Flexible Data Format). 

// This program is free distributed in the hope that it will be useful to those who are interested
// in processing MRI images (Varian) by ImageJ, but WITHOUT ANY WARRANTY; 
// without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
// PURPOSE.  See the GNU General Public License for more details.
//
// This program is free software; Therefore, you can redistribute it and/or modify it under the terms
// of the GNU General Public License as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// Michael A. Miller is highly appreciated for his pioneering effort, "FDF_Reader.java" downloadable
// from <http://php.iupui.edu/~mmiller3/ImageJ/>. 
//
// Copyright (C) Shanrong Zhang <shanrong.zhang@utsouthwestern.edu>
// Date: December 23, 2003
//
// ================================================================================

import java.io.*;
import java.util.*;
import java.lang.Math.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.filechooser.*;

import ij.*; 
import ij.plugin.*;
import ij.process.*;
import ij.io.*;
import ij.measure.*;

public class VarianFDFReader implements PlugIn {
	
	public boolean littleEndian = false;
	
	public void run(String arg) {
		OpenDialog od = new OpenDialog("Open fdf file...", arg);
		String directory = od.getDirectory();
		String name = od.getFileName();
		if (name==null)
			return;
		IJ.showStatus("Opening: " + directory + name);
		ImagePlus imp = load(directory, name);
		if (imp!=null) {
			imp.show();
			//IJ.showStatus( "Reslicing image ..." );
			//IJ.run("Reslice [/]...", "input=0.156 output=0.156 start=Left flip");
		}
	}

	public ImagePlus load(String directory, String name) {
		FileInfo fi = new FileInfo(); 
		if ((name == null) || (name == "")) return null;
//		IJ.showStatus("Reading fdf header from " + directory + name);
		try {fi = readHeader( directory, name );}
		catch (IOException e) {IJ.write("FileLoader: "+ e.getMessage());}
		FileOpener fo = new FileOpener(fi);  
 		ImagePlus imp = fo.open(false);
		return imp; 
	}


	public FileInfo readHeader(String directory, String fdffile) throws IOException { 
		FileInfo fi = new FileInfo();
		File file =  new File( directory + fdffile );
		FileReader fr = new FileReader ( file );
		BufferedReader in = new BufferedReader( fr );
		String line;
		boolean done = false;
		int count = 0;
		String subject = "NA";
		String spatial_rank = "NA";
		long xdim = 0;
		long ydim = 0;
		long zdim = 1;
		int rank = 0;
		float bits = 0;
		float xspan = 0;
		float yspan = 0;
		float zspan = 0;
		float xsize = 0;
		float ysize = 0;
		float zsize = 0;
		float elementsize = 0;
		if ( !file.exists() || !file.canRead() ) {
//			IJ.showStatus( "Can't read " + file );
			return(fi);
		}
		if ( file.isDirectory() ) {
			String [] files = file.list();
			for (int i=0; i< files.length; i++)
				IJ.showStatus( files[i] );
		}
		else try {
//			IJ.write("Parameters for " + fdffile + " : ");
			while ( ((line = in.readLine()) != null ) && !done ) {
//				IJ.write( "      FDF header>>> " + line);
				if ( line.startsWith("char  *spatial_rank") ) {
					StringTokenizer st = new StringTokenizer(line, " =;,{}\"");
					st.nextToken();	// 1st token is 'char'
					st.nextToken();	// 2nd token is '*spatial_rank'
					spatial_rank = st.nextToken();
				}
				if ( line.startsWith("float  matrix") ) {
					StringTokenizer st = new StringTokenizer(line, " =;,{}");
					st.nextToken();	// 1st token is 'float'
					st.nextToken();	// 2nd token is 'matrix'
					if ( spatial_rank.equals("2dfov") ) {
						xdim = Long.valueOf(st.nextToken()).longValue();	// 3rd token is xdim
						ydim = Long.valueOf(st.nextToken()).longValue();	// 4th token is ydim
					}
					else if ( spatial_rank.equals("3dfov") ) {
						xdim = Long.valueOf(st.nextToken()).longValue();	// 3rd token is xdim
						ydim = Long.valueOf(st.nextToken()).longValue();	// 4th token is ydim
						zdim = Long.valueOf(st.nextToken()).longValue();	// 5th token is zdim
					}
				}
				if ( line.startsWith("float  bits") ) {
					StringTokenizer st = new StringTokenizer(line, " =;,{}");
					st.nextToken();	// 1st token is 'float'
					st.nextToken();	// 2nd token is 'bits'
					bits = Long.valueOf(st.nextToken()).longValue();	// 3th token is bits
				}
				if ( line.startsWith("float  span") ) {
					StringTokenizer st = new StringTokenizer(line, " =;,{}");
					st.nextToken();	// 1st token is 'float'
					st.nextToken();	// 2nd token is 'span'
					if ( spatial_rank.equals("2dfov") ) {
						xspan = Float.valueOf(st.nextToken()).floatValue();	// 3rd token is xspan
						yspan = Float.valueOf(st.nextToken()).floatValue();	// 4th token is yspan
					}
					else if ( spatial_rank.equals("3dfov") ) {
			    			xspan = Float.valueOf(st.nextToken()).floatValue();	// 3rd token is xspan
			    			yspan = Float.valueOf(st.nextToken()).floatValue();	// 4th token is yspan
			    			zspan = Float.valueOf(st.nextToken()).floatValue();	// 5th token is zspan
					}
				}
				count = count + 1;
				if ( count > 32 ) {
					done = true;
				}
			}
		}
		catch ( FileNotFoundException e ) {
//			IJ.showStatus( "File Disappeared" );
		}
		xsize = 10 * xspan / xdim;
		ysize = 10 * yspan / ydim;
		zsize = 10 * zspan / zdim;
//		IJ.write("      xdim:  " + xdim);
//		IJ.write("      ydim:  " + ydim);
//		IJ.write("      zdim:  " + zdim);
//		IJ.write("      xspan: " + xspan);
//		IJ.write("      yspan: " + yspan);
//		IJ.write("      zspan: " + zspan);
//		IJ.write("      xsize: " + xsize);
//		IJ.write("      ysize: " + ysize);
//		IJ.write("      zsize: " + zsize);
		fi.fileName = fdffile;
		fi.directory = directory;
		fi.fileFormat = fi.RAW;
		fi.width = (int)xdim;
		fi.height = (int)ydim;
		fi.nImages = (int)zdim;
		fi.pixelWidth = xsize;
		fi.pixelHeight = ysize;
		fi.pixelDepth = zsize;
		fi.intelByteOrder = false;
		fi.fileType = FileInfo.GRAY32_FLOAT; 
		fi.unit = "mm";
		fi.offset = (int)(file.length() - xdim*ydim*zdim*bits/8); 
//		IJ.write("      fi.offset = " + fi.offset);
//		IJ.write("");
		return (fi);
	}
}
