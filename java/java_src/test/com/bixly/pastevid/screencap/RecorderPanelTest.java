/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.screencap;

import com.bixly.pastevid.Session;
import com.bixly.pastevid.util.LogUtil;
import java.awt.GraphicsConfiguration;
import com.bixly.pastevid.recorders.Recorder;
import com.bixly.pastevid.recorders.RecorderStatus;
import com.bixly.pastevid.util.TimeUtil;
import java.awt.Color;
import java.awt.GraphicsDevice;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import com.bixly.pastevid.util.LogUtil;
import static org.junit.Assert.*;

/**
 *
 * @author cevaris
 */
public class RecorderPanelTest {
    
    static ScreenRecorder jfRecorder;
    static RecorderPanel  instance;
    static boolean recoveryMode = false;
    
    public RecorderPanelTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        
        generateTempUser();
        jfRecorder = new ScreenRecorder(recoveryMode);
        instance = jfRecorder.getPanel();
    }
    
     /**
     * Generates a temporary User model instance for JUnit testing
     * Only should be used for JUnit Tests!!!
     * @return 
     */
    private static void generateTempUser(){
        Session.getInstance().user.setBaseURL("http://staging.screenbird.com/");
        Session.getInstance().user.setAnonToken("ZXCVBNMASDFGHJKLQWERTYUIOP");
        Session.getInstance().user.setCsrfToken("QWERTYUIOPASDFGHJKLZXCVBNM");
        Session.getInstance().user.setUserId("10");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        instance.getRecorder().deleteFiles();
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    private static void log(Object string) {
        LogUtil.log(RecorderPanelTest.class,string);
    }
    
    
    /**
     * Test of getRecorder method, of class RecorderPanel.
     */
    @Test
    public void testGetRecorder() {
        log("getRecorder");
        assertNotNull(instance.getRecorder());
    }
    
    /**
     * Test of getValue method, of class RecorderPanel.
     */
    @Test
    public void testGetValue() {
        log("getValue");
        long expResult = 0L;
        long result = instance.getValue();
        assertEquals(expResult, result);
    }

    /**
     * Test of checkNearCut method, of class RecorderPanel.
     */
    @Test
    public void testCheckNearCut() {
        log("checkNearCut");
        assertTrue(instance.checkTimeLimit(0, 29, 0, 29));
        assertFalse(instance.checkTimeLimit(0, 28, 0, 29));
    }

    /**
     * Test of checkCut method, of class RecorderPanel.
     */
    @Test
    public void testCheckCut() {
        log("checkCut");
        assertTrue(instance.checkTimeLimit(0, 30, 0, 30));
        assertFalse(instance.checkTimeLimit(0, 29, 0, 30));
    }

    /**
     * Test of setRecorder method, of class RecorderPanel.
     */
    @Test
    public void testSetRecorder() {
        System.out.println("setRecorder");
        Recorder recorder = instance.getRecorder();
        instance.setRecorder(recorder);
        assertEquals(recorder, instance.getRecorder());
    }

    /**
     * Test of getScreen method, of class RecorderPanel.
     */
    @Test
        public void testGetScreen() {
        System.out.println("getScreen");
        GraphicsConfiguration gd = instance.getGraphicsConfiguration();
        GraphicsDevice expResult = gd.getDevice();
        GraphicsDevice result = instance.getScreen();
        assertNotNull(result);
        assertEquals(expResult, result);
    }

    /**
     * Test of initRecorder method, of class RecorderPanel.
     */
    @Test
    public void testInitRecorder() {
        System.out.println("initRecorder");
        //If not null, then initRecorder worked
        assertNotNull(instance);
    }


    /**
     * Test of isRecorderConfigSate method, of class RecorderPanel.
     */
    @Test
    public void testIsRecorderConfigSate() {
        System.out.println("isRecorderConfigSate");

        boolean expResult = true;
        boolean result = instance.isRecorderConfigSate();
        assertEquals(expResult, result);
        
        instance.getRecorder().setStatus(RecorderStatus.STOPPED);
        result = instance.isRecorderConfigSate();
        assertEquals(expResult, result);
        
    }

    /**
     * Test of isEncodingVideo method, of class RecorderPanel.
     */
    @Test
    public void testIsEncodingVideo() {
        System.out.println("isEncodingVideo");
        boolean expResult = false;
        boolean result = instance.isEncodingVideo();
        assertEquals(expResult, result);
    }

    /**
     * Test of setClockOffset method, of class RecorderPanel.
     */
    @Ignore
    public void testSetClockOffset() {
        System.out.println("setClockOffset");
        Long offset = null;
        Long oldValue = instance.getValue();
        instance.setClockOffset(-10000L);
        assertTrue(Math.abs((instance.getValue()+10000L) - oldValue) == 0);
    }

    /**
     * Test of showRecorderMessage method, of class RecorderPanel.
     */
    @Test
    public void testShowRecorderMessage() {
        System.out.println("showRecorderMessage");
        String message = "Show Message Test";
        Color type = Color.WHITE;
        instance.showRecorderMessage(message, type);
        TimeUtil.skipToMyLou(2);
        instance.showRecorderForm();
    }

    /**
     * Test of showRecordingState method, of class RecorderPanel.
     */
    @Test
    public void testShowRecordingState() {
        System.out.println("showRecordingState");
        instance.showRecordingState();
        TimeUtil.skipToMyLou(2);
        instance.showRecorderForm();
    }

    /**
     * Test of showRecorderForm method, of class RecorderPanel.
     */
    @Test
    public void testShowRecorderForm() {
        System.out.println("showRecorderForm");
        instance.showRecorderForm();
        instance.showRecorderMessage("This is a test Message", Color.WHITE);
        TimeUtil.skipToMyLou(2);
        instance.showRecorderForm();
    }

    /**
     * Test of showSettingsForm method, of class RecorderPanel.
     */
    @Test
    public void testShowSettingsForm() {
        System.out.println("showSettingsForm");
        instance.showSettingsForm();
        TimeUtil.skipToMyLou(2);
        instance.showRecorderForm();
    }

    /**
     * Test of showPreviewPlayer method, of class RecorderPanel.
     */
    @Test
    public void testShowPreviewPlayer() {
        System.out.println("showPreviewPlayer");
        instance.showPreviewPlayer();
        TimeUtil.skipToMyLou(2);
        instance.showRecorderForm();
    }

    /**
     * Test of showUploadMessage method, of class RecorderPanel.
     */
    @Test
    public void testShowUploadMessage() {
        System.out.println("showUploadMessage");
        String message = "Test Message";
        Color type = Color.WHITE;
        instance.showUploadMessage(message, type);
        TimeUtil.skipToMyLou(2);
        instance.showRecorderForm();        
    }

}
