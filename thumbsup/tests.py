import logging

from django.test import TestCase
from django.core.urlresolvers import reverse
from django.contrib.auth.models import User

from videos.models import Video

handler = logging.StreamHandler()
formatter = logging.Formatter('%(message)s')
handler.setFormatter(formatter)

logger = logging.getLogger('thumbsup_test_suite')
logger.addHandler(handler) 
logger.setLevel(logging.DEBUG)

class VideosTest(TestCase):
    fixtures = ['test_site_data.json',
                'test_user_data.json',
                'test_channel_data.json',
                'test_videos_data.json']
                
    def setUp(self):
        self.user1 = User.objects.get(pk = 2) #Normal User
        
        self.video1 = Video.objects.get(pk = 1)
        
        #Login first for other views to work
        client = self.client
        url = reverse('login')
        
        #Connect to View and attempt to login
        response = client.post(url,
            {'username': self.user1.username, 'password': 'password'})
            
    def test_vote(self):
        #Test the vote() view
        client = self.client
        url = reverse('vote_ajax')
        
        #Check video thumbs up and down count (should be 0 for both)
        self.assertTrue(self.video1.thumbs.thumbs_up_count() == 0)
        self.assertTrue(self.video1.thumbs.thumbs_down_count() == 0)
        
        #Like the video
        response = client.post(url, 
            {
                'id': 1,
                'type': 'up',
                'action': ''
            })
            
        #Recheck video thumbs up count
        video = Video.objects.get(pk=1)
        self.assertTrue(video.thumbs.thumbs_up_count() == 1)
        
        #Dislike the video
        response = client.post(url, 
            {
                'id': 1,
                'type': 'down',
                'action': ''
            })
            
        #Recheck video thumbs down count
        video = Video.objects.get(pk=1)
        self.assertTrue(video.thumbs.thumbs_down_count() == 1)
