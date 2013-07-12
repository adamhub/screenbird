import urllib
import urllib2
import urlparse
from datetime import timedelta
from unicodedata import normalize

from django.conf import settings
from django.contrib import messages
from django.http import HttpResponseRedirect, HttpResponse
from django.contrib.auth import logout as auth_logout
from django.contrib.auth.decorators import login_required
from django.core.urlresolvers import reverse
from django.template import RequestContext
from django.shortcuts import render_to_response, get_object_or_404, redirect
from django.contrib.auth import authenticate, login as auth_login, logout as auth_logout
from django.views.decorators.cache import never_cache
from django.contrib.contenttypes.models import ContentType
from django.db import connection

from social_auth import __version__ as version
from auth_login.forms import RegistrationForm, LoginForm, DivErrorList, RegistrationNoUsernameForm
from videos.models import Video, FeaturedVideo
from thumbsup.models import ThumbUp
from flatpages.models import CustomPage
from django.conf import settings
from django.views.generic.simple import direct_to_template
from django.core.urlresolvers import reverse
from videos.utils import get_videos

PRODUCTION = settings.PRODUCTION


def index(request, template):
    """Home view, Displays features and latest videos
    """
    """
    if request.user.is_authenticated() and not request.POST:
        return HttpResponseRedirect(reverse('my-videos'))
    """    
    LIMIT = 10

    try:
        feature_section = CustomPage.objects.get(url='/')
    except CustomPage.DoesNotExist:
        feature_section = None

    videos_list = []
    all_videos = get_videos(show_private=False, include_channels = False)
    new_videos = all_videos.order_by('-created')[:LIMIT]
    videos_count = all_videos.count()
    ctype = ContentType.objects.get_for_model(Video)
    qn = connection.ops.quote_name

    
    #popular_videos = all_videos.extra(select={
    #    'votes': """
    #             SELECT COUNT(thumbs_up)
    #             FROM %s
    #             WHERE voted_object_type_id = %s
    #             AND voted_object_id = %s.id
    #             AND thumbs_up = 1
    #             """ % (qn(ThumbUp._meta.db_table), ctype.id, qn(Video._meta.db_table))
    #}, order_by=['-votes'])[:LIMIT]
    
    popular_videos = all_videos.extra(
		select={
		'hit_count': """
					 SELECT hits FROM hitcount_hit_count as h 
					 INNER JOIN django_content_type d on h.content_type_id = d.id 
					 WHERE d.model = 'video' 
					 AND h.object_pk = videos_video.id
					 """,
        'votes': """
                 SELECT COUNT(thumbs_up)
                 FROM %s
                 WHERE voted_object_type_id = %s
                 AND voted_object_id = %s.id
                 AND thumbs_up = 1
                 """ % (qn(ThumbUp._meta.db_table), ctype.id, qn(Video._meta.db_table))
		},).order_by('-hit_count','-votes')[:LIMIT]
		
    all_videos = all_videos.extra(select={
        'votes': """
                 SELECT COUNT(thumbs_up)
                 FROM %s
                 WHERE voted_object_type_id = %s
                 AND voted_object_id = %s.id
                 AND thumbs_up = 1
                 """ % (qn(ThumbUp._meta.db_table), ctype.id, qn(Video._meta.db_table))
    }, order_by=['-votes', '-created'])[:LIMIT]

    featured_videos = FeaturedVideo.objects.all()

    for video in all_videos:
        videos_list.append({
            'name': video.title,
            'slug': video.slug,
            'updated': video.updated
        })

    context = {
        'videos_list': videos_list, # by decreasing popularity and creation time
        'new_videos': new_videos, # by decreasing creation time
        'popular_videos': popular_videos, # by decreasing popularity
        'videos_count': videos_count,
        'version': version,
        'featured_videos': featured_videos,
        'feature_section': feature_section,
        'limit': LIMIT
    }
    
    return render_to_response(template, context,
                              context_instance=RequestContext(request))


@login_required
def getting_started(request):
    context = {}
    return render_to_response('getting_started.html', context,
                              context_instance=RequestContext(request))


@login_required
def info(request):
    """Login complete view, displays user data
    """
    names = request.user.social_auth.values_list('provider', flat=True)
    context = dict((name.lower().replace('-', '_'), True) for name in names)
    context['last_login'] = request.session.get('social_auth_last_login_backend')
    return render_to_response('info.html', context, context_instance=RequestContext(request))


def error(request):
    """Error view
    """
    error_msg = request.session.pop(settings.SOCIAL_AUTH_ERROR_KEY, None)
    return render_to_response('error.html', {'version': version,
                                             'error_msg': error_msg},
                              context_instance=RequestContext(request))


@login_required
def login_auth(request):
    oauth_next = request.GET.get('oauth_next', False)
    next = request.GET.get('next', False)
    last_login = request.user.last_login.replace(microsecond=0)
    date_joined = request.user.date_joined.replace(microsecond=0)
    if oauth_next:
        if '?next=' in oauth_next:
            next_link = oauth_next.split('?next=')
            if next_link[1]:
                return redirect(next_link[1])
            else:
                return HttpResponseRedirect('/')
        return HttpResponseRedirect(oauth_next)
    elif next:
        return HttpResponseRedirect(next)
    else:
        return HttpResponseRedirect('/')


