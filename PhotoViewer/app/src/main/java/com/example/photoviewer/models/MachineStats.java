package com.example.photoviewer.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * MachineStats - Data model for equipment usage statistics.
 *
 * Represents aggregated usage statistics for a specific gym machine
 * within a date range.
 */
public class MachineStats {
    private int machineId;
    private String machineName;
    private int totalStarts;
    private int totalEnds;
    private List<DailyUsage> dailyUsage;

    /**
     * DailyUsage - Nested class representing daily usage count.
     */
    public static class DailyUsage {
        private String date;  // "2024-01-15" format
        private int count;

        public DailyUsage(String date, int count) {
            this.date = date;
            this.count = count;
        }

        public String getDate() {
            return date;
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * Constructor from JSONObject
     * Parses API response JSON into MachineStats object.
     *
     * @param json JSONObject from API response
     * @throws JSONException if required fields are missing
     */
    public MachineStats(JSONObject json) throws JSONException {
        this.machineId = json.getInt("machine_id");
        this.machineName = json.optString("machine_name", "");
        this.totalStarts = json.optInt("total_starts", 0);
        this.totalEnds = json.optInt("total_ends", 0);

        // Parse daily_usage array
        this.dailyUsage = new ArrayList<>();
        JSONArray dailyArray = json.optJSONArray("daily_usage");
        if (dailyArray != null) {
            for (int i = 0; i < dailyArray.length(); i++) {
                JSONObject dayObj = dailyArray.getJSONObject(i);
                String date = dayObj.optString("date", "");
                int count = dayObj.optInt("count", 0);
                dailyUsage.add(new DailyUsage(date, count));
            }
        }
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

    public int getTotalStarts() {
        return totalStarts;
    }

    public void setTotalStarts(int totalStarts) {
        this.totalStarts = totalStarts;
    }

    public int getTotalEnds() {
        return totalEnds;
    }

    public void setTotalEnds(int totalEnds) {
        this.totalEnds = totalEnds;
    }

    public List<DailyUsage> getDailyUsage() {
        return dailyUsage;
    }

    public void setDailyUsage(List<DailyUsage> dailyUsage) {
        this.dailyUsage = dailyUsage;
    }

    /**
     * Check if statistics are empty (no usage data)
     *
     * @return true if no usage data exists
     */
    public boolean isEmpty() {
        return totalStarts == 0 && totalEnds == 0 && (dailyUsage == null || dailyUsage.isEmpty());
    }
}
