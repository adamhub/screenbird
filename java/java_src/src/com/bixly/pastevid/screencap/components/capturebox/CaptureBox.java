/*
 * CaptureBox.java
 * 
 * Version 1.1
 * 
 * 15 May 2013
 */
package com.bixly.pastevid.screencap.components.capturebox;

import com.bixly.pastevid.Settings;
import com.bixly.pastevid.recorders.Recorder;
import com.bixly.pastevid.recorders.RecorderStatus;
import com.bixly.pastevid.screencap.components.IProperties;
import com.bixly.pastevid.screencap.components.PropertiesManager;
import com.bixly.pastevid.util.LogUtil;
import com.bixly.pastevid.util.MediaUtil;
import com.bixly.pastevid.util.ScreenUtil;
import com.sun.awt.AWTUtilities;
import com.sun.awt.AWTUtilities.Translucency;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;
import javax.swing.SwingUtilities;

/**
 * @author Bixly
 */
public final class CaptureBox implements IProperties, CaptureBoxController, Serializable {
    
    static final long serialVersionUID = 6610978673487329L;
    
    private CaptureBoxState recordingViewState = CaptureBoxState.FULLSCREEN; 
    private CaptureBoxState aspectRatioState   = CaptureBoxState.WIDESCREEN;
    
    // Properties manager for Capturebox
    private  PropertiesManager propertiesManager = new PropertiesManager(Settings.SCREENBIRD_CONFIG);
    
    // Keys for holding the property of the current Capturebox instance 
    // in the properties file.
    private static final String IS_VISIBLE = "capturebox.isVisible";
    private static final String POSITION  = "capturebox.position";
    private static final String RECORDING_VIEW_STATE = "capturebox.state";
    private static final String ASPECT_RATIO_STATE   = "capturebox.aspect_ratio";

    // Constants
    private static final String DEFAULT_RECTANGLE = "390 200 780 400";
    public static final int BORDER_THICKNESS = 6;
    private static final float BOX_OPACITY = 1f;
    private static final Dimension MIN_BOX_DIMENSION = new Dimension(530, 397);
    
    private static final Rectangle customCaptureRect = new Rectangle(0, 0, 0, 0);
    private static final Dimension drawBoxRectangle = new Dimension(0, 0);
    private static final Dimension relocateDragBox = new Dimension(0, 0);
    
    // Borders for the capturebox frames
    private HBorder topBorder;
    private VBorder rightBorder;
    private HBorder bottomBorder;
    private VBorder leftBorder;
    
    // Resize handles
    private ResizeHandle nwResize, neResize, swResize, seResize;
    
    // Class level variables
    private Dimension screenSize;
    private Rectangle fullCaptureRect;
    private Recorder   recorder;
    private DragBox    dragBox;
    
    private int mouseStartX;
    private int mouseStartY;
    
    private boolean isVisble = false;
    
    // If the capturebox can be resized or repositioned
    private boolean isCaptureboxLocked = false;
    private boolean isMoving = false;
    
    public CaptureBox(Recorder recorder) {
        this.recorder = recorder;
        
        // Generates the capturebox frames
        this.topBorder = new HBorder(HBorder.AT_TOP, this.recorder.getScreen());
        this.rightBorder = new VBorder(VBorder.AT_RIGHT, this.recorder.getScreen());
        this.bottomBorder = new HBorder(HBorder.AT_BOTTOM, this.recorder.getScreen());
        this.leftBorder = new VBorder(VBorder.AT_LEFT, this.recorder.getScreen());
        
        this.nwResize = new ResizeHandle(ResizeHandle.Location.NORTHWEST);
        this.neResize = new ResizeHandle(ResizeHandle.Location.NORTHEAST);
        this.swResize = new ResizeHandle(ResizeHandle.Location.SOUTHWEST);
        this.seResize = new ResizeHandle(ResizeHandle.Location.SOUTHEAST);
        
        // Creates the dragbox for dragging the capturebox
        this.dragBox = new DragBox(this);
        
        initComponents();
        initMouseHandlers();
        this.dragBox.updateCapturingBox(false);
        updateObservers();
        this.setCaptureboxVisible(true, true, true);
        
        // Load capturebox state
        load();
    }

    /**
     * Sets the default screen size and capture rectangle to the fullscreen
     * dimensions of the current screen.
     */
    private void initComponents() {
        this.screenSize = ScreenUtil.getScreenDimension(recorder.getScreen());
        this.fullCaptureRect = new Rectangle(this.screenSize);
    }

    /**
     * Enables or disables the capturebox from being resized or repositioned
     * @param value True if the capturebox is to be locked
     */
    public synchronized void setLockCapturebox(boolean value) {
        this.isCaptureboxLocked = value;
    }
    
