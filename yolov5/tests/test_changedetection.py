"""
Unit tests for ChangeDetection class.

Tests bidirectional change detection (0→1+ start, 1+→0 end).
"""

import unittest
import tempfile
import shutil
import pathlib
import io
import numpy as np
from unittest.mock import patch, MagicMock

import sys
sys.path.insert(0, str(pathlib.Path(__file__).parent.parent))

from changedetection import ChangeDetection, load_gym_config


class TestChangeDetectionInit(unittest.TestCase):
    """Test ChangeDetection initialization."""

    def test_init_with_default_config(self):
        """Test initialization with default configuration."""
        names = ['person', 'bicycle', 'car']
        cd = ChangeDetection(names)

        self.assertEqual(cd.names, names)
        self.assertEqual(cd.result_prev, [0, 0, 0])
        self.assertEqual(cd.TARGET_CLASS, 'person')
        self.assertEqual(cd.config, {})

    def test_init_with_custom_target_class(self):
        """Test initialization with custom target_class."""
        names = ['person', 'bicycle', 'car']
        config = {'target_class': 'car'}
        cd = ChangeDetection(names, config)

        self.assertEqual(cd.TARGET_CLASS, 'car')

    def test_init_result_prev_matches_names_length(self):
        """Test result_prev array length matches names list."""
        names = ['person', 'bicycle', 'car', 'dog', 'cat']
        cd = ChangeDetection(names)

        self.assertEqual(len(cd.result_prev), len(names))


class TestChangeDetectionStartEvent(unittest.TestCase):
    """Test start event detection (0→1+)."""

    def setUp(self):
        self.names = ['person', 'bicycle', 'car']
        self.dummy_image = np.zeros((480, 640, 3), dtype=np.uint8)
        self.temp_dir = tempfile.mkdtemp()
        self.original_cwd = pathlib.Path.cwd()

    def tearDown(self):
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    @patch.object(ChangeDetection, '_send_event')
    @patch.object(ChangeDetection, '_save_event_image')
    def test_start_event_0_to_1(self, mock_save, mock_send):
        """Test start event triggered on 0→1 transition."""
        cd = ChangeDetection(self.names)
        detected = [1, 0, 0]  # 1 person detected

        result = cd.detect_changes(self.names, detected, self.dummy_image, [])

        self.assertIsNotNone(result)
        self.assertEqual(result['event_type'], 'start')
        self.assertEqual(result['prev_count'], 0)
        self.assertEqual(result['curr_count'], 1)
        mock_save.assert_called_once()
        mock_send.assert_called_once()

    @patch.object(ChangeDetection, '_send_event')
    @patch.object(ChangeDetection, '_save_event_image')
    def test_start_event_0_to_3(self, mock_save, mock_send):
        """Test start event triggered on 0→3 transition."""
        cd = ChangeDetection(self.names)
        detected = [3, 0, 0]  # 3 persons detected

        result = cd.detect_changes(self.names, detected, self.dummy_image, [])

        self.assertIsNotNone(result)
        self.assertEqual(result['event_type'], 'start')
        self.assertEqual(result['prev_count'], 0)
        self.assertEqual(result['curr_count'], 3)
        mock_send.assert_called_once()


class TestChangeDetectionEndEvent(unittest.TestCase):
    """Test end event detection (1+→0)."""

    def setUp(self):
        self.names = ['person', 'bicycle', 'car']
        self.dummy_image = np.zeros((480, 640, 3), dtype=np.uint8)

    @patch.object(ChangeDetection, '_send_event')
    @patch.object(ChangeDetection, '_save_event_image')
    def test_end_event_1_to_0(self, mock_save, mock_send):
        """Test end event triggered on 1→0 transition."""
        cd = ChangeDetection(self.names)
        cd.result_prev = [1, 0, 0]  # Previously 1 person
        detected = [0, 0, 0]  # Now 0 persons

        result = cd.detect_changes(self.names, detected, self.dummy_image, [])

        self.assertIsNotNone(result)
        self.assertEqual(result['event_type'], 'end')
        self.assertEqual(result['prev_count'], 1)
        self.assertEqual(result['curr_count'], 0)
        mock_save.assert_called_once()
        mock_send.assert_called_once()

    @patch.object(ChangeDetection, '_send_event')
    @patch.object(ChangeDetection, '_save_event_image')
    def test_end_event_3_to_0(self, mock_save, mock_send):
        """Test end event triggered on 3→0 transition."""
        cd = ChangeDetection(self.names)
        cd.result_prev = [3, 0, 0]  # Previously 3 persons
        detected = [0, 0, 0]  # Now 0 persons

        result = cd.detect_changes(self.names, detected, self.dummy_image, [])

        self.assertIsNotNone(result)
        self.assertEqual(result['event_type'], 'end')
        self.assertEqual(result['prev_count'], 3)
        self.assertEqual(result['curr_count'], 0)
        mock_send.assert_called_once()


