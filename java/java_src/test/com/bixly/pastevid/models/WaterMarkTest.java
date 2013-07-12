package com.bixly.pastevid.models;

import com.bixly.pastevid.util.LogUtil;
import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Bixly
 */
public class WaterMarkTest {

    private File captureDir;

    public WaterMarkTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        String captureDirStr = System.getProperty("java.io.tmpdir")
                + System.getProperty("file.separator")
                + System.currentTimeMillis();

        captureDir = new File(captureDirStr);
        captureDir.mkdir();
    }

    @After
    public void tearDown() {
    }
    
    private static void log(Object string) {
        LogUtil.log(WaterMarkTest.class,string);
    }

    /**
     * Test of copyWaterMark method, of class WaterMark.
     */
    @Test
    public void testCopyWaterMark() {
        log("WaterMark.copyWaterMark");
        if (captureDir.exists()) {
            File result = WaterMark.copyWaterMark(captureDir.getAbsolutePath());
            assertNotNull(result);
            assertTrue(result.exists());

            result.delete();
        } else {
            assertFalse("Directory doesn't exists", true);
        }
    }
}
