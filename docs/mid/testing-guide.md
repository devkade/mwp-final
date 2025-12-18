# 중간고사 프로젝트 테스트 가이드

**통합 문서**: PHASE2_TESTING_GUIDE.md, ERROR_HANDLING_TEST_GUIDE.md, login-security-testing.md

---

## 1. 테스트 환경 설정

### 필수 조건
- Django 서버: `localhost:8000`
- Android 에뮬레이터: API 24+
- 테스트 계정 생성 완료

### 서버 시작
```bash
cd PhotoBlogServer
source venv/bin/activate
python manage.py runserver
```

### 테스트 계정 생성
```bash
python manage.py shell
from django.contrib.auth.models import User
User.objects.create_user(username='testuser', password='testpass123')
```

---

## 2. 로그인/세션 테스트

| TC | 테스트 항목 | 예상 결과 |
|----|------------|----------|
| TC1 | 앱 최초 실행 (토큰 없음) | Splash → Login 화면 |
| TC2 | 유효한 로그인 | MainActivity로 이동, 토큰 저장 |
| TC3 | 잘못된 자격증명 | "Invalid credentials" 에러 |
| TC4 | 세션 유지 (앱 재시작) | 자동 로그인 |
| TC5 | 로그아웃 | Login 화면으로 이동 |

---

## 3. 이미지 동기화 테스트

| TC | 테스트 항목 | 예상 결과 |
|----|------------|----------|
| TC6 | 동기화 버튼 클릭 | 이미지 목록 표시 |
| TC7 | Pull to Refresh | 새로고침 동작 |
| TC8 | 이미지 클릭 → 상세보기 | 다이얼로그 표시 |
| TC9 | 포스트 수정 | 서버 업데이트 후 새로고침 |
| TC10 | 포스트 삭제 | 확인 다이얼로그 → 삭제 |

---

## 4. 에러 처리 테스트

| 에러 유형 | 사용자 메시지 | Logcat |
|----------|--------------|--------|
| Null post | "포스트를 표시할 수 없습니다" | WARNING |
| NullPointerException | "포스트 데이터를 불러올 수 없습니다" | ERROR |
| IllegalStateException | "다이얼로그를 표시할 수 없습니다" | ERROR |
| 네트워크 오류 | "네트워크 연결을 확인해주세요" | ERROR |

---

## 5. Logcat 확인 필터

```
Tag: MainActivity
Level: Debug
```

**성공 로그 예시:**
```
D/MainActivity: Total posts received: 2
D/MainActivity: ✓ Image #1 downloaded successfully
D/MainActivity: onPostClicked: dialog shown for post: 제목
```
