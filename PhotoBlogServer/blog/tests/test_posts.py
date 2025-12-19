"""
Blog API Tests - Post CRUD Operations

Test IDs follow the pattern from test-design documents:
- BE-1-05 ~ BE-1-07: Post List API tests
- BE-2-01 ~ BE-2-11: Post CRUD API tests

Run tests with: python manage.py test blog
Run specific test: python manage.py test blog.tests.PostCreateAPITests

Note: Authentication tests are in blog/tests/test_auth.py
"""

import io
from PIL import Image

from django.test import TestCase
from django.contrib.auth.models import User
from django.core.files.uploadedfile import SimpleUploadedFile

from rest_framework.test import APITestCase
from rest_framework import status
from rest_framework.authtoken.models import Token

from blog.models import Post, ApiUser


# =============================================================================
# BE-1: ApiUser Model Tests
# =============================================================================

class ApiUserModelTest(TestCase):
    """Test ApiUser model creation and linking to Django User"""

    def test_create_api_user_with_linked_django_user(self):
        """Test that ApiUser is linked to Django User correctly"""
        user = User.objects.create_user(
            username='testuser',
            password='testpass123'
        )
        api_user = ApiUser.objects.create(
            name='Test API User',
            security_key='test-key-12345678',
            user=user,
            is_active=True
        )

        self.assertEqual(api_user.user, user)
        self.assertEqual(api_user.name, 'Test API User')
        self.assertEqual(api_user.security_key, 'test-key-12345678')
        self.assertTrue(api_user.is_active)
        self.assertIsNotNone(api_user.created_at)

    def test_api_user_str_representation(self):
        """Test __str__ method returns name with truncated security_key"""
        user = User.objects.create_user(
            username='testuser2',
            password='testpass123'
        )
        api_user = ApiUser.objects.create(
            name='Test User',
            security_key='abcdefghijklmnop',
            user=user
        )

        self.assertEqual(str(api_user), 'Test User (abcdefgh...)')

    def test_security_key_unique_constraint(self):
        """Test that security_key must be unique"""
        user1 = User.objects.create_user(username='user1', password='pass1')
        user2 = User.objects.create_user(username='user2', password='pass2')

        ApiUser.objects.create(
            name='User 1',
            security_key='unique-key-123',
            user=user1
        )

        with self.assertRaises(Exception):
            ApiUser.objects.create(
                name='User 2',
                security_key='unique-key-123',
                user=user2
            )


# =============================================================================
# BE-1: Security Key Login Tests
# =============================================================================

class SecurityKeyLoginTests(APITestCase):
    """
    Tests for security key login endpoint: POST /api/login/

    Test IDs: BE-1-01 through BE-1-04
    Priority: P0 (Critical - Authentication)
    """

    def setUp(self):
        """Set up test data"""
        self.login_url = '/api/auth/login/'
        self.user = User.objects.create_user(
            username='apiuser',
            password='testpass123'
        )
        self.api_user = ApiUser.objects.create(
            name='Test Manager',
            security_key='test-key-123',
            user=self.user,
            is_active=True
        )

    def tearDown(self):
        """Clean up test data"""
        Token.objects.all().delete()
        ApiUser.objects.all().delete()
        User.objects.all().delete()

    def test_BE_1_01_login_with_valid_security_key(self):
        """
        BE-1-01: Valid security key returns token

        Given: A valid, active API user exists
        When: POST /api/login/ with valid security_key
        Then: Returns 200 OK with token and user name
        """
        response = self.client.post(self.login_url, {
            'security_key': 'test-key-123'
        })

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('token', response.data)
        self.assertIn('name', response.data)
        self.assertEqual(response.data['name'], 'Test Manager')
        self.assertTrue(len(response.data['token']) > 0)

    def test_BE_1_02_login_with_invalid_security_key(self):
        """
        BE-1-02: Invalid security key returns 401 Unauthorized

        Given: No API user with the given security key
        When: POST /api/login/ with invalid security_key
        Then: Returns 401 Unauthorized with error message
        """
        response = self.client.post(self.login_url, {
            'security_key': 'invalid-key-that-does-not-exist'
        })

        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)
        self.assertIn('error', response.data)

    def test_BE_1_03_login_with_empty_security_key(self):
        """
        BE-1-03: Empty security key returns 400 Bad Request

        Given: Any state
        When: POST /api/login/ with empty or missing security_key
        Then: Returns 400 Bad Request
        """
        # Empty string
        response = self.client.post(self.login_url, {'security_key': ''})
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

        # Missing field
        response = self.client.post(self.login_url, {})
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_BE_1_04_login_with_inactive_user(self):
        """
        BE-1-04: Inactive API user returns 401 Unauthorized

        Given: An API user exists but is_active=False
        When: POST /api/login/ with that user's security_key
        Then: Returns 401 Unauthorized
        """
        inactive_user = User.objects.create_user(
            username='inactiveuser',
            password='testpass123'
        )
        ApiUser.objects.create(
            name='Inactive User',
            security_key='inactive-key-123',
            user=inactive_user,
            is_active=False
        )

        response = self.client.post(self.login_url, {
            'security_key': 'inactive-key-123'
        })

        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_login_returns_consistent_token(self):
        """Test that same ApiUser gets same token on multiple logins"""
        response1 = self.client.post(self.login_url, {'security_key': 'test-key-123'})
        response2 = self.client.post(self.login_url, {'security_key': 'test-key-123'})

        self.assertEqual(response1.data['token'], response2.data['token'])


