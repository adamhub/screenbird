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
public class AudioFileItemTest {

    public AudioFileItemTest() {
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

    /**
     * Test of setDropped/isDropped method, of class AudioFileItem.
     */
    @Test
    public void testDropped() {
        log("AudioFileItem.dropped");

        AudioFileItem instance = new AudioFileItem();
        boolean expResult = false;
        instance.setDropped(expResult);
        boolean result = instance.isDropped();
        assertEquals(expResult, result);
    }

    /**
     * Test of setPreviousDropped/isPreviousDropped method, of class AudioFileItem.
     */
    @Test
    public void testPreviousDropped() {
        log("AudioFileItem.previousDropped");
        AudioFileItem instance = new AudioFileItem();
        boolean expResult = false;
        instance.setPreviousDropped(expResult);
        boolean result = instance.isPreviousDropped();
        assertEquals(expResult, result);
    }

    /**
     * Test of setName/getName method, of class AudioFileItem.
     */
    @Test
    public void testName() {
        log("AudioFileItem.name");
        AudioFileItem instance = new AudioFileItem();
        String expResult = "Test";
        instance.setName(expResult);
        String result = instance.getName();
        assertEquals(expResult, result);
    }

    /**
     * Test of setTimestamp/getTimestamp method, of class AudioFileItem.
     */
    @Test
    public void testTimestamp() {
        log("AudioFileItem.timestamp");
        AudioFileItem instance = new AudioFileItem();
        long expResult = System.currentTimeMillis();
        instance.setTimestamp(expResult);
        long result = instance.getTimestamp();
        assertEquals(expResult, result);
    }

    /**
     * Test of setEndMS/getEndMS method, of class AudioFileItem.
     */
    @Test
    public void testEndMS() {
        log("AudioFileItem.endMS");
        AudioFileItem instance = new AudioFileItem();
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
        log("AudioFileItem.startMS");
        AudioFileItem instance = new AudioFileItem();
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
        log("AudioFileItem.start");
        AudioFileItem instance = new AudioFileItem();
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
        log("AudioFileItem.end");
        AudioFileItem instance = new AudioFileItem();
        long currentTime = System.currentTimeMillis();
        long expResult = currentTime / 1000;
        instance.setEndMS(currentTime);
        long result = instance.getEnd();
        assertEquals(expResult, result);
    }

    private static void log(Object string) {
        LogUtil.log(AudioFileItemTest.class,string);
    }


}
