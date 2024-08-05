package com.example.kzmusic;

import com.google.android.exoplayer2.ExoPlayer;

import java.util.ArrayList;
import java.util.List;

//This class implements a queue data structure for the songs played
public class SongQueue {
    private static SongQueue instance;
    List<MusicFile> songs_played;
    MusicFile current_song;
    int current_position = -1;
    Float speed = (float) 1;
    Float pitch = (float) 1;
    int reverb_level = -1000;
    Boolean is_looping = false;
    Boolean shuffle_on = false;
    long current_time = 0;
    int pointer = 0;
    private SongQueue() {
        this.songs_played = new ArrayList<>();

    }

    public static synchronized SongQueue getInstance() {
        if (instance == null) {
            instance = new SongQueue();
        }
        return instance;
    }
    public void addSong(MusicFile song) {
        songs_played.add(song);
        this.current_song = song;
        this.pointer += 1;
    }
    public void setPosition(int pos) {
        current_position = pos;
    }
    public void setSpeed(Float speed) {
        this.speed = speed;
    }

    public void setPitch(Float pitch) {
        this.pitch = pitch;
    }

    public void setReverb_level(int reverb_level) {
        this.reverb_level = reverb_level;
    }

    public void setIs_looping(Boolean is_looping) {
        this.is_looping = is_looping;
    }

    public void setShuffle_on(Boolean shuffle_on) {
        this.shuffle_on = shuffle_on;
    }

    public void setCurrent_time(long current_time) {
        this.current_time = current_time;
    }
    public int get_size() {
        return songs_played.size();
    }
    public MusicFile get_specified(int index) {
        return songs_played.get(index);
    }
}
