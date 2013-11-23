/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.ipc;

import com.bixly.pastevid.Settings;
import com.bixly.pastevid.screencap.ScreenRecorderController;
import com.bixly.pastevid.util.TimeUtil;
import java.io.IOException;
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
public class InstanceManagerTest {
    
    public InstanceManagerTest() {
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
    
    private class FakeScreenRecorder implements ScreenRecorderController {
        
        public void bringToFocus() {
        }

        public void initRecorder(boolean show, boolean recovery) {
        }

        public void destroy() {
        }

        public void controlSetLocation(int x, int y) {
        }

        public void controlSetVisible(boolean value) {
        }

        public void controlSetAlwaysOnTop(boolean value) {
        }

        public void controlPack() {
        }
        
    }


    /**
     * Test of close method, of class InstanceManager.
     */
    @Test
    public void testClose() {
        System.out.println("close");
        InstanceManager instance = new InstanceManager(new FakeScreenRecorder());
        instance.start();
        
        TimeUtil.skipToMyLou(1);
        
        assertTrue(instance.isRunning());
        
        instance.close();
        
        assertFalse(instance.isRunning());
        
    }
    
    
    /**
     * Test of isRunning method, of class InstanceManager.
     */
    @Test
    public void testIsRunning() {
        System.out.println("isRunning");
        InstanceManager instance = new InstanceManager(new FakeScreenRecorder());
        instance.start();
        
        TimeUtil.skipToMyLou(1);
        
        assertTrue(instance.isRunning());
        
        instance.close();
    }
    
    @Test
    public void testCaptureFlag(){
        
        System.out.println("captureFlag");
        InstanceManager instance = new InstanceManager(new FakeScreenRecorder());
        instance.start();
        
        TimeUtil.skipToMyLou(1);
        
        assertFalse(Settings.FLAG_FILE.exists());
        
        try {
            //Create the flag file
            assertTrue(Settings.FLAG_FILE.createNewFile());
            
        } catch(IOException e){
            System.err.println("Could not create Flag File for JUnit test");
        }
        //Wait 3 seconds for Instance Manager to detect and delete Flag File
        TimeUtil.skipToMyLou(3);
        
        //Confirm delete of flag file
        assertFalse(Settings.FLAG_FILE.exists());
        
        instance.close();
        
    }
    
    @Test
    public void testLockFile(){
        
        System.out.println("lockFile");
        InstanceManager instance = new InstanceManager(new FakeScreenRecorder());
        
        Settings.LOCK_FILE.delete();
        
        instance.start();
        
        TimeUtil.skipToMyLou(1);
        
        //Confirm Lock file was created
        assertTrue(Settings.LOCK_FILE.exists());
        
        instance.close();
        
    }
    
    
    

}
