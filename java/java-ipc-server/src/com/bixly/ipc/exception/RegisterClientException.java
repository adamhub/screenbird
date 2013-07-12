/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.ipc.exception;

/**
 *
 * @author cevaris
 */
public class RegisterClientException extends Exception {

    String message = "";
    
    public RegisterClientException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String toString() {
        return super.toString()+" "+message;
    }
    
    
}
