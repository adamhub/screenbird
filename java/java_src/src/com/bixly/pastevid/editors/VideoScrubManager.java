/*
 * VideoScrubManager
 * 
 * Version 1.0
 * 
 * 7 May 2013
 */
package com.bixly.pastevid.editors;

import com.bixly.pastevid.Settings;
import com.bixly.pastevid.download.DownloadManager;
import com.bixly.pastevid.download.DownloadStatus;
import com.bixly.pastevid.download.DownloadThread;
import com.bixly.pastevid.recorders.AudioCache;
import com.bixly.pastevid.recorders.Recorder;
import com.bixly.pastevid.recorders.VideoCache;
import com.bixly.pastevid.screencap.RecorderPanel;
import com.bixly.pastevid.screencap.components.preview.PreviewPlayer;
import com.bixly.pastevid.screencap.components.preview.PreviewPlayerForm;
import com.bixly.pastevid.util.*;
import com.bixly.pastevid.screencap.components.progressbar.FFMpegProgressBarListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.SwingUtilities;

/**
 * Main class for all scrub feature application logic.
 * @author cevaris
 */
public class VideoScrubManager {
    // File name where to store scrub data on the computer
    public static final String SCRUB_DATA = "scrubData.dat";
    
    // JFrame and JPanels associated with this VideoScrubManager
    private PreviewPlayerForm jfPreviewPlayer;
    private PreviewPlayer     jpPreviewPlayer;
    private RecorderPanel     jpRecorderPanel;
    
    // Components associated with this VideoScrubManager
    private Recorder   recorder;
    private VideoCache videoCache;
    private AudioCache audioCache;
    
    private ArrayList<VideoScrub> scrubs;
    
    /**
     * Array of integers representing the seconds that is being skipped/scrubbed/edited 
     * out of the final video. This is used to quickly see if we should skip 
     * over a portion of the video during preview playback.
     */
    private ArrayList<Integer> scrubIndex;
    
    private HashMap<Integer, Integer> displayToInternalMap;
    private HashMap<Integer, Integer> internalToDisplayMap;
    private Integer maxCut = null;
    private Integer minCut = null;
    
    private Long    clockOffset   = 0L;
    private Integer totalSeconds  = 0;
    
    private PreviewVideo previewThread;
    
    private boolean isScrubbing = false;
    
    // Comparator for sorting
    public static final VideoScrubSortByStartTime sortByStartComparator = new VideoScrubSortByStartTime();

    public VideoScrubManager() {
        // Allocate data
        this.scrubs = new ArrayList<VideoScrub>();
        this.scrubIndex = new ArrayList<Integer>();
        this.displayToInternalMap = new HashMap<Integer, Integer>();
        this.internalToDisplayMap = new HashMap<Integer, Integer>();
        this.jfPreviewPlayer = new PreviewPlayerForm(this);
        this.jpPreviewPlayer = this.jfPreviewPlayer.getPlayerPanel();
    }
    
    /**
     * Restores video scrub data based on the previous saved data.
     * @param jpRecorderPanel
     * @param clockOffset 
     */
    public void restoreScrubFile(RecorderPanel jpRecorderPanel, long clockOffset) {
        try {
            ArrayList<VideoScrub> _scrubs = (ArrayList<VideoScrub>)FileUtil.readObjectDataFromFile(this.SCRUB_DATA);
            this.scrubs = _scrubs;
            this.scrubIndex = new ArrayList<Integer>();
            for (VideoScrub scrub : this.scrubs) {
                for (int i=scrub.start; i < scrub.end; i++) {
                    this.scrubIndex.add(i);
                }
            }
        } catch(Exception e) {
            log(e);
        }
        
        setRecorderPanel(jpRecorderPanel);
        this.clockOffset = 0L;
        
        updateScrubMaps();
        log("Scrub restored");
    }
    
    /**
     * Save the scrub data into the file named in SCRUB_DATA.
     */
    public void saveScrubFile(){
        FileUtil.saveObjectDataToFile(this.scrubs, getScrubDataName());
    }
    
    /**
     * Returns the file name where to store the scrub data.
     * @return 
     */
    private static String getScrubDataName() {
        return SCRUB_DATA;
    }
    
    /**
     * Associate this VideoScrubManager object with a RecorderPanel instance and
     * its components.
     * @param jpRecorderPanel the RecorderPanel instance to associate with
     */
    public void setRecorderPanel(RecorderPanel jpRecorderPanel) {
        this.jpRecorderPanel = jpRecorderPanel;
        this.recorder   = jpRecorderPanel.getRecorder();
        this.videoCache = this.recorder.getVideoRecorder().getCache();
        this.audioCache = this.recorder.getAudioRecorder().getCache();
        this.audioCache.setScrubs(scrubIndex);
    }

