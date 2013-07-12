# Django settings for screenbird project.
import datetime
import os.path
import profanity_list

PROJECT_ROOT = os.path.abspath(os.path.dirname(__file__))

DEBUG = True
TEMPLATE_DEBUG = DEBUG

# Local time zone for this installation. Choices can be found here:
# http://en.wikipedia.org/wiki/List_of_tz_zones_by_name
# although not all choices may be available on all operating systems.
# On Unix systems, a value of None will cause Django to use the same
# timezone as the operating system.
# If running in a Windows environment this must be set to the same as your
# system time zone.
TIME_ZONE = 'America/Chicago'
USE_TZ = True

# Language code for this installation. All choices can be found here:
# http://www.i18nguy.com/unicode/language-identifiers.html
LANGUAGE_CODE = 'en-us'

SITE_ID = 1

# If you set this to False, Django will make some optimizations so as not
# to load the internationalization machinery.
USE_I18N = True

# If you set this to False, Django will not format dates, numbers and
# calendars according to the current locale
USE_L10N = True

# Absolute filesystem path to the directory that will hold user-uploaded files.
# Example: "/home/media/media.lawrence.com/media/"
MEDIA_ROOT = os.path.join(PROJECT_ROOT, 'media/')

# URL that handles the media served from MEDIA_ROOT. Make sure to use a
# trailing slash.
# Examples: "http://media.lawrence.com/media/", "http://example.com/media/"
MEDIA_URL = '/media/'

# Absolute path to the directory static files should be collected to.
# Don't put anything in this directory yourself; store your static files
# in apps' "static/" subdirectories and in STATICFILES_DIRS.
# Example: "/home/media/media.lawrence.com/static/"
STATIC_ROOT = os.path.join(PROJECT_ROOT, 'static/')

# URL prefix for static files.
# Example: "http://media.lawrence.com/static/"
STATIC_URL = '/static/'

# URL prefix for admin static files -- CSS, JavaScript and images.
# Make sure to use a trailing slash.
# Examples: "http://foo.com/static/admin/", "/static/admin/".
ADMIN_MEDIA_PREFIX = '/static/admin/'

# Additional locations of static files
STATICFILES_DIRS = (
    # Put strings here, like "/home/html/static" or "C:/www/django/static".
    # Always use forward slashes, even on Windows.
    # Don't forget to use absolute paths, not relative paths.
)

# List of finder classes that know how to find static files in
# various locations.
STATICFILES_FINDERS = (
    'django.contrib.staticfiles.finders.FileSystemFinder',
    'django.contrib.staticfiles.finders.AppDirectoriesFinder',
#    'django.contrib.staticfiles.finders.DefaultStorageFinder',
)

# Make this unique, and don't share it with anybody.
SECRET_KEY = 'v0z2l3==1kwr%-%w+n)^_-%a(e51dme)0sh=0qa+8*n(nd59eh'

# List of callables that know how to import templates from various sources.
TEMPLATE_LOADERS = (
    'django.template.loaders.filesystem.Loader',
    'django.template.loaders.app_directories.Loader',
#     'django.template.loaders.eggs.Loader',
)

TEMPLATE_CONTEXT_PROCESSORS = (
    'context_processors.current_site',
    'django.contrib.auth.context_processors.auth',
    "django.core.context_processors.media",
    'django.core.context_processors.request',
)

MIDDLEWARE_CLASSES = (
    'django.middleware.common.CommonMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    'django.contrib.flatpages.middleware.FlatpageFallbackMiddleware'
)

ROOT_URLCONF = 'urls'

TEMPLATE_DIRS = (
    # Put strings here, like "/home/html/django_templates" or "C:/www/django/templates".
    # Always use forward slashes, even on Windows.
    # Don't forget to use absolute paths, not relative paths.
    os.path.join(PROJECT_ROOT, 'templates/'),
)

FIXTURE_DIRS = (
  os.path.join(PROJECT_ROOT, 'accounts/fixtures/'),
)

AUTHENTICATION_BACKENDS = (
    'auth_login.backends.EmailBackend',
    'social_auth.backends.facebook.FacebookBackend',
    'social_auth.backends.google.GoogleOAuthBackend',
    'social_auth.backends.google.GoogleOAuth2Backend',
    'social_auth.backends.google.GoogleBackend',
    'social_auth.backends.OpenIDBackend',
    'django.contrib.auth.backends.ModelBackend',
)

