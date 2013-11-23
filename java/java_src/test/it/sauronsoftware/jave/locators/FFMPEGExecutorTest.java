/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.sauronsoftware.jave.locators;

import com.bixly.pastevid.util.LogUtil;
import com.bixly.pastevid.screencap.components.progressbar.FFMpegProgressBarListener;
import java.io.File;
import javax.swing.JProgressBar;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author cevaris
 */
public class FFMPEGExecutorTest {
    
    protected static final int myEXEversion = 1;
    private   String separator = System.getProperty("file.separator");
    
    
    public FFMPEGExecutorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    private static void log(Object string) {
        LogUtil.log(FFMPEGExecutorTest.class,string);
    }
    
    
    /**
     * Test of catching FFMpeg executable errors method, of class FFMPEGExecutor.
     */
    //@Ignore
    @Test
    public void testFFMpegExecutable() throws Exception {
        log("FFMpegExecutable");
        
        File temp = new File(System.getProperty("java.io.tmpdir"), "/jave-"+ myEXEversion);
        if (!temp.exists()) { temp.mkdirs(); }
        // ffmpeg executable export on disk.
        String ffmpegName = "ffmpeg";
        File exe = new File(temp, ffmpegName);
        
        FFMPEGExecutor ffmpeg = FFMPEGLocator.getLocator().createExecutor();
        
        if(!ffmpeg.isExecutable()){
            ffmpeg.setAsExecutable();
        }
        
        assertTrue(ffmpeg.isExecutable());
        
        exe.delete();
        
        //assertFalse(ffmpeg.isExecutable());
        assertFalse(exe.canExecute());
        assertFalse(exe.exists());
        
    }


    /**
     * Test of setProgressBarListener method, of class FFMPEGExecutor.
     */
    @Test
    public void testSetProgressBarListener() {
        log("setProgressBarListener");
        FFMpegProgressBarListener ffmpegListener = new FFMpegProgressBarListener(new JProgressBar(), 1);
        FFMPEGExecutor instance = new FFMPEGExecutor(FFMPEGLocator.getLocator().getFFMPEGExecutablePath());
        instance.setProgressBarListener(ffmpegListener);
        assertNotNull(instance.getFFMpegProgressBarListener());
    }
}
