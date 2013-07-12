/*
 * AudioRecorder.java
 * 
 * Version 1.0
 * 
 * 8 May 2013
 */
package com.bixly.pastevid.recorders;

import com.bixly.pastevid.Settings;
import com.bixly.pastevid.models.AudioFileItem;
import com.bixly.pastevid.models.Silent;
import com.bixly.pastevid.screencap.components.IAudioObserver;
import com.bixly.pastevid.screencap.components.IAudioSubject;
import com.bixly.pastevid.util.FileUtil;
import com.bixly.pastevid.util.LogUtil;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Recorder class responsible for capturing audio on screen recording.
 * @author Bixly
 */
public class AudioRecorder implements IAudioSubject {
    public static final int MAX_VOLUME = 100000;

    // Attributes for the AudioRecorder
    private int  volume;
    private long startMS;
    private long lastFrame = 0;
    private long currentTimeStamp;
    
    // Components
    private AudioFormat    audioFormat;
    private AudioThread    audioThread;
    private AudioCache     audioCache;
    private Recorder       recorder;
    private TargetDataLine targetDataLine;
    
    private final static String recordingDir = Settings.SCREEN_CAPTURE_DIR;
    
    private Vector<AudioInputStream> audioInputStreams;
    private ArrayList<AudioFileItem> audioFiles;
    private ArrayList<IAudioObserver> observers = new ArrayList<IAudioObserver>();
    
    // State flags
    private boolean hasRecordedAudio = false;
    private boolean recording = false;
    private boolean compiling = false;
    private boolean savePrevData = false;
    private boolean firstSound = false;
    
    public AudioRecorder(Recorder recorder) {
        this.recorder = recorder;
        this.firstSound = true;
        this.recording = false;
        
        setVolume(0);
        
        this.lastFrame = 0;
        this.audioFormat = getAudioFormat();
        
        // Create a new collection for audio files
        this.setAudioFiles(new ArrayList<AudioFileItem>());
        
        this.audioCache = new AudioCache();
        this.openLine();
    }

    /**
     * Returns start time (of what?) in milliseconds.
     * @return 
     */
    public long getStartMS() {
        return startMS;
    }
    
    /**
     * Returns a silent audio.
     * @return 
     */
    private static BufferedInputStream getSilentWav() {
        File silentRealFile = Silent.copySilent(recordingDir);
        BufferedInputStream emptyAudioFile = getBufferedWav(silentRealFile);
        return emptyAudioFile;
    }

    /**
     * Returns the audio from a file as a BufferedInputStream.
     * @param file
     * @return 
     */
    private static BufferedInputStream getBufferedWav(File file) {
        BufferedInputStream audioFile = null;
        try {
            audioFile = new BufferedInputStream(new FileInputStream(file));
        } catch (Exception ex) {
        }
        return audioFile;
    }

    /**
     * Checks if the this AudioRecorder's targetDataLine is open.
     * @return 
     */
    public synchronized Boolean isLineOpen() {
        return (this.getTargetDataLine() != null && 
                this.getTargetDataLine().isOpen());
    }

    /**
     * Opens this AudioRecorder's targetDataLine.
     */
    public final void openLine() {
        try {
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            TargetDataLine tdl = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
            setTargetDataLine(tdl);
            getTargetDataLine().open(audioFormat, tdl.getBufferSize());
            getTargetDataLine().start();
            audioThread = new AudioThread();
            audioThread.start();
        } catch (LineUnavailableException e) {
            log(e);
        } catch (Exception e) {
            log(e);
        }
    }
    
    /**
     * Closes this AudioRecorder's targetDataLine using the closeLine() method.
     * Catches any exception that may be thrown while closing the.
     */
    public void dropLine() {
        try {
            this.closeLine();
            log("Close audioline on exit");
        } catch (Exception ex) {
        }
    }
            
