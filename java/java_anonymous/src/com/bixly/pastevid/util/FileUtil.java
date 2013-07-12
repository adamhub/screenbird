package com.bixly.pastevid.util;

//import com.bixly.pastevid.models.VideoFileItem;
import com.bixly.pastevid.util.view.ProgressBarUploadProgressListener;
import com.bixly.pastevid.util.view.ProgressMultiPartEntity;
//import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
//import it.sauronsoftware.jave.EncodingAttributes;
//import it.sauronsoftware.jave.InputFormatException;
//import it.sauronsoftware.jave.VideoAttributes;
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
import java.util.logging.Level;
import java.util.logging.Logger;
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


public class FileUtil {

    private static FileUtil instance;
    /**
     * Deletes all files(having the specified extension) in the specified directory
     *
     * @param directory
     * @param fileExtension
     * @throws Exception
     */
    private static final String baseDigits = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static FileUtil getInstance() {
        if (instance == null) {
            instance = new FileUtil();
        }
        return instance;
    }

   
    public static void deleteFiles(String directory, String dontDelete) throws Exception {
        File dir = new File(directory);
        String[] list = dir.list();
        if (list.length > 0) {
            for (int i = 0; i < list.length; i++) {
                File file = new File(directory, list[i]);
                if (!file.getPath().equals(dontDelete))
                    file.delete();
            }
        }
    }
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
     *
     *
     * @param directory the directory/ folder name
     * @param fileExtension filename extension to be searched
     * @return a Vector containing filenames from the specified directory, sorted in ascending order.<br>
     * returns an empty Vector if no files having the extension were found.
     */
    public static ArrayList<String> getFileList(String directory, String fileExtension) {
        ExtensionFilter filter = new ExtensionFilter(fileExtension);
        return getFileList(directory, filter);
    }

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

    public static String toBase36(long decimalNumber) {
        return fromDecimalToOtherBase(36, decimalNumber);
    }

    public static int fromBase36(String base36Number) {
        return fromOtherBaseToDecimal(36, base36Number);
    }

    private static String fromDecimalToOtherBase(long base, long decimalNumber) {
        String tempVal = decimalNumber == 0 ? "0" : "";
        long mod = 0;

        while (decimalNumber != 0) {
            mod = decimalNumber % base;
            tempVal = baseDigits.substring((int) mod, (int) (mod + 1)) + tempVal;
            decimalNumber = decimalNumber / base;
        }

        return tempVal;
    }

    public static String createExtension(String type) {
        return "." + type;
    }

    public static String addExtension(String name, String type) {
        return name + "." + type;
    }

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

    public static String[] postFile(String fileLocation, String title,
            String slug, String description, String video_type,
            String checksum, Boolean is_public, final JProgressBar pbUpload, long fileSize, String csrf_token,
            String user_id, ProgressBarUploadProgressListener listener, String upload_url) throws IOException {

        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost httppost = new HttpPost(upload_url);
        File file = new File(fileLocation);

        //MultipartEntity mpEntity = new MultipartEntity();

        ProgressMultiPartEntity mpEntity = new ProgressMultiPartEntity(listener);
        ContentBody cbFile = new FileBody(file, "video/quicktime");
        mpEntity.addPart("videoupload", cbFile);

        ContentBody cbToken = new StringBody(title);
        mpEntity.addPart("csrfmiddlewaretoken", cbToken);

        ContentBody cbUser_id = new StringBody(user_id);
        mpEntity.addPart("user_id", cbUser_id);

        ContentBody cbAnonym_id = new StringBody(MediaUtil.getMacAddress());
        mpEntity.addPart("anonym_id", cbAnonym_id);

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
        System.out.println("===================================================");
        System.out.println("executing request " + httppost.getRequestLine());
        System.out.println("===================================================");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();

        String[] result = new String[2];
        String status = response.getStatusLine().toString();
        result[0] = (status.indexOf("200") >= 0) ? "200" : status;
        System.out.println("===================================================");
        System.out.println("status from request " + result[0]);
        System.out.println("===================================================");
        if (resEntity != null) {
            result[1] = EntityUtils.toString(resEntity);
            EntityUtils.consume(resEntity);
        }

        httpclient.getConnectionManager().shutdown();
        return result;
    }
    
