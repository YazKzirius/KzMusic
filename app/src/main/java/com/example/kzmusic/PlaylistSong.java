package com.example.kzmusic;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "PlaylistSongs")
public class PlaylistSong {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "playlist_id")
    public int playlist_id;

    @ColumnInfo(name = "email")
    public String email;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "artist")
    public String artist;
}
