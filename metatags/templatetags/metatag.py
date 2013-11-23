from django import template
from metatags.models import Metatag

register = template.Library()

@register.inclusion_tag('metatags.html')
def get_metatags():
    """
    Gets a list of html metatags
    """
    metatags = Metatag.objects.all()
    return {'metatags': metatags}
