/*
 * VideoCache.java
 * 
 * Version 1.0
 * 
 * 8 May 2013
 */
package com.bixly.pastevid.recorders;

import com.bixly.pastevid.editors.ScreenshotFile;
import com.bixly.pastevid.util.LogUtil;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.imageio.ImageIO;

/**
 * Used for caching the screen shots taken during recording.
 * @author cevaris
 */
public class VideoCache {
    
    /**
     * Total number of seconds of screenshots loaded into memory.
     */
    private int totalSeconds;
    
    /**
     * Hash map holding a bucket of screen shots for each second of recording
     */
    private HashMap<Integer, ArrayList<ScreenshotFile>> store;
    
    
    public VideoCache() {
        this.store = new HashMap<Integer, ArrayList<ScreenshotFile>>();
    }
    
    /**
     * Stores the recorded screen shots.
     * @param seconds
     * @param milliSeconds
     * @param screenshotFile 
     */
    public synchronized void addScreenshot(int seconds, long milliSeconds, File screenshotFile) {
        // Skip if file does not exist        
        if (!screenshotFile.exists()) {
            log(String.format("No such file %s exists", screenshotFile.getName()));
            return;
        }
        
        // Allocate new memory if needed
        if (this.store.get(seconds) == null) {
            this.store.put(seconds, new ArrayList<ScreenshotFile>());
        }
        
        // Checks for duplicate insertion
        for (ScreenshotFile screenshot : this.store.get(seconds)) {
            if (screenshot.getAbsoluteFile().equals(screenshotFile))
                // Skip adding since screenshot is already been inserted
                return;
        }
        
        // Add screenshot
        this.store.get(seconds).add(
                new ScreenshotFile(
                    screenshotFile.getAbsolutePath(),
                    seconds, 
                    milliSeconds
                )
        );
        
        // Update if seconds is greater than total time
        if (seconds > this.totalSeconds) {
            this.totalSeconds = seconds;
        }
    }
    
    /**
     * Returns an array list of screenshot files from a given time in seconds.
     * @param time the time in seconds and index in the cache store where the
     * screenshots will be retrieved.
     * @return 
     */
    public synchronized ArrayList<ScreenshotFile> getScreenshotFiles(int time) {
        if (this.store.get(time) != null) {
            // Return screenshots for this time if they exist
            return this.store.get(time);            
        } else {
            // Return an empty array list if no screenshots are found
            return new ArrayList<ScreenshotFile>();
        }
    }
    
    /**
     * Returns the last screenshot of the given second
     * @param time The time in seconds and index in the cache store where the
     * last screenshot will be retrieved.
     * @return 
     */
    public synchronized BufferedImage getScreenshotImage(int time) {
        if (this.store.get(time) == null || this.store.get(time).isEmpty()) {
            // No screenshot recorded for this second
            return null;
        }

        try {
            // Get last image because sometimes the first image
            // is still the previous image
            int index = this.store.get(time).size()-1;
            File imageFile = this.store.get(time).get(index);
            
            // Throw a NullPointerException if the image is in cache but the
            // actual file is missing
            if (!imageFile.exists()) {
                throw new NullPointerException("Could not locate file: " + imageFile.getAbsolutePath());
            }
            return ImageIO.read(imageFile);
        } catch (NullPointerException e){
            log(e);
        } catch (IOException e) {
            log(e);
        } catch (Exception e) {
            log(e);
        }
        return null;
    }
    
    /**
     * Returns an array list of screenshot images from a given time in seconds.
     * @param time the time in seconds and index in the cache store where the
     * last screenshot will be retrieved.
     * @return 
     */
    public synchronized ArrayList<BufferedImage> getScreenshotImages(int time) {
        ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();
        
        // Assert there are screenshots
        if (this.store.get(time) != null) {
            // Get screenshot files for the images
            ArrayList<ScreenshotFile> files  = this.store.get(time);
            
            // For each file that holds a screenshot
            for(ScreenshotFile file : files) {
                try {
                    // Skip any broken screen shots
                    if (!file.exists() || file.isFlagged()) {
                        continue;
                    }
                    
                    //Load file to memory
                    images.add(ImageIO.read(file));           
                } catch (NullPointerException e) {
                    log("Could not locate file: " + file.getAbsolutePath());
                } catch (IOException e) {
                    log(e);
                } 
            }
        }
        return images;
    }
    
    /**
     * For each screenshot in this second, flag so it does not show
     * @param time time which the screenshots should be flagged
     */
    public synchronized void flagScreenshotsAt(int time) {
        // Assert there are screenshots
        if (this.store.get(time) != null) {
            // Get screenshot files for the images
            ArrayList<ScreenshotFile> files  = this.store.get(time);
            
            // For each file that holds a screenshot
            for(ScreenshotFile file : files) {
                try {
                    // Skip any broken screen shots
                    if (!file.exists()) {
                        continue;
                    }
                    file.flag();
                } catch (NullPointerException e) {
                    log("Could not locate file: " + file.getAbsolutePath());
                }
            }
        }
    }
    
    public void log(Object message) {
        LogUtil.log(VideoCache.class, message);
    }
}
