""" Use the code below to access AWS Security Token Service API. """

import urllib
import urllib2
import hmac
import hashlib
import base64

from django.conf import settings


BASE_URL = "sts.amazonaws.com"
VERSION = "2011-06-15"


def get_token():

    # Sorted
    params = [
        ('Action', 'GetSessionToken'),
        ('AWSAccessKeyID', settings.AWS_ACCESS_KEY_ID), 
        ('DurationSeconds', '3600'),
        ('SignatureMethod', 'HmacSHA1'),
        ('SignatureVersion', '2'),
        ('Version', VERSION),
    ]

    canonicalized = urllib.urlencode(params).replace("+","%20")
    print "Canonical: ", canonicalized

    # StringToSign = HTTPVerb + "\n" +
    #           ValueOfHostHeaderInLowercase + "\n" +
    #           HTTPRequestURI + "\n" +         
    #           CanonicalizedQueryString <from the preceding step>
    string_to_sign = "GET\n" + BASE_URL + "\n/\n" + canonicalized
    print "Sign: ", string_to_sign

    # Calculate sha256, encode using base-64
    dig = hmac.new(settings.AWS_SECRET_ACCESS_KEY,
                   msg=string_to_sign,
                   digestmod=hashlib.sha1).digest()
    signature = base64.b64encode(dig).decode()
    print "Signature: ", signature
    
    sig_params = [
        ('Signature', signature),
    ]
    canonicalized2 = urllib.urlencode(sig_params).replace("+","%20")
    
    qs = "/?" + canonicalized + "&" + canonicalized2
    print "Final QS: ", qs
    response = urllib.urlopen("https://" + BASE_URL + qs)
    print response.read()

