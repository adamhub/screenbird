/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.screencap;

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
public class ScreenRecorderTest {
    
    static ScreenRecorder instance;
    static boolean recoveryMode = false;
    
    public ScreenRecorderTest() {
        
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        generateTempUser();
        instance = new ScreenRecorder(recoveryMode);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
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

    /**
     * Test of initRecorder method, of class ScreenRecorder.
     */
    @Test
    public void testInitRecorder() {
        System.out.println("initRecorder");

        assertNotNull(instance);
        assertNotNull(instance.getPanel());
        
        
    }

    /**
     * Test of bringToFocus method, of class ScreenRecorder.
     */
    @Test
    public void testBringToFocus() {
        System.out.println("bringToFocus");
        
        instance.controlSetVisible(false);
        assertFalse(instance.getFrame().isVisible());
        
        TimeUtil.skipToMyLou(2);
        instance.bringToFocus();
        TimeUtil.skipToMyLou(2);
        assertTrue(instance.getFrame().isVisible());
        
    }

    /**
     * Test of controlSetLocation method, of class ScreenRecorder.
     */
    @Test
    public void testControlSetLocation() {
        System.out.println("controlSetLocation");
        int x = 0;
        int y = 0;
        
        instance.controlSetLocation(x, y);
        TimeUtil.skipToMyLou(1);
        instance.controlSetLocation(100, 100);
        TimeUtil.skipToMyLou(1);
        instance.controlSetLocation(400, 400);
        TimeUtil.skipToMyLou(1);
        instance.controlSetLocation(30, 30);
        
    }

    /**
     * Test of controlSetVisible method, of class ScreenRecorder.
     */
    @Test
    public void testControlSetVisible() {
        System.out.println("controlSetVisible");
        
        instance.controlSetVisible(false);
        assertFalse(instance.getFrame().isVisible());
        
        TimeUtil.skipToMyLou(2);
        instance.controlSetVisible(true);
        assertTrue(instance.getFrame().isVisible());
        
        
    }

}
