/*
 * MediaUtil.java
 *
 * Version 1.0
 * 
 * 17 May 2013
 */
package com.bixly.pastevid.util;

import com.bixly.pastevid.Settings;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 *
 * @author Jorge
 */
public class MediaUtil {
    /**
     * Opens the given file's default application delegated by the client's
     * operating system.
     * @param fileToBeOpen File path of the requested file to be opened. 
     * @return 
     */
    public static boolean open(String fileToBeOpen) {
        if (MediaUtil.osIsWindows()) {
            return LibraryUtil.execute(new String[] {
                "cmd",
                "/c",
                ("start " + fileToBeOpen)                
            }, Settings.SCREEN_CAPTURE_DIR, true);
        } else if (MediaUtil.osIsMac()) {
            return LibraryUtil.execute(new String[] {
                "open",
                fileToBeOpen
            }, Settings.SCREEN_CAPTURE_DIR, true);
        } else if (MediaUtil.osIsUnix()) {
            return LibraryUtil.execute(new String[] {
                "/bin/bash",
                "-c",
                ("xdg-open "+fileToBeOpen)                
            }, Settings.SCREEN_CAPTURE_DIR, true);
        } else {
            log("Computer environment is not supported");
            return false;
        }
    }

    /**
     * Checks if the current operating system is Windows.
     * @return True if OS is Windows
     */
    public static boolean osIsWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("win") >= 0);
    }

    /**
     * Checks if the current operating system is Mac OS X.
     * @return True if OS is Mac
     */
    public static boolean osIsMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("mac") >= 0);
    }

    /**
     * Checks if the current operating system is UNIX/Linux.
     * @return True if OS is UNIX/Linux
     */
    public static boolean osIsUnix() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0);
    }
    
    /**
     * Returns the normalized name for the current operating system.
     * @return name of the current OS:<br/>
     * - mac<br/>
     * - linux<br/>
     * - windows<br/>
     */
    public static String getNormalizedOSName() {
        if (MediaUtil.osIsMac()) {
            return "mac";
        } else if (MediaUtil.osIsUnix()) {
            return "linux";
        } else {
            return "windows";
        }
    }

    /**
     * Returns the MAC address for the current computer.
     * @return 
     */
    public static String getMacAddress() {
        StringBuilder macAddress = new StringBuilder();
        try {
            InetAddress address = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByInetAddress(address);
            byte[] mac = ni.getHardwareAddress();
            
            for (int i = 0; i < mac.length; i++) {
                macAddress.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "" : ""));
            }

        } catch (SocketException e) {
            log(e);
        } catch (UnknownHostException e){
            log(e);
        }
        return macAddress.toString();
    }

    private static void log(Object message){
        LogUtil.log(MediaUtil.class, message);
    }
}
