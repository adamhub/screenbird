/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.util;

import java.io.File;
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
public class PropertiesUtilTest {
    
    public PropertiesUtilTest() {
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
    
    private static File generateTempFile(String extension){
        return new File(System.getProperty("java.io.tmpdir"), Long.toString(System.nanoTime())+"."+extension);
    }
    private static File generateTempFile(){
        return generateTempFile("tmp");
    }
    private static File generateTempFile(String directory, String extension){
        return new File(directory, Long.toString(System.nanoTime())+"."+extension);
    }
    private static File createDirectory(File file){
        file.mkdirs();
        return file;
    }
    private static File generateTempDirectory(){
        return createDirectory(new File(System.getProperty("java.io.tmpdir"), Long.toString(System.nanoTime())));
    }

    /**
     * Test of saveProperty method, of class PropertiesUtil.
     */
    @Test
    public void testSaveProperty() {
        System.out.println("saveProperty");
        String propertyFile = generateTempFile("config").getAbsolutePath();
        String key = "savePropertyTest";
        String expResult = RandomUtil.generateSlug(50);
        PropertiesUtil.saveProperty(propertyFile, key, expResult);
        
        String result = PropertiesUtil.loadProperty(propertyFile, key);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.equalsIgnoreCase(expResult));
        
    }

    /**
     * Test of loadProperty method, of class PropertiesUtil.
     */
    @Test
    public void testLoadProperty() {
        System.out.println("loadProperty");
        String key = "loadPropertyTest";
        String propertyFile = generateTempFile("config").getAbsolutePath();
        String expResult = RandomUtil.generateSlug(50);
        PropertiesUtil.saveProperty(propertyFile, key, expResult);
        
        String result = PropertiesUtil.loadProperty(propertyFile, key);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.equalsIgnoreCase(expResult));
    }
}
