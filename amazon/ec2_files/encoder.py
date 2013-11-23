import time
import datetime
import boto
import os
import settings
from httplib import HTTPConnection
from ffmpeg import encode_video, encode_mobile_video
import logging
import pika
import shutil

formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)

fh = logging.FileHandler('encoder.log')
fh.setLevel(logging.DEBUG)
fh.setFormatter(formatter)
logger.addHandler(fh)

connection = pika.BlockingConnection(pika.ConnectionParameters(settings.RABBIT_MQ_DOMAIN))
video_queue = connection.channel()
video_queue.queue_declare(queue=settings.QUEUE_NAME, durable=True)

use_s3 = settings.USE_S3
if use_s3:
    s3 = boto.connect_s3(settings.AWS_ACCESS_KEY_ID, settings.AWS_SECRET_ACCESS_KEY)
    bucket = s3.get_bucket(settings.AWS_VIDEO_BUCKET_NAME)

ec2_name = settings.INSTANCE_NAME

tmp_folder = os.path.dirname(os.path.abspath(__file__))
if not use_s3:
    tmp_folder = os.path.join(tmp_folder, os.path.pardir, os.path.pardir, 'media', 'tmp')
conn = HTTPConnection(settings.DOMAIN_NAME)
conn.request("GET", "/amazon/ec2-ready/%s/" % settings.INSTANCE_ID)
conn.getresponse()

