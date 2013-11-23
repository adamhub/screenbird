/*
 * DownloadManager.java
 *
 * Version 1.0
 * 
 * 21 May 2013
 */
package com.bixly.pastevid.download;

import com.bixly.pastevid.structs.Queue;
import com.bixly.pastevid.util.LogUtil;
import com.bixly.pastevid.util.TimeUtil;
import java.util.HashMap;

/**
 * An asynchronous singleton download manager
 * @author cevaris
 */
public class DownloadManager extends Thread {
    
    /**
     * Queue data structure to assist with queueing the registered downloads by 
     * FIFO standards. We queue the jobId's of each download in order to 
     * control the downloads.
     */
    private final Queue<String> downloadQueue = new Queue<String>();
    
    /**
     * Hash map of DownloadThreads. A String jobID is provided with each DownloadThread. 
     */
    private final HashMap<String, DownloadThread> downloadStore = new HashMap<String, DownloadThread>();
    
    /**
     * Singleton reference for DownloadManager
     */
    private static DownloadManager instance;
    
    /**
     * If the DownloadManager is currently observing downloads. IsRunning state 
     * does not change after all the downloads are completed
     */
    private boolean isRunning = false;
    
    /**
     * Used as singleton, marked private to prevent instance usage
     */
    private DownloadManager() {
        super("Download Manager");
    }
    
    public synchronized static DownloadManager getInstance() {
        if (instance == null) {
            instance = new DownloadManager();
        }
        return instance;
    }
    
    /**
     * Register a file to be downloaded
     * @param jobId Id given to reference the download job
     * @param download DownloadThread object
     */
    public void registerDownload(String jobId, DownloadThread download) {
        if (this.downloadStore.containsKey(jobId)) {
            log("Job " + jobId + " has already registered");
            return;
        }
        
        // Place download in queue
        this.downloadQueue.push(jobId);
        
        // Store download
        this.downloadStore.put(jobId, download);
    }

    @Override
    public void run() {
        isRunning = true;
        while (isRunning) {
           if (downloadQueue.isEmpty()) {
                // If no downloads left, kill download thread
                log("No Registered downloads found. Killing DownloadManager");
                isRunning = false;
                continue;
            }
            
            // To be downloaded
            DownloadThread download;
            // Get to be downloaded job id
            String jobId = downloadQueue.peek();
            
            // Check to see if that job still exists
            if (this.downloadStore.containsKey(jobId)) {
                // Grab download reference using the jobId
                download = this.downloadStore.get(jobId);
                log("Checking status of Download Job " + jobId + " Status " + download.getStatus());
                
                if (download.checkStatus(DownloadStatus.NOT_STARTED)) {
                    download.start();
                } else if (download.checkStatus(DownloadStatus.FINISHED)) {
                    // If download has finsihed, deuque/unregister download
                    downloadQueue.pop();                    
                }
            }
            
            // Wait for downloads to make some progress before checking on them
            TimeUtil.skipToMyLouMS(500L);
        }
        
        log("Closed Download Manager");
        interrupt();
    }
    
    /**
     * Returns current status of download. If download does not exists or is 
     * not registered correctly, pingDownloadStatus returns null
     * @param jobId Id of given download job
     * @return 
     */
    public synchronized DownloadStatus pingDownloadStatus(String jobId) {
        if (this.downloadStore.containsKey(jobId)) {
            return this.downloadStore.get(jobId).getStatus();
        } else {
            return null;
        }
    }
    
    /**
     * Returns DownloadThread object from the store identified by jobID.
     * @param jobId Id of the download job to be obtained
     * @return 
     */
    public synchronized DownloadThread getDownload(String jobId) {
        if(downloadStore.containsKey(jobId)){
            return downloadStore.get(jobId);
        } else 
            return null;
    }
    
    /**
     * Returns true if there are active downloads and false otherwise.
     * @return 
     */
    public synchronized boolean isRunning() {
        return this.isRunning;
    }
    
    /**
     * Stops active downloads.
     */
    public synchronized void close(){
        this.isRunning = false;
    }
    
    private void log(Object message) {
        LogUtil.log(DownloadManager.class, message);
    }
}
