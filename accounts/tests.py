import logging

from django.test import TestCase
from django.core.urlresolvers import reverse
from django.contrib.auth.models import User

from paypal.standard.ipn.signals import subscription_cancel

from accounts.models import AccountLevel, UserProfile
from authorize_net.models import AuthorizeARBUser
from videos.models import Channel, ChannelOption, ConfirmationInfo, Video

from decimal import Decimal
handler = logging.StreamHandler()
formatter = logging.Formatter('%(message)s')
handler.setFormatter(formatter)

logger = logging.getLogger('accounts_test_suite')
logger.addHandler(handler) 
logger.setLevel(logging.DEBUG)

class AuthLoginTest(TestCase):
    fixtures = ['test_site_data.json',
                'test_user_data.json']
    
    def setUp(self):
        self.admin = User.objects.get(pk = 1) #Admin User
        self.user1 = User.objects.get(pk = 2) #Normal User
        
    def test_index(self):
        #Test the index() view
        client = self.client
        url = reverse('index')
        response = client.get(url)
        
        self.assertTrue(response.status_code == 200)
        logger.info('AuthLogin index() view test successful.')
    """
    def test_about(self):
        #Test the about() view
        client = self.client
        url = reverse('about')
        response = client.get(url)
        
        self.assertTrue(response.status_code == 200)
        self.assertTrue('about_page' in response.context)
    """
    
    def test_introduction(self):
        #Test the introduction() view
        client = self.client
        url = reverse('introduction')
        response = client.get(url)
        
        self.assertTrue(response.status_code == 200)
        logger.info('AuthLogin introduction() view test successful.')
        
    def test_login(self):
        #Test the login() view
        client = self.client
        url = reverse('login')
        
        #Connect to View and attempt to login
        response = client.post(url,
            {'username': self.user1.username, 'password': 'password'})
            
        self.assertTrue(response.status_code == 302)
        
        #Send no POST data
        response = client.post(url)
        
        self.assertTrue(response.status_code == 200)
        
        #Send junk POST data
        response = client.post(url,
            {'junk1': 'junk1', 'junk2': 'junk2'})
        
        self.assertTrue(response.status_code == 200)
        
        #Test a Failed Login
        response = client.post(url,
            {'username': self.user1.username, 'password': 'qwerty'})
            
        self.assertTrue(response.status_code == 200)
        self.assertTrue('error' in response.context)
        self.assertEqual(str(response.context['error']), u'*Check your username and password.')

        #Test failed login on incomplete data
        response = client.post(url,
            {'username': '', 'password': 'qwerty'})

        self.assertTrue(response.status_code == 200)
        self.assertTrue('error' in response.context)
        self.assertEqual(str(response.context['error']), u'Username or Password Incorrect')

        response = client.post(url,
            {'username': self.user1.username, 'password': ''})

        self.assertTrue(response.status_code == 200)
        self.assertTrue('error' in response.context)
        self.assertEqual(str(response.context['error']), u'Username or Password Incorrect')

        response = client.post(url,
            {'username': '', 'password': ''})

        self.assertTrue(response.status_code == 200)
        self.assertTrue('error' in response.context)
        self.assertEqual(str(response.context['error']), u'Username or Password Incorrect')

        logger.info('AuthLogin login() view test successful.')
    
    def test_register(self):
        #Test the register() view
        client = self.client
        url = reverse('register')
        
        #Connect to View and post a registration
        response = client.post(url,
            {
                'email': 'testuser3@email.com',
                'username': 'testuser4',
                'password': 'password',
                'confirm_password': 'password'
            })
            
        self.assertTrue(response.status_code == 302)

        #Connect to view and post a registration using whitespaced-email
        response = client.post(url,
            {
                'email': 'testuser5@email.com \n\r \t ',
                'username': 'testuser5',
                'password': 'password',
                'confirm_password': 'password'
            })

        self.assertTrue(response.status_code == 302)

        client.get(reverse('logout'))
        
        #Send no POST data
        response = client.post(url)
        
        self.assertTrue(response.status_code == 200)

        client.get(reverse('logout'))
        
        #Send junk POST data
        response = client.post(url,
            {'junk1': 'junk1', 'junk2': 'junk2'})
        
        self.assertTrue(response.status_code == 200)
        self.assertTrue('errors' in response.context)
        self.assertTrue('email' in response.context['errors'])
        self.assertTrue('password' in response.context['errors'])
        self.assertTrue('confirm_password' in response.context['errors'])
        logger.info('AuthLogin register() view test successful.')

    def test_complete(self):
        #Test social auth complete() view
        client = self.client
        url = reverse('complete', kwargs={'backend': 'google'})
        response = client.get(url)
        self.assertTrue(response.status_code == 302)
        logger.info('SocialAuth complete() view test successful.')

