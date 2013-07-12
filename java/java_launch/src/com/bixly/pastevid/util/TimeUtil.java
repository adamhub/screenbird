/*
 * TimeUtil.java
 *
 * Version 1.0
 * 
 * 20 May 2013
 */
package com.bixly.pastevid.util;

/**
 *
 * @author cevaris
 */
public class TimeUtil {
    
    /**
     * Waits for n seconds.
     * @param seconds 
     */
    public static void skipToMyLou(int seconds) {
        skipToMyLou((double) seconds);
    }

    /**
     * Waits for n seconds.
     * @param seconds 
     */
    public static void skipToMyLou(double seconds) {
        try {
            Thread.sleep((long) (1000 * seconds));
        } catch (InterruptedException e) {
            System.err.print(e);
        }
    }
    
}
