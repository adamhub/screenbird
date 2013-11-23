/*
 * @(#)WindowLocationAtom.java
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
class WindowLocationAtom extends LeafAtom {
	int x, y;
	public WindowLocationAtom(Atom parent,InputStream in) throws IOException {
		super(parent);
		x = read16Int(in);
		y = read16Int(in);
	}
	
	protected String getIdentifier() {
		return "WLOC";
	}

	protected long getSize() {
		return 12;
	}

	protected void writeContents(GuardedOutputStream out) throws IOException {
		write16Int(out,x);
		write16Int(out,y);
	}
}
