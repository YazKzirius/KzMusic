package com.example.kzmusic;

import android.content.Context;
import android.util.Log;

import com.spotify.android.appremote.api.SpotifyAppRemote;

public class OnlinePlayerManager {
    SpotifyAppRemote mSpotifyAppRemote;
    SearchResponse.Track current_track;
    Context current_context;
    private static OnlinePlayerManager instance;
    String access_token;
    String refresh_token;
    long expiration_time = 0;
    public static synchronized OnlinePlayerManager getInstance() {
        if (instance == null) {
            instance = new OnlinePlayerManager();
        }
        return instance;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public void setExpiration_time(long expiration_time) {
        this.expiration_time = expiration_time;
    }

    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
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

    public String getAccess_token() {
        return access_token;
    }

    public long getExpiration_time() {
        return expiration_time;
    }

    public String getRefresh_token() {
        return refresh_token;
    }
}
