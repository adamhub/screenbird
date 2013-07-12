import datetime 
import os
import random
import subprocess
import urllib
import urllib2
import warnings
from decimal import Decimal

from django.conf import settings
from django.contrib.auth.models import User
from django.contrib.contenttypes import generic
from django.contrib.contenttypes.models import ContentType
from django.contrib.sites.models import Site
from django.core.urlresolvers import reverse
from django.core.mail import send_mail, mail_managers
from django.db import models
from django.db.models import Q
from django.db.models.signals import post_save, post_delete, pre_save
from django.dispatch.dispatcher import receiver
from django.http import HttpRequest, Http404
from django.template.loader import render_to_string
from django.utils.hashcompat import sha_constructor

import tweepy
from facepy import GraphAPI
from hitcount.models import HitCount
from social_auth.models import UserSocialAuth
from thumbsup.models import ThumbsUpManager

from accounts.models import UserProfile
from amazon.utils import add_to_upload_queue, delete_from_s3


DAYS_BEFORE_EXPIRATION = getattr(settings, 'VIDEOS_DAYS_BEFORE_EXPIRATION', 60)
DAYS_BEFORE_DELETION = getattr(settings, 'VIDEOS_DAYS_BEFORE_DELETION', 90)


class Channel(models.Model):
    """
    A group that a user may create.

    """
    name = models.CharField(max_length=255)
    owner = models.ForeignKey(User)
    api_link = models.CharField(max_length=20, default="")
    channel_slug = models.SlugField(unique=True)
    
    def __unicode__(self):
        return self.name

    @models.permalink
    def get_absolute_url(self):
        return ('update_channel', (), {
            'channel_id': self.pk
        })

    def get_channel_members(self):
        return User.objects.filter(id__in=self.userprofile_set.all().values_list('user', flat=True))


class ChannelOptionManager(models.Manager):
    def sync(self):
        """
        Creates instances of `ChannelOption` for each user profile-channel
        relationship if it does not exist.

        """
        users = User.objects.all()
        for user in users:
            try:
                user_profile = user.userprofile
                channels = user_profile.channels.all()
                for channel in channels:
                    option, _ = self.get_or_create(user_profile=user_profile,
                                                   channel=channel)
            except models.get_model('accounts', 'userprofile').DoesNotExist:
                pass


class ChannelOption(models.Model):
    """
    Channel permissions for a member user.
    
    """
    user_profile = models.ForeignKey('accounts.UserProfile')
    channel = models.ForeignKey('channel')
    needs_approval = models.BooleanField(default=False, 
            help_text='If this is true, videos uploaded by this user \
            to the channel will need to be approved.')

    objects = ChannelOptionManager()

    def __unicode__(self):
        return u'%s-%s' % (self.channel, self.user_profile)


def create_channel_option(sender, instance, created, **kwargs):
    """
    Creates a `ChannelOption` for the newly created `Channel` with the
    creator's UserProfile.
    
    """
    if created:
        owner = instance.owner
        channel_option, created = ChannelOption.objects.get_or_create(
                channel=instance, user_profile=owner.userprofile)
post_save.connect(create_channel_option, sender=Channel)

class CocreateOption(models.Model):
    user = models.ForeignKey(User)
    cocreate = models.ForeignKey('CoCreate')

    class Meta:
        verbose_name = "Co-create Member"

    def __unicode__(self):
        return u'%s-%s' % (self.cocreate, self.user)


class ConfirmationInfo(models.Model):
    """
    Data associated to the invitation email when inviting channel members.

    """
    username= models.CharField(max_length=250, default='')
    email = models.EmailField()
    channel = models.ForeignKey(Channel)
    key = models.CharField(max_length=40, editable=False)
    status = models.CharField(max_length=10, editable=False, default='pending')

    class Meta:
        unique_together = ('email', 'channel')

    def save(self, *args, **kwargs):
        """
        Uses `self.key` to confirm request to join channel.

        """
        if not self.key:
            salt = sha_constructor(str(random.random())).hexdigest()[:5]
            hashed_email = sha_constructor(salt + self.email).hexdigest()[:10]
            self.key = hashed_email
        super(ConfirmationInfo, self).save(*args, **kwargs)


