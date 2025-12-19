package com.example.photoviewer;

import com.example.photoviewer.models.GymMachine;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the GymMachine model class.
 *
 * Test IDs: AN-MACHINE-01 through AN-MACHINE-15
 * Priority: P0 (Critical - Core data model)
 *
 * These tests verify GymMachine JSON parsing, field extraction, and edge cases.
 */
public class GymMachineTest {

    // =========================================================================
    // AN-MACHINE-01 to AN-MACHINE-05: Basic JSON Parsing Tests
    // =========================================================================

    @Test
    public void AN_MACHINE_01_jsonParsing_parsesAllFields() throws JSONException {
        // Given: A complete JSON object
        JSONObject json = new JSONObject();
        json.put("id", 1);
        json.put("name", "런닝머신 #1");
        json.put("machine_type", "treadmill");
        json.put("location", "1층 A구역");
        json.put("description", "ProForm 상업용 런닝머신");
        json.put("thumbnail", "/media/machines/treadmill1.jpg");
        json.put("is_active", true);
        json.put("event_count", 152);

        // When: Parsing the JSON
        GymMachine machine = new GymMachine(json);

        // Then: All fields should be correctly parsed
        assertEquals("ID should be parsed", 1, machine.getId());
        assertEquals("Name should be parsed", "런닝머신 #1", machine.getName());
        assertEquals("Machine type should be parsed", "treadmill", machine.getMachineType());
        assertEquals("Location should be parsed", "1층 A구역", machine.getLocation());
        assertEquals("Description should be parsed", "ProForm 상업용 런닝머신", machine.getDescription());
        assertEquals("Thumbnail URL should be parsed", "/media/machines/treadmill1.jpg", machine.getThumbnailUrl());
        assertTrue("isActive should be true", machine.isActive());
        assertEquals("Event count should be parsed", 152, machine.getEventCount());
    }

    @Test
    public void AN_MACHINE_02_jsonParsing_parsesLastEventObject() throws JSONException {
        // Given: JSON with last_event nested object
        JSONObject lastEvent = new JSONObject();
        lastEvent.put("event_type", "end");
        lastEvent.put("captured_at", "2023-10-27T15:21:03");

        JSONObject json = new JSONObject();
        json.put("id", 1);
        json.put("name", "Test Machine");
        json.put("last_event", lastEvent);

        // When: Parsing the JSON
        GymMachine machine = new GymMachine(json);

        // Then: LastEvent should be correctly parsed
        assertNotNull("LastEvent should not be null", machine.getLastEvent());
        assertEquals("Event type should be parsed", "end", machine.getLastEvent().getEventType());
        assertEquals("Captured at should be parsed", "2023-10-27T15:21:03", machine.getLastEvent().getCapturedAt());
    }

    @Test
    public void AN_MACHINE_03_jsonParsing_handlesNullLastEvent() throws JSONException {
        // Given: JSON without last_event
        JSONObject json = new JSONObject();
        json.put("id", 1);
        json.put("name", "Test Machine");

        // When: Parsing the JSON
        GymMachine machine = new GymMachine(json);

        // Then: LastEvent should be null
        assertNull("LastEvent should be null when not present", machine.getLastEvent());
    }

    @Test(expected = JSONException.class)
    public void AN_MACHINE_04_jsonParsing_throwsExceptionForMissingId() throws JSONException {
        // Given: JSON without required 'id' field
        JSONObject json = new JSONObject();
        json.put("name", "Test Machine");

        // When/Then: Parsing should throw JSONException
        new GymMachine(json);
    }

    @Test
    public void AN_MACHINE_05_jsonParsing_usesDefaultsForOptionalFields() throws JSONException {
        // Given: JSON with only required id field
        JSONObject json = new JSONObject();
        json.put("id", 1);

        // When: Parsing the JSON
        GymMachine machine = new GymMachine(json);

        // Then: Optional fields should have default values
        assertEquals("Name should default to empty string", "", machine.getName());
        assertEquals("Machine type should default to empty string", "", machine.getMachineType());
        assertEquals("Location should default to empty string", "", machine.getLocation());
        assertEquals("Description should default to empty string", "", machine.getDescription());
        assertEquals("Thumbnail should default to empty string", "", machine.getThumbnailUrl());
        assertTrue("isActive should default to true", machine.isActive());
        assertEquals("Event count should default to 0", 0, machine.getEventCount());
    }

    // =========================================================================
    // AN-MACHINE-06 to AN-MACHINE-10: Edge Case Tests
    // =========================================================================

    @Test
    public void AN_MACHINE_06_eventCount_handlesZero() throws JSONException {
        // Given: JSON with zero event count
        JSONObject json = new JSONObject();
        json.put("id", 1);
        json.put("event_count", 0);

        // When: Parsing the JSON
        GymMachine machine = new GymMachine(json);

        // Then: Event count should be 0
        assertEquals("Event count should be 0", 0, machine.getEventCount());
    }

