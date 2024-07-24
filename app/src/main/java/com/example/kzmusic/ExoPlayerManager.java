package com.example.kzmusic;

//Imports
import com.google.android.exoplayer2.SimpleExoPlayer;
import java.util.ArrayList;
import java.util.List;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

//This class manages player sessions for seamless playing
public class ExoPlayerManager {

    private static ExoPlayerManager instance;
    private final List<SimpleExoPlayer> playerList;
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
