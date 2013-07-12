from django.contrib import admin
from django.db.models import Q
from django.db.models.signals import pre_save
from django.http import HttpResponseRedirect
from django.contrib.auth.admin import UserAdmin
from django.contrib.auth.models import User
from django.contrib import messages
from django.forms import ValidationError
from django.utils.translation import ugettext_lazy as _

from chronograph.models import Job
from chronograph.admin import JobAdmin

from accounts.models import UserProfile, AccountLevel

class MyUserAdmin(UserAdmin):
    list_display_links = ('username', 'email')

#~ def check_email(sender, instance, **kwargs):
#~     """Before the User object of the django core
#~     saves the user's new set of information validate the
#~     email first whether it's existing already from the system.
#~     
#~     """
#~     users = User.objects.filter( ~Q(id=instance.id) & Q(email__exact=instance.email) )
#~     if users.count(): 
#~         raise ValidationError("Don't add an email that already exists. Search before you add")

class UserProfileAdmin(admin.ModelAdmin):
    list_display = ('__str__', 'email')
    readonly_fields = ('trial_signed_up', 'trial_ended', 'trial_expiry_date')
    search_fields = ['user__email', 'user__username']

    def email(self, obj):
        return obj.user.email
    
#~ pre_save.connect(check_email, User)

class CustomJobAdmin(JobAdmin):
    def run_job_view(self, request, pk):
        """
        Runs the specified job.
        """
        try:
            job = Job.objects.get(pk=pk)
        except Job.DoesNotExist:
            raise Http404
        # Rather than actually running the Job right now, we
        # simply force the Job to be run by the next cron job
        job.force_run = True
        job.save()
        messages.success(request, _('The job "%(job)s" has been scheduled to run.') % {'job': job} )
        if 'inline' in request.GET:
            redirect = request.path + '../../'
        else:
            redirect = request.REQUEST.get('next', request.path + "../")
        return HttpResponseRedirect(redirect)
    
admin.site.register(UserProfile, UserProfileAdmin)
admin.site.register(AccountLevel)
admin.site.unregister(User)
admin.site.register(User, MyUserAdmin)
admin.site.unregister(Job)
admin.site.register(Job, CustomJobAdmin)
