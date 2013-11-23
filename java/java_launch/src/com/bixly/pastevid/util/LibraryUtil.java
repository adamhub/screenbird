/*
 * LibraryUtil.java
 *
 * Version 1.0
 * 
 * 20 May 2013
 */
package com.bixly.pastevid.util;

import com.bixly.pastevid.driver.Launch;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 * @author cevaris
 */
public class LibraryUtil {
    
    /**
     * Executes a shell command given as an ArrayList.
     * @param args
     * @param binDir
     * @param waitFor
     * @param progressBarListener
     * @return 
     */
    public static boolean execute(String[] args, String binDir, boolean waitFor) {
        
        if (args == null) {
            return false;
        }
        System.out.print("Executing ");
        for (String arg : args) {
            System.out.print(arg+" ");
        }
        System.out.println("");
        
        try {
            File destDirFile = prepDestinationDir(binDir);
            // Initalize process to execute jar extraction
            ProcessBuilder b = new ProcessBuilder(args);
            
            if (binDir != null) {
                // Change working directory to destination directory for extraction
                b.directory(destDirFile);
            }
                
            Process p = b.start();
            
            // If true, wait till process returns/exits
            if (waitFor) {
                p.waitFor();
            }
            
            return true;
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
        }
        
        return false;
    }

    /**
     * Executes a script.
     * @param args
     * @param workingDir
     * @return 
     */
    public static boolean executeScript(String[] args, File workingDir) {
        
        if (args == null) {
            return false;
        }
        System.out.print("Executing ");
        for (String arg : args) { 
            System.out.print(arg+" "); 
        }
        System.out.println("");
        
        LibraryUtil.chmod("755", new File(args[0]));
        try {
            // Initalize process to execute jar extraction
            ProcessBuilder b = new ProcessBuilder(args);
            // System.out.println(b.toString());
            //Change working directory to destination directory for extraction
            b.directory(workingDir);
            
            b.start();
            
            return true;
        }catch (IOException ex){
            ex.printStackTrace(System.err);
        }
        
        return false;
    }

    /**
     * Executes a script.
     * @param args
     * @return 
     */
    public static boolean executeScript(String[] args) {
        
        if(args == null) return false;
        System.out.print("Executing ");
        for(String arg : args){ System.out.print(arg+" "); }
        System.out.println("");
        
        LibraryUtil.chmod("755", new File(args[0]));
        try{
            
            Runtime runtime = Runtime.getRuntime();
            runtime.exec(args);
            
            return true;
        }catch (IOException ex){
            ex.printStackTrace(System.err);
        }
        
        return false;
    }

    /**
     * Actual path to ffmpeg executable
     */
    private String executablePath;

    public LibraryUtil() {
    }
    
    /**
     * Downloads a file via HTTP request from a given url. File 'filename' is 
     * stored in the directory given. 
     * @param urlSource Well formed URL location
     * @param destDir Directory which the file is to be saved
     * @param filename The file name which the downloaded file is saved with
     * @return True if successful, false otherwise
     */
    public static void wget(String urlSource, String destDir, String filename){
        
        URL url;
        InputStream          stream = null;
        BufferedOutputStream out    = null;
        BufferedInputStream  in     = null;
        URLConnection        connection = null;
        FileOutputStream     outputFile = null;

        File destDirFile = prepDestinationDir(destDir);
        File destFile    = new File(destDirFile.getAbsolutePath()+File.separator+filename);
        
        try {
            url = new URL(urlSource);
            // Open the connection
            connection = url.openConnection();
            connection.setUseCaches(false);
            
            if (destFile.exists()) {
                System.out.println(String.format("Downloading file Remote[Length=%d, LastModfied=%d] Local [Length=%d, LastModfied=%d]", 
                        connection.getContentLength(), 
                        connection.getLastModified(), 
                        destFile.length(), 
                        destFile.lastModified()));
            }
            
            //If the web server's file lastmodified is equal to client's file lastmodifed
            if(destFile.exists() &&
                    (connection.getLastModified() <= destFile.lastModified()) &&
                    (connection.getContentLength() == destFile.length())) {
                System.out.println("Loading cached file " + filename);
                return;
            } else if(destFile.exists()) {
                System.out.println("Downloading new file copy of " + filename);
                destFile.delete();
            } 
            
            stream = connection.getInputStream();
            
            System.out.println("Starting download: "+urlSource);
            //Set up input stream
            in = new BufferedInputStream(stream);
            
            //Set up output stream
            outputFile = new FileOutputStream(destFile);
            out = new BufferedOutputStream(outputFile);
            
            int i;
            //Write file to destination
            while ((i = in.read()) != -1) { out.write(i); }
            out.flush();
            
            System.out.println("Finished downloading: "+destDirFile.getAbsolutePath()+File.separator+filename);
            //Imprints server's lastmodified value to cahce this file
            destFile.setLastModified(connection.getLastModified());
            
        } catch(IOException e){
            e.printStackTrace(System.err);
        } finally{
            try {
                if(out != null) out.close();
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
            try {
                if(in != null) in.close();
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
            try {
                if(stream != null) stream.close();
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        }
    }
    
    
    public static boolean chmod(String permissions, File file){
        
        if(permissions == null && permissions.length() < 0) return false;
        
        Runtime runtime = Runtime.getRuntime();
        try {
            if (MediaUtil.osIsMac() || MediaUtil.osIsUnix()) {
                // Assign executable permissions
                Process process = runtime.exec(new String[]{"/bin/chmod", permissions, file.getAbsolutePath()});
                // Wait for process to finish before returning
                // This is a must especially if file is going to be used after this 
                // method is executed
                process.waitFor();
            } else {
                // Simple hack
                System.err.println("Need to implement Windows' permissions");
                file.setExecutable(true);
                file.setWritable(true);
                file.setReadable(true);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace(System.err);
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
        }
        return false;
    }
    
    /**
     * Creates a directory by making it writable and executable.
     * @param destDir Directory to be constructed
     * @return The directory which has been created
     */
    public static File prepDestinationDir(String destDir){
        
        File binDirectory = new File(destDir);
        
        System.out.println("Making Directory: "+binDirectory.getAbsolutePath());
        
        if (!binDirectory.exists()) {
            binDirectory.mkdirs();
        }
        if (!binDirectory.canWrite()) {
            binDirectory.setWritable(true);
        }
        if (!binDirectory.canExecute()) {
            binDirectory.setExecutable(true);
        }
        
        System.out.println("Created Directory: "+binDirectory.exists()+" : "+binDirectory.getAbsolutePath());
        
        binDirectory.deleteOnExit();
        
        return binDirectory;
    }
    
    /**
     * Uses a default URL to grab FFMpeg libraries. 
     * @return URL to be used
     */
    public static  String getUrl() {
        String baseURL = "";
        if(Launch.user == null || Launch.user.getBaseURL().length() == 0){
            //Default URL
            baseURL = "http://screenbird.com/";
        } else 
            //Assigns runtime codebase
            baseURL = Launch.user.getBaseURL();
        return baseURL;
    }

    /**
     * Returns the ffmpeg executable path to be used
     * @return 
     */
    public String getFFmpegPath() {
        return this.executablePath;
    }
}
