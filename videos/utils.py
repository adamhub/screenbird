import os
import time
import datetime
import subprocess
import re
import boto
import pika
from decimal import Decimal
from itertools import chain
from urlparse import urlparse
from uuid import uuid1

from django.conf import settings
from django.contrib.auth.models import User
from django.contrib.sites.models import Site
from django.core import urlresolvers
from django.core.urlresolvers import reverse
from django.db.models import Q

from accounts.models import create_anonymous_user
from videos.models import Video, VideoOutline


DAYS_BEFORE_EXPIRATION = getattr(settings, 'VIDEOS_DAYS_BEFORE_EXPIRATION', 60)
DAYS_BEFORE_DELETION = getattr(settings, 'VIDEOS_DAYS_BEFORE_DELETION', 90)
SLUG_LENGTH = 7 


def get_or_create_anon_user(req):
    """
    Gets or creates the anonymous user associated with the `an_tok` of the 
    request. Generates the `an_tok` if none found.
    
    """
    if 'an_tok' in req.COOKIES:
        anonymous_token = req.COOKIES['an_tok']
    else:
        anonymous_token = str(uuid1())[0:25]
        new_an_tok = True
    user = get_or_create_anon_user_from_tok(anonymous_token)
    return (user, anonymous_token)


def get_or_create_anon_user_from_tok(an_tok):
    """
    Gets or creates the anonymous user associated with the `an_tok` of the 
    request.
    
    """
    try:
        user = User.objects.get(username=an_tok)
    except User.DoesNotExist:
        user = create_anonymous_user(an_tok)
    return user


def generate_back_url(url, request):
    default_url = reverse('videos')
    parsed = urlparse(url)
    if parsed.netloc == request.META['HTTP_HOST']:
        try:
            urlresolvers.resolve(parsed.path)
        except urlresolvers.Resolver404:
            back_url = default_url
        else:
            back_url = url
    else:
        back_url = default_url
    return back_url


def get_video_length(video):
    # If duration is not yet set, fetch the video in s3
    if float(str(video.video_duration)) == 0.0:
        s3 = boto.connect_s3(settings.AWS_ACCESS_KEY_ID, 
                             settings.AWS_SECRET_ACCESS_KEY)
        bucket = s3.get_bucket(settings.AWS_VIDEO_BUCKET_NAME)

        # If the video is still encoding then get tmp file
        key = bucket.get_key("%s_tmp" % video.slug)
        tmp_path = os.path.join(settings.MEDIA_ROOT, "tmp", 
                                "%s.mp4" % video.slug)
        # Else, get the encoded file
        if not key:
            key = bucket.get_key(video.slug)
        try:
            key.get_contents_to_filename(tmp_path)
        except:
            return video.video_duration
        # Get the duration using ffmpeg
        result = subprocess.Popen(['/usr/local/bin/ffmpeg', '-i', tmp_path], 
                                  stdout=subprocess.PIPE, 
                                  stderr=subprocess.STDOUT)
        info = [x for x in result.stdout.readlines() if "Duration" in x]
        if len(info) > 0:
            duration = (info[0].split(',')[0].split('  Duration: ')[1])
            hours, minutes, seconds = [float(x) for x in duration.split(':')]
            x = datetime.timedelta(hours=hours, minutes=minutes, 
                                   seconds=seconds)
            video.video_duration = Decimal(str(x.seconds / 60.0))
            video.save()
        try:
            os.remove(tmp_path)
        except:
            pass
    return video.video_duration


def is_expired(video):
    """
    Checks and save if the video is expired
    
    """
    account_level = video.get_account_level()

    video_status = False
    # If video is expiring and is not yet transferred to YouTube
    # Compute expiration date based on date created and 
    # `DAYS_BEFORE_EXPIRATION` setting
    if account_level.video_validity and not video.youtube_embed_url:
        today = datetime.date.today()
        if not video.expiry_date:
            expiry_date = (video.created + 
                           datetime.timedelta(days=DAYS_BEFORE_EXPIRATION))
            video.expiry_date = expiry_date
            video.save()
        expired = days_left(video)
        if expired <= 0:
            video_status = True
            if not video.expired:
                video.expired = True
                video.save()
    # If video has no expiration or is already transferred to YouTube
    # Clear expiration date
    if not account_level.video_validity or video.youtube_embed_url:
        video.expired = False
        video.expiry_date = None
        video.save()
    return video_status

def is_trial_expired(video):
    """
    Checks and save if the video is expired
    
    """
    account_level = video.get_account_level()

    video_status = False
    if account_level.video_validity and not video.youtube_embed_url:
        today = datetime.date.today()
        if not video.expiry_date:
            expiry_date = today + datetime.timedelta(days=DAYS_BEFORE_EXPIRATION)
            video.expiry_date = expiry_date
            video.save()
        expired = days_left(video)
        if expired <= 0:
            video_status = True
            if not video.expired:
                video.expired = True
                video.save()
    if not account_level.video_validity or video.youtube_embed_url:
        video.expired = False
        video.expiry_date = None
        video.save()
    return video_status

