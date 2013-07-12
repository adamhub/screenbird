from django import template
from django.db.models import Q
from django.core.paginator import Paginator, InvalidPage, EmptyPage
from accounts.models import UserProfile
from videos.models import Video
from videos.utils import is_expired, days_left, get_videos
from django.core.urlresolvers import reverse
from accounts.templatetags.auth_tags import get_email_localpart
from django.utils.dateformat import format
import datetime
register = template.Library()




@register.filter
def replace_newlines(s):
    """ Converts newlines to linebreaks """

    return s.replace("\r\n", "<br/>").replace("\n", "<br/>")


@register.filter
def replace_newlines_js(s):
    """ Converts newlines to linebreaks """

    return s.replace("\r\n", "\n").replace("\n", "\\\n")


@register.filter
def truncatesmart(value, limit=80):
    """
    Truncates a string after a given number of chars keeping whole words.
    
    Usage:
        {{ string|truncatesmart }}
        {{ string|truncatesmart:50 }}
    """

    try:
        limit = int(limit)
    # invalid literal for int()
    except ValueError:
        # Fail silently.
        return value

    # Make sure it's unicode
    value = unicode(value)

    # Return the string itself if length is smaller or equal to the limit
    if len(value) <= limit:
        return value

    # Cut the string
    value = value[:limit]

    # Break into words and remove the last
    words = value.split(' ')[:-1]

    # Join the words and return
    return ' '.join(words) + '...'


@register.filter
def get_vids_from_profile(profile_id, request):
    try:
        profile = UserProfile.objects.get(pk=profile_id)
    except UserProfile.DoesNotExist:
        return None

    user = profile.user
    user_channels = profile.channels.all().values_list('id', flat=True).distinct()
    my_videos = Video.objects.filter(Q(uploader=user) | Q(channel__id__in=user_channels)).order_by('-created')
    my_videos_count = my_videos.count()

    if my_videos:
        my_videos_list = []
        for vid in my_videos:
            if vid.channel:
                # if video is part of a channel
                # account_level of channel owner will be used
                account_level = vid.channel.owner.userprofile.account_level
            else:
                # else use account_level of uploader
                account_level = vid.uploader.userprofile.account_level

            video_dict = {}
            video_dict['video'] = vid
            video_dict['account_level'] = account_level
            check_video = is_expired(vid)
            if account_level.video_validity and not vid.youtube_embed_url:
                video_dict['expired'] = check_video
                video_dict['days_left'] = days_left(vid)
            my_videos_list.append(video_dict)

        paginator = Paginator(my_videos_list, 25)

        try:
            page = int(request.GET.get('page', '1'))
        except ValueError:
            page = 1
        # If page request (9999) is out of range, deliver last page of results.
        try:
            latest_submissions_page = paginator.page(page)
        except (EmptyPage, InvalidPage):
            latest_submissions_page = paginator.page(paginator.num_pages)

        next_page = latest_submissions_page.next_page_number()
        prev_page = latest_submissions_page.previous_page_number()
        context = {'latest_submissions_page':latest_submissions_page, 'paginator':paginator, 'next_page':next_page,
                       'prev_page':prev_page, 'my_videos_count':my_videos_count}
    else:
        context = None

    return context


@register.filter
def get_username_from_id(profile_id):
    try:
        profile = UserProfile.objects.get(pk=profile_id)
        return profile.user.username
    except UserProfile.DoesNotExist:
        return 'none'


