/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.util;

import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author cevaris
 */
public class TimeUtilTest {
    
    public TimeUtilTest() {
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
     * Test of skipToMyLou method, of class TimeUtil.
     */
    @Test
    public void testSkipToMyLou_int() {
        System.out.println("skipToMyLou");
        int seconds = 3;
        long start = System.currentTimeMillis();
        TimeUtil.skipToMyLou(seconds);
        long end = System.currentTimeMillis();
        assertTrue(Math.abs((end - start) - 3000) < 500);
        
    }

    /**
     * Test of skipToMyLouMS method, of class TimeUtil.
     */
    @Test
    public void testSkipToMyLouMS() {
        System.out.println("skipToMyLouMS");
        long seconds = 3000L;
        long start = System.currentTimeMillis();
        TimeUtil.skipToMyLouMS(seconds);
        long end = System.currentTimeMillis();
        
        System.out.println(String.format("%s %s %s", start, end, (end-start)));
        assertTrue(Math.abs((end - start) - 3000) < 500);
        
    }

    /**
     * Test of skipToMyLou method, of class TimeUtil.
     */
    @Test
    public void testSkipToMyLou_double() {
        System.out.println("skipToMyLou");
        double seconds = 3.0;
        long start = System.currentTimeMillis();
        TimeUtil.skipToMyLou(seconds);
        long end = System.currentTimeMillis();
        assertTrue(Math.abs((end - start) - 3000) < 500);
        
    }


}
