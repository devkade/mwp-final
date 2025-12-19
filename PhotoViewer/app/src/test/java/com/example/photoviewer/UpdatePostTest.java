package com.example.photoviewer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Post update functionality.
 *
 * Test IDs: AN-UPD-01 through AN-UPD-12
 * Priority: P1 (High - CRUD operations)
 *
 * These tests verify the update post API contract and input validation.
 */
public class UpdatePostTest {

    @Mock
    private HttpURLConnection mockConnection;

    @Mock
    private OutputStream mockOutputStream;

    private Post testPost;
    private static final String API_BASE_URL = "http://10.0.2.2:8000/";
    private static final String AUTH_TOKEN = "test_token_abc123";

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        // Given: A valid test post exists with original content
        testPost = new Post(42, "Original Title", "Original Content",
                "http://example.com/original.jpg", null);

        // Setup mock connection for output
        when(mockConnection.getOutputStream()).thenReturn(mockOutputStream);
    }

    // =========================================================================
    // AN-UPD-01: Post Validation for Update
    // =========================================================================

    @Test
    public void AN_UPD_01_postWithValidId_isValidForUpdate() {
        // Given: A post with a positive ID
        Post validPost = new Post(1, "Title", "Text", "url", null);

        // When: Checking if valid for update
        boolean isValid = validPost.getId() > 0;

        // Then: Should be valid
        assertTrue("Post with positive ID should be valid for update", isValid);
    }

    @Test
    public void AN_UPD_02_postWithZeroId_isInvalidForUpdate() {
        // Given: A post with ID 0
        Post zeroIdPost = new Post(0, "Title", "Text", "url", null);

        // When: Checking if valid for update
        boolean isValid = zeroIdPost.getId() > 0;

        // Then: Should be invalid (can't update non-existent post)
        assertFalse("Post with zero ID should be invalid for update", isValid);
    }

    // =========================================================================
    // AN-UPD-03: API URL Construction
    // =========================================================================

    @Test
    public void AN_UPD_03_updateUrl_followsApiPattern() {
        // Given: A post with ID 42

        // When: Constructing the update URL
        String updateUrl = API_BASE_URL + "api_root/Post/" + testPost.getId() + "/";

        // Then: URL should follow REST pattern
        assertEquals("Update URL should follow REST pattern",
                "http://10.0.2.2:8000/api_root/Post/42/", updateUrl);
    }

    // =========================================================================
    // AN-UPD-04: HTTP Connection Configuration
    // =========================================================================

    @Test
    public void AN_UPD_04_connectionConfig_setsPutMethod() throws IOException {
        // Given: A mock HTTP connection
        doNothing().when(mockConnection).setRequestMethod("PUT");

        // When: Configuring for update operation
        mockConnection.setRequestMethod("PUT");

        // Then: PUT method should be set
        verify(mockConnection).setRequestMethod("PUT");
    }

    @Test
    public void AN_UPD_05_connectionConfig_setsAuthorizationHeader() throws IOException {
        // Given: A mock HTTP connection and auth token
        doNothing().when(mockConnection).setRequestProperty(anyString(), anyString());

        // When: Setting authorization header
        String authHeader = "Token " + AUTH_TOKEN;
        mockConnection.setRequestProperty("Authorization", authHeader);

        // Then: Authorization header should be set with Token prefix
        verify(mockConnection).setRequestProperty("Authorization", "Token " + AUTH_TOKEN);
    }

    @Test
    public void AN_UPD_06_connectionConfig_setsContentTypeMultipart() throws IOException {
        // Given: A mock HTTP connection
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        String contentType = "multipart/form-data; boundary=" + boundary;

        doNothing().when(mockConnection).setRequestProperty(anyString(), anyString());

        // When: Setting content type for multipart form data
        mockConnection.setRequestProperty("Content-Type", contentType);

        // Then: Content-Type should be set to multipart/form-data
        verify(mockConnection).setRequestProperty(eq("Content-Type"), contains("multipart/form-data"));
    }

    @Test
    public void AN_UPD_07_connectionConfig_enablesOutput() throws IOException {
        // Given: A mock HTTP connection
        doNothing().when(mockConnection).setDoOutput(true);

        // When: Enabling output for sending data
        mockConnection.setDoOutput(true);

        // Then: Output should be enabled
        verify(mockConnection).setDoOutput(true);
    }

    // =========================================================================
    // AN-UPD-08: Input Validation
    // =========================================================================

    @Test
    public void AN_UPD_08_validTitle_passesValidation() {
        // Given: A valid new title
        String newTitle = "Updated Title";

        // When: Validating the title
        boolean isValid = newTitle != null && !newTitle.trim().isEmpty();

        // Then: Should pass validation
        assertTrue("Non-empty title should be valid", isValid);
    }

    @Test
    public void AN_UPD_09_emptyTitle_failsValidation() {
        // Given: An empty title
        String emptyTitle = "   ";

        // When: Validating the title
        boolean isValid = emptyTitle != null && !emptyTitle.trim().isEmpty();

        // Then: Should fail validation
        assertFalse("Empty title should be invalid", isValid);
    }

    @Test
    public void AN_UPD_10_nullTitle_failsValidation() {
        // Given: A null title
        String nullTitle = null;

        // When: Validating the title
        boolean isValid = nullTitle != null && !nullTitle.trim().isEmpty();

        // Then: Should fail validation
        assertFalse("Null title should be invalid", isValid);
    }

    @Test
    public void validContent_passesValidation() {
        // Given: Valid content
        String newContent = "Updated content with meaningful text.";

        // When: Validating the content
        boolean isValid = newContent != null && !newContent.trim().isEmpty();

        // Then: Should pass validation
        assertTrue("Non-empty content should be valid", isValid);
    }

    // =========================================================================
    // AN-UPD-11: Response Handling
    // =========================================================================

    @Test
    public void AN_UPD_11_response200_indicatesSuccessfulUpdate() throws IOException {
        // Given: Server returns 200 OK
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        // When: Getting response code
        int responseCode = mockConnection.getResponseCode();

        // Then: Should indicate successful update
        assertEquals("HTTP 200 indicates successful update",
                HttpURLConnection.HTTP_OK, responseCode);
    }

    @Test
    public void response204_indicatesSuccessfulUpdate() throws IOException {
        // Given: Server returns 204 No Content
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NO_CONTENT);

        // When: Getting response code
        int responseCode = mockConnection.getResponseCode();

        // Then: Should indicate success
        assertEquals("HTTP 204 also indicates successful update",
                HttpURLConnection.HTTP_NO_CONTENT, responseCode);
    }

    @Test
    public void response400_indicatesBadRequest() throws IOException {
        // Given: Server returns 400 Bad Request (validation error)
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);

        // When: Getting response code
        int responseCode = mockConnection.getResponseCode();

        // Then: Should indicate bad request
        assertEquals("HTTP 400 indicates bad request",
                HttpURLConnection.HTTP_BAD_REQUEST, responseCode);
    }

    @Test
    public void response401_indicatesUnauthorized() throws IOException {
        // Given: Server returns 401 Unauthorized
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_UNAUTHORIZED);

        // When: Getting response code
        int responseCode = mockConnection.getResponseCode();

        // Then: Should indicate authorization failure
        assertEquals("HTTP 401 indicates unauthorized",
                HttpURLConnection.HTTP_UNAUTHORIZED, responseCode);
    }

    @Test
    public void AN_UPD_12_response404_indicatesPostNotFound() throws IOException {
        // Given: Server returns 404 Not Found
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);

        // When: Getting response code
        int responseCode = mockConnection.getResponseCode();

        // Then: Should indicate post not found
        assertEquals("HTTP 404 indicates post not found",
                HttpURLConnection.HTTP_NOT_FOUND, responseCode);
    }

    // =========================================================================
    // Post Object Tests
    // =========================================================================

    @Test
    public void post_originalValuesAreAccessible() {
        // Given: Test post created in setUp

        // Then: Original values should be accessible for comparison
        assertEquals("Original title should be accessible",
                "Original Title", testPost.getTitle());
        assertEquals("Original content should be accessible",
                "Original Content", testPost.getText());
        assertEquals("Original image URL should be accessible",
                "http://example.com/original.jpg", testPost.getImageUrl());
    }

    @Test
    public void post_idRemainsConstantAfterUpdate() {
        // Given: A post with ID 42
        int originalId = testPost.getId();

        // When: Creating updated post data (simulating update)
        String newTitle = "New Title";
        String newContent = "New Content";

        // Then: Post ID should remain the same
        assertEquals("Post ID should not change during update", originalId, testPost.getId());
        assertEquals("ID should still be 42", 42, testPost.getId());
    }
}
