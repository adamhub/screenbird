/*
 * @(#)JPEGMovieAnimation.java
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

import com.bixly.pastevid.Settings;
import com.bixly.pastevid.util.LogUtil;
import com.bixly.pastevid.util.MediaUtil;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.Vector;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import com.bric.io.MeasuredOutputStream;
import java.awt.Image;
import java.awt.Toolkit;
import javax.imageio.IIOException;
import javax.swing.ImageIcon;


/** This writes a QuickTime MOV file as a series of JPEG images.
 * <P>As expected, this does not offer excellent compression.  But it
 * is the simplest QuickTime codec to implement, and it doesn't raise
 * legal implications (such as patent royalties) that other codecs
 * might.
 * <P>This object actually writes to a movie file in 2 passes:
 * the first pass writes 99% of the movie.  When <code>close()</code> is
 * called, the movie structure is added and a <code>RandomAccessFile</code> is
 * used to modify the first 4 bytes of this movie.  (They have to be
 * adjusted to reflect the size of the file, which isn't know until it is
 * written.)
 * <P>But for the most part this streams its data directly to an
 * <code>OutputStream</code>.
 * 
 * @name JPEGMovieAnimation
 * @title Movies: Writing MOV Files Without QuickTime
 * @release June 2008
 * @blurb This article presents a class that can write a .mov file as a series of JPEG images.
 * <p>The compression is, well, jpeg-level -- which is very poor for modern animation codecs.  But this format is not subject to nasty patent/royalty issues.
 * @see <a href="http://javagraphics.blogspot.com/2008/06/movies-writing-mov-files-without.html">Movies: Writing MOV Files Without QuickTime</a>
 *
 */
public class JPEGMovieAnimation {

    Vector frames = new Vector();
    int w = -1, h = -1;
    OutputStream out;
    File dest;

    /** Constructs a new <code>JPEGMovieAnimation</code>.
     * <P>By constructing this object a <code>FileOutputStream</code>
     * is opened for the destination file.  It remains open until
     * <code>close()</code> is called or this object is finalized.
     * @param file the file data is written to.  It is strongly
     * recommended that this file name end with ".mov" (or ".MOV"), although
     * this is not required.
     * @throws IOException
     */
    public JPEGMovieAnimation(File file) throws IOException {
        dest = file;
        file.createNewFile();
        out = new FileOutputStream(file);
        Atom.write32Int(out, 0); //this has to be rewritten when we finish
        Atom.write32String(out, "mdat");
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        try {
            out.close();
        } catch (IOException e) {
            log(e);
        }
    }

    /** Adds an image to this animation.
     * <P>All images must be the same dimensions; if this image is
     * a different size from previously added images an exception is thrown.
     * 
     * @param duration the duration (in seconds) this frame should
     * show.  (This value is converted to a timescale of 600.)
     * @param bi the image to add as a frame.
     * @param jpegQuality a value from [0,1] indicating the quality
     * of this image.  A value of 1 represents a losslessly encoded image.
     * A value of 0... well... don't use 0.  I don't know what it looks like, but
     * it's probably very very bad.  Probably you shouldn't go lower than .5 or .7.
     * @throws IOException
     */
    public void addFrame(float duration, BufferedImage bi, float jpegQuality) throws IOException {
        if (w == -1 && h == -1) {
            w = bi.getWidth();
            h = bi.getHeight();
        } else {
            if (w != bi.getWidth() || h != bi.getHeight()) {
                throw new IllegalArgumentException("Each frame must have the same dimension.  This frame (" + bi.getWidth() + "x" + bi.getHeight() + ") is not the same dimensions as previous frames (" + w + "x" + h + ").");
            }
        }

        frames.add(new Frame(duration, writeJPEG(out, bi, jpegQuality)));
    }

