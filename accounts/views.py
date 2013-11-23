import cgi
import datetime
import random
import logging
import string
import tweepy
import urllib

from django.template import RequestContext
from django.template.loader import render_to_string
from django.http import HttpResponse, HttpResponseRedirect, HttpResponseServerError
from django.conf import settings
from django.contrib.auth import login, REDIRECT_FIELD_NAME
from django.contrib.auth.decorators import login_required
from django.contrib import messages
from django.shortcuts import (
    get_object_or_404,
    redirect,
    render,
    render_to_response,
)
from django.contrib.auth.models import User
from django.contrib.auth import logout
from django.core.urlresolvers import reverse
from django.core.mail import EmailMultiAlternatives, send_mail
from django.utils import simplejson
from django.db import IntegrityError, transaction
from django.views.decorators.http import require_POST
from django.contrib.auth.signals import user_logged_in
from django.dispatch import receiver
from django.contrib.admin.views.decorators import staff_member_required

import persistent_messages
from persistent_messages.models import Message
from facepy import SignedRequest, GraphAPI
from social_auth.backends import get_backend
from social_auth.models import UserSocialAuth

from accounts.forms import (
    PaymentInformationForm,
    PasswordChangeForm,
    UsernameChangeForm,
    UserProfileUpdateForm,
    PayPalPaymentsForm,
)
from accounts import authorize
from accounts.social_connect import (
    parse_signed_request,
    check_twitter_connection,
    twitter_get_auth_url,
    check_facebook_connection,
)
from accounts.models import AccountLevel, UserProfile
from api.utils import generate_api_key
from auth_login.forms import (
    RegistrationNoUsernameForm, 
    LoginForm, 
    generate_username,
)
from videos.models import (
    Channel,
    CoCreate,
    CocreateOption,
    ConfirmationInfo,
    PopUpInformation,
    Video,
)
from videos.utils import is_expired
from videos.forms import (
    ChannelForm, 
    ChannelOptionForm, 
    InvitationForm, 
    UpdateChannelForm, 
    generate_channel_slug,
    generate_random_string, 
)


API_KEY_LEN = 12
CONNECTED_STATUS = 'connected'
DAYS_BEFORE_EXPIRATION = 60
ERROR_STATUS = 'error'
DENIED_STATUS = 'denied'
USERSOCIALAUTH_PASS = '!'

DEFAULT_REDIRECT = getattr(settings, 'SOCIAL_AUTH_LOGIN_REDIRECT_URL', '') or \
                   getattr(settings, 'LOGIN_REDIRECT_URL', '')

logger = logging.getLogger('videos.views')

@login_required
def account_info(request, premium=False, template="account_info.html"):
    """
    Displays the user's account information.
    Also updates logged in user's Twitter and Facebook connections.
    
    """

    user = request.user
    userprofile = user.userprofile
    provider_list = []

    try:
        oauth_user = UserSocialAuth.objects.get(user=user)
        multiple_providers = False
    except UserSocialAuth.DoesNotExist:
        oauth_user = None
        multiple_providers = False
    except UserSocialAuth.MultipleObjectsReturned:
        oauth_user = UserSocialAuth.objects.filter(user=user)
        for oauthuser in oauth_user:
            provider_list.append(oauthuser.provider)
        multiple_providers = True

    # Check if twitter connection exists, 
    # If not then sets twitter connection to false
    if userprofile and userprofile.twitter_connect:
        connected = check_twitter_connection(user)
        if not connected:
            userprofile.twitter_connect = False
            userprofile = userprofile.save()
            try:
                twit_auth = UserSocialAuth.objects.get(user=user, 
                                                       provider='twitter')
                twit_auth.delete()
            except UserSocialAuth.DoesNotExist:
                pass
    # Check if facebook connection exists,
    # If not then sets facebook connection to false
    if userprofile and userprofile.facebook_connect:
        connected = check_facebook_connection(user)
        if not connected:
            userprofile.facebook_connect = False
            userprofile = userprofile.save()
            if user.password != USERSOCIALAUTH_PASS:
                try:
                    fb_auth = UserSocialAuth.objects.get(user=user, 
                                                         provider='facebook')
                    fb_auth.delete()
                except UserSocialAuth.DoesNotExist:
                    pass

    # Get popup information
    info = {}
    popup = PopUpInformation.objects.all()
    for po in popup:
        info[po.html_id] = po.message

    context = {
        'popupinfo': info,
        'oauth_user': oauth_user,
        'userprofile': userprofile,
        'provider_list': provider_list,
        'multiple_providers': multiple_providers,
        'FACEBOOK_APPLICATION_ID': settings.FACEBOOK_APP_ID,
        'premium': premium,
    }
    return render(request, template, context)


