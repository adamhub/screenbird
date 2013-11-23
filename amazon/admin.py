from django.contrib import admin
from models import S3UploadQueue, EC2Node
from django.contrib.sites.models import Site

class S3UploadQueueAdmin(admin.ModelAdmin):
    list_display = ['__unicode__'   , 'go_to_video', 'remarks']
    search_fields = ['video__title']

class EC2NodeAdmin(admin.ModelAdmin):
    list_display = ['__unicode__'   , 'instance_id', 'status']
    readonly_fields= ('name', 'instance_id', 'status')

admin.site.register(S3UploadQueue, S3UploadQueueAdmin)
if Site.objects.get_current().domain == "screenbird.com":
    admin.site.register(EC2Node, EC2NodeAdmin)
