/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.screencap;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import javax.swing.JPanel;

import com.bixly.pastevid.util.ResourceUtil;
import java.awt.Color;

/**
 *
 * @author Thomas'
 */
public class RecorderPanelBG extends JPanel
{
    Image img = Toolkit.getDefaultToolkit().getImage(getClass().getResource(ResourceUtil.RECORDER_BG));
    
    @Override
    public void paint(Graphics g)
    {
        g.drawImage(img, 0, 0, new Color(0, 0, 0, 0), this);
        paintComponents(g);
    }
}
