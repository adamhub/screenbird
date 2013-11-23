from django.db import models
from django.utils.translation import ugettext_lazy as _
from django.contrib import admin

ATTRIBUTE_CHOICES = (
    ('name','name'),
    ('http-equiv','http-equiv'),
)

class MetatagType(models.Model):
    #metatag attribute list
    attribute   = models.CharField(_('attribute'),max_length=64,choices=ATTRIBUTE_CHOICES)
    value       = models.CharField(_('value'),max_length=128)
    
    class Meta:
        ordering = ["value"]

    def __unicode__(self):
        return self.value

class Metatag(models.Model):
    #meta tag attributes
    type        = models.ForeignKey(MetatagType)
    content     = models.TextField(_('content'), help_text = _('Content of the metatag.'))
    scheme      = models.CharField(_('scheme'), max_length=64, blank=True, help_text = _('Scheme attribute'))

    class meta():
        verbose_name = _('Html metatag')
        verbose_name_plural = _('Html metatags')

    def __unicode__(self):
        return "%s: %s" % (self.type.value, self.content)
