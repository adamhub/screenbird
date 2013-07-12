from django.conf import settings

from django import forms
from django.contrib.auth.models import User
from django.contrib.auth.tokens import default_token_generator
from django.contrib.sites.models import get_current_site
from django.core.validators import email_re
from django.template import Context, loader
from django.utils.http import int_to_base36
from django.utils.safestring import mark_safe
from django.utils.translation import ugettext_lazy as _

from paypal.standard.conf import *
from paypal.standard.forms import PayPalPaymentsForm
from social_auth.models import UserSocialAuth

from accounts.models import AccountLevel, UserProfile


class UsernameChangeForm(forms.ModelForm):
    """
    Update username form
    
    """

    def __init__(self, *args, **kwargs):
        super(UsernameChangeForm, self).__init__(*args, **kwargs)
        for myField in self.fields:
            self.fields[myField].widget.attrs['class'] = 'input-text'

    class Meta:
        model = User
        fields = ('username',)


class SetPasswordForm(forms.Form):
    """
    A form that lets a user change set his/her password without
    entering the old password
    
    """
    new_password1 = forms.CharField(label=_("New password"), 
                                    widget=forms.PasswordInput)
    new_password2 = forms.CharField(label=_("New password confirmation"), 
                                    widget=forms.PasswordInput)

    def __init__(self, user, *args, **kwargs):
        self.user = user
        super(SetPasswordForm, self).__init__(*args, **kwargs)
        for myField in self.fields:
            self.fields[myField].widget.attrs['class'] = 'input-text'

    def clean_new_password2(self):
        password1 = self.cleaned_data.get('new_password1')
        password2 = self.cleaned_data.get('new_password2')
        if password1 and password2:
            if password1 != password2:
                raise forms.ValidationError(_("The two password fields didn't match."))
        return password2

    def save(self, commit=True):
        self.user.set_password(self.cleaned_data['new_password1'])
        if commit:
            self.user.save()
        return self.user


class PasswordChangeForm(SetPasswordForm):
    """
    A form that lets a user change his/her password by entering
    their old password.
    
    """
    old_password = forms.CharField(label=_("Old password"), widget=forms.PasswordInput)

    def __init__(self, *args, **kwargs):
        super(PasswordChangeForm, self).__init__(*args, **kwargs)
        for myField in self.fields:
            self.fields[myField].widget.attrs['class'] = 'input-text'

    def clean_old_password(self):
        """
        Validates that the old_password field is correct.
        """
        old_password = self.cleaned_data["old_password"]
        if not self.user.check_password(old_password):
            raise forms.ValidationError(_("Your old password was entered incorrectly. Please enter it again."))
        return old_password
PasswordChangeForm.base_fields.keyOrder = ['old_password', 'new_password1', 'new_password2']


class UserProfileUpdateForm(forms.ModelForm):
    """
    Update nickname form
    
    """

    def __init__(self, *args, **kwargs):
        super(UserProfileUpdateForm, self).__init__(*args, **kwargs)
        for myField in self.fields:
            self.fields[myField].widget.attrs['class'] = 'input-text'

    class Meta:
        model = UserProfile
        exclude = ('user', 'account_level')
        fields = ('nickname',)


class PasswordResetForm(forms.Form):
    email_username = forms.CharField(label=_("E-mail or Username"), 
                                     max_length=75)

    def __init__(self, *args, **kwargs):
        super(PasswordResetForm, self).__init__(*args, **kwargs)
        for myField in self.fields:
            self.fields[myField].widget.attrs['class'] = 'input-text'

    def clean_email_username(self):
        """
        Validates that an active user exists with the given e-mail address or username
        
        """
        email_username = self.cleaned_data["email_username"]
        if email_re.search(email_username):
            try:
                self.users_cache = list(User.objects.filter(
                                email__iexact=email_username,
                                is_active=True
                            ))
            except User.DoesNotExist:
                pass
        else:
            try:
                self.users_cache = list(User.objects.filter(
                                username__iexact=email_username,
                                is_active=True
                            ))
            except User.DoesNotExist:
                pass

        # Allow user to reset password even if registered from a social networking site
        for user in self.users_cache: 
            try: 
                oauth_user = UserSocialAuth.objects.get(user=user)
                raise forms.ValidationError(_("Your Screenbird account is based off of either Google or Facebook. To login with either of those, please use one of these links:"))
            except UserSocialAuth.DoesNotExist:
                oauth_user = None
        if len(self.users_cache) == 0:
            raise forms.ValidationError(_("That e-mail address or username doesn't have an associated user account. Are you sure you've registered?"))
        return email_username

    def save(self, domain_override=None, email_template_name='registration/password_reset_email.html',
             use_https=False, token_generator=default_token_generator, from_email=None, request=None):
        """
        Generates a one-use only link for resetting password and sends to the user
        
        """
        from django.core.mail import send_mail
        for user in self.users_cache:
            if not domain_override:
                current_site = get_current_site(request)
                site_name = current_site.name
                domain = current_site.domain
            else:
                site_name = domain = domain_override
            t = loader.get_template(email_template_name)
            c = {
                'email': user.email,
                'domain': domain,
                'site_name': site_name,
                'uid': int_to_base36(user.id),
                'user': user,
                'token': token_generator.make_token(user),
                'protocol': use_https and 'https' or 'http',
            }
            send_mail(_("Password reset on %s") % site_name,
                t.render(Context(c)), from_email, [user.email], fail_silently=False)


