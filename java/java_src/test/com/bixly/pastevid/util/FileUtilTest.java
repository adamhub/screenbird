/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.util;

import com.bixly.pastevid.Session;
import com.bixly.pastevid.Settings;
import com.bixly.pastevid.download.DownloadFFMpeg;
import com.bixly.pastevid.download.DownloadHandbrake;
import com.bixly.pastevid.download.DownloadManager;
import com.bixly.pastevid.download.DownloadUnzip;
import com.bixly.pastevid.recorders.IMeasurable;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import com.bixly.pastevid.screencap.components.progressbar.FFMpegProgressBarListener;
import com.bixly.pastevid.util.temporary.TestSerializable;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author cevaris
 */
public class FileUtilTest {
    
    
    /**
     * A few of these unit tests require actual video/audio files. To easily create
     * sample files for testing, just run a sample recording via Screen Recorder 
     * and grab the generated files in the Screen Recorder's temp "screencap" 
     * directory. 
     */
    private final static File SAMPLE_MP4 = new File("/home/user/Videos/test.mp4");
    private final static File SAMPLE_AVI = new File("/home/user/Videos/test.avi");
    private final static File SAMPLE_MOV = new File("/home/user/Videos/test.mov");
    private final static File SAMPLE_MPG = new File("/home/user/Videos/test.mpg");
    private final static File SAMPLE_MPG2 = new File("/home/user/Videos/test2.mpg");
    private final static File SAMPLE_WAV = new File("/home/user/Videos/test.wav");
    private final static File FFMPEG_BIN = new File(Settings.getFFMpegExecutable());
    private final static File HANBRAKE_BIN = new File(Settings.getHandbrakeExecutable());

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private File properties;

    public FileUtilTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        
        generateTempUser();
        confirmLibrariesStatus();
        
        File screencap = new File(Settings.SCREEN_CAPTURE_DIR);
        screencap.delete();
        screencap.mkdirs();
        screencap.deleteOnExit();
        
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        FileUtil.deleteDirectory(new File(Settings.SCREEN_CAPTURE_DIR));
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private class TestMeasurable implements IMeasurable {

        private long initiTime;

        public TestMeasurable() {
            initiTime = System.currentTimeMillis();
        }

        public long getValue() {
            return System.currentTimeMillis() - initiTime;
        }

        public GraphicsDevice getScreen() {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsConfiguration gd = ge.getScreenDevices()[0].getDefaultConfiguration();
            GraphicsDevice device = gd.getDevice();
            return device;
        }
        
        public void relocate(int x, int y){
            // no-op.
        }
    }