class TestChangeDetectionNoEvent(unittest.TestCase):
    """Test scenarios where no event should be triggered."""

    def setUp(self):
        self.names = ['person', 'bicycle', 'car']
        self.dummy_image = np.zeros((480, 640, 3), dtype=np.uint8)

    def test_no_event_1_to_2(self):
        """Test no event on 1→2 transition (not a boundary)."""
        cd = ChangeDetection(self.names)
        cd.result_prev = [1, 0, 0]
        detected = [2, 0, 0]

        result = cd.detect_changes(self.names, detected, self.dummy_image, [])

        self.assertIsNone(result)

    def test_no_event_2_to_1(self):
        """Test no event on 2→1 transition (not a boundary)."""
        cd = ChangeDetection(self.names)
        cd.result_prev = [2, 0, 0]
        detected = [1, 0, 0]

        result = cd.detect_changes(self.names, detected, self.dummy_image, [])

        self.assertIsNone(result)

    def test_no_event_0_to_0(self):
        """Test no event on 0→0 (no change)."""
        cd = ChangeDetection(self.names)
        cd.result_prev = [0, 0, 0]
        detected = [0, 0, 0]

        result = cd.detect_changes(self.names, detected, self.dummy_image, [])

        self.assertIsNone(result)

    def test_no_event_2_to_2(self):
        """Test no event on 2→2 (no change)."""
        cd = ChangeDetection(self.names)
        cd.result_prev = [2, 0, 0]
        detected = [2, 0, 0]

        result = cd.detect_changes(self.names, detected, self.dummy_image, [])

        self.assertIsNone(result)


class TestChangeInfoStructure(unittest.TestCase):
    """Test change_info contains all required fields."""

    def setUp(self):
        self.names = ['person', 'bicycle', 'car']
        self.dummy_image = np.zeros((480, 640, 3), dtype=np.uint8)

    @patch.object(ChangeDetection, '_send_event')
    @patch.object(ChangeDetection, '_save_event_image')
    def test_change_info_has_required_fields(self, mock_save, mock_send):
        """Test change_info contains event_type, target_class, prev_count, curr_count, timestamp."""
        cd = ChangeDetection(self.names)
        detected = [1, 0, 0]

        result = cd.detect_changes(self.names, detected, self.dummy_image, [])

        self.assertIn('event_type', result)
        self.assertIn('target_class', result)
        self.assertIn('prev_count', result)
        self.assertIn('curr_count', result)
        self.assertIn('timestamp', result)

    @patch.object(ChangeDetection, '_send_event')
    @patch.object(ChangeDetection, '_save_event_image')
    def test_change_info_target_class_value(self, mock_save, mock_send):
        """Test change_info target_class matches configured value."""
        config = {'target_class': 'bicycle'}
        cd = ChangeDetection(self.names, config)
        detected = [0, 1, 0]  # 1 bicycle detected

        result = cd.detect_changes(self.names, detected, self.dummy_image, [])

        self.assertEqual(result['target_class'], 'bicycle')

    @patch.object(ChangeDetection, '_send_event')
    @patch.object(ChangeDetection, '_save_event_image')
    def test_timestamp_is_iso_format(self, mock_save, mock_send):
        """Test timestamp is in ISO format."""
        cd = ChangeDetection(self.names)
        detected = [1, 0, 0]

        result = cd.detect_changes(self.names, detected, self.dummy_image, [])

        # ISO format contains 'T' separator between date and time
        self.assertIn('T', result['timestamp'])
        # Should be parseable (basic check)
        self.assertGreater(len(result['timestamp']), 10)


