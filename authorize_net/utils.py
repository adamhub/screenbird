#!/usr/bin/env python
# -*- coding: utf-8 -*-
# from http://github.com/johnboxall/django-paypal
import re
from string import digits, split as L

# Adapted from:
# http://www.djangosnippets.org/snippets/764/
# http://www.satchmoproject.com/
# http://tinyurl.com/shoppify-credit-cards

# Well known card regular expressions.
CARDS = {
    'Visa': re.compile(r"^4\d{12}(\d{3})?$"),
    'Mastercard': re.compile(r"(5[1-5]\d{4}|677189)\d{10}$"),
    'Dinersclub': re.compile(r"^3(0[0-5]|[68]\d)\d{11}"),
    'Amex': re.compile("^3[47]\d{13}$"),
    'Discover': re.compile("^(6011|65\d{2})\d{12}$"),
}

# Well known test numbers
TEST_NUMBERS = L("378282246310005 371449635398431 378734493671000"
                 "30569309025904 38520000023237 6011111111111117"
                 "6011000990139424 555555555554444 5105105105105100"
                 "4111111111111111 4012888888881881 4222222222222")


def verify_credit_card(number, allow_test=False):
    """Returns the card type for given card number or None if invalid."""
    return CreditCard(number).verify(allow_test)


class CreditCard(object):
    def __init__(self, number):
        self.number = number

    def is_number(self):
        """Returns True if there is at least one digit in number."""
        if isinstance(self.number, basestring):
            self.number = "".join([c for c in self.number if c in digits])
            return self.number.isdigit()
        return False

    def is_mod10(self):
        """Returns True if number is valid according to mod10."""
        double = 0
        total = 0
        for i in range(len(self.number) - 1, -1, -1):
            for c in str((double + 1) * int(self.number[i])):
                total = total + int(c)
            double = (double + 1) % 2
        return (total % 10) == 0

    def is_test(self):
        """Returns True if number is a test card number."""
        return self.number in TEST_NUMBERS

    def get_type(self):
        """Return the type if it matches one of the cards."""
        for card, pattern in CARDS.iteritems():
            if pattern.match(self.number):
                return card
        return None

    def verify(self, allow_test):
        """Returns the card type if valid else None."""
        if self.is_number() and \
                (not self.is_test() or allow_test) \
                and self.is_mod10():
            return self.get_type()
        return None
