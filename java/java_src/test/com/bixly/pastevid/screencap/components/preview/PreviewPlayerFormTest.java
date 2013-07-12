/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.screencap.components.preview;

import com.bixly.pastevid.editors.VideoScrubManager;
import com.bixly.pastevid.util.TimeUtil;
import java.awt.Point;
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
public class PreviewPlayerFormTest {
    
    private static PreviewPlayerForm instance;
    private static VideoScrubManager scrubManager;
    
    public PreviewPlayerFormTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        scrubManager = new VideoScrubManager();
        instance = new PreviewPlayerForm(scrubManager);
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

    /**
     * Test of getPlayerPanel method, of class PreviewPlayerForm.
     */
    @Test
    public void testGetPlayerPanel() {
        System.out.println("getPlayerPanel");
        assertNotNull(instance.getPlayerPanel());
    }

    /**
     * Test of showPlayer method, of class PreviewPlayerForm.
     */
    @Test
    public void testShowPlayer_Point() {
        System.out.println("showPlayer");
        Point playLocation = new Point(0,0);
        instance.showPlayer(playLocation);
        TimeUtil.skipToMyLou(2);
        
        playLocation = new Point(200,200);
        instance.showPlayer(playLocation);
        TimeUtil.skipToMyLou(2);
        
        playLocation = new Point(100,100);
        instance.showPlayer(playLocation);
        TimeUtil.skipToMyLou(2);
    }

    /**
     * Test of showPlayer method, of class PreviewPlayerForm.
     */
    @Test
    public void testShowPlayer_0args() {
        System.out.println("showPlayer");
        instance.hidePlayer();
        TimeUtil.skipToMyLou(2);
        instance.showPlayer();
    }

    /**
     * Test of hidePlayer method, of class PreviewPlayerForm.
     */
    @Test
        public void testHidePlayer() {
        System.out.println("hidePlayer");
        instance.hidePlayer();
        TimeUtil.skipToMyLou(2);
        instance.showPlayer();
    }

    /**
     * Test of loadSmartPosition method, of class PreviewPlayerForm.
     */
    @Test
    public void testLoadSmartPosition() {
        System.out.println("loadSmartPosition");
        boolean show = false;
        
        instance.hidePlayer();
        instance.showPlayer(new Point(0,0));
        TimeUtil.skipToMyLou(2);
        
        instance.hidePlayer();
        instance.loadSmartPosition(show);
        instance.showPlayer();
        TimeUtil.skipToMyLou(2);
        
        instance.hidePlayer();
        instance.showPlayer(new Point(0,0));
        TimeUtil.skipToMyLou(2);
        
        instance.hidePlayer();
        instance.loadSmartPosition(true);
        instance.showPlayer();
        TimeUtil.skipToMyLou(2);
        
    }

    /**
     * Test of getFrame method, of class PreviewPlayerForm.
     */
    @Test
    public void testGetFrame() {
        System.out.println("getFrame");
        assertNotNull(instance.getFrame());
    }

    /**
     * Test of getRectangle method, of class PreviewPlayerForm.
     */
    @Test
    public void testGetRectangle() {
        System.out.println("getRectangle");
        assertNotNull(instance.getRectangle());
    }

}
