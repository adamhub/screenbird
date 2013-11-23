package com.bixly.pastevid.models;

import com.bixly.pastevid.util.LogUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Bixly
 */
public class VideoFileItemTest {

    public VideoFileItemTest() {
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
        LogUtil.log(VideoFileItemTest.class,string);
    }

    /**
     * Test of setDirectory/getDirectory method, of class VideoFileItem.
     */
    @Test
    public void testDirectory() {
        log("VideoFileItem.directory");
        VideoFileItem instance = new VideoFileItem();
        String expResult = "LongDirectory";
        instance.setDirectory(expResult);
        String result = instance.getDirectory();
        assertEquals(expResult, result);
    }

    /**
     * Test of setEndMS/getEndMS method, of class VideoFileItem.
     */
    @Test
    public void testEndMS() {
        log("VideoFileItem.endMS");
        VideoFileItem instance = new VideoFileItem();
        long expResult = System.currentTimeMillis();
        instance.setEndMS(expResult);
        long result = instance.getEndMS();
        assertEquals(expResult, result);
    }

    /**
     * Test of setStartMS/getStartMS method, of class AudioFileItem.
     */
    @Test
    public void testStartMS() {
        log("VideoFileItem.startMS");
        VideoFileItem instance = new VideoFileItem();
        long expResult = System.currentTimeMillis();
        instance.setStartMS(expResult);
        long result = instance.getStartMS();
        assertEquals(expResult, result);
    }

    /**
     * Test of getStart method, of class AudioFileItem.
     */
    @Test
    public void testGetStart() {
        log("VideoFileItem.start");
        VideoFileItem instance = new VideoFileItem();
        long currentTime = System.currentTimeMillis();
        long expResult = currentTime / 1000;
        instance.setStartMS(currentTime);
        long result = instance.getStart();
        assertEquals(expResult, result);
    }

    /**
     * Test of getEnd method, of class AudioFileItem.
     */
    @Test
    public void testGetEnd() {
        log("VideoFileItem.end");
        VideoFileItem instance = new VideoFileItem();
        long currentTime = System.currentTimeMillis();
        long expResult = currentTime / 1000;
        instance.setEndMS(currentTime);
        long result = instance.getEnd();
        assertEquals(expResult, result);
    }
}
