/*
 * MovFileFilter.java
 *
 * Version 1.0
 * 
 * 20 May 2013
 */
package com.bixly.pastevid.util.view;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author Jorge
 */
public class MovFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
        return f.getName().endsWith(".mp4") || f.isDirectory();
    }

    @Override
    public String getDescription() {
        return "Mp4 Video File";
    }
}
