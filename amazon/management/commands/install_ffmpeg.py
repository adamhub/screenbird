import subprocess
import os
from django.core.management.base import NoArgsCommand
from django.conf import settings
from amazon.models import EC2Node


class Command(NoArgsCommand):
    help = "Warn user that videos will be deleted soon."

    def handle_noargs(self, **options):
        nodes = EC2Node.objects.filter(status="P")
        for n in nodes:
            n.status = "I"
            n.save()
            pem_path = os.path.join(settings.PEM_PATH, "%s.pem" % settings.EC2_KEY_NAME);
            base = ["""/usr/bin/ssh""", """-i""", pem_path, """-o""", """StrictHostKeyChecking no""", "ubuntu@%s" % n.name]
            cmd = ["bash", "install.sh", ">>", "install.log"]
            p = subprocess.Popen(base + cmd)
