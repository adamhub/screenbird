try:
    import cStringIO as StringIO
except ImportError:
    import StringIO

import boto
import logging
import os
import pika
import re
import random
import string
import subprocess
import time
import zipfile

from django import forms
from django.forms.widgets import RadioSelect, Select
from django.conf import settings
from django.contrib.auth.models import User

from accounts.models import UserProfile
from amazon.utils import send_to_s3
from videos.models import (
    Channel,
    ChannelOption,
    CoCreate,
    CocreateOption,
    ConfirmationInfo,
    ReservedSlug,
    Section,
    Video,
    VideoStatus,
)
from videos.utils import get_video_length, add_total_upload_time, enqueue
from tasks import encode_video, encode_mobile_video

logger = logging.getLogger('videos.views')
SLUG_LENGTH = settings.VIDEO_SLUG_LENGTH


def handle_uploaded_file(video, slug):
    """
    Handler for uploaded video via the java recorder.
    Sends the video to s3 and queues it for encoding on the ec2 nodes.
    
    """
    try:
        connection = pika.BlockingConnection(
                pika.ConnectionParameters(settings.RABBITMQ_SERVER))
    except:
        logger.exception('connection problem')
        raise forms.ValidationError("We are experiencing a connection problem "
                "and cannot perform uploads for now. Please try again later.")
    filename = os.path.join(settings.MEDIA_ROOT,'tmp/%s.mp4' % slug)
    logger.debug('Opening file %s' % filename)
    tmp_file = open(filename, 'wb+')
    logger.debug('file opened')
    tmp_slug = "%s_tmp" % slug
    for chunk in video.chunks():
        tmp_file.write(chunk)
    tmp_file.close()
    logger.debug('sending to s3')

    if settings.PUSH_TO_S3:
        send_to_s3(tmp_slug, filename)
    logger.debug('done sending')

    logger.debug('enqueuing file')
    enqueue(tmp_slug)
    logger.debug('done enqueue')

    logger.debug('removing file')
    # Remove the file from the server after being uploaded to s3 and 
    # queued for encoding
    if settings.PUSH_TO_S3:
        os.remove(filename)
    logger.debug('done removing')

def generate_slug(length):
    """
    Generates a unique slug for videos.
    
    """
    while True:
        slug = generate_random_string(length)
        try:
            Video.objects.get(slug = slug)
        except Video.DoesNotExist:
            try:
                VideoStatus.objects.get(video_slug = slug)
            except VideoStatus.DoesNotExist:
                return slug

def reserve_slug(length):
    while True:
        slug = generate_random_string(length)
        reserved = ReservedSlug.objects.filter(slug=slug).exists()
        if not reserved:
            reserved_slug = ReservedSlug(slug=slug, used=False)
            reserved_slug.save()
            return slug

def generate_channel_slug(length):
    """
    Generates a unique slug for channels.
    
    """
    while True:
        slug = generate_random_string(length)
        try:
            Channel.objects.get(channel_slug = slug)
        except Channel.DoesNotExist:
            return slug

def generate_random_string(length):
    """
    Random string generator for slugs and passwords
    
    http://stackoverflow.com/questions/2257441/python-random-string-generation-with-upper-case-letters-and-digits
    
    """
    unique_string = ''.join(random.choice(string.ascii_uppercase + string.digits) for x in range(length))
    return unique_string


class YoutubeUploadForm(forms.Form):
    CATEGORY_CHOICES = (('Tech', 'Tech'),
                        ('Education', 'Education'),
                        ('Animals', 'Animals'),
                        ('People', 'People'),
                        ('Travel', 'Travel'),
                        ('Entertainment', 'Entertainment'),
                        ('Howto', 'Howto'),
                        ('Sports', 'Sports'),
                        ('Autos', 'Autos'),
                        ('Music', 'Music'),
                        ('News', 'News'),
                        ('Games', 'Games'),
                        ('Nonprofit', 'Nonprofit'),
                        ('Comedy', 'Comedy'),
                        ('Film', 'Film'))
    PRIVACY_CHOICES = [['unlisted','Unlisted'], ['public', 'Public']]

    title = forms.CharField(required=True, max_length=100)
    description = forms.CharField(widget=forms.Textarea)
    category = forms.ChoiceField(choices=CATEGORY_CHOICES, widget=forms.Select(attrs={'class':'full'}))
    privacy = forms.ChoiceField(required = True, widget=RadioSelect(), choices=PRIVACY_CHOICES)
    keywords = forms.CharField(required=False, max_length=100)

    def upload(self):
        pass


