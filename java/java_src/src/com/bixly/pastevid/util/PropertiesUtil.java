/*
 * PropertiesUtil.java
 *
 * Version 1.0
 * 
 * 17 May 2013
 */
package com.bixly.pastevid.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Utility class for handling operations on the Screenbird property file.
 * @author cevaris
 */
public class PropertiesUtil {
    
    /**
     * Saves the property identified by key with the given value to the defined
     * propertyFile.
     * @param propertyFile
     * @param key
     * @param value 
     */
    public static void saveProperty(String propertyFile, String key, String value) {
        FileOutputStream out = null;
        FileInputStream in = null;
        
        try {
            File file = new File(propertyFile);
            
            // Creates directory of the property file if needed
            if (!file.getParentFile().exists()) {
                file.mkdirs();
            }
            
            // Creates property file if needed
            if (!file.exists()) {
                file.createNewFile();
            }
            
            // Read the contents of the current properties file
            Properties prop = new Properties();
            in = new FileInputStream(file.getAbsolutePath());
            prop.load(in);
            in.close();

            // Writes the new property
            out = new FileOutputStream(file.getAbsolutePath());
            prop.setProperty(key, value.toString());
            prop.store(out, "UserData");
            
            log("Saved " + key + ":" + value + " to property file " + file);
        } catch (IOException e){
            log(e);
        } finally {
            // Close the output stream
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log(e);
                }
            }
            
            // Close the input stream
            if (in != null) {
                try{
                    in.close();
                } catch (IOException e){
                    log(e);
                }
            }
        }
    }
    
    /**
     * Reads a property value identified by key from the given propertyFile.
     * @param propertyFile
     * @param key
     * @return the value of the given key
     */
    public static String loadProperty(String propertyFile, String key){
        if (key == null) {
            return null;
        }
        
        FileInputStream in = null;
        try {
            File file = new File(propertyFile);
            
            // Make sure property file exists
            if (!file.exists()) {
                return null;
            }
            
            // Read in metadata
            Properties prop = new Properties();
            in = new FileInputStream(file.getAbsolutePath());
            prop.load(in);
            in.close();
            
            // Return read value
            if (prop.getProperty(key) != null) {
                return prop.getProperty(key);
            }
        } catch (IOException e){ 
            log(e);
        } catch (NumberFormatException e){ 
            log(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e){
                    log(e);
                }
            }
        }
        
        // If exception is thrown, return default
        return "";
    }
    
    private static void log(Object message){
        LogUtil.log(PropertiesUtil.class, message);
    }
}
