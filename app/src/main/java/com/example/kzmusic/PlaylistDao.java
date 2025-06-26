package com.example.kzmusic;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

//RoomDB for data access object
@Dao
public interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Playlist playlist);

    @Query("SELECT * FROM Playlists WHERE email = :email AND playlist_title = :title LIMIT 1")
    Playlist getPlaylistByEmailAndTitle(String email, String title);
    @Query("SELECT id FROM Playlists WHERE email = :email AND playlist_title = :title LIMIT 1")
    int getPlaylistIdByEmailAndTitle(String email, String title);
    @Query("UPDATE Playlists SET playlist_title = :title, url = :url WHERE email = :email AND playlist_title = :title")
    void update_playlist(String email, String title, String url);
    @Query("SELECT url FROM Playlists WHERE email = :email AND playlist_title = :title")
    String getUrl(String email, String title);
    @Query("SELECT * FROM Playlists WHERE email = :email")
    List<Playlist> getAllPlaylists(String email);


}