    public static String[] postData(String data, final JProgressBar pbUpload, String anonymous_token, String csrf_token,
            String user_id, ProgressBarUploadProgressListener listener, String upload_url) throws IOException {

        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost httppost = new HttpPost(upload_url);
        //File file = new File(fileLocation);

        //MultipartEntity mpEntity = new MultipartEntity();

        ProgressMultiPartEntity mpEntity = new ProgressMultiPartEntity(listener);
        //ContentBody cbFile = new FileBody(file, "video/quicktime");
        //mpEntity.addPart("videoupload", cbFile);

        //ContentBody cbToken = new StringBody(title);
        //mpEntity.addPart("csrfmiddlewaretoken", cbToken);

        ContentBody cbUser_id = new StringBody(user_id);
        mpEntity.addPart("user_id", cbUser_id);

        ContentBody cbAnonym_id = new StringBody(MediaUtil.getMacAddress());
        mpEntity.addPart("anonym_id", cbAnonym_id);

        //ContentBody cbTitle = new StringBody(title);
        //mpEntity.addPart("title", cbTitle);

//        ContentBody cbSlug = new StringBody(slug);
//        mpEntity.addPart("slug", cbSlug);

//        ContentBody cbPublic = new StringBody(is_public ? "1" : "0");
//        mpEntity.addPart("is_public", cbPublic);

//        ContentBody cbDescription = new StringBody(description);
//        mpEntity.addPart("description", cbDescription);

//        ContentBody cbChecksum = new StringBody(checksum);
//        mpEntity.addPart("checksum", cbChecksum);

//        ContentBody cbType = new StringBody(video_type);
//        mpEntity.addPart("video_type", cbType);



        httppost.setEntity(mpEntity);
        System.out.println("===================================================");
        System.out.println("executing request " + httppost.getRequestLine());
        System.out.println("===================================================");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();

        String[] result = new String[2];
        String status = response.getStatusLine().toString();
        result[0] = (status.indexOf("200") >= 0) ? "200" : status;
        System.out.println("===================================================");
        System.out.println("status from request " + result[0]);
        System.out.println("===================================================");
        if (resEntity != null) {
            result[1] = EntityUtils.toString(resEntity);
            EntityUtils.consume(resEntity);
        }

        httpclient.getConnectionManager().shutdown();
        return result;
    }
    
