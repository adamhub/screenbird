/*
 * FFMpegTask.java
 *
 * Version 1.0
 * 
 * 17 May 2013
 */
package com.bixly.pastevid.screencap.components.progressbar;


/**
 *
 * @author cevaris
 */
public class FFMpegTask {
    
    /**
     * Total needed time for the process to finish
     */
    private int duration;
    
    /**
     * Amount of time that has passed in seconds
     */
    private int seconds;
    
    
    public FFMpegTask(){
        this.duration = 0;
        this.seconds = 0;
    }
    
    public FFMpegTask(int duration, int seconds) {
        this.duration = duration;
        this.seconds = seconds;
    }

    /**
     * Returns percent value of current progress
     * @return 
     */
    public int getProgress() {
        return (int)((double)(this.seconds*100)/(double)this.duration);
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public int getDuration() {
        return duration;
    }
    
}
