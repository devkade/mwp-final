# 중간고사 프로젝트 현황 정리

**프로젝트**: Photo Blog Mobile/Web Service
**기간**: 2025-11-02 ~ 2025-11-11
**상태**: 전체 기능 구현 완료

---

## 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────────┐
│                        시스템 구성도                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐         ┌──────────────┐        ┌──────────┐ │
│  │  PhotoViewer │  HTTP   │ PhotoBlog    │        │  YOLOv5  │ │
│  │  (Android)   │ ◀─────▶ │ Server       │ ◀───── │ Detection│ │
│  │              │ REST    │ (Django)     │ POST   │          │ │
│  └──────────────┘  API    └──────────────┘        └──────────┘ │
│        │                         │                             │
│        │                         │                             │
│        ▼                         ▼                             │
│  ┌──────────────┐         ┌──────────────┐                     │
│  │ RecyclerView │         │   SQLite     │                     │
│  │ (이미지 표시) │         │  (db.sqlite3)│                     │
│  └──────────────┘         └──────────────┘                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 구현 현황

### 완료된 기능 (✅)

| 기능 | 설명 | 관련 파일 |
|------|------|-----------|
| Django 백엔드 | DRF 기반 REST API 서버 | `PhotoBlogServer/blog/views.py` |
| Token 인증 | REST Framework Token Authentication | `PhotoBlogServer/mysite/settings.py` |
| 이미지 다운로드 | API에서 포스트 목록 조회 및 이미지 다운로드 | `MainActivity.java:217-365` |
| RecyclerView 표시 | 다운로드한 이미지를 그리드/리스트로 표시 | `ImageAdapter.java` |
| 이미지 업로드 | 갤러리에서 이미지 선택 후 서버에 업로드 | `MainActivity.java:608-758` |
| 제목/내용 입력 | 업로드 시 AlertDialog로 제목/내용 입력 | `MainActivity.java:580-606`, `dialog_upload.xml` |
| Pull to Refresh | SwipeRefreshLayout으로 당겨서 새로고침 | `MainActivity.java:127-130` |
| 상세보기 | 이미지 클릭 시 포스트 상세 정보 표시 | `MainActivity.java:367-421`, `dialog_post_detail.xml` |
| 포스트 수정 | 제목/내용/이미지 편집 후 PUT 요청 | `MainActivity.java:440-527, 778-880`, `dialog_edit_post.xml` |
| ProgressBar 로딩 표시 | 다운로드/업로드/수정 시 로딩 인디케이터 표시 | `MainActivity.java:68, 219, 609, 785` |
| 포스트 삭제 | DELETE 요청으로 포스트 삭제 | `MainActivity.java:882-925` |
| 로그인 화면 | SplashActivity → LoginActivity 흐름 | `SplashActivity.java`, `LoginActivity.java` |
| 보안 토큰 저장 | EncryptedSharedPreferences 사용 | `SecureTokenManager.java` |
| 세션 관리 | 토큰 기반 세션 유효성 검사 | `SessionManager.java`, `AuthenticationService.java` |
| 로그아웃 | 세션 무효화 및 로그인 화면 이동 | `MainActivity.java:192-199` |
| 실시간 알림 | Foreground polling (30초) + 알림 표시 | `MainActivity.java:85-93, 932-995`, `NotificationHelper.java` |
| 백그라운드 동기화 | WorkManager 기반 주기적 동기화 | `BackgroundSyncWorker.java`, `SyncPreferences.java` |

### 미구현 기능 (선택사항)

| 기능 | 우선순위 | 비고 |
|------|----------|------|
| 검색 기능 | 낮음 | 클라이언트/서버 측 검색 |
| 정렬 기능 | 낮음 | 최신순/오래된순/제목순 |
| 오프라인 모드 (Room DB) | 낮음 | 로컬 캐시 저장 |
| 이미지 캐싱 (Glide) | 중간 | 메모리 최적화 |

---

## 기술 스택

### 백엔드 (PhotoBlogServer)

| 기술 | 버전 | 용도 |
|------|------|------|
| Django | 3.2+ | 웹 프레임워크 |
| Django REST Framework | 3.14+ | REST API |
| Pillow | 10.1+ | 이미지 처리 |
| SQLite | - | 데이터베이스 |

### 프론트엔드 (PhotoViewer)

| 기술 | 버전 | 용도 |
|------|------|------|
| Android SDK | API 24-36 | 모바일 앱 |
| Java | 11+ | 프로그래밍 언어 |
| RecyclerView | AndroidX | 리스트 표시 |
| SwipeRefreshLayout | AndroidX | Pull to Refresh |
| HttpURLConnection | - | HTTP 통신 |

### 객체 감지 (YOLOv5)

| 기술 | 용도 |
|------|------|
| YOLOv5 | 객체 감지 모델 |
| PyTorch | 딥러닝 프레임워크 |
| OpenCV | 이미지 처리 |

---

## Android 프로젝트 구조

