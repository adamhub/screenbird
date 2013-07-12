/*
 * VideoScrubSortByStartTime
 * 
 * Version 1.0
 * 
 * 7 May 2013
 */
package com.bixly.pastevid.editors;

import java.io.Serializable;
import java.util.Comparator;

/**
 *
 * @author cevaris
 */
public class VideoScrubSortByStartTime implements Comparator<VideoScrub>, Serializable {
    static final long serialVersionUID = 1886497409091692L;
    
    /**
     * Compares two video scrubs.
     * @param left
     * @param right
     * @return -1 if left scrub starts before right scrub<br/>
     * 0 if left scrub starts at the same time as the right scrub<br/>
     * 1 if left scrub starts after right scrub.
     */
    public int compare(VideoScrub left, VideoScrub right) {
    	if (left.start < right.start) {
            return -1;
        } else if (left.start > right.start) {
            return 1;
        }
        
        // If starting times are equal
        if (left.end < right.end) {
            return 1;
        } else if (left.end > right.end) {
            return -1;
        } else {
            return 0;
        }
    }
}
