from django.conf.urls.defaults import patterns, include, url
from django.conf import settings

urlpatterns = patterns('videos.views',
    # Video List
    url(r'^videolist/?$', 'video_list', name='videos'),
    url(r'^videolist/(?P<username>[\w|\W]+)/?$', 'user_videos', name='user_videos'),

    # Video Record and Upload (Python and Java)
    url(r'^upload/?$', 'my_video_upload', name='upload-video'),
    url(r'^upload-youtube/(?P<slug>[-\w]+)/$', 'upload_youtube', name='upload-youtube'),
    url(r'^video/upload/?$', 'java_upload', name='java-upload'),
    url(r'^record/?$', 'my_video_recording', name='record-video'),
    url(r'^progress/(?P<slug>[-\w]+)/$', 'get_encoding_progress', name='encoding_progress'),
    url(r'^cprogress/(?P<slug>[-\w]+)/$', 'get_cocreate_progress', name='cocreate_progress'),

    url(r'^cocreate/(?P<cocreate_id>\d+)/$', 'cocreate', name='cocreate'),
    url(r'^cocreate/(?P<cocreate_id>\d+)/(?P<section_idx>\d+)/$', 'cocreate', name='cocreate_section'),
    url(r'^cocreate/new/?$', 'cocreate_new', name='cocreate_new'),
    url(r'^cocreate/(?P<cocreate_id>\d+)/add/?$', 'cocreate_section', name='cocreate_newsection'),
    url(r'^cocreate/edit_section/(?P<section_id>\d+)/(?P<field>\w+)/?$', 'section_edit', name='section_edit'),
    url(r'^cocreate/edit_section/(?P<section_id>\d+)/$', 'section_edit_in_outline', name='section_edit_in_outline'),
    url(r'^cocreate/edit_section/ajax-show-edit-form/$', 'ajax_show_edit_form', name='ajax_show_edit_form'),
    url(r'^cocreate/delete_section/ajax-delete-section/$', 'ajax_delete_section', name='ajax_delete_section'),
    url(r'^cocreate/edit/(?P<cocreate_id>\d+)/(?P<field>\w+)/?$', 'cocreate_edit', name='cocreate_edit'),
    url(r'^cocreate/(?P<cocreate_id>\d+)/compile/?$', 'cocreate_compile', name="cocreate_compile"),
    url(r'^cocreate/update-outline/(?P<slug>[-\w]+)/?$', 'update_cocreate_outline', name='update_cocreate_outline'),

    # View Videos
    url(r'^content/(?P<video_id>\d+)/$', 'video_content', name='get-video-content'),
    url(r'^(?P<slug>[-\w]+)/$', 'view', name='watch-video'),
    url(r'^(?P<slug>[-\w]+)/save-outline/$', 'save_outline', name='save-outline'),
    url(r'^(?P<slug>[-\w]+)/get-outline/$', 'get_outline', name='get-outline'),
    url(r'^embed-video-page/(?P<owner_id>[-\w]+)/(?P<slug>[-\w]+)/$', 'view', {'template':'video_watch_embed.html'}, name='video-watch-embed'),
    url(r'^embed/(?P<slug>[-\w]+)/$', 'view', {'template':'video_embed.html'}, name='video-embed'),
    url(r'^embed/(?P<slug>[-\w]+)\.js', 'view', {'template': 'video_embed.js', 'mimetype': 'text/javascript'}, name='video-embed-js'),
    url(r'^manager-approve/(?P<slug>[-\w]+)/$', 'approval_link', name='approval-link'),
    url(r'^admin-confirm-delete/(?P<slug>[-\w]+)/$', 'confirm_delete_link', name='confirm-delete-link'),
    url(r'^approve/(?P<slug>[-\w]+)/$', 'approve', name='approve-video'),
    url(r'^delete/(?P<slug>[-\w]+)/$', 'delete', name='delete-video'),
    url(r'^(?P<slug>[-\w]+)/youtube_proccessing$', 'youtube_processing', name='youtube-processing'),
    url(r'^(?P<slug>[-\w]+)/yt_url_removed/(?P<yt_id>[-\w]+)$', 'youtube_url_deleted', name='youtube-url-deleted'),
    url(r'^is-available/(?P<slug>[-\w]+)/(?P<version>(web|mobile))/$', 'ajax_video_is_available', name='is-available'),
    url(r'^set-available/(?P<slug>[-\w]+)/(?P<version>(web|mobile|cocreate))/$', 'set_to_available', name='set-available'),
    url(r'^set-encoding-ec2/(?P<slug>[-\w]+)/(?P<ec2_node>[-\w.]+)/$', 'set_encoding_ec2', name='set-encoding-ec2'),
    url(r'^set-duration/(?P<slug>[-\w]+)/$', 'set_duration', name='set-duration'),
    url(r'^set-encode-time/(?P<slug>[-\w]+)/$', 'set_encode_time', name='set-encode-time'),
    url(r'^set-cocreate-node/(?P<slug>[-\w]+)/(?P<cocreate_node>[-\w.]+)/$', 'set_cocreate_node', name='set-cocreate-node'),
    url(r'^change-recording-link/(?P<user_id>[-\w]+)/$', 'change_recording_link', name='change-rlink'),
    url(r'^embed-code/(?P<user_id>[-\w]+)/$', 'video_page_embed_code', name='video-page-embed-code'),

    # Video Download
    url(r'^(?P<slug>[-\w]+)/download/$', 'download', name='download-video'),

    # Manage Videos
    url(r'^manage/edit/(?P<slug>[-\w]+)/$', 'edit', name='my-videos-edit'),
    url(r'^manage/?$', 'my_video_list', name='my-videos'),

    # Ajax
    url(r'ajax_close_recorder/?$','ajax_close_recorder', name='ajax_close_recorder'),

    # S3 encode
    url(r'encode-video/(?P<slug>[-\w]+)/$', 'encode_video', name="encode-video"),
)

if settings.DEBUG:
    urlpatterns += patterns('',
            (r'(?:.*?/)?(?P<path>(applet|css|img|js|videos)/.+)$', 'django.views.static.serve', {'document_root': settings.MEDIA_ROOT }),
    )
