package com.example.kzmusic;

import android.media.audiofx.EnvironmentalReverb;

import com.google.android.exoplayer2.ExoPlayer;

import java.util.ArrayList;
import java.util.List;

//This class implements a queue data structure for the songs played
public class SongQueue {
    private static SongQueue instance;
    List<MusicFile> song_list;
    List<MusicFile> songs_played;
    MusicFile current_song;
    String CHANNEL_ID = "media_playback_channel";
    int NOTIFICATION_ID = 1;
    int current_position = -1;
    Float speed = (float) 1;
    Float pitch = (float) 1;
    int reverb_level = -1000;
    Boolean is_looping = false;
    Boolean shuffle_on = false;
    EnvironmentalReverb reverb;
    long current_time = 0;
    long last_postion = 0;
    int pointer = 0;
    int current_resource;
    int audio_session_id;
    int duration_played = 0;
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
    public void clear_songs() {
        this.songs_played.clear();
    }
    public void setSong_list(List<MusicFile> song_list) {
        this.song_list = song_list;
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

    public void setCurrent_resource(int current_resource) {
        this.current_resource = current_resource;
    }

    public void setLast_postion(long last_postion) {
        this.last_postion = last_postion;
    }

    public void setAudio_session_id(int audio_session_id) {
        this.audio_session_id = audio_session_id;
    }

    public int getAudio_session_id() {
        return audio_session_id;
    }

    public long getLast_postion() {
        return last_postion;
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
    public void initialize_reverb(int sessionId) {
        if (this.reverb != null) {
            this.reverb.release();
            this.reverb = null;
        }
        this.reverb = new EnvironmentalReverb(0, sessionId);
    }
    public void update_id() {
        NOTIFICATION_ID += 1;
        CHANNEL_ID += ""+NOTIFICATION_ID;
    }
    public void update_duration(int duration) {
        duration_played += duration;
    }

    public void setDuration_played(int duration_played) {
        this.duration_played = duration_played;
    }

    //This function checks if a string is only digits
    public boolean isOnlyDigits(String str) {
        str = str.replaceAll(" ", "");
        if (str == null || str.isEmpty()) {
            return false;
        }

        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    //This function formats song title, removing unnecessary data
    public String format_title(String title) {
        //Removing unnecessary data
        title = title.replace("[SPOTIFY-DOWNLOADER.COM] ", "").replace(".mp3", "").replaceAll("_", " ").replaceAll("  ", " ").replace(".flac", "").replace(".wav", "");
        //Checking if prefix is a number
        String prefix = title.charAt(0) + "" + title.charAt(1) + "" + title.charAt(2);
        //Checking if title ends with empty space
        if (title.endsWith(" ")) {
            title = title.substring(0, title.lastIndexOf(" "));
        }
        //Checking if prefix is at the start and if it occurs again
        if (isOnlyDigits(prefix) && title.indexOf(prefix) == 0 && title.indexOf(prefix, 2) == -1) {
            //Removing prefix
            title = title.replaceFirst(prefix, "");
        } else {
            ;
        }
        return title;
    }

}
