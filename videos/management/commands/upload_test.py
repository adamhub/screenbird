import time
import datetime
import os
import pika
import logging
import boto

from boto.s3.key import Key

from django.conf import settings
from django.core.management.base import NoArgsCommand
from amazon.utils import delete_from_s3, get_from_s3
from amazon.s3 import Connection
from videos.utils import enqueue
from videos.models import Video

handler = logging.StreamHandler()
formatter = logging.Formatter('%(message)s')
handler.setFormatter(formatter)

logger = logging.getLogger('upload_test')
logger.addHandler(handler) 
logger.setLevel(logging.DEBUG)

FILE_LOCATION = settings.FILE_LOCATION
FILE_KEY = settings.FILE_KEY

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
        logger.info("Video is in s3.")
    else:
        logger.info("Video is not in s3.")

class Command(NoArgsCommand):
    help = "Uploads video to s3, checks if it was rendered properly, and deletes it from s3."

    def handle_noargs(self, **options):
        #check if rabbitmq is working
        try:
            connection = pika.BlockingConnection(pika.ConnectionParameters(settings.RABBITMQ_SERVER))
        except:
            logger.critical("We are experiencing a connection problem and cannot perform uploads for now. Please try again later.")
        
        logger.info("Uploading file to s3...")
        upload_to_s3(FILE_KEY, FILE_LOCATION)
        logger.info("Upload process successful.")
        
        logger.info("Enqueuing slug to rabbitmq...")
        enqueue(FILE_KEY)
        logger.info("Enqueue process successful.")

        #should indicate that video was uploaded and is stored in s3
        logger.info("Checking if video is already in s3 (should be there)...")
        check_from_s3(FILE_KEY)
        #check if an expiring url can be generated, similar to how Screenbird gets url for video playing
        logger.info("Checking generation of expiring url...")
        try:
            conn = Connection(is_secure=True)
            url = conn.generate_object_expiring_url(FILE_KEY)
            if not url:
                logger.critical("Failed to generate expiring url.")
                raise Exception
        except:
            logger.critical("Failed to check for an expiring url.")
        logger.info("Expiring url successfully generated.")
           
        logger.info("Deleting file from s3...")         
        delete_from_s3(FILE_KEY)
        logger.info("Delete process successful.")
        
        #should indicate that video is not anymore stored in s3 because of the delete
        logger.info("Checking if video is still in s3 (should NOT be there anymore)...")
        check_from_s3(FILE_KEY)
