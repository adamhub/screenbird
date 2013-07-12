import subprocess
import os
import datetime

from django.db import models
from django.core.urlresolvers import reverse
from django.contrib.contenttypes import generic
from django.contrib.contenttypes.models import ContentType
from django.db.models.signals import post_delete
from django.template.loader import render_to_string
from django.contrib.sites.models import Site
from django.conf import settings

from utils import send_to_s3
from ec2 import EC2Factory


class S3UploadQueue(models.Model):
    """
        Queue of files to be pushed to S3.
        Once the file is uploaded, the copy on the server must be deleted.
    """

    STATUSES = (
        ("P", "Pending"),
        ("U", "Uploading"),
        ("I", "Interrupted"),
    )

    video = models.ForeignKey('videos.Video')
    upload_time = models.DateTimeField()
    attempts = models.IntegerField(default=0)
    remarks = models.TextField(blank=True)
    status = models.CharField(max_length=1, choices=STATUSES, default="P")

    def go_to_video(self):
        video_type = ContentType.objects.get_for_model(self.video)
        video_app_label = video_type.app_label
        video_model_name = video_type.model
        video_url = 'admin:%s_%s_change' % (video_app_label, video_model_name)

        return '<a href="%s">%s</a>' % (reverse(video_url , args=(self.video.id,)), self.video.slug)
    go_to_video.allow_tags = True

    def __unicode__(self):
        return "%s-%s" % (self.video, self.upload_time)


class EC2Node(models.Model):

    STATUS_CHOICES = (("P", "Preparing"), ("I", "Installing"), ("R", "Running"))
    name = models.CharField(max_length=60, default="none", editable=False)
    instance_id = models.CharField(max_length=15, default="none", editable=False)
    status = models.CharField(max_length=10, choices=STATUS_CHOICES, default="P", editable=False)

    class Meta:
        verbose_name = "EC2 Node"

    def __unicode__(self):
        return self.name

    def save(self, *args, **kwargs):
        if not self.pk:
            factory = EC2Factory()
            self.name = factory.run_instance()
            self.instance_id = factory.get_instance_by_name(self.name).id
            pem_path = os.path.join(settings.PEM_PATH, "%s.pem" % settings.EC2_KEY_NAME);
            base = ["""/usr/bin/ssh""", """-i""", pem_path, """-o""", """StrictHostKeyChecking no""", "ubuntu@%s" % self.name]
            source_str = render_to_string("apt_get_source.txt")
            cmd = ["echo", "-e", '"%s"' % source_str, ">", "sources.list"]
            p = subprocess.Popen(base + cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
            p.communicate()
                
            cmd = ["sudo", "cp", "sources.list", "/etc/apt/sources.list"]
            p = subprocess.Popen(base + cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
            p.communicate()

            cmd = ["scp", "-i", pem_path, os.path.join(settings.PROJECT_ROOT, "amazon", "ec2_files", "encoder.py"), """-o""", """StrictHostKeyChecking no""", "ubuntu@%s:~" % self.name]
            p = subprocess.Popen(cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
            p.communicate()

            cmd = ["scp", "-i", pem_path, os.path.join(settings.PROJECT_ROOT, "amazon", "ec2_files", "ffmpeg.py"), """-o""", """StrictHostKeyChecking no""", "ubuntu@%s:~" % self.name]
            p = subprocess.Popen(cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
            p.communicate()

            cmd = ["scp", "-i", pem_path, os.path.join(settings.PROJECT_ROOT, "amazon", "ec2_files", "qtfaststart.py"), """-o""", """StrictHostKeyChecking no""", "ubuntu@%s:~" % self.name]
            p = subprocess.Popen(cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
            p.communicate()

            settings_str = render_to_string("amazon_config_template.txt", {"access_key": settings.AWS_ACCESS_KEY_ID,
                                                                            "secret_key": settings.AWS_SECRET_ACCESS_KEY,
                                                                            "bucket": settings.AWS_VIDEO_BUCKET_NAME,
                                                                            "queue":  settings.SQS_QUEUE_NAME,
                                                                            "domain": Site.objects.get_current().domain,
                                                                            "instance_id": self.instance_id,
                                                                            "instance_name": self.name})
            cmd = ["echo", "-e", '"%s"' % settings_str, ">", "settings.py"]
            p = subprocess.Popen(base + cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
            p.communicate()

            cmd = ["scp", "-i", pem_path, os.path.join(settings.PROJECT_ROOT, "amazon", "ec2_files", "install.sh"), """-o""", """StrictHostKeyChecking no""", "ubuntu@%s:~" % self.name]
            p = subprocess.Popen(cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE);
            p.communicate()
            
        super(EC2Node, self).save(*args, **kwargs)

def terminate_instance(sender, instance, **kwargs):
    factory = EC2Factory()
    if instance.instance_id:
        i = factory.get_instance(instance.instance_id)
        if i:
            factory.terminate_instance(i)

post_delete.connect(terminate_instance, sender=EC2Node)
