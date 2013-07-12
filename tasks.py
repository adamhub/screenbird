import time
import datetime
import decimal
import subprocess
import shlex
import os
import settings
#import uwsgi
#from uwsgidecorators import *

#@spool
def encode_video(args):
    mp4 = args['mp4']
    output_mp4 = args['output_mp4']
    cmd = ('''/usr/local/bin/ffmpeg -y '''
           '''-i "%s" '''
           '''-acodec libfaac '''
           '''-ar 48000 '''
           '''-ab 128k '''
           '''-ac 2 '''
           '''-vcodec libx264 '''
           '''-b 1200k '''
           '''-flags +loop+mv4 '''
           '''-cmp 256 '''
           '''-partitions +parti4x4+partp8x8+partb8x8 '''
           '''-subq 7 '''
           '''-trellis 1 '''
           '''-refs 5 '''
           '''-coder 0 '''
           '''-me_range 16 '''
           '''-keyint_min 25 '''
           '''-sc_threshold 40 '''
           '''-i_qfactor 0.71 '''
           '''-bt 1200k '''
           '''-maxrate 1200k '''
           '''-bufsize 1200k '''
           '''-rc_eq "blurCplx^(1-qComp)" '''
           '''-qcomp 0.6 '''
           '''-qmin 10 '''
           '''-qmax 51 '''
           '''-qdiff 4 '''
           '''-level 30 '''
           '''%s '''
           '''-r 30 '''
           '''-g 90 '''
           '''-async 2 '''
           '''"%s"''')

    pipe = subprocess.call(
        shlex.split(cmd % (mp4, '"-aspect" "16:9"', output_mp4)))

    if pipe == 1:
        pipe = subprocess.call(
        shlex.split(cmd % (mp4, '"-aspect" "16:9" "-vf" "scale=-1:360,pad=ow:oh:0:0:black"', output_mp4)))
        if pipe == 1:
            pipe = subprocess.call(
            shlex.split(cmd % (mp4, '"-aspect" "16:9" "-vf" "scale=-1:360,pad=640:360:100:0:black"', output_mp4)))
            if pipe == 1:
                pipe = subprocess.call(
                    shlex.split(cmd % (mp4, '"-aspect" "16:9" "-vf" "scale=-1:360,pad=640:360:0:0:black"', output_mp4)))
                if pipe == 1:
                    # Quick fix for input videos with odd resolution,sets video resolution to 640x360 exactly
                    pipe = subprocess.call(
                        shlex.split(cmd % (mp4, '"-aspect" "16:9" "-vf" "scale=640:360"', output_mp4)))
                    if pipe == 1:
                        # Error prompts when there is an error with the ffmpeg encoding
                        raise IOError('FFMPEG Encoding Error!')

    # For converting mp4 to progressive download
    qt = os.path.join(settings.PROJECT_ROOT, 'qtfaststart.py')
    subprocess.call([qt, output_mp4])
    # Permissions for uploaded mp4
    os.chmod(output_mp4,0774)
    #return uwsgi.SPOOL_OK


def encode_mobile_video(args):
    mp4 = args['mp4']
    output_mp4 = args['output_mp4']
    cmd = ('''/usr/local/bin/ffmpeg -y '''
           '''-i "%s" '''
           '''-s 432x320 '''
           '''-b 384k '''
           '''-vcodec libx264 '''
           '''-flags +loop+mv4 '''
           '''-cmp 256 '''
           '''-partitions +parti4x4+parti8x8+partp4x4+partp8x8 '''
           '''-subq 6 '''
           '''-trellis 0 '''
           '''-refs 5 '''
           '''-bf 0 '''
           '''-flags2 +mixed_refs '''
           '''-coder 0 '''
           '''-me_range 16 '''
           '''-g 250 '''
           '''-keyint_min 25 '''
           '''-sc_threshold 40 '''
           '''-i_qfactor 0.71 '''
           '''-qmin 10 -qmax 51 '''
           '''-qdiff 4 '''
           '''-acodec libfaac '''
           '''-ac 1 '''
           '''-ar 16000 '''
           '''-r 12 '''
           '''-ab 32000 '''
           '''-aspect 16:9 '''
           '''"%s"''')

    pipe = subprocess.call(
        shlex.split(cmd % (mp4, output_mp4)))

    if pipe == 1:
        raise IOError('FFMPEG Encoding Error!')

    # For converting mp4 to progressive download
    qt = os.path.join(settings.PROJECT_ROOT, 'qtfaststart.py')
    subprocess.call([qt, output_mp4])
    # Permissions for uploaded mp4
    os.chmod(output_mp4,0774)
    #return uwsgi.SPOOL_OK
