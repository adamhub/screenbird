import random
import string
import urllib
import urllib2
from decimal import Decimal

from django.conf import settings
from django.contrib.auth.models import User
from django.contrib.sites.models import Site
from django.db import models
from django.db.models.fields import CharField
from django.db.models.signals import post_save, post_delete, pre_save

from paypal.standard.ipn.signals import (
    payment_was_successful, 
    subscription_cancel, 
    subscription_signup,
)

from authorize_net.signals import (
    arb_subscription_signup, 
    arb_subscription_cancel,
)


class AccountLevel(models.Model):
    level = models.CharField(max_length=15)
    description = models.CharField(max_length=50, null=True, blank=True)
    max_video_length = models.IntegerField("Maximum video recording length in minutes", default=150)
    video_validity = models.IntegerField("Video Validity in days", null=True, default=60)
    authnet_line_item_id = models.CharField(max_length=31, null=False, blank=False)
    authnet_item_name = models.CharField(max_length=31, null=False, blank=False)
    authnet_item_description = models.CharField(max_length=255, null=False, blank=False)
    authnet_item_price = models.DecimalField(max_digits=11, decimal_places=2, default=Decimal('0.0'))    

    def __str__(self):
        return "%s Account" % self.level

    def __unicode__(self):
        return "%s Account" % self.level


class UserProfile(models.Model):  
    CATEGORIES = (
        ('T','Trusted'),
        ('N', 'Needs Review'),
    )

    user = models.OneToOneField(User)
    nickname = models.CharField( max_length=50, null=True, blank=True )
    account_level = models.ForeignKey(AccountLevel, default=1)
    total_upload_time = models.DecimalField( max_digits=11, decimal_places=2, default=Decimal('0.0'), null=True)
    channels = models.ManyToManyField('videos.Channel', null=True, blank=True)
    recorder_link = models.CharField(max_length=8, default="", blank=True)
    # If user is being tracked via cookie for anonymous activity
    is_anonymous = models.BooleanField(default=0)
    category = models.CharField(max_length=1, choices=CATEGORIES, default='N')

    # Social connect options
    facebook_connect = models.BooleanField(default=0)
    twitter_connect = models.BooleanField(default=0)

    # Fields for trial account usage
    # If user already signed up for trial
    trial_signed_up = models.BooleanField(editable=False, default=0)
    # If trial period already ended (either by expiry date or by upgrade to premium)
    trial_ended = models.BooleanField(editable=False, default=0)
    trial_expiry_date = models.DateTimeField(editable=False, null=True, blank=True)
    # If user has been notified two weeks before and on trial expiry date
    is_sent_early = models.BooleanField(editable=False, default=0)
    is_sent_expiry = models.BooleanField(editable=False, default=0)
    api_key = models.CharField(max_length=12, default="", blank=True)
    api_link = models.CharField(max_length=20, default="", blank=True)
    subscription_active = models.BooleanField(editable=False, default=0)
    
    def __str__(self):
        return u"%s's profile" % unicode(self.user)

    @property
    def is_paid(self):
        return self.account_level.level == "Paid"
        
    @property
    def is_using_trial(self):
        return self.trial_signed_up and not self.trial_ended and self.account_level.level == "Trial"
    
    def save(self, *args, **kwargs):
        if self.api_key == '':
            api_key = ''
            while (True):
                api_key = generate_random_string(12)
                try:
                    UserProfile.objects.get(api_key = api_key)
                except:
                    break
            self.api_key = api_key
        super(UserProfile, self).save(*args, **kwargs)


def generate_random_string(length):
    """
    Random string generator for slugs and passwords
    
    http://stackoverflow.com/questions/2257441/python-random-string-generation-with-upper-case-letters-and-digits
    """
    unique_string = ''.join(random.choice(string.ascii_uppercase + string.digits) for x in range(length))
    return unique_string

def create_anonymous_user(anonymous_token):
    try:
        user = User.objects.get(username=anonymous_token)
    except User.DoesNotExist:
        #Create User
        user = User(username=anonymous_token, first_name='Anonymous', last_name='Bixly')
        #Set Anonymous Token as password
        user.set_password(anonymous_token)
        user.save()
        #Update UserProfile
        user.get_profile().is_anonymous = True                     
        user.get_profile().save()
    return user

def create_user_profile(sender, instance, created, **kwargs):
    """Creates a user profile for the newly created user account."""
    if created:
        profile, created = UserProfile.objects.get_or_create(user=instance)

def user_post_delete(sender, instance, **kwargs):
    try:
        UserProfile.objects.get(user=instance).delete()
    except:
        pass

def my_payment_was_successful_handler(sender, **kwargs):
    '''Handles a succesful subscription of account on paypal and upgrading userprofile to Paid Account
    '''
    ipn_object = sender
    paid_account = AccountLevel.objects.get(pk=2) 
    try:
        user = User.objects.get(username=ipn_object.custom)
        userprofile = UserProfile.objects.get(user=user)
    except UserProfile.DoesNotExist:
        userprofile = None
    if userprofile:
        userprofile.subscription_active = 1
        userprofile.account_level = paid_account           
        userprofile.save()

def my_payment_cancel_handler(sender, **kwargs):
    '''Handles setting the account to non-subscriber upon payment termination
    '''
    ipn_object = sender
    try:
        user = User.objects.get(username=ipn_object.custom)
        userprofile = UserProfile.objects.get(user=user)
    except UserProfile.DoesNotExist:
        userprofile = None
    if userprofile:  
        userprofile.subscription_active = 0
        userprofile.save()

# General handler for bringing back user to Free Account when subscription is
# cancelled. This should be added to a signal for finally removing Paid Account.
def my_end_of_subscription_handler(sender, **kwargs):
    '''Handles reverting userprofile back to Free Account
    '''
    ipn_object = sender
    free_account = AccountLevel.objects.get(pk=1) 
    try:
        user = User.objects.get(username=ipn_object.custom)
        userprofile = UserProfile.objects.get(user=user)
    except UserProfile.DoesNotExist:
        userprofile = None
    if userprofile:
        if not userprofile.subscription_active:
            userprofile.account_level = free_account
            userprofile.save()

def arb_subscription_signup_handler(sender, **kwargs):
    paid_account = AccountLevel.objects.get(pk=2)
    try:
        userprofile = UserProfile.objects.get(pk=sender.user_profile.pk)
    except UserProfile.DoesNotExist:
        userprofile = None
    if userprofile and userprofile.account_level.level == 'Trial':
        userprofile.trial_ended = True
        userprofile.trial_expiry_date = None
    if userprofile:
        userprofile.account_level = paid_account
        userprofile.save()

def arb_subscription_cancel_handler(sender, **kwargs):
    free_account = AccountLevel.objects.get(pk=1)
    try:
        userprofile = UserProfile.objects.get(pk=sender.user_profile.pk)
    except UserProfile.DoesNotExist:
        userprofile = None
    if userprofile:  
        userprofile.subscription_active = 0
        userprofile.account_level = free_account
        userprofile.save()


#Paypal Signals
payment_was_successful.connect(my_payment_was_successful_handler)
subscription_signup.connect(my_payment_was_successful_handler)
subscription_cancel.connect(my_payment_cancel_handler)
subscription_cancel.connect(my_end_of_subscription_handler)

#Authorize.net ARB Signals
arb_subscription_signup.connect(arb_subscription_signup_handler)
arb_subscription_cancel.connect(arb_subscription_cancel_handler)

post_delete.connect(user_post_delete, sender=User)
post_save.connect(create_user_profile, sender=User)
