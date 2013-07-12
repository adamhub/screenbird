/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.recorders;

import java.awt.Rectangle;
import java.awt.Toolkit;
import javax.imageio.IIOImage;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.Robot;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import com.bixly.pastevid.Settings;
import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.io.File;
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
public class VideoCacheTest {
    
    public VideoCacheTest() {
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
     * Test of addScreenshot method, of class VideoCache.
     */
    @Test
    public void testAddScreenshot() {
        System.out.println("addScreenshot");
        int seconds = 0;
        long milliSeconds = 0L;
        File screenshotFile = null;
        VideoCache instance = new VideoCache();
        
        screenshotFile = generateTestImage();
        instance.addScreenshot(seconds, milliSeconds, screenshotFile);
        assertNotNull(instance.getScreenshotFiles(0));
        assertNotNull(instance.getScreenshotImage(0));
        
        screenshotFile.delete();
    }

    /**
     * Test of getScreenshotFiles method, of class VideoCache.
     */
    @Test
    public void testGetScreenshotFiles() {
        System.out.println("getScreenshotFiles");
        File screenshotFile = null;
        VideoCache instance = new VideoCache();
        
        screenshotFile = generateTestImage();
        instance.addScreenshot(0, 500L, screenshotFile);
        assertNotNull(instance.getScreenshotFiles(0));
        
        screenshotFile.delete();
    }

    /**
     * Test of getScreenshotImage method, of class VideoCache.
     */
    @Test
    public void testGetScreenshotImage() {
        System.out.println("getScreenshotImage");
        File screenshotFile = null;
        VideoCache instance = new VideoCache();
        
        screenshotFile = generateTestImage();
        instance.addScreenshot(0, 500L, screenshotFile);
        assertNotNull(instance.getScreenshotImage(0));
        
        screenshotFile.delete();

    }

    /**
     * Test of getScreenshotImages method, of class VideoCache.
     */
    @Test
    public void testGetScreenshotImages() {
        System.out.println("getScreenshotImages");
        int seconds = 0;
        long milliSeconds = 0L;
        File screenshotFile = null;
        VideoCache instance = new VideoCache();
        
        screenshotFile = generateTestImage();
        instance.addScreenshot(seconds, milliSeconds, screenshotFile);
        assertNotNull(instance.getScreenshotImages(0));
        
        screenshotFile.delete();
        
    }

    /**
     * Test of flagScreenshotsAt method, of class VideoCache.
     */
    @Test
    public void testFlagScreenshotsAt() {
        System.out.println("flagScreenshotsAt");
        
        int time = 0;
        int seconds = 0;
        long milliSeconds = 0L;
        File screenshotFile = null;
        VideoCache instance = new VideoCache();
        
        screenshotFile = generateTestImage();
        instance.addScreenshot(seconds, milliSeconds, screenshotFile);
        assertNotNull(instance.getScreenshotImages(time));
        assertFalse(instance.getScreenshotFiles(0).get(0).isFlagged());
        instance.flagScreenshotsAt(time);
        assertTrue(instance.getScreenshotFiles(0).get(0).isFlagged());
        
        screenshotFile.delete();
        
        
    }
    
    
    /**
     * Generates screenshot for JUnit testing
     * @return 
     *      File pointer to screen shot
     */
    private File generateTestImage(){
        Robot awtRobot;
        String currentCaptureDir = Settings.SCREEN_CAPTURE_DIR;
        
        try {
            awtRobot = new Robot();
            
            BufferedImage bufferedImage = awtRobot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));

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

        }catch (AWTException e){
            System.err.println(e);
        }catch (IOException e){
            System.err.println(e);
        }
        
        return null;
    }
}
