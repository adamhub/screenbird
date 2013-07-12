/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.util;

/**
 *
 * @author cevaris
 */
public class TimeUtil {
    
    
    public static void skipToMyLou(int seconds) {
        skipToMyLou((double) seconds);
    }

    public static void skipToMyLou(double seconds) {
        try {
            Thread.sleep((long) (1000 * seconds));
        } catch (InterruptedException e) {
            System.err.print(e);
        }
    }
    
}
