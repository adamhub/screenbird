/*
 * JAVE - A Java Audio/Video Encoder (based on FFMPEG)
 * 
 * Copyright (C) 2008-2009 Carlo Pelliccia (www.sauronsoftware.it)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.sauronsoftware.jave.locators;

import com.bixly.pastevid.Settings;
import com.bixly.pastevid.download.DownloadFFMpeg;
import com.bixly.pastevid.download.DownloadManager;
import com.bixly.pastevid.download.DownloadStatus;
import com.bixly.pastevid.download.DownloadThread;
import com.bixly.pastevid.util.LogUtil;
import com.bixly.pastevid.util.MediaUtil;
import com.bixly.pastevid.screencap.components.progressbar.FFMpegProgressBarListener;
import com.bixly.pastevid.screencap.components.progressbar.FFMpegTask;
import com.bixly.pastevid.util.TimeUtil;
import it.sauronsoftware.jave.Encoder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * A ffmpeg process wrapper.
 * 
 * @author Carlo Pelliccia
 */
public class FFMPEGExecutor {

    /**
     * The path of the ffmpeg executable.
     */
    private String ffmpegExecutablePath;
    /**
     * Arguments for the executable.
     */
    private ArrayList args = new ArrayList();
    /**
     * The process representing the ffmpeg execution.
     */
    private Process ffmpeg = null;
    /**
     * A process killer to kill the ffmpeg process with a shutdown hook, useful
     * if the jvm execution is shutted down during an ongoing encoding process.
     */
    private ProcessKiller ffmpegKiller = null;
    /**
     * A stream reading from the ffmpeg process standard output channel.
     */
    private InputStream inputStream = null;
    /**
     * A stream writing in the ffmpeg process standard input channel.
     */
    private OutputStream outputStream = null;
    /**
     * A stream reading from the ffmpeg process standard error channel.
     */
    private InputStream errorStream = null;
    /**
     * For monitoring progress of one or more tasks
     */
    FFMpegProgressBarListener ffmpegListener = null;

    /**
     * It build the executor.
     * 
     * @param ffmpegExecutablePath
     *            The path of the ffmpeg executable.
     */
    public FFMPEGExecutor(String ffmpegExecutablePath) {
        this.ffmpegExecutablePath = ffmpegExecutablePath;
    }

    /**
     * Adds an argument to the ffmpeg executable call.
     * 
     * @param arg
     *            The argument.
     */
    public void addArgument(String arg) {
        args.add(arg);
    }

    /**
     * Checks if FFmpeg is executable
     * @return 
     */
    public boolean isExecutable() {
        
        assert this.ffmpegExecutablePath != null : "First define an executable path";
        
        boolean canExecute = false;
        try {
            File file = new File(this.ffmpegExecutablePath);
            canExecute = file.canExecute();
        } catch (SecurityException e) {
            log(e);
        } finally {
            return canExecute;
        }
    }

    /**
     * Returns if the ffmpeg executable exists on the file path given and
     * is not corrupted.
     * 
     * @return 
     */
    public boolean validateExecutable() {
        
        assert this.ffmpegExecutablePath != null : "First define an executable path";
        
        try {
            File file = new File(this.ffmpegExecutablePath);
            return (file.exists() && file.length() > 0);
        } catch (SecurityException e) {
            log(e);
        }
        return false;
    }

    public String getExecutablePath() {
        assert this.ffmpegExecutablePath != null : "First define an executable path";
        
        return this.ffmpegExecutablePath;
    }

    /**
     * Sets FFmpeg as executable
     */
    public void setAsExecutable() {
        new File(this.ffmpegExecutablePath).setExecutable(true);
    }

    /**
     * Executes the ffmpeg process with the previous given arguments.
     * 
     * @throws IOException
     *             If the process call fails.
     */
    public void execute() throws IOException {
        execute(true);
    }

