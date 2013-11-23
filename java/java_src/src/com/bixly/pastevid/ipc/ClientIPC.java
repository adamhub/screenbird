/*
 * ClientIPC.java
 * 
 * Version 1.0
 * 
 * 7 May 2013
 */
package com.bixly.pastevid.ipc;

import com.bixly.pastevid.common.Unimplemented;
import com.bixly.pastevid.util.LogUtil;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 *
 * @author cevaris
 */
@Unimplemented
public class ClientIPC {
    // Client socket which connects to ServerSocket
    private Socket client;
    
    /**
     * Connects to SocketServer using the SOCKET_NAME and SOCK_PORT
     * @return True if client is successful in connecting, false otherwise
     */
    public boolean connect() {
        try {
            log("Connecting to Base Server...");
            this.client = new Socket(ServerIPC.SOCKET_NAME, ServerIPC.SOCKET_PORT);
            log("Connected to Base Server");
            log("Client Initiated...");
            return true;
        } catch (ConnectException e) {
            log(e);
        } catch (UnknownHostException e) {
            log("Could not connect to " + ServerIPC.SOCKET_NAME + ":" + ServerIPC.SOCKET_PORT);
        } catch (IOException e) {
            log("Could not connect to " + ServerIPC.SOCKET_NAME + ":" + ServerIPC.SOCKET_PORT);
            log(e);
        } catch (NullPointerException e) {
            log(e);
        }
        return false;
    }

    /**
     * Closes the client's socket and input/output.
     * @return 
     */
    public synchronized boolean close() {
        try {
            log("Closing Client Streams/Sockets...");
            if (this.client != null) {
                // Close I/O threads
                this.client.shutdownInput();
                this.client.shutdownOutput();
                
                // Close socket
                this.client.close();
            }
            log("Closed Client Streams/Sockets");
            return true;
        } catch (SocketException e) {
            log("Socket was closed while reading, all is fine");
            return true;
        } catch (IOException e) {
            log("Could not close Client socket " + client.toString());
            log(e);
            return false;
        } 
    }

    public void log(Object message) {
        LogUtil.log(ClientIPC.class, message);
    }
}
