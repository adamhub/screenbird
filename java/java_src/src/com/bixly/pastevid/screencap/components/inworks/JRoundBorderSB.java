/*
 * JRoundBorderSB.java
 * 
 * Version 1.0
 * 
 * 25 May 2013
 */
package com.bixly.pastevid.screencap.components.inworks;

import com.bixly.pastevid.common.Unimplemented;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.border.AbstractBorder;

/**
 * Custom rounded border used in the PreviewPlayer slider.
 * @author cevaris
 */
@Unimplemented
public class JRoundBorderSB extends AbstractBorder {
    
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int r = height - 1;
        RoundRectangle2D round = new RoundRectangle2D.Float(x, y, width - 1, height - 1, r, r);
        Container parent = c.getParent();
        if (parent != null) {
            g2.setColor(parent.getBackground());
            Area corner = new Area(new Rectangle2D.Float(x, y, width, height));
            corner.subtract(new Area(round));
            g2.fill(corner);
        }
        
        g2.setColor(Color.lightGray);
        g2.draw(round);
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(4, 8, 4, 8);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = insets.right = 8;
        insets.top = insets.bottom = 4;
        return insets;
    }
}
