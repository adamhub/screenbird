
from django.db import models
from django.contrib.contenttypes.models import ContentType
from thumbsup.models import ThumbUp


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
