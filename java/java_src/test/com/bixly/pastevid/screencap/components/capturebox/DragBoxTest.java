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
import com.bixly.pastevid.models.ScreenSize;
import com.bixly.pastevid.util.TimeUtil;
import java.awt.Dimension;
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
public class DragBoxTest {
    
    private static DragBox instance;
    
    public DragBoxTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        TestMeasurable measureable = new TestMeasurable();
        CaptureBox capturebox = new CaptureBox(new Recorder(measureable));
        instance = new DragBox(capturebox);
        instance.setCaptureBox(capturebox);
        
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
     * Test of getCurrentScreenSize method, of class DragBox.
     */
    @Test
    public void testGetCurrentScreenSize() {
        System.out.println("getCurrentScreenSize");
        ScreenSize result = instance.getCurrentScreenSize();
        assertNull(result);
    }

    /**
     * Test of setCurrentScreenSize method, of class DragBox.
     */
    @Test
    public void testSetCurrentScreenSize() {
        System.out.println("setCurrentScreenSize");
        ScreenSize oldScreenSize = instance.getCurrentScreenSize();
        ScreenSize currentScreenSize = new ScreenSize("TestScreenSize", 800, 600, ScreenSize.AR_STANDARD);
        instance.setCurrentScreenSize(currentScreenSize);
        assertEquals(currentScreenSize, instance.getCurrentScreenSize());
        instance.setCurrentScreenSize(oldScreenSize);
    }

    /**
     * Test of setAspectRatio method, of class DragBox.
     */
    @Test
    public void testSetAspectRatio() {
        System.out.println("setAspectRatio");
        CaptureBoxState captureBoxState = CaptureBoxState.STANDARD_SCREEN;
        instance.setAspectRatio(captureBoxState);
        
        TimeUtil.skipToMyLou(2);
        
        captureBoxState = CaptureBoxState.WIDESCREEN;
        instance.setAspectRatio(captureBoxState);
        
        TimeUtil.skipToMyLou(2);
        
    }

    /**
     * Test of updateSizeLabel method, of class DragBox.
     */
    @Test
    public void testUpdateSizeLabel() {
        System.out.println("updateSizeLabel");
        Dimension size = new Dimension(600,600);
        instance.updateSizeLabel(size);
        
        TimeUtil.skipToMyLou(2);
        
        size = new Dimension(800,600);
        instance.updateSizeLabel(size);
        
        TimeUtil.skipToMyLou(2);
        
        size = new Dimension(500,400);
        instance.updateSizeLabel(size);
        
        
    }
}
