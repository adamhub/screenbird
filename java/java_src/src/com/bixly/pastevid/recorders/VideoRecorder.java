/*
 * VideoRecorder.java
 * 
 * Version 1.0
 * 
 * 8 May 2013
 */
package com.bixly.pastevid.recorders;

import com.bixly.pastevid.Settings;
import com.bixly.pastevid.models.VideoFileItem;
import com.bixly.pastevid.models.WaterMark;
import com.bixly.pastevid.util.FileUtil;
import com.bixly.pastevid.util.ImageUtil;
import com.bixly.pastevid.util.LogUtil;
import com.bixly.pastevid.util.ResourceUtil;
import com.bixly.pastevid.util.TimeUtil;
import com.bric.qt.io.JPEGMovieAnimation;
import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.HeadlessException;
import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.ImageIcon;

/**
 * @author Bixly
 */
public class VideoRecorder {
    /**
     * Duration of the output video file in milliseconds.
     */
    private long outputMovieFileMillis;
    
    /**
     * File name of the output video.
     */
    private String outputMovieFilename = "";
    
    // Image type for the screenshots 
    private String imageType = ImageUtil.JPEG_FORMAT;
    
    /**
     * Frames per second setting.
     */
    private int fps = 20;
    private ImageIcon mouseIcon;
    
    // Components
    private Recorder recorder;
    private GraphicsDevice screen;
    private VideoCache videoCache;
    private Rectangle captureRectangle;
    
    private int imagesPerSecond = 0;
    private int processingCounter = 0;
    
    private VideoFileItem videoFileItem;
    private long startMS = 0;
    private ArrayList<ArrayList<String>> screenshots;
    private JPEGMovieAnimation movie;
    
    private ModifyImageThread waterMarkImageThread;
    
    /**
     * True if user is resuming from a previous video
     */
    private boolean isRecordingPreviousVideo = false;

    public VideoRecorder(Recorder recorder) {
        this.mouseIcon = new ImageIcon(getClass().getResource(ResourceUtil.NORMAL_CURSOR));
        this.recorder  = recorder;
        
        this.videoFileItem = new VideoFileItem();
        // Cache the video screen shots
        this.videoCache = new VideoCache();
    }
    
    /**
     * Returns the VideoCache for the recorded screenshots.
     * @return 
     */
    public VideoCache getCache() {
        return this.videoCache;       
    }

    /**
     * Starts recording.
     * If isRecordingPreviousVideo is true, user is resuming from a
     * previous recording 
     * @param isRecordingPreviousVideo 
     */
    public void record(boolean isRecordingPreviousVideo) {
        // Set status flag
        this.isRecordingPreviousVideo = isRecordingPreviousVideo;
        
        // Starting from scratch
        if (recorder.hasStatus(RecorderStatus.STOPPED)) {
            outputMovieFileMillis = System.currentTimeMillis();
            outputMovieFilename = recorder.getCaptureFolder() + outputMovieFileMillis + Recorder.VIDEO_EXT;
        }
        
        recorder.setStatus(RecorderStatus.RECORDING);
        
        // Start thread for capturing images
        CaptureImagesThread captureDesktopThread = new CaptureImagesThread();
        captureDesktopThread.start();
    }
    
    /**
     * Starts recording a new video.
     */
    public void record() {
        record(false);
    }

    /**
     * Pause screen recording.
     */
    public void pause() {
        recorder.setStatus(RecorderStatus.PAUSED);
        recorder.stopRecordingAudio();
    }

    /**
     * Stop screen recording.
     */
    public void stop() {
        recorder.setStatus(RecorderStatus.STOPPED);
        recorder.stopRecordingAudio();
    }

    /**
     * Wait for running processes to end. Used before starting video compilation
     * to avoid conflicts.
     */
    private void waitForProcess() {
        long initialTime = System.currentTimeMillis();
        long endTime;

        while (processingCounter > 0) {
            endTime = System.currentTimeMillis() - initialTime;
            if (endTime > 5000) {
                break;
            }
        }
    }
    
