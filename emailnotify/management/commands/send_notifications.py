import datetime
import sys

from django.core.management.base import BaseCommand, CommandError
from django.conf import settings
from django.template.loader import render_to_string
from django.db.models import Q
from django.contrib.auth.models import User
from django.contrib.sites.models import Site


EMAIL_EVERY = getattr(settings, 'EMAILNOTIFY_SEND_EVERY', 14)


class Command(BaseCommand):
    help = 'Email members a list of their videos that are about to expire.'
    
    def handle(self, *args, **options):
        verbosity = int(options.get('verbosity', 1))
    
        # Do not include anonymous users and users without a given
        # email address.
        users = User.objects.exclude(email='').exclude(email=None).exclude(userprofile__is_anonymous=True)

        datetime_now = datetime.datetime.now()
        until = datetime_now + datetime.timedelta(days=EMAIL_EVERY)

        for user in users:
            # Receive videos that are going to expire in at most
            # `EMAIL_EVERY` days.
            expiring_videos = list(user.video_set.filter(expiry_date__range=(datetime_now, until)).filter(Q(youtube_embed_url=None) | Q(youtube_embed_url='')))
            count = len(expiring_videos)
            
            if count > 0:
                if verbosity > 1:
                    sys.stdout.write('Sending notification to %s about %s expiring video%s... ' % (user, count, 's' if count > 1 else ''))
            
                message = render_to_string("email/expiring_videos.txt", {
                    'user': user,
                    'expiring_videos': expiring_videos,
                    'site': Site.objects.get_current()
                })

                sys.stdout.write('%s\n' % message)

                user.email_user('Some of your videos are about to expire!',
                                message)
                
                if verbosity > 1:
                    sys.stdout.write('[SENT]\n')

