package com.bixly.pastevid.recorders;

import com.bixly.pastevid.util.LogUtil;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import javax.swing.ImageIcon;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Bixly
 */
public class RecorderTest {
    
    
    
    public RecorderTest() {
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
    
    private static void log(Object string) {
        LogUtil.log(RecorderTest.class,string);
    }
    
    /**
     * Test of hasStatus method, of class Recorder.
     */
    @Test
    public void testHasStatus() {
        log("hasStatus");
        RecorderStatus status = RecorderStatus.RECORDING;
        ImageIcon preload = new ImageIcon(); // Preload the ImageIcon library
        Recorder instance = new Recorder(null);
        Boolean expResult = false;
        Boolean result = instance.hasStatus(status);
        assertEquals(expResult, result);
        
        status = RecorderStatus.STOPPED;
        expResult = true;
        result = instance.hasStatus(status);
        assertEquals(expResult, result);
        
        status = RecorderStatus.PAUSED;
        expResult = false;
        result = instance.hasStatus(status);
        assertEquals(expResult, result);
    }

    /**
     * Test of setStatus method, of class Recorder.
     */
    @Test
    public void testSetStatus() {
        log("setStatus");
        RecorderStatus status = RecorderStatus.STOPPED;
        Recorder instance = new Recorder(null);
        instance.setStatus(status);
        
        Boolean expResult = true;
        Boolean result = instance.hasStatus(status);
        assertEquals(expResult, result);
    }

    /**
     * Test of getMillisecondsTime method, of class Recorder.
     */
    @Test
    public void testGetMillisecondsTime() {
        log("getMillisecondsTime");
        Recorder instance = new Recorder(null);
        long expResult = 0L;
        long result = instance.getMillisecondsTime();
        assertEquals(expResult, result);
    }

    /**
     * Test of getSecondsTime method, of class Recorder.
     */
    @Test
    public void testGetSecondsTime() {
        log("getSecondsTime");
        Recorder instance = new Recorder(null);
        long expResult = 0L;
        long result = instance.getSecondsTime();
        assertEquals(expResult, result);
    }

    /**
     * Test of getScreen method, of class Recorder.
     */
    @Test
    public void testGetScreen() {
        log("getScreen");
        Recorder instance = new Recorder(null);
        GraphicsDevice expResult = null;
        GraphicsDevice result = instance.getScreen();
        assertEquals(expResult, result);
    }

    /**
     * Test of dropAudioLine method, of class Recorder.
     */
    @Test
    public void testDropAudioLine() {
         log("dropAudioLine");
        Recorder instance = new Recorder(new TestMeasurable());
        instance.dropAudioLine();
        assertFalse(instance.isLineOpen());
    }

    /**
     * Test of getMp4 method, of class Recorder.
     
     */
    @Test
    public void testGetMp4() {
        log("getMp4");
        Recorder instance = new Recorder(new TestMeasurable());
        instance.recordVideo();
        instance.stopVideo();
        String result = instance.getMp4();
        assertNotNull(result);
    }

    /**
     * Test of getWav method, of class Recorder.
     
     */
    @Test
    public void testGetWav() {
        log("getWav");
        Recorder instance = new Recorder(new TestMeasurable());
        instance.recordVideo();
        instance.stopVideo();
        String result = instance.getWav();
        assertNotNull(result);
    }

    /**
     * Test of getFile method, of class Recorder.
     
     */
    @Test
    public void testGetFile() {
        log("getFile");
        Recorder instance = new Recorder(new TestMeasurable());
        instance.recordVideo();
        instance.stopVideo();
        String result = instance.getFile();
        assertNotNull(result);
    }

    /**
     * Test of getFileMS method, of class Recorder.
     
     */
    @Test
    public void testGetFileMS() {
        log("getFileMS");
        Recorder instance = new Recorder(new TestMeasurable());
        instance.recordVideo();
        instance.stopVideo();
        long result = instance.getFileMS();
        assertNotNull(result);
    }

    @Test
    public void testGetOffset() {
        log("getOffset");
        Recorder instance = new Recorder(new TestMeasurable());
        instance.recordAudio();
        instance.recordVideo();
        instance.stopVideo();
        instance.stopRecordingAudio();
        instance.getOffset();
        assertNotNull(instance.getOffset());
        
    }

    @Test
    public void testGetCaptureFolder() {
        log("getCaptureFolder");
        Recorder instance = new Recorder(null);
        String expResult = "";
        String result = instance.getCaptureFolder();
        assertTrue(expResult.equals(result));
    }

    @Test
    public void testGetVideoCaptureFolder() {
        log("getVideoCaptureFolder");
        Recorder instance = new Recorder(null);
        String expResult = instance.getCaptureFolder();
        String result = instance.getVideoCaptureFolder();
        assertNotSame(expResult, result);
    }

    @Test
    public void testGetSeparator() {
        log("getSeparator");
        Recorder instance = new Recorder(null);
        String expResult = System.getProperty("file.separator");
        String result = instance.getSeparator();
        assertEquals(expResult, result);
    }

    @Test
    public void testDeleteFiles() {
        log("deleteFiles");
        Recorder instance = new Recorder(new TestMeasurable());
        instance.cleanAndCreateFiles();
        instance.setCaptureRectangle(new Rectangle(200, 100));
        instance.getVideoCaptureFolder();
        File dir = new File(instance.getCaptureFolder());
        Boolean isDir = dir.isDirectory();    
        assertTrue(isDir);        
        instance.recordVideo();
        while(instance.getSecondsTime() < 10) {}
        instance.stopVideo();  
        if (isDir) {
            assertTrue(dir.list().length > 0);
            int filesBefore = dir.list().length;
            instance.deleteFiles();
            assertTrue(dir.list().length < filesBefore);
        }
    }


    /**
     * Test of recordAudio method, of class Recorder.
     * 
     */
    //@Test
    @Test
    public void testRecordAudio() {
        log("recordAudio");
        Recorder instance = new Recorder(new TestMeasurable());
        instance.recordAudio();    
        while (instance.getSecondsTime() < 5)
        {            
        }
        instance.stopRecordingAudio();
        //instance.compileAudio();
    }

    /**
     * Test of isLineOpen method, of class Recorder.
     * 
     */
    @Test
    public void testIsLineOpen() {
        log("isLineOpen");
        Recorder instance = new Recorder(new TestMeasurable());
        Boolean result = instance.isLineOpen();
        assertNotNull(result);
    }

    /**
     * Test of getVolume method, of class Recorder.
     * 
     */
    @Test
    public void testGetVolume() {
        log("getVolume");
        Recorder instance = new Recorder(new TestMeasurable());
        int expResult = 0;
        int result = instance.getVolume();
        assertTrue(result >= expResult);
    }
 
    /**
     * Test of getMaxVolume method, of class Recorder.
     * 
     */
    @Ignore
    public void testGetMaxVolume() {
        log("getMaxVolume");
        Recorder instance = null;
        int expResult = 0;
        int result = instance.getMaxVolume();
        assertTrue(result >= expResult);
    }

    /**
     * Test of compileAudio method, of class Recorder.
     * 
     */
    //@Test
    @Test
    public void testCompileAudio() throws IOException {
        log("compileAudio");
        Recorder instance = new Recorder(new TestMeasurable());
        instance.setCaptureRectangle(new Rectangle(200, 100));
        instance.recordAudio();
        while(instance.getSecondsTime() < 10)
        {
        }
        instance.stopRecordingAudio();
        instance.compileAudio();
    }
    
    /**
     * Test of pauseVideo method, of class Recorder.
     
     */
    //@Test
    @Test
    public void testPauseVideo() {
       
        log("pauseVideo");
        Recorder instance = new Recorder(new TestMeasurable());
        instance.setCaptureRectangle(new Rectangle(200, 100));
        instance.recordVideo();
        assertEquals(instance.getStatus(), RecorderStatus.RECORDING);
        instance.pauseVideo();
        assertEquals(instance.getStatus(), RecorderStatus.PAUSED);
        instance.stopVideo();
    }
    
    /**
     * Test of isWaitingForAudio method, of class Recorder.
     * 
     */
    //@Test
    @Test
    public void testIsWaitingForAudio() {
        log("isWaitingForAudio");
        Recorder instance = new Recorder(new TestMeasurable());
        instance.setCaptureRectangle(new Rectangle(200, 100));
        instance.recordVideo();
        boolean result = instance.isWaitingForAudio();
        assertEquals(false, result);
        instance.stopVideo();
        instance.compileAudio();
        result = instance.isWaitingForAudio();
        assertEquals(true, result);
    }

    /**
     * Test of hasAudioToCompile method, of class Recorder.
     * 
     */
    //@Test
    @Test
    public void testHasAudioToCompile() {
        log("hasAudioToCompile");
        Recorder instance = new Recorder(new TestMeasurable());
        instance.setCaptureRectangle(new Rectangle(200, 100));
        instance.recordVideo();
        while(instance.getSecondsTime() < 10)
        {
        }
        instance.stopVideo();
        boolean result = instance.hasAudioToCompile();
        assertNotNull(result);
    }
    
    /**
     * Test of mark the creating files to mark state method, of class Recorder.
     * 
     */
    //@Test
    @Test
    public void testMarkSaveState() {
        log("markSaveState");
        Recorder instance = new Recorder(new TestMeasurable());
        instance.cleanAndCreateFiles();
        instance.markStateSaved();
        Boolean result = instance.checkStateSaved();
        assertTrue(result);
        instance.unmarkStateSaved();
        result = instance.checkStateSaved();
        assertFalse(result);
    }
    
    /**
     * Test of compileAudio method, of class Recorder.
     * 
     */
    //@Test
    @Test
    public void testCompileAudioWithSilence() throws IOException {
        log("compileAudioWithSilence");
        Recorder instance = new Recorder(new TestMeasurable());
        instance.setCaptureRectangle(new Rectangle(200, 100));
        instance.recordVideo();
        instance.recordAudio();
        while(instance.getSecondsTime() < 10)
        {
        }
        instance.pauseVideo();
        while(instance.getSecondsTime() < 20)
        {
        }
        instance.recordVideo();
        while(instance.getSecondsTime() < 30)
        {
        }
        instance.stopVideo();
        instance.compileAudio();
    }
     /**
     * Test of compileVideo method, of class Recorder.
     
     */
    //@Test
    @Test
    public void testCompileVideo() throws Exception {
        log("compileVideo");
        Recorder instance = new Recorder(new TestMeasurable());
        instance.cleanAndCreateFiles();
        instance.setCaptureRectangle(new Rectangle(200, 100));
        instance.recordVideo();
        while(instance.getSecondsTime() < 10) {}
        instance.stopVideo();        
        Boolean expResult = true;
        Boolean result = instance.compileVideo();
        assertEquals(expResult, result);
    }
    
    
    /**
     * Test of save and loading settings method, of class Recorder.
     
     */
    //@Test
    @Test
    public void testSaveLoadSettings() throws Exception {
        log("saveLoadSettings");
        Recorder instance = new Recorder(new TestMeasurable());
        
        Integer expResult = new Integer(1);
        instance.cleanAndCreateFiles();
        //Save all settings
        instance.saveVideoQuality(expResult);
        
        //Load all settings
        Integer result = instance.loadVideoQuality();
        assertEquals(expResult, result);
    }
    
    
    
    
    class TestMeasurable implements IMeasurable
    {

        private long initiTime;
        public TestMeasurable()
        {
            initiTime = System.currentTimeMillis();
        }
        public long getValue() {
            return System.currentTimeMillis() - initiTime;
        }

        public GraphicsDevice getScreen() {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsConfiguration gd = ge.getScreenDevices()[0].getDefaultConfiguration();
            GraphicsDevice device = gd.getDevice();
            return device;
        }
        
        public void relocate(int x, int y){
            
        }
        
    }
}
