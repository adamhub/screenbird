package com.bixly.pastevid.screencap;

import com.sun.awt.AWTUtilities;
import java.awt.AWTException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.netbeans.lib.awtextra.AbsoluteLayout;

import com.bixly.pastevid.Session;
import com.bixly.pastevid.Settings;
import com.bixly.pastevid.TestSettings;
import com.bixly.pastevid.download.DownloadFFMpeg;
import com.bixly.pastevid.download.DownloadHandbrake;
import com.bixly.pastevid.download.DownloadManager;
import com.bixly.pastevid.download.DownloadUnzip;
import com.bixly.pastevid.ipc.InstanceManager;
import com.bixly.pastevid.models.User;
import com.bixly.pastevid.util.LogUtil;
import com.bixly.pastevid.util.MediaUtil;
import com.bixly.pastevid.util.PropertiesUtil;
import com.bixly.pastevid.util.ResourceUtil;


/**
 * @author Bixly
 */
public class ScreenRecorder extends JFrame implements ScreenRecorderController {
    
    /**
     * Location where screen recorder fist opens  to on start
     */
    final static public Point DEFAULT_LOCATION = new Point(30,30);
    
    /**
     * Main JFrame which the Recorder Panel is deployed to
     */
    private JFrame jfRecorderPanel;
    /**
     * Main JPanel which all recording controls are located
     */
    private RecorderPanel  jpRecorderPanel;
    
    /*
     * File Listener which detects if a second instance is opening
     * 
     */
    private InstanceManager instanceManager;
    
    public ScreenRecorder(boolean recover) {
        this.initInstanceHandler();
        this.prepLogger();
        this.prepEnvironment();
        this.loadParameters();
        this.initDownloads();
        this.createTray();
        this.initComponents();
        this.initRecorder(true, recover);
    }
    
    public static void main(String[] args) {
        boolean recoveryMode = false;
        if (args.length == 1 && args[0].equals("recover")){
            recoveryMode = true;
        }
        ScreenRecorder screenbird = new ScreenRecorder(recoveryMode);
    }

    /**
     * Can be called at any time during runtime. Destroys all windows,
     * cleans up any instance handling, screen capture saving, 
     * logger handling.
     */
    public void destroy() {
        log("Destroying Instance");
        
        // If instance of RecorderPanel and Recorder exists
        if(this.jpRecorderPanel != null && this.jpRecorderPanel.getRecorder() != null){
            // Save for video recovery
            this.jpRecorderPanel.getRecorder().stopAndSaveVideoState();
            this.jpRecorderPanel.getScrubManager().saveScrubFile();
            // Drop access to hardware line
            this.dropAudioLine();
            // Hide RecorderPanel
            this.jpRecorderPanel.setVisible(false);
        }
        
        // Close download manager if any
        if (DownloadManager.getInstance() != null) {
            DownloadManager.getInstance().close();
        }
        
        if (this.instanceManager != null && this.instanceManager.isRunning()) {
            this.instanceManager.close();
        }
        
        try {
            if (this.jfRecorderPanel != null) {
                // Hide main JFrame
                this.jfRecorderPanel.setVisible(false);
                // Dispose of main JFrame
                this.jfRecorderPanel.dispose();
            }
        } catch (Exception e) {
            log(e);
        }
        // Close logging mechanism
        LogUtil.close();
        // Kill this process 
        System.exit(0);
    }

