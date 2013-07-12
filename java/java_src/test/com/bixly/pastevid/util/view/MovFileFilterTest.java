/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.util.view;

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
public class MovFileFilterTest {
    
    public MovFileFilterTest() {
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
    private static File createDirectory(File file){
        file.mkdirs();
        return file;
    }
    private static File generateTempDirectory(){
        return createDirectory(new File(System.getProperty("java.io.tmpdir"), Long.toString(System.nanoTime())));
    }

    /**
     * Test of accept method, of class MovFileFilter.
     */
    @Test
    public void testAccept() {
        System.out.println("accept");
        File f = generateTempFile("etx");
        MovFileFilter instance = new MovFileFilter();
        boolean expResult = false;
        boolean result = instance.accept(f);
        assertEquals(expResult, result);
        
        f = generateTempFile("mp4");
        expResult = true;
        result = instance.accept(f);
        assertEquals(expResult, result);
        
        f = generateTempDirectory();
        expResult = true;
        result = instance.accept(f);
        assertEquals(expResult, result);
        
    }

    /**
     * Test of getDescription method, of class MovFileFilter.
     */
    @Test
    public void testGetDescription() {
        System.out.println("getDescription");
        MovFileFilter instance = new MovFileFilter();
        String expResult = "Mp4 Video File";
        String result = instance.getDescription();
        assertEquals(expResult, result);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}
