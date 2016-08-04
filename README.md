Screenbird
==========

You have found a HUGE program, origianlly built by http://Bixly.com, and now released to the wild. 
It's a cross-platform browser based screen recording tool. Features include a Java screen recorder with local compression, multi-tenant architecture, outlines for your videos, sharing videos to Youtube, and much more. 

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



Licenced Under MIT
==================

The MIT License (MIT)


Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
