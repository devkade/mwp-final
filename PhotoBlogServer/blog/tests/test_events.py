import json
from io import BytesIO
from PIL import Image
from django.test import TestCase
from django.contrib.auth.models import User
from django.core.files.uploadedfile import SimpleUploadedFile
from django.utils import timezone
from rest_framework.test import APIClient
from rest_framework.authtoken.models import Token
from rest_framework import status
from blog.models import GymMachine, MachineEvent, ApiUser


def create_test_image(name='test.jpg', size=(100, 100), color='red'):
    """Create a test image file for upload testing"""
    file = BytesIO()
    image = Image.new('RGB', size, color)
    image.save(file, 'JPEG')
    file.seek(0)
    return SimpleUploadedFile(
        name=name,
        content=file.read(),
        content_type='image/jpeg'
    )


class MachineEventModelTestCase(TestCase):
    """Test cases for MachineEvent model - Story 2.1 AC #1, #2, #3"""

    def setUp(self):
        self.machine = GymMachine.objects.create(
            name='런닝머신 #1',
            machine_type='treadmill',
            location='1층 A구역',
            is_active=True
        )

    def test_create_machine_event_with_all_fields(self):
        """
        Given: MachineEvent model exists
        When: Creating an event with all required fields
        Then: Event is stored with correct data
        """
        event = MachineEvent.objects.create(
            machine=self.machine,
            event_type='start',
            image=create_test_image(),
            captured_at=timezone.now(),
            person_count=1,
            detections={'person': [{'bbox': [100, 50, 300, 400], 'confidence': 0.95}]},
            change_info={'event_type': 'start', 'prev_count': 0, 'curr_count': 1}
        )
        self.assertEqual(event.machine, self.machine)
        self.assertEqual(event.event_type, 'start')
        self.assertEqual(event.person_count, 1)
        self.assertIsNotNone(event.created_at)
        self.assertIn('person', event.detections)

    def test_machine_event_str_representation(self):
        """Test __str__ returns machine name - event type display (captured_at)"""
        captured = timezone.now()
        event = MachineEvent.objects.create(
            machine=self.machine,
            event_type='start',
            image=create_test_image(),
            captured_at=captured,
            person_count=1
        )
        expected = f"{self.machine.name} - 사용 시작 ({captured})"
        self.assertEqual(str(event), expected)

    def test_machine_event_ordering(self):
        """Test events are ordered by -captured_at (newest first)"""
        now = timezone.now()
        older = MachineEvent.objects.create(
            machine=self.machine,
            event_type='start',
            image=create_test_image('old.jpg'),
            captured_at=now - timezone.timedelta(hours=1),
            person_count=1
        )
        newer = MachineEvent.objects.create(
            machine=self.machine,
            event_type='end',
            image=create_test_image('new.jpg'),
            captured_at=now,
            person_count=0
        )
        events = list(MachineEvent.objects.all())
        self.assertEqual(events[0], newer)
        self.assertEqual(events[1], older)

    def test_event_type_start_stores_correctly(self):
        """
        Story 2.1 AC #2
        Given: I POST an event with event_type="start"
        When: The request is processed
        Then: The event is stored with event_type="start"
        """
        event = MachineEvent.objects.create(
            machine=self.machine,
            event_type='start',
            image=create_test_image(),
            captured_at=timezone.now(),
            person_count=1
        )
        self.assertEqual(event.event_type, 'start')
        self.assertEqual(event.get_event_type_display(), '사용 시작')

    def test_event_type_end_stores_correctly(self):
        """
        Story 2.1 AC #3
        Given: I POST an event with event_type="end"
        When: The request is processed
        Then: The event is stored with event_type="end"
        """
        event = MachineEvent.objects.create(
            machine=self.machine,
            event_type='end',
            image=create_test_image(),
            captured_at=timezone.now(),
            person_count=0
        )
        self.assertEqual(event.event_type, 'end')
        self.assertEqual(event.get_event_type_display(), '사용 종료')

    def test_cascade_delete_on_machine_delete(self):
        """Test events are deleted when machine is deleted"""
        MachineEvent.objects.create(
            machine=self.machine,
            event_type='start',
            image=create_test_image(),
            captured_at=timezone.now(),
            person_count=1
        )
        self.assertEqual(MachineEvent.objects.count(), 1)
        self.machine.delete()
        self.assertEqual(MachineEvent.objects.count(), 0)


