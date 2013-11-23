/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.ipc;

import com.bixly.pastevid.PastevidProtocol;

/**
 *
 * @author cevaris
 */
public final class IPCManager {

    /**
     * Some notes regarding this class
     * - First open instance is declared the base server
     * - Any instance open second, third, fourth, etc.. is declared a client
     *   instance.
     * - On open client will send base server their unique id and 
     *   folder location which the base server will record this info and
     *   track the client periodically. 
     * - On focus of application, the client will request for resource such
     *   as the audio line. 
     * - On close clients will send base server their id stating they are
     *   closing
     *   - The base server will unregister the closing clients
     *   - If the base server is last to close, the base server instance
     *     will call System.exit(0) to kill the zombie instance. 
     * - Base Server should also serialize the data to file as backup
     * - If a client or base server is running critical code, do not 
     *   allow the transfer of focus to another instance
     *   - Critical code is such code as Encoding, Upload, have to define 
     *     when get to that point
     *     
     */
    /**
     * Name and Port of Socket
     */
    public static final String SOCKET_NAME = "localhost";
    public static final int    SOCKET_PORT = 50600;
    /**
     * Wrapper for ServerSocket (Server)
     */
    private Client clientManager;
    private IPCProtocol ipcProtocol;
    
    /**
     * Signals
     */
    private boolean isRunning = false;
    private boolean isReady   = false;
    
    /**
     * Misc.
     */
    private static final int SLUG_LENGTH = 6;

    public IPCManager(PastevidProtocol ipcProtocol) {
        this.ipcProtocol  = ipcProtocol;
        this.initClientManager();
    }
    
    public void initClientManager() {
        //Initiate server handler
        this.clientManager = new Client(this,this.ipcProtocol);
        this.clientManager.start();
    }

    public String getClientId(){
        if (this.clientManager == null)
            return null;
        else
            return this.clientManager.getClientId();
    }
    
    public Client getClient() {
        return this.clientManager;
    }

    public boolean isRunning(){
        return this.isRunning;
    }
    public boolean setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
        return this.isRunning;
    }

    public void setIsReady(boolean val) {
        this.isReady = val;
    }

    public boolean isClientReady() {
        return this.isReady;
    }

    public void shutdown() {
        this.clientManager.closeClient();
    }
    
    
    
    
}
