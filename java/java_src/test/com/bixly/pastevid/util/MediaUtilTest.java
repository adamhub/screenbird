/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
public class MediaUtilTest {
    
    //Define correct system
    private final boolean isWindows = false;
    private final boolean isMacOSX = true;
    private final boolean isUnix = false;
    //Define Operting System Name
    private final String opSysNomalizedName = "linux";
    
    private final static File SAMPLE_MP4 = new File("/home/user/Videos/test2.mp4");
    
    public MediaUtilTest() {
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
     * Test of open method, of class MediaUtil.
     */
    @Test
    public void testOpen() throws IOException {
        System.out.println("open");
        
        assert SAMPLE_MP4.exists() : "This test requires a MP4 video sample";
        String fileToBeOpen = SAMPLE_MP4.getAbsolutePath();
        boolean expResult = true;
        boolean result = MediaUtil.open(fileToBeOpen);
        assertEquals(expResult, result);
        
        
        File html = generateTempFile("html");
        html.createNewFile();
        
        FileWriter scriptWriter = new FileWriter(html);
        scriptWriter.append("<html><head></head><body><h1>Test Successfull</h1></body></html>");
        scriptWriter.close();
        
        LibraryUtil.chmod("+x", html);
        html.setExecutable(true);
        result = MediaUtil.open(fileToBeOpen);
        assertEquals(expResult, result);
    }

    /**
     * Test of osIsWindows method, of class MediaUtil.
     */
    @Test
    public void testOsIsWindows() {
        System.out.println("osIsWindows");
        boolean expResult = isWindows;
        boolean result = MediaUtil.osIsWindows();
        assertEquals(expResult, result);
    }

    /**
     * Test of osIsMac method, of class MediaUtil.
     *
    @Test
    public void testOsIsMac() {
        System.out.println("osIsMac");
        boolean expResult = isMacOSX;
        boolean result = MediaUtil.osIsMac();
        assertEquals(expResult, result);
    }
    */

    /**
     * Test of osIsUnix method, of class MediaUtil.
     *
    @Test
    public void testOsIsUnix() {
        System.out.println("osIsUnix");
        boolean expResult = isUnix;
        boolean result = MediaUtil.osIsUnix();
        assertEquals(expResult, result);
    }
    * */

    /**
     * Test of getNormalizedOSName method, of class MediaUtil.
     */
    @Test
    public void testGetNormalizedOSName() {
        System.out.println("getNormalizedOSName");
        String expResult = "";
        if (MediaUtil.osIsMac()) expResult = "mac";
        else if (MediaUtil.osIsUnix()) expResult = "linux";
        else if (MediaUtil.osIsWindows()) expResult = "windows";
        String result = MediaUtil.getNormalizedOSName();
        assert expResult.equalsIgnoreCase(result) : expResult + " vs. " + result;
    }

    /**
     * Test of getMacAddress method, of class MediaUtil.
     */
    @Test
    public void testGetMacAddress() {
        System.out.println("getMacAddress");
        String result = MediaUtil.getMacAddress();
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
    }

}