    /**
     * Executes the ffmpeg process with the previous given arguments.
     * OutputLog boolean is used for tracking progress of each ffmpeg
     * execution. True if you want to catch and parse the data here
     * in execte(), of false if you are going to manually catch the 
     * output in another function
     * 
     * @throws IOException
     *             If the process call fails.
     */
    public void execute(boolean outputLog) throws IOException {


        DownloadThread downloadFFMpeg = 
                DownloadManager.getInstance().getDownload(Settings.getFFMpegExecutable());
        
        //Last check for FFMpeg to see if it is finished downloading
        while(!downloadFFMpeg.checkStatus(DownloadStatus.FINISHED)){
            log("Waiting for ffmpeg to finish downloading");       
            TimeUtil.skipToMyLouMS(500L);
        }


        int argsSize = args.size();
        String[] cmd = new String[argsSize + 1];
        cmd[0] = this.ffmpegExecutablePath;
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < argsSize; i++) {
            cmd[i + 1] = (String) args.get(i);
            str.append(args.get(i));
            str.append(" ");
        }
        log(str.toString());

        Runtime runtime = Runtime.getRuntime();
        ffmpeg = runtime.exec(cmd);
        ffmpegKiller = new ProcessKiller(ffmpeg,"FFmpeg Process Killer");
        runtime.addShutdownHook(ffmpegKiller);
        inputStream = ffmpeg.getInputStream();
        outputStream = ffmpeg.getOutputStream();
        errorStream = ffmpeg.getErrorStream();


