/*
 * ScreenSize
 * 
 * Version 1.0
 * 
 * 7 May 2013
 */
package com.bixly.pastevid.models;

/**
 * Model class for the screen size. 
 * @author rescian
 */
public class ScreenSize {
    // Aspect ratio settings
    public final static String WIDE = "Wide";
    public final static String STANDARD = "Standard";
    
    // Aspect ratio values
    public final static double AR_WIDE = 9.0/16.0;
    public final static double AR_STANDARD = 3.0/4.0;
    
    // Screen size attributes
    private int width;
    private int height;
    private String name;
    private double aspectRatio;
    
    public ScreenSize(String name, int width, int height, double ar) {
        this.width = width;
        this.height = height;
        this.name = name;
        this.aspectRatio = ar;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public double getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(double aspectRatio) {
        this.aspectRatio = aspectRatio;
    }
    
    @Override
    public String toString() { 
        return this.name; 
    }
}
