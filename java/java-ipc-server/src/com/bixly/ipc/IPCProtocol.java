/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.ipc;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author cevaris
 */
public class IPCProtocol {
    
    protected Server server = null;
    /**
     * General Handshaking Request/Responses such as ACK, FIN
     * 
     * Request Format:
     * [UNIQUE_ID||BS] TASK [DATA||NULL]
     * 
     * 
     */
    /**
     * Register Client with given unique id
     */
    public final static String  REGISTER_CLIENT = "REG";
    public final static Pattern REGEX_REGISTER_CLIENT = Pattern.compile(REGISTER_CLIENT+" ([A-Z]*)",
            Pattern.CASE_INSENSITIVE);
    /**
     * Default response to any request
     */
    public final static String  ACKNOWLEDGEMENT = "ACK";
    public final static Pattern REGEX_ACKNOWLEDGEMENT = Pattern.compile(ACKNOWLEDGEMENT, Pattern.CASE_INSENSITIVE);
    /**
     * Requests server for disconnection
     */
    public final static String  FINISH = "FIN";
    public final static Pattern REGEX_FINISH = Pattern.compile(FINISH, Pattern.CASE_INSENSITIVE);
    
    /**
     * A state which the instance is currently not being used
     */
    public final static String STATE_IDLE  = "STATE_IDLE";
    
    public synchronized void setServer(Server server){
        this.server = server;
    }

    public String buildACK(){
        Command command = new Command();
        command.addArgument(ACKNOWLEDGEMENT);
        return (command.compile()).trim();
    }
    
    public String buildRegister(String clientId) {
        Command command = new Command();
        command.addArgument(REGISTER_CLIENT);
        command.addArgument(clientId);
        return command.compile();
    }
    
    public String buildRegister(){
        Command command = new Command();
        command.addArgument(REGISTER_CLIENT);
        return command.compile();
    }
    public String buildFinsh(){
        Command command = new Command();
        command.addArgument(FINISH);
        return command.compile();
    }
    
    public String receiveResponse(String response) {
        throw new UnsupportedOperationException("This method should be overriden by Custom Protocal");
    }
    public String receiveRequest(String clientRequest) {
        throw new UnsupportedOperationException("This method should be overriden by Custom Protocal");
    }
    
    public String receiveRegister(String response){
        
        System.out.println("Attempting to regsiter with: "+response);
        
        if (response == null) return null;
        
        
        //Load regex
        Matcher matcher = REGEX_REGISTER_CLIENT.matcher(response);

        System.out.println("Loaded Regex");
        //Parse string and classify
        if(matcher.find() && matcher.group() != null){
            System.out.println("Found ID: "+matcher.group(1));
            return matcher.group(1);
        }else
            return null;
    }

    public boolean receiveAcknowledgement(String response){
        
        if (response == null) return false;
        //Load regex
        Matcher matcher = REGEX_ACKNOWLEDGEMENT.matcher(response);

        //Parse string and classify
        if(matcher.find())
            return true;
        else
            return false;
    }

    public boolean receiveFinish(String response){
        
        if (response == null) return false;
        
        //Load regex
        Matcher matcher = REGEX_FINISH.matcher(response);

        //Parse string and classify
        if(matcher.find())
            return true;
        else
            return false;
    }
    
    
//    protected void sendRequest(String request) {
//        this.ipcManager.getClient().queueRequest(request);
//    }
//    
//    /**
//     * Schedules a timer to prevent socket block
//     */
//    protected  void scheduleTimeout() {
//        TimerTask responseTimeout = new TimerTask() {
//            @Override
//            public synchronized void run() {
//                LogUtil.errorlog(ipcManager.getClientId(),"Timeout is called!!");
//                isWaitForTask = false;
//            }
//        };
//        LogUtil.log(this.ipcManager.getClientId(),"Setting Timeout");
//        this.responstTimeout = new Timer();
//        this.responstTimeout.schedule(responseTimeout, 2000);
//        LogUtil.log(this.ipcManager.getClientId(),"Timeout is set");
//    }
    
    
    
    protected synchronized  boolean broadcastRequest(String message, String excludeRequester) {
        
        System.out.println("Base Server Broadcast: "+message);
        
//        System.out.println("this.server == null "+(this.server == null));
//        System.out.println("message != null "+(message == null));
//        System.out.println("(message.trim().equalsIgnoreCase(\"\") "+ message.trim().equalsIgnoreCase(""));
//        System.out.println("this.server.getClientHandlers() == null "+this.server.getClientHandlers() == null);
//        System.out.println("this.server.getClientHandlers().size() <= 1 "+(this.server.getClientHandlers().size() <= 1));
//       
        //Do not broadcast if with any of these scenearios 
        if (this.server == null || message == null || (message.trim().equalsIgnoreCase(""))
                || this.server.getClientHandlers() == null)
                //Do not broadcast if only one user
                //|| this.server.getClientHandlers().size() <= 1)
            return false;
        
        if(this.server.getClientHandlers().size() <= 1){
            System.out.println("Only one client, Halting Broadcast");
            return false;
        }
            
        
        HashMap<String, ClientHandler> clients = this.server.getClientHandlers();
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            String clientId = entry.getKey();
            ClientHandler client = entry.getValue();
            
            //Skip requester if found
            if(excludeRequester != null && clientId.equalsIgnoreCase(excludeRequester))
                continue;
            
            client.sendClientRequest(message);
        }
                
        return true;
        
    }
    
    protected synchronized boolean broadcastRequest(String message) {
        return broadcastRequest(message, null);
    }

    
    
    
}
