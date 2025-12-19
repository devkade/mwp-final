"""
Bidirectional Change Detection for Gym Equipment Usage Monitoring

Detects usage start (0→1+) and usage end (1+→0) events for a target class.
Requires state to be stable for DEBOUNCE_SECONDS before triggering events.
"""

import cv2
import json
import os
import pathlib
import time
from datetime import datetime

import requests
import yaml

# Debounce time in seconds - state must be stable for this duration
DEBOUNCE_SECONDS = 5.0


def load_gym_config(config_path=None, env=None):
    """
    Load configuration from YAML file and environment variables.

    Args:
        config_path: Optional path to YAML config.
        env: Optional dict of environment variables for testing.
    """
    env = env if env is not None else os.environ
    config = {}

    if config_path:
        config_path = pathlib.Path(config_path)
        if config_path.exists():
            with config_path.open('r', encoding='utf-8') as handle:
                config = yaml.safe_load(handle) or {}

    def _env_or_config(key, env_key, default=None, cast=None):
        if env_key in env and env[env_key] != "":
            value = env[env_key]
        else:
            value = config.get(key, default)
        if cast and value is not None:
            return cast(value)
        return value

    return {
        'host': _env_or_config('host', 'GYM_HOST', 'https://mouseku.pythonanywhere.com'),
        'security_key': _env_or_config('security_key', 'GYM_SECURITY_KEY'),
        'machine_id': _env_or_config('machine_id', 'GYM_MACHINE_ID', 1, int),
        'target_class': _env_or_config('target_class', 'GYM_TARGET_CLASS', 'person'),
    }