class VideoManager(models.Manager):
    def embeddable(self):
        """
        Returns a queryset of all embeddable videos. A video is embeddable if
        it is not part of a channel, is active, and is not expired.

        """
        return self.get_query_set().filter(is_active=True,
                                           expired=False)

    def viewable_by(self, user_or_request=None):
        """
        Returns a queryset of videos viewable by `user` if specified, or by
        the public if it is `None`. A video is viewable to a user if:
         - it is active; or
            - it is not active, but it is part of a channel, and `user`
              happens to be its owner; and
         - if the video is not part of a channel:
            - it is public, or `user` uploaded it
         - if the video is part of a channel:
            - `user` is a member of it
         - it is not expired

        A video is viewable to the public if:
         - it is active; and
         - it is not part of a channel; and
         - it is not expired

        UPDATE!
        New viewing permissions (according to AdamT):
        A video is viewable to the public if:
          - it is active; and
          - it is not expired
        A private video is viewable to the public:
          - if they have the url


        """
        # Find the correct user if the object passed is an instance of
        # `HttpRequest`.
        if isinstance(user_or_request, HttpRequest):
            user = user_or_request.user

        if not user.is_authenticated():
            # Try to find the user through the `an_tok` cookie, giving up
            # when we do not find the `an_tok` cookie, or if no `User` object
            # has been made for this cookie.
            if user_or_request.COOKIES.get('an_tok'):
                try:
                    user = User.objects.get(username=user_or_request.COOKIES['an_tok'])
                except User.DoesNotExist:
                    user = None
            else:
                user = None

        if user:
            # Ability to view own videos.
            own = Video.objects.filter(uploader=user)
            # Ability to view all public videos.
            public = Video.objects.filter(is_active=True)
            return (own | public).distinct()
        else:
            return Video.objects.filter(is_active=True)