class VideoUploadForm(forms.ModelForm):
    """
    Video upload form used by the Java recorder.
    
    """
    videoupload = forms.FileField(required=True, help_text="Only the video that was produced by the PasteVid recording software will work.",
                                  error_messages={'required': 'Please upload the video which the PasteVid recorder has produced.'})

    def clean_videoupload(self):
        """
        Checks filetype of uploaded video. Only mp4's are accepted
        
        Raises a `ValidationError` if no file is uploaded or any other filetype
        is uploaded.
        
        """
        logger.debug('start: videoupload')
        video_upload = self.cleaned_data.get('videoupload',False)
        if video_upload:
            logger.debug('cleaned videoupload')
            if not video_upload.content_type in ["video/mp4","video/quicktime","video/..."]:
                logger.error('content type is not a valid mp4 video')
                raise forms.ValidationError("Content type is not a valid mp4 video: %s" % video_upload.content_type)
            if not os.path.splitext(video_upload.name)[1] in [".mp4",]:
                logger.error('uploaded file is not mp4')
                raise forms.ValidationError("Uploaded file is not mp4")
        else:
            logger.error('could not read uploaded file')
            raise forms.ValidationError("Couldn't read uploaded file")
        return video_upload

    def clean(self):
        data = cleaned_data = super(VideoUploadForm, self).clean()
        logger.debug('passing videoupload to handle_uploaded_file')
        video_upload = data.get('videoupload', None)
        if not video_upload:
            raise forms.ValidationError("could not read uploaded file")
        handle_uploaded_file(video_upload, data.get('slug'))
        logger.debug('done')
        return data

    class Meta:
        model = Video
        exclude = (
            'uploader', 
            'created', 
            'updated', 
            'video_duration', 
            'is_active', 
            'videoupload',
        )
        widgets = {
            'description': forms.Textarea(attrs={'cols': 40, 'rows': 20}),
        }


class VideoEditForm(forms.ModelForm):
    """
    Form for editing the following video properties
    - `title`
    - `is_public`
    - `description`
    
    """
    def __init__(self, *args, **kwargs):
        super(VideoEditForm, self).__init__(*args, **kwargs)
        for myField in self.fields:
            self.fields[myField].widget.attrs['class'] = 'input-text'

    class Meta:
        model = Video
        exclude = (
            'uploader', 
            'created', 
            'updated', 
            'video_type',
            'videoupload', 
            'slug',
            'video_duration',
        )
        fields = (
            'title', 
            'is_public', 
            'description',
        )
        widgets = {
            'description': forms.Textarea(attrs={'cols': 40, 'rows': 20}),
        }


class InvitationForm(forms.Form):
    """
    Form for inviting a user to a channel.
    
    """
    def __init__(self, *args, **kwargs):
        super(InvitationForm, self).__init__(*args, **kwargs)
        for myField in self.fields:
            self.fields[myField].widget.attrs['class'] = 'input-text'

    email = forms.EmailField(required=True)


class ChannelForm(forms.ModelForm):
    """
    Form for creating a channel.
    
    """
    def __init__(self, *args, **kwargs):
        super(ChannelForm, self).__init__(*args, **kwargs)
        for myField in self.fields:
            self.fields[myField].widget.attrs['class'] = 'input-text'
            
    class Meta:
        model = Channel
        exclude  = ('owner','api_link','channel_slug')


class UpdateChannelForm(forms.ModelForm):
    """
    Form for updating channel information.
    
    """
    def __init__(self, *args, **kwargs):
        super(UpdateChannelForm, self).__init__(*args, **kwargs)
        for myField in self.fields:
            self.fields[myField].widget.attrs['class'] = 'input-text'
            if myField == 'api_link' \
               and not (self.instance.owner.userprofile.is_using_trial and self.instance.owner.userprofile.is_paid):
                self.fields[myField].required = False
            
    class Meta:
        model = Channel
        exclude  = ('owner',)
    

    def is_valid(self):
        """
        Checks for duplicate `api_link` among Channels. A channel is valid if
        there are no duplicates found.
        
        """
        try:
            channel = Channel.objects.filter(api_link= self.data['api_link'])[:1:]
            if channel and not (channel == self.instance):
                return False
        except:
            return True
        return True


