package com.example.photoviewer.services;

import android.util.Log;
import com.example.photoviewer.BuildConfig;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AuthenticationService {
    // API URL automatically switches based on build type:
    // - Debug builds: http://10.0.2.2:8000 (localhost via emulator)
    // - Release builds: https://mouseku.pythonanywhere.com
    private static final String API_BASE_URL = BuildConfig.API_BASE_URL.replaceAll("/$", "");
    private static final String LOGIN_ENDPOINT = "/api/auth/login/";
    private static final String TAG = "AuthenticationService";

    public interface LoginCallback {
        void onSuccess(String token);
        void onError(String errorMessage);
    }

    public static void login(String securityKey, LoginCallback callback) {
        Log.d(TAG, "login() called with security key: " + securityKey.substring(0, Math.min(8, securityKey.length())) + "...");
        new Thread(() -> {
            try {
                Log.d(TAG, "Creating URL: " + API_BASE_URL + LOGIN_ENDPOINT);
                URL url = new URL(API_BASE_URL + LOGIN_ENDPOINT);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                Log.d(TAG, "Connection opened, preparing request body");

                // Create request body with security_key
                JSONObject requestBody = new JSONObject();
                requestBody.put("security_key", securityKey);
                Log.d(TAG, "Request body: {\"security_key\": \"***\"}");

                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                    Log.d(TAG, "Writing " + input.length + " bytes to connection");
                    os.write(input, 0, input.length);
                    Log.d(TAG, "Request sent successfully");
                }

                // Handle response
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    StringBuilder response = new StringBuilder();
                    try (java.io.BufferedReader br = new java.io.BufferedReader(
                            new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }
                    }

                    JSONObject responseJson = new JSONObject(response.toString());
                    String token = responseJson.getString("token");
                    Log.d(TAG, "Login successful, token: " + token.substring(0, Math.min(10, token.length())) + "...");
                    callback.onSuccess(token);
                } else {
                    // Handle error response
                    Log.d(TAG, "Error response code: " + responseCode);
                    StringBuilder error = new StringBuilder();
                    try (java.io.BufferedReader br = new java.io.BufferedReader(
                            new java.io.InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            error.append(line);
                        }
                    }

                    Log.d(TAG, "Error response body: " + error.toString());
                    String errorMessage = "Login failed";
                    try {
                        JSONObject errorJson = new JSONObject(error.toString());
                        if (errorJson.has("error")) {
                            errorMessage = errorJson.getString("error");
                        }
                    } catch (Exception e) {
                        // Use default error message
                        Log.e(TAG, "Error parsing error response: " + e.getMessage());
                    }

                    Log.d(TAG, "Calling onError with: " + errorMessage);
                    callback.onError(errorMessage);
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Exception in login: " + e.getMessage(), e);
                callback.onError("Network error: " + e.getMessage());
            }
        }).start();
    }
}
