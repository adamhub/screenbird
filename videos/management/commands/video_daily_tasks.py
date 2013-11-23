"""
A management command which checks expired videos.
If video is expired user would then be sent a message
informing that video is expired. After 60 days, video
would then be deleted.

"""
import os
import datetime

from django.conf import settings
from django.core.management.base import NoArgsCommand
from django.contrib.auth.models import User
from django.core.mail import send_mail

from videos.models import Video, DAYS_BEFORE_EXPIRATION, DAYS_BEFORE_DELETION
from videos.utils import is_expired, days_left

SITE = 'Screenbird'

class Command(NoArgsCommand):
    help = "Inform user if video has expired. Then delete after 60 days."

    def handle_noargs(self, **options):
        videos = Video.objects.all()
        notify = {}
        delete = {}
        clear_video = {}
        for video in videos:
            if video.uploader:
                if video.channel:
                    # if video is part of a channel
                    # account_level of channel owner will be used
                    account_level = video.channel.owner.userprofile.account_level
                else:
                    # else use account_level of uploader
                    account_level = video.uploader.userprofile.account_level

                if account_level.video_validity and not video.youtube_embed_url:
                    if not video.expired:
                        expired = is_expired(video)
                        if expired:
                            # Notify user
                            user_expired_vids = notify.get(str(video.uploader.id), [])
                            user_expired_vids.append(video)
                            notify[str(video.uploader.id)] = user_expired_vids
                    else:
                        print 'Video has already expired! %s; slug=%s' % (video, video.slug)
                        if is_deletable(video):
                            user_deleted_vids = delete.get(str(video.uploader.id), [])
                            user_deleted_vids.append(video)
                            delete[str(video.uploader.id)] = user_deleted_vids
                        else:
                            print ">>> Not deletable"
                            #send_delete_warning(video)
                            
            else:
                if not video.youtube_embed_url:
                    if not video.expiry_date:
                        expiry_date = video.created + datetime.timedelta(days=DAYS_BEFORE_EXPIRATION)
                        video.expiry_date = expiry_date
                        video.save()
                    expired = days_left(video)
                    if not video.expired:
                        if expired <= 0:
                            video_status = True
                            video.expired = True
                            video.save()
                    else:
                        if is_deletable(video):
                            user_deleted_vids = delete.get('None', [])
                            user_deleted_vids.append(video)
                            delete['None'] = user_deleted_vids
                            #send_delete_warning(video)
            if video.youtube_embed_url:
                today = datetime.date.today()
                print "Checking if %s's youtube expiry, %s, has expired." % (video, video.youtube_video_expiry_date)
                if video.youtube_video_expiry_date:
                    days = video.youtube_video_expiry_date.date() - today
                    print "YouTube Video Expiry Days left: %s" % days.days
                    if days.days <= 0:
                        user_clear_vids = delete.get(str(video.uploader.id), [])
                        user_clear_vids.append(video)
                        clear_video [str(video.uploader.id)] = user_clear_vids

        print "List of videos to notify users about expiration:"
        print notify
        print "List of videos to notify users about deletion:"
        print delete

        # Send actual notifications here

        print "Expiring videos"
        # For expiring videos
        for user, vids in notify.items():
            print user, vids
            uploader = User.objects.get(id=int(user))
            expired_video_names = "".join([v.title + "\n\t" for v in vids])
            print "Expired video names:"
            print expired_video_names

            message = \
"""
Hello %(username)s,

    You have videos expiring today, %(now)s!

        %(expired_video_names)s

    Other users and you won't be able to view the video from %(site)s, but you will have an option to download it or send it to Youtube for another 30 days.
    However after the 30 days it will be deleted from the site.


Thanks,

-------------------------
%(site)s Team
""" % \
            ({'expired_video_names': expired_video_names,
              'now': datetime.date.today().isoformat(),
              'username': uploader.username,
              'site': SITE,})

            send_mail('%s: Video Expired' % SITE,
                      message,
                      "no-reply@%s.com" % SITE.lower(),
                      [uploader.email], fail_silently=False)
            print 'Email Sent to username: %s! %s has expired.' % (uploader, expired_video_names)

        print "Videos to delete"
        # For videos to delete
        for user2, vids2 in delete.items():
            print user2, vids2
            deletable_video_names = "".join([v.title + "\n\t" for v in vids2])
            print "Deletable video names:"
            print deletable_video_names
            # Delete the actual videos
            print vids2
            for v in vids2:
                v.delete()
                print "Permanently deleted: ", v.title

            #Handles old videos with uploader are set to None
            if not user2 == 'None':
                uploader = User.objects.get(id=int(user2))
                message = \