        if (this.ffmpegListener != null && outputLog) {
            String line = "";
            InputStreamReader isr = new InputStreamReader(errorStream);
            BufferedReader br = new BufferedReader(isr);
            while ((line = br.readLine()) != null) {
                //log(line);

                parseTimeInfo(line);

                if (Settings.PRINT_EXEC_TO_CONSOLE) {
                    System.out.println(line);
                }
            }

            //Complete task
            this.ffmpegListener.taskComplete();
        }
    }

    public void setProgressBarListener(FFMpegProgressBarListener ffmpegListener) {
        this.ffmpegListener = ffmpegListener;
    }

    public FFMpegProgressBarListener getFFMpegProgressBarListener() {
        return ffmpegListener;
    }

    /**
     * Parses a line of data looking for the signals that mark the 
     * current progress of the ffmpeg exec call.
     * @param line 
     */
    public void parseTimeInfo(String line) {
        parseTimeInfoFFMpeg(line, -1);
    }

    /**
     * Parses a line of data looking for the signals that mark the 
     * current progress of the ffmpeg exec call.
     * @param line String to be parsed for information
     * @param durationOverride Override the duration that which this process
     * is supposed to run. Actually a hack, but dont mind me...
     */
    public void parseTimeInfoFFMpeg(String line, int durationOverride) {

        Matcher matcher;
        FFMpegTask task;
        int time = 0;
        int prevProgress;
        int nextProgress;

        matcher = Encoder.TIME_FFMPEG.matcher(line.toString());
        //Parse output
        if (this.ffmpegListener != null && matcher.find()) {
            //Read in match

            //if(MediaUtil.osIsMac() || MediaUtil.osIsUnix()){
            if (MediaUtil.osIsMac()) {
                //Mac or Unix
                time += Integer.parseInt(matcher.group(1)) * 3600;
                time += Integer.parseInt(matcher.group(2)) * 60;
                time += Integer.parseInt(matcher.group(3));
            } else {
                //Windows
                time += Integer.parseInt(matcher.group(1));
            }

            //log("Time: "+time);
            //Compute time delta
            task = this.ffmpegListener.getWorkingTask();
            prevProgress = task.getProgress();
            task.setSeconds(time);
            nextProgress = task.getProgress();
            //Update progresss bar
            this.ffmpegListener.setProgressByDelta(nextProgress - prevProgress);
            //Update task

            task.getProgress();
            //log("Progress: "+task.getSeconds()+" of "+task.getDuration()+" = "+task.getProgress()+"%");
        }

        matcher = Encoder.DURATION_FFMPEG.matcher(line.toString());
        //Parse output
        if (this.ffmpegListener != null && matcher.find()) {
            //Read in match

            time += Integer.parseInt(matcher.group(1)) * 3600;
            time += Integer.parseInt(matcher.group(2)) * 60;
            time += Integer.parseInt(matcher.group(3));

            //log("Duration: "+time);
            task = this.ffmpegListener.getWorkingTask();
            //log("Starting new Task "+this.ffmpegListener.getCurrentTaskNumber());

            //Overrides the found process tracker
            if (durationOverride > 0) {
                //log("Overriding duration to "+durationOverride);
                this.ffmpegListener.getWorkingTask().setDuration(durationOverride);
            } else {
//                    log("Setting duration to "+time);
                this.ffmpegListener.getWorkingTask().setDuration(time);
            }
            task.getProgress();
            //log("Progress: "+task.getSeconds()+" of "+task.getDuration()+" = "+task.getProgress()+"%");
        }

    }

    public void merge(File tempMpg1, File tempMpg2, File target) throws IOException {

        String os = System.getProperty("os.name").toLowerCase();
        //String cmd = "";                   //Jorge
        //if (os.indexOf("win") != -1) {     //Jorge
        Runtime runtime = Runtime.getRuntime();
        if (MediaUtil.osIsWindows()) {
            //cmd = "cmd /c copy /b " + tempMpg1.getAbsolutePath() + "+" + tempMpg2.getAbsolutePath() + " " + target.getAbsolutePath();
            String cmd = "cmd /c copy /b " + tempMpg1.getAbsolutePath() + "+" + tempMpg2.getAbsolutePath() + " " + target.getAbsolutePath();
            ffmpeg = runtime.exec(cmd);
        } else {
            //cmd = "cat " + tempMpg1.getAbsolutePath() + " " + tempMpg2.getAbsolutePath() + " > " + target.getAbsolutePath();
            String[] cmd = {"/bin/bash", "-c", "cat " + tempMpg1.getAbsolutePath() + " " + tempMpg2.getAbsolutePath() + " > " + target.getAbsolutePath()};
            ffmpeg = runtime.exec(cmd);
        }

        //Runtime runtime = Runtime.getRuntime(); //Jorge
        //ffmpeg = runtime.exec(cmd);             //Jorge
        try {
            ffmpeg.waitFor();
        } catch (InterruptedException ex) {
            Logger.getLogger(FFMPEGExecutor.class.getName()).log(Level.SEVERE, null, ex);
        }
        ffmpegKiller = new ProcessKiller(ffmpeg, "FFmpeg Process Killer");
        runtime.addShutdownHook(ffmpegKiller);
        inputStream = ffmpeg.getInputStream();
        outputStream = ffmpeg.getOutputStream();
        errorStream = ffmpeg.getErrorStream();
    }

    /**
     * Returns a stream reading from the ffmpeg process standard output channel.
     * 
     * @return A stream reading from the ffmpeg process standard output channel.
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Returns a stream writing in the ffmpeg process standard input channel.
     * 
     * @return A stream writing in the ffmpeg process standard input channel.
     */
    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Returns a stream reading from the ffmpeg process standard error channel.
     * 
     * @return A stream reading from the ffmpeg process standard error channel.
     */
    public InputStream getErrorStream() {
        return errorStream;
    }

    /**
     * If there's a ffmpeg execution in progress, it kills it.
     */
    public void destroy() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Throwable t) {
                ;
            }
            inputStream = null;
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (Throwable t) {
                ;
            }
            outputStream = null;
        }
        if (errorStream != null) {
            try {
                errorStream.close();
            } catch (Throwable t) {
                ;
            }
            errorStream = null;
        }
        if (ffmpeg != null) {
            ffmpeg.destroy();
            ffmpeg = null;
        }
        if (ffmpegKiller != null) {
            Runtime runtime = Runtime.getRuntime();
            runtime.removeShutdownHook(ffmpegKiller);
            ffmpegKiller = null;
        }
    }

    public String getBinDirectory() {
        if (this.ffmpegExecutablePath != null && this.ffmpegExecutablePath.length() > 0) {
            return new File(this.ffmpegExecutablePath).getParent();
        } else {
            return null;
        }
    }

    public void setFFMpegExecutablePath(String ffmpegExecutablePath) {
        this.ffmpegExecutablePath = ffmpegExecutablePath;
    }

    public void log(Object message) {
        LogUtil.log(FFMPEGExecutor.class, message);
    }
}
