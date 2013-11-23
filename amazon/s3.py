"""
    Documentation can be found on:
    http://boto.cloudhackers.com/en/latest/s3_tut.html

    Please register an Amazon Web Services account and add the following
    to your settings.py:
        AWS_ACCESS_KEY_ID = "your-access-key-id"
        AWS_SECRET_ACCESS_KEY = "your-access-key-secret"
"""

import httplib
import urlparse
import boto
from boto.s3.connection import S3Connection
from boto.s3.key import Key
from django.conf import settings


class Connection(object):

    def __init__(self, *args, **kwargs):
        """ Establish an S3 Connection """

        is_secure = kwargs.pop("is_secure", False)
        self.bucket_name = kwargs.pop("bucket", settings.AWS_VIDEO_BUCKET_NAME)

        super(Connection, self).__init__(*args, **kwargs)

        self.conn = S3Connection(settings.AWS_ACCESS_KEY_ID, settings.AWS_SECRET_ACCESS_KEY, is_secure)
        self.bucket = self.get_or_create_bucket(self.bucket_name)

    def get_or_create_bucket(self, name):
        """ Get or create bucket from s3 """

        try:
            bucket = self.conn.get_bucket(name)
        except Exception,e:
            print e
            bucket = self.conn.create_bucket(name)
        return bucket

    def delete_bucket(self, name):
        """ Delete a bucket from s3 """

        try:
            self.conn.delete_bucket(name)
        except Exception,e:
            print e
            return False
        return True

    def percentage_callback(self, complete, total):
        """ Callback method that reports progress of upload """

        msg = "Transferred %d of %d..." % (complete, total)
        print msg

    def upload_object_to_s3(self, key, filename):
        """ Upload contents from filename """

        k = Key(self.bucket)
        k.key = key
        print "Uploading", key, "..."

        if self.bucket.get_key(key):
            self.bucket.delete_key(key)

        k.set_contents_from_filename(filename, cb=self.percentage_callback, num_cb=10)

    def get_object_from_s3(self, key):
        """ Retrieve object as string. """

        obj = ""
        k = self.bucket.get_key(key)
        if k:
            obj = k.get_contents_as_string(cb=self.percentage_callback, num_cb=10)
        return obj

    def get_object_from_s3_to_filename(self, key, filename):
        """ Retrieve object and write content to filename. """

        obj = ""
        k = self.bucket.get_key(key)
        if k:
            k.get_contents_to_filename(filename, cb=self.percentage_callback, num_cb=10)

    def delete_object_from_s3(self, key):
        """ Delete object specified by key """

        try:
            self.bucket.delete_key(key)
        except Exception,e:
            print e
            return False
        return True

    def generate_object_expiring_url(self, key, seconds=3600):
        """ Use this to access a file directly from s3 """

        url = ""
        try:
            if self.bucket.get_key(key):
                url = self.conn.generate_url(seconds, 'GET', bucket=self.bucket_name, key=key, force_http=True)
                print url
        except Exception,e:
            print e
        return url

    def is_200(self, url):
        try:
            parsed = urlparse.urlparse(url)
            c = httplib.HTTPConnection(parsed.netloc)
            c.request("HEAD", parsed.path)
            res = c.getresponse()
            return res.status == 200
        except Exception,e:
            print e
        return False

