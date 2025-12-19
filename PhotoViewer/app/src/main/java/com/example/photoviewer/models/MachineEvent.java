package com.example.photoviewer.models;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * MachineEvent - Data model for equipment usage events.
 *
 * Represents a usage event for a specific gym machine.
 */
public class MachineEvent {
    private int id;
    private int machineId;
    private String machineName;
    private String eventType;
    private String eventTypeDisplay;
    private String imageUrl;
    private String capturedAt;
    private int personCount;

    /**
     * Constructor from JSONObject
     * Parses API response JSON into MachineEvent object.
     *
     * @param json JSONObject from API response
     * @throws JSONException if required fields are missing
     */
    public MachineEvent(JSONObject json) throws JSONException {
        this.id = json.getInt("id");
        int machineValue = json.has("machine") ? json.optInt("machine", 0) : 0;
        int machineIdValue = json.optInt("machine_id", 0);
        this.machineId = machineValue != 0 ? machineValue : machineIdValue;
        this.machineName = json.optString("machine_name", "");
        this.eventType = json.optString("event_type", "");
        this.eventTypeDisplay = json.optString("event_type_display", "");
        this.imageUrl = json.optString("image", "");
        this.capturedAt = json.optString("captured_at", "");
        this.personCount = json.optInt("person_count", 0);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMachineId() {
        return machineId;
    }

    public void setMachineId(int machineId) {
        this.machineId = machineId;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventTypeDisplay() {
        return eventTypeDisplay;
    }

    public void setEventTypeDisplay(String eventTypeDisplay) {
        this.eventTypeDisplay = eventTypeDisplay;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(String capturedAt) {
        this.capturedAt = capturedAt;
    }

    public int getPersonCount() {
        return personCount;
    }

    public void setPersonCount(int personCount) {
        this.personCount = personCount;
    }
}
