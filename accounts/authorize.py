from django.conf import settings
import urllib2, urllib
URL = 'https://test.authorize.net/gateway/transact.dll'

API = { 'x_login':'XXX',
        'x_tran_key':'XXX', 
        'x_method':'CC', 
        'x_type':'AUTH_ONLY',
        'x_delim_data':'TRUE', 
        'x_duplicate_window':'10', 
        'x_delim_char':'|',
        'x_relay_response':'FALSE', 
        'x_version':'3.1',
        'x_line_item':'XXX',}

def call_auth(line_item, amount, data, request_ip=None):
    '''Call authorize.net and get a result dict back'''
    
    payment_post = API
    payment_post['x_login'] = settings.AUTHNET_LOGIN_ID
    payment_post['x_tran_key'] = settings.AUTHNET_TRANSACTION_KEY
    payment_post['x_amount'] = amount
    payment_post['x_card_num'] = data['card_number']
    payment_post['x_exp_date'] = data['expiry']
    payment_post['x_first_name'] = data['first_name']
    payment_post['x_last_name'] = data['last_name']
    payment_post['x_company'] = data['company']
    payment_post['x_address'] = data['address']
    payment_post['x_city'] = data['city']
    payment_post['x_state'] = data['state']
    payment_post['x_state'] = data['province']
    payment_post['x_country'] = data['country']
    payment_post['x_email'] = data['email']
    payment_post['x_phone'] = data['phone']
    payment_post['x_card_code'] = data['card_code']
    payment_post['x_zip'] = data['zip_code']
    payment_post['x_line_item'] = "%s<|>%s<|>%s<|>%s<|>%s<|>N" %(line_item.authnet_line_item_id, line_item.authnet_item_name, line_item.authnet_item_description,1, line_item.authnet_item_price)
    payment_request = urllib2.Request(URL, urllib.urlencode(payment_post))
    r = urllib2.urlopen(payment_request).read()
    return r

def call_capture(trans_id): 
    capture_post = API
    capture_post['x_type'] = 'PRIOR_AUTH_CAPTURE'
    capture_post['x_trans_id'] = trans_id
    capture_request = urllib2.Request(URL, urllib.urlencode(capture_post))
    r = urllib2.urlopen(capture_request).read()
    return r