class MachineEventAPITestCase(TestCase):
    """Test cases for MachineEvent API endpoints - Story 2.1"""

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

        # Create test machine
        self.machine = GymMachine.objects.create(
            name='런닝머신 #1',
            machine_type='treadmill',
            location='1층 A구역',
            is_active=True
        )

    def test_event_post_success_with_valid_token_and_machine(self):
        """
        Story 2.1 AC #1
        Given: The Django server is running with MachineEvent model
        When: I POST to /api/machines/{machine_id}/events/ with valid data
        Then: A new MachineEvent is created and I receive HTTP 201
        """
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        data = {
            'event_type': 'start',
            'image': create_test_image(),
            'captured_at': timezone.now().isoformat(),
            'person_count': 1,
            'detections': json.dumps({'person': [{'bbox': [100, 50, 300, 400], 'confidence': 0.95}]}),
            'change_info': json.dumps({'event_type': 'start', 'prev_count': 0, 'curr_count': 1})
        }

        response = self.client.post(
            f'/api/machines/{self.machine.id}/events/',
            data,
            format='multipart'
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertIn('id', response.data)
        self.assertEqual(response.data['event_type'], 'start')
        self.assertEqual(response.data['machine'], self.machine.id)
        self.assertEqual(MachineEvent.objects.count(), 1)

    def test_event_post_returns_full_event_data(self):
        """
        Story 2.1 AC #1 - Verify response includes all event fields
        """
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        data = {
            'event_type': 'start',
            'image': create_test_image(),
            'captured_at': timezone.now().isoformat(),
            'person_count': 1,
        }

        response = self.client.post(
            f'/api/machines/{self.machine.id}/events/',
            data,
            format='multipart'
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

        required_fields = ['id', 'machine', 'machine_name', 'event_type',
                          'event_type_display', 'image', 'captured_at',
                          'person_count', 'detections', 'change_info']
        for field in required_fields:
            self.assertIn(field, response.data, f"Field '{field}' missing from response")

    def test_event_post_start_stores_correctly(self):
        """
        Story 2.1 AC #2
        Given: I POST an event with event_type="start"
        When: The request is processed
        Then: The event is stored with event_type="start" and person_count >= 1
        """
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        data = {
            'event_type': 'start',
            'image': create_test_image(),
            'captured_at': timezone.now().isoformat(),
            'person_count': 2,
        }

        response = self.client.post(
            f'/api/machines/{self.machine.id}/events/',
            data,
            format='multipart'
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        event = MachineEvent.objects.get(pk=response.data['id'])
        self.assertEqual(event.event_type, 'start')
        self.assertGreaterEqual(event.person_count, 1)

    def test_event_post_end_stores_correctly(self):
        """
        Story 2.1 AC #3
        Given: I POST an event with event_type="end"
        When: The request is processed
        Then: The event is stored with event_type="end" and person_count = 0
        """
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        data = {
            'event_type': 'end',
            'image': create_test_image(),
            'captured_at': timezone.now().isoformat(),
            'person_count': 0,
        }

        response = self.client.post(
            f'/api/machines/{self.machine.id}/events/',
            data,
            format='multipart'
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        event = MachineEvent.objects.get(pk=response.data['id'])
        self.assertEqual(event.event_type, 'end')
        self.assertEqual(event.person_count, 0)

    def test_event_post_nonexistent_machine_returns_404(self):
        """
        Story 2.1 AC #4
        Given: The referenced machine_id does not exist
        When: I POST an event
        Then: I receive HTTP 404 Not Found
        """
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        data = {
            'event_type': 'start',
            'image': create_test_image(),
            'captured_at': timezone.now().isoformat(),
            'person_count': 1,
        }

        response = self.client.post(
            '/api/machines/99999/events/',
            data,
            format='multipart'
        )

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_event_post_inactive_machine_returns_404(self):
        """
        Story 2.1 AC #4 - Inactive machine should also return 404
        """
        inactive_machine = GymMachine.objects.create(
            name='비활성 머신',
            machine_type='other',
            location='3층 C구역',
            is_active=False
        )

        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        data = {
            'event_type': 'start',
            'image': create_test_image(),
            'captured_at': timezone.now().isoformat(),
            'person_count': 1,
        }

        response = self.client.post(
            f'/api/machines/{inactive_machine.id}/events/',
            data,
            format='multipart'
        )

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_event_post_without_auth_returns_401(self):
        """
        Story 2.1 AC #5
        Given: I have no valid auth token
        When: I POST an event
        Then: I receive HTTP 401 Unauthorized
        """
        data = {
            'event_type': 'start',
            'image': create_test_image(),
            'captured_at': timezone.now().isoformat(),
            'person_count': 1,
        }

        response = self.client.post(
            f'/api/machines/{self.machine.id}/events/',
            data,
            format='multipart'
        )

        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_event_post_with_invalid_token_returns_401(self):
        """
        Story 2.1 AC #5 - Invalid token should also return 401
        """
        self.client.credentials(HTTP_AUTHORIZATION='Token invalid-token-xxx')

        data = {
            'event_type': 'start',
            'image': create_test_image(),
            'captured_at': timezone.now().isoformat(),
            'person_count': 1,
        }

        response = self.client.post(
            f'/api/machines/{self.machine.id}/events/',
            data,
            format='multipart'
        )

        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_event_post_with_json_string_detections(self):
        """Test that JSON string detections are properly parsed"""
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        detections_data = {'person': [{'bbox': [10, 20, 30, 40], 'confidence': 0.9}]}
        change_info_data = {'event_type': 'start', 'prev_count': 0, 'curr_count': 1}

        data = {
            'event_type': 'start',
            'image': create_test_image(),
            'captured_at': timezone.now().isoformat(),
            'person_count': 1,
            'detections': json.dumps(detections_data),
            'change_info': json.dumps(change_info_data)
        }

        response = self.client.post(
            f'/api/machines/{self.machine.id}/events/',
            data,
            format='multipart'
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        event = MachineEvent.objects.get(pk=response.data['id'])
        self.assertEqual(event.detections, detections_data)
        self.assertEqual(event.change_info, change_info_data)


class MachineEventListAPITestCase(TestCase):
    """Test cases for MachineEvent list endpoint (GET)"""

    def setUp(self):
        self.client = APIClient()
        self.user = User.objects.create_user(username='testuser', password='testpass')
        self.api_user = ApiUser.objects.create(
            name='Test API User',
            security_key='test-key-123',
            user=self.user,
            is_active=True
        )
        self.token, _ = Token.objects.get_or_create(user=self.user)

        self.machine = GymMachine.objects.create(
            name='런닝머신 #1',
            machine_type='treadmill',
            location='1층 A구역',
            is_active=True
        )

        # Create test events
        now = timezone.now()
        self.event1 = MachineEvent.objects.create(
            machine=self.machine,
            event_type='start',
            image=create_test_image('event1.jpg'),
            captured_at=now - timezone.timedelta(hours=2),
            person_count=1
        )
        self.event2 = MachineEvent.objects.create(
            machine=self.machine,
            event_type='end',
            image=create_test_image('event2.jpg'),
            captured_at=now - timezone.timedelta(hours=1),
            person_count=0
        )

    def test_event_list_success(self):
        """Test GET /api/machines/{id}/events/ returns events"""
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get(f'/api/machines/{self.machine.id}/events/')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsInstance(response.data, list)
        self.assertEqual(len(response.data), 2)

    def test_event_list_ordered_by_captured_at_desc(self):
        """Test events are ordered by captured_at descending (newest first)"""
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get(f'/api/machines/{self.machine.id}/events/')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        # event2 is newer, should be first
        self.assertEqual(response.data[0]['id'], self.event2.id)
        self.assertEqual(response.data[1]['id'], self.event1.id)

    def test_event_list_filter_by_event_type_start(self):
        """Test filtering by event_type=start"""
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get(f'/api/machines/{self.machine.id}/events/?event_type=start')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data), 1)
        self.assertEqual(response.data[0]['event_type'], 'start')

    def test_event_list_filter_by_event_type_end(self):
        """Test filtering by event_type=end"""
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        response = self.client.get(f'/api/machines/{self.machine.id}/events/?event_type=end')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data), 1)
        self.assertEqual(response.data[0]['event_type'], 'end')

    def test_event_list_without_auth_returns_401(self):
        """Test GET without auth returns 401"""
        response = self.client.get(f'/api/machines/{self.machine.id}/events/')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)
