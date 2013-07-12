from datetime import date

from django.conf import settings
from django.shortcuts import render_to_response, redirect
from django.template import RequestContext
from django.contrib.auth.decorators import login_required
from django.contrib import messages

from authorize_net import arb
from authorize.gen_xml import MONTHS_INTERVAL, DAYS_INTERVAL

from authorize_net.forms import ARBForm
from authorize_net.models import AuthorizeARBUser
from accounts.models import AccountLevel, UserProfile
from videos.models import Video
from videos.utils import is_expired


LOGIN_ID = settings.LOGIN_ID
TRANS_KEY = settings.TRANS_KEY
IS_TEST = settings.IS_TEST
DELIMITER = settings.DELIMITER
ENCAPSULATOR = settings.ENCAPSULATOR

@login_required
def authorize_monthly(request):
    
    if request.method == 'POST':
        form = ARBForm(request.POST)
        if form.is_valid():
            card_number = form.cleaned_data['card_number']
            expiration_date = form.cleaned_data['expiration_date']
            bill_first_name = form.cleaned_data['bill_first_name']
            bill_last_name = form.cleaned_data['bill_last_name']
            
            arb_api = arb.Api(LOGIN_ID, TRANS_KEY, is_test=IS_TEST, delimiter=DELIMITER, encapsulator=ENCAPSULATOR)
            
            subs = arb_api.create_subscription(
                card_number = card_number,
                expiration_date = unicode(expiration_date.strftime("%Y-%m")),
                bill_first_name = bill_first_name,
                bill_last_name = bill_last_name,
                interval_unit = MONTHS_INTERVAL,
                interval_length = 1,
                start_date = unicode(date.today().strftime("%Y-%m-%d")),
                amount = 10,
                subscription_name = unicode("Screenbird Paid Account - Monthly Subscription")
            )
            
            code_text = subs.messages.message.code.text_
            message_text = subs.messages.message.text.text_
            result_text = subs.messages.result_code.text_
            
            if code_text == 'I00001' and message_text == 'Successful.' and result_text == 'Ok':
                userprofile = UserProfile.objects.get(user__id=request.user.id)
                AuthorizeARBUser.objects.create(user_profile=userprofile, subscription_id=subs.subscription_id.text_)
                
                vids = Video.objects.filter(uploader=request.user)
                for vid in vids:
                    is_expired(vid)
                
                messages.success(request, "You have successfully upgraded your account!")
                return redirect('account_info')    
            else:
                messages.error(request, "There was a problem processing your subscription. Please try again later.")
                return redirect('features')
            
    else:
        form = ARBForm()
    
    context = {
        'form': form
    }
    
    return render_to_response('authorize_monthly.html', RequestContext(request, context))
    
@login_required
def authorize_yearly(request):
    
    if request.method == 'POST':
        form = ARBForm(request.POST)
        if form.is_valid():
            card_number = form.cleaned_data['card_number']
            expiration_date = form.cleaned_data['expiration_date']
            bill_first_name = form.cleaned_data['bill_first_name']
            bill_last_name = form.cleaned_data['bill_last_name']
            
            arb_api = arb.Api(LOGIN_ID, TRANS_KEY, is_test=IS_TEST, delimiter=DELIMITER, encapsulator=ENCAPSULATOR)
            
            subs = arb_api.create_subscription(
                card_number = card_number,
                expiration_date = unicode(expiration_date.strftime("%Y-%m")),
                bill_first_name = bill_first_name,
                bill_last_name = bill_last_name,
                interval_unit = MONTHS_INTERVAL,
                interval_length = 12,
                start_date = unicode(date.today().strftime("%Y-%m-%d")),
                amount = 120,
                subscription_name = unicode("Screenbird Paid Account - Yearly Subscription")
            )
            
            code_text = subs.messages.message.code.text_
            message_text = subs.messages.message.text.text_
            result_text = subs.messages.result_code.text_
            
            if code_text == 'I00001' and message_text == 'Successful.' and result_text == 'Ok':
                userprofile = UserProfile.objects.get(user__id=request.user.id)
                AuthorizeARBUser.objects.create(user_profile=userprofile, subscription_id=subs.subscription_id.text_)
                
                vids = Video.objects.filter(uploader=request.user)
                for vid in vids:
                    is_expired(vid)
                
                messages.success(request, "You have successfully upgraded your account!")
                return redirect('account_info')    
            else:
                messages.error(request, "There was a problem processing your subscription. Please try again later.")
                return redirect('features')
    else:
        form = ARBForm()
    
    context = {
        'form': form
    }
    
    return render_to_response('authorize_yearly.html', RequestContext(request, context))
