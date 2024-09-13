package com.example.kzmusic;
//Import important modules


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class KzmusicDatabase extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 4;
    public static final String DATABASE_NAME = "Kzmusic.db";

    public KzmusicDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys = ON");
        create_users(db);
        create_songs(db);
        create_liked(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            // Step 1: Create the new Songs table if it doesn't exist
            boolean songsTableExists = false;
            Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='Songs';", null);
            if (cursor != null && cursor.getCount() > 0) {
                songsTableExists = true;
            }
            if (cursor != null) {
                cursor.close();
            }

            if (!songsTableExists) {
                create_songs(db);
            }

            // Step 2: Check if the LikedSongs table exists and update it as necessary
            boolean likedSongsTableExists = false;
            Cursor cursorLiked = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='LikedSongs';", null);
            if (cursorLiked != null && cursorLiked.getCount() > 0) {
                likedSongsTableExists = true;
            }
            if (cursorLiked != null) {
                cursorLiked.close();
            }

            if (likedSongsTableExists) {
                db.execSQL("ALTER TABLE LikedSongs RENAME TO temp_LikedSongs");

                // Create new LikedSongs table with updated schema
                db.execSQL("CREATE TABLE IF NOT EXISTS LikedSongs (" +
                        "UserID INTEGER, " +
                        "likedSongID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "TITLE TEXT, " +
                        "ALBUM_URL TEXT, " + // New column
                        "TIMES_PLAYED INTEGER, " +
                        "FOREIGN KEY (UserID) REFERENCES Users(UserID))");

                // Copy data from old table to new table
                db.execSQL("INSERT INTO LikedSongs (UserID, likedSongID, TITLE, TIMES_PLAYED) " +
                        "SELECT UserID, likedSongID, TITLE, TIMES_PLAYED FROM temp_LikedSongs");

                // Drop the temporary table
                db.execSQL("DROP TABLE temp_LikedSongs");
            } else {
                create_liked(db);
            }
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // Enable foreign key constraints
        db.setForeignKeyConstraintsEnabled(true);
    }

    // This function creates Users table which stores ID, Username, and Email
    public void create_users(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Users (UserID INTEGER PRIMARY KEY AUTOINCREMENT, USERNAME TEXT, EMAIL TEXT, PASSWORD TEXT)");
    }

    // This function creates Songs table
    public void create_songs(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Songs (SongID INTEGER PRIMARY KEY AUTOINCREMENT, UserID INTEGER, TITLE TEXT, TOTAL_DURATION INTEGER, TIMES_PLAYED INTEGER, " +
                "FOREIGN KEY (UserID) REFERENCES Users(UserID))");
    }

    // This function creates liked songs table
    public void create_liked(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE LikedSongs (UserID INTEGER, likedSongID INTEGER PRIMARY KEY AUTOINCREMENT, TITLE TEXT, ALBUM_URL TEXT, TIMES_PLAYED INTEGER, " +
                "FOREIGN KEY (UserID) REFERENCES Users(UserID))");
    }
}