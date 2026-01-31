package com.example.studentmanagerapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class ReportsActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private long userId;

    // Layout Containers
    private LinearLayout layoutGender, layoutBirthday, layoutStats;

    // Card Views
    private MaterialCardView cardGender, cardBirthday, cardStats;

    // Views
    private GenderPieChartView genderPieChart;
    private BirthdayBarChartView birthdayBarChart;
    private TextView tvMaleCount, tvFemaleCount, tvTotalSummary;
    private MaterialButton btnShowGender, btnShowBirthday, btnShowStats, btnBack;

    private int currentSection = 1;
    private boolean isAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        db = new DatabaseHelper(this);
        userId = getIntent().getLongExtra("USER_ID", -1);

        initializeViews();
        setButtonListeners();
        loadAllData();

        // Animate entry
        animatePageEntry();
    }

    private void initializeViews() {
        // Initialize Layouts
        layoutGender = findViewById(R.id.layoutGender);
        layoutBirthday = findViewById(R.id.layoutBirthday);
        layoutStats = findViewById(R.id.layoutStats);

        // Initialize Cards
        cardGender = findViewById(R.id.cardGender);
        cardBirthday = findViewById(R.id.cardBirthday);
        cardStats = findViewById(R.id.cardStats);

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
        btnBack = findViewById(R.id.btnBack);
    }

    private void setButtonListeners() {
        btnShowGender.setOnClickListener(v -> {
            if (!isAnimating && currentSection != 1) {
                showSection(1);
            }
        });

        btnShowBirthday.setOnClickListener(v -> {
            if (!isAnimating && currentSection != 2) {
                showSection(2);
            }
        });

        btnShowStats.setOnClickListener(v -> {
            if (!isAnimating && currentSection != 3) {
                showSection(3);
            }
        });

        btnBack.setOnClickListener(v -> {
            animatePageExit();
        });
    }

    private void loadAllData() {
        loadGenderData();
        loadBirthdayStats();
        loadSummary();
    }

    private void animatePageEntry() {
        // Initial state - hide everything
        View headerCard = findViewById(R.id.headerCard);
        headerCard.setAlpha(0f);
        headerCard.setTranslationY(-100f);

        layoutGender.setAlpha(0f);
        layoutGender.setScaleX(0.8f);
        layoutGender.setScaleY(0.8f);

        btnBack.setAlpha(0f);
        btnBack.setTranslationY(100f);

        // Animate header
        headerCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Animate content with delay
        new Handler().postDelayed(() -> {
            layoutGender.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(600)
                    .setInterpolator(new OvershootInterpolator(0.8f))
                    .withEndAction(() -> {
                        // Trigger chart animation after layout is visible
                        new Handler().postDelayed(() -> {
                            if (genderPieChart != null) {
                                int male = db.getGenderCount(userId, "Male");
                                int female = db.getGenderCount(userId, "Female");
                                genderPieChart.setGenderCounts(male, female);
                            }
                            animateCounters();
                        }, 200);
                    })
                    .start();
        }, 300);

        // Animate back button
        new Handler().postDelayed(() -> {
            btnBack.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }, 500);
    }

    private void animatePageExit() {
        isAnimating = true;

        View headerCard = findViewById(R.id.headerCard);
        View currentLayout = getCurrentLayout();

        // Animate header out
        headerCard.animate()
                .alpha(0f)
                .translationY(-100f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Animate content out
        currentLayout.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Animate button out and finish
        btnBack.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> finish())
                .start();
    }

    private void showSection(int section) {
        if (isAnimating) return;

        isAnimating = true;
        int previousSection = currentSection;
        currentSection = section;

        // Get views
        View outgoingLayout = getLayoutForSection(previousSection);
        View incomingLayout = getLayoutForSection(section);

        // Animate outgoing
        outgoingLayout.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(250)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    outgoingLayout.setVisibility(View.GONE);

                    // Prepare incoming
                    incomingLayout.setVisibility(View.VISIBLE);
                    incomingLayout.setAlpha(0f);
                    incomingLayout.setScaleX(0.9f);
                    incomingLayout.setScaleY(0.9f);

                    // Animate incoming
                    incomingLayout.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(300)
                            .setInterpolator(new OvershootInterpolator(0.5f))
                            .withEndAction(() -> {
                                isAnimating = false;

                                // Trigger chart animations with fresh data
                                if (section == 1) {
                                    // Re-trigger gender chart animation
                                    new Handler().postDelayed(() -> {
                                        int male = db.getGenderCount(userId, "Male");
                                        int female = db.getGenderCount(userId, "Female");
                                        genderPieChart.setGenderCounts(male, female);
                                        animateCounters();
                                    }, 100);
                                } else if (section == 2) {
                                    // Re-trigger bar chart animation
                                    new Handler().postDelayed(() -> {
                                        int[] counts = new int[12];
                                        for (int i = 1; i <= 12; i++) {
                                            String monthNum = String.format("%02d", i);
                                            counts[i-1] = db.getBuddyCountByMonth(userId, monthNum);
                                        }
                                        birthdayBarChart.setData(counts);
                                    }, 100);
                                } else if (section == 3) {
                                    animateTotalCount();
                                }
                            })
                            .start();
                })
                .start();

        // Update button states with animation
        updateButtonStates(section);
    }

    private void updateButtonStates(int section) {
        // Reset all buttons
        animateButton(btnShowGender, false, section == 1);
        animateButton(btnShowBirthday, false, section == 2);
        animateButton(btnShowStats, false, section == 3);
    }

    private void animateButton(MaterialButton button, boolean wasSelected, boolean isSelected) {
        if (isSelected) {
            button.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        button.setBackgroundTintList(getColorStateList(R.color.primary));
                        button.setTextColor(Color.WHITE);
                        button.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200)
                                .start();
                    })
                    .start();
        } else {
            button.setBackgroundTintList(getColorStateList(android.R.color.transparent));
            button.setTextColor(getColor(R.color.text_secondary));
            button.setScaleX(1f);
            button.setScaleY(1f);
        }
    }

    private void animateCounters() {
        int male = db.getGenderCount(userId, "Male");
        int female = db.getGenderCount(userId, "Female");

        animateTextCounter(tvMaleCount, 0, male, 800);

        new Handler().postDelayed(() -> {
            animateTextCounter(tvFemaleCount, 0, female, 800);
        }, 200);
    }

    private void animateTotalCount() {
        int total = db.getBuddyCount(userId);
        animateTextCounter(tvTotalSummary, 0, total, 1000);
    }

    private void animateTextCounter(TextView textView, int from, int to, int duration) {
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(duration);
        animator.setInterpolator(new DecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            textView.setText(String.valueOf(value));
        });

        animator.start();
    }

    private View getLayoutForSection(int section) {
        switch (section) {
            case 1:
                return layoutGender;
            case 2:
                return layoutBirthday;
            case 3:
                return layoutStats;
            default:
                return layoutGender;
        }
    }

    private View getCurrentLayout() {
        return getLayoutForSection(currentSection);
    }

    private void loadGenderData() {
        // Data will be set with animation when needed
        tvMaleCount.setText("0");
        tvFemaleCount.setText("0");
    }

    private void loadSummary() {
        tvTotalSummary.setText("0");
    }

    private void loadBirthdayStats() {
        // Data will be set with animation when switching to this tab
    }

    @Override
    public void onBackPressed() {
        animatePageExit();
    }
}