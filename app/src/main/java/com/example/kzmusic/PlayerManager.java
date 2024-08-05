package com.example.kzmusic;

//Imports
import com.google.android.exoplayer2.ExoPlayer;
import java.util.ArrayList;
import java.util.List;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

//This class manages player sessions for seamless playing
public class PlayerManager {

    private static PlayerManager instance;
    private final List<ExoPlayer> playerList;
    ExoPlayer current_player;
    PlayerApi spotify_player;
    Boolean spotify_playing = false;
    SpotifyAppRemote current_remote;
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

    public void setCurrent_remote(SpotifyAppRemote current_remote) {
        this.current_remote = current_remote;
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
    public void addSpotifyPlayer(PlayerApi player) {
        Spotify_playerList.add(player);
        this.spotify_player = player;
    }

    public void setSpotify_player(PlayerApi spotify_player) {
        this.spotify_player = spotify_player;
    }
    public void set_is_playing() {
        spotify_player.subscribeToPlayerState().setEventCallback(new Subscription.EventCallback<PlayerState>() {
            @Override
            public void onEvent(PlayerState playerState) {
                if (playerState.isPaused) {
                    spotify_playing = false;
                } else {
                    spotify_playing = true;
                }
            }
        });
    }
}
