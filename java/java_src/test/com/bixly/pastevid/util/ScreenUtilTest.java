package com.bixly.pastevid.util;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Bixly
 */
public class ScreenUtilTest {

    public ScreenUtilTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
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
    
    private static void log(Object string) {
        LogUtil.log(ScreenUtilTest.class,string);
    }

    /**
     * Test of getScreenDimension method, of class ScreenUtil.
     */
    @Test
    public void testGetScreenDimension() {
        log("getScreenDimension");
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsConfiguration gd = ge.getScreenDevices()[0].getDefaultConfiguration();
        GraphicsDevice device = gd.getDevice();
        Dimension result = ScreenUtil.getScreenDimension(device);
        assertNotNull(result);
        assertNotSame(0, result.height);
        assertNotSame(0, result.width);
    }

    /**
     * Test of isMultipleScreen method, of class ScreenUtil.
     */
    @Test
    public void testIsDualScreen() {
        log("isDualScreen");
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();
        boolean expResult = screens.length > 1;
        boolean result = ScreenUtil.isMultipleScreen();
        assertEquals(expResult, result);
    }
}
