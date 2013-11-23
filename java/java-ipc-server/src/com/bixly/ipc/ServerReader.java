/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.ipc;

import com.bixly.ipc.exception.InvalidIPCUsageException;
import com.bixly.ipc.exception.UnexpectedIPCResponseException;
import com.bixly.util.LogUtil;
import com.bixly.util.TimeUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;

/**
 *
 * @author cevaris
 */
public class ServerReader extends Thread {
    
    private ClientHandler  clientHandle;
    private BufferedReader clientIn;
    private IPCProtocol    ipcProtocol;
    private String         clientId;
    
    /**
     * Threaded Signal
     */
    public  volatile boolean isReady = false;

    public ServerReader(ClientHandler client) {
        this.clientHandle      = client;
        this.ipcProtocol = client.getIPCProtocol();
        this.clientId    = client.getClientId();
    }

    @Override
    public void run() {
        
        Thread.currentThread().setName("ClientReader:"+this.clientId);
        
        try{
            //Link Reader
            prepClient();
            //Start Reading
            executeIPC();
            
            close();
        } catch (InvalidIPCUsageException e){
            e.printStackTrace(System.err);
        } catch (UnexpectedIPCResponseException e){
            e.printStackTrace(System.err);
        } catch (SocketException e){
            System.out.println("Closing Socket");
        } catch (IOException e){
            e.printStackTrace(System.err);
        }
        
    }
    
    private void executeIPC() throws 
            InvalidIPCUsageException, IOException, 
            UnexpectedIPCResponseException, SocketException{
        
        //while(client is sending requests){
        
        LogUtil.log(this.clientId,"Putting ClientHandler to work");
        
        
       
//        try{
            String clientRequest = null;
            while(this.clientHandle.isRunning()){
            //while(out.checkError()){

                LogUtil.log(this.clientId,"Waiting for incoming Client request...");
                setIsReady(true);
                while(this.clientHandle.isRunning() && (clientRequest = this.clientIn.readLine())!=null ){


                    // Catch Closing Clients
                    if(clientRequest.contains(IPCProtocol.FINISH)){
                        this.clientHandle.getServer().unregisterClient(this.clientId);
                        this.close();
                        continue;
                    }

                    LogUtil.log(this.clientId,"Recieved Client Request: "+clientRequest);
                    //Else catch any general requests
                    this.clientHandle.getIPCProtocol().receiveRequest(clientRequest);

                }
                LogUtil.log(this.clientId,"No Response from Client...");
                TimeUtil.skipToMyLou(0.5);
            }
//        }catch (SocketException e){
//            e.printStackTrace(System.err);
//        }        
//        LogUtil.log(this.clientId,"ClientHandler is finished");
//        
//        
//        if(this.ipcProtocol == null)
//            throw new InvalidIPCUsageException("Must define a Protocol before use");
//        
//        String response = null;
//
//        //Keep looping till quit
//        //while(this.client.isRunning()){
//            System.out.println("Ready To Execute Requests");
//            TimeUtil.skipToMyLou(0.5);
//            
//            setIsReady(true);
//            while(this.client.isRunning() && (response = this.clientIn.readLine()) != null) {
                
                
//                //Check for general register of client
//                if(response.contains(IPCProtocol.REGISTER_CLIENT)){
//                    this.client.setClientId(this.ipcProtocol.receiveRegister(response));
//                    //this.clientId = this.ipcProtocol.receiveRegister(response);
//                    System.out.println("See if Memory is linked with Client");
//                    System.out.println("Reader ClientId: "+this.clientId);
//                    this.client.getIPCManager().setIsReady(true);
//                    
//                    //Mark client as ready to excecute tasks
//                    LogUtil.log(this.clientId,"Client is ready!!");
//                    LogUtil.log(this.clientId,"ThreadName: "+Thread.currentThread().getName());
//                    LogUtil.log(this.clientId,"Waiting for tasks");
//                    continue;
//                }
//                
                //Handle Base Server request/responses
//                if(this.ipcProtocol.receiveResponse(response) != null){
//                    System.out.println("Successfull Handle of Received Request");
//                } else
//                    throw new UnexpectedIPCResponseException(response);
//            }
//            
            
            //If no tasks
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
        //}
    }
    
    
    private void prepClient() {
        //Create stream buffer reader/writer
        System.out.println("Initiating link to Reader...");
        try {
            clientIn  = new BufferedReader(new InputStreamReader(clientHandle.getSocket().getInputStream()));
        } catch (IOException e) {
            System.err.println("Could not Prepare Client: " + clientHandle.toString());
        }
        System.out.println("Initiated link to Reader");
    }
    
    
    public synchronized void close() {
            
        if (this.clientHandle != null){
            this.clientHandle.closeClient();
        }

        System.out.println("Closed Server SocketReader Thread");
    }

    public synchronized void updateClientId(String clientId) {
        this.clientId = clientId;
    }
    public synchronized void setIsReady(boolean val){
        this.isReady = val;
    }
    public synchronized boolean isReady(){
        return this.isReady;
    }

//    private void attachCleanup() {
//        Runtime.getRuntime().addShutdownHook(new Thread() {
//            @Override
//            public void run() {
//                System.out.print("Cleaning up...");
//                if(clientHandle != null &&
//                        clientHandle.isRunning && 
//                        clientIn!=null){
//                    clientHandle.unregisterClient();
//                }
//                System.out.print("Done wiht cleaning up");
//            }
//        });
//    }

    
    
}
