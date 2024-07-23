package com.example.kzmusic;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.net.Uri;
import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import java.io.File;
import android.widget.ImageButton;
import android.widget.Toast;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MediaOverlay#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MediaOverlay extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    View view;
    MusicFile musicFile;
    private ImageView overlayImage;
    private TextView overlaySongTitle;
    private ImageButton btnPlayPause;
    private Handler handler = new Handler();
    private ImageButton btnLoop;
    private SeekBar seekBar;
    private SimpleExoPlayer player;
    private TextView textCurrentTime, textTotalDuration;
    private CircularImageViewWithBeatTracker imageViewWithBeatTracker;
    private Runnable beatRunnable;
    private float[] beatLevels = new float[150];
    public MediaOverlay() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MediaOverlay.
     */
    // TODO: Rename and change types and number of parameters
    public static MediaOverlay newInstance(String param1, String param2) {
        MediaOverlay fragment = new MediaOverlay();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_media_overlay, container, false);
        overlayImage = view.findViewById(R.id.musicImage);
        overlaySongTitle = view.findViewById(R.id.songTitle);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnLoop = view.findViewById(R.id.btnLoop);
        seekBar = view.findViewById(R.id.seekBar);
        textCurrentTime = view.findViewById(R.id.textCurrentTime);
        textTotalDuration = view.findViewById(R.id.textTotalDuration);
        player = new SimpleExoPlayer.Builder(getContext()).build();
        if (getArguments() != null) {
            musicFile = getArguments().getParcelable("song");
            if (musicFile != null) {
                // Set song details
                playMusic(musicFile);
                overlaySongTitle.setText(musicFile.getName()+" by "+musicFile.getArtist());
                Toast.makeText(getContext(),"Playing: "+musicFile.getName(), Toast.LENGTH_SHORT).show();
                // Load album image
                Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
                Uri album_uri = Uri.withAppendedPath(albumArtUri, String.valueOf(musicFile.getAlbumId()));
                imageViewWithBeatTracker = view.findViewById(R.id.musicImage);

                // Load image using Glide
                imageViewWithBeatTracker.loadImage(album_uri);
                //Pause/play functionality
                btnPlayPause.setOnClickListener(v -> {
                    if (player.isPlaying()) {
                        player.pause();
                        btnPlayPause.setImageResource(R.drawable.ic_play);
                    } else {
                        player.play();
                        btnPlayPause.setImageResource(R.drawable.ic_pause);
                    }
                });
                // Example: Update beat levels every 500ms
                beatRunnable = new Runnable() {
                    @Override
                    public void run() {
                        // Generate random beat levels for demonstration
                        for (int i = 0; i < beatLevels.length; i++) {
                            beatLevels[i] = (float) Math.random();
                        }
                        imageViewWithBeatTracker.updateBeatLevels(beatLevels);
                        handler.postDelayed(this, 150);
                    }
                };

                handler.post(beatRunnable);
                //Loop functionality
                btnLoop.setOnClickListener(v -> {
                    boolean loop = player.getRepeatMode() == SimpleExoPlayer.REPEAT_MODE_ONE;
                    player.setRepeatMode(loop ? SimpleExoPlayer.REPEAT_MODE_OFF : SimpleExoPlayer.REPEAT_MODE_ONE);
                    btnLoop.setImageResource(loop ? R.drawable.ic_loop : R.drawable.ic_loop);
                });
                //Seekbar functionality
            }
        }
        return view;
    }
    //This function plays the specified music file
    private void playMusic(MusicFile musicFile) {
        Uri uri = Uri.fromFile(new File(musicFile.getPath()));
        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (player != null) {
            player.release();
        }
    }
}