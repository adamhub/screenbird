import time
import datetime
import os
import pika
import logging
import boto
import math

from boto.s3.key import Key
from decimal import Decimal

from django.conf import settings
from django.contrib.auth.models import User
from django.core.mail import EmailMessage
from django.core.management.base import NoArgsCommand

from amazon.s3 import Connection
from amazon.utils import delete_from_s3, get_from_s3
from videos.forms import reserve_slug
from videos.models import Video, ReservedSlug, VideoStatus
from videos.utils import enqueue

handler = logging.StreamHandler()
formatter = logging.Formatter('%(message)s')
handler.setFormatter(formatter)

logger = logging.getLogger('videos.views')
logger.addHandler(handler) 
logger.setLevel(logging.DEBUG)

FILE_KEY = settings.FILE_KEY
SLUG_LENGTH = getattr(settings, 'VIDEO_SLUG_LENGTH', 7)
LONG_VIDEO = settings.LONG_VIDEO
LONG_VIDEO_LENGTH = 5.13
LONG_VIDEO_ENCODING = 0.86
LONG_VIDEO_SIZE = 13584052
SHORT_VIDEO = settings.SHORT_VIDEO
SHORT_VIDEO_LENGTH = 2.15
SHORT_VIDEO_ENCODING = 0.35
SHORT_VIDEO_SIZE = 4212513

def upload_to_s3(key, filename):
    #similar to send_to_s3 except for the make_public() step
    try:
        conn = boto.connect_s3(settings.AWS_ACCESS_KEY_ID, settings.AWS_SECRET_ACCESS_KEY)
        bucket = conn.get_bucket(settings.AWS_VIDEO_BUCKET_NAME)
        
        k = Key(bucket)
        k.key = key
        k.set_contents_from_filename(filename)
        k.make_public()
    except:
        logger.critical("Failed to upload to s3.")

def check_from_s3(key):
    video = get_from_s3(key)
    if video:
        print ("Video is in s3.")
    else:
        print ("Video is not in s3.")

def is_encoded(slug):
    video_status = VideoStatus.objects.get(video_slug=slug)
    return video_status.web_available

