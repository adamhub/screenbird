/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.util;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;

/**
 *
 * @author Jorge
 */
public class MediaUtil {

    public static boolean playVideo(String pathToVideo) {
        boolean result = true;
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try {
                Desktop d = Desktop.getDesktop();
                d.open(new File(pathToVideo));
            } catch (IOException ex) {
                if (MediaUtil.osIsWindows()) {
                    try {
                        //Runtime.getRuntime().exec(System.getenv("ProgramFiles")+"\\Windows Media Player\\wmplayer.exe "+this.outputMovieFilename);
                        Runtime.getRuntime().exec("cmd /c \"start " + pathToVideo);
                    } catch (Exception ex1) {
                        result = false;
                    }
                } else {
                    result = false;
                }
            }
        } else {
            result = false;
        }
        return result;
    }

    public static boolean osIsWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("win") >= 0);
    }

    public static boolean osIsMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("mac") >= 0);
    }

    public static boolean osIsUnix() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0);
    }

    public static String getMacAddress() {
        String macAddress = "";
        try {
            InetAddress address = InetAddress.getLocalHost();

            NetworkInterface ni = NetworkInterface.getByInetAddress(address);
            byte[] mac = ni.getHardwareAddress();

            for (int i = 0; i < mac.length; i++) {
                macAddress += String.format("%02X%s", mac[i], (i < mac.length - 1) ? "" : "");
            }

        } catch (Exception e) {
        }

        return macAddress;
    }

    /**
     * Opens an URL.
     * 
     * @param url 
     */
    public static void navigateToUrl(String url) {
        try {
            URI uri = new URI(url);
            Desktop.getDesktop().browse(uri);
        } catch (Exception e) {
        }
    }
}
