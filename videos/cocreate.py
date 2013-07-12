import boto
import dictconfig
import logging
import os
import pika
import re
import requests
import subprocess
from httplib import HTTPConnection
try:
    import json
except ImportError:
    import simplejson as json
try:
    from django.conf import settings
except ImportError:
    import settings
try:
    from amazon.ec2_files.ffmpeg import CMD
except:
    from ffmpeg import CMD

folder = os.path.dirname(os.path.abspath(__file__))
ec2_name = settings.INSTANCE_NAME

conf = {
    'version': 1,
    'disable_existing_loggers': False,
    'formatters': {
        'standard': {
            'format': '%(asctime)s [%(levelname)s] %(name)s: %(message)s'
        },
    },
    'handlers': {
        'default': {
            'level':'DEBUG',
            'class':'logging.StreamHandler',
        },
        'file': {
            'level': 'DEBUG',
            'class': 'logging.handlers.RotatingFileHandler',
            'filename': os.path.join(folder, 'cocreate.log'),
            'maxBytes': 8388608,
            'backupCount': 3,
            'formatter': 'standard',
        }
    },
    'loggers': {
        'cocreate': {
            'handlers': ['default', 'file'],
            'level': 'DEBUG',
            'propagate': True
        },
        'django.request': {
            'handlers': ['default'],
            'level': 'WARN',
            'propagate': False
        },
    }
}
dictconfig.dictConfig(conf)
logger = logging.getLogger('cocreate')

# Initialize Rabbitmq connection
connection = pika.BlockingConnection(pika.ConnectionParameters('screenbird.com'))
cocreate_queue = connection.channel()
cocreate_queue.queue_declare(queue=settings.COCREATE_QUEUE_NAME, durable=True)

# Initialize Amazon S3 connection
s3 = boto.connect_s3(settings.AWS_ACCESS_KEY_ID, settings.AWS_SECRET_ACCESS_KEY)
bucket = s3.get_bucket(settings.AWS_VIDEO_BUCKET_NAME)

def download_from_s3(slug):
    """
    Downloads a given slug to a file from s3.
    """
    fname = os.path.join(folder, '%s.cc.mp4' % slug)
    key = bucket.get_key(slug)
    if key:
        try:
            key.get_contents_to_filename(fname)
        except:
            logger.exception("Could not retrieve file %s" % fname)


def check_audio_stream(fname):
    """
    Checks the video file for presence of an audio stream.
    """
    cmd = ['ffmpeg', '-i', fname]
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    stdout, error = p.communicate()
    # Search for an Audio stream on ffmpeg info
    match = re.search(r'Stream #[0-9a-z.()]+: (Audio):', stdout, re.DOTALL)
    return match


def merge_sections(out, sections_fname, copy_codec):
    """
    Merges the sections using mencoder.
    """
    for section in sections_fname:
        # Check for audio
        match = check_audio_stream(section)
        if not match:
            # Create a blank audio stream for videos without one to avoid
            # mencoder throwing an exception about merging with video-only sections
            # logger.debug('%s has no audio stream. Creating one now.' % section)
            slug = re.search(r'(?P<slug>\w+).cc.mp4', section, re.DOTALL).groupdict()['slug']
            cmd = 'ffmpeg -f lavfi -i aevalsrc=0 -i %s -shortest -c:v copy -c:a libfaac -strict experimental %s.av.mp4' % (section, slug)
            val = subprocess.call(cmd, shell=True)
            os.remove(section)
            command = 'mv %s.av.mp4 %s' % (slug, section)
            val = subprocess.call(command, shell=True)
            # logger.debug('Audio stream created for %s.' % section)

    output_slug = re.search(r'(?P<slug>\w+)_tmp.cc.mp4', out, re.DOTALL).groupdict()['slug']
    mencoder_output_fname = '%s_mencoder.txt' % output_slug
    mencoder_output = open(mencoder_output_fname, 'w+')
    inputs = ' '.join(sections_fname)
    codec = 'copy' if copy_codec else 'x264'
    codec = 'x264'
    # command = 'mencoder %s -ovc %s -oac mp3lame -of avi -o %s' % (inputs, codec, out)
    command = 'mencoder %s -ovc %s -oac mp3lame -of lavf -o %s' % (inputs, codec, out)
    # logger.debug('mencoder command: %s' % command)
    val = subprocess.call(command, stdout=mencoder_output, stderr=mencoder_output, shell=True)
    mencoder_output.close()
    if not val == 0:
        # Quick fix for merging with video-only sections
        logger.debug('Merging with video-only section')
        command = command + ' -nosound'
        logger.debug(command)
        val = subprocess.call(command, shell=True)
    return val == 0

