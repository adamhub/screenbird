/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.ipc.exception;

/**
 *
 * @author cevaris
 */
public class InvalidIPCUsageException extends Exception{
    
    String message = "";

    public InvalidIPCUsageException(String message) {
        super(message);
        this.message = message;
    }

}
