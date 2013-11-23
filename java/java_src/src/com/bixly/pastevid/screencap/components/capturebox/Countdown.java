/*
 * Countdown.java
 * 
 * Version 1.0
 * 
 * 16 May 2013
 */
package com.bixly.pastevid.screencap.components.capturebox;

import com.bixly.pastevid.recorders.Recorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;

/**
 * Class for displaying the countdown timer when starting the recorder.
 * @author jjmon_000
 */
public class Countdown {
    private class AlphaContainer extends JComponent {
        private JComponent component;
        
        public AlphaContainer(JComponent component){
            this.component = component;
            setLayout(new BorderLayout());
            setOpaque(false);
            component.setOpaque(false);
            add(component);
        }
        
        @Override
        protected void paintComponent(Graphics g){
            Graphics2D g2d = (Graphics2D)g;
            g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(component.getBackground());
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }
    
    private class CountdownWindow extends JWindow {
        public CountdownWindow(GraphicsConfiguration graphicsConfig){
            super(graphicsConfig);
        }
        
        @Override
        public void paintComponents(Graphics g){
            super.paintComponents(g);
            Graphics2D g2d = (Graphics2D)g;
            g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.clearRect(0, 0, getWidth(), getHeight());
            g2d.setColor(Color.red);
            g2d.drawLine(0, 0, 50, 50);
        }
    }
    
    private JLabel txtCountdownPane;
    private CountdownWindow winCountdown;
    private final Color TRANSPARENT = new Color(0, 0, 0, 0);
    
    public Countdown(boolean displayImmediately, Recorder recorder){
        txtCountdownPane = new JLabel();
        txtCountdownPane.setText("3");
        txtCountdownPane.setHorizontalAlignment(JLabel.CENTER);
        txtCountdownPane.setVerticalAlignment(JLabel.CENTER);
        txtCountdownPane.setForeground(HBorder.BORDER_FLASH_COLOR);
        txtCountdownPane.setBackground(TRANSPARENT);
        txtCountdownPane.setOpaque(false);
        setSize(txtCountdownPane, recorder.getCaptureRectangle().getSize());
        
        adjustCountdownFont();
        
        winCountdown = new CountdownWindow(recorder.getScreen().getDefaultConfiguration());
        JPanel contentPane = new JPanel();
        contentPane.setBackground(TRANSPARENT);
        winCountdown.setContentPane(new AlphaContainer(contentPane));
        setSize(winCountdown, recorder.getCaptureRectangle().getSize());
        winCountdown.setBackground(TRANSPARENT);
        winCountdown.getContentPane().add(new AlphaContainer(txtCountdownPane));
        setSize(winCountdown.getContentPane(), recorder.getCaptureRectangle().getSize());
        winCountdown.setLocation(recorder.getCaptureRectangle().x,
                                 recorder.getCaptureRectangle().y);
        winCountdown.setAlwaysOnTop(true);
        winCountdown.setVisible(displayImmediately);
    }
    
    public void setVisible(boolean isVisible){
        winCountdown.setVisible(isVisible);
    }
    
    public void setCount(int count){
        txtCountdownPane.setText(String.valueOf(count));
        adjustCountdownFont();
    }
    
    public void destroy(){
        synchronized(this){
            winCountdown.removeAll();
            txtCountdownPane = null;
            winCountdown.setVisible(false);
            winCountdown.dispose();
            winCountdown = null;
        }
    }
    
    private void adjustCountdownFont(){
        Font labelFont = txtCountdownPane.getFont();
        String labelText = txtCountdownPane.getText();
        int stringWidth = txtCountdownPane.getFontMetrics(labelFont).stringWidth(labelText);
        int componentWidth = txtCountdownPane.getWidth();
        double widthRatio = (double)componentWidth / (double)stringWidth;
        int newFontSize = (int)(labelFont.getSize() * widthRatio * 0.7);
        int componentHeight = (int)(txtCountdownPane.getHeight() * 0.7);
        int fontSizeToUse = Math.min(newFontSize, componentHeight);
        txtCountdownPane.setFont(new Font(labelFont.getName(), Font.BOLD, fontSizeToUse));
        txtCountdownPane.repaint();
    }
    
    private void setSize(Container component, Dimension size){
        component.setMinimumSize(size);
        component.setMaximumSize(size);
        component.setPreferredSize(size);
        component.setSize(size);
    }
}
