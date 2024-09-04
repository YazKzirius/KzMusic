package com.example.kzmusic;
//Import important modules

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

//This class implemnents the main Kzmusic database
//Used for user data collection
public class KzmusicDatabase extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 2;
    //Listing database attributes
    public static final String DATABASE_NAME = "Kzmusic.db";

    public KzmusicDatabase(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }
    @Override
    //Creating Database Tables
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys = ON");
        create_users(db);
        create_songs(db);
        create_liked(db);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //List of table names
        String[] tables = {"Users",};
        // Drop table if exists
        for (String table : tables) {
            db.execSQL("DROP TABLE IF EXISTS " + table);
        }
        onCreate(db);
    }
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // Enable foreign key constraints
        db.setForeignKeyConstraintsEnabled(true);
    }
    //This function creates Users table which stores ID, Username and Email
    public void create_users(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Users (UserID INTEGER PRIMARY KEY AUTOINCREMENT, USERNAME TEXT, EMAIL TEXT, PASSWORD TEXT)");
    }
    //This function creates Songs table
    public void create_songs(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Songs (SongID INTEGER PRIMARY KEY AUTOINCREMENT, UserID INTEGER, TITLE TEXT, TOTAL_DURATION INTEGER, TIMES_PLAYED INTEGER, " +
                "FOREIGN KEY (UserID) REFERENCES Users(UserID))");
    }
    //This function creates liked songs table
    public void create_liked(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE LikedSongs (UserID INTEGER, likedSongID INTEGER PRIMARY KEY AUTOINCREMENT, TITLE TEXT, TIMES_PLAYED INTEGER, " +
                "FOREIGN KEY (UserID) REFERENCES Users(UserID))");
    }


}