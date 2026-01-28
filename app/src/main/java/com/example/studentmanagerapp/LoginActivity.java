package com.example.studentmanagerapp;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText etUsername, etPassword;
    private Button btnLogin, btnGoToRegister;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = new DatabaseHelper(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Attempting login for user: " + username);

            try (Cursor cursor = db.checkLoginAndGetId(username, password)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idColumnIndex = cursor.getColumnIndex(DatabaseHelper.COL_USER_ID);
                    if (idColumnIndex != -1) {
                        long userId = cursor.getLong(idColumnIndex);
                        Log.d(TAG, "Login successful. User ID: " + userId);

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("USER_ID", userId);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.e(TAG, "Column '" + DatabaseHelper.COL_USER_ID + "' not found in cursor!");
                        Toast.makeText(this, "Database Error", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d(TAG, "Login failed: Invalid credentials");
                    Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during login process", e);
                Toast.makeText(this, "System Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        btnGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }
}
