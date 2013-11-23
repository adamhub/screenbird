/*
 * LogUtil.java
 *
 * Version 1.0
 * 
 * 17 May 2013
 */
package com.bixly.pastevid.util;

import com.bixly.pastevid.structs.Queue;
import com.bixly.pastevid.Settings;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Utility class for logging messages.
 * @author cevaris
 */
public class LogUtil {
    
    private final static File logFile   = prepLogFile();
    private static FileWriter logWriter = prepLogWriter();
    
    /**
     * Queue of messages to be written to log file
     */
    private final static Queue<String> logQueue = new Queue<String>();
    
    /**
     * Thread which handles the logging of data
     */
    private final static LogWorkerThread logWorker = new LogWorkerThread();

    /**
     * If the logger is currently open for 
     */
    private static boolean isRunning = false;
    
    /*
     * Statically start the thread
     */
    static { logWorker.start(); }
    
    
    private static class LogWorkerThread extends Thread {

        public LogWorkerThread() {
            super("Log Handler");
        }
        
        @Override
        public void run() {
            logWriter = prepLogWriter();
            isRunning = true;
            
            try { 
               
                while (isLoggerReady()) {
                    // If there is nothing to write
                    if (logQueue.isEmpty()) {
                        // Wait 50 ms
                        TimeUtil.skipToMyLouMS(50L);
                        // Prepare to check again
                        continue;
                    }
                    
                    synchronized (this) {
                        // If there is at least one log to write
                        // write full queue atomically
                        while (!logQueue.isEmpty()) {
                            logWriter.write(logQueue.pop());
                        }
                        // Write buffer to file
                        logWriter.flush();
                    }
                    
                } // end While, logger is shutting down
                
                
                // Write out left over logs
                synchronized (this) {
                    while (!logQueue.isEmpty()) {
                        logWriter.write(logQueue.pop());
                    }
                    // Write buffer to file
                    logWriter.flush();
                }
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }catch (NullPointerException e) {
                e.printStackTrace(System.err);
            } finally {
                if (logWriter != null) {
                    try {
                        logWriter.close();
                    } catch (IOException e){
                        e.printStackTrace(System.err);
                    }
                }
            }
        }
    }
    
    /**
     * Checks file path of log directory. Creates if log directory does not
     * exists. Looks in log directory for next available log file. If a log file
     * logN.txt has more than 2MB, we create and return a new log file log(N+1).txt 
     * 
     * @return Log file pointer
     */
    private static File prepLogFile() {
        
        File hiddenDir = new File(Settings.HOME_DIR);
        File logDir    = new File(Settings.SCREENBIRD_LOG_DIR);
        File log       = new File(Settings.SCREENBIRD_LOG_DIR + "log.txt");
        int logNumber  = 1;
        
        try {
            if (!hiddenDir.exists()) {
                hiddenDir.mkdirs();
            }
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            // If log does not exists, create and select log
            // If log exists and is overflowed, check next log
            // If log exists and is not overflowed, select log
            while (log.exists() && isLogOverflow(log)) {
                // Step to next log
                log = new File(Settings.SCREENBIRD_LOG_DIR+"log"+logNumber+".txt");
                // Prep next step
                logNumber++;
            }
            
            if (!log.exists()) {
                log.createNewFile();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
        
        if (MediaUtil.osIsWindows()) {
            try {
                // Need to manually hide windows file
                Runtime.getRuntime().exec(new String[]{"attrib", "+s", "+h",hiddenDir.getAbsolutePath()}).waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        } // else Unix and Mac are already hidden if they start with '.'
        return log;
    }
    
    /**
     * Checks if log file has reached 2MB max size. 
     * @param log Log file pointer
     * @return True if log file size is larger than 2MB
     */
    private static boolean isLogOverflow(File log){
        if (log.length() <= Settings.LOG_MAX_SIZE) {
            // Log file is within max size
            return false;
        } else {
            // Log file over max size
            return true;
        }
    }

    /**
     * Statically creates file writer pointer to the log file
     * @return File writer pointer
     */
    private static FileWriter prepLogWriter() {
        if (logFile.exists()) {
            try {
                return new FileWriter(logFile, true);         
            } catch(IOException e) {
                e.printStackTrace(System.err);
            }
        }
        return null;
    }
    
    /**
     * Polymorphic method which checks the type of the passed in message Object. 
     * 
     * If message Object is of java.lang.Exception type, we unpack the stack trace into
     * a string so the stack trace can be included into the log. 
     * 
     * If the message Object is of java.lang.Object type, toString() is 
     * invoked be written to the log. 
     * 
     * @param clazz Class which initiated the log call
     * @param message Object which is to be written to log
     */
    public static void log(Class clazz, Object message) {
        if (message instanceof Exception) {
            
            StringBuilder report = new StringBuilder();
            Exception exception = (Exception) message;
            StackTraceElement[] stackTrace = exception.getStackTrace();
            
            // Add Exception Message
            report.append(String.format("%s%n", exception));
            
            synchronized(Settings.LOG_DATE_FORMAT){
            // Add the stack trace
                for (Object trace : stackTrace)
                    report.append(String.format("%s : %s - %s%n", Settings.LOG_DATE_FORMAT.format(System.currentTimeMillis()),clazz.getCanonicalName(), trace));
            }

            message = report.toString();
        }   
        
        if (message != null) {
            log(clazz, message.toString());
        }
    }
    
    public synchronized static void log(Class clazz, String message) {
        // Print to screen
        if (Settings.LOG_STDOUT) {
            System.out.println(message);
        }
        
        // Signal to control logging
        if (!Settings.LOG_DATA_TO_FILE) {
            return;
        }
        
        // Format message
        message = String.format("%s : %s - %s%n", 
                Settings.LOG_DATE_FORMAT.format(System.currentTimeMillis()),
                clazz.getCanonicalName(), 
                message);
        
        // Queue message
        logQueue.push(message);

    }
    
    /**
     * @return True if logger is ready for logging
     */
    public synchronized static boolean isLoggerReady() {
        return isRunning;
    }
    
    /**
     * Prints the clients java properties to log file
     */
    public static void printSystemProperties() {
        Properties p = System.getProperties();
        for (String key : Settings.JAVA_LOG_PROPERITES) {
            log(key + ": " + p.getProperty(key));
        }
    }

    private static void log(Object message) {
        LogUtil.log(LogUtil.class, message);
    }

    /**
     * Initiates the logger shutdown.
     */
    public synchronized static void close() {
        isRunning = false;
    }

    /**
     * Writes anything stored to file writer's buffer into log file.
     */
    public synchronized static void flush() {
        try {
            logWriter.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
