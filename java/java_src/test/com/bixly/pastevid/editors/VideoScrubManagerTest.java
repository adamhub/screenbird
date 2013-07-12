/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.editors;

import com.bixly.pastevid.Session;
import com.bixly.pastevid.TestSettings;
import org.junit.Test;
import com.bixly.pastevid.screencap.ScreenRecorder;
import com.bixly.pastevid.util.LogUtil;
import com.bixly.pastevid.screencap.RecorderPanel;
import com.bixly.pastevid.screencap.components.preview.PreviewPlayer;
import com.bixly.pastevid.screencap.components.preview.PreviewPlayerForm;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import static org.junit.Assert.*;

/**
 *
 * @author cevaris
 */
public class VideoScrubManagerTest {
    
    private boolean recoveryMode = false;
    
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
        LogUtil.log(VideoScrubManagerTest.class,string);
    }
    
    
    /**
     * Generates a temporary User model instance for JUnit testing
     * Only should be used for JUnit Tests!!!
     * @return 
     */
    private void generateTempUser(){
        Session.getInstance().user.setBaseURL("http://staging.screenbird.com/");
        Session.getInstance().user.setAnonToken("ZXCVBNMASDFGHJKLQWERTYUIOP");
        Session.getInstance().user.setCsrfToken("QWERTYUIOPASDFGHJKLZXCVBNM");
        Session.getInstance().user.setUserId("10");
    }

    /**
     * Test of setPreviewPlayerFrame method, of class VideoScrubManager.
     */
    @Ignore
    public void testSetPreviewPlayerFrame() {
        System.out.println("setPreviewPlayerFrame");
        PreviewPlayerForm jfPreviewPlayer = null;
        VideoScrubManager instance = new VideoScrubManager();        
        fail("The test case is a prototype.");
    }

    /**
//     * Test of setPreviewControlPanel method, of class VideoScrubManager.
//     */
//    @Ignore
//    public void testSetPreviewControlPanel() {
//        System.out.println("setPreviewControlPanel");
//        PreviewControllerPanel jpPreviewControl = null;
//        VideoScrubManager instance = new VideoScrubManager();
//        instance.setPreviewControlPanel(jpPreviewControl);
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of setPreviewPlayerPanel method, of class VideoScrubManager.
     */
    @Ignore
    public void testSetPreviewPlayerPanel() {
        System.out.println("setPreviewPlayerPanel");
        PreviewPlayer jpPreviewPlayer = null;
        VideoScrubManager instance = new VideoScrubManager();
        
        
        fail("The test case is a prototype.");
    }

    /**
     * Test of setRecorderPanel method, of class VideoScrubManager.
     */
    @Ignore
    public void testSetRecorderPanel() {
        System.out.println("setRecorderPanel");
        RecorderPanel jpRecorderPanel = null;
        VideoScrubManager instance = new VideoScrubManager();
        instance.setRecorderPanel(jpRecorderPanel);
        
        fail("The test case is a prototype.");
    }

    /**
     * Test of previewSliderAction method, of class VideoScrubManager.
     */
    @Ignore
    public void testPreviewSliderAction() {
        System.out.println("previewSliderAction");
        int value = 0;
        VideoScrubManager instance = new VideoScrubManager();
        
        
        fail("The test case is a prototype.");
    }

    /**
     * Test of updatePrevewController method, of class VideoScrubManager.
     */
    @Ignore
    public void testUpdatePrevewController() {
        System.out.println("updatePrevewController");
        long timeMS = 0L;
        VideoScrubManager instance = new VideoScrubManager();
        instance.updatePreviewController(timeMS);
        
        fail("The test case is a prototype.");
    }

    /**
     * Test of resetControls method, of class VideoScrubManager.
     */
    @Ignore
    public void testResetControls() {
        System.out.println("resetControls");
        VideoScrubManager instance = new VideoScrubManager();
        instance.resetControls();
        
        fail("The test case is a prototype.");
    }

    /**
     * Test of getClockTime method, of class VideoScrubManager.
     */
    @Ignore
    public void testGetClockTime_int() {
        System.out.println("getClockTime");
        int time = 0;
        VideoScrubManager instance = new VideoScrubManager();
        String expResult = "";
        String result = instance.getClockTime(time, false);
        assertEquals(expResult, result);
        
        fail("The test case is a prototype.");
    }

    /**
     * Test of getClockTime method, of class VideoScrubManager.
     */
    @Ignore
    public void testGetClockTime_long() {
        System.out.println("getClockTime");
        long timeMS = 0L;
        VideoScrubManager instance = new VideoScrubManager();
        String expResult = "";
        String result = instance.getClockTimeMS(timeMS, false);
        assertEquals(expResult, result);
        
        fail("The test case is a prototype.");
    }

    /**
     * Test of addCut method, of class VideoScrubManager.
     */
    @Ignore
    public void testAddCut() {
        System.out.println("addCut");
        int start = 0;
        int end = 0;
        VideoScrubManager instance = new VideoScrubManager();
        instance.addCut(5,10);
        
        fail("The test case is a prototype.");
    }

    /**
     * Test of scrubVideo method, of class VideoScrubManager.
     */
    @Ignore
    public void testScrubVideo() {
        System.out.println("scrubVideo");
        String rawVideoPath = "";
        VideoScrubManager instance = new VideoScrubManager();
        instance.scrubVideo(rawVideoPath,null);
        
        fail("The test case is a prototype.");
    }

    /**
     * Test of destroyPreviewVideo method, of class VideoScrubManager.
     */
    @Ignore
    public void testDestroyPreviewVideo() {
        System.out.println("destroyPreviewVideo");
        VideoScrubManager instance = new VideoScrubManager();
        instance.destroyPreviewVideo();
        
        fail("The test case is a prototype.");
    }

    /**
     * Test of endPreviewVideo method, of class VideoScrubManager.
     */
    @Ignore
    public void testEndPreviewVideo() {
        System.out.println("endPreviewVideo");
        VideoScrubManager instance = new VideoScrubManager();
        instance.endPreviewVideo();
        
        fail("The test case is a prototype.");
    }
    
    /**
     * Test of endPreviewVideo method, of class VideoScrubManager.
     */
    @Ignore
    public void testScrubComplexSubsetTestAlgorithm() {
        System.out.println("scrubComplexSubsetTest");
        VideoScrubManager instance = new VideoScrubManager();
        ScreenRecorder screenRecorder  = new ScreenRecorder(recoveryMode);
        instance.setRecorderPanel(screenRecorder.getPanel());
        
        ArrayList<VideoScrub> expResult = new ArrayList<VideoScrub>(
                Arrays.asList(
                new VideoScrub(5,  10),
                new VideoScrub(15, 30),
                new VideoScrub(45, 80))
        );
        
        instance.addCut(5, 10);
        instance.addCut(15, 30);
        instance.addCut(45, 50);
        instance.addCut(70, 75);
        instance.addCut(45, 80);
        
        assertEquals(expResult,instance.getScrubs());
        
    }
    
    /**
     * Test of endPreviewVideo method, of class VideoScrubManager.
     */
    @Ignore
    public void testScrubComplexSubsetTestAlgorithmMerge() {
        System.out.println("scrubComplexSubsetTestAlgorithmMerge");
        generateTempUser();
//        ScreenRecorder screenRecorder  = new ScreenRecorder();
        VideoScrubManager instance = new VideoScrubManager();
   
        //[(5,10), (15,21), (21,26), (26,41), (27,52)]
        ArrayList<VideoScrub> expResult = new ArrayList<VideoScrub>(
                Arrays.asList(
                new VideoScrub(5,10),
                new VideoScrub(15,41)
                )
        );
        
        instance.updateScrubMaps();
        
        instance.addCut(5, 10);
        instance.addCut(15,21);
        instance.addCut(21,26);
        instance.addCut(26,41);
        
//        ArrayList<VideoScrub> expResult = new ArrayList<VideoScrub>(
//                Arrays.asList(
//                new VideoScrub(5,10),
//                new VideoScrub(15,21),
//                new VideoScrub(21,26),
//                new VideoScrub(26,41),
//                new VideoScrub(27,52),
//                new VideoScrub(45, 80))
//        );
//        
//        instance.updateScrubMaps();
//        
//        instance.addCut(5, 10);
//        instance.addCut(15, 41);
//        instance.addCut(45, 80);
        
        System.out.println("Expected " + expResult);
        System.out.println("Result   " + instance.getScrubs());
        
        assertEquals(expResult,instance.getScrubs());
        
    }
   
    /**
     * Test of endPreviewVideo method, of class VideoScrubManager.
     */
    @Ignore
    public void testScrubComplexSubsetTestAlgorithmMapping() {
        System.out.println("scrubComplexSubsetTestAlgorithmMapping");
        generateTempUser();
        VideoScrubManager instance = new VideoScrubManager();
        
        //[(5,10), (15,21), (21,26), (26,41), (27,52)]
        ArrayList<VideoScrub> expResult = new ArrayList<VideoScrub>(
                Arrays.asList(
                new VideoScrub(5,10),
                new VideoScrub(15,31),
                new VideoScrub(36,40))
        );
        
        instance.updateScrubMaps();
        
        instance.addCut(5, 10);
        instance.addCut(15,21);
        instance.addCut(21,26);
        instance.addCut(26,31);
        instance.addCut(36,40);
        
        System.out.println("Expected " + expResult);
        System.out.println("Result   " + instance.getScrubs());
        
        assertEquals(expResult,instance.getScrubs());
        
    }

    /**
     * Test of endPreviewVideo method, of class VideoScrubManager.
     */
    @Test
    public void testScrubComplexSubsetTestAlgorithmMappingB() {
        System.out.println("scrubComplexSubsetTestAlgorithmMappingB");
        generateTempUser();
        VideoScrubManager instance = new VideoScrubManager();
        ScreenRecorder screenRecorder  = new ScreenRecorder(recoveryMode);
        instance.setRecorderPanel(screenRecorder.getPanel());
   
        ArrayList<VideoScrub> expResult = new ArrayList<VideoScrub>(
                Arrays.asList(
                new VideoScrub(5, 10),
                new VideoScrub(15,20),
                new VideoScrub(25,35),
                new VideoScrub(45,65),
                new VideoScrub(70,75))
        );
        
        instance.updateScrubMaps();
        
        instance.addCut(5, 10);
        instance.addCut(15,20);
        instance.addCut(25,35);
        instance.addCut(45,65);
        instance.addCut(70,75);
        
        System.out.println("Expected " + expResult);
        System.out.println("Result   " + instance.getScrubs());
        
        assertEquals(expResult,instance.getScrubs());
        
    }

    /**
     * Test of endPreviewVideo method, of class VideoScrubManager.
     */
    @Ignore
    public void testScrubMaps() {
        System.out.println("scrubScrubMaps");
        generateTempUser();
        ScreenRecorder screenRecorder  = new ScreenRecorder(recoveryMode);
        VideoScrubManager instance = new VideoScrubManager();
        
        instance.addCut(5, 10); //5
        instance.addCut(20, 25);
        
        assertEquals(true,true);
        
    }
    
    /**
     * Test of endPreviewVideo method, of class VideoScrubManager.
     */
    @Ignore
    public void testScrubComplexSubsetTestTwoAlgorithm() {
        System.out.println("scrubComplexSubsetTestTwo");
        VideoScrubManager instance = new VideoScrubManager();
        
        ArrayList<VideoScrub> expResult = new ArrayList<VideoScrub>(
                Arrays.asList(
                new VideoScrub(5,  10),
                new VideoScrub(15, 30),
                new VideoScrub(45, 100)
        ));
        
        instance.addCut(5, 10);
        instance.addCut(15, 30);
        instance.addCut(45, 80);
        instance.addCut(70, 100);
        
        assertEquals(expResult,instance.getScrubs());
        
    }
    
    
    /**
     * Test of endPreviewVideo method, of class VideoScrubManager.
     */
    @Ignore
    public void testScrubSortByStart() {
        System.out.println("scrubSortByStart");
        
        
        ArrayList<VideoScrub> instance;
        ArrayList<VideoScrub> expResult;
        
        //======================================================================
        //Test case 1
        instance  = new ArrayList<VideoScrub>();
        expResult = new ArrayList<VideoScrub>(Arrays.asList(
                new VideoScrub(5, 10),
                new VideoScrub(15,20),
                new VideoScrub(30,40)
        ));
        
        instance.add(new VideoScrub(5, 10));
        instance.add(new VideoScrub(15, 20));
        instance.add(new VideoScrub(30, 40));
        
        System.out.println("Are Equal");
        System.out.println(instance);
        System.out.println(expResult);
        
        assertTrue(instance.equals(expResult));
        
        //======================================================================
        //Test case 2
        instance  = new ArrayList<VideoScrub>();
        expResult = new ArrayList<VideoScrub>(Arrays.asList(
                new VideoScrub(5, 10),
                new VideoScrub(15,25),
                new VideoScrub(30,40)
        ));
        
        instance.add(new VideoScrub(5, 10));
        instance.add(new VideoScrub(15, 20));
        instance.add(new VideoScrub(30, 40));
        
        System.out.println("Are Not Equal");
        System.out.println(instance);
        System.out.println(expResult);
        
        assertFalse(instance.equals(expResult));
        
        //======================================================================
        //Test case 3
        
        
        instance  = new ArrayList<VideoScrub>();
        instance.add(new VideoScrub(5, 10));
        instance.add(new VideoScrub(30, 40));
        instance.add(new VideoScrub(15, 20));
        
        expResult = new ArrayList<VideoScrub>(Arrays.asList(
                new VideoScrub(5, 10),
                new VideoScrub(15,25),
                new VideoScrub(30,40)
        ));
        
        
        System.out.println("Are Sorted by StartTime - Before");
        System.out.println(instance);
        
        Collections.sort(instance, VideoScrubManager.sortByStartComparator);
        
        System.out.println("Are Sorted by StartTime - After");
        System.out.println(instance);
        
        assertFalse(instance.equals(expResult));
        
         //======================================================================
        //Test case 3
        
        
        instance  = new ArrayList<VideoScrub>();
        instance.add(new VideoScrub(5, 10));
        instance.add(new VideoScrub(30, 40));
        instance.add(new VideoScrub(15, 20));
        
        expResult = new ArrayList<VideoScrub>(Arrays.asList(
                new VideoScrub(5, 10),
                new VideoScrub(15,25),
                new VideoScrub(30,40)
        ));
        
        
        System.out.println("Are Sorted by StartTime - Before");
        System.out.println(instance);
        
        Collections.sort(instance, VideoScrubManager.sortByStartComparator);
        
        System.out.println("Are Sorted by StartTime - After");
        System.out.println(instance);
        
        assertFalse(instance.equals(expResult));
        
        //======================================================================
        //Test case 4 Super Uranium Compound Mixture Complex Potent Scrub
        
        
        instance  = new ArrayList<VideoScrub>();
        instance.add(new VideoScrub(5, 10));
        instance.add(new VideoScrub(30, 40));
        instance.add(new VideoScrub(15, 20));
        
        expResult = new ArrayList<VideoScrub>(Arrays.asList(
                new VideoScrub(5, 10),
                new VideoScrub(15,25),
                new VideoScrub(30,40)
        ));
        
        
        System.out.println("Are Sorted by StartTime - Before");
        System.out.println(instance);
        
        Collections.sort(instance, VideoScrubManager.sortByStartComparator);
        
        System.out.println("Are Sorted by StartTime - After");
        System.out.println(instance);
        
        assertFalse(instance.equals(expResult));

        
    }

    /**
     * Test of playAudio method, of class AudioCache.
     */
