/*
 * ImageUtil.java
 *
 * Version 1.0
 * 
 * 17 May 2013
 */
package com.bixly.pastevid.util;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

/**
 * Utility class for watermarking screencap frames.
 * @author Bixly
 */
public class ImageUtil {

    // Extensions for differnt image formats
    public static final String PNG_FORMAT = "png";
    public static final String GIF_FORMAT = "gif";
    public static final String JPEG_FORMAT = "jpeg";
    
    private static final int OFFSET_X = 10;
    private static final int OFFSET_Y = 10;
    
    public static final int MARK_LEFT_TOP = 1;
    public static final int MARK_RIGHT_TOP = 2;
    public static final int MARK_CENTER = 3;
    public static final int MARK_LEFT_BOTTOM = 4;
    public static final int MARK_RIGHT_BOTTOM = 5;

    /**
     * Adds a watermark to the image and compresses it.
     * @param imageSource
     * @param compressionQuality
     * @param mark
     * @param markImageSource 
     */
    public static void markAndCompress(String imageSource, 
            float compressionQuality, boolean mark, String markImageSource) {
        
        try {
            File fileImageSource = new File(imageSource);
            if (!fileImageSource.exists()) {
                throw new ImageDoesNotExistException("Mark Image doesn't exists: " + fileImageSource.getAbsolutePath());
            }
            
            BufferedImage bufferedImage = ImageIO.read(fileImageSource);

            if (mark) {
                addMark(bufferedImage, markImageSource, 1.0f, ImageUtil.MARK_LEFT_BOTTOM);
            }
            
            // Get a jpeg writer
            ImageWriter writer = null;
            Iterator iter = ImageIO.getImageWritersByFormatName("jpg");
            if (iter.hasNext()) {
                writer = (ImageWriter) iter.next();
            }
            
            if (writer == null) {
                throw new IOException("Could not get JPEG writer");
            }
            
            // Prepare output file
            ImageOutputStream ios = ImageIO.createImageOutputStream(fileImageSource);
            
            if (ios == null) {
                throw new IOException("Could not open image stream to write image watermark");
            }
            
            writer.setOutput(ios);

            // Set the compression quality
            ImageWriteParam iwparam = new JPEGImageWriteParam(Locale.getDefault());
            iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            if (compressionQuality < 0.0F || compressionQuality > 1.0F) {
                compressionQuality = 1.0F;
            }
            iwparam.setCompressionQuality(compressionQuality);

            // Write the image
            writer.write(null, new IIOImage(bufferedImage, null, null), iwparam);

            // Cleanup
            ios.flush();
            writer.dispose();
            ios.close();
            
        } catch ( IllegalArgumentException e) {
            log(e);
        } catch ( IOException e) {
            log(e);
        } catch (ImageDoesNotExistException e){
            log(e);
        }
    }

    /**
     * Adds a watermark to the image.
     * @param bufferedImage
     * @param markImageSource
     * @param alpha
     * @param mark_position
     * @throws ImageDoesNotExistException
     * @throws IllegalArgumentException
     * @throws IOException 
     */
    public static void addMark(BufferedImage bufferedImage, 
            String markImageSource, float alpha, int mark_position) 
            throws ImageDoesNotExistException, IllegalArgumentException, IOException {
        
        File fileMarkImage = new File(markImageSource);
        if (!fileMarkImage.exists()) {
            throw new ImageDoesNotExistException("Mark Image doesn't exists: " + fileMarkImage.getAbsolutePath());
        }

        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        Graphics2D g = bufferedImage.createGraphics();
        g.drawImage(bufferedImage, 0, 0, width, height, null);

        BufferedImage bufferedMarkImage = ImageIO.read(fileMarkImage);

        int mark_img_width = bufferedMarkImage.getWidth();
        int mark_img_height = bufferedMarkImage.getHeight();

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));

        switch (mark_position) {
            case ImageUtil.MARK_LEFT_TOP:
                g.drawImage(bufferedMarkImage, OFFSET_X, OFFSET_Y, mark_img_width, mark_img_height, null);
                break;
            case ImageUtil.MARK_LEFT_BOTTOM:
                g.drawImage(bufferedMarkImage, OFFSET_X, (height - mark_img_height - OFFSET_Y), mark_img_width, mark_img_height, null);
                break;
            case ImageUtil.MARK_CENTER:
                g.drawImage(bufferedMarkImage, (width - mark_img_width - OFFSET_X) / 2, (height - mark_img_height - OFFSET_Y) / 2,
                        mark_img_width, mark_img_height, null);
                break;
            case ImageUtil.MARK_RIGHT_TOP:
                g.drawImage(bufferedMarkImage, (width - mark_img_width - OFFSET_X), OFFSET_Y, mark_img_width, mark_img_height, null);
                break;
            case ImageUtil.MARK_RIGHT_BOTTOM:
            default:
                g.drawImage(bufferedMarkImage, (width - mark_img_width - OFFSET_X), (height - mark_img_height - OFFSET_Y),
                        mark_img_width, mark_img_height, null);
        }
        g.dispose();
    }

    private static void log(Object message) {
        LogUtil.log(ImageUtil.class, message);
    }
}
