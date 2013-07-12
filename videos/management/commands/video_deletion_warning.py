import datetime

from django.core.management.base import NoArgsCommand
from django.core.mail import send_mail
from django.template.loader import render_to_string
from django.contrib.sites.models import Site

from videos.management.commands.video_daily_tasks import is_deletable
from videos.models import Video, DAYS_BEFORE_DELETION
from videos.utils import is_expired

NOTIF_SPAN_IN_DAYS = 7

class Command(NoArgsCommand):
    help = "Warn user that videos will be deleted soon."

    def handle_noargs(self, **options):
        videos = Video.objects.all()
        user_videos = {}
        for video in videos:
            print "Video: " + video.title
            if video.uploader and is_expired(video) and not is_deletable(video):
                print "Expired and will be deleted soon? True"
                if video.uploader not in user_videos:
                    user_videos[video.uploader] = []
                user_videos[video.uploader].append(video)
            else:
                print "Expired and will be deleted soon? False"
        for uploader, videos in user_videos.iteritems():
            send_delete_warning(uploader, videos)

def send_delete_warning(uploader, videos):
    ''' Checks videos if will be deleted soon in NOTIF_SPAN_IN_DAYS days.
        Sends warning to the user.
    '''
    videos_soon_deleted = []
    deletion_date_list = []
    days_left_list = []
    for video in videos:
        today = datetime.date.today()
        deletion_date = video.created + datetime.timedelta(days=DAYS_BEFORE_DELETION)
        days = deletion_date.date() - today
        if days.days <= NOTIF_SPAN_IN_DAYS:
            videos_soon_deleted.append({'video': video, 'deletion_date': deletion_date.strftime('%b %d, %Y'), 'days':days.days})
    
    message = render_to_string("email/delete-warning.txt", {'videos':videos_soon_deleted, 
                                                            'user':video.uploader, 
                                                            'site':Site.objects.get_current()
                                                            })
    try:
        send_mail('%s: Video will be Deleted' % Site.objects.get_current().name,
                  message,
                  "no-reply@%s" % Site.objects.get_current().domain,
                  [uploader.email], fail_silently=False)
    except:
        pass