    /**
     * Returns true if the capturebox is currently locked.
     * @return 
     */
    public synchronized boolean getLockCapturebox() {
        return this.isCaptureboxLocked;
    }
    
    /**
     * Initiates the move of the capturebox to a given position. Currently used
     * just for reloading the capturebox on start of screen recorder application.
     * @param rect Rectangle with the given 
     */
    private void setPosition(Rectangle rect) {
        // Redraws capturebox frames
        drawBox(rect.width+(2 * BORDER_THICKNESS), rect.height, true);
        
        // Compute for the x and y screen offsets where to move the capturebox
        int x = -this.leftBorder.getBounds().x + rect.x - BORDER_THICKNESS;
        int y = -this.leftBorder.getBounds().y + rect.y;
        
        moveBox(x, y, null);
    }
    
    /**
     * Sets the current state of the capturebox.
     * @param captureBoxState The to be state of capturebox
     */
    public void setState(CaptureBoxState captureBoxState) {
        this.recordingViewState = captureBoxState;
        updateObservers();
    }

    /**
     * Polymorphic helper to that wraps around the drawbox procedure. Specifically
     * used to call drawbox without creating new Dimension() objects.
     * 
     * @param w Width of capturebox
     * @param h Height of capturebox
     * @param showBox True if dragbox should be shown
     */
    public void drawBox(int w, int h, boolean showBox) {
        this.drawBoxRectangle.width = w;
        this.drawBoxRectangle.height = h;
        drawBox(this.drawBoxRectangle, showBox);
    }
    
    /**
     * Resizes capturebox and displays drag box on request.
     * @param size
     * @param showBox 
     */
    public void drawBox(Dimension size, boolean showBox) {
        this.dragBox.updateSizeLabel(size);
        this.setOnTop();
        
        topBorder.setMinimumSize(new Dimension(0, 0));
        topBorder.setBounds((screenSize.width - size.width) / 2, (screenSize.height - size.height) / 2, size.width, BORDER_THICKNESS);
        topBorder.setVisible(showBox);

        rightBorder.setMinimumSize(new Dimension(0, 0));
        rightBorder.setBounds(topBorder.getX() + topBorder.getWidth() - BORDER_THICKNESS, topBorder.getY() + BORDER_THICKNESS, BORDER_THICKNESS, size.height);
        rightBorder.setVisible(showBox);

        bottomBorder.setMinimumSize(new Dimension(0, 0));
        bottomBorder.setBounds(topBorder.getX(), rightBorder.getY() + rightBorder.getHeight(), topBorder.getWidth(), BORDER_THICKNESS);
        bottomBorder.setVisible(showBox);

        leftBorder.setMinimumSize(new Dimension(0, 0));
        leftBorder.setBounds(topBorder.getX(), topBorder.getY() + BORDER_THICKNESS, BORDER_THICKNESS, size.height);
        leftBorder.setVisible(showBox);

        // Reposition drag box and resize handles to the center and corners, respectively
        relocateDragBox(topBorder.getWidth() - 2 * BORDER_THICKNESS, rightBorder.getHeight(), true);
        relocateResizeHandles(topBorder.getWidth() - 2 * BORDER_THICKNESS);
        
        if (showBox) { 
            this.setDragBoxVisible(true);
        }

        updateObservers();

        if (!MediaUtil.osIsUnix() && AWTUtilities.isTranslucencySupported(Translucency.TRANSLUCENT)) {
            AWTUtilities.setWindowOpacity(topBorder, BOX_OPACITY);
            AWTUtilities.setWindowOpacity(rightBorder, BOX_OPACITY);
            AWTUtilities.setWindowOpacity(bottomBorder, BOX_OPACITY);
            AWTUtilities.setWindowOpacity(leftBorder, BOX_OPACITY);
        }
        
        repaintCaptureBox();
    }
    
    /**
     * Returns true if the capturebox is currently visible. Does not take into account
     * the visibility of the drag box.
     * @return 
     */
    public synchronized  boolean isVisible() {
        return this.isVisble;
    }
    
    /**
     * Sets the all capturebox components on top of all other windows (including
     * other applications currently running).
     */
    public void setOnTop() {
        topBorder.setAlwaysOnTop(true);
        rightBorder.setAlwaysOnTop(true);
        bottomBorder.setAlwaysOnTop(true);
        leftBorder.setAlwaysOnTop(true);
        dragBox.setAlwaysOnTop(true);
    }
    
