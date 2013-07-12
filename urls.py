from django.conf.urls.defaults import patterns, include, url
from django.conf import settings
from django.contrib import admin
from django.views.generic.simple import direct_to_template, redirect_to

admin.autodiscover()

from hitcount.views import update_hit_count_ajax
from thumbsup.views import vote

import oembed
oembed.autodiscover()

urlpatterns = patterns('',
    #Admin
    url(r'^admin/', include(admin.site.urls)),
    (r'^admin/doc/', include('django.contrib.admindocs.urls')),

    #OEmbed
    url(r'^oembed/', include('oembed.urls')),

    url(r'^accounts/', include('accounts.urls')),
    url(r'^amazon/', include('amazon.urls')),
    url(r'^contact/$', 'about.views.contact_us', name='contact_us'),
    url(r'^features/$', 'accounts.views.paypal_upgrade', name='features'),
    url(r'^features/(?P<option>\w+)/$', 'accounts.views.paypal_upgrade', name='features_list'),

    #Favicon
    url(r'^favicon.ico$', redirect_to, {'url': '%sgfx/favicon.ico' % settings.MEDIA_ROOT}),
)

# Flatpages
urlpatterns+= patterns('django.contrib.flatpages.views',
    url(r'^about/$', 'flatpage', {'url': '/about/'}, name='about'),
    url(r'^features/$', 'flatpage', {'url': '/features/'}, name='features'),
    url(r'^documentation/$', 'flatpage', {'url': '/documentation/'}, name='documentation'),
)

# Test pages
if settings.DEBUG:
    urlpatterns += patterns('',
        url(r'^oembed_test/$', 'videos.views.oembed_test', name='video-oembed-test'),
        url(r'^test/embed_test/$', direct_to_template, {'template': 'embed_test.html'}),    # Recorder through iframe test url
        url(r'^test/video_embed_test/$', direct_to_template, {'template':'embed_video.html'}),
        url(r'^template/base/$', direct_to_template, {'template': 'base.html'}),
        url(r'^template/site_base/$', direct_to_template, {'template': 'site_base.html'}),
    )

# Confirm link
urlpatterns += patterns('',
    url(r'^confirm/', 'accounts.views.confirm_channel_member', name='confirm_channel_member'),
)

# Social Auth Override
urlpatterns += patterns('',
    url(r'^complete/(?P<backend>[^/]+)/$', 'accounts.views.complete', name='complete'),
)
# "Catch-all"
urlpatterns += patterns('',
    url(r'^', include('auth_login.urls')),
    url(r'', include('api.urls')),
    url(r'^', include('videos.urls')),
    url(r'', include('social_auth.urls')),
    
)

# Hitcount URL
urlpatterns += patterns('',
    url(r'^ajax/hit/$', # you can change this url if you would like
        update_hit_count_ajax,
        name='hitcount_update_ajax'), # keep this name the same
)

# Vote AJAX
urlpatterns += patterns('',
    url(r'^ajax/vote/$', vote, name='vote_ajax'),
)

# Persistent Messages
urlpatterns += patterns('',
    url(r'^messages/', include('persistent_messages.urls')),
)
    
# Authorize.net URL's
urlpatterns += patterns('',
    url(r'^authorize/', include('authorize_net.urls')),
)

if not settings.DEBUG:
    handler500 = 'handlers.custom_500_handler'

# For serving static files during development
if settings.DEBUG:
    urlpatterns += patterns('',
            (r'(?:.*?/)?(?P<path>(applet|css|img|gfx|js|videos|tmp)/.+)$', 'django.views.static.serve', {'document_root': settings.MEDIA_ROOT }),
    )
