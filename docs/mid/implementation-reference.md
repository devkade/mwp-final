# 중간고사 프로젝트 구현 레퍼런스

**통합 문서**: api-connection-guide.md, image-upload-implementation.md, troubleshooting-recyclerview-image-display.md

---

## 1. API 연결 구조

```
Android App (MainActivity.java)
    ↓ HTTP GET/POST
    ↓ Authorization: Token <token>
Django Server (api_root/Post/)
    ↓
JSON Response + Image URLs
    ↓
RecyclerView 표시
```

### 엔드포인트

| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/api_root/Post/` | 포스트 목록 |
| POST | `/api_root/Post/` | 포스트 생성 |
| PUT | `/api_root/Post/{id}/` | 포스트 수정 |
| DELETE | `/api_root/Post/{id}/` | 포스트 삭제 |
| POST | `/api/auth/login/` | 로그인 |

### 토큰 생성 (Django Shell)
```python
from rest_framework.authtoken.models import Token
from django.contrib.auth.models import User
user = User.objects.get(username='admin')
token, _ = Token.objects.get_or_create(user=user)
print(token.key)
```

---

## 2. 이미지 업로드

### Multipart Form Data 구조
```
--boundary
Content-Disposition: form-data; name="title"
제목
--boundary
Content-Disposition: form-data; name="text"
내용
--boundary
Content-Disposition: form-data; name="image"; filename="image.jpg"
Content-Type: image/*
[바이너리 데이터]
--boundary--
```

### 서버 설정 (views.py)
```python
class BlogImages(viewsets.ModelViewSet):
    def perform_create(self, serializer):
        serializer.save(author=self.request.user, published_date=timezone.now())
```

---

## 3. 트러블슈팅

### RecyclerView 이미지 1개만 표시
- **원인**: `item_image.xml`에서 `layout_height="match_parent"`
- **해결**: `layout_height="wrap_content"`로 변경

### HTTP 401 Unauthorized
- **원인**: 토큰 만료 또는 형식 오류
- **해결**: Django shell에서 토큰 재생성

### CLEARTEXT 통신 차단
- **해결**: `network_security_config.xml`에서 `cleartextTrafficPermitted="true"`

### 업로드 시 author 누락
- **해결**: `perform_create()`에서 `request.user` 자동 설정

---

## 4. 주요 코드 위치

| 기능 | 파일 | 라인 |
|------|------|------|
| 이미지 다운로드 | MainActivity.java | 217-365 |
| 이미지 업로드 | MainActivity.java | 608-758 |
| 상세보기 | MainActivity.java | 367-421 |
| 포스트 수정 | MainActivity.java | 440-527, 778-880 |
| 포스트 삭제 | MainActivity.java | 882-925 |
