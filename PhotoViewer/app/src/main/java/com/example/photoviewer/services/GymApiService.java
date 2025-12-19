package com.example.photoviewer.services;

import android.util.Log;

import com.example.photoviewer.BuildConfig;
import com.example.photoviewer.models.GymMachine;
import com.example.photoviewer.models.MachineEvent;
import com.example.photoviewer.models.MachineStats;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * GymApiService - API client for gym machine endpoints
 *
 * Singleton service for fetching gym equipment data from the backend API.
 * Uses ExecutorService for background network operations.
 */
public class GymApiService {
    private static final String TAG = "GymApiService";
    private static final String API_BASE_URL = BuildConfig.API_BASE_URL.replaceAll("/$", "");
    private static final String MACHINES_ENDPOINT = "/api_root/machines/";
    private static final String EVENTS_ENDPOINT_TEMPLATE = "/api/machines/%d/events/";
    private static final String EVENT_DETAIL_ENDPOINT_TEMPLATE = "/api_root/events/%d/";
    private static final String STATS_ENDPOINT_TEMPLATE = "/api_root/machines/%d/stats/";

    private static GymApiService instance;
    private final ExecutorService executorService;

    /**
     * Callback interface for machines API calls
     */
    public interface MachinesCallback {
        void onSuccess(List<GymMachine> machines);
        void onError(String errorMessage);
    }

    /**
     * Callback interface for events API calls
     */
    public interface EventsCallback {
        void onSuccess(List<MachineEvent> events);
        void onError(String errorMessage);
    }

    /**
     * Callback interface for single event detail API calls
     */
    public interface EventDetailCallback {
        void onSuccess(MachineEvent event);
        void onError(String errorMessage);
    }

    /**
     * Callback interface for machine stats API calls
     */
    public interface StatsCallback {
        void onSuccess(MachineStats stats);
        void onError(String errorMessage);
    }

    /**
     * Error type constants for specific error handling
     */
    public static final String ERROR_UNAUTHORIZED = "UNAUTHORIZED";
    public static final String ERROR_NETWORK = "NETWORK_ERROR";
    public static final String ERROR_SERVER = "SERVER_ERROR";

    private GymApiService() {
        executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Get singleton instance of GymApiService
     */
    public static synchronized GymApiService getInstance() {
        if (instance == null) {
            instance = new GymApiService();
        }
        return instance;
    }

    /**
     * Fetch all active gym machines from the API
     *
     * @param callback MachinesCallback to handle success or error
     */
    public void getMachines(MachinesCallback callback) {
        executorService.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String token = SessionManager.getInstance().getToken();
                if (token == null || token.isEmpty()) {
                    Log.e(TAG, "No auth token available");
                    callback.onError(ERROR_UNAUTHORIZED);
                    return;
                }

                String urlStr = API_BASE_URL + MACHINES_ENDPOINT;
                Log.d(TAG, "Fetching machines from: " + urlStr);

                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Success - parse response
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }
                    }

