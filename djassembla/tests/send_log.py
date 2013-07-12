import logging
from djassembla.logger import AssemblaLogHandler
from django.http import HttpResponse
from logging import LoggerAdapter

def log_test_view(request):
    logger = logging.getLogger('assembla')
    logger2 = logging.getLogger('django.request')
    logger.addHandler(AssemblaLogHandler)
    logger.error("Test message2",extra={"request":request}, exc_info=1)
    try:
        int(None)
    except:
        pass
    raise Exception("Log Testing/admin email test")
#    logger2.exception("Log Testing/admin email test")
    return HttpResponse("log_test_view ok")