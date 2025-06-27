package com.example.kzmusic;

//Imports
import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Song.class, Playlist.class, PlaylistSong.class}, version = 6)
public abstract class AppDatabase extends RoomDatabase {

    public abstract SongDao songDao();
    public abstract PlaylistDao playlistDao(); // 👈 New DAO
    public abstract PlaylistSongDao playlistSongDao(); // 👈 New DAO

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 2;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context,
                                    AppDatabase.class, "local_music_db")
                            .fallbackToDestructiveMigration() // Dev-friendly, not for production
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
