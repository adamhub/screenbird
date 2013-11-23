import pika
import os
import subprocess
from optparse import make_option

from django.conf import settings
from django.core.management.base import NoArgsCommand


class Command(NoArgsCommand):
    help = "Promote ec2 files to nodes and restart encoder process"

    def handle_noargs(self, **options):
        #check if rabbitmq is working
        try:
            connection = pika.BlockingConnection(pika.ConnectionParameters(settings.RABBITMQ_SERVER))
        except:
            print "RabbitMQConnection cannot be established"
        else:
            print "RabbitMQConnection established"
        for node in settings.AMAZON_NODES:
            pem_path = os.path.join(settings.PEM_PATH, "%s.pem" % settings.EC2_KEY_NAME)
            ssh_base = ["""/usr/bin/ssh""", """-i""", pem_path, """-o""", 
                        """StrictHostKeyChecking no""", "screenbird@%s" % node]

            # Ping encoder process
            print "Pinging %s..." % node
            cmd = ["ps", "aux", "|", "grep", "encoder"]
            p = subprocess.Popen(ssh_base + cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
            info = [x for x in p.stdout.readlines() if "encoder.py" in x]
            if not info:
                print "No encoder process working on %s. Starting one now." % node
                # Start a new encoder process with the updated encoder.py
                cmd = ["nohup", "python", "encoder.py", ">", "nohup.encoder.out", "2>&1", "&", "disown"]
                p = subprocess.Popen(ssh_base + cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
                print "Encoder process successfully started."
            else:
                print "%s encoder process%s working on %s" % (len(info), 'es' if len(info) > 1 else '', node)

            # Ping cocreate process
            cmd = ["ps", "aux", "|", "grep", "cocreate"]
            p = subprocess.Popen(ssh_base + cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
            info = [x for x in p.stdout.readlines() if "cocreate.py" in x]
            if not info:
                print "No cocreate process working on %s. Starting one now." % node
                # Start a new cocreate process with the updated encoder.py
                cmd = ["nohup", "python", "cocreate.py", ">", "nohup.cocreate.out", "2>&1", "&", "disown"]
                p = subprocess.Popen(ssh_base + cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
                print "Cocreate process successfully started."
            else:
                print "%s cocreate process%s working on %s" % (len(info), 'es' if len(info) > 1 else '', node)