def register(request):
    """Registration
    """
    # Redirect to homepage if user is already registered and logged in
    if request.user.is_authenticated():
        return HttpResponseRedirect(reverse('index'))

    next = request.GET.get('next', False)
    form = RegistrationNoUsernameForm()
    context = {}
    if request.method == "POST":
        form = RegistrationNoUsernameForm(request.POST)
        if form.is_valid():
            form.save()
            message = "User succesfully registered."
            context['message'] = message
            username = ''.join(request.POST['email'].strip())
            new_user = authenticate(username=username,
                                    password=request.POST['password'].strip())
            auth_login(request, new_user)

            # Send AWeber email
            if request.user.username:
                username = request.user.username
            else:
                username = request.user.email
            test = _send_to_aweber(username, request.user.email)
            if test[0]:
                messages.success(request, str(test[1]))
            else:
                if test[1].lower() not in ("blocked", "throttled"): # too many requests
                    messages.error(request, str(test[1]))

            if next:
                return HttpResponseRedirect(next)
            else:
                return HttpResponseRedirect(reverse('getting_started'))
        else:
            errors = []
            if 'email' in form.errors:
                errors.append('email')
            if 'password' in form.errors:
                errors.append('password')
            if 'confirm_password' in form.errors:
                errors.append('confirm_password')
            context['errors'] = errors
    else:
        form = RegistrationNoUsernameForm()
    context['form'] = form
    if next:
        context['next'] = next

    return render_to_response('register.html', context,
                              context_instance=RequestContext(request))


def _send_to_aweber(username, email):
    """ Function that sends the username and email of the new subscriber to aweber """

    success_redirect = 'http://www.aweber.com/thankyou-coi.htm?m=text'
    fail_redirect = 'http://www.aweber.com/form-sorry.htm'

    params = {
        'meta_web_form_id': '856363354',
        'meta_split_id': '',
        'listname': 'screenbird',
        'redirect': success_redirect,
        'meta_adtracking': 'Create_Account',
        'meta_message': '1',
        'meta_required': 'name,email',
        'meta_tooltip': '',
        'name': normalize('NFD', unicode(username)).encode('ascii','ignore'),
        'email': email,
    }

    url = 'http://www.aweber.com/scripts/addlead.pl'
    response = urllib2.urlopen(url, urllib.urlencode(params))
    redirect = response.geturl()

    if redirect.startswith(success_redirect):
        return (True, "Account Created! (Step 1 of 2)")
    elif redirect.startswith(fail_redirect):
        query = urlparse.urlparse(redirect).query
        try:
            messages = urlparse.parse_qs(query).get('message', [])
        except AttributeError: # python < 2.6 has no urlparse method
            import cgi
            messages = cgi.parse_qs(query).get('message', [])
        return (False, " ".join([m.replace('_',' ').capitalize() for m in messages]))


def login_register(request):
    """Register or login page
    """
    next = request.GET.get('next', False)
    if request.user.is_authenticated():
        return HttpResponseRedirect('/')
    login_form = LoginForm()
    reg_form = RegistrationNoUsernameForm()
    context = {'login_form':login_form,
               'reg_form':reg_form,
               'next':next,
              }
    return render_to_response('login_register.html', context, RequestContext(request))


def login(request):
    """
    """
    next = request.GET.get('next', False)
    if request.method == 'POST':
        if request.POST.get('remember_me', None):
            request.session.set_expiry(timedelta(days=3650))
        else:
            request.session.set_expiry(timedelta(weeks=1))
        form = LoginForm(request.POST, error_class=DivErrorList)
        context = {'form': form}
        if form.is_valid():
            username = request.POST['username']
            password = request.POST['password']
            user = authenticate(username=username, password=password)
            if user is not None:
                if user.is_active:
                    if request.session.test_cookie_worked():
                        request.session.delete_test_cookie()
                    auth_login(request, user)
                    if next:
                        response = HttpResponseRedirect(next)
                    else:
                        response = HttpResponseRedirect('/')
                    response.delete_cookie('an_tok')
                    return response
                else:
                    error = u'*Account Disabled'
                    context['error'] = error
            else:
                error = u'*Check your username and password.'
                context['error'] = error
        else:
            if 'username' in form.errors or 'password' in form.errors:
                context['error'] = u'Username or Password Incorrect'
    else:
        form = LoginForm()
        context = {'form': form}
        context['username'] = request.user.username
        if request.user.is_authenticated():
            return HttpResponseRedirect(reverse('index'))
    if next:
        context['next'] = next
    request.session.set_test_cookie()
    return render_to_response('login.html', context, RequestContext(request))


def logout(request):
    """Logs out user
    """
    auth_logout(request)
    next = request.GET.get('next', False)
    if next:
        return HttpResponseRedirect(next)
    else:
        return HttpResponseRedirect('/')


def robots(request):
    if PRODUCTION:
        return direct_to_template(request, 'robots.txt', mimetype='text/plain')
    else:
        return HttpResponse();


def social_auth_begin(request, provider):
    next_url = request.GET.get('next', False)
    try:
        if next_url:
            url = reverse('begin', args=(provider,))
            return HttpResponseRedirect(url + "?next=%s" % next_url)
        else:
            return redirect('begin', provider)
    except ValueError:
        messages.error(request, "Service cannot be reached right now. Please try again later.")
        return HttpResponseRedirect('/')

