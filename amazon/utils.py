import datetime
import logging
import os

from django.conf import settings
from django.db.models import get_model
from s3 import Connection

logger = logging.getLogger(__name__)


def send_to_s3(key, filename):
    conn = Connection()
    filepath = os.path.join(settings.MEDIA_ROOT, filename)
    return conn.upload_object_to_s3(key, filepath)


def get_from_s3(key):
    conn = Connection()
    return conn.get_object_from_s3(key)

def get_from_s3_to_filename(key, filename):
    conn = Connection()
    return conn.get_object_from_s3_to_filename(key, filename)

def delete_from_s3(key):
    conn = Connection()
    return conn.delete_object_from_s3(key)


def add_to_upload_queue(video):
    S3UploadQueue = get_model('amazon', 's3uploadqueue')
    try:
        q = S3UploadQueue.objects.get(video=video)
    except S3UploadQueue.DoesNotExist:
        q = S3UploadQueue()
        q.video = video
    print "Created ", str(video.created)
    print "Delay ", str(settings.UPLOAD_DELAY)
    print "Upload Time ", str(video.created + settings.UPLOAD_DELAY)
    q.upload_time=video.created + settings.UPLOAD_DELAY
    q.save()


def purge_queue():
    print "Inside purge"
    logger.debug("Purging Queue")
    S3UploadQueue = get_model('amazon', 's3uploadqueue')
    now = datetime.datetime.now()
    print "Time now is: ", str(now)
    queue = S3UploadQueue.objects.filter(upload_time__lte=now, status__in=('P', 'I'))
    print "Queue: ", str(queue)

    fail = []
    for q in queue:
        q.status = "U"
        q.save()

        successful = False
        try:
            send_to_s3(q.video.slug, q.video.videoupload.name)

            if q.video.mobile_video:
                send_to_s3('%s__mobile' % q.video.slug, q.video.mobile_video.name)

            q.delete()
            successful = True
        except Exception, e:
            logger.debug("Raised Exception: " + str(e))
            print e
            q.attempts = q.attempts + 1
            q.remarks = e
            q.status = "I"
            q.save()
            fail.append(q)

        if successful:
            try:
                # must delete video locally --- needs to have sudo access on test/prod
                # delete desktop version
                os.remove(os.path.join(settings.MEDIA_ROOT, q.video.videoupload.name))
                logger.debug("Deleted file %s from server" % (q.video.videoupload.name))

                # and delete mobile version also
                if q.video.mobile_video:
                    os.remove(os.path.join(settings.MEDIA_ROOT, q.video.mobile_video.name))
                    logger.debug("Deleted file %s from server" % (q.video.mobile_video.name))
            except Exception,e:
                print "Can't delete file: " + str(e)

    if len(fail) > 0:
        logger.debug("Failed to upload %d files to S3" % (len(fail)))
        for f in fail:
            logger.debug("Interruped: " + q.video.videoupload.name)

    successful = queue.count() - len(fail)
    logger.debug("Successfully uploaded %d out of %d files." % (successful, queue.count()))

