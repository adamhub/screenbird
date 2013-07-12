import base64
import boto
import datetime
import gdata
import gdata.youtube
import gdata.youtube.service
from gdata.service import TokenUpgradeFailed
import hmac
import logging
import mimetypes
import os
import pika
import random
import re
import simplejson
import subprocess
import time
import urllib2
from atom import ExtensionElement
from decimal import Decimal
from gdata.media import YOUTUBE_NAMESPACE
from hashlib import sha1
from uuid import uuid1

from django.conf import settings
from django.contrib import messages
from django.contrib.auth import authenticate, login, logout
from django.contrib.auth.decorators import login_required
from django.contrib.auth.models import User
from django.contrib.sites.models import Site
from django.core.files import File
from django.core.mail import EmailMultiAlternatives, send_mail
from django.core.paginator import Paginator, InvalidPage, EmptyPage
from django.core.serializers import serialize
from django.core.urlresolvers import reverse
from django.db.models import Q
from django.http import (
    Http404,
    HttpResponse,
    HttpResponseBadRequest,
    HttpResponseRedirect, 
    HttpResponseServerError, 
)
from django.middleware.csrf import get_token
from django.shortcuts import (
    get_object_or_404, 
    redirect,
    render,
    render_to_response, 
)
from django.template import RequestContext, loader, Context
from django.template.loader import render_to_string
from django.utils.encoding import smart_str
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_POST

from persistent_messages.models import Message
from social_auth.models import UserSocialAuth

from accounts.models import AccountLevel, UserProfile, create_anonymous_user
from accounts.util import set_cookie_helper
from amazon.s3 import Connection
from amazon.utils import send_to_s3, add_to_upload_queue
from videos.forms import (
    ChannelForm,
    ChannelOptionForm, 
    CoCreateForm, 
    InvitationForm, 
    ManualUploadForm, 
    SectionForm,
    VideoEditForm, 
    VideoUploadForm, 
    YoutubeUploadForm, 
    generate_random_string,
    generate_slug, 
    reserve_slug,
)
from videos.models import (
    DAYS_BEFORE_EXPIRATION,
    Channel,
    ChannelOption,
    CoCreate,
    ConfirmationInfo,
    FeaturedVideo,
    PopUpInformation,
    ReservedSlug,
    Section,
    Video,
    VideoOutline,
    VideoOutlinePin,
    VideoStatus,
    kill_anonymous_account,
)
from videos.utils import (
    days_left, 
    enqueue, 
    enqueue_cocreate,
    generate_back_url, 
    get_or_create_anon_user,
    get_or_create_anon_user_from_tok,
    get_video_length, 
    get_videos,
    init_cocreate,
    is_expired,
    remove_list_duplicates,
    user_is_mobile,
)

logger = logging.getLogger('videos.views')


PER_PAGE = 50
PROFANITY_LIST = settings.PROFANITY_LIST
SLUG_LENGTH = getattr(settings, 'VIDEO_SLUG_LENGTH', 7)
CHANNEL_SLUG_LENGTH = getattr(settings, 'CHANNEL_SLUG_LENGTH', 7)
# Maximum allowable video length to be uploaded on youtube
YOUTUBE_MAX_LIMIT = 15 
# Number of days until the video would be deleted after YouTube upload
YOUTUBE_VIDEO_DELETION_TIMESPAN = getattr(settings, 
                                          'YOUTUBE_VIDEO_DELETION_TIMESPAN', 7)
SECONDS = 60


def GetAuthSubUrl(request,slug):
    """
    Get URL for YouTube authentication.
    
    """
    next = request.build_absolute_uri()
    scope = 'http://gdata.youtube.com'
    if settings.SECURE_KEY:
        secure = True
    else:
        secure = False
    session = True

    yt_service = gdata.youtube.service.YouTubeService()
    return yt_service.GenerateAuthSubURL(next, scope, secure, session)


def video_list(request):
    """
    Display a list of featured videos (from all users) and current user's 
    uploads. 
    Channel videos are listed together with the normal uploaded videos.
    
    """
    user = request.user
    new_an_tok = False
    if request.user.is_anonymous():
        user, anonymous_token = get_or_create_anon_user(request)

    # Private, public, and channel videos uploaded by user
    all_videos = get_videos(show_private=True, include_channels=True) #, user=user)
    videos_count = all_videos.count()
    paginator = Paginator(all_videos, PER_PAGE)

    try:
        page = int(request.GET.get('page', '1'))
    except ValueError:
        page = 1
    
    featured = []
    try:
        latest_submissions_page = paginator.page(page)
        if page == 1:
            # Display featured videos on first page only
            featured = FeaturedVideo.objects.all()
    except (EmptyPage, InvalidPage):
        # If page request is out of range, deliver last page of results.
        latest_submissions_page = paginator.page(paginator.num_pages)
        if paginator.num_pages == 1:
            featured = FeaturedVideo.objects.all()

    next_page = latest_submissions_page.next_page_number()
    prev_page = latest_submissions_page.previous_page_number()
    context = {
        'latest_submissions_page': latest_submissions_page,
        'paginator': paginator,
        'next_page': next_page,
        'prev_page': prev_page,
        'videos_count': videos_count,
        'featured': featured
    }
    response = render(request, 'video_list.html', context)
    if new_an_tok:
        set_cookie_helper(response, 'an_tok', anonymous_token, 365)
    return response


def user_videos(request, username):
    """
    Display a list of featured and normal videos uploaded by user identified by
    `username` argument.
    Channel videos are listed separately from the normal uploaded videos.

    """
    user = get_object_or_404(User, username=username)

    user_channels = user.userprofile.channels.all().values_list('id',flat=True).distinct()
    if user == request.user:
        user_videos = get_videos(user=user, show_private=True)
        channel_videos = get_videos(user=user, show_private=True, 
                                    channel_only=True)
    else:
        user_videos = get_videos(user=user, show_private=False)
        channel_videos = get_videos(user=user, show_private=False, 
                                    channel_only=True)
    user_videos_count = user_videos.count()
    channel_videos_count = channel_videos.count()
    paginator = Paginator(user_videos, PER_PAGE)

    try:
        page = int(request.GET.get('page', '1'))
    except ValueError:
        page = 1

    featured = []
    try:
        latest_submissions_page = paginator.page(page)
        if page == 1:
            # Display featured videos on first page only
            featured = FeaturedVideo.objects.all().filter(video__uploader=user)
    except (EmptyPage, InvalidPage):
        # If page request is out of range, deliver last page of results.
        latest_submissions_page = paginator.page(paginator.num_pages)
        if paginator.num_pages == 1:
            featured = FeaturedVideo.objects.all().filter(video__uploader=user)

    next_page = latest_submissions_page.next_page_number()
    prev_page = latest_submissions_page.previous_page_number()

    context = {
        'latest_submissions_page': latest_submissions_page,
        'paginator': paginator,
        'next_page': next_page,
        'prev_page': prev_page,
        'videos_count': user_videos_count,
        'featured': featured,
        'channel_videos':channel_videos,
        'channel_videos_count':channel_videos_count
    }
    return render(request, 'user_videos.html', context)


