/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.screencap.components.capturebox;

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
public class CaptureBoxTest {
    
    private static CaptureBox instance;
    
    public CaptureBoxTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        instance = new CaptureBox(new Recorder(new TestMeasurable()));
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
            // no-op
        }
        
    }

    /**
     * Test of setState method, of class CaptureBox.
     */
    @Test
    public void testSetState() {
        System.out.println("setState");
        CaptureBoxState captureBoxState;
        
        captureBoxState = CaptureBoxState.CUSTOM_SCREEN;
        instance.setState(captureBoxState);
        assertEquals(captureBoxState, instance.getState());
        
        captureBoxState = CaptureBoxState.FULLSCREEN;
        instance.setState(captureBoxState);
        assertEquals(captureBoxState, instance.getState());
        
        captureBoxState = CaptureBoxState.STANDARD_SCREEN;
        instance.setState(captureBoxState);
        assertEquals(captureBoxState, instance.getState());
        
        captureBoxState = CaptureBoxState.WIDESCREEN;
        instance.setState(captureBoxState);
        assertEquals(captureBoxState, instance.getState());
        
    }

    /**
     * Test of isVisible method, of class CaptureBox.
     */
    @Test
    public void testIsVisible() {
        System.out.println("isVisible");
        
        
        instance.setCaptureboxVisible(true,false,true);
        TimeUtil.skipToMyLou(2);
        
        boolean expResult = true;
        boolean result = instance.isVisible();
        assertEquals(expResult, result);
        
        
        instance.setCaptureboxVisible(false, false, false);
        TimeUtil.skipToMyLou(2);
        
        expResult = false;
        result = instance.isVisible();
        assertEquals(expResult, result);
        
    }


    /**
     * Test of setCaptureboxVisible method, of class CaptureBox.
     */
    @Test
    public void testSetCaptureboxVisible() {
        System.out.println("setCaptureboxVisible");
        
        instance.setCaptureboxVisible(true,false,true);
        TimeUtil.skipToMyLou(2);
        
        boolean expResult = true;
        boolean result = instance.isVisible();
        assertEquals(expResult, result);
        
        instance.setCaptureboxVisible(true, false, false);
        TimeUtil.skipToMyLou(2);
        
        expResult = true;
        result = instance.isVisible();
        assertEquals(expResult, result);
        
        instance.setCaptureboxVisible(false, false, false);
        TimeUtil.skipToMyLou(2);
        
        
        expResult = false;
        result = instance.isVisible();
        assertEquals(expResult, result);
        
        instance.setCaptureboxVisible(true,false,true);
        TimeUtil.skipToMyLou(2);
    }

    /**
     * Test of getCaptureRectangle method, of class CaptureBox.
     */
    @Test
    public void testGetCaptureRectangle() {
        System.out.println("getCaptureRectangle");
        assertNotNull(instance.getCaptureRectangle());
    }

    /**
     * Test of getState method, of class CaptureBox.
     */
    @Test
    public void testGetState() {
        System.out.println("getState");
        CaptureBoxState expResult = CaptureBoxState.FULLSCREEN;
        instance.setState(CaptureBoxState.FULLSCREEN);
        
        CaptureBoxState result = instance.getState();
        assertEquals(expResult, result);
    }

}
