/*
 * JSliderCustomKnob.java
 *
 * Version 1.0
 * 
 * 20 May 2013
 */
package com.bixly.pastevid.util.view;

import com.bixly.pastevid.util.LogUtil;
import com.bixly.pastevid.util.ResourceUtil;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 *
 * @author Jorge
 */
public class JSliderCustomKnob extends BasicSliderUI {

    private Image knobImage;
    private Dimension knobSize;

    public JSliderCustomKnob(JSlider aSlider) {
        super(aSlider);
        normal();
    }
    
    public JSliderCustomKnob(JSlider aSlider, Image image, Dimension knobSize) {
        super(aSlider);
        this.knobImage = image;
        this.knobSize = knobSize;
    }

    @Override
    public void paintThumb(Graphics g) {
        
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(this.knobImage, thumbRect.x, thumbRect.y, knobSize.width, knobSize.height, null);
        g2.dispose();
    }

    public void log(Object message) {
        LogUtil.log(JSliderCustomKnob.class, message);
    }

    public Point getLocationKnobLocation() {
        return this.thumbRect.getLocation();
    }
    
    public void hover(){
        try {

            this.knobImage = ImageIO.read(getClass().getResourceAsStream(ResourceUtil.SLIDER_KNOB_HOVER));
            this.knobSize = new Dimension(18, 18);
        } catch (IOException e) {
            log(e);
        }
    }
    
    public void normal(){
        try {

            this.knobImage = ImageIO.read(getClass().getResourceAsStream(ResourceUtil.SLIDER_KNOB));
            this.knobSize = new Dimension(18, 18);
        } catch (IOException e) {
            log(e);
        }
    }

    public int getWidth() {
        return knobSize.width;
    }
}
