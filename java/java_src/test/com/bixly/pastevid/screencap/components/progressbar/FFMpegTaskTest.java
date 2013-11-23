/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.screencap.components.progressbar;

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
public class FFMpegTaskTest {
    
    public FFMpegTaskTest() {
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

    /**
     * Test of getProgress method, of class FFMpegTask.
     */
    @Test
    public void testGetProgress() {
        System.out.println("getProgress");
        FFMpegTask instance = new FFMpegTask();
        instance.setDuration(100);
        
        int expResult = 0;
        int result = instance.getProgress();
        assertEquals(expResult, result);
        
        expResult = 10;
        instance.setSeconds(expResult);
        result = instance.getProgress();
        assertEquals(expResult, result);
    }

    /**
     * Test of setDuration method, of class FFMpegTask.
     */
    @Test
    public void testSetDuration() {
        System.out.println("setDuration");
        FFMpegTask instance = new FFMpegTask();
        instance.setDuration(100);
        
        int expResult = 0;
        int result = instance.getProgress();
        assertEquals(expResult, result);
    }

    /**
     * Test of getSeconds method, of class FFMpegTask.
     */
    @Test
    public void testGetSeconds() {
        System.out.println("getSeconds");
        FFMpegTask instance = new FFMpegTask();
        int expResult = 0;
        int result = instance.getSeconds();
        assertEquals(expResult, result);
        
        expResult = 10;
        instance.setSeconds(expResult);
        result = instance.getSeconds();
        assertEquals(expResult, result);
    }

    /**
     * Test of setSeconds method, of class FFMpegTask.
     */
    @Test
    public void testSetSeconds() {
        System.out.println("setSeconds");
        FFMpegTask instance = new FFMpegTask();
        int expResult = 0;
        int result = instance.getSeconds();
        assertEquals(expResult, result);
        
        expResult = 10;
        instance.setSeconds(expResult);
        result = instance.getSeconds();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDuration method, of class FFMpegTask.
     */
    @Test
    public void testGetDuration() {
        System.out.println("getDuration");
        FFMpegTask instance = new FFMpegTask();
        instance.setDuration(100);
        
        int expResult = 0;
        int result = instance.getProgress();
        assertEquals(expResult, result);
    }
}
