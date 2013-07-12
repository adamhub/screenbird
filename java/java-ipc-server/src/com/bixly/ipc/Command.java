/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.ipc;

/**
 *
 * @author cevaris
 */
public class Command{
    StringBuilder command;

    public Command() {
        command = new StringBuilder();
    }

    public void addArgument(String arg){
        this.command.append(arg);
        this.command.append(' ');
    }
    public void addArgument(int arg){
        this.command.append(arg);
        this.command.append(' ');
    }
    public String compile(){
        return (command.toString()).trim();
    }
}
