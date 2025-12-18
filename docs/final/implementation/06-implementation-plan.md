# 구현 순서 및 호환성

## 구현 Phase

### Phase 1: 백엔드 기반 구축

1. [ ] 모델 생성 (ApiUser, GymMachine, MachineEvent)
2. [ ] 마이그레이션 실행
3. [ ] Serializers 작성
4. [ ] ViewSets 작성
5. [ ] URL 설정
6. [ ] Admin 등록
7. [ ] 테스트 데이터 생성

### Phase 2: Edge 수정

1. [ ] ChangeDetection 양방향 감지 구현
2. [ ] 새 API 연동
3. [ ] 설정 파일 분리
4. [ ] 테스트

### Phase 3: Android 앱

1. [ ] 로그인 화면 수정 (보안키)
2. [ ] MachineListActivity 구현
3. [ ] EventListActivity 구현
4. [ ] EventDetailActivity 구현
5. [ ] 필터 기능 구현
6. [ ] UI 다듬기

### Phase 4: 테스트 및 배포

1. [ ] 통합 테스트
2. [ ] PythonAnywhere 배포
3. [ ] 실제 환경 테스트

---

## 기존 코드 호환성

### 유지되는 기존 API

| 엔드포인트 | 설명 |
|------------|------|
| `GET/POST /api_root/Post/` | 기존 블로그 API 유지 |
| `POST /api-token-auth/` | 기존 토큰 인증 유지 |

### 신규 추가 API

| 엔드포인트 | 설명 |
|------------|------|
| `POST /api/auth/login/` | 보안키 로그인 |
| `/api_root/machines/` | 운동기구 CRUD |
| `/api_root/events/` | 이벤트 CRUD |
| `/api/machines/{id}/events/` | 기구별 이벤트 |

---

## 작성 정보

- **작성일**: 2025-12-18
- **기준 문서**: docs/final/gym-service-overview.md
