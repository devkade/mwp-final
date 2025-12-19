# Story 1.1: Backend Authentication Setup

Status: review

## Story

As a **system administrator**,
I want **to create API users with security keys and authenticate them via REST API**,
so that **Edge devices and Android apps can securely access the system**.

## Acceptance Criteria

1. **Given** the Django server is running **When** I create an ApiUser with a unique security_key in the admin **Then** the ApiUser is linked to a Django User and stored in the database

2. **Given** an active ApiUser exists with security_key "test-key-123" **When** I POST to `/api/auth/login/` with `{"security_key": "test-key-123"}` **Then** I receive HTTP 200 with `{"token": "<token>", "name": "<user_name>"}`

3. **Given** no ApiUser exists with security_key "invalid-key" **When** I POST to `/api/auth/login/` with `{"security_key": "invalid-key"}` **Then** I receive HTTP 401 with `{"error": "Invalid security key"}`

4. **Given** an ApiUser exists but is_active is False **When** I POST to `/api/auth/login/` with that security_key **Then** I receive HTTP 401 with `{"error": "Invalid security key"}`

## Tasks / Subtasks

- [x] Task 1: Create ApiUser model (AC: #1)
  - [x] 1.1: Add ApiUser class to blog/models.py with fields: name, security_key (unique, max 64), user (OneToOne to AUTH_USER_MODEL), created_at, is_active
  - [x] 1.2: Add __str__ method returning `{name} ({security_key[:8]}...)`
  - [x] 1.3: Run makemigrations and migrate

- [x] Task 2: Register ApiUser in Django Admin (AC: #1)
  - [x] 2.1: Add ApiUserAdmin class in blog/admin.py with list_display, search_fields, list_filter
  - [x] 2.2: Register ApiUser with admin.site.register

- [x] Task 3: Implement security_key_login view (AC: #2, #3, #4)
  - [x] 3.1: Add SecurityKeyLoginSerializer to blog/serializers.py with security_key field
  - [x] 3.2: Modify existing login view in blog/views.py to handle security_key authentication
  - [x] 3.3: Query ApiUser by security_key AND is_active=True
  - [x] 3.4: Return token and name on success (HTTP 200)
  - [x] 3.5: Return `{"error": "Invalid security key"}` on failure (HTTP 401)

- [x] Task 4: Write tests for authentication (AC: #1, #2, #3, #4)
  - [x] 4.1: Create blog/tests/test_auth.py
  - [x] 4.2: Test ApiUser creation and linking to Django User
  - [x] 4.3: Test successful login with valid active security_key
  - [x] 4.4: Test failed login with invalid security_key (401)
  - [x] 4.5: Test failed login with inactive ApiUser (401)

## Dev Notes

### Technical Requirements

**Framework & Versions:**
- Django 5.2.6
- Django REST Framework (already installed)
- rest_framework.authtoken (already configured)

**Model Specifications (from docs/final/implementation/02-backend.md):**
```python
class ApiUser(models.Model):
    """보안키 기반 API 사용자"""
    name = models.CharField(max_length=100)
    security_key = models.CharField(max_length=64, unique=True)
    user = models.OneToOneField(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='api_user'
    )
    created_at = models.DateTimeField(auto_now_add=True)
    is_active = models.BooleanField(default=True)

    def __str__(self):
        return f"{self.name} ({self.security_key[:8]}...)"
```

**API Endpoint (from docs/final/implementation/05-api-reference.md):**
- Method: POST
- URL: `/api/auth/login/`
- Request: `{"security_key": "..."}`
- Success Response (200): `{"token": "...", "name": "..."}`
- Failure Response (401): `{"error": "Invalid security key"}`

### Architecture Compliance

**Existing Infrastructure (DO NOT modify unless required):**
- `blog/models.py`: Currently has Post model only - ADD ApiUser here
- `blog/views.py`: Has existing `login` view (username/password) - REPLACE with security_key logic
- `mysite/urls.py`: Already has `/api/auth/login/` mapped to views.login - NO CHANGE NEEDED
- Token authentication already configured in settings.py REST_FRAMEWORK

**Code Patterns to Follow:**
- Use `@api_view(['POST'])` and `@permission_classes([AllowAny])` decorators (existing pattern)
- Use `Token.objects.get_or_create(user=api_user.user)` for token generation (existing pattern)
- Use HTTP status constants from rest_framework.status (existing pattern)

### Project Structure Notes

**Files to Create:**
- `blog/tests/test_auth.py` (new test file for authentication tests)

**Files to Modify:**
- `blog/models.py`: Add ApiUser model
- `blog/admin.py`: Register ApiUser
- `blog/serializers.py`: Add SecurityKeyLoginSerializer (optional, view can work without it)
- `blog/views.py`: Modify login view to use security_key instead of username/password

**Database:**
- SQLite3 at `PhotoBlogServer/db.sqlite3`
- Run migrations from `PhotoBlogServer/` directory

### Testing Standards

**Test Framework:** Django's built-in TestCase with DRF's APIClient
**Test Location:** `blog/tests/test_auth.py`

**Required Test Cases:**
1. ApiUser model creation with linked Django User
2. Login success with valid active security_key → 200 + token + name
3. Login failure with non-existent security_key → 401
4. Login failure with inactive ApiUser → 401

**Commands:**
```bash
cd PhotoBlogServer
python manage.py test blog.tests.test_auth
```

### References

- [Source: docs/final/implementation/02-backend.md#ApiUser 모델]
- [Source: docs/final/implementation/05-api-reference.md#인증 API]
- [Source: docs/final/implementation/01-codebase-analysis.md#재사용 가능한 컴포넌트]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References

- No issues encountered during implementation

### Completion Notes List

- Created ApiUser model with all required fields (name, security_key, user FK, created_at, is_active)
- Added __str__ method with truncated security_key display
- Registered ApiUser in Django admin with list_display, search_fields, list_filter
- Added SecurityKeyLoginSerializer to serializers.py
- Modified login view to use security_key authentication instead of username/password
- Created comprehensive test suite with 8 test cases covering all acceptance criteria
- All tests pass (8/8)

### File List

**Created:**
- `PhotoBlogServer/blog/tests/__init__.py`
- `PhotoBlogServer/blog/tests/test_auth.py`
- `PhotoBlogServer/blog/migrations/0003_apiuser.py`

**Modified:**
- `PhotoBlogServer/blog/models.py` - Added ApiUser model
- `PhotoBlogServer/blog/admin.py` - Registered ApiUser with ApiUserAdmin
- `PhotoBlogServer/blog/serializers.py` - Added SecurityKeyLoginSerializer
- `PhotoBlogServer/blog/views.py` - Modified login view for security_key authentication

**Deleted:**
- `PhotoBlogServer/blog/tests.py` - Removed conflicting placeholder file

## Change Log

- 2025-12-19: Story created with comprehensive context from implementation specs
- 2025-12-19: Implementation completed - all 4 tasks done, 8 tests passing
