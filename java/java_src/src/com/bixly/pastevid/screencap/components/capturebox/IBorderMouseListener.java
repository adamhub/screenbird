/*
 * IBorderMouseListener.java
 *
 * Version 1.0
 * 
 * 16 May 2013
 */
package com.bixly.pastevid.screencap.components.capturebox;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/*
 * @author Bixly
 */
public abstract class IBorderMouseListener implements MouseListener {

    @Override
    public abstract void mouseReleased(MouseEvent me);

    @Override
    public void mouseClicked(MouseEvent me) {
    }

    @Override
    public void mouseEntered(MouseEvent me) {
    }

    @Override
    public void mouseExited(MouseEvent me) {
    }

    @Override
    public void mousePressed(MouseEvent me) {
    }
}
