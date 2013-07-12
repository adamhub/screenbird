import boto
import datetime
import logging
import math
import os
import pika
import re
import simplejson
import string
import subprocess
import time
from decimal import Decimal
from boto.s3.key import Key

from django.conf import settings
from django.contrib.auth.models import User
from django.core import mail
from django.core.management.base import NoArgsCommand
from django.core.urlresolvers import reverse
from django.test import TestCase

from amazon.utils import delete_from_s3, get_from_s3, get_from_s3_to_filename
from amazon.s3 import Connection
from videos.models import Video, VideoOutline, VideoStatus, CoCreate, Section, ReservedSlug
from videos.forms import generate_slug, reserve_slug
from videos.utils import enqueue

SLUG_LENGTH = 1
FILE_LOCATION = settings.FILE_LOCATION
FILE_KEY = settings.FILE_KEY
LONG_VIDEO = settings.LONG_VIDEO
LONG_VIDEO_LENGTH = 5.13
LONG_VIDEO_ENCODING = 0.86
LONG_VIDEO_SIZE = 13584052
SHORT_VIDEO = settings.SHORT_VIDEO
SHORT_VIDEO_LENGTH = 2.15
SHORT_VIDEO_ENCODING = 0.5
SHORT_VIDEO_SIZE = 4212513

handler = logging.StreamHandler()
formatter = logging.Formatter('%(message)s')
handler.setFormatter(formatter)

logger = logging.getLogger('videos_test_suite')
logger.addHandler(handler)
logger.setLevel(logging.DEBUG)

