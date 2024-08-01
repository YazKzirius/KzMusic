package com.example.kzmusic;

//Imports
import com.google.android.exoplayer2.ExoPlayer;
import java.util.ArrayList;
import java.util.List;
import com.spotify.android.appremote.api.PlayerApi;

//This class manages player sessions for seamless playing
public class ExoPlayerManager {

    private static ExoPlayerManager instance;
    private final List<ExoPlayer> playerList;
    private final List<PlayerApi> Spotify_playerList;

    private ExoPlayerManager() {
        this.playerList = new ArrayList<>();
        this.Spotify_playerList = new ArrayList<>();
    }

    public static synchronized ExoPlayerManager getInstance() {
        if (instance == null) {
            instance = new ExoPlayerManager();
        }
        return instance;
    }

    public void addPlayer(ExoPlayer player) {
        playerList.add(player);
    }

    public void removePlayer(ExoPlayer player) {
        playerList.remove(player);
    }


    public void stopAllPlayers() {
        for (ExoPlayer player : playerList) {
            player.stop();
        }
    }
}
