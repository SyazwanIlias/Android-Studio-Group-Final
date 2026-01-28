package com.example.studentmanagerapp;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class ReportsActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private long userId;

    // Layout Containers
    private LinearLayout layoutGender, layoutBirthday, layoutStats;

    // Views
    private GenderPieChartView genderPieChart;
    private BirthdayBarChartView birthdayBarChart;
    private TextView tvMaleCount, tvFemaleCount, tvTotalSummary;
    private MaterialButton btnShowGender, btnShowBirthday, btnShowStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        db = new DatabaseHelper(this);
        userId = getIntent().getLongExtra("USER_ID", -1);

        // Initialize Layouts
        layoutGender = findViewById(R.id.layoutGender);
        layoutBirthday = findViewById(R.id.layoutBirthday);
        layoutStats = findViewById(R.id.layoutStats);

        // Initialize Views
        genderPieChart = findViewById(R.id.genderPieChart);
        birthdayBarChart = findViewById(R.id.birthdayBarChart);
        tvMaleCount = findViewById(R.id.tvMaleCount);
        tvFemaleCount = findViewById(R.id.tvFemaleCount);
        tvTotalSummary = findViewById(R.id.tvTotalSummary);

        // Buttons
        btnShowGender = findViewById(R.id.btnShowGender);
        btnShowBirthday = findViewById(R.id.btnShowBirthday);
        btnShowStats = findViewById(R.id.btnShowStats);
        MaterialButton btnBack = findViewById(R.id.btnBack);

        // Set Button Listeners
        btnShowGender.setOnClickListener(v -> showSection(1));
        btnShowBirthday.setOnClickListener(v -> showSection(2));
        btnShowStats.setOnClickListener(v -> showSection(3));

        btnBack.setOnClickListener(v -> finish());

        // Default Load
        loadGenderData();
        loadBirthdayStats();
        loadSummary();
        showSection(1); // Show gender by default
    }

    private void showSection(int section) {
        // Reset Button Colors
        btnShowGender.setTextColor(Color.WHITE);
        btnShowBirthday.setTextColor(Color.WHITE);
        btnShowStats.setTextColor(Color.WHITE);

        // Toggle Visibility
        layoutGender.setVisibility(section == 1 ? View.VISIBLE : View.GONE);
        layoutBirthday.setVisibility(section == 2 ? View.VISIBLE : View.GONE);
        layoutStats.setVisibility(section == 3 ? View.VISIBLE : View.GONE);

        // Highlight Active Button (Neon Yellow)
        if (section == 1) btnShowGender.setTextColor(Color.parseColor("#CCFF00"));
        if (section == 2) btnShowBirthday.setTextColor(Color.parseColor("#CCFF00"));
        if (section == 3) btnShowStats.setTextColor(Color.parseColor("#CCFF00"));
    }

    private void loadGenderData() {
        int male = db.getGenderCount(userId, "Male");
        int female = db.getGenderCount(userId, "Female");

        tvMaleCount.setText(" Male: " + male);
        tvFemaleCount.setText(" Female: " + female);

        if (genderPieChart != null) {
            genderPieChart.setGenderCounts(male, female);
        }
    }

    private void loadSummary() {
        int total = db.getBuddyCount(userId);
        tvTotalSummary.setText("Total Buddies: " + total);
    }

    private void loadBirthdayStats() {
        int[] counts = new int[12];
        for (int i = 1; i <= 12; i++) {
            String monthNum = String.format("%02d", i);
            counts[i-1] = db.getBuddyCountByMonth(userId, monthNum);
        }

        if (birthdayBarChart != null) {
            birthdayBarChart.setData(counts);
        }
    }
}
