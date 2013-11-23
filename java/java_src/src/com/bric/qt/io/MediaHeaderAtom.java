/*
 * @(#)MediaHeaderAtom.java
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
import java.util.Date;

import com.bric.io.GuardedOutputStream;

public class MediaHeaderAtom extends LeafAtom {
	int version = 0;
	int flags = 0;
	Date creationTime;
	Date modificationTime;
	long timeScale;
	long duration;
	int language = 0;
	int quality = 0;
	
	public MediaHeaderAtom(long timeScale,long duration) {
		super(null);
		creationTime = new Date();
		modificationTime = creationTime;
		this.timeScale = timeScale;
		this.duration = duration;
	}
	
	public MediaHeaderAtom(Atom parent,InputStream in) throws IOException {
		super(parent);
		version = in.read();
		flags = read24Int(in);
		creationTime = readDate(in);
		modificationTime = readDate(in);
		timeScale = read32Int(in);
		duration = read32Int(in);
		language = read16Int(in);
		quality = read16Int(in);
	}
	
	protected String getIdentifier() {
		return "mdhd";
	}

	protected long getSize() {
		return 32;
	}

	protected void writeContents(GuardedOutputStream out) throws IOException {
		out.write(version);
		write24Int(out,flags);
		writeDate(out,creationTime);
		writeDate(out,modificationTime);
		write32Int(out,timeScale);
		write32Int(out,duration);
		write16Int(out,language);
		write16Int(out,quality);
	}

	public String toString() {
		return "MediaHeaderAtom[ version="+version+", "+
		"flags="+flags+", "+
		"creationTime="+creationTime+", "+
		"modificationTime="+modificationTime+", "+
		"timeScale="+timeScale+", "+
		"duration="+duration+", "+
		"language="+language+", "+
		"quality="+quality+" ]";
	}
}
