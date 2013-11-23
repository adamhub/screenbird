/*
 * @(#)DataReferenceAtom.java
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

import com.bric.io.GuardedOutputStream;

public class DataReferenceAtom extends LeafAtom {
	int version = 0;
	int flags = 0;
	DataReferenceEntry[] entries = new DataReferenceEntry[0];

	public DataReferenceAtom(int version, int flags) {
		super(null);
		this.version = version;
		this.flags = flags;
	}
	
	public DataReferenceAtom() {
		super(null);
	}
	
	public void addEntry(String type,int version,int flags,byte[] data) {
		DataReferenceEntry e = new DataReferenceEntry(type,version,flags,data);
		DataReferenceEntry[] newArray = new DataReferenceEntry[entries.length+1];
		System.arraycopy(entries,0,newArray,0,entries.length);
		newArray[newArray.length-1] = e;
		entries = newArray;
	}
	
	public DataReferenceAtom(Atom parent,InputStream in) throws IOException {
		super(parent);
		
		version = in.read();
		flags = read24Int(in);
		int entryCount = (int)read32Int(in);
		entries = new DataReferenceEntry[entryCount];
		for(int a = 0; a<entries.length; a++) {
			entries[a] = new DataReferenceEntry(in);
		}
	}
	
	protected String getIdentifier() {
		return "dref";
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
		
		return "DataReferenceAtom[ version="+version+", "+
		"flags="+flags+", "+
		"entries="+entriesString+"]";
	}
}

class DataReferenceEntry {
	byte[] data;
	String type;
	int version;
	int flags;
	
	public DataReferenceEntry(String type,int version,int flags,byte[] data) {
		this.type = type;
		this.version = version;
		this.flags = flags;
		this.data = data;
	}
	
	public DataReferenceEntry(InputStream in) throws IOException {
		long size = Atom.read32Int(in);
		type = Atom.read32String(in);
		version = in.read();
		flags = Atom.read24Int(in);
		data = new byte[(int)size-12];
		Atom.read(in,data);
	}
	
	protected long getSize() {
		return 12+data.length;
	}
	
	protected void write(OutputStream out) throws IOException {
		Atom.write32Int(out,12+data.length);
		Atom.write32String(out,type);
		out.write(version);
		Atom.write24Int(out, flags);
		out.write(data);
	}
	
	public String toString() {
		return "DataReferenceEntry[ type=\""+type+"\", "+
		"version="+version+", "+
		"flags="+flags+", "+
		"data=\""+(new String(data))+"\"]";
	}
}
