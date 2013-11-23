/*
 * @(#)MeasuredOutputStream.java
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

import java.io.*;

/** This <code>OutputStream</code> passes information along to an underlying
 * <code>OutputStream</code> while counting how many bytes are written.
 * <P>At any point calling <code>getWrittenCount()</code> tells how the amount
 * of data that has been written since this object was constructed.
 *
 */
public class MeasuredOutputStream extends OutputStream {
	protected long written = 0;
	OutputStream out;
	private boolean closed = false;
	
	public MeasuredOutputStream(OutputStream out) {
		this.out = out;
	}

	/** Returns the number of bytes written since this object was constructed.
	 *  
	 * @return the number of bytes written since this object was constructed.
	 */
	public long getBytesWritten() {
		return written;
	}
	
	public void close() throws IOException {
		out.close();
		closed = true;
	}

	public void flush() throws IOException {
		out.flush();
	}

	public void write(byte[] b, int off, int len) throws IOException {
		if(closed) throw new IOException("This OutputStream has already been closed.");
		written+=len;
		out.write(b, off, len);
	}

	public void write(byte[] b) throws IOException {
		write(b,0,b.length);
	}

	public void write(int b) throws IOException {
		if(closed) throw new IOException("This OutputStream has already been closed.");
		written++;
		out.write(b);
	}
}
