#!/bin/bash

#Grab file location or ScreenRecorder app
JAVA_EXECUTABLE="$1"
SCREEN_RECORDER="$2"
#Custom command for launching Mac OS X Screen Recorder app
#$JAVA_EXECUTABLE -Xdock:name='Screen Recorder' -classpath $SCREEN_RECORDER com.bixly.pastevid.screencap.ScreenRecorder
$JAVA_EXECUTABLE -Xdock:name='Screen Recorder' -classpath $SCREEN_RECORDER com.bixly.pastevid.screencap.ScreenRecorder