    /**
     * Sets up the system tray for screenRecorder application
     */
    private void createTray() {
        PopupMenu menu = new PopupMenu();
        
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {                
                JOptionPane.showMessageDialog(jfRecorderPanel,
                        String.format("Screenbird%nBuild Version %s", RecorderPanel.resources.getString("BUILD")));
            }
        });
        menu.add(aboutItem);
        
        // Open the settings menu
        MenuItem settingsItem = new MenuItem("Preferences");
        settingsItem.addActionListener(new ActionListener() {
public void actionPerformed(ActionEvent e) {
                if(jpRecorderPanel.isRecorderConfigSate())
                    jpRecorderPanel.showSettingsForm();
            }
        });
        menu.add(settingsItem);

        // Hide or show the recorder 
        MenuItem messageItem = new MenuItem("Hide/Show");
        messageItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (jfRecorderPanel.getState() == JFrame.NORMAL) {
                    jfRecorderPanel.setState(JFrame.ICONIFIED);
                } else {
                    jfRecorderPanel.setState(JFrame.NORMAL);
                }
            }
        });
        menu.add(messageItem);
        
        MenuItem closeItem = new MenuItem("Quit/Exit");
        closeItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                destroy();
            }
        });
        menu.add(closeItem);
        
        // Loads the pastevid logo
        Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource(ResourceUtil.LOGO_16));
        if (!MediaUtil.osIsWindows()) {
            icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource(ResourceUtil.LOGO_24));
        }
        // Assigns the pastevid logo
        TrayIcon tray = new TrayIcon(icon, "Screenbird", menu);
        try {
            SystemTray.getSystemTray().add(tray);
        } catch (AWTException ex) {
            log(ex);
        }
    }
    
    public final void initRecorder(boolean show, boolean recover) {
        // If recorder was instantiated properly
        this.initRecorderFrame();
            
        assert this.jpRecorderPanel != null :
            "Recorder Panel must be instantiated before calling this procedure";
        
        this.openRecorder(show);
        
        if (show) {      
            this.jpRecorderPanel.initCaptureBox();
            // Cannot auto check if running Junit tests
            this.jpRecorderPanel.checkPrevVideo(recover);
        }
    }
    
    private void initDownloads() {
        DownloadManager downloadManager = DownloadManager.getInstance();
        if (MediaUtil.osIsWindows()) {
            // Windows needs unzipping utility
            downloadManager.registerDownload(
                Settings.getUnzipExecutable(),
                new DownloadUnzip()
            );
        }
        
        // Register FFMpeg for downloading
        downloadManager.registerDownload(
                Settings.getFFMpegExecutable(), 
                new DownloadFFMpeg()
        );
        
        // Register Handbrake for downloading
        downloadManager.registerDownload(
                Settings.getHandbrakeExecutable(), 
                new DownloadHandbrake()
        );
        downloadManager.start();
    }
    
    /**
     * 
     */
    private void initRecorderFrame() {
        int startX = DEFAULT_LOCATION.x;
        int startY = DEFAULT_LOCATION.y;
        
        this.jfRecorderPanel = new JFrame("Screenbird");
        
        // Not sure why we have to dispose JFrame right after creation
        // but code does not work otherwise
        this.jfRecorderPanel.setVisible(false);
        this.jfRecorderPanel.dispose();
        this.jfRecorderPanel.setUndecorated(true);
        
        this.jfRecorderPanel.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource(ResourceUtil.LOGO_TASKBAR)));
        // Set name for JUnit testing
        this.jfRecorderPanel.setName("recorderCtrlFrame");
        // Mark on exit of applet, close GUI
        this.jfRecorderPanel.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // GUI should always be on view
        this.jfRecorderPanel.setAlwaysOnTop(true);
        
        // If this instance is a client or IPC failed, exit applet
        // Recording window
        this.jpRecorderPanel = new RecorderPanel();
        this.jpRecorderPanel.setController(this);
        this.jpRecorderPanel.initRecorder();
        this.jfRecorderPanel.add(this.jpRecorderPanel);
        
        try {
            if (!AWTUtilities.isTranslucencyCapable(this.jfRecorderPanel.getGraphicsConfiguration())) {
                log("Can not set transparency");
                this.jfRecorderPanel.setBackground(new Color(64, 64, 64, 255));
                this.jpRecorderPanel.setBackground(new Color(0, 0, 0, 255));
                this.jpRecorderPanel.setOpaque(true);
            } else {
                log("Transparency is set");
                AWTUtilities.setWindowOpaque(jfRecorderPanel, false);
            }
        } catch (Exception ex) {
            log(ex);
        }
        
        // Hack for handling draggable JFrames on Mac OSX
        this.jfRecorderPanel.getRootPane().putClientProperty("apple.awt.draggableWindowBackground", Boolean.FALSE);
        
        this.jfRecorderPanel.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.jfRecorderPanel.setLocation(startX, startY);
        this.jfRecorderPanel.pack();
        this.jpRecorderPanel.adjustClockTimerFont();
        this.jfRecorderPanel.setVisible(true);
    }
    
    
    /**
     * Reads the config file for session based data.
     * Each token is pre-defined.
     */
    private void loadParameters() {
        if (Settings.IS_JUNIT_TESTING) {
            return;
        }
        
        // Loading user's data
        log("Loading user's data");
        File propertyFile = new File(Settings.SCREENBIRD_CONFIG);
        Session.getInstance().user.setAnonToken(PropertiesUtil.loadProperty(propertyFile.getAbsolutePath(), User.ANON_TOKEN));
        Session.getInstance().user.setChannelId(PropertiesUtil.loadProperty(propertyFile.getAbsolutePath(), User.CHANNEL_ID));
        Session.getInstance().user.setCsrfToken(PropertiesUtil.loadProperty(propertyFile.getAbsolutePath(), User.CSRF_TOKEN));
        Session.getInstance().user.setUserId(PropertiesUtil.loadProperty(propertyFile.getAbsolutePath(),    User.USER_ID));
        Session.getInstance().user.setBaseURL(PropertiesUtil.loadProperty(propertyFile.getAbsolutePath(),   User.BASE_URL));
        Session.getInstance().user.setSlug(PropertiesUtil.loadProperty(propertyFile.getAbsolutePath(), User.SLUG));
        
        log("Loaded user's data");
        log(Session.getInstance().user);
    }

    /**
     * Opens the newly initiated Recorder Panel
     * @param show 
     */
    private void openRecorder(boolean show) {
        this.jpRecorderPanel.setController(this);
        this.jfRecorderPanel.setVisible(show);
        this.jpRecorderPanel.showRecorderForm();
        this.jfRecorderPanel.pack();
        this.addMoveListener();
    }

    /**
     * Binds Component listener to Recorder Panel so we can detect when the 
     * Recorder Panel is moved/dragged on the screen. In particular, this 
     * binding is included in the procedure for detect when the Recorder
     * Panel is being dragged to an external monitor. 
     */
    private void addMoveListener() {
        this.jfRecorderPanel.addComponentListener(new ComponentListener() {
            public void componentResized(ComponentEvent e) {
            }
            public void componentHidden(ComponentEvent e) {
            }
            public void componentShown(ComponentEvent e) {
            }
            public void componentMoved(ComponentEvent e) {
                jpRecorderPanel.displayChangeHandler();
            }
        });
    }
    
    /**
     * If client has access to the audio resources, drop access. 
     */
    private void dropAudioLine() {
        if (this.jpRecorderPanel.getRecorder() != null) {
            jpRecorderPanel.getRecorder().dropAudioLine();
        }
    }
    
    /**
     * Is called when first opened application detects a second opened 
     * application. This brings the first opened application to view. 
     */
    public synchronized void bringToFocus() {
        // Queue a process to be ran when the GUI feels like it
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                // If the RecorderPanel is not null
                if (jpRecorderPanel != null && jpRecorderPanel.getRecorder() != null) {
                    // Incase audio line was lost, reinstantiate line
                    if (!jpRecorderPanel.getRecorder().isLineOpen()) {
                        jpRecorderPanel.monitorAudioLine();
                    }
                }
                // If the main JFrame is not null
                if (jfRecorderPanel != null) {
                    // If GUI not viewable
                    if (jfRecorderPanel.getState() != JFrame.NORMAL) {
                        // Make GUI viewable
                        jfRecorderPanel.setState(JFrame.NORMAL);
                    }
                    // Update GUI positions
                    jfRecorderPanel.pack();
                    // Make sure GUI is viewable
                    jfRecorderPanel.setVisible(true);
                    // Update GUI
                    jfRecorderPanel.repaint();
                    // Make GUI focus
                    jfRecorderPanel.requestFocus();
                    // Bring GUI to the front of other windows
                    jfRecorderPanel.toFront();
                    // Set the window always to be on top
                    jfRecorderPanel.setAlwaysOnTop(true);
                }
            }
        });
    }
    
    /**
     * Instantiates multiple instance handler and checks to see if this current
     * instance is the only instance running. If this is not the only instance,
     * InstanceManager will handle the situation.
     */
    private void initInstanceHandler() {
        this.instanceManager = new InstanceManager(this);
        this.instanceManager.checkMultInstance();
    }

    /**
     * Operating System specific code that is ran for GUI compatibility.
     */
    private void prepEnvironment() {
        if (MediaUtil.osIsWindows()) {
            this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource(ResourceUtil.LOGO_TASKBAR)));
        }
    }
    
    /**
     * Same as JFrame.setLocation. Move recorder panel to coordinates (x,y)
     * 
     * @param x Integer x-coordinate
     * @param y Integer y-coordinate
     */
    public void controlSetLocation(int x, int y) {
        this.jfRecorderPanel.setLocation(x, y);
    }

    /**
     * Same as JFrame.setVisible. Renders the recorder panel visible or not
     * 
     * @param value Boolean
     */
    public void controlSetVisible(boolean value) {
        this.jfRecorderPanel.setVisible(value);
    }

    /**
     * Same as JFrame.setAlwaysOnTop. Sets the recorder panel on top of all windows;
     * 
     * including other programs.
     * @param value Boolean
     */
    public void controlSetAlwaysOnTop(boolean value) {
        this.jfRecorderPanel.setAlwaysOnTop(value);
    }

    /**
     * Same as JFrame.pack. Packs in components dynamically. 
     */
    public void controlPack() {
        this.jfRecorderPanel.pack();
    }
    
    private void prepLogger() {
        //Pads a few lines with a divider to separate runtime logs
        log("\n\n============================================");
        LogUtil.printSystemProperties();
    }
    
    /**
     * Returns the RecorderPanel. Only for JUnit Testing.
     * @return 
     */
    public RecorderPanel getPanel() {
        assert TestSettings.IS_JUNIT_TESTING : "This method is only for JUnit Testing";
        return this.jpRecorderPanel;
    }
    
    /**
     * Returns the RecorderPanel JFrame. Only for JUnit Testing.
     * @return 
     */
    public JFrame getFrame() {
        return this.jfRecorderPanel;
    }

    public void log(Object message) {
        LogUtil.log(ScreenRecorder.class, message);
    }
    
    /** 
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setBackground(new Color(64, 64, 64));
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/bixly/pastevid/resources/logo-taskbar.png")));
        setName("ScreenRecorder"); // NOI18N
        getContentPane().setLayout(new AbsoluteLayout());
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}

