import datetime
import os
from random import choice

from django.conf import settings
from django.contrib.auth.models import User
from django.core.management.base import BaseCommand, CommandError

from accounts.models import AccountLevel, UserProfile
from amazon.utils import get_from_s3, send_to_s3
from videos.forms import generate_slug
from videos.models import Video, VideoOutline, VideoOutlinePin
from videos.utils import enqueue


# Created videos follow this chart:
#
# 1: Uploaded now, created now. Normal.
# 2: "Created" 2 days ago, expired yesterday.
# 3: Expires in 4 hours.
# 4: Expires in 3 days.
# 5: "Created" yesterday.
# 6: "Created" a week previous.
# 7: Public.
# 8: Public. "Created" 2 days ago, expired yesterday.
# 9: Outlined.
# 10: Transferred to YouTube.
#
# Only the free account has expired videos


class Command(BaseCommand):
    args = '<username username ...>'
    help = "Creates free and paid test accounts."

    def handle(self, *args, **options):
        if len(args) > 2:
            raise CommandError('Too many arguments given. Provide either one or two usernames.')
        
        elif len(args) == 2:
            try:
                free_user = User(username=args[0], first_name=args[0] + '_free', last_name='User', email='fakeuser@emails.com')
                paid_user = User(username=args[1], first_name=args[1] + '_paid', last_name='User', email='fakeuser@emails.com')
                free_user.save()
                paid_user.save()
                self.stdout.write('Successfully created fake users using supplied usernames.\n')
            except:
                raise CommandError("Could not create users with supplied usernames. Try different usernames.")

        elif len(args) == 1:
            try:
                free_user = User(username=args[0] + '_free', first_name=args[0] + '_free', last_name='User', email='fakeuser@emails.com')
                paid_user = User(username=args[0] + '_paid', first_name=args[0] + '_paid', last_name='User', email='fakeuser@emails.com')
                free_user.save()
                paid_user.save()
                self.stdout.write('Successfully created fake users using the supplied username + suffixes "_free" and "_paid".\n')
            except:
                raise CommandError("Could not create users with supplied username. Try a different username.")

        elif len(args) == 0:
            try:
                free_user = User(username='fakeuser_free', first_name='fakeuser_free', last_name='User', email='fakeuser@emails.com')
                paid_user = User(username='fakeuser_paid', first_name='fakeuser_paid', last_name='User', email='fakeuser@emails.com')
                free_user.save()
                paid_user.save()
                self.stdout.write('Successfully created fake users using default usernames.\n')
            except:
                raise CommandError("Fake users already exist. Create differently named users using 'manage.py create_test_accounts <username>'")

        free_user.set_password('password')
        paid_user.set_password('password')
        free_user.save()
        paid_user.save()

        # Set paid_user account 
        paid_level = AccountLevel.objects.get(level='Paid')
        paid_user.userprofile.account_level = paid_level
        paid_user.userprofile.save()
        
        # Create videos for free users
        for u in (free_user, paid_user):
            created_videos = ()
            
            for i in range(10):
                temp_slug = generate_slug(5)
                temp_vid = Video(uploader=u, title="Test Fake Uploads Video " + str(i), slug=temp_slug)
                temp_vid.save()
                created_videos = created_videos + (temp_vid,)
    
            #### Videos

            #Normal
            temp_vid = created_videos[0]
            temp_vid.title = "Normal private video"
            temp_vid.description = "Normal private video."
            temp_vid.save()
            
            upload_to_s3(temp_vid.slug)

            ### Expired videos, for free user
            temp_vid = created_videos[1]
            if not u == free_user:
                upload_to_s3(temp_vid.slug)
            temp_vid = created_videos[2]
            upload_to_s3(temp_vid.slug)
            temp_vid = created_videos[3]
            upload_to_s3(temp_vid.slug)

            #Expired Yesterday, created 2 days ago
            temp_vid = created_videos[1]
            temp_vid.title = "Expired the day previous to creation"
            temp_vid.description = "Expired the day previous to creation."
            temp_vid.created = datetime.datetime.now() - datetime.timedelta(days=2)
            temp_vid.save()
            if u == free_user:
                temp_vid.expired = True
                temp_vid.expiry_date = datetime.datetime.now() - datetime.timedelta(days=1)
                temp_vid.save()
                temp_vid.delete()

            #Expires Today in 4 hours
            temp_vid = created_videos[2]
            temp_vid.title = "Expires 4 hours from creation"
            temp_vid.description = "Expires 4 hours from creation."
            if u == free_user:
                temp_vid.expiry_date = datetime.datetime.now() + datetime.timedelta(hours=4)
            temp_vid.save()

            #Expires in 3 days
            temp_vid = created_videos[3]
            temp_vid.title = "Expires 3 days from creation"
            temp_vid.description = "Expires 3 days from creation."
            if u == free_user:
                temp_vid.expiry_date = datetime.datetime.now() + datetime.timedelta(days=3)
            temp_vid.save()

            #Created Yesterday
            temp_vid = created_videos[4]
            upload_to_s3(temp_vid.slug)
            temp_vid.created = datetime.datetime.now() - datetime.timedelta(days=1)
            temp_vid.title = "Time-stamped as created the day previous to actual creation"
            temp_vid.description = "Time-stamped as created the day previous to actual creation."
            temp_vid.save()

            #Created A Week Previous
            temp_vid = created_videos[5]
            upload_to_s3(temp_vid.slug)
            temp_vid.created = datetime.datetime.now() - datetime.timedelta(days=7)
            temp_vid.title = "Time-stamped as created a week previous to actual creation"
            temp_vid.description = "Time-stamped as created a week previous to actual creation."
            temp_vid.save()

            #Public
            temp_vid = created_videos[6]
            upload_to_s3(temp_vid.slug)
            temp_vid.is_public = True
            temp_vid.title = "Public."
            temp_vid.description = "Public."
            temp_vid.save()

            #Public. Expired Yesterday, created 2 days ago
            temp_vid = created_videos[7]
            upload_to_s3(temp_vid.slug)
            temp_vid.title = "Public. Expired the day previous to creation"
            temp_vid.description = "Public. Expired the day previous to creation."
            temp_vid.is_public = True
            temp_vid.created = datetime.datetime.now() - datetime.timedelta(days=2)
            if u == free_user:
                temp_vid.expired = True
                temp_vid.expiry_date = datetime.datetime.now() - datetime.timedelta(days=1)
            temp_vid.save()

            #Outlined
            temp_vid = created_videos[8]
            upload_to_s3(temp_vid.slug)
            temp_vid.title = "Outlined"
            temp_vid.description = "Outlined."
            temp_vid.save()
            outline = VideoOutline(video=temp_vid)
            outline.save()
            outline_pin = VideoOutlinePin(video_outline=outline, text="Start")
            outline_pin.save()

            #Transferred to Youtube, created 2 days ago, deleted from server yesterday
            temp_vid = created_videos[9]
            upload_to_s3(temp_vid.slug)
            temp_vid.youtube_embed_url= "http://www.youtube.com/embed/yc5W-AClSlI"
            temp_vid.created = datetime.datetime.now() - datetime.timedelta(days=2)
            temp_vid.youtube_video_expiry_date = datetime.datetime.now() - datetime.timedelta(days=1)
            temp_vid.title = "YouTube video"
            temp_vid.description = "YouTube video."
            temp_vid.save()

        self.stdout.write('Successfully created videos!\n')

video_list = [
    'GXX2FICF_265_1327659950.mp4',
    'GYB0B07V_272_1328503043.mp4',
    'GYBN3SYP_272_1328541479.mp4',
    'GYFUFWU7_276_1328795515.mp4',
    'GYG2RQTO_282_1328809462.mp4',
    'GYG2Y4ZS_280_1328809761.mp4',
    'GYGHPNXU_280_1328834579.mp4',
    'GYHJD9PM_285_1328897761.mp4',
    'GYHV3XFP_286_1328917448.mp4',
    'GYIOE56Z_281_1328974954.mp4',
]

def upload_to_s3(key):
    temp_vid_slug = key + '_tmp'
    if os.path.isdir(settings.LIVE_VIDEOS_PATH):
        send_to_s3(temp_vid_slug, os.path.join(settings.LIVE_VIDEOS_PATH, choice(video_list)))
    else:
        send_to_s3(temp_vid_slug, settings.FILE_LOCATION)
    enqueue(temp_vid_slug)


def check_from_s3(key):
    video = get_from_s3(key)
    if video:
        print("Video is in s3.")
    else:
        print("Video is not in s3.")
