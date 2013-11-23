/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.util;

import com.bixly.pastevid.Settings;
import com.bixly.pastevid.download.DownloadManager;
import com.bixly.pastevid.download.DownloadUnzip;
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
public class LibraryUtilTest {
    
    private final File SAMPLE_ZIP = new File("/home/user/Videos/test.zip");
    
    public LibraryUtilTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        confirmLibrariesStatus();
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
    
    private static void confirmLibrariesStatus(){
        DownloadManager downloadManager = DownloadManager.getInstance();
        
        if(MediaUtil.osIsWindows()){
            //Windows need unzipping utility
            downloadManager.registerDownload(
                Settings.getUnzipExecutable(),
                new DownloadUnzip()
            );
        }
        
        downloadManager.start();
        
        while(downloadManager.isRunning()){
            System.out.println("Waiting for Binary Downloads before running test");
            TimeUtil.skipToMyLou(5);
        }
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
     * Test of execute method, of class LibraryUtil.
     *
    @Test
    public void testExecute_3args_1() throws IOException {
        System.out.println("execute");
        
        //MediaUtil.open calls LibraryUtil.exectue()
        File file = generateTempFile("txt");
        file.createNewFile();
        
        boolean expResult = true;
        boolean result = MediaUtil.open(file.getAbsolutePath());
        assertTrue(expResult == result);
    }
    */

    /**
     * Test of execute method, of class LibraryUtil.
     *
    @Test
    public void testExecute_4args_1() throws IOException {
        System.out.println("execute");
        //MediaUtil.open calls LibraryUtil.exectue()
        File file = generateTempFile("txt");
        file.createNewFile();
        
        boolean expResult = true;
        boolean result = MediaUtil.open(file.getAbsolutePath());
        assertTrue(expResult == result);
    }
    * */

    /**
     * Test of execute method, of class LibraryUtil.
     *
    @Test
    public void testExecute_4args_2() throws IOException {
        System.out.println("execute");
        //MediaUtil.open calls LibraryUtil.exectue()
        File file = generateTempFile("txt");
        file.createNewFile();
        
        boolean expResult = true;
        boolean result = MediaUtil.open(file.getAbsolutePath());
        assertTrue(expResult == result);
    }
    * */

    /**
     * Test of execute method, of class LibraryUtil.
     *
    @Test
    public void testExecute_3args_2() throws IOException {
        System.out.println("execute");
        //MediaUtil.open calls LibraryUtil.exectue()
        File file = generateTempFile("txt");
        file.createNewFile();
        
        boolean expResult = true;
        boolean result = MediaUtil.open(file.getAbsolutePath());
        assertTrue(expResult == result);
    }
    * */

    /**
     * Test of execute method, of class LibraryUtil.
     *
    @Test
    public void testExecute_4args_3() throws IOException {
        System.out.println("execute");
        //MediaUtil.open calls LibraryUtil.exectue()
        File file = generateTempFile("txt");
        file.createNewFile();
        
        boolean expResult = true;
        boolean result = MediaUtil.open(file.getAbsolutePath());
        assertTrue(expResult == result);
    }
    * */

    /**
     * Test of executeScript method, of class LibraryUtil.
     */
    @Test
    public void testExecuteScript_StringArr_File() throws IOException {
        System.out.println("executeScript (String[], File)");
        
        if (MediaUtil.osIsMac() || MediaUtil.osIsUnix()) {
            File workingDir = generateTempDirectory();
            File script = new File(workingDir, String.valueOf(System.currentTimeMillis()));
            script.createNewFile();

            FileWriter scriptWriter = new FileWriter(script);
            scriptWriter.append("echo \"executeScript script contents\"");
            scriptWriter.close();

            LibraryUtil.chmod("+x", script);
            script.setExecutable(true);

            String[] args = {
                script.getAbsolutePath()
            };
            boolean expResult = true;
            boolean result = LibraryUtil.executeScript(args, workingDir);
            assertEquals(expResult, result);   
        } else {
            System.out.println("This test is only for Mac/Linux");
        }
    }

    /**
     * Test of executeScript method, of class LibraryUtil.
     */
    @Test
    public void testExecuteScript_StringArr() throws IOException {
        System.out.println("executeScript (String[])");
        File script = generateTempFile("");
        script.createNewFile();
        
        FileWriter scriptWriter = new FileWriter(script);
        scriptWriter.append("echo \"executeScript script contents\"");
        scriptWriter.close();
        
        LibraryUtil.chmod("+x", script);
        script.setExecutable(true);
        
        String[] args = {
            script.getAbsolutePath()
        };
        boolean expResult = true;
        boolean result = LibraryUtil.executeScript(args);
        assertEquals(expResult, result);
    }

    /**
     * Test of wget method, of class LibraryUtil.
     */
    @Test
    public void testWget() {
        System.out.println("wget");
        String urlSource = "http://screenbird.com/media/applet/launch.jnlp";
        String destDir = generateTempDirectory().getAbsolutePath();
        String filename = String.valueOf(System.currentTimeMillis())+".jnlp";
        
        boolean expResult = true;
        boolean result = LibraryUtil.wget(urlSource, destDir, filename);
        
        File resultFile = new File(destDir,filename);
        
        assertEquals(expResult, result);
        assertTrue(resultFile.exists());
        assertTrue(resultFile.length() > 0);
        
    }

    /**
     * Test of unzip method, of class LibraryUtil.
     */
    @Test
    public void testUnzip_String() {
        System.out.println("unzip");
        
        assert SAMPLE_ZIP.exists() : "This test requires a sample zip file. Just compress/zip a few zip files";
        
        String source = SAMPLE_ZIP.getAbsolutePath();
        boolean expResult = true;
        boolean result = LibraryUtil.unzip(source);
        assertEquals(expResult, result);
        
        
    }

    /**
     * Test of unzip method, of class LibraryUtil.
     */
    @Test
    public void testUnzip_File() {
        System.out.println("unzip");
        
        assert SAMPLE_ZIP.exists() : "This test requires a sample zip file. Just compress/zip a few zip files";
        
        boolean expResult = true;
        boolean result = LibraryUtil.unzip(SAMPLE_ZIP);
        assertEquals(expResult, result);
    }

    /**
     * Test of chmod method, of class LibraryUtil.
     */
    @Test
    public void testChmod() {
        System.out.println("chmod");
        String permissions = "700";
        File file = generateTempFile("txt");
        boolean expResult = true;
        boolean result = LibraryUtil.chmod(permissions, file);
        assertEquals(expResult, result);
        
        permissions = "755";
        file = generateTempFile("txt");
        result = LibraryUtil.chmod(permissions, file);
        assertEquals(expResult, result);
        
        permissions = "7777";
        file = generateTempFile("txt");
        result = LibraryUtil.chmod(permissions, file);
        assertEquals(expResult, result);
    }

    /**
     * Test of prepDestinationDir method, of class LibraryUtil.
     */
    @Test
    public void testPrepDestinationDir() {
        System.out.println("prepDestinationDir");
        String destDir = generateTempFile("").getAbsolutePath();
        File expResult = new File(destDir);
        File result = LibraryUtil.prepDestinationDir(destDir);
        
        assertTrue(result.exists());
        assertTrue(result.isDirectory());
        assertTrue(expResult.getAbsolutePath().equalsIgnoreCase(result.getAbsolutePath()));
    }

    /**
     * Test of getUrl method, of class LibraryUtil.
     */
    @Test
    public void testGetUrl() {
        System.out.println("getUrl");
        
        assertNotNull(LibraryUtil.getUrl());
        assertTrue(LibraryUtil.getUrl().length() > 0);
        
    }
}
