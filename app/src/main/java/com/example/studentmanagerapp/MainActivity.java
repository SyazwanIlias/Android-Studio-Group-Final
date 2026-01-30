package com.example.studentmanagerapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
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
    private long userId, selectedBuddyId = -1;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        loadBuddies();
        loadUpcomingBirthday();
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
                if (position == 0) {
                    loadBuddies();
                } else {
                    String month = String.format(Locale.US, "%02d", position);
                    loadBuddiesByMonth(month);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                loadBuddies();
            }
        });
    }

    private void loadBuddiesByMonth(String month) {
        if (recyclerView == null) return;

        executorService.execute(() -> {
            Cursor cursor = db.getBuddiesByMonth(userId, month);

            mainHandler.post(() -> {
                if (adapter == null) {
                    adapter = new BuddyAdapter(cursor);
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.swapCursor(cursor);
                }
            });
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

                mainHandler.post(() -> new AlertDialog.Builder(this)
                        .setTitle(name)
                        .setItems(new String[]{"Update", "Delete", "Send Wish"}, (dialog, which) -> {
                            switch (which) {
                                case 0: // Update
                                    showBuddyDialog(buddyId);
                                    break;
                                case 1: // Delete
                                    confirmDelete(buddyId);
                                    break;
                                case 2: // Send Wish
                                    sendWhatsApp();
                                    break;
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show());
            }
            if (c != null) c.close();
        });
    }

    private void showBuddyDialog(Long id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_buddy, null);

        TextInputEditText etName = v.findViewById(R.id.etBuddyName);
        TextInputEditText etPhone = v.findViewById(R.id.etPhone);
        TextInputEditText etDob = v.findViewById(R.id.etDOB);
        TextInputEditText etEmail = v.findViewById(R.id.etEmail);
        RadioGroup rgGender = v.findViewById(R.id.rgGender);
        RadioButton rbMale = v.findViewById(R.id.rbMale);
        RadioButton rbFemale = v.findViewById(R.id.rbFemale);

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

                    mainHandler.post(() -> {
                        if (etName != null) etName.setText(name);
                        if (etPhone != null) etPhone.setText(phone);
                        if (etDob != null) etDob.setText(dob);
                        if (etEmail != null) etEmail.setText(email);
                        if ("Male".equals(gender) && rbMale != null) rbMale.setChecked(true);
                        else if ("Female".equals(gender) && rbFemale != null) rbFemale.setChecked(true);
                    });
                }
                if (c != null) c.close();
            });
        }

        builder.setView(v)
                .setTitle(id == null ? "Add Buddy" : "Update Buddy")
                .setPositiveButton("Save", (dialog, which) -> {
                    if (etName == null || etPhone == null || etDob == null || etEmail == null || rgGender == null) return;
                    String name = etName.getText() != null ? etName.getText().toString() : "";
                    String phone = etPhone.getText() != null ? etPhone.getText().toString() : "";
                    String dob = etDob.getText() != null ? etDob.getText().toString() : "";
                    String email = etEmail.getText() != null ? etEmail.getText().toString() : "";
                    String gender = rbMale.isChecked() ? "Male" : (rbFemale.isChecked() ? "Female" : "Other");

                    executorService.execute(() -> {
                        if (id == null) {
                            db.insertBuddy(name, gender, dob, phone, email, userId);
                        } else {
                            db.updateBuddy(String.valueOf(id), name, gender, dob, phone, email);
                        }
                        loadBuddies();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete(long buddyId) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this buddy?")
                .setPositiveButton("Delete", (dialog, which) -> executorService.execute(() -> {
                    db.deleteBuddy(String.valueOf(buddyId));
                    loadBuddies();
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
}
