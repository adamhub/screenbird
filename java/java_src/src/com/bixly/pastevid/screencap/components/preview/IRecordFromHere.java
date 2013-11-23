/*
 * IRecordFromHere.java
 *
 * Version 1.0
 * 
 * 16 May 2013
 */
package com.bixly.pastevid.screencap.components.preview;

/**
 * Interface that describes the relationship between the Preview Player
 * and the "Record From Here" panel.
 * @author cevaris
 */
public interface IRecordFromHere  {

    /**
     * Updates the time label.
     * @param text Label depicting the current time index
     */
    void setLabelTextTimeCurrent(String text);
    
}
