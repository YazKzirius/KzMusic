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

   @Query("UPDATE Playlists SET url = :url, playlist_title = :title WHERE email = :email AND id = :id")
    void update_playlist(String email, int id, String title, String url);

    @Query("DELETE FROM Playlists WHERE email = :email AND id = :id")
    void delete_playlist(String email, int id);
    @Query("SELECT url FROM Playlists WHERE email = :email AND playlist_title = :title")
    String getUrl(String email, String title);
    @Query("SELECT * FROM Playlists WHERE email = :email")
    List<Playlist> getAllPlaylists(String email);


}
