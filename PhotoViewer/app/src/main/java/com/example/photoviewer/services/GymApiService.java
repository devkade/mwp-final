package com.example.photoviewer.services;

import android.util.Log;

import com.example.photoviewer.BuildConfig;
import com.example.photoviewer.models.GymMachine;

import org.json.JSONArray;
import org.json.JSONException;

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
     * Parse JSON array response into list of GymMachine objects
     */
    private List<GymMachine> parseMachinesResponse(String jsonStr) throws JSONException {
        List<GymMachine> machines = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(jsonStr);

        for (int i = 0; i < jsonArray.length(); i++) {
            GymMachine machine = new GymMachine(jsonArray.getJSONObject(i));
            machines.add(machine);
        }

        return machines;
    }
}
