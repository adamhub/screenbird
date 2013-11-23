from djassembla.wrapper import Connection as a
from settings import ASSEMBLA_LOGGER_API_AUTH, ASSEMBLA_LOGGER_SETTINGS


conn = a(ASSEMBLA_LOGGER_API_AUTH)
s = conn.space(ASSEMBLA_LOGGER_SETTINGS["space"])
if not s:
    print "No space with that name"
else:
    tickets = s.tickets(
                        #report_id="u347033"
                        )
    
    for ticket in tickets:
        print ticket.number, ticket.summary