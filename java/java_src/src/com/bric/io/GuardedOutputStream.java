/*
 * @(#)GuardedOutputStream.java
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
package com.bric.io;

import java.io.IOException;
import java.io.OutputStream;

/** This restricts the amount of data that can be written
 * to an underlying <code>OutputStream</code>.
 * <P>This is especially useful in unit testing to ensure
 * that certain blocks of data do not exceed their allotted size.
 * <P>An IOException is thrown if you attempt to write more
 * data than this stream was told to allow.
 */
public class GuardedOutputStream extends MeasuredOutputStream {
	long limit;
	
	/** Constructs a new <code>GuardedOutputStream</code>.
	 * 
	 * @param out the underlying <code>OutputStream</code> to send data to.
	 * @param limit the number of bytes that can be written
	 */
	public GuardedOutputStream(OutputStream out,long limit) {
		super(out);
		this.limit = limit;
	}
	
	/** The number of bytes that can still be written to this stream.
	 * <P>(This value changes every time <code>write()</code> is called.)
	 * @return the number of bytes that can still be written to this stream.
	 */
	public long getLimit() {
		return limit;
	}

	public void write(byte[] b, int off, int len) throws IOException {
		if(len==0) return;
		
		if(limit<len) {
			throw new IOException("limit exceeded ("+(len-limit)+")");
		}
		limit -= len;
		super.write(b, off, len);
	}

	public void write(byte[] b) throws IOException {
		write(b,0,b.length);
	}

	public void write(int b) throws IOException {
		if(limit==0)
			throw new IOException("limit exceeded");
		limit--;
		super.write(b);
	}
	
	
}
