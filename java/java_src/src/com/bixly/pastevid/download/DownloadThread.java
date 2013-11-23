/*
 * DownloadThread.java
 *
 * Version 1.0
 * 
 * 21 May 2013
 */
package com.bixly.pastevid.download;

import com.bixly.pastevid.util.LibraryUtil;
import java.io.File;

/**
 * Base downloader thread class for downloading screenbird libraries.
 * @author cevaris
 */
public abstract class DownloadThread extends Thread {
    // Flag for checking if thread is currently downloading
    private boolean isDownloading = false;
    
    // File where to save download
    protected File file = null;
    
    // URL where to download the file
    protected String url = null;
    
    // Status for this download
    protected DownloadStatus status = DownloadStatus.NOT_STARTED;

    /**
     * This class can be subclassed per download file. 
     * @param downloadName String value of the Name of the thread
     */
    public DownloadThread(String downloadName) {
        super(downloadName);
    }
    
    @Override
    public final void run() {
        synchronized (this) {
            this.isDownloading = true;
            this.status = DownloadStatus.PRE_PROCESS;
        }
        
        preDownloadProcedure();
        
        synchronized (this) {
            this.status = DownloadStatus.DOWNLOAD_PROCESS;
        }
        
        LibraryUtil.wget(url, file.getParent(), file.getName());
        
        synchronized (this) {
            this.isDownloading = false;
        }
        
        synchronized (this) {
            this.status = DownloadStatus.POST_PROCESS;
        }
        
        postDownloadProcedure();
        
        synchronized (this) {
            this.status = DownloadStatus.FINISHED;
        }
    }

    protected abstract void preDownloadProcedure();
    protected abstract void postDownloadProcedure();
    
    /**
     * Returns true if given status is equal to current state of download
     * @param downloadStatus Test state of download
     * @return Boolean
     */
    public final synchronized boolean checkStatus(DownloadStatus downloadStatus) {
        return (this.status == downloadStatus);
    }

    public final synchronized DownloadStatus getStatus() {
        return status;
    }
    
    /**
     * Confirm that the file is completely downloaded. This procedure returns
     * the file pointer currently tied to the file that is being downloaded.
     * @return the downloaded file
     */
    public final synchronized File getFile(){
        return this.file;
    }

    /**
     * Returns true if the thread is currently downloading the file.
     * @return 
     */
    public final synchronized boolean isDownloading(){
        return this.isDownloading;
    }
}
