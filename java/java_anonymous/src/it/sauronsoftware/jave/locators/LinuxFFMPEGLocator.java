package it.sauronsoftware.jave.locators;

import java.io.File;
import java.io.IOException;

public class LinuxFFMPEGLocator extends DefaultFFMPEGLocator {

    public LinuxFFMPEGLocator() {

        // Temp dir?
        File temp = new File(System.getProperty("java.io.tmpdir"), "jave-"
                + myEXEversion);
        if (!temp.exists()) {
            temp.mkdirs();
            temp.deleteOnExit();
        }
        // ffmpeg executable export on disk.
        String ffmpeg = "ffmpeg";
        File exe = new File(temp, ffmpeg);
        if (exe.exists()) {
            exe.delete();
        }
        copyFile("ffmpeg", exe);
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(new String[]{"/bin/chmod", "755",
                        exe.getAbsolutePath()});
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Ok.
        this.path = exe.getAbsolutePath();
    }
}