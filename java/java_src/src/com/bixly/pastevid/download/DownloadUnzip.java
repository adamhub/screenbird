/*
 * DownloadUnzip.java
 *
 * Version 1.0
 * 
 * 21 May 2013
 */
package com.bixly.pastevid.download;

import com.bixly.pastevid.Settings;
import com.bixly.pastevid.util.LogUtil;
import java.io.File;

/**
 * Extension of DownloadThread class for downloading the Unzip library. Only run
 * on Windows since unzip is pre-installed with Mac/Linux environments.
 * @author cevaris
 */
public class DownloadUnzip extends DownloadThread {

    public DownloadUnzip() {
        super("Unzip Download");
        this.file = new File(Settings.getUnzipExecutable());
        this.url  = Settings.getUnzipLibURL();
    }
    
    @Override
    protected void preDownloadProcedure() {
        // Do nothing
    }

    @Override
    protected void postDownloadProcedure() {
        if (this.file.exists()) {
            this.file.setExecutable(true);
        } else {
            log("Could not locate "+this.file.getAbsolutePath());
        }  
    }
    
    private void log(Object message) {
        LogUtil.log(DownloadUnzip.class, message);
    }
}
