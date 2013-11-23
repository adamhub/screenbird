/*
 * WaterMark.java
 * 
 * Version 1.0
 * 
 * 7 May 2013
 */
package com.bixly.pastevid.models;

import com.bixly.pastevid.util.FileUtil;
import com.bixly.pastevid.util.ResourceUtil;
import java.io.File;

/**
 * Model class for the watermark used in the screen shots.
 * @author Bixly
 */
public class WaterMark {
    
    /**
     * Maximum number of seconds where watermark will be applied.
     */
    public final static int LIMIT = 8;
    
    /**
     * File name for the watermark.
     */
    public final static String FILE = "watermark.png";
    
    /**
     * Full path of the source watermark file.
     */
    public final static String RESOURCE = ResourceUtil.SCREENBIRD_LOGO;

    /**
     * Makes a copy of the watermark file to be applied on the screenshots on 
     * the given directory.
     * @param captureDir directory path where to copy the watermark file
     * @return File pointer to the copied watermark file
     */
    public static File copyWaterMark(String captureDir) {
        // Copied file
        File watermarkRealFile = new File(captureDir + WaterMark.FILE);
        if (!watermarkRealFile.exists()) {
            FileUtil.getInstance().copyFile(WaterMark.RESOURCE, watermarkRealFile);
        }
        return watermarkRealFile;
    }
}
