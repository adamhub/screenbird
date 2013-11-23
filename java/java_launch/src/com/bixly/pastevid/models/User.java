/*
 * User.java
 *
 * Version 1.0
 * 
 * 20 May 2013
 */
package com.bixly.pastevid.models;

/**
 *
 * @author Jorge
 */
public class User {
    
    public static final String CSRF_TOKEN = "csrf_token";
    public static final String BASE_URL   = "base_url";
    public static final String ANON_TOKEN = "an_tok";
    public static final String USER_ID    = "user_id";
    public static final String SLUG = "slug";
    
    private String userId    = "";
    private String baseURL   = "";
    private String csrfToken = "";
    private String anonToken = "";
    private String slug = "";

    public String getAnonToken() {
        return anonToken;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public String getCsrfToken() {
        return csrfToken;
    }

    public String getUserId() {
        return userId;
    }
    
    public String getSlug() {
        return slug;
    }

    public void setAnonToken(String anonToken) {
        this.anonToken = anonToken;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public void setSlug(String slug) {
        this.slug = slug;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("\n");
        str.append(BASE_URL+": "   +this.baseURL   + "\n");
        str.append(ANON_TOKEN+": " +this.anonToken + "\n");
        str.append(CSRF_TOKEN+": " +this.csrfToken + "\n");
        str.append(USER_ID+": "    +this.userId    + "\n");
        str.append(SLUG+": "       +this.slug      + "\n");
        return str.toString();       
    }
    
    
    
    
}