class TestImageSave(unittest.TestCase):
    """Test image save functionality."""

    def setUp(self):
        self.names = ['person', 'bicycle', 'car']
        # Create a test image with specific dimensions
        self.test_image = np.random.randint(0, 255, (720, 1280, 3), dtype=np.uint8)
        self.temp_dir = tempfile.mkdtemp()

    def tearDown(self):
        shutil.rmtree(self.temp_dir, ignore_errors=True)
        # Clean up events directory if created
        events_dir = pathlib.Path('runs/detect/events')
        if events_dir.exists():
            shutil.rmtree(events_dir, ignore_errors=True)

    @patch.object(ChangeDetection, '_send_event')
    def test_image_saved_to_correct_directory(self, mock_send):
        """Test image saved to runs/detect/events/."""
        cd = ChangeDetection(self.names)
        detected = [1, 0, 0]

        cd.detect_changes(self.names, detected, self.test_image, [])

        events_dir = pathlib.Path('runs/detect/events')
        self.assertTrue(events_dir.exists())
        files = list(events_dir.glob('*.jpg'))
        self.assertEqual(len(files), 1)

    @patch.object(ChangeDetection, '_send_event')
    def test_image_dimensions_640x480(self, mock_send):
        """Test saved image dimensions are 640x480."""
        import cv2

        cd = ChangeDetection(self.names)
        detected = [1, 0, 0]

        cd.detect_changes(self.names, detected, self.test_image, [])

        events_dir = pathlib.Path('runs/detect/events')
        files = list(events_dir.glob('*.jpg'))
        saved_image = cv2.imread(str(files[0]))

        self.assertEqual(saved_image.shape[1], 640)  # width
        self.assertEqual(saved_image.shape[0], 480)  # height

    @patch.object(ChangeDetection, '_send_event')
    def test_filename_contains_event_type(self, mock_send):
        """Test filename contains event type."""
        cd = ChangeDetection(self.names)
        detected = [1, 0, 0]

        cd.detect_changes(self.names, detected, self.test_image, [])

        events_dir = pathlib.Path('runs/detect/events')
        files = list(events_dir.glob('*.jpg'))
        filename = files[0].name

        self.assertIn('start', filename)

    @patch.object(ChangeDetection, '_send_event')
    def test_filename_format(self, mock_send):
        """Test filename follows YYYYMMDD_HHMMSS_eventtype.jpg format."""
        cd = ChangeDetection(self.names)
        detected = [1, 0, 0]

        cd.detect_changes(self.names, detected, self.test_image, [])

        events_dir = pathlib.Path('runs/detect/events')
        files = list(events_dir.glob('*.jpg'))
        filename = files[0].name

        # Check format: YYYYMMDD_HHMMSS_start.jpg or YYYYMMDD_HHMMSS_end.jpg
        parts = filename.replace('.jpg', '').split('_')
        self.assertEqual(len(parts), 3)
        self.assertEqual(len(parts[0]), 8)  # YYYYMMDD
        self.assertEqual(len(parts[1]), 6)  # HHMMSS
        self.assertIn(parts[2], ['start', 'end'])


class TestStateUpdate(unittest.TestCase):
    """Test state update after detection."""

    def setUp(self):
        self.names = ['person', 'bicycle', 'car']
        self.dummy_image = np.zeros((480, 640, 3), dtype=np.uint8)

    @patch.object(ChangeDetection, '_send_event')
    @patch.object(ChangeDetection, '_save_event_image')
    def test_result_prev_updated_after_event(self, mock_save, mock_send):
        """Test result_prev is updated after event detection."""
        cd = ChangeDetection(self.names)
        detected = [1, 0, 0]

        cd.detect_changes(self.names, detected, self.dummy_image, [])

        self.assertEqual(cd.result_prev, [1, 0, 0])

    def test_result_prev_updated_when_no_event(self):
        """Test result_prev is updated even when no event triggered."""
        cd = ChangeDetection(self.names)
        cd.result_prev = [1, 0, 0]
        detected = [2, 0, 0]

        cd.detect_changes(self.names, detected, self.dummy_image, [])

        self.assertEqual(cd.result_prev, [2, 0, 0])


class TestTargetClassHandling(unittest.TestCase):
    """Test target class handling edge cases."""

    def setUp(self):
        self.names = ['person', 'bicycle', 'car']
        self.dummy_image = np.zeros((480, 640, 3), dtype=np.uint8)

    def test_target_class_not_in_names_defaults_to_index_0(self):
        """Test when target_class not in names, defaults to index 0."""
        config = {'target_class': 'unknown_class'}
        cd = ChangeDetection(self.names, config)
        detected = [1, 0, 0]

        # Should use index 0 (person) as fallback
        with patch.object(cd, '_save_event_image'), patch.object(cd, '_send_event'):
            result = cd.detect_changes(self.names, detected, self.dummy_image, [])

        self.assertIsNotNone(result)
        self.assertEqual(result['event_type'], 'start')


