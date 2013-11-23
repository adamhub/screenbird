import time
import datetime
import decimal
import subprocess
import shlex
import os
import logging

formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)

fh = logging.FileHandler('ffmpeg.log')
fh.setLevel(logging.DEBUG)
fh.setFormatter(formatter)
logger.addHandler(fh)

CMD = ('''/usr/local/bin/ffmpeg -y '''
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
           '''-g 15 '''
           '''-async 2 '''
           '''-threads 4 '''
           '''"%s"''')

#@spool
def encode_video(args):
    logger.info('start encode video')
    mp4 = args['mp4']
    output_mp4 = args['output_mp4']
    text_file = args['text_file']
    # Output text file that will be ping-ed for the encoding progress
    output_text_file = open(text_file, "w+")
    cmd = CMD
    logger.info(shlex.split(cmd % (mp4, '"-aspect" "16:9" "-vf" "pad=max(iw\,ih*(16/9)):ow/(16/9):(ow-iw)/2:(oh-ih)/2:black"', output_mp4)))

    pipe = subprocess.call(shlex.split(cmd % (mp4, '"-aspect" "16:9" "-vf" "pad=max(iw\,ih*(16/9)):ow/(16/9):(ow-iw)/2:(oh-ih)/2:black"', output_mp4)), stdout=output_text_file, stderr=output_text_file)
    if pipe== 1:
        pipe = subprocess.call(shlex.split(cmd % (mp4, '"-aspect" "16:9" "-vf" "scale=-1:360,pad=max(iw\,ih*(16/9)):ow/(16/9):(ow-iw)/2:(oh-ih)/2:black"', output_mp4)), stdout=output_text_file, stderr=output_text_file)
        if pipe == 1:
            # Quick fix for input videos with odd resolution,sets video resolution to 640x360 exactly
            pipe = subprocess.call(shlex.split(cmd % (mp4, '"-aspect" "16:9" "-vf" "scale=640:360"', output_mp4)), stdout=output_text_file, stderr=output_text_file)
            if pipe == 1:
                # Error prompts when there is an error with the ffmpeg encoding
                raise IOError('FFMPEG Encoding Error!')

    output_text_file.close()
    # For converting mp4 to progressive download
    p = subprocess.call(["python", "qtfaststart.py", output_mp4])
    # Permissions for uploaded mp4
    os.chmod(output_mp4,0774)
    #return uwsgi.SPOOL_OK
    logger.info('end encode video')


def encode_mobile_video(args):
    logger.info('start encode mobile video')
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
    subprocess.call(["python", "qtfaststart.py", output_mp4])
    # Permissions for uploaded mp4
    os.chmod(output_mp4,0774)
    #return uwsgi.SPOOL_OK
    logger.info('end encode mobile video')

