import oembed

from django.utils.safestring import mark_safe
from videos.models import Video
from oembed.providers import DjangoProvider


class VideoProvider(DjangoProvider):
    resource_type = 'video'

    class Meta:
        model = Video
        queryset = Video.objects.embeddable()
        named_view = 'watch-video'
        fields_to_match = {'slug': 'slug'} # map url field to model field
        valid_sizes = ((427, 240), (640, 360), (853, 480))
        force_fit = False

    def author_name(self, obj):
        if obj.uploader:
            return obj.uploader.email
        return u'Anonymous'

    def title(self, obj):
        return obj.title

    def render_html(self, obj, context=None):
        context = context or {'width': 640,
                              'height': 360}
        return mark_safe(obj.get_embed_code(width=context['width'],
                                            height=context['height']))


oembed.site.register(VideoProvider)