class Command(NoArgsCommand):
    help = "Uploads video to s3, checks if it was rendered properly, and deletes it from s3."

    def handle_noargs(self, **options):
        short_video_size_passed = False
        short_video_duration_passed = False
        short_video_encode_time_passed = False

        long_video_size_passed = False
        long_video_duration_passed = False
        long_video_encode_time_passed = False

        #check if rabbitmq is working
        try:
            connection = pika.BlockingConnection(pika.ConnectionParameters(settings.RABBITMQ_SERVER))
        except:
            logger.critical("We are experiencing a connection problem and cannot perform uploads for now. Please try again later.")

        # Get uploader
        uploader, created = User.objects.get_or_create(username='encodetester')

        # Get video
        slug = reserve_slug(SLUG_LENGTH)
        title = "Encode Test A"
        description = "5-minute video with audio"
        video = Video(title=title, slug=slug, uploader=uploader, description=description)
        video.save()
        print ("Video slug: %s" % slug)
        slug_tmp = slug + '_tmp'

        print ("Uploading file to s3...")
        upload_to_s3(slug_tmp, LONG_VIDEO)
        print ("Upload process successful.")
        
        print ("Enqueuing slug to rabbitmq...")
        enqueue(slug_tmp)
        print ("Enqueue process successful.")

        # Mark reserved slug as used
        try:
            reserved_slug = ReservedSlug.objects.get(slug=slug)
        except:
            pass
        else:
            reserved_slug.used = True
            reserved_slug.save()

        while not is_encoded(slug):
            time.sleep(3)

        print ("Encoded!")

        # Checks
        video = Video.objects.get(slug=slug)
        video_duration = video.video_duration
        if Decimal(str(video_duration)) != Decimal(str(LONG_VIDEO_LENGTH)):
            print ("Long video duration check failed.")
            print ("%s vs %s" % (video_duration, LONG_VIDEO_LENGTH))
        else:
            print ("Long video duration check passed.")
            long_video_duration_passed = True

        video_status = VideoStatus.objects.get(video_slug=slug)
        duration_difference = math.fabs(Decimal(str(video_status.encode_duration)) - Decimal(str(LONG_VIDEO_ENCODING)))
        percent_error = (duration_difference / LONG_VIDEO_ENCODING) * 100
        if percent_error > 30.0:
            print ("Long video encoding time check failed.")
        else:
            print ("Long video encoding time check passed.")
            long_video_encode_time_passed = True
        print ("Percent error: %s" % percent_error)

        video_file_len = len(get_from_s3(slug))
        size_difference = math.fabs(video_file_len - LONG_VIDEO_SIZE)
        percent_error = (size_difference / LONG_VIDEO_SIZE) * 100
        if percent_error > 5.0:
            print ("Long video encoded size check failed.")
        else:
            print ("Long video encoded size check passed.")
            long_video_size_passed = True
        print ("Percent error: %s" % percent_error)

        # Get video
        slug = reserve_slug(SLUG_LENGTH)
        title = "Encode Test B"
        description = "2-minute video with no audio"
        video = Video(title=title, slug=slug, uploader=uploader, description=description)
        video.save()
        print ("Video slug: %s" % slug)
        slug_tmp = slug + '_tmp'

        print ("Uploading file to s3...")
        upload_to_s3(slug_tmp, SHORT_VIDEO)
        print ("Upload process successful.")
        
        print ("Enqueuing slug to rabbitmq...")
        enqueue(slug_tmp)
        print ("Enqueue process successful.")

        # Mark reserved slug as used
        try:
            reserved_slug = ReservedSlug.objects.get(slug=slug)
        except:
            pass
        else:
            reserved_slug.used = True
            reserved_slug.save()

        while not is_encoded(slug):
            time.sleep(3)

        print ("Encoded!")

        # Checks
        video = Video.objects.get(slug=slug)
        video_duration = video.video_duration
        if Decimal(str(video_duration)) != Decimal(str(SHORT_VIDEO_LENGTH)):
            print ("Short video duration check failed.")
            print ("%s vs %s" % (video_duration, SHORT_VIDEO_LENGTH))
        else:
            print ("Short video duration check passed.")
            short_video_duration_passed = True

        video_status = VideoStatus.objects.get(video_slug=slug)
        duration_difference = math.fabs(Decimal(str(video_status.encode_duration)) - Decimal(str(SHORT_VIDEO_ENCODING)))
        percent_error = (duration_difference / SHORT_VIDEO_ENCODING) * 100
        if percent_error > 30.0:
            print ("Short video encoding time check failed.")
        else:
            print ("Short video encoding time check passed.")
            short_video_encode_time_passed = True
        print ("Percent error: %s" % percent_error)

        video_file_len = len(get_from_s3(slug))
        size_difference = math.fabs(video_file_len - SHORT_VIDEO_SIZE)
        percent_error = (size_difference / SHORT_VIDEO_SIZE) * 100
        if percent_error > 5.0:
            print ("Short video encoded size check failed.")
        else:
            print ("Short video encoded size check passed.")
            short_video_size_passed = True
        print ("Percent error: %s" % percent_error)

        if not ((long_video_duration_passed and long_video_encode_time_passed and long_video_size_passed) and (short_video_duration_passed and short_video_encode_time_passed and short_video_size_passed)):
            print ("Encoding tests failed!")
            message = \
"""
5-minute video with audio
    Video time check passed: %s
    Video encoding time check passed: %s
    Video size check passed: %s

2-minute video with audio
    Video time check passed: %s
    Video encoding time check passed: %s
    Video size check passed: %s
""" % (long_video_duration_passed, long_video_encode_time_passed, long_video_size_passed, short_video_duration_passed, short_video_encode_time_passed, short_video_size_passed)

            email = EmailMessage(
                subject = "Encoding tests failed",
                body = message,
                from_email = "api@theirwork.com",
                # to = ["%s@tickets.assembla.com" % settings.ASSEMBLA_SPACE,],
                to = ["email@example.com",]
            )
        else:
            print ("Encoding tests passed!")

