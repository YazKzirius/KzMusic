package com.example.kzmusic;

//Imports
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//This manages Spotify Music recycler view display
public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {
    //Important attributes
    private List<SearchResponse.Track> trackList;
    private Context context;
    private OnItemClickListener Listener;
    SessionManager sessionManager;
    String email;
    String username;
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
        holder.trackName.setText(position+1+". "+track.getName());
        holder.artistName.setText(track.getArtists().get(0).getName());
        holder.release.setText((track.getAlbum().getRelease_date().split("-"))[0]);
        SavedSongsFirestore table = new SavedSongsFirestore(context);
        sessionManager = new SessionManager(context);
        username = sessionManager.getUsername();
        email = sessionManager.getEmail();
        //Checking if song is liked and displaying necessary icons
        String title = track.getName()+" by "+track.getArtists().get(0).getName(
        );
        table.db.collection("Users").whereEqualTo("EMAIL", email).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String user_id = querySnapshot.getDocuments().get(0).getId();
                        // 🔥 Check if the same song exists for the user
                        table.db.collection("SavedSongs")
                                .whereEqualTo("TITLE", title)
                                .whereEqualTo("USER_ID", user_id) // Ensure user does not have this song already
                                .get()
                                .addOnSuccessListener(songSnapshot -> {
                                    if (songSnapshot.isEmpty()) {
                                        // ✅ Song is unique for this user, proceed to add
                                        holder.liked.setImageResource(R.drawable.ic_liked_off);
                                    } else {
                                        holder.liked.setImageResource(R.drawable.ic_liked);
                                    }
                                })
                                .addOnFailureListener(e -> Log.e("Firebase", "Error checking song existence", e));
                    } else {
                        Log.e("Firebase", "User not found.");
                    }
                })
                .addOnFailureListener(e -> Log.e("Firebase", "Error retrieving user", e));
        // Use Glide to load album image
        Glide.with(context)
                .load(track.getAlbum().getImages().get(0).getUrl())
                .into(holder.albumImage);
        //Implementing liked button functionality
        holder.liked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SavedSongsFirestore table = new SavedSongsFirestore(context);
                sessionManager = new SessionManager(context);
                username = sessionManager.getUsername();
                email = sessionManager.getEmail();
                String title = track.getName()+" by "+track.getArtists().get(0).getName();
                String url = track.getAlbum().getImages().get(0).getUrl();
                //Saving songs to Saved collection if not already saved
                table.db.collection("Users").whereEqualTo("EMAIL", email).get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                String user_id = querySnapshot.getDocuments().get(0).getId();
                                // 🔥 Check if the same song exists for the user
                                table.db.collection("SavedSongs")
                                        .whereEqualTo("TITLE", title)
                                        .whereEqualTo("USER_ID", user_id) // Ensure user does not have this song already
                                        .get()
                                        .addOnSuccessListener(songSnapshot -> {
                                            if (songSnapshot.isEmpty()) {
                                                table.save_new_song(email, title, url);
                                                holder.liked.setImageResource(R.drawable.ic_liked);
                                            } else {
                                                table.remove_saved_song(email, title);
                                                holder.liked.setImageResource(R.drawable.ic_liked_off);
                                            }
                                        })
                                        .addOnFailureListener(e -> Log.e("Firebase", "Error checking song existence", e));
                            } else {
                                Log.e("Firebase", "User not found.");
                            }
                        })
                        .addOnFailureListener(e -> Log.e("Firebase", "Error retrieving user", e));
            }
        });
        holder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(v);
            }
        });
    }
    //This function shows pop up menu when menu button is clicked
    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.track_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_go_to_artist:
                        Toast.makeText(context, "Go to artist selected", Toast.LENGTH_SHORT).show();
                        // Add your logic here
                        return true;
                    case R.id.menu_go_to_album:
                        Toast.makeText(context, "Go to album selected", Toast.LENGTH_SHORT).show();
                        // Add your logic here
                        return true;
                    case R.id.menu_play_similar_songs:
                        Toast.makeText(context, "Play similar songs selected", Toast.LENGTH_SHORT).show();
                        // Add your logic here
                        return true;
                    default:
                        return false;
                }
            }
        });
        popupMenu.show();
    }
    @Override
    //This function gets the number of tracks in tracklist
    public int getItemCount() {
        return trackList.size();
    }
    //This function updates the tracks in the tracklist
    public void updateTracks(List<SearchResponse.Track> newTracks) {
        trackList.addAll(newTracks);
        notifyDataSetChanged();
    }
    public void clear_tracks() {
        trackList.clear();
        notifyDataSetChanged();
    }
    //This function updates the tracks in the tracklist
    public List<SearchResponse.Track> get_tracks() {
        return trackList;
    }
    //This class creates the Music item view holder display
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView trackName;
        public TextView artistName;
        public ImageView albumImage;
        TextView release;
        ImageButton liked;
        ImageButton menu;

        public ViewHolder(View itemView) {
            super(itemView);
            trackName = itemView.findViewById(R.id.track_name);
            artistName = itemView.findViewById(R.id.artist_name);
            albumImage = itemView.findViewById(R.id.album_image);
            menu = itemView.findViewById(R.id.menu_btn);
            liked = itemView.findViewById(R.id.liked_btn);
            release = itemView.findViewById(R.id.release_date);
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