@login_required
def facebook_connect(request):
    """
    Method that takes the access token of facebook return URL.
    Then creates a UserSocialAuth object for the user to connect it to his/her
    account.
    
    """
    provider = 'facebook'
    if request.method == 'GET':
        if 'code' in request.GET:
            code = request.GET['code']
            if code:
                redirect_url = 'http://%s%s' % (request.META['HTTP_HOST'],
                                                reverse('facebook_connect'))
                url = "https://graph.facebook.com/oauth/access_token" \
                      "?client_id=%s&redirect_uri=%s&client_secret=%s&code=%s"
                url = url % (settings.FACEBOOK_APP_ID, redirect_url, 
                             settings.FACEBOOK_API_SECRET, code)
                resp = cgi.parse_qs(urllib.urlopen(url).read())
                access_token = resp['access_token'][0]
                graph = GraphAPI(access_token)
                social_auth_dict = {}
                social_auth_dict['access_token'] = access_token
                graph_dict = graph.get('me/')
                facebook_id = graph_dict['id']
                social_auth_dict['expires'] = None
                social_auth_dict['id'] = facebook_id
                extra_data = simplejson.dumps(social_auth_dict)
                user = request.user
                try:
                    existing_user = UserSocialAuth.objects.get(
                            provider=provider, uid=facebook_id)
                except UserSocialAuth.DoesNotExist:
                    logger.debug("No existing social auth found")
                    user_social_auth = UserSocialAuth.objects.create(
                            provider=provider, uid=facebook_id, user=user)
                    user_social_auth.extra_data = extra_data
                    user_social_auth.save()
                    userprofile = UserProfile.objects.get(user=user)
                    userprofile.facebook_connect = True
                    userprofile.save()
                    status = CONNECTED_STATUS
                else:
                    logger.debug("Existing social auth found: %s " % existing_user.user.email)
                    if user.password == USERSOCIALAUTH_PASS:
                        logger.debug("Signed up via facebook")
                        user_social_auth = UserSocialAuth.objects.get(
                                provider=provider, uid=facebook_id, user=user)
                        user_social_auth.extra_data = extra_data
                        user_social_auth.save()
                        userprofile = UserProfile.objects.get(user=user)
                        userprofile.facebook_connect = True
                        userprofile.save()
                        status = CONNECTED_STATUS
                    else:
                        logger.debug("Existing account already found")
                        status = ERROR_STATUS
                redirect_url = "%s?status=%s" % (
                        reverse('social_connect', args=[provider,]),status)
                return HttpResponseRedirect(redirect_url)
    return HttpResponseRedirect(reverse('account_info'))


@login_required
def twitter_connect(request):
    """
    Method using tweepy for OAuth handling and creating a new UserSocialAuth 
    object for the user's account. 
    Also set's twitter_connect to True for posting videos on twitter every time 
    a user uploads a new video.
    
    """
    CONSUMER_KEY = settings.TWITTER_CONSUMER_KEY
    CONSUMER_SECRET = settings.TWITTER_CONSUMER_SECRET
    auth = tweepy.OAuthHandler(CONSUMER_KEY, CONSUMER_SECRET)
    provider = 'twitter'
    if request.method == 'POST':
        auth_url = auth.get_authorization_url()
        request.session['request_token'] = (auth.request_token.key, 
                                            auth.request_token.secret)
        return HttpResponseRedirect(auth_url)
    if request.method == 'GET':
        token = request.session['request_token']
        request.session.delete('request_token')
        auth.set_request_token(token[0], token[1])
        verifier = request.GET.get('oauth_verifier')
        if verifier:
            auth.get_access_token(verifier)
            # Based on social-auth app, access token for twitter
            access_token = 'oauth_token_secret=%s&oauth_token=%s' % (
                    auth.access_token.secret, auth.access_token.key)
            api = tweepy.API(auth)
            twitter_id = api.me().id
            social_auth_dict = {}
            social_auth_dict['access_token'] = access_token
            social_auth_dict['expires'] = None
            social_auth_dict['id'] = twitter_id
            extra_data = simplejson.dumps(social_auth_dict)
            user = request.user
            try:
                existing_user = UserSocialAuth.objects.get(provider=provider, 
                                                           uid=twitter_id)
            except UserSocialAuth.DoesNotExist:
                user_social_auth = UserSocialAuth.objects.create(provider=provider, 
                                                                 uid=twitter_id, 
                                                                 user=user)
                user_social_auth.extra_data = extra_data
                user_social_auth.save()
                userprofile = UserProfile.objects.get(user=user)
                userprofile.twitter_connect = True
                userprofile.save()
                status = CONNECTED_STATUS
            else:
                status = ERROR_STATUS
        else:
            status = DENIED_STATUS
        redirect_url = "%s?status=%s" % (
                reverse('social_connect', args=[provider,]),status)
    return HttpResponseRedirect(redirect_url)


@login_required
def social_connect(request, provider):
    """
    Returns the result of a social connection with a provider.
    
    A `status` is passed in the GET request to indicate the result of social
    connections. Possible values are
      - `connected` for a successful connection; and
      - `error` for an unsuccessful connection
      
    Returns to account information page when an invalid or no `status` value
    is supplied in the GET request.
    
    """
    if 'status' in request.GET:
        status = request.GET['status']
        if status == CONNECTED_STATUS:
            success = True
            message = \
"""You have succesfully linked your %s account to your Screenbird Account.
Created videos would be posted automatically on your %s account.
Thanks.
"""
            message = message % (provider.capitalize() ,provider.capitalize())
        elif status == ERROR_STATUS:
            success = False
            message = \
