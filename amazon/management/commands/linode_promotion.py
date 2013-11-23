import subprocess
import os
from optparse import make_option

from django.core.management.base import BaseCommand
from django.conf import settings
from amazon.models import EC2Node
from django.template.loader import render_to_string

class Command(BaseCommand):
    help = "Promote ec2 files to nodes and restart encoder process"

    option_list = BaseCommand.option_list + (
        make_option('--node',
            action='store',
            dest='node',
            type='string',
            default='',
            help='Name of the node where files will be promoted.'
        ),
    )

    def handle(self, *args, **options):
        node = options.pop('node', '')
        pem_path = os.path.join(settings.PEM_PATH, "%s.pem" % settings.EC2_KEY_NAME)
        ssh_base = ["""/usr/bin/ssh""", """-i""", pem_path, """-o""", 
                    """StrictHostKeyChecking no""", "screenbird@%s" % node]

        # Transfer qtfaststart.py
        cmd = ["scp", "-i", pem_path, os.path.join(settings.PROJECT_ROOT,
               "amazon", "ec2_files", "qtfaststart.py"), "screenbird@%s:~" % node]
        p = subprocess.Popen(cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
        msg = p.communicate()
        if msg[1] != '':
            print "Error transferring qtfaststart.py:\n%s" % msg[1]
        print "Transfer of qtfaststart.py completed"

        # Transfer ffmpeg.py
        cmd = ["scp", "-i", pem_path, os.path.join(settings.PROJECT_ROOT,
               "amazon", "ec2_files", "ffmpeg.py"), "screenbird@%s:~" % node]
        p = subprocess.Popen(cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
        msg = p.communicate()
        if msg[1] != '':
            print "Error transferring ffmpeg.py:\n%s" % msg[1]
        print "Transfer of ffmpeg.py completed"

        # Transfer encoder.py
        cmd = ["scp", "-i", pem_path, os.path.join(settings.PROJECT_ROOT,
               "amazon", "ec2_files", "encoder.py"), "screenbird@%s:~" % node]
        p = subprocess.Popen(cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
        msg = p.communicate()
        if msg[1] != '':
            print "Error transferring encoder.py:\n%s" % msg[1]
        print "Transfer completed \nRestarting encoder.py"

        # List existing encoder processes
        cmd = ["ps", "aux", "|", "grep", "encoder"]
        p = subprocess.Popen(ssh_base + cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)

        # Kill existing encoder processes to avoid issue where videos
        # get lost in an unrecognized encoder.py
        info = [x for x in p.stdout.readlines() if "encoder.py" in x]
        for item in info:
            item_split = item.split()
            pid = item_split[1]
            cmd = ["kill", "-9", pid]
            p = subprocess.Popen(ssh_base + cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
            msg = p.communicate()

        # Start a new encoder process with the updated encoder.py
        cmd = ["nohup", "python", "encoder.py", ">", "nohup.encoder.out", "2>&1", "&", "disown"]
        p = subprocess.Popen(ssh_base + cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)

        # Transfer dictconfig.py
        cmd = ["scp", "-i", pem_path, os.path.join(settings.PROJECT_ROOT,
               "videos", "dictconfig.py"), "screenbird@%s:~" % node]
        p = subprocess.Popen(cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
        msg = p.communicate()
        if msg[1] != '':
            print "Error transferring dictconfig.py:\n%s" % msg[1]
        print "Transfer of dictconfig.py completed"

        # Transfer cocreate.py
        cmd = ["scp", "-i", pem_path, os.path.join(settings.PROJECT_ROOT,
               "videos", "cocreate.py"), "screenbird@%s:~" % node]
        p = subprocess.Popen(cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
        msg = p.communicate()
        if msg[1] != '':
            print "Error transferring cocreate.py:\n%s" % msg[1]
        print "Transfer completed \nRestarting cocreate.py"

        # List existing cocreate processes
        cmd = ["ps", "aux", "|", "grep", "cocreate"]
        p = subprocess.Popen(ssh_base + cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)

        # Kill existing cocreate processes to avoid issue where videos
        # get lost in an unrecognized cocreate.py
        info = [x for x in p.stdout.readlines() if "cocreate.py" in x]
        for item in info:
            item_split = item.split()
            pid = item_split[1]
            cmd = ["kill", "-9", pid]
            p = subprocess.Popen(ssh_base + cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
            msg = p.communicate()

        # Start a new cocreate process with the updated encoder.py
        cmd = ["nohup", "python", "cocreate.py", ">", "nohup.cocreate.out", "2>&1", "&", "disown"]
        p = subprocess.Popen(ssh_base + cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)

        print "Promotion finished"