    /**
     * Checks if the targetDataLine has reached its end and closes it.
     */
    private void checkLine() {
        if (this.lastFrame == this.getTargetDataLine().getFramePosition()) {
            if (this.getTargetDataLine().available() == 0) {
                this.getTargetDataLine().stop();
                this.getTargetDataLine().close();
                this.setTargetDataLine(null);
                setVolume(0);
            }
        } else {
            this.lastFrame = this.getTargetDataLine().getFramePosition();
        }
    }

    /**
     * Monitors the targetDataLine for this AudioRecorder. Opens the 
     * targetDateLine if it is closed, and closes it if it has reached its end.
     */
    public void monitorOpenLine() {
        try {
            if (this.getTargetDataLine() != null && this.getTargetDataLine().isOpen()) {
                checkLine();
            } else {
                openLine();
            }
        } catch (Exception e) {
            log("Audio stop error");
        }
    }

    /**
     * Adds a silent audio to the recorded audio as padding to fix the difference
     * between the actual and targeted start times.
     * @param startTimestamp
     * @param correctStartTimestamp
     * @return
     * @throws UnsupportedAudioFileException 
     */
    public static AudioInputStream addEmptySound(long startTimestamp, long correctStartTimestamp) 
            throws UnsupportedAudioFileException {
        
        AudioInputStream emptyAudioStream = null;
        AudioFormat format = null;
        
        long prevLength = 0;
        long secondsRounded = (long) Math.ceil((((double) (startTimestamp - correctStartTimestamp)) / 1000.0));        
        long realMillis = startTimestamp - correctStartTimestamp;        
        
        log("silent duration rounded: " + secondsRounded);
        log("silent duration millis: " + realMillis);
        log("current file start:" + startTimestamp);
        log("current file correct start :" + correctStartTimestamp);
       
        try {
            Vector<AudioInputStream> emptyStreams = new Vector<AudioInputStream>();
            for (int i = 0; i < secondsRounded; i++) {
                InputStream emptyAudioFileWav = getSilentWav();
                if (emptyAudioFileWav != null) {
                    emptyAudioStream = AudioSystem.getAudioInputStream(emptyAudioFileWav);
                    prevLength += emptyAudioStream.getFrameLength();
                    format = emptyAudioStream.getFormat();
                    emptyStreams.add(emptyAudioStream);
                }
            }
            if (emptyStreams.size() > 0) {
                String empty = recordingDir + FileUtil.addExtension(String.valueOf(System.currentTimeMillis()), "empty");
                File file = new File(empty);
                AudioRecorder.compileAudioStreams(emptyStreams, file, format, prevLength);
                AudioRecorder.cutAudioFile(file, realMillis);
                emptyAudioStream = AudioSystem.getAudioInputStream(file);
            }
        } catch (IOException ex) {
        }
         log("silent length in millis after cut: " + String.valueOf( emptyAudioStream.getFrameLength()/emptyAudioStream.getFormat().getFrameRate()));
         log("===================");
        return emptyAudioStream;
    }

    /**
     * 
     * @param audioFile
     * @param audioLength
     * @throws UnsupportedAudioFileException
     * @throws IOException 
     */
    public static void cutAudioFile(File audioFile, long audioLength) 
            throws UnsupportedAudioFileException, IOException {
        
        AudioInputStream soundFile = AudioSystem.getAudioInputStream(audioFile);
        AudioFormat      format    = soundFile.getFormat();
        
        double frameLength = audioLength / 1000.0 * format.getFrameRate();
        long   totalBytes  = (long) (frameLength * format.getFrameSize());
        
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStream outputStream = byteArrayOutputStream;
        byte[] sampledData = new byte[format.getFrameSize()];
        
        ByteArrayInputStream byteArrayInputStream;
        AudioInputStream audioInputStream;
        byte[] abData;
        
        while (soundFile.read(sampledData) != -1) {
            outputStream.write(sampledData);
        }
        
        abData = byteArrayOutputStream.toByteArray();
        byteArrayInputStream = new ByteArrayInputStream(abData);

        audioInputStream = new AudioInputStream(byteArrayInputStream, format, totalBytes / format.getFrameSize());
        try {
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, audioFile);
        } catch (IOException e) {
        }
        