"""Error: %s Account is already linked to an existing Screenbird Account.
Contact Admin for more details. Thanks
"""
            message = message % provider.capitalize()
        elif status == DENIED_STATUS:
            success = True
            message = \
"""You have chosen not to link your %s account to your Screenbird account.
"""
            message = message % provider.capitalize()
        else:
            # Invalid status value
            return HttpResponseRedirect(reverse('account_info'))
    else:
        # No status in request.GET
        return HttpResponseRedirect(reverse('account_info'))
    context = {
        'prompt_message': message,
        'success': success,
        'provider': provider,
    }
    return render(request, 'social_connect.html', context)


@login_required
def social_disconnect(request, provider):
    """
    Disassociates a user from a social connection provider and returns to
    the account information page.
    
    Accepted `provider` values:
      - `facebook`
      - `twitter`
    
    Does nothing if an unaccepted `provider` value is supplied.
    
    """
    user = request.user
    userprofile = UserProfile.objects.get(user=user)
    if user:
        if provider == 'facebook':
            userprofile.facebook_connect = False
            if user.password != USERSOCIALAUTH_PASS:
                fb_auth = UserSocialAuth.objects.get(user=user,
                                                     provider='facebook')
                fb_auth.delete()
        elif provider == 'twitter':
            userprofile.twitter_connect = False
            if user.password != USERSOCIALAUTH_PASS:
                twit_auth = UserSocialAuth.objects.get(user=user, 
                                                       provider='twitter')
                twit_auth.delete()
        else:
            return HttpResponseRedirect(reverse('account_info'))
        userprofile.save()
    return HttpResponseRedirect(reverse('account_info'))


@login_required
def social_reconnect(request, provider):
    user = request.user
    userprofile = UserProfile.objects.get(user=user)
    if user:
        if provider == 'facebook':
            userprofile.facebook_connect = True
            userprofile = userprofile.save()
            connected = check_facebook_connection(user)
            if not connected:
                if user.password != USERSOCIALAUTH_PASS:
                    try:
                        fb_auth = UserSocialAuth.objects.get(user=user, 
                                                             provider='facebook')
                        fb_auth.delete()
                    except UserSocialAuth.DoesNotExist:
                        pass
                redirect_url = 'http://%s%s' % (request.META['HTTP_HOST'],
                                                reverse('facebook_connect'))
                url = "https://graph.facebook.com/oauth/authorize?client_id" \
                      "=%s&scope=offline_access,publish_stream&redirect_uri=%s"
                url = url % (settings.FACEBOOK_APP_ID, redirect_url)
                return HttpResponseRedirect(url)
        elif provider == 'twitter':
            userprofile.twitter_connect = True
            userprofile = userprofile.save()
            connected = check_twitter_connection(user)
            if not connected:
                try:
                    social_auth = UserSocialAuth.objects.get(user=user, 
                                                             provider='twitter')
                    social_auth.delete()
                except UserSocialAuth.DoesNotExist:
                    pass
                return twitter_get_auth_url(request)
        else:
            return HttpResponseRedirect(reverse('account_info'))
    return HttpResponseRedirect(reverse('account_info'))


def social_app_remove(request):
    """Deauthorization Callback URL for Facebook
    Note: Will check on this further, not working yet
    """
    if request.method == 'POST':
        if 'signed_request' in request.POST:
            signed_request = request.POST['signed_request']
            data = parse_signed_request(signed_request, 
                                        settings.FACEBOOK_API_SECRET)
            fb_uid = data['user_id']
            social_auth = UserSocialAuth.objects.get(uid=fb_uid, 
                                                     provider='facebook')
            user = social_auth.user
            userprofile = UserProfile.objects.get(user=user)
            userprofile.facebook_connect = False
            userprofile.save()
            if user.password != USERSOCIALAUTH_PASS:
                social_auth.delete()
    return HttpResponse('OK!')


@login_required
def manage_account(request):
    """
    Updates the user's account information.
    If user is an oauth_user, user cannot update password.
    
    """
    user = request.user
    oauth_user = UserSocialAuth.objects.filter(user=user)
    if oauth_user.exists() and oauth_user.count() >= 1:
        oauth_user = oauth_user[0]
    else:
        oauth_user = None

    try:
        userprofile = UserProfile.objects.get(user=user)
    except UserProfile.DoesNotExist:
        userprofile = UserProfile(user=user)
        userprofile = userprofile.save()
    pass_form = PasswordChangeForm(user=user)
    userprof_form = UserProfileUpdateForm()
    user_form = UsernameChangeForm()
    context = {}
    if request.method == "POST":
        userprof_form = UserProfileUpdateForm(request.POST, instance=userprofile)
        user_form = UsernameChangeForm(request.POST, instance=user)
        pass_form = PasswordChangeForm(user=user, data=request.POST)
        if 'username_update' in request.POST:
            if user_form.is_valid():
                user_form.save()
                return redirect('account_info')
        elif 'userprofile' in request.POST:
            if userprof_form.is_valid():
                userprof_form.save()
                return redirect('account_info')
        elif 'change_password' in request.POST:
            if pass_form.is_valid():
                pass_form.save()
                return redirect('account_info')
    else:
        userprof_form = UserProfileUpdateForm(instance=userprofile)
        user_form = UsernameChangeForm(instance=user)
        pass_form = PasswordChangeForm(user=user)
    context['oauth_user'] = oauth_user
    context['pass_form'] = pass_form
    context['userprof_form'] = userprof_form
    context['user_form'] = user_form
    return render(request, 'account_manage.html', context)


