/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.sauronsoftware.jave.locators;

import com.bixly.pastevid.Session;
import com.bixly.pastevid.Settings;
import com.bixly.pastevid.common.Unimplemented;
import com.bixly.pastevid.util.FileUtil;
import com.bixly.pastevid.util.LibraryUtil;
import com.bixly.pastevid.util.LogUtil;
import com.bixly.pastevid.util.MediaUtil;
import java.io.File;

/**
 *
 * @author cevaris
 */
@Unimplemented
@Deprecated
public class FFMPEGContingency{
    
    
    /**
     * Simple version handler
     */
    private final int myEXEversion = 1;
    
    /**
     * Directory where ffmpeg libraries are installed
     */
    //private String binDir = System.getProperty("java.io.tmpdir")+"/jave-"+ myEXEversion +"/";
    private String binDir = Settings.BIN_DIR;
    
    /**
     * Actual path to ffmpeg executable
     */
    private String executablePath;

    public FFMPEGContingency(String binDirectory) {
        this.binDir = binDirectory;
    }
    
    
    public String execute() {
        //Extracts the correct library
        if(MediaUtil.osIsMac())          return extractMacLibs();
        else if(MediaUtil.osIsUnix())    return extractLinuxLibs();
        else if(MediaUtil.osIsWindows()) return extractWindowsLibs();
        return "";
    }
    
    /**
     * Hard coded, procedure for downaloding/extracting/preparing Mac OS X's 
     * FFMpeg libraries. 
     */
    private String extractMacLibs(){

        final File binDirectory = FileUtil.prepDestinationDir();
        final File osLib        = new File(binDirectory.getAbsolutePath()+"/maclibs.jar");
        final String baseURL    = LibraryUtil.getUrl();
        
        //Download file
        LibraryUtil.wget(baseURL+"media/applet/lib/maclibs.jar", binDirectory.getAbsolutePath(), "maclibs.jar");
            
//        log("Downloaded binary maclibs.jar");
        
        //Execute libraries
        LibraryUtil.execute(new String[]{
            "/usr/bin/unzip", 
            "-o",
            osLib.getAbsolutePath()
        }, binDirectory.getAbsolutePath(), true);
        
        //Extract  file
//        log("Extracted binary");
        
        //Grab and relocate
        String[] toBeRelocated = {binDirectory.getAbsolutePath()+"/com/bixly/binaries/ffmpeg"};
        FileUtil.relocateFile(toBeRelocated,binDirectory.getAbsolutePath());
//        log("Relocating files");
        
        this.executablePath = binDirectory.getAbsolutePath()+"/ffmpeg";
//        log("Done");
        return this.executablePath;
    }
    
    private String extractWindowsLibs(){ 
        
        final File binDirectory = FileUtil.prepDestinationDir();
        final File osLib        = new File(binDirectory.getAbsolutePath(), "/winlibs.jar");
        final String baseURL    = getUrl();
        
        LibraryUtil.wget(baseURL+"media/applet/lib/winlibs.jar", binDirectory.getAbsolutePath(), "winlibs.jar");
            
//        log("Downloaded binary winlibs.jar");
        
        //Extract file
        File sevenZip = new File(binDirectory.getAbsolutePath(), "\\unzip.exe");
        if(!sevenZip.exists())
            LibraryUtil.wget(baseURL+"media/applet/lib/unzip.exe", binDirectory.getAbsolutePath(), "unzip.exe");
        
        sevenZip.setExecutable(true);
        
        //Extract FFMpeg libraries
        LibraryUtil.execute(new String[]{
            sevenZip.getAbsolutePath(),
            "-o",
            osLib.getAbsolutePath()
        }, binDirectory.getAbsolutePath(),true);
//        log("Extracted binary");
        
        //Grab and relocate
        String[] toBeRelocated = {
            binDirectory.getAbsolutePath()+"/com/bixly/binaries/ffmpeg.exe",
            binDirectory.getAbsolutePath()+"/com/bixly/binaries/pthreadGC2.dll",
        };
        
        FileUtil.relocateFile(toBeRelocated,binDirectory.getAbsolutePath());
//        log("Relocating files");
        
        this.executablePath = binDirectory.getAbsolutePath()+"/ffmpeg.exe";
//        log("Done");
        return this.executablePath;
    }
    
    private String extractLinuxLibs(){ 
        
        final File binDirectory = FileUtil.prepDestinationDir();
        final File osLib        = new File(binDirectory.getAbsolutePath(), "linuxlibs.jar");
        final String baseURL    = getUrl();

        LibraryUtil.wget(baseURL+"media/applet/lib/linuxlibs.jar", binDirectory.getAbsolutePath(), "linuxlibs.jar");
        
//        log("Downloaded binary linixlibs.jar");

        //Extract  file
        LibraryUtil.execute(new String[]{
            "/usr/bin/unzip", 
            "-o",
            osLib.getAbsolutePath()
        }, binDirectory.getAbsolutePath(), true);
//        log("Extracted binary");
        
        //Grab and relocate
        String[] toBeRelocated = {binDirectory.getAbsolutePath()+"/com/bixly/binaries/ffmpeg"};
        FileUtil.relocateFile(toBeRelocated,binDirectory.getAbsolutePath());
//        log("Relocating files");
        
        this.executablePath = binDirectory.getAbsolutePath()+"/ffmpeg";
        
//        log("Done");
        return this.executablePath;
    }

    /**
     * Uses a default URL to grab FFMpeg libraries. 
     * @return URL to be used
     */
    private String getUrl() {
        String baseURL = "";
        if(Session.getInstance() == null || Session.getInstance().user.getBaseURL().length() == 0){
            //Default URL
            baseURL = "http://screenbird.com/";
        } else 
            //Assigns runtime codebase
            baseURL = Session.getInstance().user.getBaseURL();
        return baseURL;
    }

    /**
     * Returns the ffmpeg executable path to be used
     * @return 
     */
    public String getFFmpegPath() {
        return this.executablePath;
    }

    public void log(Object message) {
        LogUtil.log(FFMPEGContingency.class, message);
    }
    
    
    

    
    
}
