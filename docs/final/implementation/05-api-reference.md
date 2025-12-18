# API 엔드포인트 레퍼런스

## 인증 API

| 메서드 | 엔드포인트 | 설명 | 요청 | 응답 |
|--------|------------|------|------|------|
| POST | `/api/auth/login/` | 보안키 로그인 | `{"security_key": "..."}` | `{"token": "...", "name": "..."}` |

### 요청 예시

```bash
curl -X POST https://mouseku.pythonanywhere.com/api/auth/login/ \
  -H "Content-Type: application/json" \
  -d '{"security_key": "your-security-key"}'
```

### 응답 예시

```json
{
  "token": "abc123def456...",
  "name": "헬스장 관리자"
}
```

---

## 운동기구 API

| 메서드 | 엔드포인트 | 설명 | 파라미터 |
|--------|------------|------|----------|
| GET | `/api_root/machines/` | 기구 목록 | - |
| GET | `/api_root/machines/{id}/` | 기구 상세 | - |
| GET | `/api_root/machines/{id}/stats/` | 기구 통계 | `date_from`, `date_to` |

### 기구 목록 응답 예시

```json
[
  {
    "id": 1,
    "name": "런닝머신 #1",
    "machine_type": "treadmill",
    "location": "1층 A구역",
    "description": "ProForm 상업용 런닝머신",
    "thumbnail": "/media/machines/treadmill1.jpg",
    "is_active": true,
    "event_count": 152,
    "last_event": {
      "event_type": "end",
      "captured_at": "2023-10-27T15:21:03"
    }
  }
]
```

### 기구 통계 응답 예시

```json
{
  "machine_id": 1,
  "machine_name": "런닝머신 #1",
  "total_starts": 152,
  "total_ends": 150,
  "daily_usage": [
    {"date": "2023-10-25", "count": 12},
    {"date": "2023-10-26", "count": 18},
    {"date": "2023-10-27", "count": 15}
  ]
}
```

---

## 이벤트 API

| 메서드 | 엔드포인트 | 설명 | 파라미터 |
|--------|------------|------|----------|
| GET | `/api/machines/{id}/events/` | 기구별 이벤트 | `event_type`, `date_from`, `date_to` |
| POST | `/api/machines/{id}/events/` | 이벤트 생성 (Edge) | multipart/form-data |
| GET | `/api_root/events/{id}/` | 이벤트 상세 | - |

### 이벤트 목록 요청 파라미터

| 파라미터 | 타입 | 설명 | 예시 |
|----------|------|------|------|
| `event_type` | string | 이벤트 타입 필터 | `start`, `end` |
| `date_from` | date | 시작 날짜 | `2023-10-01` |
| `date_to` | date | 종료 날짜 | `2023-10-31` |

### 이벤트 목록 응답 예시

```json
[
  {
    "id": 1,
    "machine": 1,
    "machine_name": "런닝머신 #1",
    "event_type": "start",
    "event_type_display": "사용 시작",
    "image": "/media/events/2023/10/27/event_001.jpg",
    "captured_at": "2023-10-27T14:35:10",
    "person_count": 1
  }
]
```

### 이벤트 상세 응답 예시

```json
{
  "id": 1,
  "machine": 1,
  "machine_name": "런닝머신 #1",
  "event_type": "start",
  "event_type_display": "사용 시작",
  "image": "/media/events/2023/10/27/event_001.jpg",
  "captured_at": "2023-10-27T14:35:10",
  "created_at": "2023-10-27T14:35:12",
  "person_count": 1,
  "detections": {
    "person": [
      {"bbox": [100, 50, 300, 400], "confidence": 0.95}
    ]
  },
  "change_info": {
    "event_type": "start",
    "target_class": "person",
    "prev_count": 0,
    "curr_count": 1,
    "timestamp": "2023-10-27T14:35:10"
  }
}
```

### 이벤트 생성 (Edge → Server)

```bash
curl -X POST https://mouseku.pythonanywhere.com/api/machines/1/events/ \
  -H "Authorization: Token abc123..." \
  -F "image=@/path/to/image.jpg" \
  -F "event_type=start" \
  -F "captured_at=2023-10-27T14:35:10" \
  -F "person_count=1" \
  -F 'detections={"person": [{"bbox": [100, 50, 300, 400], "confidence": 0.95}]}' \
  -F 'change_info={"event_type": "start", "prev_count": 0, "curr_count": 1}'
```

---

## 기존 호환 API

기존 Photo Blog API는 호환성을 위해 유지됩니다.

| 메서드 | 엔드포인트 | 설명 |
|--------|------------|------|
| GET | `/api_root/Post/` | 기존 블로그 포스트 목록 |
| POST | `/api_root/Post/` | 기존 블로그 포스트 생성 |
| POST | `/api-token-auth/` | 기존 토큰 인증 (username/password) |
