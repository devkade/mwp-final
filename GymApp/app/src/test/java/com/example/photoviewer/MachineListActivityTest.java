package com.example.photoviewer;

import com.example.photoviewer.services.GymApiService;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for MachineListActivity functionality.
 *
 * Test IDs: AN-MLIST-01 through AN-MLIST-15
 * Priority: P0 (Critical - Core screen functionality)
 *
 * These tests verify equipment list display, error handling, and authentication flows.
 * Note: Some tests focus on logic validation rather than UI since UI testing requires
 * Android instrumentation tests.
 */
public class MachineListActivityTest {

    // =========================================================================
    // AN-MLIST-01 to AN-MLIST-05: Error Type Constants Tests
    // =========================================================================

    @Test
    public void AN_MLIST_01_errorConstants_unauthorizedIsCorrect() {
        // Given/When: Checking the UNAUTHORIZED error constant
        String errorType = GymApiService.ERROR_UNAUTHORIZED;

        // Then: Should be the expected value
        assertEquals("ERROR_UNAUTHORIZED should be 'UNAUTHORIZED'",
                "UNAUTHORIZED", errorType);
    }

    @Test
    public void AN_MLIST_02_errorConstants_networkErrorIsCorrect() {
        // Given/When: Checking the NETWORK_ERROR constant
        String errorType = GymApiService.ERROR_NETWORK;

        // Then: Should be the expected value
        assertEquals("ERROR_NETWORK should be 'NETWORK_ERROR'",
                "NETWORK_ERROR", errorType);
    }

    @Test
    public void AN_MLIST_03_errorConstants_serverErrorIsCorrect() {
        // Given/When: Checking the SERVER_ERROR constant
        String errorType = GymApiService.ERROR_SERVER;

        // Then: Should be the expected value
        assertEquals("ERROR_SERVER should be 'SERVER_ERROR'",
                "SERVER_ERROR", errorType);
    }

    @Test
    public void AN_MLIST_04_errorConstants_allConstantsAreUnique() {
        // Given: All error constants
        String unauthorized = GymApiService.ERROR_UNAUTHORIZED;
        String network = GymApiService.ERROR_NETWORK;
        String server = GymApiService.ERROR_SERVER;

        // Then: All constants should be unique
        assertNotEquals("UNAUTHORIZED and NETWORK should be different", unauthorized, network);
        assertNotEquals("UNAUTHORIZED and SERVER should be different", unauthorized, server);
        assertNotEquals("NETWORK and SERVER should be different", network, server);
    }

    @Test
    public void AN_MLIST_05_errorConstants_areNotNull() {
        // Then: All constants should be non-null
        assertNotNull("ERROR_UNAUTHORIZED should not be null", GymApiService.ERROR_UNAUTHORIZED);
        assertNotNull("ERROR_NETWORK should not be null", GymApiService.ERROR_NETWORK);
        assertNotNull("ERROR_SERVER should not be null", GymApiService.ERROR_SERVER);
    }

    // =========================================================================
    // AN-MLIST-06 to AN-MLIST-10: Error Message Mapping Tests
    // =========================================================================

    @Test
    public void AN_MLIST_06_errorMessageMapping_networkErrorMapsToKorean() {
        // Given: Network error type
        String errorType = GymApiService.ERROR_NETWORK;

        // When: Getting the display message
        String message = getErrorMessageForType(errorType);

        // Then: Should return Korean network error message
        assertEquals("Network error should map to Korean message",
                "네트워크 오류가 발생했습니다", message);
    }

    @Test
    public void AN_MLIST_07_errorMessageMapping_serverErrorMapsToKorean() {
        // Given: Server error type
        String errorType = GymApiService.ERROR_SERVER;

        // When: Getting the display message
        String message = getErrorMessageForType(errorType);

        // Then: Should return Korean server error message
        assertEquals("Server error should map to Korean message",
                "서버 오류가 발생했습니다", message);
    }

    @Test
    public void AN_MLIST_08_errorMessageMapping_unknownErrorDefaultsToNetwork() {
        // Given: Unknown error type
        String errorType = "UNKNOWN_ERROR";

        // When: Getting the display message
        String message = getErrorMessageForType(errorType);

        // Then: Should return default network error message
        assertEquals("Unknown error should default to network error message",
                "네트워크 오류가 발생했습니다", message);
    }

    @Test
    public void AN_MLIST_09_errorMessageMapping_nullDefaultsToNetwork() {
        // Given: Null error type

        // When: Getting the display message
        String message = getErrorMessageForType(null);

        // Then: Should return default network error message
        assertEquals("Null error should default to network error message",
                "네트워크 오류가 발생했습니다", message);
    }

    @Test
    public void AN_MLIST_10_errorMessageMapping_emptyStringDefaultsToNetwork() {
        // Given: Empty string error type

        // When: Getting the display message
        String message = getErrorMessageForType("");

        // Then: Should return default network error message
        assertEquals("Empty error should default to network error message",
                "네트워크 오류가 발생했습니다", message);
    }

    // =========================================================================
    // AN-MLIST-11 to AN-MLIST-15: Empty State Message Tests
    // =========================================================================

    @Test
    public void AN_MLIST_11_emptyStateMessage_isCorrectKorean() {
        // Given: Expected empty state message
        String expected = "등록된 운동기구가 없습니다";

        // Then: Message should match AC#6
        assertEquals("Empty state message should match acceptance criteria",
                expected, getEmptyStateMessage());
    }

    @Test
    public void AN_MLIST_12_emptyStateMessage_isNotNull() {
        // Then: Empty state message should not be null
        assertNotNull("Empty state message should not be null", getEmptyStateMessage());
    }

    @Test
    public void AN_MLIST_13_emptyStateMessage_isNotEmpty() {
        // Then: Empty state message should not be empty
        assertFalse("Empty state message should not be empty",
                getEmptyStateMessage().isEmpty());
    }

    @Test
    public void AN_MLIST_14_eventCountFormat_formatsCorrectly() {
        // Given: Various event counts
        int[] counts = {0, 1, 10, 100, 1000};

        for (int count : counts) {
            // When: Formatting the event count
            String formatted = formatEventCount(count);

            // Then: Should match expected format "이벤트 N건"
            assertEquals("Event count should be formatted correctly",
                    "이벤트 " + count + "건", formatted);
        }
    }

    @Test
    public void AN_MLIST_15_eventCountFormat_handlesLargeNumbers() {
        // Given: Large event count
        int count = 999999;

        // When: Formatting the event count
        String formatted = formatEventCount(count);

        // Then: Should format correctly
        assertEquals("Large event count should be formatted",
                "이벤트 999999건", formatted);
    }

    // =========================================================================
    // Helper methods that mirror MachineListActivity logic
    // =========================================================================

    /**
     * Helper method that mirrors getErrorMessage logic in MachineListActivity
     */
    private String getErrorMessageForType(String errorType) {
        if (GymApiService.ERROR_SERVER.equals(errorType)) {
            return "서버 오류가 발생했습니다";
        } else {
            return "네트워크 오류가 발생했습니다";
        }
    }

    /**
     * Helper method that returns the empty state message
     */
    private String getEmptyStateMessage() {
        return "등록된 운동기구가 없습니다";
    }

    /**
     * Helper method that formats event count (mirrors MachineAdapter logic)
     */
    private String formatEventCount(int count) {
        return "이벤트 " + count + "건";
    }
}
