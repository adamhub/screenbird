/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.screencap.components.preview;

import com.bixly.pastevid.Settings;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import com.bixly.pastevid.editors.VideoScrubManager;
import com.bixly.pastevid.util.TimeUtil;
import java.awt.image.BufferedImage;
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
public class PreviewPlayerTest {
    
    private static PreviewPlayerForm jfPreviewPlayer;
    private static VideoScrubManager scrubManager;
    private static PreviewPlayer instance;
    
    public PreviewPlayerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        scrubManager = new VideoScrubManager();
        jfPreviewPlayer = new PreviewPlayerForm(scrubManager);
        instance = jfPreviewPlayer.getPlayerPanel();
        
        jfPreviewPlayer.showPlayer();
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
     * Generates screenshot for JUnit testing
     * @return 
     *      File pointer to screen shot
     */
    private BufferedImage generateTestImage(Rectangle captureArea){
        Robot awtRobot;
        
        if(captureArea == null){
            //Get full screen if no defined area of screen capture is defined
            captureArea = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        }
        
        try {
            awtRobot = new Robot();
            return awtRobot.createScreenCapture(captureArea);

        }catch (AWTException e){
            System.err.println(e);
        }
        
        return null;
    }

    /**
     * Generates screenshot for JUnit testing
     * @return 
     *      File pointer to screen shot
     */
    private File generateTestImageFile(Rectangle captureArea){
        Robot awtRobot;
        String currentCaptureDir = Settings.SCREEN_CAPTURE_DIR;
        
        
        if(captureArea == null){
            //Get full screen if no defined area of screen capture is defined
            captureArea = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        }
        
        try {
            BufferedImage bufferedImage = generateTestImage(captureArea);

            Iterator iter = ImageIO.getImageWritersByFormatName("jpeg");
            ImageWriter writer = (ImageWriter)iter.next();
            ImageWriteParam iwp = writer.getDefaultWriteParam();
            iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            iwp.setCompressionQuality(1.0F); 
            
            File imageFile = new File(currentCaptureDir, "test.jpeg");
            FileImageOutputStream stream = new FileImageOutputStream(imageFile);
            //Set up file access
            writer.setOutput(stream);
            //Create image
            IIOImage image = new IIOImage(bufferedImage, null, null);
            //write image
            writer.write(null, image, iwp);
            //Close image stream
            stream.close();
            
            return imageFile;

        }catch (IOException e){
            System.err.println(e);
        }
        
        return null;
    }

    /**
     * Test of setScreenshot method, of class PreviewPlayer.
     */
    @Test
    public void testSetScreenshot_BufferedImage() {
        System.out.println("setScreenshot");
        BufferedImage screenshot = generateTestImage(null);
        instance.setScreenshot(screenshot);
        instance.setScreenshot(screenshot);
        instance.updateRecordNowButtonPosition();
        TimeUtil.skipToMyLou(2);
        
        screenshot = generateTestImage(new Rectangle(0, 0, 600, 400));
        instance.setScreenshot(screenshot);
        instance.setScreenshot(screenshot);
        instance.updateRecordNowButtonPosition();
        TimeUtil.skipToMyLou(2);
        
        screenshot = generateTestImage(new Rectangle(100, 100, 600, 400));
        instance.setScreenshot(screenshot);
        instance.setScreenshot(screenshot);
        instance.updateRecordNowButtonPosition();
        TimeUtil.skipToMyLou(2);
        
        screenshot = generateTestImage(null);
        instance.setScreenshot(screenshot);
        instance.setScreenshot(screenshot);
        instance.updateRecordNowButtonPosition();
        TimeUtil.skipToMyLou(2);
    }

    /**
     * Test of setSpinnerVisible method, of class PreviewPlayer.
     */
    @Test
    public void testSetSpinnerVisible() {
        System.out.println("setSpinnerVisible");
        instance.updateRecordNowButtonPosition();
        
        
        boolean isVisible = false;
        instance.setSpinnerVisible(isVisible);
        TimeUtil.skipToMyLou(2);
        
        isVisible = true;
        instance.setSpinnerVisible(isVisible);
        TimeUtil.skipToMyLou(2);
        
        isVisible = false;
        instance.setSpinnerVisible(isVisible);
        TimeUtil.skipToMyLou(2);
        
        
    }

