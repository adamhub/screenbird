/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.util;

/**
 *
 * @author cevaris
 */
public class LogUtil {
    
    
    public static void log(String clientId, String message){
        if(clientId == null)
            System.out.println("NULL "+message);
        else
            System.out.println(clientId+" "+message);
            
    }
    public static void errorlog(String clientId, String message){
        if(clientId == null)
            System.err.println("NULL "+message);
        else
            System.err.println(clientId+" "+message);
    }
    
}
