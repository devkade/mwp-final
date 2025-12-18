# 헬스장 운동기구 감지 시스템 - 구현 스펙

> **Note**: 이 문서는 기능별로 분리되었습니다. 아래 링크를 참조하세요.

---

## 분리된 문서

상세 구현 스펙은 [implementation/](implementation/) 폴더에서 확인할 수 있습니다.

| 문서 | 설명 |
|------|------|
| [implementation/README.md](implementation/README.md) | 전체 구조 및 인덱스 |
| [implementation/01-codebase-analysis.md](implementation/01-codebase-analysis.md) | 현재 코드베이스 분석 |
| [implementation/02-backend.md](implementation/02-backend.md) | Django 백엔드 구현 스펙 |
| [implementation/03-edge-system.md](implementation/03-edge-system.md) | YOLOv5 Edge 시스템 구현 스펙 |
| [implementation/04-android.md](implementation/04-android.md) | Android 앱 구현 스펙 |
| [implementation/05-api-reference.md](implementation/05-api-reference.md) | API 엔드포인트 레퍼런스 |
| [implementation/06-implementation-plan.md](implementation/06-implementation-plan.md) | 구현 순서 및 호환성 |

---

## UI 디자인

UI 스크린 디자인은 [ui/](ui/) 폴더에서 확인할 수 있습니다.

| 화면 | 스크린샷 | HTML |
|------|----------|------|
| 로그인 | [ui/login.png](ui/login.png) | [ui/login.html](ui/login.html) |
| 운동기구 목록 | [ui/equipment_list.png](ui/equipment_list.png) | [ui/equipment_list.html](ui/equipment_list.html) |
| 사용 기록 | [ui/usage_history.png](ui/usage_history.png) | [ui/usage_history.html](ui/usage_history.html) |
| 이벤트 상세 | [ui/event_details.png](ui/event_details.png) | [ui/event_details.html](ui/event_details.html) |
| 사용 통계 | [ui/usage_statistics.png](ui/usage_statistics.png) | [ui/usage_statistics.html](ui/usage_statistics.html) |

---

**작성일**: 2025-12-18
**기준 문서**: docs/final/gym-service-overview.md