```
PhotoViewer/app/src/main/java/com/example/photoviewer/
├── MainActivity.java           # 메인 화면 (포스트 목록, CRUD, 동기화)
├── LoginActivity.java          # 로그인 화면
├── SplashActivity.java         # 스플래시 → 세션 확인 → 분기
├── PhotoViewerApplication.java # Application 클래스 (초기화)
├── Post.java                   # 포스트 데이터 모델
├── ImageAdapter.java           # RecyclerView 어댑터
├── services/
│   ├── SessionManager.java        # 세션 상태 관리 (싱글톤)
│   └── AuthenticationService.java # 서버 인증 API 호출
├── utils/
│   ├── SecureTokenManager.java    # 암호화된 토큰 저장
│   ├── SyncPreferences.java       # 동기화 상태 저장 (lastSeenPostId)
│   └── NotificationHelper.java    # 알림 생성 및 표시
└── workers/
    └── BackgroundSyncWorker.java  # WorkManager 백그라운드 동기화
```

---

## 문서 구조

```
docs/
├── mid-term-summary.md           # 중간고사 프로젝트 현황 정리 (본 문서)
└── mid/
    ├── implementation-reference.md  # API 연결 + 업로드 + 트러블슈팅 통합
    └── testing-guide.md             # 테스트 가이드 통합
```

> **Note**: 구현 완료된 계획/설계 문서(plans/, feature-implementation-plan.md 등)는 삭제됨

---

## API 엔드포인트

| 메서드 | 엔드포인트 | 설명 | 인증 |
|--------|------------|------|------|
| GET | `/api_root/Post/` | 모든 포스트 조회 | 선택 |
| POST | `/api_root/Post/` | 새 포스트 생성 | 필수 (Token) |
| GET | `/api_root/Post/{id}/` | 특정 포스트 조회 | 선택 |
| PUT | `/api_root/Post/{id}/` | 포스트 수정 | 필수 (Token) |
| DELETE | `/api_root/Post/{id}/` | 포스트 삭제 | 필수 (Token) |
| POST | `/api-token-auth/` | 인증 토큰 발급 | - |

---

## 주요 해결된 이슈

### 1. RecyclerView 이미지 표시 문제
- **증상**: 2개 이미지 다운로드 성공하나 1개만 표시
- **원인**: `item_image.xml`에서 `layout_height="match_parent"` 사용
- **해결**: `layout_height="wrap_content"`로 변경
- **문서**: `docs/mid/troubleshooting-recyclerview-image-display.md`

### 2. HTTP 401 Unauthorized
- **원인**: 토큰 형식 오류 또는 만료
- **해결**: Django shell에서 토큰 재생성
- **문서**: `docs/mid/api-connection-guide.md`

### 3. 업로드 시 author 필드 누락
- **원인**: Android 앱에서 author 전송하지 않음
- **해결**: `perform_create()`에서 `request.user` 자동 설정
- **문서**: `docs/mid/image-upload-implementation.md`

---

## 개발 환경 설정

### 서버 실행
```bash
cd PhotoBlogServer
source venv/bin/activate
python manage.py runserver
```

### Android 빌드
```bash
cd PhotoViewer
./gradlew build
```

### YOLOv5 실행
```bash
cd yolov5
source venv/bin/activate
python detect.py --weights yolov5s.pt --source 0
```

---

## 보안 고려사항

| 항목 | 현재 상태 | 비고 |
|------|-----------|------|
| Auth Token | ✅ SecureTokenManager (EncryptedSharedPreferences) | AES256-GCM 암호화 |
| 세션 관리 | ✅ SessionManager 싱글톤 | 앱 시작 시 세션 유효성 검증 |
| 로그인 | ✅ 서버 인증 후 토큰 발급 | `/api/auth/login/` 엔드포인트 |
| HTTP 통신 | BuildConfig로 분기 | Debug: HTTP, Release: HTTPS |
| YOLOv5 자격증명 | 하드코딩 | 환경변수로 이동 권장 |

---

## 구현 로드맵

### Phase 1 (완료)
- [x] Django 프로젝트 설정
- [x] REST API 구현
- [x] Android 기본 UI
- [x] 이미지 다운로드/표시
- [x] Pull to Refresh

### Phase 2 (완료)
- [x] 이미지 업로드 (제목/내용 포함)
- [x] 상세보기 다이얼로그
- [x] 포스트 수정 기능

### Phase 2.5 (완료)
- [x] ProgressBar 로딩 표시
- [x] 포스트 삭제 기능
- [x] 에러 처리 개선

### Phase 3 (완료)
- [x] 로그인 화면 구현 (SplashActivity, LoginActivity)
- [x] SecureTokenManager 적용 (EncryptedSharedPreferences)
- [x] SessionManager 및 AuthenticationService 구현
- [x] 로그아웃 기능

### Phase 3.5 (완료)
- [x] 실시간 알림 (Foreground polling 30초)
- [x] 백그라운드 동기화 (WorkManager 15분)
- [x] NotificationHelper 구현
- [x] 알림 권한 요청 (Android 13+)

### Phase 4 (선택사항 - 미구현)
- [ ] 검색/정렬 기능
- [ ] 오프라인 모드 (Room DB)
- [ ] 이미지 캐싱 (Glide)

---

## 참고 자료

- **프로젝트 루트**: `/Users/kade/Dev/mwp-finalterm-blog/`
- **CLAUDE.md**: 프로젝트 개요 및 명령어
- **Django Admin**: `http://127.0.0.1:8000/admin/`
- **API 브라우저**: `http://127.0.0.1:8000/api_root/Post/`

---

**최종 업데이트**: 2025-11-11
**작성**: Claude Code
