
from django import template
from django.core.validators import email_re

register = template.Library()


def get_email_localpart(email):
    """
    Generates a string based on the email. email must be a valid email address.
    Uses characters before the '@', generates string from the email so as not to show the email
    instead it shows the string before the '@'.
    """
    if email_re.search(email):
        email = email[:email.find('@')]
    else:
        email = email
    return email

register.filter('get_email_localpart', get_email_localpart)

def apostrophe_rule(username):
    """
    Generates a string according to the apostrophe rule.
    """
    # Updated apostrophe rule: append `'s` for all
    username = "%s's" % username
    # if username != "":
    #     if username[-1] == 's':
    #         username = "%s'" % username
    #     else:
    #         username = "%s's" % username
    return username

register.filter('apostrophe_rule', apostrophe_rule)