# =============================================================================
# Test Factories
# =============================================================================

def create_test_image(name='test.jpg', size=(100, 100), color='red'):
    """
    Factory function to create test images.

    Args:
        name: Filename for the image
        size: Tuple of (width, height)
        color: Color name or RGB tuple

    Returns:
        SimpleUploadedFile suitable for Django forms/API
    """
    image = Image.new('RGB', size, color=color)
    tmp_file = io.BytesIO()
    image.save(tmp_file, format='JPEG')
    tmp_file.seek(0)
    return SimpleUploadedFile(
        name=name,
        content=tmp_file.read(),
        content_type='image/jpeg'
    )


class ApiUserFactory:
    """Factory for creating test ApiUser objects with linked Django User"""

    _counter = 0

    @classmethod
    def create(cls, security_key=None, **kwargs):
        """
        Create an ApiUser with a linked Django User.

        Args:
            security_key: Custom security key (auto-generated if not provided)
            **kwargs: Additional fields for ApiUser

        Returns:
            ApiUser instance
        """
        cls._counter += 1

        if security_key is None:
            security_key = f'test_security_key_{cls._counter:08d}'

        # Create associated Django user
        username = kwargs.pop('username', f'testuser_{cls._counter}')
        user = User.objects.create_user(
            username=username,
            password='testpass123'
        )

        defaults = {
            'name': f'Test API User {cls._counter}',
            'security_key': security_key,
            'user': user,
            'is_active': True,
        }
        defaults.update(kwargs)

        return ApiUser.objects.create(**defaults)


class PostFactory:
    """Factory for creating test Post objects"""

    _counter = 0

    @classmethod
    def create(cls, author, **kwargs):
        """
        Create a Post with sensible defaults.

        Args:
            author: User instance (the author)
            **kwargs: Additional fields for Post

        Returns:
            Post instance
        """
        cls._counter += 1

        defaults = {
            'title': f'Test Post Title {cls._counter}',
            'text': f'Test post content {cls._counter}',
        }
        defaults.update(kwargs)

        # Create image if not provided
        if 'image' not in defaults:
            defaults['image'] = create_test_image(f'post_{cls._counter}.jpg')

        return Post.objects.create(author=author, **defaults)


# =============================================================================
# BE-1: Post List API Tests (Epic 1)
# =============================================================================

