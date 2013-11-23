/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.ipc;

import com.bixly.ipc.exception.RegisterClientException;
import com.bixly.ipc.exception.UnexpectedIPCResponseException;
import com.bixly.util.LogUtil;
import com.bixly.util.Queue;
import com.bixly.util.RandomUtil;
import com.bixly.util.TimeUtil;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
/**
 * Server side client
 * Handler for incoming clients that are to be handled by the base server.
 */
public class ClientHandler extends Thread {
    
    private static final int SLUG_LENGTH = 6;
    
    private Server        server;
    private Socket        client;
    private String        clientId;
    private ServerReader  serverReader;
    private ServerWriter  serverWriter;
    private Queue<String> queue;
    private IPCProtocol   ipcProtocal;
    
    protected boolean isRunning = false;

    public ClientHandler(Socket newWorker, IPCProtocol ipcProtocol, Server server) {
        //this.ipcManager  = ipcManager;
        this.ipcProtocal = ipcProtocol;
        this.client      = newWorker;
        this.server      = server;
        this.isRunning   = true;
        //Generate unique id
        this.clientId    = RandomUtil.generateSlug(SLUG_LENGTH);
        this.queue       = new Queue<String>();
    }

    @Override
    public void run() {
        
        Thread.currentThread().setName("ClientHandler:"+this.clientId);

        try{
            LogUtil.log(clientId, "Welcoming new Client on Server Base");
            LogUtil.log(clientId, "Client: " + client);
            //Create stream buffer reader/writer
            //prepClient();
            //Register client
            //registerClient();
            //Wait for requested task
            //receiveClientRequest();
            //Close Client
            //closeClient();
            //Unregister Client
            //unregisterClient();
            //Create stream buffer reader/writer
            prepClient();
            //Register with client
            this.registerClient();
            //Let reader/writer run till shutdown
            while(isRunning()){TimeUtil.skipToMyLou(2);}
            //Close Client
            closeClient();
        } catch (RegisterClientException e){
            e.printStackTrace(System.err);
        } catch (UnexpectedIPCResponseException e){
            e.printStackTrace(System.err);
        }catch (IOException e){
            e.printStackTrace(System.err);
        }
//        
        System.out.println(this.server);
        System.out.print("\n\n");
        
    }
    
    private void prepClient() {
        //Create stream buffer reader/writer
        LogUtil.log(this.clientId,"Initiating links to redirect stream buffers...");
        //try {
            
        //Create stream buffer reader/writer
        this.serverReader = new ServerReader(this);
        this.serverWriter = new ServerWriter(this);
        this.serverReader.start();
        this.serverWriter.start();
        
        LogUtil.log(this.clientId,"Waiting to prept Client Handle...");
        while(this.isReady()){
            LogUtil.log(this.clientId,"Waiting...");
            TimeUtil.skipToMyLou(0.2);
        }
        LogUtil.log(this.clientId,"Synced!!");
        
            //clientOut = new PrintWriter(client.getOutputStream(), true);
            //clientIn  = new BufferedReader(new InputStreamReader(client.getInputStream()));
//        } catch (IOException e) {
//            System.err.println("Could not Prepare ClientHandler: " + client.toString());
//        }
        LogUtil.log(this.clientId,"Initiated links to redirect stream buffers");
    }

    public boolean closeClient() {
        try {
            
            LogUtil.log(this.clientId,"Closing ClientHandler Streams");
            LogUtil.log(this.clientId,"Closing ClientHandler Socket");
            
            this.unregisterClient();
            
            if (client != null) {
                client.close();
                
                if(!client.isInputShutdown())
                    this.client.shutdownInput();
                if(!client.isOutputShutdown())
                    this.client.shutdownOutput();
            }

            this.isRunning = false;
            LogUtil.log(this.clientId,"Closed ClientHandler Streams/Sockets");
            return true;
        } catch (SocketException e){
            this.isRunning = false;
            LogUtil.log(this.clientId,"Closed Reading Socket");
            return true;
        } catch (IOException e) {
            LogUtil.log(this.clientId,"Could not close Client socket " + client.toString());
            return false;
        } 
    }
    
    //This is for Client, not Server
//    public synchronized void unregisterClient() {
//        if (this.clientOut != null && this.client.getSocket() != null) {
//            System.out.println("Sending Closing Command to BaseServer...");
//            this.clientOut.println(this.ipcProtocol.buildFinsh());
//            System.out.println("Sent Closing Command to BaseServer");
//        }
//    }
    
    public synchronized void unregisterClient() {
        //Create response
        String command = this.ipcProtocal.buildFinsh();
        //Send Response with client's Unique Id
        this.sendClientRequest(command);
        
        LogUtil.log(this.clientId,"Unregistering Client from server....");
        this.server.unregisterClient(clientId);
        LogUtil.log(this.clientId,"Client is no longer registered");
    }
    public void registerClient() 
            throws IOException, RegisterClientException, UnexpectedIPCResponseException {
        LogUtil.log(this.clientId,"Registering Client " + client.toString());
        //Create response
        String command = this.ipcProtocal.buildRegister(clientId);
        //Send Response with client's Unique Id
        this.sendClientRequest(command);
        
        LogUtil.log(this.clientId,"Outgoing: "+command);
        
        LogUtil.log(this.clientId,"Waiting Response of Register Client");
        
        this.server.registerClient(this.clientId, this);
        this.serverReader.updateClientId(clientId);
        this.serverWriter.updateClientId(clientId);
        
        LogUtil.log(this.clientId,"Registered Client " + client.toString());
    }

    public synchronized boolean isRunning() {
        return this.isRunning && !this.client.isClosed() 
                && !this.client.isInputShutdown()
                && !this.client.isOutputShutdown();
                //&& !this.serverWriter.isRunning();
    }

    @Override
    public String toString() {
        return "IsRunning: "+this.isRunning+" "+Thread.currentThread().getName()+" Client:"+this.client;
    }

    public synchronized void sendClientRequest(String request) {
        System.out.println("Sending Client:"+clientId+" "+ request);
        this.queue.push(request);
        System.out.println("Sent Client:"+clientId+" "+ request);
    }

    public Queue<String> getRequestQueue() {
        return this.queue;
    }

    public IPCProtocol getIPCProtocol() {
        return this.ipcProtocal;
    }

    public Socket getSocket() {
        return this.client;
    }

    public String getClientId() {
        return this.clientId;
    }

    Server getServer() {
        return this.server;
    }
    
    public synchronized boolean isReady(){
        return (this.serverReader.isReady() &&
                this.serverWriter.isReady() &&
                this.isRunning());
    }


    
}
