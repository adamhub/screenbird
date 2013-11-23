/*
 * CaptureBoxController.java
 * 
 * Version 1.0
 * 
 * 15 May 2013
 */
package com.bixly.pastevid.screencap.components.capturebox;

/**
 * Interface for the CaptureBox.
 * @author cevaris
 */
public interface CaptureBoxController {
    
    /**
     * Returns viewing state of Fullscreen or Customscreen.
     * @return Capturebox state
     */
    public CaptureBoxState getState();
    
    /**
     * Sets viewing sate of Capturebox.
     * @param state 
     */
    public void setState(CaptureBoxState state);
    
    /**
     * Initiates shutdown of capturebox.
     */
    public void destroy();
    
    /**
     * Returns true if capturebox is visible to the user.
     * @return 
     */
    public boolean isVisible();
    
    /**
     * Controls the viewable state of the dragbox.
     * @param isVisible 
     */
    public void setDragBoxVisible(boolean isVisible);
    
    /**
     * Controls the overall viewable state of CaptureBox.
     * @param isVisible Overriding visible state of capturebox
     * @param save Save CaptureBox state to file
     * @param showDragBox Sets viewable state of DragBox
     */
    public void setCaptureboxVisible(boolean isVisible, boolean save, Boolean showDragBox);
    
    /**
     * Locks the CaptureBox from being resized.
     * @param isLocked 
     */
    public void setLockCapturebox(boolean isLocked);
    
    /**
     * Begin flashing of capturebox borders.
     */
    public void beginBorderFlash();
    
    /**
     * End flashing of capturebox borders.
     */
    public void endBorderFlash();
    
    /**
     * Set capturebox on top of other windows.
     */
    public void setOnTop();
}
