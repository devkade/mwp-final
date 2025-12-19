# 기능 명세 (Feature Specifications)

## 개요

GymFlow 시스템의 핵심 기능을 컴포넌트별로 정리합니다.

---

## 1. Edge System (YOLOv5)

### 1.1 실시간 객체 감지

| 기능 | 설명 |
|------|------|
| **YOLOv5 모델** | yolov5s.pt (Small) 사전 훈련 모델 사용 |
| **MS COCO 클래스** | 80가지 객체 감지 가능 (기본: person) |
| **소스 지원** | 웹캠, RTSP 스트림, 동영상 파일 |
| **설정 가능** | 대상 클래스 변경 가능 |

**실행 예시:**
```bash
# 웹캠 사용
python detect.py --weights yolov5s.pt --source 0

# RTSP 스트림
python detect.py --weights yolov5s.pt --source 'rtsp://...'

# 동영상 파일
python detect.py --weights yolov5s.pt --source video.mp4
```

### 1.2 양방향 상태 변화 감지 (Change Detection)

| 이벤트 | 조건 | 설명 |
|--------|------|------|
| **START** | 0 → 1+ | 사람이 없다가 나타남 (사용 시작) |
| **END** | 1+ → 0 | 사람이 있다가 사라짐 (사용 종료) |

**5초 디바운싱:**
- 상태 변화 후 5초간 유지되어야 이벤트 발생
- 순간적인 감지 오류나 지나가는 사람 필터링
- 안정적인 사용 감지 보장

### 1.3 이벤트 전송

| 기능 | 설명 |
|------|------|
| **인증** | 보안키로 토큰 획득 후 API 호출 |
| **이미지 캡처** | 이벤트 발생 시점 프레임 저장 (640x480) |
| **데이터 전송** | multipart/form-data로 이미지 + 메타데이터 전송 |
| **로컬 백업** | runs/detect/events/ 폴더에 이미지 저장 |

---

## 2. Service System (Django Backend)

### 2.1 인증 시스템

| 기능 | 설명 |
|------|------|
| **Security Key 인증** | 64자 고유 보안키로 로그인 |
| **Token 발급** | DRF Token 기반 API 인증 |
| **세션 관리** | 웹 대시보드용 세션 인증 |

**보안키 특징:**
- 사용자별 고유 발급
- 관리자가 활성/비활성 제어 가능
- Django User와 1:1 연결

### 2.2 운동 기구 관리

| 기능 | 설명 |
|------|------|
| **기구 등록** | 이름, 유형, 위치, 썸네일 등록 |
| **기구 유형** | 8가지 카테고리 (런닝머신, 벤치프레스 등) |
| **활성 상태** | is_active로 표시/숨김 제어 |
| **통계 조회** | 기구별 사용량 집계 |

**기구 유형:**
- treadmill (런닝머신)
- bench_press (벤치프레스)
- squat_rack (스쿼트랙)
- lat_pulldown (랫풀다운)
- leg_press (레그프레스)
- cable_machine (케이블머신)
- dumbbell (덤벨존)
- other (기타)

### 2.3 이벤트 관리

| 기능 | 설명 |
|------|------|
| **이벤트 저장** | Edge에서 전송된 사용 이벤트 저장 |
| **이미지 저장** | 캡처 이미지를 media 폴더에 저장 |
| **필터링** | 이벤트 타입, 날짜 범위로 필터링 |
| **페이지네이션** | 20개 단위 페이지 처리 |

**저장 데이터:**
- event_type: start/end
- captured_at: 캡처 시각
- person_count: 감지 인원
- detections: YOLO 감지 결과 (JSON)
- change_info: 상태 변화 정보 (JSON)
- image: 캡처 이미지

### 2.4 통계 API

| 기능 | 설명 |
|------|------|
| **일별 사용량** | 기구별 일별 START 이벤트 수 집계 |
| **기간 필터** | date_from, date_to로 기간 지정 |
| **총계 제공** | 총 START/END 횟수 |

