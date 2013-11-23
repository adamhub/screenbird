/*
 * IMeasurable.java
 * 
 * Version 1.0
 * 
 * 8 May 2013
 */
package com.bixly.pastevid.recorders;

import java.awt.GraphicsDevice;

/**
 * Defines the relationship between recorder panel and the recorder class
 * @author Bixly
 */
public interface IMeasurable {

    /**
     * Returns the current clock time.
     */
    public long getValue();
    
    /**
     * Returns the current Display Screen.
     */
    public GraphicsDevice getScreen();
    
    public void relocate(int x, int y);
}
