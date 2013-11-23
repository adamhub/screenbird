/*
 * IListen.java
 *
 * Version 1.0
 * 
 * 17 May 2013
 */
package com.bixly.pastevid.screencap.components.progressbar;

/**
 *
 * @author cevaris
 */
public interface IListen {

    public String getKey();
    public void setKey(String key);
    
    public boolean isTaskComplete();
    public boolean isTaskStart();
    
    public double getCurrentTaskProgress();
    public double setCurrentTaskProgress(double value);
    
    
}
