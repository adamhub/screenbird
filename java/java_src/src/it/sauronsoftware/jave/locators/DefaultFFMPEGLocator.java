/*
 * JAVE - A Java Audio/Video Encoder (based on FFMPEG)
 * 
 * Copyright (C) 2008-2009 Carlo Pelliccia (www.sauronsoftware.it)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.sauronsoftware.jave.locators;

import com.bixly.pastevid.Settings;
import com.bixly.pastevid.common.Unimplemented;
import com.bixly.pastevid.util.LogUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The default ffmpeg executable locator, which exports on disk the ffmpeg
 * executable bundled with the library distributions. It should work both for
 * windows and many linux distributions. If it doesn't, try compiling your own
 * ffmpeg executable and plug it in JAVE with a custom {@link FFMPEGLocator}.
 * 
 * @author Carlo Pelliccia
 */
public class DefaultFFMPEGLocator extends FFMPEGLocator {

    /**
     * Trace the version of the bundled ffmpeg executable. It's a counter: every
     * time the bundled ffmpeg change it is incremented by 1.
     */
    protected static final int myEXEversion = 1;
    /**
     * The ffmpeg executable file path.
     */
    protected String path;

    public DefaultFFMPEGLocator(){
        
    }
    
    @Override
    protected String getFFMPEGExecutablePath() {
        return Settings.getFFMpegExecutable();
//        return path;
    }
    

    /**
     * Copies a file bundled in the package to the supplied destination.
     * 
     * @param path
     *            The name of the bundled file.
     * @param dest
     *            The destination.
     * @throws RuntimeException
     *             If aun unexpected error occurs.
     */
    @Unimplemented
    protected void copyFile(String path, File dest) throws RuntimeException {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = getClass().getResourceAsStream("/com/bixly/binaries/" + path);
            output = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int l;
            log("Copying from "+input.toString());
            while ((l = input.read(buffer)) != -1) {
                output.write(buffer, 0, l);
            }
            log("Copied to "+output.toString());
            log("Copied total bytes: "+dest.length());
        } catch (IOException e) {
            throw new RuntimeException("Cannot write file "
                    + dest.getAbsolutePath());
        } catch (NullPointerException e) {
            log("Could not located binary: /com/bixly/binaries/"+path);
        }finally {
            if (output != null) {
                try {
                    output.close();
                } catch (Throwable t) {
                    ;
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable t) {
                    ;
                }
            }
        }
    }
    @Unimplemented
    protected void copyAbsoluteFile(String path, File dest) throws RuntimeException {
        InputStream input = null;
        OutputStream output = null;
        try {
            log("Loading /"+path+" from Library");
            input = getClass().getResourceAsStream("/"+path);
            log("Storing at "+dest);
            output = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int l;
            while ((l = input.read(buffer)) != -1) {
                output.write(buffer, 0, l);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot write file " + dest.getAbsolutePath());
        } catch (NullPointerException e) {
            log("Could not located binary: "+path);
        }finally {
            if (output != null) {
                try {
                    output.close();
                } catch (Throwable t) {
                    ;
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable t) {
                    ;
                }
            }
        }
        log("Done");
    }

    public void log(Object message) {
        LogUtil.log(DefaultFFMPEGLocator.class, message);
    }
    
    
}
