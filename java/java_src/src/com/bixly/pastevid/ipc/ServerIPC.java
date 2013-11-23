/*
 * ServerIPC.java
 * 
 * Version 1.0
 * 
 * 7 May 2013
 */
package com.bixly.pastevid.ipc;

import com.bixly.pastevid.common.Unimplemented;
import com.bixly.pastevid.screencap.ScreenRecorder;
import com.bixly.pastevid.util.LogUtil;
import com.bixly.pastevid.util.TimeUtil;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.SocketException;
    
/**
 *
 * @author cevaris
 */
@Unimplemented
public class ServerIPC extends Thread{
    public ServerIPC(){
        super("Inter-Process Communcation Server");
    }
    
    // Name and port of socket
    public static final String SOCKET_NAME = "localhost";
    public static final int    SOCKET_PORT = 50600;
    
    /**
     * Instance of runtime ScreenRecorder application.
     */
    private ScreenRecorder screenRecorder;
    
    /**
     * Base server instance. Only the first opened ScreenRecorder application
     * has this ServerSocket instance running.
     */
    private ServerSocket serverSocket;
    
    /**
     * Custom class which allows ScreenRecorder/Pastevid application to 
     * interface with the Inter-Process Communication functionalities. 
     */
    private PasteivdIPC ipc;
    
    /**
     * Represents the current state of the SocketServer. <BR/>
     * False: Triggers SocketServer to close<BR/>
     * True:  Keeps the listening for incoming clients
     */
    private boolean isRunning = false;

    /**
     * Thread which the SocketServer runs on. 
     * Listens for incoming clients a.k.a. other ScreenRecorder applications. 
     * 
     * @param screenRecorder
     * @param ipc
     */
    ServerIPC(ScreenRecorder screenRecorder, PasteivdIPC ipc) {
        this.screenRecorder = screenRecorder;
        this.ipc = ipc;
    }

    @Override
    public void run() {
        // Determines if this instance is the first application opened or not
        this.initServerOrClient();
        log("Got connection");
        
        while(this.isRunning()) {
            // Listen for client
            listenForClients();
            TimeUtil.skipToMyLou(0.5);
        }
        closeServer();
    }
    
    /**
     * True if server is in running state.<BR/>False otherwise.
     * @return 
     */
    private synchronized boolean isRunning(){
        return this.isRunning;
    }
    
    /**
     * Sets the value of the current running state of the ServerSocket. 
     * This method is Thread Safe.
     * @param val
     * @return 
     */
    private synchronized boolean setRunningStatus(boolean val){
        return this.isRunning = val;
    }

    /**
     * If the ServerSocket detects a client attempting to connect, that means 
     * another (second) instance is attempting to run. The (second) instance will
     * detect the server and close itself. This instance is the <B>first</B> instance,
     * and upon detecting a second application, this instance will invoke the
     * main GUI thread to bring the <B>first</B> instance to view. 
     * 
     */
    private synchronized void listenForClients() {
        try {
            log("Listening for clients on Thread: "+Thread.currentThread().getName());
            
            // This method blocks execution while waiting for clients
            this.serverSocket.accept();
            
            log("Server requesting frame to front");
            // Found a second instance, bring this instance to view
            screenRecorder.bringToFocus();
            log("Server requested frame to front");
        } catch (SocketException e){
            log("Socket was closed unexpectedly");
            System.exit(0);
//            throw new RuntimeException("Gracefully Shutting Down Application");
        } catch (IOException e) {
            log("Accept failed: " + SOCKET_NAME + ":" + SOCKET_PORT);
            log(e);
        } 
    }

    /**
     * Closes ServerSocket and updates state to <B>Not Running</B>.
     * @return 
     */
    public boolean closeServer() {
        log("Closing Server");
        try {
            log("Closing Server");
            if (serverSocket != null) {
                serverSocket.close();
                log("Is Server Open: "+ (!serverSocket.isClosed()));
            }
            
            setRunningStatus(false);
            log("Closed Server");
            return true;
        } catch (IOException e) {
            log("Could not close Base Server " + " " + serverSocket.toString() + " " + SOCKET_NAME + ":" + SOCKET_PORT);
            return false;
        }
    }

    /**
     * Attempts to start a ServerSocket to a well-known-socket. If no 
     * BindException is thrown then ServerSocket creation was successful
     * and this instance is currently the first and only instance running. 
     * <br/><br/>
     * If the BindException is thrown, that means there is another previously
     * opened/running instance of Pastevid/ScreenRecorder running. Once
     * it is determined that this instance is not the first instance, the 
     * attempt to connect to the instance which is running the ServerSocket
     * is made. 
     * 
     * @return <b>True</b> if this Pastevid/Screenrecorder application is the 
     * first instance to be opened. <b>False</b> if it is determined that this
     * instance is not the first opened instance. 
     */
    public final synchronized boolean initServerOrClient() {
        try {
            // Open Socket
            this.serverSocket = new ServerSocket(SOCKET_PORT);
            // Update signals
            this.setRunningStatus(true);
            this.ipc.setIsServerOrClient(true);
            
            log("Base Server is up and running");
            log("Found Server, resuming application");
            return true;
        } catch (BindException e) {
            log("Base Server is already Running");
            log("Found Client, Closing GUI");
            // Update signals
            this.setRunningStatus(false);
            // This is the second instance
            // Calling original server to view
            ClientIPC client = new ClientIPC();
            client.connect();
            client.close();

            // Set this thread to kill
            this.ipc.setIsServerOrClient(false);
            
        } catch (IOException e){
            log(e);
        }
        
        // Kill process
        return false;
    }

    public void log(Object message) {
        LogUtil.log(ServerIPC.class, message);
    }
}
