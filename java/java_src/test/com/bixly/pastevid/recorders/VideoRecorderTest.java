/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.recorders;

import com.bixly.pastevid.util.LogUtil;
import java.awt.Rectangle;
import javax.swing.ImageIcon;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matias
 */
public class VideoRecorderTest {
    
    public VideoRecorderTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        ImageIcon preload = new ImageIcon(); // Fix for ImageIcon issues on OS X
    }
    
    @After
    public void tearDown() {
    }
    
    private static void log(Object string) {
        LogUtil.log(VideoRecorderTest.class,string);
    }

    /**
     * Test of record method, of class VideoRecorder.
     */
    @Test
    public void testRecord() {
        log("record");
        Recorder recorder = new Recorder(null);        
        VideoRecorder instance = new VideoRecorder(recorder);
        instance.record();
        assertTrue(recorder.getStatus() == RecorderStatus.RECORDING);
        instance.pause();
        assertTrue(recorder.getStatus() == RecorderStatus.PAUSED);
        instance.stop();
        assertTrue(recorder.getStatus() == RecorderStatus.STOPPED);
    }

    /**
     * Test of compileVideo method, of class VideoRecorder.
     */
    @Ignore
    public void testCompileVideo() throws Exception {
        log("compileVideo");

        fail("The test case is a prototype.");
    }

    /**
     * Test of getStartMS method, of class VideoRecorder.
     */
    @Test
    public void testGetStartMS() {
        log("getStartMS");
        Recorder recorder = new Recorder(null);        
        VideoRecorder instance = new VideoRecorder(recorder);
        instance.record();
        long expResult = 0L;
        long result = instance.getStartMS();
        assertEquals(expResult, result);
        instance.stop();
    }

    /**
     * Test of getOutputFileName method, of class VideoRecorder.
     */
    @Test
    public void testGetOutputFileName() {
        log("getOutputFileName");
        Recorder recorder = new Recorder(null);  
        VideoRecorder instance = new VideoRecorder(recorder);
        instance.record();        
        String result = instance.getOutputFileName();
        assertNotNull(result);
        instance.stop();
    }

    /**
     * Test of setOutputFileName method, of class VideoRecorder.
     */
    @Test
    public void testSetOutputFileName() {
        log("setOutputFileName");
        String filename = "test.mov";
        Recorder recorder = new Recorder(null);  
        VideoRecorder instance = new VideoRecorder(recorder);        
        instance.setOutputFileName(filename);
        assertTrue(instance.getOutputFileName().contains(filename));        
    }

    /**
     * Test of getOutputMovieFileMillis method, of class VideoRecorder.
     */
    @Test
    public void testGetOutputMovieFileMillis() {
        log("getOutputMovieFileMillis");
        Recorder recorder = new Recorder(null);  
        VideoRecorder instance = new VideoRecorder(recorder);
        long expResult = System.currentTimeMillis();
        instance.record();
        long result = instance.getOutputMovieFileMillis();
        instance.stop();
        assertTrue(expResult <= result);
    }

    /**
     * Test of setOutputMovieFileMillis method, of class VideoRecorder.
     */
    @Test
    public void testSetOutputMovieFileMillis() {
        log("setOutputMovieFileMillis");
        Recorder recorder = new Recorder(null);  
        VideoRecorder instance = new VideoRecorder(recorder);
        long expResult = System.currentTimeMillis();
        instance.setOutputMovieFileMillis(expResult);
        assertTrue(expResult == instance.getOutputMovieFileMillis());
    }

    /**
     * Test of getCaptureRectangle method, of class VideoRecorder.
     */
    @Test
    public void testGetCaptureRectangle() {
        log("getCaptureRectangle");
        Recorder recorder = new Recorder(null);  
        VideoRecorder instance = new VideoRecorder(recorder);
        Rectangle expResult = new Rectangle(100, 100);
        instance.setCaptureRectangle(expResult);
        Rectangle result = instance.getCaptureRectangle();
        assertEquals(expResult, result);
    }  

    /**
     * Test of setFps method, of class VideoRecorder.
     */
    @Test
    public void testSetFps() {
        log("setFps");
        int framesPerSecond = 0;
        Recorder recorder = new Recorder(null);  
        VideoRecorder instance = new VideoRecorder(recorder);
        instance.setFps(framesPerSecond);
        assertEquals(framesPerSecond, instance.getFps());
    }    
}
