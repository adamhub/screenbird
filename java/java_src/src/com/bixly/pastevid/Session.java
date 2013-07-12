/*
 * Session.java
 *
 * Version 1.0
 * 
 * 4 May 2013
 */
package com.bixly.pastevid;

import com.bixly.pastevid.models.User;

/**
 * Class for the current user session.
 * @author cevaris
 */
public class Session {
    /**
     * User associated with this session.
     */
    public final User user = new User();
    
    private static Session session;

    /**
     * Returns a user session object. Creates one if none found.
     * @return 
     */
    public static Session getInstance() {
        if (session == null) {
            session = new Session();
        }
        return session;
    }
    
}
