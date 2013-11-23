/*
 * @(#)Atom.java
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

import com.bixly.pastevid.util.LogUtil;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Vector;

import javax.media.jai.PerspectiveTransform;
import javax.swing.tree.TreeNode;

import com.bric.io.GuardedOutputStream;
import com.bric.io.NullOutputStream;

/** This is not a public class because I expect to make some significant
 * changes to this project in the next year.
 * <P>Use at your own risk.  This class (and its package) may change in future releases.
 * <P>Not that I'm promising there will be future releases.  There may not be.  :)
 */
abstract class Atom implements TreeNode {
	/** This limits the output toString() will produce.
	 * This is true by default.  Very useful for debugging so your
	 * console isn't flooded with information.
	 */
	protected static boolean ABBREVIATE = true;

	protected static Enumeration EMPTY_ENUMERATION = new Enumeration() {
		public boolean hasMoreElements() {
			return false;
		}

		public Object nextElement() {
			return null;
		}
	};
	
	protected static void read(InputStream in,byte[] array) throws IOException {
		read(in,array,0,array.length);
	}
	
	protected static String getFieldName(Class c,int i) {
		Vector answers = new Vector();
		Field[] f = c.getFields();
		for(int a = 0; a<f.length; a++) {
			if( ((f[a].getModifiers() & Modifier.STATIC) > 0) &&
					(f[a].getType()==Integer.TYPE || f[a].getType()==Integer.class) ) {
				try {
					int k = ((Number)f[a].get(null)).intValue();
					if(k==i)
						answers.add(f[a].getName());
				} catch(Exception e) {}
			}
		}
		if(answers.size()==0)
			return "unknown";
		if(answers.size()==1)
			return (String)answers.get(0);
	
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		for(int a = 0; a<answers.size(); a++) {
			if(a!=0) {
				sb.append(", ");
			}
			sb.append("\""+answers.get(a)+"\"");
		}
		sb.append("]");
		return sb.toString();
	}
	
	protected static void read(InputStream in,byte[] array,int offset,int length) throws IOException {
		if(length==0) return;
		
		int totalRead = in.read(array,offset,length);
		int lastRead = totalRead;
		if(totalRead==-1) totalRead = 0;
		while(totalRead<length && lastRead!=-1) {
			lastRead = in.read(array,totalRead+offset,length-totalRead);
			if(lastRead!=-1)
				totalRead += lastRead;
		}
		if(lastRead==-1) {
			log("read "+totalRead+" bytes, although "+length+" were requested");
			throw new EOFException();
		}
	}

	static byte[] array32 = new byte[32];
	static byte[] array36 = new byte[36];
	static double[][] matrix = new double[3][3];
	protected synchronized static final PerspectiveTransform readMatrix(InputStream in) throws IOException {
		matrix[0][0] = read16_16Float(in);
		matrix[0][1] = read16_16Float(in);
		matrix[0][2] = read2_30Float(in);
		matrix[1][0] = read16_16Float(in);
		matrix[1][1] = read16_16Float(in);
		matrix[1][2] = read2_30Float(in);
		matrix[2][0] = read16_16Float(in);
		matrix[2][1] = read16_16Float(in);
		matrix[2][2] = read2_30Float(in);
		
		return new PerspectiveTransform(matrix);
	}
	
	protected synchronized static final void writeMatrix(OutputStream out,PerspectiveTransform transform) throws IOException {
		transform.getMatrix(matrix);
		write16_16Float(out,(float)matrix[0][0]);
		write16_16Float(out,(float)matrix[0][1]);
		write2_30Float(out,(float)matrix[0][2]);
		write16_16Float(out,(float)matrix[1][0]);
		write16_16Float(out,(float)matrix[1][1]);
		write2_30Float(out,(float)matrix[1][2]);
		write16_16Float(out,(float)matrix[2][0]);
		write16_16Float(out,(float)matrix[2][1]);
		write2_30Float(out,(float)matrix[2][2]);
	}

