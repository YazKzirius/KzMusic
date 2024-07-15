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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.List;

public class MusicFileAdapter extends RecyclerView.Adapter<MusicFileAdapter.MusicViewHolder> {

    private List<MusicFile> musicFiles;
    private Context context;
    private MediaPlayer mediaPlayer;
    private int currentPlayingPosition = -1;

    public MusicFileAdapter(Context context, List<MusicFile> musicFiles) {
        this.context = context;
        this.musicFiles = musicFiles;
        this.mediaPlayer = new MediaPlayer();
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
                playMusic(musicFile, holder.getAdapterPosition());
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

    private void playMusic(MusicFile musicFile, int position) {
        try {
            if (currentPlayingPosition == position) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.start();
                }
            } else {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                }
                mediaPlayer.setDataSource(context, Uri.parse(musicFile.getPath()));
                mediaPlayer.prepare();
                mediaPlayer.start();
                currentPlayingPosition = currentPlayingPosition;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
