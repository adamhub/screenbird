class AssemblaError(Exception):
    """
    Assembla Error Handler
    """
    error_codes = {
        # Authentication and Connectivity
        100: "No authorization credentials provided",
        110: "Assembla failed to authorize with credentials",
        120: "Failure to provide necessary arguments for function",
        130: "Unexpected response from Assembla, response ({status_code}) from '{url}'.",
        # Assembla Retrieval
        200: "Cannot find '{object}' with a primary key matching '{pk}'",
        210: "Multiple objects found using arguments: '{arguments}'",
        220: "No arguments provided.",
    }

    def __init__(self, code, *args, **kwargs):
        """
        :code corresponds to a key in self.error_codes
        """
        self.code = code
        # The error messages are generated with any arguments from kwargs passed
        # into the string formatter.
        self.error_message = self.error_codes[self.code].format(**kwargs)

    def __str__(self):
        return self.error_message