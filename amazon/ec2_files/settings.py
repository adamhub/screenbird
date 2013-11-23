import os

USE_S3 = False                      # If True, this encoder will fetch videos from Amazon S3 using credentials below
AWS_ACCESS_KEY_ID = ''              # For Amazon S3
AWS_SECRET_ACCESS_KEY = ''          # For Amazon S3
AWS_VIDEO_BUCKET_NAME = ''          # For Amazon S3
QUEUE_NAME = 'screenbird_videos'    # Name of RabbitMQ queue where encoding requests are sent
COCREATE_QUEUE_NAME = 'screenbird_cocreates'    # Name of RabbitMQ queue where cocreate requests are sent
DOMAIN_NAME = 'localhost:8000'      # Domain that tracks running instances of encoder
RABBIT_MQ_DOMAIN = 'localhost'      # Domain where RabbitMQ is hosted
INSTANCE_ID = 'localhost'           # Used for tracking of if running instances of encoder
INSTANCE_NAME = 'localhost'         # Name of encoder instance for tracking where videos are processed via Django admin