class TestAuthentication(unittest.TestCase):
    """Test ChangeDetection authentication."""

    def setUp(self):
        self.names = ['person', 'bicycle', 'car']

    @patch('changedetection.requests.post')
    def test_auth_success_stores_token(self, mock_post):
        """Test successful authentication stores token."""
        response = MagicMock()
        response.raise_for_status.return_value = None
        response.json.return_value = {'token': 'abc123'}
        mock_post.return_value = response

        cd = ChangeDetection(self.names, {'host': 'https://example.com'})
        cd.SECURITY_KEY = 'key'
        cd._authenticate()

        self.assertEqual(cd.token, 'abc123')
        mock_post.assert_called_once()

    @patch('changedetection.requests.post')
    def test_auth_failure_raises_exception(self, mock_post):
        """Test failed authentication raises exception."""
        response = MagicMock()
        response.raise_for_status.side_effect = Exception('invalid key')
        mock_post.return_value = response

        cd = ChangeDetection(self.names, {'host': 'https://example.com'})
        cd.SECURITY_KEY = 'bad'
        with self.assertRaises(Exception):
            cd._authenticate()


class TestEventUpload(unittest.TestCase):
    """Test ChangeDetection event upload."""

    def setUp(self):
        self.names = ['person', 'bicycle', 'car']

    @patch('builtins.print')
    @patch('changedetection.requests.post')
    def test_send_event_success_logs(self, mock_post, mock_print):
        """Test successful event upload logs on 201."""
        response = MagicMock()
        response.status_code = 201
        mock_post.return_value = response

        cd = ChangeDetection(self.names, {'host': 'https://example.com', 'machine_id': 7})
        cd.token = 'abc123'

        with patch('changedetection.datetime') as mock_datetime, \
                patch('changedetection.open', return_value=io.BytesIO(b'test'), create=True) as mock_open:
            mock_datetime.now.return_value.isoformat.return_value = '2025-12-19T00:00:00'
            cd._send_event(
                event_type='start',
                image='path/to/image.jpg',
                person_count=2,
                detections=[{'class': 'person', 'conf': 0.9}],
                change_info={'event_type': 'start'}
            )

            mock_open.assert_called_once()
            mock_post.assert_called_once()
            args, kwargs = mock_post.call_args
            self.assertEqual(args[0], 'https://example.com/api/machines/7/events/')
            self.assertEqual(kwargs['headers']['Authorization'], 'Token abc123')
            self.assertEqual(kwargs['data']['event_type'], 'start')
            self.assertEqual(kwargs['data']['captured_at'], '2025-12-19T00:00:00')
            mock_print.assert_any_call('[ChangeDetection] Event sent: start')

    @patch('builtins.print')
    @patch('changedetection.requests.post')
    def test_send_event_error_logs(self, mock_post, mock_print):
        """Test error logging on non-201 response."""
        response = MagicMock()
        response.status_code = 500
        mock_post.return_value = response

        cd = ChangeDetection(self.names, {'host': 'https://example.com', 'machine_id': 7})
        cd.token = 'abc123'

        with patch('changedetection.open', return_value=io.BytesIO(b'test'), create=True):
            cd._send_event(
                event_type='end',
                image='path/to/image.jpg',
                person_count=0,
                detections=[],
                change_info={'event_type': 'end'}
            )
        mock_print.assert_any_call('[ChangeDetection] Send failed: 500')


class TestConfigLoading(unittest.TestCase):
    """Test config loading from env and defaults."""

    def test_config_defaults(self):
        """Default host and machine_id are applied."""
        config = load_gym_config(config_path=None, env={})
        self.assertEqual(config['host'], 'https://mouseku.pythonanywhere.com')
        self.assertEqual(config['machine_id'], 1)
        self.assertEqual(config['target_class'], 'person')

    def test_config_env_overrides(self):
        """Env vars override defaults."""
        env = {
            'GYM_HOST': 'https://example.com',
            'GYM_SECURITY_KEY': 'abc',
            'GYM_MACHINE_ID': '9',
            'GYM_TARGET_CLASS': 'bicycle',
        }
        config = load_gym_config(config_path=None, env=env)
        self.assertEqual(config['host'], 'https://example.com')
        self.assertEqual(config['security_key'], 'abc')
        self.assertEqual(config['machine_id'], 9)
        self.assertEqual(config['target_class'], 'bicycle')

    def test_config_file_loads(self):
        """Config file values are loaded when provided."""
        with tempfile.TemporaryDirectory() as tmpdir:
            path = pathlib.Path(tmpdir) / 'gym_detection.yaml'
            path.write_text(
                "host: https://config.example.com\n"
                "security_key: cfgkey\n"
                "machine_id: 3\n"
                "target_class: car\n"
            )
            config = load_gym_config(config_path=path, env={})
            self.assertEqual(config['host'], 'https://config.example.com')
            self.assertEqual(config['security_key'], 'cfgkey')
            self.assertEqual(config['machine_id'], 3)
            self.assertEqual(config['target_class'], 'car')


if __name__ == '__main__':
    unittest.main()
