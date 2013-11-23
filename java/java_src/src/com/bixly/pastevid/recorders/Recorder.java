/*
 * Recorder.java
 * 
 * Version 1.0
 * 
 * 8 May 2013
 */
package com.bixly.pastevid.recorders;

import com.bixly.pastevid.Settings;
import com.bixly.pastevid.models.AudioFileItem;
import com.bixly.pastevid.models.VideoFileItem;
import com.bixly.pastevid.screencap.components.IAudioObserver;
import com.bixly.pastevid.util.FileUtil;
import com.bixly.pastevid.util.LogUtil;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TimerTask;
import javax.swing.Timer;

/**
 * @author Bixly
 */
public class Recorder {
    public final static String VIDEO_EXT  = ".mov";
    
    // Used to save a recording to resume next time application is opened
    public final static String VIDEO_DATA = "videoData.dat";
    public final static String AUDIO_DATA = "audioData.dat";
    
    // Different states of video quality
    public static final int MODE_FAST   = 0;
    public static final int MODE_NORMAL = 1;
    public static final int MODE_BEST   = 2;
    
    // Default video quality recording mode
    private int selectedMode = 2;    
    
    /**
     * Keeps track of current state of Recorder
     */
    private RecorderStatus status = RecorderStatus.STOPPED;
    
    // Components
    private AudioRecorder audioRecorder;
    private VideoRecorder videoRecorder;
    private Timer audioLineMonitor;
    private IMeasurable measurable;
    
    // Relevant directories
    private String videoCaptureFolder = "";
    private String captureFolder      = "";
    private String videoDirName       = "";
    private String backupFolder       = "";
    
    /**
     * Native directory separator for the system.
     */
    private String separator = Settings.FILE_SEP;
    
    /**
     * Location of previously recorded video. 
     * If empty, there is no previously saved recording.
     */
    private String  prevVideoFileName = "";
    
    /**
     * Location of final video encoding.
     */
    private String  resultVideoPath   = "";
    
    // Used for tracking the audio connection
    private int MAX_AUDIO_DROP   = 5; // up to 6.3 seconds
    private int currentDropCount = 0;
    private boolean waitingForAudio = false;
    
    public Recorder(IMeasurable measurable) {
        this.measurable = measurable;
        this.audioRecorder = new AudioRecorder(this);
        this.videoRecorder = new VideoRecorder(this);
        this.videoDirName = String.valueOf(System.currentTimeMillis());
    }
    
    /**
     * Returns true if there is a previously saved recording.
     * @return 
     */
    public Boolean hasPreviousCompiledVideo() {
        return this.prevVideoFileName.length() > 0;
    }
    
    /**
     * Returns the path of the previously saved recording.
     * @return 
     */
    public String getPrevVideoFileName() {
        return prevVideoFileName;
    }
    
    /**
     * Sets the path where the final video encoding will be saved.
     * @param path 
     */
    public void setResultVideoPath(String path){
        this.resultVideoPath = path;
    }
    
    /**
     * Returns the path where the final video encoding is saved.
     * @return 
     */
    public String getResultVideoPath(){
        return this.resultVideoPath;
    }

    /**
     * Sets the path to the previously recorded video if any.
     * @param prevVideoFileName 
     */
    public void setPrevVideoFileName(String prevVideoFileName) {
        this.prevVideoFileName = prevVideoFileName;
    }
    
    /**
     * Returns the selected recording quality mode.<br/>
     * Possible values are:<br/>
     *    0: Fast<br/>
     *    1: Normal<br/>
     *    2: Best<br/>
     * @return 
     */
    public int getSelectedMode() {
        return selectedMode;
    }
    
    /**
     * Returns the image compression value based on the selected recording 
     * quality mode.
     * @return
     * @throws UnsupportedImageCompression 
     */
    public float getImageCompresion() throws UnsupportedImageCompression {
        float compresion = 1.0F;
        switch (this.getSelectedMode()) {
            case Recorder.MODE_FAST:
                compresion = 0.5F;
                break;
            case Recorder.MODE_NORMAL:
                compresion = 0.75F;
                break;
            case Recorder.MODE_BEST:
                compresion = 1.0F;
                break;
            default:
                throw new UnsupportedImageCompression("Image compress " + this.getSelectedMode() + " is not supported");
        }
       return compresion; 
    }
    
