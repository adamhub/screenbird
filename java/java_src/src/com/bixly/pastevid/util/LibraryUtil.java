/*
 * LibraryUtil.java
 *
 * Version 1.0
 * 
 * 17 May 2013
 */
package com.bixly.pastevid.util;

import com.bixly.pastevid.Session;
import com.bixly.pastevid.Settings;
import com.bixly.pastevid.screencap.components.progressbar.FFMpegProgressBarListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Utility class for setting up the Screenbird libraries.
 * @author cevaris
 */
public class LibraryUtil {
    
    public static boolean execute(String[] args, String binDir, boolean waitFor) {
        return execute(new ArrayList<String>(Arrays.asList(args)), binDir, waitFor, null);
    }
    
    public static boolean execute(String[] args, String binDir, boolean waitFor, FFMpegProgressBarListener progressBarListener) {
        return execute(new ArrayList<String>(Arrays.asList(args)), binDir, waitFor, progressBarListener);
    }
    
    public static boolean execute(ArrayList<String> args, String binDir, boolean waitFor) {
        return execute(args, binDir, waitFor, null);
    }    
    
    /**
     * Executes a shell command given as an ArrayList.
     * @param args      the command as an ArrayList
     * @param binDir    directory for the executable libraries
     * @param waitFor   if true, wait until the process returns/exists
     * @param progressBarListener   listener for the command progress. Used in FFMpeg execution
     * @return 
     */
    public static boolean execute(ArrayList<String> args, String binDir, 
            boolean waitFor, FFMpegProgressBarListener progressBarListener) {
        
        if (args == null) {
            return false;
        }
        
        // Build the command string
        StringBuilder command = new StringBuilder();
        for (String arg : args) {
            command.append(arg.toString()); command.append(" ");
        }
        log("Executing " + command.toString());
        
        
        BufferedReader reader = null;
        ProcessBuilder processBuilder = null;
        Process process = null;
        
        try {
            File destDirFile = prepDestinationDir(binDir);
            
            processBuilder = new ProcessBuilder(args);
            
            if (binDir != null) {
                // Change working directory to destination directory for extraction
                processBuilder.directory(destDirFile);
                
                // Special commands where the command is already included in the
                // OS and is not found in the bin directory
                if ((MediaUtil.osIsUnix() || MediaUtil.osIsMac()) && args.get(0).equals("unzip")
                        || args.get(0).startsWith("/")
                        || MediaUtil.osIsMac() && args.get(0).equals("open")
                        || MediaUtil.osIsWindows() && args.get(0).equals("cmd")
                        || MediaUtil.osIsWindows() && args.get(0).endsWith("handbrake.exe")) {
                    args.set(0, args.get(0));
                } else {
                    args.set(0, destDirFile + Settings.FILE_SEP + args.get(0));
                }
            }
            
            processBuilder.redirectErrorStream(true);
            
            process = processBuilder.start();
            log("Waiting for command " + args.get(0));
            
            // If true, wait till process returns/exits
            if (waitFor) {
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));          
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.length() > 0){
                        if (Settings.PRINT_EXEC_TO_CONSOLE) {
                            log(line);
                        }

                        if ((progressBarListener != null && progressBarListener.getId() == FFMpegProgressBarListener.HANDBRAKE)) {
                            progressBarListener.parseTimeInfoHandbrake(line);
                        }
                    }

                }
                log("Application " + args.get(0) + " ended with " + process.waitFor());
            } else {
                log("Application Executed " + args.get(0));
            }
            return true;
        } catch (IOException e) {
            log(e);
        } catch (InterruptedException e) {
            log(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log(e);
                }
            }
            
            if (process != null) {
                process.destroy();
            } 
        }
        return false;
    }

    /**
     * Executes a local script.
     * @param args
     * @param workingDir
     * @return 
     */
    public static boolean executeScript(String[] args, File workingDir) {
        
        if (args == null) {
            return false;
        }
        
        LibraryUtil.chmod("755", new File(args[0]));
        try {
            // Initalize process to execute jar extraction
            ProcessBuilder b = new ProcessBuilder(args);
            // Change working directory to destination directory for extraction
            b.directory(workingDir);
            
            b.start();
            return true;
        } catch (IOException ex) {
            log(ex);
        }
        
        return false;
    }
    
    /**
     * Executes a script.
     * @param args
     * @return 
     */
    public static boolean executeScript(String[] args) {
        
        if (args == null) {
            return false;
        }
        
        LibraryUtil.chmod("755", new File(args[0]));
        try {
            Runtime runtime = Runtime.getRuntime();
            runtime.exec(args);
            
            return true;
        } catch (IOException e) {
            log(e);
        }
        
        return false;
    }

    /**
     * Downloads a file via HTTP request from a given url. File 'filename' is 
     * stored in the directory given. 
     * @param urlSource Well formed URL location
     * @param destDir Directory which the file is to be saved
     * @param filename The file name which the downloaded file is saved with
     * @return True if successful, false otherwise
     */
    public static boolean wget(String urlSource, String destDir, String filename) {
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
                log(String.format("Downloading file Remote[Length=%d, LastModfied=%d] Local [Length=%d, LastModfied=%d]",
                        connection.getContentLength(), connection.getLastModified(),destFile.length(), destFile.lastModified()));
            }
            
            // If the web server's file lastmodified is equal to client's file lastmodifed
            if (destFile.exists() 
                    && (connection.getLastModified() <= destFile.lastModified()) 
                    && (connection.getContentLength() == destFile.length())) {
                log("Loading cached file " + filename);
                return false;
            } else if (destFile.exists()) {
                log("Downloading new file copy of " + filename);
                destFile.delete();
            } 
            
            stream = connection.getInputStream();
            
            // Set up input stream
            in = new BufferedInputStream(stream);
            
            // Set up output stream
            outputFile = new FileOutputStream(destFile);
            out = new BufferedOutputStream(outputFile);
            
            int i;
            int bite = 0; //byte is a keyword
            // Write file to destination 
            while ((i = in.read()) != -1) { 
                out.write(i); 
                bite++;
            }
            out.flush();
            
            System.out.println("Finished downloading: "+destDirFile.getAbsolutePath()+File.separator+filename);
            
            // Imprints server's lastmodified value to cahce this file
            destFile.setLastModified(connection.getLastModified());
            
            // Make sure it is executable
            LibraryUtil.chmod("777", destFile);
            destFile.setExecutable(true);
            
            return true;
            
        } catch (java.net.SocketTimeoutException e) {
            e.printStackTrace(System.err);
        } catch(IOException e) {
            e.printStackTrace(System.err);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        }
        
        return false;
    }
    
    /**
     * Unzips a given file identified by source.
     * @param source
     * @return true if unzip is successful.
     */
    public static boolean unzip(String source){
        return unzip(new File(source));
    }
    
    /**
     * Unzips a given File
     * @param source
     * @return true if unzip is successful
     */
    public static boolean unzip(File source){
        String execName = "unzip";
        
        if (!source.exists()) {
            return false;
        }
        
        return LibraryUtil.execute(new String[]{
                    execName,   //  unzip 
                    "-o",       // overwrite any unzipped files
                    source.getAbsolutePath(), // Compressed file
                },
                Settings.BIN_DIR, 
                true
        );
    }
    
    /**
     * Changes the permissions of the given file.
     * @param permissions
     * @param file
     * @return 
     */
    public static boolean chmod(String permissions, File file) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        
        Runtime runtime = Runtime.getRuntime();
        try {
            if (MediaUtil.osIsMac() || MediaUtil.osIsUnix()) {
                // Assign executable permissions
                Process process = runtime.exec(new String[]{"/bin/chmod", permissions, file.getAbsolutePath()});
                // Wait for process to finish before returning
                // This is a must especially if file is going to be used after 
                // this method is executed
                process.waitFor();
            } else {
                // Simple hack
                log("Need to implement Windows' permissions");
                file.setExecutable(true);
                file.setWritable(true);
                file.setReadable(true);
            }
            return true;
        } catch (IOException e) {
            log(e);
        } catch (InterruptedException e) {
            log(e);
        }
        return false;
    }
    
    /**
     * Creates a directory by making it writable and executable.
     * @param destDir Directory to be constructed
     * @return The directory which has been created
     */
    public static File prepDestinationDir(String destDir) {
        File binDirectory = new File(destDir);
        
        if (!binDirectory.exists()) {
            binDirectory.mkdirs();
        }
        if (!binDirectory.canWrite()) {
            binDirectory.setWritable(true);
        }
        if (!binDirectory.canExecute()) {
            binDirectory.setExecutable(true);
        }
        
        binDirectory.deleteOnExit();
        return binDirectory;
    }
    
    /**
     * Returns a default URL to grab FFMpeg libraries. 
     * @return URL to be used
     */
    public static  String getUrl() {
        String baseURL;
        if (Session.getInstance() == null || Session.getInstance().user.getBaseURL().length() == 0) {
            // Default URL
            baseURL = "http://screenbird.com/";
        } else { 
            //Assigns runtime codebase
            baseURL = Session.getInstance().user.getBaseURL();
        }
        return baseURL;
    }

    private static void log(Object message) {
        LogUtil.log(LibraryUtil.class, message);
    }
}