def upload_to_s3(slug, filename):
    """
    Uploads given video to s3.
    """
    new_key = bucket.new_key(slug)
    if bucket.get_key(slug):
        bucket.delete_key(slug)
    new_key.set_contents_from_filename(filename)

def enqueue_slug(slug, host):
    """
    Sends a message to enqueue the given slug for the encoder
    """
    connection = pika.BlockingConnection(pika.ConnectionParameters('screenbird.com'))
    channel = connection.channel()
    channel.queue_declare(queue=settings.QUEUE_NAME, durable=True)
    channel.basic_publish(exchange='',
                      routing_key=settings.QUEUE_NAME,
                      body="('%s', '%s')" % (slug, host))
    connection.close()

def get_video_resolution(fname):
    """
    Returns the video resolution (x, y)
    """
    process = subprocess.Popen(['ffmpeg',  '-i', fname], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    stdout, stderr = process.communicate()
    matches = re.search(r", (?P<x>\w+)x(?P<y>\w+)", stdout, re.DOTALL).groupdict()
    return int(matches['x']), int(matches['y'])

def scale_video(fname, x, y):
    """
    Scales the video to x by y size
    """
    out = fname
    tmp = '%s.tmp' % fname
    resolution = '%sx%s' % (x, y)
    # logger.info(".. renaming %s to %s" % (fname, tmp))
    try:
        os.rename(fname, tmp)
    except OSError:
        logger.debug("File %s does not exist. Downloading now" % fname)
        download_from_s3(fname[:-4])
        os.rename(fname, tmp)

    resolution_s = ' -s %s ' % resolution
#    command = 'ffmpeg -i %s -s %s %s' % (tmp, resolution, out)
    command = CMD % (tmp, resolution_s, out)
#    pipe = subprocess.call(shlex.split(cmd % (mp4, '"-aspect" "16:9" "-vf" "pad=max(iw\,ih*(16/9)):ow/(16/9):(ow-iw)/2:(oh-ih)/2:black"', output_mp4)))

    val = subprocess.call(command, shell=True)

    os.remove(tmp) # remove tmp file
    return val == 0


def get_video_duration(fname):
    """
    Returns the duration of a video file in seconds.
    """
    # logger.info('getting video duration for %s' % fname)
    if not os.path.isfile(fname):
        logger.debug('%s does not exist, downloading now' % fname)
        fname_slug = fname[:-4]
        download_from_s3(fname_slug)
    process = subprocess.Popen(['ffmpeg',  '-i', fname], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    stdout, stderr = process.communicate()
    # logger.info('subprocess communicated')
    matches = re.search(r"Duration:\s{1}(?P<hours>\d+?):(?P<minutes>\d+?):(?P<seconds>\d+\.\d+?),", stdout, re.DOTALL)
    if matches:
        matches = matches.groupdict()
    elif not os.path.isfile(fname):
        logger.debug('%s does not exist, downloading now' % fname)
        fname_slug = fname[:-4]
        download_from_s3(fname_slug)
        process = subprocess.Popen(['ffmpeg',  '-i', fname], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)   
        stdout, stderr = process.communicate()
        matches = re.search(r"Duration:\s{1}(?P<hours>\d+?):(?P<minutes>\d+?):(?P<seconds>\d+\.\d+?),", stdout, re.DOTALL).groupdict()

    # logger.info('matches found')
    h = float(matches['hours'])
    m = float(matches['minutes'])
    s = float(matches['seconds'])
    # logger.info('duration: %s:%s:%s' % (h, m, s))
    duration = h*3600 + m*60 + s
    # logger.info('returning duration: %s' % duration)
    return duration

def cumulative_sum(points):
    """
    Returns the cumulative sum.
    """
    # logger.info('calculating cumulative sum')
    csum = [0,]
    prev = 0
    # logger.info('start appending points')
    for p in points:
        csum.append(p+prev)
        prev = p+prev
    # logger.info('returning sum')
    return csum

def post_durations(slug, host, sections_fname):
    url = 'http://%s/cocreate/update-outline/%s/' % (host, slug)
    # logger.info('post durations to %s' % url)
    durations = [get_video_duration(x) for x in sections_fname]
    data = {'data': json.dumps(cumulative_sum(durations[:-1]))}
    # logger.debug(durations)
    # logger.debug(data)
    headers = {'Content-type': 'application/json', 'Accept': 'text/plain'}
    r = requests.post(url, data=data, headers=headers)
    # logger.debug(r)

def cleanup(sections_fname, out):
    slug = re.search(r'(?P<slug>\w+)_tmp.cc.mp4', out, re.DOTALL).groupdict()['slug']
    logger.debug('%s: Removing the temporary files' % slug)
    try:
        for section in sections_fname:
            os.remove(section)
        os.remove(out)
        os.remove('%s_mencoder.txt' % slug)
    except OSError:
        logger.exception('File does not exist')
    # logger.debug('done')

def cocreate_fn(slug, sections, host=""):
    """
    perform the merging of the cocreate sections and then queue it for upload on s3.
    """
    logger.info('%s: Start compilation' % slug)
    out_slug = "%s_tmp" % slug
    out = "%s.cc.mp4" % out_slug
    out = os.path.join(folder, out)

    set_cocreate_url = "/set-cocreate-node/%s/%s/"
    set_available_url = "/set-available/%s/%s/"

    try:
        conn = HTTPConnection(host)
        conn.request("GET", set_cocreate_url % (slug, ec2_name))
        response = conn.getresponse()
    except:
        pass

    # download the files for the sections
    if not sections:
        logger.warn('%s: No sections included' % slug)
        return
    # logger.debug('downloading the sections...')
    for section in sections:
        download_from_s3(section)
    sections_fname = [os.path.join(folder, '%s.cc.mp4' % x) for x in sections]
    if not all([os.path.isfile(x) for x in sections_fname]):
        logger.exception('Missing video file')
        logger.debug(['%s: %s' % (x, os.path.isfile(x)) for x in sections_fname])
        return
    # logger.debug(sections)
    # logger.debug('done.')

    # check and get the max video resolution
    sizes = [get_video_resolution(x) for x in sections_fname]
    xmax = max([x[0] for x in sizes])
    ymax = max([y[1] for y in sizes])
    # logger.debug('the video resolution will be %s' % str((xmax, ymax)))
    logger.debug('%s: Scaling the section videos' % slug)
    copy_codec = True
    for i in range(len(sections_fname)): # only scale videos not matching
        csize = sizes[i]
        if csize[0] != xmax or csize[1] != ymax:
            scale_video(sections_fname[i], xmax, ymax)
            copy_codec = False
    # logger.debug('done')

    # merge the files using mencoder
    logger.info('%s: Merging section videos' % slug)
    if merge_sections(out, sections_fname, copy_codec):
        # logger.info('done.')
        pass
    else:
        cleanup(sections_fname, out)
        logger.warn('%s: failed to merge sections' % slug)
        return

    # upload to s3
    logger.debug('%s: Uploading to s3' % slug)
    upload_to_s3(out_slug, out)
    # logger.debug('done')

    # enqueue the file for ffmpeg processing
    logger.debug('%s: Enqueuing to encoder' % slug)
    enqueue_slug(out_slug, host)
    # logger.debug('done')

    # update the duration for the sections
    logger.debug('%s: Posting durations' % slug)
    post_durations(slug, host, sections_fname)
    # logger.debug('done')

    # delete temporary files
    cleanup(sections_fname, out)

    try:
        conn = HTTPConnection(host)
        conn.request("GET", set_available_url % (slug, 'cocreate'))
        response = conn.getresponse()
    except:
        pass

    logger.info('%s: Done compiling' % slug)

def cocreate(ch, method, properties, body):
    arg = eval(body) # slug, sections, host
    cocreate_fn(arg['slug'], arg['sections'], arg['host'])
    ch.basic_ack(delivery_tag = method.delivery_tag)

# if __name__=='__main__':
cocreate_queue.basic_consume(cocreate,
                             queue=settings.COCREATE_QUEUE_NAME)
cocreate_queue.start_consuming()
