package com.example.kzmusic;

//Imports
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.google.android.exoplayer2.ExoPlayer;
import java.util.ArrayList;
import java.util.List;

//This class manages player sessions for seamless playing
public class OfflinePlayerManager {

    private static OfflinePlayerManager instance;
    private final List<ExoPlayer> playerList;
    List<MediaSessionCompat> sessions;
    ExoPlayer current_player;
    PlaybackStateCompat.Builder current_builder;
    Boolean spotify_playing = false;


    private OfflinePlayerManager() {
        this.playerList = new ArrayList<>();
        this.sessions = new ArrayList<>();
    }

    public static synchronized OfflinePlayerManager getInstance() {
        if (instance == null) {
            instance = new OfflinePlayerManager();
        }
        return instance;
    }

    public void addSession(MediaSessionCompat session) {
        sessions.add(session);
    }
    public void resetToDefaults() {
        // Stop and release all players
        // Stop and release all media sessions
        if (sessions != null && !sessions.isEmpty()) {
            for (MediaSessionCompat session : sessions) {
                session.getController().getTransportControls().stop();
                session.release();
            }
            sessions.clear();
        }
        // Reset references and flags
        current_player = null;
        current_builder = null;
        spotify_playing = false;
    }

}