class ChangeDetection:
    """양방향 Change Detection (0→1 사용시작, 1→0 사용종료)"""

    def __init__(self, names, config=None):
        """
        Initialize ChangeDetection.

        Args:
            names: List of class names from YOLO model
            config: Optional configuration dict with:
                - target_class: Class to monitor (default: 'person')
        """
        self.names = names
        self.result_prev = [0 for _ in range(len(names))]
        self.config = config or {}
        self.TARGET_CLASS = self.config.get('target_class', 'person')
        self.HOST = self.config.get('host', 'https://mouseku.pythonanywhere.com')
        self.SECURITY_KEY = self.config.get('security_key')
        self.MACHINE_ID = self.config.get('machine_id', 1)
        self.token = None

        # Debounce state tracking
        self.confirmed_state = 'idle'  # 'idle' or 'active'
        self.pending_state = None      # 'start' or 'end' or None
        self.pending_since = None      # Timestamp when pending state started
        self.pending_image = None      # Image captured when state change first detected
        self.pending_detections = None # Detections when state change first detected

        if self.SECURITY_KEY:
            self._authenticate()

    def detect_changes(self, names, detected_current, image, detections_raw):
        """
        Detect changes in target class count with debouncing.

        Only triggers events after state is stable for DEBOUNCE_SECONDS:
        - 0 → 1+ for 5s: "start" event (usage started)
        - 1+ → 0 for 5s: "end" event (usage ended)

        Args:
            names: Class name list
            detected_current: Current frame detection counts [0,1,0,1,...]
            image: Current frame image (numpy array)
            detections_raw: Raw YOLO detection results

        Returns:
            dict: change_info if event triggered, None otherwise
        """
        # Get target class index (names can be dict or list)
        try:
            if isinstance(names, dict):
                # names is dict: {0: 'person', 1: 'bicycle', ...}
                target_idx = next(k for k, v in names.items() if v == self.TARGET_CLASS)
            else:
                # names is list: ['person', 'bicycle', ...]
                target_idx = names.index(self.TARGET_CLASS)
        except (ValueError, StopIteration):
            target_idx = 0  # Default to first class if target not found

        curr_count = detected_current[target_idx]
        current_time = time.time()

        # Determine current instantaneous state
        current_instant_state = 'active' if curr_count >= 1 else 'idle'

        # Update previous state for legacy compatibility
        self.result_prev = detected_current[:]

        # Determine what event would be triggered if state changes
        if current_instant_state == 'active' and self.confirmed_state == 'idle':
            potential_event = 'start'
        elif current_instant_state == 'idle' and self.confirmed_state == 'active':
            potential_event = 'end'
        else:
            potential_event = None

        # State machine logic with debouncing
        if potential_event:
            # State change detected
            if self.pending_state == potential_event:
                # Same pending state - check if debounce time passed
                elapsed = current_time - self.pending_since
                if elapsed >= DEBOUNCE_SECONDS:
                    # Debounce complete - trigger event
                    event_type = potential_event
                    self.confirmed_state = current_instant_state

                    change_info = {
                        'event_type': event_type,
                        'target_class': self.TARGET_CLASS,
                        'prev_count': 0 if event_type == 'start' else 1,
                        'curr_count': curr_count,
                        'timestamp': datetime.now().isoformat(),
                        'debounce_seconds': DEBOUNCE_SECONDS
                    }

                    # Save image (use pending image captured at state change)
                    image_to_save = self.pending_image if self.pending_image is not None else image
                    image_path = self._save_event_image(image_to_save, event_type)
                    change_info['image_path'] = str(image_path)

                    detections = self._serialize_detections(
                        self.pending_detections if self.pending_detections is not None else detections_raw,
                        names
                    )
                    person_count = curr_count if event_type == 'start' else 0
                    self._send_event(event_type, image_path, person_count, detections, change_info)

                    # Reset pending state
                    self.pending_state = None
                    self.pending_since = None
                    self.pending_image = None
                    self.pending_detections = None

                    print(f"[ChangeDetection] Event triggered after {DEBOUNCE_SECONDS}s: {event_type}")
                    return change_info
                else:
                    # Still waiting for debounce
                    remaining = DEBOUNCE_SECONDS - elapsed
                    print(f"[ChangeDetection] Pending {potential_event}: {remaining:.1f}s remaining")
            else:
                # New pending state - start debounce timer
                self.pending_state = potential_event
                self.pending_since = current_time
                self.pending_image = image.copy() if image is not None else None
                self.pending_detections = detections_raw
                print(f"[ChangeDetection] State change detected, waiting {DEBOUNCE_SECONDS}s: {potential_event}")
        else:
            # No state change or returned to confirmed state
            if self.pending_state is not None:
                print(f"[ChangeDetection] Pending {self.pending_state} cancelled - state reverted")
                self.pending_state = None
                self.pending_since = None
                self.pending_image = None
                self.pending_detections = None

        return None

    def _authenticate(self):
        """Authenticate with the server using the security key."""
        try:
            res = requests.post(
                f"{self.HOST}/api/auth/login/",
                json={'security_key': self.SECURITY_KEY}
            )
            res.raise_for_status()
            data = res.json()
            self.token = data.get('token')
            print("[ChangeDetection] Authenticated successfully")
        except Exception as exc:
            print(f"[ChangeDetection] Auth failed: {exc}")
            raise

    def _send_event(self, event_type, image, person_count, detections, change_info):
        """Send event data to the server."""
        headers = {'Authorization': f"Token {self.token}"} if self.token else {}
        data = {
            'event_type': event_type,
            'captured_at': datetime.now().isoformat(),
            'person_count': person_count,
            'detections': json.dumps(detections),
            'change_info': json.dumps(change_info)
        }

        image_path = pathlib.Path(image) if not isinstance(image, pathlib.Path) else image
        try:
            with open(image_path, 'rb') as handle:
                files = {'image': handle}
                res = requests.post(
                    f"{self.HOST}/api/machines/{self.MACHINE_ID}/events/",
                    data=data,
                    files=files,
                    headers=headers,
                    timeout=10
                )

            if res.status_code in (200, 201):
                print(f"[ChangeDetection] Event sent: {event_type}")
            else:
                print(f"[ChangeDetection] Send failed: {res.status_code}")
        except requests.RequestException as exc:
            print(f"[ChangeDetection] Send failed: {exc}")

    def _serialize_detections(self, detections_raw, names):
        """Serialize raw detections into JSON-friendly list."""
        if detections_raw is None:
            return []
        serialized = []
        for row in detections_raw:
            try:
                x1, y1, x2, y2, conf, cls_id = row[:6]
                cls_index = int(cls_id)
                serialized.append({
                    'class': names[cls_index] if cls_index < len(names) else str(cls_index),
                    'confidence': float(conf),
                    'bbox': [float(x1), float(y1), float(x2), float(y2)]
                })
            except Exception:
                continue
        return serialized

    def _save_event_image(self, image, event_type):
        """
        Save event image to local directory.

        Args:
            image: Frame image (numpy array)
            event_type: 'start' or 'end'

        Returns:
            pathlib.Path: Path to saved image
        """
        # Create events directory
        save_dir = pathlib.Path('runs/detect/events')
        save_dir.mkdir(parents=True, exist_ok=True)

        # Generate filename with timestamp
        now = datetime.now()
        filename = f"{now.strftime('%Y%m%d_%H%M%S')}_{event_type}.jpg"
        filepath = save_dir / filename

        # Resize to 640x480 and save
        resized = cv2.resize(image, (640, 480), interpolation=cv2.INTER_AREA)
        cv2.imwrite(str(filepath), resized)

        return filepath
