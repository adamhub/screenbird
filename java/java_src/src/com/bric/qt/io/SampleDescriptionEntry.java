/*
 * @(#)SampleDescriptionEntry.java
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

public abstract class SampleDescriptionEntry {
	/** If this entry is read from an <code>InputStream</code>, then this
	 * is the size that this entry should be.
	 * <P>Subclasses may consult this value when reading from a stream to
	 * determine how much more data to read in this entry.
	 * <P>Otherwise this field is unused.
	 */
	long inputSize;
	String type;
	int dataReference;
	public SampleDescriptionEntry(String type,int dataReference) {
		this.type = type;
		this.dataReference = dataReference;
	}
	
	public SampleDescriptionEntry(InputStream in) throws IOException {
		inputSize = Atom.read32Int(in);
		type = Atom.read32String(in);
		Atom.skip(in,6); //reserved
		dataReference = Atom.read16Int(in);
	}
	
	protected abstract long getSize();
	
	protected abstract void write(OutputStream out) throws IOException;
}
