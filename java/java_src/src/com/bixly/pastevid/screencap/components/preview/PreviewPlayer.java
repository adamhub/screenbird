/*
 * PreviewPlayerPanel.java
 *
 * Version 1.0
 * 
 * Created on Mar 29, 2012, 2:07:27 PM
 * 16 May 2013
 */
package com.bixly.pastevid.screencap.components.preview;

import com.sun.awt.AWTUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.SwingUtilities;

import com.bixly.pastevid.Settings;
import com.bixly.pastevid.editors.VideoScrubManager;
import com.bixly.pastevid.util.LogUtil;
import com.bixly.pastevid.util.MediaUtil;
import com.bixly.pastevid.util.ResourceUtil;
import com.bixly.pastevid.util.ScreenUtil;
import com.bixly.pastevid.util.TimeUtil;
import com.bixly.pastevid.util.view.JRoundedPanel;
import com.bixly.pastevid.util.view.JSliderCustomKnob;
import com.bixly.pastevid.util.view.JPreviewSlider;
import com.bixly.pastevid.util.view.MouseMoveListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;


/**
 * Class for displaying the preview player.
 * @author cevaris
 */
public class PreviewPlayer extends JRoundedPanel {
    
    private final ImageIcon spinnerImage = new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource(ResourceUtil.SPINNER_24x24)));
    private final String PAUSE = "";
    private final String PLAY  = "";
    
    private VideoScrubManager scrubManager;
    private PreviewPlayerForm jfPreviewPlayer;
    
    private JDialog jfRecordFromHere;
    private JPanel  jpRecordFromHere;
    private final Point recordFromHerePosition = new Point();
    
    private Point startDragLocation;
    private Point startLocation;
    
    private JSliderCustomKnob jSliderKnob;
    
    private boolean centerPreviewPlayer = true;
    
    
    public PreviewPlayer(PreviewPlayerForm jfPreviewPlayer, VideoScrubManager scrubManager) {
        this.jfPreviewPlayer = jfPreviewPlayer;
        this.scrubManager    = scrubManager;

        initComponents();
        initView();
        this.togglePlayPause.getModel().setSelected(false);
        
        this.jSliderKnob = new JSliderCustomKnob(this.previewTimeSlider);
        this.previewTimeSlider.setUI(this.jSliderKnob);
        this.previewTimeSlider.addMouseListener(new MouseAdapter(){
            // Mouse listener that updates the slider knob on hover
            @Override
            public void mouseEntered(MouseEvent me){
                jSliderKnob.hover();
                previewTimeSlider.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent me){
                jSliderKnob.normal();
                previewTimeSlider.repaint();
            }
        });
    }
    
    /**
     * Creates "record from here" secondary window.
     */
    private void initView() {
        this.previewTimeSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                previewTimeSlider.repaint();
                updateRecordNowButtonPosition();
                scrubManager.updateTimeLabels(previewTimeSlider.getValue());
            }
        });
        
        this.jfRecordFromHere = new JDialog(this.jfPreviewPlayer, "Record From Here");
        this.jfRecordFromHere.setUndecorated(true);
        
        this.jfRecordFromHere.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource(ResourceUtil.LOGO_TASKBAR)));
        
        if (MediaUtil.osIsUnix()) {
            // Renders Linux's custom view due to transparency issues.
            this.jpRecordFromHere = new RecordFromHereLinux(this);
        } else {
            // Renders Default view
            this.jpRecordFromHere = new RecordFromHereSB(this);
        }
        this.jfRecordFromHere.add(jpRecordFromHere);
        
        try {
            if (!AWTUtilities.isTranslucencyCapable(this.jfRecordFromHere.getGraphicsConfiguration())) {
                log("Can not set transparency");
                this.setBackground(new Color(64, 64, 64, 255));
                this.jpRecordFromHere.setBackground(new Color(64, 64, 64, 255));
                this.jpRecordFromHere.setOpaque(true);
            } else {
                log("Transparency is set");
                AWTUtilities.setWindowOpaque(this.jfRecordFromHere, false);
            }
        } catch (Exception ex) {log(ex);}
        
        // Hack for handling draggable JFrames on Mac OSX
        this.jfRecordFromHere.getRootPane().putClientProperty("apple.awt.draggableWindowBackground", Boolean.FALSE);
        
        this.jfRecordFromHere.pack();
        
        this.addMouseListener(new PreivewMouseListener());
        this.addMouseMotionListener(new PreviewMotionListener());
        this.previewTimeSlider.addMouseMotionListener(new PreviewSliderMotionListener());
        
    }
    
    //==========================================================================
    //Do not modify these variables in any method other than setScreenshot()
    //This is globally defined to prevent exessive allocation of Dimension objects
    private final Dimension playerSize = new Dimension();
    private final Dimension imageSize  = new Dimension();
    //==========================================================================
    private final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private final Dimension screenshotSize = new Dimension();
    
    /**
     * Sets the displayed screenshot on the PreviewPlayer.
     * @param screenshot 
     */
    public void setScreenshot(final BufferedImage screenshot){
        this.setScreenshot(screenshot, true, false);
    }
    
    /**
     * Sets the displayed screenshot on the PreviewPlayer.
     * @param screenshot BufferedImage object of the screenshot to be displayed
     * @param center Not used
     * @param show true if to display PreviewPlayer after setting the displayed screenshot
     */
    public void setScreenshot(final BufferedImage screenshot, boolean center, boolean show) {
        BufferedImage image = screenshot;
        
        // Get image size
        imageSize.width  = image.getWidth();
        imageSize.height = image.getHeight();
        
        Dimension displaySize = ScreenUtil.getScreenDimension(getGraphicsConfiguration().getDevice());
        
        // Scale images if dimensions are larger than the current screen
        if (imageSize.width > displaySize.width || imageSize.height > displaySize.height) {
            // Allows previewing of larger external displays
            // Adjust for full screen preview from external display
            screenshotSize.width  = (int) (displaySize.width * Settings.FULLSCREEN_PREVIEW_SCALE);
            screenshotSize.height = (int) (displaySize.height * Settings.FULLSCREEN_PREVIEW_SCALE);
        } else if (screenSize.equals(imageSize)) {   // For fullscreen display
            // Adjust for full screen preview
            screenshotSize.width  = (int) (imageSize.width * Settings.FULLSCREEN_PREVIEW_SCALE);
            screenshotSize.height = (int) (imageSize.height * Settings.FULLSCREEN_PREVIEW_SCALE);
        } else {
            // Adjust for custom screen preview
            if (1.0 * (imageSize.width + 100) / displaySize.width >= Settings.FULLSCREEN_PREVIEW_SCALE 
                    || 1.0 * (imageSize.height + 100) / displaySize.height >= Settings.FULLSCREEN_PREVIEW_SCALE) {
                screenshotSize.width  = (int) (displaySize.width * Settings.FULLSCREEN_PREVIEW_SCALE);
                screenshotSize.height = (int) (displaySize.height * Settings.FULLSCREEN_PREVIEW_SCALE);
            } else {
                screenshotSize.width  = (int) (imageSize.width * Settings.CUSTOMSCREEN_PREVIEW_SCALE);
                screenshotSize.height = (int) (imageSize.height * Settings.CUSTOMSCREEN_PREVIEW_SCALE);
            }
        }
        
        // Change screen size
        loadPlayerOffset();

        this.jpImage.setImage(screenshot, screenshotSize.width, screenshotSize.height);
        ScreenUtil.changeSize(jfPreviewPlayer.getFrame(), new Dimension(playerSize.width, playerSize.height));
        if (centerPreviewPlayer) {
            this.jfPreviewPlayer.loadSmartPosition(show);
        }
    }
    
    /**
     * Loads the correct offset in width and height to the player size. Not 
     * sure if compatible with every screen. 
     */
    private void loadPlayerOffset(){
        if (MediaUtil.osIsMac() || MediaUtil.osIsUnix()) {
            playerSize.width  = screenshotSize.width  + 5;
            playerSize.height = screenshotSize.height + 110;
        } else if (MediaUtil.osIsWindows()) {
            playerSize.width  = screenshotSize.width  + 22 ;
            playerSize.height = screenshotSize.height + 118;
        }
    }
    
    /**
     * Sets the visibility of the spinner. Used to indicate that the video is
     * still being prepared for playback.
     * @param isVisible true to display the spinner; false otherwise
     */
    public void setSpinnerVisible(boolean isVisible) {
        if (isVisible) {
            // Disable changes to the PreviewPlayer (e.g. play/pause, time seek)
            this.previewTimeSlider.setEnabled(false);
            this.togglePlayPause.setEnabled(false);
            this.togglePlayPause.setIcon(spinnerImage);
            this.togglePlayPause.setText("");
        } else {
            // Indicate that the video is ready for playback
            this.titleBarLabel.setText("Ready");
            this.enableEditing();
            this.scrubManager.pausePreviewVideo();
            this.togglePlayPause.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource(ResourceUtil.PLAY_BUTTON_UNPRESSED))));
            this.setToPlay();
        }
    }

    /**
     * Sets the play/pause button to Play mode.
     */
    public void setToPlay() {
        this.togglePlayPause.setSelected(false);
        this.togglePlayPause.setText(PLAY);
        this.repaint();
    }
    
    /**
     * Returns the current value of the time slider.
     * @return 
     */
    public synchronized Integer getSliderValue() {
        return this.previewTimeSlider.getValue();
    }
    
    /**
     * Returns the maximum value of the time slider.
     * @return 
     */
    public synchronized Integer getSliderMax(){
        return this.previewTimeSlider.getMaximum();
    }
    
    /**
     * Sets the current time display value.
     * @param text 
     */
    public synchronized void setLabelTextTimeCurrent(String text) {
        this.jlblTimeCurrent.setText(text);
        ((IRecordFromHere)this.jpRecordFromHere).setLabelTextTimeCurrent(text);
        
    }
    
    /**
     * Sets the remaining time display value.
     * @param text 
     */
    public synchronized void setLabelTextTimeRemaining(String text){
        this.jlblTimeRemaining.setText(text);
    }
    
    /**
     * Returns the JFrame for this PreviewPlayer.
     * @return 
     */
    public synchronized  PreviewPlayerForm getPreviewFrame() {
        return this.jfPreviewPlayer;
    }

    /**
     * Sets the maximum value for the time slider.
     * @param value 
     */
    public synchronized void setSliderMax(Integer value) {
        this.previewTimeSlider.setMaximum(value);
    }

    /**
     * Sets the current value for the time slider.
     * @param value 
     */
    public void setSliderValue(Integer value) {
        this.previewTimeSlider.setValue(value);
    }
    
    /**
     * Destroys the PreviewPlayer and the associated RecordFromeHere JFrame.
     */
    public void destroy(){
        this.jfPreviewPlayer.dispose();
        this.jfRecordFromHere.dispose();
    }
    
    /**
     * Restarts the screen recorder from the current position of the time slider.
     */
    public void startRecordingFromHere() {
        this.scrubManager.requestAddCut();
        this.togglePlayPause.setSelected(false);
        this.togglePlayPause.setText(PLAY);
        this.scrubManager.backToRecordPanel();
        this.scrubManager.postScrubStartRecording();
        this.hideRecordFromHerePanel();
    }
    
    /**
     * Disables changes to the PreviewPlayer status.
     */
    public void disableEditing() {
        this.scrubManager.setPreviewTimeToEnd();
        updateRecordNowButtonPosition();
        this.previewTimeSlider.setEnabled(false);
        this.btnSkipToStart.setEnabled(false);
        this.togglePlayPause.setEnabled(false);
    }
    
    /**
     * Enables changes to the PreviewPlayer status.
     */
    public void enableEditing() {
        this.setToPlay();
        updateRecordNowButtonPosition();
        this.previewTimeSlider.setEnabled(true);
        this.btnSkipToStart.setEnabled(true);
        this.togglePlayPause.setEnabled(true);
    }
    
    /**
     * Updates the position of the "Record From Here" frame based on the current
     * value of the time slider.
     */
    public synchronized void updateRecordNowButtonPosition() {
        this.previewTimeSlider.setVisible(true);
        this.jPanel2.setVisible(true);
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    Point knobLocation = jSliderKnob.getLocationKnobLocation();

                    if (previewTimeSlider.isShowing()) {
                        recordFromHerePosition.x = knobLocation.x 
                                + previewTimeSlider.getLocationOnScreen().x 
                                - jfRecordFromHere.getWidth() / 2 
                                + jSliderKnob.getWidth() / 2 
                                - 2;
                        recordFromHerePosition.y = knobLocation.y
                                + previewTimeSlider.getLocationOnScreen().y 
                                + (int)(3.0 * previewTimeSlider.getHeight() / 4.0);
                    }
                } catch (IllegalComponentStateException e) {
                    log(e);
                }
                jfRecordFromHere.setLocation(recordFromHerePosition);
                jpRecordFromHere.setVisible(true);
            }

        });
                
    }
    
    public void hideRecordFromHerePanel() {
        this.jfRecordFromHere.setAlwaysOnTop(false);
        this.jpRecordFromHere.setVisible(false);
        this.jfRecordFromHere.setVisible(false);
    }
    
    public void showRecordFromHerePanel() {
        updateRecordNowButtonPosition();    
        this.jfRecordFromHere.setVisible(true);
        this.jpRecordFromHere.setVisible(true);
    }
    
    public void takeFocusToRecordFromHerePanel() {
        this.jfRecordFromHere.setAlwaysOnTop(false);
    }

    public void giveFocusToRecordFromHerePanel() {
        this.jpRecordFromHere.requestFocus();
        this.jfRecordFromHere.setAlwaysOnTop(true);
        this.jfRecordFromHere.setVisible(true);
        this.jpRecordFromHere.setVisible(true);
    }
    
    public final void log(Object message) {
        LogUtil.log(PreviewPlayer.class, message);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelPreviewRoot = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        btnCancel = new javax.swing.JButton();
        btnMinimize = new javax.swing.JButton();
        titleBarLabel = new javax.swing.JLabel();
        jPanel3 = new JRoundedPanel();
        togglePlayPause = new javax.swing.JToggleButton();
        btnSkipToStart = new javax.swing.JButton();
        btnSkipToEnd = new javax.swing.JButton();
        btnFinalizeVideo = new javax.swing.JButton();
        jlblTimeCurrent = new javax.swing.JLabel();
        jlblTimeRemaining = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        previewTimeSlider = new JPreviewSlider(this);
        jPanel4 = new javax.swing.JPanel();
        jpImage = new com.bixly.pastevid.util.view.JImagePanel();

        setBackground(new java.awt.Color(214, 214, 214));
        setLayout(new java.awt.BorderLayout());

        jPanelPreviewRoot.setBackground(new java.awt.Color(214, 214, 214));
        jPanelPreviewRoot.setOpaque(false);
        jPanelPreviewRoot.setLayout(new java.awt.BorderLayout());

        jPanel1.setBackground(new java.awt.Color(76, 76, 76));
        jPanel1.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(56, 56, 56)));
        jPanel1.setPreferredSize(new java.awt.Dimension(677, 23));

        btnCancel.setBackground(java.awt.Color.white);
        btnCancel.setForeground(new java.awt.Color(255, 255, 255));
        btnCancel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/close_normal.png"))); // NOI18N
        btnCancel.setAlignmentY(0.0F);
        btnCancel.setBorder(null);
        btnCancel.setBorderPainted(false);
        btnCancel.setContentAreaFilled(false);
        btnCancel.setDoubleBuffered(true);
        btnCancel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnCancel.setMaximumSize(new java.awt.Dimension(42, 16));
        btnCancel.setMinimumSize(new java.awt.Dimension(42, 16));
        btnCancel.setName("btnCancelRecorder"); // NOI18N
        btnCancel.setPreferredSize(new java.awt.Dimension(42, 16));
        btnCancel.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/close_normal.png"))); // NOI18N
        btnCancel.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/close_hover.png"))); // NOI18N
        btnCancel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        btnMinimize.setBackground(java.awt.Color.darkGray);
        btnMinimize.setForeground(new java.awt.Color(255, 255, 255));
        btnMinimize.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/minimize_normal.png"))); // NOI18N
        btnMinimize.setBorder(null);
        btnMinimize.setContentAreaFilled(false);
        btnMinimize.setDoubleBuffered(true);
        btnMinimize.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnMinimize.setMargin(new java.awt.Insets(2, 14, 0, 14));
        btnMinimize.setMaximumSize(new java.awt.Dimension(21, 16));
        btnMinimize.setMinimumSize(new java.awt.Dimension(21, 16));
        btnMinimize.setName("btnMinimizeRecorder"); // NOI18N
        btnMinimize.setPreferredSize(new java.awt.Dimension(21, 16));
        btnMinimize.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/minimize_normal.png"))); // NOI18N
        btnMinimize.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/minimize_hover.png"))); // NOI18N
        btnMinimize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMinimizeActionPerformed(evt);
            }
        });

        titleBarLabel.setFont(new java.awt.Font("Arial", 1, 13)); // NOI18N
        titleBarLabel.setForeground(new java.awt.Color(255, 255, 255));
        titleBarLabel.setText("Loading...");
        titleBarLabel.setMaximumSize(new java.awt.Dimension(53, 19));
        titleBarLabel.setMinimumSize(new java.awt.Dimension(53, 19));
        titleBarLabel.setPreferredSize(new java.awt.Dimension(53, 19));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(titleBarLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 584, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(btnMinimize, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(3, 3, 3)
                .addComponent(titleBarLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnMinimize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jPanelPreviewRoot.add(jPanel1, java.awt.BorderLayout.PAGE_START);

        add(jPanelPreviewRoot, java.awt.BorderLayout.PAGE_START);

        jPanel3.setBackground(new java.awt.Color(214, 214, 214));

        togglePlayPause.setBackground(java.awt.Color.darkGray);
        togglePlayPause.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        togglePlayPause.setForeground(java.awt.Color.white);
        togglePlayPause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/preview/play_normal.png"))); // NOI18N
        togglePlayPause.setToolTipText("Play Preivew");
        togglePlayPause.setBorder(null);
        togglePlayPause.setBorderPainted(false);
        togglePlayPause.setContentAreaFilled(false);
        togglePlayPause.setFocusable(false);
        togglePlayPause.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        togglePlayPause.setIconTextGap(0);
        togglePlayPause.setMaximumSize(new java.awt.Dimension(113, 36));
        togglePlayPause.setMinimumSize(new java.awt.Dimension(113, 36));
        togglePlayPause.setName("togglePlayPause"); // NOI18N
        togglePlayPause.setPreferredSize(new java.awt.Dimension(113, 36));
        togglePlayPause.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/preview/play_pushed.png"))); // NOI18N
        togglePlayPause.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/preview/play_hover.png"))); // NOI18N
        togglePlayPause.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/preview/play_pushed.png"))); // NOI18N
        togglePlayPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                togglePlayPauseActionPerformed(evt);
            }
        });

        btnSkipToStart.setBackground(java.awt.Color.white);
        btnSkipToStart.setForeground(new java.awt.Color(255, 255, 255));
        btnSkipToStart.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/preview/rewind_normal.png"))); // NOI18N
        btnSkipToStart.setToolTipText("Skip to Beginning");
        btnSkipToStart.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnSkipToStart.setBorderPainted(false);
        btnSkipToStart.setContentAreaFilled(false);
        btnSkipToStart.setDoubleBuffered(true);
        btnSkipToStart.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSkipToStart.setMaximumSize(new java.awt.Dimension(24, 24));
        btnSkipToStart.setMinimumSize(new java.awt.Dimension(24, 24));
        btnSkipToStart.setName("btnCancelRecorder"); // NOI18N
        btnSkipToStart.setPreferredSize(new java.awt.Dimension(24, 24));
        btnSkipToStart.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/preview/rewind_hover.png"))); // NOI18N
        btnSkipToStart.setRolloverSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/preview/rewind_pushed.png"))); // NOI18N
        btnSkipToStart.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                btnSkipToStartMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                btnSkipToStartMouseReleased(evt);
            }
        });
        btnSkipToStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSkipToStartActionPerformed(evt);
            }
        });

        btnSkipToEnd.setBackground(java.awt.Color.white);
        btnSkipToEnd.setForeground(new java.awt.Color(255, 255, 255));
        btnSkipToEnd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/preview/ffwd_normal.png"))); // NOI18N
        btnSkipToEnd.setToolTipText("Skip To End");
        btnSkipToEnd.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnSkipToEnd.setBorderPainted(false);
        btnSkipToEnd.setContentAreaFilled(false);
        btnSkipToEnd.setDoubleBuffered(true);
        btnSkipToEnd.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSkipToEnd.setMaximumSize(new java.awt.Dimension(24, 24));
        btnSkipToEnd.setMinimumSize(new java.awt.Dimension(24, 24));
        btnSkipToEnd.setName("btnCancelRecorder"); // NOI18N
        btnSkipToEnd.setPreferredSize(new java.awt.Dimension(24, 24));
        btnSkipToEnd.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/preview/ffwd_hover.png"))); // NOI18N
        btnSkipToEnd.setRolloverSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/preview/ffwd_pushed.png"))); // NOI18N
        btnSkipToEnd.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                btnSkipToEndMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                btnSkipToEndMouseReleased(evt);
            }
        });
        btnSkipToEnd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSkipToEndActionPerformed(evt);
            }
        });

        btnFinalizeVideo.setBackground(java.awt.Color.darkGray);
        btnFinalizeVideo.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        btnFinalizeVideo.setForeground(new java.awt.Color(255, 255, 255));
        btnFinalizeVideo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/preview/finalize_normal.png"))); // NOI18N
        btnFinalizeVideo.setToolTipText("Finalize Screen Capture");
        btnFinalizeVideo.setBorder(null);
        btnFinalizeVideo.setContentAreaFilled(false);
        btnFinalizeVideo.setDoubleBuffered(true);
        btnFinalizeVideo.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnFinalizeVideo.setMaximumSize(new java.awt.Dimension(24, 24));
        btnFinalizeVideo.setMinimumSize(new java.awt.Dimension(24, 24));
        btnFinalizeVideo.setName("btnFinalizeVideo"); // NOI18N
        btnFinalizeVideo.setPreferredSize(new java.awt.Dimension(24, 24));
        btnFinalizeVideo.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/preview/finalize_hover.png"))); // NOI18N
        btnFinalizeVideo.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/preview/finalize_pushed.png"))); // NOI18N
        btnFinalizeVideo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFinalizeVideoActionPerformed(evt);
            }
        });

        jlblTimeCurrent.setBackground(new java.awt.Color(214, 214, 214));
        jlblTimeCurrent.setForeground(new java.awt.Color(255, 255, 255));
        jlblTimeCurrent.setText("TimeCurr");
        jlblTimeCurrent.setToolTipText("Current Time of Screen Capture");
        jlblTimeCurrent.setDoubleBuffered(true);

        jlblTimeRemaining.setForeground(new java.awt.Color(255, 255, 255));
        jlblTimeRemaining.setText("TimeLeft");
        jlblTimeRemaining.setToolTipText("Time Left of Screen Capture");
        jlblTimeRemaining.setDoubleBuffered(true);

        jPanel2.setBackground(new java.awt.Color(214, 214, 214));

        previewTimeSlider.setName("jsliderpreview"); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addComponent(previewTimeSlider, javax.swing.GroupLayout.DEFAULT_SIZE, 367, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(previewTimeSlider, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(togglePlayPause, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(btnSkipToStart, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSkipToEnd, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jlblTimeCurrent)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jlblTimeRemaining)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnFinalizeVideo, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(togglePlayPause, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnSkipToStart, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnSkipToEnd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnFinalizeVideo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jlblTimeRemaining)
                            .addComponent(jlblTimeCurrent))))
                .addContainerGap(18, Short.MAX_VALUE))
        );

        add(jPanel3, java.awt.BorderLayout.PAGE_END);

        jPanel4.setBackground(new java.awt.Color(214, 214, 214));

        jpImage.setEnabled(false);

        javax.swing.GroupLayout jpImageLayout = new javax.swing.GroupLayout(jpImage);
        jpImage.setLayout(jpImageLayout);
        jpImageLayout.setHorizontalGroup(
            jpImageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 653, Short.MAX_VALUE)
        );
        jpImageLayout.setVerticalGroup(
            jpImageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 228, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jpImage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jpImage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        add(jPanel4, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void btnSkipToEndActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSkipToEndActionPerformed
        this.togglePlayPause.setText(PLAY);
        this.togglePlayPause.setSelected(false);
        this.scrubManager.pausePreviewVideo();
        TimeUtil.skipToMyLouMS(200L);
        this.scrubManager.setPreviewTimeToEnd();
    }//GEN-LAST:event_btnSkipToEndActionPerformed

    private void btnSkipToEndMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSkipToEndMousePressed
        //this.btnSkipToEnd.setBackground(COLOR_BTN_PRESSED);
    }//GEN-LAST:event_btnSkipToEndMousePressed

    private void btnSkipToEndMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSkipToEndMouseReleased
        //this.btnSkipToEnd.setBackground(COLOR_BTN_NORMAL);
    }//GEN-LAST:event_btnSkipToEndMouseReleased

    private void btnFinalizeVideoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFinalizeVideoActionPerformed
        if (this.scrubManager.isPreviewing()) {
            this.togglePlayPause.setText(PLAY);
            this.togglePlayPause.setSelected(false);
            this.scrubManager.pausePreviewVideo();
        }

        this.scrubManager.endPreviewVideo();
        this.scrubManager.finalizeVideo();
        this.hideRecordFromHerePanel();
    }//GEN-LAST:event_btnFinalizeVideoActionPerformed

    private void togglePlayPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_togglePlayPauseActionPerformed
        this.togglePlayPause.setVisible(false);
        if (this.togglePlayPause.getModel().isSelected()) {
            this.togglePlayPause.setText(PAUSE);
            this.scrubManager.startPreviewVideo();
        } else {
            this.togglePlayPause.setText(PLAY);
            this.togglePlayPause.setSelected(false);
            this.scrubManager.pausePreviewVideo();
        }
        this.togglePlayPause.setVisible(true);
    }//GEN-LAST:event_togglePlayPauseActionPerformed

    private void btnSkipToStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSkipToStartActionPerformed
        togglePlayPause.setText(PLAY);
        scrubManager.pausePreviewVideo();
        TimeUtil.skipToMyLouMS(200L);
        scrubManager.setPreviewTimeToStart();
    }//GEN-LAST:event_btnSkipToStartActionPerformed

    private void btnSkipToStartMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSkipToStartMousePressed
    }//GEN-LAST:event_btnSkipToStartMousePressed

    private void btnSkipToStartMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSkipToStartMouseReleased
    }//GEN-LAST:event_btnSkipToStartMouseReleased

    private void btnMinimizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMinimizeActionPerformed
        this.hideRecordFromHerePanel();
        this.jfPreviewPlayer.getFrame().setState(JFrame.ICONIFIED);
    }//GEN-LAST:event_btnMinimizeActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        boolean approval = (JOptionPane.showConfirmDialog(this,
            "Are you sure you want to exit?",
            "Exit Screenbird",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION);
    if (approval) {
        this.scrubManager.requestCloseRecorder();
    }

    if (this.scrubManager.isPreviewing()) {
        this.scrubManager.pausePreviewVideo();
        this.togglePlayPause.setText(PLAY);
        this.togglePlayPause.setSelected(false);
        }
    }//GEN-LAST:event_btnCancelActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JButton btnCancel;
    private javax.swing.JButton btnFinalizeVideo;
    public javax.swing.JButton btnMinimize;
    public javax.swing.JButton btnSkipToEnd;
    public javax.swing.JButton btnSkipToStart;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanelPreviewRoot;
    private javax.swing.JLabel jlblTimeCurrent;
    private javax.swing.JLabel jlblTimeRemaining;
    private com.bixly.pastevid.util.view.JImagePanel jpImage;
    private javax.swing.JSlider previewTimeSlider;
    private javax.swing.JLabel titleBarLabel;
    private javax.swing.JToggleButton togglePlayPause;
    // End of variables declaration//GEN-END:variables
    
    /**
     * Returns a Point object of the current screen position of the cursor.
     * @param e
     * @return 
     */
    private Point getScreenLocation(MouseEvent e) {
        Point cursor = e.getPoint();
        Point target_location = jPanel2.getLocationOnScreen();
        return new Point((int) (target_location.getX() + cursor.getX()),
                (int) (target_location.getY() + cursor.getY()));
    }
    
    /**
     * Subclass for handling click events of Preview Player.
     */
    private class PreivewMouseListener implements MouseListener {
        public void mouseClicked(MouseEvent me) {
        }
        
        public void mouseEntered(MouseEvent me) {
        }
        
        public void mouseExited(MouseEvent me) {
        }
        
        public void mousePressed(MouseEvent e) {
            startDragLocation = getScreenLocation(e);
            startLocation = MouseMoveListener.getFrame(jPanel2).getLocation();
            hideRecordFromHerePanel();
        }
        
        public void mouseReleased(MouseEvent me) {
            updateRecordNowButtonPosition();
        }
    }
    
    /**
     * Sublcass for handling dragging of Preview Player.
     */
    private class PreviewMotionListener implements MouseMotionListener {
        private final Point offset = new Point();
        private final Point newLocation = new Point();
        
        public void mouseMoved(MouseEvent me) {
        }

        public void mouseDragged(MouseEvent e) {
            // User has moved the preview player, now avoiding centering of preview player 
            centerPreviewPlayer = false;
            
            Point current = getScreenLocation(e);
            offset.x = (int) current.getX() - (int) startDragLocation.getX();
            offset.y = (int) current.getY() - (int) startDragLocation.getY();
            
            JFrame frame = MouseMoveListener.getFrame(jPanel2);
            newLocation.x = (int) (startLocation.getX() + offset.getX());
            newLocation.y = (int) (startLocation.getY() + offset.getY());
            frame.setLocation(newLocation);
            updateRecordNowButtonPosition();
        }
    }
    
    private class PreviewSliderMotionListener implements MouseMotionListener {
        public void mouseMoved(MouseEvent me) {
        }

        public void mouseDragged(MouseEvent e) {
            previewTimeSlider.repaint();
            updateRecordNowButtonPosition();
        }
    }    
}
