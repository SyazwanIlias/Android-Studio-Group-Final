package com.example.studentmanagerapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "BuddyApp.db";
    public static final int DATABASE_VERSION = 8;

    // Users table
    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD = "password";

    // Buddies table
    public static final String TABLE_BUDDIES = "buddies";
    public static final String COL_BUDDY_ID = "id";
    public static final String COL_BUDDY_NAME = "name";
    public static final String COL_BUDDY_GENDER = "gender";
    public static final String COL_BUDDY_DOB = "dob";
    public static final String COL_BUDDY_PHONE = "phone";
    public static final String COL_BUDDY_EMAIL = "email";
    public static final String COL_BUDDY_USER_ID = "user_id";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COL_USERNAME + " TEXT UNIQUE," +
                COL_PASSWORD + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_BUDDIES + " (" +
                COL_BUDDY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COL_BUDDY_NAME + " TEXT," +
                COL_BUDDY_GENDER + " TEXT," +
                COL_BUDDY_DOB + " TEXT," +
                COL_BUDDY_PHONE + " TEXT," +
                COL_BUDDY_EMAIL + " TEXT," +
                COL_BUDDY_USER_ID + " INTEGER," +
                "FOREIGN KEY(" + COL_BUDDY_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + "))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUDDIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // --- USER METHODS ---

    public boolean insertUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_USERNAME, username);
        cv.put(COL_PASSWORD, password);
        return db.insert(TABLE_USERS, null, cv) != -1;
    }

    public Cursor checkLoginAndGetId(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT " + COL_USER_ID + " FROM " + TABLE_USERS +
                        " WHERE " + COL_USERNAME + "=? AND " + COL_PASSWORD + "=?",
                new String[]{username, password});
    }

    // Get username by userId
    public Cursor getUsernameByUserId(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT " + COL_USERNAME + " FROM " + TABLE_USERS + " WHERE " + COL_USER_ID + "=?",
                new String[]{String.valueOf(userId)}
        );
    }

    // --- BUDDY CRUD METHODS ---

    public boolean insertBuddy(String name, String gender, String dob, String phone, String email, long userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_BUDDY_NAME, name);
        cv.put(COL_BUDDY_GENDER, gender);
        cv.put(COL_BUDDY_DOB, dob);
        cv.put(COL_BUDDY_PHONE, phone);
        cv.put(COL_BUDDY_EMAIL, email);
        cv.put(COL_BUDDY_USER_ID, userId);
        return db.insert(TABLE_BUDDIES, null, cv) != -1;
    }

    public boolean updateBuddy(String id, String name, String gender, String dob, String phone, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_BUDDY_NAME, name);
        cv.put(COL_BUDDY_GENDER, gender);
        cv.put(COL_BUDDY_DOB, dob);
        cv.put(COL_BUDDY_PHONE, phone);
        cv.put(COL_BUDDY_EMAIL, email);
        return db.update(TABLE_BUDDIES, cv, COL_BUDDY_ID + "=?", new String[]{id}) > 0;
    }

    public boolean deleteBuddy(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_BUDDIES, COL_BUDDY_ID + "=?", new String[]{id}) > 0;
    }

    public Cursor getAllBuddiesForUser(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_BUDDIES + " WHERE " + COL_BUDDY_USER_ID + "=? ORDER BY " + COL_BUDDY_NAME + " ASC",
                new String[]{String.valueOf(userId)});
    }

    public Cursor getBuddyById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_BUDDIES + " WHERE " + COL_BUDDY_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    public Cursor searchBuddiesForUser(String query, long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_BUDDIES + " WHERE " + COL_BUDDY_USER_ID + "=? AND (" + COL_BUDDY_NAME + " LIKE ? OR " + COL_BUDDY_PHONE + " LIKE ?)",
                new String[]{String.valueOf(userId), "%" + query + "%", "%" + query + "%"});
    }

    public Cursor getBuddiesByMonth(long userId, String month) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_BUDDIES + " WHERE " + COL_BUDDY_USER_ID + "=? AND " + COL_BUDDY_DOB + " LIKE ?",
                new String[]{String.valueOf(userId), "%-" + month + "-%"});
    }

    // --- ANALYTICS & REPORTS METHODS (FIXED FOR REPORTS ACTIVITY) ---

    public int getBuddyCount(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_BUDDIES + " WHERE " + COL_BUDDY_USER_ID + "=?",
                new String[]{String.valueOf(userId)});
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    // New method required by ReportsActivity to get count by specific gender
    public int getGenderCount(long userId, String gender) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_BUDDIES +
                        " WHERE " + COL_BUDDY_USER_ID + "=? AND " + COL_BUDDY_GENDER + "=?",
                new String[]{String.valueOf(userId), gender});
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    // New method required by ReportsActivity to get count by specific month
    public int getBuddyCountByMonth(long userId, String monthNum) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Since dates are YYYY-MM-DD, we look for "-MM-" pattern
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_BUDDIES +
                        " WHERE " + COL_BUDDY_USER_ID + "=? AND " + COL_BUDDY_DOB + " LIKE ?",
                new String[]{String.valueOf(userId), "%-" + monthNum + "-%"});
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public Cursor getAllBuddiesWithDob(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_BUDDIES + " WHERE " + COL_BUDDY_USER_ID + "=? AND " + COL_BUDDY_DOB + " IS NOT NULL AND " + COL_BUDDY_DOB + " != ''",
                new String[]{String.valueOf(userId)});
    }
}