    /**
     * Returns bitrate compression based on the selected recording quality mode.
     * @return
     * @throws UnsupportedBitRateCompression 
     */
    public Integer getBitRateCompresion() throws UnsupportedBitRateCompression {
        Integer compresion =  new Integer(1040000);
        switch (this.getSelectedMode()) {
            case Recorder.MODE_FAST:
                compresion =  new Integer(200000);
                break;
            case Recorder.MODE_NORMAL:
                compresion =  new Integer(700000);
                break;
            case Recorder.MODE_BEST:
                compresion = new Integer(1040000);
                break;
            default:
                throw new UnsupportedBitRateCompression("Bit Rate compress "+this.getSelectedMode() +" is not supported");
        }
       return compresion;
    }
    
    /**
     * Sets the selected recording quality mode.
     * @param selectedMode
     */
    public void setSelectedMode(int selectedMode) {
        this.selectedMode = selectedMode;
    }
    
    /**
     * Returns the audio recorder for this Recorder.
     * @return 
     */
    public AudioRecorder getAudioRecorder() {
        return audioRecorder;
    }

    /**
     * Returns the video recorder for this Recorder.
     * @return 
     */
    public VideoRecorder getVideoRecorder() {
        return videoRecorder;
    }

    /**
     * Returns the current status: RECORDING, STOPPED, PAUSED
     * @return 
     */
    public RecorderStatus getStatus() {
        return this.status;
    }

    /**
     * Returns true if the recorder is currently in the given status
     * @param status
     * @return 
     */
    public Boolean hasStatus(RecorderStatus status) {
        return this.status.equals(status);
    }

    /**
     * Sets the recorder status.
     * @param status 
     */
    public void setStatus(RecorderStatus status) {
        this.status = status;
    }
    
    public Timer getAudioLineMonitor(){
        return this.audioLineMonitor;
    }
    
    public void setAudioLineMonitor(Timer audioLineMonitor){
        this.audioLineMonitor = audioLineMonitor;
    }
    
    public int getCurrentDropCount(){
        return this.currentDropCount;
    }
    
    public void setCurrentDropCount(int currentDropCount){
        this.currentDropCount = currentDropCount;
    }
    
    /**
     * Returns true if maximum audio drops are reached.
     * @return 
     */
    public synchronized boolean isDroppedCountReached(){
        return (currentDropCount > MAX_AUDIO_DROP);
    }
    
    /**
     * Returns the recording clock value in milliseconds.
     * @return 
     */
    public long getMillisecondsTime() {
        return (this.measurable != null) ? this.measurable.getValue() : 0L;
    }
    
    /**
     * Returns the recording clock value in seconds.
     * @return 
     */
    public long getSecondsTime() {
        return (this.measurable != null) ? this.measurable.getValue() / 1000L : 0L;
    }

    /**
     * Returns the screen where the recorder is currently at.
     * @return 
     */
    public GraphicsDevice getScreen() {
        return (this.measurable != null) ? this.measurable.getScreen() : null;
    }

    /**
     * Closes the audio line and stop the audioLine timer.
     */
    public void dropAudioLine() {
        try {
            // Stop timer
            if (this.audioLineMonitor != null && this.audioLineMonitor.isRunning()) {
                this.audioLineMonitor.stop();
            }
            // Stop recording audio
            this.audioRecorder.closeLine();
        } catch (SecurityException ex) {
            log(ex);
        }
    }

    // ----------------------- Start files management --------------------------
    
    public String getMp4() {
        return this.getFile().replace(".mov", ".mp4");
    }
    public String getAvi() {
        return this.getFile().replace(".mov", ".mp4");
    }

    public String getWav() {
        return this.getFile().replace(".mov", ".wav");
    }
    
    public String getFile() {
        return this.videoRecorder.getOutputFileName();
    }
    
    public long getFileMS() {
        return this.videoRecorder.getOutputMovieFileMillis();
    }
    public long getOffset(){
        long offset = audioRecorder.getStartMS() - videoRecorder.getStartMS();
        return offset > 0? offset : 0;
    }
    public String getCaptureFolder() {
        return captureFolder;
    }
    
    public String getVideoCaptureFolder() {
        return (videoCaptureFolder = createVideoCaptureFolder());
    }
    