	protected static void skip(InputStream in,long skip) throws IOException {
		long totalSkipped = in.skip(skip);
		long lastSkipped = totalSkipped;
		while(totalSkipped<skip && lastSkipped!=-1) {
			lastSkipped = in.skip(skip-totalSkipped);
			if(lastSkipped!=-1)
				totalSkipped += lastSkipped;
		}
		if(lastSkipped==-1)
			throw new EOFException();
	}
	
	static byte[] array2 = new byte[2];
	static byte[] array3 = new byte[3];
	static byte[] array4 = new byte[4];
	static byte[] array6 = new byte[6];
	protected synchronized static final int read16Int(InputStream in) throws IOException {
		read(in,array2);
        return ((array2[0] & 0xff) << 8) + (array2[1] & 0xff);
	}
	
	protected synchronized static final void write16Int(OutputStream out,long i) throws IOException {
		array2[0] = (byte)((i >> 8) & 0xff);
		array2[1] = (byte)(i & 0xff);
		out.write(array2);
	}
	
	protected synchronized static final void write48Int(OutputStream out,long i) throws IOException {
		array6[0] = (byte)((i >> 40) & 0xff);
		array6[1] = (byte)((i >> 32) & 0xff);		
		array6[2] = (byte)((i >> 24) & 0xff);
		array6[3] = (byte)((i >> 16) & 0xff);
		array6[4] = (byte)((i >> 8) & 0xff);
		array6[5] = (byte)((i >> 0) & 0xff);
		out.write(array6);
	}
	
	protected synchronized static final void write24Int(OutputStream out,int i) throws IOException {
		array3[0] = (byte)((i >> 16) & 0xff);
		array3[1] = (byte)((i >> 8) & 0xff);
		array3[2] = (byte)(i & 0xff);
		out.write(array3);
	}
	
	protected synchronized static void write32Int(OutputStream out,long i) throws IOException {
		array4[0] = (byte)((i >> 24) & 0xff);
		array4[1] = (byte)((i >> 16) & 0xff);
		array4[2] = (byte)((i >> 8) & 0xff);
		array4[3] = (byte)(i & 0xff);
		out.write(array4);
	}
	
	protected synchronized static void write32String(OutputStream out,String s) throws IOException {
		array4[0] = (byte)(s.charAt(0));
		array4[1] = (byte)(s.charAt(1));
		array4[2] = (byte)(s.charAt(2));
		array4[3] = (byte)(s.charAt(3));
		out.write(array4);
	}
	
	protected synchronized static final int read24Int(InputStream in) throws IOException {
		read(in,array3);
		long k = (((long)(array3[0] & 0xff)) << 16) +
		(((long)(array3[1] & 0xff)) << 8) +
		(((long)(array3[2] & 0xff)));
		return (int)k;
	}
	protected synchronized static final long read32Int(InputStream in) throws IOException {
		read(in,array4);
		return create32Int(array4);
	}
	protected static final long create32Int(byte[] array) {
        long value = ( (array[0] & 0xff) << 24) + ( (array[1] & 0xff) << 16) + 
        ( (array[2] & 0xff) << 8) + (array[3] & 0xff);
        if(value > 0x80000000L) { //two's complement:
        	long t = -( (~value) & 0xffffffff );
        	value = t;
        }
        return value;
	}
	protected synchronized static final String read32String(InputStream in) throws IOException {
		read(in,array4);
		StringBuffer sb = new StringBuffer(4);
		sb.append( ((char)array4[0]) );
		sb.append( ((char)array4[1]) );
		sb.append( ((char)array4[2]) );
		sb.append( ((char)array4[3]) );
		return sb.toString();
	}
	
	
	protected synchronized static final String read32BytePascalString(InputStream in) throws IOException {
		read(in,array32);
		int size = array32[0] & 0xff;
		StringBuffer sb = new StringBuffer();
		for(int a = 0; a<size; a++) {
			sb.append( (char)array32[a+1] );
		}
		return sb.toString();
	}
	
	protected synchronized static final void write32BytePascalString(OutputStream out,String s) throws IOException {
		for(int a = 0; a<32; a++) {
			if(a==0) {
				array32[a] = (byte)s.length();
			} else if(a<=s.length()) {
				array32[a] = (byte)s.charAt(a-1);
			} else {
				array32[a] = 0;
			}
		}
		out.write(array32);
	}
	
