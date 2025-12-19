"""
Bidirectional Change Detection for Gym Equipment Usage Monitoring

Detects usage start (0→1+) and usage end (1+→0) events for a target class.
"""

import cv2
import pathlib
from datetime import datetime


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

    def detect_changes(self, names, detected_current, image, detections_raw):
        """
        Detect changes in target class count.

        Only triggers events on boundary transitions:
        - 0 → 1+: "start" event (usage started)
        - 1+ → 0: "end" event (usage ended)

        Args:
            names: Class name list
            detected_current: Current frame detection counts [0,1,0,1,...]
            image: Current frame image (numpy array)
            detections_raw: Raw YOLO detection results

        Returns:
            dict: change_info if event triggered, None otherwise
        """
        # Get target class index
        try:
            target_idx = names.index(self.TARGET_CLASS)
        except ValueError:
            target_idx = 0  # Default to first class if target not found

        prev_count = self.result_prev[target_idx]
        curr_count = detected_current[target_idx]

        event_type = None

        # 0 → 1+: Usage start
        if prev_count == 0 and curr_count >= 1:
            event_type = 'start'
        # 1+ → 0: Usage end
        elif prev_count >= 1 and curr_count == 0:
            event_type = 'end'

        # Update previous state
        self.result_prev = detected_current[:]

        if event_type:
            change_info = {
                'event_type': event_type,
                'target_class': self.TARGET_CLASS,
                'prev_count': prev_count,
                'curr_count': curr_count,
                'timestamp': datetime.now().isoformat()
            }

            # Save image locally
            self._save_event_image(image, event_type)

            return change_info

        return None

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
