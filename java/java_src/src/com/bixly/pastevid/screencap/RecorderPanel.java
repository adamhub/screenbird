/*
 * RecorderPanel.java
 * 
 * Version 1.0
 * 
 * 10 May 2013
 */
package com.bixly.pastevid.screencap;

import com.bixly.pastevid.Session;
import com.bixly.pastevid.Settings;
import com.bixly.pastevid.download.DownloadManager;
import com.bixly.pastevid.download.DownloadStatus;
import com.bixly.pastevid.download.DownloadThread;
import com.bixly.pastevid.editors.VideoScrubManager;
import com.bixly.pastevid.recorders.IMeasurable;
import com.bixly.pastevid.recorders.Recorder;
import com.bixly.pastevid.recorders.RecorderStatus;
import com.bixly.pastevid.recorders.UnsupportedBitRateCompression;
import com.bixly.pastevid.screencap.components.*;
import com.bixly.pastevid.screencap.components.capturebox.CaptureBox;
import com.bixly.pastevid.screencap.components.capturebox.CaptureBoxController;
import com.bixly.pastevid.screencap.components.capturebox.CaptureBoxState;
import com.bixly.pastevid.screencap.components.capturebox.Countdown;
import com.bixly.pastevid.screencap.components.progressbar.FFMpegProgressBarListener;
import com.bixly.pastevid.screencap.components.progressbar.ProgressBarUploadProgressListener;
import com.bixly.pastevid.screencap.components.settings.SettingsForm;
import com.bixly.pastevid.util.FileUtil;
import com.bixly.pastevid.util.LogUtil;
import com.bixly.pastevid.util.MediaUtil;
import com.bixly.pastevid.util.ResourceUtil;
import com.bixly.pastevid.util.ScreenUtil;
import com.bixly.pastevid.util.SoundUtil;
import com.bixly.pastevid.util.TimeUtil;
import com.bixly.pastevid.util.view.JRoundedPanel;
import com.bixly.pastevid.util.view.MouseMoveListener;
import com.bixly.pastevid.util.view.MovFileFilter;
import com.sun.awt.AWTUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TimerTask;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;
import javax.swing.text.JTextComponent;
import org.netbeans.lib.awtextra.AbsoluteConstraints;
import org.netbeans.lib.awtextra.AbsoluteLayout;

/**
 * @author Bixly
 */
