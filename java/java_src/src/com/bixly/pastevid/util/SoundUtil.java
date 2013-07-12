/*
 * SoundUtil.java
 *
 * Version 1.0
 * 
 * 17 May 2013
 */
package com.bixly.pastevid.util;

import javax.sound.sampled.*;

/**
 * Utility class for producing sound alerts. Used in playing countdown beeps.
 * http://stackoverflow.com/questions/3780406/how-to-play-a-sound-alert-in-a-java-application
 */
public class SoundUtil {

    public static float SAMPLE_RATE = 8000f;

    public static void tone(int hz, int msecs) throws LineUnavailableException {
        tone(hz, msecs, 1.0);
    }

    public static void tone(int hz, int msecs, double vol) throws LineUnavailableException {
        byte[] buf = new byte[1];
        AudioFormat af =
                new AudioFormat(
                SAMPLE_RATE,    // sampleRate
                8,              // sampleSizeInBits
                1,              // channels
                true,           // signed
                false);         // bigEndian
        SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
        sdl.open(af);
        sdl.start();
        for (int i = 0; i < msecs * 8; i++) {
            double angle = i / (SAMPLE_RATE / hz) * 2.0 * Math.PI;
            buf[0] = (byte) (Math.sin(angle) * 127.0 * vol);
            sdl.write(buf, 0, 1);
        }
        sdl.drain();
        sdl.stop();
        sdl.close();
    }

    // For testing
    public static void main(String[] args) throws Exception {
        SoundUtil.tone(261*2, 1000);
        //Thread.sleep(1000);
        SoundUtil.tone(294*2, 1000);
        //Thread.sleep(1000);
        SoundUtil.tone(330*2, 1000);
        //Thread.sleep(1000);
        SoundUtil.tone(261*2, 1000);
        //Thread.sleep(1000);
        SoundUtil.tone(261*2, 1000);
        //Thread.sleep(1000);
        SoundUtil.tone(294*2, 1000);
        //Thread.sleep(1000);
        SoundUtil.tone(330*2, 1000);
        //Thread.sleep(1000);
        SoundUtil.tone(261*2, 1000);
        //Thread.sleep(1000);
        SoundUtil.tone(330*2, 1000);
        SoundUtil.tone(349*2, 1000);
        SoundUtil.tone(392*2, 1000);
        Thread.sleep(1000);
        SoundUtil.tone(330*2, 1000);
        SoundUtil.tone(349*2, 1000);
        SoundUtil.tone(392*2, 1000);
        Thread.sleep(1000);
        SoundUtil.tone(392*2, 500);
        SoundUtil.tone(440*2, 500);
        SoundUtil.tone(392*2, 500);
        SoundUtil.tone(349*2, 500);
        SoundUtil.tone(330*2, 1000);
        SoundUtil.tone(261*2, 1000);
        SoundUtil.tone(392*2, 500);
        SoundUtil.tone(440*2, 500);
        SoundUtil.tone(392*2, 500);
        SoundUtil.tone(349*2, 500);
        SoundUtil.tone(330*2, 1000);
        SoundUtil.tone(261*2, 1000);
        SoundUtil.tone(261*2, 1000);
        SoundUtil.tone(196*2, 1000);
        SoundUtil.tone(261*2, 1000);
        Thread.sleep(1000);
        SoundUtil.tone(261*2, 1000);
        SoundUtil.tone(196*2, 1000);
        SoundUtil.tone(261*2, 1000);
    }
}