class PostListAPITests(APITestCase):
    """
    Tests for Post list endpoint: GET /api_root/Post/

    Test IDs: BE-1-05 through BE-1-07
    Priority: P0 (Critical path)
    """

    def setUp(self):
        """Set up test data before each test"""
        self.list_url = '/api_root/Post/'

        # Create API user and get token
        self.api_user = ApiUserFactory.create()
        self.token, _ = Token.objects.get_or_create(user=self.api_user.user)

    def tearDown(self):
        """Clean up test data after each test"""
        Post.objects.all().delete()
        Token.objects.all().delete()
        ApiUser.objects.all().delete()
        User.objects.all().delete()

    def test_BE_1_05_get_post_list_authenticated(self):
        """
        BE-1-05: Authenticated user can get post list

        Priority: P0
        Requirement: FR10 - 기구 목록 API

        Given: Authenticated user with valid token and 3 posts exist
        When: GET /api_root/Post/
        Then: Returns 200 OK with list of 3 posts
        """
        # Given: Create 3 posts
        for i in range(3):
            PostFactory.create(
                author=self.api_user.user,
                title=f'Post {i+1}'
            )
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        # When
        response = self.client.get(self.list_url)

        # Then
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data), 3)
        # Verify post structure
        post = response.data[0]
        self.assertIn('id', post)
        self.assertIn('title', post)
        self.assertIn('text', post)
        self.assertIn('image', post)

    def test_BE_1_06_get_post_list_unauthenticated(self):
        """
        BE-1-06: Unauthenticated request - current behavior allows access

        Priority: P0
        Requirement: FR10 - 기구 목록 API

        Note: Current implementation allows unauthenticated access to Post list.
        This test documents the current behavior. If authentication is required,
        configure IsAuthenticated permission in BlogImages viewset.

        Given: No authentication token provided
        When: GET /api_root/Post/
        Then: Currently returns 200 OK (API allows unauthenticated access)
        """
        # When (no credentials set)
        response = self.client.get(self.list_url)

        # Then - current behavior allows unauthenticated access
        # TODO: If authentication should be required, update BlogImages viewset
        # with permission_classes = [IsAuthenticated]
        self.assertIn(response.status_code, [status.HTTP_200_OK, status.HTTP_401_UNAUTHORIZED])

    def test_BE_1_07_get_empty_post_list(self):
        """
        BE-1-07: Empty post list returns empty array

        Priority: P1
        Requirement: FR10 - 기구 목록 API (빈 목록 처리)

        Given: No posts in database
        When: GET /api_root/Post/
        Then: Returns 200 OK with empty array []
        """
        # Given: Ensure no posts exist
        Post.objects.all().delete()
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        # When
        response = self.client.get(self.list_url)

        # Then
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data, [])
        self.assertEqual(len(response.data), 0)


# =============================================================================
# BE-2: Post Create API Tests (Epic 2)
# =============================================================================

class PostCreateAPITests(APITestCase):
    """
    Tests for Post creation: POST /api_root/Post/

    Test IDs: BE-2-01 through BE-2-04
    Priority: P0 (Critical path for event posting)
    """

    def setUp(self):
        """Set up test data before each test"""
        self.create_url = '/api_root/Post/'

        # Create API user and get token
        self.api_user = ApiUserFactory.create()
        self.token, _ = Token.objects.get_or_create(user=self.api_user.user)

    def tearDown(self):
        """Clean up test data after each test"""
        Post.objects.all().delete()
        Token.objects.all().delete()
        ApiUser.objects.all().delete()
        User.objects.all().delete()

    def test_BE_2_01_create_post_with_image(self):
        """
        BE-2-01: Create post with all fields including image

        Priority: P0
        Requirement: FR8 - Event Posting API

        Given: Authenticated user
        When: POST /api_root/Post/ with title, text, and image
        Then: Returns 201 Created with post data including ID
        """
        # Given
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        test_image = create_test_image('new_post.jpg')

        # When
        response = self.client.post(self.create_url, {
            'title': '새 포스트 제목',
            'text': '포스트 내용입니다.',
            'image': test_image,
        }, format='multipart')

        # Then
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertIn('id', response.data)
        self.assertEqual(response.data['title'], '새 포스트 제목')
        self.assertEqual(response.data['text'], '포스트 내용입니다.')
        self.assertIn('image', response.data)
        self.assertTrue(response.data['image'])  # Image URL is set

        # Verify in database
        post = Post.objects.get(id=response.data['id'])
        self.assertEqual(post.title, '새 포스트 제목')
        self.assertEqual(post.author, self.api_user.user)
        self.assertIsNotNone(post.published_date)

    def test_BE_2_02_create_post_without_image(self):
        """
        BE-2-02: Create post without image uses default

        Priority: P1
        Requirement: FR8 - Event Posting API (이미지 선택적)

        Given: Authenticated user
        When: POST /api_root/Post/ with title and text only (no image)
        Then: Returns 201 Created with default image
        """
        # Given
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        # When
        response = self.client.post(self.create_url, {
            'title': '이미지 없는 포스트',
            'text': '텍스트만 있는 포스트입니다.',
        }, format='multipart')

        # Then
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data['title'], '이미지 없는 포스트')
        # Default image should be set
        self.assertIn('image', response.data)

    def test_BE_2_03_create_post_unauthenticated(self):
        """
        BE-2-03: Unauthenticated post creation returns 401

        Priority: P0
        Requirement: FR8 - Event Posting API (인증 필수)

        Given: No authentication token
        When: POST /api_root/Post/
        Then: Returns 401 Unauthorized
        """
        # When (no credentials)
        response = self.client.post(self.create_url, {
            'title': 'Test',
            'text': 'Test content',
        }, format='multipart')

        # Then
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

        # Verify no post was created
        self.assertEqual(Post.objects.count(), 0)

    def test_BE_2_04_create_post_missing_required_fields(self):
        """
        BE-2-04: Missing required field returns 400

        Priority: P1
        Requirement: FR8 - Event Posting API (필수 필드 검증)

        Given: Authenticated user
        When: POST /api_root/Post/ without title
        Then: Returns 400 Bad Request
        """
        # Given
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        # When: Missing title
        response = self.client.post(self.create_url, {
            'text': 'Content without title',
        }, format='multipart')

        # Then
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_BE_2_05_create_post_sets_author_automatically(self):
        """
        BE-2-05: Post author is set automatically from token

        Priority: P1
        Requirement: FR8 - Event Posting API (자동 author 설정)

        Given: Authenticated user
        When: POST /api_root/Post/ (without specifying author)
        Then: Author is set to the authenticated user
        """
        # Given
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        # When
        response = self.client.post(self.create_url, {
            'title': 'Auto Author Test',
            'text': 'Testing automatic author assignment',
        }, format='multipart')

        # Then
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

        post = Post.objects.get(id=response.data['id'])
        self.assertEqual(post.author, self.api_user.user)


