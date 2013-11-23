import os
from django.conf import settings
from django.core.management.base import NoArgsCommand

from videos.models import Video


class Command(NoArgsCommand):
    help = "Cleans out files from the temp folder"

    def handle_noargs(self, **options):
        for video in Video.objects.all():
            # Remove the temporary video if it still exists
            temp_vid = os.path.join(settings.MEDIA_ROOT, 'tmp/%s.mp4' % video.slug)
            try:
                os.remove(temp_vid)
                print "Deleted temporary file: " + video.slug
            except os.error:
                pass

