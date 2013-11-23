/*
 * JSliderSB.java
 * 
 * Version 1.0
 * 
 * 25 May 2013
 */
package com.bixly.pastevid.screencap.components.inworks;

import com.bixly.pastevid.common.Unimplemented;
import com.bixly.pastevid.util.LogUtil;
import com.bixly.pastevid.util.ResourceUtil;
import com.bixly.pastevid.util.view.JSliderCustomKnob;
import java.awt.Dimension;
import java.awt.Image;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JSlider;

/**
 * Custom slider with rounded borders used in the PreviewPlayer
 * @author cevaris
 */
@Unimplemented
public class JSliderSB extends JSlider {
    private Image knobImage;
    
    public JSliderSB() {
        this.loadKnobImage();
        this.setBorder(new JRoundBorderSB());
        this.setUI(new JSliderCustomKnob(this,this.knobImage, new Dimension(18,18)));
    }
    
    private void loadKnobImage() {
        try {
            this.knobImage = ImageIO.read(getClass().getResourceAsStream(ResourceUtil.SLIDER_KNOB_SB));
        } catch(IOException e) {
            log(e);
        }
    }

    private void log(Object message) {
        LogUtil.log(JSliderSB.class, message);
    }
    
}
