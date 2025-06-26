package com.example.kzmusic;

//Imports
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

//RoomDB for data access object
@Dao
public interface SongDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Song song);

    @Query("SELECT * FROM songs WHERE email = :email AND title = :title LIMIT 1")
    Song getSongByEmailAndTitle(String email, String title);

    @Query("UPDATE songs SET times_played = times_played + 1 WHERE email = :email AND title = :title")
    void incrementTimesPlayed(String email, String title);
    @Query("SELECT * FROM songs WHERE email = :email ORDER BY times_played DESC LIMIT 100")
    List<Song> getTopSongsByUser(String email);

}
