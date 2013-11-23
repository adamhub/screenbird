from django.conf.urls.defaults import patterns, include, url
from api.resources import VideoResource, ChannelVideoResource, LatestVideoResource, LatestChannelVideoResource

urlpatterns = patterns('api.views',
        url(r'^config_record_on_account/(?P<account_id>\d+)/$','config_record_on_account', name='config-record-on-account'),
        url(r'^config_record_on_channel/(?P<channel_id>\d+)/$','config_record_on_channel', name='config-record-on-channel'),
        url(r'^get_all_videos/$', VideoResource.as_view(), name='get-all-videos'),
        url(r'^get_channel_videos/$', ChannelVideoResource.as_view(), name='get-channel-videos'),
        url(r'^get_channel_videos/(?P<channel_link>[-\w]+)/$', ChannelVideoResource.as_view(), name='get-channel-videos'),
        url(r'^get_latest_videos/$', LatestVideoResource.as_view(), name='get-latest-videos'),
        url(r'^get_latest_channel_video/$', LatestChannelVideoResource.as_view(), name='get-latest-channel-videos'),
        url(r'^get_latest_channel_video/(?P<channel_link>[-\w]+)/$', LatestChannelVideoResource.as_view(), name='get-latest-channel-videos'),
    )
