package com.example.photoviewer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.photoviewer.services.AuthenticationService;
import com.example.photoviewer.services.SessionManager;
import com.example.photoviewer.utils.SecureTokenManager;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private EditText securityKeyInput;
    private Button loginButton;
    private TextView errorMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupLoginButton();
    }

    private void initializeViews() {
        securityKeyInput = findViewById(R.id.security_key_input);
        loginButton = findViewById(R.id.login_button);
        errorMessage = findViewById(R.id.error_message);
    }

    private void setupLoginButton() {
        loginButton.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String securityKey = securityKeyInput.getText().toString().trim();

        // Clear previous error
        errorMessage.setText("");
        errorMessage.setVisibility(android.view.View.GONE);

        // Validate input - AC #4: empty security key shows Korean error
        if (securityKey.isEmpty()) {
            showError("보안키를 입력하세요");
            return;
        }

        // Disable button to prevent multiple clicks
        loginButton.setEnabled(false);
        loginButton.setText("로그인 중...");

        // Attempt login with security key - AC #2
        AuthenticationService.login(securityKey, new AuthenticationService.LoginCallback() {
            @Override
            public void onSuccess(String token) {
                Log.d(TAG, "Login successful");
                try {
                    // Save session with security key - AC #2: token stored securely
                    SessionManager.getInstance().saveSession(securityKey, token);
                } catch (RuntimeException e) {
                    Log.e(TAG, "Error saving session: " + e.getMessage());
                }

                // Navigate to MachineListActivity - AC #2
                runOnUiThread(() -> {
                    Intent intent = new Intent(LoginActivity.this, MachineListActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String errorMsg) {
                Log.d(TAG, "Login failed: " + errorMsg);
                runOnUiThread(() -> {
                    // AC #3: Show Korean error message for invalid key
                    // AC #6: Show Korean error message for network failure
                    String displayMessage;
                    if (errorMsg.contains("Invalid security key") || errorMsg.contains("401")) {
                        displayMessage = "잘못된 보안키입니다";
                    } else if (errorMsg.contains("Network error") || errorMsg.contains("timeout") ||
                               errorMsg.contains("Unable to resolve host")) {
                        displayMessage = "네트워크 오류가 발생했습니다";
                    } else {
                        displayMessage = "로그인 실패: " + errorMsg;
                    }

                    showError(displayMessage);
                    loginButton.setEnabled(true);
                    loginButton.setText("로그인");
                    securityKeyInput.setText(""); // Clear security key on error
                });
            }
        });
    }

    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisibility(android.view.View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
