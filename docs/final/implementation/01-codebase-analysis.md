# 현재 코드베이스 분석

## 재사용 가능한 컴포넌트

| 컴포넌트 | 현재 상태 | 재사용 방안 |
|----------|----------|-------------|
| Django Token 인증 | 구현됨 | 보안키 → Token 변환 로직 추가 |
| REST API 기본 구조 | 구현됨 | 새 모델용 ViewSet 추가 |
| Android 로그인 | 구현됨 | 보안키 입력 UI로 수정 |
| RecyclerView 패턴 | 구현됨 | 기구/이벤트 목록에 재사용 |
| YOLOv5 Change Detection | 0→1 구현됨 | 1→0 감지 추가 |
| 이미지 업로드 | 구현됨 | 메타데이터 필드 추가 |

## 신규 개발 필요 항목

| 항목 | 설명 |
|------|------|
| GymMachine 모델 | 운동기구 정보 저장 |
| MachineEvent 모델 | 사용 이벤트 로그 |
| ApiUser 모델 | 보안키 기반 인증 |
| 기구 목록 API | GET /api/machines/ |
| 이벤트 API | CRUD + 필터링 |
| Android 기구 목록 화면 | MachineListActivity |
| Android 이벤트 목록 화면 | EventListActivity |
| Edge 양방향 감지 | 0→1, 1→0 모두 감지 |
