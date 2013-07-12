from django import forms
from django.forms.util import ErrorList

from django.utils.translation import ugettext_lazy as _
from django.contrib.auth.models import User
from django.core.validators import email_re
from hashlib import md5
import random
import string


class DivErrorList(ErrorList):
    def __unicode__(self):
        return self.as_divs()
    def as_divs(self):
        if not self: return u''
        return u'%s' % ''.join([u'%s' % e for e in self])


def generate_username(email):
    """
    Generates a unique username based on the email. email must be a valid email address.
    Uses characters before the '@', generates additional random characters when the base string is already in use.
    """

    username = email[:email.find('@')]
    if len(username) > 30:
        username = username[:30]

    if User.objects.filter(username=username).count() > 0:
        if len(username) > 26:
            username = username[:26]
        while True:
            random_chars = ''.join(random.choice(string.ascii_letters + string.digits) for x in range(4))
            username = '%s%s' % (username, random_chars)
            if User.objects.filter(username=username).count() == 0:
                return username
    else:
        return username


class LoginForm(forms.Form):
    def __init__(self, *args, **kwargs):
        super(LoginForm, self).__init__(*args, **kwargs)
        for myField in self.fields:
            self.fields[myField].widget.attrs['class'] = 'input-text'
    username = forms.CharField(max_length=100,error_messages={'required': 'Please enter username'}) 
    password = forms.CharField(widget=forms.PasswordInput(render_value=False),
                               error_messages={'required': 'Please enter password'},max_length=100)
    remember_me = forms.BooleanField (label = _( 'Remember Me' ), initial = False, required = False,)

    def clean_username(self):
        username = self.cleaned_data["username"]
        self.username = username
        if email_re.search(username):
            try:
                if User.objects.filter(email=username).count() > 1:
                    raise forms.ValidationError("You somehow have a duplicate registered user with us. Please contact us to fix it.")
            except User.DoesNotExist:
                try:
                    user = User.objects.get(username=username).username
                except User.DoesNotExist:
                    raise forms.ValidationError("Username or Password Incorrect")
        else:
            #We have a non-email address username we should try username
            try:
                user = User.objects.get(username=username).username
            except User.DoesNotExist:
                raise forms.ValidationError("Username or Password Incorrect")

    def clean_password(self):
        password = self.cleaned_data.get('password')
        try:
            user = None
            if self.username:
                user = User.objects.get(username=self.username)
            if user:
                valid = user.check_password(self.cleaned_data['password'])
                if not valid:
                    raise forms.ValidationError("Username or Password Incorrect")
        except:
            pass
        return password


class RegistrationForm(forms.ModelForm):
    username = forms.CharField(widget=forms.TextInput(attrs={'class':'text'}),
                               error_messages={'required': 'Please enter username'}, required=True, max_length=70) 
    password = forms.CharField(widget=forms.PasswordInput(render_value=False, attrs={'class':'text'}),
                               error_messages={'required': 'Please enter password'}, required=True, max_length=70)
    confirm_password = forms.CharField(widget=forms.PasswordInput(render_value=False, attrs={'class':'text'}),
                               error_messages={'required': 'Please confirm password'}, required=True, max_length=70)

    class Meta:
        model = User
        fields = ( 'username', 'email', 'password' )

    def clean_username(self):
        username = self.cleaned_data["username"]
        try:
            User.objects.get(username=username)
        except User.DoesNotExist:
            return username
        raise forms.ValidationError(_("A user with that username already exists."))
        
    def clean_email(self):
        email = self.cleaned_data["email"]
        users = User.objects.filter(email=email)
        if users.count() > 0:
            raise forms.ValidationError(_("Email already registered."))
        return email

    def clean_confirm_password(self):
        password = self.cleaned_data.get("password", "")
        confirm_password = self.cleaned_data["confirm_password"]
        if password != confirm_password:
            raise forms.ValidationError(_("The two password fields didn't match."))
        return confirm_password

    def save(self, commit=True):
        user = super(RegistrationForm, self).save(commit=False)
        user.set_password(self.cleaned_data["password"])
        if commit:
            user.save()
        return user


class RegistrationNoUsernameForm(forms.ModelForm):
    def __init__(self, *args, **kwargs):
        super(RegistrationNoUsernameForm, self).__init__(*args, **kwargs)
        for myField in self.fields:
            self.fields[myField].widget.attrs['class'] = 'input-text'
        self.fields['email'].required = True
        
    username = forms.CharField(widget=forms.TextInput(attrs={'class':'text'}),
                               error_messages={'required': 'Please enter username'}, required=False, max_length=30) 
    password = forms.CharField(widget=forms.PasswordInput(render_value=False, attrs={'class':'text'}),
                               error_messages={'required': 'Please enter password'}, required=True, max_length=70)
    confirm_password = forms.CharField(widget=forms.PasswordInput(render_value=False, attrs={'class':'text'}),
                               error_messages={'required': 'Please confirm password'}, required=True, max_length=70)

    class Meta:
        model = User
        fields = ( 'username', 'email', 'password' )

    def clean_email(self):
        email = self.cleaned_data["email"]
        users = User.objects.filter(email=email)
        if users.count() > 0:
            raise forms.ValidationError(_("Email already registered."))
        return email

    def clean_username(self):
        if self.cleaned_data["username"]:
            username = self.cleaned_data["username"]
            try:
                User.objects.get(username=username)
            except User.DoesNotExist:
                return username
            raise forms.ValidationError(_("A user with that username already exists."))
        return None

    def clean_confirm_password(self):
        password = self.cleaned_data.get("password", "")
        confirm_password = self.cleaned_data["confirm_password"]
        if password != confirm_password:
            raise forms.ValidationError(_("The two password fields didn't match."))
        return confirm_password

    def save(self, commit=True):
        user = super(RegistrationNoUsernameForm, self).save(commit=False)
        user.set_password(self.cleaned_data["password"])
        if self.cleaned_data["username"]:
            user.username = self.cleaned_data["username"]
        else:
            user.username = generate_username(self.cleaned_data["email"])
        '''
        while True:
            user.username = str(md5(str(self.data['email']) + str(random.random())).hexdigest())[0:30]
            try:
                user = User.objects.get(username__iexact=user.username)
            except User.DoesNotExist:
                success = True
                break
        '''
        if commit:
            user.save()
        return user
