package com.bixly.pastevid.models;

import com.bixly.pastevid.util.LogUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Bixly
 */
public class ScreenSizeTest {
    
    public ScreenSizeTest() {
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
        LogUtil.log(ScreenSizeTest.class,string);
    }

    /**
     * Test of setHeight/getHeight method, of class ScreenSize.
     */
    @Test
    public void testHeight() {
        log("ScreenSize.height");
        ScreenSize instance = new ScreenSize("Name", 0, 0, 0);
        int expResult = 1024;
        instance.setHeight(expResult);
        int result = instance.getHeight();
        assertEquals(expResult, result);
    }

    /**
     * Test of setName/getName method, of class ScreenSize.
     */
    @Test
    public void testName() {
        log("ScreenSize.name");
        ScreenSize instance = new ScreenSize("Name", 0, 0, 0);
        String expResult = "Other Name";
        instance.setName(expResult);
        String result = instance.getName();
        assertEquals(expResult, result);
    }

    /**
     * Test of setWidth/getWidth method, of class ScreenSize.
     */
    @Test
    public void testWidth() {
        log("ScreenSize.width");
        ScreenSize instance = new ScreenSize("Name", 0, 0, 0);
        int expResult = 1900;
        instance.setWidth(expResult);
        int result = instance.getWidth();
        assertEquals(expResult, result);
    }

    /**
     * Test of setAspectRatio/getAspectRatio method, of class ScreenSize.
     */
    @Test
    public void testAspectRatio() {
        log("ScreenSize.aspectRatio");
        ScreenSize instance = new ScreenSize("Name", 0, 0, 0);
        double expResult = 0.75;
        instance.setAspectRatio(expResult);
        double result = instance.getAspectRatio();
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of toString method, of class ScreenSize.
     */
    @Test
    public void testToString() {
        log("ScreenSize.toString");
        ScreenSize instance = new ScreenSize("Name", 0, 0, 0);
        assertEquals("Name", instance.toString());
    }
    
    
}
