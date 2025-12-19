# 테스트 시나리오 02: 운동기구 목록 조회

## 목적
운동기구 API 및 Android 앱의 목록 조회 기능 확인

## 사전 조건
- [ ] Django 서버 실행 중
- [ ] 인증 토큰 발급 완료
- [ ] 운동기구 데이터 등록 (최소 1개 이상)

## 테스트 단계

### 1. 운동기구 목록 API 테스트

```bash
# 운동기구 목록 조회
curl -H "Authorization: Token <your-token>" \
  http://127.0.0.1:8000/api_root/machines/
```

**예상 응답 (페이지네이션):**
```json
{
  "count": 2,
  "next": null,
  "previous": null,
  "results": [
    {
      "id": 1,
      "name": "런닝머신 #1",
      "machine_type": "treadmill",
      "location": "Zone A",
      "is_active": true,
      "event_count": 5,
      "last_event": "2025-12-19T10:30:00Z"
    }
  ]
}
```

| 항목 | 예상 | 실제 | 통과 |
|------|------|------|------|
| HTTP 상태 코드 | 200 | | [ ] |
| 페이지네이션 구조 | count, results 필드 존재 | | [ ] |
| 운동기구 데이터 | name, machine_type 등 필드 포함 | | [ ] |

### 2. 개별 운동기구 상세 조회

```bash
curl -H "Authorization: Token <your-token>" \
  http://127.0.0.1:8000/api_root/machines/1/
```

| 항목 | 예상 | 실제 | 통과 |
|------|------|------|------|
| HTTP 상태 코드 | 200 | | [ ] |
| 상세 정보 반환 | 모든 필드 포함 | | [ ] |

### 3. 인증 없이 접근 테스트

```bash
curl http://127.0.0.1:8000/api_root/machines/
```

| 항목 | 예상 | 실제 | 통과 |
|------|------|------|------|
| HTTP 상태 코드 | 401 Unauthorized | | [ ] |

### 4. Android 앱 운동기구 목록 화면

1. 로그인 완료 후 메인 화면 진입
2. 운동기구 목록 화면으로 이동

| 항목 | 예상 | 실제 | 통과 |
|------|------|------|------|
| 목록 로딩 | 로딩 인디케이터 표시 | | [ ] |
| 데이터 표시 | RecyclerView에 운동기구 표시 | | [ ] |
| 기구 정보 | 이름, 타입, 위치 표시 | | [ ] |
| 항목 클릭 | 이벤트 목록 화면으로 이동 | | [ ] |

### 5. 비활성 운동기구 필터링

```bash
# 비활성 기구는 목록에서 제외되어야 함
curl -H "Authorization: Token <your-token>" \
  http://127.0.0.1:8000/api_root/machines/
```

| 항목 | 예상 | 실제 | 통과 |
|------|------|------|------|
| is_active=false 기구 | 목록에서 제외 | | [ ] |

---

## 테스트 결과

- **테스트 일시**:
- **테스트 환경**:
- **전체 통과 여부**: [ ] 통과 / [ ] 실패

### 비고