public class RecorderPanel extends JPanel 
    implements IMeasurable, IAudioObserver {

    public static final String RECORDER_MESSAGE = "RECORDER_MESSAGE";
    public static final String PREVIEW_PANEL    = "PREVIEW_PANEL";
    
    // Dimension parameters for several different states of the screen recorder application.
    public static final Dimension SETTINGS_SIZE = new Dimension(360, 46);
    public static final Dimension RECORDER_SIZE_MESSAGE = new Dimension(276, 61);
    public static final Dimension NON_RECORDING_SIZE = new Dimension(276, 62);
    public static final Dimension BACKUP_RECORDING_SIZE = new Dimension(444, 44);
    public static final Dimension RECORDING_SIZE     = new Dimension(276, 62);
    
    public static final Dimension UPLOAD_SIZE   = new Dimension(520, 411);
    public static final Dimension UPLOAD_SIZE_MESSAGE = new Dimension(520, 411);
    
    // Color settings for differnt types of messages
    public static final Color MESSAGE_OK    = new Color(49, 130, 31);
    public static final Color MESSAGE_INFO  = new Color(221, 79, 78);
    public static final Color MESSAGE_ERROR = new Color(179, 24, 24);
    
    /**
     * Used to get access to the build version of the screen recorder application.
     */
    public static final ResourceBundle resources =  ResourceBundle.getBundle("version"); 
    
    /**
     * Max tries for attempting to upload a screen capture.
     */
    public static final int MAX_UPLOAD_RETRY = 1;
    
    /**
     * Marker name for flagging whether to upload after finishing encoding.
     */
    public static final String UPLOAD_ON_ENCODE    = "uploadOnEncode";
    
    public static final String DESCRIPTION_DEFAULT = "Enter a description";
    
    /*
     * Icons that are updated per state change of the recorder
     */
    private ImageIcon recordIcon;
    private ImageIcon pauseIcon;
    private ImageIcon pauseIconOver;
    
    /**
     * Capture box which restricts a screen capture to a specific resolution.
     */
    private CaptureBoxController captureBox;
    
    /**
     * Progress bar listener for the tracking of screencapture uploads.
     */
    private ProgressBarUploadProgressListener listener;
    
    /**
     * Progress bar listener for the tracking of encoding tasks.
     */
    private FFMpegProgressBarListener pbEncodingListener;
    
    /**
     * Thread used to dispatch open command to play video locally via
     * clients default media player.
     */
    private OpenClientMediaPlayer openClientMediaPlayer;
    
    /**
     * Screen Recorder application views.
     */
    private SettingsForm jfSettings;
    
    /**
     * Videos scrubbing feature manager.
     */
    private VideoScrubManager scrubManager;
    
    private Recorder recorder;
    
    /**
     * Path of the final encoded screen capture.
     */
    private String recordingOutput;
    
    private  PropertiesManager propertiesManager = new PropertiesManager(Settings.SCREENBIRD_CONFIG);
    private final String IS_AUTO = "capturebox.isAutoUpload";
    
    private long fileSize;
    private long lastKeyWhen = 0;
    private int  retry = MAX_UPLOAD_RETRY;
    private boolean redirectOnUpload = true;
    private boolean isRecordingPreviousVideo = false;
    private String outputMovieSlug = "";
    
    private boolean audioRecovering = false;
    
    /**
     * Listener for the URL upload bar. Captures mouse click to 
     * redirect to that URL with client's default web browser. 
     */
    private MouseListener     uploadUrlListener;
    
    /**
     * Listener for the screen capture drag box.
     */
    private MouseMoveListener appMouseListener;
    
    /**
     * Default saving location of a screen capture to the users desktop
     * regardless of the user's operating system.
     */
    private String lastSavedLocation = System.getProperty("user.home") + "/Desktop/";
    
    // For marking certain states of the screen recorder application.
    // Specifically if the application is busy working on a task such as encoding or uploading.
    private boolean isProcessingVideo   = false;
    private boolean uploading = false;
    private boolean saved     = false;
    
    // Clock variables
    private long totalTime;
    private long currentTime;
    private long clockTime   = 0;
    private long clockOffset = 0;
    private Timer clockTimer;
    private Timer msgClock;
    
    // Volume monitor
    private Timer volumeMonitor;
    private GraphicsDevice lastScreen;
    
    // Countdown
    private int countdownSec     = 4;
    private Timer countdownTimer = null;
    private UploadThread uploadThread;
    
    private boolean isEncoding = false;
    private ScreenRecorderController jfRecorderPanel;
    
    /** GET & SET **/
    public Recorder getRecorder() {
        return recorder;
    }
    
    public VideoScrubManager getScrubManager(){
        return scrubManager;
    }
    
    public void setRecorder(Recorder recorder) {
        this.recorder = recorder;
        this.recorder.addAudioObserver(this);
    }

    /**
     * Sets the screen recorder controller
     * @param screenRecorder 
     */
    public void setController(ScreenRecorderController screenRecorder) {
        this.jfRecorderPanel = screenRecorder;
    }
    
    
    /**
     * Returns the current clock time. Implementation of IMeasurable
     * @return 
     */
    public long getValue() {
        return clockTime;
    }
    
    /**
     * Returns the current screen where the recorder is at.
     * @return 
     */
    public GraphicsDevice getScreen() {
        GraphicsConfiguration gd = this.getGraphicsConfiguration();
        return gd.getDevice();
    }
    
    /**
     * Redraws the recorder controls based on the position of capture box.
     * @param x x-position of the upper-left corner of the capture box
     * @param y y-position of the upper-left corner of the capture box
     */
    public void relocate(int x, int y) {
        // Verify that there is currently a capture box on screen and that we
        // are not on full screen mode.
        if (this.captureBox != null 
                && this.captureBox.getState() == CaptureBoxState.CUSTOM_SCREEN 
                && this.recorder.getCaptureRectangle() != null) {
                
            // Get capture box rectangle
            Rectangle rectangle = this.recorder.getCaptureRectangle();

            // Get current screen size
            Dimension screenSize = ScreenUtil.getScreenDimension(recorder.getScreen());

            // Borders of the capture box
            Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(this.getGraphicsConfiguration());

            // Compute for x-offset where to attach the recorder controls to
            // the capture box so that it is centered.
            x = x + rectangle.width / 2 - this.recorderPanelBG1.getWidth() / 2;

            // Check if there is still space at the bottom of the capture 
            // box to position the recorder controls.
            boolean hasSpaceBottom = screenSize.height - scnMax.bottom - rectangle.y - rectangle.height - CaptureBox.BORDER_THICKNESS > this.recorderPanelBG1.getHeight();

            // Check if there is still space at the top of the capture box
            // to position the recorder controls.
            boolean hasSpaceTop = rectangle.y - scnMax.top > this.recorderPanelBG1.getHeight();

            if (!hasSpaceBottom && hasSpaceTop) {
                // Position the recorder controls on top of the capture box
                y = y - CaptureBox.BORDER_THICKNESS - this.recorderPanelBG1.getHeight();
            } else if (hasSpaceBottom) {
                // Position the recorder controls below the capture box
                y = y + rectangle.height + CaptureBox.BORDER_THICKNESS;
            }

            // Sets the snapping area for the recorder controls
            this.appMouseListener.setSnapArea(new Rectangle(x, y, this.recorderPanelBG1.getWidth(), this.recorderPanelBG1.getHeight()));

            if (this.appMouseListener.isSnapped() && (hasSpaceBottom || hasSpaceTop)) {
                // Set location for the recorder controls
                this.jfRecorderPanel.controlSetLocation(x, y);
                this.appMouseListener.setLocked(true);
            } else {
                // Default location for recorder controls
                if (this.appMouseListener.isLocked()) {
                    this.jfRecorderPanel.controlSetLocation(100, 100);
                    this.appMouseListener.setLocked(false);
                    this.appMouseListener.setSnapArea(null);
                    this.appMouseListener.setSnapped(false);
                }
            }
        } else {
            // Unlock recorder controls and remove snap area for the capture box
            // when on full screen mode
            this.appMouseListener.setLocked(false);
            this.appMouseListener.setSnapArea(null);
        }
    }
    
    /**
     * Implementation for IAudioObserver.
     */
    public void update(IAudioSubject subject) {
        btnRecordNonRec.setEnabled(!subject.isCompiling());
        this.jToggleButton3.setEnabled(!subject.isCompiling());
    }
    
    /**
     * Main launcher for initiating the recorder panel.
     */
    public void initRecorder() {
        saved = false;
        
        this.printBuildNumber();
        this.initIcons();
        this.initComponents();
        this.screenGroup.add(this.jToggleButton1);
        this.screenGroup.add(this.jToggleButton2);
        this.jToggleButton2.setSelected(true);
        this.loadSettings();
        this.initOpSysEnvironments();
        this.setRecorder(new Recorder(this));
        this.monitorAudioLine();
        this.initSettingsForm();
        this.configureClock();
        this.initVolumeMonitor();       
        this.showRecorderForm();
        this.addShortcutsListener();
        this.addMouseListeners();
        this.initScrubFeature();
        this.initResizeTask();
    }
    
    /**
     * Loads settings from the properties file. 
     */
    public void loadSettings() {
        try {
            Properties properties = this.propertiesManager.readPropertyFile();
            if (properties.getProperty(IS_AUTO).equals("true")) {
                chkAutoUpload.setSelected(true);
                FileUtil.addMarker(UPLOAD_ON_ENCODE);
            }
        } catch (Exception exc) {
            log("Exception loading " + exc.getMessage());
        }
    }
    
    public void initCaptureBox() {
        this.captureBox = new CaptureBox(this.recorder);
        this.setToggleStateCustomFullScreen(this.captureBox.getState(),true);
    }

    /**
     * Initializes the settings form.
     */
    private void initSettingsForm() {
        this.jfSettings = new SettingsForm();
        this.jfSettings.setRecorder(this.getRecorder());
        this.jfSettings.setRecorderPanel(this);
    }
    
    /**
     * Loads Recorder panel Icons.
     */    
    private void initIcons() {
        recordIcon = new ImageIcon(getClass().getResource(ResourceUtil.RECORD));
        pauseIcon = new ImageIcon(getClass().getResource(ResourceUtil.PAUSE));
        pauseIconOver = new ImageIcon(getClass().getResource(ResourceUtil.PAUSE_PRESSED));
    }
    
    /**
     * Binds VideoScrubManager with RecorderPanel.
     */
    private void initScrubFeature() {
        this.scrubManager = new VideoScrubManager();
        this.scrubManager.setRecorderPanel(this);
    }
    
    /**
     * Initiates volume meter polling. 
     */
    private void initVolumeMonitor() {
        volumeMonitor = new Timer(60, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateVolumeLevel();
            }
        });
        volumeMonitor.start();
    }
    
    /**
     * Creates the listeners for mouse clicks and drags.
     */
    private void addMouseListeners() {
        this.appMouseListener = new MouseMoveListener(this);
        this.addMouseListener(this.appMouseListener);
        this.addMouseMotionListener(this.appMouseListener);
    }
    
    /**
     * Binds certain Recorder Panel Actions to key shortcuts. <BR/>
     * Current bindings <BR/>
     * - Start/Pause Recording <BR/>
     * - Toggling of Full/Custom sized screen capture <BR/>
     * - Bringing up the settings form <BR/>
     * - Start Preview/Editing Mode <BR/>
     * - Finalizing screen capture <BR/>
     */
    private void addShortcutsListener() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (!jpUpload.isVisible()     // We do not want to trigger any of this on upload screen 
                        && (jpRecorderNonRec.isVisible() || jpRecorderRec.isVisible() || jpRecorderBackup.isVisible()) // Recorder app is visible
                        && btnRecordNonRec.isEnabled()  // Only if applet is in recordable state
                        && (countdownTimer == null)   // Locks all input while counting down recorder
                        && (e.getWhen() > (lastKeyWhen + 1000))) { // Place 1 second threshold between key events
                    
                    if (e.getKeyCode() == KeyEvent.VK_SPACE && !scrubManager.isPreviewing()) {
                        // Start Recording
                        e.consume();
                        lastKeyWhen = e.getWhen();
                        recordOrPause(true);
                    } else if (e.getKeyCode() == KeyEvent.VK_T 
                            && (e.isControlDown() || e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())
                            && recorder.getStatus() != RecorderStatus.RECORDING
                            && !scrubManager.isPreviewing()) {
                        // Toggle Fullscreen/CustomScreen
                        e.consume();
                        lastKeyWhen = e.getWhen();
                        if (Settings.ENABLE_OOPS_FEATURE) {
                            toggleFullScreen.setSelected(!toggleFullScreen.isSelected());
                        } else {
                            toggleFullScreenBackup.setSelected(!toggleFullScreen.isSelected());
                        }
                        toggleCustomFullscreen();
                    } else if (e.getKeyCode() == KeyEvent.VK_ENTER 
                            && (e.isControlDown() || e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())
                            && !scrubManager.isPreviewing()) {    
                        // End Recording
                        e.consume();
                        lastKeyWhen = e.getWhen();
                        processVideo();
                    } else if (e.getKeyCode() == KeyEvent.VK_M
                            && (e.isControlDown() || e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())
                            && isRecorderConfigSate()
                            && !scrubManager.isPreviewing()) {
                        // Open Settings Menu
                        e.consume();
                        lastKeyWhen = e.getWhen();
                        showSettingsForm();
                    } 
                }
                return false;
            }
        });
    }
    
    /**
     * Prints to current build number to console. Build number is automatically
     * updated per compilation. 
     */
    private void printBuildNumber() {
        try {
            log("\n\n==================================================\n" +
                    "Build Version: "+ resources.getString("BUILD") + "\n");
        } catch (MissingResourceException e) {
            log(e);
        }
    }
    
    /**
     * Detects operating system and loads any operating-system-specific 
     * procedures.
     */
    private void initOpSysEnvironments() {
        if (MediaUtil.osIsMac()){
            loadMacEnvironment();
        }
        else if (MediaUtil.osIsUnix()) {
            loadUnixEnvironment();
        }
        // Do nothing for Windows
    }
    
    /**
     * Mac OS X specific procedures.
     */
    private void loadMacEnvironment(){
        log("Loading Mac Environment");
        lblClockNonRec.setFont(new Font("SansSerif", 1, 11)); 
        lblClockRec.setFont(new Font("SansSerif", 1, 11)); 
    }
    
    /**
     * Linux specific procedures.
     */
    private void loadUnixEnvironment(){
        log("Loading Unix Environment");
        lblClockNonRec.setFont(new Font("SansSerif", 1, 10)); 
        lblClockRec.setFont(new Font("SansSerif", 1, 10)); 
        btnUpload.setFont(new Font("SansSerif", 1, 11)); 
        btnSaveAs.setFont(new Font("SansSerif", 1, 10)); 
        btnAccount.setFont(new Font("SansSerif", 1, 11)); 
        btnPlay.setFont(new Font("SansSerif", 1, 11)); 
        txtUrl.setFont(new Font("SansSerif", 1, 11)); 
    }
    
    /**
     * Handles display change for Recorder Panel. 
     * Display change event is detected when the Recorder Panel is dragged from 
     * one monitor to another. 
     */
    public void displayChangeHandler() {
        if (this.lastScreen == null) {
            this.lastScreen = this.getScreen();
        }
        
        // Only if moving to a different screen and the recorder is not recording
        if ((jpRecorderNonRec.isVisible() || jpRecorderRec.isVisible()) 
                && (!this.getScreen().getIDstring().equals(lastScreen.getIDstring()))
                && (!this.recorder.hasStatus(RecorderStatus.RECORDING)) 
                && recorder.getMillisecondsTime() == 0) {
            
            // Move capture box to new screen
            log(String.format("Moving from screen %s to %s",lastScreen.getIDstring(), this.getScreen().getIDstring()));
            this.recorder.setScreen(this.getScreen());
            this.lastScreen = this.getScreen();
            this.captureBox.destroy();
            this.captureBox = null;
            this.initCaptureBox();
        }
    }
    
    /**
     * If there is a previous screen capture detected, this procedure prompts 
     * the user to confirm if they wish to start recording by using the previous
     * recording.
     * @param recover true if recovering a backup video of the previous recording
     * @return true if user opts to start recording a previous recording
     */
    public boolean checkPrevVideo(boolean recover) {
        boolean approval = false;
        
        // If previous video is found and has not been uploaded
        if (this.recorder.checkStateSaved() && this.getRecorder().previousVideoExists()) {
            if (recover) {
                // Automatically resume from last recording to proceed to preview player
                approval = true;
            } else {
                approval = (JOptionPane.showConfirmDialog(this,
                        "A previous video exists. Do you want to continue from last recording?",
                        "Previous Video",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION);
            }
            
            if (approval) {
                // Set the state to pause and recreate process of opening and
                // closing the recorder until we open the preview player.
                recorder.setStatus(RecorderStatus.PAUSED);
                prepareForRerecord();
                ProcessPrevVideoThread thread = new ProcessPrevVideoThread();
                thread.start();
                pauseRecordState();
                captureBox.endBorderFlash();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    log(e);
                }
                showPreviewPlayer();
                captureBox.setDragBoxVisible(false);
                if (recover) {
                    // Proceed to finalize video of previous recording
                    this.scrubManager.finalizeVideo();
                }
            } else {
                // Disregard previous recording and start a new one
                this.getRecorder().cleanAndCreateFiles();
            }
        } else {
            // No previous video found
            if (recover) {
                // Attempt to recover an audio-less version of the previous
                // recording by checking for remnant screenshots.
                boolean shotsExist = this.getRecorder().checkExistingShots();
                if (shotsExist) {
                    approval = (JOptionPane.showConfirmDialog(this,
                            "No information on previously recorded video was found.\n"
                            + "However, temporary files are found which can be used to re-render an audio-less version of the video.\n"
                            + "Do you want to proceed with this action?",
                            "Recover Video",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION);
                    if (approval) {
                        try {
                            this.getRecorder().getVideoRecorder().compileVideoOnly();
                        } catch (IOException e) {
                            log(e);
                        }
                        JOptionPane.showMessageDialog(this, "Video file rendered to temp.mov on the screenbird directory", "Video Recovered", JOptionPane.INFORMATION_MESSAGE);
                        System.exit(0);
                    }
                    this.getRecorder().cleanAndCreateFiles();
                } else {
                    // Prompt the user to continue launch of nomral screen
                    // if no previous video screenshots were found
                    approval = (JOptionPane.showConfirmDialog(this,
                            "No previously recorded video was found on this computer for recovery.\n"
                            + "Continue to load a new screen recorder?",
                            "No Previous Video Found",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION);
                    
                    // User declines to load a new recorder so close the app
                    if (!approval) {
                        System.exit(0);
                    }   
                }
            }
            // Did not enter recovery mode so we start a clean recorder
            this.getRecorder().cleanAndCreateFiles();
        }
        return approval;
    }
    
    /**
     * Disables or enables the upload video form
     * @param value False if disable controls are to be locked
     */
    private void setEnableUploadForm(boolean value) {
        btnUpload.setEnabled(value);
        btnSaveAs.setEnabled(value);
        btnPlay.setEnabled(value);
    }

    /**
     * Used to see if the recorder is in a state which allows 
     * changes to settings or capture box. Ultimately, checks 
     * if the recorder is in a configurable state.
     * @return 
     */
    public boolean isRecorderConfigSate() {
        return !(isProcessingVideo 
                    || uploading 
                    || recorder.hasStatus(RecorderStatus.RECORDING) 
                    || recorder.hasStatus(RecorderStatus.PAUSED) 
                    || (countdownTimer!=null && countdownTimer.isRunning()))
                && !jpUpload.isVisible() && !jfSettings.isVisible();
    }
    
    /**
     * State of video capture encoding process
     * @return True if video is currently being encoded
     */
    public synchronized boolean isEncodingVideo() {
        return isProcessingVideo;
    }

    /**
     * Saves the video as an MP4 file in local drive.
     */
    private void saveCopy() {
        JFileChooser jfc = new JFileChooser();
        File sourceFile = new File(this.recordingOutput);
        String saveFileName = sourceFile.getName();
        
        jfc.setName("saveCopyDialog");
        
        // Replace saved video filename to its title, if any
        if (this.txtTitle.getText().length() > 0) {
            saveFileName = this.txtTitle.getText().replaceAll("[^a-zA-Z 0-9]+", "") + ".mp4";
        }
        
        jfc.setSelectedFile(new File(this.lastSavedLocation + saveFileName));
        File file = new File(this.lastSavedLocation + saveFileName);
        log(file.getAbsolutePath());
        jfc.setFileFilter(new MovFileFilter());
        int option = jfc.showSaveDialog(this);
        
        if (option == JFileChooser.APPROVE_OPTION) {
            if (jfc.getSelectedFile() != null) {
                File destinationFile = jfc.getSelectedFile();
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = new FileInputStream(sourceFile);
                    out = new FileOutputStream(destinationFile);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    saved = true;
                    this.lastSavedLocation = destinationFile.getPath().replace(destinationFile.getName(), "");
                    this.recorder.unmarkStateSaved();
                } catch (Exception ex) {
                    this.showUploadMessage("Error saving.", MESSAGE_ERROR);
                } finally {            
                    if (in != null) {
                        try{
                            in.close();
                        } catch (IOException e) {
                            log(e);
                        }
                    }
                    
                    if(out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            log(e);
                        }
                    }
                }
            } 
        }
    }

    /**
     * Launches upload thread.
     */
    private void uploadCopy() {
        pbEncoding.setVisible(true);
        retry = 1;
        
        // Reserved slug from Screenbird
        String reservedSlug = Session.getInstance().user.getSlug();
        
        if (reservedSlug.length() == 0){
            String fileName = (new File(recorder.getFile())).getName().replace(".mov", "");
            if (this.outputMovieSlug != null && (this.outputMovieSlug.length() == 0)) {
                this.outputMovieSlug = FileUtil.toBase36(Long.parseLong(fileName));
            }
        } else {
            this.outputMovieSlug = reservedSlug;
        }
        
        this.showVideoURL();
        uploadThread = new UploadThread();
        uploadThread.start();
    }
    
    /**
     * Closes the app
     * Checks if the app is busy. 
     * If the video was done but not uploaded, the app ask to the user to save or upload
     * If the app is working, the app ask to the user to be sure he want to cancel.
     * If closeNow == true, we bypass dialogs and directly close the application
     */
    public void closeApp(boolean closeNow) {
        log("Requested to close app");
        boolean approval = true;
        
        if (!closeNow && (btnUpload.isEnabled() && jpUpload.isVisible() && !saved)) {
            approval = false;
            Object[] options = {
                "Upload", 
                "Save", 
                "Close"
            };
            int reply = JOptionPane.showOptionDialog(this,
                    "Are you sure you want to exit before you upload or save your video?",
                    "Exit Screenbird",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (reply == 0) {
                uploadCopy();           // upload
            } else if (reply == 1) {
                saveCopy();             // save
            } else if (reply == 2) {
                approval = true;        // close
            }
        }
        
        if (closeNow || approval) {
            try {
                this.recorder.stopVideo();  
                if (!saved) {
                    this.recorder.stopAndSaveVideoState();
                    this.scrubManager.saveScrubFile();
                    this.recorder.markStateSaved();
                }
                
                try {
                    if (uploadThread != null) {
                        uploadThread.stop();
                    }
                } catch (NoClassDefFoundError e){
                    log(e);
                } catch (Exception e) {
                    log(e);
                } 

                this.captureBox.destroy();
                
                if (this.scrubManager != null) {
                    this.scrubManager.destroyPreviewVideo();
                }
                
                this.jfSettings.setVisible(false);
                this.jfSettings.dispose();
                ((JFrame) this.getParent().getParent().getParent().getParent()).dispose();
                
                // Closes application completely
                this.jfRecorderPanel.destroy();
                log("Closed app");
            } catch (Exception e) {
                log(e);
            }
        }
    }
    
    /**
     * Updates the the volume meter based on the calculated level of volume input.
     */
    public void updateVolumeLevel() {
        if (this.jpRecorderNonRec.isVisible()) {
            if (recorder.isLineOpen()) {
                pbVolumeNonRec.setIndeterminate(false);
                pbVolumeNonRec.setToolTipText("");
                soundBar1.setIndeterminate(false);
                soundBar1.setToolTipText("");

                 // For recording non state
                this.pbVolumeNonRec.setMaximum(0);
                this.pbVolumeNonRec.setMaximum(recorder.getMaxVolume());
                this.pbVolumeNonRec.setValue(recorder.getVolume());
                this.soundBar1.setMaximum(0);
                this.soundBar1.setMaximum(recorder.getMaxVolume());
                this.soundBar1.setValue(recorder.getVolume());

            } else {
                pbVolumeNonRec.setIndeterminate(true);
                pbVolumeNonRec.setToolTipText("Searching for audio line.");
                soundBar1.setIndeterminate(true);
                soundBar1.setToolTipText("Searching for audio line.");
            }
        }
        
        if (this.recorderPanelBG1.isVisible()) {
            if (recorder.isLineOpen()) {
                soundBar1.setIndeterminate(false);
                soundBar1.setToolTipText("");

                // For recording non state
                this.soundBar1.setMaximum(0);
                this.soundBar1.setMaximum(recorder.getMaxVolume());
                this.soundBar1.setValue(recorder.getVolume());
            } else {
                soundBar1.setIndeterminate(true);
                soundBar1.setToolTipText("Searching for audio line.");
            }
        }
        
        if (this.jpRecorderRec.isVisible()) {
            if (recorder.isLineOpen()) {
                pbVolumeRec.setIndeterminate(false);
                pbVolumeRec.setToolTipText("");

                // For recording state
                this.pbVolumeRec.setMaximum(0);
                this.pbVolumeRec.setMaximum(recorder.getMaxVolume());
                this.pbVolumeRec.setValue(recorder.getVolume());
            } else {
                pbVolumeRec.setIndeterminate(true);
                pbVolumeRec.setToolTipText("Searching for audio line.");
            }
        }
        
        if (this.jpRecorderBackup.isVisible()) {
            if (recorder.isLineOpen()) {
                pbVolumeBackup.setIndeterminate(false);
                pbVolumeBackup.setToolTipText("");

                // For recording state
                pbVolumeBackup.setMaximum(0);
                pbVolumeBackup.setMaximum(recorder.getMaxVolume());
                pbVolumeBackup.setValue(recorder.getVolume());
            } else {
                pbVolumeBackup.setIndeterminate(true);
                pbVolumeBackup.setToolTipText("Searching for audio line.");
            }
        }
    }
    
    /**
     * Updates recording clock.
     */
    private void configureClock() {
        clockTime = 0;
        clockTimer = new Timer(50, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                long timeInMS = totalTime;
                if (recorder.hasStatus(RecorderStatus.RECORDING)) {
                    long diff = System.currentTimeMillis() - currentTime;
                    timeInMS += diff;
                }
                setClockTime(timeInMS);
            }
        });
    }

    private void pauseClock() {
        this.totalTime += System.currentTimeMillis() - this.currentTime;
        clockTimer.stop();
    }

    private void startClock() {
        jLabel6.setForeground(Color.BLACK);
        soundBar1.setActive(true);
        this.currentTime = System.currentTimeMillis();
        clockTimer.start();
    }

    /**
     * Sets the time on the recorder clock.
     * @param timeMS 
     */
    private void setClockTime(long timeMS) {
        clockTime = timeMS;
        long time = timeMS / 1000;
        String format = String.format("%%0%dd", 2);
        
        // So we do not throw off any timing mechanisms
        // we just add the previous video duration time 
        // to display the clock
        if (this.clockOffset != 0) {
            time = this.clockOffset + time;
        }
        int intHour = (int) time / 3600;
        int intMins = (int) ((time % 3600) / 60);
        int intSecs = (int) (time % 60);

        String hours   = String.format(format, intHour);
        String mins    = String.format(format, intMins);
        String seconds = String.format(format, intSecs);
        
        String timeStr = String.format("%s:%s",mins,seconds);
        this.lblClockNonRec.setText(timeStr);
        this.lblClockRec.setText(timeStr);
        
        this.jLabel6.setText(timeStr);
        adjustClockTimerFont();
        
        this.lblClockBackup.setText(timeStr);
        
        if (Settings.ENABLE_OOPS_FEATURE) {
            this.scrubManager.updatePreviewController(timeMS);
        }
        
        checkUserTimeOut(intHour, intMins, intSecs);
    }
    
    /**
     * Set offset of time in Milliseconds
     * @param offset 
     */
    public void setClockOffset(Long offset) {
        log(String.format("CurrentOffset[%d] NewOffset[%d]", this.clockOffset, (offset)));
        this.clockOffset = (-1) * offset;
        this.setClockTime(this.clockTime);
    }

    /**
     * Checks if the user is near the time limit of 30 minutes per recording.
     * @param intHour
     * @param intMins
     * @param intSecs 
     */
    private void checkUserTimeOut(int intHour, int intMins, int intSecs) {
        if (Session.getInstance() == null) {
            if (this.checkTimeLimit(intHour, intMins, intSecs, Settings.MAX_RECORDING_TIME - 1)) {
                this.showRecorderMessage("You are near the 30 minute cutoff mark.", MESSAGE_ERROR);
            }
            if (this.checkTimeLimit(intHour, intMins, intSecs, Settings.MAX_RECORDING_TIME)) {
                this.processVideo();
            }
        }
    }
    
    /**
     * Checks if the user is near the given time limit.
     * @param hours current hours
     * @param mins  current minutes
     * @param secs  current seconds
     * @param threshold time limit in minutes
     * @return 
     */
    public boolean checkTimeLimit(int hours, int mins, int secs, int threshold) {
        int runningTime = (hours * 360) + (mins * 60) + secs;
        int thresholdTime = threshold * 60;
        return (runningTime >= thresholdTime);
    }
    
    /**
     * Starts the video recording and the timer.
     */
    public void start() {
        log("Starting recorder");
        recorder.recordVideo(this.isRecordingPreviousVideo);
        startClock();
    }
    
    /**
     * Pauses the video recording and the timer.
     */
    public void pause() {
        log("Pausing recorder");
        pauseClock();
        recorder.pauseVideo();
    }

    /**
     * Stops the video recording and timer.
     */
    public void stop() {
        log("Stoping recorder");
        recorder.stopVideo();
        clockTimer.stop();
    }
    
    /**
     * Shows the recorder in recording state.
     */
    private void startRecordState() {
        // Reset countdown
        this.countdownTimer = null;
        this.showRecordingState();
        this.start();
    }
    
    /**
     * Shows the recorder in paused state.
     */
    private void pauseRecordState() {
        this.btnRecordNonRec.setIcon(recordIcon);
        this.btnRecordNonRec.setRolloverIcon(recordIcon);
        
        jToggleButton3.setEnabled(false);
        jToggleButton3.setSelected(false);
        
        jToggleButton4.setEnabled(false);
        jToggleButton4.setSelected(false);
        
        this.repaint();
        this.pause();
    }
    
    /**
     * Processes all images captured to create a Quicktime movie.
     */
    public void prepareVideo() {
        this.showUploadForm();
        this.setEnableUploadForm(false);
        this.btnUpload.setEnabled(true);
        this.showUploadMessage("Processing Video...", MESSAGE_INFO);
        
        Boolean movieCreated = false;
        JFrame jf = (JFrame) RecorderPanel.this.getTopLevelAncestor();
        jf.setTitle("Screen Recorder - Processing Video...");
        
        // Place in encoding state
        this.isEncoding = true;
        this.pbEncoding.setIndeterminate(true);
        this.pbEncoding.setStringPainted(false);
        this.showUploadMessage("Rendering Video...", MESSAGE_INFO);
        
        // Converts screen shots to video file
        try {
            movieCreated = recorder.compileVideo();
        } catch (Exception e) {
            log(e);
        }
        
        // Converts audio clips to single audio file
        if (recorder.hasAudioToCompile()) {
            this.showUploadMessage("Rendering Audio...", MESSAGE_INFO);
            recorder.compileAudio();
            while (recorder.isWaitingForAudio()) {
                TimeUtil.skipToMyLou(0.1); // we need to wait for the audio file.
            }
        }
        
        // Check for ffmpeg executable on the computer
        // Download if not yet available
        DownloadThread ffmpegDownload = DownloadManager .
                getInstance().getDownload(Settings.getFFMpegExecutable());
        
        if (!ffmpegDownload.checkStatus(DownloadStatus.FINISHED)) {
            this.pbEncoding.setIndeterminate(true);
            this.pbEncoding.setStringPainted(false);
            this.showUploadMessage("Preparing Files", MESSAGE_INFO);
        }

        // Wait for ffmpeg download to finish
        while (!ffmpegDownload.checkStatus(DownloadStatus.FINISHED)) {
            TimeUtil.skipToMyLouMS(500L);
            log("Current Status :" + ffmpegDownload.getStatus());
        }
        
        
        this.pbEncoding.setIndeterminate(false);
        this.pbEncoding.setStringPainted(true);
        jf.setTitle("Screen Recorder - Done.");

        // Encode video/audio
        if (movieCreated) {
            try {
                int numOfTasks = 1;
                // Assign number of tasks via the operating system of the client
                if (recorder.hasPreviousCompiledVideo()) {
                    numOfTasks = (MediaUtil.osIsMac() || MediaUtil.osIsUnix()) ? 5 : 4;
                }
                if (this.scrubManager.isVideoEdited()) {
                    numOfTasks += (4 + this.scrubManager.getScrubs().size());
                }
                
                // Set up encoding progress bar
                this.pbEncodingListener = new FFMpegProgressBarListener(pbEncoding, numOfTasks, FFMpegProgressBarListener.FFMPEG);
                this.showUploadMessage("Encoding Video...", MESSAGE_INFO);
                
                // Encode the video
                this.recordingOutput = FileUtil.encodeVideoMp4(
                        recorder.getFile(), 
                        recorder.getOffset(), 
                        recorder.getBitRateCompresion(),
                        this.pbEncodingListener);
                
                // Append to previously rendered video if continuing
                if (recorder.hasPreviousCompiledVideo()) {
                    this.recordingOutput = FileUtil.mergeVideoMp4(
                            recorder.getPrevVideoFileName(), 
                            this.recordingOutput,
                            this.pbEncodingListener);
                }
                
                this.recorder.setResultVideoPath(this.recordingOutput);
                log("Compiled Video Location: " + this.recordingOutput);
            } catch (FileNotFoundException ex) {
                log(ex);
            } catch (IOException ex) {
                log(ex);
            } catch (UnsupportedBitRateCompression e){
                log(e);
            }
            
            // Disable scrub feature for resuming previous recordings
            if (this.scrubManager != null && this.scrubManager.isVideoEdited() &&
                    !this.recorder.hasPreviousCompiledVideo()) {
                // Give it a few seconds for the video to finish encoding
                TimeUtil.skipToMyLou(3);
                this.scrubManager.scrubVideo(this.recordingOutput, this.pbEncodingListener);
            }
            
            this.setEnableUploadForm(true);
            
            // Mark applet as a non-encoding state
            this.isEncoding = false;
            
            // Start uploading copy if auto-upload is checked
            if (this.chkAutoUpload.isSelected()) {
                // Disable checkbox since is useless after encoding is done
                this.chkAutoUpload.setEnabled(false);
                this.btnUpload.setEnabled(false);

                FileUtil.removeMarker(UPLOAD_ON_ENCODE);
                // Wait a few seconds for client's system to catch up
                TimeUtil.skipToMyLou(5);
                uploadCopy(); 
            }
            
            // Set progress bar to 100%
            this.pbEncoding.setValue(pbEncoding.getMaximum());
            this.showUploadMessage("Ready for upload", MESSAGE_OK);
        } else {
            this.showRecorderMessage("Error processing video.", MESSAGE_ERROR);
            log("Movie not compiled properly.");
        }
    }
    
    
    /**
     * Shows message in recorder panel
     * @param message
     * @param type 
     */
    public void showRecorderMessage(final String message, final Color type) {
        if (this.scrubManager != null) {
            this.scrubManager.endPreviewVideo();
        }

        // Update message text
        jpRecorderMessage.setText(message);
        jLabel5.setText(message);
        
        repaint();
    }
    
    /**
     * Shows recorder panel in recording sate
     */
    public void showRecordingState() {
        // Show recorder controls
        this.jfRecorderPanel.controlSetVisible(true);
        
        // Stop preview playback, if any
        if (this.scrubManager != null) {
            this.scrubManager.endPreviewVideo();
        }
        
        // Hide settings form
        if (jfSettings != null) {
            this.jfSettings.hideSettingsForm();
        }
        
        // Hide capture box
        if (this.captureBox != null) {
            this.setToggleStateCustomFullScreen(this.captureBox.getState(), false);
            this.captureBox.setDragBoxVisible(false);
        }

        this.jpUpload.setVisible(false);
        this.jpRecorderNonRec.setVisible(false);
        this.recorderPanelBG1.setVisible(true);
        this.jToggleButton1.setEnabled(false);
        this.jToggleButton2.setEnabled(false);
        this.jToggleButton3.setSelected(true);
        this.jLabel6.setForeground(new Color(148, 148, 148));
        this.jpRecorderRec.setVisible(false);
        
        jToggleButton3.setToolTipText("Stop");
        jToggleButton3.setEnabled(true);
        jToggleButton3.setIcon(new ImageIcon(getClass().getResource(ResourceUtil.STOP_BUTTON_NORMAL)));
        jToggleButton3.setSelectedIcon(new ImageIcon(getClass().getResource(ResourceUtil.STOP_BUTTON_PRESSED)));
        jToggleButton3.setDisabledIcon(new ImageIcon(getClass().getResource(ResourceUtil.STOP_BUTTON_DISABLED)));
        jToggleButton3.setDisabledSelectedIcon(new ImageIcon(getClass().getResource(ResourceUtil.STOP_BUTTON_DISABLED)));
        jToggleButton3.setRolloverIcon(new ImageIcon(getClass().getResource(ResourceUtil.STOP_BUTTON_HOVER)));
        jToggleButton3.setRolloverSelectedIcon(new ImageIcon(getClass().getResource(ResourceUtil.STOP_BUTTON_HOVER)));

        jToggleButton4.setEnabled(true);
        
        if (Settings.ENABLE_OOPS_FEATURE) {
            this.jfRecorderPanel.controlPack();
        } else {
            this.jpRecorderRec.setVisible(false);
        }
        
    }
    
    /**
     * Shows recorder form in non-recording state. Typically used when 
     * Screen Recorder first launches.
     */
    public void showRecorderForm() {
        if (this.scrubManager != null) {
            this.scrubManager.endPreviewVideo();
        }
        
        this.jfSettings.hideSettingsForm();
        this.jpUpload.setVisible(false);
        this.jpRecorderMessage.setVisible(false);
        
        if (this.captureBox != null) {
            this.setToggleStateCustomFullScreen(this.captureBox.getState(), this.captureBox.isVisible());
        }
        
        if (Settings.ENABLE_OOPS_FEATURE) {
            this.changeSize(NON_RECORDING_SIZE);
            this.jfRecorderPanel.controlPack();
            this.jpRecorderBackup.setVisible(false);
            this.jpRecorderRec.setVisible(false);
            this.jpRecorderNonRec.setVisible(false);
            this.recorderPanelBG1.setVisible(true);
        } else {
            log("Loading Old Screen Recorder");
            this.changeSize(BACKUP_RECORDING_SIZE);
            this.jfRecorderPanel.controlPack();
            this.jpRecorderNonRec.setVisible(false);
            this.recorderPanelBG1.setVisible(false);
            this.jpRecorderRec.setVisible(false);
            this.jpRecorderBackup.setVisible(true);
        }
        
        this.redrawWindow();
        this.revalidate();
    }
    
    /**
     * Displays the recorder settings form.
     */
    public void showSettingsForm() {
        this.jfSettings.loadSmartPosition();
        this.jpUpload.setVisible(false);
        this.jfSettings.showSettingsForm();
    }

    /**
     * Brings Preview Player to view and initiates the compiling of audio.
     */
    public void showPreviewPlayer() {
        // Starts compiling audio for previewing
        CompileAudioThread compileAudio = new CompileAudioThread();
        compileAudio.start();
        
        // Initialize the preview player
        this.scrubManager.updateScrubMaps();
        this.scrubManager.setPreviewTimeToStart();
        this.scrubManager.openPreviewPlayer();
        
        // Hide capture box
        if (this.captureBox != null && this.captureBox.isVisible()) {
            this.captureBox.setCaptureboxVisible(false, false, null);
        }
        
        // Hide recorder controls
        this.jfRecorderPanel.controlSetVisible(false);
        this.jpRecorderNonRec.setVisible(false);
        this.recorderPanelBG1.setVisible(false);
        this.jpRecorderRec.setVisible(false);
        this.jfSettings.hideSettingsForm();
        this.jpUpload.setVisible(false);
    }
    
    /**
     * Brings UploadForm to view for finalizing the encoding of video and 
     * uploading video to the web site.
     */
    private void showUploadForm() {
        // End video preview, if any
        if (this.scrubManager != null) {
            this.scrubManager.endPreviewVideo();
        }
        
        // Setup slug associated with the video
        if ((this.outputMovieSlug != null) && this.outputMovieSlug.length() == 0) {
            String reservedSlug = Session.getInstance().user.getSlug();
            if (reservedSlug.length() == 0) {
                this.outputMovieSlug = FileUtil.toBase36(recorder.getFileMS());
            } else {
                this.outputMovieSlug = reservedSlug;
            }
        }
        
        // Hide recorder controls
        this.jpRecorderNonRec.setVisible(false);
        this.recorderPanelBG1.setVisible(false);
        this.jpRecorderRec.setVisible(false);
        this.jfSettings.hideSettingsForm();
        this.jpUpload.setVisible(false);
        
        // Upload options
        this.chkAutoUpload.setOpaque(false);
        this.chkPublic.setOpaque(false);
        
        // Show upload form
        this.jpUpload.setVisible(true);
        
        // Initially hide upload form components
        this.lblUploadMessage.setVisible(false);
        this.txtUrl.setVisible(false);
        this.jLabel2.setVisible(false);
        this.btnCopy.setVisible(false);   
        
        // Free up the recorder controls from being attached to the capture box
        this.appMouseListener.setLocked(false);
        this.appMouseListener.setSnapArea(null);
        
        if (uploadUrlListener == null) {
            // Add click listener to video URL label
            this.txtUrl.removeMouseListener(uploadUrlListener);
            this.uploadUrlListener = new MouseMoveListener(this){
                @Override 
                public void mouseClicked(MouseEvent e) {
                    redirectOnUpload = false;
                    uploadLinkClickAction();
                }
            };
        }
        
        // Updates video data to upload form
        this.loadVideoData();
        this.changeSize(UPLOAD_SIZE);
        this.centerRecorderPanel();
        
        // Hide capture box
        if (this.captureBox != null) {
            this.captureBox.setCaptureboxVisible(false, true, false);
        }
        
        // Remove recorder panel from focus
        this.jfRecorderPanel.controlSetAlwaysOnTop(false);
    }
    
    /**
     * Opens the link in the default browser. Action performed when clicking on
     * the link box on the upload form.
     */
    private void uploadLinkClickAction() {
        // Only perform this iaction if the baseURL is set
        if (Session.getInstance().user.getBaseURL().length() > 0) {
            String browserURL = Session.getInstance().user.getBaseURL() + this.outputMovieSlug;
            MediaUtil.open(browserURL);
        }
    }
    
    /**
     * Update upload form with current video data.
     */
    private void loadVideoData() {
        // Construct default video title details
        SimpleDateFormat formatter = new SimpleDateFormat("MMMM d, h:mma");
        String formatedDate = formatter.format(new Date());
        this.txtTitle.setText(formatedDate.substring(0, 1).toUpperCase() + formatedDate.substring(1).toLowerCase());
        this.txtTitle.setBackground(new Color(255, 255, 204));
        
        // Construct default vide description details
        this.txtDescription.setText(DESCRIPTION_DEFAULT);
        this.txtDescription.setLineWrap(true);
        
        File file = new File(recorder.getFile());
        this.fileSize = file.length();
        this.chkPublic.setSelected(false);

        this.btnUpload.setEnabled(true);
        this.resetProgressBar();
        this.retry = MAX_UPLOAD_RETRY;
    }

    /*
     * Utility Instance View functions
     */

    /**
     * Updates Recorder Panel size so it can be packed nicely and cleanly
     * trim and excess blank spaces.
     * @param size 
     */
    private void changeSize(Dimension size) {
        this.setPreferredSize(size);
        this.setMinimumSize(size);
        this.setMaximumSize(size);
        this.setSize(size);
        this.revalidate();
    }
    
    /**
     * Redraws the recorder panel by hiding and showing it again.
     */
    private void redrawWindow() {
        this.setVisible(false);
        this.setVisible(true);
    }
    
    /**
     * Centers Recorder Panel vertically and horizontally dynamically with any
     * screen resolution. Pretty handy.
     */
    private void centerRecorderPanel() {
        Dimension screenSize = ScreenUtil.getScreenDimension(this.recorder.getScreen());
        Dimension appletSize = UPLOAD_SIZE;
        
        // Determines the cooridnates to center a form to the screen
        int x = screenSize.width;
        int y = screenSize.height;
        int w = appletSize.width;
        int h = appletSize.height;
        x = (x/2)-(w/2);
        y = (y/2)-(h/2);
        this.jfRecorderPanel.controlSetLocation(x, y);
        ((JFrame) this.getParent().getParent().getParent().getParent()).pack();
    }

    /**
     * Called after finished recording to initiate the Video encoding process
     */
    private void doneRecording() {
        // Destroys capturebox 
        if (this.captureBox != null) {
            this.captureBox.destroy();
        }
        
        // Destroys preview thread
        if (this.scrubManager != null) {
            this.scrubManager.destroyPreviewVideo();
        }
        
        this.btnRecordNonRec.setEnabled(true);
        this.jToggleButton3.setEnabled(true);
        
        // Stop the recording clock
        if (this.msgClock != null && this.msgClock.isRunning()) {
            this.msgClock.stop();
        }
        
        // Stop recording and prepare video for compilation
        if (!recorder.hasStatus(RecorderStatus.STOPPED)) {
            this.stop();
            this.prepareVideo();
        }
    }
    
    /*
     * Upload methods 
     */
    
    /**
     * Displays a message on the upload form, formatted with a text color.
     * @param message
     * @param type 
     */
    public void showUploadMessage(String message, Color type) {
        int start = 0;
        int end   = 0;
        Component focusedComponent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusedComponent instanceof JTextComponent) {
            end   = ((JTextComponent) focusedComponent).getSelectionEnd();
            start = ((JTextComponent) focusedComponent).getSelectionStart();
        }
        Dimension size = new Dimension(160, 14);
        
        this.lblUploadMessage.setPreferredSize(size);
        this.lblUploadMessage.setMinimumSize(size);
        this.lblUploadMessage.setMaximumSize(size);
        this.lblUploadMessage.setSize(size);
        this.pbEncoding.setVisible(true);
        this.pbEncoding.setForeground(type);
        this.lblUploadMessage.validate();
        this.lblUploadMessage.setText(message);
        this.lblUploadMessage.setVisible(true);
        this.lblUploadMessage.setForeground(type);
        
        ((JFrame) this.getParent().getParent().getParent().getParent()).pack();
        this.redrawWindow();
        if (focusedComponent instanceof JTextComponent) {
            ((JTextComponent) focusedComponent).grabFocus();
            ((JTextComponent) focusedComponent).setSelectionStart(start);
            ((JTextComponent) focusedComponent).setSelectionEnd(end);
        }
    }
    
    /**
     * Uploads the recorded video to Screenbird.
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private void uploadRecordedVideo() throws FileNotFoundException, IOException {
        // Send a blank description if default description isn't changed
        String desc = (this.txtDescription.getText().equals(DESCRIPTION_DEFAULT)) ? "" : this.txtDescription.getText();
        String checksum = FileUtil.getChecksum(this.recordingOutput);
        this.fileSize = (new File(this.recordingOutput)).length();
        
        // Reset progress bar to display upload progress
        this.resetProgressBar();
        this.showUploadMessage("Uploading...", MESSAGE_OK);
        
        this.listener = new ProgressBarUploadProgressListener(pbEncoding, fileSize);
        this.pbEncoding.setMinimum(0);
        this.pbEncoding.setMaximum(100);
        this.pbEncoding.setValue(0);

        // Sends a post request to Screenbird to upload the recorded video
        String[] success = FileUtil.postFile(this.recordingOutput,
                this.txtTitle.getText(),
                this.outputMovieSlug,
                desc,
                "mp4",
                checksum,
                this.chkPublic.isSelected(),
                this.pbEncoding,
                this.fileSize,
                Session.getInstance().user.getCsrfToken(),
                Session.getInstance().user.getUserId(),
                Session.getInstance().user.getChannelId(),
                Session.getInstance().user.getAnonToken(),
                this.listener,
                Session.getInstance().user.getBaseURL() + "video/upload");

        if (!((String) success[0]).equals("200")) {
            // Error encountered on upload
            throw new IOException("Status " + success[0]);
        } else {
            // Successful upload
            if (this.redirectOnUpload) {
                MediaUtil.open(Session.getInstance().user.getBaseURL()+this.outputMovieSlug);
            }
            this.showUploadMessage("Upload successful ", MESSAGE_OK);
            // Saving properties applies to uploaded videos
            this.saved = true;
            this.recorder.unmarkStateSaved();
        }
    }
    
    /**
     * Displays the screenbird URL on the upload form.
     */
    private void showVideoURL() {
        this.txtUrl.setText(Session.getInstance().user.getBaseURL()+this.outputMovieSlug);
        this.txtUrl.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.txtUrl.addMouseListener(uploadUrlListener);
        this.txtUrl.setVisible(true);
        this.jLabel2.setVisible(true);
        this.btnCopy.setVisible(true);
        this.btnCopy.setEnabled(true);
        ((JFrame) this.getParent().getParent().getParent().getParent()).pack();
        this.redrawWindow();
    }
    
    /**
     * Clears current progress bar information and resets it to zero.
     */
    private void resetProgressBar() {
        this.pbEncoding.setMinimum(0);
        this.pbEncoding.setMaximum(100);
        this.pbEncoding.setValue(0);
    }

    /**
     * Handles uploading of the recorded video. 
     */
    private void uploadFile() {
        // Show the title as warning if left empty or exceeds 250 characters
        if (this.txtTitle.getText().length() == 0 || this.txtTitle.getText().length() > 250) {
            this.txtTitle.setBackground(Color.RED);
        } else {
            try {
                txtTitle.setBackground(new Color(255, 255, 204));
                // Disable upload to prevent multiple submissions
                btnUpload.setEnabled(false);
                
                // Disable edits for title, description and public/not public.
                this.txtTitle.setEnabled(false);
                this.txtDescription.setEnabled(false);
                this.chkAutoUpload.setEnabled(false);
                this.chkPublic.setEnabled(false);
                
                // Upload to Screenbird
                uploadRecordedVideo();
            } catch (IOException ex) {
                // Catch exception thrown if response of upload request
                // is not HTTP 200.
                try {
                    if (retry > 0) {
                        log(ex.getMessage());
                        retry--;
                        this.showUploadMessage("Error, retrying uploading.", MESSAGE_ERROR);
                        this.redrawWindow();
                        // retries the upload
                        uploadFile();
                    } else {
                        btnSaveAs.setVisible(true);
                        btnUpload.setEnabled(true);
                        this.showUploadMessage("Error uploading.", MESSAGE_ERROR);
                        
                        // Disable edits for title, description and public/not public.
                        this.txtTitle.setEnabled(false);
                        this.txtDescription.setEnabled(false);
                        this.chkAutoUpload.setEnabled(false);
                        this.chkPublic.setEnabled(false);
                        
                        this.redrawWindow();
                    }
                } catch (Exception e) {
                    log(e);
                }
            }
        }
    }
    
    /**
     * Prepares the recorder controls for recording when entering from a paused
     * state.
     */
    public void prepareForRerecord() {
        if (this.recorder.hasStatus(RecorderStatus.PAUSED)) {
            this.jToggleButton3.setSelected(false);
            this.jLabel5.setText("Ready to resume");
            
            jToggleButton1.setEnabled(false);
            jToggleButton2.setEnabled(false);
            
            jToggleButton3.setToolTipText("Record");
            jToggleButton3.setIcon(new ImageIcon(getClass().getResource(ResourceUtil.RECORD_BUTTON_NORMAL)));
            jToggleButton3.setSelectedIcon(new ImageIcon(getClass().getResource(ResourceUtil.RECORD_BUTTON_PRESSED)));
            jToggleButton3.setDisabledIcon(new ImageIcon(getClass().getResource(ResourceUtil.RECORD_BUTTON_DISABLED)));
            jToggleButton3.setDisabledSelectedIcon(new ImageIcon(getClass().getResource(ResourceUtil.RECORD_BUTTON_DISABLED)));
            jToggleButton3.setRolloverIcon(new ImageIcon(getClass().getResource(ResourceUtil.RECORD_BUTTON_HOVER)));
            jToggleButton3.setRolloverSelectedIcon(new ImageIcon(getClass().getResource(ResourceUtil.RECORD_BUTTON_HOVER)));
            
            jToggleButton3.setEnabled(true);
            jToggleButton3.setSelected(false);
            jToggleButton3.getModel().setEnabled(true);
            
            jToggleButton4.setEnabled(false);
            jToggleButton4.setSelected(false);
            
            if (this.captureBox != null) {
                if (this.captureBox.getState() == CaptureBoxState.CUSTOM_SCREEN) {
                    this.setToggleStateCustomFullScreen(this.captureBox.getState(),true);
                    this.captureBox.setDragBoxVisible(true);
                    this.captureBox.setLockCapturebox(true);
                }
            }
        }
    }

    /**
     * Depending on the state of the screen capture, this procedure will either
     * Pause or Start the screen capture. <BR/>
     * - If Recording, this procedure will Pause <BR/>
     * - If Paused, this procedure will start recording
     */
    public void recordOrPause(boolean showPreviewPlayerOnPause) {
        // Only if in recordable state
        if (jpRecorderRec.isVisible() || jpRecorderNonRec.isVisible() ||
                (!Settings.ENABLE_OOPS_FEATURE && jpRecorderBackup.isVisible()) || 
                this.recorderPanelBG1.isVisible()) {
            
            // If recording
            if (this.recorder.hasStatus(RecorderStatus.RECORDING)) {
                pauseRecordState();
                captureBox.endBorderFlash();
                
                if (showPreviewPlayerOnPause) {
                    if (Settings.ENABLE_OOPS_FEATURE) {
                        showPreviewPlayer();
                    } else {
                        this.btnPlayPauseBackup.setIcon(recordIcon);
                    }
                } else {
                    prepareForRerecord();
                }
            } else {
                // If have not started recording
                if (recorder.getMillisecondsTime() == 0) {
                    // Check if resuming recording
                    if (!this.recorder.hasPreviousCompiledVideo()) {
                        log("Deleting old Files on Record");
                        recorder.deleteFiles();
                    } else {
                        // Keeping files for later
                        log("Holding onto previous files");
                    }
                    
                    this.recorder.setSelectedMode(this.jfSettings.getPanel().getVideoQualitySlider().getValue());
                    this.jfSettings.setEnabled(false);
                }
                
                // Moving this outside of a thread
                if (!this.recorder.isLineOpen()) {
                    // Initiate audio grab
                    this.monitorAudioLine();
                } else {
                    log("Audio line already open");
                }
                
                captureBox.beginBorderFlash();
                startCountdown();
            }
        }
    }
    
    /**
     * Configures and starts the Audio Line Monitor to keep the audio line running
     * or if it is down, to try to turn the line on, up to <cref>MAX_AUDIO_DROP</cref>
     * attempts.
     */
    public final void monitorAudioLine() {
        // Prevent duplicate monitors from initiating 
        if (this.recorder.getAudioLineMonitor() != null && this.recorder.getAudioLineMonitor().isRunning()) {
            log("Warning: attempted to instantiate another audioLineMonitor.");
            return;
        }
        
        final RecorderPanel recorderPanel = this;
        
        this.recorder.setAudioLineMonitor(new Timer(100, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean isLineOpen = recorder.isLineOpen();
                recorder.getAudioRecorder().monitorOpenLine();
                
                if (isLineOpen) {
                    if (audioRecovering) {
                        JOptionPane.showMessageDialog(recorderPanel,
                                "Connection to the audio input was restored.\n" +
                                "You can continue recording.",
                                "Screenbird", JOptionPane.INFORMATION_MESSAGE);
                        prepareForRerecord();
                        audioRecovering = false;
                    }
                    // Reset drop count
                    recorder.setCurrentDropCount(0);
                    recorder.getAudioLineMonitor().setDelay(100);
                } else {
                    if (recorder.isDroppedCountReached()) {
                        if (audioRecovering) {
                            JOptionPane.showMessageDialog(recorderPanel,
                                    "Screenbird was not able to recover your audio within the given time.\n" +
                                    "You can continue recording without audio however.",
                                    "Screenbird", JOptionPane.ERROR_MESSAGE);
                            prepareForRerecord();
                            audioRecovering = false;
                        }
                        
                        log("Giving up on audio... entire recording will be silent.");
                        recorder.getAudioLineMonitor().stop();
                    } else {
                        log("Attempting to access Audio: " + recorder.getCurrentDropCount());
                        
                        if (!audioRecovering && recorder.hasStatus(RecorderStatus.RECORDING)) {
                            // Pause the current recording, then show a warning to the
                            // user regarding their audio line getting cut off.
                            pauseRecordState();
                            
                            JOptionPane.showMessageDialog(recorderPanel,
                                    "Connection to the audio input was lost.\n\n" +
                                    "Screenbird will attempt to recover the audio line.\n" +
                                    "Once recovered, you will be notified.\n\nStandby...",
                                    "Screenbird", JOptionPane.WARNING_MESSAGE);
                            
                            audioRecovering = true;
                        }

                        // Use exponential backoff.
                        recorder.setCurrentDropCount(recorder.getCurrentDropCount() + 1);
                        recorder.getAudioLineMonitor().setDelay(100 * (int)Math.pow(2, recorder.getCurrentDropCount()));
                    }
                }
            }
        }));
        this.recorder.getAudioLineMonitor().start();
    }

    /**
     * Initiates countdown and prepares for screen capture. 
     */
    private void startCountdown() {
        final Countdown[] countdown = new Countdown[1];
        
        countdownSec = 6;
        countdownTimer = new Timer(500, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                countdownSec--;
                log("Counting: " + countdownSec);
                
                switch (countdownSec) {
                    case 0:
                        countdownTimer.stop();
                        
                        jLabel5.setText("Recording");
                        btnRecordNonRec.setText("Stop");
                        btnRecordRec.setText("Stop");
                        
                        btnPlayPauseBackup.setText("");
                        btnPlayPauseBackup.setIcon(pauseIcon);
                        btnFinalizeBackup.setEnabled(true);
                        
                        // Destroy the countdown window.
                        countdown[0].destroy();
                        
                        try {
                            Thread.sleep(500);
                        } catch (Exception ee) {
                        }
                        startRecordState();
                        break;
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                        btnPlayPauseBackup.setText(String.valueOf(countdownSec));
                        
                        countdown[0].destroy();
                        countdown[0] = new Countdown(false, recorder);
                        countdown[0].setCount(countdownSec);
                        countdown[0].setVisible(true);
                        
                        try {
                            SoundUtil.tone(880, 500, 0.15);
                        } catch(LineUnavailableException ee){
                        }
                        
                        break;
                    case 5:
                        btnPlayPauseBackup.setText(String.valueOf(countdownSec));
                        
                        // Hide drop box
                        if (captureBox != null) {
                            captureBox.setDragBoxVisible(false);
                        }
                        
                        countdown[0] = new Countdown(false, recorder);
                        countdown[0].setCount(5);
                        countdown[0].setVisible(true);
                        
                        try {
                            SoundUtil.tone(880, 500, 0.15);
                        } catch (LineUnavailableException ee) {
                        }
                        break;
                }
            }
        });
        countdownTimer.start();
    }
    
    /*
     * Procedures for handling Capturebox toggling
     */
    
    /**
     * Depending on current state of screen capture size, the opposite is 
     * activated.
     */
    private void toggleCustomFullscreen() {
        if (Settings.ENABLE_OOPS_FEATURE) {
            // With oops feature
            if (this.toggleFullScreen.isSelected()) {
                setToggleStateCustomFullScreen(CaptureBoxState.FULLSCREEN);
            } else {
                setToggleStateCustomFullScreen(CaptureBoxState.CUSTOM_SCREEN);
            }
        } else {
            // Without oops feature
            if (this.toggleFullScreenBackup.isSelected()) {
                setToggleStateCustomFullScreen(CaptureBoxState.FULLSCREEN);
            } else {
                setToggleStateCustomFullScreen(CaptureBoxState.CUSTOM_SCREEN);
            }
        }
    }
    
    /**
     * Activates given screen capture size state with DragBox visible.
     * @param captureBoxState 
     */
    private void setToggleStateCustomFullScreen(CaptureBoxState captureBoxState) {
        setToggleStateCustomFullScreen(captureBoxState, true);
    }
    
    /**
     * Sets the state of screen capture size
     * @param captureBoxState State of Capturebox
     * @param isDragBoxVisible True if DragBox is to be visible
     */
    private void setToggleStateCustomFullScreen(final CaptureBoxState captureBoxState, final boolean isDragBoxVisible) {
        
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                if (captureBoxState == CaptureBoxState.FULLSCREEN) {
                    captureBox.setState(CaptureBoxState.FULLSCREEN);
                    log("Setting State Full Screen");
                    
                    toggleFullScreen.setText("Fullscreen");
                    toggleFullScreen.setSelected(true);
                    
                    toggleFullScreenBackup.setText("Fullscreen");
                    toggleFullScreenBackup.setSelected(true);
                    
                    captureBox.setCaptureboxVisible(false, true, false);
                    
                    jToggleButton2.setSelected(true);
                    jToggleButton1.setSelected(false);
                } else if (captureBoxState == CaptureBoxState.CUSTOM_SCREEN) {
                    
                    log("Setting State Custom Screen");
                    
                    captureBox.setState(CaptureBoxState.CUSTOM_SCREEN);
                    captureBox.setCaptureboxVisible(true, true, isDragBoxVisible);
                    
                    toggleFullScreen.setText("Custom");
                    toggleFullScreen.setSelected(false);
                    
                    toggleFullScreenBackup.setText("Custom");
                    toggleFullScreenBackup.setSelected(false);
                    
                    jToggleButton2.setSelected(false);
                    jToggleButton1.setSelected(true);
                }
            }
        });
        
    }
    
    public void log(Object message) {
        LogUtil.log(RecorderPanel.class, message);
    }
    
    /**
     * Processes video for compilation and encoding.
     */
    public void processVideo() {
        ProcessVideoThread processVideoThread = new ProcessVideoThread();
        isProcessingVideo = true;
        processVideoThread.start();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpRecorderNonRec = new JRoundedPanel();
        lblClockNonRec = new JLabel();
        btnRecordNonRec = new JButton();
        btnCancelNonRec = new JButton();
        pbVolumeNonRec = new JProgressBar();
        toggleFullScreen = new JToggleButton();
        btnMinimizeNonRec = new JButton();
        jpRecorderMessage = new RecorderMessage();
        jpRecorderRec = new JRoundedPanel();
        lblClockRec = new JLabel();
        btnRecordRec = new JButton();
        pbVolumeRec = new JProgressBar();
        btnMinimizeRec = new JButton();
        jpRecorderBackup = new JRoundedPanel();
        lblClockBackup = new JLabel();
        btnFinalizeBackup = new JButton();
        btnCancelBackup = new JButton();
        pbVolumeBackup = new JProgressBar();
        toggleFullScreenBackup = new JToggleButton();
        jpRecorderMessageBackup = new RecorderMessage();
        btnMinimizeBackup1 = new JButton();
        btnPlayPauseBackup = new JButton();
        jpUpload = new JRoundedPanel();
        txtUrl = new JTextField();
        jLabel3 = new JLabel();
        btnPlay = new JButton();
        jScrollPane2 = new JScrollPane();
        txtDescription = new JTextArea();
        chkAutoUpload = new JCheckBox();
        chkPublic = new JCheckBox();
        lblUploadMessage = new JLabel();
        jLabel1 = new JLabel();
        jLabel8 = new JLabel();
        pbEncoding = new JProgressBar();
        btnUpload = new JButton();
        btnSaveAs = new JButton();
        btnCopy = new JButton();
        jLabel2 = new JLabel();
        txtTitle = new JTextField();
        btnAccount = new JButton();
        jPanel3 = new JPanel();
        btnCancel = new JButton();
        btnMinimize1 = new JButton();
        jLabel4 = new JLabel();
        recorderPanelBG1 = new RecorderPanelBG();
        jButton1 = new JButton();
        jButton2 = new JButton();
        jLabel5 = new JLabel();
        jToggleButton1 = new JToggleButton();
        jToggleButton2 = new JToggleButton();
        jLabel6 = new JLabel();
        jToggleButton3 = new JToggleButton();
        soundBar1 = new SoundBar();
        jToggleButton4 = new JToggleButton();

        FormListener formListener = new FormListener();

        setMaximumSize(new Dimension(1363, 45));
        setMinimumSize(new Dimension(378, 45));
        setName("RecorderPanel"); // NOI18N
        setOpaque(false);
        setPreferredSize(new Dimension(579, 1000));
        setRequestFocusEnabled(false);

        jpRecorderNonRec.setMaximumSize(new Dimension(333, 44));
        jpRecorderNonRec.setMinimumSize(new Dimension(333, 44));
        jpRecorderNonRec.setPreferredSize(new Dimension(333, 44));
        jpRecorderNonRec.setLayout(new AbsoluteLayout());

        lblClockNonRec.setBackground(Color.darkGray);
        lblClockNonRec.setFont(new Font("Lucida Grande", 1, 14)); // NOI18N
        lblClockNonRec.setForeground(new Color(255, 255, 255));
        lblClockNonRec.setHorizontalAlignment(SwingConstants.CENTER);
        lblClockNonRec.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/timer.png"))); // NOI18N
        lblClockNonRec.setText("00:00");
        lblClockNonRec.setHorizontalTextPosition(SwingConstants.CENTER);
        lblClockNonRec.setIconTextGap(0);
        lblClockNonRec.setMaximumSize(new Dimension(54, 24));
        lblClockNonRec.setMinimumSize(new Dimension(54, 24));
        lblClockNonRec.setOpaque(true);
        lblClockNonRec.setPreferredSize(new Dimension(54, 24));
        jpRecorderNonRec.add(lblClockNonRec, new AbsoluteConstraints(100, 10, -1, 25));

        btnRecordNonRec.setBackground(Color.darkGray);
        btnRecordNonRec.setFont(new Font("Lucida Grande", 1, 12)); // NOI18N
        btnRecordNonRec.setForeground(new Color(255, 255, 255));
        btnRecordNonRec.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/redButton.png"))); // NOI18N
        btnRecordNonRec.setText("Record");
        btnRecordNonRec.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 2));
        btnRecordNonRec.setHorizontalTextPosition(SwingConstants.CENTER);
        btnRecordNonRec.setName("btnRecordNonRec"); // NOI18N
        btnRecordNonRec.addActionListener(formListener);
        jpRecorderNonRec.add(btnRecordNonRec, new AbsoluteConstraints(200, 10, 66, 24));

        btnCancelNonRec.setBackground(Color.darkGray);
        btnCancelNonRec.setForeground(new Color(255, 255, 255));
        btnCancelNonRec.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/x.png"))); // NOI18N
        btnCancelNonRec.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        btnCancelNonRec.setDoubleBuffered(true);
        btnCancelNonRec.setHorizontalTextPosition(SwingConstants.CENTER);
        btnCancelNonRec.setMaximumSize(new Dimension(24, 24));
        btnCancelNonRec.setMinimumSize(new Dimension(24, 24));
        btnCancelNonRec.setName("btnCancelRecorder"); // NOI18N
        btnCancelNonRec.setPreferredSize(new Dimension(24, 24));
        btnCancelNonRec.setPressedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/x.png"))); // NOI18N
        btnCancelNonRec.setRolloverIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/x.png"))); // NOI18N
        btnCancelNonRec.addActionListener(formListener);
        jpRecorderNonRec.add(btnCancelNonRec, new AbsoluteConstraints(300, 10, -1, -1));

        pbVolumeNonRec.setBackground(Color.darkGray);
        pbVolumeNonRec.setForeground(new Color(102, 255, 102));
        pbVolumeNonRec.setMaximum(3700);
        pbVolumeNonRec.setToolTipText("Volume Level");
        pbVolumeNonRec.setBorderPainted(false);
        pbVolumeNonRec.setMaximumSize(new Dimension(47, 24));
        pbVolumeNonRec.setMinimumSize(new Dimension(27, 24));
        pbVolumeNonRec.setPreferredSize(new Dimension(47, 24));
        jpRecorderNonRec.add(pbVolumeNonRec, new AbsoluteConstraints(160, 10, 30, 25));

        toggleFullScreen.setBackground(Color.darkGray);
        toggleFullScreen.setForeground(Color.white);
        toggleFullScreen.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/button-large-unpressed.png"))); // NOI18N
        toggleFullScreen.setSelected(true);
        toggleFullScreen.setText("Fullscreen");
        toggleFullScreen.setBorder(null);
        toggleFullScreen.setBorderPainted(false);
        toggleFullScreen.setContentAreaFilled(false);
        toggleFullScreen.setFocusable(false);
        toggleFullScreen.setHorizontalTextPosition(SwingConstants.CENTER);
        toggleFullScreen.setIconTextGap(0);
        toggleFullScreen.setName("toggleFullScreen"); // NOI18N
        toggleFullScreen.setPressedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/button-large-pressed.png"))); // NOI18N
        toggleFullScreen.setSelectedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/button-large-pressed.png"))); // NOI18N
        toggleFullScreen.addActionListener(formListener);
        jpRecorderNonRec.add(toggleFullScreen, new AbsoluteConstraints(10, 10, -1, 25));
        toggleFullScreen.getAccessibleContext().setAccessibleDescription("Change between Fullscreen and Custom");

        btnMinimizeNonRec.setBackground(Color.darkGray);
        btnMinimizeNonRec.setForeground(new Color(255, 255, 255));
        btnMinimizeNonRec.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/min.png"))); // NOI18N
        btnMinimizeNonRec.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        btnMinimizeNonRec.setDoubleBuffered(true);
        btnMinimizeNonRec.setHorizontalTextPosition(SwingConstants.CENTER);
        btnMinimizeNonRec.setMaximumSize(new Dimension(24, 24));
        btnMinimizeNonRec.setMinimumSize(new Dimension(24, 24));
        btnMinimizeNonRec.setName("btnMinimizeRecorder"); // NOI18N
        btnMinimizeNonRec.setPreferredSize(new Dimension(24, 24));
        btnMinimizeNonRec.setPressedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/min.png"))); // NOI18N
        btnMinimizeNonRec.setRolloverIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/min.png"))); // NOI18N
        btnMinimizeNonRec.addActionListener(formListener);
        jpRecorderNonRec.add(btnMinimizeNonRec, new AbsoluteConstraints(270, 10, -1, -1));
        jpRecorderNonRec.add(jpRecorderMessage, new AbsoluteConstraints(10, 50, 310, 40));

        jpRecorderRec.setMaximumSize(new Dimension(244, 44));
        jpRecorderRec.setMinimumSize(new Dimension(244, 44));
        jpRecorderRec.setPreferredSize(new Dimension(244, 44));
        jpRecorderRec.setLayout(new AbsoluteLayout());

        lblClockRec.setBackground(Color.darkGray);
        lblClockRec.setFont(new Font("Lucida Grande", 1, 14)); // NOI18N
        lblClockRec.setForeground(new Color(255, 255, 255));
        lblClockRec.setHorizontalAlignment(SwingConstants.CENTER);
        lblClockRec.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/timer.png"))); // NOI18N
        lblClockRec.setText("00:00");
        lblClockRec.setHorizontalTextPosition(SwingConstants.CENTER);
        lblClockRec.setIconTextGap(0);
        lblClockRec.setMaximumSize(new Dimension(54, 24));
        lblClockRec.setMinimumSize(new Dimension(54, 24));
        lblClockRec.setOpaque(true);
        lblClockRec.setPreferredSize(new Dimension(54, 24));
        jpRecorderRec.add(lblClockRec, new AbsoluteConstraints(10, 10, -1, 25));

        btnRecordRec.setBackground(Color.darkGray);
        btnRecordRec.setFont(new Font("Lucida Grande", 1, 12)); // NOI18N
        btnRecordRec.setForeground(new Color(255, 255, 255));
        btnRecordRec.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/redButton.png"))); // NOI18N
        btnRecordRec.setText("Stop");
        btnRecordRec.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 2));
        btnRecordRec.setHorizontalTextPosition(SwingConstants.CENTER);
        btnRecordRec.setName("btnRecord"); // NOI18N
        btnRecordRec.addActionListener(formListener);
        jpRecorderRec.add(btnRecordRec, new AbsoluteConstraints(110, 10, 66, 24));

        pbVolumeRec.setBackground(Color.darkGray);
        pbVolumeRec.setForeground(new Color(102, 255, 102));
        pbVolumeRec.setMaximum(3700);
        pbVolumeRec.setToolTipText("Volume Level");
        pbVolumeRec.setBorderPainted(false);
        pbVolumeRec.setMaximumSize(new Dimension(47, 24));
        pbVolumeRec.setMinimumSize(new Dimension(27, 24));
        pbVolumeRec.setPreferredSize(new Dimension(47, 24));
        jpRecorderRec.add(pbVolumeRec, new AbsoluteConstraints(70, 10, 30, 25));

        btnMinimizeRec.setBackground(Color.darkGray);
        btnMinimizeRec.setForeground(new Color(255, 255, 255));
        btnMinimizeRec.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/min.png"))); // NOI18N
        btnMinimizeRec.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        btnMinimizeRec.setDoubleBuffered(true);
        btnMinimizeRec.setHorizontalTextPosition(SwingConstants.CENTER);
        btnMinimizeRec.setMaximumSize(new Dimension(24, 24));
        btnMinimizeRec.setMinimumSize(new Dimension(24, 24));
        btnMinimizeRec.setName("btnMinimizeRecorder"); // NOI18N
        btnMinimizeRec.setPreferredSize(new Dimension(24, 24));
        btnMinimizeRec.setPressedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/min.png"))); // NOI18N
        btnMinimizeRec.setRolloverIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/min.png"))); // NOI18N
        btnMinimizeRec.addActionListener(formListener);
        jpRecorderRec.add(btnMinimizeRec, new AbsoluteConstraints(180, 10, -1, -1));

        jpRecorderBackup.setMaximumSize(new Dimension(333, 44));
        jpRecorderBackup.setMinimumSize(new Dimension(333, 44));
        jpRecorderBackup.setPreferredSize(new Dimension(333, 44));
        jpRecorderBackup.setLayout(new AbsoluteLayout());

        lblClockBackup.setBackground(Color.darkGray);
        lblClockBackup.setFont(new Font("Lucida Grande", 1, 14)); // NOI18N
        lblClockBackup.setForeground(new Color(255, 255, 255));
        lblClockBackup.setHorizontalAlignment(SwingConstants.CENTER);
        lblClockBackup.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/timer.png"))); // NOI18N
        lblClockBackup.setText("00:00");
        lblClockBackup.setHorizontalTextPosition(SwingConstants.CENTER);
        lblClockBackup.setIconTextGap(0);
        lblClockBackup.setMaximumSize(new Dimension(54, 24));
        lblClockBackup.setMinimumSize(new Dimension(54, 24));
        lblClockBackup.setOpaque(true);
        lblClockBackup.setPreferredSize(new Dimension(54, 24));
        jpRecorderBackup.add(lblClockBackup, new AbsoluteConstraints(100, 10, -1, 25));

        btnFinalizeBackup.setBackground(Color.darkGray);
        btnFinalizeBackup.setFont(new Font("Lucida Grande", 1, 12)); // NOI18N
        btnFinalizeBackup.setForeground(new Color(255, 255, 255));
        btnFinalizeBackup.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/check-red.png"))); // NOI18N
        btnFinalizeBackup.setEnabled(false);
        btnFinalizeBackup.setHorizontalTextPosition(SwingConstants.CENTER);
        btnFinalizeBackup.setMaximumSize(new Dimension(24, 24));
        btnFinalizeBackup.setMinimumSize(new Dimension(24, 24));
        btnFinalizeBackup.setName("btnFinalizeBackup"); // NOI18N
        btnFinalizeBackup.setPreferredSize(new Dimension(24, 24));
        btnFinalizeBackup.setPressedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/check-red-pressed.png"))); // NOI18N
        btnFinalizeBackup.addActionListener(formListener);
        jpRecorderBackup.add(btnFinalizeBackup, new AbsoluteConstraints(230, 10, 24, 24));

        btnCancelBackup.setBackground(Color.darkGray);
        btnCancelBackup.setForeground(new Color(255, 255, 255));
        btnCancelBackup.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/x.png"))); // NOI18N
        btnCancelBackup.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        btnCancelBackup.setDoubleBuffered(true);
        btnCancelBackup.setHorizontalTextPosition(SwingConstants.CENTER);
        btnCancelBackup.setMaximumSize(new Dimension(24, 24));
        btnCancelBackup.setMinimumSize(new Dimension(24, 24));
        btnCancelBackup.setName("btnCancelRecorder"); // NOI18N
        btnCancelBackup.setPreferredSize(new Dimension(24, 24));
        btnCancelBackup.setPressedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/x.png"))); // NOI18N
        btnCancelBackup.setRolloverIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/x.png"))); // NOI18N
        btnCancelBackup.addActionListener(formListener);
        jpRecorderBackup.add(btnCancelBackup, new AbsoluteConstraints(290, 10, -1, -1));

        pbVolumeBackup.setBackground(Color.darkGray);
        pbVolumeBackup.setForeground(new Color(102, 255, 102));
        pbVolumeBackup.setMaximum(3700);
        pbVolumeBackup.setToolTipText("Volume Level");
        pbVolumeBackup.setBorderPainted(false);
        pbVolumeBackup.setMaximumSize(new Dimension(47, 24));
        pbVolumeBackup.setMinimumSize(new Dimension(27, 24));
        pbVolumeBackup.setPreferredSize(new Dimension(47, 24));
        jpRecorderBackup.add(pbVolumeBackup, new AbsoluteConstraints(160, 10, 30, 25));

        toggleFullScreenBackup.setBackground(Color.darkGray);
        toggleFullScreenBackup.setForeground(Color.white);
        toggleFullScreenBackup.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/button-large-unpressed.png"))); // NOI18N
        toggleFullScreenBackup.setSelected(true);
        toggleFullScreenBackup.setText("Fullscreen");
        toggleFullScreenBackup.setBorder(null);
        toggleFullScreenBackup.setBorderPainted(false);
        toggleFullScreenBackup.setContentAreaFilled(false);
        toggleFullScreenBackup.setFocusable(false);
        toggleFullScreenBackup.setHorizontalTextPosition(SwingConstants.CENTER);
        toggleFullScreenBackup.setIconTextGap(0);
        toggleFullScreenBackup.setName("toggleFullScreen"); // NOI18N
        toggleFullScreenBackup.setPressedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/button-large-pressed.png"))); // NOI18N
        toggleFullScreenBackup.setSelectedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/button-large-pressed.png"))); // NOI18N
        toggleFullScreenBackup.addActionListener(formListener);
        jpRecorderBackup.add(toggleFullScreenBackup, new AbsoluteConstraints(10, 10, -1, 25));
        jpRecorderBackup.add(jpRecorderMessageBackup, new AbsoluteConstraints(10, 50, 310, 30));

        btnMinimizeBackup1.setBackground(Color.darkGray);
        btnMinimizeBackup1.setForeground(new Color(255, 255, 255));
        btnMinimizeBackup1.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/min.png"))); // NOI18N
        btnMinimizeBackup1.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        btnMinimizeBackup1.setDoubleBuffered(true);
        btnMinimizeBackup1.setHorizontalTextPosition(SwingConstants.CENTER);
        btnMinimizeBackup1.setMaximumSize(new Dimension(24, 24));
        btnMinimizeBackup1.setMinimumSize(new Dimension(24, 24));
        btnMinimizeBackup1.setName("btnMinimizeRecorder"); // NOI18N
        btnMinimizeBackup1.setPreferredSize(new Dimension(24, 24));
        btnMinimizeBackup1.setPressedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/min.png"))); // NOI18N
        btnMinimizeBackup1.setRolloverIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/min.png"))); // NOI18N
        btnMinimizeBackup1.addActionListener(formListener);
        jpRecorderBackup.add(btnMinimizeBackup1, new AbsoluteConstraints(260, 10, -1, -1));

        btnPlayPauseBackup.setBackground(Color.darkGray);
        btnPlayPauseBackup.setForeground(new Color(255, 255, 255));
        btnPlayPauseBackup.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/record.png"))); // NOI18N
        btnPlayPauseBackup.setToolTipText("");
        btnPlayPauseBackup.setHorizontalTextPosition(SwingConstants.CENTER);
        btnPlayPauseBackup.setMaximumSize(new Dimension(24, 24));
        btnPlayPauseBackup.setMinimumSize(new Dimension(24, 24));
        btnPlayPauseBackup.setName("btnPlay"); // NOI18N
        btnPlayPauseBackup.setPreferredSize(new Dimension(24, 24));
        btnPlayPauseBackup.setPressedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/record-pressed.png"))); // NOI18N
        btnPlayPauseBackup.addActionListener(formListener);
        jpRecorderBackup.add(btnPlayPauseBackup, new AbsoluteConstraints(200, 10, 24, 24));

        jpUpload.setBackground(new Color(216, 216, 216));

        txtUrl.setEditable(false);
        txtUrl.setBackground(new Color(255, 255, 255));
        txtUrl.setPreferredSize(new Dimension(14, 34));
        txtUrl.addActionListener(formListener);

        jLabel3.setFont(new Font("Arial", 1, 14)); // NOI18N
        jLabel3.setForeground(new Color(109, 109, 109));
        jLabel3.setText("Description");

        btnPlay.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/upload/play_normal.png"))); // NOI18N
        btnPlay.setBorderPainted(false);
        btnPlay.setContentAreaFilled(false);
        btnPlay.setPressedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/upload/play_pushed.png"))); // NOI18N
        btnPlay.setRolloverIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/upload/play_upload.png"))); // NOI18N
        btnPlay.addActionListener(formListener);

        txtDescription.setColumns(20);
        txtDescription.setLineWrap(true);
        txtDescription.setRows(5);
        jScrollPane2.setViewportView(txtDescription);

        chkAutoUpload.setBackground(new Color(216, 216, 216));
        chkAutoUpload.setFont(new Font("Arial", 0, 14)); // NOI18N
        chkAutoUpload.setForeground(new Color(109, 109, 109));
        chkAutoUpload.setText("Auto Upload");
        chkAutoUpload.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/upload/checkbox_unchecked.png"))); // NOI18N
        chkAutoUpload.setSelectedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/upload/checkbox_checked.png"))); // NOI18N
        chkAutoUpload.addActionListener(formListener);

        chkPublic.setBackground(new Color(216, 216, 216));
        chkPublic.setFont(new Font("Arial", 0, 14)); // NOI18N
        chkPublic.setForeground(new Color(109, 109, 109));
        chkPublic.setText("Public");
        chkPublic.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/upload/checkbox_unchecked.png"))); // NOI18N
        chkPublic.setSelectedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/upload/checkbox_checked.png"))); // NOI18N
        chkPublic.addActionListener(formListener);

        lblUploadMessage.setFont(new Font("Arial", 1, 14)); // NOI18N
        lblUploadMessage.setForeground(new Color(109, 109, 109));
        lblUploadMessage.setText("Description");

        jLabel1.setFont(new Font("Arial", 1, 14)); // NOI18N
        jLabel1.setForeground(new Color(109, 109, 109));
        jLabel1.setText("Title");

        jLabel8.setFont(new Font("Arial", 0, 14)); // NOI18N
        jLabel8.setForeground(new Color(109, 109, 109));
        jLabel8.setText("Go to your");

        pbEncoding.setForeground(new Color(211, 56, 61));
        pbEncoding.setValue(33);

        btnUpload.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/upload/upload_normal.png"))); // NOI18N
        btnUpload.setBorderPainted(false);
        btnUpload.setContentAreaFilled(false);
        btnUpload.setPressedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/upload/upload_pushed.png"))); // NOI18N
        btnUpload.setRolloverIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/upload/upload_hover.png"))); // NOI18N
        btnUpload.addActionListener(formListener);

        btnSaveAs.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/upload/save-a-copy_normal.png"))); // NOI18N
        btnSaveAs.setBorderPainted(false);
        btnSaveAs.setContentAreaFilled(false);
        btnSaveAs.setPressedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/upload/save-a-copy_pushed.png"))); // NOI18N
        btnSaveAs.setRolloverIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/upload/save-a-copy_hover.png"))); // NOI18N
        btnSaveAs.addActionListener(formListener);

        btnCopy.setFont(new Font("Arial", 0, 14)); // NOI18N
        btnCopy.setForeground(new Color(0, 51, 204));
        btnCopy.setText("copy");
        btnCopy.setBorderPainted(false);
        btnCopy.setContentAreaFilled(false);
        btnCopy.addActionListener(formListener);

        jLabel2.setFont(new Font("Arial", 1, 14)); // NOI18N
        jLabel2.setForeground(new Color(109, 109, 109));
        jLabel2.setText("Link");

        txtTitle.setPreferredSize(new Dimension(14, 34));
        txtTitle.addActionListener(formListener);

        btnAccount.setFont(new Font("Arial", 0, 14)); // NOI18N
        btnAccount.setForeground(new Color(0, 51, 204));
        btnAccount.setText("account");
        btnAccount.setBorderPainted(false);
        btnAccount.setContentAreaFilled(false);
        btnAccount.addActionListener(formListener);

        jPanel3.setBackground(new Color(76, 76, 76));
        jPanel3.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(56, 56, 56)));
        jPanel3.setPreferredSize(new Dimension(132, 23));

        btnCancel.setBackground(Color.white);
        btnCancel.setForeground(new Color(255, 255, 255));
        btnCancel.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/close_normal.png"))); // NOI18N
        btnCancel.setAlignmentY(0.0F);
        btnCancel.setBorder(null);
        btnCancel.setBorderPainted(false);
        btnCancel.setContentAreaFilled(false);
        btnCancel.setDoubleBuffered(true);
        btnCancel.setFocusPainted(false);
        btnCancel.setFocusable(false);
        btnCancel.setHorizontalTextPosition(SwingConstants.CENTER);
        btnCancel.setMaximumSize(new Dimension(42, 16));
        btnCancel.setMinimumSize(new Dimension(42, 16));
        btnCancel.setName("btnCancelRecorder"); // NOI18N
        btnCancel.setPreferredSize(new Dimension(42, 16));
        btnCancel.setPressedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/close_normal.png"))); // NOI18N
        btnCancel.setRolloverIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/close_hover.png"))); // NOI18N
        btnCancel.setVerticalAlignment(SwingConstants.TOP);
        btnCancel.addActionListener(formListener);

        btnMinimize1.setBackground(Color.darkGray);
        btnMinimize1.setForeground(new Color(255, 255, 255));
        btnMinimize1.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/minimize_normal.png"))); // NOI18N
        btnMinimize1.setBorder(null);
        btnMinimize1.setContentAreaFilled(false);
        btnMinimize1.setDoubleBuffered(true);
        btnMinimize1.setFocusPainted(false);
        btnMinimize1.setFocusable(false);
        btnMinimize1.setHorizontalTextPosition(SwingConstants.CENTER);
        btnMinimize1.setMargin(new Insets(2, 14, 0, 14));
        btnMinimize1.setMaximumSize(new Dimension(21, 16));
        btnMinimize1.setMinimumSize(new Dimension(21, 16));
        btnMinimize1.setName("btnMinimizeRecorder"); // NOI18N
        btnMinimize1.setPreferredSize(new Dimension(21, 16));
        btnMinimize1.setPressedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/minimize_normal.png"))); // NOI18N
        btnMinimize1.setRolloverIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/minimize_hover.png"))); // NOI18N
        btnMinimize1.addActionListener(formListener);

        jLabel4.setFont(new Font("Arial", 1, 13)); // NOI18N
        jLabel4.setForeground(new Color(255, 255, 255));
        jLabel4.setText("Ready");

        GroupLayout jPanel3Layout = new GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(btnMinimize1, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(btnCancel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(1, 1, 1)
                .addGroup(jPanel3Layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(jLabel4, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(btnCancel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnMinimize1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        GroupLayout jpUploadLayout = new GroupLayout(jpUpload);
        jpUpload.setLayout(jpUploadLayout);
        jpUploadLayout.setHorizontalGroup(
            jpUploadLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(jpUploadLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpUploadLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(jpUploadLayout.createSequentialGroup()
                        .addComponent(lblUploadMessage)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(pbEncoding, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jpUploadLayout.createSequentialGroup()
                        .addComponent(chkAutoUpload)
                        .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnSaveAs, GroupLayout.PREFERRED_SIZE, 104, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(btnPlay, GroupLayout.PREFERRED_SIZE, 56, GroupLayout.PREFERRED_SIZE))
                    .addGroup(jpUploadLayout.createSequentialGroup()
                        .addComponent(chkPublic)
                        .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel8)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(btnAccount))
                    .addGroup(jpUploadLayout.createSequentialGroup()
                        .addGroup(jpUploadLayout.createParallelGroup(Alignment.LEADING)
                            .addGroup(jpUploadLayout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(txtUrl, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(btnCopy))
                    .addGroup(jpUploadLayout.createSequentialGroup()
                        .addGroup(jpUploadLayout.createParallelGroup(Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addGroup(jpUploadLayout.createSequentialGroup()
                                .addComponent(txtTitle, GroupLayout.PREFERRED_SIZE, 427, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(btnUpload, GroupLayout.PREFERRED_SIZE, 65, GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel1))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addComponent(jPanel3, GroupLayout.DEFAULT_SIZE, 522, Short.MAX_VALUE)
        );
        jpUploadLayout.setVerticalGroup(
            jpUploadLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(jpUploadLayout.createSequentialGroup()
                .addComponent(jPanel3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jpUploadLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(txtUrl, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCopy, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE))
                .addGap(7, 7, 7)
                .addComponent(jLabel1)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(jpUploadLayout.createParallelGroup(Alignment.TRAILING)
                    .addComponent(txtTitle, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnUpload))
                .addGap(21, 21, 21)
                .addComponent(jLabel3)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jpUploadLayout.createParallelGroup(Alignment.TRAILING)
                    .addComponent(lblUploadMessage)
                    .addComponent(pbEncoding, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jpUploadLayout.createParallelGroup(Alignment.LEADING)
                    .addGroup(jpUploadLayout.createSequentialGroup()
                        .addComponent(chkAutoUpload)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(chkPublic))
                    .addComponent(btnSaveAs)
                    .addGroup(jpUploadLayout.createSequentialGroup()
                        .addComponent(btnPlay)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(jpUploadLayout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(btnAccount, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        recorderPanelBG1.setBackground(new Color(255, 255, 255));
        recorderPanelBG1.setMaximumSize(new Dimension(276, 62));
        recorderPanelBG1.setMinimumSize(new Dimension(276, 62));
        recorderPanelBG1.setPreferredSize(new Dimension(276, 62));

        jButton1.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/close_normal.png"))); // NOI18N
        jButton1.setBorderPainted(false);
        jButton1.setContentAreaFilled(false);
        jButton1.setFocusPainted(false);
        jButton1.setFocusable(false);
        jButton1.setMaximumSize(new Dimension(42, 17));
        jButton1.setMinimumSize(new Dimension(42, 17));
        jButton1.setPreferredSize(new Dimension(42, 17));
        jButton1.setRolloverIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/close_hover.png"))); // NOI18N
        jButton1.addActionListener(formListener);

        jButton2.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/minimize_normal.png"))); // NOI18N
        jButton2.setBorderPainted(false);
        jButton2.setContentAreaFilled(false);
        jButton2.setDefaultCapable(false);
        jButton2.setFocusPainted(false);
        jButton2.setFocusable(false);
        jButton2.setMaximumSize(new Dimension(21, 17));
        jButton2.setMinimumSize(new Dimension(21, 17));
        jButton2.setPreferredSize(new Dimension(21, 17));
        jButton2.setRolloverIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/minimize_hover.png"))); // NOI18N
        jButton2.addActionListener(formListener);

        jLabel5.setFont(new Font("Arial", 1, 13)); // NOI18N
        jLabel5.setForeground(new Color(255, 255, 255));
        jLabel5.setText("Ready");
        jLabel5.setMaximumSize(new Dimension(27, 148));

        jToggleButton1.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/record-area-btn_normal.png"))); // NOI18N
        jToggleButton1.setToolTipText("Capture part of the screen");
        jToggleButton1.setBorderPainted(false);
        jToggleButton1.setContentAreaFilled(false);
        jToggleButton1.setDisabledIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/record-area-btn_unavailable.png"))); // NOI18N
        jToggleButton1.setDisabledSelectedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/record-area-btn_unavailable.png"))); // NOI18N
        jToggleButton1.setMaximumSize(new Dimension(66, 40));
        jToggleButton1.setMinimumSize(new Dimension(66, 40));
        jToggleButton1.setPreferredSize(new Dimension(66, 40));
        jToggleButton1.setRolloverIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/record-area-btn_hover.png"))); // NOI18N
        jToggleButton1.setRolloverSelectedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/record-area-btn_active.png"))); // NOI18N
        jToggleButton1.setSelectedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/record-area-btn_active.png"))); // NOI18N
        jToggleButton1.addActionListener(formListener);

        jToggleButton2.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/full-screen-record-btn_normal.png"))); // NOI18N
        jToggleButton2.setToolTipText("Capture entire screen");
        jToggleButton2.setBorderPainted(false);
        jToggleButton2.setContentAreaFilled(false);
        jToggleButton2.setDisabledIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/full-screen-record-btn_unavailable.png"))); // NOI18N
        jToggleButton2.setDisabledSelectedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/full-screen-record-btn_unavailable.png"))); // NOI18N
        jToggleButton2.setMaximumSize(new Dimension(66, 40));
        jToggleButton2.setMinimumSize(new Dimension(66, 40));
        jToggleButton2.setPreferredSize(new Dimension(66, 40));
        jToggleButton2.setRolloverIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/full-screen-record-btn_hover.png"))); // NOI18N
        jToggleButton2.setRolloverSelectedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/full-screen-record-btn_active.png"))); // NOI18N
        jToggleButton2.setSelectedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/full-screen-record-btn_active.png"))); // NOI18N
        jToggleButton2.addActionListener(formListener);

        jLabel6.setFont(new Font("Arial", 0, 26)); // NOI18N
        jLabel6.setForeground(new Color(148, 148, 148));
        jLabel6.setText("00:00");

        jToggleButton3.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/record-btn_normal.png"))); // NOI18N
        jToggleButton3.setToolTipText("Record");
        jToggleButton3.setBorderPainted(false);
        jToggleButton3.setContentAreaFilled(false);
        jToggleButton3.setDisabledIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/record-btn_unavailable.png"))); // NOI18N
        jToggleButton3.setDisabledSelectedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/record-btn_unavailable.png"))); // NOI18N
        jToggleButton3.setMaximumSize(new Dimension(66, 40));
        jToggleButton3.setMinimumSize(new Dimension(66, 40));
        jToggleButton3.setPreferredSize(new Dimension(66, 40));
        jToggleButton3.setRolloverIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/record-btn_hover.png"))); // NOI18N
        jToggleButton3.setSelectedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/record-btn_active.png"))); // NOI18N
        jToggleButton3.addActionListener(formListener);

        soundBar1.setBorderPainted(false);
        soundBar1.setFocusable(false);

        jToggleButton4.setIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/pause-btn_normal.png"))); // NOI18N
        jToggleButton4.setToolTipText("Pause");
        jToggleButton4.setBorderPainted(false);
        jToggleButton4.setContentAreaFilled(false);
        jToggleButton4.setDisabledIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/pause-btn_unavailable.png"))); // NOI18N
        jToggleButton4.setDisabledSelectedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/pause-btn_unavailable.png"))); // NOI18N
        jToggleButton4.setEnabled(false);
        jToggleButton4.setMaximumSize(new Dimension(66, 40));
        jToggleButton4.setMinimumSize(new Dimension(66, 40));
        jToggleButton4.setPreferredSize(new Dimension(66, 40));
        jToggleButton4.setRolloverIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/pause-btn_hover.png"))); // NOI18N
        jToggleButton4.setSelectedIcon(new ImageIcon(getClass().getResource("/com/bixly/pastevid/resources/sb/recorder3/pause-btn_normal.png"))); // NOI18N
        jToggleButton4.addActionListener(formListener);

        GroupLayout recorderPanelBG1Layout = new GroupLayout(recorderPanelBG1);
        recorderPanelBG1.setLayout(recorderPanelBG1Layout);
        recorderPanelBG1Layout.setHorizontalGroup(
            recorderPanelBG1Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(recorderPanelBG1Layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addGroup(recorderPanelBG1Layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(recorderPanelBG1Layout.createSequentialGroup()
                        .addComponent(jLabel5, GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(jButton2, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(jButton1, GroupLayout.PREFERRED_SIZE, 42, GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10))
                    .addGroup(recorderPanelBG1Layout.createSequentialGroup()
                        .addComponent(jToggleButton2, GroupLayout.PREFERRED_SIZE, 34, GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(jToggleButton1, GroupLayout.PREFERRED_SIZE, 34, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(jLabel6, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE)
                        .addGap(11, 11, 11)
                        .addComponent(soundBar1, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
                        .addGap(3, 3, 3)
                        .addComponent(jToggleButton3, GroupLayout.PREFERRED_SIZE, 34, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(jToggleButton4, GroupLayout.PREFERRED_SIZE, 34, GroupLayout.PREFERRED_SIZE)
                        .addGap(14, 14, 14))))
        );
        recorderPanelBG1Layout.setVerticalGroup(
            recorderPanelBG1Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(recorderPanelBG1Layout.createSequentialGroup()
                .addGroup(recorderPanelBG1Layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(Alignment.TRAILING, recorderPanelBG1Layout.createSequentialGroup()
                        .addGroup(recorderPanelBG1Layout.createParallelGroup(Alignment.LEADING, false)
                            .addComponent(jButton1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(recorderPanelBG1Layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(jToggleButton4, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jToggleButton3, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel6, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jToggleButton1, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jToggleButton2, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addComponent(soundBar1, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 29, GroupLayout.PREFERRED_SIZE)))
                    .addGroup(recorderPanelBG1Layout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addComponent(jLabel5, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(jpRecorderRec, GroupLayout.PREFERRED_SIZE, 214, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jpUpload, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jpRecorderNonRec, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jpRecorderBackup, GroupLayout.PREFERRED_SIZE, 324, GroupLayout.PREFERRED_SIZE)
                    .addComponent(recorderPanelBG1, GroupLayout.PREFERRED_SIZE, 286, GroupLayout.PREFERRED_SIZE))
                .addGap(5, 5, 5))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jpUpload, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jpRecorderNonRec, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jpRecorderRec, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jpRecorderBackup, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(recorderPanelBG1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements ActionListener {
        FormListener() {}
        public void actionPerformed(ActionEvent evt) {
            if (evt.getSource() == btnRecordNonRec) {
                RecorderPanel.this.btnRecordNonRecActionPerformed(evt);
            }
            else if (evt.getSource() == btnCancelNonRec) {
                RecorderPanel.this.btnCancelNonRecActionPerformed(evt);
            }
            else if (evt.getSource() == toggleFullScreen) {
                RecorderPanel.this.toggleFullScreenActionPerformed(evt);
            }
            else if (evt.getSource() == btnMinimizeNonRec) {
                RecorderPanel.this.btnMinimizeNonRecActionPerformed(evt);
            }
            else if (evt.getSource() == btnRecordRec) {
                RecorderPanel.this.btnRecordRecActionPerformed(evt);
            }
            else if (evt.getSource() == btnMinimizeRec) {
                RecorderPanel.this.btnMinimizeRecActionPerformed(evt);
            }
            else if (evt.getSource() == btnFinalizeBackup) {
                RecorderPanel.this.btnFinalizeBackupActionPerformed(evt);
            }
            else if (evt.getSource() == btnCancelBackup) {
                RecorderPanel.this.btnCancelBackupActionPerformed(evt);
            }
            else if (evt.getSource() == toggleFullScreenBackup) {
                RecorderPanel.this.toggleFullScreenBackupActionPerformed(evt);
            }
            else if (evt.getSource() == btnMinimizeBackup1) {
                RecorderPanel.this.btnMinimizeBackup1ActionPerformed(evt);
            }
            else if (evt.getSource() == btnPlayPauseBackup) {
                RecorderPanel.this.btnPlayPauseBackupActionPerformed(evt);
            }
            else if (evt.getSource() == txtUrl) {
                RecorderPanel.this.txtUrlActionPerformed(evt);
            }
            else if (evt.getSource() == btnPlay) {
                RecorderPanel.this.btnPlayActionPerformed(evt);
            }
            else if (evt.getSource() == chkAutoUpload) {
                RecorderPanel.this.chkAutoUploadActionPerformed(evt);
            }
            else if (evt.getSource() == chkPublic) {
                RecorderPanel.this.chkPublicActionPerformed(evt);
            }
            else if (evt.getSource() == btnUpload) {
                RecorderPanel.this.btnUploadActionPerformed(evt);
            }
            else if (evt.getSource() == btnSaveAs) {
                RecorderPanel.this.btnSaveAsActionPerformed(evt);
            }
            else if (evt.getSource() == btnCopy) {
                RecorderPanel.this.btnCopyActionPerformed(evt);
            }
            else if (evt.getSource() == txtTitle) {
                RecorderPanel.this.txtTitleActionPerformed(evt);
            }
            else if (evt.getSource() == btnAccount) {
                RecorderPanel.this.btnAccountActionPerformed(evt);
            }
            else if (evt.getSource() == btnCancel) {
                RecorderPanel.this.btnCancelActionPerformed(evt);
            }
            else if (evt.getSource() == btnMinimize1) {
                RecorderPanel.this.btnMinimize1ActionPerformed(evt);
            }
            else if (evt.getSource() == jButton1) {
                RecorderPanel.this.jButton1ActionPerformed(evt);
            }
            else if (evt.getSource() == jButton2) {
                RecorderPanel.this.jButton2ActionPerformed(evt);
            }
            else if (evt.getSource() == jToggleButton1) {
                RecorderPanel.this.jToggleButton1ActionPerformed(evt);
            }
            else if (evt.getSource() == jToggleButton2) {
                RecorderPanel.this.jToggleButton2ActionPerformed(evt);
            }
            else if (evt.getSource() == jToggleButton3) {
                RecorderPanel.this.jToggleButton3ActionPerformed(evt);
            }
            else if (evt.getSource() == jToggleButton4) {
                RecorderPanel.this.jToggleButton4ActionPerformed(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void btnRecordNonRecActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnRecordNonRecActionPerformed
        if (countdownTimer == null || !countdownTimer.isRunning()) {
            this.recordOrPause(true);
        }
    }//GEN-LAST:event_btnRecordNonRecActionPerformed

    private void btnCancelNonRecActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnCancelNonRecActionPerformed
        
        if (this.recorder.hasStatus(RecorderStatus.RECORDING)) {
            pauseRecordState();
        }
        closeApp(false);
    }//GEN-LAST:event_btnCancelNonRecActionPerformed

    private void toggleFullScreenActionPerformed(ActionEvent evt) {//GEN-FIRST:event_toggleFullScreenActionPerformed
        toggleCustomFullscreen();
    }//GEN-LAST:event_toggleFullScreenActionPerformed

    private void btnMinimizeNonRecActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnMinimizeNonRecActionPerformed
        if (this.scrubManager != null) {
            this.scrubManager.resetControls();
        }
        ((JFrame) this.getParent().getParent().getParent().getParent()).setState(JFrame.ICONIFIED);
    }//GEN-LAST:event_btnMinimizeNonRecActionPerformed

    private void btnRecordRecActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnRecordRecActionPerformed
        if (countdownTimer == null || !countdownTimer.isRunning()) {
            this.recordOrPause(true);
        }
    }//GEN-LAST:event_btnRecordRecActionPerformed

    private void btnMinimizeRecActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnMinimizeRecActionPerformed
        if (this.scrubManager != null) {
            this.scrubManager.resetControls();
        }
        ((JFrame) this.getParent().getParent().getParent().getParent()).setState(JFrame.ICONIFIED);
    }//GEN-LAST:event_btnMinimizeRecActionPerformed

    private void btnCancelBackupActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnCancelBackupActionPerformed
        if (this.recorder.hasStatus(RecorderStatus.RECORDING)) {
            pauseRecordState();
        }
        closeApp(false);
    }//GEN-LAST:event_btnCancelBackupActionPerformed

    private void toggleFullScreenBackupActionPerformed(ActionEvent evt) {//GEN-FIRST:event_toggleFullScreenBackupActionPerformed
        toggleCustomFullscreen();
    }//GEN-LAST:event_toggleFullScreenBackupActionPerformed

    private void btnMinimizeBackup1ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnMinimizeBackup1ActionPerformed
        if (this.scrubManager != null) {
            this.scrubManager.resetControls();
        }
        ((JFrame) this.getParent().getParent().getParent().getParent()).setState(JFrame.ICONIFIED);
    }//GEN-LAST:event_btnMinimizeBackup1ActionPerformed

    private void btnPlayPauseBackupActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnPlayPauseBackupActionPerformed
        if (countdownTimer == null || !countdownTimer.isRunning()) {
            this.recordOrPause(true);
        }
    }//GEN-LAST:event_btnPlayPauseBackupActionPerformed

    private void btnFinalizeBackupActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnFinalizeBackupActionPerformed
        processVideo();
    }//GEN-LAST:event_btnFinalizeBackupActionPerformed

    private void txtUrlActionPerformed(ActionEvent evt) {//GEN-FIRST:event_txtUrlActionPerformed
        
    }//GEN-LAST:event_txtUrlActionPerformed

    private void chkPublicActionPerformed(ActionEvent evt) {//GEN-FIRST:event_chkPublicActionPerformed
        processVideo();
    }//GEN-LAST:event_chkPublicActionPerformed

    private void btnCopyActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnCopyActionPerformed
        StringSelection stringSelection = new StringSelection(Session.getInstance().user.getBaseURL()+this.outputMovieSlug);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }//GEN-LAST:event_btnCopyActionPerformed

    private void txtTitleActionPerformed(ActionEvent evt) {//GEN-FIRST:event_txtTitleActionPerformed
        if(Session.getInstance().user.getBaseURL().length() > 0){
            MediaUtil.open(Session.getInstance().user.getBaseURL()+this.outputMovieSlug);
        }
    }//GEN-LAST:event_txtTitleActionPerformed

    private void btnAccountActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnAccountActionPerformed
        MediaUtil.open(Session.getInstance().user.getBaseURL() + "manage");
    }//GEN-LAST:event_btnAccountActionPerformed

    private void btnCancelActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        closeApp(false);
    }//GEN-LAST:event_btnCancelActionPerformed

    private void btnMinimize1ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnMinimize1ActionPerformed
((JFrame) this.getParent().getParent().getParent().getParent()).setState(JFrame.ICONIFIED);
    }//GEN-LAST:event_btnMinimize1ActionPerformed

    private void btnUploadActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnUploadActionPerformed
        if (this.isEncoding) {
            // Check if user wants to auto upload
            if (FileUtil.checkMarker(UPLOAD_ON_ENCODE)) {
                // Toggle auto upload to OFF
                FileUtil.removeMarker(UPLOAD_ON_ENCODE);
                this.chkAutoUpload.setSelected(false);
            } else {
                // Toggle auto upload to ON
                FileUtil.addMarker(UPLOAD_ON_ENCODE);
                this.chkAutoUpload.setSelected(true);
            }
        } else { //The recorder is not encoding
            uploadCopy();
        }
    }//GEN-LAST:event_btnUploadActionPerformed

    private void btnPlayActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnPlayActionPerformed
        log("Clicked Play Button");
        // Hides upload form behind media player window
        jfRecorderPanel.controlSetAlwaysOnTop(false);
        
        if (this.openClientMediaPlayer == null || this.openClientMediaPlayer.isInterrupted()) {
            this.openClientMediaPlayer = new OpenClientMediaPlayer();
            this.openClientMediaPlayer.start();
            log("Playing this video: "+this.recordingOutput);
        } else {
            log("Stop clicking...still waiting on last request");
        }
    }//GEN-LAST:event_btnPlayActionPerformed

    private void btnSaveAsActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnSaveAsActionPerformed
        saveCopy();
    }//GEN-LAST:event_btnSaveAsActionPerformed

    private void chkAutoUploadActionPerformed(ActionEvent evt) {//GEN-FIRST:event_chkAutoUploadActionPerformed
        try {
            Properties properties = this.propertiesManager.readPropertyFile();

            if(this.chkAutoUpload.isSelected()){
                //Toggle auto upload to ON
                FileUtil.addMarker(UPLOAD_ON_ENCODE);
                properties.setProperty(IS_AUTO, "true");
            }else{
                //Toggle auto upload to OFF
                FileUtil.removeMarker(UPLOAD_ON_ENCODE);
                properties.setProperty(IS_AUTO, "false");
            }
            this.propertiesManager.writePropertyFile(properties,"Screenbird Metadata");
            log("Saving: "+properties);
        } catch(Exception exc) {
            log("Exception saving " + exc.getMessage());
        }
    }//GEN-LAST:event_chkAutoUploadActionPerformed

    private void jToggleButton3ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jToggleButton3ActionPerformed
        recordOrPause(true);
        jToggleButton3.setEnabled(false);
    }//GEN-LAST:event_jToggleButton3ActionPerformed

    private void jToggleButton2ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jToggleButton2ActionPerformed
        this.toggleFullScreen.doClick();
    }//GEN-LAST:event_jToggleButton2ActionPerformed

    private void jToggleButton1ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jToggleButton1ActionPerformed
        this.toggleFullScreen.doClick();
    }//GEN-LAST:event_jToggleButton1ActionPerformed

    private void jButton2ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        this.btnMinimizeNonRec.doClick();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        this.btnCancelNonRec.doClick();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jToggleButton4ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jToggleButton4ActionPerformed
        recordOrPause(false);
    }//GEN-LAST:event_jToggleButton4ActionPerformed

    private int clickNumber = 0;
    ButtonGroup screenGroup = new ButtonGroup();
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public JButton btnAccount;
    public JButton btnCancel;
    public JButton btnCancelBackup;
    public JButton btnCancelNonRec;
    public JButton btnCopy;
    private JButton btnFinalizeBackup;
    public JButton btnMinimize1;
    public JButton btnMinimizeBackup1;
    public JButton btnMinimizeNonRec;
    public JButton btnMinimizeRec;
    public JButton btnPlay;
    private JButton btnPlayPauseBackup;
    private JButton btnRecordNonRec;
    private JButton btnRecordRec;
    public JButton btnSaveAs;
    public JButton btnUpload;
    public JCheckBox chkAutoUpload;
    public JCheckBox chkPublic;
    private JButton jButton1;
    private JButton jButton2;
    public JLabel jLabel1;
    public JLabel jLabel2;
    public JLabel jLabel3;
    public JLabel jLabel4;
    private JLabel jLabel5;
    private JLabel jLabel6;
    private JLabel jLabel8;
    public JPanel jPanel3;
    private JScrollPane jScrollPane2;
    private JToggleButton jToggleButton1;
    private JToggleButton jToggleButton2;
    private JToggleButton jToggleButton3;
    private JToggleButton jToggleButton4;
    private JRoundedPanel jpRecorderBackup;
    private RecorderMessage jpRecorderMessage;
    private RecorderMessage jpRecorderMessageBackup;
    private JRoundedPanel jpRecorderNonRec;
    private JRoundedPanel jpRecorderRec;
    public JPanel jpUpload;
    private JLabel lblClockBackup;
    private JLabel lblClockNonRec;
    private JLabel lblClockRec;
    public JLabel lblUploadMessage;
    public JProgressBar pbEncoding;
    private JProgressBar pbVolumeBackup;
    private JProgressBar pbVolumeNonRec;
    private JProgressBar pbVolumeRec;
    private RecorderPanelBG recorderPanelBG1;
    private SoundBar soundBar1;
    private JToggleButton toggleFullScreen;
    private JToggleButton toggleFullScreenBackup;
    public JTextArea txtDescription;
    public JTextField txtTitle;
    public JTextField txtUrl;
    // End of variables declaration//GEN-END:variables

    public void adjustClockTimerFont() {
        Color color = jLabel6.getForeground();
        Font labelFont = jLabel6.getFont();
        String labelText = jLabel6.getText();
        int stringWidth = jLabel6.getFontMetrics(labelFont).stringWidth(labelText);
        int componentWidth = jLabel6.getWidth();
        double widthRatio = (double)componentWidth / (double)stringWidth;
        int newFontSize = (int)(labelFont.getSize() * widthRatio * 0.9);
        int componentHeight = (int)(jLabel6.getHeight() * 0.9);
        int fontSizeToUse = Math.min(newFontSize, componentHeight);
        jLabel6.setFont(new Font(labelFont.getName(), jLabel6.getFont().getStyle(), fontSizeToUse));
        jLabel6.setForeground(color);
        jLabel6.repaint();
    }
    
    public RecorderPanelBG getRecorderPanel() {
        return this.recorderPanelBG1;
    }

    /**
     * Monitors the size of the recorder and adjusts it properly, depending
     * on whether the recorder panel is visible or the upload form is visible.
     * 
     * This does nothing on systems that support transparency.
     */
    private void initResizeTask() {
        if (AWTUtilities.isTranslucencyCapable(((ScreenRecorder)this.jfRecorderPanel).getGraphicsConfiguration())) {
            log("Not doing resizing...");
            return;
        }
        
        final RecorderPanel recorderPanel = this;
        final Dimension recorderDimension = new Dimension(276, 62);
        final Dimension uploadDimension = jpUpload.getPreferredSize();
        final Dimension invisibleDimension = new Dimension(0, 0);
        
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                boolean changed = false;
                if (recorderPanel.recorderPanelBG1.isVisible() 
                        && !recorderDimension.equals(recorderPanel.getSize())) {
                    log("Resizing! " + recorderDimension + " " + ((JFrame)recorderPanel.jfRecorderPanel).getPreferredSize());
                    recorderPanel.setPreferredSize(recorderDimension);
                    recorderPanel.setMinimumSize(recorderDimension);
                    recorderPanel.setMaximumSize(recorderDimension);
                    recorderPanel.setSize(recorderDimension);
                    changed = true;
                } else if (recorderPanel.jpUpload.isVisible() 
                        && !uploadDimension.equals(recorderPanel.getSize())) {
                    log("Resizing! " + uploadDimension + " " + ((JFrame)recorderPanel.jfRecorderPanel).getPreferredSize());
                    recorderPanel.setPreferredSize(uploadDimension);
                    recorderPanel.setMinimumSize(uploadDimension);
                    recorderPanel.setMaximumSize(uploadDimension);
                    recorderPanel.setSize(uploadDimension);
                    changed = true;
                } else if (!recorderPanel.jpUpload.isVisible() 
                        && !recorderPanel.recorderPanelBG1.isVisible() 
                        && !invisibleDimension.equals(recorderPanel.getSize())) {
                    log("Resizing! " + uploadDimension + " " + ((JFrame)recorderPanel.jfRecorderPanel).getPreferredSize());
                    recorderPanel.setPreferredSize(invisibleDimension);
                    recorderPanel.setMinimumSize(invisibleDimension);
                    recorderPanel.setMaximumSize(invisibleDimension);
                    recorderPanel.setSize(invisibleDimension);
                    changed = true;
                }
                
                if (changed) {
                    ((ScreenRecorder)recorderPanel.jfRecorderPanel).controlPack();
                    ((ScreenRecorder)recorderPanel.jfRecorderPanel).repaint();
                    ((ScreenRecorder)recorderPanel.jfRecorderPanel).getContentPane().repaint();
                    recorderPanel.repaint();
                    recorderPanel.recorderPanelBG1.repaint();
                }
            }
        };
        java.util.Timer timer = new java.util.Timer();
        timer.scheduleAtFixedRate(tt, 0, 10);
    }

    /**
     * Thread for handling video upload
     */
    class UploadThread extends Thread {

        public UploadThread() {
            super("Video Upload Thread");
        }
        
        @Override
        public void run() {
            uploading = true;
            uploadFile();
            uploading = false;
        }
    }

    /**
     * Thread for compiling screen capture.
     */
    class ProcessVideoThread extends Thread {

        public ProcessVideoThread() {
            super("Process Video Thread");
        }
        
        @Override
        public void run() {
            synchronized (this) {
                isProcessingVideo = true;
            }
            doneRecording();
            synchronized (this) {
                isProcessingVideo = false;
            }
        }
    }
    
    final RecorderPanel recorderPanel = this;
    
    /**
     * Thread for compiling previous screen capture
     */
    class ProcessPrevVideoThread extends Thread {
        public ProcessPrevVideoThread() {
            super("Process Preview Video Thread");
        }
        
        @Override
        public void run() {
            getRecorder().restoreVideoState();
            long clockOffsetMS = getRecorder().getVideoFileItemLength();
            setClockTime(clockOffsetMS);
            totalTime = clockOffsetMS;
            scrubManager.updatePreviewController(clockOffsetMS);
            scrubManager.restoreScrubFile(recorderPanel, clockOffset);
        }
    }
    
    /**
     * Thread for compiling screen captures audio
     */
    class CompileAudioThread extends Thread {
        public CompileAudioThread() {
            super("Compile Audio Thread");
        }
        
        @Override
        public void run() {
            getRecorder().compileAudio();
        }
    }
    
    /**
     * Dispatches invocation of clients default media player. Using a 
     * thread because sometimes the client's media player takes some time
     * to open and if called in the event dispatch thread, this 
     * procedure locks up the GUI. 
     */
    class OpenClientMediaPlayer extends Thread {
        public OpenClientMediaPlayer() {
            super("Open Client Media Player Thread");
        }
        
        @Override
        public void run() {
            MediaUtil.open(recordingOutput);
            interrupt();
        }
    }
}