    /**
     * Checks if this VideoScrubManager is currently scrubbing.
     * @return true if scrubbing
     */
    public synchronized boolean isScrubbing() {
        return isScrubbing;
    }
    
    /**
     * Update the current and remaining time labels in the PreviewPlayer 
     * associated with this VideoScrubManager object.
     * @param currTimeSecondsVal the current time to display, in seconds
     */
    public void updateTimeLabels(int currTimeSecondsVal) {
        String currentClockTime = getClockTime(currTimeSecondsVal, false);
        String remClockTime = String.format("-%s", 
                getClockTime((int)(totalSeconds - clockOffset - currTimeSecondsVal), false));
        this.jpPreviewPlayer.setLabelTextTimeCurrent(currentClockTime);
        this.jpPreviewPlayer.setLabelTextTimeRemaining(remClockTime);
        jpPreviewPlayer.updateRecordNowButtonPosition();
    }
    
    /**
     * Open and focus screen on preview player.
     */
    public void previewSliderAction() {
        // Screenshot image to be displayed on the preview player
        BufferedImage image;
        Integer sliderValue = this.jpPreviewPlayer.getSliderValue();
        
        this.jpPreviewPlayer.setSliderMax((int)(totalSeconds-clockOffset));
        this.updateTimeLabels(sliderValue);
        log("Starting previewing at " + sliderValue);
        
        if (this.displayToInternalMap.containsKey(sliderValue)
                && this.displayToInternalMap.get(sliderValue) != null) {
            int mappedSliderValue = this.displayToInternalMap.get(sliderValue);
            
            // Check if screenshot image for the current time determined by 
            // sliderValue exists in videoCache.
            // If no image is found, a blank (grey screen) preview player will
            // be displayed.
            if ((image = videoCache.getScreenshotImage(mappedSliderValue)) != null) {
                this.jpPreviewPlayer.setScreenshot(image);
            } else {
                log("Could not locate mapped screenshot for sliderValue " + sliderValue);
            }
        } else {
            log(String.format("Request Look up failed at %d", sliderValue));
        }
        
        this.jpPreviewPlayer.requestFocus();
        this.jfPreviewPlayer.showPlayer();
    }
    
    /**
     * Update the totalSeconds of the preview.
     * @param timeMS 
     */
    public void updatePreviewController(long timeMS) {
        this.totalSeconds  = (int) (timeMS / 1000);
        
        SwingUtilities.invokeLater(new Runnable() {
             @Override
             public void run() {
                 if (jpPreviewPlayer != null) {
                     jpPreviewPlayer.setSliderMax(totalSeconds);
                     jfPreviewPlayer.repaint();
                 }
             }
         });
    }
    
    /**
     * Returns True if video has been edited, in other words, if there 
     * have been any cuts. False, otherwise. 
     * @return Boolean if the video has been scrubbed
     */
    public synchronized boolean isVideoEdited() {
        return !this.scrubs.isEmpty();
    }
    
    /**
     * Hides the associated PreviewPlayer and resets preview data to the start
     * of the recording.
     */
    public void resetControls() {
        if (jfPreviewPlayer != null) {
            this.jfPreviewPlayer.hidePlayer();
            this.updateTimeLabels(0);
            this.jpPreviewPlayer.setSliderValue(0);
            this.jpPreviewPlayer.repaint();
        }
    }
    
    /**
     * Returns a hh:mm:ss or mm:ss representation of the input time in seconds.
     * @param time Time in seconds
     * @param displayHours true if hh is to be displayed
     * @return 
     */
    public String getClockTime(int time, boolean displayHours) {
        String format = String.format("%%0%dd", 2);
        
        int intHour = (int) time / 3600;
        int intMins = (int) ((time % 3600) / 60);
        int intSecs = (int) (time % 60);

        String hours   = String.format(format, intHour);
        String mins    = String.format(format, intMins);
        String seconds = String.format(format, intSecs);
        
        if (displayHours) {
            return String.format("%s:%s:%s",hours,mins,seconds);
        } else {
            return String.format("%s:%s", mins, seconds);
        }
    }
    
