package com.example.kzmusic;

import android.util.Log;
import android.widget.Toast;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

public class SpotifyPlayerLife {
    SpotifyAppRemote mSpotifyAppRemote;
    SearchResponse.Track current_track;
    private static SpotifyPlayerLife instance;
    public static synchronized SpotifyPlayerLife getInstance() {
        if (instance == null) {
            instance = new SpotifyPlayerLife();
        }
        return instance;
    }

    public void setCurrent_track(SearchResponse.Track current_track) {
        this.current_track = current_track;
    }
    public void setmSpotifyAppRemote(SpotifyAppRemote mSpotifyAppRemote) {
        this.mSpotifyAppRemote = mSpotifyAppRemote;
    }
    public void pause_playback() {
        if (mSpotifyAppRemote.isConnected() && mSpotifyAppRemote != null) {
            mSpotifyAppRemote.getPlayerApi().pause();
        }
    }

    public void stopPlaybackAndDisconnect() {
        if (mSpotifyAppRemote != null && mSpotifyAppRemote.isConnected()) {
            mSpotifyAppRemote.getPlayerApi().pause().setResultCallback(empty -> {
                SpotifyAppRemote.disconnect(mSpotifyAppRemote);
                mSpotifyAppRemote = null;
                current_track = null;
                Log.d("SpotifyService", "Disconnected from Spotify");
            }).setErrorCallback(throwable -> {
                Log.e("SpotifyService", "Error stopping playback and disconnecting", throwable);
                SpotifyAppRemote.disconnect(mSpotifyAppRemote);
                mSpotifyAppRemote = null;
                current_track = null;
            });
        }
    }
}