HITCOUNT_KEEP_HIT_ACTIVE = { 'days': 45 }
HITCOUNT_HITS_PER_IP_LIMIT = 0
HITCOUNT_EXCLUDE_USER_GROUP = ( )

AUTH_PROFILE_MODULE='accounts.UserProfile'

INSTALLED_APPS = (
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.sites',
    'django.contrib.messages',
    'django.contrib.staticfiles',
    'django.contrib.flatpages',
    'django.contrib.admin',
    'django.contrib.admindocs',

    # External Apps
    'social_auth',
    'south',
    'hitcount',
    'paypal.standard.ipn',
    'chronograph',
    'django_extensions',
    'oembed',
    'djkombu',
    'persistent_messages',
    'djangorestframework',
    'django_jenkins',
    'widget_tweaks',

    # Local apps
    'emailnotify',
    'about',
    'videos',
    'accounts',
    'metatags',
    'amazon',
    'thumbsup',
    'flatpages',
    'api',
    'authorize_net',
)

# For Jenkins testing
PROJECT_APPS = (
    'accounts',
    'videos',
    'about',
    'amazon',
    'emailnotify',
    'metatags',
    'thumbsup',
    'api',
)

JENKINS_TASKS = (
    'django_jenkins.tasks.with_coverage',
    'django_jenkins.tasks.django_tests',   # select one django or
    #'django_jenkins.tasks.dir_tests'      # directory tests discovery
)

# A sample logging configuration. The only tangible logging
# performed by this configuration is to send an email to
# the site admins on every HTTP 500 error.
# See http://docs.djangoproject.com/en/dev/topics/logging for
# more details on how to customize your logging configuration.
LOGGING = {
    'version': 1,
    'disable_existing_loggers': False,
    'formatters': {
        'verbose': {
            'format': '%(levelname)s %(asctime)s %(module)s %(process)d %(thread)d %(message)s'
        },
        'simple': {
            'format': '%(levelname)s %(message)s'
        }
    },
    'handlers': {
        'mail_admins': {
            'level': 'ERROR',
            'class': 'django.utils.log.AdminEmailHandler',
        },
        'file': {
            'level': 'DEBUG',
            'class': 'logging.FileHandler',
            'filename': os.path.join(PROJECT_ROOT, 'amazon.log'),
            'formatter': 'verbose',
        },
        'videos': {
            'level': 'DEBUG',
            'class': 'logging.FileHandler',
            'filename': os.path.join(PROJECT_ROOT, 'videos.log'),
            'formatter': 'verbose'
        },
        'encode': {
            'level': 'DEBUG',
            'class': 'logging.FileHandler',
            'filename': os.path.join(PROJECT_ROOT, 'encode.log'),
            'formatter': 'verbose'
        }
    },
    'loggers': {
        'django.request': {
            'handlers': ['mail_admins',],
            'level': 'ERROR',
            'propagate': True,
        },
        'amazon.utils': {
            'handlers': ['file',],
            'level': 'DEBUG',
            'propagate': True,
        },
        'videos.forms': {
            'handlers': ['file',],
            'level': 'DEBUG',
            'propagate': True,
        },
        'videos.views': {
            'handlers': ['videos',],
            'level': 'DEBUG',
            'propagate': True,
        },
        'encode': {
            'handlers': ['encode',],
            'level': 'DEBUG',
            'propagate': True
        }
    }
}

PROFANITY_LIST = profanity_list.CENSORED_LIST

# Login Parameters
LOGIN_ERROR_URL    = '/login/error/'
LOGIN_URL          = '/login/'
LOGIN_REDIRECT_URL = '/login_auth/'
LOGIN_ID = ''
TRANS_KEY = ''
IS_TEST = ''
DELIMITER = ''
ENCAPSULATOR = ''

# Sites
SITE_ID = 2 # Screenbird Site ID

# Settings for upload test
FILE_LOCATION = os.path.join(MEDIA_ROOT, 'tmp/5min.mp4')
FILE_KEY = 'SAMPLE_tmp'

# Settings for encode test
LONG_VIDEO = os.path.join(MEDIA_ROOT, 'tmp/5min.mp4')
SHORT_VIDEO = os.path.join(MEDIA_ROOT, 'tmp/2min.mp4')

# for bixly test server
LIVE_VIDEOS_PATH = os.path.join(MEDIA_ROOT, "videos/")

CHANNEL_SLUG_LENGTH = 6
VIDEO_SLUG_LENGTH = 8

try:
    from local_settings import *
except ImportError:
    pass