    /**
     * Returns a mm:ss representation of the input time in milliseconds.
     * @param time Time in milliseconds
     * @return 
     */
    public String getClockTimeMS(long timeMS, boolean displayHours) {
        long time = timeMS / 1000;
        String format = String.format("%%0%dd", 2);
        
        int intHour = (int) time / 3600;
        int intMins = (int) ((time % 3600) / 60);
        int intSecs = (int) (time % 60);

        String hours   = String.format(format, intHour);
        String mins    = String.format(format, intMins);
        String seconds = String.format(format, intSecs);
        
        if (displayHours) {
            return String.format("%s:%s:%s",hours,mins,seconds);
        } else {
            return String.format("%s:%s",mins,seconds);
        }
    }
    
    /**
     * Adds a cut to the requested time.
     */
    public void requestAddCut() {
        int requestTime = this.jpPreviewPlayer.getSliderValue();
        int currentTime = (int) (this.totalSeconds - this.clockOffset);
        if (requestTime < currentTime) {
            addCut(this.displayToInternalMap.get(requestTime), totalSeconds);
        }
    }
    
    /**
     * Stores a range of integers representing a period of time which is to 
     * be removed/scrubbed from the final video.
     * @param start start time in seconds 
     * @param end end time in seconds
     */
    public void addCut(int start, int end){
        VideoScrub contestingScrub;
        
        log("Adding cut at " + start + " ending " + end);
        // May not be needed, or may break in deployment code
        // keep a close eye on this next line
        this.totalSeconds = end;
        
        // Do not cut if start==end, 
        // that is just the user pausing the recording
        if (start == end) {
            return;
        }
        
        if (this.minCut == null) {
            this.minCut = start;
        }
        
        // If first cut or latest cut, assign end time to maxCut
        if(this.maxCut == null) {
            this.maxCut = end;
        }
        
        contestingScrub = new VideoScrub(start,end);
        // Search to see if subset of previous cut
        // If not, add cut        
        this.scrubs.add(contestingScrub);
        
        // Analyze and arrange cut
        processNewCut();
        postAddCut(start,end);
        updateScrubMaps();
    }
    
    /**
     * Removes overalapping scrubs resulting from the new cut.
     */
    private void processNewCut() {
        // Sort scrubs by starting times
        Collections.sort(this.scrubs, VideoScrubManager.sortByStartComparator);
        
        log(this.scrubs.toString());
        
        // If the scrub list at most one element, we don't to do anything so we
        // just return.
        if (scrubs.size() < 1) {
            return;
        }
        
        for (int i=0, j=1; j < this.scrubs.size(); i++, j++) {
            VideoScrub currScrub = this.scrubs.get(i);
            VideoScrub nextScrub = this.scrubs.get(j);
            
            // Transposition Check
            if (currScrub.end == nextScrub.start) {
                // Remove current scrub
                this.scrubs.remove(i);
                
                // Remove next scrub
                // Make sure we keep in bounds
                if (j < this.scrubs.size()) {
                    this.scrubs.remove(j);
                }
                
                // Add
                this.scrubs.add(i, new VideoScrub(currScrub.start, nextScrub.end));
                
                // Reset
                i--; j--;
            }
             
            // Subset check
            if (currScrub.start <= nextScrub.start && 
                    currScrub.end >= nextScrub.end) {
                // NextScrub is subset of CurrScrub
                this.scrubs.remove(j);
                
                // Do not increment to next
                i--; j--;
            }
        }
        
        log(String.format("Final Scrubs:%s", this.scrubs));
    } 
    
    /*
     * Updates the scrub index for the new cut, removing old scrubs and replaces
     * them with the new ones.
     * @param start
     * @param end 
     */
    private void postAddCut(int start, int end) {
        // Update screenshot flags
        for (int i=start; i <= end; i++) {
            videoCache.flagScreenshotsAt(i);
        }
        
        // Update scrub index
        this.scrubIndex.clear();
        for (VideoScrub scrub : this.scrubs) {
            for (int i=scrub.start; i < scrub.end; i++) {
                this.scrubIndex.add(i);                
            }
        }
        audioCache.setScrubs(scrubIndex);
        log("ScrubIndex " + this.scrubIndex);

        // Calculate new time index
        Long offset = 0L;
        for (VideoScrub scrub : this.scrubs) {
            log(String.format("OldOffset[%d] Start[%d] End[%d] NewOffset[%d]",
                    offset, scrub.start, scrub.end, (offset + (scrub.end-scrub.start))));
            offset += (scrub.end - scrub.start);
        }
        
        this.clockOffset = offset;
        this.jpRecorderPanel.setClockOffset(offset);
    }
    
