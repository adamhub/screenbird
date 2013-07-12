/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.util;

import java.util.HashMap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author cevaris
 */
public class RandomUtilTest {
    
    public RandomUtilTest() {
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
     * Test of generateSlug method, of class RandomUtil.
     */
    @Ignore
    public void testGenerateSlug_int_HashMap() {
        System.out.println("generateSlug");
        int length = 0;
        HashMap<String, Object> collection = null;
        String expResult = "";
        String result = RandomUtil.generateSlug(length, collection);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of generateSlug method, of class RandomUtil.
     */
    @Test
    public void testGenerateSlug_int() {
        System.out.println("generateSlug");
        int length = 10;
        String result = RandomUtil.generateSlug(length);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.length() == length);
    }
}
