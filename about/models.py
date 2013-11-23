from django.core.mail import mail_admins, send_mail
from django.conf import settings
from django.db import models
from django.template.loader import render_to_string


class Inquiry(models.Model):
    """ Save the user messages sent through the /contact page """
    
    INQUIRY_TYPES = (
        ("Feedback","Feedback"),
        ("Billing","Billing"),
        ("Feature Request", "Feature Request"),
        ("How To Request", "How To Question"),
        ("Testimonial", "Testimonial"),
    )
    
    first_name = models.CharField(max_length=100)
    last_name = models.CharField(max_length=100)
    email = models.EmailField()
    type = models.CharField(max_length=30, choices=INQUIRY_TYPES)
    message = models.TextField()
    date_created = models.DateTimeField(auto_now_add=True, editable=False)

    class Meta:
        verbose_name = "Inquiry"
        verbose_name_plural = "Inquiries"
    
    def save(self, *args, **kwargs):
        super(Inquiry, self).save(*args, **kwargs)
        # send email notification to the admins
        rendered = render_to_string("email/inquiry.txt", { 'inquiry': self })
        mail_admins('User Inquiry', rendered, fail_silently=settings.EMAIL_FAIL_SILENTLY)
        # send a copy to the user
        # send_mail(subject='User Inquiry', message=rendered, from_email=settings.DEFAULT_FROM_EMAIL, 
        #          recipient_list=[self.email], fail_silently=settings.EMAIL_FAIL_SILENTLY)

    def get_full_name(self):
        return self.first_name.capitalize() + " " + self.last_name.capitalize()

    def __unicode__(self):
        return self.email