def view(request, slug, yt_processing=False,
                        template='video_watch.html',
                        mimetype='text/html', owner_id=None):
    """
    Returns video information of video to be played on video player
    on `template`.

    """
    if owner_id:
        back_url = reverse('video-page-embed-code', kwargs={'user_id': owner_id})
    else:
        back_url = generate_back_url(request.META.get('HTTP_REFERER', reverse('videos')), request)
    has_referer = request.META.get('HTTP_REFERER', None)

    embed_code = ""
    # Width and height for embeddable video.
    width = request.GET.get('width', 640)
    height = request.GET.get('height', 360)
    # SET some default values
    video = None
    video_filename = ''
    video_description = ''
    description_hidden = False
    is_fresh = False
    channels = []
    vikia = None

    # Get all viewable videos
    videos = Video.objects.viewable_by(request)
    try:
        # Check if video is in the viewable videos
        video = videos.get(slug=slug)

        # We needed to register the video to a section here as java_upload
        # has no access to the session and cookies of the browser.
        if not has_referer or '?section=' in has_referer:
            register_to_section(request, video)
            try:
                section = video.section
            except Section.DoesNotExist:
                pass
            else:
                return redirect(reverse('cocreate_section', args=(section.cocreate.pk, section.order)))

        # Check if video is not expired
        if not video.expired:
            # Check if video is playable (ie. is it being encoded?)
            # Get the video status
            try:
                video_status_obj = VideoStatus.objects.get_video_status(slug, request)
            except VideoStatus.DoesNotExist:
                raise Http404

            # Check if video is viewed on web or mobile
            # If on web, check if web version encoded video is available
            # If on mobile, check if mobile version encoded video is available
            user_agent = getattr(request.META, 'HTTP_USER_AGENT', '')
            if (('android' in user_agent.lower() and video_status_obj.mobile_available) or
                ('android' not in user_agent.lower() and video_status_obj.web_available)):

                video_filename = video.title
                video_status = 'OK'
                embed_code = video.get_embed_code()
                video_description = video.description
                video_desc_list = video_description.lower().split()
                description_hidden = False

                for bad_word in PROFANITY_LIST:
                    if bad_word in video_desc_list:
                        description_hidden = True
                        break

                # Check if a video is fresh. A video is fresh if it is 
                # less than a day old. 
                # TODO Make this a setting?
                is_fresh = not video.expired and (datetime.datetime.now() - 
                               video.created < datetime.timedelta(days=1))

                if request.user.is_authenticated():
                    # Channels, ordered randomly for display
                    owned_channels = request.user.channel_set.all()
                    part_channels = request.user.userprofile.channels.all()
                    user_channels = (owned_channels | part_channels).distinct()

                    channels = sorted(list(user_channels), 
                                           key=lambda x: random.randint(1, 1000))

                # Check if there are request.GET parameters video=vikia
                vikia = request.GET.get("v", None)

                # Get the outline for this video,
                outline, created = VideoOutline.objects.get_or_create(video=video)
                outline_points = outline.videooutlinepin_set.all()
            else:
                video_filename = 'Video Processing...'
                video_status = 'WAIT'
        else:
            video_status = 'EXPIRED'
    except Video.DoesNotExist: # The video is deleted, does not exist.
        try:
            video_status_obj = VideoStatus.objects.get_video_status(slug, request)
        except VideoStatus.DoesNotExist:
            video_filename = 'Video does not exist'
            video_status = 'DNE'
        else:
            video_filename = 'Video removed'
            video_status = 'DELETED'
    # This is to check if an anonymous user is the owner of a video
    anonymous_user = None
    if 'an_tok' in request.COOKIES:
        anonymous_token = request.COOKIES['an_tok']
        try:
            anonymous_user = User.objects.get(username=anonymous_token)
        except User.DoesNotExist:
            pass
    
    # Notify user about YouTube video expiration
    notify = request.GET.get('notify', False)
    notify_user = notify and video.youtube_video_expiry_date > datetime.datetime.now()
    context = {
        'video': video,
        'anonymous_user': anonymous_user,
        'video_filename': video_filename,
        'video_status': video_status,
        'video_description': video_description,
        'description_hidden': description_hidden,
        'notify_user': notify_user,
        'yt_processing': yt_processing,
        'back_url': back_url,
        'embed_code': embed_code,
        'site': Site.objects.get_current(),
        'width': width,
        'height': height,
        'is_fresh': is_fresh,
        'is_local': not settings.PUSH_TO_S3,
        'channels': channels,
        'vikia': vikia,
    }
    return render_to_response(template, context,
                              context_instance=RequestContext(request),
                              mimetype=mimetype)


def video_content(request, video_id):
    """
    Returns url of video from s3 identified by `video_id`.
    
    Raises `Http404` if video is not viewable by current user or is not 
    present in the database.
    
    """
    try:
        video = Video.objects.viewable_by(request).get(id=video_id)
    except Video.DoesNotExist:
        raise Http404

    status = get_object_or_404(VideoStatus, video_slug=video.slug)
    user_agent = getattr(request.META, 'HTTP_USER_AGENT', '')
    filename = ''
    if 'android' in user_agent.lower() and status.mobile_available:
        slug = '%s__mobile' % video.slug
    else:
        slug = video.slug

    if settings.PUSH_TO_S3:
        try:
            conn = Connection(is_secure=True)
            url = conn.generate_object_expiring_url(slug)
            return redirect(url)
        except Exception,e:
            pass
    else:
        filename = os.path.join(settings.MEDIA_ROOT,'tmp/%s.mp4' % slug)
        return redirect(filename)
    raise Http404


def download(request, slug):
    """
    Downloads the video identified by `slug` from s3
    
    Raises `Http404` if video is not viewable by current user or is not 
    present in the database.
    
    """
    try:
        video = Video.objects.viewable_by(request).get(slug=slug)
    except Video.DoesNotExist:
        raise Http404 
    
    status = get_object_or_404(VideoStatus, video_slug=video.slug)
    if status.is_encoding:
        # If the video is still encoding then get tmp file
        s3 = boto.connect_s3(settings.AWS_ACCESS_KEY_ID, settings.AWS_SECRET_ACCESS_KEY)
        bucket = s3.get_bucket(settings.AWS_VIDEO_BUCKET_NAME)
        key = bucket.get_key("%s_tmp" % video.slug)
    else:
        # Else, get the encoded file
        key = video.slug
    try:
        conn = Connection(is_secure=True)
        url = conn.generate_object_expiring_url(key) 
        remotefile = urllib2.urlopen(url)
        content_length = remotefile.info().getheaders("Content-Length")[0]
        response = HttpResponse(remotefile, mimetype='video/mp4')
        response['Content-Disposition'] = 'attachment; filename=%s.mp4' % video.title
        response['Content-Length'] = content_length
        return response
    except Exception,e:
        pass
    raise Http404


def register_to_section(request, video):
    "Registers a video to a section specified on the session"
    section_id = request.session.get('section_id', None)
    if section_id:
        try:
            section = Section.objects.get(pk=section_id)
            section.video = video
            section.save()
            del request.session['section_id']
        except Section.DoesNotExist:
            pass

def my_video_upload(request):
    """
    Handles manual video upload.
    
    Redirects to `my_video_list` when a RabbitMQ connection cannot be
    established.
    
    Form data is submitted via ajax from `manual_video_upload.html` template.
    Video is directly uploaded to s3 from the browser upon from submission.
    
    """
    # When a section id is passed as a query string, the current upload is for
    # a cocreate section
    section_id = request.REQUEST.get('section', None)
    if section_id:
        request.session['section_id'] = section_id

    if request.method == 'POST':
        form_data = simplejson.loads(request.POST.get('form-data'))
        if request.user.is_authenticated():
            user = request.user
        else:
            # Get anonymous user identified by an_tok. Creates one if none found
            if 'an_tok' in request.COOKIES:
                user = get_or_create_anon_user_from_tok(request.COOKIES['an_tok'])
            if not user:
                raise Http404
        form = ManualUploadForm(form_data, user=user)
        
        if form.is_valid():
            # Create a Video object using the form input values
            title = form.cleaned_data['title']
            slug = form.cleaned_data['slug']
            description = form.cleaned_data['description']
            channel = form.cleaned_data['channel']
            is_public = form.cleaned_data['is_public']
            video = Video(title=title, slug=slug, description=description, 
                          is_public=is_public, uploader=user)
            video.channel = channel
            video.save()
            try:
                reserved_slug = ReservedSlug.objects.get(slug=slug)
            except:
                pass
            else:
                reserved_slug.used = True
                reserved_slug.save()

            if video.channel:
                channel_option, created = video.channel.channeloption_set.get_or_create(
                    user_profile=user.userprofile)
                needs_approval = channel_option.needs_approval
                if needs_approval:
                    # Videos of untrusted users are marked active unless taken 
                    # down by admin.
                    video.send_approval_notification()  
            return HttpResponse("OK")
        else:
            return HttpResponse("FAIL")
    else:
        # Set up connection to RabbitMQ server and redirect to my videos page
        # on error.
        try:
            connection = pika.BlockingConnection(
                    pika.ConnectionParameters(settings.RABBITMQ_SERVER))
        except:
            messages.error(request, "We are experiencing a connection problem \
                    and cannot perform uploads for now. Please try again later.")
            return HttpResponseRedirect(reverse('my-videos'))
        max_time = False
        if request.user.is_authenticated():
            user = request.user
        else:
            user = None
            if 'an_tok' in request.COOKIES:
                user = get_or_create_anon_user_from_tok(request.COOKIES['an_tok'])
        if user:
            userprofile = UserProfile.objects.get(user=user)
            total_upload_time = userprofile.total_upload_time
            max_video_length = userprofile.account_level.max_video_length
            if total_upload_time >= max_video_length:
                max_time = True
        # Check first if maximum upload time limit is reached. 
        # Proceed with the upload form if still not over the limit.
        # Else, display a message notifying the user of limit reached.
        if not max_time:
            bucket_name = settings.AWS_VIDEO_BUCKET_NAME
            key = "%s_tmp" % reserve_slug(SLUG_LENGTH)
            access_key_id = settings.AWS_ACCESS_KEY_ID
            expiration_date = time.strftime("%Y-%m-%dT%H:%M:%SZ", 
                                            time.gmtime(time.mktime((datetime.datetime.now() + datetime.timedelta(hours=1)).timetuple())))
            aws_secret_key = settings.AWS_SECRET_ACCESS_KEY
            success_action_redirect = "http://%s%s" % (Site.objects.get_current().domain, reverse('encode-video', args=(key,)))
            policy_document = """   {"expiration":"%s",
                                     "conditions": [
                                            {"bucket":"%s"},
                                            {"acl": "private"},
                                            {"success_action_redirect": "%s"},
                                            {"Content-Type": "video/mp4"},
                                            {"key":"%s"}
                                        ]
                                     }
                                
            """ % (expiration_date, bucket_name, success_action_redirect, key)
            policy = base64.b64encode(policy_document)
            signature = base64.b64encode(hmac.new(aws_secret_key, policy, sha1).digest())
            form = ManualUploadForm(user=user)
            form.fields['slug'].initial = key.replace("_tmp", "")
            
            context = {
                'bucket': bucket_name,
                'key': key,
                'access_key_id': access_key_id,
                'success_action_redirect': success_action_redirect,
                'policy': policy,
                'signature': signature,
                'form': form
            }
        else:
            context = {
                'max_time': max_time,
            }
        response = render(request, "manual_video_upload.html", context)
        if not user:
            set_cookie_helper(response, 'an_tok', str(uuid1())[0:25], 365)
        return response 


