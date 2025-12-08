package ict.mgame.iotmedicinebox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;

public class LoginActivity extends Activity {

    private MaterialButton btnSignUp;
    private MaterialButton btnSignInAccount;
    private MaterialButton btnSignInGoogle;

    // Hardcoded credentials
    private static final String VALID_USERNAME = "MedBox";
    private static final String VALID_PASSWORD = "123456";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize buttons
        btnSignUp = findViewById(R.id.btnSignUp);
        btnSignInAccount = findViewById(R.id.btnSignInAccount);
        btnSignInGoogle = findViewById(R.id.btnSignInGoogle);

        // Sign Up button - Go to registration (for now, just show toast)
        btnSignUp.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, "Sign Up feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Sign In Using Account button - Show login dialog
        btnSignInAccount.setOnClickListener(v -> showLoginDialog());

        // Sign In Using Google button - Show toast (for now)
        btnSignInGoogle.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, "Google Sign-In coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void showLoginDialog() {
        // Create custom dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_login, null);
        builder.setView(dialogView);

        EditText etUsername = dialogView.findViewById(R.id.etUsername);
        EditText etPassword = dialogView.findViewById(R.id.etPassword);
        MaterialButton btnLogin = dialogView.findViewById(R.id.btnLogin);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        android.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (username.equals(VALID_USERNAME) && password.equals(VALID_PASSWORD)) {
                Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();

                // Go to HomeActivity
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(intent);
                finish(); // Close login activity
            } else {
                Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_LONG).show();
                etPassword.setText(""); // Clear password field
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}