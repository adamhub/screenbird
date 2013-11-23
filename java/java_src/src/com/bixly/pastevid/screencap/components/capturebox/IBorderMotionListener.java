/*
 * IBorderMotionListener.java
 *
 * Version 1.0
 * 
 * 16 May 2013
 */
package com.bixly.pastevid.screencap.components.capturebox;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/*
 * @author Bixly
 */
public abstract class IBorderMotionListener  implements MouseMotionListener {

    @Override
    public abstract void mouseDragged(MouseEvent e);

    @Override
    public void mouseMoved(MouseEvent e) { }
}
