package com.bixly.pastevid.recorders;

import com.bixly.pastevid.util.LogUtil;
import com.bixly.pastevid.models.AudioFileItem;
import com.bixly.pastevid.screencap.components.IAudioObserver;
import com.bixly.pastevid.screencap.components.IAudioSubject;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Bixly
 */
public class AudioRecorderTest {
    
    private Recorder recorder;
    public AudioRecorderTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        ImageIcon dummy = new ImageIcon(); // Preload the ImageIcon library
        recorder = new Recorder(null);
    }
    
    @After
    public void tearDown() {
    }
    
    private static void log(Object string) {
        LogUtil.log(AudioRecorderTest.class,string);
    }

    /**
     * Test of getStartMS method, of class AudioRecorder.
     */
    @Test
    public void testGetStartMS() {
        log("getStartMS");
        AudioRecorder instance = recorder.getAudioRecorder();
        long expResult = 0L;
        long result = instance.getStartMS();
        assertEquals(expResult, result);
    }

    /**
     * Test of isLineOpen method, of class AudioRecorder.
     */
    @Test
    public void testIsLineOpen() {
        log("isLineOpen");
        AudioRecorder instance = recorder.getAudioRecorder();
        Boolean result = instance.isLineOpen();
        assertNotNull(result);
    }

    /**
     * Test of openLine method, of class AudioRecorder.
     */
    @Test
    public void testOpenLine() {
        log("openLine");
        AudioRecorder instance = recorder.getAudioRecorder();
        instance.openLine();
        
        assertNotNull(instance.getTargetDataLine());
    }

    /**
     * Test of monitorOpenLine method, of class AudioRecorder.
     */
    @Ignore
    public void testMonitorOpenLine() {
        //can't be tested
    }

    /**
     * Test of compileAudio method, of class AudioRecorder.
     */
    @Ignore
    public void testCompileAudio() {
        
        fail("The test case is a prototype.");
    }

    /**
     * Test of record method, of class AudioRecorder.
     */
    @Ignore
    public void testRecord() {
        
        fail("The test case is a prototype.");
    }

    /**
     * Test of closeLine method, of class AudioRecorder.
     */
    @Test
    public void testCloseLine() {
        log("closeLine");
        AudioRecorder instance = recorder.getAudioRecorder();
        instance.closeLine();
        
        Boolean expResult = false;
        Boolean result = instance.isLineOpen();
        assertEquals(expResult, result);
    }

    /**
     * Test of stopRecording method, of class AudioRecorder.
     */
    @Test
    public void testStopRecording() {
        log("stopRecording");
        AudioRecorder instance = recorder.getAudioRecorder();
        instance.stopRecording();
        
        Boolean expResult = false;
        Boolean result = instance.isRecording();
        assertEquals(expResult, result);
    }

    /**
     * Test of hasRecorded method, of class AudioRecorder.
     */
    @Ignore
    public void testHasRecorded() {
        log("hasRecorded");
        
        fail("The test case is a prototype.");
    }

    /**
     * Test of setAudioFiles method, of class AudioRecorder.
     */
    @Test
    public void testSetAudioFiles() {
        log("setAudioFiles");
        ArrayList<AudioFileItem> audioFiles = new ArrayList<AudioFileItem>();
        AudioRecorder instance = recorder.getAudioRecorder();
        instance.setAudioFiles(audioFiles);
        assertNotNull(instance.getAudioFiles());
    }

    /**
     * Test of getVolume method, of class AudioRecorder.
     */
    @Test
    public void testGetVolume() {
        log("getVolume");
        AudioRecorder instance = recorder.getAudioRecorder();
        int expResult = 0;
        int result = instance.getVolume();
        assertEquals(expResult, result);
    }

    /**
     * Test of getMaxVolume method, of class AudioRecorder.
     */
    @Test
    public void testGetMaxVolume() {
        log("getMaxVolume");
        AudioRecorder instance = recorder.getAudioRecorder();
        int result = AudioRecorder.MAX_VOLUME;
        assertNotNull(result);
    }

    /**
     * Test Observers methods of class AudioRecorder.
     */
    boolean notified = false;
    @Test
    public void testObservers() {
        log("add, notify, remove observers");
        notified = false;
        AudioRecorder instance = recorder.getAudioRecorder();
        int initialSize = instance.getObservers().size();
        
        IAudioObserver obs = new IAudioObserver() {
            public void update(IAudioSubject subject) {
                notified = true;
            }
        };
                
        instance.addObserver(obs);
        assertNotSame(initialSize, instance.getObservers().size());
        
        instance.notifyObservers();
        assertEquals(notified, true);
        
        instance.removeObserver(obs);
        assertEquals(initialSize, instance.getObservers().size());
    }

    /**
     * Test of isCompiling method, of class AudioRecorder.
     */
    @Test
    public void testIsCompiling() {
        log("isCompiling");
        AudioRecorder instance = recorder.getAudioRecorder();
        Boolean expResult = false;
        Boolean result = instance.isCompiling();
        assertEquals(expResult, result);
    }

    /**
     * Test of setCompiling method, of class AudioRecorder.
     */
    @Test
    public void testSetCompiling() {
        log("setCompiling");
        Boolean compiling = true;
        AudioRecorder instance = recorder.getAudioRecorder();
        instance.addObserver(new IAudioObserver() {
            public void update(IAudioSubject subject) {
                assertTrue(subject.isCompiling());
            }
        });
        instance.setCompiling(compiling);
    }
    
    
    /**
     * Test of getCache method, of class AudioRecorder.
     */
    @Test
    public void testGetCache() {
        log("getCache");
        AudioRecorder instance = recorder.getAudioRecorder();
        assertNotNull(instance.getCache());
    }
    
    
}
