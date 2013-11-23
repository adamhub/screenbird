/*
 * ImageDoesNotExistException.java
 *
 * Version 1.0
 * 
 * 17 May 2013
 */
package com.bixly.pastevid.util;

/**
 * Custom exception for missing images.
 * @author cevaris
 */
class ImageDoesNotExistException extends Exception {

    public ImageDoesNotExistException(String string) {
        super(string);
    }
    
}
