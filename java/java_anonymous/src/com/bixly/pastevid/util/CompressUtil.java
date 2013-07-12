/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.pastevid.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author Jorge
 */
public class CompressUtil {
    public static void doCompressFile(String inFileName)
    {
        try{
            File file = new File(inFileName);           
            FileOutputStream fos = new FileOutputStream(file + ".gz");
            GZIPOutputStream gzos = new GZIPOutputStream(fos);
            FileInputStream fin = new FileInputStream(file);
            BufferedInputStream in = new BufferedInputStream(fin);
            byte[] buffer = new byte[1024];
            int i;
            while ((i = in.read(buffer)) >= 0){
                gzos.write(buffer,0,i);
            }
            in.close();
            gzos.close();
        }
        catch(IOException e){
            System.out.println("Exception is" + e);
        }
     }  
    public static String compress(String filename) throws FileNotFoundException, IOException
    {
        byte[] buffer = new byte[4096];
        int bytesRead;
        String[] entries = {".mp4"};
        String zipfile = filename.replace(".mp4", ".zip");
        if(!(new File(zipfile)).exists())
        {
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipfile));
            out.setLevel(Deflater.BEST_COMPRESSION);
            out.setMethod(ZipOutputStream.DEFLATED);
            for (int i = 0; i < entries.length; i++) {
                File f = new File(filename.replace(".mp4", entries[i]));
                if (f.exists())
                {
                    FileInputStream in = new FileInputStream(f); 
                    ZipEntry entry = new ZipEntry(f.getName()); 
                    out.putNextEntry(entry); 
                    while ((bytesRead = in.read(buffer)) != -1)
                        out.write(buffer, 0, bytesRead);
                    in.close(); 
                }
            }
            out.close();
        }
        return zipfile;
    }    
}
