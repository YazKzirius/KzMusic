package com.example.kzmusic;
//Import important modules

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class KzmusicDatabase extends SQLiteOpenHelper {
    //Listing table attributes
    public static final String DATABASE_NAME = "Kzmusic.db";

    public KzmusicDatabase(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }
    @Override
    //Creating Database Tables
    public void onCreate(SQLiteDatabase db) {
        create_users(db);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //List of table names
        String[] tables = {"Users", "Songs", "Artists", "Albums", "Playlists", "PlaylistSongs"};
        // Drop table if exists
        for (String table : tables) {
            db.execSQL("DROP TABLE IF EXISTS " + table);
        }
        onCreate(db);
    }
    //This function creates Users table
    public void create_users(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Users (UserID INTEGER PRIMARY KEY AUTOINCREMENT, USERNAME TEXT, EMAIL TEXT, PASSWORD TEXT)");
    }
    //This function creates Songs table
    public void create_songs(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Songs (SongID INTEGER PRIMARY KEY AUTOINCREMENT, TITLE TEXT, ArtistID INTEGER, AlbumID INTEGER, GENRE TEXT, DURATION INTEGER, RELEASE_DATE DATE, " +
                "FOREIGN KEY (ArtistID) REFERENCES Artists(ArtistID), FOREIGN KEY (AlbumnID) REFERENCES Albumns(AlbumID))");
    }
    //This function creates the Artists table
    public void create_artists(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Artists (ArtistID INTEGER PRIMARY KEY AUTOINCREMENT, NAME TEXT, COUNTRY TEXT, DEBUTE DATE)");
    }
    //This function creates the Albums table
    public void create_albums(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Albums (AlbumID INTEGER PRIMARY KEY AUTOINCREMENT, TITLE TEXT, ArtistID INTEGER, RELEASE_DATE DATE, GENRE TEXT," +
                "FOREIGN KEY (ArtistID) REFERENCES Artists(ArtistID))");
    }
    //This function creates the Playlists table
    public void create_playlists(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Playlists (PlaylistID INTEGER PRIMARY KEY AUTOINCREMENT, UserID INTEGER, NAME TEXT, DESCRIPTION TEXT, DATE_CREATED DATE, " +
                "FOREIGN KEY (UserID) REFERENCES Users(UserID))");
    }
    //This function creates the Playlist Songs table
    public void create_playlist_songs(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE PlaylistSongs (PlaylistID INTEGER, SongID INTEGER," +
                "FOREIGN KEY (PlaylistID) REFERENCES Playlists(PlaylistID), FOREIGN KEY (SongID) REFERENCES Songs(SongID))");
    }

}