class Video(models.Model):
    VIDEO_TYPE_CHOICES = (('', 'Video Type'),
                         ('mp4', 'mp4'),
                         ('ogv', 'ogg'),
                         ('webm', 'webm'))

    # Basic Video Information
    uploader = models.ForeignKey(User, null=True)
    title = models.CharField(max_length=255, 
                             help_text="A nice title for the video clip")
    video_duration = models.DecimalField(max_digits=11, decimal_places=2, 
                                         default=Decimal('0.0'))
    video_type = models.CharField(max_length=10, choices=VIDEO_TYPE_CHOICES, 
                                  blank=False)
    slug = models.SlugField(unique=True,
            help_text="A url friendly field for the video clip, this slug \
                       should be unique to every clip." )
    description = models.TextField( null=True, blank=True,
            help_text="A short description about your video")
    # Publication details
    is_public = models.BooleanField(default=False)
    is_active = models.BooleanField(default=True)
    created = models.DateTimeField(auto_now_add=True, editable=False)
    updated = models.DateTimeField(auto_now=True, auto_now_add=True, 
                                   editable=False)
    channel = models.ForeignKey(Channel, null=True, blank=True)
    expiry_date = models.DateTimeField(editable=False, null=True, blank=True)
    expired = models.BooleanField(default=False)
    featured = models.BooleanField(default=False)

    # Youtube Embed URL field
    youtube_embed_url = models.CharField(max_length=255, blank=True, null=True)
    youtube_video_expiry_date = models.DateTimeField(editable=False, null=True, blank=True,
            help_text="The date when the video will be deleted on server after it was uploaded to YouTube.")

    objects = VideoManager()
    thumbs = ThumbsUpManager()

    # For hitcount
    hitcount = generic.GenericRelation(HitCount, object_id_field='object_pk')

    def __unicode__(self):
        return self.title

    def hits(self):
        video_type = ContentType.objects.get_for_model(self)

        try:
            hitcount_object = HitCount.objects.filter(content_type__id=video_type.id, object_pk=self.id)[0]
            return hitcount_object.hits
        except IndexError, i:
            return 0
    hits.admin_order_field = "hitcount__hits"

    def uploader_userprofile(self):
        profile_type = ContentType.objects.get_for_model(self.uploader.userprofile)
        profile_app_label = profile_type.app_label
        profile_model_name = profile_type.model
        profile_url = 'admin:%s_%s_change' % (profile_app_label, profile_model_name)

        return '<a href="%s">%s</a>' % (reverse(profile_url , args=(self.uploader.userprofile.id,)), self.uploader)
    uploader_userprofile.allow_tags = True
    uploader_userprofile.admin_order_field = "uploader__userprofile__user__username"

    @property
    def embed_code(self):
        """
        Deprecated. Use `Video.get_embed_code` for more flexibility
        regarding the generated embed code.

        """
        warnings.warn('`Video.embed_code` is deprecated; use `Video.get_embed_code() instead for more flexibility.',
                      DeprecationWarning)
        return self.get_embed_code()

    def get_embed_code(self, width=640, height=360):
        """
        Returns the embed code for this video, given `width` and `height`.

        """
        return ''.join(['<script src="', 'http://',
                        Site.objects.get_current().domain,
                        reverse('video-embed-js', args=[self.slug]),
                        '?width=%s&amp;height=%s&r=%s' % (width, height, random.randint(1000000, 9999999)),
                        '"></script><div id="_paste_vid__master__%s"></div>' % self.slug])

    def update_expiry_date(self):
        account_level = self.get_account_level()
        if account_level.video_validity:
            self.expiry_date = datetime.datetime.now() + datetime.timedelta(days=DAYS_BEFORE_EXPIRATION)
        else:
            self.expiry_date = None
            self.expired = False
        self.save()
            
    def save(self, *args, **kwargs):
        """
        Saves the video. 
        
        The uploader's `total_upload_time` will be updated after the video's 
        encoding process is finished.
        
        """
        if not self.pk:
            account_level = self.get_account_level()
            if account_level.video_validity:
                self.expiry_date = datetime.datetime.now() + datetime.timedelta(days=DAYS_BEFORE_EXPIRATION)
            self.video_type = 'mp4'
            VideoStatus.objects.create(video_slug=self.slug)
        super(Video, self).save(*args, **kwargs)

    def delete(self, *args, **kwargs):
        """
        If the video to be deleted is a part of a channel, reassign the
        video owner to be the admin, send the appropriate notification, and
        he can then choose whether to delete or keep the video.

        In addition, updates the uploader's total upload time.

        """
        from videos.utils import get_video_length
        if self.uploader:
            try:
                profile = self.uploader.userprofile
            except UserProfile.DoesNotExist:
                return
            else:
                total_time = profile.total_upload_time
                total_time = total_time - self.video_duration
                profile.total_upload_time = str(total_time)
                profile.save()

            if self.channel and self.uploader != self.channel.owner:
                # Videos part of a channel, when about to be deleted,
                # will instead be reassigned to the channel owner to
                # act upon.
                self.uploader = self.channel.owner
                self.save()
                self._send_video_delete_notification()
                return False
        if settings.PUSH_TO_S3:
            delete_from_s3('%s_tmp' %self.slug)
            delete_from_s3(self.slug)
            delete_from_s3('%s__mobile' % self.slug)
        else:
            video_path = os.path.join(settings.MEDIA_ROOT, 'tmp', '%s.mp4' % self.slug)
            mobile_video_path = os.path.join(settings.MEDIA_ROOT, 'tmp', '%s__mobile.mp4' % self.slug)
            os.remove(video_path)
            os.remove(mobile_video_path)
        super(Video, self).delete(*args, **kwargs)

    def _send_video_delete_notification(self):
        """
        Sends the video link where the owner can watch the video, and determine
        whether to keep or delete it.

        """
        if self.channel:
            link = "".join(["http://", Site.objects.get_current().domain, self.confirm_delete_link()])
            message = render_to_string('email/delete_notification.txt', {
                'video': self,
                'link': link
            })
            subject = "Delete channel video"
            self.channel.owner.email_user(subject, message)

    def send_approval_notification(self):
        """
        Sends video to the channel admin/manager/owner for approval.

        """
        if self.channel:
            link = "".join(["http://", Site.objects.get_current().domain, self.approval_link()])
            message = render_to_string('email/approval_notification.txt', {
                'video': self,
                'link': link
            })
            subject = "Video Approval"
            self.channel.owner.email_user(subject, message)
            self.is_active = False
            self.save()

    def send_deny_notification(self):
        """
        Sends a message to the uploader that this video, marked for approval,
        has been denied by the channel admin/manager/owner.

        """
        if self.uploader.email:
            link = "".join(["http://", Site.objects.get_current().domain, self.get_absolute_url()])
            message = render_to_string('email/video_denied.txt', {
                'video': self,
                'link': link,
                'user': self.uploader
            })
            subject = "Video denied"
            self.uploader.email_user(subject, message)

    @models.permalink
    def get_absolute_url(self):
        return ('watch-video', [self.slug])

    def slug_video_link(self):
        return '<a href="%s">%s</a>' % (self.get_absolute_url(), self.slug)
    slug_video_link.allow_tags = True
    slug_video_link.admin_order_field = "slug"
    
    def confirm_delete_link(self):
        return reverse('confirm-delete-link', args=[self.slug])

    def approval_link(self):
        return reverse('approval-link', args=[self.slug])

    def get_video_status(self):
        """
        Returns the `VideoStatus` object associated with this video.

        Raises `VideoStatus.DoesNotExist` if the video status cannot be
        found/created (video is either deleted or does not exist).

        """
        return models.get_model('videos', 'videostatus').objects.get_video_status(slug=self.slug)

    def get_account_level(self):
        """Returns `account_level` associated with the video. 
        
        Use channel owner's `account_level` if video is part of a channel. 
        Otherwise, use video uploader's `account_level`.
        
        """
        if self.channel:
            account_level = self.channel.owner.userprofile.account_level
        else:
            account_level = self.uploader.userprofile.account_level
        return account_level


