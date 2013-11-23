from django.core.management.base import NoArgsCommand
from django.contrib.auth.models import User

from videos.models import Video
from accounts.models import UserProfile

class Command(NoArgsCommand):
    help = "Creates UserProfile for those users/video uploaders with no UserProfile created yet."

    def handle_noargs(self, **options):
        users = User.objects.all()
        for user in users:
            try:
                userprofile = UserProfile.objects.get(user=user)
            except UserProfile.DoesNotExist:
                userprofile = UserProfile.objects.create(user=user)
                userprofile.save()
                print 'INFO: Userprofile %s created for User %s.' % (userprofile, user)
            else:
                pass
