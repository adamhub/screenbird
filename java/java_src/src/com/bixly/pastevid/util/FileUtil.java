/*
 * FileUtil.java
 *
 * Version 1.0
 * 
 * 17 May 2013
 */
package com.bixly.pastevid.util;

import com.bixly.pastevid.Settings;
import com.bixly.pastevid.common.Unimplemented;
import com.bixly.pastevid.screencap.components.progressbar.FFMpegProgressBarListener;
import com.bixly.pastevid.screencap.components.progressbar.ProgressBarUploadProgressListener;
import com.bixly.pastevid.screencap.components.progressbar.ProgressMultiPartEntity;
import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;
import it.sauronsoftware.jave.InputFormatException;
import it.sauronsoftware.jave.VideoAttributes;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import javax.swing.JProgressBar;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;


/**
 * Utility class for manipulating files and directories.
 * @author adamc
 */
public class FileUtil {
    
    // Extensions for different file formats
    public static final String PNG = "png";
    public static final String MPG = "mpg";
    public static final String MP4 = "mp4";

    private static FileUtil instance;
    private static final String baseDigits = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * Returns an instance of FileUtil.
     * @return 
     */
    public static FileUtil getInstance() {
        if (instance == null) {
            instance = new FileUtil();
        }
        return instance;
    }

    /**
     * Deletes all files in the given directory with the exception of the 
     * given file name identified in dontDelete. The dontDelete string
     * should only contain a filename that is relative to the screen capture
     * directory, in other words this should not be an absolute path names.
     * @param directory
     * @param dontDelete Filename that is excluded from deletion
     * @throws Exception 
     */
    public static void deleteFiles(String directory, String dontDelete) throws Exception {
        File dir = new File(directory);
        String[] list = dir.list();
        if (list.length > 0) {
            for (int i = 0; i < list.length; i++) {
                File file = new File(directory, list[i]);
                if (!file.getPath().equalsIgnoreCase(dontDelete)) {
                    file.delete();
                }
            }
        }
    }
    
    /**
     * Deletes all files in this directory with the exception of the 
     * file names given in the dontDelete array. The dontDelete array
     * should only contain filename that are relative to the screen capture
     * directory, in other words there should not be absolute path names.
     * @param directory
     * @param dontDelete String array of filenames that are excluded from deletion
     * @throws Exception 
     */
    public static void deleteFiles(String directory, String[] dontDelete) throws Exception {
        File dir = new File(directory);
        String[] list = dir.list();
        
        if (dontDelete == null ) {
            // If no skip files were given, invoke normal delete
            deleteFiles(directory);
        } else if (dontDelete.length == 1 && dontDelete[0] != null) {
            // If there is only one non-null dontDelete, send to proper function
            deleteFiles(directory, dontDelete[0]);
        } else if (list.length > 0) {
            // Delete all file that are not included in dontDelete array
            for (int i = 0; i < list.length; i++) {
                File file = new File(directory, list[i]);

                boolean deleteFile = true;
                // Check if current file is in the dontDelete
                for (String skipFile : dontDelete) {
                    if (file.getAbsolutePath().equals(skipFile)) {
                        deleteFile = false;
                        break;
                    }
                }
                
                if (deleteFile) {
                    file.delete();
                }
            }
        }
    }
    
    /**
     * Deletes all files located in this directory.
     * @param directory
     * @throws Exception 
     */
    public static void deleteFiles(String directory) throws Exception {
        File dir = new File(directory);
        String[] list = dir.list();
        if (list.length > 0) {
            for (int i = 0; i < list.length; i++) {
                File file = new File(directory, list[i]);
                    file.delete();
            }
        }
    }
    
    /**
     * Deletes all subdirectories of the given directory.
     * @param directory the directory where to delete the subdirectories from
     */
    public static void deleteSubdirs(String directory) {
        File parentDir = new File(directory);
        String[] list = parentDir.list();
        File subDir;
        
        if (list.length == 0) {
            return;
        }

        for (int i = 0; i < list.length; i++) {
            subDir = new File(directory, list[i]);
            if (subDir.isDirectory()) {
                String[] fileList = subDir.list();
                if (fileList.length > 0) {
                    for (int j = 0; j < fileList.length; j++) {
                        File file = new File(subDir.getAbsoluteFile(), fileList[j]);
                        file.delete();
                    }
                }
                subDir.delete();
            }
        }
    }
    