def paypal_upgrade(request, option=False):
    """
    View that handles upgrading from a Free account to a Paid account using paypal
    
    """
    context={}
    upgrade = request.GET.get('upgrade', False)
    try:
         user = User.objects.get(username=request.user.username)
    except User.DoesNotExist:
         user = None
    context['user'] = user
    url = request.META['HTTP_HOST']

    # Monthly Subscription Paypal dict
    monthly = {
        "cmd": "_xclick-subscriptions",
        "business": settings.PAYPAL_RECEIVER_EMAIL,
        "a3": "9.00",                      # monthly price
        "p3": 1,                            # duration of each unit (depends on unit)
        "t3": "M",                          # duration unit ("M for Month")
        "src": "1",                         # make payments recur
        "sra": "1",                         # reattempt payment on payment error
        "no_note": "1",                     # remove extra notes (optional)
        "item_name": "Paid Account - Monthly Subscription",
        "notify_url": "http://%s/accounts/pastevid-ipn-handler/" % url, #Paypal IPN URL for Screenbird
        "return_url": "http://%s%s" % (url,reverse('paypal_success')),
        "cancel_return": "http://%s%s" % (url,reverse('paypal_cancel')),
        "rm": "2",
    }

    # Yearly Subscription Paypal dict
    yearly = {
        "cmd": "_xclick-subscriptions",
        "business": settings.PAYPAL_RECEIVER_EMAIL,
        "a3": "99.00",                     # annual price
        "p3": 1,                            # duration of each unit (depends on unit)
        "t3": "Y",                          # duration unit ("Y for Year")
        "src": "0",                         # make payments recur
        "sra": "1",                         # reattempt payment on payment error
        "no_note": "1",                     # remove extra notes (optional)
        "item_name": "Paid Account - One Year Subscription",
        "notify_url": "http://%s/accounts/pastevid-ipn-handler/" % url, #Paypal IPN URL for Screenbird
        "return_url": "http://%s%s" % (url,reverse('paypal_success')),
        "cancel_return": "http://%s%s" % (url,reverse('paypal_cancel')),
        "rm": "2",
    }

    if user:
        username = user.username
        try:
            userprofile = UserProfile.objects.get(user=user)
        except UserProfile.DoesNotExist:
            userprofile = UserProfile(user=user)
            userprofile = userprofile.save()
        if userprofile.account_level.video_validity:
            context['upgrade'] = upgrade
        context['userprofile'] = userprofile
        monthly["custom"] = username
        yearly["custom"] = username

        # Create the instance.
        form_month = PayPalPaymentsForm(initial=monthly, 
                                        button_type='pastevid_monthly')
        form_year = PayPalPaymentsForm(initial=yearly, 
                                       button_type='pastevid_yearly')
        context["form_month"] = form_month
        context["form_year"] = form_year
    if option == 'option':
        return render(request, "features_option.html", context)
    return render(request, "features.html", context)


@login_required
def paypal_success(request):
    """
    Displays the paypal success page.
    Needs to check if logged in first using `@login_required decorator`.
    
    """
    return render(request, "paypal_success.html", {})


@login_required
def paypal_cancel(request):
    """
    Displays page for cancelled paypal process.
    Needs to check if logged in first using `@login_required decorator`.
    
    """
    return render(request, "paypal_cancel.html", {})


@login_required
def upgrade_trial(request):
    """
    View that handles upgrading from a Free account to a Trial account.
    
    """
    userprofile = request.user.userprofile
    
    if userprofile.account_level.level == 'Paid':
        messages.error(request, 'You already have a Paid account.')
        return redirect('features')
    elif userprofile.account_level.level == 'Trial':
        messages.error(request, 'You already have a Trial account.')
        return redirect('features')
    # An account can only be upgraded to a Trial account once.
    elif userprofile.trial_signed_up:
        messages.error(request, 'You already tried a Trial account.')
        return redirect('features')
    
    trial_account = AccountLevel.objects.get(pk=4)
    
    userprofile.account_level = trial_account
    userprofile.trial_signed_up = True
    userprofile.trial_expiry_date = (datetime.date.today() + 
            datetime.timedelta(days=DAYS_BEFORE_EXPIRATION))
    userprofile.api_key = generate_api_key(API_KEY_LEN)
    userprofile.save()
    
    # Update user's videos expiration dates
    vids = Video.objects.filter(uploader=request.user)
    for vid in vids:
        is_expired(vid)
        
    messages.success(request, 'You have successfully upgraded to a Trial account.')
    return redirect('account_info')
    

