/*
 * InstanceManager.java
 * 
 * Version 1.0
 * 
 * 7 May 2013
 */
package com.bixly.pastevid.ipc;

import com.bixly.pastevid.Settings;
import com.bixly.pastevid.screencap.ScreenRecorderController;
import com.bixly.pastevid.util.LogUtil;
import com.bixly.pastevid.util.TimeUtil;
import java.io.IOException;

/**
 *
 * @author cevaris
 */
public class InstanceManager extends Thread {
    private ScreenRecorderController controller;
    private boolean isRunning = true;
    private final int POLL_TIME_SECONDS = 1;
    
    public InstanceManager(ScreenRecorderController controller) {
        super("Instance Manager");
        this.controller = controller;
    }

    @Override
    public void run() {
        // While first instance is running
        while (isRunning) {
            // Flag file is created by the second (opening) instance to 
            // notify the first instance that the second instance is 
            // attempting to be opened. 
            
            // If first instance is flagged 
            if (Settings.FLAG_FILE.exists()) {
                controller.bringToFocus();
                // Delete the flag
                Settings.FLAG_FILE.delete();
            }
            
            // If second instance deleted the lock file,
            // this if conditions will detect the deletion
            // and re-create the lock file. This stop
            // rouge undeleted lock files from preventing 
            // launch of screen recorder
            if (!Settings.LOCK_FILE.exists()) {
                try {
                    Settings.LOCK_FILE.createNewFile();
                    Settings.LOCK_FILE.deleteOnExit();
                } catch(IOException e) {
                    log(e);
                }
            }

            // Check for flag file every second
            TimeUtil.skipToMyLou(POLL_TIME_SECONDS);
        }
    }
    
    /**
     * Checks to see if there is an existing instance. If there is an existing
     * instance, flag the other instance using a file semaphore and exit. 
     */
    public void checkMultInstance() {
        try {
            if(Settings.LOCK_FILE.exists()) {
                // Second(+) instance or rouge Lock File
                
                // Temporarily unlock the system
                Settings.LOCK_FILE.deleteOnExit();
                // Flag down the first system
                Settings.FLAG_FILE.createNewFile();
                
                // Shutdown Screen Recorder
                this.controller.destroy();
                System.exit(0);
            } else {
                // First instance
                Settings.LOCK_FILE.createNewFile();
                Settings.LOCK_FILE.deleteOnExit();
                
                log("Successly marked openInstance: "+Settings.LOCK_FILE.exists()+" "+Settings.LOCK_FILE.getAbsolutePath());
                // Dispatch instance listener
                this.start();
            }
        } catch (IOException e) {
            log(e);
        }
    }

    public synchronized void close(){
        this.isRunning = false;
    }
    
    public synchronized boolean isRunning(){
        return this.isRunning;
    }

    private void log(Object message) {
        LogUtil.log(InstanceManager.class, message);
    }
}