# These signals are taken from django-paypal's ipn signals
from django.dispatch import Signal

# Sent when a payment is successfully processed.
arb_payment_was_successful = Signal()

# Sent when a payment is flagged.
arb_payment_was_flagged = Signal()

# Sent when a subscription was cancelled.
arb_subscription_cancel = Signal()

# Sent when a subscription expires.
arb_subscription_eot = Signal()

# Sent when a subscription was modified.
arb_subscription_modify = Signal()

# Sent when a subscription is created.
arb_subscription_signup = Signal()