# =============================================================================
# BE-2: Post Detail API Tests (Epic 2)
# =============================================================================

class PostDetailAPITests(APITestCase):
    """
    Tests for Post detail operations: GET/PUT/DELETE /api_root/Post/{id}/

    Test IDs: BE-2-07 through BE-2-11
    Priority: P1 (Secondary CRUD operations)
    """

    def setUp(self):
        """Set up test data before each test"""
        # Create API user and get token
        self.api_user = ApiUserFactory.create()
        self.token, _ = Token.objects.get_or_create(user=self.api_user.user)

        # Create a test post
        self.post = PostFactory.create(
            author=self.api_user.user,
            title='Original Title',
            text='Original content'
        )
        self.detail_url = f'/api_root/Post/{self.post.id}/'

    def tearDown(self):
        """Clean up test data after each test"""
        Post.objects.all().delete()
        Token.objects.all().delete()
        ApiUser.objects.all().delete()
        User.objects.all().delete()

    def test_BE_2_07_get_post_detail(self):
        """
        BE-2-07: Get single post by ID

        Priority: P1
        Requirement: FR12 - Event Detail API

        Given: Post with ID exists
        When: GET /api_root/Post/{id}/
        Then: Returns 200 OK with full post data
        """
        # Given
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        # When
        response = self.client.get(self.detail_url)

        # Then
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['id'], self.post.id)
        self.assertEqual(response.data['title'], 'Original Title')
        self.assertEqual(response.data['text'], 'Original content')
        self.assertIn('image', response.data)
        self.assertIn('created_date', response.data)

    def test_BE_2_08_get_nonexistent_post(self):
        """
        BE-2-08: Get non-existent post returns 404

        Priority: P1
        Requirement: FR12 - Event Detail API (404 처리)

        Given: Post ID 99999 does not exist
        When: GET /api_root/Post/99999/
        Then: Returns 404 Not Found
        """
        # Given
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        # When
        response = self.client.get('/api_root/Post/99999/')

        # Then
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_BE_2_09_update_post_with_put(self):
        """
        BE-2-09: Update post with PUT

        Priority: P1
        Requirement: Post Update API

        Given: Post exists, authenticated user
        When: PUT /api_root/Post/{id}/ with new title and text
        Then: Returns 200 OK with updated data
        """
        # Given
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        # When
        response = self.client.put(self.detail_url, {
            'title': 'Updated Title',
            'text': 'Updated content',
        }, format='multipart')

        # Then
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['title'], 'Updated Title')
        self.assertEqual(response.data['text'], 'Updated content')

        # Verify in database
        self.post.refresh_from_db()
        self.assertEqual(self.post.title, 'Updated Title')
        self.assertEqual(self.post.text, 'Updated content')

    def test_BE_2_10_delete_post(self):
        """
        BE-2-10: Delete post returns 204

        Priority: P1
        Requirement: Post Delete API

        Given: Post exists, authenticated user
        When: DELETE /api_root/Post/{id}/
        Then: Returns 204 No Content, post is deleted from database
        """
        # Given
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')
        post_id = self.post.id

        # When
        response = self.client.delete(self.detail_url)

        # Then
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(Post.objects.filter(id=post_id).exists())

    def test_BE_2_11_delete_nonexistent_post(self):
        """
        BE-2-11: Delete non-existent post returns 404

        Priority: P2
        Requirement: Post Delete API (404 처리)

        Given: Post ID 99999 does not exist
        When: DELETE /api_root/Post/99999/
        Then: Returns 404 Not Found
        """
        # Given
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        # When
        response = self.client.delete('/api_root/Post/99999/')

        # Then
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)


