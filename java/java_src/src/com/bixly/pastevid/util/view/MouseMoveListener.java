/*
 * MouseMoveListener.java
 *
 * Version 1.0
 * 
 * 20 May 2013
 */
package com.bixly.pastevid.util.view;

import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 *
 * @author Jorge
 */
public class MouseMoveListener implements MouseListener, MouseMotionListener {

    private JComponent target;
    private Point start_drag;
    private Point start_loc;
    
    private boolean isLocked = false;
    private boolean isSnapped = true;
    private Rectangle snapArea = null;

    public MouseMoveListener(JComponent target) {
        this.target = target;
    }

    public static JFrame getFrame(Container target) {
        if (target instanceof JFrame) {
            return (JFrame) target;
        }
        return getFrame(target.getParent());
    }

    Point getScreenLocation(MouseEvent e) {
        Point cursor = e.getPoint();
        Point target_location = this.target.getLocationOnScreen();
        return new Point((int) (target_location.getX() + cursor.getX()),
                (int) (target_location.getY() + cursor.getY()));
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        ((JFrame) target.getParent().getParent().getParent().getParent()).setState(JFrame.NORMAL);
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        this.start_drag = this.getScreenLocation(e);
        this.start_loc = MouseMoveListener.getFrame(this.target).getLocation();
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        Point current = this.getScreenLocation(e);
        double distance = this.start_drag.distance(current);
        double threshold = 50;
        
        if(distance >= threshold){
            this.setLocked(false);
        }
        
        if(!this.isLocked){
            Point offset = new Point((int) current.getX() - (int) start_drag.getX(),
                    (int) current.getY() - (int) start_drag.getY());
            JFrame frame = MouseMoveListener.getFrame(target);
            Point new_location = new Point(
                    (int) (this.start_loc.getX() + offset.getX()), (int) (this.start_loc.getY() + offset.getY()));
            
            Point newLocationFrameCenter = new Point(new_location.x + frame.getWidth() / 2,
                                          new_location.y + frame.getHeight() / 2);
            
            if (this.snapArea != null && this.snapArea.contains(newLocationFrameCenter)) {
                frame.setLocation(this.snapArea.x, this.snapArea.y);
                this.setLocked(true);
                this.isSnapped = true;
            } else {
                this.isSnapped = false;
                frame.setLocation(new_location);
            }
        }
    }

    public void mouseMoved(MouseEvent e) {
    }
    
    public void setLocked(boolean isLocked){
        this.isLocked = isLocked;
    }
    
    public boolean isLocked(){
        return this.isLocked;
    }
    
    /**
     * Sets the snap area of this listener. If set to null, then snapping
     * will be disabled.
     * 
     * @param snapArea 
     */
    public void setSnapArea(Rectangle snapArea){
        this.snapArea = snapArea;
    }
    
    public boolean isSnapped(){
        return this.isSnapped;
    }
    
    public void setSnapped(boolean isSnapped){
        this.isSnapped = isSnapped;
    }
}