class AccountsTest(TestCase):
    fixtures = ['test_site_data.json',
                'test_user_data.json',
                'test_channel_data.json']
    
    def setUp(self):
        self.admin = User.objects.get(pk = 1) #Admin User
        self.user1 = User.objects.get(pk = 2) #Paid User
        self.user2 = User.objects.get(pk = 3) #Normal User
        self.custom = self.user1.username
        #Login first for other views to work
        client = self.client
        url = reverse('login')
        
        #Connect to View and attempt to login
        response = client.post(url,
            {'username': self.user2.username, 'password': 'password'})
    
    def test_account_info(self):
        #Test the account_info() view
        client = self.client
        url = reverse('account_info')
        response = client.get(url)
        
        self.assertTrue(response.status_code == 200)
        logger.info('Accounts account_info() view test successful.')
    
    def test_account_info_premium(self):
        #Test the account_info() view with 'premium' kwarg
        client = self.client
        url = reverse('account_info', kwargs={'premium': 'premium'})
        response = client.get(url)
        
        self.assertTrue(response.status_code == 200)
        self.assertTrue('premium' in response.context)
        self.assertTrue(response.context['premium'] == 'premium')
        logger.info('Accounts account_info_premium() view test successful.')
    
    def test_features(self):
        #Test the paypal_upgrade() view
        client = self.client
        url = reverse('features')
        response = client.get(url, {}, HTTP_HOST=url)
        
        self.assertTrue(response.status_code == 200)
        logger.info('Accounts paypal_upgrade() view test successful.')
    
    def test_manage_account(self):
        #Test the manage_account() view
        client = self.client
        url = reverse('manage_account')
        
        #Connect to view and post data for username update form
        response = client.post(url,
            {
                'username_update': 'Update Username',
                'username': 'testuser4',
            })
        
        self.assertTrue(response.status_code == 302)
        
        #Connect to view and post data for nickname update form
        response = client.post(url,
            {
                'userprofile': 'Update Nickname',
                'nickname': 'testuser44',
            })
        
        self.assertTrue(response.status_code == 302)
        
        #Connect to view and post data for change password form
        response = client.post(url,
            {
                'change_password': 'Update Password',
                'old_password': 'password',
                'new_password1': 'password1',
                'new_password2': 'password1',
            })
        
        self.assertTrue(response.status_code == 302)
        
        #Send no POST data
        response = client.post(url)
        
        self.assertTrue(response.status_code == 200)
        
        #Send junk POST data
        response = client.post(url,
            {'junk1': 'junk1', 'junk2': 'junk2'})
        
        self.assertTrue(response.status_code == 200)
        logger.info('Accounts manage_account() view test successful.')
    
    def test_upgrade_trial(self):
        #Test the upgrade_trial() view
        client = self.client
        url = reverse('upgrade_trial')

        #Assert first that the logged-in user has a Free account
        self.assertTrue(self.user2.userprofile.account_level.level == 'Free')
        
        #Connect to the view
        response = client.get(url)
        
        self.assertTrue(response.status_code == 302)
        #Get again the updated User object
        user2 = User.objects.get(pk = 3)
        #Assert now that the logged-in user has a Trial account
        logger.info('New level %s ' % user2.userprofile.account_level.level)
        self.assertTrue(user2.userprofile.account_level.level == 'Trial')
        logger.info('Accounts upgrade_trial() view test successful.')

    def test_payment_cancel(self):
        #Test the arb_cancel_subscription_handler()
        
        #Assert first that the logged-in user has a Premium account
        self.assertTrue(self.user1.userprofile.account_level.level == 'Paid')
        
        #Cancel subscription payment
        self.user1.userprofile.authorizearbuser.delete()
        
        #Assert if the user's account was downgraded to Free
        user1 = User.objects.get(pk = 2)
        self.assertTrue(user1.userprofile.account_level.level == 'Free')
        logger.info('Accounts arb_cancel_subscription() handler test successful.')
    
    def test_paypal_payment_cancel(self):
        #Test the my_payment_cancel_handler()
        
        #Assert first that the logged-in user has a Premium account
        self.assertTrue(self.user1.userprofile.account_level.level == 'Paid')
        
        #Cancel subscription payment
        subscription_cancel.send(sender=self)
        
        #Assert if the user's account was downgraded to Free
        user1 = User.objects.get(pk = 2)
        self.assertTrue(user1.userprofile.account_level.level == 'Free')
        logger.info('Accounts my_payment_cancel_handler() handler test successful.')

