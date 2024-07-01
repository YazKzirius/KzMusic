package com.example.kzmusic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {

    private List<SearchResponse.Track> trackList;
    private Context context;

    public MusicAdapter(List<SearchResponse.Track> trackList, Context context) {
        this.trackList = trackList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResponse.Track track = trackList.get(position);
        holder.trackName.setText(track.getName());
        holder.artistName.setText(track.getArtists().get(0).getName());

        // Use Glide to load album image
        Glide.with(context)
                .load(track.getAlbum().getImages().get(0).getUrl())
                .into(holder.albumImage);
    }

    @Override
    public int getItemCount() {
        return trackList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView trackName;
        public TextView artistName;
        public ImageView albumImage;

        public ViewHolder(View view) {
            super(view);
            trackName = view.findViewById(R.id.track_name);
            artistName = view.findViewById(R.id.artist_name);
            albumImage = view.findViewById(R.id.album_image);
        }
    }
}
