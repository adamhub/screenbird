from django.db import models
from django.contrib import admin
from django.contrib.auth.models import User
from django.contrib.contenttypes.models import ContentType
from django.contrib.contenttypes import generic
from django.utils.translation import ugettext_lazy as _

class _HelperManager(models.Manager):
    """
    Simple enhanced manager for Thumbs
    """

    def for_object(self, instance, model=None):
        """
        Function for getting Thumbs for specified object
        """
        if model is None  and  instance is not None:
            model = ContentType.objects.get_for_model(instance.__class__)

        return self.filter(voted_object_type=model,
                voted_object_id=instance.pk)

class ThumbUp(models.Model):
    thumbs_up = models.BooleanField(default=True)

    # user who voted
    user = models.ForeignKey(User)

    # voted object
    voted_object_type = models.ForeignKey(ContentType)
    voted_object_id = models.PositiveIntegerField()
    voted_object = generic.GenericForeignKey('voted_object_type',
            'voted_object_id')

    objects = _HelperManager()

    def __unicode__(self):
        s = "up" if self.thumbs_up else "down"
        return _("User-%s thumbs %s for %s") % (self.user, s,
                self.voted_object)

class _ThumbUpManager(models.Manager):

    def __init__(self, model, instance):
        self.model = model
        self.instance = instance

    def get_query_set(self):
        """
        Get_query_set of all Thumbs Up
        """
        return ThumbUp.objects.for_object(self.instance)

    def thumbs_up_count(self):
        """
        Thumbs Up = True, Count
        """
        return ThumbUp.objects.for_object(self.instance).filter(thumbs_up=True).count()

    def thumbs_down_count(self):
        """
        Thumbs Up = False, Count
        """
        return ThumbUp.objects.for_object(self.instance).filter(thumbs_up=False).count()

    def add(self, user, thumbs_up=True):
        obj, created = ThumbUp.objects.get_or_create(user=user,
               voted_object_type=ContentType.objects.get_for_model(self.model),
               voted_object_id=self.instance.pk, defaults={"thumbs_up":thumbs_up})
        if not created  and  obj.thumbs_up != thumbs_up:
            obj.thumbs_up = thumbs_up
            obj.save()
        return obj

class ThumbsUpManager(object):
    def __get__(self, instance, model):
        if instance is not None and instance.pk is None:
            raise ValueError("%s objects need to have a primary key value "
            "before you can access their tags." % model.__name__)

        return _ThumbUpManager(model=model, instance=instance)

admin.site.register(ThumbUp)