    private static void confirmLibrariesStatus(){
        DownloadManager downloadManager = DownloadManager.getInstance();
        
        if(MediaUtil.osIsWindows()){
            //Windows need unzipping utility
            downloadManager.registerDownload(
                Settings.getUnzipExecutable(),
                new DownloadUnzip()
            );
        }
        
        //Register FFMpeg for downloading
        downloadManager.registerDownload(
                Settings.getFFMpegExecutable(), 
                new DownloadFFMpeg()
        );
        //Register Handbrake for downloading
        downloadManager.registerDownload(
                Settings.getHandbrakeExecutable(), 
                new DownloadHandbrake()
        );
        
        downloadManager.start();
        
        while(downloadManager.isRunning()){
            System.out.println("Waiting for Binary Downloads before running test");
            TimeUtil.skipToMyLou(5);
        }
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
     * Generates a temporary User model instance for JUnit testing
     * Only should be used for JUnit Tests!!!
     * @return 
     */
    private static void generateTempUser(){
        Session.getInstance().user.setBaseURL("http://staging.screenbird.com/");
        Session.getInstance().user.setAnonToken("ZXCVBNMASDFGHJKLQWERTYUIOP");
        Session.getInstance().user.setCsrfToken("QWERTYUIOPASDFGHJKLZXCVBNM");
        Session.getInstance().user.setUserId("10");
    }

    public void createFile(String name) throws IOException {
        properties = folder.newFile(name);
        BufferedWriter out = new BufferedWriter(new FileWriter(properties));
        out.write("this simulates a jpg file");
        out.close();
    }

    /**
     * Test of getInstance method, of class FileUtil.
     */
    @Test
    public void testGetInstance() {
        System.out.println("getInstance");
        FileUtil result = FileUtil.getInstance();
        assertNotNull(result);
    }

    /**
     * Test of deleteFiles method, of class FileUtil.
     */
    @Test
    public void testDeleteFiles_String_String() throws Exception {
        System.out.println("deleteFiles");
        this.createFile("file1.jpg");
        String directory = folder.getRoot().getPath();
        String fileExtension = "jpg";
        FileUtil.deleteFiles(directory, fileExtension);
        assertFalse(properties.exists());
    }

    /**
     * Test of deleteFiles method, of class FileUtil.
     */
    @Test
    public void testDeleteFiles_String() throws Exception {
        System.out.println("deleteFiles");
        this.createFile("file2.jpg");
        String directory = folder.getRoot().getPath();
        FileUtil.deleteFiles(directory);
        assertFalse(properties.exists());
    }

    /**
     * Test of deleteSubdirs method, of class FileUtil.
     */
    @Test
    public void testDeleteSubdirs() {
        System.out.println("deleteSubdirs");
        File subFolder = folder.newFolder("folder");
        String directory = folder.getRoot().getPath();
        FileUtil.deleteSubdirs(directory);
        assertFalse(subFolder.exists());
    }

    /**
     * Test of getFileList method, of class FileUtil.
     */
    @Test
    public void testGetFileList_String_String() throws IOException {
        System.out.println("getFileList");
        String directory = folder.getRoot().getPath();
        String fileExtension = "jpg";
        this.createFile("file3.jpg");
        ArrayList result = FileUtil.getFileList(directory, fileExtension);
        assertEquals(1, result.size());
    }

    /**
     * Test of getFileList method, of class FileUtil.
     */
    @Test
    public void testGetFileList_String_FilenameFilter() throws IOException {
        System.out.println("getFileListFilter");
        String directory = folder.getRoot().getPath();
        FilenameFilter filter = new FileUtil.ExtensionFilter("jpg");
        this.createFile("file4.jpg");
        ArrayList result = FileUtil.getFileList(directory, filter);
        assertEquals(1, result.size());
    }

    /**
     * Test of toBase36 method, of class FileUtil.
     */
    @Test
    public void testToBase36() {
        System.out.println("toBase36");
        long decimalNumber = 500;
        String expResult = "DW";
        String result = FileUtil.toBase36(decimalNumber);
        assertEquals(expResult, result);
    }

    /**
     * Test of fromBase36 method, of class FileUtil.
     */
    @Test
    public void testFromBase36() {
        System.out.println("fromBase36");
        String base36Number = "DW";
        int expResult = 500;
        int result = FileUtil.fromBase36(base36Number);
        assertEquals(expResult, result);
    }

    /**
     * Test of createExtension method, of class FileUtil.
     */
    @Test
    public void testCreateExtension() {
        System.out.println("createExtension");
        String type = "mp4";
        String expResult = ".mp4";
        String result = FileUtil.createExtension(type);
        assertEquals(expResult, result);
    }

    /**
     * Test of addExtension method, of class FileUtil.
     */
    @Test
    public void testAddExtension() {
        System.out.println("addExtension");
        String name = "file";
        String type = "mp4";
        String expResult = "file.mp4";
        String result = FileUtil.addExtension(name, type);
        assertEquals(expResult, result);
    }

    /**
     * Test of removeExtension method, of class FileUtil.
     */
    @Test
    public void testRemoveExtension() {
        System.out.println("removeExtension");
        String s = "filename.mp4";
        String expResult = "filename";
        String result = FileUtil.removeExtension(s);
        assertEquals(expResult, result);
    }

    /**
     * Test of getChecksum method, of class FileUtil.
     */
    @Test
    public void testGetChecksum() throws Exception {
        System.out.println("getChecksum");
        this.createFile("file5.jpg");
        String filePath = properties.getPath();
        String result = FileUtil.getChecksum(filePath);
        assertNotNull(result);
    }

    /**
     * Test of hasAudioToCompile method, of class Recorder.
    
     */
    //@Test
    @Test
    public void testFileMarkSystem() {
        System.out.println("fileMarkSystem");
        FileUtil.addMarker("testMarker12345");
        Boolean result = FileUtil.checkMarker("testMarker12345");
        assertTrue(result);
        FileUtil.removeMarker("testMarker12345");
        result = FileUtil.checkMarker("testMarker12345");
        assertFalse(result);
    }

    /**
     * Test of hasAudioToCompile method, of class Recorder.
    
     */
    //@Test
    @Test
    public void testAppendClip() throws IOException {
        System.out.println("AppendClip");
        
        assert SAMPLE_MPG.exists() && SAMPLE_MPG.length()>0 : "This test requires a MPG video file";
        
        //Give paths to three sample videos
        String sourceAPath = SAMPLE_MPG.getAbsolutePath();
        String sourceBPath = SAMPLE_MPG.getAbsolutePath();
        String sourceCPath = SAMPLE_MPG.getAbsolutePath();
        String resultPath =  new File(System.getProperty("java.io.tmpdir"), Long.toString(System.nanoTime())).getAbsolutePath();

        File sourceA = new File(sourceAPath);
        File sourceB = new File(sourceBPath);
        File sourceC = new File(sourceCPath);
        File result = new File(resultPath);

        FileUtil.appendBinary(sourceA, result);
        FileUtil.appendBinary(sourceB, result);
        FileUtil.appendBinary(sourceC, result);

        assertTrue(
                result.exists()
                && result.length() > (sourceA.length())
                && result.length() > (sourceB.length())
                && result.length() > (sourceC.length()));

    }

    /**
     * Test of retrieving the length of a video.
     */
    @Test
    public void testGetVideoLength() throws Exception {
        System.out.println("getVideoLength");
        
        assert SAMPLE_MP4.exists() : "Please provide a sample MP4 vidoe for testing";
        
        assertTrue(FileUtil.getVideoDuration(SAMPLE_MP4.getAbsolutePath()) > 0);
        
        
    }

    /**
     * Test of deleteFiles method, of class FileUtil.
     */
    @Test
    public void testDeleteFiles_String_StringArr() throws Exception {
        System.out.println("deleteFiles String[]");
        File tempDir = new File(System.getProperty("java.io.tmpdir"), Long.toString(System.nanoTime()));
        tempDir.mkdir();
        tempDir.deleteOnExit();
        assertTrue(tempDir.exists());
        assertTrue(tempDir.isDirectory());
        
        File tempFile1 = new File(tempDir,"testFile1.tmp");
        File tempFile2 = new File(tempDir,"testFile2.tmp");
        
        tempFile1.createNewFile();
        tempFile2.createNewFile();
        
        tempFile1.deleteOnExit();
        tempFile2.deleteOnExit();
        
        String directory = tempDir.getAbsolutePath();
        String[] dontDelete = {tempFile2.getAbsolutePath()};
        
        
        assertTrue(tempFile1.exists());
        assertTrue(tempFile2.exists());
        
        assertNotNull(directory);
        assertNotNull(dontDelete);
        
        try {
            FileUtil.deleteFiles(directory, dontDelete);
        } catch (Exception e){
            System.err.println(e);
        }
        
        assertTrue(tempDir.exists());
        assertFalse(tempFile1.exists());
        assertTrue(tempFile2.exists());
        
        tempDir.delete();
        tempFile1.delete();
        tempFile2.delete();
    }

    
        /**
     * Test of copyTo method, of class FileUtil.
     */
    @Test
    public void testCopyTo() {
        System.out.println("copyTo");
        assert SAMPLE_MP4.exists() : "Please provide a sample MP4 vidoe for testing";
        
        File source = SAMPLE_MP4;
        File target = generateTempFile();
        FileUtil.copyTo(source, target);
        System.out.println("FADD " +source.length() + " " + target.length());
        assertTrue(source.length() == target.length());
    }

    /**
     * Test of appendBinary method, of class FileUtil.
     */
    @Test
    public void testAppendBinary() {
        System.out.println("appendBinary");
        
        assert SAMPLE_MP4.exists() : "Please provide a sample MP4 vidoe for testing";
        
        File source = SAMPLE_MP4;
        File target = generateTempFile();
        
        if(target.length() != 0) {
            target.delete();
        }
        
        //Cope file
        FileUtil.appendBinary(source, target);
        assertTrue(target.length() == source.length());
        //Cope file again (append with previous data)
        FileUtil.appendBinary(source, target);
        assertTrue(target.length() == source.length()*2);
        
    }

    /**
     * Test of prepDestinationDir method, of class FileUtil.
     */
    @Test
    public void testPrepDestinationDir_0args() {
        System.out.println("prepDestinationDir");
        File result = FileUtil.prepDestinationDir();
        assertTrue(result.exists());
        assertTrue(result.canExecute());
        assertTrue(result.canWrite());
        
    }

    /**
     * Test of prepDestinationDir method, of class FileUtil.
     */
    @Test
    public void testPrepDestinationDir_String() {
        System.out.println("prepDestinationDir");
        File expResult = generateTempFile();
        File result = FileUtil.prepDestinationDir(expResult.getAbsolutePath());
        assertEquals(expResult, result);
        assertTrue(result.exists());
        assertTrue(result.canExecute());
        assertTrue(result.canWrite());
    }

    /**
     * Test of relocateFile method, of class FileUtil.
     */
    @Test
            public void testRelocateFile() {
        System.out.println("relocateFile");
        File one = generateTempFile();
        File two = generateTempFile();
        
        try{
            one.createNewFile();
            two.createNewFile();
        } catch (IOException e){
            System.err.println(e);
        }
        
        String[] files = {
            one.getAbsolutePath(),
            two.getAbsolutePath()
        };
        File destDirectory = generateTempDirectory();
        FileUtil.relocateFile(files, destDirectory.getAbsolutePath());
        
        File oneResult = new File(destDirectory,one.getName());
        File twoResult = new File(destDirectory,two.getName());
        
        assertTrue(destDirectory.exists());
        assertTrue(oneResult.exists());
        assertTrue(twoResult.exists());
    }
    
    
    /**
     * Test of deleteDirectory method, of class FileUtil.
     */
    @Test
    public void testDeleteDirectory() throws Exception{
        System.out.println("deleteDirectory");
        File tempDir = generateTempDirectory();
        File tempFile1 = new File(tempDir,String.valueOf(System.currentTimeMillis())+"one.tmp");
        File tempFile2 = new File(tempDir,String.valueOf(System.currentTimeMillis())+"two.tmp");
        
        try{
            tempFile1.createNewFile();
            tempFile2.createNewFile();
        } catch (IOException e){
            System.err.println(e);
        }
        
        FileUtil.deleteDirectory(tempDir);
        
    }
    
    /**
     * Test of saveObjectDataToFile method, of class FileUtil.
     */
    @Test
    public void testSaveObjectDataToFile() {
        System.out.println("saveObjectDataToFile");
        TestSerializable expResult = new TestSerializable();
        String name = "saveObjectDataToFileTest";
        FileUtil.saveObjectDataToFile(expResult, name);
        
        TestSerializable result = (TestSerializable) FileUtil.readObjectDataFromFile(name);
        
        assertEquals(expResult.getInteger(), result.getInteger());
        assertEquals(expResult.getIntegerArr()[1], result.getIntegerArr()[1]);
        assertTrue(expResult.getString().equalsIgnoreCase(result.getString()));
        
        
    }

    /**
     * Test of deleteDataObject method, of class FileUtil.
     */
    @Test
    public void testDeleteDataObject() {
        System.out.println("deleteDataObject");
        TestSerializable expResult = new TestSerializable();
        String name = "deleteDataObjectTest";
        FileUtil.saveObjectDataToFile(expResult, name);
        
        File file = new File(Settings.SCREEN_CAPTURE_DIR+name);
        assertTrue(file.exists());
        
        FileUtil.deleteDataObject(name);
        
        assertFalse(file.exists());
    }

    /**
     * Test of readObjectDataFromFile method, of class FileUtil.
     */
    @Test
    public void testReadObjectDataFromFile() {
        System.out.println("readObjectDataFromFile");
        TestSerializable expResult = new TestSerializable();
        String name = "readObjectDataFromFileTest";
        FileUtil.saveObjectDataToFile(expResult, name);
        
        TestSerializable result = (TestSerializable) FileUtil.readObjectDataFromFile(name);
        
        assertEquals(expResult.getInteger(), result.getInteger());
        assertEquals(expResult.getIntegerArr()[1], result.getIntegerArr()[1]);
        assertTrue(expResult.getString().equalsIgnoreCase(result.getString()));
    }
    
    
    /**
     * Test of checkMarker method, of class FileUtil.
     */
    @Test
    public void testCheckMarker() {
        System.out.println("checkMarker");
        String name = "checkMarkerTest";
        boolean expResult = false;
        boolean result = FileUtil.checkMarker(name);
        assertEquals(expResult, result);
        
        FileUtil.addMarker(name);
        expResult = true;
        result = FileUtil.checkMarker(name);
        assertEquals(expResult, result);
        
        FileUtil.removeMarker(name);
        expResult = false;
        result = FileUtil.checkMarker(name);
        assertEquals(expResult, result);
        
        
    }

    /**
     * Test of removeMarker method, of class FileUtil.
     */
    @Test
    public void testRemoveMarker() {
        System.out.println("removeMarker");
        String name = "removeMarkerTest";
        
        boolean expResult = false;
        boolean result = FileUtil.checkMarker(name);
        assertEquals(expResult, result);
        
        FileUtil.addMarker(name);
        expResult = true;
        result = FileUtil.checkMarker(name);
        assertEquals(expResult, result);
        
        FileUtil.removeMarker(name);
        expResult = false;
        result = FileUtil.checkMarker(name);
        assertEquals(expResult, result);
    }

    /**
     * Test of addMarker method, of class FileUtil.
     */
    @Test
    public void testAddMarker() {
        System.out.println("addMarker");
        String name = "addMarkerTest";
        boolean expResult = false;
        boolean result = FileUtil.checkMarker(name);
        assertEquals(expResult, result);
        
        FileUtil.addMarker(name);
        expResult = true;
        result = FileUtil.checkMarker(name);
        assertEquals(expResult, result);
        
        FileUtil.removeMarker(name);
        expResult = false;
        result = FileUtil.checkMarker(name);
        assertEquals(expResult, result);
        
    }

    /**
     * Test of saveProperty method, of class FileUtil.
     */
    @Test
    public void testSaveProperty() {
        System.out.println("saveProperty");
        String key = "savePropertyTest";
        String expResult = String.format("%d", RandomUtil.rand.nextInt(1000));
        FileUtil.saveProperty(key, Integer.parseInt(expResult));
        String result = FileUtil.loadProperty(key, "");
        assertTrue(expResult.equalsIgnoreCase(result));
    }

    /**
     * Test of loadProperty method, of class FileUtil.
     */
    @Test
    public void testLoadProperty() {
        System.out.println("loadProperty");
        String key = "loadPropertyTest";
        String expResult = String.format("%d", RandomUtil.rand.nextInt(1000));
        FileUtil.saveProperty(key, Integer.parseInt(expResult));
        String result = FileUtil.loadProperty(key, "");
        assertTrue(expResult.equalsIgnoreCase(result));
    }

    /**
     * Test of previousRecordingExists method, of class FileUtil.
     * Tests screen capture directory for a folder which have children. 
     * This test generates a folder in the screen capture directory with
     * children which should trigger this test. 
     */
    @Test
    public void testPreviousRecordingExists() {
        System.out.println("previousRecordingExists");
        boolean expResult = false;
        boolean result = FileUtil.previousRecordingExists();
        
        assertEquals(expResult, result);
        
        File directory = new File(Settings.SCREEN_CAPTURE_DIR,String.valueOf(System.currentTimeMillis()));
        directory.mkdirs();
        directory.deleteOnExit();
        
        try {
            for(int i=0; i < 25; i++){
                File f = new File(directory, "subFile"+i+".tmp");
                
                if(RandomUtil.rand.nextBoolean()){
                    
                    f.mkdirs();
                    f.deleteOnExit();
                    
                    File subF = new File(f, "subFile"+i+".tmp");
                    subF.createNewFile();
                    subF.deleteOnExit();
                    System.out.println(subF.getAbsoluteFile());
                    
                } else {
                    f.createNewFile();
                    f.deleteOnExit();
                    System.out.println(f.getAbsoluteFile());
                    
                }
                
            }
        } catch (IOException e) {
            System.err.println(e);
        }
        
        expResult = true;
        result = FileUtil.previousRecordingExists();
        assertEquals(expResult, result);
        
    }
    
    //FFMpeg Encoding Tests 

    /**
     * Test of mergeVideoMp4 method, of class FileUtil.
     */
    @Test
    public void testMergeVideoMp4() throws Exception {
        System.out.println("mergeVideoMp4");
        
        assert SAMPLE_MP4.exists() : "Please provide a sample MP4 vidoe for testing";
        assert FFMPEG_BIN.exists() : "FFMpeg is needed for this test";
        
        String prevFilePath = SAMPLE_MP4.getAbsolutePath();
        String newFilePath = SAMPLE_MP4.getAbsolutePath();
        FFMpegProgressBarListener ffmpegListener = null;
        
        Integer prevVideoDuration = FileUtil.getVideoDuration(prevFilePath);
        String result = FileUtil.mergeVideoMp4(prevFilePath, newFilePath, ffmpegListener);
        Integer currVideoDuration = FileUtil.getVideoDuration(result);
        
        assertTrue(prevVideoDuration < currVideoDuration);
        
        new File(result).delete();
    }

    /**
     * Test of mergeVideoMpgTo method, of class FileUtil.
     */
    @Test
    public void testMergeVideoMpgTo() {
        System.out.println("mergeVideoMpgTo");
        assert SAMPLE_MP4.exists() : "Please provide a sample MP4 vidoe for testing";
        assert FFMPEG_BIN.exists() : "FFMpeg is needed for this test";
        assert HANBRAKE_BIN.exists() : "Handbrake is needed for this test";
        
        File sourceVideo1 = FileUtil.convertMp4ToMpg(SAMPLE_MP4, null);
        File sourceVideo2 = FileUtil.convertMp4ToMpg(SAMPLE_MP4, null);
        //Create temp file to store merged video
        File targetVideo = generateTempFile("mpg");
        FFMpegProgressBarListener ffmpegListener = null;
        //Merge video
        File mergedVideoMPG = FileUtil.mergeVideoMpgTo(sourceVideo1, sourceVideo2, targetVideo, ffmpegListener);
        //Convert merged video back to MP4 video format
        File mergedVideoMP4 = FileUtil.convertMpgToMp4(targetVideo, ffmpegListener);
        
        Integer sourceVideo1Duration = FileUtil.getVideoDuration(sourceVideo1.getAbsolutePath());
        Integer sourceVideo2Duration = FileUtil.getVideoDuration(sourceVideo2.getAbsolutePath());
        Integer mergedVideoDuration  = FileUtil.getVideoDuration(mergedVideoMP4.getAbsolutePath());
        
        //Confirm merged video is larger than both the source videos in size and duration
        assert sourceVideo1Duration < mergedVideoDuration : "SV1: " + sourceVideo1Duration + " MV: " + mergedVideoDuration;
        assertTrue(sourceVideo1.length() < mergedVideoMPG.length());
        assertTrue(sourceVideo2Duration < mergedVideoDuration);
        assertTrue(sourceVideo2.length() < mergedVideoMPG.length());
        
        
        sourceVideo1.delete();
        sourceVideo2.delete();
        targetVideo.delete();
        mergedVideoMP4.delete();
        mergedVideoMPG.delete();
        
    }

    /**
     * Test of encodeVideoMp4 method, of class FileUtil.
     */
    @Test
    public void testEncodeVideoMp4() throws Exception {
        System.out.println("encodeVideoMp4");
        
        assert SAMPLE_MOV.exists() : "Please provide a sample MOV vidoe for testing";
        assert SAMPLE_WAV.exists() : "Please provide a sample WAV vidoe for testing";
        assert FFMPEG_BIN.exists() : "FFMpeg is needed for this test";
        
        
        String filePath = SAMPLE_MOV.getAbsolutePath();
        long offset = 0L;
        Integer bitrate = 1040000;
        FFMpegProgressBarListener ffmpegListener = null;
        String result = FileUtil.encodeVideoMp4(filePath, offset, bitrate, ffmpegListener);
        
        Integer sampleDuration = FileUtil.getVideoDuration(SAMPLE_MOV.getAbsolutePath());
        Integer resultDuration = FileUtil.getVideoDuration(result);
        
        assertTrue(Math.abs(sampleDuration - resultDuration) < 2);
        
    }

    /**
     * Test of encodeVideoAvi method, of class FileUtil.
     */
    @Test
    public void testEncodeVideoAvi() throws Exception {
        System.out.println("encodeVideoAvi");
        assert SAMPLE_MOV.exists() : "Please provide a sample MOV vidoe for testing";
        assert SAMPLE_WAV.exists() : "Please provide a sample WAV vidoe for testing";
        assert FFMPEG_BIN.exists() : "FFMpeg is needed for this test";
        
        String filePath = SAMPLE_MOV.getAbsolutePath();
        long offset = 0L;
        Integer bitrate = 1040000;
        FFMpegProgressBarListener ffmpegListener = null;

        String result = FileUtil.encodeVideoAvi(filePath, offset, bitrate, ffmpegListener);
        
        Integer sampleDuration = FileUtil.getVideoDuration(SAMPLE_MOV.getAbsolutePath());
        Integer resultDuration = FileUtil.getVideoDuration(result);
        
        assertTrue(Math.abs(sampleDuration - resultDuration) < 2);
    }

    /**
     * Test of compressAudio method, of class FileUtil.
     */
    @Test
    public void testCompressAudio() {
        System.out.println("compressAudio");
        
        assert SAMPLE_WAV.exists() : "CompressAudio test requires a sample WAV audio file.";
        assert FFMPEG_BIN.exists() : "FFMpeg is needed for this test";
        
        
        File source = SAMPLE_WAV;
        File target = new File(source.getAbsolutePath().replace(".wav", ".mp3"));
        float offset = 0.0F;
        FileUtil.compressAudio(source, target, offset);
        
        Integer sampleDuration = FileUtil.getVideoDuration(SAMPLE_WAV.getAbsolutePath());
        Integer resultDuration = FileUtil.getVideoDuration(target.getAbsolutePath());
        
        assertTrue(Math.abs(sampleDuration - resultDuration) < 2);
    }

    /**
     * Test of compressVideoAvi method, of class FileUtil.
     */
    @Test
    public void testCompressVideoAvi() {
        System.out.println("compressVideoAvi");
        File source = SAMPLE_AVI;
        File target = generateTempFile(".mp4");
        File audioFile = SAMPLE_WAV;
        Integer bitrate = 1040000;
        FFMpegProgressBarListener ffmpegListener = null;
        FileUtil.compressVideoAvi(source, target, audioFile, bitrate, ffmpegListener);
        
        Integer sampleDuration = FileUtil.getVideoDuration(SAMPLE_AVI.getAbsolutePath());
        Integer resultDuration = FileUtil.getVideoDuration(target.getAbsolutePath());

        assert Math.abs(sampleDuration - resultDuration) < 10 : "Diff: " + Math.abs(sampleDuration - resultDuration);
    
    }

    /**
     * Test of compressVideo method, of class FileUtil.
     */
    @Ignore
    public void testCompressVideo() {
        System.out.println("compressVideo");
        
        assert SAMPLE_MOV.exists() : "Please provide a sample MOV vidoe for testing";
        assert SAMPLE_WAV.exists() : "Please provide a sample WAV vidoe for testing";
        assert FFMPEG_BIN.exists() : "FFMpeg is needed for this test";
        assert MediaUtil.osIsWindows() : "This test only works on Window, Linux is untested, MAC OS X does not work";
        
        
        String filePath = SAMPLE_MOV.getAbsolutePath();
        long offset = 0L;
        Integer bitrate = 1040000;
        FFMpegProgressBarListener ffmpegListener = null;
        FileUtil.compressVideo(SAMPLE_MOV, SAMPLE_MP4, SAMPLE_WAV, bitrate, ffmpegListener);
        
        Integer sampleDuration = FileUtil.getVideoDuration(SAMPLE_MOV.getAbsolutePath());
        Integer resultDuration = FileUtil.getVideoDuration(SAMPLE_MP4.getAbsolutePath());
        
        assertTrue(Math.abs(sampleDuration - resultDuration) < 10);
        
    }

    /**
     * Test of convertMpgToMp4 method, of class FileUtil.
     */
    @Test
    public void testConvertMpgToMp4() {
        System.out.println("convertMpgToMp4");
        
        assert SAMPLE_MPG2.exists() : "Please provide a sample MPG vidoe for testing";
        assert HANBRAKE_BIN.exists() : "Handbrake is needed for this test";
        
        
        File source = SAMPLE_MPG2;
        FFMpegProgressBarListener ffmpegListener = null;
        FileUtil.convertMpgToMp4(source, ffmpegListener);
        
        Integer sampleDuration = FileUtil.getVideoDuration(SAMPLE_MPG2.getAbsolutePath());
        Integer resultDuration = FileUtil.getVideoDuration(SAMPLE_MP4.getAbsolutePath());
        
        assert Math.abs(sampleDuration - resultDuration) < 10 : "Expected: < 10; Sample: " + sampleDuration + " Result: " + resultDuration;
        
    }

    /**
     * Test of convertMp4ToMpg method, of class FileUtil.
     */
    @Test
    public void testConvertMp4ToMpg() {
        System.out.println("convertMp4ToMpg");
        
        assert SAMPLE_MP4.exists() : "Please provide a sample MP4 vidoe for testing";
        assert FFMPEG_BIN.exists() : "FFMpeg is needed for this test";
        
        FFMpegProgressBarListener ffmpegListener = null;
        FileUtil.convertMp4ToMpg(SAMPLE_MP4, ffmpegListener);
        
        Integer sampleDuration = FileUtil.getVideoDuration(SAMPLE_MP4.getAbsolutePath());
        Integer resultDuration = FileUtil.getVideoDuration(SAMPLE_MPG.getAbsolutePath());
        
        assertTrue(Math.abs(sampleDuration - resultDuration) < 10);
    }

    /**
     * Test of extractMpgClip method, of class FileUtil.
     */
    @Test
    public void testExtractMpgClip() {
        System.out.println("extractMpgClip");
        
        assert SAMPLE_MPG.exists() : "Please provide a sample MPG vidoe for testing";
        assert FFMPEG_BIN.exists() : "FFMpeg is needed for this test";
        assert FileUtil.getVideoDuration(SAMPLE_MPG.getAbsolutePath())> 5 : 
                "Please provide a MPG video file longer than 5 seconds";
        
        File source = SAMPLE_MPG;
        File target = generateTempFile("mpg");
        int offset = 0;
        int duration = 5;
        FFMpegProgressBarListener ffmpegListener = null;
        FileUtil.extractMpgClip(source, target, offset, duration, ffmpegListener);
        
        
        assertTrue(Math.abs(FileUtil.getVideoDuration(target.getAbsolutePath()) - duration) < 2);
        
    }

    /**
     * Test of getVideoDuration method, of class FileUtil.
     */
    @Test
    public void testGetVideoDuration() {
        System.out.println("getVideoDuration");
        
        assert SAMPLE_MP4.exists() : "Please provide a sample MP4 vidoe for testing";
        assert FFMPEG_BIN.exists() : "FFMpeg is needed for this test";
        
        String videoFile = SAMPLE_MP4.getAbsolutePath();
        Integer result = FileUtil.getVideoDuration(videoFile);
        
        assertTrue(result > 0);
    }

    /**
     * Test of getVideoAspectRatio method, of class FileUtil.
     */
    @Test
    public void testGetVideoAspectRatio() {
        System.out.println("getVideoAspectRatio");
        
        assert SAMPLE_MP4.exists() : "Please provide a sample MP4 vidoe for testing";
        assert FFMPEG_BIN.exists() : "FFMpeg is needed for this test";
        
        String videoFile = SAMPLE_MP4.getAbsolutePath();
        FileUtil instance = FileUtil.getInstance();
        int[] result = instance.getVideoAspectRatio(videoFile);
        
        assertNotNull(result);
        assertTrue(result[0] > 0);
        assertTrue(result[1] > 0);
    }

    
    /**
     */
    @Test
    public void testExtractMpgLastClip() {
        System.out.println("extractMpgLastClip");
        
        assert SAMPLE_MPG.exists() : "Please provide a sample MPG vidoe for testing";
        assert FFMPEG_BIN.exists() : "FFMpeg is needed for this test";
        assert FileUtil.getVideoDuration(SAMPLE_MPG.getAbsolutePath()) > 5 : 
                "Please provide a MPG video file longer than 5 seconds";
        
        File input = SAMPLE_MPG;

        File outputA = generateTempFile("mpg");
        File outputB = generateTempFile("mpg");
        
        outputA.deleteOnExit();
        outputB.deleteOnExit();

        // Clip 0 to 5 seconds
        FileUtil.extractMpgLastClip(input, outputA, 0, 5, null);
        // Clip 5 to 10 seconds
        FileUtil.extractMpgLastClip(input, outputB, 5, 5, null);
        
        System.out.println("Output A path: " + outputA.getAbsolutePath());
        System.out.println("Output B path: " + outputB.getAbsolutePath());
        assert Math.abs(FileUtil.getVideoDuration(outputA.getAbsolutePath()) - 5) <= 2 : "Expected: <= 2 ; Result: " + Math.abs(FileUtil.getVideoDuration(outputA.getAbsolutePath()) - 5);
        assert Math.abs(FileUtil.getVideoDuration(outputB.getAbsolutePath()) - 5) <= 2 : "Expected: <= 2 ; Result: " + Math.abs(FileUtil.getVideoDuration(outputB.getAbsolutePath()) - 5);

    }

    /**
     */
    @Test
    public void testConvertVideoAviToMp4() {
        System.out.println("extractMpgLastClip");
        
        assert SAMPLE_AVI.exists() : "Please provide a sample AVI vidoe for testing";
        assert FFMPEG_BIN.exists() : "FFMpeg is needed for this test";

        File input = SAMPLE_AVI;

        FileUtil.convertVideoAviToMp4(input, null);
        
        
        Integer sampleDuration = FileUtil.getVideoDuration(SAMPLE_AVI.getAbsolutePath());
        Integer resultDuration = FileUtil.getVideoDuration(SAMPLE_MP4.getAbsolutePath());
        
        assertTrue(Math.abs(sampleDuration - resultDuration) < 2);

    }

}
