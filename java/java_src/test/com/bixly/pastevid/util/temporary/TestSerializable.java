/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.util.temporary;

import java.io.Serializable;

/**
 *
 * @author cevaris
 */
public class TestSerializable implements Serializable{
    
    static final long serialVersionUID = 103325533232897495L;
        
    private int integer = 12;
    private String string = "I Scream for Ice Ice Cream";
    private int[] integerArr = {23,43,883,3232};

    public int getInteger() {
        return integer;
    }

    public String getString() {
        return string;
    }

    public int[] getIntegerArr() {
        return integerArr;
    }
    
}
