/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.screencap.components.settings;

import com.bixly.pastevid.recorders.IMeasurable;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import com.bixly.pastevid.recorders.Recorder;
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
public class SettingsPanelTest {
    
    private static SettingsPanel instance;
    private static Recorder recorder;
    
    public SettingsPanelTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        instance = new SettingsPanel();
        
        recorder = new Recorder(new TestMeasurable());
        instance.setRecorder(recorder);
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
    
    private static class TestMeasurable implements IMeasurable {

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
     * Test of setRecorder method, of class SettingsPanel.
     */
    @Test
    public void testSetRecorder() {
        System.out.println("setRecorder");
        instance.setRecorder(recorder);
        
    }

    /**
     * Test of getVideoQualitySlider method, of class SettingsPanel.
     */
    @Test
    public void testGetVideoQualitySlider() {
        System.out.println("getVideoQualitySlider");
        assertNotNull(instance.getVideoQualitySlider());
    }

    /**
     * Test of loadPastevidSettings method, of class SettingsPanel.
     */
    @Test
    public void testLoadPastevidSettings() {
        System.out.println("loadPastevidSettings");
        
        assertNotNull(instance.getVideoQualitySlider().getValue());
        
        //Range of video test i [0,2]
        switch(instance.getVideoQualitySlider().getValue()){
            case 0:
            case 1:
            case 2:
            default:
                assertFalse("Video quality is out of range", false);
        }
        
    }

    /**
     * Test of setEnableControls method, of class SettingsPanel.
     */
    @Test
    public void testSetEnableControls() {
        System.out.println("setEnableControls");
        boolean bln = false;
        instance.setEnableControls(bln);
        
        TimeUtil.skipToMyLou(2);
        
        bln = true;
        instance.setEnableControls(bln);
        
        
    }

}
