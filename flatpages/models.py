from django.db import models
from django.utils.safestring import mark_safe 

class CustomPage(models.Model):
    url = models.CharField( max_length=100, unique=True, help_text="This must be a URL of the page to be rendered.(About Page='/about/', Home='/')" )
    title = models.CharField( max_length=100 )
    content = models.TextField( null=True, blank=True )
    
    def __unicode__(self):
        return '%s at %s' % (self.title,self.url)

    def display_HTMLcontent(self): 
        return mark_safe(self.content)