    public static String mergeVideoMp4(String prevFilePath, String newFilePath) throws FileNotFoundException, IOException {
        File sourceVideo1 = new File(prevFilePath);
        File sourceVideo2 = new File(newFilePath);
        File targetVideo = new File(sourceVideo1.getPath().replace(sourceVideo1.getName(),(String.valueOf(System.currentTimeMillis())+".mp4")));
        Encoder encoder = new Encoder();
        try {
            encoder.merge(sourceVideo1, sourceVideo2,targetVideo);
        } catch (EncoderException ex) {
            Logger.getLogger(FileUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        return targetVideo.getAbsolutePath();
    }
    
//    public static String encodeVideoMp4(String filePath, long offset, Integer bitrate) throws FileNotFoundException, IOException {
//        File sourceAudio = new File(filePath.replace(".mov", ".wav"));
//        File targetAudio = new File(filePath.replace(".mov", ".mp3"));
//        File sourceVideo = new File(filePath);
//        File targetVideo = new File(filePath.replace(".mov", ".mp4"));
//
//        if (sourceAudio.exists()) {
//            compressAudio(sourceAudio, targetAudio, offset);
//        }
//        
//        if (MediaUtil.osIsMac()){ //Compile MP4 Mac OSX workaround
//            //Encode MOV with MP3 to an AVI file
//            String avi_path = encodeVideoAvi(filePath, offset, bitrate);
//            //Access AVI path
//            sourceVideo = new File(avi_path);
//            //Convert AVI to MP4
//            convertVideoAviToMp4(sourceVideo, bitrate);
//        }else {
//            compressVideo(sourceVideo, targetVideo, targetAudio, bitrate);
//        }
//        
//        return filePath.replace(".mov", ".mp4");
//    }
    
//    public static String encodeVideoAvi(String filePath, long offset, Integer bitrate) throws FileNotFoundException, IOException {
//        File sourceAudio = new File(filePath.replace(".mov", ".wav"));
//        File targetAudio = new File(filePath.replace(".mov", ".mp3"));
//        File sourceVideo = new File(filePath);
//        File targetVideo = new File(filePath.replace(".mov", ".avi"));
//
//        //If audio has not been compressed yet
//        if (!targetAudio.exists() && sourceAudio.exists()) {
//            compressAudio(sourceAudio, targetAudio, offset);
//        }
//
//        //Compress AVI file
//        compressVideoAvi(sourceVideo, targetVideo, targetAudio, bitrate);
//        return targetVideo.getAbsolutePath();
//    }

//    public static void compressAudio(File source, File target, float offset) {
//        try {
//            AudioAttributes audio = new AudioAttributes();
//            audio.setCodec("libmp3lame");
//            audio.setBitRate(new Integer(128000));
//            audio.setChannels(new Integer(1));
//            audio.setSamplingRate(new Integer(16000));
//            EncodingAttributes attrs = new EncodingAttributes();
//            attrs.setFormat("mp3");
//            attrs.setOffset(offset / 1000f);
//            attrs.setAudioAttributes(audio);
//            Encoder encoder = new Encoder();
//            encoder.encode(source, target, attrs);
//        } catch (InputFormatException ex) {
//            Logger.getLogger(FileUtil.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (EncoderException ex) {
//            Logger.getLogger(FileUtil.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IllegalArgumentException ex) {
//            System.out.println(ex.getMessage());
//        }
//    }
    
//    public static void compressVideoAvi(File source, File target, File audioFile,Integer bitrate) {
//        try {
//            VideoAttributes video = new VideoAttributes();
//            AudioAttributes audio = new AudioAttributes();
//            if (audioFile.exists()) {
//                audio.setExternalAudioFile(audioFile);
//            }
//            video.setCodec("msmpeg4v2");
//            video.setBitRate(bitrate);
//
//            EncodingAttributes attrs = new EncodingAttributes();
//            attrs.setFormat("avi");
//            attrs.setAudioAttributes(audio);
//            attrs.setVideoAttributes(video);
//            attrs.setMatchQuality(true); //Maintains Quality
//            //./ffmpeg -i source.mov -sameq -vcodec msmpeg4v2 -i audio.mp3 -f avi -y target.avi
//            Encoder encoder = new Encoder();
//            encoder.encode(source, target, attrs);
//        } catch (Exception ex) {
//            System.out.println(ex.getMessage());
//        }
//    }
    
//    public static void convertVideoAviToMp4(File source,Integer bitrate) {
//        try {
//            //Source:AVI -> Target:MP4
//            File target = new File(source.getAbsolutePath().replace(".avi", ".mp4"));
//            
//            VideoAttributes video = new VideoAttributes();
//            video.setCodec("mpeg4");
//
//            EncodingAttributes attrs = new EncodingAttributes();
//            attrs.setMatchQuality(true);
//            attrs.setFormat("mp4");
//            attrs.setAudioAttributes(new AudioAttributes());
//            attrs.setVideoAttributes(video);
//            //./ffmpeg -i source.avi -sameq -vcodec mpeg4 -f mp4 -y target.mp4
//            Encoder encoder = new Encoder();
//            encoder.encode(source, target, attrs);
//        } catch (Exception ex) {
//            System.out.println(ex.getMessage());
//        }
//    }

//    public static void compressVideo(File source, File target, File audioFile,Integer bitrate) {
//        try {
//            VideoAttributes video = new VideoAttributes();
//            AudioAttributes audio = new AudioAttributes();
//            if (audioFile.exists()) {
//                audio.setExternalAudioFile(audioFile);
//            }
//            video.setCodec("mpeg4");
//            video.setBitRate(bitrate);
//
//            EncodingAttributes attrs = new EncodingAttributes();
//            attrs.setFormat("mp4");
//            attrs.setAudioAttributes(audio);
//            attrs.setVideoAttributes(video);
//            //ffmpeg.exe -i source.mov -i target.mp3 -vcodec mjpeg -f mp4 -y target.mp4
//            Encoder encoder = new Encoder();
//            encoder.encode(source, target, attrs);
//        } catch (Exception ex) {
//            System.out.println(ex.getMessage());
//        }
//    }
    
    public static String getChecksum(String filePath) throws FileNotFoundException, IOException {
        FileInputStream file = new FileInputStream(filePath);
        CheckedInputStream check = new CheckedInputStream(file, new CRC32());
        BufferedInputStream in = new BufferedInputStream(check);
        while (in.read() != -1) {
            // Read file in completely
        }
        return String.valueOf(check.getChecksum().getValue());
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
    
      public static void saveObjectDataToFile(Object obj, String name) {

       try {
            FileOutputStream fout = new FileOutputStream(System.getProperty("java.io.tmpdir") + 
                    System.getProperty("file.separator") + "screencap" + 
                    System.getProperty("file.separator") + name);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(obj);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public static void deleteDataObject(String name) {
        File file = new File(System.getProperty("java.io.tmpdir") +
                    System.getProperty("file.separator") + "screencap" +
                    System.getProperty("file.separator") + name);
        if(file.exists())
            file.delete();
    }
    public static Object readObjectDataFromFile(String name) {

        Object obj = new Object();
        try {
            FileInputStream fin = new FileInputStream(System.getProperty("java.io.tmpdir") +
                    System.getProperty("file.separator") + "screencap" +
                    System.getProperty("file.separator") + name);
            ObjectInputStream ois = new ObjectInputStream(fin);
            obj = ois.readObject();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return obj;
    }
    public static boolean previousVideoExists(String name){
        File videoFile = new File(System.getProperty("java.io.tmpdir") + 
                    System.getProperty("file.separator") + "screencap" + 
                    System.getProperty("file.separator") + name );
        return videoFile.exists();
    }
    public static boolean previousRecordingExists(){
        
        boolean result = false;
        File subDir;
        File parentDir = new File(System.getProperty("java.io.tmpdir") + 
                    System.getProperty("file.separator") + "screencap" + 
                    System.getProperty("file.separator"));
        String[] list = parentDir.list();
        if (list.length == 0) {
            result = false;
        }
        else{
            for (int i = 0; i < list.length; i++) {
                subDir = new File(System.getProperty("java.io.tmpdir") + 
                    System.getProperty("file.separator") + "screencap" + 
                    System.getProperty("file.separator"), list[i]);
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
