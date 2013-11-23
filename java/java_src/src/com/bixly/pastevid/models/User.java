/*
 * User.java
 * 
 * Version 1.0
 * 
 * 7 May 2013
 */
package com.bixly.pastevid.models;

/**
 * Model class for the Screenbird user.
 * @author Jorge
 */
public class User {
    // Key name definitions for the user properties.
    public static final String CSRF_TOKEN = "csrf_token";
    public static final String BASE_URL = "base_url";
    public static final String ANON_TOKEN = "an_tok";
    public static final String CHANNEL_ID = "channel_id";
    public static final String USER_ID = "user_id";
    public static final String SLUG = "slug";
    
    // Screenbird User ID which the recorded video will be associated.
    private String userId    = "";
    
    // Base URL where source files will be downloaded. Basically, the web site
    // that initialized the screen recorder. 
    private String baseURL   = "";
    
    // CSRF token from Screenbird for sending POST data to Screenbird.
    private String csrfToken = "";
    
    // Anonymous token from Screenbird for identifying an unlogged Screenbird user.
    private String anonToken = "";
    
    // Screenbird Channel ID where the recorded video will be uploaded. Can be null.
    private String channelId  = "";
    
    // Slug reserved from Screenbird that will identify the recorded video once
    // uploaded to Screenbird.
    private String slug = "";
    

    /**
     * Returns the anonymous token associated with this User.
     * @return 
     */
    public String getAnonToken() {
        return anonToken;
    }

    /**
     * Returns the base URL associated with this User.
     * @return 
     */
    public String getBaseURL() {
        return baseURL;
    }

    /**
     * Returns the CSRF token associated with this User.
     * @return 
     */
    public String getCsrfToken() {
        return csrfToken;
    }

    /**
     * Returns the user ID associated with this User.
     * @return 
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the channel ID associated with this User.
     * @return 
     */
    public String getChannelId() {
        return channelId;
    }
    
    /**
     * Returns the slug associated with this User.
     * @return 
     */
    public String getSlug() {
        return slug;
    }

    /**
     * Sets the anonymous token for this User.
     * @param anonToken The anonymous token given by screenbird for this user.
     */
    public void setAnonToken(String anonToken) {
        this.anonToken = anonToken;
    }

    /**
     * Sets the base URL for this User.
     * @param baseURL The URL where the screen recorder files will be downloaded.
     */
    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    /**
     * Sets the CSRF token for this User.
     * @param csrfToken The csrf token from Screenbird to be used for POST 
     * requests to the server.
     */
    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }

    /**
     * Sets the user ID for this User.
     * @param userId The user's Screenbird ID.
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Sets the channel ID for this User.
     * @param channelId The channel's Screenbird ID where the recording will be
     * uploaded to.
     */
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
    
    /**
     * Sets the slug for this User.
     * @param slug The video's Screenbird slug.
     */
    public void setSlug(String slug) {
        this.slug = slug;
    }
    
    /**
     * Returns a String representation of this User object, showing information
     * on base URL, anonymous token, CSRF token, channel ID, user ID, and video
     * slug.
     * @return 
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        String baseURLLine = BASE_URL + ": " + this.baseURL + "\n";
        String anonTokenLine = ANON_TOKEN + ": " + this.anonToken + "\n";
        String csrfTokenLine = CSRF_TOKEN + ": " + this.csrfToken + "\n";
        String channelIDLine = CHANNEL_ID + ": " + this.channelId + "\n";
        String userIDLine = USER_ID + ": " + this.userId + "\n";
        String slugLine = SLUG + ": " + this.slug + "\n";
        
        str.append("\n");
        str.append(baseURLLine);
        str.append(anonTokenLine);
        str.append(csrfTokenLine);
        str.append(channelIDLine);
        str.append(userIDLine);
        str.append(slugLine);
        return str.toString();       
    }
}