//    @Ignore
//    public void testPlayAudioFromBeginning() {
//        System.out.println("playAudioFromBeginning");
//        
//        ScreenRecorder screenRecorder  = new ScreenRecorder();
//        RecorderPanel instance         = screenRecorder.getRecorderPanel();
//        VideoScrubManager scrubManager = instance.getScrubManager();
//        
//        final int TEST_TIME = 10;
//        //Start recording
//        instance.recordOrPause();
//        //Offset time
//        TimeUtil.skipToMyLou(5);
//        //Wait and record some screen
//        TimeUtil.skipToMyLou(TEST_TIME);
//        
//        //Puase video
//        instance.recordOrPause();
//        
//        scrubManager.setPreviewTime(0);
//        //Start Preview
//        scrubManager.startPreviewVideo();
//        
//        TimeUtil.skipToMyLou(TEST_TIME+1);
//        
//        scrubManager.endPreviewVideo();
//        
//        System.out.println("Done");
//    }
    
    /**
     * Test of startPreviewVideo method, of class VideoScrubManager.
     */
    //@Test
//    @Ignore
//    public void testPausePreviewVideo() {
//        System.out.println("pausePreviewVideo");
//        Session.getInsance() = generateTempUser();
//        ScreenRecorder screenRecorder  = new ScreenRecorder();
//        RecorderPanel instance         = screenRecorder.getRecorderPanel();
//        VideoScrubManager scrubManager = instance.getScrubManager();
//        
//        //Start recording
//        instance.recordOrPause();
//        
//        //Wait and record
//        TimeUtil.skipToMyLou(25);
//        
//        //Pause video
//        instance.recordOrPause();
//        
//        //Move preview track to a specific time
//        scrubManager.setPreviewTime(2);
//        //Display video preview
//        scrubManager.startPreviewVideo();
//        //Let preview video finish
//        TimeUtil.skipToMyLou(6);
//        //Pause Preview
//        scrubManager.startPreviewVideo();
//        //scrubManager.togglePlayPause();
//        //Pause for a bit
//        TimeUtil.skipToMyLou(5);
//        //Play video
//        //scrubManager.togglePlayPause();
//        scrubManager.endPreviewVideo();
//        
//        TimeUtil.skipToMyLou(20);
//        
//        //Delete all file but resulting video
//        instance.getRecorder().deleteFiles();
//    }
    /**
     * Test of startPreviewVideo method, of class VideoScrubManager.
     */
