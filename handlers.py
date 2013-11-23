from django.conf import settings
LOGGING = settings.LOGGING # force import, important to come before import logging

import logging
import sys

from django import http
from django.core import mail
from django.core.mail import EmailMessage
from django.template import Context, loader

class ScreenbirdAdminEmailHandler(logging.Handler):
    def __init__(self, include_html=False):
        logging.Handler.__init__(self)        
        self.include_html = include_html

    """An exception log handler that e-mails log entries to site admins.

    If the request is passed as the first argument to the log record,
    request data will be provided in the
    """
    def emit(self, record):
        import traceback
        from django.conf import settings
        from django.template.loader import render_to_string
        from django.views.debug import ExceptionReporter

        try:
            if sys.version_info < (2,5):
                # A nasty workaround required because Python 2.4's logging
                # module doesn't support passing in extra context.
                # For this handler, the only extra data we need is the
                # request, and that's in the top stack frame.
                request = record.exc_info[2].tb_frame.f_locals['request']
            else:
                request = record.request

            subject = '%s (%s IP): %s' % (
                record.levelname,
                (request.META.get('REMOTE_ADDR') in settings.INTERNAL_IPS and 'internal' or 'EXTERNAL'),
                record.msg
            )
            request_repr = repr(request)
        except:
            subject = '%s: %s' % (
                record.levelname,
                record.msg
            )

            request = None
            request_repr = "Request repr() unavailable"

        if record.exc_info:
            exc_info = record.exc_info
            stack_trace = '\n'.join(traceback.format_exception(*record.exc_info))
        else:
            exc_info = (None, record.msg, None)
            stack_trace = 'No stack trace available'

        description = "%s\n\n%s" % (stack_trace, request_repr)

        # Assembla settings
        milestone = settings.ASSEMBLA_MILESTONE
        component = settings.ASSEMBLA_COMPONENT
        priority = settings.ASSEMBLA_PRIORITY
        
        message = \
"""
Milestone: %(milestone)s
Component: %(component)s
Priority: %(priority)s
Description:
%(description)s
.
""" % \
        ({'milestone': milestone,
          'component': component,
          'priority': priority,
          'description': description,})

        email = EmailMessage(
            subject = subject,
            body = message,
            from_email = "api@theirwork.com",
            to = ["%s@tickets.assembla.com" % settings.ASSEMBLA_SPACE,],
        )
        email.send()


def _get_traceback(self, exc_info=None):
    """Helper function to return the traceback as a string"""
    import traceback
    return '\n'.join(traceback.format_exception(*(exc_info or sys.exc_info())))

def custom_500_handler(request, template_name='500.html'):
    """
    500 error handler.

    Templates: `500.html`
    Context: sys.exc_info() results
     """
    t = loader.get_template(template_name) # You need to create a 500.html template.
    exc_info = sys.exc_info()
    
    subject = 'Error (%s IP): %s' % ((request.META.get('REMOTE_ADDR') in settings.INTERNAL_IPS and 'internal' or 'EXTERNAL'), request.path)
    try:
        request_repr = repr(request)
    except:
        request_repr = "Request repr() unavailable"
    
    # Assembla settings
    milestone = settings.ASSEMBLA_MILESTONE
    component = settings.ASSEMBLA_COMPONENT
    priority = settings.ASSEMBLA_PRIORITY
    description = "%s\n\n%s" % (_get_traceback(exc_info), request_repr)
    message = \
"""
Milestone: %(milestone)s
Component: %(component)s
Priority: %(priority)s
Description:
%(description)s
.
""" % \
    ({'milestone': milestone,
      'component': component,
      'priority': priority,
      'description': description,})
    
    email = EmailMessage(
        subject = subject,
        body = message,
        from_email = "api@theirwork.com",
        to = ["%s@tickets.assembla.com" % settings.ASSEMBLA_SPACE,],
    )
    email.send()
        
    sys.exc_clear() #for fun, and to point out I only -think- this hasn't happened at 
                    #this point in the process already
    return http.HttpResponseServerError(t.render(Context({})))
