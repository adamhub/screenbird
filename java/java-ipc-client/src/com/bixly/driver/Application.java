/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.driver;

import com.bixly.ipc.IPCManager;
import com.bixly.pastevid.PastevidProtocol;
import com.bixly.util.RandomUtil;
import com.bixly.util.TimeUtil;

/**
 *
 * @author cevaris
 */
public class Application {


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        PastevidProtocol ipcProtocol  = new PastevidProtocol();
        IPCManager       ipcManager   = new IPCManager(ipcProtocol);
        //Using Pastvid Protocol
        ipcProtocol.setIpcManager(ipcManager);
        
        //Test Case
        ipcProtocol.requestRecord();
        
        
        TimeUtil.skipToMyLou(RandomUtil.rand.nextInt(7)+8);
        ipcProtocol.requestClose();
    }

    
}
