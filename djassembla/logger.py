import logging
from datetime import datetime
from djassembla.wrapper import Connection
from settings import ASSEMBLA_LOGGER_API_AUTH, ASSEMBLA_LOGGER_SETTINGS
import json

class AssemblaLogHandler(logging.Handler):
    level = 'ERROR'
    
    def emit(self, record):
        import traceback
        from django.views.debug import ExceptionReporter
        error_created = datetime.fromtimestamp(record.created)
        summary = record.msg[:250]
        client = " "
        
        if hasattr(record,'request'):
            if hasattr(record.request, "user"):
                if hasattr(record.request.user,"email"):
                    client1 = record.request.user.email
                    client2 = record.request.user.get_profile().get_ma_profile(record.request).get_active_account().name
                    if client1 == client2:
                        client = client1
                    else:
                        client = " , ".join((client1, client2))
                elif record.request.path == "/api/sync/":
                    post_data = json.loads(record.request.raw_post_data)
                    if "username" in post_data:
                        client = post_data['username']
                    else:
                        client = 'AnonymousUser'
                else:
                    client = 'AnonymousUser'
            else:
                    client = 'AnonymousUser'
        
        if record.exc_info and all(record.exc_info):
                formated_exc = traceback.format_exc().splitlines()
                summary = " | ".join((summary,formated_exc[-1]))[:250]
                description = "\n".join(formated_exc)
        else:
            description = " ".join(map(lambda x:str(x),(record.pathname,record.module,record.lineno)))


        con = Connection(ASSEMBLA_LOGGER_API_AUTH)
        s = con.space(ASSEMBLA_LOGGER_SETTINGS["space"])
        tickets = s.tickets(ASSEMBLA_LOGGER_SETTINGS["report_id"])
        # Check if ticket updateable
        
        for ticket in tickets:
            if ticket.summary == summary:
                
                # Gen updated description
                new_top_des = "\n".join(("Client's Affected::",
                                         " ".join((client, error_created.strftime("%c"))),
                                         ))
                ticket.description = ticket.description.replace("Client's Affected::",new_top_des)
                ticket.save()
                return
        
        # Build New Ticket Description
        description = "\n".join(("Client's Affected::",
                                 " ".join((client, error_created.strftime("%c"))),
                                 " ",
                                 description
                                 ))
        
        s.create_ticket(
                        milestone_id = ASSEMBLA_LOGGER_SETTINGS["milestone_id"],
                        summary = summary,
                        priority = ASSEMBLA_LOGGER_SETTINGS["priority"],
                        description = description
                        )
