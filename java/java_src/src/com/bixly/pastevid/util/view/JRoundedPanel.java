/*
 * JRoundedPanel.java
 * 
 * 20 May 2013
 */
package com.bixly.pastevid.util.view;

/*
 *  Copyright 2010 De Gregorio Daniele.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * This class represents a Rounded Border JPanel.
 */
public class JRoundedPanel extends JPanel {

    /** Stroke size. it is recommended to set it to 1 for better view */
    protected int strokeSize = 1;
    /** Color of shadow */
    protected Color shadowColor = Color.black;
    /** Sets if it drops shadow */
    protected boolean shady = false;
    /** Sets if it has an High Quality view */
    protected boolean highQuality = true;
    /** Double values for Horizzontal and Vertical radius of corner arcs */
    protected Dimension arcs = new Dimension(20, 20);
    /** Distance between border of shadow and border of opaque panel */
    protected int shadowGap = 0;
    /** The offset of shadow.  */
    protected int shadowOffset = 0;
    /** The transparency value of shadow. ( 0 - 255) */
    protected int shadowAlpha = 150;

    public JRoundedPanel() {
        super();
        setOpaque(false);
        Color backgrnd = new Color(Color.darkGray.getRed(),Color.darkGray.getGreen(), Color.darkGray.getBlue(),180);
        this.setBackground(backgrnd);
    }

    public JRoundedPanel(CardLayout subPanelCards) {
        super(subPanelCards);
        setOpaque(false);
        Color backgrnd = new Color(Color.darkGray.getRed(),Color.darkGray.getGreen(), Color.darkGray.getBlue(),180);
        this.setBackground(backgrnd);
    }

//    public JRoundedPanel(CardLayout cardLayout) {
//        super(cardLayout);
//        setOpaque(false);
//        Color backgrnd = new Color(Color.darkGray.getRed(),Color.darkGray.getGreen(), Color.darkGray.getBlue(),180);
//        this.setBackground(backgrnd);
//    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int width = getWidth();
        int height = getHeight();
        int shadowGap = this.shadowGap;
        Color shadowColorA = new Color(shadowColor.getRed(), shadowColor.getGreen(), shadowColor.getBlue(), shadowAlpha);
        Graphics2D graphics = (Graphics2D) g;

        //Sets antialiasing if HQ.
        if (highQuality) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        //Draws shadow borders if any.
        if (shady) {
            graphics.setColor(shadowColorA);
            graphics.fillRoundRect(
                    shadowOffset,// X position
                    shadowOffset,// Y position
                    width - strokeSize - shadowOffset, // width
                    height - strokeSize - shadowOffset, // height
                    arcs.width, arcs.height);// arc Dimension
        } else {
            shadowGap = 1;
        }

        //Draws the rounded opaque panel with borders.
        graphics.setColor(getBackground());        
        graphics.fillRoundRect(0, 0, width - shadowGap, height - shadowGap, arcs.width, arcs.height);
        graphics.setColor(getForeground());
        //graphics.setStroke(new BasicStroke(strokeSize));
        //graphics.drawRoundRect(0, 0, width - shadowGap, height - shadowGap, arcs.width, arcs.height);

        //Sets strokes to default, is better.
        graphics.setStroke(new BasicStroke());
    }

    /**
     * Check if component has High Quality enabled.
     * 
     * @return <b>TRUE</b> if it is HQ ; <b>FALSE</b> Otherwise
     */
    public boolean isHighQuality() {
        return highQuality;
    }

    /**
     * Sets whether this component has High Quality or not
     *
     * @param highQuality if <b>TRUE</b>, set this component to HQ
     */
    public void setHighQuality(boolean highQuality) {
        this.highQuality = highQuality;
    }

    /**
     * Returns the Color of shadow.
     *
     * @return a Color object.
     */
    public Color getShadowColor() {
        return shadowColor;
    }

    /**
     * Sets the Color of shadow
     *
     * @param shadowColor Color of shadow
     */
    public void setShadowColor(Color shadowColor) {
        this.shadowColor = shadowColor;
    }

    /**
     * Check if component drops shadow.
     *
     * @return <b>TRUE</b> if it drops shadow ; <b>FALSE</b> Otherwise
     */
    public boolean isShady() {
        return shady;
    }

    /**
     * Sets whether this component drops shadow
     *
     * @param shady if <b>TRUE</b>, it draws shadow
     */
    public void setShady(boolean shady) {
        this.shady = shady;
    }

    /**
     * Returns the size of strokes.
     *
     * @return the value of size.
     */
    public float getStrokeSize() {
        return strokeSize;
    }

    /**
     * Sets the stroke size value.
     *
     * @param strokeSize stroke size value
     */
    public void setStrokeSize(int strokeSize) {
        this.strokeSize = strokeSize;
    }

    /**
     * Get the value of arcs
     *
     * @return the value of arcs
     */
    public Dimension getArcs() {
        return arcs;
    }

    /**
     * Set the value of arcs
     *
     * @param arcs new value of arcs
     */
    public void setArcs(Dimension arcs) {
        this.arcs = arcs;
    }

    /**
     * Get the value of shadowOffset
     *
     * @return the value of shadowOffset
     */
    public int getShadowOffset() {
        return shadowOffset;
    }

    /**
     * Set the value of shadowOffset
     *
     * @param shadowOffset new value of shadowOffset
     */
    public void setShadowOffset(int shadowOffset) {
        if (shadowOffset >= 1) {
            this.shadowOffset = shadowOffset;
        } else {
            this.shadowOffset = 1;
        }
    }

    /**
     * Get the value of shadowGap
     *
     * @return the value of shadowGap
     */
    public int getShadowGap() {
        return shadowGap;
    }

    /**
     * Set the value of shadowGap
     *
     * @param shadowGap new value of shadowGap
     */
    public void setShadowGap(int shadowGap) {
        if (shadowGap >= 1) {
            this.shadowGap = shadowGap;
        } else {
            this.shadowGap = 1;
        }
    }

    /**
     * Get the value of shadowAlpha
     *
     * @return the value of shadowAlpha
     */
    public int getShadowAlpha() {
        return shadowAlpha;
    }

    /**
     * Set the value of shadowAlpha
     *
     * @param shadowAlpha new value of shadowAlpha
     */
    public void setShadowAlpha(int shadowAlpha) {
        if (shadowAlpha >= 0 && shadowAlpha <= 255) {
            this.shadowAlpha = shadowAlpha;
        } else {
            this.shadowAlpha = 255;
        }
    }
}
