package com.example.photoviewer;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the Post model class.
 *
 * Test IDs: AN-POST-01 through AN-POST-12
 * Priority: P0 (Critical - Core data model)
 *
 * These tests verify the Post class construction, getters, and edge cases.
 */
public class PostModelTest {

    private Post testPost;

    @Before
    public void setUp() {
        // Given: A standard test post
        testPost = new Post(1, "Test Title", "Test Content",
                "http://example.com/image.jpg", null);
    }

    // =========================================================================
    // AN-POST-01: Constructor Tests
    // =========================================================================

    @Test
    public void AN_POST_01_constructor_createsPostWithAllFields() {
        // Given/When: Creating a post with all fields
        Post post = new Post(42, "Title", "Content", "http://image.url", null);

        // Then: All fields should be set correctly
        assertEquals("ID should be set", 42, post.getId());
        assertEquals("Title should be set", "Title", post.getTitle());
        assertEquals("Content should be set", "Content", post.getText());
        assertEquals("Image URL should be set", "http://image.url", post.getImageUrl());
        assertNull("Bitmap should be null initially", post.getImageBitmap());
    }

    @Test
    public void AN_POST_02_constructor_handlesNullTitle() {
        // Given/When: Creating a post with null title
        Post post = new Post(1, null, "Content", "url", null);

        // Then: Title should be null
        assertNull("Null title should be stored as null", post.getTitle());
    }

    @Test
    public void AN_POST_03_constructor_handlesNullContent() {
        // Given/When: Creating a post with null content
        Post post = new Post(1, "Title", null, "url", null);

        // Then: Content should be null
        assertNull("Null content should be stored as null", post.getText());
    }

    @Test
    public void AN_POST_04_constructor_handlesNullImageUrl() {
        // Given/When: Creating a post with null image URL
        Post post = new Post(1, "Title", "Content", null, null);

        // Then: Image URL should be null
        assertNull("Null image URL should be stored as null", post.getImageUrl());
    }

    // =========================================================================
    // AN-POST-05: ID Validation Tests
    // =========================================================================

    @Test
    public void AN_POST_05_getId_returnsCorrectValue() {
        // Given: Test post from setUp

        // Then: getId should return the correct ID
        assertEquals("getId should return 1", 1, testPost.getId());
    }

    @Test
    public void AN_POST_06_id_handlesZero() {
        // Given/When: Creating a post with ID 0
        Post post = new Post(0, "Title", "Content", "url", null);

        // Then: ID should be 0
        assertEquals("ID 0 should be stored", 0, post.getId());
    }

    @Test
    public void AN_POST_07_id_handlesNegativeValues() {
        // Given/When: Creating a post with negative ID
        Post post = new Post(-1, "Title", "Content", "url", null);

        // Then: Negative ID should be stored
        assertEquals("Negative ID should be stored", -1, post.getId());
    }

    @Test
    public void AN_POST_08_id_handlesLargeValues() {
        // Given/When: Creating a post with large ID
        Post post = new Post(Integer.MAX_VALUE, "Title", "Content", "url", null);

        // Then: Large ID should be stored
        assertEquals("Large ID should be stored", Integer.MAX_VALUE, post.getId());
    }

    // =========================================================================
    // AN-POST-09: String Field Tests
    // =========================================================================

    @Test
    public void AN_POST_09_getTitle_returnsCorrectValue() {
        // Given: Test post from setUp

        // Then: getTitle should return the correct title
        assertEquals("getTitle should return correct value",
                "Test Title", testPost.getTitle());
    }

    @Test
    public void AN_POST_10_getText_returnsCorrectValue() {
        // Given: Test post from setUp

        // Then: getText should return the correct content
        assertEquals("getText should return correct value",
                "Test Content", testPost.getText());
    }

    @Test
    public void AN_POST_11_getImageUrl_returnsCorrectValue() {
        // Given: Test post from setUp

        // Then: getImageUrl should return the correct URL
        assertEquals("getImageUrl should return correct value",
                "http://example.com/image.jpg", testPost.getImageUrl());
    }

    // =========================================================================
    // AN-POST-12: Edge Cases
    // =========================================================================

    @Test
    public void AN_POST_12_emptyStrings_areStoredCorrectly() {
        // Given/When: Creating a post with empty strings
        Post post = new Post(1, "", "", "", null);

        // Then: Empty strings should be stored
        assertEquals("Empty title should be stored", "", post.getTitle());
        assertEquals("Empty content should be stored", "", post.getText());
        assertEquals("Empty URL should be stored", "", post.getImageUrl());
    }

    @Test
    public void title_handlesSpecialCharacters() {
        // Given: Korean and special characters
        String koreanTitle = "테스트 제목 - Test 123!@#$%";

        // When: Creating a post with special characters
        Post post = new Post(1, koreanTitle, "Content", "url", null);

        // Then: Special characters should be preserved
        assertEquals("Special characters should be preserved",
                koreanTitle, post.getTitle());
    }

    @Test
    public void content_handlesMultilineText() {
        // Given: Multiline content
        String multiline = "Line 1\nLine 2\nLine 3";

        // When: Creating a post with multiline content
        Post post = new Post(1, "Title", multiline, "url", null);

        // Then: Newlines should be preserved
        assertTrue("Content should contain newlines",
                post.getText().contains("\n"));
        assertEquals("Multiline content should be preserved", multiline, post.getText());
    }

    @Test
    public void imageUrl_handlesHttpsUrls() {
        // Given: HTTPS URL
        String httpsUrl = "https://secure.example.com/image.jpg";

        // When: Creating a post with HTTPS URL
        Post post = new Post(1, "Title", "Content", httpsUrl, null);

        // Then: HTTPS URL should be stored
        assertTrue("URL should start with https",
                post.getImageUrl().startsWith("https://"));
    }

    @Test
    public void imageUrl_handlesUrlWithQueryParams() {
        // Given: URL with query parameters
        String urlWithParams = "http://example.com/image.jpg?width=300&height=200";

        // When: Creating a post with URL containing query params
        Post post = new Post(1, "Title", "Content", urlWithParams, null);

        // Then: URL with params should be stored
        assertEquals("URL with query params should be preserved",
                urlWithParams, post.getImageUrl());
    }
}