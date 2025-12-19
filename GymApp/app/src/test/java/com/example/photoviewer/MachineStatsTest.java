package com.example.photoviewer;

import com.example.photoviewer.models.MachineStats;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the MachineStats model class.
 *
 * Test IDs: AN-STATS-01 through AN-STATS-10
 * Priority: P0 (Critical - Core data model)
 */
public class MachineStatsTest {

    @Test
    public void AN_STATS_01_jsonParsing_parsesAllFields() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("machine_id", 1);
        json.put("machine_name", "런닝머신 #1");
        json.put("total_starts", 152);
        json.put("total_ends", 150);

        JSONArray dailyArray = new JSONArray();
        JSONObject day1 = new JSONObject();
        day1.put("date", "2024-01-15");
        day1.put("count", 10);
        dailyArray.put(day1);
        json.put("daily_usage", dailyArray);

        MachineStats stats = new MachineStats(json);

        assertEquals("Machine ID should be parsed", 1, stats.getMachineId());
        assertEquals("Machine name should be parsed", "런닝머신 #1", stats.getMachineName());
        assertEquals("Total starts should be parsed", 152, stats.getTotalStarts());
        assertEquals("Total ends should be parsed", 150, stats.getTotalEnds());
        assertEquals("Daily usage should have 1 entry", 1, stats.getDailyUsage().size());
        assertEquals("Daily usage date should match", "2024-01-15", stats.getDailyUsage().get(0).getDate());
        assertEquals("Daily usage count should match", 10, stats.getDailyUsage().get(0).getCount());
    }

    @Test(expected = JSONException.class)
    public void AN_STATS_02_jsonParsing_throwsExceptionForMissingMachineId() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("machine_name", "Test");

        new MachineStats(json);
    }

    @Test
    public void AN_STATS_03_jsonParsing_usesDefaultsForOptionalFields() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("machine_id", 1);

        MachineStats stats = new MachineStats(json);

        assertEquals("Machine name should default to empty", "", stats.getMachineName());
        assertEquals("Total starts should default to 0", 0, stats.getTotalStarts());
        assertEquals("Total ends should default to 0", 0, stats.getTotalEnds());
        assertTrue("Daily usage should be empty", stats.getDailyUsage().isEmpty());
    }

    @Test
    public void AN_STATS_04_isEmpty_returnsTrueWhenNoData() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("machine_id", 1);
        json.put("total_starts", 0);
        json.put("total_ends", 0);
        json.put("daily_usage", new JSONArray());

        MachineStats stats = new MachineStats(json);

        assertTrue("isEmpty should return true", stats.isEmpty());
    }

    @Test
    public void AN_STATS_05_isEmpty_returnsFalseWhenHasStarts() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("machine_id", 1);
        json.put("total_starts", 5);

        MachineStats stats = new MachineStats(json);

        assertFalse("isEmpty should return false when has starts", stats.isEmpty());
    }

    @Test
    public void AN_STATS_06_dailyUsage_parsesMultipleEntries() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("machine_id", 1);

        JSONArray dailyArray = new JSONArray();
        for (int i = 1; i <= 7; i++) {
            JSONObject day = new JSONObject();
            day.put("date", "2024-01-" + String.format("%02d", i));
            day.put("count", i * 10);
            dailyArray.put(day);
        }
        json.put("daily_usage", dailyArray);

        MachineStats stats = new MachineStats(json);

        assertEquals("Should have 7 daily entries", 7, stats.getDailyUsage().size());
        assertEquals("First day count should be 10", 10, stats.getDailyUsage().get(0).getCount());
        assertEquals("Last day count should be 70", 70, stats.getDailyUsage().get(6).getCount());
    }

    @Test
    public void AN_STATS_07_machineName_handlesKorean() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("machine_id", 1);
        json.put("machine_name", "레그프레스 머신");

        MachineStats stats = new MachineStats(json);

        assertEquals("Korean name should be preserved", "레그프레스 머신", stats.getMachineName());
    }

    @Test
    public void AN_STATS_08_dailyUsage_handlesNullArray() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("machine_id", 1);
        // No daily_usage field

        MachineStats stats = new MachineStats(json);

        assertNotNull("Daily usage should not be null", stats.getDailyUsage());
        assertTrue("Daily usage should be empty", stats.getDailyUsage().isEmpty());
    }

    @Test
    public void AN_STATS_09_totalCounts_handlesLargeValues() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("machine_id", 1);
        json.put("total_starts", 99999);
        json.put("total_ends", 99998);

        MachineStats stats = new MachineStats(json);

        assertEquals("Large total_starts should be preserved", 99999, stats.getTotalStarts());
        assertEquals("Large total_ends should be preserved", 99998, stats.getTotalEnds());
    }

    @Test
    public void AN_STATS_10_dailyUsage_handlesZeroCount() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("machine_id", 1);

        JSONArray dailyArray = new JSONArray();
        JSONObject day = new JSONObject();
        day.put("date", "2024-01-15");
        day.put("count", 0);
        dailyArray.put(day);
        json.put("daily_usage", dailyArray);

        MachineStats stats = new MachineStats(json);

        assertEquals("Zero count should be preserved", 0, stats.getDailyUsage().get(0).getCount());
    }
}
