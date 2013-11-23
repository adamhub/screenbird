from django.conf.urls.defaults import patterns, url

urlpatterns = patterns('authorize_net.views',
    url(r'^monthly/$', 'authorize_monthly', name='authorize_monthly'),
    url(r'^yearly/$', 'authorize_yearly', name='authorize_yearly'),
)
