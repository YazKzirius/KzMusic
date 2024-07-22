package com.example.kzmusic;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MusicFileAdapter extends RecyclerView.Adapter<MusicFileAdapter.MusicViewHolder> {

    private List<MusicFile> musicFiles;
    private Context context;
    private MediaPlayer mediaPlayer;
    private int currentPlayingPosition = -1;
    private SimpleExoPlayer player;
    private MediaPlayerNavigationBar mediaPlayerNavigationBar;

    public MusicFileAdapter(Context context, List<MusicFile> musicFiles, MediaPlayerNavigationBar mediaPlayerNavigationBar) {
        this.context = context;
        this.musicFiles = musicFiles;
        this.mediaPlayerNavigationBar = mediaPlayerNavigationBar;
        player = new SimpleExoPlayer.Builder(context).build();
        setUpMediaControls();
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        MusicFile musicFile = musicFiles.get(position);
        holder.nameTextView.setText(musicFile.getName());
        holder.artistTextView.setText(musicFile.getArtist());

        Uri albumArtUri = getAlbumArtUri(musicFile.getAlbumId());
        Glide.with(context)
                .load(albumArtUri)
                .into(holder.albumImageView);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playMusic(musicFile);
                Toast.makeText(context, "Playing: "+musicFile.getName(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public int getItemCount() {
        return musicFiles.size();
    }

    private Uri getAlbumArtUri(long albumId) {
        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        return Uri.withAppendedPath(albumArtUri, String.valueOf(albumId));
    }
    private void setUpMediaControls() {
        mediaPlayerNavigationBar.getPlayPauseButton().setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
                mediaPlayerNavigationBar.getPlayPauseButton().setImageResource(R.drawable.ic_play);
            } else {
                player.play();
                mediaPlayerNavigationBar.getPlayPauseButton().setImageResource(R.drawable.ic_pause);
            }
        });

        mediaPlayerNavigationBar.getSeekBar().setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    player.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mediaPlayerNavigationBar.getLoopButton().setOnClickListener(v -> {
            boolean loop = player.getRepeatMode() == Player.REPEAT_MODE_ONE;
            player.setRepeatMode(loop ? Player.REPEAT_MODE_OFF : Player.REPEAT_MODE_ONE);
            mediaPlayerNavigationBar.getLoopButton().setImageResource(loop ? R.drawable.ic_loop : R.drawable.ic_loop);
        });

        mediaPlayerNavigationBar.getSettingsButton().setOnClickListener(v -> {
            // Handle settings click
        });

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == Player.STATE_READY && playWhenReady) {
                    mediaPlayerNavigationBar.getPlayPauseButton().setImageResource(R.drawable.ic_pause);
                } else {
                    mediaPlayerNavigationBar.getPlayPauseButton().setImageResource(R.drawable.ic_play);
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    mediaPlayerNavigationBar.getPlayPauseButton().setImageResource(R.drawable.ic_pause);
                } else {
                    mediaPlayerNavigationBar.getPlayPauseButton().setImageResource(R.drawable.ic_play);
                }
            }
        });
    }

    private void playMusic(MusicFile musicFile) {
        Uri uri = Uri.fromFile(new File(musicFile.getPath()));
        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }
    static class MusicViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView artistTextView;
        ImageView albumImageView;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.track_name);
            artistTextView = itemView.findViewById(R.id.artist_name);
            albumImageView = itemView.findViewById(R.id.album_image);
        }

    }
}
