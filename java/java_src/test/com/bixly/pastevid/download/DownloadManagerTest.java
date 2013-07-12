/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.download;

import com.bixly.pastevid.Session;
import com.bixly.pastevid.util.TimeUtil;
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
public class DownloadManagerTest {
    
    public DownloadManagerTest() {
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
     * Test of getInstance method, of class DownloadManager.
     */
    @Test
    public void testGetInstance() {
        System.out.println("getInstance");
        DownloadManager result = DownloadManager.getInstance();
        assertNotNull(result);
    }

    /**
     * Test of registerDownload method, of class DownloadManager.
     */
    @Test
    public void testRegisterDownload() {
        System.out.println("registerDownload");
        String jobId = "fakeJob";
        DownloadThread download = new DownloadHandbrake();
        DownloadManager instance = DownloadManager.getInstance();
        instance.registerDownload(jobId, download);
        
        assertNotNull(instance.pingDownloadStatus(jobId));
    }
    
    /**
     * Test of getDownload method, of class DownloadManager.
     */
    @Test
    public void testGetDownload() {
        System.out.println("getDownload");
        String jobId = "fakeJob";
        DownloadThread download = new DownloadHandbrake();
        DownloadManager instance = DownloadManager.getInstance();
        instance.registerDownload(jobId, download);
        
        assertNotNull(instance.getDownload(jobId));
    }
    
    /**
     * Test of close method, of class DownloadManager.
     */
    @Test
    public void testClose() {
        System.out.println("close");
        DownloadManager instance = DownloadManager.getInstance();
        instance.start();

        TimeUtil.skipToMyLou(1);
        instance.close();
        
        assertFalse(instance.isRunning());
    }

}
