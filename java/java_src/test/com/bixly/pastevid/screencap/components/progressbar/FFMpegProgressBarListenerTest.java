/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.screencap.components.progressbar;

import javax.swing.JProgressBar;
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
public class FFMpegProgressBarListenerTest {
    
    private static JProgressBar jprogressbar;
    
    public FFMpegProgressBarListenerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        jprogressbar = new JProgressBar();
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
     * Test of getWorkingTask method, of class FFMpegProgressBarListener.
     */
    @Test
    public void testGetWorkingTask() {
        System.out.println("getWorkingTask");
        FFMpegProgressBarListener instance = new FFMpegProgressBarListener(jprogressbar, 1);
        assertNotNull(instance.getWorkingTask());
    }

    /**
     * Test of setProgressByDelta method, of class FFMpegProgressBarListener.
     */
    @Test
    public void testSetProgressByDelta() {
        System.out.println("setProgressByDelta");
        FFMpegProgressBarListener instance = new FFMpegProgressBarListener(jprogressbar, 1);
        instance.setProgressByDelta(10);
        assertEquals(instance.getProgress(), 10);
        instance.setProgressByDelta(20);
        assertEquals(instance.getProgress(), 30);
        instance.setProgressByDelta(20);
        assertEquals(instance.getProgress(), 50);
        instance.setProgressByDelta(50);
        assertEquals(instance.getProgress(), 100);
        
    }

    /**
     * Test of setProgress method, of class FFMpegProgressBarListener.
     */
    @Test
    public void testSetProgress() {
        System.out.println("setProgress");
        double progressValue = 0.12;
        FFMpegProgressBarListener instance = new FFMpegProgressBarListener(jprogressbar, 1);
        instance.setProgress(progressValue);
        assertEquals((int)(progressValue*100), instance.getProgress());
        
    }

    /**
     * Test of updateProgressBar method, of class FFMpegProgressBarListener.
     */
    @Test
    public void testUpdateProgressBar() {
        System.out.println("updateProgressBar");
        FFMpegProgressBarListener instance = new FFMpegProgressBarListener(jprogressbar, 1);
        double progressValue = 0.12;
        instance.updateProgressBar();
        instance.setProgress(progressValue);
        assertEquals((int)(progressValue*100), jprogressbar.getValue());
    }

    /**
     * Test of getProgress method, of class FFMpegProgressBarListener.
     */
    @Test
    public void testGetProgress() {
        System.out.println("getProgress");
        double progressValue = 0.12;
        FFMpegProgressBarListener instance = new FFMpegProgressBarListener(jprogressbar, 1);
        instance.setProgress(progressValue);
        assertEquals((int)(progressValue*100), instance.getProgress());
    }

    /**
     * Test of getCurrentTaskNumber method, of class FFMpegProgressBarListener.
     */
    @Test
    public void testGetCurrentTaskNumber() {
        System.out.println("getCurrentTaskNumber");
        FFMpegProgressBarListener instance = new FFMpegProgressBarListener(jprogressbar, 1);
        int expResult = 0;
        int result = instance.getCurrentTaskNumber();
        assertEquals(expResult, result);
    }

    /**
     * Test of setId method, of class FFMpegProgressBarListener.
     */
    @Test
    public void testSetId() {
        System.out.println("setId");
        int programId = FFMpegProgressBarListener.FFMPEG;
        FFMpegProgressBarListener instance = new FFMpegProgressBarListener(jprogressbar, 1);
        instance.setId(programId);
        assertEquals(programId, instance.getId());
        
        programId = FFMpegProgressBarListener.HANDBRAKE;
        instance.setId(programId);
        assertEquals(programId, instance.getId());
    }

    /**
     * Test of getId method, of class FFMpegProgressBarListener.
     */
    @Test
    public void testGetId() {
        System.out.println("getId");
        int programId = FFMpegProgressBarListener.FFMPEG;
        FFMpegProgressBarListener instance = new FFMpegProgressBarListener(jprogressbar, 1);
        instance.setId(programId);
        assertEquals(programId, instance.getId());
        
    }

    /**
     * Test of reset method, of class FFMpegProgressBarListener.
     */
    @Test
    public void testReset() {
        System.out.println("reset");

        FFMpegProgressBarListener instance = new FFMpegProgressBarListener(jprogressbar, 1);
        instance.setProgressByDelta(10);
        assertEquals(instance.getProgress(), 10);
        instance.setProgressByDelta(20);
        assertEquals(instance.getProgress(), 30);
        instance.setProgressByDelta(20);
        assertEquals(instance.getProgress(), 50);
        
        instance.reset();
        
        assertEquals(instance.getProgress(), 0);
        assertEquals(instance.getCurrentTaskNumber(), 0);
        
        
        
    }

    /**
     * Test of parseTimeInfoHandbrake method, of class FFMpegProgressBarListener.
     */
    @Test
    public void testParseTimeInfoHandbrake() {
        System.out.println("parseTimeInfoHandbrake");
        String line = "THIS IS FAKE , 12.23 % I LIKE CHOCOLATE";
        FFMpegProgressBarListener instance = new FFMpegProgressBarListener(jprogressbar, 1);
        instance.parseTimeInfoHandbrake(line);
        
        assertEquals(instance.getProgress(), 12);
        
    }
}
