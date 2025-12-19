package com.example.photoviewer;

import com.example.photoviewer.models.MachineEvent;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the MachineEvent model class.
 *
 * Test IDs: AN-EVENT-01 through AN-EVENT-10
 * Priority: P0 (Critical - Core data model)
 */
public class MachineEventTest {

    @Test
    public void AN_EVENT_01_jsonParsing_parsesAllFields() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", 10);
        json.put("machine", 3);
        json.put("machine_name", "런닝머신 #1");
        json.put("event_type", "start");
        json.put("event_type_display", "사용 시작");
        json.put("image", "/media/events/2023/10/27/event_001.jpg");
        json.put("captured_at", "2023-10-27T14:35:10");
        json.put("person_count", 2);

        MachineEvent event = new MachineEvent(json);

        assertEquals("ID should be parsed", 10, event.getId());
        assertEquals("Machine ID should be parsed", 3, event.getMachineId());
        assertEquals("Machine name should be parsed", "런닝머신 #1", event.getMachineName());
        assertEquals("Event type should be parsed", "start", event.getEventType());
        assertEquals("Event type display should be parsed", "사용 시작", event.getEventTypeDisplay());
        assertEquals("Image URL should be parsed", "/media/events/2023/10/27/event_001.jpg", event.getImageUrl());
        assertEquals("Captured at should be parsed", "2023-10-27T14:35:10", event.getCapturedAt());
        assertEquals("Person count should be parsed", 2, event.getPersonCount());
    }

    @Test
    public void AN_EVENT_02_jsonParsing_supportsMachineIdField() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", 11);
        json.put("machine_id", 5);

        MachineEvent event = new MachineEvent(json);

        assertEquals("Machine ID should parse from machine_id", 5, event.getMachineId());
    }

    @Test(expected = JSONException.class)
    public void AN_EVENT_03_jsonParsing_throwsExceptionForMissingId() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("machine", 1);

        new MachineEvent(json);
    }

    @Test
    public void AN_EVENT_04_jsonParsing_usesDefaultsForOptionalFields() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", 1);

        MachineEvent event = new MachineEvent(json);

        assertEquals("Machine ID should default to 0", 0, event.getMachineId());
        assertEquals("Machine name should default to empty", "", event.getMachineName());
        assertEquals("Event type should default to empty", "", event.getEventType());
        assertEquals("Event type display should default to empty", "", event.getEventTypeDisplay());
        assertEquals("Image URL should default to empty", "", event.getImageUrl());
        assertEquals("Captured at should default to empty", "", event.getCapturedAt());
        assertEquals("Person count should default to 0", 0, event.getPersonCount());
    }

    @Test
    public void AN_EVENT_05_eventTypeDisplay_handlesKorean() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", 12);
        json.put("event_type_display", "사용 종료");

        MachineEvent event = new MachineEvent(json);

        assertEquals("Korean display text should be preserved", "사용 종료", event.getEventTypeDisplay());
    }

    @Test
    public void AN_EVENT_06_imageUrl_handlesAbsoluteUrl() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", 13);
        json.put("image", "https://example.com/events/1.jpg");

        MachineEvent event = new MachineEvent(json);

        assertEquals("Absolute URL should be preserved", "https://example.com/events/1.jpg", event.getImageUrl());
    }

    @Test
    public void AN_EVENT_07_personCount_handlesZero() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", 14);
        json.put("person_count", 0);

        MachineEvent event = new MachineEvent(json);

        assertEquals("Person count should be 0", 0, event.getPersonCount());
    }

    @Test
    public void AN_EVENT_08_personCount_handlesLargeValues() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", 15);
        json.put("person_count", 999);

        MachineEvent event = new MachineEvent(json);

        assertEquals("Large person count should be preserved", 999, event.getPersonCount());
    }

    @Test
    public void AN_EVENT_09_capturedAt_preservesTimestamp() throws JSONException {
        String timestamp = "2023-10-27T14:35:10.123Z";
        JSONObject json = new JSONObject();
        json.put("id", 16);
        json.put("captured_at", timestamp);

        MachineEvent event = new MachineEvent(json);

        assertEquals("Timestamp should be preserved", timestamp, event.getCapturedAt());
    }

    @Test
    public void AN_EVENT_10_setters_updateValues() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", 17);

        MachineEvent event = new MachineEvent(json);
        event.setMachineId(2);
        event.setMachineName("자전거");
        event.setEventType("end");
        event.setEventTypeDisplay("사용 종료");
        event.setImageUrl("/media/events/2023/10/27/event_010.jpg");
        event.setCapturedAt("2023-10-27T14:40:00");
        event.setPersonCount(1);

        assertEquals("Machine ID should update", 2, event.getMachineId());
        assertEquals("Machine name should update", "자전거", event.getMachineName());
        assertEquals("Event type should update", "end", event.getEventType());
        assertEquals("Event type display should update", "사용 종료", event.getEventTypeDisplay());
        assertEquals("Image URL should update", "/media/events/2023/10/27/event_010.jpg", event.getImageUrl());
        assertEquals("Captured at should update", "2023-10-27T14:40:00", event.getCapturedAt());
        assertEquals("Person count should update", 1, event.getPersonCount());
    }
}
