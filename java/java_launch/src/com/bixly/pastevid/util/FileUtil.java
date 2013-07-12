/*
 * FileUtil.java
 *
 * Version 1.0
 * 
 * 20 May 2013
 */
package com.bixly.pastevid.util;

import com.bixly.pastevid.Settings;
import com.bixly.pastevid.driver.Launch;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;


public class FileUtil {

    private static FileUtil instance;
    /**
     * Deletes all files(having the specified extension) in the specified directory
     *
     * @param directory
     * @param fileExtension
     * @throws Exception
     */
    private static final String baseDigits = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static FileUtil getInstance() {
        if (instance == null) {
            instance = new FileUtil();
        }
        return instance;
    }

   
    public static void deleteFiles(String directory, String dontDelete) throws Exception {
        File dir = new File(directory);
        String[] list = dir.list();
        if (list.length > 0) {
            for (int i = 0; i < list.length; i++) {
                File file = new File(directory, list[i]);
                if (!file.getPath().equals(dontDelete))
                    file.delete();
            }
        }
    }
    public static void deleteFiles(String directory) throws Exception {
        File dir = new File(directory);
        String[] list = dir.list();
        if (list.length > 0) {
            for (int i = 0; i < list.length; i++) {
                File file = new File(directory, list[i]);
                    file.delete();
            }
        }
    }
    public static void deleteSubdirs(String directory) {
        File parentDir = new File(directory);
        String[] list = parentDir.list();
        File subDir;
        if (list.length == 0) {
            return;
        }

        for (int i = 0; i < list.length; i++) {
            subDir = new File(directory, list[i]);
            if (subDir.isDirectory()) {
                String[] fileList = subDir.list();
                if (fileList.length > 0) {
                    for (int j = 0; j < fileList.length; j++) {
                        File file = new File(subDir.getAbsoluteFile(), fileList[j]);
                        file.delete();
                    }
                }
                subDir.delete();
            }
        }
    }

    /**
     
     * @param directory the directory/ folder name
     * @param fileExtension filename extension to be searched
     * @return a Vector containing filenames from the specified directory, sorted in ascending order.<br>
     * returns an empty Vector if no files having the extension were found.
     */
    public static ArrayList<String> getFileList(String directory, String fileExtension) {
        ExtensionFilter filter = new ExtensionFilter(fileExtension);
        return getFileList(directory, filter);
    }

    public static ArrayList<String> getFileList(String directory, FilenameFilter filter) {
        ArrayList<String> fileList = new ArrayList<String>();
        File dir = new File(directory);

        String[] list = dir.list(filter);
        for (String f : list) {
            fileList.add((directory + f));
        }

        if (!fileList.isEmpty()) {
            Collections.sort(fileList, String.CASE_INSENSITIVE_ORDER);
        }
        return fileList;
    }

    public static String toBase36(long decimalNumber) {
        return fromDecimalToOtherBase(36, decimalNumber);
    }

    public static int fromBase36(String base36Number) {
        return fromOtherBaseToDecimal(36, base36Number);
    }

    private static String fromDecimalToOtherBase(long base, long decimalNumber) {
        String tempVal = decimalNumber == 0 ? "0" : "";
        long mod = 0;

        while (decimalNumber != 0) {
            mod = decimalNumber % base;
            tempVal = baseDigits.substring((int) mod, (int) (mod + 1)) + tempVal;
            decimalNumber = decimalNumber / base;
        }

        return tempVal;
    }

    public static String createExtension(String type) {
        return "." + type;
    }

    public static String addExtension(String name, String type) {
        return name + "." + type;
    }

    private static int fromOtherBaseToDecimal(int base, String number) {
        int iterator = number.length();
        int returnValue = 0;
        int multiplier = 1;

        while (iterator > 0) {
            returnValue = returnValue + (baseDigits.indexOf(number.substring(iterator - 1, iterator)) * multiplier);
            multiplier = multiplier * base;
            --iterator;
        }
        return returnValue;
    }

    public static String removeExtension(String s) {
        String separator = System.getProperty("file.separator");
        String filename;

        // Remove the path upto the filename.
        int lastSeparatorIndex = s.lastIndexOf(separator);
        if (lastSeparatorIndex == -1) {
            filename = s;
        } else {
            filename = s.substring(lastSeparatorIndex + 1);
        }

        // Remove the extension.
        int extensionIndex = filename.lastIndexOf(".");
        if (extensionIndex == -1) {
            return filename;
        }

        return filename.substring(0, extensionIndex);
    }
    
    
    public static String getChecksum(String filePath) throws FileNotFoundException, IOException {
        FileInputStream file = new FileInputStream(filePath);
        CheckedInputStream check = new CheckedInputStream(file, new CRC32());
        BufferedInputStream in = new BufferedInputStream(check);
        while (in.read() != -1) {
            // Read file in completely
        }
        return String.valueOf(check.getChecksum().getValue());
    }

    

