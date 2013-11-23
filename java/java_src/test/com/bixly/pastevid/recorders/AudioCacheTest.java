/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.recorders;

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
public class AudioCacheTest {
    
    
    private final String RECORDER_PANEL = "jpRecorderPanel";
    private final String SCRUB_MANAGER  = "scrubManager";
    private final String RECORDER       = "recorder";
    private final String AUDIO_RECORDER = "audioRecorder";
    private final String AUDIO_CACHE    = "audioCache";
    
    public AudioCacheTest() {
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
     * Test of getCurrOffset method, of class AudioCache.
     */
    @Test
    public void testGetCurrOffset() {
        System.out.println("getCurrOffset");
        AudioCache instance = new AudioCache();
        long expResult = 0L;
        long result = instance.getCurrOffset();
        assertEquals(expResult, result);
    }

    /**
     * Test of getCurrTimeMS method, of class AudioCache.
     */
    @Test
    public void testGetCurrTimeMS() {
        System.out.println("getCurrTimeMS");
        AudioCache instance = new AudioCache();
        long expResult = 0L;
        long result = instance.getCurrTimeMS();
        assertEquals(expResult, result);
    }

    /**
     * Test of getCurrTimeSeconds method, of class AudioCache.
     */
    @Test
    public void testGetCurrTimeSeconds() {
        System.out.println("getCurrTimeSeconds");
        AudioCache instance = new AudioCache();
        int expResult = 0;
        int result = instance.getCurrTimeSeconds();
        assertEquals(expResult, result);
    }

    /**
     * Test of isPlaying method, of class AudioCache.
     */
    @Test
    public void testIsPlaying() {
        System.out.println("isPlaying");
        AudioCache instance = new AudioCache();
        boolean expResult = false;
        boolean result = instance.isPlaying();
        assertEquals(expResult, result);
    }

}
