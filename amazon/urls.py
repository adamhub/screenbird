from django.conf.urls.defaults import patterns, include, url

urlpatterns = patterns('amazon.views',
    url(r'^ec2-ready/(?P<instance_id>[-\w]+)/$', 'ec2_ready', name='ec2-ready'),
)