    /**
     * Compiles video recording without the audio. Used for the recovery option.
     */
    private void preCompileVideoOnly() {
        // Get screencap folder
        String captureFolder = recorder.getCaptureFolder();
        File file = new File(captureFolder);
        
        String[] directories = file.list(FileUtil.getDirectoryFilter());
        log("preCompileVideoOnly directories" + Arrays.toString(directories));
        
        // Check for folders of previous recordings
        if (directories.length > 0) {
            // Set the capture folder to the screenshots folder
            captureFolder = captureFolder + directories[0] + recorder.getSeparator();
            if (videoFileItem.getDirectory() == null) {
                videoFileItem.setDirectory(captureFolder);
            }
            String videoFileItemDirectory = videoFileItem.getDirectory();
            log("videoFileItemDirectory" + videoFileItemDirectory);

            // Get list of screenshots
            ArrayList<String> inputFiles = FileUtil.getFileList(videoFileItem.getDirectory(), FileUtil.createExtension(imageType));

            // Initialize the screenshots list for this VideoRecorder
            this.screenshots = new ArrayList<ArrayList<String>>(videoFileItem.getEnd() + 1);
            for (int i = 0; i < videoFileItem.getEnd() + 1; i++) {
                ArrayList<String> screenshotsSecond = new ArrayList<String>(imagesPerSecond);
                this.screenshots.add(screenshotsSecond);
            }
            for (int i = 0; i < inputFiles.size(); i++) {
                String filePath = inputFiles.get(i);
                String fileName = FileUtil.removeExtension(filePath);
                String[] fileSplitted = fileName.split("_");
                int secondSection = Integer.valueOf(fileSplitted[1]).intValue();
                try {
                    this.screenshots.get(secondSection).add(filePath);
                } catch (IndexOutOfBoundsException ex) {
                    // It shouldn't happen. We check it just in case.
                    ArrayList<String> screenshotsSecond = new ArrayList<String>(imagesPerSecond);
                    screenshotsSecond.add(filePath);
                    this.screenshots.add(screenshotsSecond);
                }
            }

            for (int i = 0; i < screenshots.size(); i++) {
                String fileToAdd;
                if (i == 0 && this.screenshots.get(i).isEmpty()) {
                    fileToAdd = getNextScreenshot(i, false);
                } else {
                    fileToAdd = getNextScreenshot(i, true);
                }
                int secImagesSize = this.screenshots.get(i).size();
                for (; secImagesSize < imagesPerSecond; secImagesSize++) {
                    this.screenshots.get(i).add(fileToAdd);
                }
            }   
        }
        videoFileItem.setDirectory(null);
    }
    
    /**
     * Builds the list of screenshots for this video by searching for existing
     * screencap folders and files.
     */
    private void preCompileVideo() {
        // Set the directory for the screencapture files
        if (videoFileItem.getDirectory() == null) {
            videoFileItem.setDirectory(recorder.getVideoCaptureFolder());
        }
        
        // Get list of screenshots for this video
        log("Images per second: " + imagesPerSecond);
        ArrayList<String> inputFiles = FileUtil.getFileList(videoFileItem.getDirectory(), 
                FileUtil.createExtension(imageType));
        this.screenshots = new ArrayList<ArrayList<String>>(videoFileItem.getEnd() + 1);
        
        // Initialize screenshots container per second
        for (int i = 0; i < videoFileItem.getEnd() + 1; i++) {
            ArrayList<String> screenshotsSecond = new ArrayList<String>(imagesPerSecond);
            this.screenshots.add(screenshotsSecond);
        }
        
        // Iterate over the screenshot files
        for (int i = 0; i < inputFiles.size(); i++) {
            // Get screenshot file path
            String filePath = inputFiles.get(i);
            
            // Get screenshot file name
            String fileName = FileUtil.removeExtension(filePath);
            
            // Parse screenshot file name to get the second it belongs to in the video
            String[] fileSplitted = fileName.split("_");
            int secondSection = Integer.valueOf(fileSplitted[1]).intValue();
            
            try {
                // Add screenshot file to its respective second
                this.screenshots.get(secondSection).add(filePath);
            } catch (IndexOutOfBoundsException ex) {
                // It shouldn't happen. We check it just in case.
                ArrayList<String> screenshotsSecond = new ArrayList<String>(imagesPerSecond);
                screenshotsSecond.add(filePath);
                this.screenshots.add(screenshotsSecond);
            }
        }

        // Iterate over the screenshots
        for (int i = 0; i < screenshots.size(); i++) {
            String fileToAdd;
            if (i == 0 && this.screenshots.get(i).isEmpty()) {
                fileToAdd = getNextScreenshot(i, false);
            } else {
                // Get last screenshot for this second
                fileToAdd = getNextScreenshot(i, true);
            }
            int secImagesSize = this.screenshots.get(i).size();
            // If we have insufficient images for this second to meet the set
            // images per second, we pad the screenshots for this second with
            // the last screenshot for the same second.
            for (; secImagesSize < imagesPerSecond; secImagesSize++) {
                this.screenshots.get(i).add(fileToAdd);
            }
        }
    }
    
