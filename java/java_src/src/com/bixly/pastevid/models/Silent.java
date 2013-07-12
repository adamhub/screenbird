/*
 * Silent.java
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
 * @author Bixly
 */
public class Silent {
    /**
     * File name for the silent audio.
     */
    public final static String FILE = "silent.wav";
    
    /**
     * Full path of the source silent audio file.
     */
    public final static String RESOURCE = ResourceUtil.SILENT;

    /**
     * Makes a copy of the silent audio file and places it in the given directory.
     * @param captureDir directory path where to copy the silent audio file
     * @return File pointer to the copied silent audio file
     */
    public static File copySilent(String captureDir) {
        // Copied silent file
        File silentRealFile = new File(captureDir, Silent.FILE);
        if (!silentRealFile.exists()) {
            FileUtil.getInstance().copyFile(Silent.RESOURCE, silentRealFile);
        }
        return silentRealFile;
    }
}
