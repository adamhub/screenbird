/*
 * ProgressMultiPartEntity.java
 *
 * Version 1.0
 * 
 * 17 May 2013
 */
package com.bixly.pastevid.screencap.components.progressbar;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;

public class ProgressMultiPartEntity extends MultipartEntity {

    private UploadProgressListener listener_;
    private ProgressOutputStream outputStream_;
    private OutputStream lastOutputStream_;

    public ProgressMultiPartEntity(UploadProgressListener listener) {
        super(HttpMultipartMode.BROWSER_COMPATIBLE);
        listener_ = listener;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {

        if ((lastOutputStream_ == null) || (lastOutputStream_ != out)) {
            lastOutputStream_ = out;
            outputStream_ = new ProgressOutputStream(out);
        }

        super.writeTo(outputStream_);
    }
    
    public static interface UploadProgressListener {
        void transferred(long num);
    }
    
    private class ProgressOutputStream extends FilterOutputStream {

        private long transferred = 0;
        private OutputStream wrappedOutputStream_;

        public ProgressOutputStream(final OutputStream out) {
            super(out);
            wrappedOutputStream_ = out;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            wrappedOutputStream_.write(b,off,len);
           
            ++transferred;
            listener_.transferred(transferred);
        }

        @Override
        public void write(int b) throws IOException {
            super.write(b);
        }
    }
}