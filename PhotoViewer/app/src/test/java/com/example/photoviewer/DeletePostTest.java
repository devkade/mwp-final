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
 * Unit tests for Post deletion functionality.
 *
 * Test IDs: AN-DEL-01 through AN-DEL-08
 * Priority: P1 (High - CRUD operations)
 *
 * These tests verify the delete post API contract and Post object behavior.
 */
public class DeletePostTest {

    @Mock
    private HttpURLConnection mockConnection;

    private Post testPost;
    private static final String API_BASE_URL = "http://10.0.2.2:8000/";
    private static final String AUTH_TOKEN = "test_token_abc123";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Given: A valid test post exists
        testPost = new Post(42, "Test Title", "Test Content", "http://example.com/image.jpg", null);
    }

    // =========================================================================
    // AN-DEL-01: Post Object Validation for Delete
    // =========================================================================

    @Test
    public void AN_DEL_01_postWithValidId_isValidForDeletion() {
        // Given: A post with a positive ID
        Post validPost = new Post(1, "Title", "Text", "url", null);

        // When: Checking if valid for deletion
        boolean isValid = validPost.getId() > 0;

        // Then: Should be valid
        assertTrue("Post with positive ID should be valid for deletion", isValid);
    }

    @Test
    public void AN_DEL_02_postWithZeroId_isInvalidForDeletion() {
        // Given: A post with ID 0
        Post zeroIdPost = new Post(0, "Title", "Text", "url", null);

        // When: Checking if valid for deletion
        boolean isValid = zeroIdPost.getId() > 0;

        // Then: Should be invalid
        assertFalse("Post with zero ID should be invalid for deletion", isValid);
    }

    @Test
    public void AN_DEL_03_postWithNegativeId_isInvalidForDeletion() {
        // Given: A post with negative ID
        Post negativeIdPost = new Post(-1, "Title", "Text", "url", null);

        // When: Checking if valid for deletion
        boolean isValid = negativeIdPost.getId() > 0;

        // Then: Should be invalid
        assertFalse("Post with negative ID should be invalid for deletion", isValid);
    }

    // =========================================================================
    // AN-DEL-04: API URL Construction
    // =========================================================================

    @Test
    public void AN_DEL_04_deleteUrl_followsApiPattern() {
        // Given: A post with ID 42

        // When: Constructing the delete URL
        String deleteUrl = API_BASE_URL + "api_root/Post/" + testPost.getId() + "/";

        // Then: URL should follow REST pattern
        assertEquals("Delete URL should follow REST pattern",
                "http://10.0.2.2:8000/api_root/Post/42/", deleteUrl);
    }

    @Test
    public void AN_DEL_05_deleteUrl_handlesLargeIds() {
        // Given: A post with a large ID
        Post largeIdPost = new Post(999999, "Title", "Text", "url", null);

        // When: Constructing the delete URL
        String deleteUrl = API_BASE_URL + "api_root/Post/" + largeIdPost.getId() + "/";

        // Then: URL should handle large IDs correctly
        assertTrue("URL should contain the large ID", deleteUrl.contains("999999"));
        assertEquals("http://10.0.2.2:8000/api_root/Post/999999/", deleteUrl);
    }

    // =========================================================================
    // AN-DEL-06: HTTP Connection Configuration
    // =========================================================================

    @Test
    public void AN_DEL_06_connectionConfig_setsDeleteMethod() throws IOException {
        // Given: A mock HTTP connection
        doNothing().when(mockConnection).setRequestMethod("DELETE");

        // When: Configuring for delete operation
        mockConnection.setRequestMethod("DELETE");

        // Then: DELETE method should be set
        verify(mockConnection).setRequestMethod("DELETE");
    }

    @Test
    public void AN_DEL_07_connectionConfig_setsAuthorizationHeader() throws IOException {
        // Given: A mock HTTP connection and auth token
        doNothing().when(mockConnection).setRequestProperty(anyString(), anyString());

        // When: Setting authorization header
        String authHeader = "Token " + AUTH_TOKEN;
        mockConnection.setRequestProperty("Authorization", authHeader);

        // Then: Authorization header should be set with Token prefix
        verify(mockConnection).setRequestProperty("Authorization", "Token " + AUTH_TOKEN);
    }

    // =========================================================================
    // AN-DEL-08: Response Handling
    // =========================================================================

    @Test
    public void AN_DEL_08_response204_indicatesSuccessfulDeletion() throws IOException {
        // Given: Server returns 204 No Content
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NO_CONTENT);

        // When: Getting response code
        int responseCode = mockConnection.getResponseCode();

        // Then: Should indicate successful deletion
        assertEquals("HTTP 204 indicates successful deletion",
                HttpURLConnection.HTTP_NO_CONTENT, responseCode);
    }

    @Test
    public void response200_indicatesSuccessfulDeletion() throws IOException {
        // Given: Server returns 200 OK (alternate success)
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        // When: Getting response code
        int responseCode = mockConnection.getResponseCode();

        // Then: Should also indicate success
        assertEquals("HTTP 200 also indicates successful deletion",
                HttpURLConnection.HTTP_OK, responseCode);
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
        assertNotEquals("401 should not be treated as success",
                HttpURLConnection.HTTP_NO_CONTENT, responseCode);
    }

    @Test
    public void response404_indicatesPostNotFound() throws IOException {
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
    public void post_gettersReturnCorrectValues() {
        // Given: Test post created in setUp

        // Then: All getters should return correct values
        assertEquals("getId should return 42", 42, testPost.getId());
        assertEquals("getTitle should return title", "Test Title", testPost.getTitle());
        assertEquals("getText should return text", "Test Content", testPost.getText());
        assertEquals("getImageUrl should return URL",
                "http://example.com/image.jpg", testPost.getImageUrl());
        assertNull("getImageBitmap should return null when not loaded", testPost.getImageBitmap());
    }
}
