/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.ipc;

import com.bixly.ipc.exception.InvalidIPCUsageException;
import com.bixly.util.Queue;
import com.bixly.ipc.exception.UnexpectedIPCResponseException;
import com.bixly.util.TimeUtil;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import static com.bixly.ipc.IPCManager.*;

/**
 * Client side client
 * Client which connects to base server
 */
public class Client  extends Thread{

    private Socket      client;
    private String      state = IPCProtocol.STATE_IDLE;
    private String      clientId;
    private IPCManager  ipcManager; 
    private IPCProtocol ipcProtocol;
    private Queue<String> queue;
    
    
    private ClientReader clientReader;
    private ClientWriter clientWriter;
    
    Client(IPCManager ipcManager, IPCProtocol ipcProtocol) {
        this.ipcProtocol = ipcProtocol;
        this.ipcManager  = ipcManager;
        this.queue       = new Queue();
        connectToBaseServer();
    }
    
    public String getClientId(){
        return this.clientId;
    }

    public void setProtocol(IPCProtocol ipcProtocol){
        this.ipcProtocol = ipcProtocol;
    }
    
    private boolean connectToBaseServer() {
        try {
            System.out.println("Connecting to Base Server...");
            this.client = new Socket(SOCKET_NAME, SOCKET_PORT);

            ipcManager.setIsRunning(true);
            
            System.out.println("Connected to Base Server");
            System.out.println("Client Initiated...");

        }catch (ConnectException e){
            e.printStackTrace(System.err);
            System.exit(-1);
        } catch (UnknownHostException e) {
            System.err.println("Could not connect to " + SOCKET_NAME + ":" + SOCKET_PORT);
            e.printStackTrace(System.err);
        } catch (IOException e) {
            System.err.println("Could not connect to " + SOCKET_NAME + ":" + SOCKET_PORT);
            e.printStackTrace(System.err);
        } catch (NullPointerException e){
            e.printStackTrace(System.err);
        }

        return false;
    }
    
    public synchronized boolean isRunning() {
        return (ipcManager.isRunning() && !this.client.isClosed() && this.client.isConnected()
                && !this.client.isInputShutdown() && !this.client.isOutputShutdown());
    }
    
    @Override
    public void run() {
        try{
            //Create stream buffer reader/writer
            prepClient();
            //Register client with server
            registerClient();
            //Let reader/writer run till shutdown
            while(isRunning()){TimeUtil.skipToMyLou(2);}
            //Close Client
            closeClient();
        }catch (NullPointerException e){
            e.printStackTrace(System.err);
        }catch (IOException e){
            e.printStackTrace(System.err);
        }catch (UnexpectedIPCResponseException e){
            e.printStackTrace(System.err);
        }catch (InvalidIPCUsageException e){
            e.printStackTrace(System.err);
            this.closeClient();
        }
    }
    
    
    public void registerClient() throws IOException, UnexpectedIPCResponseException, InvalidIPCUsageException{
        
        if(this.ipcProtocol == null)
            throw new InvalidIPCUsageException("Must define a Protocol before use");
        
        while(!clientReader.isReady||!clientWriter.isReady){
            System.out.println("Waiting for Client threads to sync");
            TimeUtil.skipToMyLou(0.2);
        }
        System.out.println("Synced!!");
        
        //String command = this.ipcProtocol.buildRegister();
        //Queue login request
        //this.queueRequest(command);
        
        //System.out.println("Queued Registering Client");
    }

//    private void executeIPC() throws IOException, UnexpectedIPCResponseException, SocketTimeoutException{
//
//        
//        String response = null;
//        //Mark client as ready to excecute tasks
//        this.ipcManager.setIsReady(true);
//        LogUtil.log(this.clientId,"Client is ready!!");
//        LogUtil.log(this.clientId,"ThreadName: "+Thread.currentThread().getName());
//        LogUtil.log(this.clientId,"Waiting for tasks");
//
//        //Keep looping till quit
//        while(this.isRunning()){
//            
//            //If no tasks
//            if(this.queue.isEmpty()){
//                //Listen to base server for task
//                //New incoming
//                try{
//                    this.client.setSoTimeout(1000);
//                    LogUtil.log(this.clientId,"No Client Request, Waiting for input from Base Server");
//                    if((response = this.clientIn.readLine())!=null){
//                        //Send response to be handled
//                        this.ipcProtocol.receiveResponse(response);
//                    }
//                    LogUtil.log(this.clientId,"Incoming: " + response);
//                    //No timeout means we received something
//                }catch (SocketTimeoutException e){
//                    TimeUtil.skipToMyLou(0.1);
//                    continue;
//                }
//                
//            }else{//Sending a Request, new outgoing
//                
//                //Get request
//                String request  = this.queue.pop();
//                LogUtil.log(this.clientId,"Found Client Request, sending new Request: "+request);
//
//                LogUtil.log(this.clientId,"Sending new Request: "+request);
//                //Send Request
//                this.clientOut.println(request);
//                this.client.setSoTimeout(2000);
//
//                LogUtil.log(this.clientId,"Sent new Request: "+request);
//
//                LogUtil.log(this.clientId,"Waiting for Response");
//                //Wait for response
//                while(this.isRunning() && (response = this.clientIn.readLine()) != null) {
//                    //Base Server sends us client id
//                    LogUtil.log(this.clientId,"Incoming: " + response);
//                    if(this.ipcProtocol.receiveResponse(response) != null){
//                        //Send ACK
//                        this.clientOut.println(this.ipcProtocol.buildACK());
//                    } else
//                        throw new UnexpectedIPCResponseException(response);
//                }
//            }
//        }
//    }
    
    public synchronized void queueRequest(String request) {
        this.queue.push(request);
    }
    
    private void prepClient() {
        //Create stream buffer reader/writer
        System.out.println("Initiating links to redirect stream buffers...");
        this.clientReader = new ClientReader(this);
        this.clientWriter = new ClientWriter(this);
        this.clientReader.start();
        this.clientWriter.start();
        System.out.println("Initiated links to redirect stream buffers");
    }
    
    public synchronized boolean closeClient() {
        try {
            
            ipcManager.setIsRunning(false);
            
            this.clientWriter.unregisterClient();
            
            System.out.println("Closing Client Streams/Sockets...");
            if (this.client != null) {
                
                //Close I/O threads
                this.client.shutdownInput();
                this.client.shutdownOutput();
                
                //Close socket
                this.client.close();
            }

            System.out.println("Closed Client Streams/Sockets");
            return true;
        } catch (SocketException e){
            System.out.println("Socket was closed while reading, all is fine");
            //e.printStackTrace(System.out);
            return true;
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.out.println("Could not close Client socket " + client.toString());
            return false;
        } 
    }
    
    public Socket getSocket(){
        return this.client;
    }
    public IPCProtocol getIPCProtocol(){
        return this.ipcProtocol;
    }
    public Queue<String> getRequestQueue(){
        return this.queue;
    }

    IPCManager getIPCManager() {
        return this.ipcManager;
    }

    public synchronized void setClientId(String clientId) {
        this.clientId = clientId;
        this.clientReader.updateClientId(clientId);
        this.clientWriter.updateClientId(clientId);
        Thread.currentThread().setName("Client:"+this.clientId);
    }

    
    
    
    

    

}