class VideoStatusManager(models.Manager):
    def get_video_status(self, slug, request_or_user=None):
        """
        Returns the status of the video. The viewability of `request_or_user` is
        also taken into consideration. If video is not viewable by the user, returns
        None

        If `request_or_user` is not defined, do not apply viewability
        restrictions.

        """
        video_status= self.get_query_set().get(video_slug=slug)
        if request_or_user:
            try:
                video = Video.objects.viewable_by(request_or_user).get(slug=slug)
            except Video.DoesNotExist:
                return None
        return video_status

class VideoStatus(models.Model):
    """
    This model monitors the state of a video, without making a lot of
    possibly breaking changes to the existing video model and video
    encoding process.

    If the video is being encoded:
     - slug = <slug>
        - video = <Video object>, but video not yet playable
     - is_encoding = True

    If the video is present and is playable:
     - slug = <slug>
        - video = <Video object>
     - is_encoding = False

    If the video has been deleted:
     - slug = <slug>
        - video = None
     - is_deleted = True

    * web_available is True when web version encoding successfully finishes.
    * mobile_available is True when mobile version encoding successfully finishes.

    """
    video_slug = models.SlugField(unique=True)
    is_encoding = models.BooleanField(default=True)
    is_deleted = models.BooleanField(default=False)
    web_available = models.BooleanField(default=False)
    mobile_available = models.BooleanField(default=False)

    # For tracking of encoding progress
    ec2_node = models.CharField(max_length=60, blank=True, null=True)
    cocreate_node = models.CharField(max_length=60, blank=True, null=True)
    cocreate_available = models.BooleanField(default=False)
    encode_duration = models.DecimalField(max_digits=11, decimal_places=2, default=Decimal('0.0'))

    class Meta:
        verbose_name_plural = "Video Status"

    objects = VideoStatusManager()

    def get_video_or_none(self):
        if not hasattr(self, '_video'):
            try:
                self._video = Video.objects.get(slug=self.video_slug)
            except Video.DoesNotExist:
                self._video = None
        return self._video

    def set_to_encoding(self):
        """
        reset video status to initial.
        """
        self.is_encoding = True
        #self.is_deleted = False
        self.web_available = False
        self.mobile_available = False
        self.cocreate_available = False
        self.save()

    @property
    def is_deleted(self):
        """
        Returns `True` if the video being referred to by `self.slug`
        no longer exists in the database, and the `is_encoding` flag is not
        set.

        """
        return not self.is_encoding and not self.get_video_or_none()

    def __unicode__(self):
        return self.video_slug


class FeaturedVideo(models.Model):
    """
    Featured videos that will appear at the top of the video list
    
    """
    video = models.ForeignKey(Video, null=True)
    position = models.IntegerField()    # The position field

    def save(self, *args, **kwargs):
        model = self.__class__
        if self.position is None:
            # Append
            try:
                last = model.objects.order_by('-position')[0]
                self.position = last.position + 1
            except IndexError:
                # First row
                self.position = 0

        return super(FeaturedVideo, self).save(*args, **kwargs)

    def __unicode__(self):
        return self.video.title

    class Meta:
        ordering = ('position',)


class VideoOutline(models.Model):
    video = models.ForeignKey(Video)

    def __unicode__(self):
        return u'%s outline' % self.video.title


