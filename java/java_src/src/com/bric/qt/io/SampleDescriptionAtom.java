/*
 * @(#)SampleDescriptionAtom.java
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

public class SampleDescriptionAtom extends LeafAtom {
	int version = 0;
	int flags = 0;
	SampleDescriptionEntry[] entries = new SampleDescriptionEntry[0];

	public SampleDescriptionAtom(int version, int flags) {
		super(null);
		this.version = version;
		this.flags = flags;
	}
	
	public SampleDescriptionAtom() {
		super(null);
	}
	
	public SampleDescriptionAtom(Atom parent,InputStream in) throws IOException {
		super(parent);
		version = in.read();
		flags = read24Int(in);
		int tableSize = (int)read32Int(in);
		entries = new SampleDescriptionEntry[tableSize];
		for(int a = 0; a<entries.length; a++) {
			entries[a] = readEntry(in);
		}
	}
	
	public void addEntry(SampleDescriptionEntry e) {
		SampleDescriptionEntry[] newArray = new SampleDescriptionEntry[entries.length+1];
		System.arraycopy(entries,0,newArray,0,entries.length);
		newArray[newArray.length-1] = e;
		entries = newArray;
	}
	
	protected SampleDescriptionEntry readEntry(InputStream in) throws IOException {
		UnknownSampleDescriptionEntry entry = new UnknownSampleDescriptionEntry(in);
		return entry.convert();
	}
		
	protected String getIdentifier() {
		return "stsd";
	}

	protected long getSize() {
		long sum = 16;
		for(int a = 0; a<entries.length; a++) {
			sum += entries[a].getSize();
		}
		return sum;
	}

	protected void writeContents(GuardedOutputStream out) throws IOException {
		out.write(version);
		write24Int(out,flags);
		write32Int(out,entries.length);
		for(int a = 0; a<entries.length; a++) {
			entries[a].write(out);
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("[ ");
		for(int a = 0; a<entries.length; a++) {
			if(a!=0) {
				sb.append(", ");
			}
			sb.append(entries[a].toString());
		}
		sb.append(" ]");
		String entriesString = sb.toString();
		
		return getClassName()+"[ version="+version+", "+
		"flags="+flags+", "+
		"entries="+entriesString+"]";
	}
	
	protected String getClassName() {
		String s = this.getClass().getName();
		if(s.indexOf('.')!=-1)
			s = s.substring(s.lastIndexOf('.')+1);
		return s;
	}
}
