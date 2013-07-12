/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.util;

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
public class LogUtilTest {
    
    public LogUtilTest() {
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
    
    private class CustomObject {
        
        private byte byteVal = 3;
        private short shortVal = 23;
        private int integer = 123;
        private float floatVal = 232.323332F;
        private double doubleVal = 323.33;
        private long longVal = 32323233244243L;
        private int[] integerArr = {23,4,599,292};
        private char charVal = 'd';
        private String string = "test string";
        private boolean booleanVal = false;

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append(byteVal + "\n");
            b.append(shortVal + "\n");
            b.append(integer + "\n");
            b.append(floatVal + "\n");
            b.append(doubleVal + "\n");
            b.append(longVal + "\n");
            b.append(integerArr + "\n");
            b.append(charVal + "\n");
            b.append(string + "\n");
            b.append(booleanVal + "\n");
            return b.toString();
        }
    }
    
    
    private class CustomException extends Exception {

        public CustomException(String string) {
            super(string);
        }
        
    }

    /**
     * Test of log method, of class LogUtil.
     */
    @Test
    public void testLog_Class_Object() {
        System.out.println("log");
        Class clazz = LogUtilTest.class;
        
        CustomObject obj = new CustomObject();
        LogUtil.log(clazz, obj);
        
        try{
            throw new CustomException("This is a Custom Exception Error");
        }catch (CustomException e){
            LogUtil.log(clazz, e);
        }
        
        
    }

    /**
     * Test of log method, of class LogUtil.
     */
    @Test
    public void testLog_Class_String() {
        System.out.println("log");
        Class clazz = LogUtilTest.class;
        String message = "Custom log String";
        LogUtil.log(clazz, message);
    }

    /**
     * Test of isLoggerReady method, of class LogUtil.
     */
    @Test
    public void testIsLoggerReady() {
        System.out.println("isLoggerReady");
        boolean expResult = true;
        boolean result = LogUtil.isLoggerReady();
        assertEquals(expResult, result);
    }

    /**
     * Test of printSystemProperties method, of class LogUtil.
     */
    @Test
    public void testPrintSystemProperties() {
        System.out.println("printSystemProperties");
        LogUtil.printSystemProperties();
    }

   

    /**
     * Test of flush method, of class LogUtil.
     */
    @Test
    public void testFlush() {
        System.out.println("flush");
        LogUtil.flush();
    }
    
    
     /**
     * Test of close method, of class LogUtil.
     */
    @Test
    public void testClose() {
        
        
        //Needs to be last test so it does not close logger on other tests
        
        System.out.println("close");
        LogUtil.close();
        
        assertFalse(LogUtil.isLoggerReady());
    }
}
