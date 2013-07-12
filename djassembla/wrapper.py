from djassembla.models import Space, Ticket
from urllib2 import urlopen, Request, build_opener, HTTPHandler
import base64
from djassembla.errors import AssemblaError
from xml.dom.minidom import parseString
from lxml import etree
import urllib

class Connection(object):
    
    _ROOT_URL = "https://www.assembla.com"

    def __init__(self,auth=None):
        self.auth = auth
        self._spaces = []
        self.authenticate()

        
    def authenticate(self):
        url = "/".join((self._ROOT_URL,"spaces/my_spaces"))
        request = Request(url)
        self._add_auth(request)
        request.add_header("Accept", "application/xml")
        result = urlopen(request)
        if result.code == 200:
            self._load_spaces(result)
        else:
            raise AssemblaError(130,url=url,status_code=result.code)
        return result
        
        
        
    def space(self,name):
        for space in self._spaces:
            if space.name == name:
                return space
        return None
    
    def _load_spaces(self,xml_response):
        doc = etree.parse(xml_response)
        spaces_xml = doc.findall("space")
        for space_dom in spaces_xml:
            self._spaces.append( Space(self,dom=space_dom) )
        return doc
    
    def get_space_names(self):
        return [s.name for s in self._spaces]
    
    def create_ticket(self,space,**kwargs):
        url = "/".join((self._ROOT_URL,"spaces",space.name,"tickets"))
        
        ticket = Ticket(**kwargs)
        ticket_xml = ticket.toxml()
        result = self.post(url,ticket_xml)
        return result
    
    def get_tickets(self,space,report_id=None):
        
        url = "/".join((self._ROOT_URL,"spaces",space.name,"tickets"))
        result = self.get(url, {'tickets_report_id':report_id} if report_id else None)
        
        if result.code == 200:
            tickets = self._tickets_from_xml(space,result)
        else:
            raise AssemblaError(130,url=url,status_code=result.code)
        return tickets
    
    def update_ticket(self,space,ticket):
        url = "/".join((self._ROOT_URL,"spaces",space.name,"tickets",str(ticket.number)))
        result = self.put(url,ticket.toxml())
        return result
    
    def _tickets_from_xml(self,space,xml):
        doc = etree.parse(xml)
        tickets_xml = doc.findall("ticket")
        
        tickets = []
        for ticket_element in tickets_xml:
            tickets.append( Ticket(space=space,dom=ticket_element))
        return tickets
        
    
    def _add_auth(self, req):
        base64string = base64.encodestring('%s:%s' % self.auth).replace('\n', '')
        req.add_header("Authorization", "Basic %s" % base64string)
        return req
    
    def post(self,url,data=None):
        if type(data) != str:
            data = urllib.urlencode(data)
        req = Request(url, data)
        req.add_header("Accept", "application/xml")
        req.add_header("Content-Type","application/xml")
        self._add_auth(req)
        result = urlopen(req)
        return result
    
    def get(self,url,data=None):
        if data and type(data) != str:
            data_encoded = urllib.urlencode(data)
            url = "".join((url,"?",data_encoded))
        req = Request(url)
        req.add_header("Accept", "application/xml")
        self._add_auth(req)
        result = urlopen(req)
        return result
    
    def put(self,url,data=None):
        opener = build_opener(HTTPHandler)
        request = Request(url, data=data)
        request.add_header("Accept", "application/xml")
        request.add_header("Content-Type","application/xml")
        self._add_auth(request)
        request.get_method = lambda: 'PUT'
        result = opener.open(request)
        return result
        
        