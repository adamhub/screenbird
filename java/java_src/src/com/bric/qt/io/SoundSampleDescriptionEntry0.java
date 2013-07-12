/*
 * @(#)SoundSampleDescriptionEntry0.java
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

/** A sound sample description entry, version 0. */
public class SoundSampleDescriptionEntry0 extends SampleDescriptionEntry {
	/** A 16-bit integer that holds the sample description version (currently 0 or 1). */
	int version;
	
	/** A 16-bit integer that must be set to 0. */
	int revision;
	
	/** A 32-bit integer that must be set to 0. */
	long vendor;
	
	/** A 16-bit integer that indicates the number
	 * of sound channels used by the sound sample
	 * Set to 1 for monaural sounds, 2 for stereo
	 * sounds. Higher numbers of channels
	 * are not supported. 
	 */
	int channelCount;
	
	/** A 16-bit integer that specifies the number
	 * of bits in each uncompressed sound sample. Allowable 
	 * values are 8 or 16. Formats using more than 16 bits
	 * per sample set this field to 16 and use sound 
	 * description version1. */
	int sampleBitSize;
	
	/** A 16-bit integer that must be set to 0 for version
	 * 0 sound descriptions. This may be set to 2 for some 
	 * version 1 sound descriptions. */
	int compressionID;
	
	/** A 16-bit integer that must be set to 0. */
	int packetSize;
	
	/** A 32-bit unsigned fixed-point number (16.16) that 
	 * indicates the rate at which the sound samples were 
	 * obtained. The integer portion of this number should
	 * match the medias timescale. Many older version 0
	 * files have values of 22254.5454 or 11127.2727, but
	 * most files have integer values, such as 44100.
	 * Sample rates greater than 2^16 are not supported. 
	 */
	float sampleRate;
	
	public SoundSampleDescriptionEntry0(String type,int dataReference,
			int version, int revision,
			int vendor,int channelCount,
			int sampleBitSize, int compressionID,
			int packetSize, float sampleRate) {
		super(type, dataReference);
		this.version = version;
		this.revision = revision;
		this.vendor = vendor;
		this.channelCount = channelCount;
		this.sampleBitSize = sampleBitSize;
		this.compressionID = compressionID;
		this.packetSize = packetSize;
		this.sampleRate = sampleRate;
	}
	
	public SoundSampleDescriptionEntry0(String type,int dataReference,byte[] data) {
		super(type, dataReference);
		
		if(data.length!=20)
			throw new IllegalArgumentException();
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		try {
			version = Atom.read16Int(in);
			revision = Atom.read16Int(in);
			vendor = Atom.read32Int(in);
			channelCount = Atom.read16Int(in);
			sampleBitSize = Atom.read16Int(in);
			compressionID = Atom.read16Int(in);
			packetSize = Atom.read16Int(in);
			sampleRate = Atom.read16_16UnsignedFloat(in);
		} catch(IOException e) {
			//this should never happen
			RuntimeException e2 = new RuntimeException();
			e2.initCause(e);
			throw e2;
		}
	}
	
	public String toString() {
		return "SoundSampleDescriptionEntry0[ type = \""+type+"\", dataRef = "+dataReference+", version = "+version+", revision = "+revision+", vendor = "+vendor+", channelCount = "+channelCount+
		", sampleBitSize = "+sampleBitSize+", compressionID = "+compressionID+", packetSize = "+packetSize+", sampleRate = "+sampleRate+"]";
	}

	protected long getSize() {
		return 36;
	}

	protected void write(OutputStream out) throws IOException {
		Atom.write32Int(out, getSize());
		Atom.write32String(out, type);
		Atom.write48Int(out, 0);
		Atom.write16Int(out, dataReference);
		
		Atom.write16Int(out, version);
		Atom.write16Int(out, revision);
		Atom.write32Int(out, vendor);
		Atom.write16Int(out, channelCount);
		Atom.write16Int(out, sampleBitSize);
		Atom.write16Int(out, compressionID);
		Atom.write16Int(out, packetSize);
		Atom.write16_16UnsignedFloat(out, sampleRate);
	}
}
