/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.models;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author Matias
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({com.bixly.pastevid.models.WaterMarkTest.class, com.bixly.pastevid.models.AudioFileItemTest.class, com.bixly.pastevid.models.UserTest.class, com.bixly.pastevid.models.SilentTest.class, com.bixly.pastevid.models.VideoFileItemTest.class, com.bixly.pastevid.models.ScreenSizeTest.class})
public class ModelSuite {

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
