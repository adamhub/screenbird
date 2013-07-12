/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.download;

import com.bixly.pastevid.Session;
import com.bixly.pastevid.Settings;
import com.bixly.pastevid.util.TimeUtil;
import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author cevaris
 */
public class DownloadHandbrakeTest {
    
    public DownloadHandbrakeTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        Session.getInstance().user.setBaseURL("http://staging.screenbird.com/");
        Session.getInstance().user.setAnonToken("ZXCVBNMASDFGHJKLQWERTYUIOP");
        Session.getInstance().user.setCsrfToken("QWERTYUIOPASDFGHJKLZXCVBNM");
        Session.getInstance().user.setUserId("10");
    }
    
    @After
    public void tearDown() {
        
    }

    /**
     * Test of HandbrakeDownload, of class DownloadHandbrake.
     */
    @Test
    public void testHandbrakeDownload() {
        
        //Delte file if currently exists
        File file = new File(Settings.getHandbrakeExecutable());
        if(file.exists()) file.delete();
        
        file = new File(Settings.BIN_DIR, "handbrake.zip");
        if(file.exists()) file.delete();
        
        
        DownloadHandbrake instance = new DownloadHandbrake();
        DownloadManager downloadManager = DownloadManager.getInstance();
        
        //Regsister file to be downloaded
        downloadManager.registerDownload(
            Settings.getHandbrakeExecutable(),
            instance
        );
        
        downloadManager.start();
        
        //While file is not finished downloading
        while(!instance.checkStatus(DownloadStatus.FINISHED)){
            System.out.println("Downloading instance");
            TimeUtil.skipToMyLou(1);
        }
        //Check if file exists
        assertTrue(instance.getFile().exists());
        
    }
}