**응답 예시:**
```json
{
  "machine_id": 1,
  "machine_name": "런닝머신 01",
  "total_starts": 42,
  "total_ends": 40,
  "daily_usage": [
    {"date": "2025-12-18", "count": 5},
    {"date": "2025-12-19", "count": 8}
  ]
}
```

### 2.5 웹 대시보드

| 페이지 | 기능 |
|--------|------|
| **로그인** | Security Key 로그인 |
| **대시보드** | 기구 목록 및 현황 |
| **기구 상세** | 개별 기구 정보 |
| **사용 이력** | 기구별 이벤트 목록 |
| **사용자 관리** | API 사용자 CRUD |
| **기구 관리** | 운동 기구 CRUD |

---

## 3. Client System (Android App)

### 3.1 로그인

| 기능 | 설명 |
|------|------|
| **보안키 입력** | Security Key 입력 필드 |
| **토큰 저장** | SecureTokenManager로 안전하게 저장 |
| **자동 로그인** | 세션 유지 시 자동 로그인 |
| **에러 처리** | 잘못된 키, 네트워크 오류 메시지 |

**에러 메시지:**
- 잘못된 보안키: "잘못된 보안키입니다"
- 네트워크 오류: "네트워크 오류가 발생했습니다"

### 3.2 기구 목록

| 기능 | 설명 |
|------|------|
| **RecyclerView 표시** | 활성 기구 목록 |
| **썸네일 표시** | 비동기 이미지 로딩 |
| **이벤트 수 표시** | 기구별 총 이벤트 수 |
| **Pull-to-Refresh** | 당겨서 새로고침 |
| **클릭 동작** | 이벤트 목록 또는 통계 선택 다이얼로그 |

### 3.3 이벤트 목록

| 기능 | 설명 |
|------|------|
| **이벤트 표시** | 기구별 사용 이벤트 목록 |
| **이벤트 필터** | 전체 / 시작 / 종료 ChipGroup |
| **날짜 필터** | 시작일 ~ 종료일 DatePicker |
| **이미지 썸네일** | 감지된 이미지 표시 |
| **이미지 확대** | 썸네일 클릭 시 전체화면 |

### 3.4 이미지 확대 보기 (ImageViewerDialog)

| 기능 | 설명 |
|------|------|
| **전체화면 표시** | 다이얼로그로 이미지 확대 |
| **정보 오버레이** | 이벤트 타입, 시간, 인원 수 표시 |
| **닫기** | X 버튼 또는 배경 터치로 닫기 |

### 3.5 통계 화면

| 기능 | 설명 |
|------|------|
| **막대 차트** | 일별 사용량 시각화 (MPAndroidChart) |
| **메트릭 카드** | 총 사용 횟수, 가장 바쁜 날 등 |
| **날짜 필터** | MaterialDatePicker로 기간 선택 |
| **기본 기간** | 최근 7일 |

### 3.6 UI 테마

| 항목 | 값 |
|------|-----|
| **Primary Color** | #13A4EC (파란색) |
| **Background** | #101C22 (다크 그레이) |
| **Surface** | #19252B (카드 배경) |
| **Theme** | Dark Theme |

---

## 4. 기능 매핑

### 요구사항 ↔ 구현 매핑

| 요구사항 | 컴포넌트 | 구현 파일 |
|---------|---------|----------|
| YOLOv5 모델 사용 | Edge | `detect.py` |
| 80클래스 객체 감지 | Edge | `coco.yaml` |
| Change Detection | Edge | `changedetection.py` |
| RESTful API 전송 | Edge | `changedetection.py` |
| 보안키 로그인 | Server | `views.py`, `models.py` |
| 이미지 블로그 | Server | `models.py` (Post) |
| 기구 관리 API | Server | `views.py` |
| 이벤트 API | Server | `views.py` |
| 통계 API | Server | `views.py` |
| 이미지 목록 뷰 | Android | `EventListActivity.java` |
| API 사용 | Android | `GymApiService.java` |
| 사용자 UI | Android | 전체 Activity |

---

*문서 버전: 1.0 | 작성일: 2025-12-20*
