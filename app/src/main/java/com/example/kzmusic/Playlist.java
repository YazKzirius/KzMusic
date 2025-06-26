package com.example.kzmusic;

//Imports
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "Playlists")
public class Playlist {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "email")
    public String email;

    @ColumnInfo(name = "playlist_title")
    public String title;

    @ColumnInfo(name = "url")
    public String url;



}