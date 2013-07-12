/*
 * ProgressBarUploadListener.java
 *
 * Version 1.0
 * 
 * 17 May 2013
 */
package com.bixly.pastevid.screencap.components.progressbar;

import com.bixly.pastevid.util.LogUtil;
import javax.swing.JProgressBar;

/**
 *  
 * @author Jorge
 */
public class ProgressBarUploadProgressListener implements ProgressMultiPartEntity.UploadProgressListener {

    private JProgressBar _pbUpload;
    private long _fileSize;
    private int progress = 0;

    public ProgressBarUploadProgressListener(JProgressBar pbUpload, long fileSize) {
        this._pbUpload = pbUpload;
        this._fileSize = fileSize;
        this._pbUpload.setMaximum(100);
    }

    /**
     * 
     * @param num 
     */
    public void transferred(long num) {
        long qty = ((num * 100) / (this._fileSize / 4096));
        this.progress = (int) (qty);
        
        // Log every megabyte uploaded, prevents excessive logging
        if (num % 512 == 0) {
            log(progress+" ("+(num)+"/"+(this._fileSize / 1024)+")");
        }

        if (progress > 101) {
            this._pbUpload.setValue(_pbUpload.getMaximum());
        } else {
            _pbUpload.setValue(progress);
        }

    }

    public void log(Object message) {
        LogUtil.log(ProgressBarUploadProgressListener.class, message);
    }
}
