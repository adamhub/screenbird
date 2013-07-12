/*
 * TestSettings.java
 *
 * Version 1.0
 * 
 * 4 May 2013
 */
package com.bixly.pastevid;

import com.bixly.pastevid.util.MediaUtil;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class for storing Screenbird settings used when testing.
 * @author cevaris
 */
public class TestSettings {
    /**
     * Native separator for the current operating system.
     */
    public static final String FILE_SEP = System.getProperty("file.separator");
    
    /* Main directories */
    // Users root home directory for storing more persistent files
    public static final String USER_HOME = System.getProperty("user.home") + FILE_SEP;
    
    // Users hidden screenbird directory where config, logs, and instance files are stored
    public static final String HOME_DIR = USER_HOME + ".screenbird" + FILE_SEP;
    
    // Directory where all runtime screen capture media is stored
    public static final String SCREEN_CAPTURE_DIR = HOME_DIR + "screencap" + FILE_SEP;
    
    // Directory where all the exectuable binaries are libraries are stored
    public static final String BIN_DIR  = HOME_DIR + "bin" + FILE_SEP;
    
    // Directory where all backups of previous video recordings are stored
    public static final String BACKUP_DIR  = HOME_DIR + "backups" + FILE_SEP;
    
    // Directory where logs are stored
    public static final String SCREENBIRD_LOG_DIR = HOME_DIR + "logs" + FILE_SEP;
    
    // File where Screen Recorder configuration is stored
    public static final String SCREENBIRD_CONFIG  = HOME_DIR + "config.sb";

    
    /* Log file semaphores for detecting multiple instances */
    // Semaphore representing that there is an open instance running
    public static final File LOCK_FILE = new File(HOME_DIR, "openInstance");
    
    // Semaphore for flagging other instance 
    public static final File FLAG_FILE = new File(HOME_DIR, "bringToFront");
    
    
    /* Debugging settings */
    // Redirects system call executions output to screen
    public static final boolean PRINT_EXEC_TO_CONSOLE = false;
    // Some code does not run correctly during JUnit tests, thus this flag
    // is needed. 
    public static final boolean IS_JUNIT_TESTING = true;
    
    
    /* Preview Playback Settings */
    // False to disable, True to enable oops feature
    public static final boolean ENABLE_OOPS_FEATURE = true;
    // Scaling of images for custom and fullscreen previewing
    public static final Float FULLSCREEN_PREVIEW_SCALE   = 0.70f;
    public static final Float CUSTOMSCREEN_PREVIEW_SCALE = 1.0f;
    // Number of screenshots that are displayed per second in preview mode
    public static final int PREVIEW_MAX_NUM_SCREENSHOT = 5;
    
    
    /* Log data settings */
    // Log all data to file or not
    public final static boolean LOG_DATA_TO_FILE = true;
    
    // Print any logged message to console, for debugging 
    public final static boolean LOG_STDOUT = true;
    
    // Max size that a log file can be before creating a new one
    public final static long    LOG_MAX_SIZE  = 2*1024*1024;
    
    // Datetime format of each log
    public final static SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    
    // Keys of the screenbird property file
    public final static List<String> JAVA_LOG_PROPERITES = Collections.unmodifiableList(Arrays.asList(
            "sun.boot.library.path",
            "java.vm.version",
            "mrj.version",
            "java.runtime.version",
            "java.version",
            "java.specification.version",
            "java.vm.name",
            "os.arch",
            "sun.arch.data.model",
            "java.home",
            "java.vm.info"
    ));
    
    
    /**
     * Returns executable path of Unzip application
     * @return 
     */
    public static String getUnzipExecutable() {
        if (MediaUtil.osIsWindows()) {
            return BIN_DIR + "unzip.exe";
        } else {
            // Since Mac/Linux have unzip pre-installed, do not need full path
            return "unzip";
        }
    }
    
    /**
     * Returns executable path of Handbrake application
     * @return 
     */
    public static String getHandbrakeExecutable() {
        if (MediaUtil.osIsWindows()) {
            return BIN_DIR + "handbrake.exe";
        } else {
            return BIN_DIR + "handbrake";
        }
    }
    
    /**
     * Returns executable path of FFMpeg application
     * @return 
     */
    public static String getFFMpegExecutable() {
        if (MediaUtil.osIsWindows()) {
            return BIN_DIR  +"ffmpeg.exe";
        } else {
            return BIN_DIR  +"ffmpeg";
        }
    }
    
    /**
     * Returns URL path to download the Unzip application
     * @return 
     */
    public static String getUnzipLibURL() {
        return String.format("%s/media/applet/lib/unzip.exe",
                Session.getInstance().user.getBaseURL()
        );
                
    }
    
    /**
     * Returns URL path to download the FFMpeg application
     * @return 
     */
    public static String getFFMpegLibURL() {
        return String.format("%s/media/applet/lib/%s/ffmpeg.zip",
                Session.getInstance().user.getBaseURL(),
                MediaUtil.getNormalizedOSName()
        );
                
    }
    
    /**
     * Returns URL path to download the Handbrake application
     * @return 
     */
    public static String getHandbrakeLibURL() {
        return String.format("%s/media/applet/lib/%s/handbrake.zip",
                Session.getInstance().user.getBaseURL(),
                MediaUtil.getNormalizedOSName()
        );
    }
}
