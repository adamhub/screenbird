/*
 * ScreenUtil.java
 *
 * Version 1.0
 * 
 * 20 May 2013
 */
package com.bixly.pastevid.util;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

/**
 *
 * @author june
 */
public class ScreenUtil {

    
    /**
     * Returns a Dimension object for the width and height of the given graphics
     * device/screen.
     * @param device
     * @return 
     */
    public static Dimension getScreenDimension(GraphicsDevice device) {
        return new Dimension(device.getDisplayMode().getWidth(), device.getDisplayMode().getHeight()); 
    }
    
    /**
     * Checks for presence of multiple screens/monitors on the current computer.
     * @return True if more than one screen is detected.
     */
    public static boolean isDualScreen() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();

        return screens.length > 1;
    }
}
