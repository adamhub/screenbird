/*
 * DownloadHandbrake.java
 *
 * Version 1.0
 * 
 * 21 May 2013
 */
package com.bixly.pastevid.download;

import com.bixly.pastevid.Settings;
import com.bixly.pastevid.util.LibraryUtil;
import com.bixly.pastevid.util.MediaUtil;
import java.io.File;

/**
 * Extension of DownloadThread class for downloading the Handbrake library.
 * @author cevaris
 */
public class DownloadHandbrake extends DownloadThread {
    // Handbrake executable file
    private final File executable = new File(Settings.getHandbrakeExecutable());

    public DownloadHandbrake() {
        super("Handbrake Download");
        this.file = new File(Settings.BIN_DIR, "handbrake.zip");
        this.url  = Settings.getHandbrakeLibURL();
    }
    
    @Override
    protected void preDownloadProcedure() {
        // Do nothing
    }

    @Override
    protected void postDownloadProcedure() {
        // Unzip compressed Handbrake           
        LibraryUtil.unzip(this.file.getAbsolutePath());
         
        // Make Handbrake executable     
        if (MediaUtil.osIsWindows()) {
            this.executable.setExecutable(true);    // Windows
        } else {
            LibraryUtil.chmod("+x", executable);    // Mac/Linux
        }
        
        // Overwrite file so it can be reference executable
        this.file = executable;
    }
}
