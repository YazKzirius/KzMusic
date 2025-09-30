package com.example.kzmusic;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private Context context;
    private List<Playlist> playlistList;


    public PlaylistAdapter(Context context, List<Playlist> playlistList) {
        this.context = context;
        this.playlistList = playlistList;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlistList.get(position);
        holder.playlistTitle.setText(playlist.title);
        //Adding number of tracks to recycler view
        SessionManager sessionManager = new SessionManager(context);
        String email = sessionManager.getEmail();
        String playlist_name = playlist.title;
        PlaylistSongDao playlistSongDao = AppDatabase.getDatabase(context).playlistSongDao();
        PlaylistDao playlistDao = AppDatabase.getDatabase(context).playlistDao();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<String> songs = playlistSongDao.get_playlist_songs(email, playlistDao.getPlaylistIdByEmailAndTitle(email, playlist_name));
            String url = playlistDao.getUrl(email, playlist_name);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (songs == null || songs.isEmpty()) {
                    ;
                } else {
                    holder.num_tracks.setText(songs.size() + " Tracks");
                }
                if (url != null && !url.isEmpty()) {
                    Glide.with(context)
                            .load(Uri.parse(url))
                            .error(R.drawable.logo) // Fallback image if loading fails
                            .into(holder.playlistArt);
                } else {
                    // Set default image if URL is missing
                    holder.playlistArt.setImageResource(R.drawable.logo);
                }
            });
        });
        holder.itemView.setOnClickListener(v -> {
            open_overlay(playlist);
        });
    }
    @Override
    public int getItemCount() {
        return playlistList.size();
    }
    //This function opens playlist overlay
    public void open_overlay(Playlist playlist) {
        Fragment playlistOverlay = new PlaylistOverlay();
        //Adding song to queue
        SongQueue.getInstance().set_current_playlist(playlist.title);
        //Opening fragment
        FragmentManager fragmentManager = ((AppCompatActivity) context).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, playlistOverlay);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        TextView playlistTitle;
        ImageView playlistArt;
        TextView num_tracks;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            playlistTitle = itemView.findViewById(R.id.playlist_title);
            playlistArt = itemView.findViewById(R.id.playlist_icon);
            num_tracks = itemView.findViewById(R.id.no_tracks_label);
        }
    }
}
