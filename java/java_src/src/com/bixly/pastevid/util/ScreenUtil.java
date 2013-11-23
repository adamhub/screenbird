/*
 * ScreenUtil.java
 *
 * Version 1.0
 * 
 * 17 May 2013
 */
package com.bixly.pastevid.util;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Utility class for screen-related methods.
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
    public static boolean isMultipleScreen() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();

        return screens.length > 1;
    }
    
    /**
     * Changes the size of the given JFrame based on the supplied Dimension.
     * @param frame the JFrame whose Dimensions are to be changed
     * @param size the Dimension object to which the JFrame is to be resized
     */
    public static void changeSize(JFrame frame, Dimension size) {
        frame.setPreferredSize(size);
        frame.setMinimumSize(size);
        frame.setMaximumSize(size);
        frame.setSize(size);
        frame.repaint();
    }
    
    /**
     * Changes the size of the given JPanel based on the supplied Dimension.
     * @param frame the JPanel whose Dimensions are to be changed
     * @param size the Dimension object to which the JPanel is to be resized
     */
    public static void changeSize(JPanel frame, Dimension size) {
        frame.setPreferredSize(size);
        frame.setMinimumSize(size);
        frame.setMaximumSize(size);
        frame.setSize(size);
    }
}
