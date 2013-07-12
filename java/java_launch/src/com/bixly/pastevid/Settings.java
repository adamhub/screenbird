/*
 * Launch.java
 *
 * Version 1.0
 * 
 * 20 May 2013
 */
package com.bixly.pastevid;

import com.bixly.pastevid.util.MediaUtil;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 *
 * @author cevaris
 */
public class Settings {
    /**
     * Native file separator for the current system.
     */
    private static final String fileSep = System.getProperty("file.separator");
    
    /**
     * Temporary directory for storing runtime files.
     */
    private static final String tempDir = System.getProperty("java.io.tmpdir")+fileSep;
    
    /**
     * Users root home directory for storing more persistent files.
     */
    public static final String USER_HOME = System.getProperty("user.home")+fileSep;
    
    
    public static final String SCREEN_CAPTURE_DIR = tempDir  + "screencap" + fileSep;
    public static final String HOME_DIR = USER_HOME + ".screenbird" + fileSep;
    public static final String BIN_DIR  = HOME_DIR + "bin" + fileSep;
    
    public static final String LINUX_LIBS = BIN_DIR  +"linxlibs.jar";
    public static final String MAC_LIBS   = BIN_DIR  +"maclibs.jar";
    public static final String WIN_LIBS   = BIN_DIR  +"winlibs.jar";
    
    public static final String SCREENBIRD_LOG_DIR = HOME_DIR+"logs"+fileSep;
    public static final String SCREENBIRD_CONFIG  = HOME_DIR+"config.sb";
    public static final File   LOCK_FILE = new File(HOME_DIR,"openInstance");
    public static final File   FLAG_FILE = new File(HOME_DIR,"bringToFront");
    
    public static boolean ACTIVATE_SCRUB_FEATURE = false;
    
    public static boolean PRINT_EXEC_TO_SCREEN = false;
    
    public static final String OS_FFMPEG_LIB = getFFMpegLib();
    
    public static final String PNG = "png";
    public static final String MPG = "mpg";
    public static final String MP4 = "mp4";
    
    /*
     * Log data settings
     */
    public final static boolean LOG_DATA   = true;
    public final static boolean LOG_STDOUT = true; 
    public final static long LOG_MAX_SIZE  = 2*1024*1024;
    public final static DateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    
    /*
     * Preview Playback Settings
     */
    public static final Float FULLSCREEN_PREVIEW_SCALE   = 0.65f;
    public static final Float CUSTOMSCREEN_PREVIEW_SCALE = 0.75f;
    
    private static String getFFMpegLib() {
        if (MediaUtil.osIsMac()) {
            return MAC_LIBS;
        } else if (MediaUtil.osIsUnix()) {
            return LINUX_LIBS;
        } else {
            return WIN_LIBS;
        }
    }
    
    public static String getFFMpegLibURL(String baseUrl) {
        if (MediaUtil.osIsMac()) {
            return String.format("%s/%s",baseUrl,"maclibs.jar");
        } else if (MediaUtil.osIsUnix()) {
            return String.format("%s/%s",baseUrl,"linxlibs.jar");
        } else {
            return String.format("%s/%s",baseUrl,"winlibs.jar");
        }
    }
}