//    @Ignore
//    public void testTwoPreviewVideo() {
//        System.out.println("twoPreviewVideo");
//        Session.getInsance() = generateTempUser();
//        ScreenRecorder screenRecorder  = new ScreenRecorder();
//        RecorderPanel instance         = screenRecorder.getRecorderPanel();
//        VideoScrubManager scrubManager = instance.getScrubManager();
//        
//        //Start recording
//        instance.recordOrPause();
//        
//        //Wait and record
//        TimeUtil.skipToMyLou(15);
//        
//        //Pause video
//        instance.recordOrPause();
//        
//        //Move preview track to a specific time
//        scrubManager.setPreviewTime(5);
//        //Display video preview
//        scrubManager.startPreviewVideo();
//        //Let preview video finish
//        TimeUtil.skipToMyLou(6);
//        
//        
//        //Move preview track to a specific time
//        scrubManager.setPreviewTime(2);
//        //Display video preview
//        scrubManager.startPreviewVideo();
//        //Let preview video finish
//        TimeUtil.skipToMyLou(5);
//        
//        //Stop Preview
//        scrubManager.endPreviewVideo();
//        
//        //Finish video recording
//        instance.processVideo();
//        //Wait for video to finish encoding
//        TimeUtil.skipToMyLou(20);
//        //Delete all file but resulting video
//        instance.getRecorder().deleteFiles();
//    }
    
    
//    /**
//     * Test of multiple scrub/edit of video
//     */
//    @Ignore
//    public void testScrubVideoSingleScrub() {
//        log("ScrubVideoSingleScrub");
//        Session.getInsance() = generateTempUser();
//        ScreenRecorder screenRecorder = new ScreenRecorder();
//        RecorderPanel instance        = screenRecorder.getRecorderPanel();
//        VideoScrubManager scrubManager  = instance.getScrubManager();
//        
//        final int TEST_TIME = 15;
//        
//        //Start recording
//        instance.recordOrPause();
//        //Wait and record some screen
//        TimeUtil.skipToMyLou(TEST_TIME+5);
//        
//        //Pause and bring up editor
//        instance.recordOrPause();
//        //Seek to cut time
//        scrubManager.setPreviewTime(TEST_TIME/2);
//        //Perform video scrub
//        //scrubManager.addCut(TEST_TIME/2, TEST_TIME);
//        //scrubManager.addCut(TEST_TIME/2,);
//        //Start recording
//        scrubManager.postScrubStartRecording();
//        
//        
//        //Offset clock
//        //instance.setClockOffset(20-30);
//        //Wait and record some screen
//        TimeUtil.skipToMyLou(10);
//        
//        //Finish recording
//        instance.processVideo();
//        
//        //Wait for video to compile/encode
//        while(instance.isProcessingVideo()) {
//            TimeUtil.skipToMyLou(1);
//        }
//        
//        //Start video editing
//        //scrubManager.scrubVideo(instance.getOutputZip());
//        //Wait for video editing
//        while(scrubManager.isScrubbing()) {
//            TimeUtil.skipToMyLou(1);
//        }
//        
//        System.out.println("Done");
//        
//    }
    
    
    /**
     * Test of multiple scrub/edit of video
     */
