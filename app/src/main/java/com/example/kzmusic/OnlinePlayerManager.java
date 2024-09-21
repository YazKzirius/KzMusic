package com.example.kzmusic;

import android.content.Context;
import android.util.Log;

import com.spotify.android.appremote.api.SpotifyAppRemote;

public class OnlinePlayerManager {
    SpotifyAppRemote mSpotifyAppRemote;
    SearchResponse.Track current_track;
    Context current_context;
    private static OnlinePlayerManager instance;
    public static synchronized OnlinePlayerManager getInstance() {
        if (instance == null) {
            instance = new OnlinePlayerManager();
        }
        return instance;
    }

    public void setCurrent_track(SearchResponse.Track current_track) {
        this.current_track = current_track;
    }

    public void setCurrent_context(Context current_context) {
        this.current_context = current_context;
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