@login_required
def upgrade_account(request):
    """
    View that handles upgrading from a Free account to a Paid account via
    Authorize.net
    
    Note: Authorize.net payment option is currently on backlog
    
    """
    error_message = ""
    success_message = ""
    if request.user.userprofile.account_level.level == 'Paid':
        error_message = "You already have a Paid account."

    if request.method == 'POST':
        form = PaymentInformationForm(request.POST)
        if form.is_valid():
            paid_account = AccountLevel.objects.get(pk=2)
            r = authorize.call_auth(
                paid_account,
                unicode(paid_account.authnet_item_price),
                data = { 
                    'card_number':form.cleaned_data['card_number'],
                    'expiry':form.cleaned_data['expiry_date'],
                    'card_code':form.cleaned_data['card_code'],
                    'first_name':form.cleaned_data['first_name'],
                    'last_name':form.cleaned_data['last_name'],
                    'company':form.cleaned_data['company'],
                    'address':form.cleaned_data['address'],
                    'city':form.cleaned_data['city'],
                    'state':form.cleaned_data['state'],
                    'province':form.cleaned_data['province'],
                    'country':form.cleaned_data['country'],
                    'zip_code':form.cleaned_data['zip_code'],
                    'email':form.cleaned_data['email'],
                    'phone':form.cleaned_data['phone']
                },
            )
            success_message = ""
            if r.split('|')[0] == '1':
                trans_id = r.split('|')[6]
                r = authorize.call_capture(trans_id)
                if r.split('|')[0] == '1':
                    error_message = ""
                    userprofile = UserProfile.objects.get(user__id=request.user.id)
                    if userprofile.account_level.level == 'Trial':
                        userprofile.trial_ended = True
                        userprofile.trial_expiry_date = None
                    userprofile.account_level = paid_account
                    api_key_len = UserProfile._meta.get_field('api_key')
                    userprofile.api_key = generate_api_key(api_key_len)
                    userprofile.save()
                    form = PaymentInformationForm(request.POST)
                    success_message = "Payment Accepted"
                    form = PaymentInformationForm(request.POST)

                    # Update user's videos expiration dates
                    vids = Video.objects.filter(uploader=request.user)
                    for vid in vids:
                        is_expired(vid)
                else:
                    error_message = r.split('|')[3]
                    form = PaymentInformationForm(request.POST)
            else:
                error_message = "%s" % (r.split('|')[3])
                form = PaymentInformationForm(request.POST)
    else:
        form = PaymentInformationForm()

    context = {
        'form': form, 
        'error_message':error_message, 
        'success_message':success_message,
    }
    return render(request, 'purchase.html', context)


@login_required
def verify_details(request):
    """
    View that handles details verification
    
    """
    if request.user.userprofile.account_level.level == 'Paid':
        error_message = "You already have a Paid account."
    if form.is_valid():
            paid_account = AccountLevel.objects.get(pk=2)
            r = authorize.call_auth(
                paid_account,
                unicode(paid_account.authnet_item_price),
                data = {
                    'card_number':form.cleaned_data['card_number'],
                    'expiry':form.cleaned_data['expiry_date'],
                    'card_code':form.cleaned_data['card_code'],
                    'first_name':form.cleaned_data['first_name'],
                    'last_name':form.cleaned_data['last_name'],
                    'company':form.cleaned_data['company'],
                    'address':form.cleaned_data['address'],
                    'city':form.cleaned_data['city'],
                    'state':form.cleaned_data['state/province'],
                    'country':form.cleaned_data['country'],
                    'zip_code':form.cleaned_data['zip_code'],
                    'email':form.cleaned_data['email'],
                    'phone':form.cleaned_data['phone']
                },
            )

    form = PaymentInformationForm(request.POST)

    context = {'form': form }
    return render(request, 'verify.html', context)


############### CHANNEL ####################
@login_required
def create_channel(request):
    """
    Creates a group.
    
    """
    if request.method == 'POST':
        form = ChannelForm(request.POST)
        if form.is_valid():
            channel = form.save(commit=False)
            channel.owner = request.user
            channel.channel_slug = generate_channel_slug(settings.CHANNEL_SLUG_LENGTH)
            channel.api_link = generate_api_key(API_KEY_LEN)
            channel.save()
            channel.owner.userprofile.channels.add(channel.id)
            return redirect(reverse('update_channel', args=[channel.id,]))
        else:
            context = {'form': form}
    else:
        form = ChannelForm()
        context = {'form': form}
    return render(request, 'create_channel.html', context)