@csrf_exempt
def java_upload(request):
    """Current user can upload video"""
    context = {'user_id': False}
    if request.method == "POST":
        user_id = request.POST.get('user_id', None)
        an_tok = request.POST.get('an_tok', None)
        slug = request.POST.get('slug', generate_slug(SLUG_LENGTH))
        if not user_id or user_id == 'None':
            user_id = request.user.id
        if isinstance(request.user, User) or user_id and user_id != 'None':
            user = get_object_or_404(User, pk=user_id)
            form = VideoUploadForm(request.POST, request.FILES)
            if form.is_valid():
                video = form.save(commit=False)
                video.uploader = user
                video.save()
                try:
                    reserved_slug = ReservedSlug.objects.get(video=video.slug)
                except:
                    pass
                else:
                    reserved_slug.used = True
                    reserved_slug.save()

                context['message'] = "Video succesfully uploaded!"
                return HttpResponse("OK", status=200)
            else:
                return HttpResponseBadRequest("invalid form data")
        else:
            user = None
            if user_id and user_id != 'None':
                user = get_object_or_404(User, pk=user_id)
            else:
                if not an_tok:
                    an_tok = str(uuid1())[0:25]
                user = get_or_create_anon_user_from_tok(an_tok)

            if user:
                form = VideoUploadForm(request.POST, request.FILES)
                if form.is_valid():
                    video = form.save(commit=False)
                    video.uploader = user
                    video.save()

                    message = "Video succesfully uploaded!"
                    context['message'] = message
                else:
                    return HttpResponseBadRequest("invalid form data")
                return HttpResponse("OK", status=200)

    form = VideoUploadForm()
    context['form'] = form
    response = render_to_response('video_upload.html', context,
                                  context_instance=RequestContext(request))
    return response


def youtube_url_deleted(request, slug, yt_id):
    """
    Check the youtube status if it's deleted, if so clear the 
    `youtube_embed_url`
    
    Raises `Http404` for videos not found in the database.
    
    """
    video = get_object_or_404(Video, slug=slug)
    youtube_embed_url = video.youtube_embed_url
    yt_service = gdata.youtube.service.YouTubeService()
    try:
        upload_status = yt_service.CheckUploadStatus(video_id=yt_id)
    except:
        video.youtube_embed_url = ""
        video.save()
        return HttpResponseRedirect(reverse('watch-video', args=(slug,)))
    else:
        return HttpResponseRedirect(reverse('youtube-processing', args=(slug,)))


def youtube_processing(request, slug):
    """
    Catch a processing event on youtube and passes a 
    `True` yt_processing argument to `view` method
    
    """
    return view(request, slug, yt_processing=True)


