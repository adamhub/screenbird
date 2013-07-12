/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.screencap.components.settings;

import com.bixly.pastevid.Session;
import com.bixly.pastevid.recorders.IMeasurable;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import com.bixly.pastevid.recorders.Recorder;
import com.bixly.pastevid.screencap.RecorderPanel;
import com.bixly.pastevid.screencap.ScreenRecorder;
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
public class SettingsFormTest {
    
    private static SettingsForm instance;
    private static ScreenRecorder jfRecorderPanel;
    private static RecorderPanel jpRecorderPanel;
    private static boolean recoveryMode = false;
    
    public SettingsFormTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        generateTempUser();
        
        jfRecorderPanel = new ScreenRecorder(recoveryMode);
        jpRecorderPanel = jfRecorderPanel.getPanel();
        
        instance = new SettingsForm();
        instance.setRecorderPanel(jpRecorderPanel);
        instance.setVisible(true);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        jpRecorderPanel.getRecorder().deleteFiles();
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
    
    class TestMeasurable implements IMeasurable {

        private long initiTime;
        public TestMeasurable()
        {
            initiTime = System.currentTimeMillis();
        }
        public long getValue() {
            return System.currentTimeMillis() - initiTime;
        }

        public GraphicsDevice getScreen() {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsConfiguration gd = ge.getScreenDevices()[0].getDefaultConfiguration();
            GraphicsDevice device = gd.getDevice();
            return device;
        }
        
        public void relocate(int x, int y){
            // no-op.
        }
        
    }

    /**
     * Test of setRecorderPanel method, of class SettingsForm.
     */
    @Test
    public void testSetRecorderPanel() {
        System.out.println("setRecorderPanel");
        RecorderPanel panel = jpRecorderPanel;
        instance.setRecorderPanel(panel);
    }

    /**
     * Test of setEnableControl method, of class SettingsForm.
     */
    @Test
    public void testSetEnableControl() {
        System.out.println("setEnableControl");
        boolean bln = false;
        instance.setEnableControl(bln);
        TimeUtil.skipToMyLou(2);
        
        bln = true;
        instance.setEnableControl(bln);

    }

    /**
     * Test of setRecorder method, of class SettingsForm.
     */
    @Test
    public void testSetRecorder() {
        System.out.println("setRecorder");
        Recorder recorder = jpRecorderPanel.getRecorder();
        instance.setRecorder(recorder);
    }

    /**
     * Test of getPanel method, of class SettingsForm.
     */
    @Test
    public void testGetPanel() {
        System.out.println("getPanel");
        assertNotNull(instance.getPanel());
    }

    /**
     * Test of loadSmartPosition method, of class SettingsForm.
     */
    @Test
    public void testLoadSmartPosition() {
        System.out.println("loadSmartPosition");
        
        instance.setLocation(300, 300);
        TimeUtil.skipToMyLou(2);
        instance.loadSmartPosition();
        TimeUtil.skipToMyLou(2);
        
    }

    /**
     * Test of showSettingsForm method, of class SettingsForm.
     */
    @Test
    public void testShowSettingsForm() {
        System.out.println("showSettingsForm");
        
        instance.hideSettingsForm();
        TimeUtil.skipToMyLou(2);
        instance.showSettingsForm();
        TimeUtil.skipToMyLou(2);
    }

    /**
     * Test of hideSettingsForm method, of class SettingsForm.
     */
    @Test
    public void testHideSettingsForm() {
        System.out.println("hideSettingsForm");
        instance.hideSettingsForm();
        TimeUtil.skipToMyLou(2);
        instance.showSettingsForm();
        TimeUtil.skipToMyLou(2);
    }

}