    /**
     * Rebuilds VideoCache from current screenshots.
     */
    private void repopulateCache() {
        int screenshotsSize = this.screenshots.size();
        
        // Iterate over screenshots per second of video
        for (int i = 0; i < screenshotsSize; i++) {
            ArrayList<String> screenshotsSecond = this.screenshots.get(i);
            
            // Iterate over screenshots for this second
            for (String filePath : screenshotsSecond) {
                // Parse screenshot file name for second and millisecond info
                String fileName = FileUtil.removeExtension(filePath);
                String[] fileSplitted = fileName.split("_");
                int secondSection = Integer.valueOf(fileSplitted[1]).intValue();
                int msSection = Integer.valueOf(fileSplitted[2]).intValue();
                
                // Add screenshot to the video cache
                this.videoCache.addScreenshot(secondSection, msSection, new File(filePath));
            }
        }
    }

    /**
     * Returns path of either the first or last screenshot for second i based on 
     * last parameter
     * @param i The second in the video where the screenshot will be fetched
     * @param last True if fetching last screenshot
     * @return 
     */
    private String getNextScreenshot(int i, boolean last) {
        String nextSs = "";
        for (int j = i; j < screenshots.size(); j++) {
            if (!this.screenshots.get(j).isEmpty()) {
                if (last) {
                    int secImagesSize = this.screenshots.get(j).size();
                    nextSs = this.screenshots.get(j).get(secImagesSize - 1);
                } else {
                    nextSs = this.screenshots.get(j).get(0);
                }
                break;
            }
        }
        return nextSs;
    }

    /**
     * Compiles the recorded video, adding the screenshots as frames to the
     * generated movie.
     * @throws IOException 
     */
    private void _compileVideo() throws IOException {
        // Iterate over seconds of the video
        for (int i = 0; i < screenshots.size(); i++) {
            // Get size of screenshots for this second
            int ssSize = screenshots.get(i).size();
            if (ssSize > 0) { 
                // Frame display duration
                float vg = 1F / ssSize;
                
                // Iterate over screenshots for this second
                for (int j = 0; j < ssSize; j++) {
                    String filePath = screenshots.get(i).get(j);
                    if (filePath != null && filePath.length() > 0) {
                        movie.addFrame(vg, new File(filePath));
                    }
                }
            }
        }
    }

    /**
     * Compiles a movie of the recorded video.
     * @return true on successful compilation.
     * @throws IOException 
     */
    public Boolean compileVideo() throws IOException {
        waitForProcess();
        preCompileVideo();

        File file = new File(this.outputMovieFilename);
        movie = new JPEGMovieAnimation(file);
        _compileVideo();

        movie.close();
        return true;
    }
    
    /**
     * Compiles an audio-less movie of the recorded video. Used on recovery
     * options.
     * @return
     * @throws IOException 
     */
    public Boolean compileVideoOnly() throws IOException {
        preCompileVideoOnly();
        outputMovieFilename = Settings.HOME_DIR + "temp" + Recorder.VIDEO_EXT;
        File file = new File(this.outputMovieFilename);
        movie = new JPEGMovieAnimation(file);
        _compileVideo();
        movie.close();
        return true;
    }

    public long getStartMS() {
        return startMS;
    }

    public String getOutputFileName() {
        return this.outputMovieFilename;
    }

    public void setOutputFileName(String filename) {
        this.outputMovieFilename = filename;
    }

    public long getOutputMovieFileMillis() {
        return this.outputMovieFileMillis;
    }

    public void setOutputMovieFileMillis(long time) {
        this.outputMovieFileMillis = time;
    }

    public Rectangle getCaptureRectangle() {
        return captureRectangle;
    }

    public void setCaptureRectangle(Rectangle captureRectangle) {
        this.captureRectangle = captureRectangle;   
    }

    public void setFps(int framesPerSecond) {
        this.fps = framesPerSecond;
    }

    public int getFps() {
        return this.fps;
    }
    
    public void saveVideoItemToFile(String name){
        FileUtil.saveObjectDataToFile(this.videoFileItem, name);
    }
    public synchronized void setScreen(GraphicsDevice screen) {
        this.screen = screen;
    }

