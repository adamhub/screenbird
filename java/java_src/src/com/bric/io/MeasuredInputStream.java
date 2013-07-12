/*
 * @(#)MeasuredInputStream.java
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
import java.io.InputStream;

/** This <code>InputStream</code> relays information from an underlying
 * <code>InputStream</code> while measuring how many bytes have been
 * read or skipped.
 * <P>Note marking is not supported in this object.
 */
public class MeasuredInputStream extends InputStream {
	InputStream in;
	long read = 0;
	
	public MeasuredInputStream(InputStream i) {
		this.in = i;
	}

	public int available() throws IOException {
		return in.available();
	}
	
	/** Returns the number of bytes that have been read (or skipped).
	 * 
	 * @return the number of bytes that have been read (or skipped).
	 */
	public long getReadBytes() {
		return read;
	}

	public void close() throws IOException {
		in.close();
	}

	public synchronized void mark(int readlimit) {
		throw new RuntimeException();
	}

	public boolean markSupported() {
		return false;
	}

	public int read() throws IOException {
		int k = in.read();
		if(k==-1)
			return -1;
		read++;
		return k;
	}

	public int read(byte[] b, int off, int len) throws IOException {
		int returnValue = in.read(b, off, len);
		if(returnValue==-1)
			return -1;
		read += returnValue;
		
		return returnValue;
	}

	public int read(byte[] b) throws IOException {
		return read(b,0,b.length);
	}

	public synchronized void reset() throws IOException {
		throw new RuntimeException();
	}

	public long skip(long n) throws IOException {
		long returnValue = in.skip(n);
		if(returnValue==-1)
			return -1;
		read += returnValue;
		
		return returnValue;
	}

	/** This skips forward to the requested position.
	 * If the requested position is less than the current
	 * position, then an exception is thrown.
	 * @param pos
	 * @throws IOException
	 */
	public void seek(long pos) throws IOException {
		if(pos==read) return;
		if(pos<read) throw new IOException("Cannot seek backwards.");
		skip(pos-read);
	}
}