def upload_youtube(request, slug):
    """
    Transfers the video identified by `slug` to YouTube.
    
    """
    video = get_object_or_404(Video, slug=slug)
    context = {'slug': slug}

    if 'token' not in request.GET and request.method != "POST":
        googleUrl = GetAuthSubUrl(request,slug)
        return HttpResponseRedirect(googleUrl)
    if 'token' in request.GET:
        authsub_token = request.GET['token']
        current_url = request.build_absolute_uri()
        context['token'] = authsub_token
        context['yt_url'] = current_url

    if request.method == "POST":
        form = YoutubeUploadForm(request.POST)
        context['form'] = form
        context['token'] = request.POST['token']
        context['yt_url'] = request.POST['yt_url']
        privacy = request.POST['privacy']
        if form.is_valid():
            authsub_token = request.POST['token']
            yt_url = request.POST['yt_url']
            try: 
                if settings.SECURE_KEY:
                    key_file = open(settings.SECURE_KEY)
                    rsa_key = key_file.read()
                    key_file.close()
                    single_use_token = gdata.auth.extract_auth_sub_token_from_url(
                            yt_url,rsa_key=rsa_key)
                    yt_service = gdata.youtube.service.YouTubeService(
                            single_use_token)
                    yt_service.UpgradeToSessionToken(single_use_token)
                else:
                    single_use_token = gdata.auth.extract_auth_sub_token_from_url(
                            yt_url)
                    yt_service = gdata.youtube.service.YouTubeService(
                            single_use_token)
                    yt_service.UpgradeToSessionToken(single_use_token)
            except TokenUpgradeFailed:
                message = "Upload to YouTube failed. Please ensure that screenbird is authorized to access your YouTube account to upload videos"
                messages.error(request, message)
                return redirect(reverse('my-videos'))

            yt_service.debug = True
            yt_service.developer_key = settings.YOUTUBE_DEV_KEY
            title = form.cleaned_data['title']
            description = form.cleaned_data['description']
            category = form.cleaned_data['category']
            default_key = 'screenbird'
            keywords = "%s, %s"%(default_key, form.cleaned_data['keywords'])
            # Prepare a media group object to hold our video's meta-data
            my_media_group = gdata.media.Group(
                title=gdata.media.Title(text=title),
                description=gdata.media.Description(description_type='plain',
                        text=description),
                keywords=gdata.media.Keywords(text=keywords),
                category=[gdata.media.Category(
                        text=category,
                        scheme='http://gdata.youtube.com/schemas/2007/categories.cat',
                        label=category)],
                player=None
            )
            if privacy == "unlisted":
                # Set video as unlisted
                kwargs = {
                    "namespace": YOUTUBE_NAMESPACE,
                    "attributes": {'action': 'list', 'permission': 'denied'},
                }
                extension = ([ExtensionElement('accessControl', **kwargs)])
                # Create the gdata.youtube.YouTubeVideoEntry to be uploaded
                video_entry = gdata.youtube.YouTubeVideoEntry(media=my_media_group, extension_elements=extension)
            else:
                video_entry = gdata.youtube.YouTubeVideoEntry(media=my_media_group)

            # Set the path for the video file binary
            # Fetch video from s3
            s3 = boto.connect_s3(settings.AWS_ACCESS_KEY_ID, settings.AWS_SECRET_ACCESS_KEY)
            bucket = s3.get_bucket(settings.AWS_VIDEO_BUCKET_NAME)
            
            # If the video is still encoding then get tmp file
            key = bucket.get_key("%s_tmp" % slug)
            tmp_path = os.path.join(settings.MEDIA_ROOT, "tmp", "%s.mp4" % slug)
            # Else, get the encoded file
            if not key:
                key = bucket.get_key(slug)
            try:
                key.get_contents_to_filename(tmp_path)
            except Exception, e:
                raise Http404
            video_file_location = tmp_path

            try:
                new_entry = yt_service.InsertVideoEntry(video_entry, 
                                                        video_file_location)
                try:
                    os.remove(tmp_path)
                except:
                    pass
            except gdata.youtube.service.YouTubeError, yt:
                context['status'] = yt.args[0]['status']
                context['body'] = yt.args[0]['body']
                context['reason'] = yt.args[0]['reason']
                logger.debug("yt_service.InsertVideoEntry: %s" % yt)
                return render(request, 'youtube_upload_error.html', context)
            except Exception, e:
                context['status'] = '400'
                context['body'] = 'Connection to YouTube failed'
                context['reason'] = 'Bad Connection'
                logger.debug("yt_service.InsertVideoEntry: %s" % e)
                return render(request, 'youtube_upload_error.html', context)

            swf_url = new_entry.GetSwfUrl()
            if privacy == "private":
                video.youtube_embed_url = swf_url
                video.save()
                new_entry.media.private = gdata.media.Private()
                new_entry = yt_service.UpdateVideoEntry(new_entry)

            upload_status = yt_service.CheckUploadStatus(new_entry)

            if upload_status is not None:
                context['status'] = upload_status[0]
                context['message'] = upload_status[1]
                video.youtube_embed_url = swf_url
                now = datetime.datetime.now()
                video.youtube_video_expiry_date = now + datetime.timedelta(
                        days=YOUTUBE_VIDEO_DELETION_TIMESPAN)
                video.expired = False
                video.save()
                notify_user = True
                url = '%s?notify=%s' % (reverse('watch-video', args=(slug,)), 
                                        notify_user)
                return HttpResponseRedirect(url)
            context['error'] = True
            return render(request, 'youtube_upload_form.html', context)
    else:
        init_description = video.description
        # Update the description with the outlines
        if video.videooutline_set.count() > 0:
            new_description = '\nOutlines:\n\n'
            for outline in video.videooutline_set.all():
                for pin in outline.videooutlinepin_set.all():
                    time = ''
                    hours = ''
                    mins = ''
                    secs = ''
                    if pin.current_time >= 3600:
                        hours = int(pin.current_time) / 3600
                        mins = int(pin.current_time - (hours * 3600)) / 60
                        secs = int(pin.current_time - (hours * 3600) - (mins * 60))
                        if mins < 10:
                            mins = '0%s' % mins
                        if secs < 10:
                            secs = '0%s' % secs
                        time = '%s:%s:%s' % (hours, mins, secs)
                    elif pin.current_time > 60:
                        mins = int(pin.current_time) / 60
                        secs = int(pin.current_time - (mins * 60))
                        if secs < 10:
                            secs = '0%s' % secs
                        time = '%s:%s' % (mins, secs)
                    else:
                        if secs < 10:
                            secs = '0%s' % secs
                        secs = int(pin.current_time)
                        if secs < 10:
                            secs = '0%s' % secs
                        time = '0:%s' % secs
                    new_description += '%s - %s\n' % (pin.text, time)
            init_description += new_description
        context['description'] = init_description
        form = YoutubeUploadForm(initial={'privacy': 'unlisted'})

    # Proceed with the upload form if video to be transferred si within allowed
    # limits.
    # Else display a message notifying of limit reached.
    if video.video_duration <= YOUTUBE_MAX_LIMIT:
        context['form'] = form
    else:
        context['max_duration'] = True
    context['video'] = video
    return render(request, 'youtube_upload_form.html', context)


def my_video_list(request):
    """
    Displays list of videos owned by current uses with options for editing and
    downloading the video.
    
    """
    context={}
    response = None
    delete_an_tok = False
    
    # Check for anonymous token marking user with uncommitted videos
    if 'an_tok' in request.COOKIES and request.COOKIES['an_tok']:
        an_tok = request.COOKIES['an_tok']
        try:
            user = User.objects.get(username=an_tok)
            # Authenticate newly found Anonymous User to check for any funny business
            user = authenticate(username=an_tok, password=an_tok)
            if not user:
                raise User.DoesNotExist()
        except User.DoesNotExist:
            # Render regular default view for non-logged in users
            response = render(request, 'my_videos.html', context)
            # Deleting Anonymous Cookie
            response.delete_cookie('an_tok')
            return response

        if not request.user.is_anonymous():
            # User is has logged in, and is not anonymous
            delete_an_tok =  kill_anonymous_account(request, user, an_tok)
        else: 
            # User is still anonymous, track activity
            request.user = user
            
    if request.user.is_authenticated():
        # Embed code for video page
        embed_code_url = "http://%s%s" % (Site.objects.get_current().domain,
                reverse('video-page-embed-code', args=[request.user.pk]))
        embed_code = "<iframe src='%s' style='width:680px; height:440px;" \
            " overflow-y:hidden;' scrolling='no' frameBorder='0'></iframe>" % embed_code_url

        # Get info messages if invited to channels
        pers_msgs = Message.objects.filter(user=request.user, read=False)
        message = ""
        if request.method == 'POST':
            videos = Video.objects.filter(id__in=request.POST.getlist('item'))
            deleted, for_approval = [], []
            for video in videos:
                name = video.title
                if video.delete() is False:
                    for_approval.append(name)
                else:
                    deleted.append(name)
            if for_approval:
                message = " ".join(["Note that the removal of videos within a",
                        "channel needs the admin's approval first.\n"])
            if deleted:
                message = message + " ".join(["The following video/s have been deleted:",
                                              ", ".join(deleted), "."])
            if for_approval:
                message = message + " ".join(["The following video/s are sent for approval to the admin:",
                                              ", ".join(for_approval), "."])
        
        user_channels = request.user.userprofile.channels.all().values_list(
                'id',flat=True).distinct()
        my_videos = get_videos(user=request.user, show_private=True, 
                include_channels=True)
        my_videos_count = my_videos.count()
        my_videos_list = []
        for vid in my_videos:
            account_level = vid.get_account_level()

            video_dict = {
                'video': vid,
                'account_level': account_level,
                'expired': vid.expired,
            }
            if not vid.expired and vid.expiry_date:
                video_dict['days_left'] = days_left(vid)
            my_videos_list.append(video_dict)
        paginator = Paginator(my_videos_list, PER_PAGE)

        try:
            page = int(request.GET.get('page', '1'))
        except ValueError:
            page = 1
        try:
            latest_submissions_page = paginator.page(page)
        except (EmptyPage, InvalidPage):
            # If page request is out of range, deliver last page of results.
            latest_submissions_page = paginator.page(paginator.num_pages)

        next_page = latest_submissions_page.next_page_number()
        prev_page = latest_submissions_page.previous_page_number()

        owned_channels = request.user.channel_set.all()
        part_channels = request.user.userprofile.channels.all()
        user_channels = (owned_channels | part_channels).distinct()

        context = {
            'pers_msgs': pers_msgs, 
            'message': message, 
            'latest_submissions_page': latest_submissions_page, 
            'paginator': paginator, 
            'next_page': next_page,
            'prev_page':prev_page, 
            'my_videos_count': my_videos_count, 
            'channels': user_channels,
            'embed_code': embed_code,
            'embed_code_url': embed_code_url,
        }
                   
        user_account_level = request.user.userprofile.account_level.level
        # If user account is paid or trial, add recording link to page
        if user_account_level == 'Paid' or user_account_level == 'Trial':
            if request.user.userprofile.recorder_link == "":
                request.user.userprofile.recorder_link = generate_slug(
                        SLUG_LENGTH)
                request.user.userprofile.save()
            context['recording_link'] = "http://%s%s?r=%s" % (
                    Site.objects.get_current().domain, 
                    reverse('record-video'), 
                    request.user.userprofile.recorder_link)
            channels = []
            for channel in user_channels:
                api_link = ""
                if channel.api_link:
                    api_link = "http://%s%s?channel_link=%s" % (
                            Site.objects.get_current().domain, 
                            reverse('get-channel-videos'),
                            channel.api_link)
                    
                channels.append({ 
                    'id':channel.id,
                    'name':channel.name,
                    'recording_link': "http://%s%s?c=%s" % (
                            Site.objects.get_current().domain, 
                            reverse('record-video'), 
                            channel.channel_slug),
                    'api_link': api_link
                })
            context['channels'] = channels
            context['is_premium'] = True
        else:
            channels = []
            for channel in user_channels:
                api_link = ""
                if channel.api_link:
                    api_link = "http://%s%s?channel_link=%s" % (
                            Site.objects.get_current().domain, 
                            reverse('get-channel-videos'),
                            channel.api_link)
                    
                channels.append({ 
                    'id':channel.id,
                    'name':channel.name,
                    'recording_link': "http://%s%s?c=%s" % (
                            Site.objects.get_current().domain, 
                            reverse('record-video'), 
                            channel.channel_slug),
                    'api_link': api_link
                })
            context['channels'] = channels

        user = request.user
        cocreates = CoCreate.objects.filter(Q(owner=user) | Q(section__assigned=user)).distinct()
        context['cocreated'] = CoCreate.objects.filter(Q(owner=user) | Q(section__assigned=user)).distinct()

        # Build list of cocreate members based on cocreate videos that the
        # user owns or is part of
        cocreators = []
        for cc in cocreates:
            cocreators += cc.cocreators
        cocreators = remove_list_duplicates(cocreators)
        cocreators_list = []
        for c in cocreators:
           sections = c.section_set.all()
           videos = []
           for section in sections:
             videos.append(section.cocreate)
           videos = remove_list_duplicates(videos)
           cocreators_list.append((c, videos))
        context['cocreators'] = cocreators_list

        info = {}
        popup = PopUpInformation.objects.all()
        for po in popup:
            info[po.html_id] = po.message    
        context["popupinfo"] = info
        response = render(request, 'my_videos.html', context)
    else:
        response = render(request, 'my_videos.html', context)

    # If requested to delete cookie
    if delete_an_tok:
        # Delete the anonymous cookie
        response.delete_cookie('an_tok')
    return response


