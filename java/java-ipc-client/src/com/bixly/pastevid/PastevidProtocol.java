/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid;

import com.bixly.ipc.Command;
import com.bixly.ipc.IPCProtocol;
import com.bixly.util.LogUtil;
import com.bixly.util.TimeUtil;
import java.util.regex.Pattern;

/**
 *
 * @author cevaris
 */
public class PastevidProtocol extends IPCProtocol {
    /**
     * ScreenCapture Application Specific Request/Responses
     */
    
    /**
     * Requests to base server for focus
     * - On recieving this server will broadcast to all instances 
     *   that there is a request for focus. If there are no 
     *   instances that are encoding, any recording instances will
     *   will pause and drop their audio lines for the requesting 
     *   instance. 
     */
    public final static String  REQUEST_RECORD       = "REQ_RECORD";
    public final static Pattern REGEX_REQUEST_RECORD = Pattern.compile(REQUEST_RECORD+" ([A-Z]*)",
            Pattern.CASE_INSENSITIVE);
    
    public final static String  REQUEST_PAUSE       = "REQ_PAUSE";
    public final static Pattern REGEX_REQUEST_PAUSE = Pattern.compile(REQUEST_PAUSE,
            Pattern.CASE_INSENSITIVE);
    
    /**
     * A state which the instance is currently recording
     */
    public final static String STATE_RECORDING = "STATE_RECORD";
    
    /**
     * A state which the instance is currently encoding
     */
    public final static String STATE_ENCODING  = "STATE_ENCODE";
    
    
    /**
     * Handle Pastevid's response
     * @param response 
     */
    @Override
    public String receiveResponse(String response) {
        System.out.println("Calling Correct Method:" +response);
        //Search for each possible Pastevid reponse
        //- Dispatch handler
        return response;
    }

    /**
     * Handle Pastevid's requests
     * @param request 
     */
    @Override
    public String receiveRequest(String request) {
        System.out.println("Calling Correct Method:" +request);
        //Search for each possible Pastevid reponse
        //- Dispatch handler
        return request;
    }
    
    
    
    public synchronized void requestRecord() {
        
        //Before any request, the client must be ready
        while(!this.ipcManager.isClientReady()){
            System.out.println("Waiting for the client to be ready");
            TimeUtil.skipToMyLou(0.5);
        }
        
        Command command = new Command();
        String request  = null;
        command.addArgument(REQUEST_RECORD);
        command.addArgument(this.ipcManager.getClientId());
        request = command.compile();
        
        LogUtil.log(this.ipcManager.getClientId(),"Creating new Request: "+request);
        //Add to queue
        this.sendRequest(command.compile());

        LogUtil.log(this.ipcManager.getClientId(),"Sent Request: "+command.compile());

    }

    public void requestClose() {
        //Before any request, the client must be ready
        this.ipcManager.shutdown();
    }

    
}