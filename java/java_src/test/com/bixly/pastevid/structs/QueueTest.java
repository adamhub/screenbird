/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.structs;

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
public class QueueTest {
    
    private static Queue<TestObj> instance;
    
    public QueueTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        
        instance = new Queue<TestObj>();
        
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
    
    private class TestObj {
        private int integer;
        private String string;
        
        public TestObj(int integer, String string) {
            this.integer = integer;
            this.string = string;
        }

        public int getInteger() {
            return integer;
        }

        public String getString() {
            return string;
        }
        
    }

    /**
     * Test of push method, of class Queue.
     */
    @Test
    public void testPush() {
        System.out.println("push");
        
        instance.clear();
        assertTrue(instance.isEmpty());
        
        TestObj object = new TestObj(10, "Willy Wonka & the Chocolate Factory");
        instance.push(object);
        
        assertFalse(instance.isEmpty());
        assertEquals(instance.peek(), object);
    }

    /**
     * Test of pop method, of class Queue.
     */
    @Test
    public void testPop() {
        System.out.println("pop");
        
        instance.clear();
        
        TestObj object = new TestObj(123, "Buggs Bunny");
        instance.push(object);
        
        assertFalse(instance.isEmpty());
        
        assertEquals(instance.pop(), object);
        
    }

    /**
     * Test of peek method, of class Queue.
     */
    @Test
    public void testPeek() {
        System.out.println("peek");
        TestObj object = new TestObj(10, "Dellaramo");
        instance.push(object);
        
        assertEquals(instance.peek(), object);
    }

    /**
     * Test of isEmpty method, of class Queue.
     */
    @Test
    public void testIsEmpty() {
        System.out.println("isEmpty");
        
        instance.clear();
        
        boolean expResult = true;
        boolean result = instance.isEmpty();
        assertEquals(expResult, result);
        
        instance.push(new TestObj(292, "cellar door"));
        
        expResult = false;
        result = instance.isEmpty();
        assertEquals(expResult, result);
        
    }

    /**
     * Test of size method, of class Queue.
     */
    @Test
    public void testSize() {
        System.out.println("size");
        
        instance.clear();
        
        assertTrue(instance.isEmpty());
        
        instance.push(new TestObj(43, "To cool for school"));
        
        assertFalse(instance.isEmpty());
        
    }
}
