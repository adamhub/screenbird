package com.bixly.pastevid.util;

/**
 * @author Bixly
 */
public class DebugUtil {
    public static boolean debug = true;
    
    public static void error(String msg) {
        if (debug) {
            System.err.println(msg);
        }
    }
    
    public static void out(String msg) {
        if (debug) {
            System.out.println(msg);
        }
    }
}