    /**
     * Updates the display to internal and internal to display scrubbing maps.
     */
    public void updateScrubMaps() {
        // Generate new scrub map
        this.displayToInternalMap.clear();
        this.internalToDisplayMap.clear();
        
        log("Updating scrub maps");
        log("Total Seconds: "+ totalSeconds + " ClockOffset "+ clockOffset);
        
        int internalIndex = 0;
        int displayIndex  = 0;
        log("Scrubs: " + this.scrubs);
        if (this.scrubs.isEmpty()) {
            // Mapping when there are no edits done to the recording
            for (; displayIndex <= (totalSeconds - clockOffset); displayIndex++, internalIndex++) {
                this.displayToInternalMap.put(displayIndex, internalIndex);
                this.internalToDisplayMap.put(internalIndex, displayIndex);
            }
        } else {
            for (VideoScrub scrub : this.scrubs) {
                // Find the next non-scrubbed second
                for (; internalIndex < scrub.start; displayIndex++, internalIndex++) {
                    this.displayToInternalMap.put(displayIndex, internalIndex);
                    this.internalToDisplayMap.put(internalIndex, displayIndex);
                    log(String.format("Display[%d] Internal[%d]", displayIndex, internalIndex));
                }
                
                // Find the next non-scrubbed second
                for (; internalIndex < scrub.end; internalIndex++) {
                    // Search for next clip of video to preview
                }
                
                // Find the next non-scrubbed second
                for(; internalIndex <= scrub.end; displayIndex++, internalIndex++) {
                    this.displayToInternalMap.put(displayIndex, internalIndex);
                    this.internalToDisplayMap.put(internalIndex, displayIndex);
                    log(String.format("Display[%d] Internal[%d]", displayIndex, internalIndex));
                }
            }
            
            for(; displayIndex <= (totalSeconds-clockOffset); displayIndex++, internalIndex++) {
                this.displayToInternalMap.put(displayIndex, internalIndex);
                this.internalToDisplayMap.put(internalIndex, displayIndex);
                log(String.format("Display[%d] Internal[%d]", displayIndex, internalIndex));
            }
        }
        
        ArrayList<Integer> internalKeys = new ArrayList(this.internalToDisplayMap.keySet());
        Collections.sort(internalKeys);
        log("Scrub maps updated");
    }
    
    /**
     * Updates the recorded video with a newly recorded portion.
     * @param rawVideoPath Location of the raw video recording which is to be 
     * used to sample from.
     */
    public void scrubVideo(String rawVideoPath, FFMpegProgressBarListener progressBarListener) {
        // Get access to scrub info
        VideoScrub scrub  = null;
        File videoClip    = null;
        File rawVideoMPG  = null;
        File rawVideoMP4  = new File(rawVideoPath);
        File finalClipMPG = null;
        File finalClipMP4 = null;
        File handbrake    = null;
        File rawVideoMP4Backup = new File(rawVideoMP4.getParent()+Settings.FILE_SEP+"raw.mp4");
        
        // Not cut, no editing needed
        if (this.scrubs.isEmpty()) {
            return;
        }
        
        this.isScrubbing = true;
        
        // Grab reference to handbrake download
        DownloadThread downloadHandbrake = DownloadManager.getInstance().getDownload(Settings.getHandbrakeExecutable());

        log(String.format("RawMP4[%s], Exists?[%s]",rawVideoPath, rawVideoMP4.exists()));
        
        // Return if raw video mp4 file does not exist
        if (!rawVideoMP4.exists() || rawVideoMP4.length() == 0) {
            return;
        }
        
        // Convert video to MPG
        rawVideoMPG = FileUtil.convertMp4ToMpg(rawVideoMP4, progressBarListener);
        
        // Check to see if video converted with any errors 
        // Return if the resulting MPG file is not found
        if (!rawVideoMPG.exists() || rawVideoMPG.length() == 0) {
            return;
        }
        
        finalClipMPG = new File(Settings.SCREEN_CAPTURE_DIR + "merged.mpg");
        
        int rawVideoMpgLength = FileUtil.getVideoDuration(rawVideoMPG.getAbsolutePath());
        
        int duration;
        int offset  = 0;
        log("Number of cuts: " + this.scrubs.size());
        
        // Search for segments which are to be extracted from raw video
        for (int i = 0; i <= this.scrubs.size(); i++) {
            // Re-use the last cut info for clip
            if (i < this.scrubs.size()) {
                scrub = this.scrubs.get(i);
            }
            
            videoClip = new File(Settings.SCREEN_CAPTURE_DIR + "clip" + i + ".mpg");
            
            log(String.format("Scrubbing out %d to %d from %s", scrub.start, scrub.end, rawVideoMPG.getName()));
            
            if (i == this.scrubs.size()) { // Nth scrub
                System.out.println("Last Clip");
                // Keep video clip N->End
                duration = rawVideoMpgLength - scrub.end;
                offset   = scrub.end;
                log(String.format("clip[%d] offset[%d] duration[%d]", i, scrub.start, duration));
                
                // Keep video clip 0->N
                FileUtil.extractMpgLastClip(rawVideoMPG, videoClip, offset, duration, progressBarListener);
            } else { // 0, 1, 2, ... N-1 scrubs
                duration = scrub.start - offset;
                log(String.format("clip[%d] offset[%d] duration[%d]", i, scrub.start, duration));
                
                // Keep video clip 0->N
                FileUtil.extractMpgClip(rawVideoMPG, videoClip, offset, duration, progressBarListener);
                videoClip.deleteOnExit();
            }
            
            
            // Save video clip to memory to merge later
            FileUtil.appendBinary(videoClip, finalClipMPG);
            
            // N->next start
            offset = scrub.end;
        }
        
        // Wait till we are finish downloading our libraries
        while(!downloadHandbrake.checkStatus(DownloadStatus.FINISHED)){
            log("Waiting for the download of handbrake executable");
            TimeUtil.skipToMyLou(1);
        }
        
        handbrake = downloadHandbrake.getFile();
        LibraryUtil.chmod("777", handbrake);
        
        progressBarListener.setId(FFMpegProgressBarListener.HANDBRAKE);
        finalClipMP4 = FileUtil.convertMpgToMp4(finalClipMPG, progressBarListener);
        progressBarListener.setId(FFMpegProgressBarListener.FFMPEG);

        FileUtil.copyTo(rawVideoMP4, rawVideoMP4Backup);
        if (!rawVideoMP4.delete()) {
            log("Could not delete" + rawVideoMP4.getAbsolutePath() +" does not exists");
        }
        
        if (!finalClipMP4.renameTo(rawVideoMP4)) {
            log("Could not rename" + finalClipMP4.getAbsolutePath() + " to " + rawVideoMP4.getAbsolutePath() + "does not exists");
        }
        
        this.isScrubbing = false;
        log("Done");
    }
    