def encode(ch, method, properties, body):
    """Fetches the temporary video file from s3 and encodes both web and mobile
    versions.
    
    Communicates with the screenbird server with the following
    - sets the `ec2_node` field of the associated `VideoStatus` for the video 
      for identification of where to get encoding progress output of ffmpeg
    - sets the `web_available` field of the associated `VideoStatus` for the
      video after successfully encoding the web version
    - sets the `mobile_available` field of the associated `VideoStatus` for the
      video after successfully encoding the mobile version
    - sets the `video_duration` field of the associated `Video` object 
    
    """
    param = eval(body)
    slug_tmp = param[0]
    host = param[1]
    mp4 = os.path.join(tmp_folder, "%s.mp4" % slug_tmp)
    slug = slug_tmp.replace("_tmp","")
    text_file = os.path.join(tmp_folder, "%s.txt" % slug)
    output_mp4 = os.path.join(tmp_folder, "%s.mp4" % slug)
    output_mobile_mp4 = os.path.join(tmp_folder, "%s__mobile.mp4" % slug)
    if not use_s3:
        shutil.copyfile(output_mp4, mp4)
    if use_s3:
        key = bucket.get_key(slug_tmp)
    else:
        key = slug
    logger.info("Started encoding for %s" % slug_tmp)
    
    # Define these as variables to make it easier to change later on if need be
    set_encoding_url = "/set-encoding-ec2/%s/%s/"
    set_available_url = "/set-available/%s/%s/"
    time_diff = None

    if key:
        if use_s3:
            # Download source video to be encoded
            try:
                key.get_contents_to_filename(mp4)
            except:
                ch.basic_ack(delivery_tag = method.delivery_tag)

        # Notify requesting server that video is being processed
        try:
            conn = HTTPConnection(host)
            conn.request("GET", set_encoding_url % (slug, ec2_name))
            response = conn.getresponse()
        except Exception, e:
            pass
        logger.info("Encoding for %s - web version" % slug_tmp)
        available = False

        # Check from requesting server if video is already encoded
        try:
            conn = HTTPConnection(host)
            conn.request("GET", "/is-available/%s/%s/" % (slug, 'web'))
            response = conn.getresponse()
            available = eval(response.read())
        except:
            pass
        try:
            # Start encoding for video if not yet encoded
            if not available:
                start_time = datetime.datetime.now()
                encode_video({
                    'mp4': str(mp4),
                    'output_mp4': str(output_mp4),
                    'text_file': str(text_file)
                })
                end_time = datetime.datetime.now()
                time_diff = end_time - start_time
                logger.info("Web encoding time: %s" % time_diff)
            else:
                logger.info("Encoded version for %s already available - web version" % slug_tmp)
        except Exception, e:
            logger.info("Encoding for %s failed - web version: %s" % (slug_tmp, e))
            ch.basic_ack(delivery_tag = method.delivery_tag)
            ch.basic_publish(exchange='',
                routing_key=settings.QUEUE_NAME,
                body=body)
        else:
            # Send encoded video to s3
            try:
                if use_s3:
                    if not available:
                        new_key = bucket.new_key(slug)
                        if bucket.get_key(slug):
                            bucket.delete_key(slug)
                        new_key.set_contents_from_filename(output_mp4)
                        new_key.set_acl('public-read')
            except:
                logger.info("Sending to S3 for %s failed - web version" % slug_tmp)
                ch.basic_ack(delivery_tag = method.delivery_tag)
                ch.basic_publish(exchange='',
                    routing_key=settings.QUEUE_NAME,
                    body=body)
            else:
                try:
                    # Notify requesting server that encoded video is already available
                    if not available:
                        conn = HTTPConnection(host)
                        conn.request("GET", set_available_url % (slug, 'web'))
                        response = conn.getresponse()

                        # Open ffmpeg output of encoding process and parse information on video
                        # duration. Send duration value to `set_duration` view to set the
                        # `video_duration` field of the `Video` object afterwards
                        ffmpeg_out = open(text_file)
                        info = [x for x in ffmpeg_out.readlines() if "Duration" in x]
                        if len(info) > 0:
                            duration = (info[0].split(',')[0].split('  Duration: ')[1])
                            hours, minutes, seconds = [float(x) for x in duration.split(':')]
                            x = datetime.timedelta(hours=hours, minutes=minutes, seconds=seconds)
                            mins = str(x.seconds / 60.0)
                            logger.info("%s Mins: %s" % (slug_tmp, mins))
                            try:
                                conn = HTTPConnection(host)
                               	conn.request("GET", "/set-duration/%s/?duration=%s" % (slug, mins))
                                response = conn.getresponse()
                            except:
                                pass
                        if time_diff:
                            mins = str(time_diff.total_seconds() / 60.0)
                            try:
                                conn = HTTPConnection(host)
                                conn.request("GET", "/set-encode-time/%s/?duration=%s" % (slug, mins))
                                response = conn.getresponse()
                            except:
                                pass
                except:
                    logger.info("Cannot ping server for %s - web version" % slug_tmp)
                    ch.basic_ack(delivery_tag = method.delivery_tag)
                    ch.basic_publish(exchange='',
                        routing_key=settings.QUEUE_NAME,
                        body=body)
                else:
                    # Start encoding mobile version of the video
                    logger.info("Encoding for %s - mobile version" % slug_tmp)
                    available = False
                    try:
                        conn = HTTPConnection(host)
                        conn.request("GET", "/is-available/%s/%s/" % (slug, 'mobile'))
                        response = conn.getresponse()
                        available = eval(response.read())
                    except:
                        pass
                    try:
                        if not available:
                            encode_mobile_video({
                                'mp4': str(mp4),
                                'output_mp4': str(output_mobile_mp4)
                            })
                        else:
                            logger.info("Encoded version for %s already available - mobile version" % slug_tmp)
                    except:
                        logger.info("Encoding for %s failed - mobile version" % slug_tmp)
                        ch.basic_ack(delivery_tag = method.delivery_tag)
                        ch.basic_publish(exchange='',
                            routing_key=settings.QUEUE_NAME,
                            body=body)
                    else:
                        # Send encoded mobile version to s3
                        mobile_slug = "%s__mobile" %slug
                        try:
                            if use_s3:
                                if not available:
                                    new_key = bucket.new_key(mobile_slug)
                                    if bucket.get_key(mobile_slug):
                                        bucket.delete_key(mobile_slug)
                                    new_key.set_contents_from_filename(output_mobile_mp4)
                                    new_key.set_acl('public-read')
                        except:
                            logger.info("Sending to S3 for %s failed - mobile version" % slug_tmp)
                            ch.basic_ack(delivery_tag = method.delivery_tag)
                            ch.basic_publish(exchange='',
                                routing_key=settings.QUEUE_NAME,
                                body=body)
                        else:
                            try:
                                if not available:
                                    conn = HTTPConnection(host)
                                    conn.request("GET", set_available_url % (slug, 'mobile'))
                                    response = conn.getresponse()
                            except:
                                logger.info("Cannot ping server for %s - mobile version" % slug_tmp)
                                ch.basic_ack(delivery_tag = method.delivery_tag)
                                ch.basic_publish(exchange='',
                                    routing_key=settings.QUEUE_NAME,
                                    body=body)
                            else:
                                if use_s3:
                                    key.delete()
                                ch.basic_ack(delivery_tag = method.delivery_tag)

        # Remove original video, temporary video encodings, and ffmpeg output
        # files from this ec2 node
        try:
            if use_s3:
                os.remove(output_mobile_mp4)
                os.remove(output_mp4)
            os.remove(text_file)
            os.remove(mp4)
            logger.info("Cleaned files for %s " % slug_tmp)
        except:
            pass

video_queue.basic_consume(encode,
                      queue=settings.QUEUE_NAME)
video_queue.start_consuming()

