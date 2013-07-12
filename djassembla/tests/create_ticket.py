from djassembla.wrapper import Connection as a
from settings import ASSEMBLA_LOGGER_API_AUTH, ASSEMBLA_LOGGER_SETTINGS


conn = a(ASSEMBLA_LOGGER_API_AUTH)
s = conn.space(ASSEMBLA_LOGGER_SETTINGS["space"])
if not s:
    print "No space with that name"
else:
    ticket = s.create_ticket(
                             milestone_id =ASSEMBLA_LOGGER_SETTINGS["milestone_id"],
                             summary = "This is a summary",
                             priority = ASSEMBLA_LOGGER_SETTINGS["priority"],
                             description = "This is the description"
                             )

    print ticket.read()