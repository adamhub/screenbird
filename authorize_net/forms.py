from django import forms

from authorize_net.fields import CreditCardField, CreditCardExpiryField

class ARBForm(forms.Form):
    card_number = CreditCardField(label="Credit Card Number")
    expiration_date = CreditCardExpiryField(label="Expiration Date")
    bill_first_name = forms.CharField(max_length=50)
    bill_last_name = forms.CharField(max_length=50)
