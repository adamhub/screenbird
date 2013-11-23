from django.core.management.base import NoArgsCommand

from videos.models import ReservedSlug, Video


class Command(NoArgsCommand):
    help = "Cleans up reserved slugs table"

    def handle_noargs(self, **options):
        reserved_slugs = ReservedSlug.objects.all()
        reserved_slugs.delete()
        
        for video in Video.objects.all():
            try:
                reserved_slug = ReservedSlug.objects.get(slug=video.slug)
            except:
                reserved_slug = ReservedSlug.objects.create(slug=video.slug, used=True)
                reserved_slug.save()

