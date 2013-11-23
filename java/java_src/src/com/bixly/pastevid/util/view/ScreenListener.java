/*
 * ScreenListener.java
 *
 * Version 1.0
 * 
 * 20 May 2013
 */
package com.bixly.pastevid.util.view;

import com.bixly.pastevid.common.Unimplemented;
import java.awt.AWTEvent;
import java.awt.Point;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 *
 * @author cevaris
 */
@Unimplemented
public class ScreenListener implements MouseMotionListener, AWTEventListener {

    public Point currPoint;

    public void mouseDragged(MouseEvent me) {
    }

    public void mouseMoved(MouseEvent me) {
        this.currPoint = me.getLocationOnScreen();
    }

    public synchronized Point getLocationOnScreen() {
        return this.currPoint;
    }

    public void eventDispatched(AWTEvent awte) {
    }
}
