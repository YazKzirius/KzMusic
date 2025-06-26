package com.example.kzmusic;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

//RoomDB for data access object
@Dao
public interface PlaylistSongDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(PlaylistSong playlistSong);


    @Query("DELETE FROM PlaylistSongs WHERE email = :email AND playlist_id = :playlistID AND title = :title")
    void remove_song(String email, int playlistID, String title);

    @Query("SELECT title FROM PlaylistSongs WHERE email = :email AND playlist_id = :playlistID AND title = :title LIMIT 1")
    String get_playlist_song(String email, int playlistID, String title);

    @Query("SELECT title FROM PlaylistSongs WHERE email = :email AND playlist_id = :playlistID")
    List<String> get_playlist_songs(String email, int playlistID);


}