    public String getSeparator() {
        return separator;
    }
    
    /**
     * Creates the temporary directory where the screen capture files for this 
     * recording will be saved.
     * @return The path of the created directory
     */
    private String createVideoCaptureFolder() {
        String capFolder   = this.getCaptureFolder();
        File newCaptureDir = new File(this.getCaptureFolder() + this.getSeparator() + this.videoDirName);
        try {
            if (!newCaptureDir.exists()) {
                newCaptureDir.mkdir();
            }
            capFolder = newCaptureDir.getPath() + this.getSeparator();
        } catch (Exception e) {
        }
        return capFolder;
    }

    /**
     * Creates the general Screenbird directory, if not yet created.
     */
    private void createGeneralDirectory() {
        this.captureFolder = Settings.SCREEN_CAPTURE_DIR;
        this.captureFolder = this.captureFolder.replace("\\\\", "\\");
        File f = new File(this.captureFolder);
        if (!f.exists()) {
            f.mkdir();
        }
    }

    /**
     * Deletes the temporary directory for the screen capture files for this
     * recording.
     */
    public void deleteFiles() {
        try {
            File f = new File(this.getCaptureFolder());
            if (f.exists()) {
                //Prevent deletion of previousFilename and properites 'metadata' file
                FileUtil.deleteFiles(this.getCaptureFolder());
                FileUtil.deleteSubdirs(this.getCaptureFolder());
            }
        } catch (Exception ex) {
        }
    }
    
    /**
     * DontDelete string is a single file location relative to the screencapture
     * directory that you wish not to delete.
     * @param dontDelete
     * @return 
     */
    public boolean deleteFiles(String dontDelete) {
        if (dontDelete == null) {
            return false;
        }
        try {
            File f = new File(this.getCaptureFolder());
            if (f.exists()) {             
                // Prevent deletion of previousFilename and properites/config file
                FileUtil.deleteFiles(this.getCaptureFolder(), 
                        new String[] {this.prevVideoFileName, 
                                      f.getPath()+ this.separator + dontDelete});                
                FileUtil.deleteSubdirs(this.getCaptureFolder());
                return true;
            }
        } catch (Exception ex) {
        }
        return false;
    }
    
    // ------------------------ End files management --------------------------

    
    // ----------------------- Start audio management -------------------------
    