//    @Ignore
//    public void testScrubVideoTwoScrub() {
//        log("ScrubVideoTwoScrub");
//        Session.getInsance() = generateTempUser();
//        ScreenRecorder screenRecorder = new ScreenRecorder();
//        RecorderPanel instance        = screenRecorder.getRecorderPanel();
//        VideoScrubManager scrubManager  = instance.getScrubManager();
//        
//        //Start recording
//        instance.recordOrPause();
//        //Wait and record some screen
//        TimeUtil.skipToMyLou(35);
//        //Perform video scrub
//        scrubManager.addCut(20, 30);
//        //Offset clock
//        //instance.setClockOffset((long) 20-30);
//        //Wait and record some screen
//        TimeUtil.skipToMyLou(20);
//        
//        //Perform 2nd video scrub
//        scrubManager.addCut(40, 50);
//        //Offset clock
//        //instance.setClockOffset((long) 40-50);
//        //Wait and record some screen
//        TimeUtil.skipToMyLou(30);
//        
//        //Finish recording
//        instance.processVideo();
//        
//        //Wait for video to compile/encode
//        while(instance.isEncodingVideo()) {
//            TimeUtil.skipToMyLou(1);
//        }
//        
//        //Start video editing
//        //scrubManager.scrubVideo(instance.getOutputZip());
//        //Wait for video editing
//        while(scrubManager.isScrubbing()) {
//            TimeUtil.skipToMyLou(1);
//        }
//        
//    }
    