	protected synchronized static final long read48Int(InputStream in) throws IOException {
		read(in,array6);
		return (((long)(array6[0] & 0xff)) << 40) +
		(((long)(array6[1] & 0xff)) << 32) +
		(((long)(array6[2] & 0xff)) << 24) +
		(((long)(array6[3] & 0xff)) << 16) +
		(((long)(array6[4] & 0xff)) << 8) +
		(((long)(array6[5] & 0xff)) << 0);
	}

	protected synchronized static final float read16_16Float(InputStream in) throws IOException {
		long value = read32Int(in);
		float multiplier = 1;
		if( (value & 0x80000000) > 0) {
			//we're in two's complement
			value = (~value) & 0xffffffff;
			value++;
			multiplier = -1;
		}
		long w = (value & 0xffff0000) >> 16;
		long f = (value & 0xffff);
		
		float floatValue = ((float)w)+((float)f)/65536f;

		return floatValue*multiplier;
	}

	protected synchronized static final float read16_16UnsignedFloat(InputStream in) throws IOException {
		int integerPart = read16Int(in);
		int fractionPart = read16Int(in);
		
		float floatValue = ((float)integerPart)+((float)fractionPart)/65536f;
		return floatValue;
	}
	
	protected synchronized static final float read2_30Float(InputStream in) throws IOException {
		long value = read32Int(in);
		long w = (value >> 30) & 0xff;
		float multiplier = 1;
		//so really, w can only be 4 values:
		//00, 01, 10, 11
		//only have to worry about 2's complement cases:
		if(w==0x10) {
			w = -2;
			value = ~value;
		} else if(w==0x11) {
			w = -1;
			value = ~value;
		}
		long f = value & 0x3FFF;
		
		float floatValue = ((float)w)+((float)f)/16384f;
		
		return floatValue*multiplier;
	}
		
	protected synchronized static final float read8_8Float(InputStream in) throws IOException {
		long value = read16Int(in);
		float multiplier = 1;
		if( (value & 0x8000) > 0) {
			//we're in two's complement
			value = (~value) & 0xffff;
			value++;
			multiplier = -1;
		}
		long w = (value & 0xff00) >> 8;
		long f = (value & 0xff);
		
		float floatValue = ((float)w)+((float)f)/256f;

		return floatValue*multiplier;
	}
	protected synchronized static final void write16_16Float(OutputStream out,float f) throws IOException {
		float v = (f>=0) ? f : -f;
	
		long wholePart = (long)v;
		long fractionPart = (long)((v-wholePart)*65536); 
		long t = (wholePart << 16) + fractionPart;
		
		if(f<0) {
			t = t-1;
		}
		write32Int(out,t);
	}

	protected synchronized static final void write16_16UnsignedFloat(OutputStream out,float f) throws IOException {
		if(f<0) throw new IllegalArgumentException(f+"<0");
		long wholePart = (long)f;
		long fractionPart = (long)((f-wholePart)*65536); 
		long t = (wholePart << 16) + fractionPart;
		write32Int(out,t);
	}
	
	protected synchronized static final void write8_8Float(OutputStream out,float f) throws IOException {
		float v = (f>=0) ? f : -f;
	
		long wholePart = (long)v;
		long fractionPart = (long)((v-wholePart)*256); 
		long t = (wholePart << 8) + fractionPart;
		
		if(f<0) {
			t = t-1;
		}
		write16Int(out,t);
	}

	protected synchronized static final void write2_30Float(OutputStream out,float f) throws IOException {
		float v = (f>=0) ? f : -f;
	
		long wholePart = (long)v;
		long fractionPart = (long)((v-wholePart)*1073741824); 
		
		long t = (wholePart << 30)+fractionPart;
		if(f<0) {
			t = t-1;
		}
		write32Int(out,t);
	}
	
