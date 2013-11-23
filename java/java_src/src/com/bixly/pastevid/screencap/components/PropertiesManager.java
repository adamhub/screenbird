/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.screencap.components;

import com.bixly.pastevid.util.LogUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author cevaris
 */
public class PropertiesManager {
    
    public final static int READ  = 0;
    public final static int WRITE = 1;
    
    /**
     * Out stream used to write to properties file
     */
    private FileOutputStream out = null;
    /**
     * Input stream used to read properties file
     */
    private FileInputStream  in  = null;
    /**
     * Properties instance which is loaded with previous serialized data or 
     * an empty properties document
     */
    private Properties properties = null;
    /**
     * File pointer to the properties file
     */
    private File propFile = null;
    
    private int state = 0;

    public PropertiesManager(String location) {
        this.propFile = new File(location);
    }
    public PropertiesManager(File propFile) {
        this.propFile = propFile;
    }

    /**
     * Reads the given properties file into a Properties object instance and returns.
     * If no properties file exists at given path, a properties file is created
     * and returned to the user.
     * 
     * @return 
     *      Properties file
     * @throws IOException  
     */
    public synchronized final Properties readPropertyFile()  throws IOException {
        
        if(!this.propFile.exists()){
            log("Creating properties file at "+propFile.getAbsolutePath());
            this.propFile.createNewFile();
        } 
        
        if(this.in != null){
            this.in.close();
            this.in = null;
        }
        
        this.in = new FileInputStream(this.propFile);
        //Create property object
        this.properties = new Properties();
        //Load the data to properties object
        this.properties.load(this.in);
        //Close reader
        this.in.close();
        
        return this.properties;
        
    }

    /**
     * Writes to the given properties file. If the properties file does not 
     * exists, a properties file is created and written to. 
     * 
     * @param properties
     *      Current properties object
     * @param title
     *      Description of properties file
     * @throws IOException 
     */
    public synchronized void writePropertyFile(Properties properties, String title) throws IOException {
        
        this.properties = properties;
        
        if(!this.propFile.exists()){
            log("Creating properties file at "+propFile.getAbsolutePath());
            this.propFile.createNewFile();
        } 
        
        if(this.out != null){
            this.out.close();
            this.out = null;
        }
        
        //Open property file to write data to
        this.out = new FileOutputStream(this.propFile);
        this.properties.store(this.out, title);
        
    }
        
    public FileOutputStream getOutputStream() {
        return out;
    }
    
    /**
     * Closes file pointer to java properties file
     */
    final public void closeProperties(){
        
        try {
            
            if(this.out != null) {
                this.out.close();
            }
            
            if(this.in != null){
                this.in.close();
            }
            
        } catch (IOException e) {
            log(e);
        }
        
    }
    
    final public File getPropertiesFile() {
        return this.propFile;
    }
    
    private static void log(Object message){
        LogUtil.log(PropertiesManager.class, message);
    }

}