    /** @return the bounds of the image provided.
     * @throws UnsupportedOperationException if the file cannot be read
     * as an image by ImageIO classes.
     * @throws IOException if an error occurred while reading the file
     */
    private static Dimension getJPEGBounds(File file) throws IOException, IIOException {
        FileInputStream in = null;
        try {
            
            //Hack for UNIX machines
            if(MediaUtil.osIsUnix()) ImageIO.setUseCache(false);
            
            in = new FileInputStream(file);
            ImageIO.setCacheDirectory(new File(Settings.SCREEN_CAPTURE_DIR));
            ImageInputStream stream = ImageIO.createImageInputStream(in);
            Iterator iter = ImageIO.getImageReaders(stream);
            ImageReader reader = (ImageReader) iter.next();
            String formatName = reader.getFormatName().toLowerCase();
            if (formatName.indexOf("jpeg") == -1 && formatName.indexOf("jpg") == -1) {
                throw new IllegalArgumentException("This image file is not a JPEG image.  This encoder only supports JPEG images.");
            }
            if (reader == null) {
                throw new UnsupportedOperationException("The file \"" + file.getName() + "\" is not a supported image.");
            }
            reader.setInput(stream, false);
            int w = reader.getWidth(0);
            int h = reader.getHeight(0);
            reader.dispose();
            stream.close();
            return new Dimension(w, h);
        } catch (IllegalArgumentException e){
            log(e);
        } catch (UnsupportedOperationException e){
            log(e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
                log(e);
            }
        }
        return null;
    }
    
    /** Writes a JPEG image to a given OutputStream
     * 
     * @param bi an image
     * @param quality the quality (between zero and one)
     * @return the amount of bytes written
     */
    protected static long writeJPEG(OutputStream out, BufferedImage bi, float quality) throws IOException {
        MeasuredOutputStream mOut = new MeasuredOutputStream(out);
        MemoryCacheImageOutputStream iOut = null;

        iOut = new MemoryCacheImageOutputStream(mOut);
        ImageWriter iw = (ImageWriter) ImageIO.getImageWritersByMIMEType("image/jpeg").next();
        ImageWriteParam iwParam = iw.getDefaultWriteParam();
        iwParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        iwParam.setCompressionQuality(quality);
        iw.setOutput(iOut);
        IIOImage img = new IIOImage(bi, null, null);
        iw.write(null, img, iwParam);
        return mOut.getBytesWritten();
    }

    /** Adds an image to this animation.
     * <P>All images must be the same dimensions; if this image is
     * a different size from previously added images an exception is thrown.
     * <P>Note this method is untested.  But I really think it has
     * a good chance of working.
     * 
     * @param duration the duration (in seconds) this frame should
     * show.  (This value is converted to a timescale of 600.)
     * @param image the JPEG to add.  (An exception is thrown if this is not
     * a valid JPEG file.)
     * @throws IOException
     */
    public void addFrame(float duration, File image) throws IOException {
        Dimension d = getJPEGBounds(image);

        if (w == -1 && h == -1) {
            w = d.width;
            h = d.height;
        } else {
            if (w != d.width || h != d.height) {
                throw new IllegalArgumentException("Each frame must have the same dimension.  This frame (" + d.width + "x" + d.height + ") is not the same dimensions as previous frames (" + w + "x" + h + ").");
            }
        }

        frames.add(new Frame(duration, write(out, image)));
    }