                    List<GymMachine> machines = parseMachinesResponse(response.toString());
                    Log.d(TAG, "Successfully parsed " + machines.size() + " machines");
                    callback.onSuccess(machines);

                } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    // 401 Unauthorized - token expired/invalid
                    Log.e(TAG, "Unauthorized - token invalid or expired");
                    SessionManager.getInstance().logout();
                    callback.onError(ERROR_UNAUTHORIZED);

                } else if (responseCode >= 500) {
                    // Server error
                    Log.e(TAG, "Server error: " + responseCode);
                    callback.onError(ERROR_SERVER);

                } else {
                    // Other error
                    Log.e(TAG, "API error: " + responseCode);
                    callback.onError(ERROR_NETWORK);
                }

            } catch (java.net.UnknownHostException e) {
                Log.e(TAG, "Network error - unknown host: " + e.getMessage());
                callback.onError(ERROR_NETWORK);
            } catch (java.net.SocketTimeoutException e) {
                Log.e(TAG, "Network error - timeout: " + e.getMessage());
                callback.onError(ERROR_NETWORK);
            } catch (java.io.IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage());
                callback.onError(ERROR_NETWORK);
            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                callback.onError(ERROR_SERVER);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    /**
     * Fetch events for a specific machine with optional filters
     */
    public void getMachineEvents(int machineId,
                                 String eventType,
                                 String dateFrom,
                                 String dateTo,
                                 EventsCallback callback) {
        executorService.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String token = SessionManager.getInstance().getToken();
                if (token == null || token.isEmpty()) {
                    Log.e(TAG, "No auth token available");
                    callback.onError(ERROR_UNAUTHORIZED);
                    return;
                }

                String urlStr = buildEventsUrl(machineId, eventType, dateFrom, dateTo);
                Log.d(TAG, "Fetching events from: " + urlStr);

                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }
                    }

                    List<MachineEvent> events = parseEventsResponse(response.toString());
                    Log.d(TAG, "Successfully parsed " + events.size() + " events");
                    callback.onSuccess(events);

                } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    Log.e(TAG, "Unauthorized - token invalid or expired");
                    SessionManager.getInstance().logout();
                    callback.onError(ERROR_UNAUTHORIZED);

                } else if (responseCode >= 500) {
                    Log.e(TAG, "Server error: " + responseCode);
                    callback.onError(ERROR_SERVER);

                } else {
                    Log.e(TAG, "API error: " + responseCode);
                    callback.onError(ERROR_NETWORK);
                }

            } catch (java.net.UnknownHostException e) {
                Log.e(TAG, "Network error - unknown host: " + e.getMessage());
                callback.onError(ERROR_NETWORK);
            } catch (java.net.SocketTimeoutException e) {
                Log.e(TAG, "Network error - timeout: " + e.getMessage());
                callback.onError(ERROR_NETWORK);
            } catch (java.io.IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage());
                callback.onError(ERROR_NETWORK);
            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                callback.onError(ERROR_SERVER);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    /**
     * Fetch a single event detail by event ID
     *
     * @param eventId Event ID to fetch
     * @param callback EventDetailCallback to handle success or error
     */
    public void getEventDetail(int eventId, EventDetailCallback callback) {
        executorService.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String token = SessionManager.getInstance().getToken();
                if (token == null || token.isEmpty()) {
                    Log.e(TAG, "No auth token available");
                    callback.onError(ERROR_UNAUTHORIZED);
                    return;
                }

                String urlStr = API_BASE_URL + String.format(EVENT_DETAIL_ENDPOINT_TEMPLATE, eventId);
                Log.d(TAG, "Fetching event detail from: " + urlStr);

                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }
                    }

                    JSONObject jsonObject = new JSONObject(response.toString());
                    MachineEvent event = new MachineEvent(jsonObject);
                    Log.d(TAG, "Successfully parsed event detail: " + eventId);
                    callback.onSuccess(event);

                } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    Log.e(TAG, "Unauthorized - token invalid or expired");
                    SessionManager.getInstance().logout();
                    callback.onError(ERROR_UNAUTHORIZED);

                } else if (responseCode >= 500) {
                    Log.e(TAG, "Server error: " + responseCode);
                    callback.onError(ERROR_SERVER);

                } else {
                    Log.e(TAG, "API error: " + responseCode);
                    callback.onError(ERROR_NETWORK);
                }

            } catch (java.net.UnknownHostException e) {
                Log.e(TAG, "Network error - unknown host: " + e.getMessage());
                callback.onError(ERROR_NETWORK);
            } catch (java.net.SocketTimeoutException e) {
                Log.e(TAG, "Network error - timeout: " + e.getMessage());
                callback.onError(ERROR_NETWORK);
            } catch (java.io.IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage());
                callback.onError(ERROR_NETWORK);
            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                callback.onError(ERROR_SERVER);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    /**
     * Fetch usage statistics for a specific machine with date range
     *
     * @param machineId Machine ID to fetch stats for
     * @param dateFrom Start date in YYYY-MM-DD format
     * @param dateTo End date in YYYY-MM-DD format
     * @param callback StatsCallback to handle success or error
     */
    public void getMachineStats(int machineId, String dateFrom, String dateTo, StatsCallback callback) {
        executorService.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String token = SessionManager.getInstance().getToken();
                if (token == null || token.isEmpty()) {
                    Log.e(TAG, "No auth token available");
                    callback.onError(ERROR_UNAUTHORIZED);
                    return;
                }

                String urlStr = buildStatsUrl(machineId, dateFrom, dateTo);
                Log.d(TAG, "Fetching stats from: " + urlStr);

                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }
                    }

                    JSONObject jsonObject = new JSONObject(response.toString());
                    MachineStats stats = new MachineStats(jsonObject);
                    Log.d(TAG, "Successfully parsed stats for machine: " + machineId);
                    callback.onSuccess(stats);

                } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    Log.e(TAG, "Unauthorized - token invalid or expired");
                    SessionManager.getInstance().logout();
                    callback.onError(ERROR_UNAUTHORIZED);

                } else if (responseCode >= 500) {
                    Log.e(TAG, "Server error: " + responseCode);
                    callback.onError(ERROR_SERVER);

                } else {
                    Log.e(TAG, "API error: " + responseCode);
                    callback.onError(ERROR_NETWORK);
                }

            } catch (java.net.UnknownHostException e) {
                Log.e(TAG, "Network error - unknown host: " + e.getMessage());
                callback.onError(ERROR_NETWORK);
            } catch (java.net.SocketTimeoutException e) {
                Log.e(TAG, "Network error - timeout: " + e.getMessage());
                callback.onError(ERROR_NETWORK);
            } catch (java.io.IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage());
                callback.onError(ERROR_NETWORK);
            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                callback.onError(ERROR_SERVER);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private String buildStatsUrl(int machineId, String dateFrom, String dateTo) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(API_BASE_URL);
        urlBuilder.append(String.format(STATS_ENDPOINT_TEMPLATE, machineId));

        boolean hasQuery = false;
        if (dateFrom != null && !dateFrom.isEmpty()) {
            urlBuilder.append("?date_from=").append(dateFrom);
            hasQuery = true;
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            urlBuilder.append(hasQuery ? "&" : "?");
            urlBuilder.append("date_to=").append(dateTo);
        }

        return urlBuilder.toString();
    }

    private String buildEventsUrl(int machineId, String eventType, String dateFrom, String dateTo) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(API_BASE_URL);
        urlBuilder.append(String.format(EVENTS_ENDPOINT_TEMPLATE, machineId));

        boolean hasQuery = false;
        if (eventType != null && !eventType.isEmpty()) {
            urlBuilder.append(hasQuery ? "&" : "?");
            urlBuilder.append("event_type=").append(eventType);
            hasQuery = true;
        }
        if (dateFrom != null && !dateFrom.isEmpty()) {
            urlBuilder.append(hasQuery ? "&" : "?");
            urlBuilder.append("date_from=").append(dateFrom);
            hasQuery = true;
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            urlBuilder.append(hasQuery ? "&" : "?");
            urlBuilder.append("date_to=").append(dateTo);
        }

        return urlBuilder.toString();
    }

    /**
     * Parse paginated JSON response into list of GymMachine objects
     */
    private List<GymMachine> parseMachinesResponse(String jsonStr) throws JSONException {
        List<GymMachine> machines = new ArrayList<>();
        JSONObject response = new JSONObject(jsonStr);
        JSONArray jsonArray = response.getJSONArray("results");

        for (int i = 0; i < jsonArray.length(); i++) {
            GymMachine machine = new GymMachine(jsonArray.getJSONObject(i));
            machines.add(machine);
        }

        return machines;
    }

    /**
     * Parse paginated JSON response into list of MachineEvent objects
     */
    private List<MachineEvent> parseEventsResponse(String jsonStr) throws JSONException {
        List<MachineEvent> events = new ArrayList<>();
        JSONObject response = new JSONObject(jsonStr);
        JSONArray jsonArray = response.getJSONArray("results");

        for (int i = 0; i < jsonArray.length(); i++) {
            MachineEvent event = new MachineEvent(jsonArray.getJSONObject(i));
            events.add(event);
        }

        return events;
    }
}
