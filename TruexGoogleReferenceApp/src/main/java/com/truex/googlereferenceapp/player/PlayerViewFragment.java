package com.truex.googlereferenceapp.player;

import static androidx.media3.common.C.CONTENT_TYPE_HLS;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.ima.ImaServerSideAdInsertionMediaSource;
import androidx.media3.exoplayer.ima.ImaServerSideAdInsertionUriBuilder;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.truex.googlereferenceapp.R;
import com.truex.googlereferenceapp.home.StreamConfiguration;

@OptIn(markerClass = UnstableApi.class)
public class PlayerViewFragment extends Fragment {
    private static final String CLASSTAG = VideoPlayer.class.getSimpleName();

    // The stream configuration for the selected content
    // The Video ID and Content ID are used to initialize the stream with the IMA SDK
    // These values should be set based on your stream
    private StreamConfiguration streamConfiguration;

    private ExoPlayer player;
    private PlayerView playerView;
    private ImaServerSideAdInsertionMediaSource.AdsLoader adsLoader;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        playerView = getView().findViewById(R.id.player_view);

        // Retrieve the stream configuration from the arguments bundle
        this.streamConfiguration = getArguments().getParcelable(StreamConfiguration.class.getSimpleName());

        initializePlayer();
    }

    @Override
    public void onPause() {
        super.onPause();
        playerView.onPause();
        if (player != null) player.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        playerView.onResume();
        if (player != null) player.setPlayWhenReady(true);
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }

    // Create a server side ad insertion (SSAI) AdsLoader.
    private ImaServerSideAdInsertionMediaSource.AdsLoader createAdsLoader() {
        ImaServerSideAdInsertionMediaSource.AdsLoader.Builder adsLoaderBuilder =
                new ImaServerSideAdInsertionMediaSource.AdsLoader.Builder(getContext(), playerView);

        return adsLoaderBuilder.setAdEventListener(buildAdEventListener()).build();
    }

    public AdEvent.AdEventListener buildAdEventListener() {
        AdEvent.AdEventListener imaAdEventListener =
                event -> {
                    AdEvent.AdEventType eventType = event.getType();
                    if (eventType == AdEvent.AdEventType.AD_PROGRESS) {
                        return;
                    }
                    String log = "IMA event: " + eventType;
                    Log.i(CLASSTAG, log);
                };

        return imaAdEventListener;
    }

    private void initializePlayer() {
        if (player != null) return;

        adsLoader = createAdsLoader();

        Context context = getContext();

        // Set up the factory for media sources, passing the ads loader.
        DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context);

        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(dataSourceFactory);

        // MediaSource.Factory to create the ad sources for the current player.
        ImaServerSideAdInsertionMediaSource.Factory adsMediaSourceFactory =
                new ImaServerSideAdInsertionMediaSource.Factory(adsLoader, mediaSourceFactory);

        // 'mediaSourceFactory' is an ExoPlayer component for the DefaultMediaSourceFactory.
        // 'adsMediaSourceFactory' is an ExoPlayer component for a MediaSource factory for IMA server
        // side inserted ad streams.
        mediaSourceFactory.setServerSideAdInsertionMediaSourceFactory(adsMediaSourceFactory);

        // Create a SimpleExoPlayer and set it as the player for content and ads.
        player = new ExoPlayer.Builder(context).setMediaSourceFactory(mediaSourceFactory).build();
        playerView.setPlayer(player);
        adsLoader.setPlayer(player);

        // Build an IMA SSAI media item to prepare the player with.
        Uri ssaiLiveUri =
                new ImaServerSideAdInsertionUriBuilder()
                        .setContentSourceId(streamConfiguration.getContentID())
                        .setVideoId(streamConfiguration.getVideoID())
                        .setFormat(CONTENT_TYPE_HLS) // Use CONTENT_TYPE_DASH for dash streams.
                        .build();

        // Create the MediaItem to play, specifying the stream URI.
        MediaItem ssaiMediaItem = MediaItem.fromUri(ssaiLiveUri);

        // Prepare the content and ad to be played with the ExoPlayer.
        player.setMediaItem(ssaiMediaItem);
        player.prepare();

        // Start playing content.
        player.setPlayWhenReady(true);
    }

    private void releasePlayer() {
        playerView.setPlayer(null);

        if (player != null) {
            player.release();
            player = null;
        }

        if (adsLoader != null) {
            adsLoader.release();
        }
    }
}