    /** This finishes writing the movie file.
     * <P>This is responsible for writing the structure of the
     * movie data, and finishing all IO operations to the file.
     * @throws IOException
     */
    public void close() throws IOException {
        try {
            long duration = 0;
            long dataSize = 0;
            for (int a = 0; a < frames.size(); a++) {
                Frame f = (Frame) frames.get(a);
                duration += f.duration;
                dataSize += f.fileLength;
            }


            /** Mind you: I don't actually know what I'm doing here.
             * Well, I know a little.  But not everything.  I have
             * some notion of what time scales and samples are,
             * but chunks and other details are all greek to me.  Here
             * I try to emulated the format of other movies I've parsed.
             */
            ParentAtom moovRoot = new ParentAtom("moov");
            MovieHeaderAtom movieHeader = new MovieHeaderAtom(600, duration);
            moovRoot.add(movieHeader);
            ParentAtom trakAtom = new ParentAtom("trak");
            moovRoot.add(trakAtom);
            TrackHeaderAtom trackHeader = new TrackHeaderAtom(1, duration, w, h);
            trackHeader.volume = 0;
            trakAtom.add(trackHeader);
            ParentAtom mdiaAtom = new ParentAtom("mdia");
            trakAtom.add(mdiaAtom);
            MediaHeaderAtom mediaHeader = new MediaHeaderAtom(600, duration);
            mdiaAtom.add(mediaHeader);
            HandlerReferenceAtom handlerRef1 = new HandlerReferenceAtom("mhlr", "vide", "java");
            mdiaAtom.add(handlerRef1);
            ParentAtom minf = new ParentAtom("minf");
            mdiaAtom.add(minf);
            VideoMediaInformationHeaderAtom vmhd = new VideoMediaInformationHeaderAtom();
            minf.add(vmhd);
            HandlerReferenceAtom handlerRef2 = new HandlerReferenceAtom("dhlr", "alis", "java");
            minf.add(handlerRef2);

            ParentAtom dinf = new ParentAtom("dinf");
            minf.add(dinf);
            DataReferenceAtom dref = new DataReferenceAtom();
            dref.addEntry("alis", 0, 1, new byte[]{});
            dinf.add(dref);

            ParentAtom stbl = new ParentAtom("stbl");
            minf.add(stbl);

            VideoSampleDescriptionAtom stsd = new VideoSampleDescriptionAtom();
            stsd.addEntry(VideoSampleDescriptionEntry.createJPEGDescription(w, h));
            stbl.add(stsd);

            /** These are the 4 atoms that really map the frames ("samples")
             * to the animation:
             */
            TimeToSampleAtom stts = new TimeToSampleAtom();
            SampleToChunkAtom stsc = new SampleToChunkAtom();
            SampleSizeAtom stsz = new SampleSizeAtom();
            ChunkOffsetAtom stco = new ChunkOffsetAtom();

            stbl.add(stts);
            stbl.add(stsc);
            stbl.add(stsz);
            stbl.add(stco);

            long CHUNK_MIN = (long) (.5 * 1024 * 1024);
            long baseFileOffset = 8;
            long totalSize = 0;
            int chunkIndex = 0;
            long chunkTime = 0;
            long chunkSize = 0;
            int samplesWritten = 0;
            for (int a = 0; a < frames.size(); a++) {
                Frame f = (Frame) frames.get(a);
                stts.addSampleTime(f.duration);
                stsz.addSampleSize(f.fileLength);
                if (a == 0 || (chunkTime + f.duration >= 600 && chunkSize > CHUNK_MIN)) {
                    chunkIndex++;
                    chunkTime = 0;
                    chunkSize = 0;
                    if (samplesWritten != 0) {
                        stsc.addChunk(chunkIndex - 1, samplesWritten, 1);
                    }
                    stco.addChunkOffset(baseFileOffset + totalSize);
                    samplesWritten = 0;
                }
                totalSize += f.fileLength;
                samplesWritten++;
                chunkTime += f.duration;
                chunkSize += f.fileLength;
            }
            if (samplesWritten != 0) {
                stsc.addChunk(chunkIndex, samplesWritten, 1);
            }

            moovRoot.write(out);
        } catch (IOException e) {
            log(e);
        } finally {
            out.close();
        }

        //very last step: we have to rewrite the first
        //4 bytes of this file now that we can conclusively say
        //how big the "mdat" atom is:

        long mdatSize = 8;
        for (int a = 0; a < frames.size(); a++) {
            Frame f = (Frame) frames.get(a);
            mdatSize += f.fileLength;
        }

        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(dest, "rw");
            raf.seek(0);
            byte[] array = new byte[4];
            array[0] = (byte) ((mdatSize >> 24) & 0xff);
            array[1] = (byte) ((mdatSize >> 16) & 0xff);
            array[2] = (byte) ((mdatSize >> 8) & 0xff);
            array[3] = (byte) (mdatSize & 0xff);
            raf.write(array);
        } catch (IOException e){
            log(e);
        } finally {
            raf.close();
        }
    }
    static byte[] block;

    protected static synchronized long write(OutputStream out, File file) throws IOException {
        if (block == null) {
            block = new byte[4096];
        }
        FileInputStream in = null;
        long written = 0;
        try {
            in = new FileInputStream(file);

            int k = in.read(block);
            while (k != -1) {
                written += k;
                out.write(block, 0, k);
                k = in.read(block);
            }
            return written;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
    }

    private static void log(Object e) {
        LogUtil.log(JPEGMovieAnimation.class,e);
    }
}
class Frame {

    /** All frames have an implicit time scale of 600. */
    int duration;
    long fileLength;

    public Frame(float duration, long fileLength) {
        if (duration < 0) {
            throw new IllegalArgumentException("duration (" + duration + ") must be greater than zero.");
        }
        if (fileLength < 0) {
            throw new IllegalArgumentException("file length (" + fileLength + ") must be greater than zero.");
        }
        this.duration = (int) (duration * 600 + .5);
        this.fileLength = fileLength;
    }
}