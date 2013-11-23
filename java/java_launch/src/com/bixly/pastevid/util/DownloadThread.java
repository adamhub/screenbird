/*
 * DownloadThread.java
 *
 * Version 1.0
 * 
 * 20 May 2013
 */
package com.bixly.pastevid.util;

import java.io.File;

/**
 * TODO refactor unused class
 * Downloads a file located at the given URL and stores 
 * at the given directory with the given file name.
 * 
 * Used for downloading large files asynchronously
 */
public class DownloadThread extends Thread {

    String urlSource;
    File   libDestination;
    boolean isRunning = true;

    public DownloadThread(String urlSource, File libDestination) {
        this.urlSource = urlSource;
        this.libDestination = libDestination;
    }

    @Override
    public void run() {
        LibraryUtil.wget(urlSource, libDestination.getParent(), libDestination.getName());
        System.out.println("Finished "+ libDestination.getAbsolutePath());
        isRunning = false;
    }

    public synchronized boolean isRunning(){
        return isRunning;               
    }

}