/*
 * Launch.java
 *
 * Version 1.0
 * 
 * 20 May 2013
 */
package com.bixly.pastevid.driver;

import com.bixly.pastevid.Settings;
import com.bixly.pastevid.models.User;
import com.bixly.pastevid.util.FileUtil;
import com.bixly.pastevid.util.LibraryUtil;
import com.bixly.pastevid.util.MediaUtil;
import com.bixly.pastevid.util.PropertiesUtil;
import java.io.File;
import java.util.Properties;
import javax.swing.JApplet;
import netscape.javascript.JSObject;

/**
 *
 * @author cevaris
 */
public class Launch extends JApplet {
    public static User user;
    
    @Override
    public void init() {
        loadParameters();
        printSystemProperties();
        launchScreenRecorder();
    }
    
    final private String[] logJavaProperties = {
        "sun.boot.library.path",
        "java.vm.version",
        "mrj.version",
        "java.runtime.version",
        "java.version",
        "java.specification.version",
        "java.vm.name",
        "os.arch",
        "sun.arch.data.model",
        "java.home",
        "java.vm.info",
    };
    
    private void printSystemProperties() {
        Properties p = System.getProperties();
        for (String key : this.logJavaProperties) {
            System.out.println(key + ": " + p.getProperty(key));
        }
    }

    /**
     * Reads in the parameters passed in by the javascript launcher. 
     * Each token is pre-defined.
     */
    private void loadParameters() {
        // Initialize user model
        Launch.user = new User();
        // Load applet data into model
        if (this.getParameter(User.CSRF_TOKEN) != null) {
            Launch.user.setCsrfToken(this.getParameter(User.CSRF_TOKEN));
        }
        if (this.getParameter(User.USER_ID) != null) {
            Launch.user.setUserId(this.getParameter(User.USER_ID));
        }
        if (this.getParameter(User.ANON_TOKEN) != null) {
            Launch.user.setAnonToken(this.getParameter(User.ANON_TOKEN));
        }
        if (this.getParameter(User.BASE_URL) != null) {
            Launch.user.setBaseURL(this.getParameter(User.BASE_URL));
        }
        if (this.getParameter(User.SLUG) != null) {
            Launch.user.setSlug(this.getParameter(User.SLUG));
        }
        System.out.println(Launch.user);
    }

    private void launchScreenRecorder() {
        final File binDirectory = LibraryUtil.prepDestinationDir(Settings.BIN_DIR);
        
        System.out.println("Created directory: " + binDirectory.getAbsolutePath());
        
        final File executableFile = new File(binDirectory.getAbsolutePath(), "ScreenRecorder.jar");
        final String baseURL      = LibraryUtil.getUrl();
        
        //Download all files
        LibraryUtil.wget(baseURL+"media/applet/ScreenRecorder.jar",             binDirectory.getAbsolutePath(),         "/ScreenRecorder.jar");
        LibraryUtil.wget(baseURL+"media/applet/lib/commons-codec-1.4.jar",      binDirectory.getAbsolutePath()+"/lib/", "commons-codec-1.4.jar");
        LibraryUtil.wget(baseURL+"media/applet/lib/commons-logging-1.1.1.jar",  binDirectory.getAbsolutePath()+"/lib/", "commons-logging-1.1.1.jar");
        LibraryUtil.wget(baseURL+"media/applet/lib/httpclient-4.1.1.jar",       binDirectory.getAbsolutePath()+"/lib/", "httpclient-4.1.1.jar");
        LibraryUtil.wget(baseURL+"media/applet/lib/httpclient-cache-4.1.1.jar", binDirectory.getAbsolutePath()+"/lib/", "httpclient-cache-4.1.1.jar");
        LibraryUtil.wget(baseURL+"media/applet/lib/httpcore-4.1.jar",           binDirectory.getAbsolutePath()+"/lib/", "httpcore-4.1.jar");
        LibraryUtil.wget(baseURL+"media/applet/lib/httpmime-4.1.1.jar",         binDirectory.getAbsolutePath()+"/lib/", "httpmime-4.1.1.jar");

        System.out.println("Done Downloading");
        System.out.println("Making binary executable");
        LibraryUtil.chmod("755", executableFile);
        
        // Save properties of user data to be loaded by screen recorder app
        File propertyFile = new File(Settings.SCREENBIRD_CONFIG);
        PropertiesUtil.saveProperty(propertyFile.getAbsolutePath(), User.ANON_TOKEN, user.getAnonToken());
        PropertiesUtil.saveProperty(propertyFile.getAbsolutePath(), User.CSRF_TOKEN, user.getCsrfToken());
        PropertiesUtil.saveProperty(propertyFile.getAbsolutePath(), User.USER_ID,  user.getUserId());
        PropertiesUtil.saveProperty(propertyFile.getAbsolutePath(), User.BASE_URL, user.getBaseURL());
        PropertiesUtil.saveProperty(propertyFile.getAbsolutePath(), User.SLUG, user.getSlug());
        
        System.out.println("Executing ScreenRecorder App");
        
        try {
//            if (MediaUtil.osIsMac()) { //Mac OS X
//                String javaPath = System.getProperty("java.home")+"/bin/java";
//                
//                // Mac's custom script for launching Screen Recorder with proper settings
//                File launchMac = new File(binDirectory+"/launch-mac");
//                FileUtil.getInstance().copyFile("/com/bixly/pastevid/resources/launch-mac.sh", launchMac);
//                System.out.println("Starting to launch Screen Capture");
//                LibraryUtil.executeScript(
//                    new String[]{
//                        launchMac.getAbsolutePath(),     //Custom mac script
//                        javaPath,                        //Java bin path
//                        executableFile.getAbsolutePath() //Screen Recorder executable path
//                });
//                System.out.println("Launched Screen Capture");
//                
//            } else if (MediaUtil.osIsWindows()) { // Windows
            
            if (MediaUtil.osIsWindows()) { // Windows
                String javaPath = "\""+System.getProperty("java.home")+"\\bin\\java.exe\"";
                
                // Normal Launching of screen recorder app
                LibraryUtil.execute(new String[]{
                    javaPath,
                    "-jar",
                    executableFile.getAbsolutePath()
                },Settings.BIN_DIR, false);

            } else { // Linux machines
                String javaPath = System.getProperty("java.home")+"/bin/java";
                
                // Normal Launching of screen recorder app
                LibraryUtil.execute(new String[]{
                    javaPath,
                    "-jar",
                    executableFile.getAbsolutePath()
                },Settings.BIN_DIR, false);

            }
        } catch (Exception e){
            e.printStackTrace(System.err);
        }
        
        System.out.println("Executed ScreenRecorder App");
        
        // Redirect to 
        redirectWebPage();
        
        // Kill applet thread
        super.destroy();
        
        System.exit(0);
    }
    
    
    /**
     * Using the Javascript located on the web page which opened this applet,
     * we close the web page; thus closing the applet with it. 
     */
    public boolean closeApplet() {
        
        try {
            JSObject win = JSObject.getWindow(this);
            System.out.println("Closing window");
            win.call("closeRecorderForm", new Object[]{});
            System.out.println("Closed window");
        } catch (Exception e) {
            System.err.println(e);
            System.err.println("Window already closed");
        }
        
        return false;
    }

    /**
     * Redirects to screenbird homepage.
     */
    private void redirectWebPage() {
        try {
            JSObject win = JSObject.getWindow(this);
            System.out.println("Redirecting window");
            win.call("redirectHome", new Object[]{});
            System.out.println("Redirected window");
        } catch (Exception e) {
            System.err.println(e);
            System.err.println("Window already closed");
        }
    }
}
