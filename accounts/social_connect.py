import settings
import tweepy
import base64
import hashlib
import hmac
import simplejson as json
from facepy import SignedRequest, GraphAPI
from django.http import HttpResponse, HttpResponseRedirect

from social_auth.models import UserSocialAuth


def twitter_get_auth_url(request):
    auth = tweepy.OAuthHandler(settings.TWITTER_CONSUMER_KEY, settings.TWITTER_CONSUMER_SECRET)
    auth_url = auth.get_authorization_url()
    request.session['request_token'] = (auth.request_token.key, auth.request_token.secret)
    return HttpResponseRedirect(auth_url)

def check_facebook_connection(user):
    """Checks facebook connection exists with the app if not,
    then returns False
    """
    try:
        user_social = UserSocialAuth.objects.get(user=user, provider='facebook')
        extra_data = eval(str(user_social.extra_data))
        access_token = extra_data['access_token']
        graph = GraphAPI(access_token)
        try:
            graph = graph.get('me/')
        except GraphAPI.Error:
            connected = False
        else:
            connected = True
    except UserSocialAuth.DoesNotExist:
        connected = False
    return connected

def check_twitter_connection(user):
    """Checks twitter connection exists with the app if not,
    then returns False
    """
    try:
        user_social = UserSocialAuth.objects.get(user=user, provider='twitter')
        extra_data = eval(str(user_social.extra_data))
        access_tokens = extra_data['access_token']
        access_token_list = access_tokens.split('oauth_token_secret=')[1].split('&oauth_token=')
        secret = access_token_list[0]
        key = access_token_list[1]
        auth = tweepy.OAuthHandler(settings.TWITTER_CONSUMER_KEY, settings.TWITTER_CONSUMER_SECRET)
        auth.set_access_token(key, secret)
        api = tweepy.API(auth)
        connected = api.verify_credentials()
    except UserSocialAuth.DoesNotExist:
        connected = False
    if connected:
        connected = True
    return connected

def base64_url_decode(inp):
    padding_factor = (4 - len(inp) % 4) % 4
    inp += "="*padding_factor 
    return base64.b64decode(unicode(inp).translate(dict(zip(map(ord, u'-_'), u'+/'))))

def parse_signed_request(signed_request, secret):
    """The signed_request parameter is a simple way to make sure that the data 
       you're receiving is the actual data sent by Facebook. It is signed using 
       your application secret which is only known by you and Facebook. If someone 
       were to make a change to the data, the signature would no longer validate as 
       they wouldn't know your application secret to also update the signature.

       Code snippet parsing "signed_request"
    """

    l = signed_request.split('.', 2)
    encoded_sig = l[0]
    payload = l[1]

    sig = base64_url_decode(encoded_sig)
    data = json.loads(base64_url_decode(payload))

    if data.get('algorithm').upper() != 'HMAC-SHA256':
        log.error('Unknown algorithm')
        return None
    else:
        expected_sig = hmac.new(secret, msg=payload, digestmod=hashlib.sha256).digest()

    if sig != expected_sig:
        return None
    else:
        log.debug('valid signed request received..')
        return data
