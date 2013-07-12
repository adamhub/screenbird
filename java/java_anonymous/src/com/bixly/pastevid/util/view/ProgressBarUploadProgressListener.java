/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.util.view;

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
    }
    public void transferred(long num) {
        long qty = ((num*100)/(this._fileSize/4096));
         this.progress = (int)(qty);
         _pbUpload.setValue(progress);             
        System.out.println(progress);
        
    }
    
}
