package com.example.kzmusic;

//Imports
import com.google.android.exoplayer2.ExoPlayer;
import java.util.ArrayList;
import java.util.List;
import com.spotify.android.appremote.api.PlayerApi;

//This class manages player sessions for seamless playing
public class PlayerManager {

    private static PlayerManager instance;
    private final List<ExoPlayer> playerList;
    ExoPlayer current_player;
    private final List<PlayerApi> Spotify_playerList;

    private PlayerManager() {
        this.playerList = new ArrayList<>();
        this.Spotify_playerList = new ArrayList<>();
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

    public void removePlayer(ExoPlayer player) {
        playerList.remove(player);
    }


    public void stopAllPlayers() {
        for (ExoPlayer player : playerList) {
            player.stop();
        }
    }
    public int get_size() {
        return playerList.size();
    }

    public void setCurrent_player(ExoPlayer current_player) {
        this.current_player = current_player;
    }
}
