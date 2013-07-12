package it.sauronsoftware.jave.locators;

import java.io.File;

public class WindowsFFMPEGLocator extends DefaultFFMPEGLocator {

    public WindowsFFMPEGLocator() {

        // Temp dir?
        File temp = new File(System.getProperty("java.io.tmpdir"), "jave-"
                + myEXEversion);

        if (!temp.exists()) {
            temp.mkdirs();
            temp.deleteOnExit();
        }
        // ffmpeg executable export on disk.
        String ffmpeg = "ffmpeg.exe";
        File exe = new File(temp, ffmpeg);
        if (exe.exists()) {
            exe.delete();
        }
        copyFile(ffmpeg, exe);

        // pthreadGC2.dll

        File pthread = new File(temp, "pthreadGC2.dll");
        if (pthread.exists()) {
            pthread.delete();
        }
        copyFile("pthreadGC2.dll", pthread);

//        // libmp3lame-0.dll
//        File lame = new File(temp, "libmp3lame-0.dll");
//        if (!lame.exists()) {
//            copyFile("libmp3lame-0.dll", lame);
//        }

        // Ok.
        this.path = exe.getAbsolutePath();
    }
}