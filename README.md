screenbird
==========

To run locally:

1. Create virtualenv

2. Install dependencies:

    pip install -r requirements.txt

3. RabbitMQ installation and configuration. By default, screenbird uses two
    queues -- screenbird_videos and screenbird_cocreates. You need to create
    these queues on your local RabbitMQ instance so that videos get processed properly.

4. Setup FFMpeg encoding handler and configure its settings.py

    cd amazon/ec2_files
    ./install.sh

5. Run the FFMpeg encoding handler in a different tab. You need this to run along
    with your main local screenbird server so that videos get processed properly.
    
    python manage.py encoder.py

6. Create your local_settings.py from the given template and update it:

    cp local_settings.py.template local_settings.py

7. Create initial databases:

    python manage.py syncdb

8. Migrate remaining apps:

    python manage.py migrate

9. Run locally:

    python manage.py runserver

10. Configure your sites settings in the Django admin
