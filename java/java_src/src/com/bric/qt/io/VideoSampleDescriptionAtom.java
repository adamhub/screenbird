/*
 * @(#)VideoSampleDescriptionAtom.java
 *
 * $Date: 2011-05-02 16:01:45 -0500 (Mon, 02 May 2011) $
 *
 * Copyright (c) 2011 by Jeremy Wood.
 * All rights reserved.
 *
 * The copyright of this software is owned by Jeremy Wood. 
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * Jeremy Wood. For details see accompanying license terms.
 * 
 * This software is probably, but not necessarily, discussed here:
 * http://javagraphics.java.net/
 * 
 * That site should also contain the most recent official version
 * of this software.  (See the SVN repository for more details.)
 */
package com.bric.qt.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** This is not a public class because I expect to make some significant
 * changes to this project in the next year.
 * <P>Use at your own risk.  This class (and its package) may change in future releases.
 * <P>Not that I'm promising there will be future releases.  There may not be.  :)
 */
class VideoSampleDescriptionAtom extends SampleDescriptionAtom {

	public VideoSampleDescriptionAtom() {
		super();
	}
	
	public VideoSampleDescriptionAtom(Atom parent, InputStream in)
			throws IOException {
		super(parent, in);
	}
	
	protected SampleDescriptionEntry readEntry(InputStream in) throws IOException {
		return new VideoSampleDescriptionEntry(in);
	}
}

class VideoSampleDescriptionEntry extends SampleDescriptionEntry {
	/** A 16-bit integer indicating the version number of the compressed data.  This is set to 0, unless
	 * a compressor has changed its format.
	 */
	int version = 0;
	/** A 16-bit integer that must be set to 0. */
	int revision = 0;
	/** A 32-bit integer that specifies the developer of the compressor that generated the compressed data.
	 * Often this field contains 'appl' to indicate Apple Computer, Inc.
	 */
	String vendor = "bric";
	/** A 32-bit integer containing a value from 0 to 1023 indicating the degree of temporal compression. */
	long temporalQuality = 0;
	/** A 32-bit integer containing a value from 0 to 1024 indicating the degree of spatial compression. */
	long spatialQuality = 512;
	/** A 16-bit integer that specifies the width of the source image in pixels. */
	int width;
	/** A 16-bit integer that specifies the height of the source image in pixels. */
	int height;
	/** A 32-bit fixed-point number containing the horizontal resolution of the image in pixels for inch. */
	float horizontalResolution = 72;
	/** A 32-bit fixed-point number containing the vertical resolution of the image in pixels for inch. */
	float verticalResolution = 72;
	/** A 32-bit integer that must be set to zero. */
	long dataSize = 0;
	
	/** A 16-bit integer that indicates how many frames of compressed data are stored in
	 * each sample.  Usually set to 1.
	 */
	int frameCount = 1;
	
	/** A Pascal string containing the name of the creator that compressed
	 * an image, such as "jpeg".
	 */
	String compressorName = "";
	
	/** A 16-bit integer that indicates the pixel depth of the compressed image.
	 * Values of 1, 2, 4, 8, 16, 24, and 32 indicate the depth of color images.
	 * The value of 32 should be used only if the image contains an alpha channel.
	 * Values of 34, 36, and 40 indicate 2-, 4-, and 9-bit grayscale, respectively,
	 * for grayscale images.
	 */
	int depth = 24;
	
	/** A 16-bit integer that identifies which color table ot use.  If this field
	 * is set to -1, the default color table should be used for the specified
	 * depth.  For all depths below 16 bits per pixels, this indicates a
	 * standard macintosh color table for the specified depth.  Depths of 16,
	 * 24, and 32 have no color table.
	 */
	int colorTableID = 65535;
	
	public VideoSampleDescriptionEntry(String type,int dataReference,int w,int h) {
		super(type,dataReference);
		width = w;
		height = h;
	}
	
	public VideoSampleDescriptionEntry(InputStream in) throws IOException {
		super(in);
		version = Atom.read16Int(in);
		revision = Atom.read16Int(in);
		vendor = Atom.read32String(in);
		temporalQuality = Atom.read32Int(in);
		spatialQuality = Atom.read32Int(in);
		width = Atom.read16Int(in);
		height = Atom.read16Int(in);
		horizontalResolution = Atom.read16_16Float(in);
		verticalResolution = Atom.read16_16Float(in);
		dataSize = Atom.read32Int(in);	
		frameCount = Atom.read16Int(in);
		compressorName = Atom.read32BytePascalString(in);
		depth = Atom.read16Int(in);
		colorTableID = Atom.read16Int(in);
	}

	protected void write(OutputStream out) throws IOException {
		Atom.write32Int(out, getSize());
		Atom.write32String(out, type);
		Atom.write48Int(out, 0);
		Atom.write16Int(out, dataReference);
		
		Atom.write16Int(out, version);
		Atom.write16Int(out, revision);
		Atom.write32String(out, vendor);
		Atom.write32Int(out, temporalQuality);
		Atom.write32Int(out, spatialQuality);
		Atom.write16Int(out, width);
		Atom.write16Int(out, height);
		Atom.write16_16Float(out, horizontalResolution);
		Atom.write16_16Float(out, verticalResolution);
		Atom.write32Int(out, dataSize);
		Atom.write16Int(out, frameCount);
		Atom.write32BytePascalString(out, compressorName);
		Atom.write16Int(out, depth);
		Atom.write16Int(out, colorTableID);
	}

	protected long getSize() {
		return 86;
	}
	
	public String toString() {
		return "VideoSampleDescriptionEntry[ type=\""+type+"\", "+
		"dataReference="+dataReference+", "+
		"version="+version+", "+
		"revision="+revision+", "+
		"vendor=\""+vendor+"\", "+
		"temporalQuality="+temporalQuality+", "+
		"spatialQuality="+spatialQuality+", "+
		"width="+width+", "+
		"height="+height+", "+
		"horizontalResolution="+horizontalResolution+", "+
		"verticalResolution="+verticalResolution+", "+
		"dataSize="+dataSize+", "+
		"frameCount="+frameCount+", "+
		"compressorName=\""+compressorName+"\", "+
		"depth="+depth+", "+
		"colorTableID="+colorTableID+" ]";
	}
	
	protected static VideoSampleDescriptionEntry createJPEGDescription(int width,int height) {
		VideoSampleDescriptionEntry e = new VideoSampleDescriptionEntry("jpeg",1,width,height);
		e.compressorName = "Photo - JPEG";
		e.version = 1;
		e.revision = 1;
		e.temporalQuality = 0;
		e.spatialQuality = 512;
		return e;
	}
}