    /**
     * Test of setToPlay method, of class PreviewPlayer.
     */
    @Test
    public void testSetToPlay() {
        System.out.println("setToPlay");
        
        instance.setToPlay();

        instance.setSliderMax(100);
        instance.setSliderValue(40);
        instance.setLabelTextTimeCurrent("00:40");
        instance.setLabelTextTimeRemaining("01:00");
        instance.updateRecordNowButtonPosition();
        instance.repaint();
        TimeUtil.skipToMyLou(2);
        
        instance.setToPlay();
        TimeUtil.skipToMyLou(2);
        
        
    }

    /**
     * Test of getSliderValue method, of class PreviewPlayer.
     */
    @Test
    public void testGetSliderValue() {
        System.out.println("getSliderValue");
        Integer expResult = 10;
        Integer result;
        
        instance.setSliderMax(50);
        instance.setSliderValue(10);
        instance.updateRecordNowButtonPosition();
        TimeUtil.skipToMyLou(2);
        
        result = instance.getSliderValue();
        
        assertEquals(expResult, result);
        
        instance.setSliderValue(0);
        
        
    }

    /**
     * Test of setLabelTextTimeCurrent method, of class PreviewPlayer.
     */
    @Test
    public void testSetLabelTextTimeCurrent() {
        System.out.println("setLabelTextTimeCurrent");
        
        instance.setSliderMax(100);
        instance.setSliderValue(50);
        instance.updateRecordNowButtonPosition();
        
        String text = "00:01";
        instance.setLabelTextTimeCurrent(text);
        TimeUtil.skipToMyLou(1);
    
        text = "00:02";
        instance.setLabelTextTimeCurrent(text);
        TimeUtil.skipToMyLou(1);
        
        text = "00:03";
        instance.setLabelTextTimeCurrent(text);
        TimeUtil.skipToMyLou(1);
        
        text = "00:04";
        instance.setLabelTextTimeCurrent(text);
        TimeUtil.skipToMyLou(1);
        
        text = "00:05";
        instance.setLabelTextTimeCurrent(text);
        TimeUtil.skipToMyLou(1);
        
        text = "00:00";
        instance.setLabelTextTimeCurrent(text);
    
    }

    /**
     * Test of setLabelTextTimeRemaining method, of class PreviewPlayer.
     */
    @Test
    public void testSetLabelTextTimeRemaining() {
        System.out.println("setLabelTextTimeRemaining");
        
        instance.setSliderMax(100);
        instance.setSliderValue(50);
        instance.updateRecordNowButtonPosition();
        
        String text = "-00:01";
        instance.setLabelTextTimeRemaining(text);
        TimeUtil.skipToMyLou(1);
    
        text = "-00:02";
        instance.setLabelTextTimeRemaining(text);
        TimeUtil.skipToMyLou(1);
        
        text = "-00:03";
        instance.setLabelTextTimeRemaining(text);
        TimeUtil.skipToMyLou(1);
        
        text = "-00:04";
        instance.setLabelTextTimeRemaining(text);
        TimeUtil.skipToMyLou(1);
        
        text = "-00:05";
        instance.setLabelTextTimeRemaining(text);
        TimeUtil.skipToMyLou(1);
        
        text = "-00:00";
        instance.setLabelTextTimeRemaining(text);
    }

    /**
     * Test of getPreviewFrame method, of class PreviewPlayer.
     */
    @Test
    public void testGetPreviewFrame() {
        System.out.println("getPreviewFrame");
        assertNotNull(instance.getPreviewFrame());
    }

