import string
from accounts.models import UserProfile
from videos.forms import generate_random_string

def generate_api_key(length):
    while True:
        api_key = generate_random_string(length)
        try:
            UserProfile.objects.get(api_key = api_key)
        except UserProfile.DoesNotExist:
            return api_key