    /**
     * Stops recording audio.
     */
    public void stopRecordingAudio() {
        java.util.Timer timer = new java.util.Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                audioRecorder.stopRecording();
            }
        }, 1000);
    }
    
    /**
     * Starts recording audio.
     */
    public void recordAudio() {
        audioRecorder.record();
    }

    /**
     * Returns true if the recording audio line is open.
     * @return 
     */
    public synchronized Boolean isLineOpen() {
        return this.audioRecorder.isLineOpen();
    }

    /**
     * Returns the volume of the recording.
     * @return 
     */
    public int getVolume() {
        return this.audioRecorder.getVolume();
    }

    /**
     * Returns the max volume for the recording.
     * @return 
     */
    public int getMaxVolume() {
        return AudioRecorder.MAX_VOLUME;
    }

    /**
     * Compiles the audio recordings.
     */
    public synchronized void compileAudio() {
        waitingForAudio = true;
        java.util.Timer timer = new java.util.Timer("Compile Audio Timer");
        timer.schedule(new TimerTask() {
            public void run() {
                audioRecorder.compileAudio(getWav());
                waitingForAudio = false;
            }
        }, 3000);
    }

    /**
     * Adds an AudioRecorder observer
     * @param audioObserver 
     */
    public void addAudioObserver(IAudioObserver audioObserver) {
        audioRecorder.addObserver(audioObserver);
    }

    /**
     * Returns true if audio is currently compiling.
     * @return 
     */
    public synchronized boolean isWaitingForAudio() {
        return waitingForAudio;
    }
    
    /**
     * Returns true if the AudioRecorder has already recorded audio that can be 
     * compiled.
     * @return 
     */
    public boolean hasAudioToCompile() {
        return audioRecorder.hasRecorded();
    }
    
    // ------------------------ End audio management --------------------------

    
    // ------------------ Start video recording management --------------------
    public Rectangle getCaptureRectangle() {
        return this.videoRecorder.getCaptureRectangle();
    }

    public void setCaptureRectangle(Rectangle captureRect) {
        this.videoRecorder.setCaptureRectangle(captureRect);
        this.measurable.relocate(captureRect.x, captureRect.y);
    }

    /**
     * Records a new video.
     */
    public void recordVideo() {
        recordVideo(false);
    }
    
    /**
     * Records a video, either creating a new one or continuing a previous
     * recording using the record method of the VideoRecorder based on 
     * isRecordingPreviousVideo parameter.
     * @param isRecordingPreviousVideo 
     */
    public void recordVideo(boolean isRecordingPreviousVideo) {
        this.videoRecorder.record(isRecordingPreviousVideo);
    }
    
    /**
     * Returns true if the recorder has a PAUSED status.
     * @return 
     */
    public boolean isPaused() {
        return this.hasStatus(RecorderStatus.PAUSED);
    }

    /**
     * Pauses the video recording.
     */
    public void pauseVideo() {
        this.videoRecorder.pause();
    }

    /**
     * Stops the video recording.
     */
    public void stopVideo() {
        this.videoRecorder.stop();
    }

    /**
     * Compiles the recorded video.
     * @return
     * @throws IOException 
     */
    public Boolean compileVideo() throws IOException {
        return videoRecorder.compileVideo();
    }
    // -------------------- End video recording management --------------------

    /**
     * Sets the screen on which the Recorder should record from.
     * @param screen 
     */
    public void setScreen(GraphicsDevice screen) {
        videoRecorder.setScreen(screen);
    }
    
    /**
     * Checks for existing screen capture files from a previous recording.
     * @return True if shots from previous recording is found.
     */
    public boolean checkExistingShots() {
        this.createGeneralDirectory();
        File file = new File(this.getCaptureFolder());
        String[] directories = file.list(FileUtil.getDirectoryFilter());
        if (directories.length > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Creates the general Screenbird directory, if not yet created and deletes
     * files from previous recordings.
     */
    public void cleanAndCreateFiles() {
        this.cleanAndCreateFiles(false);
    }
    
    /**
     * Creates the general Screenbird directory, if not yet created, creates a
     * backup video file of the previous recording, if opted, and deletes those files.
     * @param backup true if to create backup video of the previous recording
     */
    public void cleanAndCreateFiles(boolean backup) {
        this.createGeneralDirectory();
        if (backup) {
            this.createTempBackup();
        }
        this.deleteFiles();
    }
    
    /**
     * Creates the general Screenbird directory, if not yet created, and deletes
     * files from previous recordings excluding the file given in the dontDelete
     * parameter
     * @param dontDelete - the path to the file which to will be excluded in the
     * deletion.
     */
    public void cleanAndCreateFiles(String dontDelete) {
        this.createGeneralDirectory();
        this.deleteFiles(dontDelete);
    }
    
    /**
     * Creates a backup directory and video rendering of the existing recording.
     */
    public void createTempBackup(){
        // Create a directory for this backup.
        File dest = this.createBackupDirectory();
        
        // Transfer files from the general screen capture folder to the backup 
        // directory.
        File src = new File(this.captureFolder);
        try {
            FileUtil.copyFolder(src, dest);
        } catch (IOException e) {
            log(e);
        }
    }
    
    /**
     * Creates a backup directory for saving files from previous recording.
     * @return a File pointer to the created directory.
     */
    private File createBackupDirectory() {
        // Create general backups folder
        this.backupFolder = Settings.BACKUP_DIR;
        this.backupFolder = this.backupFolder.replace("\\\\", "\\");
        File f = new File(this.backupFolder);
        if (!f.exists()) {
            f.mkdir();
        }
        
        String[] list = f.list();
        int dirSize = list.length;
        String srcDir;
        String destDir;
        
        // Rotate backup directories, making room for backup 0 which will
        // contain the latest backup.
        for (int i = dirSize - 1; i >= 0; i--) {
            srcDir = this.backupFolder + Settings.FILE_SEP + i + Settings.FILE_SEP;
            destDir = this.backupFolder + Settings.FILE_SEP + (i+1) + Settings.FILE_SEP;
            File src = new File(srcDir);
            File dest = new File(destDir);
            try {
                FileUtil.copyFolder(src, dest);
            } catch (IOException e) {
                log(e);
            }
            FileUtil.deleteDirectory(src);
        }
        
        // Create new directory.
        String newDir = this.backupFolder + Settings.FILE_SEP + "0" + Settings.FILE_SEP;
        File f2 = new File(newDir);
        if (!f2.exists()) {
            f2.mkdir();
        }
        return f2;
    }
    
    /**
     * Stops recording and writes the recorded video and audio data to their 
     * respective files.
     */
    public void stopAndSaveVideoState() {
        // Makes sure that we have recorded something
        if (this.getMillisecondsTime() != 0) {
            // Stop recording
            this.videoRecorder.stop();
            
            // Save video data
            this.videoRecorder.saveVideoItemToFile(Recorder.VIDEO_DATA);
            
            // Save audio data
            if ((!this.audioRecorder.isRecording()) && (!this.audioRecorder.isCompiling())) {
                this.audioRecorder.saveAudioItemToFileNow(); 
            } else {    
                this.audioRecorder.saveAudioItemToFile(true);
            }
        }
    }
    
    /**
     * Checks for existing previous recording
     * @return true if previous recording files are found and that video data on
     * previous recording are still available.
     */
    public boolean previousVideoExists() {
        return  FileUtil.previousRecordingExists() && FileUtil.previousVideoExists(Recorder.VIDEO_DATA);
    }

    /**
     * Restores previous recording based on saved video and audio data.
     */
    public void restoreVideoState() {
        VideoFileItem videoFileItem;
        ArrayList<AudioFileItem> audioFileItems;
        
        this.createGeneralDirectory();
        
        if (FileUtil.previousVideoExists(Recorder.VIDEO_DATA)) {
            videoFileItem = (VideoFileItem) FileUtil.readObjectDataFromFile(Recorder.VIDEO_DATA);
            videoRecorder.restoreVideoFileItem(videoFileItem);
            
            // Restore audio only if it has accompanying video
            if (FileUtil.previousVideoExists(Recorder.AUDIO_DATA)){
                audioFileItems = (ArrayList)FileUtil.readObjectDataFromFile(Recorder.AUDIO_DATA);
                audioRecorder.setAudioFiles(audioFileItems);
            }
        }
    }
    
    /**
     * Returns the duration of the saved video recording.
     * @return 
     */
    public Integer getVideoDuration() {
        return FileUtil.getVideoDuration(this.prevVideoFileName);
    }
    
    public long getVideoFileItemLength() {
        return this.videoRecorder.getVideoFileItemLengthMS();
    }
    
    // Used to check if file has been saved
    /**
     * Creates the saved state marker.
     */
    public void markStateSaved() {
        FileUtil.addMarker("savedState");
    }
    /**
     * Removes the saved state marker.
     */
    public void unmarkStateSaved() {
        FileUtil.removeMarker("savedState");
    }
    /**
     * Checks if saved state marker is existing.
     * @return true if saved state marker is found.
     */
    public boolean checkStateSaved() {
        return FileUtil.checkMarker("savedState");
    }
    
    /**
     * Resets video and audio recording data and prepares a new directory for
     * the recording.
     */
    public void resetRecorderState() {
        this.videoDirName = String.valueOf(System.currentTimeMillis());
        this.videoRecorder.resetData();
        this.audioRecorder.resetData();
    }
    
    /**
     * Deletes the saved data objects on the computer.
     */
    public void deleteDataObjects() {
         FileUtil.deleteDataObject(Recorder.VIDEO_DATA);
         FileUtil.deleteDataObject(Recorder.AUDIO_DATA);
    }
    
    /**
     * Saves video quality to Screenbird's config file
     * @param val 
     */
    public void saveVideoQuality(Integer val) {
        FileUtil.saveProperty("videoQuality", val);
    }
    
    /**
     * Loads the quality value of the video quality setting
     * @return 
     */
    public Integer loadVideoQuality() {
        String value = null;
        try {
            // Defaults to best quality mode
            value = FileUtil.loadProperty("videoQuality", String.valueOf(MODE_BEST));
            if (value != null) {
                return Integer.valueOf(value);
            }
        } catch (NumberFormatException e) {
            log("Invalid Number: " + value);
        } 
        return null;
    }

    public static void log(Object message) {
        LogUtil.log(Recorder.class, message);
    }
}