@login_required
def update_channel(request, channel_id, invitation_form=None):
    """
    Allows the user to edit channel name and invite channel members.
    Note: This is visible only to the owner of the channel.
    
    """
    message = ""
    context = {}
    channel = get_object_or_404(Channel, id=channel_id)
    channel_members = list(channel.get_channel_members())

    form = UpdateChannelForm(instance=channel)
    invite_form = InvitationForm()
    
    # Get all invitations
    invitations = ConfirmationInfo.objects.filter(channel=channel)
    for channel_member in channel_members:
        # Do not allow my own permissions to be set
        if channel_member != request.user:
            channel_member.perm_form = ChannelOptionForm(
                channel=channel,
                user_profile=channel_member.userprofile,
                prefix='channel-%s' % channel_member.pk
            )
            status = 'pending'
            try:
                invitation = invitations.get(username=channel_member.username)
                status = invitation.status
            except:
                pass
            channel_member.status = status

    if (request.method == 'POST' and 
            request.POST.get('action','default') == 'delete'):
        videos = Video.objects.filter(id__in=request.POST.getlist('item'))
        videos.update(channel=None)
        video_names = [video.title for video in videos]

        if videos:
            message = " ".join(["The following video/s have been removed from this channel:",
                                        ", ".join(video_names), "."])
        """
        deleted, for_approval = [], []
        for video in videos:
            name = video.title
            if video.delete() is False:
                for_approval.append(name)
            else:
                deleted.append(name)
        if for_approval:
            message = " ".join(["Note that the removal of videos within a",
                            "channel needs the admin's approval first.\n"])
        if deleted:
            message = message + " ".join(["The following video/s have been deleted:",
                                                 ", ".join(deleted), "."])
        if for_approval:
            message = message + " ".join(["The following video/s are sent for approval to the admin:",
                                                 ", ".join(for_approval), "."])
        """

    elif request.method == 'POST':
        if not invitation_form:
            form = ChannelForm(request.POST, instance=channel)
            if form.is_valid():
                form.save()
                messages.success(request, 'Group name has been updated.')
            else:
                messages.error(request, 'Failed updating channel details.')
        else:
            # this one has been returned by the add_channel_member view
            if not invitation_form.is_valid():
                invite_form = invitation_form

    videos = [v for v in channel.video_set.all()]
    videos_to_add = Video.objects.filter(uploader=request.user).filter(channel=None).order_by('-updated')

    context = {
        'channel': channel,
        'channel_form': form,
        'invite_form': invite_form,
        'channel_members': channel_members,
        'videos': videos,
        'videos_to_add': videos_to_add,
        'message': message,
    }

    return render(request, 'update_channel.html', context)


@login_required
def add_channel_member(request, channel_id):
    """
    Invite channel members. 
    
    """

    context = {}
    channel = get_object_or_404(Channel, id=channel_id, owner=request.user)

    form = InvitationForm()
    if request.method == "POST":
        form = InvitationForm(request.POST)
        if form.is_valid():
            # Save confirmation data
            email = form.cleaned_data['email']
            status = 'pending'
            
            # Automatically adds the invited user to the channel; invited user still receives confirmation link
            # Update: Create the user when it does not exist.
            user_created = False
            try:
                invited_user = User.objects.get(email=email)
                new_username = ''
                new_password = ''
            except User.DoesNotExist:
                new_username = generate_username(email)
                new_password = generate_random_string(6)
                invited_user = User.objects.create_user(username=new_username, password=new_password, email=email)
                user_created = True
            
            invited_user_profile = invited_user.userprofile
            invited_user_profile.channels.add(channel)
            invited_user_profile.save()
            Message.objects.create(user=invited_user,
                                    from_user=request.user,
                                    level=persistent_messages.INFO,
                                    message="Congratulations! You are now a contributor on the %s channel. You can \
                                            create videos and save them to this channel after they are created. Now \
                                            you can enjoy the features of a premium account at no cost to you." % channel.name)

            confirmation_info, created = ConfirmationInfo.objects.get_or_create(
                                    email=email,
                                    channel=channel
                                    )
            # Send email to the new user
            confirmation_info.status = 'pending'
            confirmation_info.save()
            subject = "Invitation from Screenbird"
            from_mail = settings.DEFAULT_FROM_EMAIL
            link = "".join(("http://",request.META['HTTP_HOST'],reverse("confirm_channel_member"),"?key="+confirmation_info.key))
            reg_link = "".join(("http://",request.META['HTTP_HOST'],reverse("register")))
            if user_created:
                account_link = "".join(("http://",request.META['HTTP_HOST'],reverse("manage_account")))
                message = render_to_string("email/collaborate_user_created.txt", {
                                       'username':new_username, 
                                       'password':new_password,
                                       'email': email,
                                       'user':request.user.username,
                                       'account_link':account_link, 
                                       'confirm_link':link 
                })
            else:
                message = render_to_string("email/collaborate.txt", {
                                       'email': email,
                                       'user':request.user.username,
                                       'reg_link':reg_link, 
                                       'confirm_link':link 
                })
            send_mail(subject, message, from_mail, [email], fail_silently=False)
            context = {"success":True}

            messages.success(request, 'An invitation has been sent to %s.' % email)
        else:
            messages.error(request, 'Failed to send invitation.')

    context['form'] = form
    context['channel'] = channel
    return redirect(reverse('update_channel', args=[channel.id,]))


@login_required
def remove_channel_member(request, channel_id, user_id):
    """
    Delete user from channel. Make sure the videos that he
    uploaded are still accessible by the group.
    
    """
    if request.method == 'POST':
        user = get_object_or_404(User, id=user_id)
        channel = get_object_or_404(Channel, id=channel_id, owner=request.user)

        # Remove channel from user's list
        try:
            profile = user.get_profile()
            profile.channels.remove(channel)
            profile.save()
            # Delete the persistent message associated with the invitation
            message = Message.objects.filter(user=user,
                                            from_user= request.user,
                                            message__contains=channel.name)
            message.delete()
            
            messages.success(request, "You have removed %s from the group." % (user.username))
        except Exception,e:
            messages.error(request, "Cannot remove user: %s" % (str(e)))
        return HttpResponse('OK!')
    else:
        messages.error(request, "There was a problem removing the user, please try again.")
        return HttpResponse('Not OK!')

