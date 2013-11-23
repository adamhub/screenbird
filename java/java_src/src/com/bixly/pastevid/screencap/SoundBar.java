/**
 * SoundBar.java
 * 
 * Version 1.0
 * 
 * 24 May 2013
 */
package com.bixly.pastevid.screencap;

import com.bixly.pastevid.util.ResourceUtil;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import javax.swing.JProgressBar;

/**
 * Custom JProgressBar for displaying the input sound levels on the screen recorder.
 * @author Thomas'
 */
public class SoundBar extends JProgressBar {
    private Image indeterminateImage;
    private Image indeterminateImage2;
    private Image[] activeImages = new Image[3];
    private Image[] inactiveImages = new Image[3];
    
    private boolean active = false;
    
    public SoundBar() {
        super();
        setOpaque(false);
        setMaximum(3700);
        setValue(3700);
        indeterminateImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource(ResourceUtil.SOUND_INDETERMINATE));
        indeterminateImage2 = Toolkit.getDefaultToolkit().getImage(getClass().getResource(ResourceUtil.SOUND_INDETERMINATE_INACTIVE));
        for (int i = 0; i < activeImages.length; i++) {
            activeImages[i] = Toolkit.getDefaultToolkit().getImage(getClass().getResource(ResourceUtil.SOUND_ACTIVE[i]));
            inactiveImages[i] = Toolkit.getDefaultToolkit().getImage(getClass().getResource(ResourceUtil.SOUND_INACTIVE[i]));
        }
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean b) {
        active = b;
    }
    
    @Override
    public void paint(Graphics g) {
        Image img = indeterminateImage;
        if (!active) {
            img = indeterminateImage2;
        }
        int index = (this.getValue() * (activeImages.length - 1)) / this.getMaximum();
        if (!isIndeterminate()) {
            //int index = (this.getValue() * (activeImages.length - 1)) / this.getMaximum();
            
            if (this.isActive()) {
                img = activeImages[index];
            } else {
                img = inactiveImages[index];
            }
            
        }
        g.drawImage(img, 0, 0, this);
    }
}