class VideosTest(TestCase):
    fixtures = ['test_site_data.json',
                'test_user_data.json',
                'test_channel_data.json',
                'test_videos_data.json']

    def setUp(self):
        self.admin = User.objects.get(pk = 1) #Admin User
        self.user1 = User.objects.get(pk = 2) #Normal User

        self.video1 = Video.objects.get(pk = 1)
        self.video2 = Video.objects.get(pk = 2)
        self.video3 = Video.objects.get(pk = 3)
        
        #Login first for other views to work
        client = self.client
        url = reverse('login')

        #Connect to View and attempt to login
        response = client.post(url,
            {'username': self.user1.username, 'password': 'password'})

    def test_generate_slug(self):

        """
        Original test was to create all possible 1-character slugs
        except for one ('A') but after trying that out, found out
        that it can take so much time to be able to generate that
        remaining useable slug (but it's definitely possible!).
        Therefore, as a remedy, instead of leaving a single useable
        slug, 10 useable slugs were left, which are the ten digits (0-9).
        """

        #Create all possible slugs with only 1 ascii character
        for char in string.ascii_uppercase:
            Video.objects.create(slug=char, uploader=self.user1)
            
        #Now, test the generate_slug function for length 1
        slug = generate_slug(SLUG_LENGTH)
        
        self.assertTrue(slug in string.digits)
        logger.info('Videos generate_slug() test successful.')
        
    def test_video_list(self):
        #Test the video_list() view
        client = self.client
        url = reverse('videos')
        
        response = client.get(url)
        
        self.assertTrue(response.status_code == 200)
        logger.info('Videos video_list() view test successful.')
        
    def test_user_videos(self):
        #Test the user_videos() view
        client = self.client
        url = reverse('user_videos', kwargs={'username': self.user1.username})
        
        response = client.get(url)
        
        self.assertTrue(response.status_code == 200)
        logger.info('Videos user_videos() view test successful.')
        
    def test_my_video_list(self):
        #Test the my_video_list() view
        client = self.client
        url = reverse('my-videos')
        
        response = client.get(url)
        
        self.assertTrue(response.status_code == 200)
        
        #Assert that the user has three videos already
        self.assertTrue(Video.objects.filter(uploader=self.user1).count() == 3)
        
        #Now try to POST and delete "Video 1"
        response = client.post(url, {'item': [self.video1.pk]})
        
        #Assert now that the user has only two videos remaining ("Video 2" and "Video 3") and "Video 1" is not in the list anymore
        self.assertTrue(Video.objects.filter(uploader=self.user1).count() == 2)
        self.assertTrue(self.video2 in Video.objects.filter(uploader=self.user1))
        self.assertTrue(self.video1 not in Video.objects.filter(uploader=self.user1))
        logger.info('Videos my_video_list() view test successful.')
    
    def test_edit(self):
        #Test the edit() view
        client = self.client
        url = reverse('my-videos-edit', kwargs={'slug': self.video1.slug})
        
        #Assert first that the values for "Test Video 1" are as expected
        self.assertTrue(self.video1.title == 'Test Video 1')
        self.assertTrue(self.video1.description == 'Test Video 1')
        self.assertTrue(self.video1.is_public)
        
        #Update the video object
        response = client.post(url,
            {
                'title': 'New Title for Test Video 1',
                'description': 'New Description for Test Video 1',
                'is_public': False,
            })
            
        self.assertTrue(response.status_code == 200)
        self.assertTrue('message' in response.context and response.context['message'] == 'Video succesfully updated.')
        #Get the updated video object
        video1 = Video.objects.get(pk = 1)
        #Assert now that the video object has been updated as expected
        self.assertTrue(video1.title == 'New Title for Test Video 1')
        self.assertTrue(video1.description == 'New Description for Test Video 1')
        self.assertFalse(video1.is_public)
        logger.info('Videos edit() view test successful.')
    
    def test_view(self):
        #Test the view() view
        client = self.client
        url = reverse('watch-video', kwargs={'slug': self.video1.slug})
        
        response = client.get(url, {}, HTTP_HOST=url, HTTP_USER_AGENT='')
        
        self.assertTrue(response.status_code == 200)
        self.assertTrue('video' in response.context and response.context['video'])
        self.assertTrue('video_status' in response.context and response.context['video_status'] == 'OK')
        logger.info('Videos view() view test successful.')
    
    def test_upload(self):
        #Uploads video to s3, checks if it was rendered properly, and deletes it from s3.
        #This is a special test with no assertions but instead raises Exceptions to detail the error that occurred.
        
        def upload_to_s3(key, filename):
            #similar to send_to_s3 except for the make_public() step
            try:
                conn = boto.connect_s3(settings.AWS_ACCESS_KEY_ID, settings.AWS_SECRET_ACCESS_KEY)
                bucket = conn.get_bucket(settings.AWS_VIDEO_BUCKET_NAME)
                
                k = Key(bucket)
                k.key = key
                k.set_contents_from_filename(filename)
                k.make_public()
            except Exception, e:
                raise AssertionError("Failed to upload to s3: %s" % e)

        def check_from_s3(key):
            video = get_from_s3(key)
            if video:
                logger.info("Video is in s3.")
                return 'Video is in s3'
            else:
                logger.info("Video is not in s3.")
                return 'Video is not in s3'
                
        def is_encoded(slug):
            video_status = VideoStatus.objects.get(video_slug=slug)
            return video_status.web_available

        # Initialize subtest results
        short_video_size_passed = False
        short_video_duration_passed = False
        short_video_encode_time_passed = False
        long_video_size_passed = False
        long_video_duration_passed = False
        long_video_encode_time_passed = False

        logger.info("Starting Video Encoding Test.")

        #check if rabbitmq is working
        try:
            connection = pika.BlockingConnection(pika.ConnectionParameters(settings.RABBITMQ_SERVER))
        except Exception, e:
            raise AssertionError("Cannot connect to rabbitmq server: %s" % e)

        # Get uploader
        uploader, created = User.objects.get_or_create(username='encodetester')

        # Get video
        slug = 'LONGENCODE'
        title = "Encode Test A"
        description = "5-minute video with audio"
        video = Video(title=title, slug=slug, uploader=uploader, description=description)
        video.save()
        logger.info("Video slug: %s" % slug)
        slug_tmp = slug + '_tmp'

        logger.info("Uploading file to s3...")
        upload_to_s3(slug_tmp, LONG_VIDEO)
        logger.info("Upload process successful.")
        
        #should indicate that video was uploaded and is stored in s3
        logger.info("Checking if video is already in s3 (should be there)...")
        check = check_from_s3(slug_tmp)
        if check != 'Video is in s3':
            raise AssertionError("Video was not uploaded to s3 properly.")
            
        logger.info("Enqueuing slug to rabbitmq...")
        enqueue(slug_tmp)
        logger.info("Enqueue process successful.")
            
        # Mark reserved slug as used
        try:
            reserved_slug = ReservedSlug.objects.get(slug=slug)
        except:
            pass
        else:
            reserved_slug.used = True
            reserved_slug.save()

        # Simulating waiting for encoding to finish
        logger.info("Waiting for encoding to finish.")
        wait_time = math.ceil((LONG_VIDEO_ENCODING * 2) * 60)
        logger.info("ETA: %s" % wait_time)
        time.sleep(wait_time)
        check = check_from_s3(slug)
        if check != 'Video is in s3':
            raise AssertionError('Long video encoding timed out.')
        else:
            logger.info("Encoding successful.")

        #check if an expiring url can be generated, similar to how Screenbird gets url for video playing
        logger.info("Checking generation of expiring url and video rendering...")
        try:
            conn = Connection(is_secure=True)
            url = conn.generate_object_expiring_url(slug)
            if not url:
                raise AssertionError("Failed to generate expiring url and render video.")
        except Exception, e:
            raise AssertionError("Failed to check for an expiring url: %s" % e)
        logger.info("Expiring url successfully generated and video rendered.")
           
        # Checks
        video = Video.objects.get(slug=slug)
        video_duration = video.video_duration
        encoded_filename = 'longvideo.mp4'
        get_from_s3_to_filename(slug, encoded_filename)
        if os.path.isfile(encoded_filename):
            process = subprocess.Popen(['ffmpeg',  '-i', encoded_filename], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            stdout, stderr = process.communicate()
            matches = re.search(r"Duration:\s{1}(?P<hours>\d+?):(?P<minutes>\d+?):(?P<seconds>\d+\.\d+?),", stdout, re.DOTALL).groupdict()
            h = float(matches['hours'])
            m = float(matches['minutes'])
            s = float(matches['seconds'])
            total_seconds = h*3600 + m*60 + s
            total_mins = total_seconds / 60.0
            logger.info("Total minutes: %s" % total_mins)
        else:
            raise AssertionError("File did not download properly")
        self.assertTrue(Decimal(str(total_mins)) >= Decimal(str(LONG_VIDEO_LENGTH)))
        logger.info("Long video duration check passed.")

        """
        video_status = VideoStatus.objects.get(video_slug=slug)
        duration_difference = math.fabs(Decimal(str(video_status.encode_duration)) - Decimal(str(LONG_VIDEO_ENCODING)))
        percent_error = (duration_difference / LONG_VIDEO_ENCODING) * 100
        self.assertTrue(percent_error < 30.0)
        logger.info("Long video encoding time check passed.")
        """

        video_file_len = len(get_from_s3(slug))
        size_difference = math.fabs(video_file_len - LONG_VIDEO_SIZE)
        percent_error = (size_difference / LONG_VIDEO_SIZE) * 100
        logger.info("Percent error: %s" % percent_error)
        self.assertTrue(percent_error <= 10.0)
        logger.info("Long video encoded size check passed.")

        # Get video
        slug = 'SHORTENCODE'
        title = "Encode Test B"
        description = "2-minute video with no audio"
        video = Video(title=title, slug=slug, uploader=uploader, description=description)
        video.save()
        logger.info("Video slug: %s" % slug)
        slug_tmp = slug + '_tmp'

        logger.info("Uploading file to s3...")
        upload_to_s3(slug_tmp, SHORT_VIDEO)
        logger.info("Upload process successful.")
        
        #should indicate that video was uploaded and is stored in s3
        logger.info("Checking if video is already in s3 (should be there)...")
        check = check_from_s3(slug_tmp)
        if check != 'Video is in s3':
            raise AssertionError("Video was not uploaded to s3 properly.")
            
        logger.info("Enqueuing slug to rabbitmq...")
        enqueue(slug_tmp)
        logger.info("Enqueue process successful.")
            
        # Mark reserved slug as used
        try:
            reserved_slug = ReservedSlug.objects.get(slug=slug)
        except:
            pass
        else:
            reserved_slug.used = True
            reserved_slug.save()

        # Simulating waiting for encoding to finish
        logger.info("Waiting for encoding to finish.")
        wait_time = math.ceil((SHORT_VIDEO_ENCODING * 2) * 60)
        logger.info("ETA: %s" % wait_time)
        time.sleep(wait_time)
        check = check_from_s3(slug)
        if check != 'Video is in s3':
            raise AssertionError('Short video encoding timed out.')
        else:
            logger.info("Encoding successful.")

        #check if an expiring url can be generated, similar to how Screenbird gets url for video playing
        logger.info("Checking generation of expiring url and video rendering...")
        try:
            conn = Connection(is_secure=True)
            url = conn.generate_object_expiring_url(slug)
            if not url:
                raise AssertionError("Failed to generate expiring url and render video.")
        except Exception, e:
            raise AssertionError("Failed to check for an expiring url: %s" % e)
        logger.info("Expiring url successfully generated and video rendered.")
           
        # Checks
        video = Video.objects.get(slug=slug)
        video_duration = video.video_duration
        encoded_filename = 'shortvideo.mp4'
        get_from_s3_to_filename(slug, encoded_filename)
        if os.path.isfile(encoded_filename):
            process = subprocess.Popen(['ffmpeg',  '-i', encoded_filename], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            stdout, stderr = process.communicate()
            matches = re.search(r"Duration:\s{1}(?P<hours>\d+?):(?P<minutes>\d+?):(?P<seconds>\d+\.\d+?),", stdout, re.DOTALL).groupdict()
            h = float(matches['hours'])
            m = float(matches['minutes'])
            s = float(matches['seconds'])
            total_seconds = h*3600 + m*60 + s
            total_mins = total_seconds / 60.0
            logger.info("Total minutes: %s" % total_mins)
        else:
            raise AssertionError("File did not download properly")
        self.assertTrue(Decimal(str(total_mins)) >= Decimal(str(SHORT_VIDEO_LENGTH)))
        logger.info("Short video duration check passed.")

        """
        video_status = VideoStatus.objects.get(video_slug=slug)
        duration_difference = math.fabs(Decimal(str(video_status.encode_duration)) - Decimal(str(SHORT_VIDEO_ENCODING)))
        percent_error = (duration_difference / SHORT_VIDEO_ENCODING) * 100
        self.assertTrue(percent_error < 30.0)
        logger.info("Short video encoding time check passed.")
        """

        video_file_len = len(get_from_s3(slug))
        size_difference = math.fabs(video_file_len - SHORT_VIDEO_SIZE)
        percent_error = (size_difference / SHORT_VIDEO_SIZE) * 100
        logger.info("Percent error: %s" % percent_error)
        self.assertTrue(percent_error <= 10.0)
        logger.info("Short video encoded size check passed.")

        logger.info('Test for uploading video to s3, video rendering, and encoding video successful.')
    
    def test_save_outline(self):
        #Test the save_outline() view
        client = self.client
        url = reverse('save-outline', kwargs={'slug': self.video1.slug})
        
        pins = {
                    'pin1': {
                        'text': 'Outline 1',
                        'position': 1
                    },
                    'pin2': {
                        'text': 'Outline 2',
                        'position': 2
                    },
                    'pin3': {
                        'text': 'Outline 3',
                        'position': 3
                    }
                }
        pins = simplejson.dumps(pins)
        response = client.post(url, {'pins': pins})
        
        self.assertTrue(response.content == 'OK')
        #check if the VideoOutline object was created properly
        video_outline = VideoOutline.objects.get(video=self.video1)
        self.assertTrue(video_outline.videooutlinepin_set.exists())
        #check if the added outline texts are present
        for pin in video_outline.videooutlinepin_set.all():
            self.assertTrue(pin.text in ('Outline 1', 'Outline 2', 'Outline 3'))
        logger.info('Videos save_outline() view test successful.')
    
    def test_get_outline(self):
        #Test the get_outline() view
        client = self.client
        url = reverse('get-outline', kwargs={'slug': self.video1.slug})
        
        response = client.post(url)
        
        self.assertTrue(response.status_code == 200)
        self.assertTrue(response._headers['content-type'][1] == 'application/json')
        logger.info('Videos get_outline() view test successful.')
    
    def test_approval_link(self):
        #Test the approval_link() view
        client = self.client
        url = reverse('approval-link', kwargs={'slug': self.video1.slug})
        
        #Check for a video with channel
        response = client.get(url)
        
        self.assertTrue(response.status_code == 200)
        self.assertTrue('video' in response.context and response.context['video'])
        
        #Check for a video with no channel
        url = reverse('approval-link', kwargs={'slug': self.video3.slug})
        
        response = client.get(url)
        self.assertTrue(response.status_code == 404)
        self.assertFalse('video' in response.context and response.context['video'])
    
    def test_confirm_delete_link(self):
        #Test the confirm_delete_link() view
        client = self.client
        url = reverse('confirm-delete-link', kwargs={'slug': self.video1.slug})
        
        response = client.get(url)
        
        self.assertTrue(response.status_code == 200)
        self.assertTrue('action' in response.context and response.context['action'] == 'delete')
    
    def test_approve(self):
        #Test the approve() view
        client = self.client
        #Publish the video
        url = reverse('approve-video', kwargs={'slug': self.video1.slug}) + '?decision=publish'
        response = client.get(url)
        
        self.assertTrue(response.status_code == 200)
        self.assertTrue('action' in response.context and response.context['action'] == 'approve')
        self.assertTrue('mode' in response.context and response.context['mode'] == 'publish')
        self.assertTrue('video' in response.context and not response.context['video'])
        
        #Deny the video
        url = reverse('approve-video', kwargs={'slug': self.video1.slug}) + '?decision=deny'
        response = client.get(url)
        
        self.assertTrue(response.status_code == 200)
        self.assertTrue('action' in response.context and response.context['action'] == 'approve')
        self.assertTrue('mode' in response.context and response.context['mode'] == 'deny')
        #Get the denied video and check if it's active and its expiry
        video1 = Video.objects.get(pk=1)
        self.assertFalse(video1.is_active)
        self.assertTrue(video1.expiry_date.date() == datetime.datetime.now().date())
        
        #Send an invalid decision
        url = reverse('approve-video', kwargs={'slug': self.video1.slug}) + '?decision=invalid'
        response = client.get(url)
        
        self.assertTrue(response.status_code == 200)
        self.assertTrue('action' in response.context and response.context['action'] == 'approve')
        self.assertTrue('mode' in response.context and response.context['mode'] == 'invalid')
        self.assertTrue('video' in response.context and response.context['video'])
    
    def test_delete(self):
        #Test the delete() view
        client = self.client
        url = reverse('delete-video', kwargs={'slug': self.video1.slug})
        
        response = client.get(url)
        #Confirm that the video has been deleted
        self.assertFalse(Video.objects.filter(pk=1).exists())
    
    def test_ajax_video_is_available(self):
        #Test the ajax_video_is_available() view
        client = self.client
        #Test if available via web
        url = reverse('is-available', kwargs={'slug': self.video1.slug, 'version': 'web'})
        response = client.get(url)
        self.assertTrue(response.cookies['available'].value)
        
        #Test if available via mobile
        url = reverse('is-available', kwargs={'slug': self.video1.slug, 'version': 'mobile'})
        response = client.get(url)
        self.assertTrue(response.cookies['available'].value)
    
    def test_set_to_available(self):
        #Test the set_to_available() view
        client = self.client
        
        #Get a video that is not available via web and mobile
        video3 = Video.objects.get(pk=3)
        videostatus3 = VideoStatus.objects.get(video_slug=video3.slug)
        self.assertFalse(videostatus3.web_available)
        self.assertFalse(videostatus3.mobile_available)
        self.assertTrue(videostatus3.is_encoding)
        
        #Set to available by visiting the link for web version...
        url = reverse('set-available', kwargs={'slug': video3.slug, 'version': 'web'})
        response = client.get(url)
        #and for mobile version
        url = reverse('set-available', kwargs={'slug': video3.slug, 'version': 'mobile'})
        response = client.get(url)
        
        #Check if the video is available via web and mobile now
        videostatus3 = VideoStatus.objects.get(video_slug=video3.slug)
        self.assertTrue(videostatus3.web_available)
        self.assertTrue(videostatus3.mobile_available)
        self.assertFalse(videostatus3.is_encoding)
        
    def test_change_recording_link(self):
        #Test the change_recording_link() view
        client = self.client
        
        #Store recorder link of user1 to be compared later
        current_recorder_link = self.user1.userprofile.recorder_link
        
        url = reverse('change-rlink', kwargs={'user_id': self.user1.id})
        response = client.get(url)
        
        #Get the new recorder link
        new_recorder_link = response.content.split('?r=')[1]
        #Compare to make sure they are not the same and the recoreder link has been updated properly
        self.assertFalse(current_recorder_link == new_recorder_link)
    
    def test_video_page_embed_code(self):
        #Test the video_page_embed_code() view
        client = self.client
        
        url = reverse('video-page-embed-code', kwargs={'user_id': self.user1.id})
        response = client.get(url)
        self.assertTrue(response.status_code == 200)

class CocreateTest(TestCase):
    fixtures = ['test_site_data.json', 'test_cocreate.json',]

    def setUp(self):
        #Login first for other views to work
        client = self.client
        client.defaults["HTTP_USER_AGENT"] = "Mozilla/5.0"
        client.login(username='durianslasher', password='password')

    def test_add_cocreate(self):
        """
        Test add cocreate view.
        """
        client = self.client
        url = reverse('cocreate_new')
        # check get
        response = client.get(url)
        self.assertEqual(response.status_code, 200)

        # check post
        response = client.post(url, {'title': 'Cocreate Test',
                                     'description': 'short description',
                                     'notes': 'upload now'})
        self.assertEqual(response.status_code, 302)
        
        logger.info('Add cocreate successful.')

    def test_add_section(self):
        """
        Test add section view.
        """
        client = self.client

        # check get
        url = reverse('cocreate_newsection', args=(3,))
        response = client.get(url)
        self.assertEqual(response.status_code, 200)

        # check normal post
        response = client.post(url, {'name': 'Section Test',
                                     'order': '0',
                                     'assigned': ''})
        self.assertEqual(response.status_code, 302)

        # check ajax post
        response = client.post(url, {'name': 'Section Test',
                                     'order': '0',
                                     'assigned': ''}, HTTP_X_REQUESTED_WITH='XMLHttpRequest')
        self.assertEqual(response.status_code, 200)

        """
        # check email notification
        response = client.post(url, {'name': 'Section Test',
                                     'order': '0',
                                     'assigned': '1'}, HTTP_X_REQUESTED_WITH='XMLHttpRequest')
        self.assertEqual(response.status_code, 200)

        # check if outbox has received the notification
        self.assertEqual(len(mail.outbox), 1)
        """

        logger.info('Add cocreate section successful.')

    def test_index(self):
        """
        Test cocreate index page
        """
        client = self.client

        # check landing page
        url = reverse('cocreate', args=(3,))
        response = client.get(url)
        self.assertEqual(response.status_code, 200)

        # check section with video
        url = reverse('cocreate_section', args=(3, 1))
        response = client.get(url)
        self.assertEqual(response.status_code, 200)
        context = response.context
        self.assertEqual(context['selected_index'], u'1')
        self.assertEqual(context['video_status'], 'WAIT')

        # check section within limit without video
        url = reverse('cocreate_section', args=(3, 2))
        response = client.get(url)
        self.assertEqual(response.status_code, 200)
        context = response.context
        self.assertEqual(context['selected_index'], u'2')
        self.assertEqual(context['video_status'], 'DNE')

        # check section outside the limit
        url = reverse('cocreate_section', args=(3, 100))
        response = client.get(url)
        self.assertEqual(response.status_code, 200)
        context = response.context
        self.assertEqual(context['selected'], None)
        self.assertEqual(context['video_status'], 'NONE')

    def test_edit(self):
        """
        Test cocreate edit.
        """
        client = self.client
        test = 'Test'
        # check notes
        url = reverse('cocreate_edit', args=(3, 'notes'))
        response = client.post(url, {'value': test})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.content, test)
        obj = CoCreate.objects.get(pk=3)
        self.assertEqual(obj.notes, test)

        # check description
        url = reverse('cocreate_edit', args=(3, 'description'))
        response = client.post(url, {'value': test})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.content, test)
        obj = CoCreate.objects.get(pk=3)
        self.assertEqual(obj.description, test)

    def test_section_edit(self):
        """
        Test section edit.
        """
        client = self.client
        test = 'Test'

        # check name
        url = reverse('section_edit', args=(26, 'name'))
        response = client.post(url, {'value': test,})
        self.assertEqual(response.status_code, 200)
        obj = Section.objects.get(pk=26)
        self.assertEqual(obj.name, test)

    def test_cocreate_compile(self):
        """
        Test cocreate compile
        """
        client = self.client

        cocreate = CoCreate.objects.all()[0]
        url = reverse('cocreate_compile', args=(cocreate.id,))
        response = client.post(url)
        self.assertEqual(response.status_code, 200)

    def test_update_outline(self):
        """
        Test update outline.
        """
        client = self.client
        from decimal import Decimal

        cocreate = CoCreate.objects.all()[0]
        video = Video.objects.all()[0]
        cocreate.output_video = video
        cocreate.save()
        outline = VideoOutline.objects.create(video=video)
        asections = ['part 1', 'part 2', 'part 3']
        for i in xrange(len(asections)):
            outline.videooutlinepin_set.create(text=asections[i],
                                               current_time=Decimal(str(i)))



        # check name
        url = reverse('update_cocreate_outline', args=(video.slug,))
        response = client.post(url, {'data': '[0.0, 5.0, 10.0]',})
        self.assertEqual(response.status_code, 200)
