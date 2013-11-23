import datetime

from django.core.management.base import BaseCommand
from django.conf import settings
from django.template.loader import render_to_string
from django.contrib.auth.models import User
from django.contrib.sites.models import Site

from accounts.models import AccountLevel
from videos.models import Video
from videos.utils import is_trial_expired

class Command(BaseCommand):
    def handle(self, *args, **options):
        
        # Check all users even those without email addresses
        # because they can still apply for a Trial account
        users = User.objects.all()
        
        today = datetime.date.today()
        free_account = AccountLevel.objects.get(pk=1)
        
        for user in users:
            userprofile = user.userprofile
            
            # Check if user is using the trial account
            if userprofile.is_using_trial():
            
                # Check if expiration date is two weeks to go
                two_weeks_before_expiry = userprofile.trial_expiry_date.date() - datetime.timedelta(14)
                
                if today > two_weeks_before_expiry and not userprofile.is_sent_early:
                    userprofile.is_sent_early = True
                    userprofile.save()
                    # Send email if user has a registered email address
                    if user.email:
                        message = render_to_string("email/expiring_trial.txt", {
                            'user': user,
                            'site': Site.objects.get_current()
                        })
                        
                        user.email_user('Your 60-Day Trial Account is about to expire!', message)
                    
                # Check if today is the expiration date of the trial account
                elif today > userprofile.trial_expiry_date.date() and not userprofile.is_sent_expiry:
                    userprofile.account_level = free_account
                    userprofile.trial_ended = True
                    userprofile.is_sent_expiry = True
                    userprofile.save()
                    
                    vids = Video.objects.filter(uploader=user)
                    for vid in vids:
                        is_trial_expired(vid)
                        
                    # Send email if user has a registered email address
                    if user.email:
                        message = render_to_string("email/expired_trial.txt", {
                            'user': user,
                            'site': Site.objects.get_current()
                        })
                        
                        user.email_user('Your 60-Day Trial Account has expired!', message)