    public void setPrevData(VideoFileItem videoFileItem) {
        this.videoFileItem = videoFileItem;
        outputMovieFileMillis = System.currentTimeMillis();
        outputMovieFilename = recorder.getCaptureFolder() + outputMovieFileMillis + Recorder.VIDEO_EXT;
    }
    
    /**
     * Restores the given VideoFileItem for the recorder.
     * @param videoFileItem 
     */
    public void restoreVideoFileItem(VideoFileItem videoFileItem) {
        this.videoFileItem = videoFileItem;
        outputMovieFileMillis = System.currentTimeMillis();
        outputMovieFilename = recorder.getCaptureFolder() + outputMovieFileMillis + Recorder.VIDEO_EXT;
        preCompileVideo();
        repopulateCache();
    }
    
    public long getVideoFileItemLengthMS() {
        return this.videoFileItem.getEndMS();
    }
    
    /**
     * Resets the VideoFileItem for this VideoRecorder.
     */
    public void resetData() {
        this.videoFileItem = new VideoFileItem();
    }

    public void log(Object message) {
        LogUtil.log(VideoRecorder.class, message);
    }

    /**
     * Handles capturing of screenshots for the screen recording.
     */
    class CaptureImagesThread extends Thread {

        public CaptureImagesThread() {
            super("Capture Images Thread");
        }
        
        @Override
        public void run() {
            Robot awtRobot;
            int imgCounter = 0;
            long prevSecond = -1;
            long prevMSecond = -1;

            // We need to reset both vars always
            imagesPerSecond = 0;
            processingCounter = 0;
            try {
                // Start watermarking thread
                if (waterMarkImageThread != null && waterMarkImageThread.isAlive()) {
                    waterMarkImageThread.flush();
                }
                waterMarkImageThread = new ModifyImageThread();
                waterMarkImageThread.start();
                
                // Set up awtRobot
                if (screen != null) {
                    awtRobot = new Robot(screen);
                } else {
                    awtRobot = new Robot();
                }
                
                // Set directory for the video data file of this recording
                if (videoFileItem.getDirectory() == null || videoFileItem.getDirectory().equals("")) {
                    videoFileItem.setDirectory(recorder.getVideoCaptureFolder());
                }
                String currentCaptureDir = videoFileItem.getDirectory();
                
                // Start recording audio
                recorder.recordAudio();
                if (startMS == 0) {
                    startMS = System.currentTimeMillis();
                }
                
                // Set up JPEG writer
                Iterator iter = ImageIO.getImageWritersByFormatName("jpeg");
                ImageWriter writer = (ImageWriter) iter.next();
                if (writer == null) {
                    throw new IOException("JPEG Writer is Null");
                }
                ImageWriteParam iwp = writer.getDefaultWriteParam();
                iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                iwp.setCompressionQuality(recorder.getImageCompresion()); 
                
                BufferedImage prevImage = null;

                // While the recorder is still on RECORDING status
                while (recorder.hasStatus(RecorderStatus.RECORDING)) {
                    // We have to record it here because it may change later.
                    long recorderTimeMS = recorder.getMillisecondsTime();
                    long recorderTime = recorder.getSecondsTime();
                    int screenshotInterval = 150;
                    if (prevSecond != recorderTime) {
                        prevSecond = recorderTime;
                        imgCounter = 0;
                    }
                    videoFileItem.setEndMS(recorderTimeMS);

                    // Control screencap frames
                    if ((imgCounter < 8) && (prevMSecond != recorderTimeMS) && ((recorderTimeMS - prevMSecond) > screenshotInterval || prevMSecond == -1)) {
                        long start = System.nanoTime();
                        
                        prevMSecond = recorderTimeMS;
                        BufferedImage bufferedImage = awtRobot.createScreenCapture(captureRectangle);
                        
                        // Draws the mouse pointer.
                        PointerInfo mouseInfo = MouseInfo.getPointerInfo();
                        if (recorder.getScreen().getIDstring().equals(mouseInfo.getDevice().getIDstring())) {
                            // Draw the mouse pointer if the recording screen
                            // and the screen the mouse is in is the same.
                            int mouseX = mouseInfo.getLocation().x - captureRectangle.x;
                            int mouseY = mouseInfo.getLocation().y - captureRectangle.y;
                            Graphics2D graphics2D = bufferedImage.createGraphics();
                            graphics2D.drawImage(mouseIcon.getImage(), mouseX, mouseY, 14, 20, null);
                        }
                        
                        if (compareImages(bufferedImage, prevImage)) {
                            continue;
                        } 
                        long msName = System.currentTimeMillis();   
                        File imageFile = new File(currentCaptureDir
                                + msName
                                + "_"
                                + recorderTime
                                + "_"
                                + recorderTimeMS
                                + FileUtil.createExtension(imageType));

                        try {
                            FileImageOutputStream stream = new FileImageOutputStream(imageFile);
                            // Set up file access
                            writer.setOutput(stream);
                            // Create image
                            IIOImage image = new IIOImage(bufferedImage, null, null);
                            // write image
                            writer.write(null, image, iwp);
                            // Close image stream
                            stream.close();
                            imgCounter++;
                            if (imgCounter > imagesPerSecond && imgCounter < 8) {
                                imagesPerSecond = imgCounter;
                            }

                            // If user is resuming recording, do not watermark
                            if (recorderTime <= WaterMark.LIMIT && !isRecordingPreviousVideo) {
                                waterMarkImageThread.addFile(recorderTimeMS, imageFile);
                            }
                            
                            videoCache.addScreenshot((int)recorderTime, recorderTimeMS, imageFile);
                            
                        } catch (IOException ex) {
                            log(ex.toString());
                        }
                        log("Capture time: " + (System.nanoTime() - start));
                    }
                }
                waterMarkImageThread.flush();
                writer.dispose();
                
            } catch (AWTException e) {
                log(e);
            } catch (HeadlessException e) {
                log(e);
            } catch (IOException e) {
                log(e);
            } catch (UnsupportedImageCompression e) {
                log(e);
            } finally {
                if(waterMarkImageThread != null) {
                    waterMarkImageThread.flush();
                }
            }

        }        
        