	protected static final long QT_TO_JAVA_MS_CHANGE = (new GregorianCalendar(1970,GregorianCalendar.JANUARY,1)).getTimeInMillis()-(new GregorianCalendar(1904,GregorianCalendar.JANUARY,1)).getTimeInMillis();
	protected static final Date readDate(InputStream in) throws IOException {
		return new Date(read32Int(in)*1000+QT_TO_JAVA_MS_CHANGE);
	}
	protected static final void writeDate(OutputStream out,Date d) throws IOException {
		long millis = d.getTime();
		long qtMillis = millis-QT_TO_JAVA_MS_CHANGE;
		long qtSeconds = qtMillis/1000;
		write32Int(out,qtSeconds);
	}
	
	/** Returns the complete size of this atom, including the 8 byte header that is not
	 * written in the <code>writeContents</code> method.
	 * <P>Subclasses are encouraged to override this method if it is easy to calculate
	 * the number of bytes that will be written.  By default this method uses a
	 * <code>NullOutputStream</code> to actually write the data and measure its length.
	 * @return the complete size of this atom.
	 */
	protected long getSize() {
		GuardedOutputStream out = new GuardedOutputStream(new NullOutputStream(),Long.MAX_VALUE);
		try {
			writeContents(out);
		} catch(IOException e) {
			//very unlikely in a NullOutputStream!
			RuntimeException e2 = new RuntimeException();
			e2.initCause(e);
			throw e2;
		}
		return out.getBytesWritten()+8;
	}
	
	protected Atom parent;
	
	protected Atom(Atom parent) {
		this.parent = parent;
	}
	
	public TreeNode getParent() {
		return parent;
	}
	
	/** Returns the 4-byte identifier this atom uses.  These identifiers are often
	 * defined in the QT file format.
	 * <P>(Although this uses a <code>java.lang.String</code>, serious badness
	 * will follow if this value is not exactly 4-bytes long.
	 * 
	 * @return the 4-byte identifier this atom uses (such as "moov", "trak", etc.)
	 */
	protected abstract String getIdentifier();
	
	/** Writes the contents of this atom, minus the first 8 bytes.  This is called
	 * from the <code>write()</code> method.
	 * <P>(The first 8 bytes are always the size of this atom, and its identifier.
	 * Those are already written when this is called.)
	 * @param out a <code>GuardedOutputStream</code> that is restricted to write only
	 * a fixed number of bytes.
	 * @throws IOException
	 */
	protected abstract void writeContents(GuardedOutputStream out) throws IOException;
	
	/** Write this atom to an OutputStream.
	 */
	public final void write(OutputStream out) throws IOException {
		if(this.getClass().equals(EmptyAtom.class)) {
			write32Int(out,0);
			return;
		}
		long size = getSize();
		write32Int(out,size);
		String id = getIdentifier();
		write32String(out,id);
		
		GuardedOutputStream out2 = new GuardedOutputStream(out,size-8);
		writeContents(out2);
		if(out2.getLimit()!=0) {
			log(this);
			throw new IOException("This atom is "+out2.getLimit()+" byte(s) too small.");
		}
	}
	
	/** Returns the root of this tree node. */
	public Atom getRoot() {
		if(parent==null) return this;
		return parent.getRoot();
	}
	
	/** Returns the first child of the class provided. */
	public Atom getChild(Class t) {
		for(int a = 0; a<getChildCount(); a++) {
			Atom atom = (Atom)getChildAt(a);
			if(t.isInstance(atom))
				return atom;
		}
		return null;
	}
	
	/** Call this on the root of the dom tree to identify all
	 * <code>TrackHeaderAtoms</code> and determine the highest
	 * track ID used in this movie
	 * 
	 * @return the highest track ID contained in this node
	 */
	public long getHighestTrackID() {
		long maxID = 1;
		if(this instanceof TrackHeaderAtom) {
			TrackHeaderAtom t = (TrackHeaderAtom)this;
			if(t.trackID>maxID)
				maxID = t.trackID;
		}
		for(int a = 0; a<getChildCount(); a++) {
			Atom atom = (Atom)getChildAt(a);
			long k = atom.getHighestTrackID();
			if(k>maxID) maxID = k;
		}
		return maxID;
	}

    public static void log(Object message) {
        LogUtil.log(Atom.class, message);
    }
        
        
}