def days_left(video):
    today = datetime.date.today()
    expiry = video.expiry_date
    days = expiry.date() - today
    return days.days

def add_total_upload_time(current_duration, total_upload_time):
    total = current_duration + total_upload_time
    return total

def enqueue(slug):
    """
    Sends video to RabbitMQ for encoding.
    
    """
    connection = pika.BlockingConnection(
            pika.ConnectionParameters(settings.RABBITMQ_SERVER))
    channel = connection.channel()
    channel.queue_declare(queue=settings.QUEUE_NAME, durable=True)
    channel.basic_publish(exchange='',
            routing_key=settings.QUEUE_NAME,
            body="('%s', '%s')" % (slug, Site.objects.get_current().domain))
    connection.close()

def enqueue_cocreate(cocreate):
    """
    enqueue task to compile cocreate on rabbitmq
    """
    body = {}
    body['slug'] = cocreate.output_video.slug
    body['sections'] = cocreate.available_videos
    body['host'] = Site.objects.get_current().domain

    connection = pika.BlockingConnection(pika.ConnectionParameters('screenbird.com'))
    channel = connection.channel()
    channel.queue_declare(queue=settings.COCREATE_QUEUE_NAME, durable=True)
    channel.basic_publish(exchange='',
                      routing_key=settings.COCREATE_QUEUE_NAME,
                      body=repr(body))
    connection.close()
    
def init_cocreate(cocreate, generate_slug):
    if len(cocreate.videos) <= 0:
        return
    
    if not cocreate.output_video:
        slug = generate_slug(SLUG_LENGTH)
        video = Video(title=cocreate.title, slug=slug, description=cocreate.description, uploader=cocreate.owner)
        video.save()
        cocreate.output_video = video
        cocreate.save()
    else:
        # reset video status to encoding
        video_status = cocreate.output_video.get_video_status()
        video_status.set_to_encoding()

    #create outline from the sections
    VideoOutline.objects.filter(video=cocreate.output_video).delete()
    outline = VideoOutline.objects.create(video=cocreate.output_video)
    asections = cocreate.available_sections
    for i in xrange(len(asections)):
        outline.videooutlinepin_set.create(text=asections[i],
                                           current_time=Decimal(str(i)))

    # enqueue on cocreate task queue
    enqueue_cocreate(cocreate)

def get_video_duration(fname):
    process = subprocess.Popen(['ffmpeg',  '-i', fname], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    stdout, stderr = process.communicate()
    matches = re.search(r"Duration:\s{1}(?P<hours>\d+?):(?P<minutes>\d+?):(?P<seconds>\d+\.\d+?),", stdout, re.DOTALL).groupdict()
    h = matches['hours']
    m = matches['minutes']
    s = matches['seconds']
    return h,m,s

def get_videos(user=None, user_id=None, username=None, show_private=False,
               include_channels=False, channel_only=False):
    """
        returns all videos or videos by_user
        args may include:
            username - return user videos
            user_id - return user videos
            user - user object
            show_private - boolean includes private videos
            include_channels - boolean returns user vids including channel vids
            channel_only - boolean return ONLY channel videos for user
    """

    if not user and user_id:
        user = User.objects.get(id=user_id)
    elif not user and username:
        user = User.objects.get(username=username)

    all_videos = Video.objects.filter(is_public=True, is_active=True,
            expired=False, featured=False).order_by('-created')

    if show_private:
        private_videos = Video.objects.filter(is_active=True, is_public=False, 
                uploader=user, expired=False, featured=False).order_by('-created')
        all_videos = all_videos | private_videos

    if not channel_only and not include_channels:
        all_videos = all_videos.filter(channel__isnull=True)

    if user:
        if include_channels:
            user_channels = user.userprofile.channels.all().values_list('id',flat=True).distinct()
            user_videos = all_videos.filter(Q(uploader=user) | Q(channel__id__in=user_channels)).order_by('-created')
        elif channel_only:
            user_channels = user.userprofile.channels.all().values_list('id',flat=True).distinct()
            user_videos = all_videos.filter(Q(channel__id__in=user_channels)).exclude(Q(uploader=user)).order_by('-created')        
        else:        
            user_videos = all_videos.filter(Q(uploader=user)).order_by('-created')
        return user_videos
    else:
        return all_videos

def user_is_mobile(user_agent):
    ua = user_agent.lower()
    return ua.find('iphone')>0 or ua.find('ipad')>0 or ua.find('android')>0

def remove_list_duplicates(seq):
    seen = set()
    seen_add = seen.add
    return [ x for x in seq if x not in seen and not seen_add(x)]