class ChannelsTest(TestCase):
    fixtures = ['test_site_data.json',
                'test_user_data.json',
                'test_channel_data.json']
    
    def setUp(self):
        self.user1 = User.objects.get(pk = 2) #Normal User
        
        #Login first for other views to work
        client = self.client
        url = reverse('login')
        
        #Connect to View and attempt to login
        response = client.post(url,
            {'username': self.user1.username, 'password': 'password'})
    
    def test_create_channel(self):
        #Test the create_channel() view
        client = self.client
        url = reverse('create_channel')
        
        #Assert first that there is only one existing Channel object (the one from the fixture for updating purposes)
        self.assertTrue(Channel.objects.count() == 1)
        #Assert first that the user doesn't own a chanell
        self.assertTrue(self.user1.userprofile.channels.count() == 0)
        
        #Connect to view and post data for username update form
        response = client.post(url,
            {
                'name': 'Channel1'
            })
        
        self.assertTrue(response.status_code == 302)
        #Assert now that there are more than 1 existing Channel objects
        self.assertTrue(Channel.objects.count() > 1)
        #Assert that the user now owns a channel
        self.assertTrue(self.user1.userprofile.channels.count() > 0)
        logger.info('Channels create_channel() view test successful.')
    
    def test_update_channel(self):
        #Test the update_channel() view
        channel = Channel.objects.get(pk=1)
        client = self.client
        url = reverse('update_channel', kwargs={'channel_id': channel.id})
        
        #Assert first that the name of the Channel object is the same as with that in the fixture
        self.assertTrue(channel.name == 'Channel 1')
        
        response = client.post(url,
            {
                'name': 'Channel 1'
            })

        self.assertTrue(response.status_code == 200)
        #Get again the updated Channel object
        channel = Channel.objects.get(pk=1)
        #Assert now that the name of the updated Channel object is the same as what was POSTed
        self.assertTrue(channel.name == 'Channel 1')
        logger.info('Channels update_channel() view test successful.')
    
    def test_add_channel_member(self):
        #Test the add_channel_member() view
        channel = Channel.objects.get(pk=1)
        
        client = self.client
        url = reverse('add_channel_member', kwargs={'channel_id': channel.id})
        
        #Assert first that the user to be added is not a member of the channel
        self.assertTrue(self.user1 not in channel.get_channel_members())
        
        response = client.post(url,
            {
                'email': self.user1.email
            }, HTTP_HOST=url)

        self.assertTrue(response.status_code == 302)
        #Get again the updated Channel object
        channel = Channel.objects.get(pk=1)
        #Assert now that the user is a member of the channel
        self.assertTrue(self.user1 in channel.get_channel_members())
        logger.info('Channels add_channel_member() view test successful.')
    
    def test_remove_channel_member(self):
        #Test the remove_channel_member() view
        channel = Channel.objects.get(pk=1)
        client = self.client
        
        #First, add a channel_member
        url = reverse('add_channel_member', kwargs={'channel_id': channel.id})
        
        response = client.post(url,
            {
                'email': self.user1.email
            }, HTTP_HOST=url)

        #Get again the updated Channel object
        channel = Channel.objects.get(pk=1)
        #Make sure that the user was added to the channel_member's list
        self.assertTrue(self.user1 in channel.get_channel_members())
        
        #Then remove the added channel_member
        url = reverse('remove_channel_member', kwargs={'channel_id': channel.id, 'user_id':self.user1.id})
        
        response = client.post(url, {})
        
        #Get again the updated Channel object
        channel = Channel.objects.get(pk=1)
        #Make sure the user was removed from the channel_member's list
        self.assertTrue(self.user1 not in channel.get_channel_members())
        logger.info('Channels remove_channel() view test successful.')
    
    def test_update_channel_member_permissions(self):
        #Test the update_channel_member_permissions() view
        channel = Channel.objects.get(pk=1)
        client = self.client
        
        #First, add a channel_member
        url = reverse('add_channel_member', kwargs={'channel_id': channel.id})
        
        response = client.post(url,
            {
                'email': self.user1.email
            }, HTTP_HOST=url)
        
        #Then visit the update_channel_member_permissions view to create initially the ChannelOption object for the channel and the user
        url = reverse('update_channel_member_permissions', kwargs={'channel_id': channel.id})
        
        response = client.post(url,{})
        
        #Assert that the ChannelOption for the user's profile and the channel already exists
        channel_option = ChannelOption.objects.get(user_profile=self.user1.userprofile, channel=channel)
        self.assertTrue(channel_option)
        #Assert that initially, the channel does not need to approve video uploads by the user
        self.assertFalse(channel_option.needs_approval)
        
        #Then update the user's permissions
        approval_field = 'channel-%s-needs_approval' % self.user1.pk
        response = client.post(url, 
            {
                approval_field : True
            })
            
        #Get again the updated ChannelOption object
        channel_option = ChannelOption.objects.get(user_profile=self.user1.userprofile, channel=channel)
        #Assert now that the user's video uploades need to be approved first
        self.assertTrue(channel_option.needs_approval)
        logger.info('Channels update_channel_member_permissions() view test successful.')
     
    def test_confirm_channel_member(self):
        #Test the update_channel_member_permissions() view
        channel = Channel.objects.get(pk=1)
        client = self.client
        
        #First, add a channel_member
        url = reverse('add_channel_member', kwargs={'channel_id': channel.id})
        
        response = client.post(url,
            {
                'email': self.user1.email
            }, HTTP_HOST=url)
            
        confirmation_info = ConfirmationInfo.objects.get(email=self.user1.email, channel=channel)
        #Assert that the confirmation status is still pending
        status = confirmation_info.status
        self.assertEqual(status, 'pending')
        
        #Get the confirmation key for the user
        key = confirmation_info.key
        
        #Now confirm the user's joining the channel
        url = reverse('confirm_channel_member') + '?key=' + key
        
        response = client.get(url, {})
        
        #Get again the added user to the channel_member's list
        channel_member = User.objects.get(pk=2)
        #Assert that the channel has been added to the user's channels list
        self.assertTrue(channel in channel_member.userprofile.channels.all())
        
        #Get again the updated ConfirmationInfo object
        confirmation_info = ConfirmationInfo.objects.get(email=self.user1.email, channel=channel)
        #Assert now that the user is confirmed
        status = confirmation_info.status
        self.assertEqual(status, 'confirmed')
        
        #Try to confirm again
        response = client.get(url, {})
        logger.info('Channels confirm_channel_member() view test successful.')
    
    def test_add_video_to_channel(self):
        #Test the add_video_to_channel() view
        channel = Channel.objects.get(pk=1)
        client = self.client
        
        #Create first a Video object
        video = Video.objects.create(
                uploader = self.user1,
                title = 'Video 1',
                video_type = 'mp4',
                slug = 'video-1',
            )
        
        #Assert that the video is not part of a channel yet
        self.assertFalse(video.channel)
        
        #Then visit the view
        url = reverse('add_video_to_channel', kwargs={'channel_id': channel.id, 'video_id': video.id})
        
        response = client.get(url, {})
        
        #Get again the updated video object
        video = Video.objects.get(
                uploader = self.user1,
                title = 'Video 1',
                video_type = 'mp4',
                slug = 'video-1',
            )

        #Assert now that the video is part of the channel
        self.assertTrue(video.channel == channel)
        logger.info('Channels add_video_to_channel() view test successful.')


