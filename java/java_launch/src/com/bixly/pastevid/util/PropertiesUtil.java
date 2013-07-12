/*
 * PropertiesUtil.java
 *
 * Version 1.0
 * 
 * 20 May 2013
 */
package com.bixly.pastevid.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
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
    public static void saveProperty(String propertyFile, String key, String value){
        try{
            FileOutputStream out;
            File file = new File(propertyFile);
            
            //Creates directory if needed
            if(!file.getParentFile().exists()){
                file.getParentFile().mkdirs();
            }
            //Creates property file if needed
            if(!file.exists()){
                file.createNewFile();
            }
            
            Properties prop = new Properties();
            FileInputStream in = new FileInputStream(file.getAbsolutePath());
            prop.load(in);
            in.close();

            out = new FileOutputStream(file.getAbsolutePath());
            prop.setProperty(key, value.toString());
            prop.store(out, "UserData");
            out.close();
            
            System.out.println("Saved "+key+":"+value);
           
        } catch (IOException e) {
            System.err.println(e);
        }
    }
    
    /**
     * Reads a property value identified by key from the given propertyFile.
     * @param propertyFile
     * @param key
     * @return the value of the given key
     */
    public static String loadProperty(String propertyFile, String key){
        
        if(key == null) return null;
        
        try{
            FileInputStream in = null;
            File file = new File(propertyFile);
            
            //Return default if metadata not found
            if(!file.exists()){return null;}
            //Read in metadata
            Properties prop = new Properties();
            in = new FileInputStream(file.getAbsolutePath());
            prop.load(in);
            in.close();
            
            //If data is not found, return default
            if(prop.getProperty(key) != null) 
                return prop.getProperty(key);
                
        } catch (IOException e){ 
            e.printStackTrace(System.err);
        } catch (NumberFormatException e){ 
            e.printStackTrace(System.err);
        }
        //If exception is thrown, return default
        return null;
    }
    
}
