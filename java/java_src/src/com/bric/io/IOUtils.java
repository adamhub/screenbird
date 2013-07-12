/*
 * @(#)IOUtils.java
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

import com.bixly.pastevid.util.LogUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** A collection of static methods relating to Files and IO operations.
 * <P>These methods should have very few class dependencies; if you're
 * considering adding a method that will require several other classes/packages,
 * please put it in a different location.
 *
 */
public class IOUtils {

	/** Delete this file and all its children, if any.
	 * @return true if the delete was successful.
	 */
	public static boolean delete(File file) {
		boolean success = true;
		if(file.isDirectory()) {
			File[] children = file.listFiles();
			for(int a = 0; a<children.length; a++) {
				if(delete(children[a])==false)
					success = false;
			}
		}
		if(file.delete()==false)
			success = false;
		return success;
	}
	
	/** Return true if this file is a zip file. */
	public static boolean isZip(File file) throws IOException {
		if(file.exists()==false)
			return false;
		
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			ZipInputStream zipIn = new ZipInputStream(in);
			ZipEntry e = zipIn.getNextEntry();
			if(e==null) return false;
			int ctr = 0;
			while(e!=null && ctr<4) {
				e = zipIn.getNextEntry();
				ctr++;
			}
			return true;
		} catch(Throwable t) {
			return false;
		} finally { 
			try {
				in.close();
			} catch(Throwable t) {}
		}
	}
	
	/** Returns an InputStream that will read a specific entry
	 * from a zip file.
	 * @param file the zip file.
	 * @param entryName the name of the entry.
	 * @return an InputStream that reads that entry.
	 * @throws IOException
	 */
	public static InputStream getZipEntry(File file,String entryName) throws IOException {
		FileInputStream in = new FileInputStream(file);
		ZipInputStream zipIn = new ZipInputStream(in);
		ZipEntry e = zipIn.getNextEntry();
		while(e!=null) {
			if(e.getName().equals(entryName))
				return zipIn;
			e = zipIn.getNextEntry();
		}
		return null;
	}
	
	/** Returns true if these zip files act like equivalent sets.
	 * The order of the zip entries is not important: if they contain
	 * exactly the same contents, this returns true.
	 * @param zip1 one zip file 
	 * @param zip2 another zip file
	 * @return true if the two zip archives are equivalent sets
	 * @throws IOException
	 */
	public static boolean zipEquals(File zip1,File zip2) throws IOException {
		if(zip1.equals(zip2))
			return true;
		
		InputStream in = null;
		ZipInputStream zipIn = null;
		try {
			in = new FileInputStream(zip1);
			zipIn = new ZipInputStream(in);
			ZipEntry e = zipIn.getNextEntry();
			Vector entries = new Vector();
			while(e!=null) {
				entries.add(e.getName());
				
				InputStream other = getZipEntry(zip2,e.getName());
				if(other==null) {
					return false;
				}
				
				if(equals(zipIn,other)==false) {
					return false;
				}
				e = zipIn.getNextEntry();
			}
			//now we've established everything in zip1 is in zip2
			
			//but what if zip2 has entries zip1 doesn't?
			zipIn.close();
			in.close();
			
			in = new FileInputStream(zip2);
			zipIn = new ZipInputStream(in);
			e = zipIn.getNextEntry();
			while(e!=null) {
				if(entries.contains(e.getName())==false) {
					return false;
				}
				e = zipIn.getNextEntry();
			}
			
			//the entries are exactly the same
			return true;
		} finally {
			try {
				zipIn.close();
			} catch(Throwable t) {}
			try {
				in.close();
			} catch(Throwable t) {}
		}
	}
	static private byte[] b1;
	static private byte[] b2;
	
	public synchronized static boolean equals(InputStream in1,InputStream in2) throws IOException {
		if(b1==null)
			b1 = new byte[4096];
		if(b2==null)
			b2 = new byte[4096];
		
		int k1 = read(in1,b1);
		int k2 = read(in2,b2);
		while(k1>0 && k2>0) {
			if(k1!=k2) {
				return false;
			}
			if(equals(b1,b2,k1)==false) {
				return false;
			}
			k1 = read(in1,b1);
			k2 = read(in2,b2);
		}
		return true;
	}

    public static boolean equals(byte[] a, byte[] a2,int length) {
        if (a==a2)
            return true;
        if (a==null || a2==null)
            return false;

        if(length>a.length)
        	throw new IllegalArgumentException();
        if(length>a2.length)
        	throw new IllegalArgumentException();

        for (int i=0; i<length; i++)
            if (a[i] != a2[i])
                return false;

        return true;
    }
	
	/** Return true if two files are exactly equal.
	 * This will call <code>zipEquals()</code> if both files
	 * are zip files.
	 */
	public static boolean equals(File file1,File file2) throws IOException {
		if(isZip(file1) && isZip(file2)) {
			return zipEquals(file1,file2);
		}
		
		if(file1.length()!=file2.length())
			return false;
		
		InputStream in1 = null;
		InputStream in2 = null;
		try {
			in1 = new FileInputStream(file1);
			in2 = new FileInputStream(file2);
			return equals(in1,in2);
		} finally {
			try {
				if(in1!=null)
					in1.close();
			} catch(IOException e) {}
			try {
				if(in2!=null)
					in2.close();
			} catch(IOException e) {}
		}
	}
	
	/** Read data into the destination array.
	 * 
	 * @param in the InputStream to read.
	 * @param dest the destination to write to
	 * @return the number of bytes read (note this will be less than dest.length when the end of the stream is reached).
	 * @throws IOException
	 */
	public static int read(InputStream in,byte[] dest) throws IOException {
		int length = dest.length;
		int read = 0;
		int k = in.read(dest,read,length-read);
		while(k!=-1 && read<dest.length) {
			read += k;
			k = in.read(dest,read,dest.length-read);
		}
		if(k!=-1) {
			read += k;
		}
		return read;
	}
	
	/** Empty this directory.
	 * @return true if the delete was successful.
	 */
	public static boolean empty(File file) {
		if(file.isDirectory()==false) {
			return false;
		}
		boolean success = true;
		File[] children = file.listFiles();
		for(int a = 0; a<children.length; a++) {
			if(delete(children[a])==false)
				success = false;
		}
		return success;
	}

	
	/** Writes a copy of a file.
	 * 
	 * @param src the file to copy
	 * @param dst the location to write the new file
	 * @throws IOException
	 */
	public synchronized static void copy(File src,File dst) throws IOException {
		if(b1==null)
			b1 = new byte[4096];
		
		FileInputStream in = null;
		FileOutputStream out = null;
		try {
			 in = new FileInputStream(src);
			 dst.getParentFile().mkdirs();
			 dst.createNewFile();
			 out = new FileOutputStream(dst);
			 int k = in.read(b1);
			 while(k!=-1) {
				 out.write(b1,0,k);
				 k = in.read(b1);
			 }
		} finally {
			try {
				in.close();
			} catch(Throwable t) {
				log(t);
			}
			try {
				out.close();
			} catch(Throwable t) {
				log(t);
			}
		}
	}
	
	/** Write the text provided to a File.
	 * 
	 * @param file the file to write to.
	 * @param text the text to write.
	 * @throws IOException
	 */
	public static void write(File file,String text) throws IOException {
		FileOutputStream out = null;
		try {
			file.getParentFile().mkdirs();
			file.delete();
			file.createNewFile();
			out = new FileOutputStream(file);
			OutputStreamWriter writer = new OutputStreamWriter(out);
			writer.write(text);
			writer.flush();
		} finally {
			try {
				out.close();
			} catch(Throwable t) {
				log(t);
			}
		}
	}
	
	public static String read(File file) throws IOException {
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			return read(in);
		} finally {
			try {
				if(in!=null)
					in.close();
			} catch(Throwable t) {}
		}
	}
	
	public static String read(InputStream in) throws IOException {
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			StringBuffer sb = null;
			String s = br.readLine();
			while(s!=null) {
				if(sb==null) {
					sb = new StringBuffer();
					sb.append(s);
				} else {
					sb.append("\n");
					sb.append(s);
				}
				s = br.readLine();
			}
			return sb.toString();
	}
	
	/** Writes the entire InputStream into the destination file.
	* @param in the input stream to write
	* @param dest the file to write to.
	*/
	public static void write(InputStream in,File dest) throws IOException {
		OutputStream out = null;
		try {
			if(dest.exists()==false)
				dest.createNewFile();
			
			out = new FileOutputStream(dest);
			write(in,out);
		} finally {
			if(out!=null) {
				try {
					out.close();
				} catch(Throwable t) {}
			}
		}
	}
	
	/** Writes a <code>Properties</code> object to
	* a <code>File</code>.
	*/
	public static void write(Properties p,File file) throws IOException {
		OutputStream out = null;
		try {
			out = new FileOutputStream(file);
			p.store(out,"");
		} finally {
			if(out!=null) {
				try {
					out.close();
				} catch(Throwable t) {}
			}
		}
	}
	
	/** Loads properties into a file.
	* <P>This assumes the file was written with the
	* <code>Properties.store()</code> method.
	*/
	public static void load(Properties p,File file) throws IOException {
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			p.load(in);
		} finally {
			if(in!=null) {
				try {
					in.close();
				} catch(Throwable t) {}
			}
		}
	}
	
	/** Loads properties into a file.
	* <P>This assumes the file was written with the
	* <code>Properties.store()</code> method.
	*/
	public static void load(Properties p,URL url) throws IOException {
		InputStream in = null;
		try {
			in = url.openStream();
			p.load(in);
		} finally {
			if(in!=null) {
				try {
					in.close();
				} catch(Throwable t) {}
			}
		}
	}
	
	/** Writes a file to an OutputStream.
	* @param file the file to write.
	* @param out the stream to write to.
	*/
	public static void write(File file,OutputStream out) throws IOException {
		InputStream in = null;
		try {
			
			in = new FileInputStream(file);
			write(in,out);
		} finally {
			if(in!=null) {
				try {
					in.close();
				} catch(Throwable t) {}
			}
		}
	}
	
	/** Writes the InputStream into the OutputStream.
	 * This does not close anything.
	* @param in the data to read.
	* @param out the destination to write to.
	*/
	public synchronized static void write(InputStream in,OutputStream out) throws IOException {
		if(b1==null)
			b1 = new byte[4096];
		
		int t = in.read(b1);
		while(t!=-1) {
			out.write(b1,0,t);
			t = in.read(b1);
		}
	}

    private static void log(Object message) {
        LogUtil.log(IOUtils.class, message);
    }
        
        
}