//    /**
//     * Test of multiple scrub/edit of video
//     */
//    @Test
//    public void testScrubVideoTwoScrubWatch() {
//        log("ScrubVideoTwoScrubWatch");
//        Session.getInsance() = generateTempUser();
//        ScreenRecorder screenRecorder = new ScreenRecorder();
//        RecorderPanel instance        = screenRecorder.getRecorderPanel();
//        VideoScrubManager scrubManager  = instance.getScrubManager();
//        
//        //Start recording
//        instance.recordOrPause();
//        //Wait and record some screen
//        TimeUtil.skipToMyLou(15);
//        instance.recordOrPause();
//        scrubManager.setPreviewTime(5);
//        scrubManager.addCut();
//        scrubManager.backToRecordPanel();
//        scrubManager.postScrubStartRecording();
//        //Wait and record some screen
//        TimeUtil.skipToMyLou(20);
//        instance.recordOrPause();
//        scrubManager.setPreviewTime(20);
//        scrubManager.addCut();
//        scrubManager.backToRecordPanel();
//        scrubManager.postScrubStartRecording();
//        //Offset clock
//        //instance.setClockOffset((long) 40-50);
//        //Wait and record some screen
//        TimeUtil.skipToMyLou(10);
//        instance.recordOrPause();
//        scrubManager.setPreviewTime(0);
//        scrubManager.startPreviewVideo();
//        
//        //Watch video
//        TimeUtil.skipToMyLou(25);
//        scrubManager.endPreviewVideo();
//        
//        //Finish recording
//        instance.processVideo();
//        
//        //Wait for video to compile/encode
//        while(instance.isProcessingVideo()) {
//            TimeUtil.skipToMyLou(50);
//        }
//        
//        //Start video editing
////        scrubManager.scrubVideo(instance.getOutputZip());
//        //Wait for video editing
//        while(scrubManager.isScrubbing()) {
//            TimeUtil.skipToMyLou(50);
//        }
//        
//    }
    
    /**
     * Test of multiple scrub/edit of video
     */
