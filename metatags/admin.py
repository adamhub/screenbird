from django.contrib import admin
from metatags.models import Metatag, MetatagType

class MetatagAdmin(admin.ModelAdmin):
    list_display = ('type', 'content')

admin.site.register(MetatagType)
admin.site.register(Metatag, MetatagAdmin)