@login_required
def cancel_invitation(request, invitation_id):
    """
    Cancel invitation to user from channel. 
    
    """
    confirmation_info = get_object_or_404(ConfirmationInfo, id=invitation_id)
    confirmation_info.status = 'canceled'
    confirmation_info.save()
    return HttpResponse('OK!')


@login_required
@require_POST
def update_channel_member_permissions(request, channel_id):
    channel = get_object_or_404(Channel, id=channel_id, owner=request.user)
    forms = [ChannelOptionForm(channel=channel,
                               user_profile=channel_member.userprofile, 
                               data=request.POST, 
                               prefix='channel-%s' % channel_member.pk)
             for channel_member in channel.get_channel_members()]

    all_valid = reduce(lambda a, b: a and b, [i.is_valid() for i in forms])
    if all_valid:
        for form in forms:
            form.save()
        messages.success(request, 'Permissions have been updated.')
        return redirect('update_channel', channel_id=channel.id)
    return redirect(reverse('update_channel', args=[channel.id,]))

@login_required
def confirm_channel_member(request):
    """
    Confirm a request sent by another user. Make sure user has an account
    with the login_required decorator.
    
    """
    if request.GET.get('key', ""):
        if request.user.is_authenticated():
            confirmation_info = get_object_or_404(ConfirmationInfo, 
                                                  key=request.GET['key'])
            if request.user.email != confirmation_info.email:
                messages.error(request, "You are not allowed to confirm this " \
                                        "channel invitation.")
                return redirect(reverse('my-videos'))
            else:
                if confirmation_info.status == 'confirmed':
                    messages.error(request, "You have already confirmed the " \
                                            "channel request.")
                    return redirect(reverse('my-videos'))
                elif confirmation_info.status == 'canceled':
                    messages.error(request, "Channel invitation has been canceled.")
                    return redirect(reverse('my-videos'))

            # Add channel to user's list
            profile = request.user.get_profile()
            profile.channels.add(confirmation_info.channel)
            profile.save()
            # Mark confirmation as done
            confirmation_info.status = 'confirmed'
            confirmation_info.save()
            messages.success(request, "Congratulations! You are a member of " \
                "the %s channel. You can create videos and save them to this " \
                "channel after they are created. Now you can enjoy the " \
                "features of a premium account at no cost to you." \
                % (confirmation_info.channel.name))
            return redirect(reverse('my-videos'))
        else:
            # Check if already registered
            confirmation_info = get_object_or_404(ConfirmationInfo, key=request.GET['key'])
            user = None
            try:
                user = User.objects.get(email=confirmation_info.email)
            except User.DoesNotExist:
                user = None
            next = '/confirm/' + request.GET['key']
            if user is not None:
                # Make him login
                messages.info(request,'Login your account first to proceed with confirming the channel invite.')
                form = LoginForm({'username':user.username})
                context = {
                    'form': form,
                    'username': user.username,
                    'next': next,
                }
                request.session.set_test_cookie()
                return render(request, 'login.html', context)
            else:
                # Signup (preload fields)
                messages.info(request,'Register an account first to proceed with confirming the channel invite.')
                form = RegistrationNoUsernameForm({'email' :confirmation_info.email})
                context = {
                    'form': form, 
                    'next': next,
                }
                return render(request, 'register.html', context)
    else:
        redirect('account_info')

@login_required
def add_video_to_channel(request, channel_id, video_id):
    """
    Adds the selected video to the selected channel.

    """
    channel = get_object_or_404(Channel, id=channel_id, owner=request.user)
    video = get_object_or_404(Video, id=video_id, uploader=request.user)

    # Set the channel. Set expiry date to None.
    video.channel = channel
    if video.channel.owner.userprofile.account_level.level == 'Paid':
        video.expiry_date = None
    video.save()
    is_expired(video)
    messages.success(request, '%s has been added to %s.' % (video, channel))

    # Redirect to referer, or to the value of `next`, or to the home page,
    # whichever is present first, in that order.
    return redirect(request.META.get('HTTP_REFERER', request.GET.get('next', '/')))

@staff_member_required
@receiver(user_logged_in)
def generate_admin_api_key(**kwargs):
    """
    A function that checks if admin has no api key. If so, screenbird 
    generates one for them randomly on the spot.
    
    """
    try:
        if kwargs['request'].META['HTTP_REFERER'].split('/')[3] == 'admin':
            user_id = User.objects.get(username=kwargs['request'].POST['username']).id
            user = UserProfile.objects.get(user=user_id)
            if user.api_key == "" and len(user.api_key) == 0:
                user.api_key = generate_api_key(API_KEY_LEN)
            user.save()
    except:
        pass
    return

