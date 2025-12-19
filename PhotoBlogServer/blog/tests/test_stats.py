from datetime import date, timedelta
from django.test import TestCase
from django.contrib.auth.models import User
from django.utils import timezone
from rest_framework.test import APIClient
from rest_framework.authtoken.models import Token
from rest_framework import status
from blog.models import GymMachine, MachineEvent, ApiUser


class MachineStatsAPITestCase(TestCase):
    """Test cases for machine statistics API endpoint"""

    def setUp(self):
        self.client = APIClient()
        # Create user and get token for authenticated requests
        self.user = User.objects.create_user(username='testuser', password='testpass')
        self.api_user = ApiUser.objects.create(
            name='Test API User',
            security_key='test-key-stats-123',
            user=self.user,
            is_active=True
        )
        self.token, _ = Token.objects.get_or_create(user=self.user)

        # Create test machine
        self.machine = GymMachine.objects.create(
            name='런닝머신 #1',
            machine_type='treadmill',
            location='1층 A구역',
            is_active=True
        )

        # Create another machine for testing
        self.machine2 = GymMachine.objects.create(
            name='벤치프레스 #1',
            machine_type='bench_press',
            location='2층 B구역',
            is_active=True
        )

    def _create_event(self, machine, event_type, captured_at):
        """Helper to create a MachineEvent"""
        return MachineEvent.objects.create(
            machine=machine,
            event_type=event_type,
            image='events/test.jpg',
            captured_at=captured_at,
            person_count=1,
            detections={},
            change_info={}
        )

    def test_stats_endpoint_returns_200_with_valid_token(self):
        """AC#1: Test stats endpoint returns 200 with valid token"""
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get(f'/api_root/machines/{self.machine.id}/stats/')

        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_stats_response_contains_required_fields(self):
        """AC#2: Test response contains machine_id, machine_name, total_starts, total_ends, daily_usage"""
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get(f'/api_root/machines/{self.machine.id}/stats/')

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        required_fields = ['machine_id', 'machine_name', 'total_starts', 'total_ends', 'daily_usage']
        for field in required_fields:
            self.assertIn(field, response.data, f"Field '{field}' missing from response")

        self.assertEqual(response.data['machine_id'], self.machine.id)
        self.assertEqual(response.data['machine_name'], self.machine.name)

    def test_stats_counts_events_correctly(self):
        """AC#2: Test total_starts and total_ends are counted correctly"""
        now = timezone.now()

        # Create 3 start events and 2 end events
        for i in range(3):
            self._create_event(self.machine, 'start', now - timedelta(hours=i))
        for i in range(2):
            self._create_event(self.machine, 'end', now - timedelta(hours=i))

        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get(f'/api_root/machines/{self.machine.id}/stats/')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['total_starts'], 3)
        self.assertEqual(response.data['total_ends'], 2)

    def test_date_from_filter(self):
        """AC#3: Test date_from filter excludes earlier events"""
        today = timezone.now().replace(hour=12, minute=0, second=0, microsecond=0)
        yesterday = today - timedelta(days=1)
        two_days_ago = today - timedelta(days=2)

        # Create events on different days
        self._create_event(self.machine, 'start', two_days_ago)
        self._create_event(self.machine, 'start', yesterday)
        self._create_event(self.machine, 'start', today)

        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        # Filter from yesterday - should get 2 events
        date_from = yesterday.date().isoformat()
        response = self.client.get(f'/api_root/machines/{self.machine.id}/stats/?date_from={date_from}')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['total_starts'], 2)

    def test_date_to_filter(self):
        """AC#3: Test date_to filter excludes later events"""
        today = timezone.now().replace(hour=12, minute=0, second=0, microsecond=0)
        yesterday = today - timedelta(days=1)
        two_days_ago = today - timedelta(days=2)

        # Create events on different days
        self._create_event(self.machine, 'start', two_days_ago)
        self._create_event(self.machine, 'start', yesterday)
        self._create_event(self.machine, 'start', today)

        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        # Filter to yesterday - should get 2 events (two_days_ago and yesterday)
        date_to = yesterday.date().isoformat()
        response = self.client.get(f'/api_root/machines/{self.machine.id}/stats/?date_to={date_to}')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['total_starts'], 2)

    def test_combined_date_filters(self):
        """AC#3: Test combined date_from and date_to filters"""
        today = timezone.now().replace(hour=12, minute=0, second=0, microsecond=0)
        yesterday = today - timedelta(days=1)
        two_days_ago = today - timedelta(days=2)
        three_days_ago = today - timedelta(days=3)

        # Create events on different days
        self._create_event(self.machine, 'start', three_days_ago)
        self._create_event(self.machine, 'start', two_days_ago)
        self._create_event(self.machine, 'start', yesterday)
        self._create_event(self.machine, 'start', today)

        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        # Filter from two_days_ago to yesterday - should get 2 events
        date_from = two_days_ago.date().isoformat()
        date_to = yesterday.date().isoformat()
        response = self.client.get(
            f'/api_root/machines/{self.machine.id}/stats/?date_from={date_from}&date_to={date_to}'
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['total_starts'], 2)

    def test_daily_usage_format(self):
        """AC#4: Test daily_usage contains date and count fields"""
        today = timezone.now().replace(hour=12, minute=0, second=0, microsecond=0)

        # Create 2 start events on today
        self._create_event(self.machine, 'start', today)
        self._create_event(self.machine, 'start', today - timedelta(hours=1))

        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get(f'/api_root/machines/{self.machine.id}/stats/')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsInstance(response.data['daily_usage'], list)
        self.assertEqual(len(response.data['daily_usage']), 1)

        daily_item = response.data['daily_usage'][0]
        self.assertIn('date', daily_item)
        self.assertIn('count', daily_item)
        self.assertEqual(daily_item['count'], 2)

    def test_daily_usage_ordered_by_date(self):
        """AC#4: Test daily_usage is ordered by date ascending"""
        today = timezone.now().replace(hour=12, minute=0, second=0, microsecond=0)
        yesterday = today - timedelta(days=1)
        two_days_ago = today - timedelta(days=2)

        # Create events on different days (out of order)
        self._create_event(self.machine, 'start', today)
        self._create_event(self.machine, 'start', two_days_ago)
        self._create_event(self.machine, 'start', yesterday)

        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get(f'/api_root/machines/{self.machine.id}/stats/')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        daily_usage = response.data['daily_usage']
        self.assertEqual(len(daily_usage), 3)

        # Verify dates are in ascending order
        dates = [item['date'] for item in daily_usage]
        self.assertEqual(dates, sorted(dates))

    def test_daily_usage_only_counts_start_events(self):
        """AC#4: Test daily_usage only counts start events, not end events"""
        today = timezone.now().replace(hour=12, minute=0, second=0, microsecond=0)

        # Create start and end events
        self._create_event(self.machine, 'start', today)
        self._create_event(self.machine, 'start', today - timedelta(hours=1))
        self._create_event(self.machine, 'end', today - timedelta(hours=2))

        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get(f'/api_root/machines/{self.machine.id}/stats/')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data['daily_usage']), 1)
        self.assertEqual(response.data['daily_usage'][0]['count'], 2)  # Only start events

    def test_empty_results_for_machine_with_no_events(self):
        """AC#5: Test empty results when no events exist"""
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get(f'/api_root/machines/{self.machine.id}/stats/')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['total_starts'], 0)
        self.assertEqual(response.data['total_ends'], 0)
        self.assertEqual(response.data['daily_usage'], [])

    def test_404_for_nonexistent_machine(self):
        """AC#6: Test 404 for non-existent machine"""
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get('/api_root/machines/99999/stats/')

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_401_for_unauthenticated_request(self):
        """Test unauthenticated request returns 401"""
        response = self.client.get(f'/api_root/machines/{self.machine.id}/stats/')

        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_stats_only_includes_events_for_specific_machine(self):
        """Test stats only counts events for the requested machine"""
        now = timezone.now()

        # Create events for machine1
        self._create_event(self.machine, 'start', now)
        self._create_event(self.machine, 'start', now - timedelta(hours=1))

        # Create events for machine2
        self._create_event(self.machine2, 'start', now)

        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get(f'/api_root/machines/{self.machine.id}/stats/')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['total_starts'], 2)  # Only machine1's events
