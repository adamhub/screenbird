package com.bixly.pastevid.util;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

/**
 *
 * @author june
 */
public class ScreenUtil {

    public static Dimension getScreenDimension(GraphicsDevice device) {
        return new Dimension(device.getDisplayMode().getWidth(), device.getDisplayMode().getHeight()); 
    }
    
    public static boolean isDualScreen() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();

        return screens.length > 1;
    }
}
