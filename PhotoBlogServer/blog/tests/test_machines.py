from django.test import TestCase
from django.contrib.auth.models import User
from rest_framework.test import APIClient
from rest_framework.authtoken.models import Token
from rest_framework import status
from blog.models import GymMachine, ApiUser


class GymMachineModelTestCase(TestCase):
    """Test cases for GymMachine model"""

    def test_create_gym_machine_with_all_fields(self):
        """Test creating a GymMachine with all fields"""
        machine = GymMachine.objects.create(
            name='런닝머신 #1',
            machine_type='treadmill',
            location='1층 A구역',
            description='ProForm 상업용 런닝머신',
            is_active=True
        )
        self.assertEqual(machine.name, '런닝머신 #1')
        self.assertEqual(machine.machine_type, 'treadmill')
        self.assertEqual(machine.location, '1층 A구역')
        self.assertEqual(machine.description, 'ProForm 상업용 런닝머신')
        self.assertTrue(machine.is_active)
        self.assertIsNotNone(machine.created_at)

    def test_gym_machine_str_representation(self):
        """Test __str__ returns name (location)"""
        machine = GymMachine.objects.create(
            name='벤치프레스',
            machine_type='bench_press',
            location='2층 B구역'
        )
        self.assertEqual(str(machine), '벤치프레스 (2층 B구역)')

    def test_gym_machine_default_is_active(self):
        """Test is_active defaults to True"""
        machine = GymMachine.objects.create(
            name='Test Machine',
            machine_type='other',
            location='Test Location'
        )
        self.assertTrue(machine.is_active)

    def test_gym_machine_ordering(self):
        """Test machines are ordered by location, then name"""
        GymMachine.objects.create(name='Machine B', machine_type='other', location='Zone B')
        GymMachine.objects.create(name='Machine A', machine_type='other', location='Zone B')
        GymMachine.objects.create(name='Machine C', machine_type='other', location='Zone A')

        machines = list(GymMachine.objects.all())
        # Zone A comes before Zone B
        self.assertEqual(machines[0].name, 'Machine C')  # Zone A
        # Within Zone B, alphabetical by name
        self.assertEqual(machines[1].name, 'Machine A')  # Zone B, A
        self.assertEqual(machines[2].name, 'Machine B')  # Zone B, B


class GymMachineAPITestCase(TestCase):
    """Test cases for GymMachine API endpoints"""

    def setUp(self):
        self.client = APIClient()
        # Create user and get token for authenticated requests
        self.user = User.objects.create_user(username='testuser', password='testpass')
        self.api_user = ApiUser.objects.create(
            name='Test API User',
            security_key='test-key-123',
            user=self.user,
            is_active=True
        )
        self.token, _ = Token.objects.get_or_create(user=self.user)

        # Create test machines
        self.machine1 = GymMachine.objects.create(
            name='런닝머신 #1',
            machine_type='treadmill',
            location='1층 A구역',
            description='ProForm 상업용 런닝머신',
            is_active=True
        )
        self.machine2 = GymMachine.objects.create(
            name='벤치프레스 #1',
            machine_type='bench_press',
            location='2층 B구역',
            is_active=True
        )
        self.inactive_machine = GymMachine.objects.create(
            name='비활성 머신',
            machine_type='other',
            location='3층 C구역',
            is_active=False
        )

    def test_machine_list_success_with_valid_token(self):
        """Test GET /api_root/machines/ with valid token returns 200"""
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get('/api_root/machines/')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsInstance(response.data, list)

    def test_machine_list_includes_required_fields(self):
        """Test each machine object includes all required fields"""
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get('/api_root/machines/')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertGreater(len(response.data), 0)

        machine = response.data[0]
        required_fields = ['id', 'name', 'machine_type', 'location',
                           'description', 'thumbnail', 'is_active',
                           'event_count', 'last_event']
        for field in required_fields:
            self.assertIn(field, machine, f"Field '{field}' missing from response")

    def test_machine_list_event_count_is_zero(self):
        """Test event_count is 0 when no events exist"""
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get('/api_root/machines/')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        for machine in response.data:
            self.assertEqual(machine['event_count'], 0)

    def test_machine_list_last_event_is_null(self):
        """Test last_event is null when no events exist"""
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get('/api_root/machines/')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        for machine in response.data:
            self.assertIsNone(machine['last_event'])

    def test_inactive_machines_excluded(self):
        """Test is_active=False machines are NOT in response"""
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get('/api_root/machines/')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        # Should have 2 active machines, not 3
        self.assertEqual(len(response.data), 2)

        machine_names = [m['name'] for m in response.data]
        self.assertNotIn('비활성 머신', machine_names)

    def test_unauthorized_without_token(self):
        """Test GET /api_root/machines/ without token returns 401"""
        response = self.client.get('/api_root/machines/')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_machines_ordered_by_location_then_name(self):
        """Test machines are ordered by location, then by name"""
        # Clear existing machines
        GymMachine.objects.all().delete()

        # Create machines in specific order to test sorting
        GymMachine.objects.create(name='Machine Z', machine_type='other', location='Zone B')
        GymMachine.objects.create(name='Machine A', machine_type='other', location='Zone B')
        GymMachine.objects.create(name='Machine M', machine_type='other', location='Zone A')

        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get('/api_root/machines/')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data), 3)

        # Zone A comes before Zone B
        self.assertEqual(response.data[0]['location'], 'Zone A')
        self.assertEqual(response.data[0]['name'], 'Machine M')

        # Within Zone B, alphabetical by name
        self.assertEqual(response.data[1]['location'], 'Zone B')
        self.assertEqual(response.data[1]['name'], 'Machine A')

        self.assertEqual(response.data[2]['location'], 'Zone B')
        self.assertEqual(response.data[2]['name'], 'Machine Z')

    def test_machine_list_returns_correct_machine_type(self):
        """Test machine_type field contains correct value"""
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get('/api_root/machines/')

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        machine_types = {m['name']: m['machine_type'] for m in response.data}
        self.assertEqual(machine_types.get('런닝머신 #1'), 'treadmill')
        self.assertEqual(machine_types.get('벤치프레스 #1'), 'bench_press')
