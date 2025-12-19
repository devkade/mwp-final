# 테스트 시나리오 목록

## 개요

Gym Equipment Detection System 수동 테스트 시나리오 문서입니다.

## 테스트 시나리오

| # | 시나리오 | 설명 | 파일 |
|---|---------|------|------|
| 01 | [로그인](TEST_01_LOGIN.md) | 토큰 인증 및 Android 로그인 | TEST_01_LOGIN.md |
| 02 | [운동기구 목록](TEST_02_MACHINE_LIST.md) | 운동기구 API 및 목록 화면 | TEST_02_MACHINE_LIST.md |
| 03 | [이벤트](TEST_03_EVENTS.md) | Edge→Backend→Android 이벤트 흐름 | TEST_03_EVENTS.md |
| 04 | [통계](TEST_04_STATISTICS.md) | 통계 API 및 차트 화면 | TEST_04_STATISTICS.md |

## 테스트 환경

### 서버 시작

```bash
cd PhotoBlogServer
source venv/bin/activate
python manage.py runserver
```

### Android 빌드

```bash
cd PhotoViewer
./gradlew assembleDebug
```

### YOLOv5 (이벤트 생성용)

```bash
cd yolov5
source venv/bin/activate
python detect.py --weights yolov5s.pt --source 0
```

## 테스트 진행 상태

| 시나리오 | 상태 | 테스트 일시 | 비고 |
|---------|------|------------|------|
| 01 로그인 | ⬜ 대기 | | |
| 02 운동기구 목록 | ⬜ 대기 | | |
| 03 이벤트 | ⬜ 대기 | | |
| 04 통계 | ⬜ 대기 | | |

**상태 범례**: ⬜ 대기 / 🔄 진행중 / ✅ 통과 / ❌ 실패