class PayPalPaymentsForm(PayPalPaymentsForm):
    '''Extended django-paypals PayPalPaymentsForm to customize button image and render
    '''
    MONTHLY_IMAGE = settings.MEDIA_URL + 'gfx/premium_button%201.png'
    YEARLY_IMAGE = settings.MEDIA_URL + 'gfx/premium_button%202.png'
    PASTEVID_MONTHLY = 'pastevid_monthly'
    PASTEVID_YEARLY = 'pastevid_yearly'

    def render(self):
        if settings.SITE_ID == 2:
            if self.button_type == self.PASTEVID_MONTHLY:
                link_text = "Monthly"
                tagline = "$9/month"
            else:
                link_text = "Yearly"
                tagline = "$99/year"
            rendered_form = mark_safe(u"""<form action="%s" method="post" id="%s">
    %s
    <a href="javascript:{}" style="text-align:center;" class="buy_now" onclick="document.getElementById('%s').submit(); return false;">%s</a><div class="tagline">%s</div><br><br>
</form>""" % (POSTBACK_ENDPOINT, self.button_type, self.as_p(), self.button_type, link_text, tagline))
        else:
            rendered_form = mark_safe(u"""<form action="%s" method="post" id="%s">
    %s
    <input type="image" src="%s" border="0" name="submit" alt="Buy it Now" />
</form>""" % (POSTBACK_ENDPOINT, self.button_type, self.as_p(), self.get_image()))

        return rendered_form

    def get_image(self):
        return {
            (True, self.PASTEVID_MONTHLY): self.MONTHLY_IMAGE,
            (True, self.PASTEVID_YEARLY): self.YEARLY_IMAGE,
            (True, self.SUBSCRIBE): SUBSCRIPTION_SANDBOX_IMAGE,
            (True, self.BUY): SANDBOX_IMAGE,
            (True, self.DONATE): DONATION_SANDBOX_IMAGE,
            (False, self.PASTEVID_MONTHLY): self.MONTHLY_IMAGE,
            (False, self.PASTEVID_YEARLY): self.YEARLY_IMAGE,
            (False, self.SUBSCRIBE): SUBSCRIPTION_IMAGE,
            (False, self.BUY): IMAGE,
            (False, self.DONATE): DONATION_IMAGE,
        }[TEST, self.button_type]


class PaymentInformationForm(forms.Form):
    """
    A form that lets users enter their payment information to be used with
    Authorize.net
    
    Note: Authorize.net payment option is currently on backlog
    
    """
    card_number = forms.CharField(required=True, max_length=16)
    expiry_date = forms.DateField(required=True, widget=forms.widgets.DateInput(format="%m/%d/%Y") )

    card_code = forms.CharField(required=True, max_length=10)
    first_name = forms.CharField(required=False, max_length=30)
    last_name = forms.CharField(required=False, max_length=30)
    company = forms.CharField(required=False, max_length=150)
    address = forms.CharField(required=False, max_length=150)
    city = forms.CharField(required=False, max_length=150)
    state = forms.CharField(required=False, max_length=150)
    province = forms.CharField(required=False, max_length=150)
    country  = forms.CharField(required=False, max_length=150)
    zip_code  = forms.CharField(required=False, max_length=150)
    email = forms.EmailField(required=False)
    phone = forms.CharField(required=False, max_length=15)
    
    
