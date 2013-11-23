/*
 * TimeUtil.java
 *
 * Version 1.0
 * 
 * 17 May 2013
 */
package com.bixly.pastevid.util;

/**
 * Utility class for triggering waiting times.
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
     * Wait for n seconds.
     * @param seconds 
     */
    public static void skipToMyLou(double seconds) {
        try {
            Thread.sleep((long) (1000 * seconds));
        } catch (InterruptedException e) {
            log(e);
        }
    }

    /**
     * Waits for n milliseconds.
     * @param milliseconds 
     */
    public static void skipToMyLouMS(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            log(e);
        }
    }
    
    /**
     * Waits for n milliseconds.
     * @param milliseconds
     * @throws InterruptedException 
     */
    public static void skipToMyLouMSWithException(long milliseconds) throws InterruptedException {
        Thread.sleep(milliseconds);
    }
    
    private static void log(Object message){
        LogUtil.log(MediaUtil.class, message);
    }
}
