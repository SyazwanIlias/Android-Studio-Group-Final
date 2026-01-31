package com.example.studentmanagerapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private BuddyAdapter adapter;
    private RecyclerView recyclerView;
    private Spinner monthFilterSpinner;
    private EditText etSearch;
    private long userId, selectedBuddyId = -1;
    private String currentSearchQuery = "";
    private int currentMonthPosition = 0;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Photo selection
    private String currentPhotoBase64 = null;
    private ImageView currentPhotoImageView = null;
    private ActivityResultLauncher<Intent> photoPickerLauncher;

    // Animation views
    private View appBarLayout;
    private MaterialCardView searchCard, filterCard, upcomingBirthdayCard;
    private FloatingActionButton fabAddBuddy, fabStatistics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize photo picker launcher
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            try {
                                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                                // Resize bitmap to save space
                                Bitmap resizedBitmap = resizeBitmap(bitmap, 300, 300);

                                // Convert to base64
                                currentPhotoBase64 = bitmapToBase64(resizedBitmap);

                                // Update ImageView
                                if (currentPhotoImageView != null) {
                                    currentPhotoImageView.setImageBitmap(resizedBitmap);
                                    currentPhotoImageView.setPadding(0, 0, 0, 0);
                                    currentPhotoImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    currentPhotoImageView.setImageTintList(null);

                                }

                                if (inputStream != null) inputStream.close();
                            } catch (IOException e) {
                                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        db = new DatabaseHelper(this);
        userId = getIntent().getLongExtra("USER_ID", 1);

        TextView tvWelcome = findViewById(R.id.tvWelcome);

        executorService.execute(() -> {
            Cursor c = db.getUsernameByUserId(userId);

            if (c != null && c.moveToFirst()) {

                String username = c.getString(
                        c.getColumnIndexOrThrow(DatabaseHelper.COL_USERNAME)
                );

                mainHandler.post(() -> tvWelcome.setText(username));

                c.close();
            }
        });

        recyclerView = findViewById(R.id.rvBuddies);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        // Initialize search EditText
        etSearch = findViewById(R.id.etSearch);
        setupSearchListener();

        monthFilterSpinner = findViewById(R.id.spinnerMonthFilter);
        setupMonthFilterSpinner();

        FloatingActionButton fabAddBuddy = findViewById(R.id.fab_add_buddy);
        fabAddBuddy.setOnClickListener(v -> showBuddyDialog(null));

        FloatingActionButton fabStatistics = findViewById(R.id.fab_statistics);
        fabStatistics.setOnClickListener(v -> {
            Intent i = new Intent(this, ReportsActivity.class);
            i.putExtra("USER_ID", userId);
            startActivity(i);
        });

        ImageButton btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // Initialize animation views
        initAnimationViews();

        // Start entrance animations
        startEntranceAnimations();

        loadBuddies();
        loadUpcomingBirthday();
    }

    private void setupSearchListener() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s.toString().trim();
                    applyFilters();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    private void applyFilters() {
        if (recyclerView == null) return;

        executorService.execute(() -> {
            Cursor cursor;

            // If there's a search query, use it (it will override month filter)
            if (!currentSearchQuery.isEmpty()) {
                cursor = db.searchBuddiesForUser(currentSearchQuery, userId);
            }
            // If month filter is selected (not "All Months")
            else if (currentMonthPosition > 0) {
                String month = String.format(Locale.US, "%02d", currentMonthPosition);
                cursor = db.getBuddiesByMonth(userId, month);
            }
            // Otherwise show all buddies
            else {
                cursor = db.getAllBuddiesForUser(userId);
            }

            mainHandler.post(() -> {
                if (adapter == null) {
                    adapter = new BuddyAdapter(cursor);

                    adapter.setOnItemClickListener(id1 -> {
                        selectedBuddyId = id1;
                        showBuddyOptionsDialog(id1);
                    });

                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.swapCursor(cursor);
                }
            });
        });
    }

    private void loadUpcomingBirthday() {
        executorService.execute(() -> {
            Cursor cursor = db.getAllBuddiesWithDob(userId);
            if (cursor == null) return;

            String closestBuddyName = null;
            long minDays = Long.MAX_VALUE;
            String closestDate = null;

            Calendar today = Calendar.getInstance();
            int currentYear = today.get(Calendar.YEAR);

            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BUDDY_NAME));
                String dobString = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BUDDY_DOB));

                try {
                    Date dob = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dobString);
                    Calendar dobCal = Calendar.getInstance();
                    if (dob != null) {
                        dobCal.setTime(dob);
                    }
                    dobCal.set(Calendar.YEAR, currentYear);

                    if (dobCal.before(today)) {
                        dobCal.add(Calendar.YEAR, 1);
                    }

                    long diff = dobCal.getTimeInMillis() - today.getTimeInMillis();
                    long days = diff / (24 * 60 * 60 * 1000);

                    if (days < minDays) {
                        minDays = days;
                        closestBuddyName = name;
                        closestDate = new SimpleDateFormat("MMMM d", Locale.getDefault()).format(dobCal.getTime());
                    }
                } catch (ParseException e) {
                    // Log the exception
                }
            }
            cursor.close();

            String finalClosestBuddyName = closestBuddyName;
            String finalClosestDate = closestDate;

            mainHandler.post(() -> {
                CardView upcomingBirthdayCard = findViewById(R.id.cardUpcomingBirthday);
                if (finalClosestBuddyName != null) {
                    TextView tvName = findViewById(R.id.tvUpcomingBuddyName);
                    TextView tvDate = findViewById(R.id.tvUpcomingBuddyDate);

                    tvName.setText(finalClosestBuddyName);
                    tvDate.setText(finalClosestDate);

                    upcomingBirthdayCard.setVisibility(View.VISIBLE);

                    // Animate the card when it appears
                    upcomingBirthdayCard.setAlpha(0f);
                    AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                    fadeIn.setDuration(500);

                    TranslateAnimation slideUp = new TranslateAnimation(
                            Animation.RELATIVE_TO_SELF, 0,
                            Animation.RELATIVE_TO_SELF, 0,
                            Animation.RELATIVE_TO_SELF, 0.3f,
                            Animation.RELATIVE_TO_SELF, 0
                    );
                    slideUp.setDuration(500);

                    AnimationSet cardAnimation = new AnimationSet(true);
                    cardAnimation.addAnimation(fadeIn);
                    cardAnimation.addAnimation(slideUp);
                    cardAnimation.setInterpolator(new DecelerateInterpolator());

                    upcomingBirthdayCard.startAnimation(cardAnimation);
                    upcomingBirthdayCard.setAlpha(1f);
                } else {
                    upcomingBirthdayCard.setVisibility(View.GONE);
                }
            });
        });
    }

    private void setupMonthFilterSpinner() {
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.months_array, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthFilterSpinner.setAdapter(spinnerAdapter);

        monthFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentMonthPosition = position;
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentMonthPosition = 0;
                applyFilters();
            }
        });
    }

    private void loadBuddies() {
        if (recyclerView == null) return;

        executorService.execute(() -> {
            Cursor cursor = db.getAllBuddiesForUser(userId);

            mainHandler.post(() -> {
                if (adapter == null) {
                    adapter = new BuddyAdapter(cursor);

                    adapter.setOnItemClickListener(id1 -> {
                        selectedBuddyId = id1;
                        showBuddyOptionsDialog(id1);
                    });

                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.swapCursor(cursor);
                }
            });
        });
    }

    private void showBuddyOptionsDialog(long buddyId) {
        executorService.execute(() -> {
            Cursor c = db.getBuddyById(buddyId);
            if (c != null && c.moveToFirst()) {
                String name = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_BUDDY_NAME));

                // Load photo
                int photoIndex = c.getColumnIndex(DatabaseHelper.COL_BUDDY_PHOTO);
                String photoBase64 = (photoIndex != -1) ? c.getString(photoIndex) : null;

                mainHandler.post(() -> {
                    // Inflate custom dialog layout
                    View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_buddy_options, null);

                    // Set buddy name
                    TextView tvBuddyName = dialogView.findViewById(R.id.tvBuddyNameDialog);
                    tvBuddyName.setText(name);

                    // Set buddy photo
                    ImageView ivBuddyPhoto = dialogView.findViewById(R.id.ivBuddyPhotoDialog);
                    if (photoBase64 != null && !photoBase64.isEmpty() && ivBuddyPhoto != null) {
                        try {
                            Bitmap bitmap = base64ToBitmap(photoBase64);
                            ivBuddyPhoto.setImageBitmap(bitmap);
                            ivBuddyPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            ivBuddyPhoto.setPadding(0, 0, 0, 0);
                            ivBuddyPhoto.setImageTintList(null);
                        } catch (Exception e) {
                            // Keep default icon if photo fails to load
                        }
                    }

                    // Get cards
                    MaterialCardView cardUpdate = dialogView.findViewById(R.id.cardUpdate);
                    MaterialCardView cardDelete = dialogView.findViewById(R.id.cardDelete);
                    MaterialCardView cardSendWish = dialogView.findViewById(R.id.cardSendWish);

                    // Create dialog
                    AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                            .setView(dialogView)
                            .setCancelable(true)
                            .create();

                    // Make dialog background transparent for rounded corners
                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    }

                    // Set click listeners
                    cardUpdate.setOnClickListener(v -> {
                        animateCardClick(cardUpdate);
                        new Handler().postDelayed(() -> {
                            dialog.dismiss();
                            showBuddyDialog(buddyId);
                        }, 200);
                    });

                    cardDelete.setOnClickListener(v -> {
                        animateCardClick(cardDelete);
                        new Handler().postDelayed(() -> {
                            dialog.dismiss();
                            confirmDelete(buddyId);
                        }, 200);
                    });

                    cardSendWish.setOnClickListener(v -> {
                        animateCardClick(cardSendWish);
                        new Handler().postDelayed(() -> {
                            dialog.dismiss();
                            sendWhatsApp();
                        }, 200);
                    });

                    // Cancel button
                    dialogView.findViewById(R.id.btnCancelDialog).setOnClickListener(v -> {
                        dialog.dismiss();
                    });

                    dialog.show();

                    // Animate dialog entry
                    animateDialogEntry(dialogView);
                });
            }
            if (c != null) c.close();
        });
    }

    private void animateCardClick(View card) {
        card.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    card.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private void animateDialogEntry(View dialogView) {
        dialogView.setAlpha(0f);
        dialogView.setScaleX(0.9f);
        dialogView.setScaleY(0.9f);

        dialogView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void showBuddyDialog(Long id) {
        // Only reset photo if adding new buddy, keep it for updates
        if (id == null) {
            currentPhotoBase64 = null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_buddy, null);

        // Update title
        TextView tvDialogTitle = v.findViewById(R.id.tvDialogTitle);
        if (tvDialogTitle != null) {
            tvDialogTitle.setText(id == null ? "Add Buddy" : "Update Buddy");
        }

        // Photo elements
        ImageView ivBuddyPhoto = v.findViewById(R.id.ivBuddyPhoto);
        MaterialCardView btnSelectPhoto = v.findViewById(R.id.btnSelectPhoto);
        currentPhotoImageView = ivBuddyPhoto;

        TextInputEditText etName = v.findViewById(R.id.etBuddyName);
        TextInputEditText etPhone = v.findViewById(R.id.etPhone);
        TextInputEditText etDob = v.findViewById(R.id.etDOB);
        TextInputEditText etEmail = v.findViewById(R.id.etEmail);
        RadioGroup rgGender = v.findViewById(R.id.rgGender);
        RadioButton rbMale = v.findViewById(R.id.rbMale);
        RadioButton rbFemale = v.findViewById(R.id.rbFemale);

        // Gender cards
        MaterialCardView cardMale = v.findViewById(R.id.cardMale);
        MaterialCardView cardFemale = v.findViewById(R.id.cardFemale);

        // Create dialog first so we can dismiss it later
        AlertDialog dialog = builder.setView(v).create();

        // Make dialog background transparent for rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Handle photo selection
        if (btnSelectPhoto != null) {
            btnSelectPhoto.setOnClickListener(view -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                photoPickerLauncher.launch(intent);
            });
        }

        // Handle gender card clicks
        if (cardMale != null && rbMale != null) {
            cardMale.setOnClickListener(view -> {
                rbMale.setChecked(true);
                updateGenderCardSelection(cardMale, cardFemale, true);
            });
        }

        if (cardFemale != null && rbFemale != null) {
            cardFemale.setOnClickListener(view -> {
                rbFemale.setChecked(true);
                updateGenderCardSelection(cardMale, cardFemale, false);
            });
        }

        if (etDob != null) {
            etDob.setOnClickListener(view -> {
                Calendar c = Calendar.getInstance();
                new DatePickerDialog(this, (dp, y, m, d) -> {
                    String date = y + "-" + String.format(Locale.US, "%02d", (m + 1)) + "-" + String.format(Locale.US, "%02d", d);
                    etDob.setText(date);
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
            });
        }

        if (id != null) {
            executorService.execute(() -> {
                Cursor c = db.getBuddyById(id);
                if (c != null && c.moveToFirst()) {
                    String name = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_BUDDY_NAME));
                    String phone = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_BUDDY_PHONE));
                    String dob = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_BUDDY_DOB));
                    String email = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_BUDDY_EMAIL));
                    String gender = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_BUDDY_GENDER));

                    // Load photo
                    int photoIndex = c.getColumnIndex(DatabaseHelper.COL_BUDDY_PHOTO);
                    String photoBase64 = (photoIndex != -1) ? c.getString(photoIndex) : null;

                    mainHandler.post(() -> {
                        if (etName != null) etName.setText(name);
                        if (etPhone != null) etPhone.setText(phone);
                        if (etDob != null) etDob.setText(dob);
                        if (etEmail != null) etEmail.setText(email);

                        // Set photo if exists
                        if (photoBase64 != null && !photoBase64.isEmpty() && ivBuddyPhoto != null) {
                            currentPhotoBase64 = photoBase64;
                            Bitmap bitmap = base64ToBitmap(photoBase64);
                            ivBuddyPhoto.setImageBitmap(bitmap);
                            ivBuddyPhoto.setPadding(0, 0, 0, 0);
                            ivBuddyPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            ivBuddyPhoto.setImageTintList(null);
                        }

                        if ("Male".equals(gender) && rbMale != null) {
                            rbMale.setChecked(true);
                            updateGenderCardSelection(cardMale, cardFemale, true);
                        } else if ("Female".equals(gender) && rbFemale != null) {
                            rbFemale.setChecked(true);
                            updateGenderCardSelection(cardMale, cardFemale, false);
                        }
                    });
                }
                if (c != null) c.close();
            });
        }

        // Handle Save button
        v.findViewById(R.id.btnSaveAdd).setOnClickListener(view -> {
            if (etName == null || etPhone == null || etDob == null || etEmail == null || rbMale == null || rbFemale == null) return;

            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
            String dob = etDob.getText() != null ? etDob.getText().toString().trim() : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String gender = rbMale.isChecked() ? "Male" : (rbFemale.isChecked() ? "Female" : "Other");

            // UPDATED VALIDATION - All fields required except Email and Photo
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (dob.isEmpty()) {
                Toast.makeText(this, "Please select a birthday", Toast.LENGTH_SHORT).show();
                return;
            }
            if (phone.isEmpty()) {
                Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!rbMale.isChecked() && !rbFemale.isChecked()) {
                Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show();
                return;
            }

            // Format phone number with +60 prefix
            if (!phone.isEmpty()) {
                phone = phone.replaceAll("[^0-9]", "");
                if (phone.startsWith("60")) {
                    phone = phone.substring(2);
                }
                if (phone.startsWith("0")) {
                    phone = phone.substring(1);
                }
                phone = "+60" + phone;
            }

            String finalPhone = phone;
            // Make sure photo is properly captured - use empty string if null
            String finalPhoto = (currentPhotoBase64 != null && !currentPhotoBase64.trim().isEmpty()) ? currentPhotoBase64 : "";

            executorService.execute(() -> {
                boolean success;
                if (id == null) {
                    success = db.insertBuddy(name, gender, dob, finalPhone, email, finalPhoto, userId);
                } else {
                    success = db.updateBuddy(String.valueOf(id), name, gender, dob, finalPhone, email, finalPhoto);
                }

                if (success) {
                    applyFilters();
                    loadUpcomingBirthday();

                    mainHandler.post(() -> {
                        Toast.makeText(this, id == null ? "Buddy added!" : "Buddy updated!", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Error saving buddy", Toast.LENGTH_SHORT).show();
                    });
                }
            });

            dialog.dismiss();
        });

        // Handle Cancel button
        v.findViewById(R.id.btnCancelAdd).setOnClickListener(view -> dialog.dismiss());

        dialog.show();

        // Animate dialog entry
        animateDialogEntry(v);
    }

    private void updateGenderCardSelection(MaterialCardView cardMale, MaterialCardView cardFemale, boolean isMaleSelected) {
        if (cardMale == null || cardFemale == null) return;

        if (isMaleSelected) {
            cardMale.setCardElevation(8f);
            cardMale.setStrokeWidth(4);
            cardMale.setStrokeColor(getColor(R.color.primary));

            cardFemale.setCardElevation(2f);
            cardFemale.setStrokeWidth(0);
        } else {
            cardFemale.setCardElevation(8f);
            cardFemale.setStrokeWidth(4);
            cardFemale.setStrokeColor(getColor(R.color.primary));

            cardMale.setCardElevation(2f);
            cardMale.setStrokeWidth(0);
        }
    }

    private void confirmDelete(long buddyId) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this buddy?")
                .setPositiveButton("Delete", (dialog, which) -> executorService.execute(() -> {
                    db.deleteBuddy(String.valueOf(buddyId));
                    // Refresh the list with current filters
                    applyFilters();
                    // Also refresh the upcoming birthday card
                    loadUpcomingBirthday();
                }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendWhatsApp() {
        if (selectedBuddyId == -1) return;

        executorService.execute(() -> {
            Cursor c = db.getBuddyById(selectedBuddyId);
            if (c != null && c.moveToFirst()) {
                String phone = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_BUDDY_PHONE));
                String name = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_BUDDY_NAME));

                mainHandler.post(() -> {
                    try {
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_VIEW);
                        String url = "https://api.whatsapp.com/send?phone=" + phone + "&text=" + "Happy Birthday " + name + "!";
                        sendIntent.setData(Uri.parse(url));
                        startActivity(sendIntent);
                    } catch (Exception e) {
                        Toast.makeText(this, "WhatsApp not installed.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            if (c != null) c.close();
        });
    }

    // Photo helper methods
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String base64String) {
        byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float ratioBitmap = (float) width / (float) height;
        float ratioMax = (float) maxWidth / (float) maxHeight;

        int finalWidth = maxWidth;
        int finalHeight = maxHeight;

        if (ratioMax > ratioBitmap) {
            finalWidth = (int) ((float) maxHeight * ratioBitmap);
        } else {
            finalHeight = (int) ((float) maxWidth / ratioBitmap);
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);
    }

    // Animation Methods
    private void initAnimationViews() {
        appBarLayout = findViewById(R.id.appBarLayout);
        upcomingBirthdayCard = findViewById(R.id.cardUpcomingBirthday);
        fabAddBuddy = findViewById(R.id.fab_add_buddy);
        fabStatistics = findViewById(R.id.fab_statistics);

        // Set initial visibility for animated views
        if (appBarLayout != null) appBarLayout.setAlpha(0f);
        if (upcomingBirthdayCard != null) upcomingBirthdayCard.setAlpha(0f);
        if (fabAddBuddy != null) fabAddBuddy.setAlpha(0f);
        if (fabStatistics != null) fabStatistics.setAlpha(0f);
        if (recyclerView != null) recyclerView.setAlpha(0f);
    }

    private void startEntranceAnimations() {
        // Animate App Bar - Slide down and fade in
        if (appBarLayout != null) {
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(600);

            TranslateAnimation slideDown = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, -0.5f,
                    Animation.RELATIVE_TO_SELF, 0
            );
            slideDown.setDuration(600);

            AnimationSet appBarAnimation = new AnimationSet(true);
            appBarAnimation.addAnimation(fadeIn);
            appBarAnimation.addAnimation(slideDown);
            appBarAnimation.setInterpolator(new DecelerateInterpolator());

            appBarLayout.startAnimation(appBarAnimation);
            appBarLayout.setAlpha(1f);
        }

        // Note: Birthday card animation is handled in loadUpcomingBirthday() when data loads

        // Animate RecyclerView - Fade in
        if (recyclerView != null) {
            new Handler().postDelayed(() -> {
                AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                fadeIn.setDuration(600);
                fadeIn.setInterpolator(new DecelerateInterpolator());

                recyclerView.startAnimation(fadeIn);
                recyclerView.setAlpha(1f);
            }, 500);
        }

        // Animate FABs - Scale and fade in
        animateFAB(fabStatistics, 700);
        animateFAB(fabAddBuddy, 850);
    }

    private void animateFAB(FloatingActionButton fab, long delay) {
        if (fab != null) {
            new Handler().postDelayed(() -> {
                fab.setScaleX(0f);
                fab.setScaleY(0f);
                fab.setAlpha(0f);

                fab.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(400)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }, delay);
        }
    }
}