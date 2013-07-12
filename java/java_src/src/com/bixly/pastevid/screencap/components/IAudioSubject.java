package com.bixly.pastevid.screencap.components;

/**
 * @author Bixly
 */
public interface IAudioSubject {

    public void addObserver(IAudioObserver o);

    public void removeObserver(IAudioObserver o);
    
    public Boolean isCompiling();
    
}