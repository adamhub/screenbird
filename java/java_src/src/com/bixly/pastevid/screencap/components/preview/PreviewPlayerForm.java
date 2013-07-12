/*
 * PreviewPlayer.java
 * 
 * Version 1.0
 *
 * Created on Mar 29, 2012, 12:39:14 PM
 * 16 May 2013
 */
package com.bixly.pastevid.screencap.components.preview;

import com.bixly.pastevid.editors.VideoScrubManager;
import com.bixly.pastevid.util.LogUtil;
import com.bixly.pastevid.util.ResourceUtil;
import com.bixly.pastevid.util.ScreenUtil;
import com.sun.awt.AWTUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import javax.swing.JFrame;

/**
 *
 * @author cevaris
 */
public class PreviewPlayerForm extends javax.swing.JFrame {
    private PreviewPlayer jpPreviewPlayer;
    private JFrame        jfPreviewPlayer;

    private VideoScrubManager scrubManager; 
    
    private boolean isRecordingPreviousVideo = false;
    
    final private Rectangle playerRect = new Rectangle();
    
    public PreviewPlayerForm(VideoScrubManager scrubManager) {
        this.scrubManager = scrubManager;
        initComponents();
        initView();
    }

    /**
     * Returns the PreviewPlayer panel for this form.
     * @return 
     */
    public PreviewPlayer getPlayerPanel() {
        return jpPreviewPlayer;
    }

    /**
     * Displays the PreviewPlayer on the given Point on screen.
     * @param playLocation 
     */
    public void showPlayer(Point playLocation) {
        this.jfPreviewPlayer.setLocation(playLocation);
        this.jfPreviewPlayer.setVisible(true);
        this.jpPreviewPlayer.showRecordFromHerePanel();
        
        if (this.isRecordingPreviousVideo) {
            this.jpPreviewPlayer.disableEditing();
        }
    }
    
    public void showPlayer() {
        this.jfPreviewPlayer.setVisible(true);
        this.jpPreviewPlayer.setVisible(true);
        this.jpPreviewPlayer.showRecordFromHerePanel();
        
        if (this.isRecordingPreviousVideo) {
            this.jpPreviewPlayer.disableEditing();
        }
    }
    
    public void hidePlayer() {
        this.jfPreviewPlayer.setVisible(false);
        this.jpPreviewPlayer.hideRecordFromHerePanel();
    }
    
    /**
     * Moves the PreviewPlayer to the center of the screen.
     * @param jSliderPreview
     * @param show 
     */
    public synchronized void loadSmartPosition(boolean show) {
        Dimension screenSize = ScreenUtil.getScreenDimension(getGraphicsConfiguration().getDevice());
        Dimension playerSize = this.jfPreviewPlayer.getSize();
        
        // Determines the cooridnates to center a form to the screen
        int x = screenSize.width;
        int y = screenSize.height;
        int w = playerSize.width;
        int h = playerSize.height;
        x = (x/2) - (w/2);
        y = (y/2) - (h/2);
        this.jfPreviewPlayer.setLocation(x, y);
        
        if (show) {
            this.showPlayer();
        }
    }
    
    /**
     * Returns the JFrame for this preview player.
     * @return 
     */
    public JFrame getFrame() {
        return this.jfPreviewPlayer;
    }
    
    /**
     * Returns the rectangle object for the player.
     * @return 
     */
    public synchronized Rectangle getRectangle(){
        this.playerRect.setSize(this.getSize());
        this.playerRect.setLocation(this.getLocation());
        return this.playerRect;
    }

    /**
     * Initializes the preview player view.
     */
    private void initView() {
        this.setVisible(false);
        
        if (this.jfPreviewPlayer != null) {
            this.jfPreviewPlayer.dispose();
        }

        this.jfPreviewPlayer = new JFrame("Screenbird PreviewPlayer");
        this.jfPreviewPlayer.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource(ResourceUtil.LOGO_TASKBAR)));
        this.jpPreviewPlayer = new PreviewPlayer(this, this.scrubManager);
        this.jfPreviewPlayer.setUndecorated(true);
        this.jfPreviewPlayer.add(this.jpPreviewPlayer);
        
        try {
            if (!AWTUtilities.isTranslucencyCapable(this.getGraphicsConfiguration())) {
                log("Can not set transparency for Preview Player");
                this.setBackground(new Color(64, 64, 64, 255));
                this.jpPreviewPlayer.setBackground(new Color(64, 64, 64, 255));
                this.jpPreviewPlayer.setOpaque(true);
            } else {
                log("Transparency is set for Preview Player");
                AWTUtilities.setWindowOpaque(this.jfPreviewPlayer, false);
            }
        } catch (Exception ex) {
            log(ex);
        }
        
        // Hack for handling draggable JFrames on Mac OSX
        this.jfPreviewPlayer.getRootPane().putClientProperty("apple.awt.draggableWindowBackground", Boolean.FALSE);
        
        this.jfPreviewPlayer.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.jfPreviewPlayer.setResizable(true);
        this.jfPreviewPlayer.pack();
        this.jfPreviewPlayer.setVisible(false);
        
        // Window focus listener for synchronizing display of RecordFromHere 
        // panel with the PreviewPlayer
        this.jfPreviewPlayer.addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent we) {
                jpPreviewPlayer.giveFocusToRecordFromHerePanel();
            }

            public void windowLostFocus(WindowEvent we) {
                jpPreviewPlayer.takeFocusToRecordFromHerePanel();
            }
        });
    }
    
    public synchronized void setIsRecordingPreviousVideo(boolean recordingPreviousVideo) {
        log("setting setIsRecordingPreviousVideo "+recordingPreviousVideo);
        this.isRecordingPreviousVideo = recordingPreviousVideo;
    }
    
    public void log(Object message) {
        LogUtil.log(PreviewPlayerForm.class, message);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setUndecorated(true);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 380, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 53, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
