from django.db import models

from accounts.models import UserProfile
from authorize_net.signals import arb_subscription_signup, arb_subscription_cancel

class AuthorizeARBUser(models.Model):
    user_profile = models.OneToOneField('accounts.UserProfile')
    subscription_id = models.CharField(max_length=15, default='')
    
    def __str__(self):
        return "%s" % self.subscription_id
        
    def save(self, *args, **kwargs):
        super(AuthorizeARBUser, self).save(*args, **kwargs)
        arb_subscription_signup.send(sender=self)
        
    def delete(self):
        arb_subscription_cancel.send(sender=self)
        super(AuthorizeARBUser, self).delete()
