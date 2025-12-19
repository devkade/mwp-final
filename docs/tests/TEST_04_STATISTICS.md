# 테스트 시나리오 04: 통계 조회

## 목적
운동기구별 사용 통계 API 및 Android 앱 통계 화면 기능 확인

## 사전 조건
- [ ] Django 서버 실행 중
- [ ] 인증 토큰 발급 완료
- [ ] 운동기구 데이터 등록 완료
- [ ] 이벤트 데이터 존재 (통계 계산용)

## 테스트 단계

### 1. 운동기구 통계 API 테스트 (기간: week)

```bash
curl -H "Authorization: Token <your-token>" \
  "http://127.0.0.1:8000/api_root/machines/1/stats/?period=week"
```

**예상 응답:**
```json
{
  "machine_id": 1,
  "machine_name": "런닝머신 #1",
  "period": "week",
  "total_events": 15,
  "daily_stats": [
    {"date": "2025-12-13", "count": 2},
    {"date": "2025-12-14", "count": 5},
    ...
  ]
}
```

| 항목 | 예상 | 실제 | 통과 |
|------|------|------|------|
| HTTP 상태 코드 | 200 | | [ ] |
| period 값 | "week" | | [ ] |
| daily_stats 배열 | 7일간 데이터 | | [ ] |
| total_events | 합계 정확 | | [ ] |

### 2. 다른 기간 테스트 (month)

```bash
curl -H "Authorization: Token <your-token>" \
  "http://127.0.0.1:8000/api_root/machines/1/stats/?period=month"
```

| 항목 | 예상 | 실제 | 통과 |
|------|------|------|------|
| HTTP 상태 코드 | 200 | | [ ] |
| period 값 | "month" | | [ ] |
| daily_stats 배열 | 30일간 데이터 | | [ ] |

### 3. 이벤트 없는 운동기구 통계

```bash
# 이벤트가 없는 운동기구 ID로 테스트
curl -H "Authorization: Token <your-token>" \
  "http://127.0.0.1:8000/api_root/machines/999/stats/?period=week"
```

| 항목 | 예상 | 실제 | 통과 |
|------|------|------|------|
| 존재하지 않는 ID | 404 Not Found | | [ ] |
| 이벤트 0개인 기구 | total_events: 0 | | [ ] |

### 4. Android 앱 통계 화면

1. 메인 화면에서 통계 메뉴 진입
2. 운동기구별 통계 확인

| 항목 | 예상 | 실제 | 통과 |
|------|------|------|------|
| 통계 화면 진입 | 정상 로딩 | | [ ] |
| 바 차트 표시 | 일별 데이터 시각화 | | [ ] |
| 기간 선택 | week/month 전환 가능 | | [ ] |
| 총 이벤트 수 | 정확한 합계 표시 | | [ ] |

### 5. 차트 데이터 정확성

1. API 응답의 daily_stats와 차트 비교
2. 각 막대의 높이가 count 값과 일치하는지 확인

| 항목 | 예상 | 실제 | 통과 |
|------|------|------|------|
| 날짜별 데이터 일치 | API = 차트 | | [ ] |
| X축 날짜 표시 | 정확한 날짜 | | [ ] |
| Y축 스케일 | 적절한 범위 | | [ ] |

---

## 테스트 결과

- **테스트 일시**:
- **테스트 환경**:
- **전체 통과 여부**: [ ] 통과 / [ ] 실패

### 비고

