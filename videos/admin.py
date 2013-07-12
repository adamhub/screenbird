from django.conf import settings
from django.contrib import admin

from videos.forms import generate_channel_slug
from videos.models import (
    Channel,
    ChannelOption,
    CoCreate,
    CocreateOption,
    ConfirmationInfo,
    FeaturedVideo,
    PopUpInformation,
    ReservedSlug,
    Section,
    Video,
    VideoStatus,
)

admin.site.register(CocreateOption)
admin.site.register(ChannelOption)
admin.site.register(ConfirmationInfo)
admin.site.register(PopUpInformation)

class ChannelAdmin(admin.ModelAdmin):
    actions = ['reset_slug']

    def reset_slug(self, request, queryset):
        for q in queryset:
            channel_slug = generate_channel_slug(settings.CHANNEL_SLUG_LENGTH)
            q.channel_slug = channel_slug
            q.save()
    reset_slug.short_description = "Reset selected Channels' slugs"

class VideoAdmin(admin.ModelAdmin):
    list_display = ['title', 'uploader_userprofile', 'created', 
                    'slug_video_link', 'hits']
    prepopulated_fields = {
        'slug': ('title',),
    }
    search_fields = ['title']


class VideoStatusAdmin(admin.ModelAdmin):
    list_display = ['video_slug','is_encoding']


class ReservedSlugAdmin(admin.ModelAdmin):
    list_display = ['slug', 'used']

class FeaturedVideoAdmin(admin.ModelAdmin):

    class Media:
        js = (
            'js/jquery-1.5.1.min.js',
            'js/jquery-ui-1.8.16.custom.min.js',
            'js/admin-list-reorder.js',
        )
    list_display_links = ['video',]
    list_display = ['position','video']
    list_editable = ['position',]  # 'position' is the name of the model field which holds the position of an element

admin.site.register(Channel, ChannelAdmin)
admin.site.register(FeaturedVideo, FeaturedVideoAdmin)
admin.site.register(VideoStatus, VideoStatusAdmin)
admin.site.register(ReservedSlug, ReservedSlugAdmin)
admin.site.register(Video, VideoAdmin)
admin.site.register(CoCreate)
admin.site.register(Section)
