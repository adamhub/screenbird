/*
 * @(#)SoundMediaInformationHeaderAtom.java
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

public class SoundMediaInformationHeaderAtom extends LeafAtom {

	/** A 1-byte specification of the version of this sound media information header atom. */
	int version = 0;
	/** A 3-byte space for sound media information flags. Set this field to 0. */
	int flags = 0;
	/** A 16-bit integer that specifies the sound balance of this sound media. Sound balance is the setting 
	 * that controls the mix of sound between the two speakers of a computer. This field is normally set to 0.
	 */
	int balance = 0;
	
	public SoundMediaInformationHeaderAtom() {
		super(null);
	}
	
	protected SoundMediaInformationHeaderAtom(Atom parent,InputStream in) throws IOException {
		super(parent);
		version = in.read();
		flags = Atom.read24Int(in);
		balance = Atom.read16Int(in);
		Atom.read16Int(in); //reserved unused space
	}

	protected String getIdentifier() {
		return "smhd";
	}
	
	protected long getSize() {
		return 16;
	}
	
	public String toString() {
		return "SoundMediaInformationHeaderAtom[ version = "+version+", flags = "+flags+", balance = "+balance+"]";
	}

	protected void writeContents(GuardedOutputStream out) throws IOException {
		out.write(version);
		Atom.write24Int(out, flags);
		Atom.write16Int(out, balance);
		Atom.write16Int(out, 0);
	}
	
}
