package com.example.kzmusic;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class MediaPlayerNavigationBar extends LinearLayout {

    private ImageButton btnPlayPause;
    private SeekBar seekBar;
    private ImageButton btnLoop;
    private ImageButton btnSettings;

    public MediaPlayerNavigationBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.media_player_navigation_bar, this, true);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        seekBar = findViewById(R.id.seekBar);
        btnLoop = findViewById(R.id.btnLoop);
        btnSettings = findViewById(R.id.btnSettings);
    }

    public ImageButton getPlayPauseButton() {
        return btnPlayPause;
    }

    public SeekBar getSeekBar() {
        return seekBar;
    }

    public ImageButton getLoopButton() {
        return btnLoop;
    }

    public ImageButton getSettingsButton() {
        return btnSettings;
    }
}
