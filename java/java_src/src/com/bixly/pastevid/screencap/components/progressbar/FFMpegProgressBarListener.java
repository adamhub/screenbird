/*
 * FFMpegProgressBarListener.java
 *
 * Version 1.0
 * 
 * 17 May 2013
 */
package com.bixly.pastevid.screencap.components.progressbar;

import com.bixly.pastevid.util.LogUtil;
import it.sauronsoftware.jave.Encoder;
import java.util.regex.Matcher;
import javax.swing.JProgressBar;

/**
 *
 * @author cevaris
 */
public class FFMpegProgressBarListener{
    // Program IDs
    public static final int FFMPEG    = 0;
    public static final int HANDBRAKE = 0;
    
    /**
     * Upload bar that displays the progress to the user
     */
    private JProgressBar pbUbpload;
    
    /**
     * Array of tasks that are being tracked by this listener
     */
    private FFMpegTask[] queue;
    
    /**
     * Current index of the task that is in queue
     */
    private int currentTask;
    
    /**
     * Total accumulated progress, typically 0->100
     */
    private double progress;
    
    /**
     * Number of processes/tasks in FFMpeg queue
     */
    private int segments = 1;
    
    private int programId;
    
    /**
     * Creates a new FFMpegProgressBarListener.
     * @param pbUpload JProgressBar that displays current progress 
     * @param numberOfTasks Integer that represent number of tasks being tracked
     */
    public FFMpegProgressBarListener(JProgressBar pbUpload, int numberOfTasks) {
        this.segments  = numberOfTasks;
        this.pbUbpload = pbUpload;
        this.progress  = 0;
        this.currentTask = 0;
        this.programId = FFMPEG;
        
        this.setTasks(numberOfTasks);
    }
    
    /**
     * Creates a new FFMpegProgressBarListener for the given programId
     * @param pbUpload JProgressBar that displays current progress 
     * @param numberOfTasks Integer that represent number of tasks being tracked
     * @param programId
     */
    public FFMpegProgressBarListener(JProgressBar pbUpload, int numberOfTasks, int programId) {
        this.segments  = numberOfTasks;
        this.pbUbpload = pbUpload;
        this.progress  = 0;
        this.currentTask = 0;
        this.programId = programId;
        
        this.setTasks(numberOfTasks);
    }
    
    /**
     * Sets the number of tasks being tracked.
     * @param numberOfTasks Integer that represent number of tasks being tracked
     */
    private void setTasks(int numberOfTasks) {
        this.queue = new FFMpegTask[numberOfTasks];
        for (int i = 0; i < numberOfTasks; i++){
            this.queue[i] = new FFMpegTask();
        }
        log("Initializing FFMpeg Progress Bar with " + numberOfTasks + " Tasks");
    }
    
    /**
     * Returns the current FFMPegTask that is currently being tracked
     * @return FFMpegTask
     */
    public FFMpegTask getWorkingTask() {
        return this.queue[this.currentTask];
    }
    
    /**
     * Called when after a task is completed in the queue. <BR>
     *  - Updates the progress bar to the correct position <BR>
     *  - Iterates to next FFMpeg task <BR>
     *  - Updates the progress bar component <BR>
     */
    public void taskComplete() {
        log("Task " + this.currentTask + " Complete\n\n\n");
        log("Current Task: " + this.currentTask + " # of Segments: "+ this.segments);
        
        // Increment up to the start of next segment
        this.progress = (int)((double)((this.currentTask+1)*100)/(double)this.segments);
        
        // Move to next task
        this.currentTask++;
        
        // Update progress bar
        this.updateProgressBar();
    }
    
    /**
     * Updated the progress by adding a delta to the current progress.
     * @param progressDelta The difference in percentage from the next progress
     * and the previous progress. The accumulated progress increases by progressDelta
     * divided by the number of tasks being ran.
     */
    public void setProgressByDelta(int progressDelta) {
        this.progress += (double)progressDelta/(double)this.segments;
        this.updateProgressBar();
    }
    
    /**
     * Sets the current progress with the given value.
     * @param progressValue the value of the current progress.
     */
    public void setProgress(double progressValue) {
        this.progress = (((double)currentTask/(double)this.segments) + (progressValue/(double)this.segments))*100;
        this.updateProgressBar();
    }
    
    /**
     * Updates the progress bar that is linked to this listener
     */
    public void updateProgressBar(){
        if (this.pbUbpload != null) {
            this.pbUbpload.setValue((int)this.progress);
        }
    }

    /**
     * Returns current accumulated progress of the progress bar
     * @return 
     */
    public int getProgress() {
        return (int)this.progress;
    }
    
    /**
     * Returns the current number of tasks being tracked.
     * @return 
     */
    public int getCurrentTaskNumber() {
        return this.currentTask;
    }
    
    public void setId(int programId) {
        this.programId = programId;
    }
    public int getId() {
        return this.programId;
    }

    /**
     * Resets the current progress and number of tasks being tracked.
     */
    public void reset() {
        this.progress = 0;
        this.currentTask = 0;
    }
    
    /**
     * Parses a line of data looking for the signals that mark the 
     * current progress of the ffmpeg exec call.
     * @param line String to be parsed for information
     * @param durationOverride Override the duration that which this process
     * is supposed to run. Actually a hack, but dont mind me...
     */
    public void parseTimeInfoHandbrake(String line) {
        Matcher matcher;
        double percentage = 0;

        matcher = Encoder.TIME_HANDBRAKE.matcher(line);
 
        // Parse output
        if (matcher.find()) {
            // Read in match
            // Tens place
            percentage += Integer.parseInt(matcher.group(1));
            // Decimal Place
            percentage += Integer.parseInt(matcher.group(2))/100.0;

            // Update progress
            setProgress(percentage/100.0);
            // Update task
        } 
    }
    
    public void log(Object message) {
        LogUtil.log(FFMpegProgressBarListener.class,message);
    }
}

