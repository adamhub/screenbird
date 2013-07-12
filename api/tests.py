"""
This file demonstrates writing tests using the unittest module. These will pass
when you run "manage.py test".

Replace this with more appropriate tests for your application.
"""

import urllib2, logging
import simplejson as json
from django.test import TestCase
from django.core.urlresolvers import reverse
from django.contrib.auth.models import User
from videos.models import Video, Channel
from accounts.models import UserProfile
from django.test import Client
from django.db.models import signals
from django.dispatch import Signal
from django.core.handlers.wsgi import WSGIRequest
from api.utils import generate_api_key

handler = logging.StreamHandler()
formatter = logging.Formatter('%(message)s')
handler.setFormatter(formatter)

logger = logging.getLogger('api_test_suite')
logger.addHandler(handler) 
logger.setLevel(logging.DEBUG)
API_KEY_LEN = 12
class ApiTest(TestCase):
   fixtures = ['test_site_data.json',
                'test_user_data.json',
                'test_channel_data.json',
                'test_videos_data.json']

   def setUp(self):
        self.user1 = User.objects.get(pk = 2)
        self.api_key = self.user1.userprofile.api_key

   def test_invalid_api_key(self):
        url = reverse('get-all-videos')
        rs = self.client.get(url,{'api_key':"a"})
        self.assertEqual(rs.status_code,  401)
        logger.info('Api invalid_api_key test successful.')
    
   def test_no_api_key(self):
        url = reverse('get-all-videos')
        rs = self.client.get(url)
        self.assertEqual(rs.status_code,  400)
        logger.info('Api no_api_key test successful.')

   def test_get_all_videos(self):
        url = reverse('get-all-videos')
        rs = self.client.get(url,{'api_key':self.api_key})
        self.assertEqual(rs.status_code,  200)
        content = rs.content        
        #logger.info(content)
        content_dict = json.loads(content)
        videos_list = content_dict['videos']
        #assert 3 videos
        #check number if same
        self.assertEqual(len(videos_list), 3)
        
        logger.info('Api get_all_videos test successful.')

   def test_get_latest_videos(self):
        url = reverse('get-latest-videos')
        rs = self.client.get(url,{'api_key':self.api_key})
        self.assertEqual(rs.status_code,  200)
        #logger.info(rs.content)
        content = rs.content
        content_dict = json.loads(content)
        videos_list = content_dict['videos']
        latest_video_id_from_api = videos_list[0]['id']
        latest_vid_id_from_query = Video.objects.all().order_by('-created')[0].id
        #check if same id
        self.assertEqual(latest_video_id_from_api, latest_vid_id_from_query)
        logger.info('Api get_latest_videos test successful.')

   def test_get_channel_videos(self):
        api_link = "a"
        url = reverse('get-channel-videos')
        rs = self.client.get(url,{'api_key':self.api_key, 'channel_link': api_link})
        self.assertEqual(rs.status_code,  200)
        content = rs.content
        content_dict = json.loads(content)
        videos_list = content_dict['videos']
        channel = Channel.objects.get(api_link=api_link)
        videos = Video.objects.filter(channel=channel)
        #check number if same
        self.assertEqual(len(videos_list), videos.count())
        logger.info('Api get_channel_videos test successful.')
        

   def test_get_latest_channel_videos(self):
        api_link = "a"
        url = reverse('get-latest-channel-videos')
        rs = self.client.get(url,{'api_key':self.api_key, 'channel_link': api_link})
        self.assertEqual(rs.status_code,  200)
        content = rs.content
        content_dict = json.loads(content)
        videos_list = content_dict['videos']
        latest_video_id_from_api = videos_list[0]['id']
        channel = Channel.objects.get(api_link=api_link)
        latest_channel_video_id_from_query = Video.objects.filter(channel=channel).order_by('-created')[0].id
        #check first vid if same id
        self.assertEqual(latest_channel_video_id_from_query, latest_video_id_from_api)
        logger.info('Api get_latest_channel_videos test successful.')


   def test_generate_admin_api_key(self):            
        """def generate_admin_api_key(request):
            if request.META['HTTP_REFERER'].split('/')[3] == 'admin':
                user_id = User.objects.get(username=request.POST['username']).id
                print request
                user = UserProfile.objects.get(user=user_id)
                if user.api_key == "" and len(user.api_key) == 0:
                    user.api_key = generate_api_key(12)
                user.save()
        """
        user = User.objects.create_superuser(username="testtest",email="test@test.com",password="asd")
        # create fake request using django test Client
        c = Client()
        test_request = c.post('/', {'username': user.username,})
        print test_request.request
        environ = {
            'HTTP_COOKIE': test_request.cookies,
            'HTTP_REFERER': 'http://localhost:8000/admin/',
            'PATH_INFO': '/',
            'QUERY_STRING': '',
            'REQUEST_METHOD': 'GET',
            'SCRIPT_NAME': '',
            'SERVER_NAME': 'testserver',
            'SERVER_PORT': 80,
            'SERVER_PROTOCOL': 'HTTP/1.1',
        }
        environ.update({})
        environ.update(test_request.request)
        wsgi_request = WSGIRequest(environ)
        #this is the generate admin api key function in account app that needs to be tested
        if wsgi_request.META['HTTP_REFERER'].split('/')[3] == 'admin':
            user_id = User.objects.get(username=wsgi_request.POST['username']).id
            user = UserProfile.objects.get(user=user_id)
            if user.api_key == "" and len(user.api_key) == 0:
                user.api_key = generate_api_key(12)
            user.save()
            print user.api_key
            
        admin_user = UserProfile.objects.get(user=user.id)
        # check results
        self.assertEqual(len(admin_user.api_key),12)
        logger.info('Api generate_admin_api_key test successful.')