"""
Hello %(username)s,

    The following videos have been deleted from %(site)s today, %(now)s!

        %(deletable_video_names)s

    The video has already exceeded the allowable time it would be stored on %(site)s.com, for further info contact us.


Thanks,

-------------------------
%(site)s Team
""" % \
            ({'username': uploader.username,
              'now': datetime.date.today().isoformat(),
              'deletable_video_names': deletable_video_names,
              'site': SITE,})

                send_mail('%s: Video Deleted' % SITE,
                      message,
                      "no-reply@%s.com" % SITE.lower(),
                      [uploader.email], fail_silently=False)

        for user3, vids3 in clear_video.items():
            print user3, vids3
            uploader = User.objects.get(id=int(user3))
            clear_videoupload_names = "".join([vid3.title + "\n\t" for vid3 in vids3])
            print "Deletable video files:"
            print clear_videoupload_names
            # Delete the videofiles
            for vid3 in vids3:
                try:
                    delete_videoupload(vid3)
                    vid3.youtube_video_expiry_date = None
                    vid3.save()
                    print "Deleted video file on server: ", vid3.title
                    message = \
"""
Hello %(username)s,

    The following videos have been deleted from %(site)s today, %(now)s!

        %(deletable_video_names)s

    The video has already exceeded the allowable time it would be stored on %(site)s.com, for further info contact us.


Thanks,

-------------------------
%(site)s Team
""" % \
                    ({'username': uploader.username,
                      'now': datetime.date.today().isoformat(),
                      'deletable_video_names': clear_videoupload_names,
                      'site': SITE,})

                    send_mail('%s: Video Deleted' % SITE,
                              message,
                              "no-reply@%s.com" % SITE.lower(),
                              [uploader.email], fail_silently=False)
                    print "Email Sent to %s" % uploader.username
                except os.error:
                    print 'ERROR when deleting'


def is_deletable(video):
    # Check whether the file should be deleted or not
    today = datetime.date.today()
    deletion_date = video.created + datetime.timedelta(days=DAYS_BEFORE_DELETION)
    days = deletion_date.date() - today
    print days.days
    if days.days <= 0:
        return True
    return False


def delete_video(video):
    if is_deletable(video):
        if video.uploader:
            message = \
"""
Hello,
    Your video, %(video)s, has already been deleted today(%(deletion_date)s) on %(site)s.
    The video has already exceeded the allowable time it would be stored on %(site)s.com, for further info contact us.

Thanks,

-------------------------
%(site)s Team
""" \
            % ({'video': video.title,
                'deletion_date': deletion_date.date(),
                'site': SITE,})
            send_mail('%s: Video Deleted' % SITE,
                      message,
                      "no-reply@%s.com" % SITE.lower(),
                      [video.uploader.email], fail_silently=False)
        print "DELETED! %s" % video
        video.delete()


### TESTING TOOLS

def make_videos_expire(videos):
    """
        This method is a tool for testing the video_daily_tasks management command.
        It accepts a list/queryset of videos and forces them to expire, which you
        can then use as test data to the code above.

        Note that for a video to expire, the following conditions must be met:
        (1) user is using a free account
        (2) video has not been marked as expired before (expired=False),
            this means the user has not been modified yet
        (3) time of video creation is at least DAYS_BEFORE_EXPIRATION days ago from today
    """

    for v in videos:
        force_creation_date = datetime.datetime.now() - datetime.timedelta(days=DAYS_BEFORE_EXPIRATION)
        v.created = force_creation_date
        # reload expiration date if set already
        v.expiry_date = None
        v.expired = False
        v.save()


def make_videos_deletable(videos):
    """
        Conditions for the video to be deletable:
        (1) must have expired already
        (2) must have exceeded maximum allowable stay as defined by DAYS_BEFORE_DELETION
    """

    for v in videos:
        delta = datetime.datetime.now() - v.created
        print "Delta: ", delta.days
        if delta.days >= DAYS_BEFORE_DELETION:
            pass
        else:
            force_creation_date = v.created - datetime.timedelta(days=DAYS_BEFORE_DELETION-delta.days)
            print "New creation date: ", force_creation_date
            v.created = force_creation_date
            v.expired = True
            v.save()
