package com.bixly.pastevid.util;

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
@Suite.SuiteClasses({
    MediaUtilTest.class,
    ScreenUtilTest.class, 
    FileUtilTest.class, 
    ImageUtilTest.class
})
public class UtilsSuite {

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
    
    private static void log(Object string) {
        LogUtil.log(UtilsSuite.class,string);
    }
    
}
