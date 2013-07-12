from django.conf.urls.defaults import patterns, include, url
from django.conf import settings
from django.views.generic.simple import direct_to_template


urlpatterns = patterns('auth_login.views',
    url(r'^$', 'index', {'template':'home.html'}, name='index'),
    url(r'^info/$', 'info', name='info'),
    url(r'^error/$', 'error', name='error'),
    url(r'^logout/$', 'logout', name='logout'),
    url(r'^register/$', 'register', name='register'),
    url(r'^login/$', 'login', name='login'),
    url(r'^login_register/$', 'login_register', name='login_register'),
    url(r'^login_auth/$', 'login_auth', name='login_auth'),
    url(r'^new_account/$', 'getting_started', name='getting_started'),
    url(r'^getting_started/$', direct_to_template, {'template': 'intro.html'}, name='introduction'),
    url(r'^robots.txt$', 'robots', name='robots'),
    url(r'^auth_login/(?P<provider>[-\w]+)/$', 'social_auth_begin', name='social_auth_begin'),
)

if settings.DEBUG:
    urlpatterns += patterns('',
            (r'(?:.*?/)?(?P<path>(css|img|js|videos)/.+)$', 'django.views.static.serve', {'document_root': settings.MEDIA_ROOT }),
    )