    /**
     * Test of setSliderMax method, of class PreviewPlayer.
     */
    @Test
    public void testSetSliderMax() {
        System.out.println("setSliderMax");
        Integer value = 50;
        instance.setSliderMax(value);
        instance.setLabelTextTimeRemaining("-00:50");
        instance.updateRecordNowButtonPosition();
        TimeUtil.skipToMyLou(2);
        
        value = 100;
        instance.setSliderMax(value);
        instance.setLabelTextTimeRemaining("-01:40");
        instance.updateRecordNowButtonPosition();
        TimeUtil.skipToMyLou(2);
        
        value = 200;
        instance.setSliderMax(value);
        instance.setLabelTextTimeRemaining("-03:20");
        instance.updateRecordNowButtonPosition();
        TimeUtil.skipToMyLou(2);       
        
        instance.setLabelTextTimeRemaining("-00:00");
        instance.setSliderMax(100);
        instance.setSliderValue(0);
        instance.updateRecordNowButtonPosition();
    }

    /**
     * Test of setSliderValue method, of class PreviewPlayer.
     */
    @Test
    public void testSetSliderValue() {
        System.out.println("setSliderValue");
        Integer value = 50;
        instance.setSliderMax(300);
        instance.setSliderValue(value);
        instance.setLabelTextTimeCurrent("00:50");
        instance.updateRecordNowButtonPosition();
        TimeUtil.skipToMyLou(2);
        
        value = 100;
        instance.setSliderMax(300);
        instance.setSliderValue(value);
        instance.setLabelTextTimeCurrent("01:40");
        instance.updateRecordNowButtonPosition();
        TimeUtil.skipToMyLou(2);
        
        value = 200;
        instance.setSliderMax(300);
        instance.setSliderValue(value);
        instance.setLabelTextTimeCurrent("03:20");
        instance.updateRecordNowButtonPosition();
        TimeUtil.skipToMyLou(2);  
        
        instance.setSliderMax(100);
        instance.setSliderValue(0);
        instance.setLabelTextTimeCurrent("00:00");
        instance.updateRecordNowButtonPosition();
    }

    /**
     * Test of disableEditing method, of class PreviewPlayer.
     */
    @Test
    public void testDisableEditing() {
        System.out.println("disableEditing");
        instance.disableEditing();
        TimeUtil.skipToMyLou(2);
        instance.enableEditing();
    }

    /**
     * Test of enableEditing method, of class PreviewPlayer.
     */
    @Test
    public void testEnableEditing() {
        System.out.println("enableEditing");
        instance.disableEditing();
        TimeUtil.skipToMyLou(2);
        instance.enableEditing();
    }

    /**
     * Test of updateRecordNowButtonPosition method, of class PreviewPlayer.
     */
    @Test
    public void testUpdateRecordNowButtonPosition() {
        System.out.println("updateRecordNowButtonPosition");
        
        
        instance.setSliderMax(50);
        instance.setSliderValue(10);
        instance.updateRecordNowButtonPosition();
        TimeUtil.skipToMyLou(2);
        
        instance.setSliderValue(0);
        instance.updateRecordNowButtonPosition();
    }

    /**
     * Test of hideRecordFromHerePanel method, of class PreviewPlayer.
     */
    @Test
    public void testHideRecordFromHerePanel() {
        System.out.println("hideRecordFromHerePanel");
        instance.hideRecordFromHerePanel();
        TimeUtil.skipToMyLou(2);
        instance.showRecordFromHerePanel();
    }

    /**
     * Test of showRecordFromHerePanel method, of class PreviewPlayer.
     */
    @Test
    public void testShowRecordFromHerePanel() {
        System.out.println("showRecordFromHerePanel");
        instance.hideRecordFromHerePanel();
        TimeUtil.skipToMyLou(2);
        instance.showRecordFromHerePanel();
    }


    /**
     * Test of giveFocusToRecordFromHerePanel method, of class PreviewPlayer.
     */
    @Test
    public void testGiveFocusToRecordFromHerePanel() {
        System.out.println("giveFocusToRecordFromHerePanel");
        instance.setVisible(false);
        TimeUtil.skipToMyLou(2);
        instance.giveFocusToRecordFromHerePanel();
    }
}
