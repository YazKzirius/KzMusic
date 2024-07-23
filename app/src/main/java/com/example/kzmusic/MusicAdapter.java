package com.example.kzmusic;

//Imports
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

//This manages Spotify Music recycler view display
public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {
    //Important attributes
    private List<SearchResponse.Track> trackList;
    private Context context;
    private OnItemClickListener Listener;
    public interface OnItemClickListener {
        void onItemClick(SearchResponse.Track track);
    }
    public MusicAdapter(List<SearchResponse.Track> trackList, Context context, OnItemClickListener listener) {
        this.trackList = trackList;
        this.context = context;
        this.Listener = listener;
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
        holder.bind(track, Listener);
        holder.trackName.setText(track.getName());
        holder.artistName.setText(track.getArtists().get(0).getName());

        // Use Glide to load album image
        Glide.with(context)
                .load(track.getAlbum().getImages().get(0).getUrl())
                .into(holder.albumImage);
    }

    @Override
    //This function gets the number of tracks in tracklist
    public int getItemCount() {
        return trackList.size();
    }
    //This function updates the tracks in the tracklist
    public void updateTracks(List<SearchResponse.Track> newTracks) {
        trackList = newTracks;
        notifyDataSetChanged();
    }

    //This class creates the Music item view holder display
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView trackName;
        public TextView artistName;
        public ImageView albumImage;

        public ViewHolder(View itemView) {
            super(itemView);
            trackName = itemView.findViewById(R.id.track_name);
            artistName = itemView.findViewById(R.id.artist_name);
            albumImage = itemView.findViewById(R.id.album_image);
        }
        //This function manages music item view clicking
        public void bind(final SearchResponse.Track track, final OnItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(track);
                }
            });
        }
    }
}
