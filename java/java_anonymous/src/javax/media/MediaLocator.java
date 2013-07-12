/*
 * @(#)MediaLocator.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import java.net.*;

/**
 * <code>MediaLocator</code> describes the location of
 * media content. <code>MediaLocator</code> is closely
 * related to <code>URL</code>. <code>URLs</code>
 * can be obtained from <code>MediaLocators</code>, and
 * <code>MediaLocators</code> can be constructed from
 * <code>URL</code>.
 * Unlike a <code>URL</code>, a <code>MediaLocator</code>
 * can be instanced without a <code>URLStreamHandler</code>
 * installed on the System. 
 * 
 * @see java.net.URL
 * @see java.net.URLStreamHandler
 **/
public class MediaLocator implements java.io.Serializable {
 
    private URL url;
    private String locatorString;

    /**
     * @param url The <CODE>URL</CODE> to construct this media locator from.
     */
    public MediaLocator(URL url) {
	this.url = url;
	// $jdr: Should we check for more stuff that
	// might be wrong in this string?
	locatorString = url.toString().trim();
    }

    /**
     *
     */
    public MediaLocator(String locatorString) {
	this.locatorString = locatorString.trim();
    }

    /**
     * Get the <CODE>URL</CODE> associated with this <CODE>MediaLocator</CODE>.
     */
    public URL getURL() throws MalformedURLException {
	if( url == null) {
	    url = new URL(locatorString);
	}

	return url;
    }

    /**
     * Get the beginning of the locator string
     * up to but not including the first colon.
     * @return The protocol for this <CODE>MediaLocator</CODE>.
     */
    public String getProtocol() {
	String protocol = "";
	int colonIndex = locatorString.indexOf(':');

	// $jdr: Should this throw an exception or
	// return an empty string?
	if( colonIndex != -1) {
	    protocol = locatorString.substring(0,colonIndex);
	}

	return protocol;
    }

    /**
     * Get the <CODE>MediaLocator</CODE> string with the protocol removed.
     *
     * @return The argument string.
     */
    public String getRemainder() {
	String remainder = "";
	int colonIndex = locatorString.indexOf(":");

	if( colonIndex != -1) {
	    remainder = locatorString.substring(colonIndex + 1);
	}

	return remainder;
    }
    
    /**
     * Used for printing <CODE>MediaLocators</CODE>.
     * @return A string for printing <CODE>MediaLocators</CODE>.
     */
    public String toString() {
	return locatorString;
    }

    /**
     * Create a string from the <CODE>URL</CODE> argument that can
     * be used to construct the <CODE>MediaLocator</CODE>.
     *
     * @return A string for the <CODE>MediaLocator</CODE>.
     */
    public String toExternalForm() {
	return locatorString;
    }
}
