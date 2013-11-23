/*
 * ScreenshotFile
 * 
 * Version 1.0
 * 
 * 4 May 2013
 */
package com.bixly.pastevid.editors;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;

/**
 * Describes a single screenshot file for a frame of the screen recording.
 * @author cevaris
 */
public class ScreenshotFile extends File implements Serializable {
    
    private static final long serialVersionUID = 5652656941291987L;
    
    /**
     * Second of the video where the screenshot file belongs to.
     */
    private int seconds;
    
    /**
     * Precise time in milliseconds in the video where the screenshot file belongs to.
     */
    private long timeMS;
    
    /**
     * Flag if the screenshot file is cached.
     */
    private boolean cached = false;
    
    /**
     * Image for the screenshot file.
     */
    private BufferedImage image; 
    
    /**
     * Flagged if screenshot is not to be displayed.
     */
    boolean isFlagged = false;
    
    public ScreenshotFile(String absolutePath, int seconds, long milliSeconds) {
        super(absolutePath);
        synchronized(this) {
            this.seconds = seconds;
            this.timeMS  = milliSeconds;
        }
    }
    
    /**
     * Sets the image for the screenshot file.
     * @param image 
     */
    public synchronized void setImage(BufferedImage image) {
        this.image = image;        
    }
    
    /**
     * Return screenshot image.
     * @return BufferedImage
     */
    public synchronized BufferedImage getImage() {
        return image;        
    }

    /**
     * Flags screenshot to be hidden.
     */
    public synchronized void flag() {
        isFlagged = true;        
    }

    /**
     * Return if screen shot is flagged or not. Screen shot is flagged if the
     * screen shot has been removed/edited out of screen capture.
     */
    public synchronized boolean isFlagged() {
        return this.isFlagged;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ScreenshotFile) {
            return (o.hashCode() == this.hashCode());
        } else {
            return false;
        }
    }

    @Override
    public synchronized int hashCode() {
        int hash = 5;
        hash = 31 * hash + this.seconds;
        hash = 31 * hash + (int) (this.timeMS ^ (this.timeMS >>> 32));
        hash = 31 * hash + (this.cached ? 1 : 0);
        hash = 31 * hash + (this.isFlagged ? 1 : 0);
        return hash;
    }
}