    /**
     * Sets the current time for the preview panel player
     * @param newTime 
     */
    public  void setPreviewTime(int newTime){
        final int time = newTime;
        
        if (jpPreviewPlayer == null) {
            // If there is no PreviewPlayer associated with this VideoScrubManager
            // we don't have anything to set into so we return.
            return;
        }
        
        // Set slider value to the selected time.
        jpPreviewPlayer.setSliderValue(time);
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Update clock value
                updateTimeLabels(time);
          }
        });
    }

    /**
     * Destroys the preview information and hides the PreviewPlayer. To be
     * called on close of application.
     */
    public void destroyPreviewVideo() {
        this.endPreviewVideo();
        
        if (this.jfPreviewPlayer != null) {
            this.jfPreviewPlayer.hidePlayer();
        }
        
        if (this.previewThread != null) {
            this.previewThread.dispose();
        }
    }
    
    /**
     * Stops preview playback and hides player from screen.
     */
    public void endPreviewVideo() {
        if (this.jfPreviewPlayer != null) {
            this.jfPreviewPlayer.hidePlayer();
        }
        
        if (this.previewThread != null) {
            this.previewThread.stopPlaying();
        }
    }
    
    /**
     * Starts preview playback by compiling the audio, bringing the video
     * player to view, and starting the thread which displays the 
     * screen shots to the preview player.
     */
    public void startPreviewVideo(){;
        this.audioCache.setCacheFile(new File(this.jpRecorderPanel.getRecorder().getWav()));
        
        // Reset the play time to zero if we are at the end.
        if (this.isPreviewTimeAtEnd()) {
            this.setPreviewTimeToStart();
        }
        
        // Get the selected time to start preview
        int start = this.jpPreviewPlayer.getSliderValue();
        
        try {
            // Update GUI slider component
            int displayToInternal = displayToInternalMap.get(start);
            start = displayToInternal;
            log(String.format("Starting Playback at %d=>%d", 
                    this.jpPreviewPlayer.getSliderValue(), start));
        } catch (NullPointerException e) {
            log(e);
        }
        
        // Display the preview player
        if (this.jfPreviewPlayer != null) {
            log("Repainting preview player");
            this.jfPreviewPlayer.showPlayer();
            jpPreviewPlayer.setSliderMax((int)(totalSeconds-clockOffset));
            jfPreviewPlayer.repaint();
        }
        
        updateScrubMaps();
        
        if (this.previewThread.isPlaying()) {
            // If the player is playing already, just modify the time
            log("Reseting preview to " + start);
            this.previewThread.resetTo(start);
        } else {
            // First time playing, start the thread
            log("Starting new preview at " + start);
            this.previewThread.startPlaying(start);
            
            // If preview thread is not running, fork thread
            if (!this.previewThread.isRunning()) {
                this.previewThread.start();
            }
        }
    }
    
    /**
     * Pauses the playback of the preview video.
     */
    public void pausePreviewVideo() {
        if (this.audioCache != null && audioCache.isPlaying()) {
            audioCache.stopAudio();
        }
        
        if (this.previewThread != null) {
            this.previewThread.pausePlaying();
        }
    }

    public ArrayList<VideoScrub> getScrubs() {
        // Only for JUnit tests
        return this.scrubs;
    }


    public void postScrubStartRecording() {
        this.jpRecorderPanel.prepareForRerecord();
    }

    /**
     * Finalizes the recorded video and prepares it to be encoded and uploaded
     * to Screenbird.
     */
    public void finalizeVideo(){
        this.backToRecordPanel();
        this.jpPreviewPlayer.hideRecordFromHerePanel();
        this.jpRecorderPanel.processVideo();
    }

    /**
     * Returns true if the preview player is open and recorded video is 
     * currently being previewed.
     * @return 
     */
    public boolean isPreviewing() {
        if (previewThread == null) {
            return false;
        } else {
            return this.previewThread.isPlaying() && this.jpPreviewPlayer.isVisible();
        }
    }

    /**
     * Hides the preview panel from screen and brings up the recorder panel controls.
     */
    public void backToRecordPanel() {
        this.endPreviewVideo();
        this.jpRecorderPanel.setVisible(true);
        this.jpRecorderPanel.showRecordingState();
    }
    
    /*
     * Not happy with this RecorderPanel, VideoScrubManager, PreviewPlayer
     * relationship, but RecorderPanel has the needed closeApp procedure.
     */
    public void requestCloseRecorder() {
        this.jpRecorderPanel.closeApp(false);
    }

    /**
     * Sets the current time of the preview to the end of the video.
     */
    public synchronized void setPreviewTimeToEnd() {
        this.setPreviewTime((int)(totalSeconds-clockOffset));
    }

    /**
     * Sets the current time of the preview to the start of the video.
     */
    public synchronized void setPreviewTimeToStart() {
        this.setPreviewTime(0);
    }

    /**
     * Checks if the current time of the preview is at the end of the video.
     * @return true if at end
     */
    public synchronized boolean isPreviewTimeAtEnd() {
        int sliderValue = jpPreviewPlayer.getSliderValue();
        return sliderValue == jpPreviewPlayer.getSliderMax();
    }

    /**
     * Sets flag for recording previous video.
     * @param recordingPreviousVideo 
     */
    public void setIsRecordingPreviousVideo(boolean recordingPreviousVideo) {
        this.jfPreviewPlayer.setIsRecordingPreviousVideo(recordingPreviousVideo);
    }

    /**
     * Displays or hides the spinner on the preview player to let the user know some 
     * process is running on the background.
     * @param isVisible 
     */
    public void setSpinnerVisibile(boolean isVisible) {
        this.jpPreviewPlayer.setSpinnerVisible(isVisible);
    }

    /**
     * Opens the preview player panel.
     */
    public void openPreviewPlayer() {
        if (this.previewThread == null) {
            this.previewThread = initPreviewThread();
        }
        
        // Brings player to view
        this.previewSliderAction();
        
        if (this.recorder.isLineOpen() && !this.recorder.isDroppedCountReached()) {
            // Disables preview till audio is ready
            this.setSpinnerVisibile(true);

            Thread compileAudio = new Thread(new Runnable() {
                public void run() {
                    while(jpRecorderPanel.getRecorder().isWaitingForAudio()){
                        TimeUtil.skipToMyLouMS(50L);
                    }

                    setSpinnerVisibile(false);
                }
            });
            compileAudio.start();
        }
    }

    /**
     * Based on whether or not the user recorder the video with audio 
     * (microphone available) or not.
     * @return 
     */
    private PreviewVideo initPreviewThread() {
        if (!this.recorder.isDroppedCountReached() && this.recorder.isLineOpen()) {
            // If we have audio to sync
            log("Previewing video with audio");
            return new PreviewVideoWithAudio();
        } else if(this.recorder.isDroppedCountReached()) {
            // If we have no audio to sync
            log("Previewing video with no audio");
            return new PreviewVideoNoAudio();
        } else {
            // Give some time so the audio can confirm connection satus
            TimeUtil.skipToMyLouMS(1000L);
            // Check audio connection again
            return initPreviewThread();
        }
    }

    /**
     * Daemon thread that controls previewing of recorded data. 
     */
    private abstract class PreviewVideo extends Thread {
        // Time variables for start and current times of screenshot display
        protected int start     = 0;
        protected int currTime  = 0;
        
        // If the screen shots are currently being displayed to the screen
        protected boolean isPlaying = false;
        // If the thread is ready to accept the video player controller actions
        protected boolean isPreviewReady = false;
        
        /**
         * Starts playing at the given time.
         * @param start Time to start preview, in seconds
         */
        protected abstract void startPlaying(int start);

        /**
         * Reset preview playback to the given time.
         * @param start Reset video player to given time, in seconds
         */
        public abstract void resetTo(int start);
        
        /**
         * Starts playing at where last ended.
         */
        public synchronized void continuePlaying() {
            this.isPlaying = true;
            this.currTime  = this.start;
            log("Continuing preview playback");
        }
        
        /**
         * Stops playing of preview video.
         */
        public synchronized void stopPlaying() {
            this.isPlaying = false;
            log("Stopping preview playback");
            if (audioCache.isPlaying()) {
                audioCache.stopAudio();
            }
        }
        
        /**
         * Pauses preview video playback.
         */
        public synchronized void pausePlaying() {
            this.isPlaying = false;
            // Bookmark when player is resumed
            this.start = currTime;
            if (audioCache.isPlaying()) {
                audioCache.stopAudio();
            }
        }
        
        /**
         * Is the video player currently previewing recording
         * @return True if video player is playing
         */
        public synchronized boolean isPlaying() {
            return this.isPlaying;
        }
        
        /**
         * Is video player thread active and ready for previewing
         * @return True if video player is ready for previewing
         */
        public synchronized boolean isRunning() {
            return this.isPreviewReady;
        }
        
        /**
         * Returns True if the video player is paused.
         * @return 
         */
        public synchronized boolean isPaused() {
            return !this.isPlaying;
        }
        
        /**
         * Stops preview thread from running. Essentially kills the thread.
         * This should be called when closing/restarting screen recorder.
         */
        public synchronized void dispose() {
            this.isPlaying = false;
            this.isPreviewReady = false;
            log("Disposing of PreviewThread");
        }
        
        private void log(Object message){
            LogUtil.log(PreviewVideo.class, message);
        }
    }
    
    /**
     * Daemon thread that controls previewing of recorded data. 
     */
    private class PreviewVideoWithAudio extends PreviewVideo {
        
        PreviewVideoWithAudio() {
            super();
            super.setName("Preview Video With Audio");
        }
        
        @Override
        protected synchronized void startPlaying(int start) {
            this.start     = start;
            this.isPlaying = true;
            
            // Start audio at given staring point plus audio/video recording offset
            audioCache.playAudioMS(
                    start * 1000 + jpRecorderPanel.getRecorder().getOffset());
        }

        @Override
        public synchronized void resetTo(int start) {
            this.start     = start;
            this.currTime  = start;
            this.isPlaying = true;
            
            
            // Start audio at given staring point plus audio/video recording offset
            audioCache.playAudioMS(
                    start * 1000 + jpRecorderPanel.getRecorder().getOffset());
        }
        
        
        @Override
        public void run() {
            // Mark preview as ready
            this.isPreviewReady = true;
            int internalToDisplay = 0;
            
            while (this.isPreviewReady) {
                // For each second from start to end of video, a list of images 
                // are read from memory the given second that is shown in the player. 
                // This is does not happen with complete linearity, but some 
                // seconds can be skipped over, or some screenshots will not be shown. 
                // This is entirely based off the current time of the audio playback.
                synchronized (this) {
                    currTime = start;
                }
                
                while (isPlaying && (currTime <= totalSeconds) && audioCache.isPlaying()) {
                    // Get all screenshots that make up the given second
                    ArrayList<BufferedImage> screenshots = videoCache.getScreenshotImages(currTime);
                    
                    // Throttle for small sets of screenshots
                    int maxNumScreenhots = Math.min(Settings.PREVIEW_MAX_NUM_SCREENSHOT, screenshots.size());
                    
                    int oldtime = currTime;
                    
                    try {
                        // Update GUI slider component
                        internalToDisplay = internalToDisplayMap.get(currTime);
                        setPreviewTime(internalToDisplay);
                        log("Setting time to " + internalToDisplay);
                        
                    } catch (NullPointerException e) {
                        log(e);
                        log("Attempting to access " + currTime);
                        log(internalToDisplayMap);
                        log(displayToInternalMap);
                    }
                    
                    for (int ignus=0; isPlaying && ignus < maxNumScreenhots && audioCache.isPlaying(); ignus++) {
                        if (audioCache.getCurrTimeSeconds() != oldtime) {
                            // Time was updated, move to next set of screenshots
                            break;
                        } 
                        
                        // Update screenshot in preview player
                        jpPreviewPlayer.setScreenshot(screenshots.get(ignus));
                        
                        // Dynamically determine the amount of time to wait for
                        // showing a single screenshot based off the number of 
                        // screenshot the set of screenshot for that second
                        long waitTime = (long) ((1.0/(maxNumScreenhots+1))*1000);
                        
                        // Wait
                        TimeUtil.skipToMyLouMS(waitTime);
                    }
                    
                    synchronized (this) {
                        currTime = audioCache.getCurrTimeSeconds();
                    }
                }
                
                jpPreviewPlayer.repaint();
                
                // If preview reached end of video playback, reset controls
                synchronized (this) {
                    if (isPlaying) {
                        audioCache.stopAudio();
                        jpPreviewPlayer.setToPlay();
                        setPreviewTimeToEnd();
                        isPlaying = false;
                    }
                }
                
                // Let some other resource run
                TimeUtil.skipToMyLouMS(500L);
            }
        }
        
        private void log(Object message){
            LogUtil.log(PreviewVideoWithAudio.class, message);
        }
        
    }
    
    /**
     * Daemon thread that controls previewing of recorded data. 
     */
    private class PreviewVideoNoAudio extends PreviewVideo {
        
        PreviewVideoNoAudio() {
            super();
            super.setName("Preview Video Without Audio");
        }

        @Override
        protected synchronized void startPlaying(int start) {
            this.start     = start;
            this.isPlaying = true;
        }
        
        @Override
        public synchronized void resetTo(int start) {
            this.start     = start;
            this.currTime  = start;
            this.isPlaying = true;
        }
        
        private synchronized void setCurrentTime(int time) {
            this.currTime = time;
        }
        
        
        @Override
        public void run() {
            // Mark preview as ready
            this.isPreviewReady = true;
            
            while (this.isPreviewReady) {
                synchronized (this) {
                    currTime = start;
                }
                
                while (isPlaying && (currTime <= totalSeconds)) {
                    // Get all screenshots that make up the given second
                    ArrayList<BufferedImage> screenshots = videoCache.getScreenshotImages(currTime);
                    
                    // Throttle for small sets of screenshots
                    int maxNumScreenhots = Math.min(Settings.PREVIEW_MAX_NUM_SCREENSHOT, screenshots.size());
                    
                    try {
                        // Update GUI slider component
                        int internalToDisplay = internalToDisplayMap.get(currTime);
                        setPreviewTime(internalToDisplay);
                        
                    } catch (NullPointerException e) {
                        log(e);
                        log("Attemting to access " + currTime);
                        log(internalToDisplayMap);
                        log(displayToInternalMap);
                    }
                    
                    for (int ignus = 0; isPlaying && ignus < maxNumScreenhots; ignus++) {
                        // Update screenshot in preview player
                        jpPreviewPlayer.setScreenshot(screenshots.get(ignus));
                        
                        // Dynamically determine the amount of time to wait for
                        // showing a single screenshot based off the number of 
                        // screenshot the set of screenshot for that second
                        long waitTime = (long) ((1.0/(maxNumScreenhots+1))*1000);
                        
                        // Wait
                        TimeUtil.skipToMyLouMS(waitTime);
                    }
                    
                    screenshots = null;
                    synchronized (this) {
                        currTime++;
                    } 
                }
                
                jpPreviewPlayer.repaint();
                
                // If preview reached end of video playback, reset controls
                if (isPlaying) {
                    jpPreviewPlayer.setToPlay();
                    setPreviewTimeToEnd();
                    isPlaying = false;
                }
                
                // Let some other resource run
                TimeUtil.skipToMyLouMS(500L);
            }
        }
        
        private void log(Object message) {
            LogUtil.log(PreviewVideoNoAudio.class, message);
        }
    }
    
    private static void log(Object message) {
        LogUtil.log(VideoScrubManager.class, message);
    }
}
