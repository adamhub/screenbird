/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.driver;

import com.bixly.ipc.Server;
import com.bixly.pastevid.PastevidProtocol;

/**
 *
 * @author cevaris
 */
public class Driver {


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        PastevidProtocol ipcProtocol = new PastevidProtocol();
        Server server  = new Server(ipcProtocol);
    }

    
}
