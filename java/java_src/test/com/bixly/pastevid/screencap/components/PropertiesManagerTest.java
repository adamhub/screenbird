/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.screencap.components;

import org.junit.Ignore;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
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
public class PropertiesManagerTest {
    
    private  static PropertiesManager propertiesManager;
    private static File testPropFile;
    
    private Integer test_int     = 12;
    private Double  test_double  = 15.03;
    private Float   test_float   = 2323.23F;
    private Long    test_long    = 3983838L;
    private Boolean test_boolean = false;
    private String  test_string  = "This is the test";
    
    private String DOUBLE = "double";
    private String INTEGER = "integer";
    private String BOOLEAN = "boolean";
    private String FLOAT = "float";
    private String LONG = "long";
    private String STRING = "string";
    
    public PropertiesManagerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        
        testPropFile = File.createTempFile("testPopFile", "tmp");
        
        //Make sure starting with empty file
        if(testPropFile.length()>0){
            testPropFile.delete();
            testPropFile = File.createTempFile("testPopFile", "tmp");
        }
        
        testPropFile.deleteOnExit();
        
        propertiesManager = new PropertiesManager(testPropFile);
        
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
     * Test of openProperties method, of class PropertiesManager.
     */
    @Test
    public void testWriteProperties() throws Exception {
        System.out.println("writeProperties");
        
        Properties properties = propertiesManager.readPropertyFile();
        properties.setProperty(INTEGER, test_int.toString());
        properties.setProperty(DOUBLE, test_double.toString());
        properties.setProperty(FLOAT, test_float.toString());
        properties.setProperty(LONG, test_long.toString());
        properties.setProperty(BOOLEAN, test_boolean.toString());
        properties.setProperty(STRING, test_string);
        propertiesManager.writePropertyFile(properties,"Screenbird Metadata");
        
        
        properties = propertiesManager.readPropertyFile();
        
        assertEquals(test_int.intValue(), Integer.parseInt(properties.getProperty(INTEGER,"0")));
        assertEquals(test_double.doubleValue(), Double.parseDouble(properties.getProperty(DOUBLE,"0.0")), 1.0);    
        assertEquals(test_float.floatValue(), Float.parseFloat(properties.getProperty(FLOAT,"0.0")), 1.0F);
        assertEquals(test_long.longValue(), Long.parseLong(properties.getProperty(LONG,"0")));
        assertEquals(test_boolean.booleanValue(), Boolean.parseBoolean(properties.getProperty(BOOLEAN)));
        assertTrue(test_string.equalsIgnoreCase(properties.getProperty(STRING)));
        
    }

    /**
     * Test of getOut method, of class PropertiesManager.
     */
    @Test
    public void testGetOutputStream() {
        System.out.println("getOut");
        assertNotNull(propertiesManager.getOutputStream());
    }

    /**
     * Test of locationFile method, of class PropertiesManager.
     */
    @Ignore
    public void testGetPropertiesFile() {
        System.out.println("getPropertiesFile");
        File expResult = testPropFile;
        File result = propertiesManager.getPropertiesFile();
        assertEquals(expResult, result);
    }

    /**
     * Test of readPropertyFile method, of class PropertiesManager.
     */
    @Test
    public void testReadPropertyFile() throws Exception {
        System.out.println("readPropertyFile");
        assertNotNull(propertiesManager.readPropertyFile());
    }
    
    
    /**
     * Test of closeProperties method, of class PropertiesManager.
     */
    @Test
    public void testCloseProperties() {
        System.out.println("closeProperties");
        
        try{
            assertTrue(propertiesManager.getOutputStream().getFD().valid());
            propertiesManager.closeProperties();
            assertFalse(propertiesManager.getOutputStream().getFD().valid());
        }catch (IOException e){
            
        }
    }

}
