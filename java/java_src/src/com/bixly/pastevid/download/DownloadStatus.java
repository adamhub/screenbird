/*
 * DownloadStatus.java
 *
 * Version 1.0
 * 
 * 21 May 2013
 */
package com.bixly.pastevid.download;

/**
 * An enumerated type for different download statuses.
 * @author cevaris
 */
public enum DownloadStatus {
    NOT_STARTED,
    PRE_PROCESS,
    DOWNLOAD_PROCESS,
    POST_PROCESS,
    FINISHED
}
