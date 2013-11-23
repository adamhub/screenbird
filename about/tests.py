from django.test import TestCase
from django.core.urlresolvers import reverse
from django.contrib.auth.models import User

class AboutTest(TestCase):
    fixtures = ['test_site_data.json']

    def setUp(self):
        pass
        
    def test_contact_us(self):
        #Test the contact_us() view
        client = self.client
        url = reverse('contact_us')
        
        response = client.post(url, {
            'first_name': 'Test',
            'last_name': 'User',
            'email': 'test@email.com',
            'message': 'This is just a Test Message for the Contact Us form.',
            'type': 'Feedback'
        })
        
        self.assertTrue(response.status_code == 200)