    public static void saveObjectDataToFile(Object obj, String name) {

       try {
            FileOutputStream fout = new FileOutputStream(System.getProperty("java.io.tmpdir") + 
                    System.getProperty("file.separator") + "screencap" + 
                    System.getProperty("file.separator") + name);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(obj);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

    }
    public static void deleteDataObject(String name) {
        File file = new File(System.getProperty("java.io.tmpdir") +
                    System.getProperty("file.separator") + "screencap" +
                    System.getProperty("file.separator") + name);
        if(file.exists())
            file.delete();
    }
    public static Object readObjectDataFromFile(String name) {

        Object obj = new Object();
        try {
            FileInputStream fin = new FileInputStream(System.getProperty("java.io.tmpdir") +
                    System.getProperty("file.separator") + "screencap" +
                    System.getProperty("file.separator") + name);
            ObjectInputStream ois = new ObjectInputStream(fin);
            obj = ois.readObject();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        return obj;
    }
    public static boolean previousVideoExists(String name){
        File videoFile = new File(System.getProperty("java.io.tmpdir") + 
                    System.getProperty("file.separator") + "screencap" + 
                    System.getProperty("file.separator") + name );
        return videoFile.exists();
    }
    public static boolean previousRecordingExists(){
        
        boolean result = false;
        File subDir;
        File parentDir = new File(System.getProperty("java.io.tmpdir") + 
                    System.getProperty("file.separator") + "screencap" + 
                    System.getProperty("file.separator"));
        String[] list = parentDir.list();
        if (list.length == 0) {
            result = false;
        }
        else{
            for (int i = 0; i < list.length; i++) {
                subDir = new File(System.getProperty("java.io.tmpdir") + 
                    System.getProperty("file.separator") + "screencap" + 
                    System.getProperty("file.separator"), list[i]);
                if (subDir.isDirectory()) {
                    String[] fileList = subDir.list();
                    if (fileList.length > 0) {
                      result = true;
                      break;
                    }
                }
            }
        }
        return result;
    }
    /**
     *
     */
    public static final class ExtensionFilter implements FilenameFilter {

        private String extension;

        public ExtensionFilter(String extension) {
            this.extension = extension;
        }

        public boolean accept(File dir, String name) {
            return (name.endsWith(extension));
        }
    }
    
    /**
     * Uses default default to prepare Contingency Plan
     * @return 
     */
    public static File prepDestinationDir(){ return prepDestinationDir(Settings.BIN_DIR); }
    /**
     * Creates a directory by making it writable and executable.
     * @param destDir Directory to be constructed
     * @return The directory which has been created
     */
    public static File prepDestinationDir(String destDir){
        
        File binDirectory = new File(destDir);
        
        if(!binDirectory.exists())     binDirectory.mkdirs();
        if(!binDirectory.canWrite())   binDirectory.setWritable(true);
        if(!binDirectory.canExecute()) binDirectory.setExecutable(true);
        
        //binDirectory.deleteOnExit();
        
        return binDirectory;
    }
    
    /**
     * A method which to move a list of files to another directory/location. 
     * Also makes each file in the given list executable.
     * @param files List of files to be relocated
     * @param destDirectory Directory which the files are to be moved to
     * @return 
     */
    public static boolean relocateFile(String[] files, String destDirectory) {
        
        File destDirFile = prepDestinationDir(destDirectory);
        
        //For each file to be relocated
        for(String filepath : files){
            //Get access to file
            File file = new File(filepath);
            File relocateFile = new File(destDirFile.getAbsoluteFile()+"/"+file.getName());
            
            
            LibraryUtil.chmod("755", file);
            LibraryUtil.chmod("755", relocateFile);
            
            System.out.print("Relocating "+file.getAbsolutePath()+" to "+relocateFile.getAbsolutePath()+"...");
            
            //Relocates file to proper destination
            file.renameTo(relocateFile);
            
            if(relocateFile.exists()){
                System.out.println("Succesfull");
            }else
                System.out.println("ERROR - Unsuccesfull");
        }
        
        return true;
        
    }
    
    /**
     * Copies a file bundled in the package to the supplied destination.
     * 
     * @param path
     *            The name of the bundled file.
     * @param dest
     *            The destination.
     * @throws RuntimeException
     *             If aun unexpected error occurs.
     */
    public boolean copyFile(String path, File dest) throws RuntimeException {
        InputStream input = null;
        OutputStream output = null;
        
        //Create parent folders if needed
        if(dest.isFile() && !dest.getParentFile().exists())
            dest.getParentFile().mkdirs();
        
        try {
            System.out.println("Looking up file path "+path);
            System.out.println("Storing file at "+dest);
            //input = getClass().getResourceAsStream(path);
            input = Launch.class.getResourceAsStream(path);
            output = new FileOutputStream(dest);
            
            if(input == null) throw new IOException("Cannot Find Resource: "+path);
            
            byte[] buffer = new byte[1024];
            int l;
            while ((l = input.read(buffer)) != -1) {
                output.write(buffer, 0, l);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace(System.out);
            return false;
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (Throwable t) {
                    ;
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable t) {
                    ;
                }
            }
        }
    }
}