//    @Ignore
//    public void testScrubVideoOverlap() {
//        log("scrubVideoOverlap");
//        Session.getInsance() = generateTempUser();
//        ScreenRecorder screenRecorder  = new ScreenRecorder();
//        RecorderPanel instance         = screenRecorder.getRecorderPanel();
//        VideoScrubManager scrubManager = instance.getScrubManager();
//        
//        //Start recording
//        instance.recordOrPause();
//        //Offset time
//        TimeUtil.skipToMyLou(5);
//        //Wait and record some screen
//        TimeUtil.skipToMyLou(15);
//        //Perform video scrub
//        scrubManager.addCut(10, 15);
//        System.out.println(scrubManager.getScrubs());
//        //Offset clock
//        //instance.setClockOffset((long) 10-15);
//        
//        //Wait and record some screen
//        TimeUtil.skipToMyLou(15);
//
//        //Perform 2nd video scrub
//        scrubManager.addCut(5,30);
//        System.out.println(scrubManager.getScrubs());
//        //Offset clock
//        //instance.setClockOffset((long) 5-25);
//        //Wait and record some screen
//        TimeUtil.skipToMyLou(10);
//        
//        //Finish recording
//        instance.processVideo();
//        
//        //Wait for video to compile/encode
//        while(instance.isEncodingVideo()) {
//            TimeUtil.skipToMyLou(1);
//        }
//        
//        //Start video editing
////        scrubManager.scrubVideo(instance.getOutputZip());
//        //Wait for video editing
//        while(scrubManager.isScrubbing()) {
//            TimeUtil.skipToMyLou(1);
//        }
//        
//    }
    
}
