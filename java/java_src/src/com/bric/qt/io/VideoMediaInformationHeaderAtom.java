/*
 * @(#)VideoMediaInformationHeaderAtom.java
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

import com.bric.io.GuardedOutputStream;

/** This is not a public class because I expect to make some significant
 * changes to this project in the next year.
 * <P>Use at your own risk.  This class (and its package) may change in future releases.
 * <P>Not that I'm promising there will be future releases.  There may not be.  :)
 */
class VideoMediaInformationHeaderAtom extends LeafAtom {
	int version = 0;
	
	/** This should always be 1, unless you're dealing with
	 * a QT v1.0 file.
	 */
	int flags = 1;
	
	/** The most standard graphics mode is DITHER_COPY. */
	int graphicsMode = GraphicsModeConstants.DITHER_COPY;
	long opColor = 0x800080008000L;
	
	public VideoMediaInformationHeaderAtom() {
		super(null);
	}
	
	public VideoMediaInformationHeaderAtom(Atom parent,InputStream in) throws IOException {
		super(parent);
		version = in.read();
		flags = read24Int(in);
		graphicsMode = read16Int(in);
		opColor = read48Int(in);
	}
	
	protected String getIdentifier() {
		return "vmhd";
	}

	protected long getSize() {
		return 20;
	}

	protected void writeContents(GuardedOutputStream out) throws IOException {
		out.write(version);
		write24Int(out,flags);
		write16Int(out,graphicsMode);
		write48Int(out,opColor);
	}

	public String toString() {
		return "VideoMediaInformationHeaderAtom[ version="+version+", "+
		"flags="+flags+", "+
		"graphicsMode="+getFieldName(GraphicsModeConstants.class,graphicsMode)+", "+
		"opColor=0x"+Long.toString(opColor,16)+"]";
	}
}
