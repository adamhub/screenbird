/*
 * AudioCache.java
 * 
 * Version 1.0
 * 
 * 8 May 2013
 */
package com.bixly.pastevid.recorders;

import com.bixly.pastevid.util.LogUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Cache for the audio component of the screen recording.
 * @author cevaris
 */
public class AudioCache  {
    /**
     * File pointer to which the audio is temporarily stored for preview playback.
     */
    private File cacheFile = null;
    
    /**
     * OutputStream for writing into the cache file.
     */
    FileOutputStream cacheStream = null;
    
    /**
     * Audio format, needs to match recording audio.
     */
    private final AudioFormat audioFormat = AudioRecorder.getAudioFormat();
    
    /**
     * Hardware access buffer to audio line.
     */
    private SourceDataLine dataLine = null;
    
    /**
     * True if audio is currently being played.
     */
    private boolean isPlaying = false;
    
    /**
     * Thread which audio playback is implemented.
     */
    private Thread playThread = null;

    /**
     * Current offset in the file which holding the temporary audio data for 
     * preview video. 
     */
    private long currOffset  = 0L;
    
    /**
     * Starting time which the audio is to start playing. 
     */
    private long startTimeMS = 0L;    
    
    /**
     * The current time of the audio playback. 
     */
    private long currTimeMS  = 0L;

    private ArrayList<Integer> scrubIndex = null;
    private long totalTimeMS = 0L;
    
    /**
     * Sets the file where the audio will be temporarily stored for preview
     * playback.
     * @param file File pointer where the audio cache will be stored.
     */
    public synchronized void setCacheFile(File file) {
        this.cacheFile = file;
        
        try {
            if (!this.cacheFile.exists()) {
                // Create a new cache file when setting for the first time
                this.cacheFile.createNewFile();
                this.cacheStream = new FileOutputStream(this.cacheFile);
            } else {
                // Append audio to existing cache file
                this.cacheStream = new FileOutputStream(this.cacheFile, true);
            }
        } catch (IOException e) {
            log(e);
        }
        
        log(String.format("Setting CacheFile[%s] [%s]",
            this.cacheFile.getAbsolutePath(), this.cacheFile.exists()));
    }

    /**
     * Returns the current offset which the audio is reading from the audio 
     * cache file.
     * @return Offset of audio cache file
     */
    public synchronized long getCurrOffset() {
        return currOffset;
    }

    /**
     * Returns the current time of audio playback in milliseconds. 
     * Used to sync timing of screen shots in the scrub feature's preview video. 
     * @return Current time of audio playback in milliseconds
     */
    public synchronized long getCurrTimeMS() {
        return currTimeMS;
    }
    
    /**
     * Returns the current time of audio playback in seconds.
     * @return Current time of audio playback in seconds
     */
    public synchronized int getCurrTimeSeconds() {
        return (int) (currTimeMS/1000);
    }
        
    /**
     * Stops audio playback.
     */
    public synchronized void stopAudio() {
        // Do nothing if already stopped
        if (!this.isPlaying()) {
            return;
        }
        
        // Trigger audio stop
        this.isPlaying = false;
        
        // Kill the connection to hardware audio line
        if (this.dataLine != null) {
            this.dataLine.stop();
            this.dataLine.close();
            this.dataLine = null;
        }
    }
    
    /**
     * Thread safe call to check if the audio is currently playing sound. 
     * @return True if audio is currently playing
     */
    public synchronized boolean isPlaying() {
        return this.isPlaying;
    }
    
    /**
     * Starts audio playback with millisecond accuracy.
     * @param timeMS Time in milliseconds which audio is to start playing
     */
    public synchronized void playAudioMS(long timeMS) {
        this.startTimeMS = timeMS;
        this.playAudio();
        log("Request play audio at time "+this.startTimeMS);
    }
    
    /**
     * Sets the scrub index for this AudioCache.
     * Used to synchronize audio with video playback.
     * @param scrubIndex 
     */
    public synchronized void setScrubs(ArrayList<Integer> scrubIndex) {
        this.scrubIndex = scrubIndex;
    }
    
