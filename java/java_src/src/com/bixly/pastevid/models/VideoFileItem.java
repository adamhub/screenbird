/*
 * VideoFileItem.java
 * 
 * Version 1.0
 * 
 * 7 May 2013
 */
package com.bixly.pastevid.models;

import java.io.Serializable;

/**
 * Model class for the video data of the recording saved to file.
 * @author Bixly
 */
public class VideoFileItem implements Serializable  {
    
    static final long serialVersionUID = 2903734673739792L;

    // Attributes for the video file item
    private String directory;
    private int start = 0;
    private int end = 0;
    private long startMS = 0;
    private long endMS = 0;

    /**
     * Returns the directory for this video file.
     * @return 
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * Sets the directory for this video file.
     * @param directory 
     */
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    /**
     * Returns the end time for this VideoFileItem in seconds.
     * @return 
     */
    public int getEnd() {
        return end;
    }

    /**
     * Sets the end time for this VideoFileItem in seconds.
     * @param end 
     */
    private void setEnd(int end) {
        this.end = end;
    }

    /**
     * Returns the start time for this VideoFileItem in seconds.
     * @return 
     */
    public int getStart() {
        return start;
    }

    /**
     * Sets the start time for this VideoFileItem in seconds.
     * @param start 
     */
    private void setStart(int start) {
        this.start = start;
    }

    /**
     * Returns the end time for this VideoFileItem in milliseconds.
     * @return 
     */
    public long getEndMS() {
        return endMS;
    }

    /**
     * Sets the end time for this VideoFileItem in milliseconds.
     * @param endMS 
     */
    public void setEndMS(long endMS) {
        this.endMS = endMS;
        this.setEnd((int)(endMS / 1000));
    }

    /** 
     * Returns the start time for this VideoFileItem in milliseconds.
     * @return 
     */
    public long getStartMS() {
        return startMS;
    }

    /**
     * Sets the start time for this VideoFileItem in milliseconds.
     * @param startMS 
     */
    public void setStartMS(long startMS) {
        this.startMS = startMS;
        this.setStart((int)(startMS / 1000));
    }
}