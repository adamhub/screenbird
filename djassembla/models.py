from lxml import etree

class Ticket(object):
    
    offical_attributes=[
                        'assigned_to_id',
                        'created_on',
                        'id',
                        'importance',
                        'importance_float',
                        'is_story',
                        'milestone_id',
                        'notification_list',
                        'number',
                        'priority',
                        'reporter_id',
                        'space_id',
                        'status',
                        'status_name',
                        'story_importance',
                        'summary',
                        'description',
                        'update_at',
                        'working_hours',
                        'working_hour',
                        'assigned_to',
                        'reporter',
                        'documents'
                        ]
    
    xml_supported_attrs = [
                           'description',
                           'milestone_id',
                           'status',
                           'priority',
                           'summary',
                           ]


    def __init__(self,space=None,dom=None,xml=None,**kwarg):
        
        self.space = space
        # Init offical attributes
        for attr in self.offical_attributes:
            setattr(self,attr,None)
        
        if dom is not None:
            self.fromdom(dom)
        elif xml:
            self.fromxml(xml)
        else:
            for key, arg in kwarg.iteritems():
                if hasattr(self,key):
                    setattr(self,key,arg)
        
        
    
    def get(self,number):
        pass
    
    def filter(self):
        pass
    
    def all(self):
        pass
    
    def toxml(self):
        root = etree.Element('ticket')
        for attr in self.xml_supported_attrs:
            if getattr(self,attr) != None:
                sub = etree.SubElement(root, attr.replace("_","-"))
                sub.text = str(getattr(self,attr,''))
        
        return etree.tostring(root)
    
    def fromxml(self,xml):
        pass
    
    def fromdom(self,dom):
        for attr in self.offical_attributes:
            val = dom.findtext(attr.replace('_','-'))
            if val:
                setattr(self,attr,val)
                
    
    def save(self):
        self.space.update_ticket(self)


class Space(object):
    def __init__(self,connection,dom=None,xml=None):
        self.connection = connection
        if dom is not None:
            self._init_from_dom(dom)
        elif xml != None:
            self.fromxml(xml)
        
    def create_ticket(self,**kwarg):
        ticket = self.connection.create_ticket(self,**kwarg)
        return ticket
    
    def tickets(self,report_id=None):
        """
        Returns all tickets in space
        
        if report_id set then only returns tickets in that report
        """
        
        tickets = self.connection.get_tickets(self,report_id)
        return tickets
        
    def _init_from_dom(self, dom):
        self.name = dom.findtext("name")
    
    def toxml(self):
        pass
    
    def fromxml(self,xml):
        pass
    
    def update_ticket(self,ticket):
        self.connection.update_ticket(self,ticket)
        
class Milestone(object):
    pass

class User(object):
    pass

class Document(object):
    pass