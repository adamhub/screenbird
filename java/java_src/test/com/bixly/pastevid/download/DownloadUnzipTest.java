/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.download;

import com.bixly.pastevid.Session;
import com.bixly.pastevid.Settings;
import com.bixly.pastevid.util.MediaUtil;
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
public class DownloadUnzipTest {
    
    public DownloadUnzipTest() {
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
     * Test of UnzipDownload, of class DownloadUnzip.
     */
    @Test
    public void testUnzipDownload() {
        
        if(MediaUtil.osIsWindows()) {
            //Delte file if currently exists
            File file = new File(Settings.getUnzipExecutable());
            if(file.exists()) file.delete();

            file = new File(Settings.BIN_DIR, "unzip.zip");
            if(file.exists()) file.delete();


            DownloadUnzip instance = new DownloadUnzip();
            DownloadManager downloadManager = DownloadManager.getInstance();

            //Regsister file to be downloaded
            downloadManager.registerDownload(
                Settings.getUnzipExecutable(),
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
        } else {
            System.out.println("testUnzipDownload can only be completed on Windows since Mac and Linux already have the Unzip application");
        }
        
    }
}
