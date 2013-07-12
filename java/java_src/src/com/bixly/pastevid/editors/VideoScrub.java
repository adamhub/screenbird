/*
 * VideoScrub
 * 
 * Version 1.0
 * 
 * 7 May 2013
 */
package com.bixly.pastevid.editors;

import java.io.Serializable;

/**
 *
 * @author cevaris
 */
public class VideoScrub implements Serializable {
    int start = 0;
    int end = 0;
    int duration = 0;

    public VideoScrub(int start, int end) {
        this.start = start;
        this.end = end;
        this.duration = end - start;
    }
    
    public Integer getOffset() {
        return this.end - this.start;
    }

    public Integer getStart() {
        return start;
    }

    public Integer getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return String.format("(%d,%d)", this.start, this.end);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof VideoScrub) {
            VideoScrub scrub = (VideoScrub) o;
            return ((this.start == scrub.start) && (this.end == scrub.end));
        } else if (o != null) {
            throw new ClassCastException("Illegal cast " + o.getClass() + " to VideoScrub");
        } else {
            throw new IllegalArgumentException("Comparing object is null");
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash += hash*start;
        hash += hash*end;
        return hash;
    }
}
