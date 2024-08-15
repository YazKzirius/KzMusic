package com.example.kzmusic;

//Imports
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.google.android.exoplayer2.ExoPlayer;
import java.util.ArrayList;
import java.util.List;

//This class manages player sessions for seamless playing
public class PlayerManager {

    private static PlayerManager instance;
    private final List<ExoPlayer> playerList;
    private final List<MediaSessionCompat> sessions;
    ExoPlayer current_player;
    PlaybackStateCompat.Builder current_builder;
    Boolean spotify_playing = false;


    private PlayerManager() {
        this.playerList = new ArrayList<>();
        this.sessions = new ArrayList<>();
    }

    public static synchronized PlayerManager getInstance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    public void addPlayer(ExoPlayer player) {
        playerList.add(player);
    }
    public void addSession(MediaSessionCompat session) {
        sessions.add(session);
    }

    public void removePlayer(ExoPlayer player) {
        playerList.remove(player);
    }

    public void setCurrent_builder(PlaybackStateCompat.Builder current_builder) {
        this.current_builder = current_builder;
    }

    public void stopAllPlayers() {
        for (ExoPlayer player : playerList) {
            player.stop();
        }
    }
    public void StopAllSessions() {
        for (MediaSessionCompat session : sessions) {
            session.getController().getTransportControls().stop();
            session.release();
        }
    }
    public int get_size() {
        return playerList.size();
    }

    public void setCurrent_player(ExoPlayer current_player) {
        this.current_player = current_player;
    }

}
