from django.http import HttpResponse
from django.shortcuts import render_to_response, get_object_or_404

from amazon.models import EC2Node

def ec2_ready(request, instance_id):
    """Sets the node status to ready to notify screenbird that it can take
    encoding jobs.
    
    """
    node = get_object_or_404(EC2Node, instance_id=instance_id)
    node.status = "R"
    node.save()
    return HttpResponse("OK")
