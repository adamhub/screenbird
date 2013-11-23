/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.util;

import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import com.bixly.pastevid.models.WaterMark;
import java.io.File;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author cevaris
 */
public class ImageUtilTest {
    
    private static File watermarkRealFile;
    
    public ImageUtilTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        watermarkRealFile = WaterMark.copyWaterMark(generateTempDirectory().getAbsolutePath());
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
            
            File imageFile = generateTempFile("jpeg");
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
    
    private static String writeBufferedImageToFile(BufferedImage im, File output){
        try {
            
            ImageIO.write(im,"jpg",output);
            return output.getAbsolutePath();
            
        } catch (IllegalArgumentException e){
            System.err.println(e);
        } catch (IOException e){
            System.err.println(e);
        }
        
        return "";

    }
    
    private static File generateTempFile(String extension){
        return new File(System.getProperty("java.io.tmpdir"), Long.toString(System.nanoTime())+"."+extension);
    }
    private static File generateTempFile(){
        return generateTempFile("tmp");
    }
    private static File createDirectory(File file){
        file.mkdirs();
        return file;
    }
    private static File generateTempDirectory(){
        return createDirectory(new File(System.getProperty("java.io.tmpdir"), Long.toString(System.nanoTime())));
    }

    /**
     * Test of markAndCompress method, of class ImageUtil.
     */
    @Test
    public void testMarkAndCompress() {
        System.out.println("markAndCompress");
        File imageFile = generateTestImageFile(new Rectangle(0,0,600,600));
        String imageSource = imageFile.getAbsolutePath();
        float compressionQuality = 1.0F;
        boolean mark = true;
        String markImageSource = watermarkRealFile.getAbsolutePath();
        ImageUtil.markAndCompress(imageSource, compressionQuality, mark, markImageSource);

        assert imageFile.exists() : "Image was not created properly";
        MediaUtil.open(imageSource);
        
        mark = false;
        imageFile = generateTestImageFile(new Rectangle(0,0,600,600));
        imageSource = imageFile.getAbsolutePath();
        ImageUtil.markAndCompress(imageSource, compressionQuality, mark, markImageSource);

        assert imageFile.exists() : "Image was not created properly";
        MediaUtil.open(imageSource);
        
    }

    /**
     * Test of addMark method, of class ImageUtil.
     */
    @Test
    public void testAddMark() throws Exception {
        System.out.println("addMark");
        BufferedImage bufferedImage = generateTestImage(new Rectangle(0,0,400,400));
        String markImageSource = watermarkRealFile.getAbsolutePath();
        float alpha = 1.0F;
        int mark_position = ImageUtil.MARK_CENTER;
        bufferedImage = generateTestImage(new Rectangle(0,0,400,400));
        ImageUtil.addMark(bufferedImage, markImageSource, alpha, mark_position);
        MediaUtil.open(writeBufferedImageToFile(bufferedImage, generateTempFile("jpeg")));
        
        mark_position = ImageUtil.MARK_LEFT_BOTTOM;
        bufferedImage = generateTestImage(new Rectangle(0,0,400,400));
        ImageUtil.addMark(bufferedImage, markImageSource, alpha, mark_position);
        MediaUtil.open(writeBufferedImageToFile(bufferedImage, generateTempFile("jpeg")));
        
        mark_position = ImageUtil.MARK_LEFT_TOP;
        bufferedImage = generateTestImage(new Rectangle(0,0,400,400));
        ImageUtil.addMark(bufferedImage, markImageSource, alpha, mark_position);
        MediaUtil.open(writeBufferedImageToFile(bufferedImage, generateTempFile("jpeg")));
        
        mark_position = ImageUtil.MARK_RIGHT_BOTTOM;
        bufferedImage = generateTestImage(new Rectangle(0,0,400,400));
        ImageUtil.addMark(bufferedImage, markImageSource, alpha, mark_position);
        MediaUtil.open(writeBufferedImageToFile(bufferedImage, generateTempFile("jpeg")));
        
        mark_position = ImageUtil.MARK_RIGHT_TOP;
        bufferedImage = generateTestImage(new Rectangle(0,0,400,400));
        ImageUtil.addMark(bufferedImage, markImageSource, alpha, mark_position);
        MediaUtil.open(writeBufferedImageToFile(bufferedImage, generateTempFile("jpeg")));
        
        mark_position = ImageUtil.MARK_CENTER;
        alpha = 0.75F;
        bufferedImage = generateTestImage(new Rectangle(0,0,400,400));
        MediaUtil.open(writeBufferedImageToFile(bufferedImage, generateTempFile("jpeg")));
        ImageUtil.addMark(bufferedImage, markImageSource, alpha, mark_position);
        
        alpha = 0.50F;
        bufferedImage = generateTestImage(new Rectangle(0,0,400,400));
        ImageUtil.addMark(bufferedImage, markImageSource, alpha, mark_position);
        MediaUtil.open(writeBufferedImageToFile(bufferedImage, generateTempFile("jpeg")));
        
        alpha = 0.25F;
        bufferedImage = generateTestImage(new Rectangle(0,0,400,400));
        ImageUtil.addMark(bufferedImage, markImageSource, alpha, mark_position);
        MediaUtil.open(writeBufferedImageToFile(bufferedImage, generateTempFile("jpeg")));
        
        alpha = 0.0F;
        bufferedImage = generateTestImage(new Rectangle(0,0,400,400));
        ImageUtil.addMark(bufferedImage, markImageSource, alpha, mark_position);
        MediaUtil.open(writeBufferedImageToFile(bufferedImage, generateTempFile("jpeg")));
        
    }
}
