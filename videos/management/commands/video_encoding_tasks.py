"""
A management command which checks videos
ready for encoding(VideoStatus--> is_encoding=True)
which could be found on /tmp folder.
After video is encoded it would be placed on
/videos folder and the original
video on /tmp folder would then be deleted.

"""
import os
import datetime

from django.conf import settings
from django.core.management.base import NoArgsCommand
from django.contrib.auth.models import User
from django.core.mail import send_mail

from videos.models import Video, VideoStatus
from videos.utils import is_expired, days_left
from tasks import encode_video, encode_mobile_video

SITE = 'Screenbird'

class Command(NoArgsCommand):
    help = "Encodes video on /tmp folder. Deletes video on /tmp after uploading."

    def handle_noargs(self, **options):
        videos = Video.objects.all()
        # Commented out since there encoding happens in ec2 now
        comment = '''
        for video in videos:
            #Check videos status if is_encoding or not
            try:
                video_status_obj = VideoStatus.objects.get(video_slug=video.slug)
            except VideoStatus.DoesNotExist:
                pass
            else:
                if video_status_obj.is_encoding:
                    print "Video Ready for encoding: %s" % video.title
                    #if videos status is_encoding = True; Encode video;
                    temporary_name = video.temporary_name
                    browser_mp4 = video.videoupload.path
                    mobile_mp4 = video.mobile_video.path

                    print "%s" % settings.MEDIA_ROOT
                    print "Browser Mp4: %s" % browser_mp4
                    print "Mobile Mp4: %s" % mobile_mp4

                    mp4 = os.path.join(settings.MEDIA_ROOT,'tmp/%s.mp4' % temporary_name)
                    output_mp4 = os.path.join(settings.MEDIA_ROOT, browser_mp4)
                    output_mobile_mp4 = os.path.join(settings.MEDIA_ROOT, mobile_mp4)

                    # Video encoding
                    try:
                        encode_video({
                            'mp4': str(mp4),
                            'output_mp4': str(output_mp4)
                        })
                        video_status_obj.web_available = True
                        video_status_obj.save()
                        encode_mobile_video({
                            'mp4': str(mp4),
                            'output_mp4': str(output_mobile_mp4)
                        })
                        video_status_obj.mobile_available = True
                        video_status_obj.save()
                    except Exception, e:
                        # do not set is_encoding flag for video to True
                        # the next time this task is run, encoding will
                        # be done for the same video

                        print "Error in encoding %s: %s" % (video.title, str(e))
                    else:
                        # Finished encoding for video
                        video_status_obj.is_encoding = False
                        video_status_obj.save()
                        print "Video finished encoding: %s" % video.title

                        # Remove temporary video
                        os.remove(mp4)
                        print "Video Deleted: %s" % mp4'''
