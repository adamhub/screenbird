from django.contrib import messages
from django.shortcuts import render, redirect

from about.forms import InquiryForm


def contact_us(request, template="contact.html"):

    form = InquiryForm()

    if request.method == 'POST':
        form = InquiryForm(request.POST)
        if form.is_valid():
            form.save()
            form = InquiryForm() # Fresh new instance
            messages.success(request, "Message sent!")
        else:
            messages.error(request, "Please correct the errors below.")

    return render(request, template, {'form': form})
