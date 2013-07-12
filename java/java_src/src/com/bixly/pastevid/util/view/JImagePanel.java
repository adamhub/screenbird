/*
 * JImagePanel.java
 *
 * Version 1.0
 * 
 * 17 May 2013
 */
package com.bixly.pastevid.util.view;

import com.bixly.pastevid.util.LogUtil;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/**
 * Custom JPanel for Images.
 * @author Jorge
 */
public class JImagePanel extends JPanel{

    private Image image;
    private int width  = 0;
    private int height = 0;
    
    public JImagePanel() {
        super();
    }
    
    public synchronized void setImage(BufferedImage screenshot, int width, int height) {
        this.image = screenshot;
        this.width = width;
        this.height = height;
    }

    @Override
    public void paintComponent(Graphics g) {

        super.paintComponent(g);
                
        Graphics2D g2 = (Graphics2D)g;
        
        synchronized (this) {
            if (image != null) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(image, 0, 0, this.width, this.height, null);
            }
        }
        g2.dispose();
    }
    
    public void log(Object message) {
        LogUtil.log(JImagePanel.class, message);
    }

}