package it.sauronsoftware.jave.locators;

import com.bixly.pastevid.Settings;
import java.io.File;
import java.io.IOException;

public class LinuxFFMPEGLocator extends DefaultFFMPEGLocator {

    public LinuxFFMPEGLocator() {

        // Temp dir?
        //File temp = new File(System.getProperty("java.io.tmpdir"), "/jave-"+ myEXEversion);
        File temp = new File(Settings.BIN_DIR);
        
        if (!temp.exists()) {
            temp.mkdirs();
            temp.deleteOnExit();
        }
        // ffmpeg executable export on disk.
//        String ffmpeg = "ffmpeg";
        File exe = new File(Settings.getFFMpegExecutable());
        
//        if (exe.exists()) {
//            log("Found old binary");
//            exe.delete();
//            log("Deleted Old binary: "+exe.exists());
//        }
//        
//        log("Loading new ffmpeg");
//        copyFile("ffmpeg", exe);
//        log("Loaded new ffmpeg");
//        Runtime runtime = Runtime.getRuntime();
//        try {
//            runtime.exec(new String[]{"/bin/chmod", "755", exe.getAbsolutePath()});
//        } catch (IOException e) {
//           log(e);
//        }
        // Ok.
        this.path = exe.getAbsolutePath();
    }
}