    /**
     * Thread for playing audio.
     */
    private synchronized void playAudio() {       
        try {
            // Start a new thread for playing audio
            if (this.playThread != null) {
                this.playThread = null;
            }
            
            if (this.cacheStream != null) {
                // Release for reading
                this.cacheStream.flush();
                this.cacheStream.close();
                this.cacheStream = null;
            }
            
            // Load audio cache 
            log(String.format("Loading audio cache %s %d", 
                    this.cacheFile.getAbsolutePath(), 
                    this.cacheFile.length()));
            final FileInputStream input = new FileInputStream(this.cacheFile);
            log("Loaded audio cache file with size" + input.available());
            
            // Set up hardware for playback
            try {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(this.cacheFile);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioStream.getFormat());
                dataLine = (SourceDataLine) AudioSystem.getLine(info);
            } catch (IOException e) {
                log(e);
            } catch (UnsupportedAudioFileException e) {
                log(e);
            }
            
            // Set up audio playback thread.
            Thread runner = new Thread("Preview Audio Thread") {
                @Override
                public void run() {
                    try {
                        // Marks audio in playing state
                        isPlaying = true;
                        
                        // Prep hardware for audio playback
                        dataLine.open();
                        dataLine.start();
                       
                        // Read data for playing audio
                        int bytesRead = 0;
                        int buffSize  = dataLine.getBufferSize();
                        byte[] data   = new byte[buffSize];
                        
                        // Compute total time of audio playback
                        totalTimeMS = (long)((input.available()*1000) / ((double)audioFormat.getFrameSize() * audioFormat.getSampleRate()));
                        log("TimeMS: " + (startTimeMS));
                        
                        // Prep offsets for accurate audio playback
                        currOffset = (long)((startTimeMS/1000) * (double)audioFormat.getFrameSize() * audioFormat.getSampleRate());
                        currTimeMS = startTimeMS;
                        log(String.format("Seek to MS[%d] Bytes[%d] TotalBytes[%d]", startTimeMS, currOffset, input.available()));
                                               
                        // If not starting at begining of audio file
                        input.skip(currOffset);
                                
                        // Play the entire audio
                        while ((bytesRead = input.read(data,0,data.length)) != -1 
                                && isPlaying) {
                            currOffset += bytesRead;
                            
                            // Update current time of audio that is being played
                            currTimeMS = (long)((currOffset*1000) / ((double)audioFormat.getFrameSize() * audioFormat.getSampleRate()))-600;
                         
                            // Check to see if sequence has been scrubbed
                            if (scrubIndex != null && !scrubIndex.isEmpty() &&
                                // Is current second in scrub index array
                                scrubIndex.indexOf((int)(currTimeMS/1000)) >= 0) {
                                    // Do not write to audio line
                                    continue;
                            } 
                            
                            // Write to audio line
                            dataLine.write(data, 0, data.length);
                        } 
                        
                        if (isPlaying && dataLine != null) {
                            dataLine.drain(); 
                            dataLine.flush();
                        }
                        
                        // Kills video feed
                        currTimeMS = totalTimeMS;
                        isPlaying = false;
                        
                        if (dataLine != null) {
                            dataLine.stop();
                        }
                        log("Done with Audio");
                    } catch (LineUnavailableException e) {
                        log("No sound line available!" + e);
                    } catch (IOException e) {
                        log(e);  
                    } finally {
                        // Release audio playback hardware
                        try {
                            dataLine.close();
                        } catch (NullPointerException e){
                            // This always throws an exception for some reason
                        }
                        
                        try {
                            input.close();
                        } catch (IOException e) {
                            log(e);
                        } catch (NullPointerException e) {
                            // Do nothing
                        }
                    }
                    
                    // Stop running playback thread
                    this.interrupt();
                    playThread = null;
                }
            };
            
            // Start audio playback thread
            playThread = null;
            playThread = runner;
            playThread.start();
        } catch (LineUnavailableException e) {
            System.err.println("Line unavailable: " + e);
            System.exit(-4);
        } catch (FileNotFoundException e) {
            log(e);
        } catch (IOException e) {
            log(e);
        } 
    }
    
    private void log(Object message) {
        LogUtil.log(AudioCache.class, message);
    }
}