@transaction.commit_on_success
def complete(request, backend):
    """Authentication complete view, override this view if transaction
    management doesn't suit your needs."""
    return complete_process(request, backend)

def complete_process(request, backend):
    """Authentication complete process"""
    backend = get_backend(backend, request, request.path)
    if not backend:
        return HttpResponseServerError('Incorrect authentication service')

    try:
        user = backend.auth_complete()
    except ValueError, e:  # some Authentication error ocurred
        user = None
        error_key = getattr(settings, 'SOCIAL_AUTH_ERROR_KEY', None)
        if error_key:  # store error in session
            request.session[error_key] = str(e)
    except KeyError, e:
        user = None
        error_key = getattr(settings, 'SOCIAL_AUTH_ERROR_KEY', None)
        if error_key:  # store error in session
            request.session[error_key] = str(e)
    except Exception, e:
        user = None
        error_key = getattr(settings, 'SOCIAL_AUTH_ERROR_KEY', None)
        if error_key:  # store error in session
            request.session[error_key] = str(e)

    if user and getattr(user, 'is_active', True):
        login(request, user)
        if getattr(settings, 'SOCIAL_AUTH_SESSION_EXPIRATION', True):
            # Set session expiration date if present and not disabled by
            # setting. Use last social-auth instance for current provider,
            # users can associate several accounts with a same provider.
            backend_name = backend.AUTH_BACKEND.name
            social_user = user.social_auth.filter(provider=backend_name) \
                                          .order_by('-id')[0]
            if social_user.expiration_delta():
                request.session.set_expiry(social_user.expiration_delta())
        url = request.session.pop(REDIRECT_FIELD_NAME, '') or DEFAULT_REDIRECT
    else:
        url = getattr(settings, 'LOGIN_ERROR_URL', settings.LOGIN_URL)
    return HttpResponseRedirect(url)


@login_required
def add_cocreate_member(request, cocreate_id):
    """ Invite cocreate members. """
    context = {}
    cocreate = get_object_or_404(CoCreate, id=cocreate_id, owner=request.user)
    form = InvitationForm()
    if request.method == "POST":
        form = InvitationForm(request.POST)
        if form.is_valid():
            # Save confirmation data
            email = form.cleaned_data['email']
            status = 'pending'

            # Automatically adds the invited user to the channel; invited user still receives confirmation link
            # Update: Create the user when it does not exist.
            created = False
            try:
                invited_user = User.objects.get(email=email)
                new_username = ''
                new_password = ''
            except User.DoesNotExist:
                new_username = generate_username(email)
                new_password = generate_random_string(6)
                invited_user = User.objects.create_user(username=new_username, password=new_password, email=email)
                created = True

            option = CocreateOption(user=invited_user, cocreate=cocreate)
            option.save()
            Message.objects.create(user=invited_user,
                                    from_user=request.user,
                                    level=persistent_messages.INFO,
                                    message="Congratulations! You are now a contributor on %s co-create videos. You can \
                                            create videos and save them as a part of one big video project" % request.user.username)
            
            """
            # Send email to the new user
            subject = "Invitation from Screenbird"
            from_mail = settings.DEFAULT_FROM_EMAIL
            reg_link = "".join(("http://",request.META['HTTP_HOST'],reverse("register")))
            # TODO: Move mailing part to after create cocreate section
            # TODO: use date_joined and last_login to determine whether user is new or not
            if created:
                account_link = "".join(("http://",request.META['HTTP_HOST'],reverse("manage_account")))
                cocreate_section_link = "".join(("http://", request.META['HTTP_HOST'], reverse("cocreate", args=(cocreate.pk,))))
                message = render_to_string("email/cocreate_invite_screenbird.txt", {
                                       'username':new_username,
                                       'password':new_password,
                                       'email': email,
                                       'user':request.user.username,
                                       'account_link':account_link,
                                       'cocreate_section_link': cocreate_section_link,
                })
                html_message = render_to_string("email/cocreate_invite_screenbird.html", {
                                       'username': new_username,
                                       'password': new_password,
                                       'email': email,
                                       'user': request.user.username,
                                       'account_link': account_link,
                                       'cocreate_section_link': cocreate_section_link
                })
                mail = EmailMultiAlternatives(subject, message, from_mail, [email])
                mail.attach_alternative(html_message, "text/html")
                mail.send()
            else:
                message = render_to_string("email/cocreate_collaborate.txt", {
                                       #'name':name,
                                       'email': email,
                                       'user':request.user.username,
                                       'reg_link':reg_link,
                })
                send_mail(subject, message, from_mail, [email], fail_silently=False)
            """
            context = {"success":True}

            messages.success(request, 'An invitation has been sent to %s.' % email)

            if request.is_ajax():
                return HttpResponse(simplejson.dumps({
                    'status': 'OK',
                    'pk': invited_user.pk,
                    'username': invited_user.username
                }), mimetype='application/json')
        else:
            messages.error(request, 'Failed to send invitation.')

            if request.is_ajax():
                return HttpResponse(simplejson.dumps({
                    'status': 'ERROR',
                    'errors': form.errors
                }), mimetype='application/json')
    return redirect(reverse('cocreate', args=[cocreate.id,]))
