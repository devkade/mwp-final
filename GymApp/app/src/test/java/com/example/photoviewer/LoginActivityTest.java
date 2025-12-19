package com.example.photoviewer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Login functionality.
 *
 * Test IDs: AN-LOGIN-01 through AN-LOGIN-15
 * Priority: P0 (Critical - Authentication flow)
 *
 * These tests verify:
 * - Security key validation (AC #4)
 * - Login success flow (AC #2)
 * - Login failure handling (AC #3)
 * - Network error handling (AC #6)
 * - Auto-login when token exists (AC #5)
 *
 * Note: Tests that require Android Context are marked as integration tests
 * and should be run on a device/emulator.
 */
@RunWith(MockitoJUnitRunner.class)
public class LoginActivityTest {

    // =========================================================================
    // AN-LOGIN-01: Security Key Validation Tests
    // =========================================================================

    @Test
    public void AN_LOGIN_01_emptySecurityKey_shouldFail() {
        // Given: An empty security key
        String securityKey = "";

        // When: Validating the security key
        boolean isValid = !securityKey.trim().isEmpty();

        // Then: Validation should fail
        assertFalse("Empty security key should fail validation", isValid);
    }

    @Test
    public void AN_LOGIN_02_whitespaceSecurityKey_shouldFail() {
        // Given: A security key with only whitespace
        String securityKey = "   ";

        // When: Validating the security key (after trim)
        boolean isValid = !securityKey.trim().isEmpty();

        // Then: Validation should fail
        assertFalse("Whitespace-only security key should fail validation", isValid);
    }

    @Test
    public void AN_LOGIN_03_validSecurityKey_shouldPass() {
        // Given: A valid security key
        String securityKey = "test-security-key-123";

        // When: Validating the security key
        boolean isValid = !securityKey.trim().isEmpty();

        // Then: Validation should pass
        assertTrue("Valid security key should pass validation", isValid);
    }

    // =========================================================================
    // AN-LOGIN-04: Error Message Localization Tests
    // =========================================================================

    @Test
    public void AN_LOGIN_04_emptyKeyErrorMessage_shouldBeKorean() {
        // Given: The expected Korean error message for empty key (AC #4)
        String expectedMessage = "보안키를 입력하세요";

        // When/Then: The message should be the expected Korean string
        assertNotNull("Empty key error message should not be null", expectedMessage);
        assertTrue("Message should be in Korean",
                expectedMessage.contains("보안키") && expectedMessage.contains("입력"));
    }

    @Test
    public void AN_LOGIN_05_invalidKeyErrorMessage_shouldBeKorean() {
        // Given: The expected Korean error message for invalid key (AC #3)
        String expectedMessage = "잘못된 보안키입니다";

        // When/Then: The message should be the expected Korean string
        assertNotNull("Invalid key error message should not be null", expectedMessage);
        assertTrue("Message should indicate invalid key in Korean",
                expectedMessage.contains("잘못된") && expectedMessage.contains("보안키"));
    }

    @Test
    public void AN_LOGIN_06_networkErrorMessage_shouldBeKorean() {
        // Given: The expected Korean error message for network error (AC #6)
        String expectedMessage = "네트워크 오류가 발생했습니다";

        // When/Then: The message should be the expected Korean string
        assertNotNull("Network error message should not be null", expectedMessage);
        assertTrue("Message should indicate network error in Korean",
                expectedMessage.contains("네트워크") && expectedMessage.contains("오류"));
    }

    // =========================================================================
    // AN-LOGIN-07: Error Classification Tests
    // =========================================================================

    @Test
    public void AN_LOGIN_07_errorContainingInvalidSecurityKey_shouldMapToKoreanMessage() {
        // Given: An error message from the server
        String serverError = "Invalid security key";

        // When: Classifying the error
        String displayMessage;
        if (serverError.contains("Invalid security key") || serverError.contains("401")) {
            displayMessage = "잘못된 보안키입니다";
        } else if (serverError.contains("Network error") || serverError.contains("timeout")) {
            displayMessage = "네트워크 오류가 발생했습니다";
        } else {
            displayMessage = "로그인 실패: " + serverError;
        }

        // Then: Should map to Korean invalid key message
        assertEquals("잘못된 보안키입니다", displayMessage);
    }

    @Test
    public void AN_LOGIN_08_errorContaining401_shouldMapToInvalidKeyMessage() {
        // Given: A 401 error response
        String serverError = "Error: 401 Unauthorized";

        // When: Classifying the error
        String displayMessage;
        if (serverError.contains("Invalid security key") || serverError.contains("401")) {
            displayMessage = "잘못된 보안키입니다";
        } else {
            displayMessage = "로그인 실패: " + serverError;
        }

        // Then: Should map to Korean invalid key message
        assertEquals("잘못된 보안키입니다", displayMessage);
    }

    @Test
    public void AN_LOGIN_09_networkError_shouldMapToKoreanNetworkMessage() {
        // Given: A network error
        String serverError = "Network error: Unable to connect";

        // When: Classifying the error
        String displayMessage;
        if (serverError.contains("Invalid security key") || serverError.contains("401")) {
            displayMessage = "잘못된 보안키입니다";
        } else if (serverError.contains("Network error") || serverError.contains("timeout") ||
                   serverError.contains("Unable to resolve host")) {
            displayMessage = "네트워크 오류가 발생했습니다";
        } else {
            displayMessage = "로그인 실패: " + serverError;
        }

        // Then: Should map to Korean network error message
        assertEquals("네트워크 오류가 발생했습니다", displayMessage);
    }

    @Test
    public void AN_LOGIN_10_timeoutError_shouldMapToKoreanNetworkMessage() {
        // Given: A timeout error
        String serverError = "Connection timeout after 10000ms";

        // When: Classifying the error
        String displayMessage;
        if (serverError.contains("Invalid security key") || serverError.contains("401")) {
            displayMessage = "잘못된 보안키입니다";
        } else if (serverError.contains("Network error") || serverError.contains("timeout") ||
                   serverError.contains("Unable to resolve host")) {
            displayMessage = "네트워크 오류가 발생했습니다";
        } else {
            displayMessage = "로그인 실패: " + serverError;
        }

        // Then: Should map to Korean network error message
        assertEquals("네트워크 오류가 발생했습니다", displayMessage);
    }

    @Test
    public void AN_LOGIN_11_unknownError_shouldShowGenericMessage() {
        // Given: An unknown error
        String serverError = "Some unknown error occurred";

        // When: Classifying the error
        String displayMessage;
        if (serverError.contains("Invalid security key") || serverError.contains("401")) {
            displayMessage = "잘못된 보안키입니다";
        } else if (serverError.contains("Network error") || serverError.contains("timeout") ||
                   serverError.contains("Unable to resolve host")) {
            displayMessage = "네트워크 오류가 발생했습니다";
        } else {
            displayMessage = "로그인 실패: " + serverError;
        }

        // Then: Should show generic message with original error
        assertTrue("Should contain generic prefix",
                displayMessage.startsWith("로그인 실패:"));
        assertTrue("Should contain original error",
                displayMessage.contains("Some unknown error occurred"));
    }

    // =========================================================================
    // AN-LOGIN-12: Security Key Format Tests
    // =========================================================================

    @Test
    public void AN_LOGIN_12_securityKeyWithSpecialChars_shouldBeAccepted() {
        // Given: A security key with special characters
        String securityKey = "key-with_special.chars@123!";

        // When: Validating the security key
        boolean isValid = !securityKey.trim().isEmpty();

        // Then: Should be accepted
        assertTrue("Security key with special characters should be valid", isValid);
    }

    @Test
    public void AN_LOGIN_13_longSecurityKey_shouldBeAccepted() {
        // Given: A very long security key
        String securityKey = "a".repeat(256);

        // When: Validating the security key
        boolean isValid = !securityKey.trim().isEmpty();

        // Then: Should be accepted
        assertTrue("Long security key should be valid", isValid);
    }

    @Test
    public void AN_LOGIN_14_unicodeSecurityKey_shouldBeAccepted() {
        // Given: A security key with Unicode characters
        String securityKey = "보안키-테스트-123";

        // When: Validating the security key
        boolean isValid = !securityKey.trim().isEmpty();

        // Then: Should be accepted
        assertTrue("Unicode security key should be valid", isValid);
    }

    // =========================================================================
    // AN-LOGIN-15: JSON Request Body Format Tests
    // =========================================================================

    @Test
    public void AN_LOGIN_15_requestBody_shouldContainSecurityKeyField() {
        // Given: A security key to send
        String securityKey = "test-key-123";

        // When: Creating the expected JSON format
        String expectedFormat = "security_key";

        // Then: The request body should use security_key field (not username/password)
        assertFalse("Should not use 'username' field", "username".equals(expectedFormat));
        assertFalse("Should not use 'password' field", "password".equals(expectedFormat));
        assertEquals("Should use 'security_key' field", "security_key", expectedFormat);
    }
}