    /**
     * Manages the visibility of capturebox's drag box.
     * @param isVisible 
     */
    public void setDragBoxVisible(final boolean isVisible) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                boolean showResizeHandles = isVisible && recorder.hasStatus(RecorderStatus.STOPPED);
                dragBox.setVisible(isVisible);
                nwResize.setVisible(showResizeHandles);
                neResize.setVisible(showResizeHandles);
                swResize.setVisible(showResizeHandles);
                seResize.setVisible(showResizeHandles);
            }
        });
    }
    
    /**
     * Manages visibility of capturebox.
     * 
     * @param isVisible If True, then capturebox, as a whole, is visible
     * @param save If True, then the current capturebox sate is saved to file
     * @param showDragBox If True, then capturebox is displayed with capturebox
     */
    public void setCaptureboxVisible(final boolean isVisible, final boolean save, final Boolean showDragBox) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                isVisble = isVisible;
                topBorder.setVisible(isVisible);
                rightBorder.setVisible(isVisible);
                bottomBorder.setVisible(isVisible);
                leftBorder.setVisible(isVisible);
                
                // If input for dragbox exists
                if (showDragBox != null) {
                    dragBox.setVisible(showDragBox);
                    
                    boolean showResizeHandles = isVisible && recorder.hasStatus(RecorderStatus.STOPPED);
                    nwResize.setVisible(showResizeHandles);
                    neResize.setVisible(showResizeHandles);
                    swResize.setVisible(showResizeHandles);
                    seResize.setVisible(showResizeHandles);
                }

                // Saves visible state
                if (save) {
                    save();
                }
            }
        });

    }

    /**
     * A boolean state check to see if the recorder is in paused or recording state.
     * In other words, if the recorder has yet to start recording or is finished 
     * recording, isRecording() return False, else True
     * @return True if recorder has started or paused
     */
    private boolean isRecording() {
        return recorder.hasStatus(RecorderStatus.RECORDING);
    }

    /**
     * Returns current state of the capturebox size and position
     * @return Returns a Rectangle with the coordinates and size of capturebox
     */
    public  Rectangle getCaptureRectangle() {
        this.customCaptureRect.width  = this.topBorder.getBounds().width - (BORDER_THICKNESS * 2);
        this.customCaptureRect.height = this.leftBorder.getBounds().height;
        this.customCaptureRect.x      = (this.leftBorder.getBounds().x + BORDER_THICKNESS);
        this.customCaptureRect.y      = (this.leftBorder.getBounds().y);
        return this.customCaptureRect;
    }
    
    /**
     * Returns the viewing state of capturebox (FULLSCREEN | CUSTOMSCREEN)
     * @return Returns view state of (FULLSCREEN | CUSTOMSCREEN)
     */
    public CaptureBoxState getState() {
        return this.recordingViewState;
    }
    
    /**
     * Mouse listener for the capture box borders.
     */
    private class CaptureBoxMouseListener extends IBorderMouseListener {
        @Override
        public void mouseReleased(MouseEvent me) {
            if (!isRecording()) {
                save();
            }
        }
    }

    /**
     * Binds mouse handlers to detect and implement the resizing of the capturebox.
     * Binds each corner of capturebox with a draggable point. After the capturebox
     * is finished resizing, the state of the caputurebox is saved to file.
     */
    private void initMouseHandlers() {
        // Northeast resize handle listeners
        this.neResize.addMouseListener(new CaptureBoxMouseListener());
        this.neResize.addMouseMotionListener(new IBorderMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // Disallow resizing while recording
                if (!isRecording()) {
                    // New right x and min allowed right x
                    int x =  e.getXOnScreen();
                    int xMin = leftBorder.getX() + BORDER_THICKNESS + MIN_BOX_DIMENSION.width;

                    // Internal bounds
                    int width =  x - leftBorder.getX() - BORDER_THICKNESS;
                    int height =  (int)(width * dragBox.getCurrentScreenSize().getAspectRatio());
                    int minHeight = MIN_BOX_DIMENSION.height;
                    
                    Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(dragBox.getGraphicsConfiguration());
                    
                    if (x > xMin && height > minHeight) {
                        if (bottomBorder.getY() - height - BORDER_THICKNESS >= scnMax.top 
                                && leftBorder.getX() + width + BORDER_THICKNESS <= screenSize.width - scnMax.right - BORDER_THICKNESS) {
                            resizeFromTopRight(x, width, height);
                        } else if (topBorder.getY() + height + BORDER_THICKNESS < screenSize.height - scnMax.bottom - BORDER_THICKNESS 
                                && leftBorder.getX() + width + BORDER_THICKNESS <= screenSize.width - scnMax.right - BORDER_THICKNESS) {
                            resizeFromBottomRight(x, width, height);
                        }
                    }
                }
            }

        });
        
        // Southeast resize handle listeners
        this.seResize.addMouseListener(new CaptureBoxMouseListener());
        this.seResize.addMouseMotionListener(new IBorderMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // Disallow resizing while recording
                if (!isRecording()) {
                    // New right x and min allowed right x
                    int x =  e.getXOnScreen();
                    int xMin =  leftBorder.getX() + BORDER_THICKNESS + MIN_BOX_DIMENSION.width;

                    // Internal bounds
                    int width =  x - leftBorder.getX() - BORDER_THICKNESS;
                    int height =  (int)(width * dragBox.getCurrentScreenSize().getAspectRatio());
                    int minHeight = MIN_BOX_DIMENSION.height;

                    Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(dragBox.getGraphicsConfiguration());

                    if (x > xMin && height > minHeight) {
                        if (topBorder.getY() + height + BORDER_THICKNESS < screenSize.height - scnMax.bottom - BORDER_THICKNESS 
                                && leftBorder.getX() + width + BORDER_THICKNESS <= screenSize.width - scnMax.right - BORDER_THICKNESS) {
                            resizeFromBottomRight(x, width, height);
                        }
                        else if (bottomBorder.getY() - height - BORDER_THICKNESS >= scnMax.top 
                                && leftBorder.getX() + width + BORDER_THICKNESS <= screenSize.width - scnMax.right - BORDER_THICKNESS) {
                            resizeFromTopRight(x, width, height);
                        }
                    }
                }
            }

        });

        // Northwest resize handle listeners
        this.nwResize.addMouseListener(new CaptureBoxMouseListener());
        this.nwResize.addMouseMotionListener(new IBorderMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // Disallow resizing while recording
                if (!isRecording()) {
                    // New left x and min allowed left x
                    int x =  e.getXOnScreen();
                    int xMin =  rightBorder.getX() - MIN_BOX_DIMENSION.width;
                    
                    // Internal bounds
                    int width =  rightBorder.getX() - x;
                    int height =  (int)(width * dragBox.getCurrentScreenSize().getAspectRatio());
                    int minHeight = MIN_BOX_DIMENSION.height;
                    
                    Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(dragBox.getGraphicsConfiguration());
                    
                    if (x > 0 && x < xMin && height > minHeight) {
                        if (bottomBorder.getY() - height - BORDER_THICKNESS >= scnMax.top 
                                && rightBorder.getX() - width - BORDER_THICKNESS >= scnMax.left) {
                            resizeFromTopLeft(x, width, height);
                        } else if (topBorder.getY() + height + BORDER_THICKNESS < screenSize.height - scnMax.bottom - BORDER_THICKNESS 
                                && rightBorder.getX() - width - BORDER_THICKNESS >= scnMax.left) {
                            resizeFromBottomLeft(x, width, height);
                        }
                    }
                }
            }
        });

        // Southwest resize handle listeners
        this.swResize.addMouseListener(new CaptureBoxMouseListener());
        this.swResize.addMouseMotionListener(new IBorderMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // Disallow resizing while recording
                if (!isRecording()) {
                    // New left x and min allowed left x
                    int x =  e.getXOnScreen();
                    int xMin =  rightBorder.getX() - MIN_BOX_DIMENSION.width;

                    // Internal bounds
                    int width =  rightBorder.getX() - x;
                    int height =  (int)(width * dragBox.getCurrentScreenSize().getAspectRatio());
                    int minHeight = MIN_BOX_DIMENSION.height;

                    Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(dragBox.getGraphicsConfiguration());
                    
                    if (x > 0 && x < xMin && height > minHeight) {
                        if (topBorder.getY() + height + BORDER_THICKNESS < screenSize.height - scnMax.bottom - BORDER_THICKNESS 
                                && rightBorder.getX() - width - BORDER_THICKNESS >= scnMax.left) {
                            resizeFromBottomLeft(x, width, height);
                        } else if (bottomBorder.getY() - height - BORDER_THICKNESS >= scnMax.top 
                                && rightBorder.getX() - width - BORDER_THICKNESS >= scnMax.left) {
                            resizeFromTopLeft(x, width, height);
                        }
                    }
                }
            }

        });
        
        // Dragbox mouse listeners
        this.dragBox.addMouseListener(new BoxMovementMouseAdapter());
        this.dragBox.addMouseMotionListener(new BoxMovementMouseListener());
    }

    /**
     * In order to hold the current state of the capturebox, we implement a 
     * load/save feature using Java Properties. Upon calling load(), the 
     * saved state is loaded into the current running state. 
     * 
     * @return True if everything saved properly
     */
    public boolean load() {
        try {
            Properties properties = this.propertiesManager.readPropertyFile();
            String position = properties.getProperty(POSITION);
            String isVisible = properties.getProperty(IS_VISIBLE);
            String arState = properties.getProperty(ASPECT_RATIO_STATE, CaptureBoxState.WIDESCREEN.name());
            
            // If data is not found, return default
            if (position == null || isVisible == null) {
                return false;
            }
            
            // Load Capturebox Screen Aspect Ratio
            if (arState.equalsIgnoreCase(CaptureBoxState.WIDESCREEN.name())) {
                this.aspectRatioState = CaptureBoxState.WIDESCREEN;
            } else if (arState.equalsIgnoreCase(CaptureBoxState.STANDARD_SCREEN.name())) {
                this.aspectRatioState = CaptureBoxState.STANDARD_SCREEN;
            }
            
            log("Capturebox Aspect Ratio " + this.aspectRatioState.name());
            
            // Update loaded Capturebox Screen Aspect Ratio data
            if (this.dragBox != null) {
                this.dragBox.setAspectRatio(this.aspectRatioState);
            } else {
                throw new UnsupportedOperationException("Can not set dragbox aspect because it is null");
            }
            
            log("Loading data capturebox: " + position);
            log("Loading data isVisible:  " + isVisible);
            String[] rectangle = properties.getProperty(POSITION, DEFAULT_RECTANGLE).split(" ");
            
            // Build and return data 
            this.setPosition(new Rectangle(
                    Integer.parseInt(rectangle[0]),
                    Integer.parseInt(rectangle[1]), 
                    Integer.parseInt(rectangle[2]),
                    Integer.parseInt(rectangle[3])));
            
            if (Boolean.valueOf(properties.getProperty(IS_VISIBLE, "false"))) {
                this.setCaptureboxVisible(true, false, true);
            } else {
                this.setCaptureboxVisible(false, false, null);
            }
            
            if (properties.getProperty(RECORDING_VIEW_STATE, recordingViewState.name()).equalsIgnoreCase(CaptureBoxState.FULLSCREEN.name())) {
                this.recordingViewState = CaptureBoxState.FULLSCREEN;
            } else if (properties.getProperty(RECORDING_VIEW_STATE).equalsIgnoreCase(CaptureBoxState.CUSTOM_SCREEN.name())) {
                this.recordingViewState = CaptureBoxState.CUSTOM_SCREEN;  
                this.setDragBoxVisible(true);
            }
            return true;
        } catch (IOException e) { 
            log(e); 
        } catch (NumberFormatException e) {
            log(e);
        } finally {
            this.propertiesManager.closeProperties();
        }
        return false;
    }

    /**
     * Saves the current state of capturebox to file to be loaded when the 
     * app next launches.
     * @return True if save executed correctly
     */
    public boolean save() {
        try {
            Properties properties = this.propertiesManager.readPropertyFile();
            Rectangle rect = this.getCaptureRectangle();
            String position = rect.x + " " + rect.y + " " + rect.width + " " + rect.height;
            
            properties.setProperty(POSITION, position);
            properties.setProperty(IS_VISIBLE, String.valueOf(this.isVisible()));
            properties.setProperty(RECORDING_VIEW_STATE, this.recordingViewState.name());
            properties.setProperty(ASPECT_RATIO_STATE, this.aspectRatioState.name());
            this.propertiesManager.writePropertyFile(properties, "Screenbird Metadata");
            log("Saving: " + properties);
            return true;
        } catch (IOException e) {
            log(e);
        } finally {
            this.propertiesManager.closeProperties();
        }
        return false;
    }

    /**
     * Destroys the current running instance of capturebox.
     */
    public void destroy() {
        if (this.topBorder != null) {
            this.topBorder.dispose();
        }
        if (this.bottomBorder != null) {
            this.bottomBorder.dispose();
        }
        if (this.rightBorder != null) {
            this.rightBorder.dispose();
        }
        if (this.leftBorder != null) {
            this.leftBorder.dispose();
        }
        if (this.dragBox != null) {
            this.dragBox.dispose();
        }
    }

    /**
     * Sets the aspect ratio state of the capturebox (WIDESCREEN | STANDARD_SCREEN).
     * @param captureBoxState Aspect Ratio
     */
    public void setAspectRatio(CaptureBoxState captureBoxState) {
        this.aspectRatioState = captureBoxState;
    }
    
    /**
     * Begins flashing the border to the color given.
     */
    public void beginBorderFlash() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                topBorder.getContentPane().setBackground(HBorder.BORDER_FLASH_COLOR);
                bottomBorder.getContentPane().setBackground(HBorder.BORDER_FLASH_COLOR);
                rightBorder.getContentPane().setBackground(HBorder.BORDER_FLASH_COLOR);
                leftBorder.getContentPane().setBackground(HBorder.BORDER_FLASH_COLOR);
            }
        });
    }
    
    /**
     * Stops flashing the border, reverting it to their original color.
     */
    public void endBorderFlash() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                topBorder.getContentPane().setBackground(HBorder.BORDER_COLOR);
                bottomBorder.getContentPane().setBackground(HBorder.BORDER_COLOR);
                rightBorder.getContentPane().setBackground(HBorder.BORDER_COLOR);
                leftBorder.getContentPane().setBackground(HBorder.BORDER_COLOR);
            }
        });
    }
    
    /**
     * Repaints the borders and dragbox.
     */
    private void repaintCaptureBox() {
        topBorder.paintAll(topBorder.getGraphics());
        leftBorder.paintAll(leftBorder.getGraphics());
        rightBorder.paintAll(rightBorder.getGraphics());
        bottomBorder.paintAll(bottomBorder.getGraphics());
        dragBox.paintAll(dragBox.getGraphics());
    }
    
    /**
     * Private class that tracks the movement of the capturebox. The differences
     * in the mouse positions between pressed events are used for moving the capturebox.
     */
    private class BoxMovementMouseAdapter extends MouseAdapter {
        BoxMovementMouseAdapter() {
            super();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            mouseStartX = e.getXOnScreen();
            mouseStartY = e.getYOnScreen();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            setOnTop();
            save();     // Save properties
        }
    }
    
    /**
     * Keep track of mouse movements to assist with calculating the movement
     * of the capturebox.
     */
    private class BoxMovementMouseListener extends MouseMotionAdapter {
        BoxMovementMouseListener() { 
            super(); 
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            int deltaX = e.getXOnScreen() - mouseStartX;
            int deltaY = e.getYOnScreen() - mouseStartY;
            moveBox(deltaX, deltaY, e);
        }
    }
    
    
    
    /**
     * Calculates the next move for the capture box. Based off the given deltaX
     * and deltaY offsets, calculations are done to prevent the capture box from
     * being dragged off screen. If there are more than one monitors/displays,
     * then disregard boundary checking and allow the capture box to be dragged
     * off screen. 
     * 
     * @param deltaX Next N amount of pixels to move the capture box left or right
     * @param deltaY Next N amount of pixels to move the capture box up or down
     */
    private void moveBox(int deltaX, int deltaY, MouseEvent me) {
        long start = System.currentTimeMillis();
        
        if (isRecording() || isMoving) {
            return;
        }
        
        isMoving = true;
        Rectangle r = this.getCaptureRectangle();
        
        // Disable clamping if there is more than one monitor.
        boolean clamp = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length == 1;
        boolean clampedX = false, clampedY = false;
        
        // Used to consider the "effective" screen size.
        Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(this.dragBox.getGraphicsConfiguration());
        
        int newTop = r.y + deltaY;
        int newLeft = r.x + deltaX;
        int newRight = r.width + r.x + deltaX;
        int newBottom = r.height + r.y + deltaY;
        
        if (clamp) {
            if (newTop <= scnMax.top + BORDER_THICKNESS) {
                newTop = scnMax.top + BORDER_THICKNESS;
                newBottom = newTop + r.height;
                clampedY = true;
            } else if (newTop >= screenSize.height - scnMax.bottom - BORDER_THICKNESS) {
                newTop = screenSize.height - scnMax.bottom - BORDER_THICKNESS;
                newBottom = newTop + r.height;
                clampedY = true;
            } else if (newBottom <= scnMax.top + BORDER_THICKNESS) {
                newBottom = scnMax.top + BORDER_THICKNESS;
                newTop = newBottom - r.height;
                clampedY = true;
            } else if (newBottom >= screenSize.height - scnMax.bottom - BORDER_THICKNESS) {
                newBottom = screenSize.height - scnMax.bottom - BORDER_THICKNESS;
                newTop = newBottom - r.height;
                clampedY = true;
            }
            
            if (newLeft <= scnMax.left + BORDER_THICKNESS) {
                newLeft = scnMax.left + BORDER_THICKNESS;
                newRight = newLeft + r.width;
                clampedX = true;
            } else if (newLeft >= screenSize.width - scnMax.right - BORDER_THICKNESS) {
                newLeft = screenSize.width - scnMax.right - BORDER_THICKNESS;
                newRight = newLeft + r.width;
                clampedX = true;
            } else if (newRight <= scnMax.left + BORDER_THICKNESS) {
                newRight = scnMax.left + BORDER_THICKNESS;
                newLeft = newRight - r.width;
                clampedX = true;
            } else if (newRight >= screenSize.width - scnMax.right - BORDER_THICKNESS) {
                newRight = screenSize.width - scnMax.right - BORDER_THICKNESS;
                newLeft = newRight - r.width;
                clampedX = true;
            }
        }
        
        if (!clampedX && me != null) {
            mouseStartX = me.getXOnScreen();
        }
        if (!clampedY && me != null) {
            mouseStartY = me.getYOnScreen();
        }
        
        topBorder.setLocation(newLeft - BORDER_THICKNESS, newTop - BORDER_THICKNESS);
        leftBorder.setLocation(newLeft - BORDER_THICKNESS, newTop);
        rightBorder.setLocation(newRight, newTop);
        bottomBorder.setLocation(newLeft - BORDER_THICKNESS, newBottom);
        
        relocateDragBox(topBorder.getWidth() - 2 * BORDER_THICKNESS, rightBorder.getHeight(), false);
        relocateResizeHandles(topBorder.getWidth() - 2 * BORDER_THICKNESS);
        
        updateObservers();
        repaintCaptureBox();
        
        isMoving = false;
        long end = System.currentTimeMillis();
        System.out.println("Move time: " + (end - start));
    }
    
    /**
     * Top right corner resizing procedure
     * @param topRightX 
     * @param width
     * @param height 
     */
    private void resizeFromTopRight(int topRightX, int width, int height) {
        if (isCaptureboxLocked) {
            return;
        }
        
        if (width < MIN_BOX_DIMENSION.width) {
            width = MIN_BOX_DIMENSION.width;
        }
        if (height < MIN_BOX_DIMENSION.height) {
            height= MIN_BOX_DIMENSION.height;
        }
        
        // Relocate & resize all the borders.
        topBorder.setBounds(topBorder.getX(), bottomBorder.getY() - height - BORDER_THICKNESS, width + 2 * BORDER_THICKNESS, BORDER_THICKNESS);
        rightBorder.setBounds(topRightX, bottomBorder.getY() - height, BORDER_THICKNESS, height);
        leftBorder.setBounds(leftBorder.getX(), topBorder.getY()  + BORDER_THICKNESS, BORDER_THICKNESS, height);
        bottomBorder.setBounds(bottomBorder.getX(), bottomBorder.getY(), width + 2 * BORDER_THICKNESS, BORDER_THICKNESS);

        relocateDragBox(width, height, true);
        relocateResizeHandles(width);
        
        updateObservers();
        repaintCaptureBox();
    }

    /**
     * Bottom right corner resizing procedure
     * @param topRightX 
     * @param width
     * @param height 
     */
    private void resizeFromBottomRight(int bottomRightX, int width, int height) {
        if (isCaptureboxLocked) {
            return;
        }
        
        if (width < MIN_BOX_DIMENSION.width) {
            width = MIN_BOX_DIMENSION.width;
        }
        if (height < MIN_BOX_DIMENSION.height) {
            height= MIN_BOX_DIMENSION.height;
        }
        
        // Relocate & resize all the borders.
        topBorder.setBounds(topBorder.getX(), topBorder.getY(), width + 2 * BORDER_THICKNESS, BORDER_THICKNESS);
        rightBorder.setBounds(bottomRightX, topBorder.getY() + BORDER_THICKNESS, BORDER_THICKNESS, height);
        leftBorder.setBounds(leftBorder.getX(), topBorder.getY() + BORDER_THICKNESS, BORDER_THICKNESS, height);
        bottomBorder.setBounds(bottomBorder.getX(), topBorder.getY() + height + BORDER_THICKNESS, width + 2 * BORDER_THICKNESS, BORDER_THICKNESS);
        
        relocateDragBox(width, height, true);
        relocateResizeHandles(width);
        
        updateObservers();
        repaintCaptureBox();
    }

    /**
     * Top left corner resizing procedure
     * @param topRightX 
     * @param width
     * @param height 
     */
    private void resizeFromTopLeft(int topLeftX, int width, int height) {
        if (isCaptureboxLocked) {
            return;
        }
        
        if (width < MIN_BOX_DIMENSION.width) {
            width = MIN_BOX_DIMENSION.width;
        }
        if (height < MIN_BOX_DIMENSION.height) {
            height= MIN_BOX_DIMENSION.height;
        }
        
        // Relocate & resize all the borders.
        topBorder.setBounds(topLeftX - BORDER_THICKNESS, bottomBorder.getY() - height - BORDER_THICKNESS, width + 2 * BORDER_THICKNESS, BORDER_THICKNESS);
        rightBorder.setBounds(rightBorder.getX(), bottomBorder.getY() - height, BORDER_THICKNESS, height);
        leftBorder.setBounds(topLeftX - BORDER_THICKNESS, bottomBorder.getY() - height, BORDER_THICKNESS, height);
        bottomBorder.setBounds(topLeftX - BORDER_THICKNESS, bottomBorder.getY(), width + 2 * BORDER_THICKNESS, BORDER_THICKNESS);

        relocateDragBox(width, height, true);
        relocateResizeHandles(width);
        
        updateObservers();
        repaintCaptureBox();
    }

    /**
     * Bottom left corner resizing procedure
     * @param topRightX 
     * @param width
     * @param height 
     */
    private void resizeFromBottomLeft(int bottomLeftX, int width, int height) {
        if (isCaptureboxLocked) {
            return;
        }
        
        if (width < MIN_BOX_DIMENSION.width) {
            width = MIN_BOX_DIMENSION.width;
        }
        if (height < MIN_BOX_DIMENSION.height) {
            height= MIN_BOX_DIMENSION.height;
        }
        
        // Relocate & resize all the borders.
        topBorder.setBounds(bottomLeftX - BORDER_THICKNESS, topBorder.getY(), width + 2 * BORDER_THICKNESS, BORDER_THICKNESS);
        rightBorder.setBounds(rightBorder.getX(), topBorder.getY() + BORDER_THICKNESS, BORDER_THICKNESS, height);
        leftBorder.setBounds(bottomLeftX - BORDER_THICKNESS, topBorder.getY() + BORDER_THICKNESS, BORDER_THICKNESS, height);
        bottomBorder.setBounds(bottomLeftX - BORDER_THICKNESS, topBorder.getY() + height + BORDER_THICKNESS, width + 2 * BORDER_THICKNESS, BORDER_THICKNESS);

        relocateDragBox(width, height, true);
        relocateResizeHandles(width);
        
        updateObservers();
        repaintCaptureBox();
    }

    /**
     * Updates the internal state of screen recorder so the recorder knows 
     * the size and position to take screenshots.
     */
    private void updateObservers() {
        if (recordingViewState == CaptureBoxState.FULLSCREEN) {
            // Set current screen as full screen
            recorder.setCaptureRectangle(this.fullCaptureRect);
        } else if (recordingViewState == CaptureBoxState.CUSTOM_SCREEN) {
            // Set current screen as custom
            recorder.setCaptureRectangle(this.getCaptureRectangle());
        }
        
    }

    /**
     * Moves dragbox to given location
     * @param width width of drag box
     * @param height hieght of dragbox
     * @param showSize Updates drag box label with capturebox size if True
     */
    private void relocateDragBox(int width, int height, boolean showSize) {
        if (showSize) {
            this.relocateDragBox.width  = width;
            this.relocateDragBox.height = height;
            this.dragBox.updateSizeLabel(relocateDragBox);
        }
        
        dragBox.setLocation(
                topBorder.getX() + BORDER_THICKNESS + (width - dragBox.getWidth()) / 2,
                topBorder.getY() + BORDER_THICKNESS + (height - dragBox.getHeight()) / 2);
    }
    
    /**
     * Moves the resize handles based on the current width of the CaptureBox.
     * @param width 
     */
    private void relocateResizeHandles(int width) {
        nwResize.setLocation(
                topBorder.getX() + (BORDER_THICKNESS / 2) - (nwResize.getWidth() / 2), 
                topBorder.getY() + (BORDER_THICKNESS / 2) - (nwResize.getHeight() / 2));
        neResize.setLocation(
                topBorder.getX() + BORDER_THICKNESS + (BORDER_THICKNESS / 2) + width - (neResize.getWidth() / 2), 
                topBorder.getY() + (BORDER_THICKNESS / 2) - (neResize.getHeight() / 2));
        swResize.setLocation(
                bottomBorder.getX() + (BORDER_THICKNESS / 2) - (swResize.getWidth() / 2),
                bottomBorder.getY() + (BORDER_THICKNESS / 2) - (swResize.getHeight() / 2));
        seResize.setLocation(
                bottomBorder.getX() + BORDER_THICKNESS + (BORDER_THICKNESS / 2) + width - (seResize.getWidth() / 2),
                bottomBorder.getY() + (BORDER_THICKNESS / 2) - (seResize.getHeight() / 2));
    }
    
    private static void log(Object message){
        LogUtil.log(CaptureBox.class, message);
    }
}