        private boolean compareImages(BufferedImage img1, BufferedImage img2) {
            if (img1 == null || img2 == null) {
                return false;
            }
            
            for (int x = 0; x < img1.getWidth(); x+=2) {
                for (int y = 0; y < img1.getHeight(); y+=2) {
                    if (img1.getRGB(x, y) != img1.getRGB(x, y)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
    
    /**
     * Handles watermarking of screen shots
     */
    private class ModifyImageThread extends Thread {
        
        /**
         * Queue of screen shots file pointers
         */
        private final ConcurrentSkipListMap<Long, File> imageMap = new ConcurrentSkipListMap<Long, File>();
        
        /**
         * Signal for controlling the thread
         */
        private boolean isRunning = false;
        
        public ModifyImageThread() {
            super("Modify Image Thread");
        }
        
        /**
         * Add screen shot file pointer to queue
         * @param imageFile 
         */
        public void addFile(long timestampMS, File imageFile) {
            imageMap.put(timestampMS, imageFile);
        }

        @Override
        public void run() {
            log("Modify Image Thread has started.");
            
            isRunning = true;
            //While thread is running
            while(this.isRunning) {
                executeModifyImage();
            }
            
            log("Modify Image Thread is closed");
        }
        
        /**
         * Processes all remaining images in queue and then stops the capture
         * of images to watermark.
         */
        public synchronized void flush() {
            this.isRunning = false;
            // Flush remaining images in queue.
            while(!this.imageMap.isEmpty()) {
                executeModifyImage();
            }
        }

        private void executeModifyImage() {
            // Current file pointer
            File imageFile;
            long timestampMS;
            // Check for screen shots
            if (!this.imageMap.isEmpty()) {
                //There is at least one screen shot
                Entry<Long, File> entry = imageMap.pollFirstEntry();
                timestampMS = entry.getKey();
                imageFile = entry.getValue();
            } else {
                // There are currently no screen shots, just keep running
                // and checking for any new watermark requests
                TimeUtil.skipToMyLouMS(1000L); 
                return;
            }

            processingCounter++;
            boolean mark = false;
            String waterMark = "";
            if (timestampMS <= WaterMark.LIMIT * 1000) {
                mark = true;
                File watermarkRealFile = WaterMark.copyWaterMark(recorder.getCaptureFolder());
                waterMark = watermarkRealFile.getAbsolutePath();
            }

            try {
                ImageUtil.markAndCompress(imageFile.getAbsolutePath(), recorder.getImageCompresion(), mark, waterMark);
            } catch (UnsupportedImageCompression e) {
                log(e);
            }
            processingCounter--;
        }
    }
}