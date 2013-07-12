/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.ipc.exception;

/**
 *
 * @author cevaris
 */
public class UnexpectedIPCResponseException extends Exception {
    
    String message = "";

    public UnexpectedIPCResponseException(String message) {
        super(message);
        this.message = message;
    }

}