@login_required
def approval_link(request, slug):
    """
    Videos from untrusted uploaders are sent to the site MANAGERS for
    approval.
    
    """
    video = get_object_or_404(Video, slug=slug)

    # Only the channel manager if any, can act on a video's status
    if (video.channel and
            video.channel.owner != request.user):
        raise Http404("This video cannot be approved -- %s %s" % (
                video.channel, video.channel.owner == request.user))
    if not video.channel:
        raise Http404("This video cannot be approved -- "
                "No related channel for this video")
    return render(request, 'admin_confirm_delete.html', {
        'video':video, 
        'action': 'approve'
    })


@login_required
def approve(request, slug):
    """
    Approves or denies a video depending on the `decision` passed onto the
    query string.
    
    Accepted `decision` values are:
    - `publish`
    - `deny`
    
    Raises `Http404` if video is part of a channel but the current user is not
    the channel owner.
    Sends an `invalid action` message for any `decision` values supplied other
    than those accepted.
    
    """
    decision = request.GET.get("decision", "")
    video = get_object_or_404(Video, slug=slug)

    # Only the channel manager if any, can act on a video's status
    if (video.channel and
            video.channel.owner != request.user) or not video.channel:
        raise Http404

    mode = ''
    if decision == "publish":
        video.is_active = True
        video.save()
        video = None
        mode = 'publish'
        messages.success(request, "You have activated the video")
    elif decision == "deny":
        video.is_active = False
        # Automatically expire video here, send message back to uploader
        video.expiry_date = datetime.datetime.now()
        video.save()
        video.send_deny_notification()
        video = None
        mode = 'deny'
        messages.success(request, "You have denied the video")
    else:
        mode = 'invalid'
        messages.error(request, "That action is invalid!")
    return render(request, 'admin_confirm_delete.html', {
        'video': video,
        'action': 'approve',
        'mode': mode
    })


@login_required
def confirm_delete_link(request, slug):
    """
    When a channel member deletes a channel video, the system doesn't delete
    the video just yet. It sends a notification to the admin (transferring
    ownership to the admin) first, where he can decide to keep or delete the
    video.
    
    """
    video = get_object_or_404(Video, slug=slug)
    return render(request, 'admin_confirm_delete.html', {
        'video':video, 
        'action':'delete'
    })


@login_required
def delete(request, slug):
    """
    Deletes a video.
    
    Raises `Http404` if video identified by `slug` is not found.
    
    """
    video = get_object_or_404(Video, slug=slug)
    try:
        video.delete()
        messages.success(request, "Successfully deleted video")
    except Exception,e:
        messages.error(request, "Cannot delete video: " + str(e))
    return render(request, 'admin_confirm_delete.html', {
        'video': None,
        'action':'delete'
    })


@login_required
def edit(request, slug):
    """
    Edits the information of current video.
    
    """
    video = get_object_or_404(Video, slug=slug)
    form = VideoEditForm(request.user)
    context = {}
    if request.method == "POST":
        form = VideoEditForm(request.POST, instance=video)
        if form.is_valid():
            video = form.save(commit=False)
            video.save()
            message = "Video succesfully updated."
            context['message'] = message
            form = VideoEditForm(instance=video)
    else:
        form = VideoEditForm(instance=video)
    context['form'] = form
    return render(request, 'my_videos_edit.html', context)


@csrf_exempt
def my_video_recording(request):
    """
    Record video page. 
    Fires up the recorder Java app.
    
    """
    section_id = request.REQUEST.get('section', None)
    if section_id:
        request.session['section_id'] = section_id

    anonymous_token = 'None'
    delete_an_tok = False
    user_id = None
    channel_id = None
    if request.method == "GET":
        # For shared recording link from premium accounts
        recorder_link = request.GET.get("r", None)
        if recorder_link:
            try:        
                profile = UserProfile.objects.get(recorder_link=recorder_link)
            except UserProfile.DoesNotExist:
                user_id = None
            else:
                user_id = profile.user.id
            
    if request.method == "GET":
        # For channel recording link
        channel_slug = request.GET.get("c", None)
        if channel_slug:
            channel = Channel.objects.get(channel_slug=channel_slug)
            channel_id = channel.id
            user_id = channel.owner_id
            if (request.user.is_authenticated() and 
                    request.user.userprofile.account_level.level != 'Paid'):
                user_id = None
            
    if not request.user.is_authenticated():
        if "an_tok" in request.COOKIES:
            # Use old cookie
            anonymous_token = request.COOKIES['an_tok']
        else:
            # Generate new cookie
            anonymous_token = str(uuid1())[0:25]
    elif "an_tok" in request.COOKIES:
        delete_an_tok = True

    if isinstance(request.user, User):
        userprofile = UserProfile.objects.get(user=request.user)
        total_upload_time = userprofile.total_upload_time
        max_video_length = userprofile.account_level.max_video_length
        if total_upload_time >= max_video_length:
            context = {}
            context['max_time']=True
            return render(request, 'record_video.html', context)

    # These doesn't seem to be used
    user = request.user
    allow_recording = False

    context = {
        'csrf_token': get_token(request), 
        'an_tok': anonymous_token, 
        'allow_recording': allow_recording, 
        'user_id':user_id, 
        'channel_id':channel_id,
        'reserved_slug': reserve_slug(SLUG_LENGTH),
        'slug_length': SLUG_LENGTH,
    }
    response = render(request, 'record_video.html', context)

    # Set a cookie to track their anonymous activity if user is not logged in
    if not request.user.is_authenticated() and not "an_tok" in request.COOKIES:
        set_cookie_helper(response, 'an_tok', anonymous_token, 365)
    # Delete `an_tok` if user is logged in with an old cookie
    if delete_an_tok:
        response.delete_cookie('an_tok')

    return response


def ajax_close_recorder(request):
    if request.POST:
        user = request.user
    return HttpResponse()


def oembed_test(request):
    """
    Tests the oembed capabilities of Screenbird.
    This should only be available when DEBUG = True.
    
    """
    url = request.GET.get('url', '')
    return render(request, 'video_oembed_test.html', {'url': url})


def ajax_video_is_available(request, slug, version):
    """
    Returns True if the version specified is already available.
    
    """
    status = get_object_or_404(VideoStatus, video_slug=slug)
    if version == 'web':
        response = status.web_available
    else:
        response = status.mobile_available
    http_response = HttpResponse(response)
    http_response.set_cookie('available', value=response)
    return http_response