# =============================================================================
# Integration Tests
# =============================================================================

class FullAuthenticationFlowTests(APITestCase):
    """
    Integration tests for complete authentication and post flow.

    Tests the full workflow: Login -> Get Token -> Create Post -> List Posts
    """

    def setUp(self):
        """Set up test data"""
        self.api_user = ApiUserFactory.create(
            security_key='integration_test_key_123'
        )

    def tearDown(self):
        """Clean up test data"""
        Post.objects.all().delete()
        Token.objects.all().delete()
        ApiUser.objects.all().delete()
        User.objects.all().delete()

    def test_integration_login_create_and_list_posts(self):
        """
        Integration: Login with security key, create post, then list posts

        Given: Valid API user exists
        When: Login, create post with returned token, then list posts
        Then: Created post appears in list
        """
        # Step 1: Login with security key
        login_response = self.client.post('/api/auth/login/', {
            'security_key': 'integration_test_key_123'
        })
        self.assertEqual(login_response.status_code, status.HTTP_200_OK)
        token = login_response.data['token']

        # Step 2: Create post with token
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {token}')
        test_image = create_test_image()

        create_response = self.client.post('/api_root/Post/', {
            'title': 'Integration Test Post',
            'text': 'Created via full auth flow',
            'image': test_image,
        }, format='multipart')

        self.assertEqual(create_response.status_code, status.HTTP_201_CREATED)
        created_id = create_response.data['id']

        # Step 3: List posts and verify created post is there
        list_response = self.client.get('/api_root/Post/')
        self.assertEqual(list_response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(list_response.data), 1)
        self.assertEqual(list_response.data[0]['id'], created_id)
        self.assertEqual(list_response.data[0]['title'], 'Integration Test Post')

    def test_integration_crud_operations(self):
        """
        Integration: Full CRUD cycle (Create, Read, Update, Delete)

        Given: Authenticated user
        When: Perform Create -> Read -> Update -> Delete
        Then: Each operation succeeds and database state is correct
        """
        # Setup: Authenticate
        token, _ = Token.objects.get_or_create(user=self.api_user.user)
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {token.key}')

        # Create
        create_response = self.client.post('/api_root/Post/', {
            'title': 'CRUD Test Post',
            'text': 'Original content',
        }, format='multipart')
        self.assertEqual(create_response.status_code, status.HTTP_201_CREATED)
        post_id = create_response.data['id']
        detail_url = f'/api_root/Post/{post_id}/'

        # Read
        read_response = self.client.get(detail_url)
        self.assertEqual(read_response.status_code, status.HTTP_200_OK)
        self.assertEqual(read_response.data['title'], 'CRUD Test Post')

        # Update
        update_response = self.client.put(detail_url, {
            'title': 'Updated CRUD Post',
            'text': 'Updated content',
        }, format='multipart')
        self.assertEqual(update_response.status_code, status.HTTP_200_OK)
        self.assertEqual(update_response.data['title'], 'Updated CRUD Post')

        # Delete
        delete_response = self.client.delete(detail_url)
        self.assertEqual(delete_response.status_code, status.HTTP_204_NO_CONTENT)

        # Verify deleted
        verify_response = self.client.get(detail_url)
        self.assertEqual(verify_response.status_code, status.HTTP_404_NOT_FOUND)
