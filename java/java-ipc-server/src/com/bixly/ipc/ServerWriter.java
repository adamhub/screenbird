/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.ipc;

import com.bixly.util.LogUtil;
import com.bixly.util.Queue;
import com.bixly.util.TimeUtil;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;

/**
 *
 * @author cevaris
 */
public class ServerWriter extends Thread {
    
    private ClientHandler client;
    private String        clientId;
    private PrintWriter   clientOut;
    //private IPCProtocol   ipcProtocol;
    private Queue<String> queue;
    
    /**
     * Threaded Signal
     */
    public  volatile boolean isReady = false;

    public ServerWriter(ClientHandler client) {
        this.client = client;
        this.queue  = client.getRequestQueue();
        //this.ipcProtocol = client.getIPCProtocol();
    }
    
    @Override
    public void run() {
        
        Thread.currentThread().setName("ClientWriter:"+this.clientId);
        
        try{
            
            prepClient();
            
            executeIPC();
        }catch (NullPointerException e){
            e.printStackTrace(System.err);
        }
//        catch (IOException e){
//            e.printStackTrace(System.err);
//        }catch (UnexpectedIPCResponseException e){
//            e.printStackTrace(System.err);
//        }catch (InvalidIPCUsageException e){
//            e.printStackTrace(System.err);
//            this.client.closeClient();
//        }
        
    }
    
//    public synchronized void registerClient() throws IOException, UnexpectedIPCResponseException, InvalidIPCUsageException{
//        
//        if(this.ipcProtocol == null)
//            throw new InvalidIPCUsageException("Must define a Protocol before use");
//        
//        String command = this.ipcProtocol.buildRegister();
//        //While client is not registered
//        while(this.clientId == null){
//            
//            //Send Regiser client command
//            this.clientOut.println(command);
//            
//            System.out.println("Waiting to register");
//            TimeUtil.skipToMyLou(0.5);
//            //Resend Client request to register
//            this.clientOut.println(command);
//            System.out.println("Resent Register Command");
//        }
//        System.out.println("Finished Registering Client");
//    }
    
    private void prepClient() {
        //Create stream buffer reader/writer
        LogUtil.log(this.clientId,"Initiating link to Writer...");
        try {
            clientOut = new PrintWriter(client.getSocket().getOutputStream(), true);
        } catch (IOException e) {
            LogUtil.errorlog(this.clientId,"Could not Prepare Server Writer: " + client.toString());
        }
        
        LogUtil.log(this.clientId,"Initiated link to Writer");
    }
    
    public synchronized void close() {
            
        if (this.client != null){
            this.client.closeClient();
        }

        LogUtil.log(this.clientId,"Closed Server SocketWriter Thread");
    }

    private void executeIPC() {
        //String response = null;
        //Client is now registered, so retreive clientId
        //this.clientId = this.client.getClientId();
        
//        LogUtil.log(this.clientId,"Client is ready!!");
//        LogUtil.log(this.clientId,"ThreadName: "+Thread.currentThread().getName());
//        LogUtil.log(this.clientId,"Waiting for tasks");

        setIsReady(true);
        //Keep looping till quit
        while(this.client.isRunning() && !this.clientOut.checkError()){
            
            if(this.clientOut.checkError()){
                LogUtil.log(this.clientId,"Found Out Stream CheckError Flag");
                this.close();
                break;
            }
            
            //If no tasks
            if(this.queue.isEmpty()){
                //Listen to base server for task
                TimeUtil.skipToMyLou(0.5);
                continue;
            }//Sending a Request, new outgoing
                
            //Get request
            String request = this.queue.pop();
            LogUtil.log(this.clientId,"Found Server Request, sending new Request: "+request);

            LogUtil.log(this.clientId,"Sending new Request: "+request);
            //Send Request
            this.clientOut.println(request);
//            this.client.setSoTimeout(2000);

            LogUtil.log(this.clientId,"Sent new Request: "+request);

            //LogUtil.log(this.clientId,"Waiting for Response");
            //Wait for response
//            while(this.isRunning() && (response = this.clientIn.readLine()) != null) {
//                //Base Server sends us client id
//                LogUtil.log(this.clientId,"Incoming: " + response);
//                if(this.ipcProtocol.receiveResponse(response) != null){
//                    //Send ACK
//                    this.clientOut.println(this.ipcProtocol.sendACK());
//                } else
//                    throw new UnexpectedIPCResponseException(response);
//            }
        }
        
        this.close();
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

//    public synchronized boolean isRunning() {
//        if (this.clientOut != null)
//            return !this.clientOut.checkError();
//        else
//            return true;
//    }
}
