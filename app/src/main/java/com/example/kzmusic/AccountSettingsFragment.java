package com.example.kzmusic;

//Imports
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.media.session.MediaButtonReceiver;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

import java.util.Random;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AccountSettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

//Account settings class
public class AccountSettingsFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    String mParam1;
    String mParam2;
    View view;
    String username;
    String email;
    ImageView art;
    TextView title;
    TextView Artist;
    ImageButton ic_down;
    RelativeLayout playback_bar;
    private static final String CHANNEL_ID = "media_playback_channel5";
    private static final int NOTIFICATION_ID = 5;
    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    public AccountSettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AccountSettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AccountSettingsFragment newInstance(String param1, String param2) {
        AccountSettingsFragment fragment = new AccountSettingsFragment();
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
        view = inflater.inflate(R.layout.fragment_account_settings, container, false);
        art = view.findViewById(R.id.current_song_art);
        title = view.findViewById(R.id.current_song_title);
        Artist = view.findViewById(R.id.current_song_artist);
        ic_down = view.findViewById(R.id.down_button);
        playback_bar = view.findViewById(R.id.playback_bar);
        //Setting up bottom playback navigator
        set_up_spotify_play();
        set_up_play_bar();
        if (SongQueue.getInstance().current_song != null) {
            createNotificationChannel();
            initializeMediaSession();
        } else {
            ;
        }
        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ;
    }
    //This function assigns data from playback overlay to bottom navigation
    public void set_up_play_bar() {
        if (SongQueue.getInstance().songs_played.size() == 0) {
            ;
        } else {
            MusicFile song = SongQueue.getInstance().current_song;
            int pos = SongQueue.getInstance().current_position;
            design_bar();
            //When bottom song navigator is clicked, relocate back to playback overlay
            Artist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    open_new_overlay(song, pos);
                }
            });
            title.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    open_new_overlay(song, pos);
                }
            });
            art.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    open_new_overlay(song, pos);
                }
            });
            playback_bar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    open_new_overlay(song, pos);
                }
            });
            //Implementing down button functionality
            ic_down.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Opening song overay
                    open_new_overlay(song, pos);}
            });
        }
    }
    //This function designs the bottom playback bar
    public void design_bar() {
        MusicFile song = SongQueue.getInstance().current_song;
        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        Uri album_uri = Uri.withAppendedPath(albumArtUri, String.valueOf(song.getAlbumId()));
        Glide.with(getContext()).asBitmap().load(album_uri).circleCrop().into(art);
        title.setText("Now playing "+format_title(song.getName()));
        Artist.setText(song.getArtist().replaceAll("/", ", "));
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
    //This function opens a new song overlay
    public void open_new_overlay(MusicFile file, int position) {
        //Adding song to queue
        SongQueue.getInstance().addSong(file);
        SongQueue.getInstance().setPosition(position);
        Fragment media_page = new MediaOverlay();
        FragmentManager fragmentManager = ((AppCompatActivity) getContext()).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, media_page);
        fragmentTransaction.commit();
    }
    //This function handles Spotify overlay play/pause
    public void set_up_spotify_play() {
        if (SpotifyPlayerLife.getInstance().mSpotifyAppRemote != null) {
            SpotifyPlayerLife.getInstance().mSpotifyAppRemote.getPlayerApi().subscribeToPlayerState().setEventCallback(new Subscription.EventCallback<PlayerState>() {
                @Override
                public void onEvent(PlayerState playerState) {
                    if (playerState.isPaused) {
                        ;
                    } else {
                        if (PlayerManager.getInstance().current_player != null) {
                            PlayerManager.getInstance().current_player.pause();
                        } else {
                            ;
                        }
                    }
                }
            });
        }
    }
    //This function creates the media playback notification channel
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Media playback",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Media playback controls");

            NotificationManager notificationManager = getActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    //This function creates the playback controls for notification channel
    private void showNotification(PlaybackStateCompat state) {
        // Load album image
        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        Uri album_uri = Uri.withAppendedPath(albumArtUri, String.valueOf(SongQueue.getInstance().current_song.getAlbumId()));
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), CHANNEL_ID);
        builder.setContentTitle(format_title(SongQueue.getInstance().current_song.getName()))
                .setContentText(SongQueue.getInstance().current_song.getArtist().replaceAll("/", ", "))
                .setSmallIcon(R.drawable.library)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0));
        Glide.with(getContext()).asBitmap().load(album_uri).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                //Once the resource loads, it changes to that background picture
                builder.setLargeIcon(resource);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                ;
            }
        });
        if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_pause, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(
                    getContext(), PlaybackStateCompat.ACTION_PAUSE)));
        } else {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_play, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(
                    getContext(), PlaybackStateCompat.ACTION_PLAY)));
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getContext());
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
    //This function creates a new media session for specific player
    private void initializeMediaSession() {
        mediaSession = new MediaSessionCompat(getContext(), "ExoPlayerMediaSession");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        Random rand = new Random();
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY)
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, SongQueue.getInstance().speed);
        mediaSession.setPlaybackState(stateBuilder.build());
        PlayerManager.getInstance().addSession(mediaSession);
        showNotification(stateBuilder.build());
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                // Update playback state to playing
                PlayerManager.getInstance().current_player.play();
                stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f);
                mediaSession.setPlaybackState(stateBuilder.build());
                showNotification(stateBuilder.build());
            }

            @Override
            public void onPause() {
                super.onPause();
                //Update playback state to paused
                PlayerManager.getInstance().current_player.pause();
                stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f);
                mediaSession.setPlaybackState(stateBuilder.build());
                showNotification(stateBuilder.build());
            }
        });
        mediaSession.setActive(true);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaSession != null) {
            mediaSession.getController().getTransportControls().stop();
            mediaSession.release();
        }
    }
}