class ChannelOptionForm(forms.Form):
    """
    Form for managing channel members' permissions.
    Required arguments:
    - `channel` : the channel on which the permission will be used
    - `userprofile` : the user's profile on which the permission will apply
    """
    needs_approval = forms.BooleanField(required=False)

    def __init__(self, channel, user_profile, *args, **kwargs):
        """
        Set initial value for the `needs_approval` field based on the input
        Channel and UserProfile objects.
        
        """
        self.channel = channel
        self.user_profile = user_profile
        super(ChannelOptionForm, self).__init__(*args, **kwargs)
        try:
            self.channel_option = ChannelOption.objects.get(channel=channel,
                    user_profile=user_profile)
        except ChannelOption.DoesNotExist:
            self.channel_option = ChannelOption(channel=channel,
                    user_profile=user_profile, needs_approval=False)
            self.channel_option.save()
        self.initial['needs_approval'] = self.channel_option.needs_approval
        self.fields['needs_approval'].label = user_profile.user.email

    def save(self, commit=True):
        self.channel_option.needs_approval = self.cleaned_data.get('needs_approval', False)
        if commit:
            self.channel_option.save()
        return self.channel_option


class ManualUploadForm(forms.Form):
    """
    Form for manual video uploads.
    Optional keyword argument:
    - `user` : the currently logged-in user
    
    """
    title = forms.CharField(max_length=100, 
                            help_text="A nice title for the video clip")
    description = forms.CharField(max_length=250, 
                                  help_text="A short description about your video", 
                                  widget=forms.Textarea, 
                                  required=False)
    is_public = forms.BooleanField(required=False)
    slug = forms.CharField(max_length=8, widget=forms.HiddenInput)
    channel = forms.ModelChoiceField(queryset=Channel.objects.none())

    def __init__(self, *args, **kwargs):
        """
        Initialise choices for `channel` field based on existing Channel objects
        for the logged in user, if any.
        
        """
        user = kwargs.pop("user", None)
        super(ManualUploadForm, self).__init__(*args, **kwargs)
        if user:
            self.fields['channel'] = forms.ModelChoiceField(
                    queryset=user.userprofile.channels.all(), required=False)


class CoCreateForm(forms.ModelForm):
    class Meta:
        model = CoCreate
        exclude = ('owner','output_video')


class SectionForm(forms.ModelForm):
    def __init__(self, *args, **kwargs):
        default_cocreate = kwargs.pop('default_cocreate')
        super(SectionForm, self).__init__(*args, **kwargs)
        self.fields['assigned'].empty_label = "Unassigned"
        default_order = Section.objects.filter(cocreate=default_cocreate).count()
        self.fields['order'].initial = default_order + 1
        sections = Section.objects.filter(cocreate__owner=default_cocreate.owner)
        channel_options = ChannelOption.objects.filter(channel__owner=default_cocreate.owner)
        cocreate_options = CocreateOption.objects.filter(cocreate__owner=default_cocreate.owner)
        section_assigned_ids = set()
        section_assigned_ids.add(default_cocreate.owner.id)
        for section in sections:
            if section.assigned: section_assigned_ids.add(section.assigned.id)
        for channel_option in channel_options:
            if channel_option.user_profile: section_assigned_ids.add(channel_option.user_profile.user.id)
        for cocreate_option in cocreate_options:
            if cocreate_option.user: section_assigned_ids.add(cocreate_option.user.id)

        queryset = User.objects.filter(id__in=section_assigned_ids)
        self.fields['assigned'].queryset = queryset
        choices = list(self.fields['assigned'].choices)
        choices.append((u'-1', u'Add Member'))
        self.fields['assigned'].choices = choices

    class Meta:
        model = Section
        exclude = ('cocreate', 'video')
        widgets = {
            'order': forms.TextInput(attrs={'placeholder': 'No.'}),
            'name': forms.TextInput(attrs={'placeholder': 'Section Title'}),
        }
