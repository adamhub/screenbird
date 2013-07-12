/*
 * AudioFileItem
 * 
 * Version 1.0
 * 
 * 7 May 2013
 */
package com.bixly.pastevid.models;

import java.io.Serializable;

/**
 * Model class for audio data of the recording saved to file.
 */
public class AudioFileItem implements Serializable {

    private String  name;
    private boolean dropped;
    private boolean previousDropped;
    private long timestamp;
    
    // Attributes used in audio compilation to keep the sync
    private long start;
    private long end;
    private long startMS;
    private long endMS;

    /**
     * Indicates if the line is dropped and causes the audio file to finish.
     * @return true if dropped
     */
    public boolean isDropped() {
        return dropped;
    }

    /**
     * Sets the dropped flag of the audio line.
     * @param dropped true if dropped
     */
    public void setDropped(boolean dropped) {
        this.dropped = dropped;
    }

    /**
     * Indicates if the line is dropped before this audio file.
     * @return 
     */
    public boolean isPreviousDropped() {
        return previousDropped;
    }

    /**
     * Flag the previous audio line as dropped.
     * @param hasPreviousDropped 
     */
    public void setPreviousDropped(boolean hasPreviousDropped) {
        this.previousDropped = hasPreviousDropped;
    }

    /**
     * Returns the name for this audio file.
     * @return 
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name for this audio file.
     * @param name 
     */
    public void setName(String name) {
        this.name = name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getStart() {
        return start;
    }

    private void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    private void setEnd(long end) {
        this.end = end;
    }

    public long getEndMS() {
        return endMS;
    }

    public void setEndMS(long endMS) {
        this.endMS = endMS;
        this.setEnd(endMS / 1000);
    }

    public long getStartMS() {
        return startMS;
    }

    public void setStartMS(long startMS) {
        this.startMS = startMS < 1000 ? 0 : startMS;
        this.setStart(startMS / 1000);
    }
    
    @Override
    public String toString() {
        return String.format("Start[%d] End[%d] TimeStamp[%d]", this.startMS, this.endMS, this.timestamp);
    }
}