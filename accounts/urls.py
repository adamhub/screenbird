from django.conf.urls.defaults import patterns, include, url
from django.conf import settings

from accounts.forms import PasswordResetForm

urlpatterns = patterns('accounts.views',
    url(r'^verify/$', 'verify_details', name='verify_details'),
    url(r'^info/$', 'account_info', name='account_info'),
    url(r'^info/(?P<premium>\w+)/$', 'account_info', name='account_info'),
    url(r'^manage/$', 'manage_account', name='manage_account'),
    url(r'^upgrade_trial/$', 'upgrade_trial', name='upgrade_trial'),
    url(r'^pastevid-ipn-handler/', include('paypal.standard.ipn.urls')), #Paypal IPN URL for Pastevid
    url(r'^paypal_success/$','paypal_success',name='paypal_success'),
    url(r'^paypal_cancel/$','paypal_cancel',name='paypal_cancel'),
    url(r'^channels/new/$', 'create_channel', name='create_channel'),
    url(r'^channels/(?P<channel_id>\d+)/add/$', 'add_channel_member', name='add_channel_member'),
    url(r'^channels/(?P<channel_id>\d+)/remove/(?P<user_id>\d+)/$', 'remove_channel_member', name='remove_channel_member'),
    url(r'^channels/cancel/(?P<invitation_id>\d+)/$', 'cancel_invitation', name='cancel_invitation'),
    url(r'^channels/(?P<channel_id>\d+)/update/$', 'update_channel', name='update_channel'),
    url(r'^channels/(?P<channel_id>\d+)/update-perms/$', 'update_channel_member_permissions', name='update_channel_member_permissions'),
    url(r'^channels/(?P<channel_id>\d+)/add-video/(?P<video_id>\d+)/$', 'add_video_to_channel', name='add_video_to_channel'),
    url(r'^cocreate/(?P<cocreate_id>\d+)/member/add/$', 'add_cocreate_member', name='add_cocreate_member'),

    #Accounts Social Connect
    url(r'^social_app_remove/$', 'social_app_remove', name='social_app_remove'),
    url(r'^social_disconnect/(?P<provider>[-\w]+)/$', 'social_disconnect', name='social_disconnect'),
    url(r'^social_reconnect/(?P<provider>[-\w]+)/$', 'social_reconnect', name='social_reconnect'),
    url(r'^social_connect/(?P<provider>[-\w]+)/$', 'social_connect', name='social_connect'),
    url(r'^account_facebook_connect/$', 'facebook_connect', name='facebook_connect'),
    url(r'^account_twitter_connect/$', 'twitter_connect', name='twitter_connect'),
)

#Forgot Password/Username URL
urlpatterns += patterns('',
    (r'^password_reset/$', 'django.contrib.auth.views.password_reset',
        {'post_reset_redirect' : '/accounts/password_reset/done/', 'password_reset_form': PasswordResetForm,}),
    (r'^password_reset/done/$', 'django.contrib.auth.views.password_reset_done'),
    (r'^reset/(?P<uidb36>[0-9A-Za-z]+)-(?P<token>.+)/$', 'django.contrib.auth.views.password_reset_confirm',
        {'post_reset_redirect' : '/accounts/reset/done/'}),
    (r'^reset/done/$', 'django.contrib.auth.views.password_reset_complete'),
)

if settings.DEBUG:
    urlpatterns += patterns('',
            (r'(?:.*?/)?(?P<path>(applet|css|img|js|videos)/.+)$', 'django.views.static.serve', {'document_root': settings.MEDIA_ROOT }),
    )
