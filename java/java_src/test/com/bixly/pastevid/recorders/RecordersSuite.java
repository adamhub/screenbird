package com.bixly.pastevid.recorders;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Bixly
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({AudioRecorderTest.class, RecorderTest.class, VideoRecorderTest.class})
public class RecordersSuite {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }
    
}
