package com.example.photoviewer.models;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * GymMachine - Data model for gym equipment
 *
 * Represents a gym machine with its properties and last event information.
 * Used to display equipment in the Equipment List screen.
 */
public class GymMachine {
    private final int id;
    private final String name;
    private final String machineType;
    private final String location;
    private final String description;
    private final String thumbnailUrl;
    private final boolean isActive;
    private final int eventCount;
    private final LastEvent lastEvent;

    /**
     * Nested class representing the last usage event for this machine
     */
    public static class LastEvent {
        private final String eventType;
        private final String capturedAt;

        public LastEvent(String eventType, String capturedAt) {
            this.eventType = eventType;
            this.capturedAt = capturedAt;
        }

        /**
         * Parse LastEvent from JSON object
         * @param json JSONObject containing last_event data
         * @return LastEvent instance or null if json is null
         */
        public static LastEvent fromJson(JSONObject json) {
            if (json == null) {
                return null;
            }
            try {
                String eventType = json.optString("event_type", "");
                String capturedAt = json.optString("captured_at", "");
                return new LastEvent(eventType, capturedAt);
            } catch (Exception e) {
                return null;
            }
        }

        public String getEventType() {
            return eventType;
        }

        public String getCapturedAt() {
            return capturedAt;
        }
    }

    /**
     * Constructor from JSONObject
     * Parses API response JSON into GymMachine object
     *
     * @param json JSONObject from API response
     * @throws JSONException if required fields are missing
     */
    public GymMachine(JSONObject json) throws JSONException {
        this.id = json.getInt("id");
        this.name = json.optString("name", "");
        this.machineType = json.optString("machine_type", "");
        this.location = json.optString("location", "");
        this.description = json.optString("description", "");
        this.thumbnailUrl = json.optString("thumbnail", "");
        this.isActive = json.optBoolean("is_active", true);
        this.eventCount = json.optInt("event_count", 0);

        // Parse nested last_event object
        JSONObject lastEventJson = json.optJSONObject("last_event");
        this.lastEvent = LastEvent.fromJson(lastEventJson);
    }

    // Getters

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMachineType() {
        return machineType;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getEventCount() {
        return eventCount;
    }

    public LastEvent getLastEvent() {
        return lastEvent;
    }
}
