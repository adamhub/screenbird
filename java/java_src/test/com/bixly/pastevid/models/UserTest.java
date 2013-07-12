/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.models;

import com.bixly.pastevid.util.LogUtil;
import java.util.HashMap;
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
public class UserTest {
    
    public UserTest() {
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
    
    static {
        HashMap<String, String> parameters = new HashMap<String, String>();
    }
    
    private static void log(Object string) {
        LogUtil.log(UserTest.class,string);
    }
    
    /**
     * Generates a temporary User model instance for JUnit testing
     * Only should be used for JUnit Tests!!!
     * @return 
     */
    private User generateTempUser(){
        
        User user = new User();
        user.setBaseURL("http://staging.screenbird.com/");
        user.setAnonToken("ZXCVBNMASDFGHJKLQWERTYUIOP");
        user.setCsrfToken("QWERTYUIOPASDFGHJKLZXCVBNM");
        user.setUserId("10");
        return user;
    }

    /**
     * Test of getAnonToken method, of class User.
     */
    @Test
    public void testGetAnonToken() {
        log("getAnonToken");
        User instance = generateTempUser();
        String expResult = "ZXCVBNMASDFGHJKLQWERTYUIOP";
        String result = instance.getAnonToken();
        assertEquals(expResult, result);
    }

    /**
     * Test of getBaseURL method, of class User.
     */
    @Test
    public void testGetBaseURL() {
        log("getBaseURL");
        User instance = generateTempUser();
        String expResult = "http://staging.screenbird.com/";
        String result = instance.getBaseURL();
        assertEquals(expResult, result);
    }

    /**
     * Test of getCsrfToken method, of class User.
     */
    @Test
    public void testGetCsrfToken() {
        log("getCsrfToken");
        User instance = generateTempUser();
        String expResult = "QWERTYUIOPASDFGHJKLZXCVBNM";
        String result = instance.getCsrfToken();
        assertEquals(expResult, result);
    }

    /**
     * Test of getUserId method, of class User.
     */
    @Test
    public void testGetUserId() {
        log("getUserId");
        User instance = generateTempUser();
        String expResult = "10";
        String result = instance.getUserId();
        assertEquals(expResult, result);
    }

    /**
     * Test of setAnonToken method, of class User.
     */
    @Test
    public void testSetAnonToken() {
        log("setAnonToken");
        User instance = generateTempUser();
        assertTrue(instance.getAnonToken().length() > 0);
    }

    /**
     * Test of setBaseURL method, of class User.
     */
    @Test
    public void testSetBaseURL() {
        log("setBaseURL");
        User instance = generateTempUser();
        assertTrue(instance.getBaseURL().length() > 0);
    }

    /**
     * Test of setCsrfToken method, of class User.
     */
    @Test
    public void testSetCsrfToken() {
        log("setCsrfToken");
        User instance = generateTempUser();
        assertTrue(instance.getCsrfToken().length() > 0);
    }

    /**
     * Test of setUserId method, of class User.
     */
    @Test
    public void testSetUserId() {
        log("setUserId");
        User instance = generateTempUser();
        assertTrue(instance.getUserId().length() > 0);
    }
}