    /**
     * Copies a folder from the given source to the given destination. Runs
     * recursively to copy the directory contents.
     * @param src 
     * @param dest
     * @throws IOException 
     */
    public static void copyFolder(File src, File dest) throws IOException {
        // If copying a folder
    	if (src.isDirectory()) {
            
            // If destination directory does not exist, create it
            if(!dest.exists()) {
               dest.mkdir();
               System.out.println("Directory copied from " + src + "  to " + dest);
            }
 
            // List all the directory contents
            String files[] = src.list();
 
            for (String file : files) {
               // Construct the src and dest file structure
               File srcFile = new File(src, file);
               File destFile = new File(dest, file);
               
               // Recursive copy
               copyFolder(srcFile,destFile);
            }
    	} else { // If copying a file
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest); 

            byte[] buffer = new byte[1024];

            int length;
            // Copy the file content in bytes 
            while ((length = in.read(buffer)) > 0){
               out.write(buffer, 0, length);
            }

            in.close();
            out.close();
            System.out.println("File copied from " + src + " to " + dest);
    	}
    }
    
    /**
     * Deletes the directory and all of its subdirectories and files.
     * @param dir
     * @return True if directory is successfully deleted.
     */
    public static boolean deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDirectory(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    /**
     *
     * @param directory the directory/folder name
     * @param fileExtension filename extension to be searched
     * @return an ArrayList containing filenames from the specified directory, sorted in ascending order.<br>
     * returns an empty ArrayList if no files having the extension were found.
     */
    public static ArrayList<String> getFileList(String directory, String fileExtension) {
        ExtensionFilter filter = new ExtensionFilter(fileExtension);
        return getFileList(directory, filter);
    }

    /**
     * 
     * @param directory
     * @param filter
     * @return an ArrayList containing filenames from the specified directory, sorted in ascending order.<br>
     * Returns an empty ArrayList if no files matching the filter were found.
     */
    public static ArrayList<String> getFileList(String directory, FilenameFilter filter) {
        ArrayList<String> fileList = new ArrayList<String>();
        File dir = new File(directory);

        String[] list = dir.list(filter);
        for (String f : list) {
            fileList.add((directory + f));
        }

        if (!fileList.isEmpty()) {
            Collections.sort(fileList, String.CASE_INSENSITIVE_ORDER);
        }
        return fileList;
    }
    
    /**
     * Converts a given decimal value into its Base36 equivalent.
     * @param decimalNumber
     * @return 
     */
    public static String toBase36(long decimalNumber) {
        return fromDecimalToOtherBase(36, decimalNumber);
    }

    /**
     * Converts a given Base36 value into its deciamal equivalent.
     * @param base36Number
     * @return 
     */
    public static int fromBase36(String base36Number) {
        return fromOtherBaseToDecimal(36, base36Number);
    }
    
    /**
     * Converts a decimal number to another counting system.
     * @param base the counting system to convert the the number to.
     * @param decimalNumber the input number to be converted
     * @return the converted number
     */
    private static String fromDecimalToOtherBase(long base, long decimalNumber) {
        String tempVal = decimalNumber == 0 ? "0" : "";
        long mod;

        while (decimalNumber != 0) {
            mod = decimalNumber % base;
            tempVal = baseDigits.substring((int) mod, (int) (mod + 1)) + tempVal;
            decimalNumber = decimalNumber / base;
        }

        return tempVal;
    }

    /**
     * Converts a number from another base to the decimal counting system
     * @param base the base counting system of the input
     * @param number the input value to be converted
     * @return the converted decimal value
     */
    private static int fromOtherBaseToDecimal(int base, String number) {
        int iterator = number.length();
        int returnValue = 0;
        int multiplier = 1;

        while (iterator > 0) {
            returnValue = returnValue + (baseDigits.indexOf(number.substring(iterator - 1, iterator)) * multiplier);
            multiplier = multiplier * base;
            --iterator;
        }
        return returnValue;
    }

    /**
     * Creates an extension string for the given type that can be concatenated
     * to a base filename or for filtering files with the given type.
     * @param type
     * @return 
     */
    public static String createExtension(String type) {
        return "." + type;
    }

    public static String addExtension(String name, String type) {
        return name + createExtension(type);
    }

    /**
     * Removes the path and extension from the input
     * @param s the input path to be stripped
     * @return the filename without the path and extension
     */
    public static String removeExtension(String s) {
        String separator = System.getProperty("file.separator");
        String filename;

        // Remove the path upto the filename.
        int lastSeparatorIndex = s.lastIndexOf(separator);
        if (lastSeparatorIndex == -1) {
            filename = s;
        } else {
            filename = s.substring(lastSeparatorIndex + 1);
        }

        // Remove the extension.
        int extensionIndex = filename.lastIndexOf(".");
        if (extensionIndex == -1) {
            return filename;
        }

        return filename.substring(0, extensionIndex);
    }

    /**
     * Uploads a file via a POST request to a URL.
     * @param fileLocation  path to the file that will be uploaded
     * @param title         title of the video
     * @param slug          slug of the video 
     * @param description   description of the video
     * @param video_type    format of the video (mp4, mpg, avi, etc.)
     * @param checksum      checksum of the file that will be uploaded
     * @param is_public     publicity settings of the file
     * @param pbUpload      progress bar for the upload
     * @param fileSize      the length, in bytes, of the file to be uploaded
     * @param csrf_token    csrf token for the POST request
     * @param user_id       Screenbird user id of the uploader
     * @param channel_id    Screenbird channel id to where the video will be uploaded to
     * @param an_tok        anonymous token identifier if the uploader is not logged in to Screenbird
     * @param listener      listener for the upload
     * @param upload_url    URL where the POST request will be sent to.
     * @return
     * @throws IOException 
     */
    public static String[] postFile(String fileLocation, String title,
                String slug, String description, String video_type,
                String checksum, Boolean is_public, final JProgressBar pbUpload, 
                long fileSize, String csrf_token, String user_id, String channel_id, 
                String an_tok, ProgressBarUploadProgressListener listener, 
                String upload_url) 
            throws IOException {

        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        
        HttpPost httppost = new HttpPost(upload_url);
        File file = new File(fileLocation);

        ProgressMultiPartEntity mpEntity = new ProgressMultiPartEntity(listener);
        ContentBody cbFile = new FileBody(file, "video/quicktime");
        mpEntity.addPart("videoupload", cbFile);

        ContentBody cbToken = new StringBody(title);
        mpEntity.addPart("csrfmiddlewaretoken", cbToken);

        ContentBody cbUser_id = new StringBody(user_id);
        mpEntity.addPart("user_id", cbUser_id);
        
        ContentBody cbChannel_id = new StringBody(channel_id);
        mpEntity.addPart("channel_id", cbChannel_id);

        ContentBody cbAnonym_id = new StringBody(an_tok);
        mpEntity.addPart("an_tok", cbAnonym_id);

        ContentBody cbTitle = new StringBody(title);
        mpEntity.addPart("title", cbTitle);

        ContentBody cbSlug = new StringBody(slug);
        mpEntity.addPart("slug", cbSlug);
        
        ContentBody cbPublic = new StringBody(is_public ? "1" : "0");
        mpEntity.addPart("is_public", cbPublic);

        ContentBody cbDescription = new StringBody(description);
        mpEntity.addPart("description", cbDescription);

        ContentBody cbChecksum = new StringBody(checksum);
        mpEntity.addPart("checksum", cbChecksum);

        ContentBody cbType = new StringBody(video_type);
        mpEntity.addPart("video_type", cbType);
        
        httppost.setEntity(mpEntity);
        log("===================================================");
        log("executing request " + httppost.getRequestLine());
        log("===================================================");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();

        String[] result = new String[2];
        String status = response.getStatusLine().toString();
        result[0] = (status.indexOf("200") >= 0) ? "200" : status;
        log("===================================================");
        log("response " + response.toString());
        log("===================================================");
        if (resEntity != null) {
            result[1] = EntityUtils.toString(resEntity);
            EntityUtils.consume(resEntity);
        }

        httpclient.getConnectionManager().shutdown();
        return result;
    }
    
    /**
     * Merges two MP4 videos into a new video.
     * @param prevFilePath
     * @param newFilePath
     * @param ffmpegListener
     * @return the path of the resulting merged video
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static String mergeVideoMp4(String prevFilePath, String newFilePath, 
            FFMpegProgressBarListener ffmpegListener) 
            throws FileNotFoundException, IOException {
        
        File sourceVideo1 = new File(prevFilePath);
        File sourceVideo2 = new File(newFilePath);
        File targetVideo = new File(sourceVideo1.getPath().replace(sourceVideo1.getName(),(String.valueOf(System.currentTimeMillis())+".mp4")));
        Encoder encoder = new Encoder();
        
        try {
            encoder.merge(sourceVideo1, sourceVideo2, targetVideo, ffmpegListener);
        } catch (EncoderException ex) {
            log(ex);
        }
        return targetVideo.getAbsolutePath();
    }
    
    /**
     * Merges two MPG videos into a new specified video.
     * @param sourceVideo1
     * @param sourceVideo2
     * @param targetVideo
     * @param ffmpegListener
     * @return 
     */
    public static File mergeVideoMpgTo(File sourceVideo1, File sourceVideo2, 
            File targetVideo, FFMpegProgressBarListener ffmpegListener)  {
        Encoder encoder = new Encoder();
        try {
            encoder.mergeMpg(sourceVideo1, sourceVideo2,targetVideo,ffmpegListener);
        } catch (EncoderException ex) {
            log(ex);
        }
        return targetVideo;
    }
    
    /**
     * Encodes the given video in MP4 format.
     * @param filePath
     * @param offset
     * @param bitrate
     * @param ffmpegListener
     * @return the path of the resulting encoded video.
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static String encodeVideoMp4(String filePath, long offset, 
            Integer bitrate,FFMpegProgressBarListener ffmpegListener) 
            throws FileNotFoundException, IOException {
        
        File sourceAudio = new File(filePath.replace(".mov", ".wav"));
        File targetAudio = new File(filePath.replace(".mov", ".mp3"));
        File sourceVideo = new File(filePath);
        File targetVideo = new File(filePath.replace(".mov", ".mp4"));

        if (sourceAudio.exists()) {
            compressAudio(sourceAudio, targetAudio, offset);
        }
        
        if (MediaUtil.osIsMac()){ //Compile MP4 Mac OSX workaround
            // Encode MOV with MP3 to an AVI file
            String avi_path = encodeVideoAvi(filePath, offset, bitrate,ffmpegListener);
            // Access AVI path
            sourceVideo = new File(avi_path);
            // Convert AVI to MP4
            convertVideoAviToMp4(sourceVideo, ffmpegListener);
        } else {
            // compressVideo(sourceVideo, targetVideo, targetAudio, bitrate);
            compressVideo(sourceVideo, targetVideo, targetAudio, bitrate,ffmpegListener);
        }
        
        return filePath.replace(".mov", ".mp4");
    }
    
    /**
     * Encodes the given video in AVI format.
     * @param filePath
     * @param offset
     * @param bitrate
     * @param ffmpegListener
     * @return the path of the resulting encoded video.
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static String encodeVideoAvi(String filePath, long offset, 
            Integer bitrate, FFMpegProgressBarListener ffmpegListener) 
            throws FileNotFoundException, IOException {
        File sourceAudio = new File(filePath.replace(".mov", ".wav"));
        File targetAudio = new File(filePath.replace(".mov", ".mp3"));
        File sourceVideo = new File(filePath);
        File targetVideo = new File(filePath.replace(".mov", ".avi"));

        // If audio has not been compressed yet
        if (!targetAudio.exists() && sourceAudio.exists()) {
            compressAudio(sourceAudio, targetAudio, offset);
        }

        // Compress AVI file
        compressVideoAvi(sourceVideo, targetVideo, targetAudio, bitrate, ffmpegListener);
        return targetVideo.getAbsolutePath();
    }
    
    /**
     * 
     * @param source
     * @param target
     * @param offset 
     */
    public static void compressAudio(File source, File target, float offset) {
            EncodingAttributes attrs = new EncodingAttributes();
        try {
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libmp3lame");
            audio.setBitRate(128000);
            audio.setChannels(1);
            audio.setSamplingRate(16000);
            attrs.setFormat("mp3");
            attrs.setOffset(offset / 1000f);
            attrs.setAudioAttributes(audio);
            Encoder encoder = new Encoder();
            encoder.encode(source, target, attrs);
        } catch (InputFormatException ex) {
            log(ex);
        } catch (EncoderException ex) {
            log(ex);
        } catch (IllegalArgumentException e) {
            log(e);
        }
    }
    
    /**
     * 
     * @param source
     * @param target
     * @param audioFile
     * @param bitrate
     * @param ffmpegListener 
     */
    public static void compressVideoAvi(File source, File target,
            File audioFile, Integer bitrate, 
            FFMpegProgressBarListener ffmpegListener) {
        
        try {
            VideoAttributes video = new VideoAttributes();
            AudioAttributes audio = new AudioAttributes();
            if (audioFile.exists()) {
                audio.setExternalAudioFile(audioFile);
            }
            video.setCodec("msmpeg4v2");
            video.setBitRate(bitrate);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setFormat("avi");
            attrs.setAudioAttributes(audio);
            attrs.setVideoAttributes(video);
            attrs.setMatchQuality(true); //Maintains Quality
            Encoder encoder = new Encoder();
            encoder.encode(source, target, attrs, null, ffmpegListener);
        } catch (Exception e) {
            log(e);
        }
    }
    
    /**
     * 
     * @param source
     * @param ffmpegListener 
     */
    public static void convertVideoAviToMp4(File source, FFMpegProgressBarListener ffmpegListener) {
        try {
            File target = new File(source.getAbsolutePath().replace(".avi", ".mp4"));
            
            VideoAttributes video = new VideoAttributes();
            video.setCodec("copy");

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setMatchQuality(true);
            attrs.setFormat("mp4");
            attrs.setAudioAttributes(new AudioAttributes());
            attrs.setVideoAttributes(video);
            Encoder encoder = new Encoder();
            encoder.encode(source, target, attrs, null, ffmpegListener);
        } catch (Exception e) {
            log(e);
        }
    }

    /**
     * 
     * @param source
     * @param target
     * @param audioFile
     * @param bitrate
     * @param ffmpegListener 
     */
    public static void compressVideo(File source, File target, 
            File audioFile, Integer bitrate, 
            FFMpegProgressBarListener ffmpegListener) {
        
        try {
            VideoAttributes video = new VideoAttributes();
            AudioAttributes audio = new AudioAttributes();
            if (audioFile.exists()) {
                audio.setExternalAudioFile(audioFile);
            }
            video.setCodec("mpeg4");
            video.setBitRate(bitrate);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setFormat("mp4");
            attrs.setAudioAttributes(audio);
            attrs.setVideoAttributes(video);
            attrs.setMatchQuality(true);
            Encoder encoder = new Encoder();
            encoder.encode(source, target, attrs, null, ffmpegListener);
        } catch (Exception e) {
            log(e);
        }
    }

    /**
     * Converts an MPG video into MP4 format.
     * @param source
     * @param ffmpegListener
     * @return the converted video file
     */
    public static File convertMpgToMp4(File source,
            FFMpegProgressBarListener ffmpegListener) {
        
        try {
            if (!source.exists()) {
                throw new Exception("File does not exist");
            }
            if (!source.getName().endsWith(MPG)) {
                throw new Exception("File must be an MPG file type");
            }
            
            File target = new File(source.getAbsolutePath().replace(MPG, MP4));
            
            LibraryUtil.execute(new String[] {
                    Settings.getHandbrakeExecutable(),
                    "-T", // Turbo speed
                    "-e",
                    "x264",
                    "-i", // Input
                    source.getAbsolutePath(),
                    "-o", // Output
                    target.getAbsolutePath()
                }, Settings.BIN_DIR, true, ffmpegListener);
            return target;
            
        } catch (Exception ex) {
            log(ex);
            return null;
        }
    }
    
    /**
     * Converts an MP4 video into MPG format.
     * @param source
     * @param ffmpegListener
     * @return the converted video file
     */
    public static File convertMp4ToMpg(File source,
            FFMpegProgressBarListener ffmpegListener) {
        
        try {
            if (!source.exists()) {
                throw new Exception("File does not exist");
            }
            if (!source.getName().endsWith(".mp4")) {
                throw new Exception("File must be an MP4 file type");
            }
            
            File target = new File(source.getAbsolutePath().replace("mp4", "mpg"));
            
            
            AudioAttributes audio = new AudioAttributes();
            
            VideoAttributes video = new VideoAttributes();
            video.setFrameRate(24);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setMatchQuality(true);
            attrs.setVideoAttributes(video);
            attrs.setAudioAttributes(audio);
            
            Encoder encoder = new Encoder();
            encoder.encode(source, target, attrs, null, ffmpegListener);
            return target;
            
        } catch (Exception e) {
            log(e);
            return null;
        }
        
    }
    
    /**
     * 
     * @param source
     * @param target
     * @param offset
     * @param duration
     * @param ffmpegListener 
     */
    public static void extractMpgClip(File source, File target, int offset, int duration, 
            FFMpegProgressBarListener ffmpegListener) {
        
        try {
            AudioAttributes audio = new AudioAttributes();
            VideoAttributes video = new VideoAttributes();
            EncodingAttributes attrs = new EncodingAttributes();
            
            attrs.setMatchQuality(true);
            video.setCodec("copy");
            audio.setCodec("copy");
            
            attrs.setOffset((float)offset);
            attrs.setDuration((float)duration);
            
            attrs.setVideoAttributes(video);
            attrs.setAudioAttributes(audio);
            
            Encoder encoder = new Encoder();
            encoder.encode(source, target, attrs, null, ffmpegListener);
        } catch (Exception e) {
            log(e);
        }
    }
    
    /**
     * 
     * @param source
     * @param target
     * @param offset
     * @param duration
     * @param ffmpegListener 
     */
    public static void extractMpgLastClip(File source, File target, int offset, int duration, 
            FFMpegProgressBarListener ffmpegListener) {
        
        try {
            AudioAttributes audio = new AudioAttributes();
            VideoAttributes video = new VideoAttributes();
            EncodingAttributes attrs = new EncodingAttributes();
            
            attrs.setMatchQuality(true);
            video.setCodec("copy");

            attrs.setMatchQuality(true);
            attrs.setOffset((float)offset);
            attrs.setDuration((float)duration);
            
            attrs.setVideoAttributes(video);
            attrs.setAudioAttributes(audio);
            
            Encoder encoder = new Encoder();
            encoder.encode(source, target, attrs, null, ffmpegListener);
        } catch (Exception e) {
            log(e);
        }
    }
    
    /**
     * Returns the duration of the video in seconds.
     * @param videoFile
     * @return 
     */
    public static Integer getVideoDuration(String videoFile){
        Encoder encode = new Encoder();
        HashMap<String,String> timeData = new HashMap<String, String>();
        Integer time = 0;
        File preVideo = null;
        
        preVideo = new File(videoFile);
        
        timeData = encode.getVideoDuration(preVideo);
        
        time += Integer.parseInt(timeData.get(Encoder.HOURS))  * 3600;
        time += Integer.parseInt(timeData.get(Encoder.MINUTES))* 60;
        time += Integer.parseInt(timeData.get(Encoder.SECONDS));
        log("Time: "+time);
        
        return time;
        
    }

    /**
     * May or not may work on Windows. Need to be tested. Returns size/resolution
     * of a video. 
     * @param videoFile
     * @return 
     *      Integer[], 0th index == x, 1st index = y
     */
    public int[] getVideoAspectRatio(String videoFile){
        Encoder encode = new Encoder();
        int[] aspectRatio = null;
        File preVideo = new File(videoFile);
        
        aspectRatio = encode.getVideoAspectRatio(preVideo);
        
        return aspectRatio;
    }
    
    
    @Unimplemented
    @Deprecated
    public static void mergeNClips(ArrayList<String> clips, File target) {
        
        if( clips == null ) return;

        //Runtime runtime = Runtime.getRuntime();
        if (MediaUtil.osIsWindows()) {
            //Windows comand "  for %f in (*.log) do type â€œ%fâ€� >> aggregate.txt   "
            throw new UnsupportedOperationException("Need to test N-Merge for Windows");
            //String cmd = "cmd /c copy /b " + tempMpg1.getAbsolutePath() + "+" + tempMpg2.getAbsolutePath() + " " + target.getAbsolutePath();
            //ffmpeg = runtime.exec(cmd);
        } else {
            
            //String[] cmd = {"/bin/bash","-c","cat", clips, ">>", target.getAbsolutePath()};
            ArrayList<String> command = new ArrayList<String>();
            //command.add("/bin/bash");
            //command.add("-c");
            command.add("cat");
            
            //Add all clips to command
            //StringBuilder clipsStr = new StringBuilder();
            //clipsStr.append("cat");
            
            for(String clipPath : clips) {
                //clipsStr.append(clipPath);
                //clipsStr.append(" ");
                command.add(clipPath);
            }
            //command.add(clipsStr.toString());
            
            command.add(">");
            command.add(target.getAbsolutePath());
            
            LibraryUtil.execute(
                    command, //Command to be executed
                    Settings.BIN_DIR, //Destination folder
                    true//Wait till process is done
            );
            
        }
    }
    
    public static void copyTo(File source, File target){
        appendBinary(source, target);
    }
    
    /**
     * Appends sequential data from source to target file
     * @param source
     * @param target 
     */
    public static void appendBinary(File source, File target) {
        
        if( source == null ) return;

        
        FileInputStream sourceStream  = null;
        FileOutputStream targetStream = null;
        
        log(String.format("Appending %s to %s",source.getAbsolutePath(), target.getAbsolutePath()));
        try {

            if(!target.exists()) target.createNewFile();
            
            sourceStream = new FileInputStream(source);
            targetStream = new FileOutputStream(target,true);
            
            long currByte = 0L;
            long totalBytes = 0L;
            byte[] data = new byte[2048];
            while((currByte = sourceStream.read(data)) != -1){
                targetStream.write(data,0, (int)currByte);
                totalBytes += data.length;
                
//                if( totalBytes % 10240 == 0)
//                    log(String.format("[%d/%d] %2.0f%%", totalBytes, source.length(), (double)totalBytes/(double)source.length()));
            }
            targetStream.flush();


        } catch(FileNotFoundException e){

            log(e);
        } catch (IOException e){
            log(e);
        } finally {

            if(sourceStream != null) try {
                sourceStream.close();
            } catch (IOException e){
                log(e);
            }

            if(targetStream != null) try {
                targetStream.close();
            } catch (IOException e){
                log(e);
            }
        }
            
    }
    
    
    public static String getChecksum(String filePath) throws FileNotFoundException, IOException {
        FileInputStream file = new FileInputStream(filePath);
        CheckedInputStream check = new CheckedInputStream(file, new CRC32());
        BufferedInputStream in = new BufferedInputStream(check);
        String checkSum = "";
        
        while (in.read() != -1) {
            // Read file in completely
        }
        
        if(in != null){
            in.close();
        }
        
        if(file != null)
            file.close();
        
        if(check != null){
            checkSum = String.valueOf(check.getChecksum().getValue());
            check.close();
        }
        
        return checkSum;
    }

    /**
     * Copies a file bundled in the package to the supplied destination.
     * 
     * @param path
     *            The name of the bundled file.
     * @param dest
     *            The destination.
     * @throws RuntimeException
     *             If aun unexpected error occurs.
     */
    public void copyFile(String path, File dest) throws RuntimeException {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = getClass().getResourceAsStream(path);
            output = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int l;
            while ((l = input.read(buffer)) != -1) {
                output.write(buffer, 0, l);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot write file "
                    + dest.getAbsolutePath());
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (Throwable t) {
                    ;
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable t) {
                    ;
                }
            }
        }
    }
    
    
    /**
     * Uses default default to prepare Contingency Plan
     * @return 
     */
    public static File prepDestinationDir(){ return prepDestinationDir(Settings.BIN_DIR); }
    /**
     * Creates a directory by making it writable and executable.
     * @param destDir Directory to be constructed
     * @return The directory which has been created
     */
    public static File prepDestinationDir(String destDir){
        
        File binDirectory = new File(destDir);
        
        if(!binDirectory.exists())     binDirectory.mkdirs();
        if(!binDirectory.canWrite())   binDirectory.setWritable(true);
        if(!binDirectory.canExecute()) binDirectory.setExecutable(true);
        
        //binDirectory.deleteOnExit();
        
        return binDirectory;
    }
    
    /**
     * A method which to move a list of files to another directory/location. 
     * Also makes each file in the given list executable.
     * @param files List of files to be relocated
     * @param destDirectory Directory which the files are to be moved to
     * @return 
     */
    public static boolean relocateFile(String[] files, String destDirectory) {
        
        File destDirFile = prepDestinationDir(destDirectory);
        
        //For each file to be relocated
        for(String filepath : files){
            //Get access to file
            File file = new File(filepath);
            File relocateFile = new File(destDirFile.getAbsoluteFile()+"/"+file.getName());
            
            
            LibraryUtil.chmod("755", file);
            LibraryUtil.chmod("755", relocateFile);
            
            System.out.print("Relocating "+file.getAbsolutePath()+" to "+relocateFile.getAbsolutePath()+"...");
            
            //Relocates file to proper destination
            file.renameTo(relocateFile);
            
            if(relocateFile.exists()){
                log("Succesfull");
            }else
                log("ERROR - Unsuccesfull");
        }
        
        return true;
        
    }
    public static void saveObjectDataToFile(Object obj, String name) {

       try {
            FileOutputStream fout = new FileOutputStream(Settings.SCREEN_CAPTURE_DIR+name);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(obj);
            oos.close();
        } catch (Exception e) {
            log(e);
        }

    }
    public static void deleteDataObject(String name) {
        File file = new File(Settings.SCREEN_CAPTURE_DIR+name);
        if(file.exists())
            file.delete();
    }
    public static Object readObjectDataFromFile(String name) {

        Object obj = new Object();
        try {
            FileInputStream fin = new FileInputStream(Settings.SCREEN_CAPTURE_DIR+name);
            ObjectInputStream ois = new ObjectInputStream(fin);
            obj = ois.readObject();
            ois.close();
        } catch (ClassNotFoundException e) {
            log(e);
        } catch (IOException e) {
            log(e);
        }

        return obj;
    }
    public static boolean previousVideoExists(String name){
        File videoFile = new File(Settings.SCREEN_CAPTURE_DIR+name);
        return videoFile.exists();
    }
    public static boolean checkMarker(String name){
        File marker = new File(Settings.SCREEN_CAPTURE_DIR+name);
        log("Checking for previous video "+marker+" exists? "+marker.exists());
        return marker.exists();
    }
    public static void removeMarker(String name){
        File marker = new File(Settings.SCREEN_CAPTURE_DIR+name);
        if(marker.exists())
            marker.delete();
    }
    public static boolean addMarker(String name){
        try{
            File marker = new File(Settings.SCREEN_CAPTURE_DIR+name);
            marker.createNewFile();
        }catch (IOException e){
            log(e);
            return false;
        }
        return true;
    }
    
    public static void saveProperty(String key, Integer value){
        FileOutputStream out = null;
        FileInputStream in = null;
        try{
            
            File file = new File(Settings.SCREENBIRD_CONFIG);
            
            if(!file.exists()){file.createNewFile();}
            
            Properties prop = new Properties();
            in = new FileInputStream(file.getAbsolutePath());
            prop.load(in);

            out = new FileOutputStream(file.getAbsolutePath());
            prop.setProperty(key, value.toString());
            prop.store(out, "Pastevid Metadata");
           
        }catch (IOException e){
            log(e);
        } finally {
            
            if(in != null)try{
                in.close();
            } catch(IOException e){
                log(e);
            }
            
            if(out != null) try{
                out.close();
            } catch(IOException e){
                log(e);
            }
        } 
    }
    public static String loadProperty(String key, String defaultValue){
        
        if(key == null) return null;
        
        FileInputStream in = null;

        try{
            
            File file = new File(Settings.SCREENBIRD_CONFIG);
            
            log("Does "+file.getAbsolutePath()+" Exist? "+ file.exists());
            
            //Return default if metadata not found
            if(!file.exists()){return defaultValue;}
            //Read in metadata
            Properties prop = new Properties();
            in = new FileInputStream(file.getAbsolutePath());
            prop.load(in);
            in.close();
            //If data is not found, return default
            if(prop.getProperty(key) == null) 
                return defaultValue;
            else 
                return prop.getProperty(key);
        }
        catch (IOException e){ log(e); }
        catch (NumberFormatException e){ log(e); }
        finally {
            if(in != null)try{
                in.close();
            } catch(IOException e){
                log(e);
            }
        }
        //If exception is thrown, return default
        return null;
    }
    public static boolean previousRecordingExists(){
        
        boolean result = false;
        File subDir;
        File parentDir = new File(Settings.SCREEN_CAPTURE_DIR);
        String[] list = parentDir.list();
        if (list != null && list.length == 0) {
            result = false;
        } else if(list != null){
            for (int i = 0; i < list.length; i++) {
                subDir = new File(Settings.SCREEN_CAPTURE_DIR, list[i]);
                if (subDir.isDirectory()) {
                    String[] fileList = subDir.list();
                    if (fileList.length > 0) {
                      result = true;
                      break;
                    }
                }
            }
        }
        return result;
    }
    
    public static FilenameFilter getDirectoryFilter() {
        return (new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        });
    }

    private static void log(Object message){
        LogUtil.log(FileUtil.class, message);
    }

    /**
     *
     */
    public static final class ExtensionFilter implements FilenameFilter {

        private String extension;

        public ExtensionFilter(String extension) {
            this.extension = extension;
        }

        public boolean accept(File dir, String name) {
            return (name.endsWith(extension));
        }
    }
    
    
}
