package com.example.kzmusic;

//Imports
import com.google.android.exoplayer2.SimpleExoPlayer;
import java.util.ArrayList;
import java.util.List;

//This class manages player sessions for seamless playing
public class ExoPlayerManager {

    private static ExoPlayerManager instance;
    private final List<SimpleExoPlayer> playerList;

    private ExoPlayerManager() {
        playerList = new ArrayList<>();
    }

    public static synchronized ExoPlayerManager getInstance() {
        if (instance == null) {
            instance = new ExoPlayerManager();
        }
        return instance;
    }

    public void addPlayer(SimpleExoPlayer player) {
        playerList.add(player);
    }

    public void removePlayer(SimpleExoPlayer player) {
        playerList.remove(player);
    }

    public void stopAllPlayers() {
        for (SimpleExoPlayer player : playerList) {
            player.stop();
        }
    }
}
