from django.template import RequestContext
from django.http import HttpResponseRedirect, HttpResponse, HttpResponseServerError, Http404
from django.core.urlresolvers import reverse
from django.contrib.auth.decorators import login_required
from django.shortcuts import render_to_response, get_object_or_404, redirect
from django.views.decorators.csrf import csrf_exempt
from django.utils import simplejson

from videos.models import Video

@login_required
def vote(request):
    video_id = int(request.POST.get('id'))
    vote_type = request.POST.get('type')
    vote_action = request.POST.get('action')

    try:
        video = Video.objects.get(id=video_id)
    except Video.DoesNotExist:
        video = None

    if video:
        user = request.user
        if vote_type == 'down':
            action = False
        else:
            action = True
        thumbs_up = video.thumbs.add(user, thumbs_up=action)
        likes = video.thumbs.thumbs_up_count()
        dislikes = video.thumbs.thumbs_down_count()
        return HttpResponse(
            simplejson.dumps({'likes':likes,'dislikes':dislikes}),
            content_type = 'application/javascript; charset=utf8'
        )
        #return HttpResponse({'likes':likes,'dislikes':dislikes})
    else:
        return HttpResponse('Error - Video does not exist!')
