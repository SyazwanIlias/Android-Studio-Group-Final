package com.example.studentmanagerapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private BuddyAdapter adapter;
    private RecyclerView recyclerView;
    private long userId, selectedBuddyId = -1;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);
        userId = getIntent().getLongExtra("USER_ID", 1);

        recyclerView = findViewById(R.id.rvBuddies);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        FloatingActionButton btnAdd = findViewById(R.id.btnAdd);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> showBuddyDialog(null));
        }

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

        setupBottomNavigation();
        loadBuddies();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav == null) return;

        bottomNav.setBackground(null);
        bottomNav.setItemActiveIndicatorEnabled(false);

        // Setup color changing for selected/unselected states
        setupBottomNavColors(bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // Already on home screen
                return true;

            } else if (id == R.id.nav_stats) {
                // Open statistics screen
                Intent i = new Intent(this, ReportsActivity.class);
                i.putExtra("USER_ID", userId);
                startActivity(i);
                return true;

            } else if (id == R.id.nav_wish) {
                // Send WhatsApp wish
                sendWhatsApp();
                return true;

            } else if (id == R.id.nav_settings) {
                // Settings placeholder
                Toast.makeText(this, "Settings - Coming Soon", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    private void setupBottomNavColors(BottomNavigationView bottomNav) {
        // Create color state list for icon and text colors
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },   // selected
                new int[] { -android.R.attr.state_checked }   // unselected
        };

        int[] colors = new int[] {
                ContextCompat.getColor(this, R.color.primary),         // green when selected
                ContextCompat.getColor(this, R.color.text_tertiary)    // grey when unselected
        };

        ColorStateList colorStateList = new ColorStateList(states, colors);
        bottomNav.setItemIconTintList(colorStateList);
        bottomNav.setItemTextColor(colorStateList);
    }

    private void loadBuddies() {
        if (recyclerView == null) return;

        executorService.execute(() -> {
            Cursor cursor = db.getAllBuddiesForUser(userId);

            mainHandler.post(() -> {
                if (adapter == null) {
                    adapter = new BuddyAdapter(cursor);

                    // Item click now shows options dialog (Update, Delete, Send Wish)
                    adapter.setOnItemClickListener(id -> {
                        selectedBuddyId = id;
                        showBuddyOptionsDialog(id);
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

                mainHandler.post(() -> {
                    new AlertDialog.Builder(this)
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
                            .show();
                });
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
                    String date = y + "-" + String.format("%02d", (m + 1)) + "-" + String.format("%02d", d);
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
                    String name = etName.getText().toString();
                    String phone = etPhone.getText().toString();
                    String dob = etDob.getText().toString();
                    String email = etEmail.getText().toString();
                    String gender = rbMale.isChecked() ? "Male" : (rbFemale.isChecked() ? "Female" : "Other");

                    executorService.execute(() -> {
                        if (id == null) db.insertBuddy(name, gender, dob, phone, email, userId);
                        else db.updateBuddy(String.valueOf(id), name, gender, dob, phone, email);
                        loadBuddies();
                        mainHandler.post(() -> {
                            Toast.makeText(MainActivity.this,
                                    id == null ? "Buddy added!" : "Buddy updated!",
                                    Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendWhatsApp() {
        if (selectedBuddyId == -1) {
            Toast.makeText(this, "Select a buddy first!", Toast.LENGTH_SHORT).show();
            return;
        }
        executorService.execute(() -> {
            Cursor c = db.getBuddyById(selectedBuddyId);
            if (c != null && c.moveToFirst()) {
                String phone = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_BUDDY_PHONE));
                String cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "");
                mainHandler.post(() -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=Hello!"));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "WhatsApp not installed!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            if (c != null) c.close();
        });
    }

    private void confirmDelete(long buddyId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Buddy")
                .setMessage("Are you sure you want to delete this buddy?")
                .setPositiveButton("Yes", (d, w) -> executorService.execute(() -> {
                    db.deleteBuddy(String.valueOf(buddyId));
                    selectedBuddyId = -1;
                    loadBuddies();
                    mainHandler.post(() -> {
                        Toast.makeText(MainActivity.this, "Buddy deleted!", Toast.LENGTH_SHORT).show();
                    });
                }))
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}