@register.filter
def video_list(obj_list, videos_type):
    """ 
    A reusable function for listing videos.
    Usage:
        {{ <list_name_here>|video_list:"<extra_parameter>" }}
        extra parameter example: channel,featured,latest (these are strings)
    """

    if videos_type != "latest":
        html_str = "<ul>\n\t\t\t"
        closer = ''
    else:
        html_str = ""
    for video in obj_list:
        if videos_type == "featured":
            video = video.video
        if videos_type == "featured" or videos_type == "channel":
            opener = "\n\t\t\t<li>\n\t\t\t\t"
            closer = "\n\t\t\t</li>\n"
        elif videos_type == "latest":
            if video.channel:
                opener = "\n\t\t\t<div class='list-wrapper'>\n\t\t\t\t<div class='content-left channel'>"
            else:
                opener = "\n\t\t\t<div class='list-wrapper'>\n\t\t\t\t<div class='content-left'>"
            opener += "\n\t\t\t\t\t"
            closer = "\n\t\t\t\t</div>\n\t\t\t\t<div class='content-right'>"
            if video.is_public:
                closer += "\n\t\t\t\t\t<span class='unlock' title='public'><a href='#'>&nbsp;</a></span>"
                closer += "\n\t\t\t\t</div>\n\t\t\t</div>"
            else:
                closer += "<span class='lock' title='private'><a href='#'>&nbsp;</a></span>"
                closer += "\n\t\t\t\t\t</div>\n</div>"

        html_str += opener + "<h4><a href='" + unicode(reverse('watch-video', args=[unicode(video.slug), ]))\
                    + "' title='" + video.title + "'>" + truncatesmart(unicode(video.title), 40) + "</a>"\
                    + "</h4><p>by"
        if video.uploader.username:
            if video.uploader.userprofile.is_anonymous:
                html_str += "<a href='" + unicode(reverse('user_videos', args=[unicode(video.uploader.username)]))\
                            + "'>Anonymous User</a>"
            else:
                html_str += "<a href='" + unicode(reverse('user_videos', args=[unicode(video.uploader.username)])) + "'> "
                if video.uploader.userprofile.nickname:
                    html_str += unicode(video.uploader.userprofile.nickname)
                else:
                    if video.uploader.first_name:
                        html_str += unicode(video.uploader.first_name)
                    else:
                        if video.uploader.username:
                            html_str += unicode(video.uploader.username)
                        else:
                            html_str += unicode(get_email_localpart(video.uploader.email))
            html_str += "</a>"
        else:
            if video.uploader.email:
                html_str += unicode(get_email_localpart(video.uploader.email))
            else:
                html_str += "<strong>Anonymous User</strong>"
        html_str += " on " + unicode(format(video.created.date(), "M d, Y"))
        if video.channel:
            html_str += ("<br/>part of " + '<a href="/accounts/channels/' + str(video.channel.id)
                         + '/update/" title="' + video.channel.name + '">' + video.channel.name 
                         + "</a></p>" + closer)
        else:
            html_str += "</p>" + closer
    if len(obj_list) == 0:
        html_str = "<ul><li>No Featured Videos Available.</li></ul>"
        if videos_type == "featured":
            html_str = "<li>No Featured Videos Available.</li>"
        else:
            html_str = "<li>No Videos Available.</li>"
    if videos_type != "latest":
        html_str += "\t\t</ul>"
    else:
        html_str += ""
    return html_str


@register.filter
def get_range(value):
    return range(value)


@register.filter
def get_page_range(value):
    """
    Returns a list of choices for items per page used in video_page_embed.

    """
    return [x * 10 for x in range(1, value + 1)]


def getvids(context):
    """
        returns all videos or videos by_user
        context may include:
            username - return user videos
            user_id - return user videos
            user - user object
            show_private - boolean includes private videos
            include_channels - boolean returns user vids including channel vids
            channel_only - boolean return ONLY channel videos for user
    """
    params = {}
    if 'username' in context:
        params['username'] = context['username']
    if 'user_id' in context:
        params['user_id'] = context['user_id']
    if 'user' in context:
        params['user'] = context['user']
    if 'show_private' in context:
        params['show_private'] = context['show_private']
    if 'include_channels' in context:
        params['include_channels'] = context['include_channels']
    if 'channel_only' in context:
        params['channel_only'] = context['channel_only']
    return get_videos(**params)
    
register.simple_tag(takes_context=True)(getvids)
