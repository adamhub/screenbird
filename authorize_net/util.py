from authorize.gen_xml import base

from authorize_net.signals import *
from authorize_net.models import AuthorizeARBUser

def request(func):
    def req(self, **kw):
        args = func(**kw)
        return self.request(base(args[0], self.login, self.key, *args[1:]))
    req.__name__ = func.__name__
    req.func = func # for tests that need access to inner function
    return req