class VideoOutlinePin(models.Model):
    """
    This model is used to mark a specific point in the video to be used as a
    reference in the video's outline.
    
    """
    video_outline = models.ForeignKey(VideoOutline)
    text = models.CharField(max_length=50)
    current_time = models.DecimalField(default=0.0, max_digits=6, decimal_places=2)

    def __unicode__(self):
        return u'%s outline pin at %s' % (self.video_outline, self.current_time)


class CoCreate(models.Model):
    title = models.CharField(max_length=255)
    owner = models.ForeignKey(User)
    description = models.TextField(blank=True)
    notes = models.TextField(blank=True)
    # the output video
    output_video = models.OneToOneField(Video, null=True, blank=True,
                                        on_delete=models.SET_NULL)

    def __unicode__(self):
        return self.title

    @property
    def ordered_sections(self):
        """
        returns an ordered sections queryset.
        """
        return self.section_set.all().order_by('order', 'name')

    @property
    def cocreators(self):
        """
        returns a list of cocreators
        """
        # TODO: This should be unique
        cocreators = [x.assigned for x in self.section_set.exclude(assigned=None)]
        return remove_list_duplicates(cocreators)

    def get_cocreators_string(self):
        """
        returns a string containing unique first name of cocreators
        """
        return ', '.join(set([x.first_name if x.first_name else x.username for x in self.cocreators]))

    @property
    def videos(self):
        """
        returns a list of videos ordered by order, name.
        """
        return [x.video for x in self.section_set.exclude(video=None).order_by('order', 'name')]

    @property
    def available_videos(self):
        """
        returns a list of video slugs that are already encoded and available to merge.
        """
        return [x.slug for x in self.videos if x.get_video_status().web_available]

    @property
    def available_sections(self):
        """
        returns a list of the sections with available videos
        """
        return [x.name for x in self.section_set.exclude(video=None).order_by('order','name') if x.video.get_video_status().web_available]


class Section(models.Model):
    cocreate = models.ForeignKey(CoCreate)
    order = models.PositiveIntegerField()
    name = models.CharField(max_length=255)
    assigned = models.ForeignKey(User, null=True, blank=True,
                                 on_delete=models.SET_NULL)
    video = models.OneToOneField(Video, null=True, blank=True,
                                 on_delete=models.SET_NULL)

    def __unicode__(self):
        return self.name


class ReservedSlug(models.Model):
    slug = models.SlugField(unique=True)
    used = models.BooleanField(default=False)
    
    def __unicode__(self):
        return u'%s: %s' % (self.slug, self.used)

@receiver(pre_save, sender=Section, dispatch_uid="videos.models.Section.assigned")
def assigned_change_handler(sender, instance, **kwargs):
    notify_assigned = False
    try:
        obj = Section.objects.get(pk=instance.pk)
    except Section.DoesNotExist:
        notify_assigned = True
    else:
        if not obj.assigned == instance.assigned: # Field has changed
            notify_assigned = True

    if notify_assigned and instance.assigned:
        send_cocreate_notification(instance.assigned, instance.cocreate.owner,
                                   instance.cocreate.pk, instance.pk)


def send_cocreate_notification(assigned, owner, cocreate_id, section_id):
    """
    Sends invitation for cocreate
    """
    pass

    """
    subject = "Cocreate Invitation"
    if section_id:
        link = reverse('cocreate_section', args=(cocreate_id, section_id))
    else:
        link = reverse('cocreate', args=(cocreate_id,))

    cocreate_link = "http://%s%s" % ( Site.objects.get_current().domain, link)

    message = render_to_string("email/cocreate.txt",
                                {
                                   'user': assigned.get_full_name() or assigned.username,
                                   'owner': owner.get_full_name() or owner.username,
                                   'cocreate_link': cocreate_link,
                                }
                               )
    assigned.email_user(subject, message)
    """


def kill_anonymous_account(request, anonymous_user, an_tok):
    """
    Transfer's an anonymous user's uploaded video to a user account.
    Called when a user first used screenbird anonymously then signs up with a
    new account.
    
    """
    active_user = request.user

    # Get all of the anonymous uploaded videos
    an_videos = Video.objects.filter(uploader=anonymous_user)
    # For each video
    for video in an_videos:
        # Re-assign anonymous to active user
        video.uploader = active_user
        video.save()

    return True

