import time
import datetime
import os
from django.conf import settings
from django.core.management.base import NoArgsCommand

from amazon.models import S3UploadQueue
from amazon.utils import purge_queue


class Command(NoArgsCommand):
    help = "Uploads video to s3 and deletes it from the server."

    def handle_noargs(self, **options):
        # If enabled, push to S3.
        if settings.PUSH_TO_S3:
            print "Purging Queue"
            purge_queue()
        else:
            print "S3 Upload not enabled"
        # Testing chronograph
        # f = open(os.path.join(settings.PROJECT_ROOT, 'chronograph_test'), "a")
        # f.write(str(datetime.datetime.now()) + "\n")
        # f.close()

