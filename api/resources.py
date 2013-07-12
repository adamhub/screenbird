from django.core.urlresolvers import reverse
from djangorestframework.compat import View
from djangorestframework.mixins import ResponseMixin
from djangorestframework.renderers import JSONRenderer
from django.contrib.auth.models import User
from djangorestframework.resources import ModelResource
from djangorestframework.response import Response
from accounts.models import UserProfile
from videos.models import Video, Channel

def is_allowed(user):
    """
    """
    return (user.userprofile.is_paid) or (user.userprofile.is_using_trial) or (user.userprofile.api_key)


class VideoResource(ResponseMixin, View):
    """
    Returns all videos under the account of the api_key provided.
    The format of the response is in JSON.
    """    
    renderers = [JSONRenderer,]
    csrf_exempt = True
    def get(self, request):
        key = request.GET.get('api_key', None)
        account_id = -1
        if key:
            userprofile = None
            try:
                userprofile = UserProfile.objects.get(api_key=key)
                account_id = userprofile.user_id
            except:
                pass
            if userprofile:
                user = None
                try:
                    user = User.objects.get(pk=account_id)
                except:
                    pass
                if user:
                    if is_allowed(user):
                        videos = Video.objects.filter(uploader__id=account_id)
                        json_videos = []
                        for video in videos:
                            channel_name = None
                            if video.channel:
                                channel_name = video.channel.name
                            json_videos.append(
                                                {
                                                    'id':video.id,
                                                    'channel':channel_name,
                                                    'url':video.get_absolute_url(),
                                                    'title':video.title,
                                                    'embed_code':video.get_embed_code()
                                                }
                                              )
                        response = Response(200, {'success':True,'videos':json_videos} )
                    else:
                        response = Response(401)
                else:
                     response = Response(401)
            else:
                response = Response(401)
        else:
            response = Response(400)
        return self.render(response)
        
        
class ChannelVideoResource(ResponseMixin, View):
    """
    Returns all videos under the channel of an account of the api_key provided.
    The format of the response is in JSON.
    """    
    
    renderers = [JSONRenderer,]
    csrf_exempt = True
    
    
    def get(self, request):
        channel_link = request.GET.get('channel_link', None)
        key = request.GET.get('api_key', None)
        account_id = -1
        if key and channel_link:
            userprofile = None
            try:
                userprofile = UserProfile.objects.get(api_key=key)
                account_id = userprofile.user_id
            except:
                pass
            channel = None
            try:
                channel = Channel.objects.get(api_link=channel_link)
            except:
                pass
            
            if channel:
                if (channel.owner.id == account_id) and is_allowed(channel.owner):
                    videos = Video.objects.filter(channel=channel)
                    json_videos = []
                    for video in videos:
                        channel_name = None
                        if video.channel:
                            channel_name = video.channel.name
                        json_videos.append(
                                            {
                                                'id':video.id,
                                                'channel':channel_name,
                                                'url':video.get_absolute_url(),
                                                'title':video.title,
                                                'embed_code':video.get_embed_code()
                                            }
                                          )
                    response = Response(200, {'success':True,'videos':json_videos} )
                else:
                    response = Response(401)
            else:
                response = Response(401)
        else:
            response = Response(401)
            
        
        return self.render(response)
        

class LatestVideoResource(ResponseMixin, View):
    """
    Returns the latest video under the account of the api_key provided.
    The format of the response is in JSON.
    """    
    renderers = [JSONRenderer,]
    csrf_exempt = True
    def get(self, request):
        key = request.GET.get('api_key', None)
        account_id = -1
        if key:
            userprofile = None
            try:
                userprofile = UserProfile.objects.get(api_key=key)
                account_id = userprofile.user_id
            except:
                pass
            if userprofile:
                user = None
                try:
                    user = User.objects.get(pk=account_id)
                except:
                    pass
                if user:
                    if is_allowed(user):
                        videos = Video.objects.filter(uploader__id=account_id).order_by('-created')[:1:]
                        json_videos = []
                        for video in videos:
                            channel_name = None
                            if video.channel:
                                channel_name = video.channel.name
                            json_videos.append(
                                                {
                                                    'id':video.id,
                                                    'channel':channel_name,
                                                    'url':video.get_absolute_url(),
                                                    'title':video.title,
                                                    'embed_code':video.get_embed_code()
                                                }
                                              )
                        response = Response(200, {'success':True,'videos':json_videos} )
                    else:
                        response = Response(401)
                else:
                     response = Response(401)
            else:
                response = Response(401)
        else:
            response = Response(400)
        return self.render(response)

        
        
class LatestChannelVideoResource(ResponseMixin, View):
    renderers = [JSONRenderer,]
    csrf_exempt = True
    
    
    def get(self, request):
        key = request.GET.get('api_key', None)
        channel_link = request.GET.get('channel_link', None)
        account_id = -1
        if key and channel_link:
            userprofile = None
            try:
                userprofile = UserProfile.objects.get(api_key=key)
                account_id = userprofile.user_id
            except:
                pass
            channel = None
            try:
                channel = Channel.objects.get(api_link=channel_link)
            except:
                pass
            
            if channel:
                if (channel.owner.id == account_id) and is_allowed(channel.owner):
                    videos = Video.objects.filter(channel=channel).order_by('-created')[:1:]
                    json_videos = []
                    for video in videos:
                        channel_name = None
                        if video.channel:
                            channel_name = video.channel.name
                        json_videos.append(
                                            {
                                                'id':video.id,
                                                'channel':channel_name,
                                                'url':video.get_absolute_url(),
                                                'title':video.title,
                                                'embed_code':video.get_embed_code()
                                            }
                                          )
                    response = Response(200, {'success':True,'videos':json_videos} )
                else:
                    response = Response(401)
            else:
                response = Response(401)
        else:
            response = Response(401)
            
        
        return self.render(response)
