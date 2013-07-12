/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.util;

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
public class ResourceUtilTest {

    public ResourceUtilTest() {
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

    @Test
    public void testResourceExists() {

        System.out.println("resourceExists");

        assertNotNull(getClass().getResource(ResourceUtil.BUTTON_LARGE_PRESSED));
        assertNotNull(getClass().getResource(ResourceUtil.BUTTON_LARGE_UNPRESSED));
        assertNotNull(getClass().getResource(ResourceUtil.LOGO_24));
        assertNotNull(getClass().getResource(ResourceUtil.LOGO_TASKBAR));
        assertNotNull(getClass().getResource(ResourceUtil.NORMAL_CURSOR));
        assertNotNull(getClass().getResource(ResourceUtil.PAUSE));
        assertNotNull(getClass().getResource(ResourceUtil.PAUSE_PRESSED));
        assertNotNull(getClass().getResource(ResourceUtil.RECORD));
        assertNotNull(getClass().getResource(ResourceUtil.SCREENBIRD_LOGO));
        assertNotNull(getClass().getResource(ResourceUtil.SILENT));
        assertNotNull(getClass().getResource(ResourceUtil.SLIDER_KNOB));
        assertNotNull(getClass().getResource(ResourceUtil.SPINNER_24x24));
        assertNotNull(getClass().getResource(ResourceUtil.SPINNER_28x28));
        assertNotNull(getClass().getResource(ResourceUtil.SPINNER_30));
        assertNotNull(getClass().getResource(ResourceUtil.SPINNER_32x32));

    }
}