def save_outline(request, slug):
    """
    Saves the points of an outline.
    
    """
    if request.method == 'POST':
        video = get_object_or_404(Video, slug=slug)
        outline, created = VideoOutline.objects.get_or_create(video=video)
        if not created:
            outline.videooutlinepin_set.all().delete()
        pins = simplejson.loads(request.POST.get('pins', None))
        for pin in pins:
            text = ''
            position = 0
            try:
                text = pin['text']
                position = pin['position']
            except:
                text = pins[pin]['text']
                position = pins[pin]['position']
            VideoOutlinePin.objects.create(video_outline=outline, 
                                            text=text,
                                            current_time=str(position))  # Can't convert float to Decimal, change to string first.
        return HttpResponse("OK")
    return HttpResponse("FAIL")


def get_outline(request, slug):
    """
    Returns the points of an outline as JSON.
    
    """
    if request.method == 'POST':
        video = get_object_or_404(Video, slug=slug)
        outline, created = VideoOutline.objects.get_or_create(video=video)
        points = outline.videooutlinepin_set.all().order_by('current_time')
        response = serialize("json", points)
        return HttpResponse(response, mimetype="application/json")
    return HttpResponse("FAIL")


def encode_video(request, slug):
    """
    Enqueues the slug to RabbitMQ to begin encoding of video.
    
    """
    try:
        enqueue(slug)
    except:
        messages.error(request, "There was a connection problem when " \
                "uploading your video. Please try again later.")
        return HttpResponseRedirect(reverse('my-videos'))
    return HttpResponseRedirect(reverse('watch-video', 
                                args=(slug.replace("_tmp", ""),)))


def set_to_available(request, slug, version):
    """
    Updates the video status. 
    Sets the version already encoded to available.
    
    """
    video = get_object_or_404(Video, slug=slug)
    status, created = VideoStatus.objects.get_or_create(video_slug=slug)
    if version == 'web':
        status.web_available = True
    elif version == 'cocreate':
        status.cocreate_available = True
    else:
        status.mobile_available = True
        status.is_encoding = False
    status.encode_duration = Decimal(str(status.encode_duration))
    status.save()

    # If the video is part of a cocreate project, auto-compile the cocreate project.
    try:
        if video.section and video.section.cocreate:
            cocreate_obj = video.section.cocreate
            init_cocreate(cocreate_obj, generate_slug)
    except Section.DoesNotExist:
        pass

    return HttpResponse("OK")


@csrf_exempt
@login_required
def change_recording_link(request, user_id):
    """
    Changes the recording link for the user specified by `user_id`.
    
    Raises `Http404` if a user attempts to change another user's recording link.
    
    """
    if type(user_id) == int:
        user = get_object_or_404(User, pk=user_id)
        if request.user.id == int(user_id):
            profile = get_object_or_404(UserProfile, user=user)
            profile.recorder_link = generate_slug(CHANNEL_SLUG_LENGTH)
            profile.save()
            link = "http://%s%s?r=%s" % (Site.objects.get_current().domain, 
                                         reverse('record-video'), 
                                         profile.recorder_link)
            return HttpResponse(link)
        else:
            raise Http404
    else:
        profile = get_object_or_404(UserProfile, user=request.user)
        user_account_level = profile.account_level.level
        if user_account_level == 'Paid' or user_account_level == 'Trial':
            profile.recorder_link = generate_slug(CHANNEL_SLUG_LENGTH)
            profile.save()
            link = "http://%s%s?r=%s" % (Site.objects.get_current().domain, 
                                         reverse('record-video'), 
                                         profile.recorder_link)
            return HttpResponse(link)
        else:
            raise Http404


def video_page_embed_code(request, user_id):
    user = get_object_or_404(User, pk=user_id)
    owned_channels = user.channel_set.all()
    part_channels = user.userprofile.channels.all()
    user_channels = (owned_channels | part_channels).distinct()
    channel_videos = Video.objects.filter(channel__in=user_channels, 
                                          is_public=True)
    user_videos = Video.objects.filter(uploader=user, is_public=True)

    # Default values
    scroll = False
    pages = int(request.GET.get('pages', '10'))
    checked = True

    # Pre-conditions for building the queryset
    if request.GET:
        if request.GET.get('show_channel','false') == 'true':
            checked = True
        else:
            checked = False

    if checked:
        # Display owned public videos including channel videos
        my_videos = (user_videos | channel_videos).distinct()
    else:
        # Display owned public videos only
        my_videos = user_videos.exclude(channel__in=user_channels)
    my_videos = my_videos.order_by('-created', 'channel')
    my_videos_list = my_videos.values('id', 'slug', 'title', 'channel__name')

    # Post-conditions for displaying scrollbar based on queryset
    if request.GET:
        if request.GET.get('scroll','false') == 'on':
            scroll = True
        else:
            if pages > 10 and my_videos.count() > 10:
                scroll = True
            else:
                scroll = False
    else:
        if pages > 10 and my_videos.count() > 10:
            scroll = True


    paginator = Paginator(my_videos_list, pages)
    try:
        page = int(request.GET.get('page', '1'))
    except ValueError:
        page = 1
    try:
        latest_submissions_page = paginator.page(page)
    except (EmptyPage, InvalidPage):
        # If page request is out of range, deliver last page of results.
        latest_submissions_page = paginator.page(paginator.num_pages)
    next_page = latest_submissions_page.next_page_number()
    prev_page = latest_submissions_page.previous_page_number()
    context = {
        'latest_submissions_page':latest_submissions_page, 
        'paginator':paginator, 
        'next_page':next_page,
        'prev_page':prev_page, 
        'user': user, 
        'scroll': scroll, 
        'pages':pages, 
        'checked':checked,
    }
    return render(request, 'my_video_page_embed.html', context)


@login_required
def cocreate(request, cocreate_id=None, section_idx=None):
    """
    Returns the landing page for given cocreate project and it's sections.
    """
    cocreate = get_object_or_404(CoCreate, id=cocreate_id)
    is_owner = cocreate.owner == request.user
    if not is_owner and not request.user in cocreate.cocreators:
        return render(request, 'cocreate/invalid.html')

    sections = cocreate.ordered_sections

    form = SectionForm(default_cocreate=cocreate)
    invite_form = InvitationForm()
    #form.fields['assigned'].queryset = User.objects.filter(userprofile__is_anonymous=False)

    # Determine if the given section index is in this cocreate's sections.
    try:
        selected = sections[int(section_idx)-1]
        selected_index = section_idx
    except:
        selected = None

    # If we have a selected section, check the video status
    if selected:
        is_assigned = selected.assigned == request.user
        if selected.video:
            if selected.video.expired:
                video_status = 'EXPIRED'
            else:
                video_status_obj = VideoStatus.objects.get_video_status(selected.video.slug, request)
                user_agent = request.META['HTTP_USER_AGENT']
                if (('android' in user_agent.lower() and video_status_obj.mobile_available) or
                    ('android' not in user_agent.lower() and video_status_obj.web_available)):
                    video_status = 'OK'
                else:
                    video_status = 'WAIT'
        else:
            video_status = 'DNE'
    else:
        video_status = 'NONE' # OK DELETED WAIT EXPIRED?


    return render(request, 'cocreate/cocreate.html', locals())

@login_required
def cocreate_new(request):
    """
    Create a new cocreate project.
    """
    if request.method =="POST":
        form = CoCreateForm(request.POST)
        if form.is_valid():
            cocreate = form.save(commit=False)
            cocreate.owner = request.user
            cocreate.save()
            return redirect('cocreate', cocreate_id=cocreate.id)
    else:
        form = CoCreateForm()

    title = "Co-Create Form"
    form_name = "Co-Create a Video:"
    return render(request, 'cocreate/cocreate_new.html', locals())