class UserTimeUploadedTest(TestCase):
    """ Test for Users' time uploaded """
    
    def setUp(self):
        """ Sets Up initial Data """
        fixtures = ['test_site_data.json',
                'initial_data.json']
        
        
        trial_account = AccountLevel.objects.get(pk=4)
        admin_account = AccountLevel.objects.get(pk=3)
        paid_account = AccountLevel.objects.get(pk=2)
        free_account = AccountLevel.objects.get(pk=1)
        
        self.u1 = User.objects.create_user('test_user1', 'test1@email.com', 'test_password')
        self.u2 = User.objects.create_user('test_user2', 'test2@email.com', 'test_password')
        self.u3 = User.objects.create_user('test_user3', 'test3@email.com', 'test_password')
        self.u4 = User.objects.create_user('test_user4', 'test4@email.com', 'test_password')
        
        #Create profile for every user at varying account level
        self.profile1 = UserProfile.objects.get(user=self.u1)
        self.profile1.account_level = trial_account
        self.profile1.save()
        self.profile2 = UserProfile.objects.get(user=self.u2)
        self.profile2.account_level = admin_account
        self.profile2.save()
        self.profile3 = UserProfile.objects.get(user=self.u3)
        self.profile3.account_level = paid_account
        self.profile3.save()
        self.profile4 = UserProfile.objects.get(user=self.u4)
        self.profile4.account_level = free_account
        self.profile4.save()
        
        #Create Video objects for each user
        self.video1 = Video.objects.create(uploader=self.u1, title="Test1", video_duration=499, video_type="mp4",
                                        slug="S2FE4R", description="Test1", is_public=True, created="2012-01-01")
        self.video2 = Video.objects.create(uploader=self.u2, title="Test2", video_duration=9998, video_type="mp4",
                                        slug="S2FE4T", description="Test2", is_public=True, created="2012-01-01")
        self.video3 = Video.objects.create(uploader=self.u3, title="Test3", video_duration=499, video_type="mp4",
                                        slug="S2FE4Y", description="Test3", is_public=True, created="2012-01-01")
        self.video4 = Video.objects.create(uploader=self.u4, title="Test4", video_duration=149, video_type="mp4",
                                        slug="S2FE4U", description="Test4", is_public=True, created="2012-01-01")
        
        #Update users' total upload time
        users = User.objects.all()
        for user in users:
            videos = Video.objects.filter(uploader=user)
            total_time = Decimal('0.0')
            for v in videos:
                total_time = Decimal(str(total_time)) + Decimal(str(v.video_duration))
            profile = user.userprofile
            profile.total_upload_time = total_time
            profile.save()
        
    def test_video_length(self):
        """ Test the users' total_upload_time """
        
        u1 = User.objects.get(pk=1)
        u2 = User.objects.get(pk=2)
        u3 = User.objects.get(pk=3)
        u4 = User.objects.get(pk=4)
        
        self.assertTrue(u1.userprofile.total_upload_time==499)
        self.assertTrue(u2.userprofile.total_upload_time==9998)
        self.assertTrue(u3.userprofile.total_upload_time==499)
        self.assertTrue(u4.userprofile.total_upload_time==149)
        logger.info('Accounts test_total_upload_time() view test successful.')
    

    def test_account_level_change(self):
        """ Test total_upload time if the user changes account level
        """
        #User with free account
        u4 = User.objects.get(pk=4)
        client = self.client
        url = reverse('login')
        #Connect to View and attempt to login
        response = client.post(url,
            {'username': u4.username, 'password': 'test_password'})
            
        self.assertTrue(response.status_code == 302)
        
        #No post data
        response = client.post(url)
        
        self.assertTrue(response.status_code == 200)

        #Check first if it has a free account
        self.assertTrue(u4.userprofile.account_level.level == 'Free')
        
        paid_account = AccountLevel.objects.get(pk=2)

        
        u4.userprofile.account_level = paid_account
        u4.save()
        self.assertTrue(u4.userprofile.account_level.level == 'Paid')
        #Check if the total upload time is still the same
        self.assertTrue(u4.userprofile.total_upload_time==149)
        
        url = reverse('upgrade_trial')
        response = client.get(url)
        self.assertTrue(response.status_code == 302)
        #Get again the updated User object
        u4 = User.objects.get(pk = 4)
        #Assert now that the logged-in user has a Trial account
        self.assertTrue(u4.userprofile.account_level.level == 'Trial')
        #Check if the total upload time is stil the same
        self.assertTrue(u4.userprofile.total_upload_time==149)
        
        logger.info('Accounts account_level_change() view test successful.')
