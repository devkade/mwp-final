# Edge System 구현 스펙 (yolov5)

## ChangeDetection 클래스 수정

```python
# yolov5/changedetection.py

import os
import cv2
import json
import pathlib
import requests
from datetime import datetime


class ChangeDetection:
    """양방향 Change Detection (0→1 사용시작, 1→0 사용종료)"""

    def __init__(self, names, config=None):
        self.names = names
        self.result_prev = [0 for _ in range(len(names))]

        # 설정
        self.config = config or {}
        self.HOST = self.config.get('host', 'https://mouseku.pythonanywhere.com')
        self.SECURITY_KEY = self.config.get('security_key', '')
        self.MACHINE_ID = self.config.get('machine_id', 1)
        self.TARGET_CLASS = self.config.get('target_class', 'person')

        self.token = None
        self._authenticate()

    def _authenticate(self):
        """보안키로 인증"""
        try:
            res = requests.post(
                f'{self.HOST}/api/auth/login/',
                json={'security_key': self.SECURITY_KEY}
            )
            res.raise_for_status()
            self.token = res.json()['token']
            print(f"[ChangeDetection] Authenticated successfully")
        except Exception as e:
            print(f"[ChangeDetection] Auth failed: {e}")
            raise

    def detect_changes(self, names, detected_current, image, detections_raw):
        """
        변화 감지 및 이벤트 전송

        Args:
            names: 클래스 이름 리스트
            detected_current: 현재 프레임 검출 결과 [0,1,0,1,...]
            image: 현재 프레임 이미지
            detections_raw: YOLO 원본 검출 결과

        Returns:
            dict: 변화 정보 (변화 없으면 None)
        """
        target_idx = names.index(self.TARGET_CLASS) if self.TARGET_CLASS in names else 0

        prev_count = self.result_prev[target_idx]
        curr_count = detected_current[target_idx]

        event_type = None

        # 0 → 1 이상: 사용 시작
        if prev_count == 0 and curr_count >= 1:
            event_type = 'start'
        # 1 이상 → 0: 사용 종료
        elif prev_count >= 1 and curr_count == 0:
            event_type = 'end'

        # 상태 업데이트
        self.result_prev = detected_current[:]

        if event_type:
            change_info = {
                'event_type': event_type,
                'target_class': self.TARGET_CLASS,
                'prev_count': prev_count,
                'curr_count': curr_count,
                'timestamp': datetime.now().isoformat()
            }

            self._send_event(
                event_type=event_type,
                image=image,
                person_count=curr_count,
                detections=detections_raw,
                change_info=change_info
            )

            return change_info

        return None

    def _send_event(self, event_type, image, person_count, detections, change_info):
        """서버로 이벤트 전송"""
        try:
            # 이미지 저장
            now = datetime.now()
            save_dir = pathlib.Path('runs/detect/events')
            save_dir.mkdir(parents=True, exist_ok=True)

            filename = f"{now.strftime('%Y%m%d_%H%M%S')}_{event_type}.jpg"
            filepath = save_dir / filename

            # 리사이즈 및 저장
            resized = cv2.resize(image, (640, 480), interpolation=cv2.INTER_AREA)
            cv2.imwrite(str(filepath), resized)

            # API 호출
            headers = {'Authorization': f'Token {self.token}'}

            data = {
                'machine': self.MACHINE_ID,
                'event_type': event_type,
                'captured_at': now.isoformat(),
                'person_count': person_count,
                'detections': json.dumps(detections),
                'change_info': json.dumps(change_info)
            }

            files = {'image': open(filepath, 'rb')}

            res = requests.post(
                f'{self.HOST}/api/machines/{self.MACHINE_ID}/events/',
                data=data,
                files=files,
                headers=headers
            )

            if res.status_code in [200, 201]:
                print(f"[ChangeDetection] Event sent: {event_type}")
            else:
                print(f"[ChangeDetection] Send failed: {res.status_code}")

        except Exception as e:
            print(f"[ChangeDetection] Error: {e}")
```

## detect.py 수정 사항

```python
# yolov5/detect.py (수정 부분)

# ChangeDetection 초기화 (run 함수 내)
change_detector = ChangeDetection(
    names=model.names,
    config={
        'host': 'https://mouseku.pythonanywhere.com',
        'security_key': os.environ.get('GYM_SECURITY_KEY', 'your-key'),
        'machine_id': int(os.environ.get('GYM_MACHINE_ID', 1)),
        'target_class': 'person'
    }
)

# 프레임 처리 루프 내
for path, im, im0s, vid_cap, s in dataset:
    # ... YOLO 추론 ...

    # 검출 결과를 배열로 변환
    detected = [0] * len(names)
    for *xyxy, conf, cls in reversed(det):
        c = int(cls)
        detected[c] += 1

    # Change Detection 수행
    change_info = change_detector.detect_changes(
        names=names,
        detected_current=detected,
        image=im0,
        detections_raw=[...]  # 검출 결과 리스트
    )

    if change_info:
        print(f"Change detected: {change_info['event_type']}")
```

## 설정 파일

```yaml
# yolov5/config/gym_detection.yaml

# 서버 설정
server:
  host: https://mouseku.pythonanywhere.com
  security_key: ${GYM_SECURITY_KEY}  # 환경변수에서 로드

# 기구 설정
machine:
  id: 1
  name: "런닝머신 #1"

# 감지 설정
detection:
  target_class: person
  confidence_threshold: 0.5

# 이미지 설정
image:
  width: 640
  height: 480
  quality: 85
```
