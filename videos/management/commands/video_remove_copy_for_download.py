import os
from django.conf import settings
from django.core.management.base import NoArgsCommand

from videos.models import Video


class Command(NoArgsCommand):
    help = "Removes copies of videos that were obtained from S3 needed by the video download view."

    def handle_noargs(self, **options):
        pass
        #~for video in Video.objects.filter(expired=True):
            # Since this is an expired video object, the file should
            # only exist on S3 and not on our servers
            #~if video.videoupload:
            #~    video_path = video.videoupload.path

            #~    if os.path.exists(video_path) and os.path.isfile(video_path):
            #~        os.remove(video_path)
            #~        print 'Removed %s from the server' % video_path

            # also do this for the mobile version
            #~if video.mobile_video:
            #~    mobile_video_path = video.mobile_video.path

            #~    if os.path.exists(mobile_video_path) and os.path.isfile(mobile_video_path):
            #~        os.remove(mobile_video_path)
            #~        print 'Removed %s from the server' % mobile_video_path

