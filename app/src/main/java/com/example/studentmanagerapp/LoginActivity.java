package com.example.studentmanagerapp;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText etUsername, etPassword;
    private Button btnLogin, btnGoToRegister;
    private DatabaseHelper db;
    private View headerBackground, tvAppLogo;
    private MaterialCardView loginCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = new DatabaseHelper(this);

        // Initialize views
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);
        headerBackground = findViewById(R.id.headerBackground);
        tvAppLogo = findViewById(R.id.tvAppLogo);
        loginCard = findViewById(R.id.loginCard);

        // Set views invisible before animation
        tvAppLogo.setVisibility(View.INVISIBLE);
        tvAppLogo.setAlpha(0f);
        loginCard.setVisibility(View.INVISIBLE);
        loginCard.setAlpha(0f);

        // Start animations
        animateViews();

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
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
    }

    private void animateViews() {
        // Animate header background - slide down
        TranslateAnimation slideDown = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, -1,
                Animation.RELATIVE_TO_SELF, 0
        );
        slideDown.setDuration(800);
        slideDown.setFillAfter(true);
        headerBackground.startAnimation(slideDown);

        // Animate logo - fade in and slide from left
        new Handler().postDelayed(() -> {
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(600);

            TranslateAnimation slideRight = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, -0.5f,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0
            );
            slideRight.setDuration(600);

            AnimationSet logoAnimation = new AnimationSet(true);
            logoAnimation.addAnimation(fadeIn);
            logoAnimation.addAnimation(slideRight);
            logoAnimation.setFillAfter(true);

            tvAppLogo.setVisibility(View.VISIBLE);
            tvAppLogo.setAlpha(1f);
            tvAppLogo.startAnimation(logoAnimation);
        }, 300);

        // Animate login card - slide up and fade in
        new Handler().postDelayed(() -> {
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(700);

            TranslateAnimation slideUp = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0.3f,
                    Animation.RELATIVE_TO_SELF, 0
            );
            slideUp.setDuration(700);

            AnimationSet cardAnimation = new AnimationSet(true);
            cardAnimation.addAnimation(fadeIn);
            cardAnimation.addAnimation(slideUp);
            cardAnimation.setFillAfter(true);

            if (loginCard != null) {
                loginCard.setVisibility(View.VISIBLE);
                loginCard.setAlpha(1f);
                loginCard.startAnimation(cardAnimation);
            }
        }, 600);
    }
}