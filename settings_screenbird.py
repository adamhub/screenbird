# Django settings for pastevid project.
import datetime
import os.path
import profanity_list

PROJECT_ROOT = os.path.abspath(os.path.dirname(__file__))

DEBUG = True
TEMPLATE_DEBUG = DEBUG

# Use to determine what robots.txt to serve. To allow all crawlers set this to True.
PRODUCTION = False

ADMINS = (
    ('adam', 'adam@bixly.com'),
    ('caleb', 'caleb@bixly.com'),
)

MANAGERS = ADMINS

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3', # Add 'postgresql_psycopg2', 'postgresql', 'mysql', 'sqlite3' or 'oracle'.
        'NAME': os.path.join(PROJECT_ROOT, 'pastevid.db'),                      # Or path to database file if using sqlite3.
        'USER': '',                      # Not used with sqlite3.
        'PASSWORD': '',                  # Not used with sqlite3.
        'HOST': '',                      # Set to empty string for localhost. Not used with sqlite3.
        'PORT': '',                      # Set to empty string for default. Not used with sqlite3.
    }
}

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
SECRET_KEY = '<django-secret-key>'

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
  os.path.join(PROJECT_ROOT, 'videos/fixtures/'),
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
HITCOUNT_EXCLUDE_USER_GROUP = ( 'Editor', )

AUTH_PROFILE_MODULE='accounts.UserProfile'

INSTALLED_APPS = (
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.sites',
    'django.contrib.messages',
    'django.contrib.staticfiles',
    'django.contrib.flatpages',
    # Uncomment the next line to enable the admin:
    'django.contrib.admin',
    # Uncomment the next line to enable admin documentation:
    'django.contrib.admindocs',

    #External Apps
    'social_auth',
    'south',
    'hitcount',
    'paypal.standard.ipn',
    'chronograph',
    'django_extensions',
    'oembed',
    'django_jenkins',
    'widget_tweaks',

    #Local apps
    'emailnotify',
    'about',
    'videos',
    'accounts',
    'metatags',
    'amazon',
    'thumbsup',
    'flatpages',
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
            'class': 'django.utils.log.AdminEmailHandler'
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
            'handlers': ['mail_admins'],
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
            'level': 'WARN',
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

# Paypal Account
PAYPAL_RECEIVER_EMAIL = "<your-paypal-reciever-email>"

PROFANITY_LIST = profanity_list.CENSORED_LIST

############# EMAIL SETTINGS ###############
# override these in your local_settings.py
EMAIL_BACKEND = 'django.core.mail.backends.smtp.EmailBackend'
EMAIL_USE_TLS = True
EMAIL_HOST = ''
EMAIL_HOST_USER = ''
EMAIL_HOST_PASSWORD = ''
EMAIL_PORT = 465

HOST = ""

DEFAULT_FROM_EMAIL = ''
EMAIL_SUBJECT_PREFIX = '[Screenbird]'
EMAIL_FAIL_SILENTLY = False

# AMAZON
PUSH_TO_S3 = True
AWS_ACCESS_KEY_ID = ""
AWS_SECRET_ACCESS_KEY = ""
AWS_VIDEO_BUCKET_NAME = "%s-%s" % (AWS_ACCESS_KEY_ID.lower(), "videos")
UPLOAD_DELAY = datetime.timedelta(hours=12)
UPLOAD_CHECKING = datetime.timedelta(minutes=30)
EC2_KEY_NAME = ''
QUEUE_NAME = "video_queue"
COCREATE_QUEUE_NAME = 'cocreate_queue'
PEM_PATH = os.path.join(PROJECT_ROOT, "amazon", "ec2_files")

# Use to determine what robots.txt to serve. To allow all crawlers set this to True.
PRODUCTION = False

#Facebook OAuth Keys
#Production's APP ID; Override on local_settings for test site
FACEBOOK_APP_ID              = ''
FACEBOOK_API_SECRET          = ''
FACEBOOK_EXTENDED_PERMISSIONS = ['offline_access','publish_stream','email']

#Twitter OAuth Keys
#Production's APP ID; Override on local_settings for test site
TWITTER_CONSUMER_KEY         = ''
TWITTER_CONSUMER_SECRET      = ''

#Social_Auth Parameters
SOCIAL_AUTH_CREATE_USERS          = True
SOCIAL_AUTH_FORCE_RANDOM_USERNAME = False
SOCIAL_AUTH_DEFAULT_USERNAME      = 'socialauth_user'
SOCIAL_AUTH_COMPLETE_URL_NAME     = 'complete'
SOCIAL_AUTH_ASSOCIATE_BY_MAIL     = True

#Youtube
YOUTUBE_DEV_KEY = ''

#Login Parameters
LOGIN_ERROR_URL    = '/login/error/'
LOGIN_URL          = '/login/'
LOGIN_REDIRECT_URL = '/login_auth/'

#Sites
SITE_ID = 2 #Screen Bird Site ID

# If you are using secure AuthSub, be sure to set your RSA private key so a SecureAuthSubToken is created
# http://code.google.com/apis/gdata/docs/auth/authsub.html#No-Library
# SECURE_KEY the location of the RSA private key(For production). None if AuthSub is not secured.
SECURE_KEY = None #os.path.join(PROJECT_ROOT, '')

ENABLE_VIDEO_APPROVAL = True

# Default Authorize.net credentials
LOGIN_ID = u''
TRANS_KEY = u''
IS_TEST = True
DELIMITER = u','
ENCAPSULATOR = u''

#settings for upload test
FILE_LOCATION = os.path.join(MEDIA_ROOT, 'tmp/sample_video.mp4')
FILE_KEY = 'SAMPLE'


try:
    from local_settings import *
except ImportError:
    pass
