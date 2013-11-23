/**
 * ScreenRecorderController.java
 * 
 * Version 1.0
 * 
 * 24 May 2013
 */
package com.bixly.pastevid.screencap;

/**
 * Defines the relationship between ScreenRecorder JFrame and the main 
 * RecorderPanel JPanel.
 * @author cevaris
 */
public interface ScreenRecorderController {
    
    /**
     * Brings recrorder panel to view, if minimized or not visible.
     */
    public void bringToFocus();
    
    /**
     * Initiates the recorder panel.
     */
    public void initRecorder(boolean show, boolean recoveryMode);
    
    /**
     * Initiates system shutdown a.k.a. self destruct.
     */
    public void destroy();
    
    // Limiting Controler to a subset of JFrame methods
    public void controlSetLocation(int x, int y);
    public void controlSetVisible(boolean value);
    public void controlSetAlwaysOnTop(boolean value);
    public void controlPack();
}