@login_required
def cocreate_section(request, cocreate_id=None):
    """
    Create new sections, supports normal POST and ajax.
    """

    cocreate = get_object_or_404(CoCreate, id=cocreate_id)
    is_owner = cocreate.owner == request.user
    # allow only the owner of this cocreate project
    if not is_owner and not request.user in cocreate.cocreators:
        if request.is_ajax():
            return HttpResponse(simplejson.dumps({'status': 'ERROR'}))
        else:
            return render(request, 'cocreate/invalid.html')

    if request.method =="POST":
        form = SectionForm(request.POST, default_cocreate=cocreate)
        section = None
        if form.is_valid():
            section = form.save(commit=False)
            section.cocreate = cocreate
            section.save()

            if section.assigned and not section.assigned == request.user:
                user = section.assigned
                new_username = user.username
                new_password = generate_random_string(6)
                user.set_password(new_password)
                user.save()
                email = user.email

                # Send email to the new user
                subject = "Invitation from Screenbird"
                from_mail = settings.DEFAULT_FROM_EMAIL
                reg_link = "".join(("http://",request.META['HTTP_HOST'],reverse("register")))
                login_link = "".join(("http://", request.META['HTTP_HOST'], reverse("login")))
                account_link = "".join(("http://",request.META['HTTP_HOST'],reverse("manage_account")))
                cocreate_section_link = "".join(("http://", request.META['HTTP_HOST'], reverse("cocreate_section", args=(cocreate.pk, section.order))))
                if user.date_joined == user.last_login:
                    message = render_to_string("email/cocreate_invite_screenbird.txt", {
                                           'username': new_username,
                                           'password':new_password,
                                           'email': email,
                                           'user':request.user.username,
                                           'account_link':account_link,
                                           'cocreate_section_link': cocreate_section_link,
                    })
                    html_message = render_to_string("email/cocreate_invite_screenbird.html", {
                                           'username': new_username,
                                           'password': new_password,
                                           'email': email,
                                           'user': request.user.username,
                                           'account_link': account_link,
                                           'cocreate_section_link': cocreate_section_link
                    })
                    mail = EmailMultiAlternatives(subject, message, from_mail, [email])
                    mail.attach_alternative(html_message, "text/html")
                    mail.send()
                elif UserSocialAuth.objects.filter(user=user).exists():
                    message = render_to_string("email/cocreate_invite_social.txt", {
                        'login_link': login_link,
                        'cocreate_section_link': cocreate_section_link,
                    })
                    html_message = render_to_string("email/cocreate_invite_social.html", {
                        'login_link': login_link,
                        'cocreate_section_link': cocreate_section_link,
                    })
                    mail = EmailMultiAlternatives(subject, message, from_mail, [email])
                    mail.attach_alternative(html_message, "text/html")
                    mail.send()
                else:
                    message = render_to_string("email/cocreate_invite.txt", {
                        'cocreate_section_link': cocreate_section_link
                    })
                    html_message = render_to_string("email/cocreate_invite.html", {
                        'cocreate_section_link': cocreate_section_link
                    })
                    mail = EmailMultiAlternatives(subject, message, from_mail, [email])
                    mail.attach_alternative(html_message, "text/html")
                    mail.send()

            if request.is_ajax():
                if section:
                    t = loader.get_template("cocreate/sections.html")
                    c = RequestContext(request, {'cocreate':cocreate, 'sections':cocreate.ordered_sections, 'is_owner':True,
                                                 'form':SectionForm(default_cocreate=cocreate) })
                    section_url = reverse('cocreate_section', args=(cocreate.pk, section.order))
                    return HttpResponse(simplejson.dumps({'status': 'OK', 'content': t.render(c), 'section_url': section_url}))
                else:
                    return HttpResponse(simplejson.dumps({'status': 'ERROR'}))
            else:
                    return redirect('cocreate', cocreate_id=cocreate.id)
        else:
            return HttpResponse(simplejson.dumps({'status': 'ERROR', 'errors':form.errors}))
    else:
        form = SectionForm(default_cocreate=cocreate)
    title = "Add a section"
    form_name = "Add a section:"
    return render(request, 'cocreate/cocreate_new.html', locals())

@login_required
@require_POST
def section_edit(request, section_id, field):
    """
    Updates the section fields.

    """
    value = request.POST.get('value', None)
    section = get_object_or_404(Section, id=section_id)

    is_owner = section.cocreate.owner == request.user
    if not is_owner and not request.user in section.cocreate.cocreators:
        return HttpResponse(value or '')

    if field == 'name':
        section.name = value or ''
        section.save()
    # TODO: assigned

    return HttpResponse(value or '')

@login_required
@require_POST
def section_edit_in_outline(request, section_id):
    """
    Updates sections in outline, supports normal POST and ajax.
    """

    old_section = get_object_or_404(Section, id=section_id)
    is_owner = old_section.cocreate.owner == request.user
    # allow only the owner of this cocreate project
    if not is_owner and not request.user in old_section.cocreate.cocreators:
        if request.is_ajax():
            return HttpResponse(simplejson.dumps({'status': 'ERROR'}))
        else:
            return render(request, 'cocreate/invalid.html')

    if request.method =="POST":
        edit_form = SectionForm(request.POST, instance=old_section, default_cocreate=old_section.cocreate)
        section = None
        if edit_form.is_valid():
            section = edit_form.save()
            if request.is_ajax():
                if section:
                    t = loader.get_template("cocreate/sections.html")
                    c = RequestContext(
                            request, 
                            {
                                'cocreate':old_section.cocreate, 
                                'sections':old_section.cocreate.ordered_sections, 
                                'is_owner':True,
                                'form':SectionForm(default_cocreate=old_section.cocreate),
                            }
                        )
                    return HttpResponse(simplejson.dumps({'status': 'OK', 'content': t.render(c)}))
                else:
                    return HttpResponse(simplejson.dumps({'status': 'ERROR'}))
            else:
                    return redirect('cocreate', cocreate_id=old_section.cocreate.id)
        else:
            return HttpResponse(simplejson.dumps({'status': 'ERROR', 'errors':edit_form.errors}))
    else:
        edit_form = SectionForm(instance=old_section)
    title = "Update a section"
    form_name = "Update a section:"
    return render(request, 'cocreate/cocreate_new.html', locals())

@login_required
def ajax_show_edit_form(request):
    """
    Loads bound form for editing a section
    """

    if request.is_ajax():
        if request.method == "POST":
            section = get_object_or_404(Section, id=int(request.POST.get('section_id', 0)))
            is_owner = section.cocreate.owner == request.user

            edit_form = SectionForm(instance=section, default_cocreate=section.cocreate)

            context = {
                'section': section,
                'is_owner': is_owner,
                'edit_form': edit_form,
            }

            return render_to_response(
                'cocreate/ajax_edit_section.html', 
                locals(), 
                context_instance=RequestContext(request)
            )

@login_required
def ajax_delete_section(request):
    """
    Deletes a section in the outline
    """

    section = get_object_or_404(Section, id=int(request.POST.get('section_id', 0)))
    cocreate = section.cocreate
    is_owner = section.cocreate.owner == request.user

    if request.method =="POST":
        if request.is_ajax():
            section.delete()
            t = loader.get_template("cocreate/sections.html")
            c = RequestContext(
                    request, 
                    {
                        'cocreate': cocreate, 
                        'sections': cocreate.ordered_sections, 
                        'is_owner': is_owner,
                        'form': SectionForm(default_cocreate=cocreate),
                    }
                )
            return HttpResponse(simplejson.dumps({'status': 'OK', 'content': t.render(c)}))  
        else:
            return redirect('cocreate', cocreate_id=cocreate.id)