    @Test
    public void AN_MACHINE_07_eventCount_handlesLargeValues() throws JSONException {
        // Given: JSON with large event count
        JSONObject json = new JSONObject();
        json.put("id", 1);
        json.put("event_count", 999999);

        // When: Parsing the JSON
        GymMachine machine = new GymMachine(json);

        // Then: Large event count should be stored
        assertEquals("Large event count should be stored", 999999, machine.getEventCount());
    }

    @Test
    public void AN_MACHINE_08_isActive_handlesFalse() throws JSONException {
        // Given: JSON with is_active = false
        JSONObject json = new JSONObject();
        json.put("id", 1);
        json.put("is_active", false);

        // When: Parsing the JSON
        GymMachine machine = new GymMachine(json);

        // Then: isActive should be false
        assertFalse("isActive should be false", machine.isActive());
    }

    @Test
    public void AN_MACHINE_09_name_handlesKoreanCharacters() throws JSONException {
        // Given: JSON with Korean name
        String koreanName = "런닝머신 #1 - 테스트";
        JSONObject json = new JSONObject();
        json.put("id", 1);
        json.put("name", koreanName);

        // When: Parsing the JSON
        GymMachine machine = new GymMachine(json);

        // Then: Korean characters should be preserved
        assertEquals("Korean characters should be preserved", koreanName, machine.getName());
    }

    @Test
    public void AN_MACHINE_10_thumbnailUrl_handlesAbsoluteUrl() throws JSONException {
        // Given: JSON with absolute thumbnail URL
        String absoluteUrl = "https://example.com/images/machine.jpg";
        JSONObject json = new JSONObject();
        json.put("id", 1);
        json.put("thumbnail", absoluteUrl);

        // When: Parsing the JSON
        GymMachine machine = new GymMachine(json);

        // Then: Absolute URL should be stored
        assertEquals("Absolute URL should be preserved", absoluteUrl, machine.getThumbnailUrl());
    }

    // =========================================================================
    // AN-MACHINE-11 to AN-MACHINE-15: LastEvent Tests
    // =========================================================================

    @Test
    public void AN_MACHINE_11_lastEvent_fromJson_returnsNullForNullInput() {
        // When: Creating LastEvent from null
        GymMachine.LastEvent lastEvent = GymMachine.LastEvent.fromJson(null);

        // Then: Should return null
        assertNull("fromJson should return null for null input", lastEvent);
    }

    @Test
    public void AN_MACHINE_12_lastEvent_parsesEventTypes() throws JSONException {
        // Test different event types
        String[] eventTypes = {"start", "end", "pause", "resume"};

        for (String eventType : eventTypes) {
            // Given: JSON with specific event type
            JSONObject json = new JSONObject();
            json.put("event_type", eventType);
            json.put("captured_at", "2023-10-27T15:00:00");

            // When: Parsing
            GymMachine.LastEvent lastEvent = GymMachine.LastEvent.fromJson(json);

            // Then: Event type should be correctly parsed
            assertNotNull("LastEvent should not be null", lastEvent);
            assertEquals("Event type should be " + eventType, eventType, lastEvent.getEventType());
        }
    }

    @Test
    public void AN_MACHINE_13_lastEvent_handlesEmptyEventType() throws JSONException {
        // Given: JSON with empty event_type
        JSONObject json = new JSONObject();
        json.put("event_type", "");
        json.put("captured_at", "2023-10-27T15:00:00");

        // When: Parsing
        GymMachine.LastEvent lastEvent = GymMachine.LastEvent.fromJson(json);

        // Then: Empty string should be stored
        assertNotNull("LastEvent should not be null", lastEvent);
        assertEquals("Empty event type should be preserved", "", lastEvent.getEventType());
    }

    @Test
    public void AN_MACHINE_14_lastEvent_parsesDifferentTimestampFormats() throws JSONException {
        // Given: Various timestamp formats
        String[] timestamps = {
                "2023-10-27T15:21:03",
                "2023-10-27T15:21:03.123",
                "2023-10-27T15:21:03Z"
        };

        for (String timestamp : timestamps) {
            JSONObject json = new JSONObject();
            json.put("event_type", "end");
            json.put("captured_at", timestamp);

            // When: Parsing
            GymMachine.LastEvent lastEvent = GymMachine.LastEvent.fromJson(json);

            // Then: Timestamp should be preserved exactly
            assertNotNull("LastEvent should not be null", lastEvent);
            assertEquals("Timestamp should be preserved", timestamp, lastEvent.getCapturedAt());
        }
    }

    @Test
    public void AN_MACHINE_15_lastEvent_handlesMissingFields() throws JSONException {
        // Given: Empty JSON object for last_event
        JSONObject json = new JSONObject();

        // When: Parsing
        GymMachine.LastEvent lastEvent = GymMachine.LastEvent.fromJson(json);

        // Then: Should return LastEvent with default empty strings
        assertNotNull("LastEvent should not be null", lastEvent);
        assertEquals("Event type should default to empty", "", lastEvent.getEventType());
        assertEquals("Captured at should default to empty", "", lastEvent.getCapturedAt());
    }
}
