package com.example.kzmusic;

//Imports
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.SimpleExoPlayer;

import com.bumptech.glide.Glide;

import java.util.List;

//This class manages music files in user directory
public class MusicFileAdapter extends RecyclerView.Adapter<MusicFileAdapter.MusicViewHolder> {

    private List<MusicFile> musicFiles;
    private Context context;
    private SimpleExoPlayer player;
    private MusicViewHolder currentlyPlayingHolder;


    public MusicFileAdapter(Context context, List<MusicFile> musicFiles) {
        this.context = context;
        this.musicFiles = musicFiles;
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        MusicFile musicFile = musicFiles.get(position);
        //Updating song position
        int new_position = SongQueue.getInstance().song_list.indexOf(musicFile);
        holder.nameTextView.setText(new_position+1+". "+format_title(musicFile.getName()));
        holder.artistTextView.setText(musicFile.getArtist().replaceAll("/",", "));
        Uri albumArtUri = getAlbumArtUri(musicFile.getAlbumId());
        Glide.with(context).asBitmap().load(albumArtUri).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                holder.albumImageView.setImageBitmap(resource);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                ;
            }
            @Override
            public void onLoadFailed(@Nullable Drawable errordrawable) {
                Glide.with(context)
                        .asBitmap()
                        .load(R.drawable.logo) // Backup image resource
                        .circleCrop()
                        .into(holder.albumImageView);
            }

        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Opening media playback overlay
                open_overlay(musicFile, new_position);
                currentlyPlayingHolder = holder;
            }
        });
    }
    //This function formats song title display
    //Removes unnecessary data from title
    public String format_title(String title) {
        //Removing unnecessary data
       title = title.replace("[SPOTIFY-DOWNLOADER.COM] ", "").replace(".mp3", "").replaceAll("_", " ").replaceAll("  ", " ").replace(".flac", "").replace(".wav", "");
       //Checking if prefix is a number
       String prefix = title.charAt(0)+""+title.charAt(1)+""+title.charAt(2);
       //Checking if prefix is at the start and if it occurs again
       if (isOnlyDigits(prefix) && title.indexOf(prefix) == 0 && title.indexOf(prefix, 2) == -1) {
           //Removing prefix
           title = title.replaceFirst(prefix, "");
       } else {
           ;
       }
       return title;
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

    @Override
    public int getItemCount() {
        return musicFiles.size();
    }
    //This function opens the playback handling overlay
    //Whilst sending necessary data over
    public void open_overlay(MusicFile musicFile, int position) {
        Fragment media_page = new MediaOverlay();
        //Adding song to queue
        SongQueue.getInstance().addSong(musicFile);
        SongQueue.getInstance().setPosition(position);
        //Opening fragment
        FragmentManager fragmentManager = ((AppCompatActivity) context).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, media_page);
        fragmentTransaction.commit();
    }
    private Uri getAlbumArtUri(long albumId) {
        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        return Uri.withAppendedPath(albumArtUri, String.valueOf(albumId));
    }
    //This class implements the music recycler view holder
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
