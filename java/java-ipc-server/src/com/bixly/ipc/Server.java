/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.ipc;
import com.bixly.ipc.exception.RegisterClientException;
import java.net.BindException;
import com.bixly.pastevid.PastevidProtocol;
import com.bixly.util.LogUtil;
import com.bixly.util.TimeUtil;
import java.util.HashMap;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
/**
 *
 * @author cevaris
 */
public final class Server{
    
    /**
     * Name and Port of Socket
     */
    public static final String SOCKET_NAME = "localhost";
    public static final int    SOCKET_PORT = 50600;
    
    private PastevidProtocol ipcProtocol;
    private ServerSocket serverSocket;
    private HashMap<String,ClientHandler> clientHandlers;
    private boolean isRunning;
    
    private Timer timer = new Timer();
    
    public Server(PastevidProtocol ipcProtocol) {
        this.ipcProtocol    = ipcProtocol;
        this.clientHandlers = new HashMap<String, ClientHandler>();
        //Setup server
        this.init();
        //Start listening for clients
        this.main();
    }
    
    //public void main(){
    public void main() {
        
        //Name Thread
        Thread.currentThread().setName("Base Server");
        
        while(this.isRunning()){

            //Listen for client
            listenForClients();
            
            
            TimeUtil.skipToMyLou(0.5);
        }
        
        closeServer();

    }
    
    private boolean isRunning(){
        return this.isRunning;
    }
    
    private boolean setRunningStatus(boolean val){
        return this.isRunning = val;
    }

    private void listenForClients() {
        try {
            
            
            System.out.println("Listening for clients on Thread: "+Thread.currentThread().getName());
            
            setAutoSelfDestruct();
            
            
            //Accept Client and starts thread to register 
            ClientHandler handler = new ClientHandler(this.serverSocket.accept(), 
                                                      this.ipcProtocol, this);
            //Reset inactivity timer task
            this.timer.cancel();
            //Thread off new client
            handler.start();
            
            
            
        } catch (IOException e) {
            System.out.println("Accept failed: " + SOCKET_NAME + ":" + SOCKET_PORT);
            System.exit(-1);
        }
    }

    public synchronized boolean closeServer() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            
            if(this.clientHandlers != null && !this.clientHandlers.isEmpty()){
                System.err.println("Clients are still connected, need to kill the clients");
            }
            
            setRunningStatus(false);
            return true;
        } catch (IOException e) {
            System.out.println("Could not close Base Server " + " " + serverSocket.toString() + " " + SOCKET_NAME + ":" + SOCKET_PORT);
            return false;
        }
    }

    public void init() {
        try{
            //Open Socket
            this.serverSocket = new ServerSocket(SOCKET_PORT);
            //Update signals
            this.isRunning    = true;
            System.out.println("Base Server is up and running");
            //Start server
            //this.serverManager.main();
            this.ipcProtocol.setServer(this);
        }catch (BindException e){
            System.out.println("Base Server is already Running");
            //Kill process
            System.exit(0);
        }catch (IOException e){
            e.printStackTrace(System.err);
        }
    }

    public synchronized void unregisterClient(String clientId) {
        if(this.clientHandlers.get(clientId) != null){
            LogUtil.log(clientId, "Removing a Client: "+clientId);
            this.clientHandlers.remove(clientId);
            System.out.println(this);
        }else
            LogUtil.log(clientId, "Client already removed");
            
    }
    public synchronized void registerClient(String clientId, ClientHandler clientHandle) 
            throws RegisterClientException {
        
        LogUtil.log(clientId,"Adding Client");
        if((clientHandle != null) && (this.clientHandlers.get(clientId) == null)){
            LogUtil.log(clientId,"ID: "+clientId+" " +clientHandle);
            this.clientHandlers.put(clientId,clientHandle);
            System.out.println(this);
        }
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        
        if(this.clientHandlers != null && !this.clientHandlers.isEmpty()){
            for (Map.Entry<String, ClientHandler> entry : this.clientHandlers.entrySet()) {
                String key = entry.getKey();
                ClientHandler value = entry.getValue();
                
                str.append("Client: ");
                str.append(key);
                str.append(" : ");
                str.append(value);
                str.append("\n");
            }
            
        }
        
        if(str.length() == 0)
            return "EMPTY";
        
        return str.toString();
    }

    public HashMap<String, ClientHandler> getClientHandlers() {
        return clientHandlers;
    }

    /**
     * Runs a clean up method
     * 
     * Detects if there are any active clients, if not shut system down
     * If there are clients detected, re-run method
     */
    private void setAutoSelfDestruct() {
        
        try{
            //Sets task to shut server down 
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(clientHandlers.isEmpty()){
                        System.err.println("Closing due to inactivity");
                        System.exit(0);
                    }else{
                        setAutoSelfDestruct();
                    }
                }
            }, (60000*3));
        }catch( IllegalStateException e){
            System.err.println("Timer is being reset");
            this.timer = new Timer();
            setAutoSelfDestruct();
        }
    }
    
}
