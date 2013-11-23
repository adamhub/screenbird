from django import forms

from about.models import Inquiry


class InquiryForm(forms.ModelForm):
    class Meta:
        model = Inquiry
        exclude = ('date_created',)