def get_encoding_progress(request, slug):
    """
    Fetches and returns ffmpeg encoding output for video identified by `slug`
    from associated ec2_node.
    
    """
    video = get_object_or_404(Video, slug=slug)
    status = get_object_or_404(VideoStatus, video_slug=slug)
    try:
        cocreate = video.cocreate
    except:
        pass
    else:
        if not status.cocreate_available:
            return get_cocreate_progress(request, slug)

    node = status.ec2_node
    pem_path = os.path.join(settings.PEM_PATH, "%s.pem" % settings.EC2_KEY_NAME)
    # Base command for ssh with ec2
    base = ["""/usr/bin/ssh""", """-i""", pem_path, """-o""", 
            """StrictHostKeyChecking no""", "screenbird@%s" % node]
    
    # Check encoder log for the video slug
    cmd = ["tail", "-n", "40", "encoder.log"]
    p = subprocess.Popen(base + cmd, stderr=subprocess.PIPE, 
                         stdout=subprocess.PIPE)
    p.wait()
    encoder_log, error = p.communicate()
        
    if slug in encoder_log:
        # Read ffmpeg output for video if encoding already started
        cmd = ["cat", "%s.txt" % slug]
        p = subprocess.Popen(base + cmd, stderr=subprocess.PIPE, 
                             stdout=subprocess.PIPE)
        ffmpeg_out, error = p.communicate()
        matches = re.search(r'Duration:\s*(?P<hours>\d+):(?P<minutes>\d+):(?P<seconds>[\d.]+)', ffmpeg_out, re.DOTALL)
        if not matches:
            return HttpResponse(simplejson.dumps({
                'phase': 'encode',
                'status': 'Enqueued for encoding',
            }))
        md = matches.groupdict()
        total_time = (int(md['hours']) * 3600) + (int(md['minutes']) * 60) + float(md['seconds'])
        matches = re.findall(r'frame=[\w\s=.]+time=\d+:\d+:[\d.]+', ffmpeg_out)
        if matches:
            matches = matches[-1]
            nm = re.search(r'frame=[\w\s=.]+time=(?P<hours>\d+):(?P<minutes>\d+):(?P<seconds>[\d.]+)', matches, re.DOTALL).groupdict()
            current_time = (int(nm['hours']) * 3600) + (int(nm['minutes']) * 60) + float(nm['seconds'])
            return HttpResponse(simplejson.dumps({
                'phase': 'encode',
                'status': 'Encoding video',
                'progress': current_time / total_time,
            }))
        # return HttpResponse("ENCODE: " + ffmpeg_out)
    return HttpResponse(simplejson.dumps({
        'phase': 'encode',
        'status': 'Enqueued for encoding',
    }))

def get_cocreate_progress(request, slug):
    """
    Fetches and returns cocreate progress output for video identified by `slug`
    from associated `cocreate_node`.
    
    """
    status = get_object_or_404(VideoStatus, video_slug=slug)
    video = get_object_or_404(Video, slug=slug)
    cocreate = None
    try:
        cocreate = video.cocreate
    except:
        raise Http404
    node = status.cocreate_node
    pem_path = os.path.join(settings.PEM_PATH, "%s.pem" % settings.EC2_KEY_NAME)
    # Base command for ssh with ec2
    base = ["""/usr/bin/ssh""", """-i""", pem_path, """-o""", 
            """StrictHostKeyChecking no""", "screenbird@%s" % node]
    
    # Check encoder log for the video slug
    cmd = ["tail", "cocreate.log"]
    p = subprocess.Popen(base + cmd, stderr=subprocess.PIPE, 
                         stdout=subprocess.PIPE)
    p.wait()
    cocreate_log, error = p.communicate()
    # last status
    match = re.search(r'cocreate: %s: (?P<status>[\w\s]+)\s$' % slug, cocreate_log)
    if match:
        status = match.groupdict()['status']
        if status == 'Merging section videos':
            status = 'Compiling video'
            progress = None 

            cmd = ["cat", "%s_mencoder.txt" % slug]
            p = subprocess.Popen(base + cmd, stderr=subprocess.PIPE, 
                                 stdout=subprocess.PIPE)
            ffmpeg_out, error = p.communicate()
            matches = re.findall(r'Pos:\s+[0-9.]+s\s+\d+f\s+\(\s*\d+%\)[[\]0-9a-za-z. -:]+Trem:\s*\d+min', ffmpeg_out)
            if matches:
                matches = matches[-1]
                new_matches = re.search(r'Pos:\s+(?P<position>[0-9.]+)s\s+\d+f\s+\(\s*(?P<progress>\d+)%\)[[\]0-9a-za-z. -:]+Trem:\s*\d+min', matches, re.DOTALL).groupdict()
                position = new_matches['position']
                duration = video.video_duration
                if not duration:
                    duration = Decimal("0.0")
                    for video in cocreate.available_videos:
                        duration += video.video_duration
                progress = Decimal(str(position)) / Decimal(str((duration * SECONDS)))

            # return HttpResponse("CREATE: " + matches)
            return HttpResponse(simplejson.dumps({
                'phase': 'cocreate',
                'status': status,
                'progress': progress,
            }))
        else:
            return HttpResponse(simplejson.dumps({
                'phase': 'cocreate',
                'status': status,
            }))
            # return HttpResponse("CREATE: " + status)
    else:
        # return HttpResponse("CREATE: " + "ENQUEUED")
        return HttpResponse(simplejson.dumps({
            'phase': 'cocreate',
            'status': status,
        }))

def set_encoding_ec2(request, slug, ec2_node):
    """
    Associates the video with an ec2 node the sent the request. This view is
    called from an ec2 node.
    
    """
    status = get_object_or_404(VideoStatus, video_slug=slug)
    status.ec2_node = ec2_node
    status.save()
    return HttpResponse("OK")


def set_cocreate_node(request, slug, cocreate_node):
    """
    Associates the video with an ec2 node that complies the cocreate project.
    This view is called from an ec2 node.

    """
    status = get_object_or_404(VideoStatus, video_slug=slug)
    status.cocreate_node = cocreate_node
    status.save()
    return HttpResponse("OK")


def set_duration(request, slug):
    """
    Sets the `video_duration` of video identified by `slug` from ffmpeg 
    output. Also updates total_upload_time for the video uploader. 
    This view is called from the ec2 node where the video is encoded after 
    both web and mobile versions are encoded.
    
    """
    video = get_object_or_404(Video, slug=slug)
    uploader_profile = video.uploader.userprofile
    duration = request.GET.get("duration", "0.0")
    old_duration = video.video_duration
    video.video_duration = Decimal(duration)
    video.save()

    # Update total_upload_time for video uploader
    total_time = uploader_profile.total_upload_time
    total_time = Decimal(str(total_time)) + Decimal(str(video.video_duration)) - Decimal(str(old_duration))
    uploader_profile.total_upload_time = total_time
    uploader_profile.save()
    return HttpResponse("OK")


def set_encode_time(request, slug):
    """
    Sets the `encode_duration` of video identified by `slug` from ffmpeg 
    output. Also updates total_upload_time for the video uploader. 
    This view is called from the ec2 node where the video is encoded after 
    both web and mobile versions are encoded.
    
    """
    video_status = get_object_or_404(VideoStatus, video_slug=slug)
    duration = request.GET.get("duration", "0.0")
    video_status.encode_duration = Decimal(str(duration))
    video_status.save()
    return HttpResponse("OK")


@login_required
@require_POST
def cocreate_edit(request, cocreate_id, field):
    """
    Updates the cocreate fields.
    
    """
    value = request.POST.get('value', None)
    cocreate = get_object_or_404(CoCreate, id=cocreate_id)

    is_owner = cocreate.owner == request.user
    if not is_owner and not request.user in cocreate.cocreators:
        return HttpResponse(value or '')

    if field == 'notes':
        cocreate.notes = value
        cocreate.save()
    elif field == 'description':
        cocreate.description = value
        cocreate.save()

    return HttpResponse(value)

@login_required
def cocreate_compile(request, cocreate_id):
    """
    Compile the videos for this cocreate project when there is at least 1 section
    with a video.
    """
    cocreate = get_object_or_404(CoCreate, id=cocreate_id)
    is_owner = cocreate.owner == request.user

    if is_owner:
        if len(cocreate.videos) > 0:
            init_cocreate(cocreate, generate_slug)

            return redirect(reverse('watch-video', args=(cocreate.output_video.slug,)))
        else:
            messages.error(request, 'You must have at least 1 completed section to compile a cocreate project.')
            return redirect(reverse('cocreate', args=(cocreate.pk,)))
    else:
        return render(request, 'cocreate/invalid.html')

@csrf_exempt
def update_cocreate_outline(request, slug):
    """
    update the duration of the cocreate outline
    """
    video = get_object_or_404(Video, slug=slug)
    if not video.cocreate:
        return HttpResponse('No cocreate')
    if request.method == 'POST':
        data = request.POST.get('data', None)
        if data:
            points = eval(data)
            outline = video.videooutline_set.all()
            if outline.count() > 0:
                pins = list(outline[0].videooutlinepin_set.order_by('current_time'))
                for i in range(len(points)):
                    try:
                        curpin = pins[i]
                        curpin.current_time = Decimal(str(points[i]))
                        curpin.save()
                    except Exception, e:
                        logger.debug('Error saving pin: %s')
    return HttpResponse('OK')
