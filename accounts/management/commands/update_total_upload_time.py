from decimal import Decimal

from django.contrib.auth.models import User
from django.core.management.base import BaseCommand

class Command(BaseCommand):
    args = '<username username ...>'
    help = "Update the specified users' total_upload_time. Updates all users' upload time if no argument is supplied."

    def handle(self, *args, **options):
        if args:
            users = User.objects.filter(username__in=args)
        else:
            users = User.objects.all()

        for user in users:
            total = Decimal('0.0')
            videos = user.video_set.all()
            for video in videos.iterator():
                if video.video_duration > 0.0:
                    total = total + video.video_duration
                else:
                    video_length = get_video_length(video)
                    total = total + video_length
            user.userprofile.total_upload_time = total
            user.userprofile.save()
        
        print "Successfully updated total time for %s user%s" % (users.count(), 's' if users.count() > 1 else '')
