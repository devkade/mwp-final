# 헬스장 운동기구 감지 시스템 - 구현 스펙

**문서 목적**: gym-service-overview.md 설계를 현재 코드베이스 기반으로 구현하기 위한 상세 스펙

**기준 코드베이스**: 중간고사 Photo Blog 프로젝트 (PhotoBlogServer, PhotoViewer, yolov5)

---

## 문서 구조

| 파일 | 설명 |
|------|------|
| [01-codebase-analysis.md](01-codebase-analysis.md) | 현재 코드베이스 분석 및 재사용 컴포넌트 |
| [02-backend.md](02-backend.md) | Django 백엔드 구현 스펙 (모델, 시리얼라이저, 뷰) |
| [03-edge-system.md](03-edge-system.md) | YOLOv5 Edge 시스템 구현 스펙 |
| [04-android.md](04-android.md) | Android 앱 구현 스펙 (Activity, 레이아웃) |
| [05-api-reference.md](05-api-reference.md) | API 엔드포인트 레퍼런스 |
| [06-implementation-plan.md](06-implementation-plan.md) | 구현 순서 및 호환성 |

---

## 시스템 구성

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Edge System   │────▶│  Service System │◀────│  Client System  │
│    (yolov5)     │     │    (Django)     │     │    (Android)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       │
   YOLOv5 검출            REST API              RecyclerView
   Change Detection       Token 인증           이미지 목록/상세
   이벤트 전송            이미지 저장          필터/통계
```

---

## 관련 문서

- **설계 문서**: [../gym-service-overview.md](../gym-service-overview.md)
- **UI 디자인**: [../ui/README.md](../ui/README.md)

---

**작성일**: 2025-12-18
