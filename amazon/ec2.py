import os
import time

from boto.ec2.connection import EC2Connection
import settings

class EC2Factory(object):

    def __init__(self):
        self.conn = EC2Connection(settings.AWS_ACCESS_KEY_ID, settings.AWS_SECRET_ACCESS_KEY)

    def create_key_pair(self):
        key_pair = self.conn.create_key_pair(settings.EC2_KEY_NAME)
        path = settings.PEM_PATH
        if path:
            key_pair.save(path)
            self.path = path
        else:
            key_pair.save(".")
            self.path = "."
        file_path = os.path.join(self.path, "%s.pem" % settings.EC2_KEY_NAME)
        os.chmod(file_path, 600)

    def delete_key_pair(self):
        self.conn.delete_key_pair(settings.EC2_KEY_NAME)
        
    def create_security_group(self):
        sec = self.conn.create_security_group('screenbird', 'Screenbird Group')
        sec.authorize('tcp', 22, 22, '0.0.0.0/0')

    def run_instance(self):
        r = self.conn.run_instances(image_id='ami-0fac7566', 
                                    key_name=settings.EC2_KEY_NAME, 
                                    security_groups=['screenbird'],
                                    user_data="""#!/bin/bash
                                    sudo apt-get install -y python-software-properties
                                    """)
        # Wait for at least 1 minute while node is booting        
        time.sleep(60)
        instance_name = self.get_instance_name(r.id)
        return instance_name

    def get_instance_name(self, reservation_id):
        for r in self.conn.get_all_instances():
            if r.id == reservation_id:
                return r.instances[0].public_dns_name
                break
        return ''

    def terminate_instance(self, instance):
        self.conn.terminate_instances(instance_ids=[instance.id,])

    def get_all_instances(self):
        instances = []
        reservation = self.conn.get_all_instances()
        for r in reservation:
            for i in r.instances:
                instances.append(i)
        return instances

    def get_instance(self, instance_id):
        for i in self.get_all_instances():
            if i.id == instance_id:
                return i
        return None

    def get_instance_by_name(self, instance_name):
        for i in self.get_all_instances():
            if i.public_dns_name == instance_name:
                return i
        return None
