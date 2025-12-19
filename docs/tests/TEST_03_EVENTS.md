# 테스트 시나리오 03: 이벤트 생성 및 조회

## 목적
Edge 시스템에서 이벤트 생성 → Backend 저장 → Android 앱 조회 전체 흐름 확인

## 사전 조건
- [ ] Django 서버 실행 중
- [ ] 인증 토큰 발급 완료
- [ ] 운동기구 데이터 등록 완료
- [ ] YOLOv5 환경 설정 완료 (이벤트 생성 테스트용)

## 테스트 단계

### 1. 이벤트 생성 API 테스트

```bash
# 테스트용 이미지 생성 (없는 경우)
# macOS/Linux에서 간단한 테스트 이미지 생성
convert -size 320x240 xc:blue /tmp/test_event.jpg 2>/dev/null || \
  curl -o /tmp/test_event.jpg https://via.placeholder.com/320x240.jpg

# 이벤트 생성 (POST) - multipart/form-data 형식
curl -X POST http://127.0.0.1:8000/api_root/events/ \
  -H "Authorization: Token <your-token>" \
  -F "machine=1" \
  -F "event_type=start" \
  -F "captured_at=2025-12-19T10:30:00Z" \
  -F "image=@/tmp/test_event.jpg"
```

**필수 필드 설명:**
- `machine`: 운동기구 ID (GymMachine의 pk)
- `event_type`: `start` (사용 시작) 또는 `end` (사용 종료)
- `captured_at`: ISO 8601 형식 시간 (예: `2025-12-19T10:30:00Z`)
- `image`: 이미지 파일 (multipart/form-data로 전송)

| 항목 | 예상 | 실제 | 통과 |
|------|------|------|------|
| HTTP 상태 코드 | 201 Created | | [ ] |
| 이벤트 ID 반환 | id 필드 존재 | | [ ] |
| captured_at 저장 | 요청한 시간 기록 | | [ ] |
| image URL 반환 | image 필드에 URL | | [ ] |

### 2. 이벤트 목록 조회 API

```bash
curl -H "Authorization: Token <your-token>" \
  "http://127.0.0.1:8000/api_root/events/"
```

**예상 응답:**
```json
{
  "count": 10,
  "next": "http://127.0.0.1:8000/api_root/events/?page=2",
  "previous": null,
  "results": [
    {
      "id": 1,
      "machine": 1,
      "event_type": "start",
      "detected_object": "person",
      "timestamp": "2025-12-19T10:30:00Z"
    }
  ]
}
```

| 항목 | 예상 | 실제 | 통과 |
|------|------|------|------|
| HTTP 상태 코드 | 200 | | [ ] |
| 페이지네이션 | count, next, previous, results | | [ ] |
| 최신순 정렬 | timestamp 내림차순 | | [ ] |

### 3. 특정 운동기구 이벤트 필터링

```bash
curl -H "Authorization: Token <your-token>" \
  "http://127.0.0.1:8000/api_root/events/?machine=1"
```

| 항목 | 예상 | 실제 | 통과 |
|------|------|------|------|
| 필터링 동작 | machine=1 이벤트만 반환 | | [ ] |

### 4. 이벤트 상세 조회

```bash
curl -H "Authorization: Token <your-token>" \
  http://127.0.0.1:8000/api_root/events/1/
```

| 항목 | 예상 | 실제 | 통과 |
|------|------|------|------|
| HTTP 상태 코드 | 200 | | [ ] |
| 상세 정보 | 모든 필드 포함 | | [ ] |

### 5. Edge → Backend 이벤트 업로드 (YOLOv5)

```bash
cd yolov5
source venv/bin/activate
python detect.py --weights yolov5s.pt --source 0
```

1. 웹캠 앞에 사람 등장
2. 객체 감지 및 이벤트 생성 확인

| 항목 | 예상 | 실제 | 통과 |
|------|------|------|------|
| 사람 감지 | YOLOv5 콘솔에 감지 로그 | | [ ] |
| 이벤트 생성 | API 호출 성공 로그 | | [ ] |
| 이미지 저장 | runs/detect/detected/ 폴더에 저장 | | [ ] |

### 6. Android 앱 이벤트 목록 화면

1. 운동기구 선택
2. 이벤트 목록 화면 진입

| 항목 | 예상 | 실제 | 통과 |
|------|------|------|------|
| 이벤트 목록 로딩 | 정상 표시 | | [ ] |
| 이벤트 정보 표시 | 타입, 시간 등 표시 | | [ ] |
| 스크롤/페이징 | 추가 데이터 로드 | | [ ] |

### 7. Android 앱 이벤트 상세 화면

1. 이벤트 목록에서 항목 선택
2. 상세 화면 확인

| 항목 | 예상 | 실제 | 통과 |
|------|------|------|------|
| 상세 정보 표시 | 전체 이벤트 정보 | | [ ] |
| 이미지 표시 | 감지된 이미지 (있는 경우) | | [ ] |

---

## 테스트 결과

- **테스트 일시**:
- **테스트 환경**:
- **전체 통과 여부**: [ ] 통과 / [ ] 실패

### 비고