def is_featured(sender, instance, created, **kwargs):
    if instance:
        video = instance
        if video.featured:
            obj, created = FeaturedVideo.objects.get_or_create(video=video)
        else:
            try:
                obj = FeaturedVideo.objects.get(video=video)
            except FeaturedVideo.DoesNotExist:
                pass
            else:
                obj.delete()

def delete_featured(sender, instance, **kwargs):
    video = instance
    try:
        obj = FeaturedVideo.objects.get(video=video)
    except FeaturedVideo.DoesNotExist:
        pass
    else:
        obj.delete()

def facebook_post_video(sender, instance, created, **kwargs):
    '''Once video is created, if facebook connect and video public is set to True
    Video link would then be posted to Facebook page of user
    '''
    video = instance
    user = instance.uploader
    if user and created:
        profile = UserProfile.objects.get(user=user)
        if profile.facebook_connect and video.is_public:
            user_social = UserSocialAuth.objects.get(user=user, provider='facebook')
            extra_data = eval(str(user_social.extra_data))
            access_token = extra_data['access_token']
            graph = GraphAPI(access_token)
            domain = Site.objects.get_current().domain
            url = 'http://%s/%s' % (domain, video.slug)
            img_url = 'http://%s/%s' % (domain, 'media/gfx/logo.png')
            graph.post(
                path = 'me/links',
                picture = img_url,
                message = "Hi Guys! Posted a new video, %s. Check it out on the AWESOME %s site!" % (video.title, Site.objects.get_current().name),
                link = url,
            )

def twitter_post_video(sender, instance, created, **kwargs):
    '''Once video is created, if twitter connect and video public is set to True
    Video link would then be posted to Twitter timeline of user
    '''
    video = instance
    user = instance.uploader
    if user and created:
        profile = UserProfile.objects.get(user=user)
        if profile.twitter_connect and video.is_public:
            user_social = UserSocialAuth.objects.get(user=user, provider='twitter')
            extra_data = eval(str(user_social.extra_data))
            access_tokens = extra_data['access_token']
            access_token_list = access_tokens.split('oauth_token_secret=')[1].split('&oauth_token=')
            secret = access_token_list[0]
            key = access_token_list[1]
            domain = Site.objects.get_current().domain
            url = u'http://%s/%s' % (domain, video.slug)
            # tinyurl'ze the object's link
            create_api = 'http://tinyurl.com/api-create.php'
            data = urllib.urlencode(dict(url=url))
            link = urllib2.urlopen(create_api, data=data).read().strip()
            message = "Hi Guys! Posted a new video, %s. Check it out on the AWESOME %s site! %s" % (video.title, Site.objects.get_current().name, link)
            auth = tweepy.OAuthHandler(settings.TWITTER_CONSUMER_KEY, settings.TWITTER_CONSUMER_SECRET)
            auth.set_access_token(key, secret)
            api = tweepy.API(auth)
            try:
                api.update_status(message)
            except tweepy.TweepError:
                # Usually a duplicate status error
                pass

def notify_owner(sender, instance, created, **kwargs):
    """
    Once a video is created, sends an email to the owner of the account or the uploader if he/she didn't
    use another user's recording link.
    
    """
    if created:
        video = instance
        channel = None
        if video.channel_id:
            channel = channel.objects.get(id=video.channel_id)
        subject = "New video uploaded!"
        from_mail = settings.DEFAULT_FROM_EMAIL
        user = User.objects.get(id=video.uploader_id)
        video_link = "http://%s/%s" % ( Site.objects.get_current().domain, video.slug )

        message = render_to_string("email/collaborate.txt", {
           'user': user,
           'video': video,
           'video_link': video_link,
           'channel': channel
        })
        send_mail(subject, message, from_mail, [user.email], fail_silently=False)
        
def remove_list_duplicates(seq):
    seen = set()
    seen_add = seen.add
    return [ x for x in seq if x not in seen and not seen_add(x)]

# Posts to facebook
post_save.connect(facebook_post_video, Video, dispatch_uid="videos.models")
# Posts to twitter
post_save.connect(twitter_post_video, sender=Video)

post_save.connect(is_featured, sender=Video)
post_delete.connect(delete_featured, sender=Video)
#post_save.connect(notify_owner, sender=Video)


class PopUpInformation(models.Model):
    html_id = models.CharField(max_length=100)
    message = models.TextField()

    def __unicode__(self):
        return self.html_id