        soundFile.close();
    }

    /**
     * 
     * @param streams
     * @param file
     * @param format
     * @param length
     * @throws IOException 
     */
    public static void compileAudioStreams(Vector<AudioInputStream> streams, File file, AudioFormat format, long length) 
            throws IOException {
        
        AudioInputStream appendedFiles = new AudioInputStream(new SequenceInputStream(streams.elements()), format, length);
        AudioSystem.write(appendedFiles, AudioFileFormat.Type.WAVE, file);

        for (int i=0; i < streams.size(); i++) {
            AudioInputStream audioStream = streams.get(i);
            audioStream.close();
        }
    }

    /**
     * 
     * @param audioFileName 
     */
    public void compileAudio(String audioFileName) {
        if (this.getAudioFiles().size() > 0) {
            try {
                AudioFormat format = null;
                int audioFilesSize = this.getAudioFiles().size();

                for (int i = 0; i < audioFilesSize; i++) {
                    if (i < audioFilesSize - 1) {
                        AudioFileItem audioFileItem = this.getAudioFiles().get(i);

                        File audioFile = new File(audioFileItem.getName());
                        if (audioFile.isFile()) {
                            long audioLength = audioFileItem.getEndMS() - audioFileItem.getStartMS();
                            AudioRecorder.cutAudioFile(audioFile, audioLength);
                        }
                    }
                }


                File file = new File(audioFileName);
                this.audioInputStreams = new Vector<AudioInputStream>();

                long length = 0;
                long correctStartTimestamp = 0;

                // Iterate over audio files
                for (int i = 0; i < audioFilesSize; i++) {
                    AudioFileItem audioFileItem = this.getAudioFiles().get(i);
                    File audioFile = new File(audioFileItem.getName());
                    if (audioFile.isFile()) {
                        AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                        
                        // Checks if we need to add empty sound before adding the sound file
                        if (audioFileItem.getStartMS() > correctStartTimestamp && audioFileItem.isPreviousDropped()) {
                            // The correct start time for the audio file should 
                            // be 0 if it's the first file or equals the end 
                            // time of the prev audio file
                            AudioInputStream audioStreamEmpty = addEmptySound(audioFileItem.getStartMS(), correctStartTimestamp);
                            length += audioStreamEmpty.getFrameLength();
                            this.audioInputStreams.add(audioStreamEmpty);
                        }
                        
                        correctStartTimestamp = audioFileItem.getStartMS() + 
                                (long) (1000 * audioStream.getFrameLength() / audioStream.getFormat().getFrameRate());
                        format = audioStream.getFormat();
                        length += audioStream.getFrameLength();
                        this.audioInputStreams.add(audioStream);
                    }
                }
                compileAudioStreams(this.audioInputStreams, file, format, length);
            } catch (IOException ex) {
                log(ex);
            } catch (UnsupportedAudioFileException e){
                log(e);
            }
        }
    }

    /**
     * Flag this AudioRecorder as recording.
     */
    public void record() {
        this.recording = true;
    }

    /**
     * Closes this AudioRecorder's targetDataLine.
     * @throws SecurityException 
     */
    public void closeLine() throws SecurityException {
        if (this.getTargetDataLine() != null && this.getTargetDataLine().isOpen()) {
            this.getTargetDataLine().stop();
            this.getTargetDataLine().close();
        }
    }

    /**
     * Flag this AudioRecorder as not recording.
     */
    public synchronized void stopRecording() {
        this.recording = false;
    }
    
    /**
     * Returns the recording status of this AudioRecorder.
     * @return True if recording<BR/> False otherwise
     */
    public synchronized boolean isRecording() {
        return this.recording;
    }

    /**
     * Checks if this AudioRecorder has recorded audio.
     * @return 
     */
    public synchronized boolean hasRecorded() {
        return this.hasRecordedAudio;
    }
    
    /**
     * Generates an audio file item for the recording.
     * @param postfix
     * @param dropped
     * @param previousDropped
     * @return 
     */
    private AudioFileItem generateAudioFileItem(long postfix, boolean dropped, boolean previousDropped) {
        String audioFile = recordingDir + FileUtil.addExtension(String.valueOf(System.currentTimeMillis()), "audio");
        AudioFileItem audioFileItem = new AudioFileItem();
        audioFileItem.setName(audioFile);
        audioFileItem.setDropped(dropped);
        audioFileItem.setPreviousDropped(previousDropped);
        audioFileItem.setTimestamp(postfix);
        this.getAudioFiles().add(audioFileItem);
        return audioFileItem;
    }

    /**
     * Returns the following AudioFormat:
     * sampleRate           = 16000.0F
     * sampleSizeInBits     = 16
     * channels             = 1
     * signed               = true
     * bigEndian            = true
     * @return 
     */
    public static AudioFormat getAudioFormat() {
        return new AudioFormat(16000.0F, 16, 1, true, true);
    }

    /**
     * Returns the audio files recorded by this AudioRecorder
     * @return 
     */
    public ArrayList<AudioFileItem> getAudioFiles() {
        return this.audioFiles;
    }

    /**
     * Sets the audio files recorded by this AudioRecorder
     * @param audioFiles 
     */
    public final void setAudioFiles(ArrayList<AudioFileItem> audioFiles) {
        this.audioFiles = audioFiles;
    }

    /**
     * Returns the target data line for this AudioRecorder.
     * @return 
     */
    public TargetDataLine getTargetDataLine() {
        return this.targetDataLine;
    }

    /**
     * Sets the target data line for this AudioRecorder.
     * @param targetDataLine 
     */
    private void setTargetDataLine(TargetDataLine targetDataLine) {
        this.targetDataLine = targetDataLine;
    }

    /**
     * Sets the input volume for this AudioRecorder
     * @param vol 
     */
    private void setVolume(int vol) {
        this.volume = vol;
    }

    /**
     * Returns the input volume for this AudioRecorder
     * @return 
     */
    public int getVolume() {
        return this.volume;
    }

    /**
     * Calculates the Root Mean Square level of the audio.
     * @param audioData
     * @return 
     */
    public static int calculateRMSLevel(byte[] audioData) {
        double averageMeanSquare = 0;
        double sumMeanSquare = 0d;
        for (int j = 0; j < audioData.length; j++) {
            sumMeanSquare += Math.abs(audioData[j]);
        }
        averageMeanSquare = (sumMeanSquare / audioData.length) / 127;

        int intVol = (int) (averageMeanSquare * 100);
        double vol = Math.pow(1.5, intVol);

        if (vol > MAX_VOLUME) {
            intVol = MAX_VOLUME;
        } else {
            intVol = (int) vol;
        }
        return intVol;
    }

    /**
     * Returns the audio observers.
     * @return 
     */
    public ArrayList<IAudioObserver> getObservers() {
        return this.observers;
    }
    
    /**
     * Adds an observer to the list of audio observers.
     * @param o 
     */
    public void addObserver(IAudioObserver o) {
        this.observers.add(o);
    }

    /**
     * Remove an observer from the list of audio observers.
     * @param o 
     */
    public void removeObserver(IAudioObserver o) {
        this.observers.remove(o);
    }

    /**
     * Notify the observers that this AudioRecorder is compiling.
     */
    public void notifyObservers() {
        Iterator<IAudioObserver> iterator = this.observers.iterator();
        while (iterator.hasNext()) {
            IAudioObserver observer = iterator.next();
            observer.update(this);
        }
    }

    /**
     * Returns the compiling status flag.
     * @return True if this AudioRecorder is compiling
     */
    public synchronized Boolean isCompiling() {
        return this.compiling;
    }

    /**
     * Sets the compiling status flag.
     * @param compiling 
     */
    public synchronized void setCompiling(Boolean compiling) {
        this.compiling = compiling;
        notifyObservers();
    }

    /**
     * Sets the savePrevData status flag for determining if this AudioRecorder
     * has saved the audio to a file.
     * @param value 
     */
    public synchronized void saveAudioItemToFile(Boolean value) {
        this.savePrevData = value;
    }

    /**
     * Saves the recorded files to the file pointed by this AudioRecorder's
     * Recorder.AUDIO_DATA.
     */
    public void saveAudioItemToFileNow() {
        FileUtil.saveObjectDataToFile(this.audioFiles, Recorder.AUDIO_DATA);
    }
    
    /**
     * Resets the audio files and status flags of this AudioRecorder.
     */
    public void resetData() {
       this.audioFiles       = new ArrayList<AudioFileItem>();
       this.hasRecordedAudio = false;
       this.savePrevData     = false;
    }
    
    public AudioCache getCache() {
        return this.audioCache;
    }

    public static void log(Object message) {
        LogUtil.log(AudioRecorder.class, message);
    }

    /**
     * Thread for recording audio.
     */
    class AudioThread extends Thread {
        public AudioThread() {
            super("AudioThread");
        }
        
        AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStream outputStream = byteArrayOutputStream;
        AudioFormat format = getTargetDataLine().getFormat();
        
        int nFrameSize   = format.getFrameSize();
        byte[] abBuffer  = new byte[getTargetDataLine().getBufferSize() / nFrameSize];
        boolean recorded = false;
        
        @Override
        public void run() {
            // If audio line is open
            while (getTargetDataLine() != null && getTargetDataLine().isOpen()) {
                // Read in the data to abBuffer and record number of frames
                int nFramesRead = getTargetDataLine().read(abBuffer, 0, abBuffer.length);
                
                if (nFramesRead == 0) {
                    setVolume(0);
                    log("Not able to read any audio input.");
                } else {
                    setVolume(calculateRMSLevel(abBuffer));
                }

                if (recording) {
                    hasRecordedAudio = true;
                    if (!recorded) {
                        if (firstSound) {
                            startMS = System.currentTimeMillis();
                            firstSound = false;
                        }
                        log("Start Audio " + System.currentTimeMillis());
                        currentTimeStamp = recorder.getMillisecondsTime();
                    }
                    try {
                        outputStream.write(abBuffer, 0, nFramesRead);
                    } catch (IOException e) {
                    }
                    recorded = true;
                }
                
                // If Done with recording
                if (!recording && recorded) {
                    if (byteArrayOutputStream.size() > 0) {
                        writeBufferToFile(false);
                    }
                    recorded = false;
                }
            }
            
            // Line isn't open but there is still some buffer to save
            if (recorded && byteArrayOutputStream.size() > 0) {
                writeBufferToFile(true);
            }
            setVolume(0);
        }

        private void writeBufferToFile(boolean lineDropped) {
            // Mark to compile state
            setCompiling(true);
            
            long    endTime = recorder.getMillisecondsTime();
            boolean previousDropped;
            
            if (!getAudioFiles().isEmpty()) {
                AudioFileItem audioFileItem = getAudioFiles().get(getAudioFiles().size() - 1);
                previousDropped = audioFileItem.isDropped();
            } else {
                previousDropped = (currentTimeStamp != 0);
            }
            
            AudioFileItem audioFileItem = generateAudioFileItem(currentTimeStamp, lineDropped, previousDropped);
            
            // Mark bounds for this audio file
            audioFileItem.setStartMS(currentTimeStamp);
            audioFileItem.setEndMS(endTime);

            File audioFile = new File(audioFileItem.getName());
            
            try {
                byteArrayOutputStream.close();
            } catch (IOException e) {}
            
            byte[] abData = byteArrayOutputStream.toByteArray();
            
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(abData);
            AudioInputStream audioInputStream         = new AudioInputStream(byteArrayInputStream, format, abData.length / format.getFrameSize());
            
            try {
                AudioSystem.write(audioInputStream, fileType, audioFile);
            } catch (IOException e) {}
            
            try {
                audioInputStream.close();
            } catch (Exception e) {}
            
            // Reset 
            recorded = false;
            byteArrayOutputStream = new ByteArrayOutputStream();
            outputStream          = byteArrayOutputStream;
            if(savePrevData)  FileUtil.saveObjectDataToFile(audioFiles, Recorder.AUDIO_DATA);
            
            setCompiling(false);           
        }
    }
}