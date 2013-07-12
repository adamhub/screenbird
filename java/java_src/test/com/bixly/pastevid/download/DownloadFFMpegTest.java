/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.download;

import com.bixly.pastevid.Session;
import com.bixly.pastevid.TestSettings;
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
public class DownloadFFMpegTest {
    
    public DownloadFFMpegTest() {
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
     * Test of FFmpegDownload, of class DownloadFFMpeg.
     */
    @Test
    public void testFFmpegDownload() {
        
        //Delte file if currently exists
        File file = new File(TestSettings.getFFMpegExecutable());
        if(file.exists()) file.delete();
        
        file = new File(TestSettings.BIN_DIR, "ffmpeg.zip");
        if(file.exists()) file.delete();
        
        System.out.println("BASE URL: " + Session.getInstance().user.getBaseURL());
        DownloadFFMpeg instance = new DownloadFFMpeg();
        DownloadManager downloadManager = DownloadManager.getInstance();
        
        //Regsister file to be downloaded
        downloadManager.registerDownload(
            TestSettings.getFFMpegExecutable(),
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
