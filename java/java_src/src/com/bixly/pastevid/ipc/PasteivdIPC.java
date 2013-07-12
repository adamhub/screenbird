/*
 * PastevidIPC.java
 * 
 * Version 1.0
 * 
 * 7 May 2013
 */
package com.bixly.pastevid.ipc;

import com.bixly.pastevid.common.Unimplemented;
import com.bixly.pastevid.screencap.ScreenRecorder;
import com.bixly.pastevid.util.LogUtil;

/**
 *
 * @author cevaris
 */
@Unimplemented
public final class PasteivdIPC {
    /**
     * Instance of the Server Inter-Process Communication Thread.
     */
    private ServerIPC serverIPC;
    
    /**
     * Marks this instance as the first to opened or not.
     * True = First to be opened, False = Not the first.
     */
    private boolean isServerOrClient = true;
    
    /**
     * Represents if the instance has determined if it is 
     * the first application opened or not. 
     */
    private boolean isReady = false;
    
    
    public PasteivdIPC(ScreenRecorder screenRecorder) {
        this.serverIPC = new ServerIPC(screenRecorder,this);
        this.serverIPC.start();
    }

    public boolean closeIPC(){
        return this.serverIPC.closeServer();
    }
    
    public synchronized ServerIPC getServerIPC() {
        return this.serverIPC;
    }
    
    public synchronized boolean getIsServerOrClient(){
        return this.isServerOrClient;
    }
    
    /**
     * Sets the state representing if this instance is 
     * the first application opened or not.
     * Marks this instance into ready state.
     * @param val
     */
    public synchronized void setIsServerOrClient(boolean val){
        this.isServerOrClient = val;
        this.setIsReady(true);
    }

    public synchronized boolean isReady() {
        return isReady;
    }

    public synchronized void setIsReady(boolean value) {
        this.isReady = value;
    }
    
    /**
     * Invokes initServerClient method and saves the returned state.
     * @return 
     */
    public synchronized boolean initIPC() {
        // Sets current status of if this instance is a server or client
        this.setIsServerOrClient(this.serverIPC.initServerOrClient());
        if (this.getIsServerOrClient()) {
            log("Server Initiated"); 
        } else {
            log("Client Initiated");
        }
        return this.getIsServerOrClient();
    }

    public void log(Object message) {
        LogUtil.log(PasteivdIPC.class, message);
    }

}
